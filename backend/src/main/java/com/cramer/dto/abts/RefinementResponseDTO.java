package com.cramer.dto.abts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO from the Refinement Agent (Agent 2)
 * Contains the refined JSON and a list of patches applied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefinementResponseDTO {

    /**
     * The complete refined JSON output
     */
    private String refinedJson;

    /**
     * List of patches/changes applied by Agent 2
     */
    private List<RefinementPatch> patches;

    /**
     * New validation result after refinement
     */
    private RefinementRequestDTO.ValidationResultDTO newValidation;

    /**
     * Whether refinement was successful
     */
    private boolean success;

    /**
     * Error message if refinement failed
     */
    private String errorMessage;

    /**
     * Individual patch representing a single fix
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefinementPatch {
        private String issueId; // Which issue this fixes
        private Integer questionNumber; // Affected question
        private String field; // JSON field modified (e.g., "correct_answer", "question_content.text")
        private String beforeValue; // Original value
        private String afterValue; // New value
        private String description; // Human-readable description of the change
    }
}
