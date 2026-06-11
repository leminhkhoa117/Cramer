package com.cramer.writing.service;

/**
 * Builds the DeepSeek grading prompt (SPEC-13 §4). Text-only: images are conveyed via the
 * {@code imageDescription}; the model is asked to return a strict JSON object with the
 * SPEC-13 §4.2 fields. Task 1 targets ≥150 words, Task 2 ≥250.
 */
public final class WritingPromptBuilder {

    private WritingPromptBuilder() {
    }

    public static String system() {
        return """
                You are a certified IELTS Writing examiner. Grade the candidate's essay strictly
                against the official band descriptors for the four criteria: Task Achievement/Response,
                Coherence and Cohesion, Lexical Resource, and Grammatical Range and Accuracy.
                Respond with a single JSON object only (no markdown), using these keys:
                band_scores (object with the four criteria as numbers in 0.5 steps),
                overall_band (number), sentence_corrections (array), paragraph_rewrites (array),
                vocabulary_highlights (array), error_analysis (array), sample_essay_band_plus_one (string),
                sample_essay_band_9 (string), feedback_summary (string), word_analysis (object),
                criteria_comments (object). Use only the provided text; you cannot see images.
                """;
    }

    public static String user(int taskNumber, String essay, String promptText, String imageDescription) {
        int minWords = taskNumber == 1 ? 150 : 250;
        StringBuilder sb = new StringBuilder();
        sb.append("IELTS Writing Task ").append(taskNumber)
                .append(" (target at least ").append(minWords).append(" words).\n\n");
        if (promptText != null && !promptText.isBlank()) {
            sb.append("Task prompt:\n").append(promptText.trim()).append("\n\n");
        }
        if (imageDescription != null && !imageDescription.isBlank()) {
            sb.append("Description of the task image (the candidate saw an image; you cannot):\n")
                    .append(imageDescription.trim()).append("\n\n");
        }
        sb.append("Candidate essay:\n").append(essay == null ? "" : essay.trim());
        return sb.toString();
    }
}
