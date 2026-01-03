package com.cramer.dto.abts;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * DTO for AI-generated IELTS content.
 * Represents the structured output from OpenRouter API that can be
 * directly mapped to Section and Question entities.
 * 
 * @since 2025-12-20 - ABTS v2.0
 */
public class GeneratedContentDTO {

    /**
     * Generated section data (passage/transcript).
     */
    private GeneratedSectionDTO section;

    /**
     * Generated questions.
     */
    private List<GeneratedQuestionDTO> questions;

    /**
     * For Writing: Chart/graph data for Task 1.
     */
    private JsonNode chartData;

    /**
     * For Writing: Task type and word requirement.
     */
    private String taskType;
    private Integer wordRequirement;

    /**
     * For Writing Task 1 (GT): Letter context details.
     */
    private JsonNode letterContext;

    /**
     * For Writing Task 2: Essay metadata.
     */
    private JsonNode essayMetadata;

    /**
     * Detailed description for figures (maps, diagrams, process flows).
     */
    private JsonNode figureDescription;

    /**
     * For Listening: Audio placeholder metadata.
     */
    private AudioPlaceholderDTO audioPlaceholder;

    /**
     * Generation metadata from AI.
     */
    private ContentMetadataDTO metadata;

    // ==================== INNER DTOs ====================

    /**
     * Generated section DTO.
     */
    public static class GeneratedSectionDTO {
        private String passageText;
        private String taskText;
        private Integer wordCount;
        private Boolean wordCountValid;
        private String wordCountMessage;
        private JsonNode sectionLayout; // For Listening blocks
        private Integer partNumber;

        public String getPassageText() {
            return passageText;
        }

        public void setPassageText(String passageText) {
            this.passageText = passageText;
        }

        public String getTaskText() {
            return taskText;
        }

        public void setTaskText(String taskText) {
            this.taskText = taskText;
        }

        public Integer getWordCount() {
            return wordCount;
        }

        public void setWordCount(Integer wordCount) {
            this.wordCount = wordCount;
        }

        public Boolean getWordCountValid() {
            return wordCountValid;
        }

        public void setWordCountValid(Boolean wordCountValid) {
            this.wordCountValid = wordCountValid;
        }

        public String getWordCountMessage() {
            return wordCountMessage;
        }

        public void setWordCountMessage(String wordCountMessage) {
            this.wordCountMessage = wordCountMessage;
        }

        public JsonNode getSectionLayout() {
            return sectionLayout;
        }

        public void setSectionLayout(JsonNode sectionLayout) {
            this.sectionLayout = sectionLayout;
        }

        public Integer getPartNumber() {
            return partNumber;
        }

        public void setPartNumber(Integer partNumber) {
            this.partNumber = partNumber;
        }
    }

    /**
     * Generated question DTO.
     */
    public static class GeneratedQuestionDTO {
        private Integer questionNumber;
        private String questionType;
        private JsonNode questionContent;
        private List<String> correctAnswer;
        /**
         * Structured explanation object:
         * {
         *   "detail": "Detailed explanation in Vietnamese",
         *   "quote": "Direct quote from passage/transcript (in English)",
         *   "strategy": "Strategy tip for this question type (in Vietnamese)"
         * }
         */
        private JsonNode explanation;
        private String wordLimit;
        private String imageUrl;

        public Integer getQuestionNumber() {
            return questionNumber;
        }

        public void setQuestionNumber(Integer questionNumber) {
            this.questionNumber = questionNumber;
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

        public List<String> getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(List<String> correctAnswer) {
            this.correctAnswer = correctAnswer;
        }

        public JsonNode getExplanation() {
            return explanation;
        }

        public void setExplanation(JsonNode explanation) {
            this.explanation = explanation;
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

    /**
     * Audio placeholder for Listening parts.
     */
    public static class AudioPlaceholderDTO {
        private String durationEstimate;
        private Integer speakerCount;
        private List<String> speakerGenders;
        private String accentRecommendation;
        private String pacingNotes;
        private String backgroundAmbient;
        private Boolean ttsReady;

        public String getDurationEstimate() {
            return durationEstimate;
        }

        public void setDurationEstimate(String durationEstimate) {
            this.durationEstimate = durationEstimate;
        }

        public Integer getSpeakerCount() {
            return speakerCount;
        }

        public void setSpeakerCount(Integer speakerCount) {
            this.speakerCount = speakerCount;
        }

        public List<String> getSpeakerGenders() {
            return speakerGenders;
        }

        public void setSpeakerGenders(List<String> speakerGenders) {
            this.speakerGenders = speakerGenders;
        }

        public String getAccentRecommendation() {
            return accentRecommendation;
        }

        public void setAccentRecommendation(String accentRecommendation) {
            this.accentRecommendation = accentRecommendation;
        }

        public String getPacingNotes() {
            return pacingNotes;
        }

        public void setPacingNotes(String pacingNotes) {
            this.pacingNotes = pacingNotes;
        }

        public String getBackgroundAmbient() {
            return backgroundAmbient;
        }

        public void setBackgroundAmbient(String backgroundAmbient) {
            this.backgroundAmbient = backgroundAmbient;
        }

        public Boolean getTtsReady() {
            return ttsReady;
        }

        public void setTtsReady(Boolean ttsReady) {
            this.ttsReady = ttsReady;
        }
    }

    /**
     * Content metadata from AI generation.
     */
    public static class ContentMetadataDTO {
        private String topic;
        private String difficulty;
        private String bandRange;
        private String generatedAt;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }

        public String getBandRange() {
            return bandRange;
        }

        public void setBandRange(String bandRange) {
            this.bandRange = bandRange;
        }

        public String getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(String generatedAt) {
            this.generatedAt = generatedAt;
        }
    }

    // ==================== GETTERS AND SETTERS ====================

    public GeneratedSectionDTO getSection() {
        return section;
    }

    public void setSection(GeneratedSectionDTO section) {
        this.section = section;
    }

    public List<GeneratedQuestionDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<GeneratedQuestionDTO> questions) {
        this.questions = questions;
    }

    public JsonNode getChartData() {
        return chartData;
    }

    public void setChartData(JsonNode chartData) {
        this.chartData = chartData;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Integer getWordRequirement() {
        return wordRequirement;
    }

    public void setWordRequirement(Integer wordRequirement) {
        this.wordRequirement = wordRequirement;
    }

    public JsonNode getLetterContext() {
        return letterContext;
    }

    public void setLetterContext(JsonNode letterContext) {
        this.letterContext = letterContext;
    }

    public JsonNode getEssayMetadata() {
        return essayMetadata;
    }

    public void setEssayMetadata(JsonNode essayMetadata) {
        this.essayMetadata = essayMetadata;
    }

    public JsonNode getFigureDescription() {
        return figureDescription;
    }

    public void setFigureDescription(JsonNode figureDescription) {
        this.figureDescription = figureDescription;
    }

    public AudioPlaceholderDTO getAudioPlaceholder() {
        return audioPlaceholder;
    }

    public void setAudioPlaceholder(AudioPlaceholderDTO audioPlaceholder) {
        this.audioPlaceholder = audioPlaceholder;
    }

    public ContentMetadataDTO getMetadata() {
        return metadata;
    }

    public void setMetadata(ContentMetadataDTO metadata) {
        this.metadata = metadata;
    }
}
