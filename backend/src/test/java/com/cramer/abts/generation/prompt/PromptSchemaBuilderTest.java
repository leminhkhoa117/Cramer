package com.cramer.abts.generation.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptSchemaBuilderTest {

    private final PromptSchemaBuilder schemas = new PromptSchemaBuilder();

    @Test
    void readingQuestionsSchemaIsStrictCompatible() {
        ObjectNode schema = schemas.readingQuestionsSchema();
        assertThat(schema.path("type").asText()).isEqualTo("object");
        assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.path("required").toString()).contains("questions");

        JsonNode question = schema.path("properties").path("questions").path("items");
        assertThat(question.path("additionalProperties").asBoolean()).isFalse();
        // correct_answer is always an array of strings (no union type)
        assertThat(question.path("properties").path("correct_answer").path("type").asText()).isEqualTo("array");
        assertThat(question.path("properties").path("correct_answer").path("items").path("type").asText())
                .isEqualTo("string");
    }

    @Test
    void listeningStemsSchemaHasNoAnswers() {
        ObjectNode schema = schemas.listeningStemsSchema();
        JsonNode question = schema.path("properties").path("questions").path("items");
        assertThat(question.path("properties").has("correct_answer")).isFalse();
        assertThat(schema.path("properties").has("section_layout")).isTrue();
    }

    @Test
    void writingTaskSchemaCarriesTaskTypeMetaFields() {
        ObjectNode schema = schemas.writingTaskSchema();
        JsonNode props = schema.path("properties");
        assertThat(props.has("chart_data")).isTrue();
        assertThat(props.has("letter_context")).isTrue();
        assertThat(props.has("essay_metadata")).isTrue();
        assertThat(schema.path("required").toString()).contains("task_prompt").contains("word_requirement");
    }
}
