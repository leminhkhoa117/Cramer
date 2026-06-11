package com.cramer.assessment.service;

import com.cramer.platform.common.ielts.QuestionType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Objective answer grading (SPEC-12 §4). Pure, stateless: operates on a question type, its
 * {@code correct_answer} JSON, and the user's submitted value.
 *
 * <p>Normalisation: underscores → spaces, trim, lowercase, collapse internal whitespace.
 * For {@link QuestionType#multiSelect()} types the user's selected set is compared to the
 * correct set as an unordered set (the fix for the old scalar-vs-array mis-scoring).
 */
@Service
public class ScoringService {

    /**
     * @param type          the question type
     * @param correctAnswer the {@code correct_answer} JSON (array of acceptable values, or scalar)
     * @param userValue     the user's submitted answer (raw text; for multi-select, delimited by
     *                      {@code ,} or {@code ;})
     * @return true if the answer is correct
     */
    public boolean isCorrect(QuestionType type, JsonNode correctAnswer, String userValue) {
        if (userValue == null || userValue.isBlank()) {
            return false;
        }
        Set<String> correct = normalizedSet(correctAnswer);
        if (correct.isEmpty()) {
            return false;
        }
        if (type != null && type.multiSelect()) {
            return splitNormalized(userValue).equals(correct);
        }
        return correct.contains(normalize(userValue));
    }

    /** Extract the correct answers as a normalized set (handles JSON array or scalar). */
    private Set<String> normalizedSet(JsonNode correctAnswer) {
        Set<String> out = new LinkedHashSet<>();
        if (correctAnswer == null || correctAnswer.isNull()) {
            return out;
        }
        if (correctAnswer.isArray()) {
            correctAnswer.forEach(node -> addNormalized(out, node.asText()));
        } else {
            // a scalar correct answer may itself encode a multi-value string
            splitNormalized(correctAnswer.asText()).forEach(v -> out.add(v));
        }
        return out;
    }

    private Set<String> splitNormalized(String value) {
        Set<String> out = new LinkedHashSet<>();
        for (String part : value.split("[,;]")) {
            addNormalized(out, part);
        }
        return out;
    }

    private void addNormalized(Set<String> out, String raw) {
        if (raw != null) {
            String n = normalize(raw);
            if (!n.isEmpty()) {
                out.add(n);
            }
        }
    }

    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('_', ' ').trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
