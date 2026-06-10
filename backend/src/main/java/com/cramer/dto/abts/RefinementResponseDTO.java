package com.cramer.dto.abts;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO from the Refinement Agent (Agent 2)
 * Contains the refined JSON and the structured diff hunks for per-hunk review.
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
     * Structured JSON-Patch style hunks diffing originalJson vs refinedJson.
     * Each hunk is independently applyable/rejectable by the user (per-hunk
     * approval). Populated by {@code RefinementHunkBuilder}.
     */
    private List<RefinementHunk> hunks;

    /**
     * Refinement loop iteration counter. Incremented by the caller per loop
     * iteration (0 = first refinement has not run; 1 = after first refinement).
     */
    private int round;

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
     * Structured diff hunk using RFC 6902 (JSON Patch) vocabulary.
     * Represents a single, independently-applyable change between the original
     * and refined JSON.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefinementHunk {
        /** Stable id: "hunk_" + sha1(path + "|" + before + "|" + after).substring(0,12) */
        private String id;
        /** RFC 6902 operation: one of add, remove, replace */
        private String op;
        /** JSON Pointer to the affected value, e.g. /questions/3/correct_answer/0 */
        private String path;
        /** Current value at path; null for add */
        private JsonNode before;
        /** New value; null for remove */
        private JsonNode after;
        /** Which validation issues this hunk addresses (may be empty) */
        private List<String> issueIds;
        /** 1-line human-readable summary, e.g. "/questions/3/correct_answer/0: A -> B" */
        private String summary;
        /** Severity carried from the issue: error, warning, info */
        private String severity;
    }
}
