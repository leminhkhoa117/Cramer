import { describe, expect, it } from 'vitest';
import {
    getAIStudioConfigReadiness,
    getAIStudioIssueCounts,
    getAIStudioStepState,
    getAIStudioValidationBuckets,
} from '../../../admin/components/abts/aiStudioStatus';

describe('aiStudioStatus', () => {
    it('builds validation buckets from the backend ValidationView shape', () => {
        const buckets = getAIStudioValidationBuckets({
            validation: {
                valid: false,
                issues: [
                    { id: 'rd-passage-missing', severity: 'ERROR', path: '/section/passage_text', message: 'Reading section.passage_text is required' },
                    { id: 'rd-passage-short', severity: 'WARNING', path: '/section/passage_text', message: 'Passage is shorter than 700 words' },
                ],
                errors: ['Reading section.passage_text is required'],
                warnings: ['Passage is shorter than 700 words'],
                errorCount: 1,
                warningCount: 1,
            },
        });

        expect(buckets.contentErrors[0]).toMatchObject({
            id: 'rd-passage-missing',
            message: 'Reading section.passage_text is required',
            severity: 'error',
        });
        expect(buckets.warnings[0]).toMatchObject({
            id: 'rd-passage-short',
            message: 'Passage is shorter than 700 words',
            severity: 'warning',
        });
    });

    it('counts validation errors and warnings separately', () => {
        const counts = getAIStudioIssueCounts({
            validation: {
                issues: [
                    { id: 'a', severity: 'ERROR', message: 'A' },
                    { id: 'b', severity: 'ERROR', message: 'B' },
                    { id: 'd', severity: 'WARNING', message: 'D' },
                    { id: 'e', severity: 'WARNING', message: 'E' },
                ],
            },
        });

        expect(counts).toEqual({ errorCount: 2, warningCount: 2, total: 4 });
    });

    it('keeps config readiness aligned with multi-part generate requirements', () => {
        const ready = getAIStudioConfigReadiness({
            skill: 'READING',
            generationMode: 'AUTO',
            selectedParts: [1],
            partConfigs: {
                1: {
                    topic: 'Climate change',
                    questionTypes: ['FILL_IN_BLANK', 'TRUE_FALSE_NOT_GIVEN'],
                },
            },
        });

        expect(ready.canGenerate).toBe(true);
        expect(ready.issues).toEqual([]);

        const blocked = getAIStudioConfigReadiness({
            skill: 'READING',
            generationMode: 'CUSTOM_FACTS',
            selectedParts: [1],
            partConfigs: {
                1: {
                    topic: 'AI',
                    questionTypes: ['FILL_IN_BLANK'],
                    facts: ['one'],
                },
            },
        });

        expect(blocked.canGenerate).toBe(false);
        expect(blocked.issues).toContain('Part 1: topic needs at least 3 characters');
        expect(blocked.issues).toContain('Part 1: select at least 2 question types');
        expect(blocked.issues).toContain('Part 1: add at least 3 facts');
    });

    it('sets active and completed steps for review and save states', () => {
        const reviewSteps = getAIStudioStepState({
            view: 'preview',
            isGenerating: false,
            generationResult: { content: { questions: [] } },
            isSaving: false,
            isSaveModalOpen: false,
            canGenerate: true,
        });

        expect(reviewSteps.find(step => step.id === 'review').isActive).toBe(true);
        expect(reviewSteps.find(step => step.id === 'configure').isComplete).toBe(true);
        expect(reviewSteps.find(step => step.id === 'generate').isComplete).toBe(true);

        const saveSteps = getAIStudioStepState({
            view: 'preview',
            isGenerating: false,
            generationResult: { content: { questions: [] } },
            isSaving: true,
            isSaveModalOpen: false,
            canGenerate: true,
        });

        expect(saveSteps.find(step => step.id === 'save').isActive).toBe(true);
        expect(saveSteps.find(step => step.id === 'review').isComplete).toBe(true);
    });
});