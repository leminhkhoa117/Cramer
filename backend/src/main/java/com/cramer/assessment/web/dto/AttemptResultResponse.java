package com.cramer.assessment.web.dto;

import java.time.OffsetDateTime;

/**
 * Result of grading an attempt (SPEC-12 §4.1). {@code bandScore} is populated for Reading/Listening
 * (the fix: the old result DTO omitted it) and null for Writing (graded separately, SPEC-13).
 */
public record AttemptResultResponse(
        Long attemptId,
        String status,
        int score,
        int totalQuestions,
        Double bandScore,
        OffsetDateTime completedAt) {
}
