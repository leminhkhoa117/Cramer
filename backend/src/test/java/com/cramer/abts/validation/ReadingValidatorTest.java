package com.cramer.abts.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReadingValidatorTest {

    private final ReadingValidator validator = new ReadingValidator();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** A valid Part 1 (Q1–13) with two question types and a long-enough passage. */
    private JsonNode validPart1() {
        StringBuilder q = new StringBuilder();
        for (int n = 1; n <= 13; n++) {
            if (n > 1) {
                q.append(',');
            }
            String type = n <= 7 ? "TRUE_FALSE_NOT_GIVEN" : "MULTIPLE_CHOICE";
            q.append("{\"question_number\":").append(n).append(",\"question_type\":\"").append(type).append("\"}");
        }
        String passage = ("word ".repeat(720)).trim();
        return json("{\"passage_text\":\"" + passage + "\",\"questions\":[" + q + "]}");
    }

    @Test
    @DisplayName("a well-formed Part 1 with 13 questions and 2 types validates")
    void validReading() {
        ValidationResult r = validator.validate(validPart1(), 1);
        assertThat(r.isValid()).isTrue();
        assertThat(r.errors()).isEmpty();
    }

    @Test
    @DisplayName("missing passage and empty questions are errors")
    void missingEssentials() {
        ValidationResult r = validator.validate(json("{\"questions\":[]}"), 1);
        assertThat(r.isValid()).isFalse();
        assertThat(r.errors()).anyMatch(i -> i.id().equals("rd-passage-missing"));
        assertThat(r.errors()).anyMatch(i -> i.id().equals("rd-questions-missing"));
    }

    @Test
    @DisplayName("wrong count, out-of-range numbers, duplicates, and single-type are all flagged")
    void countAndRangeErrors() {
        // 3 questions, all MULTIPLE_CHOICE; q40 is out of Part 1 range; q5 duplicated
        JsonNode content = json("""
                {"passage_text":"%s",
                 "questions":[
                   {"question_number":5,"question_type":"MULTIPLE_CHOICE"},
                   {"question_number":5,"question_type":"MULTIPLE_CHOICE"},
                   {"question_number":40,"question_type":"MULTIPLE_CHOICE"}
                 ]}
                """.formatted(("word ".repeat(720)).trim()));
        ValidationResult r = validator.validate(content, 1);

        assertThat(r.errors()).anyMatch(i -> i.id().equals("rd-count-mismatch"));
        assertThat(r.errors()).anyMatch(i -> i.id().equals("rd-q40-out-of-range"));
        assertThat(r.errors()).anyMatch(i -> i.id().equals("rd-q5-duplicate"));
        assertThat(r.errors()).anyMatch(i -> i.id().equals("rd-too-few-types"));
    }

    @Test
    @DisplayName("a short passage is a warning, not an error")
    void shortPassageWarning() {
        StringBuilder q = new StringBuilder();
        for (int n = 1; n <= 13; n++) {
            if (n > 1) {
                q.append(',');
            }
            String type = n <= 7 ? "MATCHING_HEADINGS" : "FILL_IN_BLANK";
            q.append("{\"question_number\":").append(n).append(",\"question_type\":\"").append(type).append("\"}");
        }
        JsonNode content = json("{\"passage_text\":\"too short\",\"questions\":[" + q + "]}");
        ValidationResult r = validator.validate(content, 1);

        assertThat(r.isValid()).isTrue(); // warning doesn't block
        assertThat(r.warnings()).anyMatch(i -> i.id().equals("rd-passage-short"));
    }
}
