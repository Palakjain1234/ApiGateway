package com.gatekeeper.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Payload for POST /api/auth/login
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
