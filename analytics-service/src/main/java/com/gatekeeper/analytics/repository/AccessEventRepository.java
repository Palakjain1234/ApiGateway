package com.gatekeeper.analytics.repository;

import com.gatekeeper.analytics.document.AccessEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB repository for access events.
 */
@Repository
public interface AccessEventRepository extends MongoRepository<AccessEventDocument, String> {

    List<AccessEventDocument> findByOrganizationSlugAndOccurredAtBetween(
            String organizationSlug, LocalDateTime start, LocalDateTime end);

    List<AccessEventDocument> findByRouteIdAndOccurredAtBetween(
            String routeId, LocalDateTime start, LocalDateTime end);

    long countByOrganizationSlug(String organizationSlug);

    long countByRateLimitedTrue();

    long countByIdempotencyReplayTrue();
}
