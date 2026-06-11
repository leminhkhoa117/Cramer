package com.cramer.abts.service;

import com.cramer.abts.web.dto.ModelConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCapabilityRegistryTest {

    private final ModelCapabilityRegistry registry = new ModelCapabilityRegistry();

    @Test
    void detectsReasoningModelsBySlug() {
        assertThat(registry.supportsReasoning("deepseek/deepseek-r1")).isTrue();
        assertThat(registry.supportsReasoning("openai/gpt-5")).isTrue();
        assertThat(registry.supportsReasoning("anthropic/claude-sonnet-4")).isTrue();
        assertThat(registry.supportsReasoning("meta-llama/llama-3.1-70b-instruct")).isFalse();
    }

    @Test
    void returnsNullPayloadWhenReasoningDisabled() {
        ModelConfig cfg = new ModelConfig("deepseek/deepseek-r1", null, null, false, "high", null, null);
        assertThat(registry.reasoningPayload("deepseek/deepseek-r1", cfg)).isNull();
    }

    @Test
    void degradesGracefullyOnNonReasoningModel() {
        ModelConfig cfg = new ModelConfig("meta-llama/llama-3.1-70b-instruct", null, null, true, "high", null, null);
        assertThat(registry.reasoningPayload("meta-llama/llama-3.1-70b-instruct", cfg)).isNull();
    }

    @Test
    void buildsEffortPayloadAndBudgetPayload() {
        ModelConfig effort = new ModelConfig("openai/gpt-5", null, null, true, "high", null, null);
        JsonNode effortPayload = registry.reasoningPayload("openai/gpt-5", effort);
        assertThat(effortPayload.path("effort").asText()).isEqualTo("high");

        ModelConfig budget = new ModelConfig("openai/gpt-5", null, null, true, null, 4096, null);
        JsonNode budgetPayload = registry.reasoningPayload("openai/gpt-5", budget);
        assertThat(budgetPayload.path("max_tokens").asInt()).isEqualTo(4096);
    }
}
