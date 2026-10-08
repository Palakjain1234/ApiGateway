package com.gatekeeper.management.entity;

import com.gatekeeper.management.enums.RouteChangeType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Immutable snapshot of a route's configuration at the moment of each change.
 *
 * Answers the question: "What did the route look like at version N?"
 *
 * Deliberately NOT foreign-keyed to api_routes so history survives
 * after the live route is deleted. Live state and historical state
 * have different lifecycles.
 */
@Entity
@Table(
    name = "route_configuration_versions",
    indexes = @Index(
        name = "idx_route_versions_org_route",
        columnList = "organization_id, route_id"
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteConfigurationVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The organization that owns the route. Stored as a plain value, not a FK. */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /** The route_id as stored in api_routes. Stored as a plain value, not a FK. */
    @Column(name = "route_id", nullable = false, length = 100)
    private String routeId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private RouteChangeType changeType;

    /**
     * Full JSON snapshot of the route including allowed methods and header rules
     * at the time this version was created. Stored as a JSON column.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration_snapshot", nullable = false, columnDefinition = "json")
    private Map<String, Object> configurationSnapshot;

    /** Username of the management user who triggered the change. */
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
