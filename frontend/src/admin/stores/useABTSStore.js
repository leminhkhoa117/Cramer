/**
 * ABTS Store - Zustand store for AI-Based Test Generation System
 * 
 * Manages state for the generation wizard, API calls, and results.
 * Includes caching for templates and models.
 * 
 * @since 2025-12-20 - ABTS v2.0
 */
import { create } from 'zustand';
import {
    generateReading,
    generateListening,
    generateWriting,
    regenerateQuestions,
    generateReadingStream,
    generateListeningStream,
    generateWritingStream,
    getAvailableModels,
    getTemplateCategories,
    getTemplatesByCategory,
    getStatus,
    saveGeneratedTest,
    refineContentStream,
    SKILL_TYPES,
    GENERATION_SCOPES
} from '../services/abtsApi';

// ==================== IELTS QUESTION TYPE POOLS ====================
// Part-specific question types for semi-random selection (based on Cambridge IELTS)

export const READING_PART_TYPES = {
    1: ['FILL_IN_BLANK', 'SUMMARY_COMPLETION', 'TABLE_COMPLETION', 'TRUE_FALSE_NOT_GIVEN', 'DIAGRAM_LABEL_COMPLETION'],
    2: ['MATCHING_HEADINGS', 'MATCHING_INFORMATION', 'SUMMARY_COMPLETION', 'FILL_IN_BLANK', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS'],
    3: ['MULTIPLE_CHOICE', 'YES_NO_NOT_GIVEN', 'MATCHING_SENTENCE_ENDINGS', 'SUMMARY_COMPLETION_OPTIONS', 'MATCHING_FEATURES']
};

export const LISTENING_PART_TYPES = {
    1: ['FILL_IN_BLANK', 'MULTIPLE_CHOICE', 'MATCHING'],
    2: ['FILL_IN_BLANK', 'MATCHING', 'MULTIPLE_CHOICE'],
    3: ['MULTIPLE_CHOICE', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', 'MATCHING', 'FILL_IN_BLANK'],
    4: ['FILL_IN_BLANK', 'MULTIPLE_CHOICE', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS']
};

// Fixed question counts per part
export const QUESTION_COUNTS = {
    READING: { 1: 13, 2: 13, 3: 14 },
    LISTENING: { 1: 10, 2: 10, 3: 10, 4: 10 }
};

// Helper to extract issue category from warning message
function extractCategory(message) {
    if (!message) return 'UNKNOWN';
    const lower = message.toLowerCase();
    if (lower.includes('word count') || lower.includes('word limit')) return 'WORD_LIMIT';
    if (lower.includes('placeholder') || lower.includes('____')) return 'MISSING_PLACEHOLDER';
    if (lower.includes('options') || lower.includes('choices')) return 'INCONSISTENT_OPTIONS';
    if (lower.includes('answer') && lower.includes('passage')) return 'ANSWER_NOT_IN_PASSAGE';
    if (lower.includes('diagram') || lower.includes('label')) return 'DIAGRAM_NO_LABELS';
    if (lower.includes('format')) return 'INVALID_WORD_LIMIT_FORMAT';
    return 'GENERAL';
}

// Initial form state
const initialFormState = {
    skill: null,
    scope: 'MULTI_PART', // Always use multi-part mode (v7.0 - removed SINGLE_PART toggle)
    partNumber: 1,
    topic: '',
    hashtags: [],
    facts: [],
    difficulty: 'INTERMEDIATE',
    explanationLanguage: 'VI',
    testType: 'ACADEMIC',
    questionTypes: [],
    model: null,
    enableReasoning: true,
    reasoningEffort: 'high',
    temperature: 1.0, // AI creativity: 0.0 (deterministic) to 2.0 (creative)
    existingPassageText: null,
    questionsToRegenerate: null,
    generationMode: 'AUTO', // 'AUTO' | 'CUSTOM_FACTS'

    // Power-user settings (v5.0)
    questionTypeCounts: {}, // { 'TRUE_FALSE_NOT_GIVEN': 3, 'MULTIPLE_CHOICE': 2 }
    // partNumber is defined above (line 31)
    passageLength: 'MEDIUM', // 'SHORT' (800-900) | 'MEDIUM' (900-1000) | 'LONG' (1000-1200)
    customInstructions: '', // Custom prompt additions
    showJsonPreview: false, // Toggle JSON preview panel
    maxTokens: 64000, // Max output tokens (Gemini 3 Flash supports 65k)
    totalQuestions: 13, // Target total questions (10-20)

    // Multi-part generation (v6.0)
    selectedParts: [], // e.g., [1, 2] for Parts 1 and 2
    partConfigs: {}, // { 1: { topic: '', facts: [], questionTypes: [] }, 2: {...} }

    // Refinement settings (v8.0 - cost optimization)
    refinementModel: null, // Model for refinement (null = use default Gemini Flash)
    enableRefinementCaching: true, // Enable context caching for cost reduction
    enableRefinementReasoning: false, // Enable reasoning/thinking tokens for refinement (default OFF for speed)
};

const useABTSStore = create((set, get) => ({
    // ==================== WIZARD STATE ====================
    currentStep: 1,
    isWizardOpen: false,

    // ==================== FORM DATA ====================
    formData: { ...initialFormState },

    // ==================== GENERATION STATE ====================
    isGenerating: false,
    generationResult: null,
    generationError: null,
    generationProgress: 0,
    streamEvents: [],       // Array of streaming events for UI display
    abortStream: null,      // Function to abort streaming
    reasoning: '',          // Accumulated real-time reasoning tokens from AI_THINKING

    // ==================== CACHE DATA ====================
    models: [],
    templateCategories: [],
    templatesCache: {}, // { categoryId: templates[] }
    abtsStatus: null,

    // ==================== LOADING STATES ====================
    isLoadingModels: false,
    isLoadingTemplates: false,
    isLoadingStatus: false,

    // ==================== TIMESTAMPS ====================
    lastModelsFetch: null,
    lastStatusFetch: null,

    // ==================== SAVE STATE ====================
    isSaving: false,
    saveResult: null,
    saveError: null,
    selectedSetId: null,
    selectedSetCode: null,
    selectedTestId: null,

    // ==================== REFINEMENT STATE (AGENT 2) ====================
    selectedIssues: [],          // IDs of issues selected for refinement
    isRefining: false,           // Refinement in progress
    refinementResult: null,      // Result from Agent 2
    refinementStream: [],        // Streaming events during refinement
    abortRefinement: null,       // Function to abort refinement

    // ==================== AUDIO URLS (Listening) ====================
    audioUrls: {},               // { partNumber: url } - Audio URLs for Listening parts

    // ==================== WIZARD ACTIONS ====================

    /**
     * Open the generation wizard
     */
    openWizard: () => {
        set({
            isWizardOpen: true,
            currentStep: 1,
            formData: { ...initialFormState },
            generationResult: null,
            generationError: null
        });
    },

    /**
     * Close the generation wizard
     */
    closeWizard: () => {
        set({
            isWizardOpen: false,
            currentStep: 1,
            generationResult: null,
            generationError: null
        });
    },

    /**
     * Navigate to a specific step
     */
    goToStep: (step) => {
        const { currentStep, isStepValid } = get();
        if (step <= currentStep || isStepValid(step - 1)) {
            set({ currentStep: step });
        }
    },

    /**
     * Go to next step
     */
    nextStep: () => {
        const { currentStep, isStepValid } = get();
        if (currentStep < 5 && isStepValid(currentStep)) {
            set({ currentStep: currentStep + 1 });
        }
    },

    /**
     * Go to previous step
     */
    prevStep: () => {
        const { currentStep } = get();
        if (currentStep > 1) {
            set({ currentStep: currentStep - 1 });
        }
    },

    /**
     * Check if a step is valid
     */
    isStepValid: (step) => {
        const { formData, generationResult } = get();
        switch (step) {
            case 1:
                return formData.skill !== null;
            case 2:
                if (formData.existingPassageText) return true;
                if (formData.generationMode === 'AUTO') {
                    return formData.topic.trim().length >= 3;
                }
                return formData.topic.trim().length >= 3 && formData.facts.length >= 5; // Relaxed from 10
            case 3:
                return true; // Configuration always valid (has defaults)
            case 4:
                return generationResult !== null && generationResult.status !== 'FAILED';
            default:
                return true;
        }
    },

    // ==================== FORM ACTIONS ====================

    /**
     * Update form data
     * When skill changes, filter selectedParts to only include valid parts for the new skill
     */
    updateFormData: (updates) => {
        set(state => {
            const newFormData = { ...state.formData, ...updates };

            // If skill is being changed, filter selectedParts to valid parts for that skill
            if (updates.skill && updates.skill !== state.formData.skill) {
                const maxParts = updates.skill === 'READING' ? 3 :
                    updates.skill === 'LISTENING' ? 4 : 2;
                newFormData.selectedParts = (newFormData.selectedParts || [])
                    .filter(p => p >= 1 && p <= maxParts);
                // Also clear partConfigs for invalid parts
                if (newFormData.partConfigs) {
                    const validConfigs = {};
                    Object.keys(newFormData.partConfigs).forEach(key => {
                        const partNum = parseInt(key, 10);
                        if (partNum >= 1 && partNum <= maxParts) {
                            validConfigs[key] = newFormData.partConfigs[key];
                        }
                    });
                    newFormData.partConfigs = validConfigs;
                }
            }

            return { formData: newFormData };
        });
    },

    /**
     * Set a single form field
     */
    setFormField: (field, value) => {
        set(state => ({
            formData: { ...state.formData, [field]: value }
        }));
    },

    /**
     * Reset form to initial state
     */
    resetForm: () => {
        set({
            formData: { ...initialFormState },
            generationResult: null,
            generationError: null,
            currentStep: 1,
            audioUrls: {}
        });
    },

    /**
     * Set audio URL for a specific part (Listening only)
     */
    setAudioUrl: (partNumber, url) => {
        set(state => ({
            audioUrls: { ...state.audioUrls, [partNumber]: url }
        }));
    },

    // ==================== MULTI-PART ACTIONS ====================

    /**
     * Toggle part selection for multi-part generation
     */
    togglePartSelection: (partNumber) => {
        const { formData } = get();
        const currentParts = formData.selectedParts || [];
        const newParts = currentParts.includes(partNumber)
            ? currentParts.filter(p => p !== partNumber)
            : [...currentParts, partNumber].sort((a, b) => a - b);

        set({
            formData: {
                ...formData,
                selectedParts: newParts,
                // Always use MULTI_PART mode (v7.0 - removed SINGLE_PART toggle)
                scope: 'MULTI_PART',
                // Set partNumber for backward compatibility with save logic
                partNumber: newParts.length >= 1 ? newParts[0] : formData.partNumber
            }
        });
    },

    /**
     * Set configuration for a specific part
     */
    setPartConfig: (partNumber, config) => {
        const { formData } = get();
        const updatedConfigs = {
            ...formData.partConfigs,
            [partNumber]: {
                ...(formData.partConfigs[partNumber] || {}),
                ...config
            }
        };
        set({
            formData: { ...formData, partConfigs: updatedConfigs }
        });
    },

    /**
     * Apply global config to all selected parts
     */
    applyGlobalConfigToAllParts: () => {
        const { formData } = get();
        const { selectedParts, topic, facts, questionTypes } = formData;
        const partConfigs = {};

        selectedParts.forEach(part => {
            partConfigs[part] = { topic, facts: [...facts], questionTypes: [...questionTypes] };
        });

        set({
            formData: { ...formData, partConfigs }
        });
    },

    /**
     * Clear all part selections
     */
    clearPartSelections: () => {
        set(state => ({
            formData: {
                ...state.formData,
                selectedParts: [],
                partConfigs: {},
                scope: 'MULTI_PART' // Always stay in MULTI_PART mode (v7.0)
            }
        }));
    },

    /**
     * Semi-randomize question types for a specific part.
     * Uses IELTS-realistic type pools with balanced counts.
     */
    randomizePartConfig: (partNumber) => {
        const { formData } = get();
        const skill = formData.skill;

        if (!skill || skill === 'WRITING') return;

        const typePool = skill === 'READING'
            ? READING_PART_TYPES[partNumber]
            : LISTENING_PART_TYPES[partNumber];

        const totalQuestions = QUESTION_COUNTS[skill]?.[partNumber] || 10;

        // Pick 2-3 types randomly
        const numTypes = Math.random() < 0.5 ? 2 : 3;
        const shuffled = [...typePool].sort(() => 0.5 - Math.random());
        const selectedTypes = shuffled.slice(0, numTypes);

        // Calculate balanced counts
        const counts = {};
        const baseCount = Math.floor(totalQuestions / numTypes);
        let remainder = totalQuestions % numTypes;

        selectedTypes.forEach(type => {
            counts[type] = baseCount + (remainder > 0 ? 1 : 0);
            remainder--;
        });

        // Update partConfigs
        const updatedConfigs = {
            ...formData.partConfigs,
            [partNumber]: {
                ...(formData.partConfigs[partNumber] || {}),
                questionTypes: selectedTypes,
                questionTypeCounts: counts
            }
        };

        set({ formData: { ...formData, partConfigs: updatedConfigs } });
    },

    /**
     * Randomize all selected parts at once
     */
    randomizeAllParts: () => {
        const { formData } = get();
        const skill = formData.skill;

        if (!skill || skill === 'WRITING') return;

        const updatedConfigs = { ...formData.partConfigs };

        formData.selectedParts.forEach(partNumber => {
            const typePool = skill === 'READING'
                ? READING_PART_TYPES[partNumber]
                : LISTENING_PART_TYPES[partNumber];

            const totalQuestions = QUESTION_COUNTS[skill]?.[partNumber] || 10;
            const numTypes = Math.random() < 0.5 ? 2 : 3;
            const shuffled = [...typePool].sort(() => 0.5 - Math.random());
            const selectedTypes = shuffled.slice(0, numTypes);

            const counts = {};
            const baseCount = Math.floor(totalQuestions / numTypes);
            let remainder = totalQuestions % numTypes;
            selectedTypes.forEach(type => {
                counts[type] = baseCount + (remainder > 0 ? 1 : 0);
                remainder--;
            });

            updatedConfigs[partNumber] = {
                ...(formData.partConfigs[partNumber] || {}),
                questionTypes: selectedTypes,
                questionTypeCounts: counts
            };
        });

        set({ formData: { ...formData, partConfigs: updatedConfigs } });
    },

    /**
     * Toggle a question type for a specific part (manual selection)
     */
    togglePartQuestionType: (partNumber, typeId) => {
        const { formData } = get();
        const skill = formData.skill;
        const totalQuestions = QUESTION_COUNTS[skill]?.[partNumber] || 13;

        const partConfig = formData.partConfigs[partNumber] || { questionTypes: [], questionTypeCounts: {} };
        const currentTypes = partConfig.questionTypes || [];

        let newTypes, newCounts;

        if (currentTypes.includes(typeId)) {
            // Remove type
            newTypes = currentTypes.filter(t => t !== typeId);
            newCounts = { ...partConfig.questionTypeCounts };
            delete newCounts[typeId];

            // Recalculate counts for remaining types
            if (newTypes.length > 0) {
                const baseCount = Math.floor(totalQuestions / newTypes.length);
                let remainder = totalQuestions % newTypes.length;
                newTypes.forEach(type => {
                    newCounts[type] = baseCount + (remainder > 0 ? 1 : 0);
                    remainder--;
                });
            }
        } else {
            // Add type (max 3)
            if (currentTypes.length >= 3) return;
            newTypes = [...currentTypes, typeId];

            // Recalculate balanced counts
            newCounts = {};
            const baseCount = Math.floor(totalQuestions / newTypes.length);
            let remainder = totalQuestions % newTypes.length;
            newTypes.forEach(type => {
                newCounts[type] = baseCount + (remainder > 0 ? 1 : 0);
                remainder--;
            });
        }

        const updatedConfigs = {
            ...formData.partConfigs,
            [partNumber]: { ...partConfig, questionTypes: newTypes, questionTypeCounts: newCounts }
        };

        set({ formData: { ...formData, partConfigs: updatedConfigs } });
    },

    /**
     * Set topic for a specific part
     */
    setPartTopic: (partNumber, topic) => {
        const { formData } = get();
        const updatedConfigs = {
            ...formData.partConfigs,
            [partNumber]: {
                ...(formData.partConfigs[partNumber] || {}),
                topic: topic
            }
        };
        set({ formData: { ...formData, partConfigs: updatedConfigs } });
    },

    /**
     * Add a fact to a specific part
     */
    addPartFact: (partNumber, fact) => {
        const { formData } = get();
        const partConfig = formData.partConfigs[partNumber] || { facts: [] };
        const currentFacts = partConfig.facts || [];
        if (fact.trim() && currentFacts.length < 30) {
            const updatedConfigs = {
                ...formData.partConfigs,
                [partNumber]: {
                    ...partConfig,
                    facts: [...currentFacts, fact.trim()]
                }
            };
            set({ formData: { ...formData, partConfigs: updatedConfigs } });
        }
    },

    /**
     * Remove a fact from a specific part
     */
    removePartFact: (partNumber, index) => {
        const { formData } = get();
        const partConfig = formData.partConfigs[partNumber] || { facts: [] };
        const currentFacts = partConfig.facts || [];
        const updatedConfigs = {
            ...formData.partConfigs,
            [partNumber]: {
                ...partConfig,
                facts: currentFacts.filter((_, i) => i !== index)
            }
        };
        set({ formData: { ...formData, partConfigs: updatedConfigs } });
    },

    /**
     * Set passage length for a specific part (Reading only)
     */
    setPartPassageLength: (partNumber, length) => {
        const { formData } = get();
        const updatedConfigs = {
            ...formData.partConfigs,
            [partNumber]: {
                ...(formData.partConfigs[partNumber] || {}),
                passageLength: length
            }
        };
        set({ formData: { ...formData, partConfigs: updatedConfigs } });
    },

    /**
     * Update a generated question in the result (for preview editing)
     */
    updateGeneratedQuestion: (questionId, updates) => {
        const { generationResult } = get();
        if (!generationResult?.content?.questions) return;

        const updatedQuestions = generationResult.content.questions.map((q, idx) => {
            const syntheticId = `abts-q-${idx}`;
            // Match against real ID or synthetic ID (StepPreview uses synthetic)
            if (questionId === q.id || questionId === syntheticId) {
                return { ...q, ...updates };
            }
            return q;
        });

        set({
            generationResult: {
                ...generationResult,
                content: {
                    ...generationResult.content,
                    questions: updatedQuestions
                }
            }
        });
    },

    /**
     * Add a fact
     */
    addFact: (fact) => {
        const { formData } = get();
        if (fact.trim() && formData.facts.length < 30) {
            set({
                formData: {
                    ...formData,
                    facts: [...formData.facts, fact.trim()]
                }
            });
        }
    },

    /**
     * Remove a fact by index
     */
    removeFact: (index) => {
        const { formData } = get();
        set({
            formData: {
                ...formData,
                facts: formData.facts.filter((_, i) => i !== index)
            }
        });
    },

    /**
     * Toggle a question type
     */
    toggleQuestionType: (typeId) => {
        const { formData } = get();
        const currentTypes = formData.questionTypes || [];
        const newTypes = currentTypes.includes(typeId)
            ? currentTypes.filter(t => t !== typeId)
            : [...currentTypes, typeId];

        // Also update counts: add with default 2, or remove
        const newCounts = { ...formData.questionTypeCounts };
        if (newTypes.includes(typeId) && !newCounts[typeId]) {
            newCounts[typeId] = 2; // Default count
        } else if (!newTypes.includes(typeId)) {
            delete newCounts[typeId];
        }

        set({
            formData: { ...formData, questionTypes: newTypes, questionTypeCounts: newCounts }
        });
    },

    /**
     * Set question count for a specific type
     */
    setQuestionTypeCount: (typeId, count) => {
        const { formData } = get();
        const clampedCount = Math.max(1, Math.min(10, count));

        // Ensure type is in questionTypes
        const currentTypes = formData.questionTypes || [];
        const newTypes = currentTypes.includes(typeId)
            ? currentTypes
            : [...currentTypes, typeId];

        const newCounts = {
            ...formData.questionTypeCounts,
            [typeId]: clampedCount
        };

        set({
            formData: {
                ...formData,
                questionTypes: newTypes,
                questionTypeCounts: newCounts
            }
        });
    },

    /**
     * Load a template into form
     */
    loadTemplate: (template) => {
        set(state => ({
            formData: {
                ...state.formData,
                topic: template.name,
                hashtags: template.hashtags || [],
                facts: template.facts || []
            }
        }));
    },

    // ==================== GENERATION ACTIONS ====================

    /**
     * Generate content based on current form data
     */
    generate: async () => {
        const { formData } = get();
        set({
            isGenerating: true,
            generationError: null,
            generationProgress: 10
        });

        try {
            const resolvePartNumber = () => {
                if (formData.skill === SKILL_TYPES.WRITING) {
                    const types = formData.questionTypes || [];
                    const hasTask1 = types.includes('TASK_1');
                    const hasTask2 = types.includes('TASK_2');
                    if (hasTask2 && !hasTask1) return 2;
                    if (hasTask1 && !hasTask2) return 1;
                }
                return formData.partNumber;
            };

            const hasCustomCounts = Object.keys(formData.questionTypeCounts).length > 0;
            const shouldSendTotalQuestions = hasCustomCounts
                || formData.questionTypes.length > 0
                || (typeof formData.totalQuestions === 'number' && formData.totalQuestions !== 13);

            // Build request
            const request = {
                skill: formData.skill,
                scope: formData.scope,
                partNumber: resolvePartNumber(),
                topic: formData.topic,
                hashtags: formData.hashtags,
                facts: formData.facts,
                difficulty: formData.difficulty,
                explanationLanguage: formData.explanationLanguage,
                testType: formData.testType,
                questionTypes: formData.questionTypes.length > 0 ? formData.questionTypes : null,
                model: formData.model,
                enableReasoning: formData.enableReasoning,
                reasoningEffort: formData.reasoningEffort,
                temperature: formData.temperature,
                questionTypeCounts: Object.keys(formData.questionTypeCounts).length > 0
                    ? formData.questionTypeCounts : null,
                passageLength: formData.passageLength,
                customInstructions: formData.customInstructions || null,
                maxTokens: formData.maxTokens,
                totalQuestions: shouldSendTotalQuestions ? formData.totalQuestions : null,
                writingEssayType: formData.writingEssayType || null,
                // Multi-part generation (v6.0)
                partsToGenerate: formData.selectedParts?.length > 0 ? formData.selectedParts : null,
                partConfigs: Object.keys(formData.partConfigs || {}).length > 0 ? formData.partConfigs : null
            };

            set({ generationProgress: 30 });

            // Call appropriate API based on skill
            let result;
            switch (formData.skill) {
                case SKILL_TYPES.READING:
                    result = await generateReading(request);
                    break;
                case SKILL_TYPES.LISTENING:
                    result = await generateListening(request);
                    break;
                case SKILL_TYPES.WRITING:
                    result = await generateWriting(request);
                    break;
                default:
                    throw new Error('Unsupported skill type');
            }

            set({ generationProgress: 90 });

            // Handle result
            if (result.status === 'SUCCESS' || result.status === 'PARTIAL_SUCCESS') {
                set({
                    generationResult: result,
                    isGenerating: false,
                    generationProgress: 100,
                    currentStep: 5 // Auto-advance to preview
                });
            } else {
                set({
                    generationResult: result,
                    generationError: result.errors?.join(', ') || 'Generation failed',
                    isGenerating: false,
                    generationProgress: 0
                });
            }

            return result;

        } catch (error) {
            console.error('Generation failed:', error);
            set({
                generationError: error.message || 'Failed to generate content',
                isGenerating: false,
                generationProgress: 0
            });
            throw error;
        }
    },

    /**
     * Generate content with streaming progress updates
     */
    generateStreaming: async () => {
        const { formData } = get();

        set({
            isGenerating: true,
            generationError: null,
            generationProgress: 0,
            streamEvents: [],
            generationResult: null,
            reasoning: '' // Reset accumulated reasoning
        });

        const resolvePartNumber = () => {
            if (formData.skill === SKILL_TYPES.WRITING) {
                const types = formData.questionTypes || [];
                const hasTask1 = types.includes('TASK_1');
                const hasTask2 = types.includes('TASK_2');
                if (hasTask2 && !hasTask1) return 2;
                if (hasTask1 && !hasTask2) return 1;
            }
            return formData.partNumber;
        };

        const hasCustomCounts = Object.keys(formData.questionTypeCounts).length > 0;
        const shouldSendTotalQuestions = hasCustomCounts
            || formData.questionTypes.length > 0
            || (typeof formData.totalQuestions === 'number' && formData.totalQuestions !== 13);

        // Build request
        const request = {
            skill: formData.skill,
            scope: formData.scope,
            partNumber: resolvePartNumber(),
            topic: formData.topic,
            hashtags: formData.hashtags,
            facts: formData.facts,
            difficulty: formData.difficulty,
            explanationLanguage: formData.explanationLanguage,
            testType: formData.testType,
            questionTypes: formData.questionTypes.length > 0 ? formData.questionTypes : null,
            model: formData.model,
            enableReasoning: formData.enableReasoning,
            reasoningEffort: formData.reasoningEffort,
            temperature: formData.temperature,
            // Power-user settings
            questionTypeCounts: Object.keys(formData.questionTypeCounts).length > 0
                ? formData.questionTypeCounts : null,
            passageLength: formData.passageLength,
            customInstructions: formData.customInstructions || null,
            maxTokens: formData.maxTokens,
            totalQuestions: shouldSendTotalQuestions ? formData.totalQuestions : null,
            writingEssayType: formData.writingEssayType || null,
            // Multi-part generation (v6.0)
            partsToGenerate: formData.selectedParts?.length > 0 ? formData.selectedParts : null,
            partConfigs: Object.keys(formData.partConfigs || {}).length > 0 ? formData.partConfigs : null
        };

        // Callbacks for streaming events
        const callbacks = {
            onProgress: (event) => {
                // AI_THINKING: Only accumulate reasoning, don't add to log (prevents flooding)
                if (event.type === 'AI_THINKING' && event.message) {
                    set(state => ({
                        reasoning: state.reasoning + event.message
                    }));
                    return; // Don't add to streamEvents
                }

                // AI_CHUNK: Skip from log display (JSON data, not useful to show)
                if (event.type === 'AI_CHUNK') {
                    return; // Don't add to streamEvents
                }

                // All other events: add to log normally
                set(state => ({
                    streamEvents: [...state.streamEvents, event],
                    generationProgress: event.progress ?? state.generationProgress
                }));
            },
            onComplete: (result) => {
                set({
                    generationResult: result,
                    isGenerating: false,
                    generationProgress: 100,
                    currentStep: 5 // Auto-advance to preview
                });
            },
            onError: (errorMessage) => {
                set({
                    generationError: errorMessage,
                    isGenerating: false,
                    generationProgress: 0
                });
            },
            // Abort function is provided immediately via this callback
            onAbort: (abortFn) => {
                set({ abortStream: abortFn });
            }
        };

        try {
            // Route to skill-specific streaming function
            switch (formData.skill) {
                case SKILL_TYPES.READING:
                    await generateReadingStream(request, callbacks);
                    break;
                case SKILL_TYPES.LISTENING:
                    await generateListeningStream(request, callbacks);
                    break;
                case SKILL_TYPES.WRITING:
                    await generateWritingStream(request, callbacks);
                    break;
                default:
                    // Fallback to non-streaming for unsupported skills (e.g., Speaking)
                    return get().generate();
            }
        } catch (error) {
            console.error('Streaming generation failed:', error);
            set({
                generationError: error.message || 'Streaming failed',
                isGenerating: false,
                generationProgress: 0
            });
        }
    },

    /**
     * Abort current streaming generation
     */
    abortGeneration: () => {
        const { abortStream } = get();

        // Call the abort function if available
        if (abortStream) {
            try {
                abortStream();
            } catch (e) {
                console.warn('Error aborting stream:', e);
            }
        }

        // ALWAYS reset state, even if abortStream was null
        set({
            isGenerating: false,
            generationProgress: 0,
            abortStream: null,
            streamEvents: [...get().streamEvents, { type: 'ABORTED', message: 'Generation aborted by user' }]
        });
    },

    /**
     * Regenerate specific questions
     */
    regenerateQuestions: async (questionNumbers) => {
        const { formData, generationResult } = get();

        if (!generationResult?.content?.section?.passageText) {
            throw new Error('No existing passage to regenerate questions for');
        }

        set({ isGenerating: true, generationError: null });

        try {
            const request = {
                ...formData,
                existingPassageText: generationResult.content.section.passageText,
                questionsToRegenerate: questionNumbers
            };

            const result = await regenerateQuestions(request);

            if (result.status === 'SUCCESS' || result.status === 'PARTIAL_SUCCESS') {
                // Merge regenerated questions with existing
                const existingQuestions = generationResult.content.questions || [];
                const newQuestions = result.content?.questions || [];

                // Replace only the regenerated questions
                const mergedQuestions = existingQuestions.map(q => {
                    const regenerated = newQuestions.find(nq => nq.questionNumber === q.questionNumber);
                    return regenerated || q;
                });

                set({
                    generationResult: {
                        ...generationResult,
                        content: {
                            ...generationResult.content,
                            questions: mergedQuestions
                        }
                    },
                    isGenerating: false
                });
            } else {
                set({
                    generationError: result.errors?.join(', ') || 'Regeneration failed',
                    isGenerating: false
                });
            }

            return result;

        } catch (error) {
            console.error('Question regeneration failed:', error);
            set({
                generationError: error.message,
                isGenerating: false
            });
            throw error;
        }
    },

    /**
     * Clear generation result
     */
    clearResult: () => {
        set({
            generationResult: null,
            generationError: null,
            generationProgress: 0
        });
    },

    /**
     * Update the generated passage text
     */
    updateGeneratedPassage: (newText) => {
        const { generationResult } = get();
        if (generationResult?.content?.section) {
            set({
                generationResult: {
                    ...generationResult,
                    content: {
                        ...generationResult.content,
                        section: {
                            ...generationResult.content.section,
                            passageText: newText,
                            // Invalidate word count since text changed
                            wordCountValid: null
                        }
                    }
                }
            });
        }
    },

    // NOTE: updateGeneratedQuestion is defined earlier in the file (around line 571)
    // It handles both questionId string matching (e.g., 'abts-q-0') and partial updates
    // DO NOT add a duplicate here - the one above handles StepPreview editing correctly

    // ==================== SAVE ACTIONS ====================

    /**
     * Set save target options (TestSet, Test, etc.)
     */
    setSaveOptions: (options) => {
        set(state => ({
            selectedSetId: options.setId ?? state.selectedSetId,
            selectedSetCode: options.setCode ?? state.selectedSetCode,
            selectedTestId: options.testId ?? state.selectedTestId
        }));
    },

    /**
     * Save the generated content to the database using the test hierarchy.
     */
    saveGeneratedContent: async (options = {}) => {
        const { generationResult, formData, selectedSetId, selectedSetCode, selectedTestId } = get();

        if (!generationResult?.content) {
            throw new Error('No generated content to save');
        }

        set({ isSaving: true, saveError: null });

        try {
            // Prepare multi-part data if sections array exists
            let partsToSave = null;
            if (generationResult.content.sections && generationResult.content.sections.length > 0) {
                partsToSave = generationResult.content.sections.map(section => {
                    // Filter questions by standard IELTS ranges based on part number
                    const pn = section.partNumber;
                    const skill = formData.skill?.toUpperCase();

                    const filteredQuestions = generationResult.content.questions.filter(q => {
                        const qn = q.questionNumber;

                        if (skill === 'READING') {
                            if (pn === 1) return qn >= 1 && qn <= 13;
                            if (pn === 2) return qn >= 14 && qn <= 26;
                            if (pn === 3) return qn >= 27 && qn <= 40;
                        } else if (skill === 'LISTENING') {
                            if (pn === 1) return qn >= 1 && qn <= 10;
                            if (pn === 2) return qn >= 11 && qn <= 20;
                            if (pn === 3) return qn >= 21 && qn <= 30;
                            if (pn === 4) return qn >= 31 && qn <= 40;
                        }
                        return true; // Include question if skill/part not matched
                    });

                    // Match backend PartSaveData structure: { partNumber, content }
                    return {
                        partNumber: pn,
                        content: {
                            section: section,
                            questions: filteredQuestions
                        }
                    };
                });
            }


            const saveRequest = {
                skill: formData.skill?.toLowerCase() || 'reading',
                partNumber: formData.partNumber || 1,
                content: generationResult.content,
                // New fields for naming
                setId: options.setId ?? selectedSetId,
                setCode: options.setCode ?? selectedSetCode ?? 'ai_generated',
                setName: options.setName || null, // Pass user-provided set name
                testId: options.testId ?? selectedTestId,
                testName: options.testName || null, // Pass user-provided test name

                topic: formData.topic || null,
                difficulty: formData.difficulty || 'INTERMEDIATE',
                hashtagCodes: formData.hashtags || [],
                generationConfig: {
                    topic: formData.topic,
                    facts: formData.facts,
                    difficulty: formData.difficulty,
                    testType: formData.testType,
                    questionTypes: formData.questionTypes,
                    model: formData.model,
                    temperature: formData.temperature,
                    writingEssayType: formData.writingEssayType || null
                },
                examSource: options.examSource || 'ai_generated',
                testNumber: options.testNumber || null,
                partsToSave: partsToSave // Include the split parts
            };

            const result = await saveGeneratedTest(saveRequest);

            // Invalidate test set cache so new content appears in lists immediately
            try {
                const { default: useTestSetStore } = await import('./useTestSetStore');
                useTestSetStore.getState().invalidateCache();
                console.log('[ABTS] Test set cache invalidated after save');
            } catch (cacheError) {
                console.warn('[ABTS] Could not invalidate cache:', cacheError);
            }

            set({
                isSaving: false,
                saveResult: result,
                saveError: null
            });

            return result;

        } catch (error) {
            console.error('Failed to save generated content:', error);
            set({
                isSaving: false,
                saveError: error.message || 'Failed to save content'
            });
            throw error;
        }
    },

    /**
     * Clear save result and error
     */
    clearSaveResult: () => {
        set({
            saveResult: null,
            saveError: null
        });
    },

    // ==================== DATA FETCHING ====================

    /**
     * Fetch available AI models
     */
    fetchModels: async (force = false) => {
        const { lastModelsFetch, isLoadingModels } = get();
        const now = Date.now();

        // Skip if loading or cache valid (5 min)
        if (isLoadingModels) return;
        if (!force && lastModelsFetch && (now - lastModelsFetch) < 5 * 60 * 1000) return;

        set({ isLoadingModels: true });

        try {
            const models = await getAvailableModels();
            set({
                models,
                isLoadingModels: false,
                lastModelsFetch: Date.now()
            });
        } catch (error) {
            console.error('Failed to fetch models:', error);
            set({ isLoadingModels: false });
        }
    },

    /**
     * Fetch template categories
     */
    fetchTemplateCategories: async () => {
        const { isLoadingTemplates, templateCategories } = get();

        if (isLoadingTemplates || templateCategories.length > 0) return;

        set({ isLoadingTemplates: true });

        try {
            const categories = await getTemplateCategories();
            set({
                templateCategories: categories,
                isLoadingTemplates: false
            });
        } catch (error) {
            console.error('Failed to fetch template categories:', error);
            set({ isLoadingTemplates: false });
        }
    },

    /**
     * Fetch templates for a category
     */
    fetchTemplates: async (categoryId) => {
        const { templatesCache } = get();

        // Return cached if exists
        if (templatesCache[categoryId]) {
            return templatesCache[categoryId];
        }

        try {
            const templates = await getTemplatesByCategory(categoryId);
            set({
                templatesCache: {
                    ...templatesCache,
                    [categoryId]: templates
                }
            });
            return templates;
        } catch (error) {
            console.error('Failed to fetch templates:', error);
            return [];
        }
    },

    /**
     * Fetch ABTS status
     */
    fetchStatus: async (force = false) => {
        const { lastStatusFetch, isLoadingStatus } = get();
        const now = Date.now();

        if (isLoadingStatus) return;
        if (!force && lastStatusFetch && (now - lastStatusFetch) < 60 * 1000) return;

        set({ isLoadingStatus: true });

        try {
            const status = await getStatus();
            set({
                abtsStatus: status,
                isLoadingStatus: false,
                lastStatusFetch: Date.now()
            });
        } catch (error) {
            console.error('Failed to fetch ABTS status:', error);
            set({ isLoadingStatus: false });
        }
    },

    /**
     * Initialize ABTS - fetch all needed data
     */
    initialize: async () => {
        const { fetchModels, fetchTemplateCategories, fetchStatus } = get();
        await Promise.all([
            fetchModels(),
            fetchTemplateCategories(),
            fetchStatus()
        ]);
    },

    // ==================== SELECTORS ====================

    /**
     * Get current skill label
     */
    getSkillLabel: () => {
        const { formData } = get();
        const skills = {
            READING: 'Reading',
            LISTENING: 'Listening',
            WRITING: 'Writing',
            SPEAKING: 'Speaking'
        };
        return skills[formData.skill] || '';
    },

    /**
     * Get facts count and validity
     */
    getFactsStatus: () => {
        const { formData } = get();
        const count = formData.facts.length;
        return {
            count,
            isValid: count >= 5 && count <= 30,
            remaining: count < 5 ? 5 - count : 0
        };
    },

    /**
     * Check if can generate
     */
    canGenerate: () => {
        const { formData, isGenerating } = get();
        if (isGenerating || formData.skill === null) return false;

        if (formData.existingPassageText) return true;

        return formData.topic.trim().length >= 5 && formData.facts.length >= 5;
    },

    // ==================== REFINEMENT ACTIONS (AGENT 2) ====================

    /**
     * Toggle selection of an issue for refinement
     */
    toggleIssueSelection: (issueId) => {
        const { selectedIssues } = get();
        const newSelection = selectedIssues.includes(issueId)
            ? selectedIssues.filter(id => id !== issueId)
            : [...selectedIssues, issueId];
        set({ selectedIssues: newSelection });
    },

    /**
     * Select all issues for refinement
     */
    selectAllIssues: (issueIds) => {
        set({ selectedIssues: [...issueIds] });
    },

    /**
     * Clear all issue selections
     */
    clearIssueSelection: () => {
        set({ selectedIssues: [] });
    },

    /**
     * Start refinement with Agent 2
     */
    startRefinement: async () => {
        const { selectedIssues, generationResult, formData, isRefining } = get();

        if (isRefining) return;

        if (selectedIssues.length === 0) {
            console.warn('No issues selected for refinement');
            return;
        }

        set({
            isRefining: true,
            refinementResult: null,
            refinementStream: []
        });

        try {
            // Build validation result with proper issue objects
            // Selected issues are from warnings, so we need to construct matching objects
            const warnings = generationResult?.warnings || [];
            const validationIssues = warnings.map((msg, idx) => ({
                id: `warn-${idx}`,
                type: 'WARNING',
                message: typeof msg === 'string' ? msg : msg.message,
                questionNumber: typeof msg === 'object' ? msg.questionNumber : null,
                category: extractCategory(typeof msg === 'string' ? msg : msg.message)
            }));

            // Build request with model and caching settings for cost optimization
            const request = {
                originalJson: JSON.stringify(generationResult.content),
                selectedIssueIds: selectedIssues,
                originalPrompt: generationResult.metadata?.fullPrompt || null,
                skill: formData.skill,
                partNumber: formData.partNumber,
                model: formData.refinementModel, // User-selected model for refinement
                enableCaching: formData.enableRefinementCaching !== false, // Default to true
                enableReasoning: formData.enableRefinementReasoning === true, // Default to false
                validationResult: {
                    errors: [],
                    warnings: validationIssues
                }
            };

            // Create abort controller
            const abortController = new AbortController();
            set({ abortRefinement: () => abortController.abort() });

            // Call streaming refinement API
            const callbacks = {
                onProgress: (event) => {
                    set(state => ({
                        refinementStream: [...state.refinementStream, event]
                    }));
                },
                onComplete: (result) => {
                    set({
                        refinementResult: result,
                        isRefining: false
                    });
                },
                onError: (error) => {
                    console.error('Refinement error:', error);
                    set({
                        isRefining: false,
                        refinementResult: { error: error.message || 'Refinement failed' }
                    });
                }
            };

            await refineContentStream(request, callbacks, abortController.signal);

        } catch (error) {
            console.error('Refinement failed:', error);
            set({
                isRefining: false,
                refinementResult: { error: error.message || 'Refinement failed' }
            });
        }
    },

    /**
     * Apply refinement result to generation result and revalidate
     */
    applyRefinement: async () => {
        const { refinementResult, generationResult, formData } = get();

        if (!refinementResult) {
            console.warn('No refinement result to apply');
            return;
        }

        // Check for errors from backend
        if (!refinementResult.success || refinementResult.errorMessage) {
            console.error('Refinement failed:', refinementResult.errorMessage);
            set({
                refinementResult: {
                    ...refinementResult,
                    error: refinementResult.errorMessage || 'Refinement failed'
                }
            });
            return;
        }

        if (!refinementResult.refinedJson) {
            console.warn('No refined JSON to apply');
            return;
        }

        try {
            const refinedContent = JSON.parse(refinementResult.refinedJson);

            // Apply refined content first
            set({
                generationResult: {
                    ...generationResult,
                    content: refinedContent,
                    warnings: [] // Will be updated by validation
                },
                refinementResult: null,
                selectedIssues: [],
                refinementStream: []
            });
            console.log('Refinement applied, now revalidating...');

            // Call backend to revalidate the refined content
            try {
                const userId = localStorage.getItem('userId');
                const response = await fetch('/api/admin/abts/validate', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-User-Id': userId || 'admin'
                    },
                    body: JSON.stringify({
                        skill: formData.skill,
                        content: refinedContent
                    })
                });

                if (response.ok) {
                    const validationResult = await response.json();
                    const newWarnings = validationResult.warnings || [];

                    // Update with new validation warnings
                    set((state) => ({
                        generationResult: {
                            ...state.generationResult,
                            warnings: newWarnings
                        }
                    }));

                    console.log(`Revalidation complete: ${newWarnings.length} warnings remaining`);
                } else {
                    console.warn('Validation request failed, warnings not updated');
                }
            } catch (validationError) {
                console.warn('Revalidation failed:', validationError);
                // Content is already applied, just log the validation error
            }
        } catch (error) {
            console.error('Failed to parse refined JSON:', error);
            set({
                refinementResult: {
                    ...refinementResult,
                    error: `JSON parse error: ${error.message}`
                }
            });
        }
    },

    /**
     * Discard refinement result
     */
    discardRefinement: () => {
        const { abortRefinement } = get();
        if (abortRefinement) {
            abortRefinement();
        }
        set({
            isRefining: false,
            refinementResult: null,
            selectedIssues: [],
            refinementStream: [],
            abortRefinement: null
        });
    }
}));

export default useABTSStore;
