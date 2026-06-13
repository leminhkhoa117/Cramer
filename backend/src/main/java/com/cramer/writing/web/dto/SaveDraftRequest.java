package com.cramer.writing.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Draft save payload (SPEC-13 §3). */
public record SaveDraftRequest(
        @NotBlank String essayText) {
}
