package com.gatekeeper.management.config;

import com.gatekeeper.management.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT-based security for the API Management control plane.
 *
 * Public endpoints:
 *   POST /api/auth/login    → login, no token required
 *   GET  /actuator/health   → health check
 *
 * Role matrix:
 *   PLATFORM_ADMIN   → /api/organizations/**  (full CRUD on orgs and tenant admin provisioning)
 *   TENANT_ADMIN     → /api/routes/**         (manage own org's routes)
 *   GATEWAY_SERVICE  → GET /api/gateway/**    (read active route config)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ── Public ───────────────────────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // ── Platform admin — org management ──────────────────────────
                .requestMatchers("/api/organizations/**").hasRole("PLATFORM_ADMIN")

                // ── Tenant admin — route management ───────────────────────────
                .requestMatchers("/api/routes/**").hasAnyRole("TENANT_ADMIN", "PLATFORM_ADMIN")

                // ── Gateway service account — read active config ──────────────
                .requestMatchers(HttpMethod.GET, "/api/gateway/**")
                    .hasAnyRole("GATEWAY_SERVICE", "PLATFORM_ADMIN")

                // ── Everything else needs authentication ──────────────────────
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
