package com.cramer.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Inbound payload for {@code POST /api/auth/check-email} (SPEC-10 §2.1). */
public record CheckEmailRequest(
        @NotBlank @Email String email) {
}
