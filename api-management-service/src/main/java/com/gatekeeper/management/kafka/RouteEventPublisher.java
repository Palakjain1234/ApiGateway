package com.gatekeeper.management.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes route-change events to Kafka so Smart Gateway can refresh
 * its local route registry without polling.
 *
 * Message key = "{organizationSlug}/{routeId}"
 * This ensures all events for the same route are processed in order
 * (same Kafka partition).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RouteEventPublisher {

    private final KafkaTemplate<String, RouteChangeEvent> kafkaTemplate;

    @Value("${gatekeeper.kafka.topics.route-changes}")
    private String routeChangesTopic;

    public void publish(RouteChangeEvent event) {
        String key = event.getOrganizationSlug() + "/" + event.getRouteId();

        CompletableFuture<SendResult<String, RouteChangeEvent>> future =
                kafkaTemplate.send(routeChangesTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish route-change event for key={}: {}", key, ex.getMessage());
            } else {
                log.debug("Route-change event published: key={} offset={}",
                        key, result.getRecordMetadata().offset());
            }
        });
    }
}
