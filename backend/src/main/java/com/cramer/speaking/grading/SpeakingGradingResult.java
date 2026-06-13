package com.cramer.speaking.grading;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Schema-driven Speaking grading result (SPEC-14 §6.1). <strong>Fix:</strong> ONE typed object
 * (not 24 fragmented DTOs). The overall + four criterion bands drive the persisted band columns;
 * the structured detail is kept as JSON for the review UI. Deserializes tolerantly (unknown
 * fields like {@code _worker} metadata are ignored by the shared mapper).
 *
 * @param schemaVersion     contract version
 * @param overallBand       overall IELTS band (0–9, half-steps)
 * @param fluencyBand       fluency & coherence band
 * @param lexicalBand       lexical resource band
 * @param grammarBand       grammatical range & accuracy band
 * @param pronunciationBand pronunciation band
 * @param gradingMode       {@code multimodal} | {@code text_only}
 * @param degradedReason    why text-only (null when multimodal)
 * @param criteria          per-criterion structured detail
 * @param perPartFeedback   per-part feedback array
 * @param perTurnFeedback   per-turn feedback array
 * @param improvementTips   improvement tips array
 */
public record SpeakingGradingResult(
        Integer schemaVersion,
        Double overallBand,
        Double fluencyBand,
        Double lexicalBand,
        Double grammarBand,
        Double pronunciationBand,
        String gradingMode,
        String degradedReason,
        JsonNode criteria,
        JsonNode perPartFeedback,
        JsonNode perTurnFeedback,
        JsonNode improvementTips) {

    /** All five bands present, within 0–9 and on a half-step. */
    public boolean bandsValid() {
        return valid(overallBand) && valid(fluencyBand) && valid(lexicalBand)
                && valid(grammarBand) && valid(pronunciationBand);
    }

    private static boolean valid(Double band) {
        if (band == null || band < 0.0 || band > 9.0) {
            return false;
        }
        double doubled = band * 2.0;
        return Math.abs(doubled - Math.rint(doubled)) < 1e-9;
    }
}
