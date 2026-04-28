package com.cramer.entity;

public enum SpeakingSessionStatus {
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    GRADING("grading"),
    GRADED("graded"),
    GRADING_FAILED("grading_failed"),
    ABANDONED("abandoned"),
    EXPIRED("expired");

    private final String dbValue;

    SpeakingSessionStatus(String dbValue) { this.dbValue = dbValue; }
    public String getDbValue() { return dbValue; }
    public static SpeakingSessionStatus fromDbValue(String dbValue) {
        for (SpeakingSessionStatus s : values()) {
            if (s.dbValue.equals(dbValue)) return s;
        }
        throw new IllegalArgumentException("Unknown status: " + dbValue);
    }
}
