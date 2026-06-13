package com.cramer.assessment.web.dto;

import java.util.List;

/**
 * Save in-progress state (SPEC-12 §3). All fields optional; when {@code answers} is present it
 * replaces the stored answers (non-empty values only). {@code is_correct} is not set here.
 */
public record SaveProgressRequest(
        Integer timeLeft,
        Integer currentPart,
        List<AnswerInput> answers) {
}
