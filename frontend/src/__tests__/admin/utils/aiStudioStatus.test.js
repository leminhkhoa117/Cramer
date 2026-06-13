import { describe, expect, it } from 'vitest';
import {
    getAIStudioConfigReadiness,
    getAIStudioIssueCounts,
    getAIStudioStepState,
    getAIStudioValidationBuckets,
} from '../../../admin/components/abts/aiStudioStatus';

describe('aiStudioStatus', () => {
    it('builds validation buckets from current generation result data', () => {
        const buckets = getAIStudioValidationBuckets({
            validation: {
                schemaErrors: ['Missing section'],
                contentErrors: [{ message: 'Question 2 has no answer' }],
                businessRuleErrors: [],
            },
            warnings: [{ id: 'backend-warning-1', message: 'Low word count' }],
        });

        expect(buckets.schemaErrors[0]).toMatchObject({ id: 'schema-0', message: 'Missing section', severity: 'error' });
        expect(buckets.contentErrors[0]).toMatchObject({ id: 'content-0', message: 'Question 2 has no answer' });
        expect(buckets.warnings[0]).toMatchObject({ id: 'warn-0', message: 'Low word count', type: 'WARNING' });
    });

    it('counts validation errors and warnings separately', () => {
        const counts = getAIStudioIssueCounts({
            validation: {
                schemaErrors: ['A'],
                contentErrors: ['B'],
                businessRuleErrors: ['C'],
            },
            warnings: ['D', 'E'],
        });

        expect(counts).toEqual({ errorCount: 3, warningCount: 2, total: 5 });
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