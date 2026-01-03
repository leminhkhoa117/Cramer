package com.cramer.dto.abts;

import java.util.List;

/**
 * DTO for ABTS generation responses.
 * Contains generated content, validation results, and metadata.
 * 
 * @since 2025-12-20 - ABTS v2.0
 */
public class GenerationResponseDTO {

    /**
     * Generation status.
     */
    public enum GenerationStatus {
        SUCCESS, // All content generated and validated
        PARTIAL_SUCCESS, // Content generated but has warnings
        FAILED // Generation failed
    }

    /**
     * Overall generation status.
     */
    private GenerationStatus status;

    /**
     * Generated content (section and questions).
     * Used for SINGLE_PART scope.
     */
    private GeneratedContentDTO content;

    /**
     * Multiple generated parts (for FULL_SKILL scope).
     * Each entry contains content for one part.
     * Index 0 = Part 1, Index 1 = Part 2, etc.
     */
    private List<GeneratedContentDTO> parts;

    /**
     * Validation results.
     */
    private ValidationResultDTO validation;

    /**
     * Generation metadata (timing, token usage, etc.).
     */
    private GenerationMetadataDTO metadata;

    /**
     * AI reasoning/Chain-of-Thought content (if enabled).
     */
    private String reasoning;

    /**
     * Error messages (if any).
     */
    private List<String> errors;

    /**
     * Warning messages (if any).
     */
    private List<String> warnings;

    // ==================== STATIC FACTORY METHODS ====================

    /**
     * Create a success response.
     */
    public static GenerationResponseDTO success(GeneratedContentDTO content, GenerationMetadataDTO metadata) {
        GenerationResponseDTO response = new GenerationResponseDTO();
        response.setStatus(GenerationStatus.SUCCESS);
        response.setContent(content);
        response.setMetadata(metadata);
        return response;
    }

    /**
     * Create an error response.
     */
    public static GenerationResponseDTO error(String errorCode, String message, boolean retryable) {
        GenerationResponseDTO response = new GenerationResponseDTO();
        response.setStatus(GenerationStatus.FAILED);
        response.setErrors(List.of(errorCode + ": " + message));

        GenerationMetadataDTO metadata = new GenerationMetadataDTO();
        metadata.setRetryable(retryable);
        response.setMetadata(metadata);

        return response;
    }

    // ==================== GETTERS AND SETTERS ====================

    public GenerationStatus getStatus() {
        return status;
    }

    public void setStatus(GenerationStatus status) {
        this.status = status;
    }

    public GeneratedContentDTO getContent() {
        return content;
    }

    public void setContent(GeneratedContentDTO content) {
        this.content = content;
    }

    public List<GeneratedContentDTO> getParts() {
        return parts;
    }

    public void setParts(List<GeneratedContentDTO> parts) {
        this.parts = parts;
    }

    public ValidationResultDTO getValidation() {
        return validation;
    }

    public void setValidation(ValidationResultDTO validation) {
        this.validation = validation;
    }

    public GenerationMetadataDTO getMetadata() {
        return metadata;
    }

    public void setMetadata(GenerationMetadataDTO metadata) {
        this.metadata = metadata;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    // ==================== INNER DTOs ====================

    /**
     * Validation result DTO.
     */
    public static class ValidationResultDTO {
        private boolean valid;
        private List<String> schemaErrors;
        private List<String> contentErrors;
        private List<String> businessRuleErrors;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<String> getSchemaErrors() {
            return schemaErrors;
        }

        public void setSchemaErrors(List<String> schemaErrors) {
            this.schemaErrors = schemaErrors;
        }

        public List<String> getContentErrors() {
            return contentErrors;
        }

        public void setContentErrors(List<String> contentErrors) {
            this.contentErrors = contentErrors;
        }

        public List<String> getBusinessRuleErrors() {
            return businessRuleErrors;
        }

        public void setBusinessRuleErrors(List<String> businessRuleErrors) {
            this.businessRuleErrors = businessRuleErrors;
        }
    }

    /**
     * Generation metadata DTO.
     */
    public static class GenerationMetadataDTO {
        private String topic;
        private String difficulty;
        private String bandRange;
        private Integer wordCount;
        private Integer questionCount;
        private Double generationTimeSeconds;
        private String generatedAt;
        private String modelUsed;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer reasoningTokens;
        private Double estimatedCostUsd;
        private Boolean retryable;
        private Integer failedAttempts;
        private String lastError;

        // Getters and Setters
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

        public Integer getWordCount() {
            return wordCount;
        }

        public void setWordCount(Integer wordCount) {
            this.wordCount = wordCount;
        }

        public Integer getQuestionCount() {
            return questionCount;
        }

        public void setQuestionCount(Integer questionCount) {
            this.questionCount = questionCount;
        }

        public Double getGenerationTimeSeconds() {
            return generationTimeSeconds;
        }

        public void setGenerationTimeSeconds(Double generationTimeSeconds) {
            this.generationTimeSeconds = generationTimeSeconds;
        }

        public String getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(String generatedAt) {
            this.generatedAt = generatedAt;
        }

        public String getModelUsed() {
            return modelUsed;
        }

        public void setModelUsed(String modelUsed) {
            this.modelUsed = modelUsed;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public Integer getReasoningTokens() {
            return reasoningTokens;
        }

        public void setReasoningTokens(Integer reasoningTokens) {
            this.reasoningTokens = reasoningTokens;
        }

        public Double getEstimatedCostUsd() {
            return estimatedCostUsd;
        }

        public void setEstimatedCostUsd(Double estimatedCostUsd) {
            this.estimatedCostUsd = estimatedCostUsd;
        }

        public Boolean getRetryable() {
            return retryable;
        }

        public void setRetryable(Boolean retryable) {
            this.retryable = retryable;
        }

        public Integer getFailedAttempts() {
            return failedAttempts;
        }

        public void setFailedAttempts(Integer failedAttempts) {
            this.failedAttempts = failedAttempts;
        }

        public String getLastError() {
            return lastError;
        }

        public void setLastError(String lastError) {
            this.lastError = lastError;
        }
    }
}
