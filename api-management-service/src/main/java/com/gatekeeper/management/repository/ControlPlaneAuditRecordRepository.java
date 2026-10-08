package com.gatekeeper.management.repository;

import com.gatekeeper.management.entity.ControlPlaneAuditRecord;
import com.gatekeeper.management.enums.AuditResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access for control-plane audit records.
 *
 * All query methods return in insertion order (most recent last)
 * unless a Pageable with sorting is provided.
 */
@Repository
public interface ControlPlaneAuditRecordRepository extends JpaRepository<ControlPlaneAuditRecord, Long> {

    /** All audit records for a specific actor (management user). */
    Page<ControlPlaneAuditRecord> findByActorOrderByCreatedAtDesc(String actor, Pageable pageable);

    /** All audit records for a specific organization. */
    Page<ControlPlaneAuditRecord> findByOrganizationIdOrderByCreatedAtDesc(
            Long organizationId, Pageable pageable);

    /** All audit records for a specific action type, e.g. CREATE_ROUTE. */
    List<ControlPlaneAuditRecord> findByActionAndCreatedAtAfterOrderByCreatedAtDesc(
            String action, LocalDateTime after);

    /** All failed operations — useful for security monitoring. */
    Page<ControlPlaneAuditRecord> findByResultOrderByCreatedAtDesc(
            AuditResult result, Pageable pageable);
}
