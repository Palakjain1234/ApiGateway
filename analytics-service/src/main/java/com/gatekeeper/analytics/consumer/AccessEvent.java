package com.gatekeeper.analytics.consumer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mirror of the AccessEvent published by Smart Gateway.
 */
@Data
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
