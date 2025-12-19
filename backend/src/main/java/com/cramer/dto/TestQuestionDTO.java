package com.cramer.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Safe DTO for Question entity responses used in Test Taking mode.
 * EXCLUDES correct answers and explanations to prevent cheating.
 */
public class TestQuestionDTO {
    private Long id;
    private Long sectionId;
    private Integer questionNumber;
    private String questionUid;
    private String questionType;
    private JsonNode questionContent;
    private String wordLimit;
    private String imageUrl;

    public TestQuestionDTO() {
    }

    public TestQuestionDTO(Long id, Long sectionId, Integer questionNumber, String questionUid,
                      String questionType, JsonNode questionContent,
                      String wordLimit, String imageUrl) {
        this.id = id;
        this.sectionId = sectionId;
        this.questionNumber = questionNumber;
        this.questionUid = questionUid;
        this.questionType = questionType;
        this.questionContent = questionContent;
        this.wordLimit = wordLimit;
        this.imageUrl = imageUrl;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

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

    public String getWordLimit() {
        return wordLimit;
    }

    public void setWordLimit(String wordLimit) {
        this.wordLimit = wordLimit;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
