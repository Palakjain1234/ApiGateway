package com.gatekeeper.management.security;

import com.gatekeeper.management.entity.ManagementUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates and validates JWT tokens for GateKeeper management users.
 *
 * Token claims:
 *   sub  → username
 *   role → role name (PLATFORM_ADMIN, TENANT_ADMIN, GATEWAY_SERVICE)
 *   org  → organization slug (null for platform-level users)
 */
@Service
@Slf4j
public class JwtService {

    @Value("${gatekeeper.jwt.secret}")
    private String secret;

    @Value("${gatekeeper.jwt.expiration-ms}")
    private long expirationMs;

    // ── Token generation ─────────────────────────────────────────────────────

    public String generateToken(ManagementUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().getName().name());

        String orgSlug = user.getOrganization() != null
                ? user.getOrganization().getSlug()
                : null;
        claims.put("org", orgSlug);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey())
                .compact();
    }

    // ── Token validation ─────────────────────────────────────────────────────

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractOrgSlug(String token) {
        return parseClaims(token).get("org", String.class);
    }

    public long getExpirationFromToken(String token) {
        return parseClaims(token).getExpiration().getTime();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

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
