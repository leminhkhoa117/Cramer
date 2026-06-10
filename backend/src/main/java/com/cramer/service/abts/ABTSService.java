package com.cramer.service.abts;

import com.cramer.config.OpenRouterConfig;
import com.cramer.dto.abts.GenerationRequestDTO;
import com.cramer.dto.abts.GenerationResponseDTO;
import com.cramer.dto.abts.SaveContentRequestDTO;
import com.cramer.dto.abts.SaveContentResponseDTO;
import com.cramer.dto.abts.StreamEventDTO;
import com.cramer.repository.HashtagRepository;
import com.cramer.repository.IeltsTestRepository;
import com.cramer.repository.SectionRepository;
import com.cramer.repository.TestSetRepository;
import com.cramer.service.HashtagService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ABTS Service - public orchestration facade for AI-Based Test Generation.
 */
@Service
public class ABTSService {

    private static final Logger logger = LoggerFactory.getLogger(ABTSService.class);

    private final OpenRouterConfig config;
    private final JsonValidatorService jsonValidatorService;
    private final AbtsTemplateCatalogService templateCatalogService;
    private final AbtsModelCatalogService modelCatalogService;
    private final AbtsContentSaver contentSaver;
    private final AbtsGenerationRunner generationRunner;
    private final AbtsSkillStreamingRunner skillStreamingRunner;
    private final AbtsMultiPartStreamingRunner multiPartStreamingRunner;

    public ABTSService(
            OpenRouterConfig config,
            OpenRouterClient openRouterClient,
            PromptBuilderService promptBuilderService,
            JsonValidatorService jsonValidatorService,
            JdbcTemplate jdbcTemplate,
            TestSetRepository testSetRepository,
            IeltsTestRepository ieltsTestRepository,
            HashtagRepository hashtagRepository,
            HashtagService hashtagService,
            SectionRepository sectionRepository) {
        this.config = config;
        this.jsonValidatorService = jsonValidatorService;

        ObjectMapper objectMapper = new ObjectMapper();
        ModelCapabilityRegistry capabilityRegistry = new ModelCapabilityRegistry();
        AbtsGenerationSupport generationSupport = new AbtsGenerationSupport(config, capabilityRegistry);

        this.templateCatalogService = new AbtsTemplateCatalogService(jdbcTemplate);
        this.modelCatalogService = new AbtsModelCatalogService(config, openRouterClient, capabilityRegistry);
        this.contentSaver = new AbtsContentSaver(
                jdbcTemplate,
                testSetRepository,
                ieltsTestRepository,
                hashtagRepository,
                hashtagService,
                sectionRepository,
                objectMapper);
        this.generationRunner = new AbtsGenerationRunner(
                openRouterClient,
                promptBuilderService,
                jsonValidatorService,
                generationSupport);
        this.multiPartStreamingRunner = new AbtsMultiPartStreamingRunner(
                openRouterClient,
                promptBuilderService,
                jsonValidatorService,
                objectMapper,
                generationSupport,
                generationRunner,
                config);
        this.skillStreamingRunner = new AbtsSkillStreamingRunner(
                openRouterClient,
                promptBuilderService,
                jsonValidatorService,
                generationSupport,
                generationRunner,
                multiPartStreamingRunner);
    }

    /**
     * Main generation entry point.
     */
    public GenerationResponseDTO generate(GenerationRequestDTO request) {
        long startTime = System.currentTimeMillis();

        try {
            if (!config.hasApiKey()) {
                return GenerationResponseDTO.error(
                        "AUTH_FAILED",
                        "OpenRouter API key not configured. Set OPENROUTER_API_KEY environment variable.",
                        false);
            }

            GenerationResponseDTO response = generationRunner.generate(request);

            double generationTime = (System.currentTimeMillis() - startTime) / 1000.0;
            if (response.getMetadata() != null) {
                response.getMetadata().setGenerationTimeSeconds(generationTime);
                response.getMetadata()
                        .setGeneratedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }

            return response;

        } catch (OpenRouterClient.OpenRouterException e) {
            logger.error("OpenRouter API error during generation: {}", e.getMessage());
            return GenerationResponseDTO.error(e.getErrorCode(), e.getMessage(), e.isRetryable());
        } catch (Exception e) {
            logger.error("ABTS generation failed: {}", e.getMessage(), e);
            return GenerationResponseDTO.error("GENERATION_FAILED", e.getMessage(), false);
        }
    }

