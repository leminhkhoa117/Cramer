package com.cramer.identity.web.dto;

/**
 * Self-service profile update (SPEC-10 §2.2). Only these fields are mutable; {@code username},
 * {@code is_admin}, {@code account_status}, and {@code created_at} are not updatable here.
 *
 * <p>{@code llmApiKey} semantics (SPEC-10 §2.3): {@code ""} clears the stored key; a non-empty
 * value stores it; {@code null}/absent leaves it unchanged.
 */
public record UpdateProfileRequest(
        String fullName,
        String phoneNumber,
        String address,
        String avatarUrl,
        String heroBackgroundUrl,
        String pageBackgroundUrl,
        String llmApiKey,
        String llmModel,
        String llmProvider) {
}
