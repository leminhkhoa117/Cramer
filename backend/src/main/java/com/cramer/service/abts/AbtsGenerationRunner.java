package com.cramer.service.abts;

import com.cramer.dto.abts.GeneratedContentDTO;
import com.cramer.dto.abts.GenerationRequestDTO;
import com.cramer.dto.abts.GenerationResponseDTO;
import com.cramer.dto.abts.GenerationResponseDTO.GenerationMetadataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

final class AbtsGenerationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AbtsGenerationRunner.class);
    private static final int MAX_RETRIES = 3;

    private final OpenRouterClient openRouterClient;
    private final PromptBuilderService promptBuilderService;
    private final JsonValidatorService jsonValidatorService;
    private final AbtsGenerationSupport generationSupport;

    AbtsGenerationRunner(
            OpenRouterClient openRouterClient,
            PromptBuilderService promptBuilderService,
            JsonValidatorService jsonValidatorService,
            AbtsGenerationSupport generationSupport) {
        this.openRouterClient = openRouterClient;
        this.promptBuilderService = promptBuilderService;
        this.jsonValidatorService = jsonValidatorService;
        this.generationSupport = generationSupport;
    }

    GenerationResponseDTO generate(GenerationRequestDTO request) {
        return switch (request.getSkill()) {
            case READING -> generateReading(request);
            case LISTENING -> generateListening(request);
            case WRITING -> generateWriting(request);
            case SPEAKING -> GenerationResponseDTO.error(
                    "NOT_IMPLEMENTED",
                    "Speaking generation is not yet implemented.",
                    false);
        };
    }

    GenerationResponseDTO generateSinglePart(GenerationRequestDTO request) {
        return generate(request);
    }

    GenerationResponseDTO generateReading(GenerationRequestDTO request) {
        logger.info("Starting Reading generation for topic: {}", request.getTopic());

        int attempts = 0;
        String lastError = null;

        while (attempts < MAX_RETRIES) {
            attempts++;
            logger.info("Reading generation attempt {}/{}", attempts, MAX_RETRIES);

            try {
                String systemPrompt = promptBuilderService.buildReadingSystemPrompt();
                String userPrompt = promptBuilderService.buildReadingPrompt(request);
                Map<String, Object> jsonSchema = promptBuilderService.getReadingJsonSchema();

                String model = generationSupport.resolveModel(request, false);
                Map<String, Object> reasoningConfig = generationSupport.buildReasoningConfig(request);

                boolean enableWebSearch = Boolean.TRUE.equals(request.getEnableWebSearch())
                        && (request.getFacts() == null || request.getFacts().isEmpty());
                boolean enableCaching = Boolean.TRUE.equals(request.getEnableContextCaching());

                OpenRouterClient.OpenRouterResponse aiResponse = openRouterClient.callChatCompletionWithFeatures(
                        model,
                        systemPrompt,
                        userPrompt,
                        jsonSchema,
                        List.of("anthropic/claude-3.5-sonnet", "deepseek/deepseek-chat"),
                        reasoningConfig,
                        request.getTemperature(),
                        generationSupport.resolveMaxTokens(request, 16384),
                        enableWebSearch,
                        enableCaching);

                JsonValidatorService.ValidationResult validationResult = jsonValidatorService
                        .validateReadingContent(aiResponse.getContent(), request);

                if (!validationResult.isValid()) {
                    lastError = String.join("; ", validationResult.getAllErrors());
                    logger.warn("Validation failed on attempt {}: {}", attempts, lastError);

                    if (attempts < MAX_RETRIES) {
                        continue;
                    }

                    logger.warn("Max retries reached. Forcing acceptance of invalid content as PARTIAL_SUCCESS.");
                }

                GeneratedContentDTO content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());

                GenerationResponseDTO response = new GenerationResponseDTO();
                response.setStatus(GenerationResponseDTO.GenerationStatus.SUCCESS);
                response.setContent(content);
                response.setReasoning(aiResponse.getReasoning());
                response.setValidation(generationSupport.buildValidationDto(validationResult));

                List<String> allWarnings = new ArrayList<>(validationResult.getWarnings());
                if (!validationResult.isValid()) {
                    allWarnings.addAll(validationResult.getAllErrors());
                    response.setStatus(GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                } else if (!validationResult.getWarnings().isEmpty()) {
                    response.setStatus(GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                }
                if (!allWarnings.isEmpty()) {
                    response.setWarnings(allWarnings);
                }

                GenerationMetadataDTO metadata = generationSupport.buildMetadata(request, aiResponse, content);
                response.setMetadata(metadata);

                logger.info("Reading generation successful on attempt {}", attempts);
                return response;

            } catch (OpenRouterClient.OpenRouterException e) {
                lastError = e.getMessage();
                logger.error("OpenRouter error on attempt {}: {}", attempts, lastError);

                if (!e.isRetryable()) {
                    return generationSupport.buildErrorResponse(e.getErrorCode(), e.getMessage(), attempts, false);
                }
            } catch (Exception e) {
                lastError = e.getMessage();
                logger.error("Error on attempt {}: {}", attempts, lastError, e);
            }
        }

        return generationSupport.buildErrorResponse("MAX_RETRIES_EXCEEDED",
                "Failed after " + MAX_RETRIES + " attempts. Last error: " + lastError,
                attempts, false);
    }

    GenerationResponseDTO generateListening(GenerationRequestDTO request) {
        logger.info("Starting Listening generation for topic: {}", request.getTopic());

        int attempts = 0;
        String lastError = null;

        while (attempts < MAX_RETRIES) {
            attempts++;

            try {
                String systemPrompt = promptBuilderService.buildListeningSystemPrompt();
                String userPrompt = promptBuilderService.buildListeningPrompt(request);
                Map<String, Object> jsonSchema = promptBuilderService.getListeningJsonSchema();

                String model = generationSupport.resolveModel(request, false);
                Map<String, Object> reasoningConfig = generationSupport.buildReasoningConfig(request);

                boolean enableWebSearch = Boolean.TRUE.equals(request.getEnableWebSearch())
                        && (request.getFacts() == null || request.getFacts().isEmpty());
                boolean enableCaching = Boolean.TRUE.equals(request.getEnableContextCaching());

                OpenRouterClient.OpenRouterResponse aiResponse = openRouterClient.callChatCompletionWithFeatures(
                        model,
                        systemPrompt,
                        userPrompt,
                        jsonSchema,
                        List.of("anthropic/claude-3.5-sonnet", "deepseek/deepseek-chat"),
                        reasoningConfig,
                        request.getTemperature(),
                        generationSupport.resolveMaxTokens(request, 16384),
                        enableWebSearch,
                        enableCaching);

                JsonValidatorService.ValidationResult validationResult = jsonValidatorService
                        .validateListeningContent(aiResponse.getContent(), request);

                if (!validationResult.isValid()) {
                    lastError = String.join("; ", validationResult.getAllErrors());
                    if (attempts < MAX_RETRIES)
                        continue;
                    logger.warn("Max retries reached for Listening. Forcing PARTIAL_SUCCESS.");
                }

                GeneratedContentDTO content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());

                GenerationResponseDTO response = new GenerationResponseDTO();
                response.setStatus(validationResult.isValid()
                        ? GenerationResponseDTO.GenerationStatus.SUCCESS
                        : GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                response.setContent(content);
                response.setReasoning(aiResponse.getReasoning());
                response.setMetadata(generationSupport.buildMetadata(request, aiResponse, content));
                response.setValidation(generationSupport.buildValidationDto(validationResult));

                List<String> allWarnings = new ArrayList<>(validationResult.getWarnings());
                if (!validationResult.isValid()) {
                    allWarnings.addAll(validationResult.getAllErrors());
                }
                if (!allWarnings.isEmpty()) {
                    response.setWarnings(allWarnings);
                }

                return response;

            } catch (OpenRouterClient.OpenRouterException e) {
                lastError = e.getMessage();
                if (!e.isRetryable()) {
                    return generationSupport.buildErrorResponse(e.getErrorCode(), e.getMessage(), attempts, false);
                }
            } catch (Exception e) {
                lastError = e.getMessage();
            }
        }

        return generationSupport.buildErrorResponse("MAX_RETRIES_EXCEEDED", lastError, attempts, false);
    }

    GenerationResponseDTO generateWriting(GenerationRequestDTO request) {
        logger.info("Starting Writing generation for topic: {}", request.getTopic());

        int attempts = 0;
        String lastError = null;

        while (attempts < MAX_RETRIES) {
            attempts++;

            try {
                String systemPrompt = promptBuilderService.buildWritingSystemPrompt();
                String userPrompt = promptBuilderService.buildWritingPrompt(request);
                Map<String, Object> jsonSchema = promptBuilderService.getWritingJsonSchema();

                String model = generationSupport.resolveModel(request, false);
                Map<String, Object> reasoningConfig = generationSupport.buildReasoningConfig(request);

                boolean enableWebSearch = Boolean.TRUE.equals(request.getEnableWebSearch())
                        && (request.getFacts() == null || request.getFacts().isEmpty());
                boolean enableCaching = Boolean.TRUE.equals(request.getEnableContextCaching());

                OpenRouterClient.OpenRouterResponse aiResponse = openRouterClient.callChatCompletionWithFeatures(
                        model,
                        systemPrompt,
                        userPrompt,
                        jsonSchema,
                        List.of("anthropic/claude-3.5-sonnet", "deepseek/deepseek-chat"),
                        reasoningConfig,
                        request.getTemperature(),
                        generationSupport.resolveMaxTokens(request, 16384),
                        enableWebSearch,
                        enableCaching);

                JsonValidatorService.ValidationResult validationResult = jsonValidatorService
                        .validateWritingContent(aiResponse.getContent(), request);

                if (!validationResult.isValid()) {
                    lastError = String.join("; ", validationResult.getAllErrors());
                    if (attempts < MAX_RETRIES)
                        continue;
                    logger.warn("Max retries reached for Writing. Forcing PARTIAL_SUCCESS.");
                }

                GeneratedContentDTO content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());

                GenerationResponseDTO response = new GenerationResponseDTO();
                response.setStatus(validationResult.isValid()
                        ? GenerationResponseDTO.GenerationStatus.SUCCESS
                        : GenerationResponseDTO.GenerationStatus.PARTIAL_SUCCESS);
                response.setContent(content);
                response.setReasoning(aiResponse.getReasoning());
                response.setMetadata(generationSupport.buildMetadata(request, aiResponse, content));
                response.setValidation(generationSupport.buildValidationDto(validationResult));

                List<String> allWarnings = new ArrayList<>(validationResult.getWarnings());
                if (!validationResult.isValid()) {
                    allWarnings.addAll(validationResult.getAllErrors());
                }
                if (!allWarnings.isEmpty()) {
                    response.setWarnings(allWarnings);
                }

                return response;

            } catch (OpenRouterClient.OpenRouterException e) {
                lastError = e.getMessage();
                if (!e.isRetryable()) {
                    return generationSupport.buildErrorResponse(e.getErrorCode(), e.getMessage(), attempts, false);
                }
            } catch (Exception e) {
                lastError = e.getMessage();
            }
        }

        return generationSupport.buildErrorResponse("MAX_RETRIES_EXCEEDED", lastError, attempts, false);
    }

    GenerationResponseDTO regenerateQuestions(GenerationRequestDTO request) {
        logger.info("Starting question regeneration for {} question(s)",
                request.getQuestionsToRegenerate() != null ? request.getQuestionsToRegenerate().size() : "all");

        try {
            GenerationRequestDTO.SkillType skill = request.getSkill() != null
                    ? request.getSkill()
                    : GenerationRequestDTO.SkillType.READING;

            return switch (skill) {
                case READING -> regenerateReadingQuestions(request);
                case LISTENING -> regenerateListeningQuestions(request);
                case WRITING -> generateWriting(request);
                case SPEAKING -> GenerationResponseDTO.error(
                        "NOT_IMPLEMENTED",
                        "Speaking regeneration is not yet implemented.",
                        false);
            };

        } catch (OpenRouterClient.OpenRouterException e) {
            logger.error("Question regeneration failed: {}", e.getMessage());
            return generationSupport.buildErrorResponse(e.getErrorCode(), e.getMessage(), 1, false);
        } catch (Exception e) {
            logger.error("Question regeneration failed: {}", e.getMessage());
            return GenerationResponseDTO.error("REGENERATION_FAILED", e.getMessage(), true);
        }
    }

    private GenerationResponseDTO regenerateReadingQuestions(GenerationRequestDTO request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## TASK: Regenerate IELTS Questions for Existing Passage\n\n");
        prompt.append("### Existing Passage\n");
        prompt.append(request.getExistingPassageText()).append("\n\n");
        prompt.append("You MUST include the original passage in the output without any changes.\n");

        if (request.getQuestionsToRegenerate() != null && !request.getQuestionsToRegenerate().isEmpty()) {
            prompt.append("### Questions to Regenerate\n");
            prompt.append("Regenerate ONLY questions: ");
            prompt.append(request.getQuestionsToRegenerate().stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + ", " + b).orElse(""));
            prompt.append("\n");
            prompt.append("Return ONLY the specified questions in the questions array.\n\n");
        }

        String model = generationSupport.resolveModel(request, true);
        Map<String, Object> jsonSchema = promptBuilderService.getReadingJsonSchema();

        OpenRouterClient.OpenRouterResponse aiResponse = openRouterClient.callChatCompletion(
                model,
                promptBuilderService.buildReadingSystemPrompt(),
                prompt.toString(),
                jsonSchema,
                List.of("anthropic/claude-3.5-sonnet", "deepseek/deepseek-chat"),
                generationSupport.buildReasoningConfig(request, model),
                request.getTemperature(),
                generationSupport.resolveMaxTokens(request, 8192));

        GeneratedContentDTO content;
        try {
            content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());
        } catch (Exception e) {
            logger.error("Failed to parse regenerated Reading content: {}", e.getMessage());
            return GenerationResponseDTO.error("PARSE_ERROR", e.getMessage(), false);
        }

        GenerationResponseDTO response = new GenerationResponseDTO();
        response.setStatus(GenerationResponseDTO.GenerationStatus.SUCCESS);
        response.setContent(content);
        response.setReasoning(aiResponse.getReasoning());
        response.setMetadata(generationSupport.buildMetadata(request, aiResponse, content));

        return response;
    }

    private GenerationResponseDTO regenerateListeningQuestions(GenerationRequestDTO request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## TASK: Regenerate IELTS Listening Questions for Existing Transcript\n\n");
        prompt.append("### Existing Transcript (DO NOT MODIFY)\n");
        prompt.append(request.getExistingPassageText()).append("\n\n");
        prompt.append("You MUST include the original transcript in the output without any changes.\n");

        if (request.getQuestionsToRegenerate() != null && !request.getQuestionsToRegenerate().isEmpty()) {
            prompt.append("### Questions to Regenerate\n");
            prompt.append("Regenerate ONLY questions: ");
            prompt.append(request.getQuestionsToRegenerate().stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + ", " + b).orElse(""));
            prompt.append("\n");
            prompt.append("Return ONLY the specified questions in the questions array.\n\n");
        }

        String model = generationSupport.resolveModel(request, true);
        Map<String, Object> jsonSchema = promptBuilderService.getListeningJsonSchema();

        OpenRouterClient.OpenRouterResponse aiResponse = openRouterClient.callChatCompletion(
                model,
                promptBuilderService.buildListeningSystemPrompt(),
                prompt.toString(),
                jsonSchema,
                List.of("anthropic/claude-3.5-sonnet", "deepseek/deepseek-chat"),
                generationSupport.buildReasoningConfig(request, model),
                request.getTemperature(),
                generationSupport.resolveMaxTokens(request, 8192));

        GeneratedContentDTO content;
        try {
            content = jsonValidatorService.parseGeneratedContent(aiResponse.getContent());
        } catch (Exception e) {
            logger.error("Failed to parse regenerated Listening content: {}", e.getMessage());
            return GenerationResponseDTO.error("PARSE_ERROR", e.getMessage(), false);
        }

        GenerationResponseDTO response = new GenerationResponseDTO();
        response.setStatus(GenerationResponseDTO.GenerationStatus.SUCCESS);
        response.setContent(content);
        response.setReasoning(aiResponse.getReasoning());
        response.setMetadata(generationSupport.buildMetadata(request, aiResponse, content));

        return response;
    }
}