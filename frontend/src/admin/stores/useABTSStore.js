/**
 * ABTS Store - Zustand store for AI-Based Test Generation System
 * 
 * Manages state for the generation wizard, API calls, and results.
 * Includes caching for templates and models.
 * API calls go through the shared lib/api client (contract-correct
 * against the backend AbtsController, SPEC-25).
 * 
 * @since 2025-12-20 - ABTS v2.0
 * @updated 2026-08 - wired to lib/api abtsApi + openAbtsStream
 */
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { abtsApi, openAbtsStream } from '../../lib/api';
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

// Canonical "clean slate" for the loopable-refinement slice. Used by
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
    lastSkippedHunks: [], // [{id, reason}] from the most recent apply
    lastError: null,      // surfaced apply/refine failure message
};

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

/** Stream events the UI synthesizes locally before the server stream opens. */
const localConnectingEvent = (message, progress) => ({ type: 'CONNECTING', message, progress, data: null, timestamp: Date.now() });

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
    partErrors: null,       // per-part error map on PARTIAL_SUCCESS / all-parts-failed
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
        hunks: [],               // Proposed hunks: {id, op, path, before, after, description}
        acceptedHunkIds: [],     // Hunk IDs the user has accepted (default: all)
        appliedHistory: [],      // [{ round, appliedCount, rejectedCount, at }]
        isApplying: false,       // Apply-accepted request in flight
        isLooping: false,        // Refine-again request in flight
        lastSkippedHunks: [],    // [{id, reason}] from the most recent apply
        lastError: null,         // surfaced apply/refine failure message
    },

    // ==================== MEDIA URLS ====================
    audioUrls: {},               // { partNumber: url } - Audio URLs for Listening parts
    imageUrls: {},               // { partNumber: url } - Figure/image URLs (map/plan labeling)

    // ==================== WIZARD ACTIONS ====================

    /**
     * Open the generation wizard
     */
    openWizard: () => {
        get().resetRefinementState();
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
        get().resetRefinementState();
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
     * Generate content based on current form data (synchronous API).
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

            const skill = String(formData.skill || '').toLowerCase();
            const result = await abtsApi.generate(skill, request);

            set({ generationProgress: 90 });

            if (result.status === 'SUCCESS' || result.status === 'PARTIAL_SUCCESS') {
                set({
                    generationResult: result,
                    isGenerating: false,
                    generationProgress: 100,
                    partErrors: result.partErrors ?? null,
                    currentStep: 5 // Auto-advance to preview
                });
            } else {
                set({
                    generationResult: result,
                    generationError: result.errorCode || 'Generation failed',
                    isGenerating: false,
                    generationProgress: 0
                });
            }

            return result;
        } catch (error) {
            console.error('Generation failed:', error);
            const message = error?.response?.data?.message || error.message || 'Failed to generate content';
            set({
                generationError: message,
                isGenerating: false,
                generationProgress: 0
            });
            throw error;
        }
    },

    /**
     * Generate content with streaming progress updates.
     */
    generateStreaming: async () => {
        const { formData } = get();

        get().resetRefinementState();
        set({
            isGenerating: true,
            generationError: null,
            generationProgress: 0,
            partErrors: null,
            streamEvents: [],
            generationResult: null,
            streamPreview: '',
            streamChunkCount: 0,
            reasoning: ''
        });

        const request = buildABTSGenerationRequest(formData);
        const skill = String(formData.skill || '').toLowerCase();
        let terminalEventReceived = false;

        const onProgress = (event) => {
            if (event.type === 'AI_THINKING') {
                const delta = typeof event.data === 'string' ? event.data : '';
                if (delta) {
                    set((state) => ({
                        reasoning: state.reasoning + delta
                    }));
                }
                return;
            }

            if (event.type === 'AI_CHUNK') {
                const chunk = typeof event.data === 'string'
                    ? event.data
                    : event.data == null
                        ? ''
                        : JSON.stringify(event.data);

                if (chunk) {
                    set((state) => ({
                        streamPreview: state.streamPreview + chunk,
                        streamChunkCount: state.streamChunkCount + 1
                    }));
                }
                return;
            }

            if (event.type === 'COMPLETED') {
                terminalEventReceived = true;
                set({
                    generationResult: event.data,
                    isGenerating: false,
                    generationProgress: 100,
                    partErrors: event.data?.partErrors ?? null,
                    currentStep: 5
                });
                return;
            }

            if (event.type === 'FAILED') {
                terminalEventReceived = true;
                set({
                    generationError: event.message || event.errorCode || 'Generation failed',
                    isGenerating: false,
                    generationProgress: 0
                });
                return;
            }

            if (event.type === 'ABORTED') {
                terminalEventReceived = true;
                set({
                    isGenerating: false,
                    generationProgress: 0,
                    streamEvents: [...get().streamEvents, event]
                });
                return;
            }

            set((state) => ({
                streamEvents: [...state.streamEvents, event],
                generationProgress: Math.max(state.generationProgress ?? 0, event.progress ?? 0)
            }));
        };

        const onError = (error) => {
            terminalEventReceived = true;
            if (error?.name === 'AbortError') return; // abortGeneration handles state
            set({
                generationError: error?.message || 'Stream failed',
                isGenerating: false,
                generationProgress: 0
            });
        };

        const onDone = () => {
            if (!terminalEventReceived) {
                set({
                    generationError: 'Stream ended before the server reported a result',
                    isGenerating: false,
                    generationProgress: 0
                });
            }
        };

        try {
            set({ streamEvents: [localConnectingEvent(`Requesting ${skill} generation...`, 5)] });
            const abortFn = openAbtsStream(`/admin/abts/generate/${skill}/stream`, request, {
                onEvent: onProgress,
                onError,
                onDone,
            });
            set({ abortStream: abortFn });
        } catch (error) {
            console.error('Streaming generation failed:', error);
            onError(error);
        }
    },

    /**
     * Abort current streaming generation
     */
    abortGeneration: () => {
        const { abortStream } = get();

        if (abortStream) {
            try {
                abortStream();
            } catch (e) {
                console.warn('Error aborting stream:', e);
            }
        }

        set({
            isGenerating: false,
            generationProgress: 0,
            abortStream: null,
            streamEvents: [...get().streamEvents, { type: 'ABORTED', message: 'Generation aborted by user' }]
        });
    },

    /**
     * Regenerate questions against the existing passage/transcript.
     * The backend regenerates the whole first part (SPEC-21 §9); the result
     * replaces that part's content.
     */
    regenerateQuestions: async () => {
        const { formData, generationResult } = get();
        const content = generationResult?.content;

        if (!content) {
            throw new Error('No generated content to regenerate from');
        }

        const existingPassage = content?.section?.passage_text ?? content?.transcript ?? null;
        if (!existingPassage) {
            throw new Error('No existing passage to regenerate questions for');
        }

        set({ isGenerating: true, generationError: null });

        try {
            const request = {
                ...buildABTSGenerationRequest(formData),
                existingPassageText: existingPassage,
            };
            const skill = String(formData.skill || '').toLowerCase();
            const result = await abtsApi.generateQuestions(skill, request);

            if (result.status === 'SUCCESS' || result.status === 'PARTIAL_SUCCESS') {
                const firstPart = request.partsToGenerate?.[0] ?? formData.partNumber ?? 1;
                let nextContent = result.content;

                if (Array.isArray(content.sections)) {
                    const sections = content.sections.map((section) =>
                        (section.part ?? section.partNumber) === firstPart ? result.content : section
                    );
                    nextContent = { ...content, sections };
                }

                set({
                    generationResult: { ...generationResult, content: nextContent, status: result.status },
                    partErrors: result.partErrors ?? null,
                    isGenerating: false
                });
            } else {
                set({
                    generationError: result.errorCode || 'Regeneration failed',
                    isGenerating: false
                });
            }

            return result;
        } catch (error) {
            console.error('Question regeneration failed:', error);
            set({
                generationError: error?.response?.data?.message || error.message,
                isGenerating: false
            });
            throw error;
        }
    },

    /**
     * Clear generation result
     */
    clearResult: () => {
        get().resetRefinementState();
        set({
            generationResult: null,
            generationError: null,
            generationProgress: 0
        });
    },

    /**
     * Update the generated passage text (raw snake_case shape).
     */
    updateGeneratedPassage: (newText) => {
        const { generationResult } = get();
        const section = generationResult?.content?.section;
        if (section) {
            set({
                generationResult: {
                    ...generationResult,
                    content: {
                        ...generationResult.content,
                        section: {
                            ...section,
                            passage_text: newText
                        }
                    }
                }
            });
        }
    },

    // ==================== SAVE ACTIONS ====================

    /**
     * Set save target options (TestSet, Test, etc.)
     */
    setSaveOptions: (options) => {
        set((state) => ({
            selectedSetId: options.setId ?? state.selectedSetId,
            selectedSetCode: options.setCode ?? state.selectedSetCode,
            selectedTestId: options.testId ?? state.selectedTestId
        }));
    },

    /**
     * Save the generated content to the database using the test hierarchy.
     */
    saveGeneratedContent: async (options = {}) => {
        const { generationResult, formData, selectedSetId, selectedSetCode, selectedTestId, audioUrls, imageUrls } = get();

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
                audioUrls,
                imageUrls,
            });

            const result = await abtsApi.save(saveRequest);

            try {
                const { default: useTestSetStore } = await import('./useTestSetStore');
                useTestSetStore.getState().invalidateCache();
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
                saveError: error?.response?.data?.message || error.message || 'Failed to save content'
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

        if (isLoadingModels) return;
        if (!force && lastModelsFetch && (now - lastModelsFetch) < 5 * 60 * 1000) return;

        set({ isLoadingModels: true });

        try {
            const models = await abtsApi.models();

            const capabilities = {};
            (models || []).forEach((model) => {
                if (model && model.id && model.capabilities) {
                    capabilities[model.id] = model.capabilities;
                }
            });

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
            const categories = await abtsApi.templates();
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

        if (templatesCache[categoryId]) {
            return templatesCache[categoryId];
        }

        try {
            const templates = await abtsApi.templatesByCategory(categoryId);
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
            const status = await abtsApi.status();
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
            ? selectedIssues.filter((id) => id !== issueId)
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
     * Start refinement with Agent 2 (streams proposed hunks).
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

        let terminalEventReceived = false;

        const request = {
            originalJson: generationResult?.content ?? null,
            issueIds: selectedIssues,
            skill: String(formData.skill || 'reading').toLowerCase(),
            part: formData.partNumber ?? null,
            taskType: null,
            model: {
                model: formData.refinementModel ?? null,
                temperature: null,
                maxTokens: null,
                enableReasoning: formData.enableRefinementReasoning === true,
                reasoningEffort: null,
                reasoningBudget: null,
                contextCache: formData.enableRefinementCaching !== false,
            },
            round: get().refinement?.round || 0,
            validation: generationResult?.validation ?? null,
        };

        const onEvent = (event) => {
            if (event.type === 'REFINEMENT_COMPLETED') {
                terminalEventReceived = true;
                set({ isRefining: false });
                get().setRefinementResponse(event.data);
                return;
            }
            if (event.type === 'FAILED') {
                terminalEventReceived = true;
                set({
                    isRefining: false,
                    refinementResult: { error: event.message || event.errorCode || 'Refinement failed' }
                });
                return;
            }
            set((state) => ({
                refinementStream: [...state.refinementStream, event]
            }));
        };

        const onError = (error) => {
            terminalEventReceived = true;
            if (error?.name === 'AbortError') return; // resetRefinementState handles aborts
            console.error('Refinement error:', error);
            set({
                isRefining: false,
                refinementResult: { error: error?.message || 'Refinement failed' }
            });
        };

        const onDone = () => {
            if (!terminalEventReceived) {
                set({
                    isRefining: false,
                    refinementResult: { error: 'Refinement stream ended before a result arrived' }
                });
            }
        };

        try {
            const abortFn = openAbtsStream('/admin/abts/refine/stream', request, {
                onEvent,
                onError,
                onDone,
            });
            set({ abortRefinement: abortFn });
        } catch (error) {
            onError(error);
        }
    },

    // ==================== LOOPABLE REFINEMENT ACTIONS ====================

    /**
     * Ingest the refinement result (a bare hunks array from
     * REFINEMENT_COMPLETED data) into per-hunk approval state.
     * Defaults to ALL hunks accepted (opt-out model).
     */
    setRefinementResponse: (response) => {
        if (!response || response.error) return;
        const rawHunks = Array.isArray(response) ? response : [];
        const hunks = rawHunks.map((hunk, index) => ({
            ...hunk,
            id: hunk.id || `hunk-${index}`,
            summary: hunk.description ?? hunk.summary ?? null,
        }));
        set((state) => ({
            refinementResult: { hunks },
            refinement: {
                ...state.refinement,
                round: (state.refinement.round || 0) + 1,
                hunks,
                acceptedHunkIds: hunks.map((hunk) => hunk.id),
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
            const skill = String(formData.skill || 'reading').toLowerCase();
            const partNumber = formData.partNumber || 1;
            const acceptedHunks = hunks.filter((hunk) => acceptedHunkIds.includes(hunk.id));

            const result = await abtsApi.applyRefinement({
                originalJson: generationResult.content,
                acceptedHunks,
                skill,
                part: partNumber,
                taskType: null,
            });

            const historyEntry = {
                round: refinement.round,
                appliedCount: acceptedHunks.length - (result.skipped?.length ?? 0),
                rejectedCount: hunks.length - acceptedHunks.length,
                at: Date.now(),
            };

            set((state) => ({
                generationResult: {
                    ...state.generationResult,
                    content: result.content,
                    validation: result.validation ?? state.generationResult?.validation ?? null,
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
                    lastError: null,
                    lastSkippedHunks: (result.skipped || []).map((id) => ({ id, reason: 'backend-skip' })),
                }
            }));
        } catch (error) {
            console.error('Apply accepted hunks failed:', error);
            const apiMsg = error?.response?.data?.message || error?.message || 'Failed to apply hunks';
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
     * Server caps at maxRefinementRounds; guard client-side too.
     */
    refineAgain: async () => {
        const { refinement, isRefining, startRefinement, abtsStatus } = get();
        if (isRefining || refinement.isLooping || refinement.isApplying) return;

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
     * Single source of truth for tearing down ALL refinement state.
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
    // Persist only the user's last model + skill choice across reloads.
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
 * Resolve the recommended/default generation model id.
 * Prefers the backend-advertised default (abtsStatus.defaultGenerationModel) and
 * falls back to the local DEFAULT_MODEL_ID constant when status is unavailable.
 *
 * @param {Object} state - the ABTS store state
 * @returns {string} the default generation model id
 */
export const selectDefaultModelId = (state) =>
    state.abtsStatus?.defaultGenerationModel ?? DEFAULT_MODEL_ID;

export default useABTSStore;
