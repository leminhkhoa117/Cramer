package com.cramer.platform.integration.openrouter;

import com.cramer.platform.common.json.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.BooleanSupplier;

/**
 * Client for the OpenRouter chat-completions + models API (SPEC-24 §2). Supports
 * JSON-schema-constrained non-streaming calls, true SSE streaming with cancellation, and model
 * listing. Transport/HTTP errors are normalized to {@link OpenRouterException} carrying an
 * {@link OpenRouterError} with a {@code retryable} flag (consumed by the ABTS retry logic,
 * SPEC-21 §2). No business logic lives here.
 */
@Component
public class OpenRouterClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    private final OpenRouterProperties props;
    private final RestClient http;
    private final HttpClient streamHttp;

    public OpenRouterClient(OpenRouterProperties props) {
        this.props = props;
        // JDK client with explicit connect timeout; per-call read timeout comes
        // from openrouter.api-timeout-ms so a hung upstream call cannot block
        // a Tomcat thread forever.
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(props.resolvedTimeoutMs()));
        this.http = RestClient.builder().baseUrl(props.resolvedBaseUrl()).requestFactory(factory).build();
        this.streamHttp = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public boolean isConfigured() {
        return props.hasApiKey();
    }

    // ---------------------------------------------------------------- non-streaming

    /** Run a (non-streaming) chat completion and return the parsed result (SPEC-22 §1). */
    public OpenRouterChatResult chat(OpenRouterChatRequest request) {
        requireKey();
        ObjectNode body = buildBody(request, false);
        String raw;
        try {
            raw = http.post()
                    .uri("/chat/completions")
                    .headers(this::authHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Json.toJson(body))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw mapStatus(e.getStatusCode().value(), e, retryAfter(e));
        } catch (Exception e) {
            throw new OpenRouterException(OpenRouterError.UPSTREAM_ERROR, "OpenRouter request failed", e);
        }
        return parseCompletion(Json.readTree(raw), request.model());
    }

    // ---------------------------------------------------------------- streaming

    /**
     * Run a streaming chat completion. Content/reasoning deltas are forwarded to {@code listener}
     * as they arrive; the accumulated result is returned at the end. The {@code cancelled}
     * supplier is checked between SSE lines — when it flips true the upstream connection is
     * closed immediately so no further tokens are billed (SPEC-21 §6).
     */
    public OpenRouterChatResult streamChat(OpenRouterChatRequest request,
                                           OpenRouterStreamListener listener,
                                           BooleanSupplier cancelled) {
        requireKey();
        ObjectNode body = buildBody(request, true);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(props.resolvedBaseUrl() + "/chat/completions"))
                .timeout(Duration.ofMillis(props.resolvedTimeoutMs()))
                .header("Authorization", "Bearer " + props.apiKey())
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", props.resolvedSiteUrl())
                .header("X-Title", props.resolvedSiteName())
                .POST(HttpRequest.BodyPublishers.ofString(Json.toJson(body), StandardCharsets.UTF_8))
                .build();

        StringBuilder content = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        JsonNode usageNode = null;
        try {
            HttpResponse<InputStream> response =
                    streamHttp.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw mapStatus(response.statusCode(),
                        new IllegalStateException("OpenRouter stream HTTP " + response.statusCode() + ": " + errBody),
                        parseRetryAfter(response.headers().firstValue("Retry-After").orElse(null)));
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (cancelled.getAsBoolean()) {
                        log.debug("OpenRouter stream cancelled by caller; disconnecting");
                        break;
                    }
                    if (line.isBlank() || line.startsWith(":")) {
                        continue; // SSE comment / keep-alive
                    }
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    JsonNode chunk = tryParse(data);
                    if (chunk == null) {
                        continue;
                    }
                    JsonNode delta = chunk.path("choices").path(0).path("delta");
                    String c = textOrNull(delta.path("content"));
                    if (c != null) {
                        content.append(c);
                        listener.onContentDelta(c);
                    }
                    String r = textOrNull(delta.path("reasoning"));
                    if (r != null) {
                        reasoning.append(r);
                        listener.onReasoningDelta(r);
                    }
                    if (chunk.hasNonNull("usage")) {
                        usageNode = chunk.get("usage");
                    }
                }
            }
        } catch (OpenRouterException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new OpenRouterException(OpenRouterError.TIMEOUT, "OpenRouter stream timed out", e);
        } catch (Exception e) {
            throw new OpenRouterException(OpenRouterError.UPSTREAM_ERROR, "OpenRouter stream failed", e);
        }

        String rawContent = content.toString();
        JsonNode parsed = tryParse(rawContent);
        if (parsed == null) {
            parsed = Json.mapper().getNodeFactory().textNode(rawContent);
        }
        return new OpenRouterChatResult(
                parsed, rawContent,
                reasoning.length() == 0 ? null : reasoning.toString(),
                intOrNull(usageNode, "prompt_tokens"),
                intOrNull(usageNode, "completion_tokens"),
                intOrNull(usageNode, "total_tokens"),
                doubleOrNull(usageNode, "cost"),
                request.model());
    }

    // ---------------------------------------------------------------- models

    /** Fetch the OpenRouter model catalog ({@code GET /models}). Returns the {@code data} array. */
    public JsonNode listModels() {
        requireKey();
        try {
            String raw = http.get()
                    .uri("/models")
                    .headers(this::authHeaders)
                    .retrieve()
                    .body(String.class);
            return Json.readTree(raw).path("data");
        } catch (RestClientResponseException e) {
            throw mapStatus(e.getStatusCode().value(), e, retryAfter(e));
        } catch (Exception e) {
            throw new OpenRouterException(OpenRouterError.UPSTREAM_ERROR, "OpenRouter /models failed", e);
        }
    }

    // ---------------------------------------------------------------- helpers

    private void requireKey() {
        if (!props.hasApiKey()) {
            throw new OpenRouterException(OpenRouterError.AUTH_FAILED, "OPENROUTER_API_KEY is not configured");
        }
    }

    private void authHeaders(org.springframework.http.HttpHeaders headers) {
        headers.setBearerAuth(props.apiKey());
        headers.add("HTTP-Referer", props.resolvedSiteUrl());
        headers.add("X-Title", props.resolvedSiteName());
    }

    private ObjectNode buildBody(OpenRouterChatRequest request, boolean stream) {
        ObjectNode body = Json.mapper().createObjectNode();
        body.put("model", request.model());
        body.put("temperature", request.temperature());
        if (request.maxTokens() > 0) {
            body.put("max_tokens", request.maxTokens());
        }
        if (stream) {
            body.put("stream", true);
            body.set("usage", Json.mapper().createObjectNode().put("include", true));
        }
        ArrayNode messages = body.putArray("messages");
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(Json.mapper().createObjectNode().put("role", "system").put("content", request.systemPrompt()));
        }
        messages.add(Json.mapper().createObjectNode().put("role", "user").put("content", request.userPrompt()));

        if (request.jsonSchema() != null && !request.jsonSchema().isMissingNode()) {
            ObjectNode responseFormat = body.putObject("response_format");
            responseFormat.put("type", "json_schema");
            ObjectNode schemaWrap = responseFormat.putObject("json_schema");
            schemaWrap.put("name", request.schemaName() == null ? "content" : request.schemaName());
            schemaWrap.put("strict", true);
            schemaWrap.set("schema", request.jsonSchema());
        }
        if (request.reasoning() != null && !request.reasoning().isMissingNode()) {
            body.set("reasoning", request.reasoning());
        }
        return body;
    }

    private OpenRouterChatResult parseCompletion(JsonNode root, String model) {
        JsonNode message = root.path("choices").path(0).path("message");
        String rawContent = textOrNull(message.path("content"));
        if (rawContent == null || rawContent.isBlank()) {
            throw new OpenRouterException(OpenRouterError.UPSTREAM_ERROR, "OpenRouter returned no content");
        }
        JsonNode parsed = tryParse(rawContent);
        if (parsed == null) {
            parsed = Json.mapper().getNodeFactory().textNode(rawContent);
        }
        String reasoning = textOrNull(message.path("reasoning"));
        JsonNode usage = root.path("usage");
        return new OpenRouterChatResult(
                parsed, rawContent, reasoning,
                intOrNull(usage, "prompt_tokens"),
                intOrNull(usage, "completion_tokens"),
                intOrNull(usage, "total_tokens"),
                doubleOrNull(usage, "cost"),
                root.path("model").asText(model));
    }

    private OpenRouterException mapStatus(int status, Throwable cause, Long retryAfterMs) {
        OpenRouterError code = OpenRouterError.fromHttpStatus(status);
        return new OpenRouterException(code, "OpenRouter HTTP " + status, retryAfterMs, cause);
    }

    /** Parse a Retry-After header (seconds or HTTP-date) into milliseconds, or null. */
    private static Long retryAfter(RestClientResponseException e) {
        return parseRetryAfter(e.getResponseHeaders().getFirst("Retry-After"));
    }

    private static Long parseRetryAfter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim()) * 1000L;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static JsonNode tryParse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Json.readTree(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String textOrNull(JsonNode node) {
        return (node == null || node.isMissingNode() || node.isNull()) ? null : node.asText();
    }

    private static Integer intOrNull(JsonNode parent, String field) {
        if (parent == null) {
            return null;
        }
        JsonNode n = parent.path(field);
        return n.isNumber() ? n.asInt() : null;
    }

    private static Double doubleOrNull(JsonNode parent, String field) {
        if (parent == null) {
            return null;
        }
        JsonNode n = parent.path(field);
        return n.isNumber() ? n.asDouble() : null;
    }
}
