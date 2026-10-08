package com.gatekeeper.management.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * One HTTP method that is allowed on a route.
 * Child of ApiRoute — deleted automatically when the route is deleted (CASCADE).
 *
 * Example: a route may allow GET, POST and DELETE as separate rows.
 */
@Entity
@Table(
    name = "route_allowed_methods",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_route_method",
        columnNames = {"route_id", "http_method"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteAllowedMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private ApiRoute route;

    /** HTTP verb in uppercase, e.g. GET, POST, PUT, DELETE, PATCH */
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;
}
