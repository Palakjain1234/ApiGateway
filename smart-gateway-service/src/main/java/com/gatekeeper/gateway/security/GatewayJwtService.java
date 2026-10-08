package com.gatekeeper.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Local JWT validation inside Smart Gateway.
 *
 * The Gateway validates management JWTs (GATEWAY_SERVICE account) locally
 * without calling API Management on every request.
 * This follows the principle: avoid synchronous calls to other services
 * on the hot path.
 */
@Service
@Slf4j
public class GatewayJwtService {

    @Value("${gatekeeper.jwt.secret}")
    private String secret;

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            log.debug("JWT invalid: {}", ex.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
