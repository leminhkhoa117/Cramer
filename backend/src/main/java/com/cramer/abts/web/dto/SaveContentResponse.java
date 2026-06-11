package com.cramer.abts.web.dto;

import java.util.List;

/**
 * Save-as-draft response (SPEC-24 §5). Failures propagate to the global handler — never a
 * {@code 200 {success:false}} wrapper.
 */
public record SaveContentResponse(
        boolean success,
        Long setId,
        String setCode,
        Long testId,
        Integer testNumber,
        List<Long> sectionIds,
        int questionCount,
        String message) {
}