    /**
     * Generate content with streaming progress updates.
     */
    public void generateWithStream(GenerationRequestDTO request, SseEmitter emitter,
            java.util.concurrent.atomic.AtomicBoolean cancelled) throws IOException {
        try {
            if (cancelled != null && cancelled.get()) {
                sendEvent(emitter, StreamEventDTO.aborted());
                emitter.complete();
                return;
            }

            sendEvent(emitter, StreamEventDTO.started());

            if (!config.hasApiKey()) {
                sendEvent(emitter, StreamEventDTO.failed("OpenRouter API key not configured"));
                emitter.complete();
                return;
            }

            if (cancelled != null && cancelled.get()) {
                sendEvent(emitter, StreamEventDTO.aborted());
                emitter.complete();
                return;
            }

            List<Integer> partsToGenerate = request.getPartsToGenerate();
            if (partsToGenerate != null && partsToGenerate.size() > 1) {
                multiPartStreamingRunner.generateMultiplePartsWithStream(request, emitter, cancelled);
                return;
            }

            skillStreamingRunner.generateWithStream(request, emitter, cancelled);

        } catch (OpenRouterClient.OpenRouterException e) {
            if (cancelled != null && cancelled.get()) {
                logger.info("Generation was cancelled by user");
                return;
            }
            logger.error("OpenRouter API error during streaming generation: {}", e.getMessage());
            sendEvent(emitter, StreamEventDTO.failed(e.getMessage()));
            emitter.complete();
        } catch (Exception e) {
            if (cancelled != null && cancelled.get()) {
                logger.info("Generation was cancelled by user");
                return;
            }
            logger.error("Streaming generation failed: {}", e.getMessage(), e);
            sendEvent(emitter, StreamEventDTO.failed(e.getMessage()));
            emitter.complete();
        }
    }

    /**
     * Backward-compatible overload for generateWithStream.
     */
    public void generateWithStream(GenerationRequestDTO request, SseEmitter emitter) throws IOException {
        generateWithStream(request, emitter, null);
    }

    /**
     * Regenerate specific questions while keeping existing passage.
     */
    public GenerationResponseDTO regenerateQuestions(GenerationRequestDTO request) {
        return generationRunner.regenerateQuestions(request);
    }

    /**
     * Validate content against JSON schemas and business rules.
     */
    public Map<String, Object> validateContent(Map<String, Object> content) {
        Map<String, Object> result = new HashMap<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.valueToTree(content);

            GenerationRequestDTO request = new GenerationRequestDTO();
            request.setDifficulty(GenerationRequestDTO.DifficultyLevel.INTERMEDIATE);
            request.setTestType(GenerationRequestDTO.TestType.ACADEMIC);

            if (root.has("explanation_language")) {
                String lang = root.get("explanation_language").asText();
                request.setExplanationLanguage("EN".equalsIgnoreCase(lang)
                        ? GenerationRequestDTO.ExplanationLanguage.EN
                        : GenerationRequestDTO.ExplanationLanguage.VI);
            }

            JsonValidatorService.ValidationResult validationResult;

            if (root.has("task_prompt")) {
                request.setSkill(GenerationRequestDTO.SkillType.WRITING);
                request.setPartNumber(detectWritingPart(root));
                validationResult = jsonValidatorService.validateWritingContent(root.toString(), request);
            } else if (root.has("transcript")) {
                request.setSkill(GenerationRequestDTO.SkillType.LISTENING);
                request.setPartNumber(detectListeningPart(root));
                validationResult = jsonValidatorService.validateListeningContent(root.toString(), request);
            } else {
                request.setSkill(GenerationRequestDTO.SkillType.READING);
                validationResult = jsonValidatorService.validateReadingContent(root.toString(), request);
            }

            result.put("valid", validationResult.isValid());
            result.put("schemaErrors", validationResult.getSchemaErrors());
            result.put("contentErrors", validationResult.getContentErrors());
            result.put("businessRuleErrors", validationResult.getBusinessRuleErrors());
            result.put("warnings", validationResult.getWarnings());
            // FIX 3: additive structured issues for the refinement UI/agent.
            result.put("issues", validationResult.getIssues());

        } catch (Exception e) {
            result.put("valid", false);
            result.put("schemaErrors", List.of("Invalid content: " + e.getMessage()));
            result.put("contentErrors", List.of());
            result.put("businessRuleErrors", List.of());
            result.put("warnings", List.of());
            result.put("issues", List.of()); // FIX 3
        }

