/**
 * ABTS Store - Zustand store for AI-Based Test Generation System
 * 
 * Manages state for the generation wizard, API calls, and results.
 * Includes caching for templates and models.
 * 
 * @since 2025-12-20 - ABTS v2.0
 */
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
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
    applyRefinementHunks,
    validateContent,
    SKILL_TYPES,
    GENERATION_SCOPES
} from '../services/abtsApi';
import { buildABTSGenerationRequest } from '../utils/abtsGenerationPayload';
import { buildABTSSaveRequest } from '../utils/abtsSavePayload';
import { createABTSFormActions } from './abtsFormActions';

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

// Recommended default model (capability-driven picker falls back to this on first load)
export const DEFAULT_MODEL_ID = 'deepseek/deepseek-v4-flash';

// FIX 2: canonical "clean slate" for the loopable-refinement slice. Used by
// resetRefinementState() so every lifecycle entry point (new generation, clear
// result, open/close wizard) starts from an identical, known-good state and no
// stale hunks / round counters / in-flight flags leak between runs.
const REFINEMENT_RESET = {
    round: 0,
    hunks: [],
    acceptedHunkIds: [],
    appliedHistory: [],
    isApplying: false,
    isLooping: false,
    lastSkippedHunks: [], // FIX 9: [{id, reason}] from the most recent apply
    lastError: null,      // FIX 5: surfaced apply/refine failure message
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
    reasoningBudget: null, // Explicit thinking-token budget (vendor-specific); null = derive from effort/defaults
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

const useABTSStore = create(persist((set, get) => ({
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
    partErrors: null,       // FIX 12: per-part error map on PARTIAL_SUCCESS / all-parts-failed
    streamEvents: [],       // Array of streaming events for UI display
    streamPreview: '',      // Accumulated AI response chunks for live preview
    streamChunkCount: 0,    // Number of response chunks received
    abortStream: null,      // Function to abort streaming
    reasoning: '',          // Accumulated real-time reasoning tokens from AI_THINKING

    // ==================== CACHE DATA ====================
    models: [],
    capabilities: {}, // { modelId: capabilityDescriptor } - filled from models[].capabilities on fetch
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

    // ==================== LOOPABLE REFINEMENT (per-hunk approval) ====================
    refinement: {
        round: 0,                // Current refinement round (server caps at maxRefinementRounds)
        hunks: [],               // Proposed hunks: {id, op, path, before, after, issueIds, summary, severity}
        acceptedHunkIds: [],     // Hunk IDs the user has accepted (default: all)
        appliedHistory: [],      // [{ round, appliedCount, rejectedCount, at }]
        isApplying: false,       // Apply-accepted request in flight
        isLooping: false,        // Refine-again request in flight
        lastSkippedHunks: [],    // FIX 9: [{id, reason}] from the most recent apply
        lastError: null,         // FIX 5: surfaced apply/refine failure message
    },


    // ==================== AUDIO URLS (Listening) ====================
    audioUrls: {},               // { partNumber: url } - Audio URLs for Listening parts

    // ==================== WIZARD ACTIONS ====================

    /**
     * Open the generation wizard
     */
    openWizard: () => {
        get().resetRefinementState(); // FIX 2: never inherit a prior run's hunks
        set({
            isWizardOpen: true,
            currentStep: 1,
            formData: { ...initialFormState },
            generationResult: null,
            generationError: null,
            streamPreview: '',
            streamChunkCount: 0
        });
    },

    /**
     * Close the generation wizard
     */
    closeWizard: () => {
        get().resetRefinementState(); // FIX 2
        set({
            isWizardOpen: false,
            currentStep: 1,
            generationResult: null,
            generationError: null,
            streamPreview: '',
            streamChunkCount: 0
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

    ...createABTSFormActions(set, get, {
        initialFormState,
        READING_PART_TYPES,
        LISTENING_PART_TYPES,
        QUESTION_COUNTS
    }),

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
            const request = buildABTSGenerationRequest(formData);

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

        get().resetRefinementState(); // FIX 2: a fresh generation must start with no carried-over hunks
        set({
            isGenerating: true,
            generationError: null,
            generationProgress: 0,
            partErrors: null, // FIX 12: clear stale per-part errors from a prior run
            streamEvents: [],
            generationResult: null,
            streamPreview: '',
            streamChunkCount: 0,
            reasoning: '' // Reset accumulated reasoning
        });

        const request = buildABTSGenerationRequest(formData);

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

                // AI_CHUNK: Keep content chunks out of the event log, but surface them in the live preview.
                if (event.type === 'AI_CHUNK') {
                    const chunk = typeof event.data === 'string'
                        ? event.data
                        : event.data == null
                            ? ''
                            : JSON.stringify(event.data);

                    if (chunk) {
                        set(state => ({
                            streamPreview: state.streamPreview + chunk,
                            streamChunkCount: state.streamChunkCount + 1
                        }));
                    }
                    return; // Don't add to streamEvents
                }

                // All other events: add to log normally
                set(state => ({
                    streamEvents: [...state.streamEvents, event],
                    // FIX 3: progress must never move backwards (parts report local 0-100 ranges).
                    generationProgress: Math.max(state.generationProgress ?? 0, event.progress ?? 0)
                }));
            },
            onComplete: (result) => {
                set({
                    generationResult: result,
                    isGenerating: false,
                    generationProgress: 100,
                    // FIX 12: capture per-part errors so the UI can show a PARTIAL_SUCCESS banner.
                    partErrors: result?.partErrors ?? null,
                    currentStep: 5 // Auto-advance to preview
                });
            },
            onError: (errorMessage, data) => {
                set({
                    generationError: errorMessage,
                    // FIX 11/12: a FAILED event may carry a per-part errors map in its data payload.
                    partErrors: (data && typeof data === 'object') ? data : null,
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
        get().resetRefinementState(); // FIX 2
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
            const saveRequest = buildABTSSaveRequest({
                content: generationResult.content,
                formData,
                saveConfig: options,
                selectedSetId,
                selectedSetCode,
                selectedTestId,
            });

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

            // Build id -> capability descriptor map from the catalog payload.
            const capabilities = {};
            (models || []).forEach((model) => {
                if (model && model.id && model.capabilities) {
                    capabilities[model.id] = model.capabilities;
                }
            });

            // Pick a sensible default model the first time the catalog loads:
            // prefer the recommended deepseek/deepseek-v4-flash, else the first model.
            const { formData } = get();
            let nextModel = formData.model;
            if (!nextModel && Array.isArray(models) && models.length > 0) {
                const recommended = models.find((m) => m.id === DEFAULT_MODEL_ID);
                nextModel = recommended ? recommended.id : models[0].id;
            }

            set({
                models,
                capabilities,
                formData: nextModel === formData.model
                    ? formData
                    : { ...formData, model: nextModel },
                isLoadingModels: false,
                lastModelsFetch: Date.now()
            });
        } catch (error) {
            console.error('Failed to fetch models:', error);
            set({ isLoadingModels: false });
        }
    },

    /**
     * Look up the capability descriptor for a model id (defaults to the
     * currently selected model). Returns null when unknown.
     */
    selectCapabilitiesForModel: (modelId) => {
        const { capabilities, formData } = get();
        const id = modelId || formData.model;
        if (!id) return null;
        return capabilities[id] || null;
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
                round: get().refinement?.round || 0, // Loopable refinement round (server caps at 5)
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
                    // Loopable refinement: ingest hunks + round into refinement state
                    get().setRefinementResponse(result);
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

    // ==================== LOOPABLE REFINEMENT ACTIONS ====================

    /**
     * Ingest a refinement stream result into per-hunk approval state.
     * Defaults to ALL hunks accepted (opt-out model).
     */
    setRefinementResponse: (response) => {
        if (!response || response.error) return;
        const hunks = Array.isArray(response.hunks) ? response.hunks : [];
        const round = typeof response.round === 'number'
            ? response.round
            : (get().refinement?.round || 0) + 1;
        set((state) => ({
            refinement: {
                ...state.refinement,
                round,
                hunks,
                acceptedHunkIds: hunks.map((h) => h.id),
                isLooping: false,
            }
        }));
    },

    /** Accept a single hunk by id. */
    acceptHunk: (id) => set((state) => ({
        refinement: {
            ...state.refinement,
            acceptedHunkIds: state.refinement.acceptedHunkIds.includes(id)
                ? state.refinement.acceptedHunkIds
                : [...state.refinement.acceptedHunkIds, id],
        }
    })),

    /** Reject a single hunk by id. */
    rejectHunk: (id) => set((state) => ({
        refinement: {
            ...state.refinement,
            acceptedHunkIds: state.refinement.acceptedHunkIds.filter((h) => h !== id),
        }
    })),

    /** Accept every proposed hunk. */
    acceptAllHunks: () => set((state) => ({
        refinement: {
            ...state.refinement,
            acceptedHunkIds: state.refinement.hunks.map((h) => h.id),
        }
    })),

    /** Reject every proposed hunk. */
    rejectAllHunks: () => set((state) => ({
        refinement: {
            ...state.refinement,
            acceptedHunkIds: [],
        }
    })),

    /**
     * Apply accepted hunks via backend patch, then revalidate.
     */
    applyAcceptedHunks: async () => {
        const { refinement, generationResult, formData } = get();
        const { hunks, acceptedHunkIds, isApplying } = refinement;

        if (isApplying || acceptedHunkIds.length === 0 || hunks.length === 0) {
            return;
        }

        set((state) => ({ refinement: { ...state.refinement, isApplying: true } }));

        try {
            const result = await applyRefinementHunks({
                originalJson: JSON.stringify(generationResult.content),
                hunks,
                acceptedHunkIds,
            });

            if (!result?.success || !result?.patchedJson) {
                const failMsg = result?.errorMessage || 'Failed to apply hunks';
                set((state) => ({
                    refinement: { ...state.refinement, isApplying: false, lastError: failMsg },
                    refinementResult: {
                        ...(state.refinementResult || {}),
                        error: failMsg,
                    }
                }));
                return;
            }

            const patchedContent = JSON.parse(result.patchedJson);
            const historyEntry = {
                round: refinement.round,
                appliedCount: result.appliedCount ?? acceptedHunkIds.length,
                rejectedCount: result.rejectedCount ?? (hunks.length - acceptedHunkIds.length),
                at: Date.now(),
            };

            // Apply patched content; clear current hunk batch + streaming/selection
            set((state) => ({
                generationResult: {
                    ...state.generationResult,
                    content: patchedContent,
                    warnings: [],
                },
                refinementResult: null,
                refinementStream: [],
                selectedIssues: [],
                refinement: {
                    ...state.refinement,
                    hunks: [],
                    acceptedHunkIds: [],
                    appliedHistory: [...state.refinement.appliedHistory, historyEntry],
                    isApplying: false,
                    lastError: null, // FIX 5: clear any prior error on success
                    lastSkippedHunks: result.skippedHunks || [], // FIX 9: surface skipped hunks + reasons
                }
            }));

            // Revalidate patched content via the canonical helper.
            // FIX 1 (silent bug): post the content object directly so the
            // backend infers the correct skill from its top-level keys.
            try {
                const validationResult = await validateContent(patchedContent);
                set((state) => ({
                    generationResult: {
                        ...state.generationResult,
                        warnings: validationResult.warnings || [],
                        validationIssues: validationResult.issues || [], // FIX 3
                    }
                }));
            } catch (validationError) {
                console.warn('Revalidation after apply failed:', validationError);
            }
        } catch (error) {
            console.error('Apply accepted hunks failed:', error);
            // FIX 5: prefer a structured backend message (400 malformed body, etc.)
            // over the bare axios message so the user sees something actionable.
            const apiMsg = error?.data?.errorMessage
                || error?.response?.data?.errorMessage
                || error?.message
                || 'Failed to apply hunks';
            set((state) => ({
                refinement: { ...state.refinement, isApplying: false, lastError: apiMsg },
                refinementResult: {
                    ...(state.refinementResult || {}),
                    error: apiMsg,
                }
            }));
        }
    },

    /**
     * Run another refinement round on the still-selected issues.
     * Server caps at 5; guard client-side too.
     */
    refineAgain: async () => {
        const { refinement, isRefining, startRefinement, abtsStatus } = get();
        // FIX 4: also block while an apply is mid-flight.
        if (isRefining || refinement.isLooping || refinement.isApplying) return;

        // FIX 11: round cap comes from backend status, not a hardcoded 5.
        const maxRounds = abtsStatus?.maxRefinementRounds || 5;
        const nextRound = (refinement.round || 0) + 1;
        if (nextRound > maxRounds) {
            set((state) => ({
                refinementResult: {
                    ...(state.refinementResult || {}),
                    error: `Refinement limit reached (${maxRounds} rounds)`,
                }
            }));
            return;
        }

        set((state) => ({ refinement: { ...state.refinement, isLooping: true } }));
        await startRefinement();
    },

    /**
     * Close the refinement loop and reset all refinement state.
     */
    closeRefinement: () => {
        get().resetRefinementState();
    },

    /**
     * FIX 2: single source of truth for tearing down ALL refinement state.
     * Aborts any in-flight stream and resets both the legacy Agent-2 slice and
     * the loopable per-hunk slice to a clean baseline. Call this on every
     * lifecycle boundary (new generation, clear result, open/close wizard) so
     * stale hunks, round counters, or in-flight flags never leak between runs.
     */
    resetRefinementState: () => {
        const { abortRefinement } = get();
        if (abortRefinement) {
            try {
                abortRefinement();
            } catch (e) {
                console.warn('Error aborting refinement during reset:', e);
            }
        }
        set({
            isRefining: false,
            refinementResult: null,
            selectedIssues: [],
            refinementStream: [],
            abortRefinement: null,
            refinement: { ...REFINEMENT_RESET },
        });
    }
}), {
    // FIX 12: persist only the user's last model + skill choice across reloads.
    // Everything else (cache, streaming, results) is intentionally NOT persisted.
    name: 'abts-form-v1',
    partialize: (state) => ({
        formData: {
            model: state.formData?.model,
            skill: state.formData?.skill,
        },
    }),
    // Zustand's default merge is SHALLOW, which would replace the whole `formData`
    // object with the persisted `{ model, skill }` and wipe every other default
    // (selectedParts, partConfigs, difficulty, ...), crashing components that read
    // those nested fields. Deep-merge `formData` so persisted values only overlay
    // the initial defaults.
    merge: (persisted, current) => ({
        ...current,
        ...(persisted || {}),
        formData: {
            ...current.formData,
            ...((persisted && persisted.formData) || {}),
        },
    }),
}));

/**
 * FIX 9: resolve the recommended/default generation model id.
 * Prefers the backend-advertised default (abtsStatus.defaultGenerationModel) and
 * falls back to the local DEFAULT_MODEL_ID constant when status is unavailable.
 *
 * @param {Object} state - the ABTS store state
 * @returns {string} the default generation model id
 */
export const selectDefaultModelId = (state) =>
    state.abtsStatus?.defaultGenerationModel ?? DEFAULT_MODEL_ID;

export default useABTSStore;
