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
