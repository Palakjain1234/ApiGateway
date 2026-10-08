package com.gatekeeper.gateway.filter;

import com.gatekeeper.gateway.forwarder.RequestForwarder;
import com.gatekeeper.gateway.kafka.AccessEvent;
import com.gatekeeper.gateway.kafka.AccessEventPublisher;
import com.gatekeeper.gateway.model.GatewayRequest;
import com.gatekeeper.gateway.model.RouteDefinition;
import com.gatekeeper.gateway.policy.IdempotencyPolicy;
import com.gatekeeper.gateway.policy.RateLimitPolicy;
import com.gatekeeper.gateway.registry.RouteRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Core WebFlux filter — intercepts every incoming HTTP request and runs
 * the full Gateway policy pipeline:
 *
 *   1. Skip internal paths (/actuator, /health)
 *   2. Match request to a registered route
 *   3. Check allowed HTTP method
 *   4. Enforce rate limit (Redis)
 *   5. Enforce idempotency (Redis) — only for idempotency-enabled routes
 *   6. Apply header rules + forward to backend
 *   7. Publish access event to Kafka (fire-and-forget)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayFilter implements WebFilter {

    private static final List<String> SKIP_PREFIXES = List.of("/actuator", "/health");

    private final RouteRegistry       routeRegistry;
    private final RateLimitPolicy     rateLimitPolicy;
    private final IdempotencyPolicy   idempotencyPolicy;
    private final RequestForwarder    requestForwarder;
    private final AccessEventPublisher accessEventPublisher;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path   = request.getPath().value();
        String method = request.getMethod().name();

        // Skip internal paths — pass to next filter
        if (SKIP_PREFIXES.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();

        // 1. Route matching
        Optional<RouteDefinition> matchedRoute = routeRegistry.match(path);
        if (matchedRoute.isEmpty()) {
            return respondWith(exchange, HttpStatus.NOT_FOUND,
                    "No route found for path: " + path);
        }
        RouteDefinition route = matchedRoute.get();

        // 2. Method check
        if (!route.getAllowedMethods().contains(method.toUpperCase())) {
            return respondWith(exchange, HttpStatus.METHOD_NOT_ALLOWED,
                    "Method " + method + " not allowed on this route");
        }

        String clientIp = getClientIp(request);

        // 3. Rate limit check
        return rateLimitPolicy.isAllowed(route, clientIp)
                .flatMap(allowed -> {
                    if (!allowed) {
                        publishAccessEvent(route, method, path, 429, startTime, clientIp, true, false);
                        return respondWith(exchange, HttpStatus.TOO_MANY_REQUESTS,
                                "Rate limit exceeded");
                    }

                    // 4. Idempotency check (only for idempotency-enabled routes)
                    String idempotencyKey = request.getHeaders().getFirst("Idempotency-Key");
                    if (route.isIdempotencyEnabled() && isMutatingMethod(method)) {
                        if (idempotencyKey == null || idempotencyKey.isBlank()) {
                            return respondWith(exchange, HttpStatus.BAD_REQUEST,
                                    "Idempotency-Key header is required for this route");
                        }
                        return idempotencyPolicy
                                .isReplay(route.getOrganizationSlug(), route.getRouteId(), idempotencyKey)
                                .flatMap(isReplay -> {
                                    if (isReplay) {
                                        publishAccessEvent(route, method, path, 200, startTime, clientIp, false, true);
                                        return respondWith(exchange, HttpStatus.OK,
                                                "Duplicate request — already processed");
                                    }
                                    return doForward(exchange, request, route, method, path,
                                            startTime, clientIp, idempotencyKey);
                                });
                    }

                    return doForward(exchange, request, route, method, path,
                            startTime, clientIp, idempotencyKey);
                });
    }

    // ── Forward ──────────────────────────────────────────────────────────────

    private Mono<Void> doForward(
            ServerWebExchange exchange,
            ServerHttpRequest request,
            RouteDefinition route,
            String method, String path,
            long startTime, String clientIp,
            String idempotencyKey) {

        return request.getBody()
                .reduce(new byte[0], (acc, buffer) -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    byte[] combined = new byte[acc.length + bytes.length];
                    System.arraycopy(acc, 0, combined, 0, acc.length);
                    System.arraycopy(bytes, 0, combined, acc.length, bytes.length);
                    return combined;
                })
                .defaultIfEmpty(new byte[0])
                .flatMap(body -> {
                    GatewayRequest gatewayRequest = GatewayRequest.builder()
                            .incomingPath(path)
                            .method(HttpMethod.valueOf(method))
                            .headers(request.getHeaders())
                            .body(body)
                            .route(route)
                            .clientIp(clientIp)
                            .idempotencyKey(idempotencyKey)
                            .build();

                    return requestForwarder.forward(gatewayRequest)
                            .flatMap(backendResponse -> {
                                int status = backendResponse.getStatusCode().value();
                                publishAccessEvent(route, method, path, status,
                                        startTime, clientIp, false, false);
                                return writeResponse(exchange, backendResponse);
                            });
                });
    }

    // ── Response helpers ─────────────────────────────────────────────────────

    private Mono<Void> respondWith(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        byte[] body = message.getBytes();
        exchange.getResponse().getHeaders().setContentLength(body.length);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange,
                                     ResponseEntity<byte[]> backendResponse) {
        exchange.getResponse().setStatusCode(backendResponse.getStatusCode());
        exchange.getResponse().getHeaders().addAll(backendResponse.getHeaders());
        byte[] body = backendResponse.getBody() != null ? backendResponse.getBody() : new byte[0];
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    // ── Event publishing ─────────────────────────────────────────────────────

    private void publishAccessEvent(RouteDefinition route, String method, String path,
                                    int status, long startTime, String clientIp,
                                    boolean rateLimited, boolean idempotencyReplay) {
        accessEventPublisher.publish(AccessEvent.builder()
                .organizationSlug(route.getOrganizationSlug())
                .routeId(route.getRouteId())
                .httpMethod(method)
                .path(path)
                .statusCode(status)
                .durationMs(System.currentTimeMillis() - startTime)
                .clientIp(clientIp)
                .rateLimited(rateLimited)
                .idempotencyReplay(idempotencyReplay)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    // ── Misc helpers ─────────────────────────────────────────────────────────

    private String getClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    private boolean isMutatingMethod(String method) {
        return switch (method.toUpperCase()) {
            case "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
    }
}
