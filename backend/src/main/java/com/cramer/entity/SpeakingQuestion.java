package com.cramer.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Entity representing speaking questions for IELTS Speaking tests.
 * Questions are organized by topic and part (1, 2, or 3).
 */
@Entity
@Table(name = "speaking_questions", schema = "public")
public class SpeakingQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "topic_id")
    private Long topicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", insertable = false, updatable = false)
    private SpeakingTopic topic;

    @Column(name = "part", nullable = false)
    private Integer part;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cue_card_bullets", columnDefinition = "jsonb")
    private List<String> cueCardBullets;

    @Column(name = "difficulty", length = 10)
    private String difficulty;

    @Column(name = "register", length = 20)
    private String register;

    @Column(name = "expected_length_seconds")
    private Integer expectedLengthSeconds;

    @Column(name = "follow_up_allowed")
    private Boolean followUpAllowed = false;

    @Column(name = "follow_up_question_ids", columnDefinition = "bigint[]")
    private Long[] followUpQuestionIds;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // TTS (Text-to-Speech) fields for examiner audio
    @Column(name = "examiner_audio_url", length = 500)
    private String examinerAudioUrl;

    @Column(name = "examiner_audio_duration_ms")
    private Integer examinerAudioDurationMs;

    @Column(name = "tts_voice_id", length = 100)
    private String ttsVoiceId = "Rachel";

    @Column(name = "tts_generated_at")
    private OffsetDateTime ttsGeneratedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Constructors
    public SpeakingQuestion() {
    }

    public SpeakingQuestion(Long topicId, Integer part, String questionText) {
        this.topicId = topicId;
        this.part = part;
        this.questionText = questionText;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public SpeakingTopic getTopic() {
        return topic;
    }

    public void setTopic(SpeakingTopic topic) {
        this.topic = topic;
    }

    public Integer getPart() {
        return part;
    }

    public void setPart(Integer part) {
        this.part = part;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public List<String> getCueCardBullets() {
        return cueCardBullets;
    }

    public void setCueCardBullets(List<String> cueCardBullets) {
        this.cueCardBullets = cueCardBullets;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getRegister() {
        return register;
    }

    public void setRegister(String register) {
        this.register = register;
    }

    public Integer getExpectedLengthSeconds() {
        return expectedLengthSeconds;
    }

    public void setExpectedLengthSeconds(Integer expectedLengthSeconds) {
        this.expectedLengthSeconds = expectedLengthSeconds;
    }

    public Boolean getFollowUpAllowed() {
        return followUpAllowed;
    }

    public void setFollowUpAllowed(Boolean followUpAllowed) {
        this.followUpAllowed = followUpAllowed;
    }

    public Long[] getFollowUpQuestionIds() {
        return followUpQuestionIds;
    }

    public void setFollowUpQuestionIds(Long[] followUpQuestionIds) {
        this.followUpQuestionIds = followUpQuestionIds;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public String getTtsVoiceId() {
        return ttsVoiceId;
    }

    public void setTtsVoiceId(String ttsVoiceId) {
        this.ttsVoiceId = ttsVoiceId;
    }

    public OffsetDateTime getTtsGeneratedAt() {
        return ttsGeneratedAt;
    }

    public void setTtsGeneratedAt(OffsetDateTime ttsGeneratedAt) {
        this.ttsGeneratedAt = ttsGeneratedAt;
    }

    @Override
    public String toString() {
        return "SpeakingQuestion{" +
                "id=" + id +
                ", topicId=" + topicId +
                ", part=" + part +
                ", questionText='" + (questionText != null && questionText.length() > 50
                    ? questionText.substring(0, 50) + "..." : questionText) + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
