package com.cramer.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class QuestionReviewDTO {
    private Integer questionNumber;
    private String questionUid;
    private String questionType;
    private JsonNode questionContent;
    private JsonNode userAnswerContent;
    private JsonNode correctAnswer;
    private Boolean isCorrect;
    private String explanation;

    // Constructors
    public QuestionReviewDTO() {}

    public QuestionReviewDTO(Integer questionNumber, String questionUid, String questionType, JsonNode questionContent, JsonNode userAnswerContent, JsonNode correctAnswer, Boolean isCorrect, String explanation) {
        this.questionNumber = questionNumber;
        this.questionUid = questionUid;
        this.questionType = questionType;
        this.questionContent = questionContent;
        this.userAnswerContent = userAnswerContent;
        this.correctAnswer = correctAnswer;
        this.isCorrect = isCorrect;
        this.explanation = explanation;
    }

    // Getters and Setters
    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestionUid() {
        return questionUid;
    }

    public void setQuestionUid(String questionUid) {
        this.questionUid = questionUid;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public JsonNode getQuestionContent() {
        return questionContent;
    }

    public void setQuestionContent(JsonNode questionContent) {
        this.questionContent = questionContent;
    }

    public JsonNode getUserAnswerContent() {
        return userAnswerContent;
    }

    public void setUserAnswerContent(JsonNode userAnswerContent) {
        this.userAnswerContent = userAnswerContent;
    }

    public JsonNode getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(JsonNode correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
