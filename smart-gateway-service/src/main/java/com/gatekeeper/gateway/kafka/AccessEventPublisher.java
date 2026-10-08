package com.gatekeeper.gateway.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes an AccessEvent to Kafka after every request.
 * The client never waits for this — it fires and forgets.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccessEventPublisher {

    private final KafkaTemplate<String, AccessEvent> kafkaTemplate;

    @Value("${gatekeeper.kafka.topics.access-events}")
    private String accessEventsTopic;

    public void publish(AccessEvent event) {
        String key = event.getOrganizationSlug() + "/" + event.getRouteId();
        kafkaTemplate.send(accessEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish access event: {}", ex.getMessage());
                    }
                });
    }
}
