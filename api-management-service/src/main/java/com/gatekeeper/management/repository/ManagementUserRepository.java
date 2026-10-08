package com.gatekeeper.management.repository;

import com.gatekeeper.management.entity.ManagementUser;
import com.gatekeeper.management.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access for ManagementUsers (platform admins, tenant admins, gateway service account).
 */
@Repository
public interface ManagementUserRepository extends JpaRepository<ManagementUser, Long> {

    /** Used by Spring Security to load a user during authentication. */
    Optional<ManagementUser> findByUsername(String username);

    /** Prevents duplicate usernames before creating a new user. */
    boolean existsByUsername(String username);

    /**
     * Finds the tenant admin for a given organization.
     * Returns empty if the org has no admin yet.
     */
    @Query("""
        SELECT u FROM ManagementUser u
        JOIN u.role r
        WHERE u.organization.id = :orgId
        AND r.name = :roleName
        """)
    Optional<ManagementUser> findByOrganizationIdAndRoleName(
            @Param("orgId") Long orgId,
            @Param("roleName") RoleName roleName
    );

    /**
     * Checks whether an organization already has a tenant admin.
     * Used to enforce the one-admin-per-org rule before creating a new one.
     */
    @Query("""
        SELECT COUNT(u) > 0 FROM ManagementUser u
        JOIN u.role r
        WHERE u.organization.id = :orgId
        AND r.name = 'TENANT_ADMIN'
        """)
    boolean existsTenantAdminForOrganization(@Param("orgId") Long orgId);
}
