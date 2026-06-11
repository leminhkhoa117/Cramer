package com.cramer.catalog.service;

import com.cramer.platform.common.ielts.QuestionType;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Full authored question data for attempt review (SPEC-12 §5), returned by
 * {@link ContentLookupPort#reviewContent}. Includes the answer key and explanation; used only by
 * the owner-review surface (SPEC-04 §3), never by delivery.
 */
public record ReviewQuestion(
        long questionId,
        Integer questionNumber,
        String questionUid,
        QuestionType questionType,
        JsonNode questionContent,
        JsonNode correctAnswer,
        JsonNode explanation) {
}
