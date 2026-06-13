package com.cramer.assessment.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** A saved answer projection (SPEC-12 §2: {@code GET /answers}). */
public record AnswerView(
        Long questionId,
        JsonNode answerContent,
        String userAnswer,
        Boolean isCorrect) {
}
