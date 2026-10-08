package com.gatekeeper.management.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Returned after creating a tenant admin.
 * Password hash is never included in any response.
 */
@Data
public class ManagementUserResponse {

    private Long id;
    private String username;
    private String role;
    private String organizationSlug;   // null for platform-level users
    private boolean enabled;
    private LocalDateTime createdAt;
}
