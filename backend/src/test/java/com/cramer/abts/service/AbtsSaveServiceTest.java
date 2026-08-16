package com.cramer.abts.service;

import com.cramer.abts.web.dto.SaveContentRequest;
import com.cramer.catalog.service.ContentDraftPort;
import com.cramer.platform.common.json.Json;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbtsSaveServiceTest {

    private final ContentDraftPort port = mock(ContentDraftPort.class);
    private final AbtsSaveService service = new AbtsSaveService(port);

    @Test
    void rejectsUnknownQuestionTypes() {
        SaveContentRequest.SaveSectionInput section = new SaveContentRequest.SaveSectionInput(
                "reading", 1, "passage", null, null, null,
                Json.readTree("[{\"question_number\":1,\"question_type\":\"NOT_A_TYPE\"}]"));
        SaveContentRequest request = new SaveContentRequest("ai_generated", null, null, null,
                null, null, List.of(), null, List.of(section));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown question_type");
    }

    @Test
    void rejectsReadingSectionsWithoutQuestions() {
        SaveContentRequest.SaveSectionInput section = new SaveContentRequest.SaveSectionInput(
                "reading", 1, "passage", null, null, null, Json.readTree("[]"));
        SaveContentRequest request = new SaveContentRequest("ai_generated", null, null, null,
                null, null, List.of(), null, List.of(section));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one question");
    }

    @Test
    void savesValidSectionsThroughTheDraftPort() {
        when(port.saveDraft(any())).thenReturn(new ContentDraftPort.SaveDraftResult(1L, "ai_generated", 2L, 3, List.of(7L), 1));

        SaveContentRequest.SaveSectionInput section = new SaveContentRequest.SaveSectionInput(
                "reading", 1, "passage", null, null, null,
                Json.readTree("[{\"question_number\":1,\"question_type\":\"FILL_IN_BLANK\"}]"));
        SaveContentRequest request = new SaveContentRequest("ai_generated", null, null, null,
                "AI Test", "INTERMEDIATE", List.of(), null, List.of(section));

        var result = service.save(request);

        assertThat(result.success()).isTrue();
        assertThat(result.sectionIds()).containsExactly(7L);
        assertThat(result.questionCount()).isEqualTo(1);
    }
}
