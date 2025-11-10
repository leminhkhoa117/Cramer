package com.cramer.service;

import com.cramer.entity.Profile;
import com.cramer.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for Profile entity.
 * Handles business logic for user profile management.
 */
@Service
@Transactional
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    private final ProfileRepository profileRepository;

    @Autowired
    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /**
     * Get all profiles.
     * 
     * @return list of all profiles
     */
    @Transactional(readOnly = true)
    public List<Profile> getAllProfiles() {
        logger.info("Fetching all profiles");
        return profileRepository.findAll();
    }

    /**
     * Get profile by ID.
     * 
     * @param id the profile UUID
     * @return Optional containing the profile if found
     */
    @Transactional(readOnly = true)
    public Optional<Profile> getProfileById(UUID id) {
        try {
            logger.info("🔍 Fetching profile by ID: {}", id);
            
            if (id == null) {
                logger.error("❌ Profile ID cannot be null");
                throw new IllegalArgumentException("Profile ID cannot be null");
            }
            
            Optional<Profile> profile = profileRepository.findById(id);
            
            if (profile.isPresent()) {
                logger.info("✅ Profile found: id={}, username={}", id, profile.get().getUsername());
            } else {
                logger.warn("⚠️ Profile not found for id={}", id);
            }
            
            return profile;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error fetching profile by ID: {}", id, e);
            throw new RuntimeException("Failed to fetch profile: " + e.getMessage(), e);
        }
    }

    /**
     * Get profile by username.
     * 
     * @param username the username
     * @return Optional containing the profile if found
     */
    @Transactional(readOnly = true)
    public Optional<Profile> getProfileByUsername(String username) {
        logger.info("Fetching profile by username: {}", username);
        return profileRepository.findByUsername(username);
    }

    /**
     * Create a new profile.
     * 
     * @param profile the profile to create
     * @return the created profile
     * @throws IllegalArgumentException if username already exists
     */
    public Profile createProfile(Profile profile) {
        logger.info("Creating new profile with username: {}", profile.getUsername());
        
        if (profileRepository.existsByUsername(profile.getUsername())) {
            logger.error("Username already exists: {}", profile.getUsername());
            throw new IllegalArgumentException("Username already exists: " + profile.getUsername());
        }
        
        Profile savedProfile = profileRepository.save(profile);
        logger.info("Profile created successfully with ID: {}", savedProfile.getId());
        return savedProfile;
    }

    /**
     * Update an existing profile.
     * 
     * @param id the profile UUID
     * @param updatedProfile the updated profile data
     * @return the updated profile
     * @throws IllegalArgumentException if profile not found or username conflict
     */
    public Profile updateProfile(UUID id, Profile updatedProfile) {
        logger.info("Updating profile with ID: {}", id);
        
        Profile existingProfile = profileRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Profile not found with ID: {}", id);
                    return new IllegalArgumentException("Profile not found with ID: " + id);
                });
        
        // Check if new username conflicts with another user
        if (!existingProfile.getUsername().equals(updatedProfile.getUsername()) 
                && profileRepository.existsByUsername(updatedProfile.getUsername())) {
            logger.error("Username already taken: {}", updatedProfile.getUsername());
            throw new IllegalArgumentException("Username already taken: " + updatedProfile.getUsername());
        }
        
        existingProfile.setUsername(updatedProfile.getUsername());
        
        Profile savedProfile = profileRepository.save(existingProfile);
        logger.info("Profile updated successfully: {}", id);
        return savedProfile;
    }

    /**
     * Delete a profile by ID.
     * 
     * @param id the profile UUID
     * @throws IllegalArgumentException if profile not found
     */
    public void deleteProfile(UUID id) {
        logger.info("Deleting profile with ID: {}", id);
        
        if (!profileRepository.existsById(id)) {
            logger.error("Profile not found with ID: {}", id);
            throw new IllegalArgumentException("Profile not found with ID: " + id);
        }
        
        profileRepository.deleteById(id);
        logger.info("Profile deleted successfully: {}", id);
    }

    /**
     * Check if username exists.
     * 
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        logger.info("Checking if username exists: {}", username);
        return profileRepository.existsByUsername(username);
    }

    /**
     * Get total profile count.
     * 
     * @return total number of profiles
     */
    @Transactional(readOnly = true)
    public long getTotalProfileCount() {
        logger.info("Getting total profile count");
        return profileRepository.count();
    }
}
