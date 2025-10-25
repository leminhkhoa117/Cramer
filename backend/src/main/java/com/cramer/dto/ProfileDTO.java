package com.cramer.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for Profile entity responses.
 */
public class ProfileDTO {
    private UUID id;
    private String username;
    private OffsetDateTime createdAt;

    public ProfileDTO() {
    }

    public ProfileDTO(UUID id, String username, OffsetDateTime createdAt) {
        this.id = id;
        this.username = username;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
