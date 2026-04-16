package com.cramer.websocket;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Protocol abstraction for a live Gemini speaking connection.
 *
 * <p>This interface decouples the Spring WebSocket handler from the concrete
 * Gemini transport implementation so the runtime can:
 * <ul>
 *   <li>swap transport implementations more easily,</li>
 *   <li>unit test handler logic with fake connections, and</li>
 *   <li>keep real-time session orchestration focused on business flow.</li>
 * </ul>
 *
 * <p>Typical lifecycle:
 * <ol>
 *   <li>Create connection instance</li>
 *   <li>Call {@link #connect()}</li>
 *   <li>Wait until {@link #isReady()} becomes {@code true}</li>
 *   <li>Send examiner prompts and user audio</li>
 *   <li>Close via {@link #close()}</li>
 * </ol>
 */
public interface GeminiLiveConnection {

    /**
     * Opens the underlying live connection to Gemini.
     */
    void connect();

    /**
     * Closes the underlying live connection and releases any resources.
     */
    void close();

    /**
     * @return {@code true} when the underlying transport is currently open
     */
    boolean isOpen();

    /**
     * @return {@code true} when Gemini setup has completed and the connection
     *         can accept prompt/audio traffic safely
     */
    boolean isReady();

    /**
     * Sends the frozen speaking turn prompt derived from the session blueprint.
     *
     * @param questionSnapshot immutable runtime truth for the active turn
     */
    void sendQuestionPrompt(JsonNode questionSnapshot);

    /**
     * Streams one chunk of raw PCM audio from the user to Gemini.
     *
     * @param audioData raw 16-bit PCM audio bytes
     */
    void sendAudioChunk(byte[] audioData);

    /**
     * Signals that the current user turn has ended.
     */
    void sendEndOfTurn();
}
