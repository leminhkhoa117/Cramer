package com.cramer.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new speaking session.
 */
public class CreateSpeakingSessionDTO {

    @NotNull(message = "Session mode is required")
    private String mode;

    private Long topicId;

    // Constructors
    public CreateSpeakingSessionDTO() {
    }

    public CreateSpeakingSessionDTO(String mode, Long topicId) {
        this.mode = mode;
        this.topicId = topicId;
    }

    // Getters and Setters
    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }
}
