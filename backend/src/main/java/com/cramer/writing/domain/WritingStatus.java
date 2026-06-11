package com.cramer.writing.domain;

/**
 * Grading lifecycle of a {@code writing_submissions} row (SPEC-13 §1). The terminal success
 * state is {@code COMPLETED} (not {@code GRADED}).
 */
public enum WritingStatus {
    PENDING,
    GRADING,
    COMPLETED,
    FAILED;

    public static WritingStatus from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("writing status is required");
        }
        return WritingStatus.valueOf(raw.trim().toUpperCase());
    }
}
