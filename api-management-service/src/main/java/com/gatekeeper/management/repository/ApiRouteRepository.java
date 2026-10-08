package com.gatekeeper.management.repository;

import com.gatekeeper.management.entity.ApiRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for ApiRoutes.
 *
 * Route identity is (organizationId + routeId) — never just routeId alone.
 * Two organizations can have routes with the same routeId.
 */
@Repository
public interface ApiRouteRepository extends JpaRepository<ApiRoute, Long> {

    /**
     * Find a route by its tenant-scoped identity.
     * Always use this instead of searching by routeId alone.
     */
    Optional<ApiRoute> findByOrganizationIdAndRouteId(Long organizationId, String routeId);

    /** Prevents duplicate route IDs within the same organization. */
    boolean existsByOrganizationIdAndRouteId(Long organizationId, String routeId);

    /** Returns all routes belonging to an organization (for TENANT_ADMIN listing). */
    List<ApiRoute> findByOrganizationId(Long organizationId);

    /**
     * Returns all active routes for an organization.
     * Used by the Gateway Service account to load live configuration.
     */
    List<ApiRoute> findByOrganizationIdAndActiveTrue(Long organizationId);

    /**
     * Returns all active routes across all organizations.
     * Used by Smart Gateway at startup to build its full route registry.
     */
    @Query("""
        SELECT r FROM ApiRoute r
        JOIN FETCH r.organization
        JOIN FETCH r.allowedMethods
        WHERE r.active = true
        """)
    List<ApiRoute> findAllActiveWithOrganizationAndMethods();
}
