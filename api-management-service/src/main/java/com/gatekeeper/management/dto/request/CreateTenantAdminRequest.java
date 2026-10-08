package com.gatekeeper.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for POST /api/organizations/{orgId}/admin (PLATFORM_ADMIN only)
 *
 * Creates the one TENANT_ADMIN account for an organization.
 */
@Data
public class CreateTenantAdminRequest {

    @NotBlank(message = "Username is required")
    @Size(max = 100, message = "Username must not exceed 100 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
