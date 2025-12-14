package com.cramer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for writing test review data with AI grading results.
 */
public class WritingReviewDTO {
    
    private Long attemptId;
    private String examSource;
    private String testNumber;
    private String skill = "writing";
    private String status;
    private OffsetDateTime completedAt;
    private Long duration; // in seconds
    
    // Overall scores
    private BigDecimal overallBand;
    private Map<String, Object> averageBandScores; // Average across tasks
    
    // Individual task submissions
    private List<WritingTaskReviewDTO> tasks;
    
    // Task prompt information
    private List<WritingTaskPromptDTO> prompts;

    // Inner class for task review
    public static class WritingTaskReviewDTO {
        private Integer taskNumber;
        private String essayText;
        private Integer wordCount;
        private String gradingStatus;
        private BigDecimal overallBand;
        private Map<String, Object> bandScores;
        private Map<String, Object> aiFeedback;
        private OffsetDateTime submittedAt;
        private OffsetDateTime gradedAt;

        // Getters and Setters
        public Integer getTaskNumber() { return taskNumber; }
        public void setTaskNumber(Integer taskNumber) { this.taskNumber = taskNumber; }
        
        public String getEssayText() { return essayText; }
        public void setEssayText(String essayText) { this.essayText = essayText; }
        
        public Integer getWordCount() { return wordCount; }
        public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
        
        public String getGradingStatus() { return gradingStatus; }
        public void setGradingStatus(String gradingStatus) { this.gradingStatus = gradingStatus; }
        
        public BigDecimal getOverallBand() { return overallBand; }
        public void setOverallBand(BigDecimal overallBand) { this.overallBand = overallBand; }
        
        public Map<String, Object> getBandScores() { return bandScores; }
        public void setBandScores(Map<String, Object> bandScores) { this.bandScores = bandScores; }
        
        public Map<String, Object> getAiFeedback() { return aiFeedback; }
        public void setAiFeedback(Map<String, Object> aiFeedback) { this.aiFeedback = aiFeedback; }
        
        public OffsetDateTime getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }
        
        public OffsetDateTime getGradedAt() { return gradedAt; }
        public void setGradedAt(OffsetDateTime gradedAt) { this.gradedAt = gradedAt; }
    }

    // Inner class for task prompts
    public static class WritingTaskPromptDTO {
        private Integer taskNumber;
        private String promptText;
        private String imageUrl;
        private String questionType;

        // Getters and Setters
        public Integer getTaskNumber() { return taskNumber; }
        public void setTaskNumber(Integer taskNumber) { this.taskNumber = taskNumber; }
        
        public String getPromptText() { return promptText; }
        public void setPromptText(String promptText) { this.promptText = promptText; }
        
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        
        public String getQuestionType() { return questionType; }
        public void setQuestionType(String questionType) { this.questionType = questionType; }
    }

    // Constructors
    public WritingReviewDTO() {}

    // Getters and Setters
    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    
    public String getExamSource() { return examSource; }
    public void setExamSource(String examSource) { this.examSource = examSource; }
    
    public String getTestNumber() { return testNumber; }
    public void setTestNumber(String testNumber) { this.testNumber = testNumber; }
    
    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
    
    public BigDecimal getOverallBand() { return overallBand; }
    public void setOverallBand(BigDecimal overallBand) { this.overallBand = overallBand; }
    
    public Map<String, Object> getAverageBandScores() { return averageBandScores; }
    public void setAverageBandScores(Map<String, Object> averageBandScores) { this.averageBandScores = averageBandScores; }
    
    public List<WritingTaskReviewDTO> getTasks() { return tasks; }
    public void setTasks(List<WritingTaskReviewDTO> tasks) { this.tasks = tasks; }
    
    public List<WritingTaskPromptDTO> getPrompts() { return prompts; }
    public void setPrompts(List<WritingTaskPromptDTO> prompts) { this.prompts = prompts; }
}
