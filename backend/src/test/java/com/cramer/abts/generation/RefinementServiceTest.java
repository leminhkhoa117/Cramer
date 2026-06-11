package com.cramer.abts.generation;

import com.cramer.abts.config.AbtsProperties;
import com.cramer.abts.domain.Hunk;
import com.cramer.abts.service.ModelCapabilityRegistry;
import com.cramer.abts.validation.ContentValidator;
import com.cramer.abts.validation.ValidationResult;
import com.cramer.abts.web.dto.RefinementApplyRequest;
import com.cramer.abts.web.dto.RefinementApplyResponse;
import com.cramer.platform.common.json.Json;
import com.cramer.platform.integration.openrouter.OpenRouterClient;
import com.cramer.platform.integration.openrouter.OpenRouterProperties;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefinementServiceTest {

    private ContentValidator validator;
    private RefinementService service;

    @BeforeEach
    void setUp() {
        validator = mock(ContentValidator.class);
        OpenRouterProperties props = new OpenRouterProperties("key", null, "deepseek/deepseek-chat", 120_000);
        ModelResolver resolver = new ModelResolver(props, new ModelCapabilityRegistry());
        OpenRouterClient client = mock(OpenRouterClient.class);
        AbtsProperties abtsProps = new AbtsProperties(new AbtsProperties.Streaming(1_800_000, 600_000, 8, 4), 5);
        service = new RefinementService(client, resolver, validator, abtsProps);
    }

    private ObjectNode content() {
        ObjectNode root = Json.mapper().createObjectNode();
        ArrayNode questions = root.putArray("questions");
        ObjectNode q = questions.addObject();
        q.put("question_number", 1);
        q.putArray("correct_answer").add("old");
        return root;
    }

    @Test
    void appliesAcceptedHunkAndRevalidates() {
        when(validator.validate(any(), anyInt(), any(), any())).thenReturn(new ValidationResult());

        ArrayNode after = Json.mapper().createArrayNode();
        after.add("new");
        Hunk accepted = new Hunk("hunk-0", "replace", "/questions/0/correct_answer",
                null, after, "fix answer");
        RefinementApplyRequest request = new RefinementApplyRequest(content(), List.of(accepted), "reading", 1, null);

        RefinementApplyResponse response = service.applyAccepted(request);

        assertThat(response.content().path("questions").get(0).path("correct_answer").get(0).asText())
                .isEqualTo("new");
        assertThat(response.skipped()).isEmpty();
        assertThat(response.validation().valid()).isTrue();
    }

    @Test
    void skipsHunkWithUnresolvablePath() {
        when(validator.validate(any(), anyInt(), any(), any())).thenReturn(new ValidationResult());

        Hunk bad = new Hunk("hunk-bad", "replace", "/questions/9/correct_answer",
                null, Json.mapper().getNodeFactory().textNode("x"), "out of range");
        RefinementApplyRequest request = new RefinementApplyRequest(content(), List.of(bad), "reading", 1, null);

        RefinementApplyResponse response = service.applyAccepted(request);

        assertThat(response.skipped()).containsExactly("hunk-bad");
    }
}
