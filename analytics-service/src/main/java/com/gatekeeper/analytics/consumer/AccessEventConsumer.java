package com.gatekeeper.analytics.consumer;

import com.gatekeeper.analytics.document.AccessEventDocument;
import com.gatekeeper.analytics.repository.AccessEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes access events from Kafka and persists them to MongoDB.
 *
 * Deduplication: each event is stored with a unique eventKey derived
 * from the Kafka topic + partition + offset. If the same message is
 * delivered twice (at-least-once semantics), the duplicate insert fails
 * on the unique index and is silently ignored.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccessEventConsumer {

    private final AccessEventRepository repository;

    @KafkaListener(
        topics  = "${gatekeeper.kafka.topics.access-events}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(
            AccessEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC)     String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int    partition,
            @Header(KafkaHeaders.OFFSET)             long   offset) {

        String eventKey = topic + "-" + partition + "-" + offset;

        AccessEventDocument doc = AccessEventDocument.builder()
                .id(UUID.randomUUID().toString())
                .organizationSlug(event.getOrganizationSlug())
                .routeId(event.getRouteId())
                .httpMethod(event.getHttpMethod())
                .path(event.getPath())
                .statusCode(event.getStatusCode())
                .durationMs(event.getDurationMs())
                .clientIp(event.getClientIp())
                .rateLimited(event.isRateLimited())
                .idempotencyReplay(event.isIdempotencyReplay())
                .occurredAt(event.getOccurredAt())
                .eventKey(eventKey)
                .build();

        try {
            repository.save(doc);
            log.debug("Access event stored: {}/{} status={}", 
                    event.getOrganizationSlug(), event.getRouteId(), event.getStatusCode());
        } catch (DuplicateKeyException ex) {
            log.debug("Duplicate event ignored: {}", eventKey);
        }
    }
}
