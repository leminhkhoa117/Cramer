package com.cramer.abts.web.dto;

import com.cramer.abts.domain.Hunk;
import com.cramer.abts.validation.ValidationView;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Apply accepted refinement hunks (SPEC-23 §5.2). Per-hunk failures are skipped (not fatal) and
 * reported in the response.
 *
 * @param originalJson  content to patch
 * @param acceptedHunks hunks the author accepted
 * @param skill         reading / listening / writing
 * @param part          part/task number (for re-validation)
 * @param taskType      writing task type (writing only)
 */
public record RefinementApplyRequest(
        JsonNode originalJson,
        List<Hunk> acceptedHunks,
        String skill,
        Integer part,
        String taskType) {

    public List<Hunk> safeHunks() {
        return acceptedHunks == null ? List.of() : acceptedHunks;
    }
}
