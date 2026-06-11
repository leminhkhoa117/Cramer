package com.cramer.abts.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * A single SSE payload (SPEC-21 §5). All ABTS stream events share this shape; nullable fields
 * are omitted by Jackson. Factory methods cover the event catalog.
 *
 * @param type       authoritative event type
 * @param message    human-readable note
 * @param progress   0–100 progress (phase/part)
 * @param attempt    current attempt (RETRY)
 * @param maxAttempts max attempts (RETRY)
 * @param data       delta text or a result/hunks node
 * @param partNumber active part (multi-part)
 * @param totalParts total parts (multi-part)
 * @param errorCode  normalized error code (FAILED)
 * @param timestamp  server time (epoch millis)
 */
public record StreamEvent(
        String type,
        String message,
        Integer progress,
        Integer attempt,
        Integer maxAttempts,
        JsonNode data,
        Integer partNumber,
        Integer totalParts,
        String errorCode,
        long timestamp) {

    private static StreamEvent base(StreamEventType type, String message) {
        return new StreamEvent(type.name(), message, null, null, null, null, null, null, null, now());
    }

    public static StreamEvent started(String message) {
        return base(StreamEventType.STARTED, message);
    }

    public static StreamEvent promptBuilt(String message, Integer partNumber) {
        return new StreamEvent(StreamEventType.PROMPT_BUILT.name(), message, null, null, null, null,
                partNumber, null, null, now());
    }

    public static StreamEvent aiThinking(String delta, Integer partNumber) {
        return new StreamEvent(StreamEventType.AI_THINKING.name(), null, null, null, null,
                text(delta), partNumber, null, null, now());
    }

    public static StreamEvent aiChunk(String delta, Integer partNumber) {
        return new StreamEvent(StreamEventType.AI_CHUNK.name(), null, null, null, null,
                text(delta), partNumber, null, null, now());
    }

    public static StreamEvent progress(int progress, Integer partNumber, Integer totalParts, String message) {
        return new StreamEvent(StreamEventType.PROGRESS.name(), message, progress, null, null, null,
                partNumber, totalParts, null, now());
    }

    public static StreamEvent retry(int attempt, int maxAttempts, String message) {
        return new StreamEvent(StreamEventType.RETRY.name(), message, null, attempt, maxAttempts, null,
                null, null, null, now());
    }

    public static StreamEvent completed(JsonNode data) {
        return new StreamEvent(StreamEventType.COMPLETED.name(), null, 100, null, null, data,
                null, null, null, now());
    }

    public static StreamEvent failed(String message, String errorCode) {
        return new StreamEvent(StreamEventType.FAILED.name(), message, null, null, null, null,
                null, null, errorCode, now());
    }

    public static StreamEvent aborted(String message) {
        return base(StreamEventType.ABORTED, message);
    }

    public static StreamEvent refinementCompleted(JsonNode hunks) {
        return new StreamEvent(StreamEventType.REFINEMENT_COMPLETED.name(), null, 100, null, null, hunks,
                null, null, null, now());
    }

    private static JsonNode text(String value) {
        return com.cramer.platform.common.json.Json.mapper().getNodeFactory().textNode(value == null ? "" : value);
    }

    private static long now() {
        return Instant.now().toEpochMilli();
    }
}
