package com.cramer.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Create/update payload for a hashtag (SPEC-11 §4.1). {@code code} matches {@code ^[a-z0-9_-]+$}.
 */
public record HashtagRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9_-]+$") String code,
        String name,
        @NotBlank String category,
        String icon,
        String color) {
}
