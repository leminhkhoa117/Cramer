import { describe, expect, it } from 'vitest';
import { buildABTSGenerationRequest } from '../../../admin/utils/abtsGenerationPayload';

describe('abtsGenerationPayload', () => {
    it('maps a single selected part into the backend parts map using its part config', () => {
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
            generationMode: 'AUTO',
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

        expect(request.partsToGenerate).toEqual([2]);
        expect(request.parts).toMatchObject({
            '2': {
                topic: 'Part 2 topic',
                facts: ['part fact'],
                factsMode: 'AUTO',
                questionTypes: ['MATCHING_HEADINGS'],
                questionTypeCounts: { MATCHING_HEADINGS: 7 },
                totalQuestions: 7,
                difficulty: 'INTERMEDIATE',
                taskType: null,
            },
        });
        expect(request.explanationLanguage).toBe('vi');
        expect(request.model).toMatchObject({
            model: null,
            temperature: null,
        });
    });

    it('keeps multi selected parts in the backend parts map', () => {
        const request = buildABTSGenerationRequest({
            skill: 'LISTENING',
            scope: 'MULTI_PART',
            selectedParts: [1, 3],
            topic: 'Listening topic',
            difficulty: 'INTERMEDIATE',
            explanationLanguage: 'EN',
            testType: 'ACADEMIC',
            questionTypes: [],
            questionTypeCounts: {},
            generationMode: 'AUTO',
            partConfigs: {
                1: { topic: 'Part 1' },
                3: { topic: 'Part 3' },
            },
        });

        expect(request.partsToGenerate).toEqual([1, 3]);
        expect(request.parts['1'].topic).toBe('Part 1');
        expect(request.parts['3'].topic).toBe('Part 3');
        expect(request.explanationLanguage).toBe('en');
    });

    it('maps writing tasks to TASK_2/ACADEMIC_TASK_1 task types', () => {
        const request = buildABTSGenerationRequest({
            skill: 'WRITING',
            questionTypes: ['TASK_1', 'TASK_2'],
            testType: 'ACADEMIC',
            topic: 'Cities',
            generationMode: 'AUTO',
            partConfigs: {},
        });

        expect(request.partsToGenerate).toEqual([1, 2]);
        expect(request.parts['1'].taskType).toBe('ACADEMIC_TASK_1');
        expect(request.parts['2'].taskType).toBe('TASK_2');
        expect(request.parts['1'].questionTypes).toBeNull();
    });
});
