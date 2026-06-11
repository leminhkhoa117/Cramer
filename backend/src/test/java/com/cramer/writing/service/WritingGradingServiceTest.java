package com.cramer.writing.service;

import com.cramer.platform.integration.llm.DeepSeekClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WritingGradingServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock DeepSeekClient deepSeek;

    private WritingGradingService service() {
        return new WritingGradingService(deepSeek, new WritingBandCalculator());
    }

    private JsonNode json(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("an empty essay scores 0 without calling the API (local shortcut)")
    void emptyEssayShortcut() {
        GradingOutcome o = service().grade(2, "   ", "Discuss...", null, null);
        assertThat(o.overallBand().doubleValue()).isEqualTo(0.0);
        verify(deepSeek, never()).chatJson(any(), any(), any(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("a normal essay is graded by DeepSeek; overall is recomputed from criteria, ignoring the model's overall")
    void gradesAndRecomputesOverall() {
        when(deepSeek.chatJson(any(), any(), any(), anyDouble(), anyInt())).thenReturn(json("""
                {
                  "band_scores": {"taskResponse":6,"coherenceCohesion":7,"lexicalResource":6,"grammaticalRange":6.5},
                  "overall_band": 9.0,
                  "feedback_summary": "Solid response.",
                  "sentence_corrections": [{"from":"a","to":"b"}]
                }
                """));

        String essay = "word ".repeat(260);
        GradingOutcome o = service().grade(2, essay, "Discuss both views", "a bar chart", null);

        assertThat(o.overallBand().doubleValue()).isEqualTo(6.5); // avg(6,7,6,6.5)=6.375 -> 6.5, NOT 9.0
        assertThat(o.aiFeedback().get("feedback_summary").asText()).isEqualTo("Solid response.");
        assertThat(o.aiFeedback().has("sentence_corrections")).isTrue();
        assertThat(o.aiFeedback().has("band_scores")).isFalse(); // bands kept separate
    }
}
