package com.gatekeeper.analytics.service;

import com.gatekeeper.analytics.document.AccessEventDocument;
import com.gatekeeper.analytics.repository.AccessEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes operational statistics from stored access events.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AccessEventRepository repository;

    /** Summary stats for a specific organization over a time range. */
    public Map<String, Object> getOrgStats(String orgSlug, LocalDateTime from, LocalDateTime to) {
        List<AccessEventDocument> events =
                repository.findByOrganizationSlugAndOccurredAtBetween(orgSlug, from, to);

        return buildStats(events);
    }

    /** Summary stats for a specific route over a time range. */
    public Map<String, Object> getRouteStats(String routeId, LocalDateTime from, LocalDateTime to) {
        List<AccessEventDocument> events =
                repository.findByRouteIdAndOccurredAtBetween(routeId, from, to);

        return buildStats(events);
    }

    /** Platform-wide totals. */
    public Map<String, Object> getPlatformStats() {
        long total          = repository.count();
        long rateLimited    = repository.countByRateLimitedTrue();
        long replays        = repository.countByIdempotencyReplayTrue();

        return Map.of(
            "totalRequests",      total,
            "rateLimitedCount",   rateLimited,
            "idempotencyReplays", replays
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> buildStats(List<AccessEventDocument> events) {
        if (events.isEmpty()) {
            return Map.of(
                "totalRequests", 0,
                "avgDurationMs", 0,
                "successCount",  0,
                "errorCount",    0,
                "rateLimited",   0,
                "replays",       0
            );
        }

        long total       = events.size();
        long successCount = events.stream().filter(e -> e.getStatusCode() < 400).count();
        long errorCount  = total - successCount;
        long rateLimited = events.stream().filter(AccessEventDocument::isRateLimited).count();
        long replays     = events.stream().filter(AccessEventDocument::isIdempotencyReplay).count();
        double avgMs     = events.stream().mapToLong(AccessEventDocument::getDurationMs).average().orElse(0);

        // Top 5 slowest routes
        Map<String, Double> slowestRoutes = events.stream()
                .collect(Collectors.groupingBy(
                        AccessEventDocument::getRouteId,
                        Collectors.averagingLong(AccessEventDocument::getDurationMs)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return Map.of(
            "totalRequests", total,
            "successCount",  successCount,
            "errorCount",    errorCount,
            "avgDurationMs", Math.round(avgMs),
            "rateLimited",   rateLimited,
            "replays",       replays,
            "slowestRoutes", slowestRoutes
        );
    }
}
