package com.cramer.assessment.web.dto;

import com.cramer.assessment.domain.Attempt;

import java.time.OffsetDateTime;

/** Attempt state projection for start/resume/get (SPEC-12 §3). */
public record AttemptView(
        Long id,
        String examSource,
        String testNumber,
        String skill,
        String status,
        Integer score,
        Integer currentPart,
        Integer timeLeft,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt) {

    public static AttemptView of(Attempt a) {
        return new AttemptView(
                a.getId(), a.getExamSource(), a.getTestNumber(), a.getSkill(),
                a.getStatus() == null ? null : a.getStatus().name(),
                a.getScore(), a.getCurrentPart(), a.getTimeLeft(), a.getStartedAt(), a.getCompletedAt());
    }
}
