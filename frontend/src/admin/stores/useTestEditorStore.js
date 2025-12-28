/**
 * Zustand store for Test Editor page.
 * Manages test editing state, sections, questions, and AI generation integration.
 * 
 * @since 2025-12-20 - Phase 4
 */

import { create } from 'zustand';
import adminApi from '../api/adminApi';
import abtsApi from '../services/abtsApi';

const useTestEditorStore = create((set, get) => ({
    // ==================== STATE ====================

    // Test data
    test: null,
    sections: [],
    questions: [],
    allQuestions: [], // All questions for all sections (for footer display)

    // Active selections
    activeSkill: 'reading',
    activeSection: null,

    // Loading states
    isLoading: false,
    isLoadingSections: false,
    isLoadingQuestions: false,
    isSaving: false,
    isPublishing: false,

    // AI Generation state
    isGenerating: false,
    generationMode: 'FULL', // FULL, QUESTIONS_ONLY, PASSAGE_ONLY, FIX_QUESTION
    generationContext: null,
    generationProgress: 0,
    generationResult: null,
    generationError: null,

    // UI state
    showAIGenerationModal: false,
    showSectionEditor: false,
    showQuestionEditor: false,
    editingQuestion: null,

    // Error handling
    error: null,

    // ==================== SELECTORS ====================

    getActiveSection: () => {
        const { sections, activeSection } = get();
        return sections.find(s => s.id === activeSection) || null;
    },

    getSectionQuestions: (sectionId) => {
        const { questions } = get();
        return questions.filter(q => q.sectionId === sectionId);
    },

    getSkillStatus: (skillId) => {
        const { test } = get();
        return test?.skills?.[skillId]?.status || 'empty';
    },

    getSectionName: (section) => {
        const { activeSkill } = get();
        if (!section) return '';
        if (activeSkill === 'reading') return `Passage ${section.partNumber}`;
        if (activeSkill === 'listening') return `Part ${section.partNumber}`;
        if (activeSkill === 'writing') return `Task ${section.partNumber}`;
        if (activeSkill === 'speaking') return `Part ${section.partNumber}`;
        return `Section ${section.partNumber}`;
    },

    // ==================== ACTIONS ====================

    /**
     * Initialize editor with test data (Legacy).
     */
    initializeEditor: async (examSource, testNumber) => {
        set({ isLoading: true, error: null });

        try {
            const test = await adminApi.content.getTestDetails(examSource, parseInt(testNumber));
            set({ test, isLoading: false });

            // Also fetch sections for default skill
            get().fetchSections(examSource, testNumber, 'reading');
        } catch (error) {
            console.error('Failed to initialize editor:', error);
            set({ error: error.message || 'Failed to load test', isLoading: false });
        }
    },

    /**
     * Initialize editor with Test ID (New).
     */
    initializeEditorByTestId: async (testId) => {
        set({ isLoading: true, error: null });

        try {
            const dto = await adminApi.testsApi.getById(testId);

            // Transform DTO to match store state shape
            const test = {
                id: dto.id,
                examSource: dto.setCode,
                testNumber: dto.testNumber,
                name: dto.nameVi || dto.nameEn || `Test ${dto.testNumber}`,
                status: dto.isPublished ? 'PUBLISHED' : 'DRAFT',
                skills: {
                    reading: { status: dto.skillSectionCounts?.reading > 0 ? 'full' : 'empty' },
                    listening: { status: dto.skillSectionCounts?.listening > 0 ? 'full' : 'empty' },
                    writing: { status: dto.skillSectionCounts?.writing > 0 ? 'full' : 'empty' },
                    speaking: { status: dto.skillSectionCounts?.speaking > 0 ? 'full' : 'empty' }
                }
            };

            set({ test, isLoading: false });

            // Fetch sections for active skill (using new API or from DTO)
            const { activeSkill } = get();

            // If DTO has sections, use them
            if (dto.sectionsBySkill && dto.sectionsBySkill[activeSkill]) {
                set({ sections: dto.sectionsBySkill[activeSkill] });

                // Load all questions for footer
                get().fetchAllQuestions();

                // Auto-select first section
                if (dto.sectionsBySkill[activeSkill].length > 0) {
                    get().setActiveSection(dto.sectionsBySkill[activeSkill][0].id);
                }
            } else {
                // Otherwise fetch
                get().fetchSections(dto.setCode, dto.testNumber, activeSkill);
            }

        } catch (error) {
            console.error('Failed to initialize editor by ID:', error);
            set({ error: error.message || 'Failed to load test', isLoading: false });
        }
    },

    /**
     * Set active skill and fetch its sections.
     */
    setActiveSkill: async (skill, examSource, testNumber) => {

        set({
            activeSkill: skill,
            activeSection: null,
            questions: [],
            allQuestions: [],
            sections: []
        });
        await get().fetchSections(examSource, testNumber, skill);
    },

    /**
     * Fetch sections for a skill.
     */
    fetchSections: async (examSource, testNumber, skill) => {
        console.log('[TestEditor] fetchSections called:', { examSource, testNumber, skill });
        set({ isLoadingSections: true });
        const { test } = get();

        try {
            let sections;

            // If we have a Test ID, use the new API
            if (test && test.id) {
                sections = await adminApi.testsApi.getSections(test.id, skill);
            } else {
                // Fallback to legacy API
                sections = await adminApi.content.getSections(
                    examSource,
                    parseInt(testNumber),
                    skill
                );
            }

            set({ sections: sections || [], isLoadingSections: false });

            // Load all questions and auto-select first section
            if (sections && sections.length > 0) {
                // Do not await this, let it run in background for footer
                get().fetchAllQuestions();

                // Auto-select first section immediately
                get().setActiveSection(sections[0].id);
            }
        } catch (error) {
            console.error('Failed to fetch sections:', error);
            set({ sections: [], isLoadingSections: false });
        }
    },

    /**
     * Fetch all questions for all current sections.
     * Used to populate the footer with full test data.
     */
    fetchAllQuestions: async () => {
        const { sections } = get();
        if (!sections || sections.length === 0) return;

        try {
            const allQuestionsPromises = sections.map(s =>
                adminApi.content.getQuestions(s.id).catch(() => [])
            );
            const allQuestionsArrays = await Promise.all(allQuestionsPromises);

            // Safely parse each question's JSON fields
            const allQuestions = allQuestionsArrays.flat().map(q => {
                let parsedContent = q.questionContent;
                let parsedAnswer = q.correctAnswer;

                try {
                    if (typeof q.questionContent === 'string') {
                        parsedContent = JSON.parse(q.questionContent);
                    }
                } catch (e) {
                    // console.warn('Failed to parse questionContent for question:', q.id);
                }

                try {
                    if (typeof q.correctAnswer === 'string' && q.correctAnswer.startsWith('{')) {
                        parsedAnswer = JSON.parse(q.correctAnswer);
                    }
                } catch (e) {
                    // Keep as string
                }

                return { ...q, questionContent: parsedContent, correctAnswer: parsedAnswer };
            });

            console.log('[TestEditor] Loaded allQuestions:', allQuestions.length);
            set({ allQuestions });
        } catch (error) {
            console.error('Failed to fetch all questions:', error);
        }
    },


    /**
     * Set active section and fetch its questions.
     * Note: allQuestions is populated by fetchSections, not here.
     */
    setActiveSection: async (sectionId) => {
        set({ activeSection: sectionId, isLoadingQuestions: true });

        if (!sectionId) {
            set({ questions: [], isLoadingQuestions: false });
            return;
        }

        try {
            const questions = await adminApi.content.getQuestions(sectionId);
            // Parse questionContent if it's a JSON string  
            const parsedQuestions = (questions || []).map(q => {
                let parsedContent = q.questionContent;
                let parsedAnswer = q.correctAnswer;

                try {
                    if (typeof q.questionContent === 'string') {
                        parsedContent = JSON.parse(q.questionContent);
                    }
                } catch (e) { /* keep as is */ }

                try {
                    if (typeof q.correctAnswer === 'string' && q.correctAnswer.startsWith('{')) {
                        parsedAnswer = JSON.parse(q.correctAnswer);
                    }
                } catch (e) { /* keep as is */ }

                return { ...q, questionContent: parsedContent, correctAnswer: parsedAnswer };
            });

            set({ questions: parsedQuestions, isLoadingQuestions: false });
        } catch (error) {
            console.error('Failed to fetch questions:', error);
            set({ questions: [], isLoadingQuestions: false });
        }
    },

    /**
     * Save test as draft.
     */
    saveDraft: async (examSource, testNumber) => {
        const { isSaving } = get();
        if (isSaving) return false;

        set({ isSaving: true });

        try {
            await adminApi.content.updateTestStatus(examSource, parseInt(testNumber), 'DRAFT');
            set(state => ({
                test: { ...state.test, status: 'DRAFT' },
                isSaving: false
            }));
            return true;
        } catch (error) {
            console.error('Failed to save draft:', error);
            set({ isSaving: false });
            return false;
        }
    },

    /**
     * Publish test.
     */
    publishTest: async (examSource, testNumber) => {
        const { isPublishing } = get();
        if (isPublishing) return false;

        set({ isPublishing: true });

        try {
            await adminApi.content.updateTestStatus(examSource, parseInt(testNumber), 'PUBLISHED');
            set(state => ({
                test: { ...state.test, status: 'PUBLISHED' },
                isPublishing: false
            }));
            return true;
        } catch (error) {
            console.error('Failed to publish:', error);
            set({ isPublishing: false });
            return false;
        }
    },

    /**
     * Add new section.
     */
    addSection: async (examSource, testNumber) => {
        const { sections, activeSkill } = get();

        try {
            const existingParts = sections.map(s => s.partNumber);
            const nextPart = existingParts.length > 0 ? Math.max(...existingParts) + 1 : 1;

            const result = await adminApi.content.createSection({
                examSource,
                testNumber: parseInt(testNumber),
                skill: activeSkill,
                partNumber: nextPart
            });

            if (result.success) {
                // Refresh sections
                await get().fetchSections(examSource, testNumber, activeSkill);
                return result.sectionId;
            }
            return null;
        } catch (error) {
            console.error('Failed to add section:', error);
            return null;
        }
    },

    /**
     * Add new question to active section.
     */
    addQuestion: async (questionType = 'FILL_IN_BLANK') => {
        const { activeSection } = get();
        if (!activeSection) return null;

        try {
            const result = await adminApi.content.createQuestion(activeSection, {
                questionType,
                questionContent: JSON.stringify({ text: '' }),
                correctAnswer: JSON.stringify({ answer: '' })
            });

            if (result.success) {
                // Refresh questions
                await get().setActiveSection(activeSection);
                return result.questionNumber;
            }
            return null;
        } catch (error) {
            console.error('Failed to add question:', error);
            return null;
        }
    },

    /**
     * Delete question.
     */
    deleteQuestion: async (questionId) => {
        try {
            await adminApi.content.deleteQuestion(questionId);
            set(state => ({
                questions: state.questions.filter(q => q.id !== questionId)
            }));
            return true;
        } catch (error) {
            console.error('Failed to delete question:', error);
            return false;
        }
    },

    // ==================== AI GENERATION ====================

    /**
     * Open AI generation modal.
     */
    /**
     * Open AI generation modal with context.
     * @param {string} mode - 'FULL', 'QUESTIONS_ONLY', 'PASSAGE_ONLY', 'FIX_QUESTION'
     * @param {Object} context - Existing data (passage text, question content, etc.)
     */
    openAIGeneration: (mode = 'FULL', context = null) => {
        set({
            showAIGenerationModal: true,
            generationMode: mode,
            generationContext: context,
            generationResult: null,
            generationError: null,
            generationProgress: 0
        });
    },

    /**
     * Close AI generation modal.
     */
    closeAIGeneration: () => {
        set({ showAIGenerationModal: false });
    },

    /**
     * Generate content using AI.
     */
    generateWithAI: async (generationParams) => {
        set({
            isGenerating: true,
            generationProgress: 0,
            generationError: null
        });

        // Simulate progress
        const progressInterval = setInterval(() => {
            set(state => ({
                generationProgress: Math.min(state.generationProgress + 10, 90)
            }));
        }, 1500);

        try {
            // Select API based on skill
            let result;
            const skill = generationParams.skill?.toUpperCase() || 'READING';

            switch (skill) {
                case 'READING':
                    result = await abtsApi.generateReading(generationParams);
                    break;
                case 'LISTENING':
                    result = await abtsApi.generateListening(generationParams);
                    break;
                case 'WRITING':
                    result = await abtsApi.generateWriting(generationParams);
                    break;
                default:
                    result = await abtsApi.generateReading(generationParams);
            }

            clearInterval(progressInterval);
            set({
                generationResult: result,
                generationProgress: 100,
                isGenerating: false
            });

            return result;
        } catch (error) {
            clearInterval(progressInterval);
            console.error('AI generation failed:', error);
            set({
                generationError: error.message || 'Generation failed',
                isGenerating: false,
                generationProgress: 0
            });
            return null;
        }
    },

    /**
     * Apply generated content to database.
     * Creates sections and questions from AI-generated content.
     * Uses contentAdapter for consistent data transformation.
     * 
     * @param {Object} generatedContent - The generated content from AI
     * @param {string} examSource - Target exam source (e.g., 'cam17')
     * @param {number} testNumber - Target test number
     */
    applyGeneratedContent: async (generatedContent, examSource, testNumber) => {
        const { activeSkill } = get();

        console.log('[TestEditor] applyGeneratedContent called:', {
            examSource,
            testNumber,
            skill: activeSkill,
            hasContent: !!generatedContent
        });

        if (!generatedContent || !examSource || !testNumber) {
            console.error('[TestEditor] Missing required parameters');
            return false;
        }

        set({ isSaving: true });

        try {
            // Import content adapter dynamically to avoid circular deps
            const { transformForDatabase, validateForSave, createSavePreview } = await import('../utils/contentAdapter');

            // Transform content using adapter
            const transformed = transformForDatabase(generatedContent, activeSkill);

            if (!transformed) {
                console.error('[TestEditor] Content transformation failed');
                set({ isSaving: false });
                return false;
            }

            // Validate before save
            const validation = validateForSave(transformed);
            console.log('[TestEditor] Validation result:', validation);

            if (!validation.valid) {
                console.error('[TestEditor] Validation errors:', validation.errors);
                set({ isSaving: false });
                return false;
            }

            if (validation.warnings.length > 0) {
                console.warn('[TestEditor] Validation warnings:', validation.warnings);
            }

            // Log save preview
            const preview = createSavePreview(transformed);
            console.log('[TestEditor] Save preview:', preview);

            // Determine part number for the new section
            const { sections } = get();
            const existingParts = sections.map(s => s.partNumber);
            const nextPart = existingParts.length > 0 ? Math.max(...existingParts) + 1 : 1;

            // Create the section
            const sectionPayload = {
                examSource,
                testNumber: parseInt(testNumber),
                skill: activeSkill,
                partNumber: nextPart,
                passageText: transformed.section.passageText || '',
                audioUrl: transformed.section.audioUrl || null,
                sectionLayout: transformed.section.sectionLayout || null,
                imageDescription: transformed.section.imageDescription || null,
                displayContentUrl: transformed.section.displayContentUrl || null
            };

            console.log('[TestEditor] Creating section:', sectionPayload);
            const sectionResult = await adminApi.content.createSection(sectionPayload);

            if (!sectionResult.success || !sectionResult.sectionId) {
                console.error('[TestEditor] Failed to create section:', sectionResult);
                set({ isSaving: false });
                return false;
            }

            const newSectionId = sectionResult.sectionId;
            console.log('[TestEditor] Created section:', newSectionId);

            // Create questions
            let successCount = 0;
            let errorCount = 0;

            for (const question of transformed.questions) {
                try {
                    const questionPayload = {
                        questionType: question.questionType,
                        questionContent: question.questionContent,
                        correctAnswer: question.correctAnswer,
                        explanation: question.explanation,
                        wordLimit: question.wordLimit,
                        imageUrl: question.imageUrl
                    };

                    await adminApi.content.createQuestion(newSectionId, questionPayload);
                    successCount++;
                } catch (qError) {
                    console.error(`[TestEditor] Failed to create question ${question.questionNumber}:`, qError);
                    errorCount++;
                }
            }

            console.log(`[TestEditor] Created ${successCount} questions, ${errorCount} failed`);

            // Refresh sections to show new data
            await get().fetchSections(examSource, testNumber, activeSkill);

            // Select the new section
            await get().setActiveSection(newSectionId);

            set({
                showAIGenerationModal: false,
                isSaving: false
            });

            return true;
        } catch (error) {
            console.error('[TestEditor] Failed to apply generated content:', error);
            set({ isSaving: false });
            return false;
        }
    },

    // ==================== QUESTION EDITING ====================

    /**
     * Open question editor for a specific question.
     */
    openQuestionEditor: (question) => {
        set({
            showQuestionEditor: true,
            editingQuestion: question
        });
    },

    /**
     * Close question editor.
     */
    closeQuestionEditor: () => {
        set({
            showQuestionEditor: false,
            editingQuestion: null
        });
    },

    /**
     * Save question edits.
     */
    saveQuestion: async (questionId, updates) => {
        try {
            await adminApi.content.updateQuestion(questionId, updates);

            // Update local state
            set(state => ({
                questions: state.questions.map(q =>
                    q.id === questionId ? { ...q, ...updates } : q
                ),
                showQuestionEditor: false,
                editingQuestion: null
            }));

            return true;
        } catch (error) {
            console.error('Failed to save question:', error);
            return false;
        }
    },

    // ==================== RESET ====================

    /**
     * Reset store state.
     */
    reset: () => {
        set({
            test: null,
            sections: [],
            questions: [],
            activeSkill: 'reading',
            activeSection: null,
            isLoading: false,
            isLoadingSections: false,
            isLoadingQuestions: false,
            isSaving: false,
            isPublishing: false,
            isGenerating: false,
            generationProgress: 0,
            generationResult: null,
            generationError: null,
            showAIGenerationModal: false,
            showSectionEditor: false,
            showQuestionEditor: false,
            editingQuestion: null,
            error: null
        });
    }
}));

export default useTestEditorStore;
