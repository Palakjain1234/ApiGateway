package com.gatekeeper.gateway.registry;

import com.gatekeeper.gateway.model.RouteDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Loads all active routes from API Management into the RouteRegistry
 * at application startup.
 *
 * Uses the GATEWAY_SERVICE JWT to authenticate with API Management.
 * If the load fails (API Management is down), the gateway starts with
 * an empty registry and logs a warning. Routes will be populated once
 * a route-change Kafka event arrives or on next scheduled refresh.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RouteRegistryLoader {

    private final RouteRegistry  routeRegistry;
    private final WebClient.Builder webClientBuilder;

    @Value("${gatekeeper.management.base-url}")
    private String managementBaseUrl;

    @Value("${gatekeeper.management.gateway-token}")
    private String gatewayToken;

    @EventListener(ApplicationReadyEvent.class)
    public void loadRoutesAtStartup() {
        log.info("Loading active routes from API Management: {}", managementBaseUrl);
        try {
            List<RouteDefinition> routes = fetchActiveRoutes();
            routeRegistry.replaceAll(routes);
            log.info("Route registry loaded — {} active routes", routes.size());
        } catch (Exception ex) {
            log.warn("Failed to load routes at startup: {}. Gateway starts with empty registry.",
                    ex.getMessage());
        }
    }

    public void refresh() {
        log.info("Refreshing route registry from API Management");
        try {
            List<RouteDefinition> routes = fetchActiveRoutes();
            routeRegistry.replaceAll(routes);
        } catch (Exception ex) {
            log.error("Route registry refresh failed: {}", ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<RouteDefinition> fetchActiveRoutes() {
        // Response envelope: { success: true, data: [...] }
        Map<String, Object> response = webClientBuilder.build()
                .get()
                .uri(managementBaseUrl + "/api/gateway/routes/active")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + gatewayToken)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (response == null || !(Boolean) response.get("success")) {
            throw new IllegalStateException("API Management returned unsuccessful response");
        }

        // Jackson deserialises the data array as List<Map> — convert to RouteDefinition
        List<Map<String, Object>> rawRoutes = (List<Map<String, Object>>) response.get("data");
        return rawRoutes.stream()
                .map(this::mapToRouteDefinition)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private RouteDefinition mapToRouteDefinition(Map<String, Object> raw) {
        List<Map<String, Object>> rawRules =
                (List<Map<String, Object>>) raw.getOrDefault("headerRules", List.of());

        List<RouteDefinition.HeaderRule> headerRules = rawRules.stream()
                .map(r -> RouteDefinition.HeaderRule.builder()
                        .action((String) r.get("action"))
                        .headerName((String) r.get("headerName"))
                        .headerValue((String) r.get("headerValue"))
                        .build())
                .toList();

        return RouteDefinition.builder()
                .routeId((String) raw.get("routeId"))
                .organizationSlug((String) raw.get("organizationSlug"))
                .pathPattern((String) raw.get("pathPattern"))
                .targetBaseUrl((String) raw.get("targetBaseUrl"))
                .targetPathPrefix((String) raw.getOrDefault("targetPathPrefix", ""))
                .allowedMethods((List<String>) raw.getOrDefault("allowedMethods", List.of()))
                .requestsPerMinute((Integer) raw.get("requestsPerMinute"))
                .responseTimeoutMs((int) raw.getOrDefault("responseTimeoutMs", 30000))
                .idempotencyEnabled((Boolean) raw.getOrDefault("idempotencyEnabled", false))
                .configurationVersion((int) raw.getOrDefault("configurationVersion", 1))
                .headerRules(headerRules)
                .build();
    }
}
