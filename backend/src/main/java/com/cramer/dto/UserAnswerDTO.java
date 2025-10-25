package com.cramer.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for UserAnswer entity responses.
 */
public class UserAnswerDTO {
    private Long id;
    private UUID userId;
    private Long questionId;
    private String userAnswer;
    private OffsetDateTime submittedAt;
    private Boolean isCorrect;
    private OffsetDateTime createdAt;

    public UserAnswerDTO() {
    }

    public UserAnswerDTO(Long id, UUID userId, Long questionId, String userAnswer,
                        OffsetDateTime submittedAt, Boolean isCorrect, OffsetDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.questionId = questionId;
        this.userAnswer = userAnswer;
        this.submittedAt = submittedAt;
        this.isCorrect = isCorrect;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
