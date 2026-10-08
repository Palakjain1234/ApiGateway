package com.gatekeeper.management.entity;

import com.gatekeeper.management.enums.AuditResult;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * An immutable record of who performed which operation and what happened.
 *
 * Answers the question: "Who attempted which operation, and what was the result?"
 *
 * Different from RouteConfigurationVersion:
 *   Version  → what did the configuration look like?
 *   Audit    → who did what, when, and did it succeed?
 */
@Entity
@Table(
    name = "control_plane_audit_records",
    indexes = {
        @Index(name = "idx_audit_actor",  columnList = "actor"),
        @Index(name = "idx_audit_org",    columnList = "organization_id"),
        @Index(name = "idx_audit_action", columnList = "action")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlPlaneAuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NULL for platform-level operations (PLATFORM_ADMIN actions). */
    @Column(name = "organization_id")
    private Long organizationId;

    /** Username of the management user who made the request. */
    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    /** Operation name, e.g. CREATE_ROUTE, UPDATE_ORGANIZATION, LOGIN. */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /** Type of the resource acted upon, e.g. ROUTE, ORGANIZATION. */
    @Column(name = "resource_type", length = 100)
    private String resourceType;

    /** Identifier of the resource, e.g. "orders", "42". */
    @Column(name = "resource_id", length = 200)
    private String resourceId;

    /** Request trace/correlation ID for distributed tracing. */
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    /** How long the operation took in milliseconds. */
    @Column(name = "duration_ms")
    private Long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private AuditResult result;

    /** Additional context stored as JSON (e.g. error message, changed fields). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "json")
    private Map<String, Object> details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
