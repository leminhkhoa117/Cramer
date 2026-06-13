package com.cramer.abts.generation;

import com.cramer.abts.domain.QuestionRange;
import com.cramer.platform.common.ielts.Skill;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Renumbers a part's questions to its canonical range (SPEC-20 §4.1, SPEC-21 §4) during a
 * multi-part merge. Reading P2 → 14..26, Listening P3 → 21..30, etc. Reassigns
 * {@code question_number} sequentially in array order and rewrites any matching
 * {@code answers[].question_number} so answer keys stay aligned. Pure and stateless.
 */
@Component
public class QuestionRenumberer {

    /**
     * Renumber {@code content.questions[]} (and aligned {@code content.answers[]}) to the part's
     * canonical range. Mutates and returns the same node.
     */
    public JsonNode renumber(JsonNode content, Skill skill, int part) {
        if (skill == Skill.WRITING || skill == Skill.SPEAKING) {
            return content; // not number-ranged
        }
        QuestionRange range = QuestionRange.of(skill, part);
        JsonNode questions = content.path("questions");
        if (!questions.isArray()) {
            return content;
        }

        // Map old number → new number, assigning sequentially from range.first().
        int next = range.first();
        java.util.Map<Integer, Integer> remap = new java.util.HashMap<>();
        for (JsonNode q : questions) {
            if (q instanceof ObjectNode obj) {
                Integer oldNum = obj.path("question_number").isInt() ? obj.get("question_number").asInt() : null;
                int newNum = next++;
                if (oldNum != null) {
                    remap.put(oldNum, newNum);
                }
                obj.put("question_number", newNum);
            }
        }

        // Realign answers[] that key on question_number.
        JsonNode answers = content.path("answers");
        if (answers.isArray()) {
            for (JsonNode a : answers) {
                if (a instanceof ObjectNode obj && obj.path("question_number").isInt()) {
                    Integer mapped = remap.get(obj.get("question_number").asInt());
                    if (mapped != null) {
                        obj.put("question_number", mapped);
                    }
                }
            }
        }
        return content;
    }
}
