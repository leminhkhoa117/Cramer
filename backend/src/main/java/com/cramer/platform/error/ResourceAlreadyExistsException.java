package com.cramer.platform.error;

/**
 * Thrown when attempting to create a resource that already exists. Mapped to HTTP 409
 * (SPEC-04 §2.2).
 */
public class ResourceAlreadyExistsException extends RuntimeException {

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
