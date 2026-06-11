package com.cramer.catalog.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Answer-free question projection for test delivery (SPEC-11 §2, SPEC-04 §3). Deliberately omits
 * {@code correct_answer} and {@code explanation} — there is no field through which an answer key
 * could leak (a compile-time guarantee).
 */
public record TestQuestionView(
        long id,
        Integer questionNumber,
        String questionUid,
        String questionType,
        JsonNode questionContent,
        String imageUrl,
        String wordLimit) {
}
