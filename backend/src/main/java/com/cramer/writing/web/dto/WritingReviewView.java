package com.cramer.writing.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Full writing review for an attempt (SPEC-13 §5). {@code weightedOverallBand} = Task1·⅓ +
 * Task2·⅔ (rounded 0.5). {@code averageBandScores} is populated (the old DTO left it empty).
 */
public record WritingReviewView(
        Long attemptId,
        Double weightedOverallBand,
        JsonNode averageBandScores,
        List<WritingTaskReview> tasks) {
}
