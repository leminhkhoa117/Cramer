package com.cramer.abts.generation;

import com.cramer.abts.validation.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Outcome of generating one part (SPEC-21 §2, §4): either content + validation, or a failure code.
 * A failed outcome never aborts the other parts (partial success, SPEC-21 §4).
 */
record PartOutcome(int part, JsonNode content, ValidationResult validation,
                   String errorCode, boolean retryable, int attempts) {

    static PartOutcome success(int part, JsonNode content, ValidationResult validation, int attempts) {
        return new PartOutcome(part, content, validation, null, false, attempts);
    }

    static PartOutcome failure(int part, String errorCode, boolean retryable, int attempts) {
        return new PartOutcome(part, null, null, errorCode, retryable, attempts);
    }

    boolean failed() {
        return content == null;
    }
}
