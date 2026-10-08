package com.gatekeeper.gateway.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Duplicate-request protection backed by Redis.
 *
 * Key pattern: idempotency:{organizationSlug}:{routeId}:{idempotencyKey}
 * TTL:         24 hours
 *
 * On first request:  key is not present → request is allowed → key is created
 * On replay:         key is present → request is a duplicate → caller gets the same
 *                    cached status code back (200 OK with replay flag)
 *
 * Only applies to routes with idempotencyEnabled = true.
 * The client must send an Idempotency-Key header on mutating requests.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyPolicy {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${gatekeeper.policy.fail-closed:true}")
    private boolean failClosed;

    /**
     * Checks whether this idempotency key has been seen before.
     *
     * @return true  → this is a replay (duplicate request)
     *         false → this is a new request
     */
    public Mono<Boolean> isReplay(String organizationSlug, String routeId, String idempotencyKey) {
        String redisKey = "idempotency:" + organizationSlug + ":" + routeId + ":" + idempotencyKey;

        // SETNX (set if not exists) — atomic check-and-set
        return redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "seen", IDEMPOTENCY_TTL)
                .map(wasSet -> !wasSet)   // wasSet=true → new request; wasSet=false → replay
                .onErrorResume(ex -> {
                    log.error("Redis unavailable for idempotency check: {}", ex.getMessage());
                    return Mono.just(failClosed);   // fail-closed → treat as replay (reject)
                });
    }
}
