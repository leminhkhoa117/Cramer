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
    SKILL_TYPES,
    GENERATION_SCOPES
} from '../services/abtsApi';

// Initial form state
const initialFormState = {
    skill: null,
    scope: GENERATION_SCOPES.SINGLE_PART,
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
    partNumber: 1, // Reading Part (1, 2, or 3)
    passageLength: 'MEDIUM', // 'SHORT' (800-900) | 'MEDIUM' (900-1000) | 'LONG' (1000-1200)
    customInstructions: '', // Custom prompt additions
    showJsonPreview: false, // Toggle JSON preview panel
    maxTokens: 8000, // Max output tokens (4000-16000)
    totalQuestions: 13, // Target total questions (10-20)
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
     */
    updateFormData: (updates) => {
        set(state => ({
            formData: { ...state.formData, ...updates }
        }));
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
            currentStep: 1
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
            // Build request
            const request = {
                skill: formData.skill,
                scope: formData.scope,
                partNumber: formData.partNumber,
                topic: formData.topic,
                hashtags: formData.hashtags,
                facts: formData.facts,
                difficulty: formData.difficulty,
                explanationLanguage: formData.explanationLanguage,
                testType: formData.testType,
                questionTypes: formData.questionTypes.length > 0 ? formData.questionTypes : null,
                model: formData.model,
                enableReasoning: formData.enableReasoning,
                reasoningEffort: formData.reasoningEffort
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

        // Build request
        const request = {
            skill: formData.skill,
            scope: formData.scope,
            partNumber: formData.partNumber,
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
            partNumber: formData.partNumber, // Reading Part (1, 2, or 3)
            passageLength: formData.passageLength,
            customInstructions: formData.customInstructions || null,
            maxTokens: formData.maxTokens,
            totalQuestions: formData.totalQuestions
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

    /**
     * Update a specific generated question
     */
    updateGeneratedQuestion: (questionIndex, updatedQuestion) => {
        const { generationResult } = get();
        if (generationResult?.content?.questions) {
            const newQuestions = [...generationResult.content.questions];
            // Ensure we're updating the right question by index if questionNumber matches
            if (newQuestions[questionIndex]) {
                newQuestions[questionIndex] = updatedQuestion;
                set({
                    generationResult: {
                        ...generationResult,
                        content: {
                            ...generationResult.content,
                            questions: newQuestions
                        }
                    }
                });
            }
        }
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
    }
}));

export default useABTSStore;
