package com.cramer.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for Profile entity responses.
 */
public class ProfileDTO {
    private UUID id;
    private String username;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String avatarUrl;
    private String heroBackgroundUrl;
    private String pageBackgroundUrl;
    private String geminiApiKey;
    private boolean hasGeminiApiKey;
    private OffsetDateTime createdAt;

    public ProfileDTO() {
    }

    public ProfileDTO(UUID id, String username, String fullName, String phoneNumber, String address, String avatarUrl, String heroBackgroundUrl, String pageBackgroundUrl, String geminiApiKey, boolean hasGeminiApiKey, OffsetDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.heroBackgroundUrl = heroBackgroundUrl;
        this.pageBackgroundUrl = pageBackgroundUrl;
        this.geminiApiKey = geminiApiKey;
        this.hasGeminiApiKey = hasGeminiApiKey;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getHeroBackgroundUrl() {
        return heroBackgroundUrl;
    }

    public void setHeroBackgroundUrl(String heroBackgroundUrl) {
        this.heroBackgroundUrl = heroBackgroundUrl;
    }

    public String getPageBackgroundUrl() {
        return pageBackgroundUrl;
    }

    public void setPageBackgroundUrl(String pageBackgroundUrl) {
        this.pageBackgroundUrl = pageBackgroundUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }


    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public boolean isHasGeminiApiKey() {
        return hasGeminiApiKey;
    }

    public void setHasGeminiApiKey(boolean hasGeminiApiKey) {
        this.hasGeminiApiKey = hasGeminiApiKey;
    }
}
