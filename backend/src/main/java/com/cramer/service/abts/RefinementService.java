package com.cramer.service.abts;

import com.cramer.config.OpenRouterConfig;
import com.cramer.dto.abts.RefinementRequestDTO;
import com.cramer.dto.abts.RefinementRequestDTO.ValidationIssue;
import com.cramer.dto.abts.RefinementResponseDTO;
import com.cramer.dto.abts.RefinementResponseDTO.RefinementPatch;
import com.cramer.dto.abts.StreamEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RefinementService - Orchestrates Agent 2 (Refinement Agent)
 * 
 * Handles the refinement workflow:
 * 1. Receives original JSON + selected issues from user
 * 2. Builds refinement prompt using RefinementPromptBuilder
 * 3. Calls AI with full context (via OpenRouter caching)
 * 4. Streams refinement output with patch extraction
 * 5. Validates refined output
 * 
 * @since 2026-01-04
 */
@Service
public class RefinementService {

    private static final Logger logger = LoggerFactory.getLogger(RefinementService.class);

    private final OpenRouterConfig config;
    private final OpenRouterClient openRouterClient;
    private final RefinementPromptBuilder refinementPromptBuilder;
    private final JsonValidatorService jsonValidatorService;
    private final JsonPatcher jsonPatcher;
    private final ObjectMapper objectMapper;

