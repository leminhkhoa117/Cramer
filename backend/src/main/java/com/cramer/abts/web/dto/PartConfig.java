package com.cramer.abts.web.dto;

/**
 * Per-part / per-task generation config (SPEC-21 §1). For Reading/Listening {@code partNumber}
 * is the IELTS part; for Writing it is the task number (1 or 2) and {@code taskType} names the
 * task variant.
 *
 * @param topic              part topic (required for Reading/Listening readiness)
 * @param factsMode          {@code AUTO} | {@code STRICT}
 * @param facts              supplied facts (STRICT needs ≥3)
 * @param questionTypes      requested question types (Reading/Listening need ≥2)
 * @param questionTypeCounts optional per-type target counts
 * @param totalQuestions     optional total question target
 * @param passageLength      reading passage length hint (e.g. {@code MEDIUM})
 * @param difficulty         difficulty hint
 * @param taskType           writing task type ({@code ACADEMIC_TASK_1}|{@code GENERAL_TASK_1}|{@code TASK_2})
 */
public record PartConfig(
        String topic,
        String factsMode,
        java.util.List<String> facts,
        java.util.List<String> questionTypes,
        java.util.Map<String, Integer> questionTypeCounts,
        Integer totalQuestions,
        String passageLength,
        String difficulty,
        String taskType) {

    public java.util.List<String> safeFacts() {
        return facts == null ? java.util.List.of() : facts;
    }

    public java.util.List<String> safeQuestionTypes() {
        return questionTypes == null ? java.util.List.of() : questionTypes;
    }

    public java.util.Map<String, Integer> safeTypeCounts() {
        return questionTypeCounts == null ? java.util.Map.of() : questionTypeCounts;
    }
}
