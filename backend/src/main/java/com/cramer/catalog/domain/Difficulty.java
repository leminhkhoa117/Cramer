package com.cramer.catalog.domain;

/**
 * Test difficulty, stored uppercase in {@code tests.difficulty} (default INTERMEDIATE). SPEC-11 §4.1.
 */
public enum Difficulty {
    BEGINNER,
    LOWER_INTERMEDIATE,
    INTERMEDIATE,
    UPPER_INTERMEDIATE,
    ADVANCED;

    public static Difficulty fromOrDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            return INTERMEDIATE;
        }
        try {
            return Difficulty.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return INTERMEDIATE;
        }
    }
}
