package com.gatekeeper.management.entity;

import com.gatekeeper.management.enums.HeaderAction;
import jakarta.persistence.*;
import lombok.*;

/**
 * A header manipulation rule applied to every request that matches a route.
 * Child of ApiRoute — deleted automatically when the route is deleted (CASCADE).
 *
 * ADD    → inject the header with headerValue before forwarding
 * REMOVE → strip the header from the request before forwarding
 */
@Entity
@Table(name = "route_header_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteHeaderRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private ApiRoute route;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 10)
    private HeaderAction action;

    @Column(name = "header_name", nullable = false, length = 200)
    private String headerName;

    /** Required when action = ADD. Null when action = REMOVE. */
    @Column(name = "header_value", length = 500)
    private String headerValue;
}
