import { describe, expect, it } from 'vitest';
import { buildABTSGenerationRequest } from '../../../admin/utils/abtsGenerationPayload';

describe('abtsGenerationPayload', () => {
    it('flattens a single selected part into SINGLE_PART request using part config', () => {
        const request = buildABTSGenerationRequest({
            skill: 'READING',
            scope: 'MULTI_PART',
            partNumber: 1,
            selectedParts: [2],
            topic: 'Global topic',
            facts: ['global fact'],
            difficulty: 'INTERMEDIATE',
            explanationLanguage: 'VI',
            testType: 'ACADEMIC',
            questionTypes: [],
            questionTypeCounts: {},
            totalQuestions: 13,
            partConfigs: {
                2: {
                    topic: 'Part 2 topic',
                    facts: ['part fact'],
                    questionTypes: ['MATCHING_HEADINGS'],
                    questionTypeCounts: { MATCHING_HEADINGS: 7 },
                    totalQuestions: 7,
                },
            },
        });

        expect(request).toMatchObject({
            scope: 'SINGLE_PART',
            partNumber: 2,
            topic: 'Part 2 topic',
            facts: ['part fact'],
            questionTypes: ['MATCHING_HEADINGS'],
            questionTypeCounts: { MATCHING_HEADINGS: 7 },
            totalQuestions: 7,
            partsToGenerate: null,
            partConfigs: null,
        });
    });

    it('keeps multi selected parts in MULTI_PART request', () => {
        const request = buildABTSGenerationRequest({
            skill: 'LISTENING',
            scope: 'MULTI_PART',
            selectedParts: [1, 3],
            topic: 'Listening topic',
            difficulty: 'INTERMEDIATE',
            explanationLanguage: 'VI',
            testType: 'ACADEMIC',
            questionTypes: [],
            questionTypeCounts: {},
            partConfigs: {
                1: { topic: 'Part 1' },
                3: { topic: 'Part 3' },
            },
        });

        expect(request.scope).toBe('MULTI_PART');
        expect(request.partsToGenerate).toEqual([1, 3]);
        expect(request.partConfigs).toEqual({
            1: { topic: 'Part 1' },
            3: { topic: 'Part 3' },
        });
    });
});