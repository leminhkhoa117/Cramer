package com.cramer.catalog.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

/**
 * Create/update payload for a section (SPEC-11 §4, admin). Carries both the FK ({@code testId})
 * and the legacy lookup keys ({@code examSource}/{@code testNumber}); {@code skill} and
 * {@code status} are parsed against their enums.
 */
public record SectionRequest(
        Long testId,
        String examSource,
        Integer testNumber,
        @NotBlank String skill,
        Integer partNumber,
        String passageText,
        String audioUrl,
        JsonNode sectionLayout,
        String imageDescription,
        String displayContentUrl,
        String status) {
}
