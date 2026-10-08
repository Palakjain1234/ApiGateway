package com.gatekeeper.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatekeeper.management.audit.Auditable;
import com.gatekeeper.management.dto.request.CreateRouteRequest;
import com.gatekeeper.management.dto.request.UpdateRouteRequest;
import com.gatekeeper.management.dto.response.ActiveRouteConfigResponse;
import com.gatekeeper.management.dto.response.RouteResponse;
import com.gatekeeper.management.dto.response.RouteVersionResponse;
import com.gatekeeper.management.entity.*;
import com.gatekeeper.management.enums.RouteChangeType;
import com.gatekeeper.management.exception.BadRequestException;
import com.gatekeeper.management.exception.ConflictException;
import com.gatekeeper.management.exception.ResourceNotFoundException;
import com.gatekeeper.management.kafka.RouteChangeEvent;
import com.gatekeeper.management.kafka.RouteEventPublisher;
import com.gatekeeper.management.mapper.RouteMapper;
import com.gatekeeper.management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Business logic for API route management.
 *
 * TENANT_ADMIN operations are scoped to their own organization only.
 * GATEWAY_SERVICE can read all active routes for startup/refresh.
 *
 * Every write operation:
 *   1. Applies the change to api_routes
 *   2. Saves a version snapshot to route_configuration_versions
 *   3. (Kafka event published via AOP or explicit call — see RouteEventPublisher)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteService {

    private final ApiRouteRepository                    routeRepository;
    private final OrganizationRepository                orgRepository;
    private final RouteConfigurationVersionRepository   versionRepository;
    private final RouteMapper                           routeMapper;
    private final ObjectMapper                          objectMapper;
    private final RouteEventPublisher                   eventPublisher;

    // ── Read operations ──────────────────────────────────────────────────────

    public List<RouteResponse> getRoutesForOrganization(Long orgId) {
        validateOrgExists(orgId);
        return routeMapper.toResponseList(routeRepository.findByOrganizationId(orgId));
    }

    public RouteResponse getRoute(Long orgId, String routeId) {
        ApiRoute route = findRouteOrThrow(orgId, routeId);
        return routeMapper.toResponse(route);
    }

    public List<RouteVersionResponse> getRouteHistory(Long orgId, String routeId) {
        validateOrgExists(orgId);
        return routeMapper.toVersionResponseList(
            versionRepository.findByOrganizationIdAndRouteIdOrderByVersionNumberDesc(orgId, routeId)
        );
    }

    /**
     * Used by Smart Gateway (GATEWAY_SERVICE) to load the full active
     * route configuration at startup or after a config-change notification.
     */
    public List<ActiveRouteConfigResponse> getAllActiveRoutes() {
        return routeMapper.toActiveConfigList(
            routeRepository.findAllActiveWithOrganizationAndMethods()
        );
    }

    // ── Write operations (TENANT_ADMIN, scoped to their org) ─────────────────

    @Transactional
    @Auditable(action = "CREATE_ROUTE", resourceType = "ROUTE")
    public RouteResponse createRoute(Long orgId, CreateRouteRequest request) {
        Organization org = findOrgOrThrow(orgId);

        if (routeRepository.existsByOrganizationIdAndRouteId(orgId, request.getRouteId())) {
            throw new ConflictException(
                "Route '" + request.getRouteId() + "' already exists in this organization");
        }

        ApiRoute route = ApiRoute.builder()
                .organization(org)
                .routeId(request.getRouteId())
                .name(request.getName())
                .pathPattern(request.getPathPattern())
                .targetBaseUrl(request.getTargetBaseUrl())
                .targetPathPrefix(request.getTargetPathPrefix())
                .requestsPerMinute(request.getRequestsPerMinute())
                .responseTimeoutMs(request.getResponseTimeoutMs())
                .idempotencyEnabled(request.isIdempotencyEnabled())
                .active(true)
                .configurationVersion(1)
                .build();

        // Attach allowed methods
        request.getAllowedMethods().stream()
                .map(String::toUpperCase)
                .map(m -> RouteAllowedMethod.builder().httpMethod(m).build())
                .forEach(route::addAllowedMethod);

        // Attach header rules
        request.getHeaderRules().forEach(hr -> {
            RouteHeaderRule rule = RouteHeaderRule.builder()
                    .action(hr.getAction())
                    .headerName(hr.getHeaderName())
                    .headerValue(hr.getHeaderValue())
                    .build();
            route.addHeaderRule(rule);
        });

        ApiRoute saved = routeRepository.save(route);
        saveVersion(saved, RouteChangeType.CREATED);
        publishEvent(saved, RouteChangeType.CREATED);

        log.info("Route created: {}/{}", org.getSlug(), saved.getRouteId());
        return routeMapper.toResponse(saved);
    }

    @Transactional
    @Auditable(action = "UPDATE_ROUTE", resourceType = "ROUTE")
    public RouteResponse updateRoute(Long orgId, String routeId, UpdateRouteRequest request) {
        ApiRoute route = findRouteOrThrow(orgId, routeId);

        // Replace the route fields
        route.setName(request.getName());
        route.setPathPattern(request.getPathPattern());
        route.setTargetBaseUrl(request.getTargetBaseUrl());
        route.setTargetPathPrefix(request.getTargetPathPrefix());
        route.setRequestsPerMinute(request.getRequestsPerMinute());
        route.setResponseTimeoutMs(request.getResponseTimeoutMs());
        route.setIdempotencyEnabled(request.isIdempotencyEnabled());
        route.setConfigurationVersion(route.getConfigurationVersion() + 1);

        // Replace allowed methods (orphanRemoval handles the old ones)
        route.getAllowedMethods().clear();
        request.getAllowedMethods().stream()
                .map(String::toUpperCase)
                .map(m -> RouteAllowedMethod.builder().httpMethod(m).build())
                .forEach(route::addAllowedMethod);

        // Replace header rules
        route.getHeaderRules().clear();
        request.getHeaderRules().forEach(hr -> {
            RouteHeaderRule rule = RouteHeaderRule.builder()
                    .action(hr.getAction())
                    .headerName(hr.getHeaderName())
                    .headerValue(hr.getHeaderValue())
                    .build();
            route.addHeaderRule(rule);
        });

        ApiRoute saved = routeRepository.save(route);
        saveVersion(saved, RouteChangeType.UPDATED);
        publishEvent(saved, RouteChangeType.UPDATED);

        log.info("Route updated: {}/{} (v{})", route.getOrganization().getSlug(),
                routeId, saved.getConfigurationVersion());
        return routeMapper.toResponse(saved);
    }

    @Transactional
    @Auditable(action = "ACTIVATE_ROUTE", resourceType = "ROUTE")
    public RouteResponse activateRoute(Long orgId, String routeId) {
        ApiRoute route = findRouteOrThrow(orgId, routeId);
        if (route.isActive()) {
            throw new BadRequestException("Route is already active");
        }
        route.setActive(true);
        route.setConfigurationVersion(route.getConfigurationVersion() + 1);
        ApiRoute saved = routeRepository.save(route);
        saveVersion(saved, RouteChangeType.ACTIVATED);
        publishEvent(saved, RouteChangeType.ACTIVATED);
        return routeMapper.toResponse(saved);
    }

    @Transactional
    @Auditable(action = "DEACTIVATE_ROUTE", resourceType = "ROUTE")
    public RouteResponse deactivateRoute(Long orgId, String routeId) {
        ApiRoute route = findRouteOrThrow(orgId, routeId);
        if (!route.isActive()) {
            throw new BadRequestException("Route is already inactive");
        }
        route.setActive(false);
        route.setConfigurationVersion(route.getConfigurationVersion() + 1);
        ApiRoute saved = routeRepository.save(route);
        saveVersion(saved, RouteChangeType.DEACTIVATED);
        publishEvent(saved, RouteChangeType.DEACTIVATED);
        return routeMapper.toResponse(saved);
    }

    @Transactional
    @Auditable(action = "DELETE_ROUTE", resourceType = "ROUTE")
    public void deleteRoute(Long orgId, String routeId) {
        ApiRoute route = findRouteOrThrow(orgId, routeId);
        // Save history BEFORE deleting the live route
        saveVersion(route, RouteChangeType.DELETED);
        publishEvent(route, RouteChangeType.DELETED);
        routeRepository.delete(route);
        log.info("Route deleted: {}/{}", route.getOrganization().getSlug(), routeId);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private ApiRoute findRouteOrThrow(Long orgId, String routeId) {
        return routeRepository.findByOrganizationIdAndRouteId(orgId, routeId)
                .orElseThrow(() -> {
                    String slug = orgRepository.findById(orgId)
                            .map(Organization::getSlug).orElse(String.valueOf(orgId));
                    return ResourceNotFoundException.route(routeId, slug);
                });
    }

    private Organization findOrgOrThrow(Long orgId) {
        return orgRepository.findById(orgId)
                .orElseThrow(() -> ResourceNotFoundException.organization(orgId));
    }

    private void validateOrgExists(Long orgId) {
        if (!orgRepository.existsById(orgId)) {
            throw ResourceNotFoundException.organization(orgId);
        }
    }

    /**
     * Captures an immutable snapshot of the route at this moment.
     * The snapshot includes the full route config, methods and header rules.
     * History is not foreign-keyed to the live route — it survives route deletion.
     */
    @SuppressWarnings("unchecked")
    private void saveVersion(ApiRoute route, RouteChangeType changeType) {
        int nextVersion = versionRepository
                .findLatestVersionNumber(route.getOrganization().getId(), route.getRouteId())
                .map(v -> v + 1)
                .orElse(1);

        // Build the snapshot as a plain Map so it serialises cleanly to JSON
        Map<String, Object> snapshot = objectMapper.convertValue(
                routeMapper.toResponse(route), Map.class);

        String actor = currentUsername();

        RouteConfigurationVersion version = RouteConfigurationVersion.builder()
                .organizationId(route.getOrganization().getId())
                .routeId(route.getRouteId())
                .versionNumber(nextVersion)
                .changeType(changeType)
                .configurationSnapshot(snapshot)
                .createdBy(actor)
                .build();

        versionRepository.save(version);
    }

    /** Returns the username of the currently authenticated management user. */
    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    /** Publishes a Kafka event so Smart Gateway knows to refresh its registry. */
    private void publishEvent(ApiRoute route, RouteChangeType changeType) {
        RouteChangeEvent event = RouteChangeEvent.builder()
                .organizationSlug(route.getOrganization().getSlug())
                .routeId(route.getRouteId())
                .changeType(changeType)
                .configurationVersion(route.getConfigurationVersion())
                .changedBy(currentUsername())
                .occurredAt(java.time.LocalDateTime.now())
                .build();
        eventPublisher.publish(event);
    }
}
