package com.cramer.platform.error;

/**
 * Thrown when a requested resource does not exist. Mapped to HTTP 404 (SPEC-04 §2.2).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /** Convenience factory: {@code "Test set not found: 9"}. */
    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException(resource + " not found: " + id);
    }
}
