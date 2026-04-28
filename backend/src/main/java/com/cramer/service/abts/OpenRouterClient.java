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

    public OpenRouterClient(OpenRouterConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();

        // Configure RestTemplate with timeout
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 seconds to connect
        factory.setReadTimeout(config.getTimeoutMs()); // Configurable read timeout

        this.restTemplate = new RestTemplate(factory);
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

        HttpHeaders headers = buildHeaders();
        Map<String, Object> requestBody = buildRequestBody(
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

            return parseResponse(response.getBody(), duration);

        } catch (HttpClientErrorException e) {
            return handleHttpError(e);
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

        HttpHeaders headers = buildHeaders();
        Map<String, Object> requestBody = buildRequestBodyWithFeatures(
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

            return parseResponse(response.getBody(), duration);

        } catch (HttpClientErrorException e) {
            return handleHttpError(e);
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

        try {
            // Build streaming request body
            Map<String, Object> body = buildStreamingRequestBody(
                    model, systemPrompt, userPrompt, jsonSchema,
                    reasoningConfig, temperature, maxTokens);

            String requestJson = objectMapper.writeValueAsString(body);

            // Create HTTP connection
            java.net.URL apiUrl = java.net.URI.create(url).toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) apiUrl.openConnection();
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
                String errorBody = readStream(conn.getErrorStream());
                callback.onError("API error " + responseCode + ": " + parseErrorMessage(errorBody));
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

        } catch (Exception e) {
            logger.error("Streaming API call failed: {}", e.getMessage(), e);
            callback.onError("Streaming failed: " + e.getMessage());
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

        try {
            // Build streaming request body with caching support
            Map<String, Object> body = buildStreamingRequestBodyWithCaching(
                    model, systemPrompt, userPrompt, jsonSchema,
                    reasoningConfig, temperature, maxTokens, enableCaching);

            String requestJson = objectMapper.writeValueAsString(body);

            // Create HTTP connection
            java.net.URL apiUrl = java.net.URI.create(url).toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) apiUrl.openConnection();
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
                String errorBody = readStream(conn.getErrorStream());
                callback.onError("API error " + responseCode + ": " + parseErrorMessage(errorBody));
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

        } catch (Exception e) {
            logger.error("Streaming API call failed: {}", e.getMessage(), e);
            callback.onError("Streaming failed: " + e.getMessage());
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

        if (reasoningConfig != null && !reasoningConfig.isEmpty()) {
            body.put("reasoning", reasoningConfig);
        }

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

        if (reasoningConfig != null && !reasoningConfig.isEmpty()) {
            body.put("reasoning", reasoningConfig);
        }

        return body;
    }

    /**
     * Read input stream to string.
     */
    private String readStream(java.io.InputStream stream) {
        if (stream == null)
            return "";
        try (java.util.Scanner scanner = new java.util.Scanner(stream, "UTF-8").useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    /**
     * Build HTTP headers for OpenRouter API.
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Objects.requireNonNull(config.getApiKey()));
        headers.set("HTTP-Referer", config.getSiteUrl());
        headers.set("X-Title", config.getSiteName());
        return headers;
    }

    /**
     * Build request body for OpenRouter API.
     */
    private Map<String, Object> buildRequestBody(
            String model,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> jsonSchema,
            List<String> fallbackModels,
            Map<String, Object> reasoningConfig,
            Double temperature,
            Integer maxTokens) {

        Map<String, Object> body = new HashMap<>();

        // Model selection
        body.put("model", model);

        // Messages array
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);

        // Generation parameters
        body.put("temperature", temperature != null ? temperature : 1.0);
        body.put("max_tokens", maxTokens != null ? maxTokens : 8192);
        body.put("stream", false);

        // JSON Schema mode for structured output
        // Per OpenRouter docs: Most models support structured outputs including many
        // free ones
        // If model doesn't support it, OpenRouter will gracefully fallback
        if (jsonSchema != null && !jsonSchema.isEmpty()) {
            body.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", "ielts_content_response",
                            "strict", true,
                            "schema", jsonSchema)));
            logger.debug("Using JSON schema mode for model: {}", model);
        } else {
            // Fallback to basic JSON mode when no schema provided
            body.put("response_format", Map.of("type", "json_object"));
            logger.debug("Using basic JSON object mode for model: {}", model);
        }

        // Model fallbacks for reliability
        if (fallbackModels != null && !fallbackModels.isEmpty()) {
            List<String> allModels = new ArrayList<>();
            allModels.add(model);
            allModels.addAll(fallbackModels);
            body.put("models", allModels);
            body.put("route", "fallback");
        }

        // Provider preferences
        // Note: "data_collection" set to "allow" to support free/community models
        // that require training data. For privacy-sensitive use cases, set to "deny"
        body.put("provider", Map.of(
                "allow_fallbacks", true,
                "data_collection", "allow"));

        // Reasoning tokens (for thinking models)
        if (reasoningConfig != null && !reasoningConfig.isEmpty()) {
            body.put("reasoning", reasoningConfig);
        }

        return body;
    }

    /**
     * Build request body with web search and context caching support.
     */
    private Map<String, Object> buildRequestBodyWithFeatures(
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

        Map<String, Object> body = new HashMap<>();

        // Model selection - append :online suffix for web search if enabled
        String effectiveModel = enableWebSearch ? model + ":online" : model;
        body.put("model", effectiveModel);

        // Messages array
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);

        // Generation parameters
        body.put("temperature", temperature != null ? temperature : 1.0);
        body.put("max_tokens", maxTokens != null ? maxTokens : 8192);
        body.put("stream", false);

        // Context caching for faster repeated prompts
        if (enableContextCaching) {
            body.put("cache_prompt", true);
            logger.debug("Context caching enabled for model: {}", effectiveModel);
        }

        // JSON Schema mode for structured output
        if (jsonSchema != null && !jsonSchema.isEmpty()) {
            body.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", "ielts_content_response",
                            "strict", true,
                            "schema", jsonSchema)));
            logger.debug("Using JSON schema mode for model: {}", effectiveModel);
        } else {
            body.put("response_format", Map.of("type", "json_object"));
            logger.debug("Using basic JSON object mode for model: {}", effectiveModel);
        }

        // Web search plugin configuration (when not using :online suffix)
        // Note: Using :online suffix is simpler and recommended
        if (enableWebSearch) {
            logger.info("Web search enabled for AI fact research on model: {}", effectiveModel);
            // The :online suffix handles this automatically
            // But we can also explicitly add plugins if needed:
            // body.put("plugins", List.of(Map.of("id", "web", "max_results", 5)));
        }

        // Model fallbacks for reliability
        if (fallbackModels != null && !fallbackModels.isEmpty()) {
            List<String> allModels = new ArrayList<>();
            allModels.add(effectiveModel);
            allModels.addAll(fallbackModels);
            body.put("models", allModels);
            body.put("route", "fallback");
        }

        // Provider preferences
        body.put("provider", Map.of(
                "allow_fallbacks", true,
                "data_collection", "allow"));

        // Reasoning tokens (for thinking models)
        if (reasoningConfig != null && !reasoningConfig.isEmpty()) {
            body.put("reasoning", reasoningConfig);
        }

        return body;
    }

    /**
     * Parse OpenRouter API response.
     */
    private OpenRouterResponse parseResponse(String responseBody, long durationMs) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            OpenRouterResponse response = new OpenRouterResponse();
            response.setDurationMs(durationMs);

            // Extract model actually used (may differ if fallback was triggered)
            if (root.has("model")) {
                response.setModelUsed(root.get("model").asText());
            }

            // Extract content from choices
            JsonNode choices = root.path("choices");
            if (!choices.isMissingNode() && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.path("message");

                if (!message.isMissingNode()) {
                    // Main content
                    if (message.has("content")) {
                        response.setContent(message.get("content").asText());
                    }

                    // Reasoning content (Chain-of-Thought)
                    if (message.has("reasoning")) {
                        response.setReasoning(message.get("reasoning").asText());
                    }
                }
            }

            // Extract usage information
            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode()) {
                if (usage.has("prompt_tokens")) {
                    response.setPromptTokens(usage.get("prompt_tokens").asInt());
                }
                if (usage.has("completion_tokens")) {
                    response.setCompletionTokens(usage.get("completion_tokens").asInt());
                }
                if (usage.has("reasoning_tokens")) {
                    response.setReasoningTokens(usage.get("reasoning_tokens").asInt());
                }
            }

            return response;

        } catch (Exception e) {
            logger.error("Failed to parse OpenRouter response: {}", e.getMessage());
            throw new OpenRouterException("Failed to parse response: " + e.getMessage(), "PARSE_ERROR", false);
        }
    }

    /**
     * Handle HTTP errors from OpenRouter API.
     */
    private OpenRouterResponse handleHttpError(HttpClientErrorException e) {
        int statusCode = e.getStatusCode().value();
        String errorMessage = parseErrorMessage(e.getResponseBodyAsString());

        logger.error("OpenRouter API error {}: {}", statusCode, errorMessage);

        String errorCode;
        boolean retryable;

        switch (statusCode) {
            case 400:
                errorCode = "INVALID_FORMAT";
                retryable = false;
                break;
            case 401:
                errorCode = "AUTH_FAILED";
                retryable = false;
                break;
            case 402:
                errorCode = "INSUFFICIENT_CREDITS";
                retryable = false;
                break;
            case 403:
                errorCode = "CONTENT_FILTERED";
                retryable = true;
                break;
            case 422:
                errorCode = "INVALID_PARAMS";
                retryable = false;
                break;
            case 429:
                errorCode = "RATE_LIMITED";
                retryable = true;
                break;
            case 502:
                errorCode = "MODEL_UNAVAILABLE";
                retryable = true;
                break;
            case 503:
                errorCode = "NO_PROVIDERS";
                retryable = true;
                break;
            default:
                errorCode = "UNKNOWN_ERROR";
                retryable = false;
        }

        throw new OpenRouterException(errorMessage, errorCode, retryable);
    }

    /**
     * Parse error message from OpenRouter error response.
     */
    private String parseErrorMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("error")) {
                JsonNode error = root.get("error");
                String message = error.has("message") ? error.get("message").asText() : "Unknown error";
                String code = error.has("code") ? error.get("code").asText() : null;
                return code != null ? code + ": " + message : message;
            }
            return responseBody;
        } catch (Exception e) {
            return responseBody;
        }
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

        HttpHeaders headers = buildHeaders();
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

            return parseResponse(response.getBody(), duration);

        } catch (HttpClientErrorException e) {
            return handleHttpError(e);
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
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(Objects.requireNonNull(config.getApiKey()));
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("HTTP-Referer", "https://cramer.edu.vn");
            headers.set("X-Title", "Cramer ABTS");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = config.getBaseUrl().replace("/api/v1", "") + "/api/v1/models";
            logger.info("Fetching models from: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    Objects.requireNonNull(HttpMethod.GET),
                    entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode dataNode = rootNode.get("data");

                if (dataNode != null && dataNode.isArray()) {
                    List<Map<String, Object>> models = new ArrayList<>();

                    for (JsonNode modelNode : dataNode) {
                        Map<String, Object> model = new HashMap<>();

                        // Basic info
                        model.put("id", getTextValue(modelNode, "id"));
                        model.put("name", getTextValue(modelNode, "name"));
                        model.put("description", getTextValue(modelNode, "description"));
                        model.put("context_length", modelNode.has("context_length")
                                ? modelNode.get("context_length").asInt()
                                : null);

                        // Pricing
                        if (modelNode.has("pricing")) {
                            JsonNode pricing = modelNode.get("pricing");
                            Map<String, String> pricingMap = new HashMap<>();
                            pricingMap.put("prompt", getTextValue(pricing, "prompt"));
                            pricingMap.put("completion", getTextValue(pricing, "completion"));
                            model.put("pricing", pricingMap);
                        }

                        // Supported parameters (for filtering JSON support)
                        if (modelNode.has("supported_parameters")) {
                            JsonNode params = modelNode.get("supported_parameters");
                            if (params.isArray()) {
                                List<String> paramList = new ArrayList<>();
                                for (JsonNode param : params) {
                                    paramList.add(param.asText());
                                }
                                model.put("supported_parameters", paramList);
                            }
                        }

                        // Top provider info
                        if (modelNode.has("top_provider")) {
                            JsonNode topProvider = modelNode.get("top_provider");
                            model.put("max_completion_tokens", topProvider.has("max_completion_tokens")
                                    ? topProvider.get("max_completion_tokens").asInt()
                                    : null);
                        }

                        models.add(model);
                    }

                    logger.info("Fetched {} models from OpenRouter", models.size());
                    return models;
                }
            }

            logger.warn("Failed to fetch models, returning empty list");
            return Collections.emptyList();

        } catch (Exception e) {
            logger.error("Error fetching models from OpenRouter: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getTextValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }
}
