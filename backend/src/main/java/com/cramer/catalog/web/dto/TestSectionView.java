package com.cramer.catalog.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Answer-free section projection for test delivery (SPEC-11 §2). Contains the passage/audio/layout
 * a candidate needs, plus its {@link TestQuestionView}s — never any answer key.
 */
public record TestSectionView(
        long id,
        Long testId,
        String skill,
        Integer partNumber,
        String passageText,
        String audioUrl,
        JsonNode sectionLayout,
        String displayContentUrl,
        String imageDescription,
        List<TestQuestionView> questions) {
}
