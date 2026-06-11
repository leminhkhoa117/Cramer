package com.cramer.abts.generation.prompt;

import com.cramer.abts.domain.FactsMode;
import com.cramer.abts.domain.QuestionRange;
import com.cramer.abts.web.dto.PartConfig;
import com.cramer.platform.common.ielts.Skill;
import org.springframework.stereotype.Component;

/**
 * Reading prompt builder (SPEC-22 §2): passage phase then questions phase, each constrained by
 * the part's canonical question range (SPEC-20 §4.1). Composes shared fragments + the
 * single-source {@link QuestionTypeInstructionBuilder} (no inline duplication).
 */
@Component
public class ReadingPromptBuilder {

    private final PromptFragments fragments;
    private final QuestionTypeInstructionBuilder typeInstructions;
    private final PromptSchemaBuilder schemas;

    public ReadingPromptBuilder(PromptFragments fragments, QuestionTypeInstructionBuilder typeInstructions,
                                PromptSchemaBuilder schemas) {
        this.fragments = fragments;
        this.typeInstructions = typeInstructions;
        this.schemas = schemas;
    }

    public PhasePrompt passagePhase(PartConfig cfg, int part, String language, String custom) {
        String system = fragments.ieltsConstraints();
        String user = String.join("\n\n",
                "Generate ONLY the reading passage for IELTS Reading Part " + part + ".",
                "Topic: " + safeTopic(cfg),
                "Difficulty: " + safeDifficulty(cfg),
                passageLength(cfg),
                fragments.factsMode(isStrict(cfg), cfg == null ? null : cfg.safeFacts()),
                fragments.customInstructions(custom),
                "Return JSON: { \"section\": { \"passage_text\": \"<the passage>\" } }.");
        return new PhasePrompt(system, user, "reading_passage", schemas.readingPassageSchema());
    }

    public PhasePrompt questionsPhase(PartConfig cfg, int part, String language, String custom, String passageText) {
        QuestionRange range = QuestionRange.of(Skill.READING, part);
        String system = fragments.ieltsConstraints() + "\n" + fragments.explanationLanguage(language)
                + "\n" + fragments.wordLimit();
        String user = String.join("\n\n",
                "Write the questions for IELTS Reading Part " + part + ".",
                "Number questions " + range.first() + " to " + range.last() + " ("
                        + targetCount(cfg, range.count()) + " questions). Use at least TWO distinct question types.",
                typeInstructions.instructionsFor(cfg == null ? null : cfg.safeQuestionTypes()),
                typeCounts(cfg),
                "Base every question and answer strictly on this passage:\n\"\"\"\n" + passageText + "\n\"\"\"",
                fragments.customInstructions(custom),
                "Return JSON: { \"questions\": [ ... ] } matching the schema.");
        return new PhasePrompt(system, user, "reading_questions", schemas.readingQuestionsSchema());
    }

    // ---- helpers ----

    private boolean isStrict(PartConfig cfg) {
        return cfg != null && FactsMode.parse(cfg.factsMode()) == FactsMode.STRICT && cfg.safeFacts().size() >= 3;
    }

    private String safeTopic(PartConfig cfg) {
        return (cfg == null || cfg.topic() == null || cfg.topic().isBlank()) ? "an academic subject" : cfg.topic().trim();
    }

    private String safeDifficulty(PartConfig cfg) {
        return (cfg == null || cfg.difficulty() == null || cfg.difficulty().isBlank()) ? "INTERMEDIATE" : cfg.difficulty().trim();
    }

    private String passageLength(PartConfig cfg) {
        String len = (cfg == null || cfg.passageLength() == null || cfg.passageLength().isBlank()) ? "MEDIUM" : cfg.passageLength().trim();
        return "Passage length: " + len + " (authentic IELTS reading length, ~700-900 words).";
    }

    private int targetCount(PartConfig cfg, int fallback) {
        return (cfg != null && cfg.totalQuestions() != null && cfg.totalQuestions() > 0) ? cfg.totalQuestions() : fallback;
    }

    private String typeCounts(PartConfig cfg) {
        if (cfg == null || cfg.safeTypeCounts().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Target counts per type:\n");
        cfg.safeTypeCounts().forEach((t, n) -> sb.append("- ").append(t).append(": ").append(n).append('\n'));
        return sb.toString();
    }
}
