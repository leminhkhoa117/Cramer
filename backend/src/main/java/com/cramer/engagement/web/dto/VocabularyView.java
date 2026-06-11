package com.cramer.engagement.web.dto;

import com.cramer.engagement.domain.Vocabulary;

import java.time.OffsetDateTime;

/** Vocabulary projection (SPEC-16 §3). */
public record VocabularyView(
        Long id,
        String word,
        String translation,
        String phonetic,
        String partOfSpeech,
        String definition,
        String exampleSentence,
        String sourceContext,
        String notes,
        Boolean isMastered,
        Integer reviewCount,
        OffsetDateTime lastReviewedAt,
        OffsetDateTime createdAt) {

    public static VocabularyView of(Vocabulary v) {
        return new VocabularyView(v.getId(), v.getWord(), v.getTranslation(), v.getPhonetic(),
                v.getPartOfSpeech(), v.getDefinition(), v.getExampleSentence(), v.getSourceContext(),
                v.getNotes(), v.getIsMastered(), v.getReviewCount(), v.getLastReviewedAt(), v.getCreatedAt());
    }
}
