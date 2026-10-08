package com.gatekeeper.management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple health check endpoint — publicly accessible, no auth required.
 * Actuator /actuator/health provides deeper checks.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status",  "UP",
            "service", "api-management-service"
        ));
    }
}
