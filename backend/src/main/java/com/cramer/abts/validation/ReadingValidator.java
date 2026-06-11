package com.cramer.abts.validation;

import com.cramer.abts.domain.QuestionRange;
import com.cramer.platform.common.ielts.QuestionType;
import com.cramer.platform.common.ielts.Skill;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Reading content validation (SPEC-23 §2). Pure and stateless: takes the generated content JSON
 * and the part number, returns structured issues. Checks (errors unless noted):
 *
 * <ul>
 *   <li>{@code passage_text} present (non-blank);</li>
 *   <li>{@code questions[]} present and non-empty;</li>
 *   <li>each question has a number + allowed {@link QuestionType};</li>
 *   <li>question count matches the part range, numbers are in-range, sequential, no duplicates;</li>
 *   <li>at least two distinct question types in the part;</li>
 *   <li>passage word count below ~700 is a <em>warning</em> only.</li>
 * </ul>
 */
@Component
public class ReadingValidator {

    private static final Set<QuestionType> ALLOWED = Set.of(
            QuestionType.FILL_IN_BLANK, QuestionType.SUMMARY_COMPLETION, QuestionType.SUMMARY_COMPLETION_OPTIONS,
            QuestionType.TRUE_FALSE_NOT_GIVEN, QuestionType.YES_NO_NOT_GIVEN, QuestionType.MATCHING_INFORMATION,
            QuestionType.MATCHING_HEADINGS, QuestionType.MATCHING_FEATURES, QuestionType.MATCHING_SENTENCE_ENDINGS,
            QuestionType.MULTIPLE_CHOICE, QuestionType.MULTIPLE_CHOICE_MULTIPLE_ANSWERS, QuestionType.TABLE_COMPLETION,
            QuestionType.FLOW_CHART_COMPLETION, QuestionType.DIAGRAM_LABEL_COMPLETION);

    public ValidationResult validate(JsonNode content, int part) {
        ValidationResult result = new ValidationResult();
        QuestionRange range = QuestionRange.of(Skill.READING, part);

        JsonNode passage = content.path("passage_text");
        if (passage.isMissingNode() || passage.asText("").isBlank()) {
            result.addError("rd-passage-missing", "/passage_text", "Reading passage_text is required");
        } else if (wordCount(passage.asText()) < 700) {
            result.addWarning("rd-passage-short", "/passage_text",
                    "Passage is shorter than a typical IELTS reading passage (~700+ words)");
        }

        JsonNode questions = content.path("questions");
        if (!questions.isArray() || questions.isEmpty()) {
            result.addError("rd-questions-missing", "/questions", "Reading questions[] is required");
            return result;
        }

        Set<Integer> seen = new HashSet<>();
        Set<QuestionType> types = new HashSet<>();
        int index = 0;
        for (JsonNode q : questions) {
            String path = "/questions/" + index;
            JsonNode numNode = q.path("question_number");
            if (!numNode.isInt()) {
                result.addError("rd-q" + index + "-no-number", path + "/question_number",
                        "Question is missing an integer question_number");
            } else {
                int n = numNode.asInt();
                if (!range.contains(n)) {
                    result.addError("rd-q" + n + "-out-of-range", path + "/question_number",
                            "Question " + n + " is outside part " + part + " range " + range.first() + ".." + range.last());
                }
                if (!seen.add(n)) {
                    result.addError("rd-q" + n + "-duplicate", path + "/question_number",
                            "Duplicate question number " + n);
                }
            }
            QuestionType type = parseType(q.path("question_type").asText(null));
            if (type == null) {
                result.addError("rd-q" + index + "-bad-type", path + "/question_type",
                        "Unknown or missing question_type");
            } else if (!ALLOWED.contains(type)) {
                result.addError("rd-q" + index + "-type-not-allowed", path + "/question_type",
                        "Question type not allowed for Reading: " + type);
            } else {
                types.add(type);
            }
            index++;
        }

        if (questions.size() != range.count()) {
            result.addError("rd-count-mismatch", "/questions",
                    "Expected " + range.count() + " questions for part " + part + ", got " + questions.size());
        }
        if (types.size() < 2) {
            result.addError("rd-too-few-types", "/questions",
                    "A Reading part should use at least two question types");
        }
        return result;
    }

    private static QuestionType parseType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return QuestionType.from(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int wordCount(String text) {
        return text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
    }
}
