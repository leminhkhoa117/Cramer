package com.cramer.catalog.web.dto;

import com.cramer.catalog.domain.Section;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Full section projection for <strong>admin</strong> (SPEC-11 §4). Unlike {@link TestSectionView}
 * (delivery), admin views may expose all authored fields.
 */
public record SectionAdminView(
        Long id,
        Long testId,
        String examSource,
        Integer testNumber,
        String skill,
        Integer partNumber,
        String passageText,
        String audioUrl,
        JsonNode sectionLayout,
        String imageDescription,
        String displayContentUrl,
        String status) {

    public static SectionAdminView of(Section s) {
        return new SectionAdminView(
                s.getId(), s.getTestId(), s.getExamSource(), s.getTestNumber(),
                s.getSkill() == null ? null : s.getSkill().dbValue(),
                s.getPartNumber(), s.getPassageText(), s.getAudioUrl(), s.getSectionLayout(),
                s.getImageDescription(), s.getDisplayContentUrl(),
                s.getStatus() == null ? null : s.getStatus().name());
    }
}
