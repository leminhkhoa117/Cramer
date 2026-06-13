package com.cramer.abts.validation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Writing content validation (SPEC-23 §4). Requires {@code task_prompt} and
 * {@code word_requirement}; warns on missing {@code task_type}. Per task type: Academic Task 1
 * needs {@code chart_data}, GT Task 1 needs {@code letter_context}, Task 2 needs
 * {@code essay_metadata}. Sample-answer length is a warning only.
 */
@Component
public class WritingValidator {

    public ValidationResult validate(JsonNode content, String taskType) {
        ValidationResult result = new ValidationResult();

        if (content.path("task_prompt").asText("").isBlank()) {
            result.addError("wr-prompt-missing", "/task_prompt", "Writing task_prompt is required");
        }
        if (content.path("word_requirement").isMissingNode()) {
            result.addError("wr-wordreq-missing", "/word_requirement", "word_requirement is required");
        }

        String type = taskType == null ? content.path("task_type").asText("") : taskType;
        if (type == null || type.isBlank()) {
            result.addWarning("wr-tasktype-missing", "/task_type", "task_type is missing");
        } else {
            switch (type) {
                case "ACADEMIC_TASK_1" -> requirePresent(result, content, "chart_data",
                        "wr-chartdata-missing", "Academic Task 1 requires chart_data");
                case "GENERAL_TASK_1" -> requirePresent(result, content, "letter_context",
                        "wr-letter-missing", "General Task 1 requires letter_context");
                case "TASK_2" -> requirePresent(result, content, "essay_metadata",
                        "wr-essaymeta-missing", "Task 2 requires essay_metadata");
                default -> result.addWarning("wr-tasktype-unknown", "/task_type", "Unknown task_type: " + type);
            }
        }

        JsonNode sample = content.path("sample_answer");
        if (sample.isTextual() && wordCount(sample.asText()) < 150) {
            result.addWarning("wr-sample-short", "/sample_answer", "Sample answer is shorter than recommended");
        }
        return result;
    }

    private static void requirePresent(ValidationResult result, JsonNode content, String field, String id, String msg) {
        if (content.path(field).isMissingNode()) {
            result.addError(id, "/" + field, msg);
        }
    }

    private static int wordCount(String text) {
        return text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
    }
}
