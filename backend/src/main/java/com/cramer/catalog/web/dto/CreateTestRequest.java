package com.cramer.catalog.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Create payload for a test within a set (SPEC-11 §4.1). {@code testNumber} is optional; when
 * omitted the service assigns {@code max+1}. Defaults: difficulty INTERMEDIATE,
 * estimatedTimeMinutes 170, unpublished.
 */
public record CreateTestRequest(
        @Min(1) Integer testNumber,
        @Size(max = 255) String name,
        String description,
        String difficulty,
        @Min(1) Integer estimatedTimeMinutes,
        Boolean isPublished) {
}
