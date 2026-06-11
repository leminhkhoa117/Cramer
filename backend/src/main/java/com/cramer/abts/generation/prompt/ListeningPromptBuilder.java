package com.cramer.abts.generation.prompt;

import com.cramer.abts.domain.FactsMode;
import com.cramer.abts.domain.QuestionRange;
import com.cramer.abts.web.dto.PartConfig;
import com.cramer.platform.common.ielts.Skill;
import org.springframework.stereotype.Component;

/**
 * Listening prompt builder (SPEC-22 §2): three phases — transcript (+audio placeholder), stems
 * +layout (no answers), then answers keyed by question number. Allowed interaction types only
 * (SPEC-20 §4.2). Phase separation keeps stems answer-free until the answers phase.
 */
@Component
public class ListeningPromptBuilder {

    private final PromptFragments fragments;
    private final QuestionTypeInstructionBuilder typeInstructions;
    private final PromptSchemaBuilder schemas;

    public ListeningPromptBuilder(PromptFragments fragments, QuestionTypeInstructionBuilder typeInstructions,
                                  PromptSchemaBuilder schemas) {
        this.fragments = fragments;
        this.typeInstructions = typeInstructions;
        this.schemas = schemas;
    }

    public PhasePrompt transcriptPhase(PartConfig cfg, int part, String custom) {
        String system = fragments.ieltsConstraints();
        String speakers = (part == 1 || part == 3)
                ? "Use a conversation between two or more named speakers."
                : "Use a single-speaker monologue.";
        String user = String.join("\n\n",
                "Generate ONLY the listening transcript for IELTS Listening Part " + part + ".",
                "Topic: " + safeTopic(cfg), "Difficulty: " + safeDifficulty(cfg),
                speakers + " Label every speaker turn (e.g. \"TUTOR:\").",
                fragments.factsMode(isStrict(cfg), cfg == null ? null : cfg.safeFacts()),
                fragments.customInstructions(custom),
                "Provide an `audio_placeholder` describing the intended audio (accent, pace).",
                "Return JSON: { \"transcript\": \"...\", \"audio_placeholder\": \"...\" }.");
        return new PhasePrompt(system, user, "listening_transcript", schemas.listeningTranscriptSchema());
    }

    public PhasePrompt stemsPhase(PartConfig cfg, int part, String transcript, String custom) {
        QuestionRange range = QuestionRange.of(Skill.LISTENING, part);
        String system = fragments.ieltsConstraints() + "\n" + fragments.wordLimit();
        String user = String.join("\n\n",
                "Write the question STEMS and the `section_layout` for IELTS Listening Part " + part + ".",
                "Number questions " + range.first() + " to " + range.last() + " (exactly 10). DO NOT include answers in this phase.",
                "Allowed interaction types: FILL_IN_BLANK, MULTIPLE_CHOICE, MULTIPLE_CHOICE_MULTIPLE_ANSWERS, MATCHING.",
                "section_layout.blocks group questions; allowed block types: NOTE_COMPLETION, INSTRUCTIONS_ONLY, MATCHING_FEATURES, PLAN_MAP_DIAGRAM_LABELING. Each block lists its question_numbers.",
                typeInstructions.instructionsFor(cfg == null ? null : cfg.safeQuestionTypes()),
                "Transcript (context only):\n\"\"\"\n" + transcript + "\n\"\"\"",
                fragments.customInstructions(custom),
                "Return JSON: { \"questions\": [ ...no correct_answer... ], \"section_layout\": { \"blocks\": [...] } }.");
        return new PhasePrompt(system, user, "listening_stems", schemas.listeningStemsSchema());
    }

    public PhasePrompt answersPhase(int part, String transcript, String stemsJson, String language, String custom) {
        QuestionRange range = QuestionRange.of(Skill.LISTENING, part);
        String system = fragments.ieltsConstraints() + "\n" + fragments.explanationLanguage(language);
        String user = String.join("\n\n",
                "Provide the ANSWERS for IELTS Listening Part " + part + " questions " + range.first() + "-" + range.last() + ".",
                "Return exactly one answer entry per question number, each with explanation + transcript evidence.",
                "Transcript:\n\"\"\"\n" + transcript + "\n\"\"\"",
                "Question stems:\n" + stemsJson,
                fragments.customInstructions(custom),
                "Return JSON: { \"answers\": [ { \"question_number\": n, \"correct_answer\": [...], \"explanation\": {...} } ] }.");
        return new PhasePrompt(system, user, "listening_answers", schemas.listeningAnswersSchema());
    }

    // ---- helpers ----

    private boolean isStrict(PartConfig cfg) {
        return cfg != null && FactsMode.parse(cfg.factsMode()) == FactsMode.STRICT && cfg.safeFacts().size() >= 3;
    }

    private String safeTopic(PartConfig cfg) {
        return (cfg == null || cfg.topic() == null || cfg.topic().isBlank()) ? "an everyday situation" : cfg.topic().trim();
    }

    private String safeDifficulty(PartConfig cfg) {
        return (cfg == null || cfg.difficulty() == null || cfg.difficulty().isBlank()) ? "INTERMEDIATE" : cfg.difficulty().trim();
    }
}
