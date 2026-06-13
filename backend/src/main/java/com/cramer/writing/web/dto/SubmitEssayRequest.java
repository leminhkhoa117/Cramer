package com.cramer.writing.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Submit 1–2 essays for grading (SPEC-13 §3): a map of task number (1|2) → essay text.
 */
public record SubmitEssayRequest(
        @NotEmpty @Size(min = 1, max = 2) Map<Integer, String> essays) {
}
