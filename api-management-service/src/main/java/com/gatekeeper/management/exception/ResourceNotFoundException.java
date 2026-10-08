package com.gatekeeper.management.exception;

/**
 * Thrown when a requested resource does not exist.
 * Maps to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException organization(Long id) {
        return new ResourceNotFoundException("Organization not found with id: " + id);
    }

    public static ResourceNotFoundException organizationBySlug(String slug) {
        return new ResourceNotFoundException("Organization not found with slug: " + slug);
    }

    public static ResourceNotFoundException route(String routeId, String orgSlug) {
        return new ResourceNotFoundException(
            "Route '" + routeId + "' not found in organization '" + orgSlug + "'");
    }
}
