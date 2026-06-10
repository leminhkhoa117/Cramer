package com.cramer.service.abts;

import com.cramer.config.OpenRouterConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

final class OpenRouterClientSupport {

    private static final Logger logger = LoggerFactory.getLogger(OpenRouterClientSupport.class);

    private final OpenRouterConfig config;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    OpenRouterClientSupport(OpenRouterConfig config, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    /**
     * Spread a reasoning payload into an OpenRouter request body.
     *
     * <p>Vendor-aware payloads from {@link ModelCapabilityRegistry} already carry the
     * correct top-level key(s) (e.g. {@code reasoning}, {@code thinking},
     * {@code thinking_config}) and are spread verbatim. For backward compatibility a
     * legacy <em>flat</em> reasoning map (containing {@code effort}/{@code max_tokens}/
     * {@code enabled} at the top level, as still produced by the refinement flow) is
     * nested under {@code "reasoning"} so existing callers keep working.</p>
     */
    static void applyReasoning(Map<String, Object> body, Map<String, Object> reasoningConfig) {
        if (reasoningConfig == null || reasoningConfig.isEmpty()) {
            return;
        }
        if (reasoningConfig.containsKey("effort")
                || reasoningConfig.containsKey("max_tokens")
                || reasoningConfig.containsKey("enabled")) {
            body.put("reasoning", reasoningConfig);
        } else {
            body.putAll(reasoningConfig);
        }
    }

    String readStream(java.io.InputStream stream) {
        if (stream == null)
            return "";
        try (java.util.Scanner scanner = new java.util.Scanner(stream, "UTF-8").useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Objects.requireNonNull(config.getApiKey()));
        headers.set("HTTP-Referer", config.getSiteUrl());
        headers.set("X-Title", config.getSiteName());
        return headers;
    }

    Map<String, Object> buildRequestBody(
            String model,
            String systemPrompt,
            String userPrompt,
            Map<String, Object> jsonSchema,
            List<String> fallbackModels,
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
        body.put("stream", false);

        if (jsonSchema != null && !jsonSchema.isEmpty()) {
            body.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", "ielts_content_response",
                            "strict", true,
                            "schema", jsonSchema)));
            logger.debug("Using JSON schema mode for model: {}", model);
        } else {
            body.put("response_format", Map.of("type", "json_object"));
            logger.debug("Using basic JSON object mode for model: {}", model);
        }

        if (fallbackModels != null && !fallbackModels.isEmpty()) {
            List<String> allModels = new ArrayList<>();
            allModels.add(model);
            allModels.addAll(fallbackModels);
            body.put("models", allModels);
            body.put("route", "fallback");
        }

        body.put("provider", Map.of(
                "allow_fallbacks", true,
                "data_collection", "allow"));

        applyReasoning(body, reasoningConfig);

        return body;
    }

    Map<String, Object> buildRequestBodyWithFeatures(
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

        String effectiveModel = enableWebSearch ? model + ":online" : model;
        body.put("model", effectiveModel);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);

        body.put("temperature", temperature != null ? temperature : 1.0);
        body.put("max_tokens", maxTokens != null ? maxTokens : 8192);
        body.put("stream", false);

        if (enableContextCaching) {
            body.put("cache_prompt", true);
            logger.debug("Context caching enabled for model: {}", effectiveModel);
        }

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

        if (enableWebSearch) {
            logger.info("Web search enabled for AI fact research on model: {}", effectiveModel);
        }

        if (fallbackModels != null && !fallbackModels.isEmpty()) {
            List<String> allModels = new ArrayList<>();
            allModels.add(effectiveModel);
            allModels.addAll(fallbackModels);
            body.put("models", allModels);
            body.put("route", "fallback");
        }

        body.put("provider", Map.of(
                "allow_fallbacks", true,
                "data_collection", "allow"));

        applyReasoning(body, reasoningConfig);

        return body;
    }

    OpenRouterClient.OpenRouterResponse parseResponse(String responseBody, long durationMs) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            OpenRouterClient.OpenRouterResponse response = new OpenRouterClient.OpenRouterResponse();
            response.setDurationMs(durationMs);

            if (root.has("model")) {
                response.setModelUsed(root.get("model").asText());
            }

            JsonNode choices = root.path("choices");
            if (!choices.isMissingNode() && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.path("message");

                if (!message.isMissingNode()) {
                    if (message.has("content")) {
                        response.setContent(message.get("content").asText());
                    }
                    if (message.has("reasoning")) {
                        response.setReasoning(message.get("reasoning").asText());
                    }
                }
            }

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
            throw new OpenRouterClient.OpenRouterException(
                    "Failed to parse response: " + e.getMessage(), "PARSE_ERROR", false);
        }
    }

    OpenRouterClient.OpenRouterResponse handleHttpError(HttpClientErrorException e) {
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

        throw new OpenRouterClient.OpenRouterException(errorMessage, errorCode, retryable);
    }

    String parseErrorMessage(String responseBody) {
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

    List<Map<String, Object>> fetchAvailableModels() {
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

                        model.put("id", getTextValue(modelNode, "id"));
                        model.put("name", getTextValue(modelNode, "name"));
                        model.put("description", getTextValue(modelNode, "description"));
                        model.put("context_length", modelNode.has("context_length")
                                ? modelNode.get("context_length").asInt()
                                : null);

                        if (modelNode.has("pricing")) {
                            JsonNode pricing = modelNode.get("pricing");
                            Map<String, String> pricingMap = new HashMap<>();
                            pricingMap.put("prompt", getTextValue(pricing, "prompt"));
                            pricingMap.put("completion", getTextValue(pricing, "completion"));
                            model.put("pricing", pricingMap);
                        }

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