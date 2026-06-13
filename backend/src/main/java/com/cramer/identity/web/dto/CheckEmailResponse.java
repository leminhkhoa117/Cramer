package com.cramer.identity.web.dto;

/** Outbound payload for {@code POST /api/auth/check-email} (SPEC-10 §2.1). */
public record CheckEmailResponse(boolean exists) {
}
