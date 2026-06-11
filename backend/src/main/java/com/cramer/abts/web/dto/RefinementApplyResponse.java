package com.cramer.abts.web.dto;

import com.cramer.abts.validation.ValidationView;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Result of applying accepted hunks (SPEC-23 §5.2): the patched content, any skipped hunk ids
 * with reasons, and a fresh validation of the patched content.
 */
public record RefinementApplyResponse(
        JsonNode content,
        List<String> skipped,
        ValidationView validation) {
}
