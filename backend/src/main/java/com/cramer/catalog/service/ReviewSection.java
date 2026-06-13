package com.cramer.catalog.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * A section with its full authored questions for attempt review (SPEC-12 §5), returned by
 * {@link ContentLookupPort#reviewContent}.
 */
public record ReviewSection(
        long sectionId,
        Integer partNumber,
        String passageText,
        String audioUrl,
        String displayContentUrl,
        JsonNode sectionLayout,
        String imageDescription,
        List<ReviewQuestion> questions) {
}
