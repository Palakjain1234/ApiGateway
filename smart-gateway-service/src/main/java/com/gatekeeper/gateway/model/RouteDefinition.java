package com.gatekeeper.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * In-memory representation of an active route inside the Smart Gateway.
 *
 * Loaded from API Management at startup and refreshed whenever a
 * route-change Kafka event arrives. Never fetched from API Management
 * on every request — that would add unacceptable latency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDefinition {

    private String routeId;
    private String organizationSlug;

    /** Incoming path pattern, e.g. /foodfast/orders/** */
    private String pathPattern;

    /** Backend base URL, e.g. http://order-service:8092 */
    private String targetBaseUrl;

    /** Optional path prefix to prepend when forwarding, e.g. /api */
    private String targetPathPrefix;

    /** Allowed HTTP methods for this route, e.g. [GET, POST] */
    private List<String> allowedMethods;

    /** Max requests per minute per client. Null = no rate limit. */
    private Integer requestsPerMinute;

    /** Backend response timeout in milliseconds. */
    private int responseTimeoutMs;

    /** Whether an Idempotency-Key header is required for mutating requests. */
    private boolean idempotencyEnabled;

    /** Version from API Management — used to detect stale registry entries. */
    private int configurationVersion;

    /** Header manipulation rules applied before forwarding. */
    private List<HeaderRule> headerRules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeaderRule {
        private String action;       // ADD | REMOVE
        private String headerName;
        private String headerValue;  // null when action = REMOVE
    }
}
