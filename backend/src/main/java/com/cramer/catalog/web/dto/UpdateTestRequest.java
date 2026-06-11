package com.cramer.catalog.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Update payload for a test (SPEC-11 §4). All fields optional; null leaves the field unchanged.
 */
public record UpdateTestRequest(
        @Min(1) Integer testNumber,
        @Size(max = 255) String name,
        String description,
        String difficulty,
        @Min(1) Integer estimatedTimeMinutes,
        Boolean isPublished) {
}
