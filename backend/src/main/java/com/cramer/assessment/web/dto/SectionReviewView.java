package com.cramer.assessment.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** A section grouping in an attempt review (SPEC-12 §5). */
public record SectionReviewView(
        Long sectionId,
        Integer partNumber,
        String passageText,
        String audioUrl,
        String displayContentUrl,
        JsonNode sectionLayout,
        String imageDescription,
        List<QuestionReviewView> questions) {
}
