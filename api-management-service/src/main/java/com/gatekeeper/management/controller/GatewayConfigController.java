package com.gatekeeper.management.controller;

import com.gatekeeper.management.dto.response.ActiveRouteConfigResponse;
import com.gatekeeper.management.dto.response.ApiResponse;
import com.gatekeeper.management.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only endpoint consumed by Smart Gateway service account (GATEWAY_SERVICE).
 *
 * GET /api/gateway/routes/active
 *   → returns all active route configurations across all organizations.
 *   → Smart Gateway calls this at startup and after route-change Kafka events.
 *
 * The GATEWAY_SERVICE account can only GET — it cannot modify configuration.
 * This follows the principle of least privilege.
 */
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class GatewayConfigController {

    private final RouteService routeService;

    @GetMapping("/routes/active")
    @PreAuthorize("hasAnyRole('GATEWAY_SERVICE', 'PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<ActiveRouteConfigResponse>>> getActiveRoutes() {
        return ResponseEntity.ok(ApiResponse.ok(routeService.getAllActiveRoutes()));
    }
}
