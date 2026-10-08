package com.gatekeeper.management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload for PUT /api/routes/{routeId} (TENANT_ADMIN only)
 *
 * All fields are required — this is a full replacement, not a partial update.
 * Every PUT creates a new version snapshot.
 */
@Data
public class UpdateRouteRequest {

    @NotBlank(message = "Route name is required")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "Path pattern is required")
    @Size(max = 500)
    private String pathPattern;

    @NotBlank(message = "Target base URL is required")
    @Size(max = 500)
    private String targetBaseUrl;

    @Size(max = 500)
    private String targetPathPrefix;

    @NotEmpty(message = "At least one allowed HTTP method is required")
    private List<String> allowedMethods = new ArrayList<>();

    @Min(value = 1, message = "Rate limit must be at least 1 request per minute")
    private Integer requestsPerMinute;

    @Min(value = 100, message = "Timeout must be at least 100ms")
    private int responseTimeoutMs = 30_000;

    private boolean idempotencyEnabled = false;

    @Valid
    private List<HeaderRuleRequest> headerRules = new ArrayList<>();
}
