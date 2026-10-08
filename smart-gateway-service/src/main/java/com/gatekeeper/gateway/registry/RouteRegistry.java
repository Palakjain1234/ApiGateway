package com.gatekeeper.gateway.registry;

import com.gatekeeper.gateway.model.RouteDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of all active routes.
 *
 * Key: "{organizationSlug}/{routeId}"
 *
 * Thread-safe via ConcurrentHashMap — Kafka consumer and HTTP request
 * threads may access this concurrently.
 *
 * Routes are loaded at startup by RouteRegistryLoader and kept fresh
 * by RouteChangeConsumer (Kafka listener).
 */
@Component
@Slf4j
public class RouteRegistry {

    private final Map<String, RouteDefinition> routes = new ConcurrentHashMap<>();

    // ── Write operations (called by loader and Kafka consumer) ───────────────

    public void register(RouteDefinition route) {
        String key = key(route.getOrganizationSlug(), route.getRouteId());
        routes.put(key, route);
        log.debug("Route registered: {}", key);
    }

    public void remove(String organizationSlug, String routeId) {
        String key = key(organizationSlug, routeId);
        routes.remove(key);
        log.debug("Route removed from registry: {}", key);
    }

    public void replaceAll(Collection<RouteDefinition> newRoutes) {
        routes.clear();
        newRoutes.forEach(this::register);
        log.info("Route registry refreshed — {} routes loaded", routes.size());
    }

    // ── Read operations (called by GatewayFilter on every request) ───────────

    /**
     * Finds the first route whose pathPattern matches the incoming path.
     * Pattern matching: supports /** wildcard suffix.
     *
     * Example: pattern /foodfast/orders/** matches /foodfast/orders/123
     */
    public Optional<RouteDefinition> match(String incomingPath) {
        return routes.values().stream()
                .filter(r -> matchesPattern(r.getPathPattern(), incomingPath))
                .findFirst();
    }

    public Collection<RouteDefinition> all() {
        return routes.values();
    }

    public int size() {
        return routes.size();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String key(String orgSlug, String routeId) {
        return orgSlug + "/" + routeId;
    }

    /**
     * Simple pattern matching:
     *   /foodfast/orders/**   → matches anything starting with /foodfast/orders/
     *   /foodfast/catalog     → exact match only
     */
    private boolean matchesPattern(String pattern, String path) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return pattern.equals(path);
    }
}
