package com.cramer.abts.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Refinement request (SPEC-23 §5). The author selects {@code issueIds} to fix; the backend asks
 * the model for targeted patches, applies them, and streams back diff hunks. Empty selection is
 * rejected (400); {@code round} is enforced against {@code abts.max-refinement-rounds}.
 *
 * @param originalJson the content to refine
 * @param issueIds     selected validation issue ids (non-empty)
 * @param skill        reading / listening / writing
 * @param part         part/task number (null for whole-skill)
 * @param taskType     writing task type (writing only)
 * @param model        model + reasoning config
 * @param round        current refinement round (0-based)
 * @param validation   the current validation result (issue context)
 */
public record RefinementRequest(
        JsonNode originalJson,
        List<String> issueIds,
        String skill,
        Integer part,
        String taskType,
        ModelConfig model,
        Integer round,
        JsonNode validation) {

    public List<String> safeIssueIds() {
        return issueIds == null ? List.of() : issueIds;
    }

    public int safeRound() {
        return round == null ? 0 : round;
    }

    public ModelConfig safeModel() {
        return model == null ? new ModelConfig(null, null, null, null, null, null, null) : model;
    }
}
