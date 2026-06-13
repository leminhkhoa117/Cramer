package com.cramer.catalog.web.dto;

import com.cramer.catalog.domain.TestSet;

import java.time.OffsetDateTime;

/**
 * Outbound test-set projection (SPEC-11 §3/§4). {@code testCount} is filled by the service where
 * relevant, else null.
 */
public record TestSetView(
        Long id,
        String code,
        String name,
        String description,
        String coverImageUrl,
        String sourceType,
        Boolean isPublished,
        Integer displayOrder,
        Long testCount,
        OffsetDateTime createdAt) {

    public static TestSetView of(TestSet s, Long testCount) {
        return new TestSetView(
                s.getId(), s.getCode(), s.getName(), s.getDescription(), s.getCoverImageUrl(),
                s.getSourceType(), s.getIsPublished(), s.getDisplayOrder(), testCount, s.getCreatedAt());
    }

    public static TestSetView of(TestSet s) {
        return of(s, null);
    }
}
