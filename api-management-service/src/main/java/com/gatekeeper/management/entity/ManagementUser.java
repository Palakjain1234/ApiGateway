package com.gatekeeper.management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A human or machine identity allowed to operate the GateKeeper control plane.
 *
 * organization is nullable:
 *   PLATFORM_ADMIN  → null  (belongs to GateKeeper, not a tenant)
 *   GATEWAY_SERVICE → null  (machine account)
 *   TENANT_ADMIN    → references an Organization
 *
 * The unique constraint on organization_id enforces at most one TENANT_ADMIN
 * per organization. MySQL allows multiple NULLs in a unique index, so multiple
 * platform-level accounts can coexist.
 */
@Entity
@Table(name = "management_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagementUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The organization this user administers.
     * NULL for PLATFORM_ADMIN and GATEWAY_SERVICE.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", unique = true)
    private Organization organization;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
