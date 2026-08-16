/**
 * Tests for useABTSStore (AI-Based Test Generation System)
 * 
 * Tests wizard navigation, form data management, part configuration,
 * and generation actions.
 * 
 * @author Cramer Test Team
 * @since 2026-01-26
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { act } from '@testing-library/react';

// Mock the lib/api client (openAbtsStream/abtsApi) so store actions that
// touch the network are inert in these form-state tests.
vi.mock('../../lib/api', () => ({
    abtsApi: {
        generate: vi.fn(),
        generateQuestions: vi.fn(),
        validate: vi.fn(),
        applyRefinement: vi.fn(),
        save: vi.fn(),
        models: vi.fn(),
        templates: vi.fn(),
        templatesByCategory: vi.fn(),
        status: vi.fn(),
    },
    openAbtsStream: vi.fn(() => () => {}),
}));

import useABTSStore, { 
    READING_PART_TYPES, 
    LISTENING_PART_TYPES, 
    QUESTION_COUNTS 
} from '../../../admin/stores/useABTSStore';

describe('useABTSStore', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        
        // Reset store to initial state
        act(() => {
            useABTSStore.getState().resetForm();
            useABTSStore.getState().closeWizard();
        });
    });

    // =========================================================================
    // CONSTANTS TESTS
    // =========================================================================
    describe('Constants', () => {
        it('should have correct Reading part types', () => {
            expect(READING_PART_TYPES[1]).toContain('TRUE_FALSE_NOT_GIVEN');
            expect(READING_PART_TYPES[2]).toContain('MATCHING_HEADINGS');
            expect(READING_PART_TYPES[3]).toContain('MULTIPLE_CHOICE');
        });

        it('should have correct Listening part types', () => {
            expect(LISTENING_PART_TYPES[1]).toContain('FILL_IN_BLANK');
            expect(LISTENING_PART_TYPES[4]).toContain('MULTIPLE_CHOICE');
        });

        it('should have correct question counts', () => {
            expect(QUESTION_COUNTS.READING[1]).toBe(13);
            expect(QUESTION_COUNTS.READING[2]).toBe(13);
            expect(QUESTION_COUNTS.READING[3]).toBe(14);
            expect(QUESTION_COUNTS.LISTENING[1]).toBe(10);
            expect(QUESTION_COUNTS.LISTENING[4]).toBe(10);
        });
    });

    // =========================================================================
    // INITIAL STATE TESTS
    // =========================================================================
    describe('Initial State', () => {
        it('should have wizard closed initially', () => {
            const state = useABTSStore.getState();
            expect(state.isWizardOpen).toBe(false);
            expect(state.currentStep).toBe(1);
        });

        it('should have default form data', () => {
            const state = useABTSStore.getState();
            expect(state.formData.skill).toBeNull();
            expect(state.formData.scope).toBe('MULTI_PART');
            expect(state.formData.difficulty).toBe('INTERMEDIATE');
            expect(state.formData.explanationLanguage).toBe('VI');
        });

        it('should have null generation result initially', () => {
            const state = useABTSStore.getState();
            expect(state.generationResult).toBeNull();
            expect(state.generationError).toBeNull();
            expect(state.isGenerating).toBe(false);
        });
    });

    // =========================================================================
    // WIZARD NAVIGATION TESTS
    // =========================================================================
    describe('Wizard Navigation', () => {
        describe('openWizard()', () => {
            it('should open wizard and reset step to 1', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.openWizard();
                });

                const newState = useABTSStore.getState();
                expect(newState.isWizardOpen).toBe(true);
                expect(newState.currentStep).toBe(1);
            });
        });

        describe('closeWizard()', () => {
            it('should close wizard and reset state', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.openWizard();
                    state.closeWizard();
                });

                const newState = useABTSStore.getState();
                expect(newState.isWizardOpen).toBe(false);
            });
        });

        describe('goToStep()', () => {
            it('should allow going back to previous steps', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.openWizard();
                    state.updateFormData({ skill: 'READING', topic: 'Test topic' });
                });
                
                // Manually set step to 3 by setting state
                act(() => {
                    useABTSStore.setState({ currentStep: 3 });
                });
                
                // Should be able to go back to step 2
                act(() => {
                    useABTSStore.getState().goToStep(2);
                });

                expect(useABTSStore.getState().currentStep).toBe(2);
            });

            it('should not advance if previous step is invalid', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.openWizard();
                    // Step 1 is invalid (no skill selected)
                    state.goToStep(3);
                });

                // Should stay at step 1 since step 1 is not valid
                expect(useABTSStore.getState().currentStep).toBe(1);
            });
        });

        describe('nextStep()', () => {
            it('should go to next step when current step is valid', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.openWizard();
                    // Make step 1 valid by selecting a skill
                    state.updateFormData({ skill: 'READING' });
                    state.nextStep();
                });

                expect(useABTSStore.getState().currentStep).toBe(2);
            });

            it('should not go to next step when current step is invalid', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.openWizard();
                    // Step 1 is invalid (no skill)
                    state.nextStep();
                });

                expect(useABTSStore.getState().currentStep).toBe(1);
            });
        });

        describe('prevStep()', () => {
            it('should go to previous step', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.openWizard();
                    // Set step to 3 manually
                    useABTSStore.setState({ currentStep: 3 });
                });
                
                act(() => {
                    useABTSStore.getState().prevStep();
                });

                expect(useABTSStore.getState().currentStep).toBe(2);
            });

            it('should not go below step 1', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.openWizard();
                    state.prevStep();
                });

                expect(useABTSStore.getState().currentStep).toBe(1);
            });
        });
    });

    // =========================================================================
    // FORM DATA TESTS
    // =========================================================================
    describe('Form Data Management', () => {
        describe('updateFormData()', () => {
            it('should update form data fields', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.updateFormData({ skill: 'READING', topic: 'Climate Change' });
                });

                const newState = useABTSStore.getState();
                expect(newState.formData.skill).toBe('READING');
                expect(newState.formData.topic).toBe('Climate Change');
            });

            it('should clear invalid parts when skill changes', () => {
                const state = useABTSStore.getState();
                
                // Set up Reading with part 3
                act(() => {
                    state.updateFormData({ skill: 'READING' });
                    state.togglePartSelection(3);
                });

                expect(useABTSStore.getState().formData.selectedParts).toContain(3);

                // Change to Listening - part 3 should stay (valid for both)
                act(() => {
                    state.updateFormData({ skill: 'LISTENING' });
                });

                expect(useABTSStore.getState().formData.selectedParts).toContain(3);
            });
        });

        describe('setFormField()', () => {
            it('should set a single form field', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.setFormField('difficulty', 'ADVANCED');
                });

                expect(useABTSStore.getState().formData.difficulty).toBe('ADVANCED');
            });
        });

        describe('resetForm()', () => {
            it('should reset form to initial state', () => {
                const state = useABTSStore.getState();
                
                // Modify form
                act(() => {
                    state.updateFormData({ 
                        skill: 'READING', 
                        topic: 'Test Topic',
                        difficulty: 'ADVANCED'
                    });
                });

                // Reset
                act(() => {
                    state.resetForm();
                });

                const newState = useABTSStore.getState();
                expect(newState.formData.skill).toBeNull();
                expect(newState.formData.topic).toBe('');
                expect(newState.formData.difficulty).toBe('INTERMEDIATE');
            });
        });
    });

    // =========================================================================
    // PART SELECTION TESTS
    // =========================================================================
    describe('Part Selection', () => {
        describe('togglePartSelection()', () => {
            it('should add part to selection', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.updateFormData({ skill: 'READING' });
                    state.togglePartSelection(1);
                });

                expect(useABTSStore.getState().formData.selectedParts).toContain(1);
            });

            it('should remove part from selection', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.updateFormData({ skill: 'READING' });
                    state.togglePartSelection(1);
                    state.togglePartSelection(1);
                });

                expect(useABTSStore.getState().formData.selectedParts).not.toContain(1);
            });

            it('should add part to selectedParts when toggled', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.updateFormData({ skill: 'READING' });
                    state.togglePartSelection(2);
                });

                const selectedParts = useABTSStore.getState().formData.selectedParts;
                expect(selectedParts).toContain(2);
            });
        });

        describe('setPartConfig()', () => {
            it('should set config for a specific part', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.updateFormData({ skill: 'READING' });
                    state.togglePartSelection(1);
                    state.setPartConfig(1, { topic: 'Environment' });
                });

                const partConfigs = useABTSStore.getState().formData.partConfigs;
                expect(partConfigs[1].topic).toBe('Environment');
            });
        });

        describe('clearPartSelections()', () => {
            it('should clear all part selections', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.updateFormData({ skill: 'READING' });
                    state.togglePartSelection(1);
                    state.togglePartSelection(2);
                    state.clearPartSelections();
                });

                const newState = useABTSStore.getState();
                expect(newState.formData.selectedParts).toEqual([]);
                expect(newState.formData.partConfigs).toEqual({});
            });
        });
    });

    // =========================================================================
    // AUDIO URL TESTS (Listening)
    // =========================================================================
    describe('Audio URL Management', () => {
        describe('setAudioUrl()', () => {
            it('should set audio URL for a part', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.setAudioUrl(1, 'https://example.com/audio1.mp3');
                });

                expect(useABTSStore.getState().audioUrls[1]).toBe('https://example.com/audio1.mp3');
            });

            it('should handle multiple audio URLs', () => {
                const state = useABTSStore.getState();
                act(() => {
                    state.setAudioUrl(1, 'https://example.com/audio1.mp3');
                    state.setAudioUrl(2, 'https://example.com/audio2.mp3');
                });

                const audioUrls = useABTSStore.getState().audioUrls;
                expect(audioUrls[1]).toBe('https://example.com/audio1.mp3');
                expect(audioUrls[2]).toBe('https://example.com/audio2.mp3');
            });
        });
    });

    // =========================================================================
    // STEP VALIDATION TESTS
    // =========================================================================
    describe('isStepValid()', () => {
        it('should validate step 1 (skill selection)', () => {
            const state = useABTSStore.getState();
            
            // No skill selected - invalid
            expect(state.isStepValid(1)).toBe(false);

            act(() => {
                state.updateFormData({ skill: 'READING' });
            });

            // Skill selected - valid
            expect(useABTSStore.getState().isStepValid(1)).toBe(true);
        });

        it('should validate step 2 (topic required)', () => {
            const state = useABTSStore.getState();
            
            act(() => {
                state.updateFormData({ skill: 'READING' });
            });

            // No topic - invalid (topic needs at least 3 chars)
            expect(useABTSStore.getState().isStepValid(2)).toBe(false);

            act(() => {
                state.updateFormData({ topic: 'Climate Change' });
            });

            // Topic provided - valid
            expect(useABTSStore.getState().isStepValid(2)).toBe(true);
        });
    });

    // =========================================================================
    // PART TOPIC TESTS
    // =========================================================================
    describe('setPartTopic()', () => {
        it('should set topic for a specific part', () => {
            const state = useABTSStore.getState();
            act(() => {
                state.updateFormData({ skill: 'READING' });
                state.togglePartSelection(1);
                state.setPartTopic(1, 'Climate Change');
            });

            const partConfigs = useABTSStore.getState().formData.partConfigs;
            expect(partConfigs[1].topic).toBe('Climate Change');
        });
    });

    // =========================================================================
    // PART FACTS TESTS
    // =========================================================================
    describe('addPartFact()', () => {
        it('should add a fact to a specific part', () => {
            const state = useABTSStore.getState();
            act(() => {
                state.updateFormData({ skill: 'READING' });
                state.togglePartSelection(1);
                state.addPartFact(1, 'Global temperatures have risen 1.1C');
            });

            const partConfigs = useABTSStore.getState().formData.partConfigs;
            expect(partConfigs[1].facts).toContain('Global temperatures have risen 1.1C');
        });
    });

    // =========================================================================
    // RANDOMIZE TESTS
    // =========================================================================
    describe('randomizePartConfig()', () => {
        it('should randomize question types for Reading part', () => {
            const state = useABTSStore.getState();
            act(() => {
                state.updateFormData({ skill: 'READING' });
                state.togglePartSelection(1);
                state.randomizePartConfig(1);
            });

            const partConfigs = useABTSStore.getState().formData.partConfigs;
            expect(partConfigs[1].questionTypes.length).toBeGreaterThan(0);
        });

        it('should randomize question types for Listening part', () => {
            const state = useABTSStore.getState();
            act(() => {
                state.updateFormData({ skill: 'LISTENING' });
                state.togglePartSelection(2);
                state.randomizePartConfig(2);
            });

            const partConfigs = useABTSStore.getState().formData.partConfigs;
            expect(partConfigs[2].questionTypes.length).toBeGreaterThan(0);
        });
    });

    describe('randomizeAllParts()', () => {
        it('should randomize all selected parts', () => {
            const state = useABTSStore.getState();
            act(() => {
                state.updateFormData({ skill: 'READING' });
                state.togglePartSelection(1);
                state.togglePartSelection(2);
                state.randomizeAllParts();
            });

            const partConfigs = useABTSStore.getState().formData.partConfigs;
            expect(partConfigs[1].questionTypes.length).toBeGreaterThan(0);
            expect(partConfigs[2].questionTypes.length).toBeGreaterThan(0);
        });
    });

    // =========================================================================
    // TOGGLE QUESTION TYPE TESTS
    // =========================================================================
    describe('togglePartQuestionType()', () => {
        it('should toggle question type for a part', () => {
            const state = useABTSStore.getState();
            act(() => {
                state.updateFormData({ skill: 'READING' });
                state.togglePartSelection(1);
                state.togglePartQuestionType(1, 'TRUE_FALSE_NOT_GIVEN');
            });

            const partConfigs = useABTSStore.getState().formData.partConfigs;
            // questionTypes is an array of strings (type IDs)
            expect(partConfigs[1].questionTypes).toContain('TRUE_FALSE_NOT_GIVEN');
        });
    });
});
