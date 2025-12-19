package com.cramer.service.implement;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cramer.dto.ProfileDTO;
import com.cramer.entity.Profile;
import com.cramer.repository.ProfileRepository;
import com.cramer.service.ProfileService;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDTO getProfileById(UUID id) {
        Profile profile = profileRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Profile not found with id: " + id));
        return convertToDto(profile);
    }

    @Override
    @Transactional
    public ProfileDTO updateProfile(UUID id, ProfileDTO profileDto) {
        Profile profile = profileRepository.findById(Objects.requireNonNull(id))
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

        // Update LLM API key if provided (empty string clears it)
        if (profileDto.getLlmApiKey() != null) {
            if (profileDto.getLlmApiKey().isEmpty()) {
                profile.setLlmApiKey(null); // Clear the key
            } else {
                profile.setLlmApiKey(profileDto.getLlmApiKey());
            }
        }

        // Update LLM model if provided
        if (profileDto.getLlmModel() != null) {
            profile.setLlmModel(profileDto.getLlmModel());
        }

        // Update LLM provider if provided
        if (profileDto.getLlmProvider() != null) {
            profile.setLlmProvider(profileDto.getLlmProvider());
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
        dto.setHasLlmApiKey(profile.getLlmApiKey() != null && !profile.getLlmApiKey().isEmpty());
        dto.setLlmModel(profile.getLlmModel());
        dto.setLlmProvider(profile.getLlmProvider());
        dto.setIsAdmin(profile.getIsAdmin());
        dto.setCreatedAt(profile.getCreatedAt());
        return dto;
    }
}
