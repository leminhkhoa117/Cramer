package com.cramer.assessment.domain;

/**
 * Lifecycle status of a {@code test_attempts} row. See SPEC-12 §1.
 */
public enum AttemptStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public static AttemptStatus from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("attempt status is required");
        }
        return AttemptStatus.valueOf(raw.trim().toUpperCase());
    }
}
