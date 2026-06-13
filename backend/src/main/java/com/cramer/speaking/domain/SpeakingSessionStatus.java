package com.cramer.speaking.domain;

/**
 * Speaking session lifecycle status (SPEC-14 §2). Stored <strong>lowercase</strong> in
 * {@code speaking_sessions.status} (verified live DB).
 */
public enum SpeakingSessionStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
    EXPIRED,
    GRADING,
    GRADED,
    GRADING_FAILED;

    /** DB representation (lowercase, e.g. {@code in_progress}). */
    public String dbValue() {
        return name().toLowerCase();
    }

    public static SpeakingSessionStatus from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("speaking status is required");
        }
        return SpeakingSessionStatus.valueOf(raw.trim().toUpperCase());
    }
}
