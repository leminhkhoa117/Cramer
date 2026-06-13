package com.cramer.platform.common.ielts;

/**
 * IELTS question interaction types — shared-kernel vocabulary (SPEC-18 §7) used by catalog,
 * assessment scoring, and ABTS (SPEC-20 §4.2, SPEC-22 §5). Stored as strings in
 * {@code questions.question_type} (verified to match these names exactly in live data).
 */
public enum QuestionType {

    // --- Reading + shared completion/choice/matching ---
    FILL_IN_BLANK,
    SUMMARY_COMPLETION,
    SUMMARY_COMPLETION_OPTIONS,
    TRUE_FALSE_NOT_GIVEN,
    YES_NO_NOT_GIVEN,
    MATCHING_INFORMATION,
    MATCHING_HEADINGS,
    MATCHING_FEATURES,
    MATCHING_SENTENCE_ENDINGS,
    MULTIPLE_CHOICE,
    MULTIPLE_CHOICE_MULTIPLE_ANSWERS,
    TABLE_COMPLETION,
    FLOW_CHART_COMPLETION,
    DIAGRAM_LABEL_COMPLETION,

    // --- Listening interaction type ---
    MATCHING,

    // --- Speaking authored parts ---
    PART_1,
    PART_2,
    PART_3;

    /**
     * Whether a question of this type has a set of correct answers that must be matched as a
     * set (order-independent). Drives the scoring fix in SPEC-12 §4.
     */
    public boolean multiSelect() {
        return this == MULTIPLE_CHOICE_MULTIPLE_ANSWERS;
    }

    /** Parse tolerantly (case-insensitive); unknown values raise IllegalArgumentException. */
    public static QuestionType from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("questionType is required");
        }
        return QuestionType.valueOf(raw.trim().toUpperCase());
    }
}
