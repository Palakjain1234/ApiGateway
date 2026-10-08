package com.gatekeeper.analytics.controller;

import com.gatekeeper.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Exposes operational statistics from stored Gateway access events.
 *
 * GET /api/analytics/platform                          → platform-wide totals
 * GET /api/analytics/org/{slug}?from=...&to=...        → org stats by time range
 * GET /api/analytics/route/{routeId}?from=...&to=...   → route stats by time range
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/platform")
    public ResponseEntity<Map<String, Object>> platformStats() {
        return ResponseEntity.ok(analyticsService.getPlatformStats());
    }

    @GetMapping("/org/{slug}")
    public ResponseEntity<Map<String, Object>> orgStats(
            @PathVariable String slug,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return ResponseEntity.ok(analyticsService.getOrgStats(slug, from, to));
    }

    @GetMapping("/route/{routeId}")
    public ResponseEntity<Map<String, Object>> routeStats(
            @PathVariable String routeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return ResponseEntity.ok(analyticsService.getRouteStats(routeId, from, to));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "analytics-service"));
    }
}