    public RefinementService(
            OpenRouterConfig config,
            OpenRouterClient openRouterClient,
            RefinementPromptBuilder refinementPromptBuilder,
            JsonValidatorService jsonValidatorService,
            JsonPatcher jsonPatcher) {
        this.config = config;
        this.openRouterClient = openRouterClient;
        this.refinementPromptBuilder = refinementPromptBuilder;
        this.jsonValidatorService = jsonValidatorService;
        this.jsonPatcher = jsonPatcher;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Refine content with streaming output.
     * Sends SSE events to the emitter during refinement.
     */
    public void refineWithStream(
            RefinementRequestDTO request,
            SseEmitter emitter,
            AtomicBoolean cancelled) throws IOException {

        try {
            // Validate request
            if (request.getSelectedIssueIds() == null || request.getSelectedIssueIds().isEmpty()) {
                sendEvent(emitter, StreamEventDTO.failed("No issues selected for refinement"));
                emitter.complete();
                return;
            }

            sendEvent(emitter, StreamEventDTO.started());
            sendEvent(emitter, StreamEventDTO.progress(5,
                    "Preparing refinement for " + request.getSelectedIssueIds().size() + " issues..."));

            // Get selected issues from validation result
            List<ValidationIssue> selectedIssues = extractSelectedIssues(request);
            if (selectedIssues.isEmpty()) {
                sendEvent(emitter, StreamEventDTO.failed("Could not find selected issues"));
                emitter.complete();
                return;
            }

            // Build prompts
            sendEvent(emitter, StreamEventDTO.progress(10, "Building refinement prompt..."));
            String systemPrompt = refinementPromptBuilder.buildSystemPrompt();
            String refinementPrompt = refinementPromptBuilder.buildRefinementPrompt(
                    request.getOriginalJson(),
                    selectedIssues,
                    extractPassageContext(request.getOriginalJson()));

            // Check cancellation
            if (cancelled != null && cancelled.get()) {
                sendEvent(emitter, StreamEventDTO.aborted());
                emitter.complete();
                return;
            }

            // Call AI with streaming
            sendEvent(emitter, StreamEventDTO.progress(15, "Calling Agent 2 (Refinement AI)..."));

            StringBuilder fullResponse = new StringBuilder();
            final Object lock = new Object();
            final boolean[] completed = { false };
            final Exception[] errorHolder = new Exception[1];

            // Use model from request, or default to Gemini Flash
            String model = request.getModel() != null
                    ? request.getModel()
                    : "google/gemini-3-flash-preview";
            boolean enableCaching = request.getEnableCaching() != null
                    ? request.getEnableCaching()
                    : true;
            boolean enableReasoning = request.getEnableReasoning() != null
                    ? request.getEnableReasoning()
                    : false;

            // Build reasoning config if enabled (for models like DeepSeek R1)
            // API only allows ONE of: "effort" OR "max_tokens" (not both)
            Map<String, Object> reasoningConfig = enableReasoning
                    ? Map.of("effort", "high")
                    : Map.of();

            logger.info("Refinement using model: {} (caching: {}, reasoning: {})", model, enableCaching,
                    enableReasoning);

            openRouterClient.callChatCompletionStreaming(
                    model,
                    systemPrompt,
                    buildConversationPrompt(request, refinementPrompt),
                    null, // No JSON schema for refinement (we want free-form with JSON block)
                    reasoningConfig, // Reasoning config from request
                    0.3, // Lower temperature for precise fixes
                    32000, // Increased from 8192 to handle large JSON output
                    enableCaching, // Enable caching for cost reduction
                    new OpenRouterClient.StreamCallback() {
                        @Override
                        public void onReasoningChunk(String chunk) {
                            sendEvent(emitter, StreamEventDTO.aiThinking(chunk));
                        }

                        @Override
                        public void onContentChunk(String chunk) {
                            fullResponse.append(chunk);
                            sendEvent(emitter, StreamEventDTO.aiChunk(chunk));
                        }

                        @Override
                        public void onProgress(int percent, String message) {
                            sendEvent(emitter, StreamEventDTO.progress(
                                    15 + (percent * 70 / 100), // Scale to 15-85%
                                    "Refining: " + message));
                        }

                        @Override
                        public void onComplete(OpenRouterClient.OpenRouterResponse response) {
                            synchronized (lock) {
                                completed[0] = true;
                                lock.notify();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            synchronized (lock) {
                                errorHolder[0] = new RuntimeException(error);
                                completed[0] = true;
                                lock.notify();
                            }
                        }
                    },
                    cancelled);

            // Wait for streaming to complete
            synchronized (lock) {
                while (!completed[0]) {
                    lock.wait();
                }
            }

            // Check for errors
            if (errorHolder[0] != null) {
                throw errorHolder[0];
            }

            // Check cancellation
            if (cancelled != null && cancelled.get()) {
                sendEvent(emitter, StreamEventDTO.aborted());
                emitter.complete();
                return;
            }

            // Parse response - now expects patch format
            sendEvent(emitter, StreamEventDTO.progress(90, "Processing patches..."));
            RefinementResponseDTO response = parseAndApplyPatches(
                    fullResponse.toString(),
                    request.getOriginalJson(),
                    selectedIssues);

            // Validate refined JSON
            if (response.getRefinedJson() != null && !response.getRefinedJson().isEmpty()) {
                sendEvent(emitter, StreamEventDTO.progress(95, "Validating refined content..."));
                // Re-validate to show new status
                // (Skipping deep validation for now - just parse check)
                try {
                    objectMapper.readTree(response.getRefinedJson());
                    response.setSuccess(true);
                } catch (Exception e) {
                    response.setSuccess(false);
                    response.setErrorMessage("Refined JSON is invalid: " + e.getMessage());
                }
            }

            // Send completion with response
            sendEvent(emitter, StreamEventDTO.refinementCompleted(response));
            emitter.complete();

            logger.info("Refinement completed. Fixed {} issues.",
                    response.getPatches() != null ? response.getPatches().size() : 0);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendEvent(emitter, StreamEventDTO.aborted());
            emitter.complete();
        } catch (Exception e) {
            logger.error("Refinement failed: {}", e.getMessage(), e);
            sendEvent(emitter, StreamEventDTO.failed("Refinement failed: " + e.getMessage()));
            emitter.complete();
        }
    }

    /**
     * Extract selected issues from the validation result based on IDs
     */
    private List<ValidationIssue> extractSelectedIssues(RefinementRequestDTO request) {
        List<ValidationIssue> result = new ArrayList<>();
        Set<String> selectedIds = new HashSet<>(request.getSelectedIssueIds());

        if (request.getValidationResult() != null) {
            if (request.getValidationResult().getErrors() != null) {
                result.addAll(request.getValidationResult().getErrors().stream()
                        .filter(issue -> selectedIds.contains(issue.getId()))
                        .collect(Collectors.toList()));
            }
            if (request.getValidationResult().getWarnings() != null) {
                result.addAll(request.getValidationResult().getWarnings().stream()
                        .filter(issue -> selectedIds.contains(issue.getId()))
                        .collect(Collectors.toList()));
            }
        }

        return result;
    }

    /**
     * Extract passage or transcript text from the original JSON response.
     * This provides the necessary context for the Refinement AI.
     */
    private String extractPassageContext(String originalJson) {
        if (originalJson == null || originalJson.isBlank())
            return null;

        try {
            // Parse the original JSON from Agent 1
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(originalJson);

            // 1. Try to find 'transcript' (Listening)
            if (root.has("transcript")) {
                String transcript = root.get("transcript").asText();
                if (transcript != null && !transcript.isBlank()) {
                    return "## Transcript Context\n" + transcript;
                }
            }

            // 2. Try to find 'section.passage_text' (Reading)
            if (root.has("section") && root.get("section").has("passage_text")) {
                String passage = root.get("section").get("passage_text").asText();
                if (passage != null && !passage.isBlank()) {
                    return "## Passage Context\n" + passage;
                }
            }

            // 3. Try generic 'passage_text' at root
            if (root.has("passage_text")) {
                return "## Passage Context\n" + root.get("passage_text").asText();
            }

        } catch (Exception e) {
            logger.warn("Failed to extract context from original JSON: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Build the conversation prompt including original context for caching
     */
    private String buildConversationPrompt(RefinementRequestDTO request, String refinementPrompt) {
        StringBuilder prompt = new StringBuilder();

        // Include original prompt for context (will be cached by OpenRouter)
        if (request.getOriginalPrompt() != null) {
            prompt.append("## Original Generation Context\n");
            prompt.append("(The following was the original prompt used for generation)\n\n");
            prompt.append(request.getOriginalPrompt());
            prompt.append("\n\n---\n\n");
        }

        // Add refinement instructions
        prompt.append(refinementPrompt);

        return prompt.toString();
    }

    /**
     * Parse AI response to extract patches and apply them to the original JSON.
     * New patch-based approach - AI returns patches, we apply them.
     */
    private RefinementResponseDTO parseAndApplyPatches(
            String aiResponse,
            String originalJson,
            List<ValidationIssue> issues) {

        RefinementResponseDTO result = new RefinementResponseDTO();
        List<RefinementPatch> patches = new ArrayList<>();

        try {
            // Extract JSON from response (may be in code block)
            String patchJson = extractJsonFromResponse(aiResponse);
            if (patchJson == null) {
                result.setSuccess(false);
                result.setErrorMessage("Could not extract patches JSON from AI response");
                return result;
            }

            // Parse the patches object
            com.fasterxml.jackson.databind.JsonNode patchRoot = objectMapper.readTree(patchJson);
            com.fasterxml.jackson.databind.JsonNode patchesArray = patchRoot.get("patches");

            if (patchesArray == null || !patchesArray.isArray()) {
                result.setSuccess(false);
                result.setErrorMessage("AI response missing 'patches' array");
                return result;
            }

            // Convert to JsonPatcher.Patch objects
            List<JsonPatcher.Patch> patcherPatches = new ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode patchNode : patchesArray) {
                JsonPatcher.Patch patch = new JsonPatcher.Patch();
                patch.setIssueId(getTextOrNull(patchNode, "issueId"));
                patch.setQuestionNumber(getIntOrNull(patchNode, "questionNumber"));
                patch.setOperationFromString(getTextOrNull(patchNode, "operation"));
                patch.setPath(getTextOrNull(patchNode, "path"));
                patch.setIndex(getIntOrNull(patchNode, "index"));
                patch.setOldValue(getValueOrNull(patchNode, "oldValue"));

                // newValue can be complex object for insert operations
                com.fasterxml.jackson.databind.JsonNode newValueNode = patchNode.get("newValue");
                if (newValueNode != null) {
                    if (newValueNode.isTextual()) {
                        patch.setNewValue(newValueNode.asText());
                    } else if (newValueNode.isObject() || newValueNode.isArray()) {
                        // Keep as JsonNode for complex objects (insert)
                        patch.setNewValue(newValueNode);
                    } else if (newValueNode.isNumber()) {
                        patch.setNewValue(newValueNode.asInt());
                    } else {
                        patch.setNewValue(newValueNode.asText());
                    }
                }

                patch.setReason(getTextOrNull(patchNode, "reason"));
                patcherPatches.add(patch);

                // Also create RefinementPatch for response
                RefinementPatch refinementPatch = RefinementPatch.builder()
                        .issueId(patch.getIssueId())
                        .questionNumber(patch.getQuestionNumber())
                        .description(patch.getReason())
                        .build();
                patches.add(refinementPatch);
            }

            // Apply patches to original JSON
            JsonPatcher.PatchResult patchResult = jsonPatcher.applyPatches(originalJson, patcherPatches);

            result.setRefinedJson(patchResult.getPatchedJson());
            result.setPatches(patches);
            result.setSuccess(patchResult.getFailCount() == 0);

            // Extract summary if present
            com.fasterxml.jackson.databind.JsonNode summaryNode = patchRoot.get("summary");
            if (summaryNode != null && summaryNode.isTextual()) {
                logger.info("Refinement summary: {}", summaryNode.asText());
            }

            if (patchResult.getFailCount() > 0) {
                result.setErrorMessage(String.format("Applied %d patches, %d failed: %s",
                        patchResult.getSuccessCount(),
                        patchResult.getFailCount(),
                        String.join("; ", patchResult.getErrors())));
            }

            logger.info("Applied {} patches successfully, {} failed",
                    patchResult.getSuccessCount(), patchResult.getFailCount());

        } catch (Exception e) {
            logger.error("Failed to parse/apply patches: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage("Failed to process patches: " + e.getMessage());
        }

        return result;
    }

    /**
     * Extract JSON from AI response, handling code blocks.
     */
    private String extractJsonFromResponse(String response) {
        // Try to find JSON in code block first
        Pattern jsonPattern = Pattern.compile("```json\\s*\\n(.*?)\\n```", Pattern.DOTALL);
        Matcher jsonMatcher = jsonPattern.matcher(response);
        if (jsonMatcher.find()) {
            return jsonMatcher.group(1).trim();
        }

        // Try to find raw JSON
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }

        return null;
    }

    private String getTextOrNull(com.fasterxml.jackson.databind.JsonNode node, String field) {
        com.fasterxml.jackson.databind.JsonNode fieldNode = node.get(field);
        return (fieldNode != null && fieldNode.isTextual()) ? fieldNode.asText() : null;
    }

    private Integer getIntOrNull(com.fasterxml.jackson.databind.JsonNode node, String field) {
        com.fasterxml.jackson.databind.JsonNode fieldNode = node.get(field);
        return (fieldNode != null && fieldNode.isNumber()) ? fieldNode.asInt() : null;
    }

    private Object getValueOrNull(com.fasterxml.jackson.databind.JsonNode node, String field) {
        com.fasterxml.jackson.databind.JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull())
            return null;
        if (fieldNode.isTextual())
            return fieldNode.asText();
        if (fieldNode.isNumber())
            return fieldNode.asInt();
        if (fieldNode.isBoolean())
            return fieldNode.asBoolean();
        return fieldNode.asText(); // Fallback to text
    }

    /**
     * Send SSE event safely
     */
    private void sendEvent(SseEmitter emitter, StreamEventDTO event) {
        try {
            String eventName = event.getType() != null ? event.getType().name() : "UNKNOWN";
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(event));
        } catch (Exception e) {
            logger.debug("Failed to send SSE event: {}", e.getMessage());
        }
    }
}
