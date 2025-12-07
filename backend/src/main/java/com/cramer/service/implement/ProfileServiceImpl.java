package com.cramer.service.implement;

import com.cramer.dto.ProfileDTO;
import com.cramer.entity.Profile;
import com.cramer.repository.ProfileRepository;
import com.cramer.service.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDTO getProfileById(UUID id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile not found with id: " + id));
        return convertToDto(profile);
    }

    @Override
    @Transactional
    public ProfileDTO updateProfile(UUID id, ProfileDTO profileDto) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile not found with id: " + id));

        // Update fields from DTO
        profile.setFullName(profileDto.getFullName());
        profile.setPhoneNumber(profileDto.getPhoneNumber());
        profile.setAddress(profileDto.getAddress());
        
        if (profileDto.getAvatarUrl() != null) {
            profile.setAvatarUrl(profileDto.getAvatarUrl());
        }
        if (profileDto.getHeroBackgroundUrl() != null) {
            profile.setHeroBackgroundUrl(profileDto.getHeroBackgroundUrl());
        }
        if (profileDto.getPageBackgroundUrl() != null) {
            profile.setPageBackgroundUrl(profileDto.getPageBackgroundUrl());
        }
        
        // Update Gemini API key if provided (empty string clears it)
        if (profileDto.getGeminiApiKey() != null) {
            if (profileDto.getGeminiApiKey().isEmpty()) {
                profile.setGeminiApiKey(null); // Clear the key
            } else {
                profile.setGeminiApiKey(profileDto.getGeminiApiKey());
            }
        }
        
        // Update Gemini model if provided
        if (profileDto.getGeminiModel() != null) {
            profile.setGeminiModel(profileDto.getGeminiModel());
        }

        Profile updatedProfile = profileRepository.save(profile);
        return convertToDto(updatedProfile);
    }

    private ProfileDTO convertToDto(Profile profile) {
        ProfileDTO dto = new ProfileDTO();
        dto.setId(profile.getId());
        dto.setUsername(profile.getUsername());
        dto.setFullName(profile.getFullName());
        dto.setPhoneNumber(profile.getPhoneNumber());
        dto.setAddress(profile.getAddress());
        dto.setAvatarUrl(profile.getAvatarUrl());
        dto.setHeroBackgroundUrl(profile.getHeroBackgroundUrl());
        dto.setPageBackgroundUrl(profile.getPageBackgroundUrl());
        // For security, don't return the actual API key - just indicate if it exists
        dto.setHasGeminiApiKey(profile.getGeminiApiKey() != null && !profile.getGeminiApiKey().isEmpty());
        dto.setGeminiModel(profile.getGeminiModel());
        return dto;
    }
}
