package com.cramer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for speaking transcript responses.
 */
public class SpeakingTranscriptDTO {

    private Long transcriptId;
    private Long questionId;
    private Integer part;
    private String audioUrl;
    private Integer audioDurationSeconds;
    private String transcriptText;
    private BigDecimal transcriptConfidence;
    private OffsetDateTime recordedAt;
    private String questionText; // Question text for context

    // Constructors
    public SpeakingTranscriptDTO() {
    }

    // Getters and Setters
    public Long getTranscriptId() {
        return transcriptId;
    }

    public void setTranscriptId(Long transcriptId) {
        this.transcriptId = transcriptId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Integer getPart() {
        return part;
    }

    public void setPart(Integer part) {
        this.part = part;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public Integer getAudioDurationSeconds() {
        return audioDurationSeconds;
    }

    public void setAudioDurationSeconds(Integer audioDurationSeconds) {
        this.audioDurationSeconds = audioDurationSeconds;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
    }

    public BigDecimal getTranscriptConfidence() {
        return transcriptConfidence;
    }

    public void setTranscriptConfidence(BigDecimal transcriptConfidence) {
        this.transcriptConfidence = transcriptConfidence;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }
}
