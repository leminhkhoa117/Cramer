package com.cramer.assessment.web.dto;

import jakarta.validation.constraints.NotNull;

/** A single submitted answer: the question id and the raw text value (SPEC-12 §3). */
public record AnswerInput(
        @NotNull Long questionId,
        String value) {
}
