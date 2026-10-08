package com.gatekeeper.management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload for POST /api/routes (TENANT_ADMIN only)
 */
@Data
public class CreateRouteRequest {

    /**
     * Human-readable route identifier. Unique within the organization.
     * e.g. "orders", "catalog", "auth"
     */
    @NotBlank(message = "Route ID is required")
    @Pattern(
        regexp = "^[a-z0-9-]+$",
        message = "Route ID must contain only lowercase letters, digits and hyphens"
    )
    @Size(max = 100)
    private String routeId;

    @NotBlank(message = "Route name is required")
    @Size(max = 200)
    private String name;

    /**
     * Incoming path pattern including the tenant slug prefix.
     * e.g. /foodfast/orders/**
     */
    @NotBlank(message = "Path pattern is required")
    @Size(max = 500)
    private String pathPattern;

    /**
     * Backend base URL the Gateway should forward requests to.
     * e.g. http://order-service:8092
     */
    @NotBlank(message = "Target base URL is required")
    @Size(max = 500)
    private String targetBaseUrl;

    /** Optional path prefix to prepend on the backend, e.g. /api */
    @Size(max = 500)
    private String targetPathPrefix;

    /**
     * Allowed HTTP methods. At least one is required.
     * e.g. ["GET", "POST"]
     */
    @NotEmpty(message = "At least one allowed HTTP method is required")
    private List<String> allowedMethods = new ArrayList<>();

    /** Max requests per minute. Null means no rate limit. */
    @Min(value = 1, message = "Rate limit must be at least 1 request per minute")
    private Integer requestsPerMinute;

    /** Backend response timeout in milliseconds. Defaults to 30 seconds. */
    @Min(value = 100, message = "Timeout must be at least 100ms")
    private int responseTimeoutMs = 30_000;

    /** Enables duplicate-request protection using an Idempotency-Key header. */
    private boolean idempotencyEnabled = false;

    /** Header manipulation rules applied to every matching request. */
    @Valid
    private List<HeaderRuleRequest> headerRules = new ArrayList<>();
}
