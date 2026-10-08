package com.gatekeeper.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Returned by POST /api/auth/login on success.
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String username;
    private String role;
    /** Token expiry in milliseconds from epoch */
    private long expiresAt;
}
