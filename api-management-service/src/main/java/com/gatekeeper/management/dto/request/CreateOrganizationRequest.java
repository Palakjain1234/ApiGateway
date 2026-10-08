package com.gatekeeper.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for POST /api/organizations (PLATFORM_ADMIN only)
 */
@Data
public class CreateOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    /**
     * URL-safe slug — lowercase letters, digits and hyphens only.
     * Used as the first path segment in routes: /foodfast/orders/**
     */
    @NotBlank(message = "Slug is required")
    @Pattern(
        regexp = "^[a-z0-9-]+$",
        message = "Slug must contain only lowercase letters, digits and hyphens"
    )
    @Size(max = 100, message = "Slug must not exceed 100 characters")
    private String slug;
}
