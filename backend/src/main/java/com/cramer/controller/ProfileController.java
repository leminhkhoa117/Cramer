package com.cramer.controller;

import com.cramer.dto.ProfileDTO;
import com.cramer.entity.Profile;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.service.ProfileService;
import com.cramer.util.EntityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Profile management.
 * Provides CRUD operations for user profiles.
 */
@RestController
@RequestMapping("/api/profiles")
@CrossOrigin(origins = "*")
@Tag(name = "Profile Management", description = "APIs for managing user profiles")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;

    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Get all profiles.
     * GET /api/profiles
     */
    @Operation(
        summary = "Get all profiles",
        description = "Retrieve a list of all user profiles in the system"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<ProfileDTO>> getAllProfiles() {
        logger.info("REST request to get all profiles");
        List<ProfileDTO> profiles = profileService.getAllProfiles()
                .stream()
                .map(EntityMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(profiles);
    }

    /**
     * Get profile by ID.
     * GET /api/profiles/{id}
     */
    @Operation(
        summary = "Get profile by ID",
        description = "Retrieve a specific profile by its UUID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile found"),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProfileDTO> getProfileById(
            @Parameter(description = "UUID of the profile to retrieve") 
            @PathVariable UUID id) {
        logger.info("REST request to get profile by ID: {}", id);
        Profile profile = profileService.getProfileById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "id", id));
        return ResponseEntity.ok(EntityMapper.toDTO(profile));
    }

    /**
     * Get profile by username.
     * GET /api/profiles/username/{username}
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<ProfileDTO> getProfileByUsername(@PathVariable String username) {
        logger.info("REST request to get profile by username: {}", username);
        Profile profile = profileService.getProfileByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "username", username));
        return ResponseEntity.ok(EntityMapper.toDTO(profile));
    }

    /**
     * Create a new profile.
     * POST /api/profiles
     */
    @PostMapping
    public ResponseEntity<ProfileDTO> createProfile(@RequestBody ProfileDTO profileDTO) {
        logger.info("REST request to create profile: {}", profileDTO.getUsername());
        Profile profile = EntityMapper.toEntity(profileDTO);
        Profile createdProfile = profileService.createProfile(profile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EntityMapper.toDTO(createdProfile));
    }

    /**
     * Update an existing profile.
     * PUT /api/profiles/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProfileDTO> updateProfile(
            @PathVariable UUID id,
            @RequestBody ProfileDTO profileDTO) {
        logger.info("REST request to update profile: {}", id);
        Profile profile = EntityMapper.toEntity(profileDTO);
        Profile updatedProfile = profileService.updateProfile(id, profile);
        return ResponseEntity.ok(EntityMapper.toDTO(updatedProfile));
    }

    /**
     * Delete a profile.
     * DELETE /api/profiles/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID id) {
        logger.info("REST request to delete profile: {}", id);
        profileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if username exists.
     * GET /api/profiles/check-username/{username}
     */
    @GetMapping("/check-username/{username}")
    public ResponseEntity<Boolean> checkUsername(@PathVariable String username) {
        logger.info("REST request to check username: {}", username);
        boolean exists = profileService.usernameExists(username);
        return ResponseEntity.ok(exists);
    }

    /**
     * Get total profile count.
     * GET /api/profiles/count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getProfileCount() {
        logger.info("REST request to get profile count");
        long count = profileService.getTotalProfileCount();
        return ResponseEntity.ok(count);
    }
}
