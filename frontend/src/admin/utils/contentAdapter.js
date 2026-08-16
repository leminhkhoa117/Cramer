/**
 * ABTS Content Adapter - Transforms AI-generated content to database format.
 * 
 * This utility ensures consistent data mapping between ABTS output and 
 * the Test Editor's expected input format.
 * 
 * @since 2025-12-21 - ABTS Integration Fix
 */

/**
 * Expected ABTS Generation Result structure:
 * {
 *   status: 'SUCCESS' | 'PARTIAL_SUCCESS' | 'ERROR',
 *   content: {
 *     section: {
 *       passageText: string,
 *       wordCount: number,
 *       wordCountValid: boolean,
 *       partNumber: number
 *     },
 *     questions: [
 *       {
 *         questionNumber: number,
 *         questionType: string,
 *         questionContent: object | string,
 *         correctAnswer: string | array,
 *         explanation: string,
 *         wordLimit?: number,
 *         imageUrl?: string
 *       }
 *     ]
 *   },
 *   metadata: { ... }
 * }
 */

/**
 * Transform ABTS generation result to database-ready format.
 * 
 * @param {Object} generationResult - The raw result from ABTS generation
 * @param {string} skill - Target skill (reading, listening, writing, speaking)
 * @returns {Object|null} Transformed content ready for database, or null if invalid
 */
export function transformForDatabase(generationResult, skill = 'reading') {
    console.log('[ContentAdapter] Transforming generation result:', generationResult);

    if (!generationResult) {
        console.error('[ContentAdapter] No generation result provided');
        return null;
    }

    // Handle different result structures
    const content = extractContent(generationResult);

    if (!content) {
        console.error('[ContentAdapter] Could not extract content from result');
        return null;
    }

    // Transform section data
    const section = transformSection(content, skill);

    // Transform questions
    const questions = transformQuestions(content);

    const transformed = {
        section,
        questions,
        metadata: generationResult.metadata || {}
    };

    console.log('[ContentAdapter] Transformation complete:', {
        sectionHasPassage: !!section.passageText,
        questionCount: questions.length,
        skill
    });

    return transformed;
}

/**
 * Extract content from various possible result structures.
 */
function extractContent(result) {
    // Direct content property
    if (result.content) {
        return result.content;
    }

    // Result might be the content itself
    if (result.section || result.questions || result.passage) {
        return result;
    }

    // Nested data property
    if (result.data) {
        return result.data;
    }

    // Try to find content in generationResult
    if (result.generationResult?.content) {
        return result.generationResult.content;
    }

    return null;
}

/**
 * Transform section data to database format.
 * Accepts both the new backend snake_case shape
 * ({ section: { passage_text }, transcript, task_prompt }) and the legacy
 * camelCase shape ({ section: { passageText } }).
 */
function transformSection(content, skill) {
    const section = content.section || {};

    // Get passage text from various possible locations
    let passageText = section.passage_text
        || section.taskText
        || section.passageText
        || content.transcript
        || content.task_prompt
        || content.passageText
        || content.passage?.text
        || content.passage
        || '';

    // Ensure it's a string
    if (typeof passageText === 'object') {
        passageText = passageText.text || passageText.content || JSON.stringify(passageText);
    }

    return {
        passageText: passageText,
        audioUrl: section.audioUrl || content.audioUrl || null,
        sectionLayout: section.section_layout || section.sectionLayout || content.section_layout || content.sectionLayout || null,
        audioPlaceholder: section.audioPlaceholder || content.audioPlaceholder || content.audio_placeholder || null,
        imageDescription: section.imageDescription || content.imageDescription || content.image_description || null,
        displayContentUrl: section.displayContentUrl || content.displayContentUrl || null,
        partNumber: section.partNumber || section.part || content.partNumber || 1,
        wordCount: section.wordCount || content.wordCount || countWords(passageText),
        skill: skill
    };
}

/**
 * Transform questions array to database format.
 */
function transformQuestions(content) {
    // Try various locations for questions
    let rawQuestions = content.questions
        || content.questionGroups?.flatMap(g => g.questions)
        || [];

    // Handle question groups format
    if (content.questionGroups && Array.isArray(content.questionGroups)) {
        rawQuestions = content.questionGroups.flatMap(group => {
            const groupQuestions = group.questions || [];
            // Attach group instructions to questions if needed
            return groupQuestions.map(q => ({
                ...q,
                groupInstructions: group.instructions || group.instruction
            }));
        });
    }

    return rawQuestions.map((q, index) => transformQuestion(q, index));
}

/**
 * Transform a single question to database format.
 * Accepts both backend snake_case (question_number, question_type,
 * question_content, correct_answer) and legacy camelCase fields.
 */
