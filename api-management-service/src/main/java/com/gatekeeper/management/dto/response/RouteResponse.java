package com.gatekeeper.management.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full route representation returned by route endpoints.
 */
@Data
public class RouteResponse {

    private Long id;
    private String routeId;
    private String name;
    private String organizationSlug;
    private String pathPattern;
    private String targetBaseUrl;
    private String targetPathPrefix;
    private List<String> allowedMethods;
    private Integer requestsPerMinute;
    private int responseTimeoutMs;
    private boolean idempotencyEnabled;
    private boolean active;
    private int configurationVersion;
    private List<HeaderRuleResponse> headerRules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
