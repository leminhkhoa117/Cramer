package com.cramer.abts.web.dto;

import java.util.List;
import java.util.Map;

/**
 * Generation request body (SPEC-21 §1, SPEC-25 §1). The skill comes from the path. For Writing,
 * {@code partsToGenerate} holds task numbers (1, 2). Multi-part requests carry one
 * {@link PartConfig} per part in {@code parts} (keyed by part number as a string).
 *
 * @param partsToGenerate    parts/tasks to generate (e.g. {@code [1,3]})
 * @param parts              per-part config keyed by part number (string key for JSON)
 * @param model              model + reasoning config
 * @param explanationLanguage explanation language (e.g. {@code vi}/{@code en})
 * @param customInstructions extra author instructions
 * @param existingPassageText existing passage/transcript for {@code /generate/questions}
 */
public record GenerationRequest(
        List<Integer> partsToGenerate,
        Map<String, PartConfig> parts,
        ModelConfig model,
        String explanationLanguage,
        String customInstructions,
        String existingPassageText) {

    public List<Integer> safeParts() {
        return partsToGenerate == null ? List.of() : partsToGenerate;
    }

    public PartConfig partConfig(int part) {
        if (parts == null) {
            return null;
        }
        return parts.get(String.valueOf(part));
    }

    public ModelConfig safeModel() {
        return model == null
                ? new ModelConfig(null, null, null, null, null, null, null)
                : model;
    }

    public String resolvedLanguage() {
        return (explanationLanguage == null || explanationLanguage.isBlank()) ? "en" : explanationLanguage.trim();
    }
}
