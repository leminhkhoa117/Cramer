package com.cramer.identity.web.dto;

import com.cramer.identity.domain.Profile;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outbound profile projection (SPEC-10 §2.2/§2.3). The stored {@code llm_api_key} is never
 * returned — only {@code hasLlmApiKey}. {@code username}, {@code isAdmin}, {@code accountStatus},
 * and {@code createdAt} are read-only here.
 */
public record ProfileResponse(
        UUID id,
        String username,
        String fullName,
        String phoneNumber,
        String address,
        String avatarUrl,
        String heroBackgroundUrl,
        String pageBackgroundUrl,
        boolean hasLlmApiKey,
        String llmModel,
        String llmProvider,
        Boolean isAdmin,
        String accountStatus,
        OffsetDateTime createdAt) {

    public static ProfileResponse from(Profile p) {
        return new ProfileResponse(
                p.getId(),
                p.getUsername(),
                p.getFullName(),
                p.getPhoneNumber(),
                p.getAddress(),
                p.getAvatarUrl(),
                p.getHeroBackgroundUrl(),
                p.getPageBackgroundUrl(),
                p.hasLlmApiKey(),
                p.getLlmModel(),
                p.getLlmProvider(),
                p.getIsAdmin(),
                p.getAccountStatus() == null ? null : p.getAccountStatus().name(),
                p.getCreatedAt());
    }
}
