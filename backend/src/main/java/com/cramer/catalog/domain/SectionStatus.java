package com.cramer.catalog.domain;

/**
 * Section publication lifecycle, stored in {@code sections.status} (verified DB values:
 * DRAFT, PUBLISHED; ARCHIVED reserved). Default PUBLISHED. SPEC-11 §1.
 */
public enum SectionStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
