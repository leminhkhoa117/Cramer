package com.cramer.catalog.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A reference to an authored Speaking prompt, returned by
 * {@link ContentLookupPort#speakingBank(long, int)} for blueprint building (SPEC-11 §5, SPEC-14 §3).
 * {@code questionContent} is the authored prompt JSON ({@code promptText}, cue-card, timing, …).
 */
public record SpeakingQuestionRef(
        long questionId,
        Integer partNumber,
        String questionUid,
        JsonNode questionContent) {
}
