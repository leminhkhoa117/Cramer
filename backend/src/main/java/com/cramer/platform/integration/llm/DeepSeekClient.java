package com.cramer.platform.integration.llm;

import com.cramer.platform.common.json.Json;
import com.cramer.platform.error.UpstreamServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Thin client for the DeepSeek OpenAI-compatible {@code /chat/completions} endpoint
 * (SPEC-18 §4, SPEC-13 §4.1). No business logic — callers supply prompts and parse the result.
 * Failures surface as {@link UpstreamServiceException} (→ 503).
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final LlmProperties props;
    private final RestClient http;

    public DeepSeekClient(LlmProperties props) {
        this.props = props;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(props.resolvedTimeoutMs()));
        this.http = RestClient.builder().baseUrl(props.resolvedBaseUrl()).requestFactory(factory).build();
    }

    public boolean isConfigured() {
        return props.hasApiKey();
    }

    /**
     * Run a JSON-object chat completion (no streaming) and return the parsed message content.
     *
     * @param model        model id (null → configured grading model)
     * @param systemPrompt system role content
     * @param userPrompt   user role content
     * @param temperature  sampling temperature
     * @param maxTokens    max output tokens
     * @return the assistant message content parsed as JSON
     */
    public JsonNode chatJson(String model, String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        if (!props.hasApiKey()) {
            throw new UpstreamServiceException("DEEPSEEK_API_KEY is not configured");
        }
        ObjectNode body = Json.mapper().createObjectNode();
        body.put("model", (model == null || model.isBlank()) ? props.resolvedGradingModel() : model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.set("response_format", Json.mapper().createObjectNode().put("type", "json_object"));
        ArrayNode messages = body.putArray("messages");
        messages.add(Json.mapper().createObjectNode().put("role", "system").put("content", systemPrompt));
        messages.add(Json.mapper().createObjectNode().put("role", "user").put("content", userPrompt));

        String raw;
        try {
            raw = http.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Json.toJson(body))
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("DeepSeek call failed: {}", e.getMessage());
            throw new UpstreamServiceException("DeepSeek request failed", e);
        }

        JsonNode root = Json.readTree(raw);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new UpstreamServiceException("DeepSeek returned no content");
        }
        return Json.readTree(content.asText());
    }
}
