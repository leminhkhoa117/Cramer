package com.cramer.catalog.service;

import com.cramer.platform.common.ielts.QuestionType;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A question with its answer key, returned by {@link ContentLookupPort#gradableQuestions(long)}
 * for <strong>server-side scoring only</strong> (SPEC-11 §5). Never serialized to an HTTP client.
 */
public record GradableQuestion(
        long questionId,
        Integer questionNumber,
        QuestionType questionType,
        JsonNode correctAnswer) {
}
