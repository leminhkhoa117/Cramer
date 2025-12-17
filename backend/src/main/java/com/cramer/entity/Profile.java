package com.cramer.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing user profiles.
 * Linked to Supabase auth.users table via id (UUID).
 */
@Entity
@Table(name = "profiles", schema = "public")
public class Profile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id; // Mirrors auth.users.id

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "hero_background_url")
    private String heroBackgroundUrl;

    @Column(name = "page_background_url")
    private String pageBackgroundUrl;

    @Column(name = "llm_api_key")
    private String llmApiKey;

    @Column(name = "llm_model")
    private String llmModel;

    @Column(name = "llm_provider", nullable = true)
    private String llmProvider = "deepseek";

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public Profile() {
    }

    public Profile(UUID id, String username) {
        this.id = id;
        this.username = username;
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

    public String getLlmApiKey() {
        return llmApiKey;
    }

    public void setLlmApiKey(String llmApiKey) {
        this.llmApiKey = llmApiKey;
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

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }


    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
