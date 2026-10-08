package com.gatekeeper.analytics.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document storing one Gateway access event.
 *
 * Collection: access_events
 *
 * Indexed on organizationSlug + occurredAt for efficient
 * stats queries by tenant and time range.
 */
@Document(collection = "access_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessEventDocument {

    @Id
    private String id;

    @Indexed
    private String organizationSlug;

    @Indexed
    private String routeId;

    private String httpMethod;
    private String path;
    private int    statusCode;
    private long   durationMs;
    private String clientIp;
    private boolean rateLimited;
    private boolean idempotencyReplay;

    @Indexed
    private LocalDateTime occurredAt;

    /** Deduplication key — prevents storing the same Kafka message twice. */
    @Indexed(unique = true, sparse = true)
    private String eventKey;
}
