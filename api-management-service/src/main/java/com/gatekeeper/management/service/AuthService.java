package com.gatekeeper.management.service;

import com.gatekeeper.management.dto.request.LoginRequest;
import com.gatekeeper.management.dto.response.LoginResponse;
import com.gatekeeper.management.entity.ManagementUser;
import com.gatekeeper.management.repository.ManagementUserRepository;
import com.gatekeeper.management.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles authentication for GateKeeper management users.
 *
 * There is no public registration endpoint — management accounts are
 * created through the controlled provisioning flow (PLATFORM_ADMIN creates orgs
 * and tenant admins). This service only handles login.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final ManagementUserRepository userRepository;
    private final PasswordEncoder          passwordEncoder;
    private final JwtService               jwtService;

    /**
     * Authenticates a management user and returns a signed JWT.
     *
     * @throws BadCredentialsException if the username does not exist,
     *         the password is wrong, or the account is disabled.
     */
    public LoginResponse login(LoginRequest request) {
        ManagementUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token     = jwtService.generateToken(user);
        long   expiresAt = jwtService.getExpirationFromToken(token);

        log.info("Login successful for user: {}", user.getUsername());

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRole().getName().name(),
                expiresAt
        );
    }
}