function transformQuestion(question, index) {
    // Normalize question type
    const questionType = normalizeQuestionType(
        question.question_type || question.questionType || question.type || 'FILL_IN_BLANK'
    );

    // Get question content
    let questionContent = question.question_content || question.questionContent || question.content || {};

    // If questionContent is a string, try to parse it or wrap it
    if (typeof questionContent === 'string') {
        try {
            questionContent = JSON.parse(questionContent);
        } catch {
            questionContent = { text: questionContent };
        }
    }

    // Get correct answer in consistent format
    let correctAnswer = question.correct_answer || question.correctAnswer || question.answer || '';

    // Ensure it's a string for storage
    if (typeof correctAnswer === 'object') {
        correctAnswer = JSON.stringify(correctAnswer);
    } else {
        correctAnswer = JSON.stringify(correctAnswer);
    }

    const wordLimit = question.word_limit || question.wordLimit || null;
    const imageUrl = question.image_url || question.imageUrl || null;

    return {
        questionNumber: question.question_number || question.questionNumber || index + 1,
        questionType: questionType,
        questionContent: JSON.stringify(questionContent),
        correctAnswer: correctAnswer,
        explanation: question.explanation || null,
        wordLimit: wordLimit,
        imageUrl: imageUrl
    };
}

/**
 * Normalize question type to expected format.
 */
function normalizeQuestionType(type) {
    if (!type) return 'FILL_IN_BLANK';

    // Already in correct format
    if (type === type.toUpperCase() && (type.includes('_') || type === 'MATCHING')) {
        return type;
    }

    // Convert common variations
    const typeMap = {
        'true_false_not_given': 'TRUE_FALSE_NOT_GIVEN',
        'tfng': 'TRUE_FALSE_NOT_GIVEN',
        'yes_no_not_given': 'YES_NO_NOT_GIVEN',
        'ynng': 'YES_NO_NOT_GIVEN',
        'multiple_choice': 'MULTIPLE_CHOICE',
        'mcq': 'MULTIPLE_CHOICE',
        'matching': 'MATCHING',
        'matching_headings': 'MATCHING_HEADINGS',
        'matching_information': 'MATCHING_INFORMATION',
        'matching_features': 'MATCHING_FEATURES',
        'fill_in_blank': 'FILL_IN_BLANK',
        'fill_in_the_blank': 'FILL_IN_BLANK',
        'note_completion': 'FILL_IN_BLANK',
        'sentence_completion': 'SENTENCE_COMPLETION',
        'summary_completion': 'SUMMARY_COMPLETION',
        'short_answer': 'SHORT_ANSWER',
        'table_completion': 'TABLE_COMPLETION',
        'diagram_completion': 'DIAGRAM_LABEL_COMPLETION',
        'diagram_label_completion': 'DIAGRAM_LABEL_COMPLETION',
        'flow_chart_completion': 'FLOW_CHART_COMPLETION',
    };

    const normalized = type.toLowerCase().replace(/-/g, '_');
    return typeMap[normalized] || type.toUpperCase().replace(/-/g, '_');
}

/**
 * Count words in a text.
 */
function countWords(text) {
    if (!text || typeof text !== 'string') return 0;
    return text.trim().split(/\s+/).filter(word => word.length > 0).length;
}

/**
 * Validate transformed content before saving.
 * 
 * @param {Object} transformed - Transformed content from transformForDatabase
 * @returns {Object} Validation result { valid: boolean, errors: string[], warnings: string[] }
 */
export function validateForSave(transformed) {
    const errors = [];
    const warnings = [];

    if (!transformed) {
        errors.push('No content to validate');
        return { valid: false, errors, warnings };
    }

    const { section, questions } = transformed;

    // Section validation
    if (!section) {
        errors.push('Missing section data');
    } else {
        if (!section.passageText && section.skill === 'reading') {
            warnings.push('Reading section has no passage text');
        }

        if (section.skill === 'reading' && section.wordCount < 600) {
            warnings.push(`Passage word count (${section.wordCount}) is below recommended 600+ words`);
        }
    }

    // Questions validation
    if (!questions || questions.length === 0) {
        warnings.push('No questions to save');
    } else {
        questions.forEach((q, i) => {
            if (!q.questionType) {
                errors.push(`Question ${i + 1}: Missing question type`);
            }
            if (!q.correctAnswer || q.correctAnswer === 'null' || q.correctAnswer === '""') {
                warnings.push(`Question ${i + 1}: Missing correct answer`);
            }
        });
    }

    return {
        valid: errors.length === 0,
        errors,
        warnings
    };
}

/**
 * Create a save preview summary.
 * 
 * @param {Object} transformed - Transformed content
 * @returns {Object} Preview summary for UI display
 */
export function createSavePreview(transformed) {
    if (!transformed) {
        return {
            hasSection: false,
            hasPassage: false,
            passageWordCount: 0,
            questionCount: 0,
            questionTypes: [],
            questionsWithAnswers: 0
        };
    }

    const { section, questions } = transformed;

    // Count questions with valid answers
    const questionsWithAnswers = questions.filter(q =>
        q.correctAnswer &&
        q.correctAnswer !== 'null' &&
        q.correctAnswer !== '""' &&
        q.correctAnswer !== '[]'
    ).length;

    // Get unique question types
    const questionTypes = [...new Set(questions.map(q => q.questionType))];

    return {
        hasSection: !!section,
        hasPassage: !!(section?.passageText),
        passageWordCount: section?.wordCount || 0,
        questionCount: questions.length,
        questionTypes,
        questionsWithAnswers
    };
}

export default {
    transformForDatabase,
    validateForSave,
    createSavePreview
};
