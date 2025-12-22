package com.cramer.dto.abts;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for streaming generation events via SSE.
 * 
 * @since 2025-12-20 - ABTS v2.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamEventDTO {

    public enum EventType {
        STARTED, // Generation started
        PROMPT_BUILT, // Prompts constructed
        AI_CALLING, // Calling AI API
        AI_THINKING, // Reasoning tokens from AI (real-time streaming)
        AI_CHUNK, // Partial AI response (for streaming)
        AI_COMPLETED, // AI response received
        VALIDATING, // Validating content
        VALIDATION_RESULT, // Validation complete
        RETRY, // Retrying due to validation error
        COMPLETED, // Generation successful
        FAILED, // Generation failed
        PROGRESS // General progress update
    }

    private EventType type;
    private String message;
    private Integer progress; // 0-100
    private Integer attempt; // Current attempt number
    private Integer maxAttempts;
    private Object data; // Additional data (e.g., partial content, errors)
    private Long timestamp;

    public StreamEventDTO() {
        this.timestamp = System.currentTimeMillis();
    }

    public static StreamEventDTO started() {
        StreamEventDTO event = new StreamEventDTO();
        event.setType(EventType.STARTED);
        event.setMessage("Generation started");
        event.setProgress(0);
        return event;
    }

    public static StreamEventDTO promptBuilt() {
        StreamEventDTO event = new StreamEventDTO();
        event.setType(EventType.PROMPT_BUILT);
        event.setMessage("Prompts constructed");
        event.setProgress(10);
        return event;
    }

    public static StreamEventDTO aiCalling(int attempt, int maxAttempts) {
        StreamEventDTO event = new StreamEventDTO();
        event.setType(EventType.AI_CALLING);
        event.setMessage("Calling AI model...");
        event.setProgress(20);
        event.setAttempt(attempt);
        event.setMaxAttempts(maxAttempts);
        return event;
    }

    /**
     * Real-time reasoning/thinking token from AI.
     * Shows AI's chain-of-thought process.
     */
    public static StreamEventDTO aiThinking(String reasoningDelta) {
        StreamEventDTO event = new StreamEventDTO();
        event.setType(EventType.AI_THINKING);
        event.setMessage(reasoningDelta);
        return event;
    }

    /**
     * Content chunk from AI (JSON data).
     */
    public static StreamEventDTO aiChunk(String chunk) {
        StreamEventDTO event = new StreamEventDTO();
        event.setType(EventType.AI_CHUNK);
        event.setData(chunk);
        return event;
    }

    public static StreamEventDTO aiCompleted(long durationMs) {
        StreamEventDTO event = new StreamEventDTO();
        event.setType(EventType.AI_COMPLETED);
        event.setMessage("AI response received in " + (durationMs / 1000.0) + "s");
        event.setProgress(60);
        return event;
    }

    public static StreamEventDTO validating() {
        StreamEventDTO event = new StreamEventDTO();
        event.setType(EventType.VALIDATING);
        event.setMessage("Validating content...");
        event.setProgress(70);
        return event;
    }

    public static StreamEventDTO validationResult(boolean valid, Object errors) {
        StreamEventDTO event = new StreamEventDTO();
        event.setType(EventType.VALIDATION_RESULT);
        event.setMessage(valid ? "Validation passed" : "Validation failed");
        event.setProgress(valid ? 90 : 70);
        event.setData(errors);
        return event;
    }

    public static StreamEventDTO retry(int attempt, int maxAttempts, String reason) {
        StreamEventDTO event = new StreamEventDTO();
        event.setType(EventType.RETRY);
        event.setMessage("Retrying: " + reason);
        event.setProgress(15);
        event.setAttempt(attempt);
        event.setMaxAttempts(maxAttempts);
        return event;
    }

    public static StreamEventDTO completed(Object result) {
        StreamEventDTO event = new StreamEventDTO();
        event.setType(EventType.COMPLETED);
        event.setMessage("Generation completed successfully");
        event.setProgress(100);
        event.setData(result);
        return event;
    }

    public static StreamEventDTO failed(String error) {
        StreamEventDTO event = new StreamEventDTO();
        event.setType(EventType.FAILED);
        event.setMessage(error);
        event.setProgress(0);
        return event;
    }

    public static StreamEventDTO progress(int progress, String message) {
        StreamEventDTO event = new StreamEventDTO();
        event.setType(EventType.PROGRESS);
        event.setMessage(message);
        event.setProgress(progress);
        return event;
    }

    // Getters and Setters
    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
