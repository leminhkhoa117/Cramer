package com.cramer.assessment.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Submit answers for grading (SPEC-12 §3). */
public record SubmitAnswersRequest(
        @NotNull List<AnswerInput> answers) {
}
