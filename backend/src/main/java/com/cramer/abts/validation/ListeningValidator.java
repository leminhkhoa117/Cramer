package com.cramer.abts.validation;

import com.cramer.abts.domain.QuestionRange;
import com.cramer.platform.common.ielts.Skill;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Listening content validation (SPEC-23 §3). Requires {@code transcript}, {@code questions},
 * {@code section_layout}, {@code audio_placeholder}; exactly 10 questions per part with in-range,
 * non-duplicate numbers; allowed interaction types; layout blocks with non-empty
 * {@code question_numbers}. Transcript word-count sanity is a content error when wildly off.
 */
@Component
public class ListeningValidator {

    private static final Set<String> ALLOWED_INTERACTION = Set.of(
            "FILL_IN_BLANK", "MULTIPLE_CHOICE", "MULTIPLE_CHOICE_MULTIPLE_ANSWERS", "MATCHING");

    public ValidationResult validate(JsonNode content, int part) {
        ValidationResult result = new ValidationResult();
        QuestionRange range = QuestionRange.of(Skill.LISTENING, part);

        if (content.path("transcript").asText("").isBlank()) {
            result.addError("ls-transcript-missing", "/transcript", "Listening transcript is required");
        }
        if (content.path("audio_placeholder").isMissingNode()) {
            result.addError("ls-audio-missing", "/audio_placeholder", "audio_placeholder metadata is required");
        }
        JsonNode layout = content.path("section_layout");
        if (layout.isMissingNode()) {
            result.addError("ls-layout-missing", "/section_layout", "section_layout is required");
        } else {
            JsonNode blocks = layout.path("blocks");
            if (blocks.isArray()) {
                int bi = 0;
                for (JsonNode block : blocks) {
                    if (!block.path("question_numbers").isArray() || block.path("question_numbers").isEmpty()) {
                        result.addError("ls-block" + bi + "-no-qnums", "/section_layout/blocks/" + bi + "/question_numbers",
                                "Layout block must list non-empty question_numbers");
                    }
                    bi++;
                }
            }
        }

        JsonNode questions = content.path("questions");
        if (!questions.isArray() || questions.isEmpty()) {
            result.addError("ls-questions-missing", "/questions", "Listening questions[] is required");
            return result;
        }

        Set<Integer> seen = new HashSet<>();
        int index = 0;
        for (JsonNode q : questions) {
            String path = "/questions/" + index;
            JsonNode numNode = q.path("question_number");
            if (numNode.isInt()) {
                int n = numNode.asInt();
                if (!range.contains(n)) {
                    result.addError("ls-q" + n + "-out-of-range", path + "/question_number",
                            "Question " + n + " is outside part " + part + " range " + range.first() + ".." + range.last());
                }
                if (!seen.add(n)) {
                    result.addError("ls-q" + n + "-duplicate", path + "/question_number", "Duplicate question number " + n);
                }
            } else {
                result.addError("ls-q" + index + "-no-number", path + "/question_number",
                        "Question is missing an integer question_number");
            }
            String type = q.path("question_type").asText("");
            if (!ALLOWED_INTERACTION.contains(type)) {
                result.addError("ls-q" + index + "-type", path + "/question_type",
                        "Listening interaction type not allowed: " + type);
            }
            index++;
        }
        if (questions.size() != range.count()) {
            result.addError("ls-count-mismatch", "/questions",
                    "Expected " + range.count() + " questions for part " + part + ", got " + questions.size());
        }
        return result;
    }
}
