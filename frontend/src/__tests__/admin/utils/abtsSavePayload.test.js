import { describe, expect, it } from 'vitest';
import { buildABTSSaveRequest, buildPartsToSave } from '../../../admin/utils/abtsSavePayload';

describe('abtsSavePayload', () => {
    it('builds reading partsToSave using IELTS question ranges', () => {
        const content = {
            metadata: { model: 'deepseek' },
            sections: [
                { partNumber: 1, passageText: 'Part 1' },
                { partNumber: 2, passageText: 'Part 2' },
            ],
            questions: [
                { questionNumber: 1 },
                { questionNumber: 13 },
                { questionNumber: 14 },
                { questionNumber: 26 },
                { questionNumber: 27 },
            ],
        };

        const parts = buildPartsToSave(content, 'reading');

        expect(parts).toHaveLength(2);
        expect(parts[0].content.questions.map(q => q.questionNumber)).toEqual([1, 13]);
        expect(parts[1].content.questions.map(q => q.questionNumber)).toEqual([14, 26]);
        expect(parts[0].content.metadata).toEqual({ model: 'deepseek' });
    });

    it('filters partsToSave to selected parts', () => {
        const content = {
            sections: [
                { partNumber: 1, passageText: 'Part 1' },
                { partNumber: 2, passageText: 'Part 2' },
                { partNumber: 3, passageText: 'Part 3' },
            ],
            questions: [
                { questionNumber: 1 },
                { questionNumber: 14 },
                { questionNumber: 27 },
            ],
        };

        const parts = buildPartsToSave(content, 'reading', [2]);

        expect(parts).toHaveLength(1);
        expect(parts[0].partNumber).toBe(2);
        expect(parts[0].content.questions.map(q => q.questionNumber)).toEqual([14]);
    });

    it('builds listening partsToSave using IELTS question ranges', () => {
        const content = {
            sections: [
                { partNumber: 3, transcript: 'Part 3' },
                { partNumber: 4, transcript: 'Part 4' },
            ],
            questions: [
                { questionNumber: 20 },
                { questionNumber: 21 },
                { questionNumber: 30 },
                { questionNumber: 31 },
                { questionNumber: 40 },
            ],
        };

        const parts = buildPartsToSave(content, 'LISTENING');

        expect(parts[0].content.questions.map(q => q.questionNumber)).toEqual([21, 30]);
        expect(parts[1].content.questions.map(q => q.questionNumber)).toEqual([31, 40]);
    });

    it('builds save request with generationConfig from form data', () => {
        const request = buildABTSSaveRequest({
            content: {
                metadata: { topic: 'Fallback topic' },
                section: { partNumber: 2 },
                questions: [],
            },
            formData: {
                skill: 'READING',
                topic: 'Transport',
                facts: ['Fact A'],
                difficulty: 'ADVANCED',
                testType: 'ACADEMIC',
                questionTypes: ['MATCHING_INFORMATION'],
                questionTypeCounts: { MATCHING_INFORMATION: 6 },
                model: 'deepseek-chat',
                temperature: 0.7,
                selectedParts: [1, 2],
                scope: 'MULTI_PART',
                partNumber: 1,
                partConfigs: { 1: { topic: 'Part 1' } },
            },
            saveConfig: {
                setId: 10,
                existingTestId: 20,
                setNameVi: 'AI Set',
                testNameVi: 'AI Test',
                hashtagIds: [1, 2],
            },
        });

        expect(request).toMatchObject({
            examSource: 'AI-GEN',
            skill: 'reading',
            topic: 'Transport',
            difficulty: 'ADVANCED',
            setId: 10,
            testId: 20,
            setName: 'AI Set',
            testName: 'AI Test',
            hashtagIds: [1, 2],
        });
        expect(request.generationConfig).toMatchObject({
            topic: 'Transport',
            facts: ['Fact A'],
            model: 'deepseek-chat',
            questionTypeCounts: { MATCHING_INFORMATION: 6 },
            partsToGenerate: [1, 2],
            scope: 'MULTI_PART',
            partNumber: 1,
        });
    });
});