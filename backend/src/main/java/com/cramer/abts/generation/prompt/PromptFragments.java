package com.cramer.abts.generation.prompt;

import org.springframework.stereotype.Component;

/**
 * Reusable prompt fragments shared by all skill builders (SPEC-22 §1, §2). Keeping these in one
 * place means cross-cutting rules (English-only, explanation objects, word limits, facts modes)
 * are authored once and never drift between skills.
 */
@Component
public class PromptFragments {

    /** Global IELTS authoring constraints prepended to every generation prompt. */
    public String ieltsConstraints() {
        return """
                You are an expert IELTS content author. Follow these non-negotiable rules:
                - All passages, transcripts, questions and options MUST be in English.
                - Produce authentic, exam-realistic content at the requested difficulty.
                - Return ONLY JSON conforming to the provided schema. No prose outside JSON.
                - Every question's `correct_answer` MUST be an array of acceptable answer strings
                  (use a single-element array for single-answer questions).
                - Every `explanation` MUST be an object: { "text": "...", "evidence": "..." }.
                  Put a direct quote/locator from the passage or transcript in `evidence`.
                """;
    }

    /** Explanation-language directive (SPEC-22 §6: passages English; explanations follow language). */
    public String explanationLanguage(String language) {
        String lang = (language == null || language.isBlank()) ? "en" : language.trim();
        if (lang.equalsIgnoreCase("vi")) {
            return "Write each explanation's `text` in Vietnamese; keep all quoted `evidence` in English.";
        }
        return "Write each explanation's `text` in English.";
    }

    /** Word-limit fragment for completion-type questions. */
    public String wordLimit() {
        return """
                For completion questions (FILL_IN_BLANK, *_COMPLETION), set `word_limit` to the
                applicable constraint (e.g. "ONE WORD", "NO MORE THAN TWO WORDS",
                "ONE WORD AND/OR A NUMBER"); leave it as an empty string for non-completion types.
                """;
    }

    /** Facts/strict-mode directive (SPEC-22 §3). */
    public String factsMode(boolean strict, java.util.List<String> facts) {
        if (strict && facts != null && !facts.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("STRICT MODE: build ALL content strictly around the following facts; ")
              .append("do not invent contradicting data. Facts:\n");
            for (String f : facts) {
                sb.append("- ").append(f).append('\n');
            }
            return sb.toString();
        }
        return "AUTO MODE: research and use plausible, accurate academic details for the topic.";
    }

    /** Custom author instructions (optional). */
    public String customInstructions(String custom) {
        return (custom == null || custom.isBlank())
                ? ""
                : "Additional author instructions (obey when not conflicting with IELTS rules):\n" + custom.trim();
    }
}
