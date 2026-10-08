package com.gatekeeper.management.repository;

import com.gatekeeper.management.entity.RouteConfigurationVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for immutable route configuration version snapshots.
 *
 * No FK to api_routes — history survives route deletion.
 */
@Repository
public interface RouteConfigurationVersionRepository extends JpaRepository<RouteConfigurationVersion, Long> {

    /** Full version history for a route in reverse-chronological order. */
    List<RouteConfigurationVersion> findByOrganizationIdAndRouteIdOrderByVersionNumberDesc(
            Long organizationId, String routeId);

    /**
     * Gets the latest version number for a route so the next snapshot
     * can increment it correctly.
     */
    @Query("""
        SELECT MAX(v.versionNumber)
        FROM RouteConfigurationVersion v
        WHERE v.organizationId = :orgId AND v.routeId = :routeId
        """)
    Optional<Integer> findLatestVersionNumber(
            @Param("orgId") Long orgId,
            @Param("routeId") String routeId
    );
}
