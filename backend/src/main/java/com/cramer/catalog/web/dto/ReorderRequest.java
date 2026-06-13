package com.cramer.catalog.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Reorder payload for test sets (SPEC-11 §4): an ordered list of test-set ids; the index becomes
 * the new {@code display_order}.
 */
public record ReorderRequest(
        @NotEmpty List<Long> orderedIds) {
}
