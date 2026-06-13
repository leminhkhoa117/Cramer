package com.cramer.abts.generation;

import com.cramer.abts.domain.StreamEvent;
import com.cramer.abts.generation.prompt.WritingPromptBuilder;
import com.cramer.abts.web.dto.PartConfig;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.common.json.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Writing three-phase generator (SPEC-21 §3.3): task (cached) → sample answer (cached) → band
 * breakdown. Merges everything into one section object so {@code sample_answer} /
 * {@code band_breakdown} / {@code key_phrases} / {@code grading_notes} are persisted (SPEC-24 §4.4).
 */
@Component
public class WritingGenerator implements PartGenerator {

    private final WritingPromptBuilder prompts;

    public WritingGenerator(WritingPromptBuilder prompts) {
        this.prompts = prompts;
    }

    @Override
    public Skill skill() {
        return Skill.WRITING;
    }

    @Override
    public ObjectNode generatePart(int taskNumber, PartConfig config, GenerationContext ctx) {
        ctx.emit(StreamEvent.progress(15, taskNumber, null, "Generating writing task"));
        JsonNode task = ctx.runPhase("task", prompts.taskPhase(config, taskNumber,
                ctx.customInstructions()), taskNumber, true);

        ctx.emit(StreamEvent.progress(50, taskNumber, null, "Generating sample answer"));
        JsonNode sample = ctx.runPhase("sample", prompts.samplePhase(Json.toJson(task), ctx.language(),
                ctx.customInstructions()), taskNumber, true);
        String sampleAnswer = sample.path("sample_answer").asText("");

        ctx.emit(StreamEvent.progress(80, taskNumber, null, "Generating band breakdown"));
        JsonNode band = ctx.runPhase("band", prompts.bandPhase(Json.toJson(task), sampleAnswer,
                ctx.language(), ctx.customInstructions()), taskNumber, false);

        ObjectNode merged = task.deepCopy();
        merged.put("sample_answer", sampleAnswer);
        merged.set("band_breakdown", band.path("band_breakdown"));
        merged.set("key_phrases", band.path("key_phrases"));
        merged.set("grading_notes", band.path("grading_notes"));
        ctx.emit(StreamEvent.progress(90, taskNumber, null, "Writing task assembled"));
        return merged;
    }
}
