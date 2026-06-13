package com.cramer.writing.service;

import com.cramer.platform.common.ielts.BandScale;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.OptionalDouble;

/**
 * Pure IELTS Writing band math (SPEC-13 §4.2/§5). Stateless and side-effect-free.
 *
 * <ul>
 *   <li><b>Per-task overall</b> = average of the four criterion band scores, rounded to 0.5.
 *       The model's own {@code overall_band} is <b>not</b> trusted (SPEC-13 §4.2 fix).</li>
 *   <li><b>Weighted overall</b> across tasks = Task1·⅓ + Task2·⅔, rounded to 0.5; a single task
 *       contributes its own band (SPEC-13 §5).</li>
 *   <li><b>Local shortcuts</b>: empty essay → 0.0; fewer than 20 words → 1.0 (SPEC-13 §4.1).</li>
 * </ul>
 */
@Component
public class WritingBandCalculator {

    private static final int SHORTCUT_WORD_THRESHOLD = 20;

    /** Average of the numeric criterion values in {@code band_scores}, rounded to 0.5 (0 if none).
     * Any {@code overall*} key is ignored so a model-supplied overall is never folded in. */
    public double overallFromBandScores(JsonNode bandScores) {
        if (bandScores == null || !bandScores.isObject() || bandScores.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        int n = 0;
        var fields = bandScores.properties().iterator();
        while (fields.hasNext()) {
            var entry = fields.next();
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase();
            JsonNode v = entry.getValue();
            if (v.isNumber() && !key.contains("overall")) {
                sum += v.asDouble();
                n++;
            }
        }
        return n == 0 ? 0.0 : BandScale.roundToHalf(sum / n);
    }

    /** Weighted overall across Task 1 (⅓) and Task 2 (⅔), rounded to 0.5 (SPEC-13 §5). */
    public Double weightedOverall(Double task1Band, Double task2Band) {
        if (task1Band != null && task2Band != null) {
            return BandScale.roundToHalf(task1Band / 3.0 + task2Band * 2.0 / 3.0);
        }
        if (task1Band != null) {
            return BandScale.roundToHalf(task1Band);
        }
        if (task2Band != null) {
            return BandScale.roundToHalf(task2Band);
        }
        return null;
    }

    /** A forced band for trivial essays, or empty when the essay should be graded by the model. */
    public OptionalDouble localShortcutBand(String essay) {
        if (essay == null || essay.isBlank()) {
            return OptionalDouble.of(0.0);
        }
        if (countWords(essay) < SHORTCUT_WORD_THRESHOLD) {
            return OptionalDouble.of(1.0);
        }
        return OptionalDouble.empty();
    }

    /** Whitespace-delimited word count (SPEC-13 §3 draft recompute). */
    public int countWords(String essay) {
        if (essay == null || essay.isBlank()) {
            return 0;
        }
        return essay.trim().split("\\s+").length;
    }
}
