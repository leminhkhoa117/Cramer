package com.cramer.service.abts;

import com.cramer.config.OpenRouterConfig;
import com.cramer.dto.abts.GeneratedContentDTO;
import com.cramer.dto.abts.GenerationRequestDTO;
import com.cramer.dto.abts.GenerationResponseDTO;
import com.cramer.dto.abts.GenerationResponseDTO.GenerationMetadataDTO;
import com.cramer.dto.abts.GenerationResponseDTO.ValidationResultDTO;

import java.util.List;
import java.util.Map;

final class AbtsGenerationSupport {

    private final OpenRouterConfig config;
    private final ModelCapabilityRegistry capabilityRegistry;

    AbtsGenerationSupport(OpenRouterConfig config, ModelCapabilityRegistry capabilityRegistry) {
        this.config = config;
        this.capabilityRegistry = capabilityRegistry;
    }

    /**
     * Build a vendor-aware reasoning payload for the request's effective generation model.
     *
     * <p>The returned map is already keyed with the correct top-level vendor schema
     * (e.g. {@code reasoning}, {@code thinking}, {@code thinking_config}) and must be
     * spread into the request body via {@code body.putAll(...)}.</p>
     */
    Map<String, Object> buildReasoningConfig(GenerationRequestDTO request) {
        return buildReasoningConfig(request, resolveModel(request, false));
    }

    /**
     * Build a vendor-aware reasoning payload for an explicit model id (e.g. regeneration model).
     */
    Map<String, Object> buildReasoningConfig(GenerationRequestDTO request, String modelId) {
        return capabilityRegistry.buildReasoningPayload(modelId, request);
    }

    GenerationResponseDTO buildErrorResponse(
            String errorCode, String message, int attempts, boolean retryable) {

        GenerationResponseDTO response = new GenerationResponseDTO();
        response.setStatus(GenerationResponseDTO.GenerationStatus.FAILED);
        response.setErrors(List.of(errorCode + ": " + message));

        GenerationMetadataDTO metadata = new GenerationMetadataDTO();
        metadata.setFailedAttempts(attempts);
        metadata.setLastError(message);
        metadata.setRetryable(retryable);
        response.setMetadata(metadata);

        return response;
    }

    GenerationMetadataDTO buildMetadata(
            GenerationRequestDTO request,
            OpenRouterClient.OpenRouterResponse aiResponse,
            GeneratedContentDTO content) {

        GenerationMetadataDTO metadata = new GenerationMetadataDTO();

        metadata.setTopic(request.getTopic());
        metadata.setDifficulty(request.getDifficulty().getDisplayName());
        metadata.setBandRange(request.getDifficulty().getBandRange());
        metadata.setModelUsed(aiResponse.getModelUsed());
        metadata.setPromptTokens(aiResponse.getPromptTokens());
        metadata.setCompletionTokens(aiResponse.getCompletionTokens());
        metadata.setReasoningTokens(aiResponse.getReasoningTokens());

        if (content != null && content.getSection() != null) {
            metadata.setWordCount(content.getSection().getWordCount());
        }
        if (content != null && content.getQuestions() != null) {
            metadata.setQuestionCount(content.getQuestions().size());
        }

        metadata.setEstimatedCostUsd(estimateCost(aiResponse));

        return metadata;
    }

    ValidationResultDTO buildValidationDto(JsonValidatorService.ValidationResult validationResult) {
        ValidationResultDTO validationDTO = new ValidationResultDTO();
        validationDTO.setValid(validationResult.isValid());
        validationDTO.setSchemaErrors(validationResult.getSchemaErrors());
        validationDTO.setContentErrors(validationResult.getContentErrors());
        validationDTO.setBusinessRuleErrors(validationResult.getBusinessRuleErrors());
        return validationDTO;
    }

    double estimateCost(OpenRouterClient.OpenRouterResponse response) {
        double inputCost = (response.getPromptTokens() != null ? response.getPromptTokens() : 0) * 0.00000055;
        double outputCost = (response.getCompletionTokens() != null ? response.getCompletionTokens() : 0) * 0.00000219;
        double reasoningCost = (response.getReasoningTokens() != null ? response.getReasoningTokens() : 0) * 0.00000219;

        return Math.round((inputCost + outputCost + reasoningCost) * 10000.0) / 10000.0;
    }

    String resolveModel(GenerationRequestDTO request, boolean forRegeneration) {
        String base = request.getModel() != null
                ? request.getModel()
                : (forRegeneration ? config.getRegenerationModel() : config.getGenerationModel());

        if (request.getModelVariant() != null && !request.getModelVariant().isBlank()) {
            return base + request.getModelVariant();
        }

        return base;
    }

    Integer resolveMaxTokens(GenerationRequestDTO request, int defaultValue) {
        Integer requested = request.getMaxTokens();
        if (requested != null && requested > 0) {
            return requested;
        }
        return defaultValue;
    }

    boolean shouldFallbackToNonStreaming(Exception e) {
        String message = e != null && e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("response_format")
                || message.contains("json_schema")
                || message.contains("stream")
                || message.contains("streaming")
                || message.contains("not supported")
                || message.contains("unsupported");
    }
}