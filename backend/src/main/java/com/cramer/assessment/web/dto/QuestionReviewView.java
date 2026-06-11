package com.cramer.assessment.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One question in an attempt review (SPEC-12 §5). The only user-facing surface that exposes the
 * answer key + explanation (owner only, SPEC-04 §3).
 */
public record QuestionReviewView(
        Long questionId,
        Integer questionNumber,
        String questionUid,
        String questionType,
        JsonNode questionContent,
        String userAnswer,
        JsonNode correctAnswer,
        Boolean isCorrect,
        JsonNode explanation) {
}
