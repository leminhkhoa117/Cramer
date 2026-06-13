package com.cramer.abts.domain;

import com.cramer.abts.validation.ValidationView;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * The outcome of a generation (SPEC-21 §8): merged content + validation + accounting. Returned
 * by the sync API and embedded as the {@code data} of a {@code COMPLETED} stream event.
 *
 * @param status     SUCCESS / PARTIAL_SUCCESS / FAILED / NOT_IMPLEMENTED
 * @param skill      reading / listening / writing
 * @param content    merged generated JSON (null when FAILED)
 * @param validation validation result view (null when not validated)
 * @param partErrors per-part error map (multi-part partial success); null/empty otherwise
 * @param reasoning  model reasoning text (null when none)
 * @param usage      token/cost accounting
 * @param model      model slug used
 * @param attempts   number of generation attempts spent
 * @param errorCode  normalized error code when FAILED (null otherwise)
 * @param retryable  whether a FAILED result is retryable
 */
public record GenerationResult(
        GenerationStatus status,
        String skill,
        JsonNode content,
        ValidationView validation,
        Map<Integer, String> partErrors,
        String reasoning,
        TokenUsage usage,
        String model,
        int attempts,
        String errorCode,
        Boolean retryable) {

    public static GenerationResult notImplemented(String skill) {
        return new GenerationResult(GenerationStatus.NOT_IMPLEMENTED, skill, null, null, null, null,
                TokenUsage.ZERO, null, 0, "NOT_IMPLEMENTED", false);
    }

    public static GenerationResult failed(String skill, String model, int attempts, String errorCode, boolean retryable) {
        return new GenerationResult(GenerationStatus.FAILED, skill, null, null, null, null,
                TokenUsage.ZERO, model, attempts, errorCode, retryable);
    }
}
