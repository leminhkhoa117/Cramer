package com.cramer.assessment.web.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Full attempt review (SPEC-12 §5): metadata + score + a flat {@code questions[]} and grouped
 * {@code sections[]}. Owner-only; exposes answer keys.
 */
public record AttemptReviewView(
        Long attemptId,
        String examSource,
        String testNumber,
        String skill,
        String status,
        Integer score,
        int totalQuestions,
        Double bandScore,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Long durationSeconds,
        List<QuestionReviewView> questions,
        List<SectionReviewView> sections) {
}
