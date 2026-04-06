package com.cramer.service.implement;

import com.cramer.config.OpenRouterConfig;
import com.cramer.config.SpeakingSelectionProperties;
import com.cramer.config.SpeakingSessionProperties;
import com.cramer.service.SpeakingSelectionPlannerService;
import com.cramer.service.speaking.SpeakingSelectionPromptBuilder;
import com.cramer.service.speaking.SpeakingSelectionPromptBuilder.PromptPair;
import com.cramer.service.speaking.SpeakingSelectionPromptBuilder.SelectionParseResult;
import com.fasterxml.jackson.databind.JsonNode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * LLM-powered implementation of {@link SpeakingSelectionPlannerService}.
 *
 * <p>Calls an OpenRouter-compatible LLM to select a coherent subset of
 * questions from the authored bank. On any failure (timeout, invalid
 * response, parse error), falls back to the heuristic planner.</p>
 *
 * <p>This class is <strong>not</strong> annotated with {@code @Service};
 * it is wired via {@code SpeakingSelectionPlannerConfig}.</p>
 *
 * @since 2026-04-05
 */
public class LlmSpeakingSelectionPlannerService implements SpeakingSelectionPlannerService {

    private static final Logger log = LoggerFactory.getLogger(LlmSpeakingSelectionPlannerService.class);
    private static final String STRATEGY_LLM = "llm_selection_v1";

    private final OpenRouterConfig openRouterConfig;
    private final SpeakingSelectionProperties selectionProperties;
    private final SpeakingSelectionPromptBuilder promptBuilder;
    private final HeuristicSpeakingSelectionPlannerService heuristicFallback;
    private final RestTemplate selectionRestTemplate;
    private final SecureRandom random = new SecureRandom();

    public LlmSpeakingSelectionPlannerService(
            OpenRouterConfig openRouterConfig,
            SpeakingSelectionProperties selectionProperties,
            SpeakingSelectionPromptBuilder promptBuilder,
            HeuristicSpeakingSelectionPlannerService heuristicFallback,
            RestTemplate selectionRestTemplate) {
        this.openRouterConfig = openRouterConfig;
        this.selectionProperties = selectionProperties;
        this.promptBuilder = promptBuilder;
        this.heuristicFallback = heuristicFallback;
        this.selectionRestTemplate = selectionRestTemplate;
    }

    // ──────────────────────────────────────────────────────────────
    // Public interface methods
    // ──────────────────────────────────────────────────────────────

    @Override
    public SelectionResult selectPart1(List<PlannerCandidate> bank, SpeakingSessionProperties.PartPlan config) {
        if (!isLlmAvailable("Part 1")) {
            return heuristicFallback.selectPart1(bank, config);
        }
        int targetTurnCount = pickTargetTurnCount(config);
        PromptPair prompt = promptBuilder.buildPart1Prompt(bank, targetTurnCount);
        return callLlmWithFallback("Part 1", bank, targetTurnCount, prompt,
                () -> heuristicFallback.selectPart1(bank, config));
    }

    @Override
    public SelectionResult selectPart2(List<PlannerCandidate> bank, SpeakingSessionProperties.PartPlan config) {
        // Part 2 always uses heuristic — picking 1 cue card from a bank of 1
        return heuristicFallback.selectPart2(bank, config);
    }

    @Override
    public SelectionResult selectIndependentPart3(List<PlannerCandidate> bank, SpeakingSessionProperties.PartPlan config) {
        if (!isLlmAvailable("Part 3 independent")) {
            return heuristicFallback.selectIndependentPart3(bank, config);
        }
        int targetTurnCount = pickTargetTurnCount(config);
        PromptPair prompt = promptBuilder.buildIndependentPart3Prompt(bank, targetTurnCount);
        return callLlmWithFallback("Part 3 independent", bank, targetTurnCount, prompt,
                () -> heuristicFallback.selectIndependentPart3(bank, config));
    }

    @Override
    public SelectionResult selectFollowUpPart3(
            List<PlannerCandidate> bank,
            JsonNode part2QuestionSnapshot,
            String part2TranscriptText,
            SpeakingSessionProperties.PartPlan config) {
        if (!isLlmAvailable("Part 3 follow-up")) {
            return heuristicFallback.selectFollowUpPart3(bank, part2QuestionSnapshot, part2TranscriptText, config);
        }
        int targetTurnCount = pickTargetTurnCount(config);
        PromptPair prompt = promptBuilder.buildFollowUpPart3Prompt(
                bank, targetTurnCount, part2QuestionSnapshot, part2TranscriptText);
        return callLlmWithFallback("Part 3 follow-up", bank, targetTurnCount, prompt,
                () -> heuristicFallback.selectFollowUpPart3(bank, part2QuestionSnapshot, part2TranscriptText, config));
    }

