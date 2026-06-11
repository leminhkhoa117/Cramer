package com.cramer.writing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;

class WritingBandCalculatorTest {

    private final WritingBandCalculator calc = new WritingBandCalculator();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("per-task overall is the average of criterion bands, rounded to 0.5 (not the model's value)")
    void overallFromBandScores() {
        // TR 6, CC 7, LR 6, GRA 6.5 -> avg 6.375 -> 6.5
        JsonNode bands = json("{\"taskResponse\":6,\"coherence\":7,\"lexical\":6,\"grammar\":6.5,\"overall_band\":9}");
        assertThat(calc.overallFromBandScores(bands)).isEqualTo(6.5);
    }

    @Test
    @DisplayName("weighted overall = Task1·1/3 + Task2·2/3 rounded to 0.5")
    void weightedOverall() {
        // 6.0/3 + 7.0*2/3 = 2 + 4.667 = 6.667 -> 6.5
        assertThat(calc.weightedOverall(6.0, 7.0)).isEqualTo(6.5);
        // 7.0/3 + 8.0*2/3 = 2.333 + 5.333 = 7.667 -> 7.5
        assertThat(calc.weightedOverall(7.0, 8.0)).isEqualTo(7.5);
    }

    @Test
    @DisplayName("a single task contributes its own band")
    void weightedSingleTask() {
        assertThat(calc.weightedOverall(null, 6.5)).isEqualTo(6.5);
        assertThat(calc.weightedOverall(7.0, null)).isEqualTo(7.0);
        assertThat(calc.weightedOverall(null, null)).isNull();
    }

    @Test
    @DisplayName("local shortcuts: empty essay -> 0, under 20 words -> 1, otherwise graded")
    void localShortcuts() {
        assertThat(calc.localShortcutBand("  ")).hasValue(0.0);
        assertThat(calc.localShortcutBand("only a few words here")).hasValue(1.0);
        OptionalDouble graded = calc.localShortcutBand("word ".repeat(30));
        assertThat(graded).isEmpty();
    }

    @Test
    @DisplayName("word count is whitespace-delimited")
    void wordCount() {
        assertThat(calc.countWords("the quick brown fox")).isEqualTo(4);
        assertThat(calc.countWords("  ")).isZero();
    }
}
