package com.cramer.catalog.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Create/update payload for a question (SPEC-11 §4, admin). Admin authoring includes the answer
 * key ({@code correctAnswer}) and {@code explanation}; these are never exposed by delivery.
 */
public record QuestionRequest(
        @NotNull Long sectionId,
        Integer questionNumber,
        String questionUid,
        @NotBlank String questionType,
        JsonNode questionContent,
        JsonNode correctAnswer,
        JsonNode explanation,
        String imageUrl,
        String wordLimit) {
}
