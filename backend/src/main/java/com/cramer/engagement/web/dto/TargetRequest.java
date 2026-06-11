package com.cramer.engagement.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Upsert an IELTS goal (SPEC-16 §5). Band fields are optional but constrained 0–9 to match the
 * DB check constraints.
 */
public record TargetRequest(
        @NotBlank String examName,
        LocalDate examDate,
        @DecimalMin("0") @DecimalMax("9") Double listening,
        @DecimalMin("0") @DecimalMax("9") Double reading,
        @DecimalMin("0") @DecimalMax("9") Double writing,
        @DecimalMin("0") @DecimalMax("9") Double speaking) {
}
