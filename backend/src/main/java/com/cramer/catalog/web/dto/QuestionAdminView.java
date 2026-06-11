package com.cramer.catalog.web.dto;

import com.cramer.catalog.domain.Question;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Full question projection for <strong>admin</strong> (SPEC-11 §4), including the answer key and
 * explanation. Only admin and review (owner) surfaces may return these fields (SPEC-04 §3).
 */
public record QuestionAdminView(
        Long id,
        Long sectionId,
        Integer questionNumber,
        String questionUid,
        String questionType,
        JsonNode questionContent,
        JsonNode correctAnswer,
        JsonNode explanation,
        String imageUrl,
        String wordLimit) {

    public static QuestionAdminView of(Question q) {
        return new QuestionAdminView(
                q.getId(), q.getSectionId(), q.getQuestionNumber(), q.getQuestionUid(),
                q.getQuestionType() == null ? null : q.getQuestionType().name(),
                q.getQuestionContent(), q.getCorrectAnswer(), q.getExplanation(),
                q.getImageUrl(), q.getWordLimit());
    }
}
