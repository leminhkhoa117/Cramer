package com.cramer.speaking.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Create a speaking session (SPEC-14 §2). {@code sessionMode} ∈ FULL/PART_1/PART_2/PART_3/
 * PART_2_AND_3; {@code accent} ∈ british/american/australian/neutral; {@code speed} ∈ 0.85/1.00/1.15.
 */
public record CreateSessionRequest(
        @NotNull Long testId,
        @NotBlank String sessionMode,
        String accent,
        Double speed) {
}
