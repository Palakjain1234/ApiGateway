package com.gatekeeper.gateway.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published to Kafka after every request processed by the Smart Gateway.
 * Consumed by the Analytics Service.
 *
 * Topic: gatekeeper.access.events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessEvent {

    private String        organizationSlug;
    private String        routeId;
    private String        httpMethod;
    private String        path;
    private int           statusCode;
    private long          durationMs;
    private String        clientIp;
    private boolean       rateLimited;
    private boolean       idempotencyReplay;
    private LocalDateTime occurredAt;
}
