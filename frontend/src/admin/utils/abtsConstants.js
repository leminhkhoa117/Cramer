/**
 * ABTS shared constants (moved from services/abtsApi.js during the
 * lib/api migration so no caller depends on the legacy client).
 */

/** Skill types for generation. */
export const SKILL_TYPES = {
    READING: 'READING',
    LISTENING: 'LISTENING',
    WRITING: 'WRITING',
    SPEAKING: 'SPEAKING'
};

/** Generation scopes. */
export const GENERATION_SCOPES = {
    FULL_SKILL: 'FULL_SKILL',
    SINGLE_PART: 'SINGLE_PART',
    MULTI_PART: 'MULTI_PART',
    QUESTION_GROUP: 'QUESTION_GROUP'
};

/** Difficulty levels mapped to IELTS bands. */
export const DIFFICULTY_LEVELS = {
    BEGINNER: { value: 'BEGINNER', label: 'Beginner', bandRange: '4.0-5.0' },
    LOWER_INTERMEDIATE: { value: 'LOWER_INTERMEDIATE', label: 'Lower-Intermediate', bandRange: '5.0-6.0' },
    INTERMEDIATE: { value: 'INTERMEDIATE', label: 'Intermediate', bandRange: '6.0-7.0' },
    UPPER_INTERMEDIATE: { value: 'UPPER_INTERMEDIATE', label: 'Upper-Intermediate', bandRange: '7.0-8.0' },
    ADVANCED: { value: 'ADVANCED', label: 'Advanced/IELTS-like', bandRange: '8.0-9.0' }
};

/** Explanation languages. */
export const EXPLANATION_LANGUAGES = {
    VI: { value: 'VI', label: 'Tiếng Việt' },
    EN: { value: 'EN', label: 'English' }
};

/** Test types. */
export const TEST_TYPES = {
    ACADEMIC: { value: 'ACADEMIC', label: 'Academic' },
    GENERAL_TRAINING: { value: 'GENERAL_TRAINING', label: 'General Training' }
};

/** Generation status values. */
export const GENERATION_STATUS = {
    SUCCESS: 'SUCCESS',
    PARTIAL_SUCCESS: 'PARTIAL_SUCCESS',
    FAILED: 'FAILED'
};

export default {
    SKILL_TYPES,
    GENERATION_SCOPES,
    DIFFICULTY_LEVELS,
    EXPLANATION_LANGUAGES,
    TEST_TYPES,
    GENERATION_STATUS
};
