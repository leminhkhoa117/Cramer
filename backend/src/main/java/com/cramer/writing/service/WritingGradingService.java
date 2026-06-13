package com.cramer.writing.service;

import com.cramer.platform.common.json.Json;
import com.cramer.platform.integration.llm.DeepSeekClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.OptionalDouble;

/**
 * Grades a single essay (SPEC-13 §4). Applies local shortcuts (empty → 0, &lt;20 words → 1)
 * without calling the API, otherwise calls DeepSeek for a JSON grade and <strong>recomputes the
 * overall band from the criterion scores</strong> (never trusting the model's own overall).
 */
@Service
public class WritingGradingService {

    private static final double TEMPERATURE = 0.4;
    private static final int MAX_TOKENS = 8192;

    private final DeepSeekClient deepSeek;
    private final WritingBandCalculator calculator;

    public WritingGradingService(DeepSeekClient deepSeek, WritingBandCalculator calculator) {
        this.deepSeek = deepSeek;
        this.calculator = calculator;
    }

    /** Whether grading can run at all (server key configured) — SPEC-13 §4. */
    public boolean isAvailable() {
        return deepSeek.isConfigured();
    }

    public GradingOutcome grade(int taskNumber, String essay, String promptText, String imageDescription, String model) {
        OptionalDouble shortcut = calculator.localShortcutBand(essay);
        if (shortcut.isPresent()) {
            return shortcutOutcome(shortcut.getAsDouble());
        }
        JsonNode result = deepSeek.chatJson(model,
                WritingPromptBuilder.system(),
                WritingPromptBuilder.user(taskNumber, essay, promptText, imageDescription),
                TEMPERATURE, MAX_TOKENS);
        return parse(result);
    }

    /** Parse a model JSON grade into a stored outcome with a recomputed overall band. */
    GradingOutcome parse(JsonNode result) {
        JsonNode bandScores = result.path("band_scores");
        double overall = calculator.overallFromBandScores(bandScores);

        ObjectNode feedback = Json.mapper().createObjectNode();
        copyIfPresent(result, feedback, "sentence_corrections");
        copyIfPresent(result, feedback, "paragraph_rewrites");
        copyIfPresent(result, feedback, "vocabulary_highlights");
        copyIfPresent(result, feedback, "error_analysis");
        copyIfPresent(result, feedback, "sample_essay_band_plus_one");
        copyIfPresent(result, feedback, "sample_essay_band_9");
        copyIfPresent(result, feedback, "feedback_summary");
        copyIfPresent(result, feedback, "word_analysis");
        copyIfPresent(result, feedback, "criteria_comments");

        return new GradingOutcome(BigDecimal.valueOf(overall),
                bandScores.isMissingNode() ? null : bandScores, feedback);
    }

    private GradingOutcome shortcutOutcome(double band) {
        ObjectNode bands = Json.mapper().createObjectNode();
        bands.put("taskResponse", band);
        bands.put("coherenceCohesion", band);
        bands.put("lexicalResource", band);
        bands.put("grammaticalRange", band);
        ObjectNode feedback = Json.mapper().createObjectNode();
        feedback.put("feedback_summary", band == 0.0
                ? "No essay was submitted."
                : "The response is far too short to be assessed against the band descriptors.");
        return new GradingOutcome(BigDecimal.valueOf(band), bands, feedback);
    }

    private static void copyIfPresent(JsonNode src, ObjectNode dst, String field) {
        JsonNode v = src.get(field);
        if (v != null && !v.isNull()) {
            dst.set(field, v);
        }
    }
}
