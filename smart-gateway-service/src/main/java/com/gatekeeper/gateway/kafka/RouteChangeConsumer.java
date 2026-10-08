package com.gatekeeper.gateway.kafka;

import com.gatekeeper.gateway.registry.RouteRegistryLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens for route-change events published by API Management.
 * Triggers a full registry refresh so the Gateway never serves stale config.
 *
 * A full refresh (instead of partial update) keeps the logic simple and
 * correct — the API Management endpoint is the single source of truth.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RouteChangeConsumer {

    private final RouteRegistryLoader registryLoader;

    @KafkaListener(
        topics   = "${gatekeeper.kafka.topics.route-changes}",
        groupId  = "${spring.kafka.consumer.group-id}"
    )
    public void onRouteChange(RouteChangeEvent event) {
        log.info("Route-change event received: {}/{} → {} (v{})",
                event.getOrganizationSlug(),
                event.getRouteId(),
                event.getChangeType(),
                event.getConfigurationVersion());

        registryLoader.refresh();
    }
}
