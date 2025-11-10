package com.cramer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public class UserAnswerDTO {
    private Long id;
    private UUID userId;
    private Long questionId;
    private JsonNode answerContent;
    private OffsetDateTime submittedAt;
    private boolean isCorrect;

    public UserAnswerDTO(Long id, UUID userId, Long questionId, JsonNode answerContent, OffsetDateTime submittedAt, boolean isCorrect) {
        this.id = id;
        this.userId = userId;
        this.questionId = questionId;
        this.answerContent = answerContent;
        this.submittedAt = submittedAt;
        this.isCorrect = isCorrect;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public JsonNode getAnswerContent() { return answerContent; }
    public void setAnswerContent(JsonNode answerContent) { this.answerContent = answerContent; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }
    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
}