        return result;
    }

    private Integer detectListeningPart(JsonNode root) {
        if (root.has("part_number")) {
            return root.get("part_number").asInt();
        }
        if (root.has("partNumber")) {
            return root.get("partNumber").asInt();
        }
        JsonNode questions = root.get("questions");
        if (questions != null && questions.isArray() && !questions.isEmpty()) {
            int min = Integer.MAX_VALUE;
            for (JsonNode question : questions) {
                if (question.has("question_number")) {
                    min = Math.min(min, question.get("question_number").asInt());
                }
            }
            if (min != Integer.MAX_VALUE) {
                int part = ((min - 1) / 10) + 1;
                return Math.max(1, Math.min(4, part));
            }
        }
        return 1;
    }

    private Integer detectWritingPart(JsonNode root) {
        if (root.has("task_type")) {
            String taskType = root.get("task_type").asText().toUpperCase();
            if (taskType.startsWith("TASK_2")) {
                return 2;
            }
        }
        return 1;
    }

    public List<Map<String, Object>> getTemplateCategories() {
        return templateCatalogService.getTemplateCategories();
    }

    public List<Map<String, Object>> getTemplatesByCategory(String categoryId) {
        return templateCatalogService.getTemplatesByCategory(categoryId);
    }

    public void incrementTemplateUseCount(String templateId) {
        templateCatalogService.incrementTemplateUseCount(templateId);
    }

    public List<Map<String, Object>> getAvailableModels() {
        return modelCatalogService.getAvailableModels();
    }

    public List<Map<String, Object>> getAvailableModelsWithCapabilities() {
        return modelCatalogService.getAvailableModelsWithCapabilities();
    }

    public Map<String, Object> getModelCapabilities(String modelId) {
        return modelCatalogService.getModelCapabilities(modelId);
    }

    public Map<String, Object> getStatus() {
        return modelCatalogService.getStatus();
    }

    /**
     * Validate configured default models against the live OpenRouter catalog at startup.
     * Fatal ({@link IllegalStateException}) only when the generation model is invalid and
     * no fallback resolves; all other failures are logged and tolerated.
     */
    @PostConstruct
    void validateConfiguredModels() {
        try {
            modelCatalogService.validateConfiguredDefaults();
        } catch (IllegalStateException fatal) {
            throw fatal;
        } catch (Exception ex) {
            logger.warn("Model default validation skipped due to error: {}", ex.getMessage());
        }
    }

    /**
     * Save AI-generated content to the database using the new test hierarchy.
     */
    @Transactional
    public SaveContentResponseDTO saveContent(SaveContentRequestDTO request, String adminUserId) {
        return contentSaver.saveContent(request, adminUserId);
    }

    private void sendEvent(SseEmitter emitter, StreamEventDTO event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(Objects.requireNonNull(event.getType().name().toLowerCase()))
                    .data(event));
        } catch (IOException e) {
            logger.debug("Failed to send SSE event (connection closed): {}", e.getMessage());
        } catch (IllegalStateException e) {
            logger.debug("Emitter already completed, skipping event: {}", event.getType());
        }
    }
}
