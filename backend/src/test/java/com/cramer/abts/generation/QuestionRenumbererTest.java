package com.cramer.abts.generation;

import com.cramer.platform.common.ielts.Skill;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionRenumbererTest {

    private final QuestionRenumberer renumberer = new QuestionRenumberer();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Reading Part 2 questions are renumbered to 14..26 with answers realigned")
    void readingPart2() {
        // model produced 1..13; merging as Part 2 must shift to 14..26
        JsonNode content = json("""
                {"questions":[{"question_number":1},{"question_number":2},{"question_number":3}],
                 "answers":[{"question_number":2,"value":"B"}]}
                """);
        JsonNode out = renumberer.renumber(content, Skill.READING, 2);

        assertThat(out.path("questions").get(0).path("question_number").asInt()).isEqualTo(14);
        assertThat(out.path("questions").get(2).path("question_number").asInt()).isEqualTo(16);
        // answer that pointed at old #2 now points at 15
        assertThat(out.path("answers").get(0).path("question_number").asInt()).isEqualTo(15);
    }

    @Test
    @DisplayName("Listening Part 4 starts at 31")
    void listeningPart4() {
        JsonNode content = json("{\"questions\":[{\"question_number\":1},{\"question_number\":2}]}");
        JsonNode out = renumberer.renumber(content, Skill.LISTENING, 4);
        assertThat(out.path("questions").get(0).path("question_number").asInt()).isEqualTo(31);
        assertThat(out.path("questions").get(1).path("question_number").asInt()).isEqualTo(32);
    }

    @Test
    @DisplayName("Writing content is returned unchanged (not number-ranged)")
    void writingUnchanged() {
        JsonNode content = json("{\"task_prompt\":\"x\"}");
        JsonNode out = renumberer.renumber(content, Skill.WRITING, 1);
        assertThat(out).isEqualTo(content);
    }
}
