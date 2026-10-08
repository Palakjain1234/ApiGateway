package com.gatekeeper.management.exception;

/**
 * Thrown when a creation request conflicts with existing data.
 * Maps to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
