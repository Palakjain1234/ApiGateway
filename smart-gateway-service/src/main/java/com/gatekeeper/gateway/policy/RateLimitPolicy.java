package com.gatekeeper.gateway.policy;

import com.gatekeeper.gateway.model.RouteDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Token-bucket rate limiting backed by Redis.
 *
 * Key pattern: rate:{organizationSlug}:{routeId}:{clientIp}
 * TTL:         60 seconds (one sliding window)
 *
 * On each request, the counter for this key is incremented atomically.
 * If the resulting count exceeds requestsPerMinute, the request is rejected.
 *
 * Fail-closed: if Redis is unavailable and fail-closed=true (default),
 * the request is rejected rather than silently bypassed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitPolicy {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${gatekeeper.policy.fail-closed:true}")
    private boolean failClosed;

    /**
     * Returns true if the request is allowed to proceed.
     * Returns false if the rate limit has been exceeded.
     */
    public Mono<Boolean> isAllowed(RouteDefinition route, String clientIp) {
        if (route.getRequestsPerMinute() == null) {
            return Mono.just(true);   // no rate limit configured for this route
        }

        String key = "rate:" + route.getOrganizationSlug()
                   + ":" + route.getRouteId()
                   + ":" + clientIp;

        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        // First request in this window — set the TTL
                        return redisTemplate.expire(key, Duration.ofSeconds(60))
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .map(count -> count <= route.getRequestsPerMinute())
                .onErrorResume(ex -> {
                    log.error("Redis unavailable for rate-limit check: {}", ex.getMessage());
                    return Mono.just(!failClosed);   // fail-closed → reject; fail-open → allow
                });
    }
}
