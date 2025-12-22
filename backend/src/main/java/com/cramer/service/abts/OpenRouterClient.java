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

        return callChatCompletion(
                model, systemPrompt, userPrompt, jsonSchema,
                List.of("anthropic/claude-3.5-sonnet", "meta-llama/llama-3.1-70b-instruct:free"),
                Map.of("effort", "high"),
                1.0,
                8192);
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
            StreamCallback callback) {

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
                            if (delta.has("reasoning")) {
                                String reasoningDelta = delta.get("reasoning").asText();
                                reasoningBuilder.append(reasoningDelta);
                                callback.onReasoningChunk(reasoningDelta);
                            }

                            // Content tokens
                            if (delta.has("content")) {
                                String contentDelta = delta.get("content").asText();
                                contentBuilder.append(contentDelta);
                                callback.onContentChunk(contentDelta);
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
            headers.setBearerAuth(config.getApiKey());
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
