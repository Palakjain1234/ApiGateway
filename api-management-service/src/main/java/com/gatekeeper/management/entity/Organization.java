package com.gatekeeper.management.entity;

import com.gatekeeper.management.enums.OrganizationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A company / tenant that uses GateKeeper.
 *
 * slug is the URL-safe identifier used as the first path segment in routes.
 * e.g.  name = "FoodFast"  →  slug = "foodfast"  →  /foodfast/orders/**
 */
@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** URL-safe, lowercase, hyphen-separated. Must be unique across all tenants. */
    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Soft-delete timestamp. NULL means the organization is not deleted. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Relationships ────────────────────────────────────────────────────────

    /** The one tenant admin that belongs to this organization (may be null). */
    @OneToOne(mappedBy = "organization", fetch = FetchType.LAZY)
    private ManagementUser tenantAdmin;

    /** All routes configured under this organization. */
    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<ApiRoute> routes = new ArrayList<>();
}
