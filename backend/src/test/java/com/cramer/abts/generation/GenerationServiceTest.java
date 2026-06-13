package com.cramer.abts.generation;

import com.cramer.abts.domain.GenerationResult;
import com.cramer.abts.domain.GenerationStatus;
import com.cramer.abts.service.ModelCapabilityRegistry;
import com.cramer.abts.validation.ContentValidator;
import com.cramer.abts.validation.ValidationResult;
import com.cramer.abts.web.dto.GenerationRequest;
import com.cramer.abts.web.dto.PartConfig;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.common.json.Json;
import com.cramer.platform.integration.openrouter.OpenRouterClient;
import com.cramer.platform.integration.openrouter.OpenRouterError;
import com.cramer.platform.integration.openrouter.OpenRouterException;
import com.cramer.platform.integration.openrouter.OpenRouterProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerationServiceTest {

    private PartGenerator readingGenerator;
    private ContentValidator validator;
    private GenerationService service;

    private final PartConfig cfg = new PartConfig("space travel", "AUTO", null,
            List.of("FILL_IN_BLANK", "MULTIPLE_CHOICE"), null, null, "MEDIUM", "INTERMEDIATE", null);

    @BeforeEach
    void setUp() {
        readingGenerator = mock(PartGenerator.class);
        when(readingGenerator.skill()).thenReturn(Skill.READING);
        validator = mock(ContentValidator.class);
        QuestionRenumberer renumberer = new QuestionRenumberer();
        OpenRouterProperties props = new OpenRouterProperties("key", null, "deepseek/deepseek-chat", 120_000);
        ModelResolver resolver = new ModelResolver(props, new ModelCapabilityRegistry());
        OpenRouterClient client = mock(OpenRouterClient.class);
        service = new GenerationService(List.of(readingGenerator), validator, renumberer, resolver, client, props);
    }

    private ObjectNode readingContent(int... numbers) {
        ObjectNode root = Json.mapper().createObjectNode();
        root.putObject("section").put("passage_text", "passage");
        ArrayNode questions = root.putArray("questions");
        for (int n : numbers) {
            ObjectNode q = questions.addObject();
            q.put("question_number", n);
            q.put("question_type", "FILL_IN_BLANK");
            q.putArray("correct_answer").add("x");
        }
        return root;
    }

    private GenerationRequest request(List<Integer> parts) {
        return new GenerationRequest(parts, Map.of("1", cfg, "2", cfg), null, "en", null, null);
    }

    @Test
    void multiPartMergeRenumbersSecondPartToCanonicalRange() {
        when(readingGenerator.generatePart(eq(1), any(), any())).thenReturn(readingContent(1, 2, 3));
        when(readingGenerator.generatePart(eq(2), any(), any())).thenReturn(readingContent(1, 2, 3));
        when(validator.validate(any(), anyInt(), any(), any())).thenReturn(new ValidationResult());

        GenerationResult result = service.generate(Skill.READING, request(List.of(1, 2)));

        assertThat(result.status()).isEqualTo(GenerationStatus.SUCCESS);
        JsonNode sections = result.content().path("sections");
        assertThat(sections).hasSize(2);
        assertThat(sections.get(1).path("questions").get(0).path("question_number").asInt()).isEqualTo(14);
        assertThat(sections.get(1).path("questions").get(2).path("question_number").asInt()).isEqualTo(16);
    }

    @Test
    void partialSuccessWhenOnePartFails() {
        when(readingGenerator.generatePart(eq(1), any(), any())).thenReturn(readingContent(1, 2, 3));
        when(readingGenerator.generatePart(eq(2), any(), any()))
                .thenThrow(new OpenRouterException(OpenRouterError.AUTH_FAILED, "no key"));
        when(validator.validate(any(), anyInt(), any(), any())).thenReturn(new ValidationResult());

        GenerationResult result = service.generate(Skill.READING, request(List.of(1, 2)));

        assertThat(result.status()).isEqualTo(GenerationStatus.PARTIAL_SUCCESS);
        assertThat(result.partErrors()).containsEntry(2, "AUTH_FAILED");
        assertThat(result.content().path("sections")).hasSize(1);
    }

    @Test
    void allPartsFailedYieldsFailed() {
        when(readingGenerator.generatePart(anyInt(), any(), any()))
                .thenThrow(new OpenRouterException(OpenRouterError.AUTH_FAILED, "no key"));

        GenerationResult result = service.generate(Skill.READING, request(List.of(1, 2)));

        assertThat(result.status()).isEqualTo(GenerationStatus.FAILED);
        assertThat(result.content()).isNull();
        assertThat(result.errorCode()).isEqualTo("AUTH_FAILED");
    }

    @Test
    void retriesUpToThreeTimesThenPartialOnPersistentValidationErrors() {
        when(readingGenerator.generatePart(eq(1), any(), any())).thenReturn(readingContent(1, 2, 3));
        ValidationResult invalid = new ValidationResult().addError("rd-bad", "/questions/0", "boom");
        when(validator.validate(any(), anyInt(), any(), any())).thenReturn(invalid);

        GenerationResult result = service.generate(Skill.READING, request(List.of(1)));

        assertThat(result.status()).isEqualTo(GenerationStatus.PARTIAL_SUCCESS);
        assertThat(result.attempts()).isEqualTo(3);
        verify(readingGenerator, times(3)).generatePart(eq(1), any(), any());
    }
}
