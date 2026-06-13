package com.cramer.abts.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListeningWritingValidatorTest {

    private final ListeningValidator listening = new ListeningValidator();
    private final WritingValidator writing = new WritingValidator();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Listening part 1 needs transcript/layout/audio and exactly 10 in-range questions")
    void listeningEssentials() {
        ValidationResult r = listening.validate(json("{\"questions\":[]}"), 1);
        assertThat(r.errors()).anyMatch(i -> i.id().equals("ls-transcript-missing"));
        assertThat(r.errors()).anyMatch(i -> i.id().equals("ls-audio-missing"));
        assertThat(r.errors()).anyMatch(i -> i.id().equals("ls-layout-missing"));
        assertThat(r.errors()).anyMatch(i -> i.id().equals("ls-questions-missing"));
    }

    @Test
    @DisplayName("Listening flags a disallowed interaction type and a count mismatch")
    void listeningTypeAndCount() {
        JsonNode content = json("""
                {"transcript":"speaker one ...","audio_placeholder":{},
                 "section_layout":{"blocks":[{"question_numbers":[1]}]},
                 "questions":[{"question_number":1,"question_type":"ESSAY"}]}
                """);
        ValidationResult r = listening.validate(content, 1);
        assertThat(r.errors()).anyMatch(i -> i.id().equals("ls-q0-type"));
        assertThat(r.errors()).anyMatch(i -> i.id().equals("ls-count-mismatch"));
    }

    @Test
    @DisplayName("Academic Task 1 requires chart_data; missing prompt is an error")
    void writingAcademicTask1() {
        ValidationResult r = writing.validate(json("{\"word_requirement\":150}"), "ACADEMIC_TASK_1");
        assertThat(r.errors()).anyMatch(i -> i.id().equals("wr-prompt-missing"));
        assertThat(r.errors()).anyMatch(i -> i.id().equals("wr-chartdata-missing"));
    }

    @Test
    @DisplayName("Task 2 with prompt + word_requirement + essay_metadata is valid")
    void writingTask2Valid() {
        JsonNode content = json("""
                {"task_prompt":"Some people believe ...","word_requirement":250,
                 "essay_metadata":{"position":"discuss"}}
                """);
        ValidationResult r = writing.validate(content, "TASK_2");
        assertThat(r.isValid()).isTrue();
    }
}
