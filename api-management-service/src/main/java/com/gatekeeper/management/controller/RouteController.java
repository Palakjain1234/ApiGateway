package com.gatekeeper.management.controller;

import com.gatekeeper.management.dto.request.CreateRouteRequest;
import com.gatekeeper.management.dto.request.UpdateRouteRequest;
import com.gatekeeper.management.dto.response.ApiResponse;
import com.gatekeeper.management.dto.response.RouteResponse;
import com.gatekeeper.management.dto.response.RouteVersionResponse;
import com.gatekeeper.management.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TENANT_ADMIN — manages routes for their own organization.
 * PLATFORM_ADMIN can also access these endpoints.
 *
 * GET    /api/organizations/{orgId}/routes                          → list routes
 * GET    /api/organizations/{orgId}/routes/{routeId}               → get route
 * POST   /api/organizations/{orgId}/routes                         → create route
 * PUT    /api/organizations/{orgId}/routes/{routeId}               → update route
 * PUT    /api/organizations/{orgId}/routes/{routeId}/activate      → activate
 * PUT    /api/organizations/{orgId}/routes/{routeId}/deactivate    → deactivate
 * DELETE /api/organizations/{orgId}/routes/{routeId}               → delete route
 * GET    /api/organizations/{orgId}/routes/{routeId}/history       → version history
 */
@RestController
@RequestMapping("/api/organizations/{orgId}/routes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'PLATFORM_ADMIN')")
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RouteResponse>>> getRoutes(
            @PathVariable Long orgId) {

        return ResponseEntity.ok(ApiResponse.ok(routeService.getRoutesForOrganization(orgId)));
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<ApiResponse<RouteResponse>> getRoute(
            @PathVariable Long orgId,
            @PathVariable String routeId) {

        return ResponseEntity.ok(ApiResponse.ok(routeService.getRoute(orgId, routeId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RouteResponse>> createRoute(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateRouteRequest request) {

        RouteResponse created = routeService.createRoute(orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PutMapping("/{routeId}")
    public ResponseEntity<ApiResponse<RouteResponse>> updateRoute(
            @PathVariable Long orgId,
            @PathVariable String routeId,
            @Valid @RequestBody UpdateRouteRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(routeService.updateRoute(orgId, routeId, request)));
    }

    @PutMapping("/{routeId}/activate")
    public ResponseEntity<ApiResponse<RouteResponse>> activateRoute(
            @PathVariable Long orgId,
            @PathVariable String routeId) {

        return ResponseEntity.ok(ApiResponse.ok(routeService.activateRoute(orgId, routeId)));
    }

    @PutMapping("/{routeId}/deactivate")
    public ResponseEntity<ApiResponse<RouteResponse>> deactivateRoute(
            @PathVariable Long orgId,
            @PathVariable String routeId) {

        return ResponseEntity.ok(ApiResponse.ok(routeService.deactivateRoute(orgId, routeId)));
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(
            @PathVariable Long orgId,
            @PathVariable String routeId) {

        routeService.deleteRoute(orgId, routeId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{routeId}/history")
    public ResponseEntity<ApiResponse<List<RouteVersionResponse>>> getRouteHistory(
            @PathVariable Long orgId,
            @PathVariable String routeId) {

        return ResponseEntity.ok(ApiResponse.ok(routeService.getRouteHistory(orgId, routeId)));
    }
}
