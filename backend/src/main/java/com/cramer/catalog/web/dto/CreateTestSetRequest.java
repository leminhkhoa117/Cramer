package com.cramer.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload for a test set (SPEC-11 §4.1). {@code code} and {@code name} are required;
 * {@code sourceType} defaults to {@code custom} and {@code isPublished} to false when omitted.
 */
public record CreateTestSetRequest(
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 255) String name,
        String description,
        String coverImageUrl,
        String sourceType,
        Boolean isPublished,
        Integer displayOrder) {
}
