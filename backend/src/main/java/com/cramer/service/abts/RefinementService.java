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
    private final ObjectMapper objectMapper;

    public RefinementService(
            OpenRouterConfig config,
            OpenRouterClient openRouterClient,
            RefinementPromptBuilder refinementPromptBuilder,
            JsonValidatorService jsonValidatorService) {
        this.config = config;
        this.openRouterClient = openRouterClient;
        this.refinementPromptBuilder = refinementPromptBuilder;
        this.jsonValidatorService = jsonValidatorService;
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
                    extractPassageText(request.getOriginalPrompt()));

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

            // Use the same model as generation (configurable)
            String model = "google/gemini-3-flash-preview";

            openRouterClient.callChatCompletionStreaming(
                    model,
                    systemPrompt,
                    buildConversationPrompt(request, refinementPrompt),
                    null, // No JSON schema for refinement (we want free-form with JSON block)
                    Map.of(), // No reasoning config for refinement
                    0.3, // Lower temperature for precise fixes
                    32000, // Increased from 8192 to handle large JSON output
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

            // Parse response
            sendEvent(emitter, StreamEventDTO.progress(90, "Processing refinement result..."));
            RefinementResponseDTO response = parseRefinementResponse(fullResponse.toString(), selectedIssues);

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
     * Extract passage text from the original prompt
     */
    private String extractPassageText(String originalPrompt) {
        if (originalPrompt == null)
            return null;

        // Look for passage section
        int passageStart = originalPrompt.indexOf("## Passage");
        if (passageStart == -1) {
            passageStart = originalPrompt.indexOf("passage_text");
        }

        if (passageStart != -1) {
            int passageEnd = originalPrompt.indexOf("##", passageStart + 10);
            if (passageEnd == -1)
                passageEnd = originalPrompt.length();
            return originalPrompt.substring(passageStart, Math.min(passageEnd, passageStart + 5000));
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
     * Parse the AI response to extract refined JSON and patches
     */
    private RefinementResponseDTO parseRefinementResponse(String response, List<ValidationIssue> issues) {
        RefinementResponseDTO result = new RefinementResponseDTO();
        List<RefinementPatch> patches = new ArrayList<>();

        // Extract FIXES APPLIED section
        Pattern fixesPattern = Pattern.compile("FIXES APPLIED:\\s*\\n(.*?)\\n```", Pattern.DOTALL);
        Matcher fixesMatcher = fixesPattern.matcher(response);

        if (fixesMatcher.find()) {
            String fixesSection = fixesMatcher.group(1);
            // Parse each fix line
            String[] fixLines = fixesSection.split("\\n");
            for (int i = 0; i < fixLines.length; i++) {
                String line = fixLines[i].trim();
                if (line.startsWith("-") || line.startsWith("•")) {
                    RefinementPatch patch = RefinementPatch.builder()
                            .description(line.substring(1).trim())
                            .issueId(issues.size() > i ? issues.get(i).getId() : "unknown")
                            .build();

                    // Try to extract question number
                    Pattern qNumPattern = Pattern.compile("Q(\\d+)");
                    Matcher qNumMatcher = qNumPattern.matcher(line);
                    if (qNumMatcher.find()) {
                        patch.setQuestionNumber(Integer.parseInt(qNumMatcher.group(1)));
                    }

                    patches.add(patch);
                }
            }
        }

        // Extract JSON block
        Pattern jsonPattern = Pattern.compile("```json\\s*\\n(.*?)\\n```", Pattern.DOTALL);
        Matcher jsonMatcher = jsonPattern.matcher(response);

        String jsonContent = null;
        if (jsonMatcher.find()) {
            jsonContent = jsonMatcher.group(1).trim();
        } else {
            // Try to find raw JSON if no code block
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}");
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                jsonContent = response.substring(jsonStart, jsonEnd + 1);
            }
        }

        // Validate and clean JSON if found
        if (jsonContent != null) {
            try {
                // Parse to validate - this will throw if invalid
                objectMapper.readTree(jsonContent);
                result.setRefinedJson(jsonContent);
                result.setSuccess(true);
            } catch (Exception e) {
                logger.warn("Extracted JSON is invalid: {}", e.getMessage());
                result.setSuccess(false);
                result.setErrorMessage("AI returned invalid JSON: " + e.getMessage());
            }
        } else {
            result.setSuccess(false);
            result.setErrorMessage("Could not extract JSON from AI response");
        }

        result.setPatches(patches);
        return result;
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
