package com.cramer.catalog.service;

import com.cramer.platform.common.ielts.Skill;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Published contract (SPEC-04 §4, SPEC-24 §4) for persisting AI-generated content into the
 * catalog as <strong>draft</strong>. ABTS calls this instead of touching catalog repositories.
 * Saving never publishes (new sets/tests unpublished; sections {@code DRAFT}).
 */
public interface ContentDraftPort {

    SaveDraftResult saveDraft(SaveDraftCommand command);

    /** A save request: resolve/create set+test, then upsert sections + questions as draft. */
    record SaveDraftCommand(
            String setCode,
            Long setId,
            Integer testNumber,
            Long testId,
            JsonNode generationMetadata,
            List<DraftSection> sections) {
    }

    record DraftSection(
            Skill skill,
            int partNumber,
            String passageText,
            String audioUrl,
            JsonNode sectionLayout,
            String imageDescription,
            String displayContentUrl,
            List<DraftQuestion> questions) {
    }

    record DraftQuestion(
            int questionNumber,
            String questionType,
            JsonNode questionContent,
            JsonNode correctAnswer,
            JsonNode explanation,
            String wordLimit,
            String imageUrl) {
    }

    record SaveDraftResult(
            Long setId,
            String setCode,
            Long testId,
            Integer testNumber,
            List<Long> sectionIds,
            int questionCount) {
    }
}