    // ──────────────────────────────────────────────────────────────
    // Guard: check LLM availability (model + API key)
    // ──────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if LLM selection can be attempted (model and API key
     * are both configured). Logs a warning and returns {@code false} otherwise.
     */
    private boolean isLlmAvailable(String partLabel) {
        if (!selectionProperties.hasModel()) {
            log.warn("LLM selection for {} skipped: no model configured, falling back to heuristic", partLabel);
            return false;
        }
        if (!openRouterConfig.hasApiKey()) {
            log.warn("LLM selection for {} skipped: OPENROUTER_API_KEY not configured, falling back to heuristic", partLabel);
            return false;
        }
        return true;
    }

    // ──────────────────────────────────────────────────────────────
    // Core LLM call with fallback
    // ──────────────────────────────────────────────────────────────

    private SelectionResult callLlmWithFallback(
            String partLabel,
            List<PlannerCandidate> bank,
            int targetTurnCount,
            PromptPair prompt,
            FallbackSupplier fallback) {

        try {
            long startTime = System.currentTimeMillis();
            String llmContent = callOpenRouter(prompt);
            long duration = System.currentTimeMillis() - startTime;

            log.info("LLM selection for {} completed in {}ms", partLabel, duration);

            SelectionParseResult parseResult = promptBuilder.parseAndValidate(llmContent, bank, targetTurnCount);
            if (!parseResult.valid()) {
                log.warn("LLM selection for {} returned invalid output: {}. Falling back to heuristic.",
                        partLabel, parseResult.error());
                return fallback.get();
            }

            // Resolve IDs back to PlannerCandidate objects, preserving LLM order
            List<PlannerCandidate> selected = resolveSelectedCandidates(bank, parseResult.selectedIds());

            if (!parseResult.reasoningSummary().isEmpty()) {
                log.info("LLM selection reasoning for {}: {}", partLabel, parseResult.reasoningSummary());
            }

            return new SelectionResult(targetTurnCount, selected, STRATEGY_LLM);

        } catch (Exception e) {
            log.warn("LLM selection for {} failed with {}: {}. Falling back to heuristic.",
                    partLabel, e.getClass().getSimpleName(), e.getMessage());
            return fallback.get();
        }
    }

    // ──────────────────────────────────────────────────────────────
    // OpenRouter HTTP call
    // ──────────────────────────────────────────────────────────────

    private String callOpenRouter(PromptPair prompt) {
        String url = openRouterConfig.getBaseUrl() + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Objects.requireNonNull(openRouterConfig.getApiKey()));
        headers.set("HTTP-Referer", openRouterConfig.getSiteUrl());
        headers.set("X-Title", openRouterConfig.getSiteName());

        Map<String, Object> requestBody = buildRequestBody(prompt);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = selectionRestTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("OpenRouter returned status: " + response.getStatusCode());
        }

        return extractContentFromResponse(response.getBody());
    }

    private Map<String, Object> buildRequestBody(PromptPair prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", selectionProperties.getModel());

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", prompt.systemPrompt()));
        messages.add(Map.of("role", "user", "content", prompt.userPrompt()));
        body.put("messages", messages);

        body.put("temperature", selectionProperties.getTemperature());
        body.put("max_tokens", selectionProperties.getMaxTokens());
        body.put("stream", false);

        // Structured JSON output via schema
        Map<String, Object> schema = promptBuilder.getSelectionResponseSchema();
        body.put("response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "speaking_selection_response",
                        "strict", true,
                        "schema", schema)));

        // Provider preferences
        body.put("provider", Map.of(
                "allow_fallbacks", true,
                "data_collection", "allow"));

        return body;
    }

    private String extractContentFromResponse(String responseBody) {
        try {
            JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("No choices in OpenRouter response");
            }
            JsonNode message = choices.get(0).get("message");
            if (message == null || !message.hasNonNull("content")) {
                throw new RuntimeException("No message content in OpenRouter response");
            }
            return message.get("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenRouter response: " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private List<PlannerCandidate> resolveSelectedCandidates(
            List<PlannerCandidate> bank, List<Long> selectedIds) {
        Map<Long, PlannerCandidate> lookup = bank.stream()
                .collect(Collectors.toMap(PlannerCandidate::sourceQuestionId, c -> c, (a, b) -> a));
        List<PlannerCandidate> resolved = new ArrayList<>();
        for (Long id : selectedIds) {
            PlannerCandidate candidate = lookup.get(id);
            if (candidate != null) {
                resolved.add(candidate);
            }
        }
        return resolved;
    }

    private int pickTargetTurnCount(SpeakingSessionProperties.PartPlan config) {
        int min = config.getMinSelected();
        int max = config.getMaxSelected();
        if (min <= 0 || max <= 0 || max < min) {
            throw new IllegalStateException("Invalid Speaking selection window configuration.");
        }
        if (min == max) {
            return min;
        }
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * Functional interface for heuristic fallback suppliers.
     */
    @FunctionalInterface
    private interface FallbackSupplier {
        SelectionResult get();
    }
}
