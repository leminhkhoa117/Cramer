package com.cramer.abts.generation;

import com.cramer.abts.domain.StreamEvent;
import com.cramer.abts.generation.prompt.ListeningPromptBuilder;
import com.cramer.abts.web.dto.PartConfig;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.common.json.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listening three-phase generator (SPEC-21 §3.2): transcript (cached) → stems+layout (cached,
 * no answers) → answers. Merges answers back onto the stems by {@code question_number} to produce
 * {@code { transcript, audio_placeholder, section_layout, questions: [...with answers] }}.
 */
@Component
public class ListeningGenerator implements PartGenerator {

    private final ListeningPromptBuilder prompts;

    public ListeningGenerator(ListeningPromptBuilder prompts) {
        this.prompts = prompts;
    }

    @Override
    public Skill skill() {
        return Skill.LISTENING;
    }

    @Override
    public ObjectNode generatePart(int part, PartConfig config, GenerationContext ctx) {
        ctx.emit(StreamEvent.progress(10, part, null, "Generating transcript"));
        JsonNode transcript = ctx.runPhase("transcript", prompts.transcriptPhase(config, part,
                ctx.customInstructions()), part, true);
        String transcriptText = transcript.path("transcript").asText("");
        String audioPlaceholder = transcript.path("audio_placeholder").asText("");

        ctx.emit(StreamEvent.progress(40, part, null, "Generating question stems + layout"));
        JsonNode stems = ctx.runPhase("stems", prompts.stemsPhase(config, part, transcriptText,
                ctx.customInstructions()), part, true);

        ctx.emit(StreamEvent.progress(70, part, null, "Generating answers"));
        JsonNode answers = ctx.runPhase("answers", prompts.answersPhase(part, transcriptText,
                Json.toJson(stems), ctx.language(), ctx.customInstructions()), part, false);

        Map<Integer, JsonNode> answerByNumber = new HashMap<>();
        if (answers.path("answers").isArray()) {
            for (JsonNode a : answers.get("answers")) {
                if (a.path("question_number").isInt()) {
                    answerByNumber.put(a.get("question_number").asInt(), a);
                }
            }
        }

        ArrayNode mergedQuestions = Json.mapper().createArrayNode();
        if (stems.path("questions").isArray()) {
            for (JsonNode stem : stems.get("questions")) {
                ObjectNode q = stem.deepCopy();
                int number = q.path("question_number").asInt(-1);
                JsonNode answer = answerByNumber.get(number);
                if (answer != null) {
                    q.set("correct_answer", answer.path("correct_answer"));
                    q.set("explanation", answer.path("explanation"));
                }
                mergedQuestions.add(q);
            }
        }

        ObjectNode merged = Json.mapper().createObjectNode();
        merged.put("transcript", transcriptText);
        merged.put("audio_placeholder", audioPlaceholder);
        merged.set("section_layout", stems.path("section_layout").isObject()
                ? stems.get("section_layout") : Json.mapper().createObjectNode());
        merged.set("questions", mergedQuestions);
        ctx.emit(StreamEvent.progress(90, part, null, "Listening part assembled"));
        return merged;
    }
}
