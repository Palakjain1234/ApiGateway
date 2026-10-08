package com.gatekeeper.management.kafka;

import com.gatekeeper.management.enums.RouteChangeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published to Kafka whenever a route is created, updated,
 * activated, deactivated or deleted.
 *
 * Smart Gateway consumes this to know when to refresh its local route registry
 * without polling — it fetches fresh config from API Management on receipt.
 *
 * Topic: gatekeeper.route.changes
 * Key:   organizationSlug/routeId  (ensures ordered processing per route)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteChangeEvent {

    private String          organizationSlug;
    private String          routeId;
    private RouteChangeType changeType;
    private int             configurationVersion;
    private String          changedBy;
    private LocalDateTime   occurredAt;
}
