package com.cramer.dto;

import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for Profile entity responses.
 */
public class ProfileDTO {
    private UUID id;
    
    @Size(max = 100, message = "Username must be at most 100 characters")
    private String username;
    
    @Size(max = 255, message = "Full name must be at most 255 characters")
    private String fullName;
    
    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phoneNumber;
    
    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;
    
    @Size(max = 2048, message = "Avatar URL must be at most 2048 characters")
    private String avatarUrl;
    
    @Size(max = 2048, message = "Hero background URL must be at most 2048 characters")
    private String heroBackgroundUrl;
    
    @Size(max = 2048, message = "Page background URL must be at most 2048 characters")
    private String pageBackgroundUrl;
    
    @Size(max = 500, message = "LLM API key must be at most 500 characters")
    private String llmApiKey;
    
    private boolean hasLlmApiKey;
    
    @Size(max = 100, message = "LLM model must be at most 100 characters")
    private String llmModel;

    @Size(max = 50, message = "LLM provider must be at most 50 characters")
    private String llmProvider;
    
    private OffsetDateTime createdAt;

    public ProfileDTO() {
    }

    public ProfileDTO(UUID id, String username, String fullName, String phoneNumber, String address, String avatarUrl, String heroBackgroundUrl, String pageBackgroundUrl, String llmApiKey, boolean hasLlmApiKey, String llmModel, String llmProvider, OffsetDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.heroBackgroundUrl = heroBackgroundUrl;
        this.pageBackgroundUrl = pageBackgroundUrl;
        this.llmApiKey = llmApiKey;
        this.hasLlmApiKey = hasLlmApiKey;
        this.llmModel = llmModel;
        this.llmProvider = llmProvider;
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

    public String getLlmApiKey() {
        return llmApiKey;
    }

    public void setLlmApiKey(String llmApiKey) {
        this.llmApiKey = llmApiKey;
    }

    public boolean isHasLlmApiKey() {
        return hasLlmApiKey;
    }

    public void setHasLlmApiKey(boolean hasLlmApiKey) {
        this.hasLlmApiKey = hasLlmApiKey;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }
}
