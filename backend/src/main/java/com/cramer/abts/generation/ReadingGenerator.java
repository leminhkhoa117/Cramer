package com.cramer.abts.generation;

import com.cramer.abts.domain.StreamEvent;
import com.cramer.abts.generation.prompt.ReadingPromptBuilder;
import com.cramer.abts.web.dto.PartConfig;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.common.json.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Reading two-phase generator (SPEC-21 §3.1): passage phase (cached) then questions phase. Merges
 * passage + questions into {@code { section: { passage_text }, questions: [...] }}.
 */
@Component
public class ReadingGenerator implements PartGenerator {

    private final ReadingPromptBuilder prompts;

    public ReadingGenerator(ReadingPromptBuilder prompts) {
        this.prompts = prompts;
    }

    @Override
    public Skill skill() {
        return Skill.READING;
    }

    @Override
    public ObjectNode generatePart(int part, PartConfig config, GenerationContext ctx) {
        ctx.emit(StreamEvent.progress(15, part, null, "Generating reading passage"));
        JsonNode passage = ctx.runPhase("passage", prompts.passagePhase(config, part, ctx.language(),
                ctx.customInstructions()), part, true);
        String passageText = passage.path("section").path("passage_text").asText("");

        ctx.emit(StreamEvent.progress(55, part, null, "Generating reading questions"));
        JsonNode questionsNode = ctx.runPhase("questions", prompts.questionsPhase(config, part, ctx.language(),
                ctx.customInstructions(), passageText), part, false);

        ObjectNode merged = Json.mapper().createObjectNode();
        ObjectNode section = merged.putObject("section");
        section.put("passage_text", passageText);
        merged.set("questions", questionsNode.path("questions").isArray()
                ? questionsNode.get("questions") : Json.mapper().createArrayNode());
        ctx.emit(StreamEvent.progress(90, part, null, "Reading part assembled"));
        return merged;
    }
}
