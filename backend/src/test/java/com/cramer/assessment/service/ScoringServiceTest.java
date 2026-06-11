package com.cramer.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cramer.platform.common.ielts.QuestionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScoringServiceTest {

    private final ScoringService scoring = new ScoringService();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("single multiple-choice matches one correct letter, case-insensitively")
    void singleChoice() {
        JsonNode correct = json("[\"B\"]");
        assertThat(scoring.isCorrect(QuestionType.MULTIPLE_CHOICE, correct, "b")).isTrue();
        assertThat(scoring.isCorrect(QuestionType.MULTIPLE_CHOICE, correct, "C")).isFalse();
    }

    @Test
    @DisplayName("fill-in-blank accepts any acceptable variant and normalises underscores/case")
    void fillBlank() {
        JsonNode correct = json("[\"population\", \"populations\"]");
        assertThat(scoring.isCorrect(QuestionType.FILL_IN_BLANK, correct, "Population")).isTrue();
        assertThat(scoring.isCorrect(QuestionType.FILL_IN_BLANK, correct, "people")).isFalse();

        JsonNode city = json("[\"new york\"]");
        assertThat(scoring.isCorrect(QuestionType.FILL_IN_BLANK, city, "new_york")).isTrue();
    }

    @Test
    @DisplayName("multi-select is graded as an unordered set (SPEC-12 fix)")
    void multiSelectSetEquality() {
        JsonNode correct = json("[\"A\", \"C\"]");
        // correct set regardless of order / whitespace
        assertThat(scoring.isCorrect(QuestionType.MULTIPLE_CHOICE_MULTIPLE_ANSWERS, correct, "C,A")).isTrue();
        assertThat(scoring.isCorrect(QuestionType.MULTIPLE_CHOICE_MULTIPLE_ANSWERS, correct, "A, C")).isTrue();
        // missing one selection
        assertThat(scoring.isCorrect(QuestionType.MULTIPLE_CHOICE_MULTIPLE_ANSWERS, correct, "A")).isFalse();
        // extra wrong selection
        assertThat(scoring.isCorrect(QuestionType.MULTIPLE_CHOICE_MULTIPLE_ANSWERS, correct, "A,C,D")).isFalse();
    }

    @Test
    @DisplayName("blank or null user answers are never correct")
    void blankAnswers() {
        JsonNode correct = json("[\"A\"]");
        assertThat(scoring.isCorrect(QuestionType.MULTIPLE_CHOICE, correct, "")).isFalse();
        assertThat(scoring.isCorrect(QuestionType.MULTIPLE_CHOICE, correct, null)).isFalse();
    }
}
