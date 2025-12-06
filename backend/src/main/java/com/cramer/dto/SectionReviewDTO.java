package com.cramer.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * DTO representing a section (passage/part) for the test review page.
 * Contains the section content and associated questions.
 */
public class SectionReviewDTO {
    private Long sectionId;
    private Integer partNumber;
    private String passageText;          // Full text content for Reading passages
    private String displayContentUrl;     // Optional URL to image/PDF
    private String audioUrl;              // URL for listening audio files
    private JsonNode sectionLayout;       // Flexible block-based layouts for Listening
    private List<QuestionReviewDTO> questions;

    // Constructors
    public SectionReviewDTO() {}

    public SectionReviewDTO(Long sectionId, Integer partNumber, String passageText, 
                            String displayContentUrl, String audioUrl, JsonNode sectionLayout,
                            List<QuestionReviewDTO> questions) {
        this.sectionId = sectionId;
        this.partNumber = partNumber;
        this.passageText = passageText;
        this.displayContentUrl = displayContentUrl;
        this.audioUrl = audioUrl;
        this.sectionLayout = sectionLayout;
        this.questions = questions;
    }

    // Getters and Setters
    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    public String getPassageText() {
        return passageText;
    }

    public void setPassageText(String passageText) {
        this.passageText = passageText;
    }

    public String getDisplayContentUrl() {
        return displayContentUrl;
    }

    public void setDisplayContentUrl(String displayContentUrl) {
        this.displayContentUrl = displayContentUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public JsonNode getSectionLayout() {
        return sectionLayout;
    }

    public void setSectionLayout(JsonNode sectionLayout) {
        this.sectionLayout = sectionLayout;
    }

    public List<QuestionReviewDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionReviewDTO> questions) {
        this.questions = questions;
    }
}
