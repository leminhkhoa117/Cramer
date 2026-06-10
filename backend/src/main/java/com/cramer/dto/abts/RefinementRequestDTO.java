package com.cramer.dto.abts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for the Refinement Agent (Agent 2)
 * Contains the original output and selected issues to fix.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefinementRequestDTO {

    /**
     * The original JSON output from Agent 1
     */
    private String originalJson;

    /**
     * List of issue identifiers selected by the user to fix.
     * Format: "Q{number}:{issue_type}" e.g., "Q2:WORD_LIMIT",
     * "Q8:MISSING_PLACEHOLDER"
     */
    private List<String> selectedIssueIds;

    /**
     * The original full prompt sent to Agent 1 (for context caching)
     */
    private String originalPrompt;

    /**
     * The skill type (READING, LISTENING, WRITING)
     */
    private String skill;

    /**
     * Part number (1, 2, or 3)
     */
    private Integer partNumber;

    /**
     * The validation result containing all detected issues
     */
    private ValidationResultDTO validationResult;

    /**
     * AI model to use for refinement (optional).
     * If null, defaults to google/gemini-3-flash-preview.
     * Example: "deepseek/deepseek-chat", "google/gemini-3-flash-preview"
     */
    private String model;

    /**
     * Enable context caching for cost reduction (defaults to true).
     * Uses OpenRouter's cache_control feature for supported models.
     */
    private Boolean enableCaching;

    /**
     * Enable reasoning/thinking tokens for refinement (defaults to false).
     * Set to true for models like DeepSeek R1 that benefit from chain-of-thought.
     */
    private Boolean enableReasoning;

    /**
     * Refinement loop iteration counter. Null/0 on first refinement. The service
     * increments this and echoes it on the response. Hard-capped to prevent
     * runaway loops (see OpenRouterConfig.maxRefinementRounds).
     */
    private Integer round;

    /**
     * Nested DTO for validation issues
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationIssue {
        private String id; // Unique identifier
        private String type; // ERROR, WARNING
        private String message; // Human-readable description
        private Integer questionNumber; // Affected question (if applicable)
        private String category; // WORD_LIMIT, PLACEHOLDER, OPTIONS, etc.
        private List<String> affectedPaths; // JSON Pointer paths this issue touches (optional)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResultDTO {
        private List<ValidationIssue> errors;
        private List<ValidationIssue> warnings;
    }
}
