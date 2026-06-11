package com.cramer.catalog.web.dto;

import com.cramer.catalog.domain.Test;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Outbound test projection (SPEC-11 §4). {@code hashtagCodes} included where the service resolves
 * them, else null.
 */
public record TestView(
        Long id,
        Long setId,
        Integer testNumber,
        String name,
        String description,
        String difficulty,
        Integer estimatedTimeMinutes,
        Boolean isPublished,
        Boolean isAiGenerated,
        List<String> hashtagCodes,
        OffsetDateTime createdAt) {

    public static TestView of(Test t, List<String> hashtagCodes) {
        return new TestView(
                t.getId(), t.getSetId(), t.getTestNumber(), t.getName(), t.getDescription(),
                t.getDifficulty() == null ? null : t.getDifficulty().name(),
                t.getEstimatedTimeMinutes(), t.getIsPublished(), t.getIsAiGenerated(),
                hashtagCodes, t.getCreatedAt());
    }

    public static TestView of(Test t) {
        return of(t, null);
    }
}
