export const QUESTION_TYPE_CONFIG = {
    FILL_IN_BLANK: {
        category: 'text',
        label: 'Fill in Blank',
        contentFields: ['sectionTitle', 'text'],
        answerType: 'text',
        placeholder: '____',
    },
    SUMMARY_COMPLETION: {
        category: 'text',
        label: 'Summary Completion',
        contentFields: ['text'],
        answerType: 'text',
    },
    NOTE_COMPLETION: {
        category: 'text',
        label: 'Note Completion',
        contentFields: ['sectionTitle', 'text'],
        answerType: 'text',
    },
    TABLE_COMPLETION: {
        category: 'text',
        label: 'Table Completion',
        contentFields: ['text'],
        answerType: 'text',
    },
    FLOW_CHART_COMPLETION: {
        category: 'text',
        label: 'Flow Chart Completion',
        contentFields: ['text'],
        answerType: 'text',
    },
    DIAGRAM_LABEL_COMPLETION: {
        category: 'text',
        label: 'Diagram Label',
        contentFields: ['text'],
        answerType: 'text',
    },
    SENTENCE_COMPLETION: {
        category: 'text',
        label: 'Sentence Completion',
        contentFields: ['text'],
        answerType: 'text',
    },
    SHORT_ANSWER: {
        category: 'text',
        label: 'Short Answer',
        contentFields: ['text'],
        answerType: 'text',
    },
    TRUE_FALSE_NOT_GIVEN: {
        category: 'boolean',
        label: 'True/False/Not Given',
        contentFields: ['text'],
        answerType: 'tfng',
        options: ['TRUE', 'FALSE', 'NOT GIVEN'],
    },
    YES_NO_NOT_GIVEN: {
        category: 'boolean',
        label: 'Yes/No/Not Given',
        contentFields: ['text'],
        answerType: 'ynng',
        options: ['YES', 'NO', 'NOT GIVEN'],
    },
    MULTIPLE_CHOICE: {
        category: 'choice',
        label: 'Multiple Choice (Single)',
        contentFields: ['text', 'options'],
        answerType: 'single-select',
    },
    MULTIPLE_CHOICE_MULTIPLE_ANSWERS: {
        category: 'choice',
        label: 'Multiple Choice (Multiple)',
        contentFields: ['text', 'options'],
        answerType: 'multi-select',
    },
    MATCHING: {
        category: 'matching',
        label: 'Matching',
        contentFields: ['text'],
        answerType: 'letter',
    },
    MATCHING_INFORMATION: {
        category: 'matching',
        label: 'Matching Information',
        contentFields: ['text'],
        answerType: 'letter',
    },
    MATCHING_HEADINGS: {
        category: 'matching',
        label: 'Matching Headings',
        contentFields: ['text'],
        answerType: 'letter',
    },
    MATCHING_FEATURES: {
        category: 'matching',
        label: 'Matching Features',
        contentFields: ['text'],
        answerType: 'letter',
    },
    MATCHING_SENTENCE_ENDINGS: {
        category: 'matching',
        label: 'Matching Sentence Endings',
        contentFields: ['text'],
        answerType: 'letter',
    },
    SUMMARY_COMPLETION_OPTIONS: {
        category: 'matching',
        label: 'Summary with Options',
        contentFields: ['text'],
        answerType: 'letter',
    },
    LIST_SELECTION: {
        category: 'matching',
        label: 'List Selection',
        contentFields: ['text'],
        answerType: 'letter',
    },
};

export const TYPES_ALLOWING_EMPTY_CONTENT = [
    'MATCHING_INFORMATION',
    'MATCHING_HEADINGS',
    'MATCHING_FEATURES',
    'MATCHING_SENTENCE_ENDINGS',
    'MAP_DIAGRAM_LABELLING',
    'TABLE_COMPLETION',
    'FLOW_CHART_COMPLETION',
    'FORM_COMPLETION',
    'NOTE_COMPLETION',
];

export const getQuestionTypeOptions = (questionTypes = []) => {
    if (questionTypes.length > 0) return questionTypes;

    return Object.entries(QUESTION_TYPE_CONFIG).map(([value, config]) => ({
        value,
        label: config.label,
    }));
};