package com.gatekeeper.gateway.filter;

import com.gatekeeper.gateway.registry.RouteRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple health endpoint for the Smart Gateway.
 * Also reports how many routes are currently loaded in the registry.
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final RouteRegistry routeRegistry;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status",       "UP",
            "service",      "smart-gateway-service",
            "routesLoaded", routeRegistry.size()
        ));
    }
}
