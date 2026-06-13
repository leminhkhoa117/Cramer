package com.cramer.identity.web;

import com.cramer.identity.service.ProfileService;
import com.cramer.identity.web.dto.ProfileResponse;
import com.cramer.identity.web.dto.UpdateProfileRequest;
import com.cramer.platform.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Self-service profile endpoints (SPEC-10 §2.2). The authenticated user (from {@link CurrentUser})
 * must equal the path id, else 403 (IDOR guard).
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUser currentUser;

    public ProfileController(ProfileService profileService, CurrentUser currentUser) {
        this.profileService = profileService;
        this.currentUser = currentUser;
    }

    @GetMapping("/{id}")
    public ProfileResponse getProfile(@PathVariable UUID id) {
        return profileService.getProfile(currentUser.requireUserId(), id);
    }

    @PutMapping("/{id}")
    public ProfileResponse updateProfile(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(currentUser.requireUserId(), id, request);
    }
}
