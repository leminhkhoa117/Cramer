package com.cramer.engagement.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Create/update a vocabulary entry (SPEC-16 §3). */
public record VocabularyRequest(
        @NotBlank String word,
        String translation,
        String phonetic,
        String partOfSpeech,
        String definition,
        String exampleSentence,
        String sourceContext,
        Long sourceTestId,
        Long sourceSectionId,
        String notes) {
}
