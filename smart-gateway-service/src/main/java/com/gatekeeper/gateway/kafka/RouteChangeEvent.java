package com.gatekeeper.gateway.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mirror of the RouteChangeEvent published by api-management-service.
 * Consumed by Smart Gateway to know when to refresh its route registry.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteChangeEvent {
    private String        organizationSlug;
    private String        routeId;
    private String        changeType;
    private int           configurationVersion;
    private String        changedBy;
    private LocalDateTime occurredAt;
}
