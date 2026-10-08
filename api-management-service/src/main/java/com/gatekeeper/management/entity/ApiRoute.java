package com.gatekeeper.management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A route belongs to an organization and describes how a public path
 * maps to a backend service, together with all Gateway policies.
 *
 * Identity is (organization + routeId). routeId only needs to be unique
 * within an organization — two organizations can both have a route named "orders".
 */
@Entity
@Table(
    name = "api_routes",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_api_routes_org_route",
        columnNames = {"organization_id", "route_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** Human-readable route identifier, e.g. "orders". Unique within the org. */
    @Column(name = "route_id", nullable = false, length = 100)
    private String routeId;

    /** Display name, e.g. "FoodFast Order Service". */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Incoming path pattern, e.g. /foodfast/orders/** */
    @Column(name = "path_pattern", nullable = false, length = 500)
    private String pathPattern;

    /** Backend base URL, e.g. http://order-service:8092 */
    @Column(name = "target_base_url", nullable = false, length = 500)
    private String targetBaseUrl;

    /** Optional prefix to prepend on the target path, e.g. /api */
    @Column(name = "target_path_prefix", length = 500)
    private String targetPathPrefix;

    /** Max requests per minute. NULL means no rate limit. */
    @Column(name = "requests_per_minute")
    private Integer requestsPerMinute;

    /** How long to wait for the backend before returning a timeout error. */
    @Column(name = "response_timeout_ms", nullable = false)
    @Builder.Default
    private int responseTimeoutMs = 30_000;

    /** Whether duplicate-request protection is active on this route. */
    @Column(name = "idempotency_enabled", nullable = false)
    @Builder.Default
    private boolean idempotencyEnabled = false;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Incremented on every configuration change. Used by Gateway for staleness detection. */
    @Column(name = "configuration_version", nullable = false)
    @Builder.Default
    private int configurationVersion = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Child collections ────────────────────────────────────────────────────

    @OneToMany(mappedBy = "route", fetch = FetchType.EAGER,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RouteAllowedMethod> allowedMethods = new ArrayList<>();

    @OneToMany(mappedBy = "route", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RouteHeaderRule> headerRules = new ArrayList<>();

    // ── Convenience helpers ──────────────────────────────────────────────────

    public void addAllowedMethod(RouteAllowedMethod method) {
        method.setRoute(this);
        allowedMethods.add(method);
    }

    public void addHeaderRule(RouteHeaderRule rule) {
        rule.setRoute(this);
        headerRules.add(rule);
    }
}
