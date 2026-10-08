package com.gatekeeper.management.dto.response;

import lombok.Data;

import java.util.List;

/**
 * Lightweight route projection returned to Smart Gateway via
 * GET /api/gateway/routes/active (GATEWAY_SERVICE role only).
 *
 * Contains only the fields the Gateway needs to match and forward requests.
 */
@Data
public class ActiveRouteConfigResponse {

    private String routeId;
    private String organizationSlug;
    private String pathPattern;
    private String targetBaseUrl;
    private String targetPathPrefix;
    private List<String> allowedMethods;
    private Integer requestsPerMinute;
    private int responseTimeoutMs;
    private boolean idempotencyEnabled;
    private int configurationVersion;
    private List<HeaderRuleResponse> headerRules;
}
