package com.cramer.abts.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Save-as-draft request (SPEC-24 §4, SPEC-25 §2.4). Resolves/creates a set + test, then upserts
 * the sections and replaces their questions — all as <strong>draft</strong>. The generated
 * question objects are passed through per section.
 *
 * @param setCode            target set code (default {@code ai_generated})
 * @param setId              target set id (alternative to code)
 * @param testNumber         target test number (null → max+1 within set)
 * @param testId             target test id (alternative to number)
 * @param testName           test display name
 * @param difficulty         test difficulty
 * @param hashtags           up to 20 hashtag codes
 * @param generationMetadata model/prompt/usage metadata to record on a new test
 * @param sections           generated sections to persist
 */
public record SaveContentRequest(
        String setCode,
        Long setId,
        Integer testNumber,
        Long testId,
        String testName,
        String difficulty,
        List<String> hashtags,
        JsonNode generationMetadata,
        List<SaveSectionInput> sections) {

    public List<SaveSectionInput> safeSections() {
        return sections == null ? List.of() : sections;
    }

    /**
     * One generated section to save.
     *
     * @param skill            reading / listening / writing
     * @param partNumber       IELTS part (or writing task number)
     * @param passageText      reading passage / listening transcript text
     * @param audioUrl         listening audio URL (persisted, SPEC-24 §4.4)
     * @param sectionLayout    layout / writing-meta JSONB
     * @param imageDescription figure description → {@code sections.image_description}
     * @param questions        array of generated question objects
     */
    public record SaveSectionInput(
            String skill,
            int partNumber,
            String passageText,
            String audioUrl,
            JsonNode sectionLayout,
            String imageDescription,
            JsonNode questions) {
    }
}
