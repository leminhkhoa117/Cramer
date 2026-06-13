package com.cramer.engagement.web.dto;

/** Vocabulary stats (SPEC-16 §3). */
public record VocabularyStats(long total, long mastered, long learning, int masteredPercent) {
}
