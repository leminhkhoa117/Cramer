package com.cramer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for speaking session result/evaluation responses.
 */
public class SpeakingResultDTO {

    private Long sessionId;
    private String sessionMode;
    private String sessionStatus; // Status: completed, grading, graded, failed
    private BigDecimal overallBand;

    // Criteria scores
    private CriterionDTO fluency;
    private CriterionDTO lexical;
    private CriterionDTO grammar;
    private CriterionDTO pronunciation;

    // Overall feedback
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> suggestions;
    private String overallFeedback;

    // Transcripts
    private List<TranscriptWithQuestionDTO> transcripts;

    // Sample answers
    private Map<String, String> sampleAnswers;

    private OffsetDateTime completedAt;
    private Integer totalDurationSeconds;

    // Nested DTO for criterion scores
    public static class CriterionDTO {
        private BigDecimal band;
        private String label;
        private String notes;
        private List<String> strengths;
        private List<String> weaknesses;
        private Map<String, Object> evidence;

        // Constructors
        public CriterionDTO() {
        }

        public CriterionDTO(BigDecimal band, String label) {
            this.band = band;
            this.label = label;
        }

        // Getters and Setters
        public BigDecimal getBand() {
            return band;
        }

        public void setBand(BigDecimal band) {
            this.band = band;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public List<String> getStrengths() {
            return strengths;
        }

        public void setStrengths(List<String> strengths) {
            this.strengths = strengths;
        }

        public List<String> getWeaknesses() {
            return weaknesses;
        }

        public void setWeaknesses(List<String> weaknesses) {
            this.weaknesses = weaknesses;
        }

        public Map<String, Object> getEvidence() {
            return evidence;
        }

        public void setEvidence(Map<String, Object> evidence) {
            this.evidence = evidence;
        }
    }

    // Nested DTO for transcript with question info
    public static class TranscriptWithQuestionDTO {
        private Long transcriptId;
        private Long questionId;
        private Integer part;
        private String questionText;
        private String transcriptText;
        private String audioUrl;
        private Integer audioDurationSeconds;
        private String examinerAudioUrl;
        private Integer examinerAudioDurationMs;

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

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public String getTranscriptText() {
            return transcriptText;
        }

        public void setTranscriptText(String transcriptText) {
            this.transcriptText = transcriptText;
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

    // Constructors
    public SpeakingResultDTO() {
    }

    // Getters and Setters
    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionMode() {
        return sessionMode;
    }

    public void setSessionMode(String sessionMode) {
        this.sessionMode = sessionMode;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public BigDecimal getOverallBand() {
        return overallBand;
    }

    public void setOverallBand(BigDecimal overallBand) {
        this.overallBand = overallBand;
    }

    public CriterionDTO getFluency() {
        return fluency;
    }

    public void setFluency(CriterionDTO fluency) {
        this.fluency = fluency;
    }

    public CriterionDTO getLexical() {
        return lexical;
    }

    public void setLexical(CriterionDTO lexical) {
        this.lexical = lexical;
    }

    public CriterionDTO getGrammar() {
        return grammar;
    }

    public void setGrammar(CriterionDTO grammar) {
        this.grammar = grammar;
    }

    public CriterionDTO getPronunciation() {
        return pronunciation;
    }

    public void setPronunciation(CriterionDTO pronunciation) {
        this.pronunciation = pronunciation;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public String getOverallFeedback() {
        return overallFeedback;
    }

    public void setOverallFeedback(String overallFeedback) {
        this.overallFeedback = overallFeedback;
    }

    public List<TranscriptWithQuestionDTO> getTranscripts() {
        return transcripts;
    }

    public void setTranscripts(List<TranscriptWithQuestionDTO> transcripts) {
        this.transcripts = transcripts;
    }

    public Map<String, String> getSampleAnswers() {
        return sampleAnswers;
    }

    public void setSampleAnswers(Map<String, String> sampleAnswers) {
        this.sampleAnswers = sampleAnswers;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(Integer totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }
}
