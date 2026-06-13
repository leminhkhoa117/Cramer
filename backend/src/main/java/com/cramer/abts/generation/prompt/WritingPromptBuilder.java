package com.cramer.abts.generation.prompt;

import com.cramer.abts.domain.FactsMode;
import com.cramer.abts.web.dto.PartConfig;
import org.springframework.stereotype.Component;

/**
 * Writing prompt builder (SPEC-22 §2, §5.4): task phase (prompt + requirement + task-type meta),
 * sample-answer phase, then band-breakdown phase. Task 1 variants carry chart/letter context;
 * Task 2 carries essay metadata.
 */
@Component
public class WritingPromptBuilder {

    private final PromptFragments fragments;
    private final PromptSchemaBuilder schemas;

    public WritingPromptBuilder(PromptFragments fragments, PromptSchemaBuilder schemas) {
        this.fragments = fragments;
        this.schemas = schemas;
    }

    public PhasePrompt taskPhase(PartConfig cfg, int taskNumber, String custom) {
        String taskType = resolveTaskType(cfg, taskNumber);
        String system = fragments.ieltsConstraints();
        String meta = switch (taskType) {
            case "ACADEMIC_TASK_1" -> "Provide `chart_data` (chart_type, description, labels[], values[]). Min 150 words.";
            case "GENERAL_TASK_1" -> "Provide `letter_context` (situation + bullet points to cover). Min 150 words.";
            default -> "Provide `essay_metadata` (prompt_type, key_points[]). Min 250 words.";
        };
        String user = String.join("\n\n",
                "Generate the IELTS Writing " + (taskNumber == 1 ? "Task 1" : "Task 2") + " prompt.",
                "Task type: " + taskType, "Topic: " + safeTopic(cfg), "Difficulty: " + safeDifficulty(cfg),
                meta,
                fragments.factsMode(isStrict(cfg), cfg == null ? null : cfg.safeFacts()),
                fragments.customInstructions(custom),
                "Return JSON with task_prompt, word_requirement, task_type, and the matching meta field (others null).");
        return new PhasePrompt(system, user, "writing_task", schemas.writingTaskSchema());
    }

    public PhasePrompt samplePhase(String taskJson, String language, String custom) {
        String system = fragments.ieltsConstraints();
        String user = String.join("\n\n",
                "Write a band-9 model sample answer for this IELTS writing task:",
                taskJson,
                fragments.customInstructions(custom),
                "Return JSON: { \"sample_answer\": \"...\" }.");
        return new PhasePrompt(system, user, "writing_sample", schemas.writingSampleSchema());
    }

    public PhasePrompt bandPhase(String taskJson, String sampleAnswer, String language, String custom) {
        String system = fragments.ieltsConstraints() + "\n" + fragments.explanationLanguage(language);
        String user = String.join("\n\n",
                "Produce a band breakdown for the sample answer against the four IELTS writing criteria",
                "(Task Achievement/Response, Coherence & Cohesion, Lexical Resource, Grammatical Range & Accuracy).",
                "Task:\n" + taskJson,
                "Sample answer:\n\"\"\"\n" + sampleAnswer + "\n\"\"\"",
                fragments.customInstructions(custom),
                "Return JSON: { \"band_breakdown\": \"...\", \"key_phrases\": [...], \"grading_notes\": \"...\" }.");
        return new PhasePrompt(system, user, "writing_band", schemas.writingBandSchema());
    }

    public String resolveTaskType(PartConfig cfg, int taskNumber) {
        if (cfg != null && cfg.taskType() != null && !cfg.taskType().isBlank()) {
            return cfg.taskType().trim().toUpperCase();
        }
        return taskNumber == 1 ? "ACADEMIC_TASK_1" : "TASK_2";
    }

    // ---- helpers ----

    private boolean isStrict(PartConfig cfg) {
        return cfg != null && FactsMode.parse(cfg.factsMode()) == FactsMode.STRICT && cfg.safeFacts().size() >= 3;
    }

    private String safeTopic(PartConfig cfg) {
        return (cfg == null || cfg.topic() == null || cfg.topic().isBlank()) ? "a general topic" : cfg.topic().trim();
    }

    private String safeDifficulty(PartConfig cfg) {
        return (cfg == null || cfg.difficulty() == null || cfg.difficulty().isBlank()) ? "INTERMEDIATE" : cfg.difficulty().trim();
    }
}
