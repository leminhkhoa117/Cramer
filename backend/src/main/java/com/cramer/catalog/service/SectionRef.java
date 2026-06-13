package com.cramer.catalog.service;

import com.cramer.platform.common.ielts.Skill;

/**
 * Lightweight reference to a section, returned by {@link ContentLookupPort}. Carries both the FK
 * and legacy identifiers so runtime callers can resolve content either way (SPEC-11 §1.1/§5).
 */
public record SectionRef(
        long sectionId,
        Long testId,
        String examSource,
        Integer testNumber,
        Skill skill,
        Integer partNumber) {
}
