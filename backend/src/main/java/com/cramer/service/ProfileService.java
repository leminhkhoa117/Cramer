package com.cramer.service;

import com.cramer.dto.ProfileDTO;

import java.util.UUID;

public interface ProfileService {
    ProfileDTO getProfileById(UUID id);
    ProfileDTO updateProfile(UUID id, ProfileDTO profileDto);
}
