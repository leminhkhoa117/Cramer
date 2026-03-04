package com.cramer.controller;

import com.cramer.dto.ProfileDTO;
import com.cramer.service.ProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.UUID;

/**
 * REST Controller for Profile management.
 * Provides CRUD operations for user profiles.
 */
@RestController
@RequestMapping("/api/profiles")
@Tag(name = "Profile Management", description = "APIs for managing user profiles")
public class ProfileController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;

    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Get profile by ID", description = "Retrieve a specific profile by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile found"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProfileDTO> getProfileById(
            @Parameter(description = "UUID of the profile to retrieve") @PathVariable UUID id,
            Authentication authentication) {
        logger.info("📥 GET /api/profiles/{} - Fetching profile", id);

        // IDOR protection: validate user owns this profile
        UUID currentUserId = getCurrentUserId(authentication);
        if (!currentUserId.equals(id)) {
            logger.warn("🚨 IDOR attempt: User {} tried to access profile {}", currentUserId, id);
            throw new AccessDeniedException("You can only access your own profile");
        }

        ProfileDTO profileDTO = profileService.getProfileById(id);
        return ResponseEntity.ok(profileDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileDTO> updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody ProfileDTO profileDTO,
            Authentication authentication) {
        logger.info("REST request to update profile: {}", id);

        // IDOR protection: validate user owns this profile
        UUID currentUserId = getCurrentUserId(authentication);
        if (!currentUserId.equals(id)) {
            logger.warn("🚨 IDOR attempt: User {} tried to update profile {}", currentUserId, id);
            throw new AccessDeniedException("You can only update your own profile");
        }

        ProfileDTO updatedProfile = profileService.updateProfile(id, profileDTO);
        return ResponseEntity.ok(updatedProfile);
    }

}
