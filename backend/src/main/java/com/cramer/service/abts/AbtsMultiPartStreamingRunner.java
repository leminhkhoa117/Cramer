package com.cramer.service.abts;

import com.cramer.dto.abts.GeneratedContentDTO;
import com.cramer.dto.abts.GenerationRequestDTO;
import com.cramer.dto.abts.GenerationRequestDTO.PartConfigDTO;
import com.cramer.dto.abts.GenerationResponseDTO;
import com.cramer.dto.abts.GenerationResponseDTO.GenerationMetadataDTO;
import com.cramer.dto.abts.QuestionCountConfig;
import com.cramer.dto.abts.StreamEventDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class AbtsMultiPartStreamingRunner {

    private static final Logger logger = LoggerFactory.getLogger(AbtsMultiPartStreamingRunner.class);
    private static final int MAX_RETRIES = 3;

    private final OpenRouterClient openRouterClient;
    private final PromptBuilderService promptBuilderService;
    private final JsonValidatorService jsonValidatorService;
    private final ObjectMapper objectMapper;
    private final AbtsGenerationSupport generationSupport;
    private final AbtsGenerationRunner generationRunner;
    private final com.cramer.config.OpenRouterConfig openRouterConfig;

    AbtsMultiPartStreamingRunner(
            OpenRouterClient openRouterClient,
            PromptBuilderService promptBuilderService,
            JsonValidatorService jsonValidatorService,
            ObjectMapper objectMapper,
            AbtsGenerationSupport generationSupport,
            AbtsGenerationRunner generationRunner,
            com.cramer.config.OpenRouterConfig openRouterConfig) {
        this.openRouterClient = openRouterClient;
        this.promptBuilderService = promptBuilderService;
        this.jsonValidatorService = jsonValidatorService;
        this.objectMapper = objectMapper;
        this.generationSupport = generationSupport;
        this.generationRunner = generationRunner;
        this.openRouterConfig = openRouterConfig;
    }

    void generateMultiplePartsWithStream(GenerationRequestDTO request, SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled) throws IOException {
        List<Integer> parts = request.getPartsToGenerate();
        int totalParts = parts.size();

        logger.info("Starting multi-part generation for {} parts: {}", totalParts, parts);
        sendEvent(emitter, StreamEventDTO.progress(0,
                "Starting generation for " + totalParts + " parts", null, totalParts));

        List<GeneratedContentDTO> partContents = new ArrayList<>();
        // B2: track which part numbers actually succeeded so the merge maps content to the
        // correct part even when some parts were skipped.
        List<Integer> succeededPartNumbers = new ArrayList<>();
        // B2: collect per-part failure messages instead of aborting the whole run.
        Map<Integer, String> partErrors = new LinkedHashMap<>();
        List<String> allWarnings = new ArrayList<>();
        String combinedReasoning = "";
        long totalDurationMs = 0;
        // FIX 11: accumulate token usage + cost across all parts, not just duration.
        int totalPromptTokens = 0;
        int totalCompletionTokens = 0;
        int totalReasoningTokens = 0;
        double totalCostUsd = 0.0;
        // B1: monotonic progress — never emit a percentage lower than the previous one.
        int lastEmittedPct = 0;

        for (int index = 0; index < totalParts; index++) {
            int partNumber = parts.get(index);
            // B1: each part owns a contiguous slice of the global 0-100 progress bar.
            int outerStart = (index * 100) / totalParts;
            int outerEnd = ((index + 1) * 100) / totalParts;

            if (cancelled != null && cancelled.get()) {
                sendEvent(emitter, StreamEventDTO.aborted());
                emitter.complete();
                return;
            }

            lastEmittedPct = Math.max(lastEmittedPct, outerStart);
            sendEvent(emitter, StreamEventDTO.progress(lastEmittedPct,
                    "🔄 Starting Part " + partNumber + " (" + (index + 1) + "/" + totalParts + ")",
                    partNumber, totalParts));

            GenerationRequestDTO partRequest = buildPartRequest(request, partNumber);

            GenerationResponseDTO partResponse;
            try {
                partResponse = generateSinglePartWithStreamingTokens(
                        partRequest, emitter, cancelled, partNumber, totalParts, outerStart, outerEnd);
            } catch (Exception e) {
                // B4: a thrown exception during cancellation is an abort, not a failure.
                if (cancelled != null && cancelled.get()) {
                    sendEvent(emitter, StreamEventDTO.aborted());
                    emitter.complete();
                    return;
                }
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                logger.error("Error generating part {}: {}", partNumber, errorMsg, e);
                lastEmittedPct = recordPartFailure(emitter, partErrors, allWarnings,
                        partNumber, totalParts, errorMsg, lastEmittedPct, outerEnd);
                continue;
            }

            // B4: per-part methods return null when cancelled mid-flight.
            if (partResponse == null && cancelled != null && cancelled.get()) {
                sendEvent(emitter, StreamEventDTO.aborted());
                emitter.complete();
                return;
            }

            // B2: a null or FAILED response is a part-level failure — record it and keep going.
            if (partResponse == null
                    || partResponse.getStatus() == GenerationResponseDTO.GenerationStatus.FAILED) {
                String errorMsg = "Unknown error";
                if (partResponse != null && partResponse.getErrors() != null
                        && !partResponse.getErrors().isEmpty()) {
                    errorMsg = partResponse.getErrors().get(0);
                } else if (partResponse == null) {
                    errorMsg = "Part returned no content";
                }
                // FIX 7: a failed part may still have consumed tokens (e.g. it produced
                // output that later failed validation). Roll those costs up so billing
                // and usage metrics reflect real consumption, not just successful parts.
                if (partResponse != null && partResponse.getMetadata() != null) {
                    GenerationMetadataDTO failedMeta = partResponse.getMetadata();
                    if (failedMeta.getPromptTokens() != null) {
                        totalPromptTokens += failedMeta.getPromptTokens();
                    }
                    if (failedMeta.getCompletionTokens() != null) {
                        totalCompletionTokens += failedMeta.getCompletionTokens();
                    }
                    if (failedMeta.getReasoningTokens() != null) {
                        totalReasoningTokens += failedMeta.getReasoningTokens();
                    }
                    if (failedMeta.getEstimatedCostUsd() != null) {
                        totalCostUsd += failedMeta.getEstimatedCostUsd();
                    }
                }
                lastEmittedPct = recordPartFailure(emitter, partErrors, allWarnings,
                        partNumber, totalParts, errorMsg, lastEmittedPct, outerEnd);
                continue;
            }

            GeneratedContentDTO content = partResponse.getContent();
            if (content != null) {
                renumberQuestionsInContent(content, partNumber, request.getSkill());
                partContents.add(content);
                succeededPartNumbers.add(partNumber);
            }

            if (partResponse.getWarnings() != null) {
                allWarnings.addAll(partResponse.getWarnings());
            }

            if (partResponse.getReasoning() != null) {
                combinedReasoning += "\n\n--- Part " + partNumber + " Reasoning ---\n"
                        + partResponse.getReasoning();
            }

            if (partResponse.getMetadata() != null) {
                GenerationMetadataDTO partMeta = partResponse.getMetadata();
                totalDurationMs += partMeta.getGenerationTimeSeconds() != null
                        ? (long) (partMeta.getGenerationTimeSeconds() * 1000)
                        : 0;
                // FIX 11: roll up token + cost figures from each part.
                if (partMeta.getPromptTokens() != null) {
                    totalPromptTokens += partMeta.getPromptTokens();
                }
                if (partMeta.getCompletionTokens() != null) {
                    totalCompletionTokens += partMeta.getCompletionTokens();
                }
                if (partMeta.getReasoningTokens() != null) {
                    totalReasoningTokens += partMeta.getReasoningTokens();
                }
                if (partMeta.getEstimatedCostUsd() != null) {
                    totalCostUsd += partMeta.getEstimatedCostUsd();
                }
            }

            lastEmittedPct = Math.max(lastEmittedPct, outerEnd);
            sendEvent(emitter, StreamEventDTO.progress(lastEmittedPct,
                    "✅ Part " + partNumber + " completed", partNumber, totalParts));
        }

        // B2: only abort the whole run when EVERY part failed; otherwise return a partial result.
        if (partContents.isEmpty()) {
            StringBuilder summary = new StringBuilder();
            for (Map.Entry<Integer, String> entry : partErrors.entrySet()) {
                if (summary.length() > 0) {
                    summary.append("; ");
                }
                summary.append("Part ").append(entry.getKey()).append(": ").append(entry.getValue());
            }
            sendEvent(emitter, StreamEventDTO.failedWithData("All parts failed: " + summary, partErrors));
            emitter.complete();
            logger.error("Multi-part generation failed: all {} parts failed", totalParts);
            return;
        }

        GenerationResponseDTO combinedResponse = new GenerationResponseDTO();
        // FIX 8: only genuine per-part failures downgrade the run to PARTIAL_SUCCESS;
        // validation warnings on otherwise-successful parts keep the run SUCCESS.
        combinedResponse.setStatus(partErrors.isEmpty()
                ? GenerationResponseDTO.GenerationStatus.SUCCESS
                : GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);

        // B2: merge only successful parts, keyed by their real part numbers.
        GeneratedContentDTO mergedContent = mergePartContents(partContents, succeededPartNumbers);
        combinedResponse.setContent(mergedContent);
        combinedResponse.setReasoning(combinedReasoning.trim());

        if (!allWarnings.isEmpty()) {
            combinedResponse.setWarnings(allWarnings);
        }
        // B2: expose which parts failed so the caller/UI can surface them.
        if (!partErrors.isEmpty()) {
            combinedResponse.setPartErrors(partErrors);
        }

        GenerationMetadataDTO metadata = new GenerationMetadataDTO();
        metadata.setGenerationTimeSeconds(totalDurationMs / 1000.0);
        metadata.setGeneratedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        metadata.setTopic(request.getTopic());
        // FIX 11: surface aggregated token usage + cost on the combined response.
        metadata.setPromptTokens(totalPromptTokens);
        metadata.setCompletionTokens(totalCompletionTokens);
        metadata.setReasoningTokens(totalReasoningTokens);
        metadata.setEstimatedCostUsd(Math.round(totalCostUsd * 10000.0) / 10000.0);
        combinedResponse.setMetadata(metadata);

        sendEvent(emitter, StreamEventDTO.completed(combinedResponse));
        emitter.complete();
        logger.info("Multi-part generation completed: {} succeeded, {} failed (of {} parts)",
                succeededPartNumbers.size(), partErrors.size(), totalParts);
    }

    /**
     * B2: record a single part's failure (without aborting the run), emit a
     * monotonic "continuing" progress event, and return the new high-water mark.
     */
    private int recordPartFailure(SseEmitter emitter, Map<Integer, String> partErrors,
            List<String> allWarnings, int partNumber, int totalParts, String errorMsg,
            int lastEmittedPct, int outerEnd) {
        partErrors.put(partNumber, errorMsg);
        allWarnings.add("Part " + partNumber + " failed (continuing with remaining parts): " + errorMsg);
        int next = Math.max(lastEmittedPct, outerEnd);
        sendEvent(emitter, StreamEventDTO.progress(next,
                "⚠️ Part " + partNumber + " failed; continuing...", partNumber, totalParts));
        return next;
    }

    /**
     * B1: scale a per-part local progress percentage (0-100) into the part's global
     * window [outerStart, outerEnd], clamped to that window.
     */
    private static int scale(int innerPct, int outerStart, int outerEnd) {
        int v = outerStart + (innerPct * (outerEnd - outerStart) / 100);
        if (v < outerStart) {
            v = outerStart;
        }
        if (v > outerEnd) {
            v = outerEnd;
        }
        return v;
    }

    private GenerationRequestDTO buildPartRequest(GenerationRequestDTO originalRequest, int partNumber) {
        GenerationRequestDTO partRequest = new GenerationRequestDTO();

        partRequest.setSkill(originalRequest.getSkill());
        partRequest.setScope(GenerationRequestDTO.GenerationScope.SINGLE_PART);
        partRequest.setPartNumber(partNumber);
        partRequest.setDifficulty(originalRequest.getDifficulty());
        partRequest.setExplanationLanguage(originalRequest.getExplanationLanguage());
        partRequest.setHashtags(originalRequest.getHashtags());
        partRequest.setWordCountRange(originalRequest.getWordCountRange());
        partRequest.setTestType(originalRequest.getTestType());
        partRequest.setModel(originalRequest.getModel());
        partRequest.setModelVariant(originalRequest.getModelVariant());
        partRequest.setEnableReasoning(originalRequest.getEnableReasoning());
        partRequest.setReasoningEffort(originalRequest.getReasoningEffort());
        partRequest.setTemperature(originalRequest.getTemperature());
        partRequest.setMaxTokens(originalRequest.getMaxTokens());
        partRequest.setEnableWebSearch(originalRequest.getEnableWebSearch());
        partRequest.setEnableContextCaching(originalRequest.getEnableContextCaching());
        partRequest.setPassageLength(originalRequest.getPassageLength());
        partRequest.setCustomInstructions(originalRequest.getCustomInstructions());
        partRequest.setWritingEssayType(originalRequest.getWritingEssayType());

        Map<Integer, PartConfigDTO> partConfigs = originalRequest.getPartConfigs();
        if (partConfigs != null && partConfigs.containsKey(partNumber)) {
            PartConfigDTO config = partConfigs.get(partNumber);
            partRequest.setTopic(config.getTopic() != null ? config.getTopic() : originalRequest.getTopic());
            partRequest.setFacts(config.getFacts() != null ? config.getFacts() : originalRequest.getFacts());
            partRequest.setQuestionTypes(config.getQuestionTypes() != null
                    ? config.getQuestionTypes()
                    : originalRequest.getQuestionTypes());
        } else {
            partRequest.setTopic(originalRequest.getTopic());
            partRequest.setFacts(originalRequest.getFacts());
            partRequest.setQuestionTypes(originalRequest.getQuestionTypes());
        }

        return partRequest;
    }

    private GenerationResponseDTO generateSinglePartWithStreamingTokens(
            GenerationRequestDTO request,
            SseEmitter parentEmitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled,
            int partNumber,
            int totalParts,
            int outerStart,
            int outerEnd) throws Exception {

        try {
            sendEvent(parentEmitter, StreamEventDTO.progress(scale(0, outerStart, outerEnd),
                    "⏳ Part " + partNumber + ": Building prompt...", partNumber, totalParts));

            return switch (request.getSkill()) {
                case READING -> generateReadingForPart(request, parentEmitter, cancelled,
                        partNumber, totalParts, outerStart, outerEnd);
                case LISTENING -> generateListeningForPart(request, parentEmitter, cancelled,
                        partNumber, totalParts, outerStart, outerEnd);
                case WRITING -> generateWritingForPart(request, parentEmitter, cancelled,
                        partNumber, totalParts, outerStart, outerEnd);
                default -> GenerationResponseDTO.error("UNSUPPORTED_SKILL",
                        "Skill not supported: " + request.getSkill(), false);
            };
        } catch (Exception e) {
            logger.error("Error in generateSinglePartWithStreamingTokens: {}", e.getMessage(), e);
            throw e;
        }
    }

    GenerationResponseDTO generateReadingForPart(
            GenerationRequestDTO request,
            SseEmitter parentEmitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled,
            int partNumber,
            int totalParts,
            int outerStart,
            int outerEnd) throws Exception {

        String lastError = null;
        // V10: soft per-part timeout — bail this part (recorded as failed by caller) if exceeded.
        final long perPartDeadline = System.currentTimeMillis() + openRouterConfig.getPerPartTimeoutMs();

        for (int attempts = 1; attempts <= MAX_RETRIES; attempts++) {
            if (cancelled != null && cancelled.get()) {
                return null;
            }

            if (System.currentTimeMillis() > perPartDeadline) {
                throw new PartTimeoutException("[TIMEOUT] Part " + partNumber
                        + " exceeded per-part timeout of "
                        + openRouterConfig.getPerPartTimeoutMs() + "ms");
            }

            try {
                Map<String, Object> reasoningConfig = generationSupport.buildReasoningConfig(request);

                sendEvent(parentEmitter, StreamEventDTO.progress(scale(5, outerStart, outerEnd),
                    "📝 Part " + partNumber + " (Phase 1/2): Generating Passage...", partNumber, totalParts));

                String passagePrompt = promptBuilderService.buildReadingPassagePrompt(request);
                Map<String, Object> passageSchema = promptBuilderService.getReadingPassageSchema();

                OpenRouterClient.OpenRouterResponse passageResponse = performStreamingCall(
                        request, passagePrompt, passageSchema, reasoningConfig,
                        parentEmitter, cancelled, scale(5, outerStart, outerEnd),
                        scale(45, outerStart, outerEnd), "Passage", partNumber, totalParts);

                if (passageResponse == null) {
                    return null;
                }

                JsonNode passageRoot = objectMapper.readTree(passageResponse.getContent());
                String passageText = passageRoot.has("passage_text")
                        ? passageRoot.get("passage_text").asText()
                        : "";
                int wordCount = passageRoot.has("word_count")
                        ? passageRoot.get("word_count").asInt()
                        : 0;

                if (passageText.isBlank()) {
                    throw new RuntimeException("AI failed to generate a valid passage in Phase 1");
                }

                logger.info("Phase 1 complete: Generated passage with {} words", wordCount);

                assertWithinDeadline(perPartDeadline, partNumber);

                sendEvent(parentEmitter, StreamEventDTO.progress(scale(50, outerStart, outerEnd),
                    "❓ Part " + partNumber + " (Phase 2/2): Generating Questions...", partNumber, totalParts));

                String questionsPrompt = promptBuilderService.buildReadingQuestionsPrompt(request, passageText);
                Map<String, Object> questionsSchema = promptBuilderService.getReadingQuestionsSchema();

                OpenRouterClient.OpenRouterResponse questionsResponse = performStreamingCall(
                        request, questionsPrompt, questionsSchema, reasoningConfig,
                        parentEmitter, cancelled, scale(50, outerStart, outerEnd),
                        scale(85, outerStart, outerEnd), "Questions", partNumber, totalParts);

                if (questionsResponse == null) {
                    return null;
                }

                sendEvent(parentEmitter, StreamEventDTO.progress(scale(90, outerStart, outerEnd),
                    "🔍 Part " + partNumber + ": Validating...", partNumber, totalParts));

                JsonNode questionsRoot = objectMapper.readTree(questionsResponse.getContent());

                if (questionsRoot instanceof ObjectNode mergedRoot) {
                    ObjectNode sectionNode = objectMapper.createObjectNode();
                    sectionNode.put("passage_text", passageText);
                    sectionNode.put("word_count", wordCount);
                    mergedRoot.set("section", sectionNode);

                    String mergedJson = objectMapper.writeValueAsString(mergedRoot);

                    JsonValidatorService.ValidationResult validationResult = jsonValidatorService
                            .validateReadingContent(mergedJson, request);

                    if (!validationResult.isValid() && !validationResult.getSchemaErrors().isEmpty()) {
                        lastError = String.join("; ", validationResult.getAllErrors());
                        if (attempts < MAX_RETRIES) {
                            logger.warn("Validation failed on attempt {}, retrying: {}", attempts, lastError);
                            continue;
                        }
                    }

                    GeneratedContentDTO content = jsonValidatorService.parseGeneratedContent(mergedJson);

                    GenerationResponseDTO response = new GenerationResponseDTO();
                    response.setStatus(validationResult.isValid()
                            ? GenerationResponseDTO.GenerationStatus.SUCCESS
                            : GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                    response.setContent(content);
                    response.setReasoning(passageResponse.getReasoning() + "\n\n---\n\n"
                            + questionsResponse.getReasoning());
                    response.setValidation(generationSupport.buildValidationDto(validationResult));

                    List<String> allWarnings = new ArrayList<>(validationResult.getWarnings());
                    if (!validationResult.isValid()) {
                        allWarnings.addAll(validationResult.getAllErrors());
                    }
                    if (!allWarnings.isEmpty()) {
                        response.setWarnings(allWarnings);
                    }

                    // FIX 10: aggregate token usage across both reading phases (passage + questions).
                    OpenRouterClient.OpenRouterResponse aggregatedUsage = aggregateUsage(
                            questionsResponse, response.getReasoning(), passageResponse, questionsResponse);
                    GenerationMetadataDTO metadata = generationSupport.buildMetadata(request, aggregatedUsage, content);
                    response.setMetadata(metadata);

                    return response;
                } else {
                    throw new RuntimeException("Phase 2 output was not a JSON object");
                }

            } catch (Exception e) {
                lastError = e.getMessage();
                logger.error("Error generating reading for part {} on attempt {}: {}",
                        partNumber, attempts, lastError);
                if (attempts >= MAX_RETRIES) {
                    throw e;
                }
            }
        }

        return GenerationResponseDTO.error("GENERATION_FAILED", lastError, true);
    }

    GenerationResponseDTO generateListeningForPart(
            GenerationRequestDTO request,
            SseEmitter parentEmitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled,
            int partNumber,
            int totalParts,
            int outerStart,
            int outerEnd) throws Exception {

        String lastError = null;
        // V10: soft per-part timeout — bail this part (recorded as failed by caller) if exceeded.
        final long perPartDeadline = System.currentTimeMillis() + openRouterConfig.getPerPartTimeoutMs();

        // FIX 9: cache successful phases OUTSIDE the retry loop so that a later-phase
        // failure only re-runs the failing phase instead of regenerating everything.
        OpenRouterClient.OpenRouterResponse transcriptResponse = null;
        OpenRouterClient.OpenRouterResponse questionsResponse = null;
        String transcriptText = null;
        JsonNode audioPlaceholder = null;
        ObjectNode mergedRoot = null;
        JsonNode questionsNode = null;

        Map<String, Object> reasoningConfig = generationSupport.buildReasoningConfig(request);

        for (int attempts = 1; attempts <= MAX_RETRIES; attempts++) {
            if (cancelled != null && cancelled.get()) {
                return null;
            }

            if (System.currentTimeMillis() > perPartDeadline) {
                throw new PartTimeoutException("[TIMEOUT] Part " + partNumber
                        + " exceeded per-part timeout of "
                        + openRouterConfig.getPerPartTimeoutMs() + "ms");
            }

            // FIX 8: surface retry attempts to the client.
            if (attempts > 1) {
                sendEvent(parentEmitter, StreamEventDTO.retry(attempts, MAX_RETRIES,
                        lastError != null ? lastError : "previous attempt failed"));
            }

            try {
                // === PHASE 1/3: Transcript (cached across retries) ===
                if (transcriptResponse == null) {
                    sendEvent(parentEmitter, StreamEventDTO.progress(scale(5, outerStart, outerEnd),
                        "📝 Part " + partNumber + " (Phase 1/3): Generating Transcript...", partNumber, totalParts));

                    String transcriptPrompt = promptBuilderService.buildListeningTranscriptPrompt(request);
                    Map<String, Object> transcriptSchema = promptBuilderService.getListeningTranscriptSchema();

                    transcriptResponse = performStreamingCall(
                            request, transcriptPrompt, transcriptSchema, reasoningConfig,
                            parentEmitter, cancelled,
                            scale(5, outerStart, outerEnd), scale(30, outerStart, outerEnd),
                            "Transcript",
                            generationSupport.resolveMaxTokens(request, 16384), partNumber, totalParts);

                    if (transcriptResponse == null)
                        return null;

                    JsonNode transcriptRoot = objectMapper.readTree(transcriptResponse.getContent());
                    transcriptText = transcriptRoot.has("transcript")
                            ? transcriptRoot.get("transcript").asText() : "";
                    audioPlaceholder = transcriptRoot.get("audio_placeholder");

                    if (transcriptText == null || transcriptText.isBlank()) {
                        transcriptResponse = null; // force regeneration on next attempt
                        throw new RuntimeException("AI failed to generate a valid transcript in Phase 1");
                    }
                }

                // === PHASE 2/3: Questions (cached across retries) ===
                if (questionsResponse == null) {
                    assertWithinDeadline(perPartDeadline, partNumber);
                    sendEvent(parentEmitter, StreamEventDTO.progress(scale(30, outerStart, outerEnd),
                        "❓ Part " + partNumber + " (Phase 2/3): Generating Questions...", partNumber, totalParts));

                    String questionsPrompt = promptBuilderService.buildListeningQuestionsPrompt(request, transcriptText);
                    Map<String, Object> questionsSchema = promptBuilderService.getListeningQuestionsSchema();

                    questionsResponse = performStreamingCall(
                            request, questionsPrompt, questionsSchema, reasoningConfig,
                            parentEmitter, cancelled,
                            scale(30, outerStart, outerEnd), scale(60, outerStart, outerEnd),
                            "Questions", partNumber, totalParts);

                    if (questionsResponse == null)
                        return null;

                    JsonNode questionsRoot = objectMapper.readTree(questionsResponse.getContent());

                    if (!(questionsRoot instanceof ObjectNode qRoot)) {
                        questionsResponse = null;
                        throw new RuntimeException("Phase 2 output was not a JSON object");
                    }

                    JsonNode qNode = qRoot.get("questions");
                    if (qNode == null || !qNode.isArray()) {
                        questionsResponse = null;
                        throw new RuntimeException("Phase 2 output missing 'questions' array");
                    }
                    // Item 17 (null safety): fail fast if Phase 2 produced no questions,
                    // otherwise Phase 3 would be asked to answer an empty question set.
                    if (qNode.isEmpty()) {
                        questionsResponse = null;
                        throw new RuntimeException(
                                "Phase 2 produced zero questions; aborting before Phase 3 (nothing to answer)");
                    }
                    mergedRoot = qRoot;
                    questionsNode = qNode;
                }

                // Collects non-fatal merge anomalies (duplicate / extra answers) for the response.
                List<String> mergeWarnings = new ArrayList<>();

                // === PHASE 3/3: Answers + Explanations (always re-run on retry) ===
                assertWithinDeadline(perPartDeadline, partNumber);
                sendEvent(parentEmitter, StreamEventDTO.progress(scale(60, outerStart, outerEnd),
                    "🔑 Part " + partNumber + " (Phase 3/3): Generating Answers...", partNumber, totalParts));

                String questionsJson = objectMapper.writeValueAsString(questionsNode);
                String answersPrompt = promptBuilderService.buildListeningAnswersPrompt(
                        request, transcriptText, questionsJson);
                Map<String, Object> answersSchema = promptBuilderService.getListeningAnswersSchema();

                OpenRouterClient.OpenRouterResponse answersResponse = performStreamingCall(
                        request, answersPrompt, answersSchema, reasoningConfig,
                        parentEmitter, cancelled,
                        scale(60, outerStart, outerEnd), scale(88, outerStart, outerEnd),
                        "Answers",
                        generationSupport.resolveMaxTokens(request, 16384), partNumber, totalParts);

                if (answersResponse == null)
                    return null;

                JsonNode answersRoot = objectMapper.readTree(answersResponse.getContent());
                JsonNode answersArray = answersRoot.get("answers");
                if (answersArray == null || !answersArray.isArray()) {
                    throw new RuntimeException("Phase 3 output missing 'answers' array");
                }

                // Index answers by question_number for merge.
                // Item 14: warn (not fail) on duplicate answers; keep the last occurrence.
                Map<Integer, JsonNode> answersByNumber = new java.util.HashMap<>();
                for (JsonNode answer : answersArray) {
                    if (answer.has("question_number")) {
                        int answerNum = answer.get("question_number").asInt();
                        if (answersByNumber.containsKey(answerNum)) {
                            String dupWarning = "Phase 3 returned duplicate answer for question " + answerNum
                                    + "; keeping last occurrence.";
                            logger.warn("Part {}: {}", partNumber, dupWarning);
                            mergeWarnings.add(dupWarning);
                        }
                        answersByNumber.put(answerNum, answer);
                    }
                }

                // Item 14: detect extra answers that do not map to any Phase 2 question; drop with a warning.
                java.util.Set<Integer> questionNumbers = new java.util.HashSet<>();
                for (JsonNode q : questionsNode) {
                    if (q.has("question_number")) {
                        questionNumbers.add(q.get("question_number").asInt());
                    }
                }
                for (Integer answerNum : answersByNumber.keySet()) {
                    if (!questionNumbers.contains(answerNum)) {
                        String extraWarning = "Phase 3 returned an answer for question " + answerNum
                                + " with no matching Phase 2 question; dropping it.";
                        logger.warn("Part {}: {}", partNumber, extraWarning);
                        mergeWarnings.add(extraWarning);
                    }
                }

                // Merge correct_answer + explanation + evidence into each question; fail if any missing.
                List<Integer> missingAnswers = new ArrayList<>();
                List<String> cardinalityErrors = new ArrayList<>();
                for (JsonNode q : questionsNode) {
                    if (!(q instanceof ObjectNode questionObj) || !q.has("question_number")) {
                        continue;
                    }
                    int qNum = q.get("question_number").asInt();
                    JsonNode answer = answersByNumber.get(qNum);
                    if (answer == null) {
                        missingAnswers.add(qNum);
                        continue;
                    }
                    if (answer.has("correct_answer")) {
                        JsonNode correct = answer.get("correct_answer");
                        questionObj.set("correct_answer", correct);
                        // FIX 3: validate answer cardinality for the question type.
                        String qType = questionObj.has("question_type")
                                ? questionObj.get("question_type").asText() : "";
                        String cardErr = validateAnswerCardinality(qNum, qType, correct);
                        if (cardErr != null) {
                            cardinalityErrors.add(cardErr);
                        }
                    } else {
                        missingAnswers.add(qNum);
                    }
                    // FIX 3: strip any Phase-2-leaked explanation when Phase 3 omits it,
                    // so a stale/duplicate explanation can never survive the merge.
                    if (answer.has("explanation")) {
                        questionObj.set("explanation", answer.get("explanation"));
                    } else {
                        questionObj.remove("explanation");
                    }
                    // FIX 15: persist supporting transcript evidence onto the question.
                    if (answer.has("evidence_from_transcript")) {
                        questionObj.set("evidence_from_transcript", answer.get("evidence_from_transcript"));
                    }
                }

                if (!missingAnswers.isEmpty()) {
                    lastError = "Phase 3 did not produce answers for questions: " + missingAnswers;
                    logger.warn("Part {}: {}", partNumber, lastError);
                    if (attempts < MAX_RETRIES)
                        continue;
                    throw new RuntimeException(lastError);
                }

                // FIX 3: a cardinality mismatch is a hard error (retry, then fail).
                if (!cardinalityErrors.isEmpty()) {
                    lastError = "Answer cardinality mismatch: " + String.join("; ", cardinalityErrors);
                    logger.warn("Part {}: {}", partNumber, lastError);
                    if (attempts < MAX_RETRIES)
                        continue;
                    throw new RuntimeException(lastError);
                }

                sendEvent(parentEmitter, StreamEventDTO.progress(scale(90, outerStart, outerEnd),
                    "🔍 Part " + partNumber + ": Validate & Formatting...", partNumber, totalParts));

                mergedRoot.put("transcript", transcriptText);
                if (audioPlaceholder != null)
                    mergedRoot.set("audio_placeholder", audioPlaceholder);

                String mergedContentJson = objectMapper.writeValueAsString(mergedRoot);

                JsonValidatorService.ValidationResult validationResult = jsonValidatorService
                        .validateListeningContent(mergedContentJson, request);

                if (!validationResult.isValid() && !validationResult.getSchemaErrors().isEmpty()) {
                    lastError = String.join("; ", validationResult.getAllErrors());
                    logger.warn("Validation failed for Part {}: {}", partNumber, lastError);
                    if (attempts < MAX_RETRIES)
                        continue;
                }

                GeneratedContentDTO content = jsonValidatorService.parseGeneratedContent(mergedContentJson);

                GenerationResponseDTO response = new GenerationResponseDTO();
                response.setStatus(validationResult.isValid()
                        ? GenerationResponseDTO.GenerationStatus.SUCCESS
                        : GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                response.setContent(content);

                String fullReasoning = "--- PHASE 1 (TRANSCRIPT) ---\n" +
                        (transcriptResponse.getReasoning() != null ? transcriptResponse.getReasoning() : "N/A") +
                        "\n\n--- PHASE 2 (QUESTIONS) ---\n" +
                        (questionsResponse.getReasoning() != null ? questionsResponse.getReasoning() : "N/A") +
                        "\n\n--- PHASE 3 (ANSWERS) ---\n" +
                        (answersResponse.getReasoning() != null ? answersResponse.getReasoning() : "N/A");
                response.setReasoning(fullReasoning);

                response.setValidation(generationSupport.buildValidationDto(validationResult));

                List<String> allWarnings = new ArrayList<>(validationResult.getWarnings());
                allWarnings.addAll(mergeWarnings);
                if (!validationResult.isValid()) {
                    allWarnings.addAll(validationResult.getAllErrors());
                }
                if (!allWarnings.isEmpty()) {
                    response.setWarnings(allWarnings);
                }

                // FIX 10: aggregate token usage across all three phases so the reported
                // cost reflects the full multi-phase generation, not just Phase 3.
                OpenRouterClient.OpenRouterResponse aggregatedUsage = aggregateUsage(
                        answersResponse, fullReasoning,
                        transcriptResponse, questionsResponse, answersResponse);
                GenerationMetadataDTO metadata = generationSupport.buildMetadata(request, aggregatedUsage, content);
                response.setMetadata(metadata);

                return response;

            } catch (Exception e) {
                lastError = e.getMessage();
                logger.error("Error generating listening for part {} on attempt {}: {}", partNumber, attempts,
                        lastError);
                if (attempts >= MAX_RETRIES)
                    throw e;
            }
        }

        return GenerationResponseDTO.error("GENERATION_FAILED", lastError, true);
    }

    GenerationResponseDTO generateWritingForPart(
            GenerationRequestDTO request,
            SseEmitter parentEmitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled,
            int partNumber,
            int totalParts,
            int outerStart,
            int outerEnd) throws Exception {

        String lastError = null;
        // V10: soft per-part timeout — bail this part (recorded as failed by caller) if exceeded.
        final long perPartDeadline = System.currentTimeMillis() + openRouterConfig.getPerPartTimeoutMs();

        // FIX 9: cache successful phases OUTSIDE the retry loop so a Phase 3 failure
        // does not regenerate the task and sample answer.
        OpenRouterClient.OpenRouterResponse taskResponse = null;
        OpenRouterClient.OpenRouterResponse sampleResponse = null;
        ObjectNode mergedRoot = null;
        String taskJson = null;
        String sampleJson = null;

        Map<String, Object> reasoningConfig = generationSupport.buildReasoningConfig(request);

        for (int attempts = 1; attempts <= MAX_RETRIES; attempts++) {
            if (cancelled != null && cancelled.get()) {
                return null;
            }

            if (System.currentTimeMillis() > perPartDeadline) {
                throw new PartTimeoutException("[TIMEOUT] Part " + partNumber
                        + " exceeded per-part timeout of "
                        + openRouterConfig.getPerPartTimeoutMs() + "ms");
            }

            // FIX 8: surface retry attempts to the client.
            if (attempts > 1) {
                sendEvent(parentEmitter, StreamEventDTO.retry(attempts, MAX_RETRIES,
                        lastError != null ? lastError : "previous attempt failed"));
            }

            try {
                // === PHASE 1/3: Task (cached across retries) ===
                if (taskResponse == null) {
                    sendEvent(parentEmitter, StreamEventDTO.progress(scale(5, outerStart, outerEnd),
                        "📝 Part " + partNumber + " (Phase 1/3): Generating Task...", partNumber, totalParts));

                    String taskPrompt = promptBuilderService.buildWritingTaskPrompt(request);
                    Map<String, Object> taskSchema = promptBuilderService.getWritingTaskSchema();

                    taskResponse = performStreamingCall(
                            request, taskPrompt, taskSchema, reasoningConfig,
                            parentEmitter, cancelled, scale(5, outerStart, outerEnd),
                            scale(35, outerStart, outerEnd), "Task", partNumber, totalParts);

                    if (taskResponse == null)
                        return null;

                    JsonNode taskRoot = objectMapper.readTree(taskResponse.getContent());
                    if (!(taskRoot instanceof ObjectNode tRoot)) {
                        taskResponse = null;
                        throw new RuntimeException("Phase 1 output was not a JSON object");
                    }
                    if (!tRoot.has("task_prompt") || tRoot.get("task_prompt").asText().isBlank()) {
                        taskResponse = null;
                        throw new RuntimeException("AI failed to generate a valid task in Phase 1");
                    }
                    mergedRoot = tRoot;
                    taskJson = objectMapper.writeValueAsString(mergedRoot);
                }

                // === PHASE 2/3: Sample Answer (cached across retries) ===
                if (sampleResponse == null) {
                    assertWithinDeadline(perPartDeadline, partNumber);
                    sendEvent(parentEmitter, StreamEventDTO.progress(scale(35, outerStart, outerEnd),
                        "✍️ Part " + partNumber + " (Phase 2/3): Writing Sample Answer...", partNumber, totalParts));

                    String samplePrompt = promptBuilderService.buildWritingSamplePrompt(request, taskJson);
                    Map<String, Object> sampleSchema = promptBuilderService.getWritingSampleSchema();

                    sampleResponse = performStreamingCall(
                            request, samplePrompt, sampleSchema, reasoningConfig,
                            parentEmitter, cancelled, scale(35, outerStart, outerEnd),
                            scale(70, outerStart, outerEnd), "Sample",
                            generationSupport.resolveMaxTokens(request, 16384), partNumber, totalParts);

                    if (sampleResponse == null)
                        return null;

                    JsonNode sampleRoot = objectMapper.readTree(sampleResponse.getContent());
                    JsonNode sampleAnswer = sampleRoot.get("sample_answer");
                    if (sampleAnswer == null || !sampleAnswer.isObject()) {
                        sampleResponse = null;
                        throw new RuntimeException("Phase 2 output missing 'sample_answer' object");
                    }
                    mergedRoot.set("sample_answer", sampleAnswer);
                    sampleJson = objectMapper.writeValueAsString(sampleRoot);
                }

                // === PHASE 3/3: Band Breakdown (always re-run on retry) ===
                assertWithinDeadline(perPartDeadline, partNumber);
                sendEvent(parentEmitter, StreamEventDTO.progress(scale(70, outerStart, outerEnd),
                    "🎯 Part " + partNumber + " (Phase 3/3): Grading Sample...", partNumber, totalParts));

                String bandPrompt = promptBuilderService.buildWritingBandPrompt(request, taskJson, sampleJson);
                Map<String, Object> bandSchema = promptBuilderService.getWritingBandSchema();

                OpenRouterClient.OpenRouterResponse bandResponse = performStreamingCall(
                        request, bandPrompt, bandSchema, reasoningConfig,
                        parentEmitter, cancelled, scale(70, outerStart, outerEnd),
                        scale(90, outerStart, outerEnd), "Band",
                        generationSupport.resolveMaxTokens(request, 16384), partNumber, totalParts);

                if (bandResponse == null)
                    return null;

                JsonNode bandRoot = objectMapper.readTree(bandResponse.getContent());
                if (bandRoot.has("band_breakdown")) {
                    mergedRoot.set("band_breakdown", bandRoot.get("band_breakdown"));
                }
                if (bandRoot.has("key_phrases")) {
                    mergedRoot.set("key_phrases", bandRoot.get("key_phrases"));
                }
                if (bandRoot.has("grading_notes")) {
                    mergedRoot.set("grading_notes", bandRoot.get("grading_notes"));
                }

                sendEvent(parentEmitter, StreamEventDTO.progress(scale(90, outerStart, outerEnd),
                    "🔍 Part " + partNumber + ": Validating...", partNumber, totalParts));

                String mergedContentJson = objectMapper.writeValueAsString(mergedRoot);

                JsonValidatorService.ValidationResult validationResult = jsonValidatorService
                        .validateWritingContent(mergedContentJson, request);

                if (!validationResult.isValid() && !validationResult.getSchemaErrors().isEmpty()) {
                    lastError = String.join("; ", validationResult.getAllErrors());
                    logger.warn("Validation failed for Writing Part {}: {}", partNumber, lastError);
                    if (attempts < MAX_RETRIES)
                        continue;
                }

                GeneratedContentDTO content = jsonValidatorService.parseGeneratedContent(mergedContentJson);

                GenerationResponseDTO response = new GenerationResponseDTO();
                response.setStatus(validationResult.isValid()
                        ? GenerationResponseDTO.GenerationStatus.SUCCESS
                        : GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                response.setContent(content);

                String fullReasoning = "--- PHASE 1 (TASK) ---\n" +
                        (taskResponse.getReasoning() != null ? taskResponse.getReasoning() : "N/A") +
                        "\n\n--- PHASE 2 (SAMPLE) ---\n" +
                        (sampleResponse.getReasoning() != null ? sampleResponse.getReasoning() : "N/A") +
                        "\n\n--- PHASE 3 (BAND) ---\n" +
                        (bandResponse.getReasoning() != null ? bandResponse.getReasoning() : "N/A");
                response.setReasoning(fullReasoning);

                response.setValidation(generationSupport.buildValidationDto(validationResult));

                List<String> allWarnings = new ArrayList<>(validationResult.getWarnings());
                if (!validationResult.isValid()) {
                    allWarnings.addAll(validationResult.getAllErrors());
                }
                if (!allWarnings.isEmpty()) {
                    response.setWarnings(allWarnings);
                }

                // FIX 10: aggregate token usage across all three writing phases.
                OpenRouterClient.OpenRouterResponse aggregatedUsage = aggregateUsage(
                        bandResponse, fullReasoning,
                        taskResponse, sampleResponse, bandResponse);
                GenerationMetadataDTO metadata = generationSupport.buildMetadata(request, aggregatedUsage, content);
                response.setMetadata(metadata);

                return response;

            } catch (Exception e) {
                lastError = e.getMessage();
                logger.error("Error generating writing for part {} on attempt {}: {}", partNumber, attempts,
                        lastError);
                if (attempts >= MAX_RETRIES)
                    throw e;
            }
        }

        return GenerationResponseDTO.error("GENERATION_FAILED", lastError, true);
    }

    private OpenRouterClient.OpenRouterResponse performStreamingCall(
            GenerationRequestDTO request,
            String prompt,
            Map<String, Object> schema,
            Map<String, Object> reasoningConfig,
            SseEmitter parentEmitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled,
            int startProgress, int endProgress,
            String phaseLabel, int partNumber, int totalParts) throws Exception {
        // Default budget for small-output phases (e.g. question generation).
        return performStreamingCall(request, prompt, schema, reasoningConfig,
                parentEmitter, cancelled, startProgress, endProgress, phaseLabel, null,
                partNumber, totalParts);
    }

    /**
     * FIX 4: large-output phases (transcript, writing sample, writing band, listening
     * answers) must be able to request a larger token budget than the 8192 default,
     * otherwise long structured outputs are silently truncated. Callers pass
     * {@code generationSupport.resolveMaxTokens(request, 16384)} for those phases.
     *
     * @param maxTokensOverride explicit budget; when null or &le; 0 the 8192 default applies.
     */
    private OpenRouterClient.OpenRouterResponse performStreamingCall(
            GenerationRequestDTO request,
            String prompt,
            Map<String, Object> schema,
            Map<String, Object> reasoningConfig,
            SseEmitter parentEmitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled,
            int startProgress, int endProgress,
            String phaseLabel,
            Integer maxTokensOverride,
            int partNumber, int totalParts) throws Exception {

        String systemPrompt = "You are an expert IELTS exam content creator.";
        final Object lock = new Object();
        final boolean[] completed = { false };
        final OpenRouterClient.OpenRouterResponse[] responseHolder = new OpenRouterClient.OpenRouterResponse[1];
        final Exception[] errorHolder = new Exception[1];

        int maxTokens = (maxTokensOverride != null && maxTokensOverride > 0) ? maxTokensOverride : 8192;

        // FIX 4: per-request monotonic guard so emitted progress for this phase never
        // moves backwards (out-of-order or duplicate provider progress events).
        final java.util.concurrent.atomic.AtomicInteger lastInnerPct =
                new java.util.concurrent.atomic.AtomicInteger(startProgress);

        openRouterClient.callChatCompletionStreaming(
                // FIX 13: resolve the effective model (request override OR configured default +
                // variant) so the model actually called matches the model the reasoning config
                // was built for. Passing the raw request.getModel() (often null) sent the call
                // to the provider default while reasoning was tuned for the real model.
                generationSupport.resolveModel(request, false),
                systemPrompt,
                prompt,
                schema,
                reasoningConfig,
                request.getTemperature(),
                maxTokens,
                new OpenRouterClient.StreamCallback() {
                    @Override
                    public void onReasoningChunk(String chunk) {
                        sendEvent(parentEmitter, StreamEventDTO.aiThinking(chunk), cancelled);
                    }

                    @Override
                    public void onContentChunk(String chunk) {
                        sendEvent(parentEmitter, StreamEventDTO.aiChunk(chunk), cancelled);
                    }

                    @Override
                    public void onProgress(int percent, String message) {
                        int globalPercent = startProgress + (percent * (endProgress - startProgress) / 100);
                        // FIX 4: clamp to this phase's [startProgress, endProgress] window, then
                        // enforce monotonic non-decreasing progress.
                        globalPercent = Math.min(endProgress, Math.max(startProgress, globalPercent));
                        int monotonic = lastInnerPct.accumulateAndGet(globalPercent, Math::max);
                        sendEvent(parentEmitter, StreamEventDTO.progress(monotonic,
                                phaseLabel + ": " + message, partNumber, totalParts), cancelled);
                    }

                    @Override
                    public void onComplete(OpenRouterClient.OpenRouterResponse response) {
                        synchronized (lock) {
                            responseHolder[0] = response;
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

                    @Override
                    public void onCancelled() {
                        // FIX 5: user cancellation is NOT an error. Leave both holders null
                        // so performStreamingCall returns null and the caller unwinds cleanly
                        // (the multi-part loop then emits a single ABORTED event).
                        synchronized (lock) {
                            completed[0] = true;
                            lock.notify();
                        }
                    }
                },
                cancelled);

        synchronized (lock) {
            while (!completed[0])
                lock.wait();
        }

        if (errorHolder[0] != null)
            throw errorHolder[0];
        return responseHolder[0];
    }

    /**
     * FIX 10: mid-phase per-part deadline guard. Multi-phase generation
     * (reading/listening/writing) can exhaust the per-part budget partway through a
     * single attempt; the loop-top check only fires at retry boundaries, so we also
     * assert the deadline before each subsequent phase to avoid launching an expensive
     * phase call that is already over budget.
     */
    private void assertWithinDeadline(long perPartDeadline, int partNumber) {
        if (System.currentTimeMillis() > perPartDeadline) {
            throw new PartTimeoutException("[TIMEOUT] Part " + partNumber
                    + " exceeded per-part timeout of "
                    + openRouterConfig.getPerPartTimeoutMs() + "ms");
        }
    }

    /**
     * FIX 3: enforce per-type answer cardinality. Returns an error string when the
     * {@code correct_answer} array is null/empty, contains a blank entry, or its
     * length does not match the question type's expected cardinality; returns
     * {@code null} when the answer is valid.
     */
    private String validateAnswerCardinality(int qNum, String questionType, JsonNode correctAnswer) {
        if (correctAnswer == null || !correctAnswer.isArray() || correctAnswer.isEmpty()) {
            return "Q" + qNum + " has no correct_answer values";
        }
        for (JsonNode value : correctAnswer) {
            if (value == null || value.isNull() || value.asText().isBlank()) {
                return "Q" + qNum + " contains a blank correct_answer entry";
            }
        }
        int len = correctAnswer.size();
        switch (questionType) {
            case "MULTIPLE_CHOICE":
            case "MATCHING":
                if (len != 1) {
                    return "Q" + qNum + " (" + questionType + ") expects exactly 1 answer but got " + len;
                }
                break;
            case "MULTIPLE_CHOICE_MULTIPLE_ANSWERS":
                if (len != 2) {
                    return "Q" + qNum + " (" + questionType + ") expects exactly 2 answers but got " + len;
                }
                break;
            default:
                // FILL_IN_BLANK and other free-text types: at least one non-blank value
                // (already validated above).
                break;
        }
        return null;
    }

    /**
     * FIX 10: build a synthetic {@link OpenRouterClient.OpenRouterResponse} whose token
     * counters are the sum across every phase, so {@link AbtsGenerationSupport#buildMetadata}
     * reports the true cost of the full multi-phase generation rather than only the last phase.
     *
     * @param base             response whose model/content is reused for the aggregate
     * @param combinedReasoning concatenated reasoning across all phases
     * @param phases           every phase response to sum (nulls are skipped)
     */
    private OpenRouterClient.OpenRouterResponse aggregateUsage(
            OpenRouterClient.OpenRouterResponse base,
            String combinedReasoning,
            OpenRouterClient.OpenRouterResponse... phases) {
        OpenRouterClient.OpenRouterResponse agg = new OpenRouterClient.OpenRouterResponse();
        agg.setModelUsed(base.getModelUsed());
        agg.setContent(base.getContent());
        agg.setReasoning(combinedReasoning);

        int promptTokens = 0;
        int completionTokens = 0;
        int reasoningTokens = 0;
        long durationMs = 0;
        for (OpenRouterClient.OpenRouterResponse phase : phases) {
            if (phase == null)
                continue;
            if (phase.getPromptTokens() != null)
                promptTokens += phase.getPromptTokens();
            if (phase.getCompletionTokens() != null)
                completionTokens += phase.getCompletionTokens();
            if (phase.getReasoningTokens() != null)
                reasoningTokens += phase.getReasoningTokens();
            if (phase.getDurationMs() != null)
                durationMs += phase.getDurationMs();
        }
        agg.setPromptTokens(promptTokens);
        agg.setCompletionTokens(completionTokens);
        agg.setReasoningTokens(reasoningTokens);
        agg.setDurationMs(durationMs);
        return agg;
    }

    private void renumberQuestionsInContent(GeneratedContentDTO content, int partNumber,
            GenerationRequestDTO.SkillType skill) {
        if (content == null || content.getQuestions() == null)
            return;

        int startNumber = QuestionCountConfig.getStartQuestionNumber(skill, partNumber);

        for (int index = 0; index < content.getQuestions().size(); index++) {
            GeneratedContentDTO.GeneratedQuestionDTO question = content.getQuestions().get(index);
            if (question != null) {
                int localNumber = index + 1;
                int actualNumber = startNumber + localNumber - 1;
                question.setQuestionNumber(actualNumber);
            }
        }
    }

    private GeneratedContentDTO mergePartContents(List<GeneratedContentDTO> partContents, List<Integer> parts) {
        GeneratedContentDTO merged = new GeneratedContentDTO();

        List<GeneratedContentDTO.GeneratedQuestionDTO> allQuestions = new ArrayList<>();
        List<GeneratedContentDTO.GeneratedSectionDTO> allSections = new ArrayList<>();

        for (int index = 0; index < partContents.size(); index++) {
            GeneratedContentDTO partContent = partContents.get(index);
            // B2: defensively skip any null content so one failed part can't break the merge.
            if (partContent == null) {
                continue;
            }
            int partNumber = parts.get(index);

            if (partContent.getQuestions() != null) {
                allQuestions.addAll(partContent.getQuestions());
            }

            if (partContent.getSection() != null) {
                GeneratedContentDTO.GeneratedSectionDTO section = partContent.getSection();
                section.setPartNumber(partNumber);
                allSections.add(section);
            }
        }

        allQuestions.sort((first, second) -> {
            int firstNumber = first.getQuestionNumber() != null ? first.getQuestionNumber() : 0;
            int secondNumber = second.getQuestionNumber() != null ? second.getQuestionNumber() : 0;
            return Integer.compare(firstNumber, secondNumber);
        });

        merged.setQuestions(allQuestions);
        merged.setSections(allSections);

        if (!allSections.isEmpty()) {
            merged.setSection(allSections.get(0));
        }

        return merged;
    }

    private void sendEvent(SseEmitter emitter, StreamEventDTO event) {
        sendEvent(emitter, event, null);
    }

    /**
     * FIX 6: cancellation-aware SSE send. When the underlying connection is already
     * closed (IOException), flip the shared {@code cancelled} flag so in-flight
     * OpenRouter streaming calls observe the disconnect and stop early instead of
     * burning tokens writing to a dead emitter.
     */
    private void sendEvent(SseEmitter emitter, StreamEventDTO event,
            java.util.concurrent.atomic.AtomicBoolean cancelled) {
        try {
            emitter.send(SseEmitter.event()
                    .name(Objects.requireNonNull(event.getType().name().toLowerCase()))
                    .data(event));
        } catch (IOException e) {
            logger.debug("Failed to send SSE event (connection closed): {}", e.getMessage());
            if (cancelled != null) {
                cancelled.set(true);
            }
        } catch (IllegalStateException e) {
            logger.debug("Emitter already completed, skipping event: {}", event.getType());
            if (cancelled != null) {
                cancelled.set(true);
            }
        }
    }
}