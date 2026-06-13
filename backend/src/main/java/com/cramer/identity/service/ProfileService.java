package com.cramer.identity.service;

import com.cramer.identity.domain.Profile;
import com.cramer.identity.repository.ProfileRepository;
import com.cramer.identity.web.dto.ProfileResponse;
import com.cramer.identity.web.dto.UpdateProfileRequest;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Self-service profile read/update (SPEC-10 §2.2). Enforces self-access (IDOR guard): the
 * authenticated requester must equal the target id, else 403. A missing profile is 404 (the
 * corrected behavior — the old code threw → 500).
 */
@Service
public class ProfileService {

    private final ProfileRepository profiles;

    public ProfileService(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID requesterId, UUID targetId) {
        requireSelf(requesterId, targetId);
        return ProfileResponse.from(load(targetId));
    }

    @Transactional
    public ProfileResponse updateProfile(UUID requesterId, UUID targetId, UpdateProfileRequest req) {
        requireSelf(requesterId, targetId);
        Profile p = load(targetId);

        p.setFullName(req.fullName());
        p.setPhoneNumber(req.phoneNumber());
        p.setAddress(req.address());
        if (req.avatarUrl() != null) {
            p.setAvatarUrl(req.avatarUrl());
        }
        if (req.heroBackgroundUrl() != null) {
            p.setHeroBackgroundUrl(req.heroBackgroundUrl());
        }
        if (req.pageBackgroundUrl() != null) {
            p.setPageBackgroundUrl(req.pageBackgroundUrl());
        }
        if (req.llmModel() != null) {
            p.setLlmModel(req.llmModel());
        }
        if (req.llmProvider() != null) {
            p.setLlmProvider(req.llmProvider());
        }
        // llmApiKey: "" clears; non-empty stores; null leaves unchanged (SPEC-10 §2.3)
        if (req.llmApiKey() != null) {
            p.setLlmApiKey(req.llmApiKey().isEmpty() ? null : req.llmApiKey());
        }

        return ProfileResponse.from(profiles.save(p));
    }

    private Profile load(UUID id) {
        return profiles.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Profile", id));
    }

    private void requireSelf(UUID requesterId, UUID targetId) {
        if (requesterId == null || !requesterId.equals(targetId)) {
            throw new OperationNotAllowedException("Cannot access another user's profile");
        }
    }
}
