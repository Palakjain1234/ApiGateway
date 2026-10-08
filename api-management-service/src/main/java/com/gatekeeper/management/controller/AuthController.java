package com.gatekeeper.management.controller;

import com.gatekeeper.management.dto.request.LoginRequest;
import com.gatekeeper.management.dto.response.ApiResponse;
import com.gatekeeper.management.dto.response.LoginResponse;
import com.gatekeeper.management.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles authentication for all GateKeeper management users.
 *
 * POST /api/auth/login  → authenticate and receive a JWT
 *
 * No registration endpoint exists — accounts are provisioned through
 * the controlled PLATFORM_ADMIN flow.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
