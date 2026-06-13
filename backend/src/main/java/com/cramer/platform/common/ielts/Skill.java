package com.cramer.platform.common.ielts;

/**
 * IELTS skill — shared-kernel vocabulary used across catalog, assessment, speaking, and abts
 * (SPEC-18 §7). Maps to {@code sections.skill}/{@code test_attempts.skill}, stored
 * <strong>lowercase</strong> in the DB.
 */
public enum Skill {
    READING,
    LISTENING,
    WRITING,
    SPEAKING;

    /** Parse a DB/string value tolerantly (case-insensitive). */
    public static Skill from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("skill is required");
        }
        return Skill.valueOf(raw.trim().toUpperCase());
    }

    /** DB representation (lowercase). */
    public String dbValue() {
        return name().toLowerCase();
    }
}
