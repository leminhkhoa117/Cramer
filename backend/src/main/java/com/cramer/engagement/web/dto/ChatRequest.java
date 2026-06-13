package com.cramer.engagement.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Chat request (SPEC-16 §2). */
public record ChatRequest(@NotBlank String message) {
}
