package com.cramer.engagement.web.dto;

/** Translation result (SPEC-16 §3). */
public record TranslationView(
        String translation,
        String phonetic,
        String partOfSpeech,
        String definition,
        String exampleSentence) {
}
