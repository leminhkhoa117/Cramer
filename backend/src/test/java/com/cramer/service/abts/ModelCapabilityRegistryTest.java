package com.cramer.service.abts;

import com.cramer.dto.abts.GenerationRequestDTO;
import com.cramer.service.abts.ModelCapabilityRegistry.ModelCapability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ModelCapabilityRegistry}: vendor knob inference,
 * vendor-correct reasoning payload shapes, explicit-budget clamping, and the
 * client descriptor key shape.
 */
class ModelCapabilityRegistryTest {

    private final ModelCapabilityRegistry registry = new ModelCapabilityRegistry();

    private GenerationRequestDTO reasoningRequest() {
        GenerationRequestDTO req = new GenerationRequestDTO();
        req.setEnableReasoning(true);
        return req;
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "deepseek/v4-flash, DEEPSEEK_TOGGLE",
            "deepseek/r1, DEEPSEEK_TOGGLE",
            "deepseek/chat, NONE",
            "anthropic/claude-sonnet-4.5, ANTHROPIC_BUDGET",
            "google/gemini-2.5-flash, GEMINI_BUDGET",
            "openai/gpt-5-mini, EFFORT_LOW_MED_HIGH",
            "openai/o1-mini, EFFORT_LOW_MED_HIGH",
            "qwen/qwen3-thinking, QWEN_THINKING",
            "z-ai/glm-4.6, GLM_THINKING",
            "moonshotai/kimi-k2, KIMI_NONE",
            "unknown/foo, NONE"
    })
    void inferFromModelId_mapsSlugToKnob(String modelId, String expectedKnob) {
        assertThat(registry.inferFromModelId(modelId).knob().name()).isEqualTo(expectedKnob);
    }

    @Test
    void buildReasoningPayload_anthropic_usesReasoningMaxTokens() {
        Map<String, Object> payload =
                registry.buildReasoningPayload("anthropic/claude-sonnet-4.5", reasoningRequest());

        assertThat(payload).containsKey("reasoning");
        assertThat(payload).doesNotContainKeys("thinking", "thinking_config");

        @SuppressWarnings("unchecked")
        Map<String, Object> reasoning = (Map<String, Object>) payload.get("reasoning");
        assertThat(reasoning).containsKey("max_tokens");
    }

    @Test
    void buildReasoningPayload_gemini_usesReasoningMaxTokens() {
        Map<String, Object> payload =
                registry.buildReasoningPayload("google/gemini-2.5-flash", reasoningRequest());

        assertThat(payload).containsKey("reasoning");
        assertThat(payload).doesNotContainKeys("thinking", "thinking_config");

        @SuppressWarnings("unchecked")
        Map<String, Object> reasoning = (Map<String, Object>) payload.get("reasoning");
        assertThat(reasoning).containsKey("max_tokens");
    }

    @Test
    void buildReasoningPayload_deepseekV4_usesReasoningMaxTokens() {
        Map<String, Object> payload =
                registry.buildReasoningPayload("deepseek/v4-flash", reasoningRequest());

        assertThat(payload).containsKey("reasoning");
        assertThat(payload).doesNotContainKeys("thinking", "thinking_config");

        @SuppressWarnings("unchecked")
        Map<String, Object> reasoning = (Map<String, Object>) payload.get("reasoning");
        assertThat(reasoning).containsKey("max_tokens");
    }

    @Test
    void buildReasoningPayload_reasoningDisabled_returnsEmptyMap() {
        GenerationRequestDTO req = reasoningRequest();
        req.setEnableReasoning(false);

        assertThat(registry.buildReasoningPayload("anthropic/claude-sonnet-4.5", req)).isEmpty();
    }

    @Test
    void buildReasoningPayload_explicitBudget_clampedToAnthropicMax() {
        GenerationRequestDTO req = reasoningRequest();
        req.setReasoningBudget(100000); // exceeds Anthropic max of 65536

        Map<String, Object> payload =
                registry.buildReasoningPayload("anthropic/claude-sonnet-4.5", req);

        @SuppressWarnings("unchecked")
        Map<String, Object> reasoning = (Map<String, Object>) payload.get("reasoning");
        assertThat(reasoning.get("max_tokens")).isEqualTo(65536);
    }

    @Test
    void toClientDescriptor_hasExpectedKeyShape() {
        ModelCapability cap = registry.inferFromModelId("openai/gpt-5-mini");
        Map<String, Object> descriptor = registry.toClientDescriptor(cap);

        assertThat(descriptor).containsKeys(
                "id", "displayName", "vendor", "knobType", "validEfforts",
                "budgetRange", "defaultMaxTokens", "contextLength",
                "supportsJsonSchema", "supportsStreaming");
        assertThat(descriptor.get("knobType")).isEqualTo("EFFORT_LOW_MED_HIGH");

        @SuppressWarnings("unchecked")
        Map<String, Object> budgetRange = (Map<String, Object>) descriptor.get("budgetRange");
        assertThat(budgetRange).containsKeys("min", "max");
    }
}
