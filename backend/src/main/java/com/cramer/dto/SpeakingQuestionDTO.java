package com.cramer.dto;

import java.util.List;

/**
 * DTO for SpeakingQuestion responses.
 */
public class SpeakingQuestionDTO {

    private Long id;
    private Integer part;
    private String text;
    private Long topicId;
    private List<String> cueCardBullets;
    private Integer prepTimeSeconds;
    private Integer talkTimeSeconds;
    private String difficulty;

    // TTS (Text-to-Speech) fields for examiner audio
    private String examinerAudioUrl;
    private Integer examinerAudioDurationMs;

    // Constructors
    public SpeakingQuestionDTO() {
    }

    public SpeakingQuestionDTO(Long id, Integer part, String text, Long topicId) {
        this.id = id;
        this.part = part;
        this.text = text;
        this.topicId = topicId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPart() {
        return part;
    }

    public void setPart(Integer part) {
        this.part = part;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public List<String> getCueCardBullets() {
        return cueCardBullets;
    }

    public void setCueCardBullets(List<String> cueCardBullets) {
        this.cueCardBullets = cueCardBullets;
    }

    public Integer getPrepTimeSeconds() {
        return prepTimeSeconds;
    }

    public void setPrepTimeSeconds(Integer prepTimeSeconds) {
        this.prepTimeSeconds = prepTimeSeconds;
    }

    public Integer getTalkTimeSeconds() {
        return talkTimeSeconds;
    }

    public void setTalkTimeSeconds(Integer talkTimeSeconds) {
        this.talkTimeSeconds = talkTimeSeconds;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    // TTS Getters and Setters
    public String getExaminerAudioUrl() {
        return examinerAudioUrl;
    }

    public void setExaminerAudioUrl(String examinerAudioUrl) {
        this.examinerAudioUrl = examinerAudioUrl;
    }

    public Integer getExaminerAudioDurationMs() {
        return examinerAudioDurationMs;
    }

    public void setExaminerAudioDurationMs(Integer examinerAudioDurationMs) {
        this.examinerAudioDurationMs = examinerAudioDurationMs;
    }
}
