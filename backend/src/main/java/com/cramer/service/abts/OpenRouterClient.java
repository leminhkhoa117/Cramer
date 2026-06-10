package com.cramer.service.abts;

import com.cramer.config.OpenRouterConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Client for OpenRouter unified AI API.
 * Provides access to 400+ AI models (OpenAI, Anthropic, Google, DeepSeek, Meta,
 * etc.)
 * through a single API interface.
 * 
 * Features:
 * - JSON Schema mode for structured outputs
 * - Model fallbacks for reliability
 * - Reasoning tokens (Chain-of-Thought) support
 * - Streaming support (SSE)
 * 
 * @see <a href="https://openrouter.ai/docs">OpenRouter Documentation</a>
 * @since 2025-12-20 - ABTS v2.0
 */
@Service
public class OpenRouterClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenRouterClient.class);

    private static final String CHAT_ENDPOINT = "/chat/completions";

    private final OpenRouterConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenRouterClientSupport support;

    public OpenRouterClient(OpenRouterConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();

        // Configure RestTemplate with timeout
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofMillis(30000)); // 30 seconds to connect
        factory.setReadTimeout(java.time.Duration.ofMillis(config.getTimeoutMs())); // Configurable read timeout

        this.restTemplate = new RestTemplate(factory);
        this.support = new OpenRouterClientSupport(config, objectMapper, restTemplate);
    }

    /**
     * Call OpenRouter Chat Completions API with JSON Schema mode.
     * 
     * @param model           Primary model to use (e.g.,
     *                        "deepseek/deepseek-r1:thinking")
     * @param systemPrompt    System message content
     * @param userPrompt      User message content
     * @param jsonSchema      JSON Schema for structured output validation
     * @param fallbackModels  Optional fallback models if primary fails
     * @param reasoningConfig Reasoning configuration (effort, max_tokens)
     * @param temperature     Temperature setting (0.0-2.0)
     * @param maxTokens       Maximum tokens for completion
     * @return API response as JsonNode
     * @throws OpenRouterException if API call fails
     */
    public OpenRouterResponse callChatCompletion(
            String model,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> jsonSchema,
            List<String> fallbackModels,
            Map<String, Object> reasoningConfig,
            Double temperature,
            Integer maxTokens) {

        if (!config.hasApiKey()) {
            throw new OpenRouterException("OPENROUTER_API_KEY not configured", "AUTH_FAILED", false);
        }

        String url = config.getBaseUrl() + CHAT_ENDPOINT;

        HttpHeaders headers = support.buildHeaders();
        Map<String, Object> requestBody = support.buildRequestBody(
                model, systemPrompt, userPrompt, jsonSchema,
                fallbackModels, reasoningConfig, temperature, maxTokens);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            long startTime = System.currentTimeMillis();

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("OpenRouter API call completed in {}ms", duration);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new OpenRouterException(
                        "OpenRouter API returned status: " + response.getStatusCode(),
                        "API_ERROR",
                        true);
            }

            return support.parseResponse(response.getBody(), duration);

        } catch (HttpClientErrorException e) {
            return support.handleHttpError(e);
        } catch (Exception e) {
            logger.error("OpenRouter API call failed: {}", e.getMessage(), e);
            throw new OpenRouterException("Failed to call OpenRouter API: " + e.getMessage(), "UNKNOWN_ERROR", false);
        }
    }

    /**
     * Simplified call with default settings.
     */
    public OpenRouterResponse callChatCompletion(
            String model,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> jsonSchema) {

        return callChatCompletionWithFeatures(
                model, systemPrompt, userPrompt, jsonSchema,
                List.of("anthropic/claude-3.5-sonnet", "meta-llama/llama-3.1-70b-instruct:free"),
                Map.of("effort", "high"),
                1.0,
                8192,
                false, // enableWebSearch
                false); // enableContextCaching
    }

    /**
     * Full-featured call with web search and context caching support.
     *
     * @param model                Primary model to use
     * @param systemPrompt         System message content
     * @param userPrompt           User message content
     * @param jsonSchema           JSON Schema for structured output
     * @param fallbackModels       Optional fallback models
     * @param reasoningConfig      Reasoning configuration
     * @param temperature          Temperature (0.0-2.0)
     * @param maxTokens            Maximum output tokens
     * @param enableWebSearch      Enable OpenRouter web plugin for real-time search
     * @param enableContextCaching Enable prompt caching for faster responses
     * @return API response
     */
    public OpenRouterResponse callChatCompletionWithFeatures(
            String model,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> jsonSchema,
            List<String> fallbackModels,
            Map<String, Object> reasoningConfig,
            Double temperature,
            Integer maxTokens,
            boolean enableWebSearch,
            boolean enableContextCaching) {

        if (!config.hasApiKey()) {
            throw new OpenRouterException("OPENROUTER_API_KEY not configured", "AUTH_FAILED", false);
        }

        String url = config.getBaseUrl() + CHAT_ENDPOINT;

        HttpHeaders headers = support.buildHeaders();
        Map<String, Object> requestBody = support.buildRequestBodyWithFeatures(
                model, systemPrompt, userPrompt, jsonSchema,
                fallbackModels, reasoningConfig, temperature, maxTokens,
                enableWebSearch, enableContextCaching);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            long startTime = System.currentTimeMillis();

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("OpenRouter API call completed in {}ms (webSearch={}, caching={})",
                    duration, enableWebSearch, enableContextCaching);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new OpenRouterException(
                        "OpenRouter API returned status: " + response.getStatusCode(),
                        "API_ERROR",
                        true);
            }

            return support.parseResponse(response.getBody(), duration);

        } catch (HttpClientErrorException e) {
            return support.handleHttpError(e);
        } catch (Exception e) {
            logger.error("OpenRouter API call failed: {}", e.getMessage(), e);
            throw new OpenRouterException("Failed to call OpenRouter API: " + e.getMessage(), "UNKNOWN_ERROR", false);
        }
    }

    /**
     * Streaming callback interface for receiving SSE chunks.
     */
    public interface StreamCallback {
        void onReasoningChunk(String reasoningDelta);

        void onContentChunk(String contentDelta);

        void onProgress(int percent, String message);

        void onComplete(OpenRouterResponse response);

        void onError(String error);

        /**
         * FIX 5: typed error callback carrying a classified {@link OpenRouterException}
         * (error code + retryable flag). Defaults to delegating to {@link #onError(String)}
         * so existing implementations keep working.
         */
        default void onErrorTyped(OpenRouterException error) {
            onError(error.getMessage());
        }

        /**
         * FIX 5: invoked when generation is cancelled by the user (cancellation
         * flag flipped). Distinct from a real error so callers can exit quietly
         * without emitting a FAILED event. Defaults to surfacing a sentinel via
         * {@link #onError(String)} for implementations that do not override it.
         */
        default void onCancelled() {
            onError("CANCELLED");
        }
    }

    /**
     * FIX 5: classify an HTTP status + error body into a typed {@link OpenRouterException}.
     */
    private OpenRouterException classifyHttpError(int statusCode, String parsedMessage) {
        String message = "API error " + statusCode + ": " + parsedMessage;
        switch (statusCode) {
            case 429:
                return new OpenRouterException(message, "RATE_LIMITED", true);
            case 401:
                return new OpenRouterException(message, "AUTH_FAILED", false);
            case 402:
                return new OpenRouterException(message, "INSUFFICIENT_CREDITS", false);
            case 400:
                return new OpenRouterException(message, "INVALID_FORMAT", false);
            default:
                if (statusCode >= 500) {
                    return new OpenRouterException(message, "SERVER_ERROR", true);
                }
                return new OpenRouterException(message, "API_ERROR", false);
        }
    }

    /**
     * Call OpenRouter API with true SSE streaming.
     * Receives tokens as they're generated and forwards them via callback.
     */
    public void callChatCompletionStreaming(
            String model,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> jsonSchema,
            Map<String, Object> reasoningConfig,
            Double temperature,
            Integer maxTokens,
            StreamCallback callback,
            java.util.concurrent.atomic.AtomicBoolean cancelled) {

        if (!config.hasApiKey()) {
            callback.onError("OpenRouter API key not configured");
            return;
        }

        String url = config.getBaseUrl() + CHAT_ENDPOINT;

        java.net.HttpURLConnection conn = null;
        try {
            // Build streaming request body
            Map<String, Object> body = buildStreamingRequestBody(
                    model, systemPrompt, userPrompt, jsonSchema,
                    reasoningConfig, temperature, maxTokens);

            String requestJson = objectMapper.writeValueAsString(body);

            // Create HTTP connection
            java.net.URL apiUrl = java.net.URI.create(url).toURL();
            conn = (java.net.HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(config.getTimeoutMs());

            // Set headers
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
            conn.setRequestProperty("HTTP-Referer", config.getSiteUrl());
            conn.setRequestProperty("X-Title", config.getSiteName());
            conn.setRequestProperty("Accept", "text/event-stream");

            // Send request body
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(requestJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorBody = support.readStream(conn.getErrorStream());
                // FIX 5: emit a typed, classified error so callers can decide retryability.
                callback.onErrorTyped(classifyHttpError(responseCode, support.parseErrorMessage(errorBody)));
                return;
            }

            // Parse SSE stream
            long startTime = System.currentTimeMillis();
            StringBuilder reasoningBuilder = new StringBuilder();
            StringBuilder contentBuilder = new StringBuilder();
            String modelUsed = model;
            Integer promptTokens = null;
            Integer completionTokens = null;
            Integer reasoningTokens = null;

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

                String line;
                int chunkCount = 0;

                while ((line = reader.readLine()) != null) {
                    if (cancelled != null && cancelled.get()) {
                        // V7: proactively tear down the live HTTP connection on cancel (finally also disconnects; idempotent).
                        conn.disconnect();
                        throw new OpenRouterException("Generation cancelled by user", "CANCELLED", false);
                    }
                    if (line.isEmpty())
                        continue;
                    if (!line.startsWith("data: "))
                        continue;

                    String data = line.substring(6).trim();
                    if (data.equals("[DONE]")) {
                        break;
                    }

                    try {
                        JsonNode chunk = objectMapper.readTree(data);
                        chunkCount++;

                        // Extract model if present
                        if (chunk.has("model")) {
                            modelUsed = chunk.get("model").asText();
                        }

                        // Extract delta content
                        JsonNode choices = chunk.path("choices");
                        if (choices.isArray() && choices.size() > 0) {
                            JsonNode delta = choices.get(0).path("delta");

                            // Reasoning tokens
                            if (delta.hasNonNull("reasoning")) {
                                String reasoningDelta = delta.get("reasoning").asText();
                                if (reasoningDelta != null && !reasoningDelta.equals("null")) {
                                    reasoningBuilder.append(reasoningDelta);
                                    callback.onReasoningChunk(reasoningDelta);
                                }
                            }

                            // Content tokens
                            if (delta.hasNonNull("content")) {
                                String contentDelta = delta.get("content").asText();
                                if (contentDelta != null && !contentDelta.equals("null")) {
                                    contentBuilder.append(contentDelta);
                                    callback.onContentChunk(contentDelta);
                                }
                            }
                        }

                        // Extract usage from final chunk
                        if (chunk.has("usage")) {
                            JsonNode usage = chunk.get("usage");
                            if (usage.has("prompt_tokens"))
                                promptTokens = usage.get("prompt_tokens").asInt();
                            if (usage.has("completion_tokens"))
                                completionTokens = usage.get("completion_tokens").asInt();
                            if (usage.has("reasoning_tokens"))
                                reasoningTokens = usage.get("reasoning_tokens").asInt();
                        }

                        // Send progress updates sparingly (every 100 chunks) to avoid flooding UI
                        if (chunkCount % 100 == 0) {
                            int percent = Math.min(75, 25 + (chunkCount / 20));
                            callback.onProgress(percent, "AI is generating content...");
                        }

                    } catch (Exception e) {
                        logger.warn("Failed to parse SSE chunk: {}", e.getMessage());
                    }
                }
            }

            // Build final response
            long duration = System.currentTimeMillis() - startTime;
            OpenRouterResponse response = new OpenRouterResponse();
            response.setContent(contentBuilder.toString());
            response.setReasoning(reasoningBuilder.toString());
            response.setModelUsed(modelUsed);
            response.setDurationMs(duration);
            response.setPromptTokens(promptTokens);
            response.setCompletionTokens(completionTokens);
            response.setReasoningTokens(reasoningTokens);

            callback.onComplete(response);

        } catch (OpenRouterException e) {
            // FIX 5: distinguish user cancellation from genuine API failures.
            if ("CANCELLED".equals(e.getErrorCode())) {
                logger.info("Streaming cancelled by user");
                callback.onCancelled();
            } else {
                logger.error("Streaming API call failed [{}]: {}", e.getErrorCode(), e.getMessage());
                callback.onErrorTyped(e);
            }
        } catch (Exception e) {
            logger.error("Streaming API call failed: {}", e.getMessage(), e);
            callback.onError("Streaming failed: " + e.getMessage());
        } finally {
            // FIX 6: always release the underlying socket.
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Call OpenRouter API with streaming and explicit caching control.
     * For Gemini models, adds cache_control markers to reduce costs.
     */
    public void callChatCompletionStreaming(
            String model,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> jsonSchema,
            Map<String, Object> reasoningConfig,
            Double temperature,
            Integer maxTokens,
            boolean enableCaching,
            StreamCallback callback,
            java.util.concurrent.atomic.AtomicBoolean cancelled) {

        if (!config.hasApiKey()) {
            callback.onError("OpenRouter API key not configured");
            return;
        }

        String url = config.getBaseUrl() + CHAT_ENDPOINT;

        java.net.HttpURLConnection conn = null;
        try {
            // Build streaming request body with caching support
            Map<String, Object> body = buildStreamingRequestBodyWithCaching(
                    model, systemPrompt, userPrompt, jsonSchema,
                    reasoningConfig, temperature, maxTokens, enableCaching);

            String requestJson = objectMapper.writeValueAsString(body);

            // Create HTTP connection
            java.net.URL apiUrl = java.net.URI.create(url).toURL();
            conn = (java.net.HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(config.getTimeoutMs());

            // Set headers
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
            conn.setRequestProperty("HTTP-Referer", config.getSiteUrl());
            conn.setRequestProperty("X-Title", config.getSiteName());
            conn.setRequestProperty("Accept", "text/event-stream");

            // Send request body
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(requestJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorBody = support.readStream(conn.getErrorStream());
                // FIX 5: emit a typed, classified error so callers can decide retryability.
                callback.onErrorTyped(classifyHttpError(responseCode, support.parseErrorMessage(errorBody)));
                return;
            }

            // Parse SSE stream (same logic as non-cached version)
            long startTime = System.currentTimeMillis();
            StringBuilder reasoningBuilder = new StringBuilder();
            StringBuilder contentBuilder = new StringBuilder();
            String modelUsed = model;
            Integer promptTokens = null;
            Integer completionTokens = null;
            Integer reasoningTokens = null;

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

                String line;
                int chunkCount = 0;

                while ((line = reader.readLine()) != null) {
                    if (cancelled != null && cancelled.get()) {
                        // V7: proactively tear down the live HTTP connection on cancel (finally also disconnects; idempotent).
                        conn.disconnect();
                        throw new OpenRouterException("Generation cancelled by user", "CANCELLED", false);
                    }
                    if (line.isEmpty())
                        continue;
                    if (!line.startsWith("data: "))
                        continue;

                    String data = line.substring(6).trim();
                    if (data.equals("[DONE]")) {
                        break;
                    }

                    try {
                        JsonNode chunk = objectMapper.readTree(data);
                        chunkCount++;

                        if (chunk.has("model")) {
                            modelUsed = chunk.get("model").asText();
                        }

                        JsonNode choices = chunk.path("choices");
                        if (choices.isArray() && choices.size() > 0) {
                            JsonNode delta = choices.get(0).path("delta");

                            if (delta.hasNonNull("reasoning")) {
                                String reasoningDelta = delta.get("reasoning").asText();
                                if (reasoningDelta != null && !reasoningDelta.equals("null")) {
                                    reasoningBuilder.append(reasoningDelta);
                                    callback.onReasoningChunk(reasoningDelta);
                                }
                            }

                            if (delta.hasNonNull("content")) {
                                String contentDelta = delta.get("content").asText();
                                if (contentDelta != null && !contentDelta.equals("null")) {
                                    contentBuilder.append(contentDelta);
                                    callback.onContentChunk(contentDelta);
                                }
                            }
                        }

                        if (chunk.has("usage")) {
                            JsonNode usage = chunk.get("usage");
                            if (usage.has("prompt_tokens"))
                                promptTokens = usage.get("prompt_tokens").asInt();
                            if (usage.has("completion_tokens"))
                                completionTokens = usage.get("completion_tokens").asInt();
                            if (usage.has("reasoning_tokens"))
                                reasoningTokens = usage.get("reasoning_tokens").asInt();
                        }

                        if (chunkCount % 100 == 0) {
                            int percent = Math.min(75, 25 + (chunkCount / 20));
                            callback.onProgress(percent, "AI is generating content...");
                        }

                    } catch (Exception e) {
                        logger.warn("Failed to parse SSE chunk: {}", e.getMessage());
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            OpenRouterResponse response = new OpenRouterResponse();
            response.setContent(contentBuilder.toString());
            response.setReasoning(reasoningBuilder.toString());
            response.setModelUsed(modelUsed);
            response.setDurationMs(duration);
            response.setPromptTokens(promptTokens);
            response.setCompletionTokens(completionTokens);
            response.setReasoningTokens(reasoningTokens);

            if (enableCaching) {
                logger.info("Streaming completed with caching enabled (model: {})", model);
            }

            callback.onComplete(response);

        } catch (OpenRouterException e) {
            // FIX 5: distinguish user cancellation from genuine API failures.
            if ("CANCELLED".equals(e.getErrorCode())) {
                logger.info("Streaming cancelled by user");
                callback.onCancelled();
            } else {
                logger.error("Streaming API call failed [{}]: {}", e.getErrorCode(), e.getMessage());
                callback.onErrorTyped(e);
            }
        } catch (Exception e) {
            logger.error("Streaming API call failed: {}", e.getMessage(), e);
            callback.onError("Streaming failed: " + e.getMessage());
        } finally {
            // FIX 6: always release the underlying socket.
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 
     * Build request body for streaming API call.
     */
    private Map<String, Object> buildStreamingRequestBody(
            String model,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> jsonSchema,
            Map<String, Object> reasoningConfig,
            Double temperature,
            Integer maxTokens) {

        Map<String, Object> body = new HashMap<>();

        body.put("model", model);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);

        body.put("temperature", temperature != null ? temperature : 1.0);
        body.put("max_tokens", maxTokens != null ? maxTokens : 8192);
        body.put("stream", true); // Enable streaming

        // JSON Schema mode (NOTE: some models may not support streaming + JSON schema
        // together)
        if (jsonSchema != null && !jsonSchema.isEmpty()) {
            body.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", "ielts_content_response",
                            "strict", true,
                            "schema", jsonSchema)));
        }

        body.put("provider", Map.of(
                "allow_fallbacks", true,
                "data_collection", "allow"));

        OpenRouterClientSupport.applyReasoning(body, reasoningConfig);

        return body;
    }

    /**
     * Build request body for streaming API call with optional caching support.
     * For Gemini/Anthropic models, adds cache_control markers to system message.
     */
    private Map<String, Object> buildStreamingRequestBodyWithCaching(
            String model,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> jsonSchema,
            Map<String, Object> reasoningConfig,
            Double temperature,
            Integer maxTokens,
            boolean enableCaching) {

        Map<String, Object> body = new HashMap<>();

        body.put("model", model);

        // Build messages with optional cache_control for Gemini/Anthropic
        List<Map<String, Object>> messages = new ArrayList<>();

        if (enableCaching && (model.contains("gemini") || model.contains("claude"))) {
            // Use cache_control for Gemini/Anthropic models per OpenRouter docs
            // The system message is cached so repeated calls with same context are cheaper
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", List.of(Map.of(
                    "type", "text",
                    "text", systemPrompt,
                    "cache_control", Map.of("type", "ephemeral"))));
            messages.add(systemMessage);
            logger.debug("Added cache_control to system message for model: {}", model);
        } else {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);

        body.put("temperature", temperature != null ? temperature : 1.0);
        body.put("max_tokens", maxTokens != null ? maxTokens : 8192);
        body.put("stream", true);

        // JSON Schema mode
        if (jsonSchema != null && !jsonSchema.isEmpty()) {
            body.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", "ielts_content_response",
                            "strict", true,
                            "schema", jsonSchema)));
        }

        body.put("provider", Map.of(
                "allow_fallbacks", true,
                "data_collection", "allow"));

        OpenRouterClientSupport.applyReasoning(body, reasoningConfig);

        return body;
    }

    // ==================== MULTIMODAL AUDIO SUPPORT ====================

    /**
     * Record for audio input data.
     */
    public record AudioInput(String base64Data, String format) {}

    /**
     * Sealed interface for content parts (text or audio).
     */
    public sealed interface ContentPart permits TextPart, AudioPart {}

    /**
     * Text content part.
     */
    public record TextPart(String text) implements ContentPart {}

    /**
     * Audio content part with base64 encoded data.
     */
    public record AudioPart(AudioInput audio) implements ContentPart {}

    /**
     * Call OpenRouter with multimodal content (text + audio).
     * Used for IELTS Speaking grading with Gemini models.
     *
     * @param systemPrompt  System message for the AI
     * @param contentParts  List of text and audio content parts
     * @param model         Model to use (e.g., "google/gemini-2.5-flash")
     * @param jsonSchema    JSON Schema for structured output validation
     * @param schemaName    Name for the JSON schema
     * @return OpenRouterResponse with parsed content
     */
    public OpenRouterResponse callWithAudio(
            String systemPrompt,
            List<ContentPart> contentParts,
            String model,
            Map<String, Object> jsonSchema,
            String schemaName) {

        return callWithAudio(systemPrompt, contentParts, model, jsonSchema, schemaName, 4096, "allow");
    }

    /**
     * Call OpenRouter with multimodal content (text + audio) with configurable
     * max tokens and data collection settings.
     * Used for IELTS Speaking grading with Gemini models.
     *
     * @param systemPrompt   System message for the AI
     * @param contentParts   List of text and audio content parts
     * @param model          Model to use (e.g., "google/gemini-2.5-flash")
     * @param jsonSchema     JSON Schema for structured output validation
     * @param schemaName     Name for the JSON schema
     * @param maxTokens      Maximum tokens for the completion
     * @param dataCollection Data collection preference ("allow" or "deny")
     * @return OpenRouterResponse with parsed content
     */
    public OpenRouterResponse callWithAudio(
            String systemPrompt,
            List<ContentPart> contentParts,
            String model,
            Map<String, Object> jsonSchema,
            String schemaName,
            int maxTokens,
            String dataCollection) {

        if (!config.hasApiKey()) {
            throw new OpenRouterException("OPENROUTER_API_KEY not configured", "AUTH_FAILED", false);
        }

        String url = config.getBaseUrl() + CHAT_ENDPOINT;

        // Build content array from parts
        List<Map<String, Object>> content = new ArrayList<>();
        for (ContentPart part : contentParts) {
            if (part instanceof TextPart textPart) {
                content.add(Map.of(
                    "type", "text",
                    "text", textPart.text()
                ));
            } else if (part instanceof AudioPart audioPart) {
                content.add(Map.of(
                    "type", "input_audio",
                    "input_audio", Map.of(
                        "data", audioPart.audio().base64Data(),
                        "format", audioPart.audio().format()
                    )
                ));
            }
        }

        // Build messages array
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", content));

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model != null ? model : config.getGenerationModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("stream", false);

        // Add JSON schema if provided
        if (jsonSchema != null && !jsonSchema.isEmpty()) {
            requestBody.put("response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                    "name", schemaName != null ? schemaName : "response",
                    "strict", true,
                    "schema", jsonSchema
                )
            ));
        }

        // Provider settings
        requestBody.put("provider", Map.of(
            "allow_fallbacks", true,
            "data_collection", dataCollection
        ));

        HttpHeaders headers = support.buildHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            long startTime = System.currentTimeMillis();

            logger.info("Calling OpenRouter with {} content parts (model: {})",
                contentParts.size(), model);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("OpenRouter multimodal call completed in {}ms", duration);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new OpenRouterException(
                    "OpenRouter API returned status: " + response.getStatusCode(),
                    "API_ERROR",
                    true);
            }

            return support.parseResponse(response.getBody(), duration);

        } catch (HttpClientErrorException e) {
            return support.handleHttpError(e);
        } catch (Exception e) {
            logger.error("OpenRouter multimodal call failed: {}", e.getMessage(), e);
            throw new OpenRouterException(
                "Failed to call OpenRouter API: " + e.getMessage(),
                "UNKNOWN_ERROR",
                false);
        }
    }

    // ==================== RESPONSE DTO ====================

    /**
     * OpenRouter API response wrapper.
     */
    public static class OpenRouterResponse {
        private String content;
        private String reasoning;
        private String modelUsed;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer reasoningTokens;
        private Long durationMs;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getReasoning() {
            return reasoning;
        }

        public void setReasoning(String reasoning) {
            this.reasoning = reasoning;
        }

        public String getModelUsed() {
            return modelUsed;
        }

        public void setModelUsed(String modelUsed) {
            this.modelUsed = modelUsed;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public Integer getReasoningTokens() {
            return reasoningTokens;
        }

        public void setReasoningTokens(Integer reasoningTokens) {
            this.reasoningTokens = reasoningTokens;
        }

        public Long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(Long durationMs) {
            this.durationMs = durationMs;
        }

        public int getTotalTokens() {
            int total = 0;
            if (promptTokens != null)
                total += promptTokens;
            if (completionTokens != null)
                total += completionTokens;
            if (reasoningTokens != null)
                total += reasoningTokens;
            return total;
        }
    }

    // ==================== EXCEPTION ====================

    /**
     * OpenRouter API exception.
     */
    public static class OpenRouterException extends RuntimeException {
        private final String errorCode;
        private final boolean retryable;

        public OpenRouterException(String message, String errorCode, boolean retryable) {
            super(message);
            this.errorCode = errorCode;
            this.retryable = retryable;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }

    // ==================== MODELS API ====================

    /**
     * Fetch available models from OpenRouter API.
     * Returns comprehensive model information including pricing, context length,
     * and supported parameters.
     * 
     * @return List of model objects with id, name, pricing, context_length,
     *         supported_parameters
     */
    public List<Map<String, Object>> fetchAvailableModels() {
        return support.fetchAvailableModels();
    }
}
