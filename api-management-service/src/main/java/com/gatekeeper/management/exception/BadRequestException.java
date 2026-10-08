package com.gatekeeper.management.exception;

/**
 * Thrown when the request is syntactically valid but semantically wrong.
 * Maps to HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
