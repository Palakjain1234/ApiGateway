package com.gatekeeper.management.repository;

import com.gatekeeper.management.entity.Organization;
import com.gatekeeper.management.enums.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for Organizations (tenants).
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    /** Lookup by URL-safe slug (e.g. "foodfast"). */
    Optional<Organization> findBySlug(String slug);

    /** Check uniqueness before creating a new org. */
    boolean existsBySlug(String slug);

    /** Used by PLATFORM_ADMIN to list all orgs in a given status. */
    List<Organization> findByStatus(OrganizationStatus status);

    /**
     * Fetch all organizations that are not soft-deleted.
     * Soft-deleted orgs have deletedAt set to a non-null value.
     */
    @Query("SELECT o FROM Organization o WHERE o.deletedAt IS NULL")
    List<Organization> findAllActive();
}
