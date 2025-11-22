package com.cramer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public class UserAnswerDTO {
    private Long id;
    private UUID userId;
    private Long questionId;
    private JsonNode answerContent;
    private String userAnswer; // The plain text answer, useful for resuming
    private OffsetDateTime submittedAt;
    private Boolean isCorrect;

    public UserAnswerDTO() {
    }

    // Constructor used by EntityMapper
    public UserAnswerDTO(Long id, UUID userId, Long questionId, JsonNode answerContent, OffsetDateTime submittedAt, Boolean isCorrect) {
        this.id = id;
        this.userId = userId;
        this.questionId = questionId;
        this.answerContent = answerContent;
        this.submittedAt = submittedAt;
        this.isCorrect = isCorrect;
    }
    
    // Constructor I was using, let's keep a variation for flexibility
    public UserAnswerDTO(Long questionId, String userAnswer, JsonNode answerContent) {
        this.questionId = questionId;
        this.userAnswer = userAnswer;
        this.answerContent = answerContent;
    }

    // Getters and Setters for all fields

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



    public JsonNode getAnswerContent() {
        return answerContent;
    }

    public void setAnswerContent(JsonNode answerContent) {
        this.answerContent = answerContent;
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

    public Boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(Boolean correct) {
        isCorrect = correct;
    }
}
