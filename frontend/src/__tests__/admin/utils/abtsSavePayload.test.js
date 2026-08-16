import { describe, expect, it, vi } from 'vitest';
import { buildABTSSaveRequest, buildPartsToSave } from '../../../admin/utils/abtsSavePayload';

vi.mock('../../../admin/stores/useHashtagStore', () => ({
    default: {
        getState: () => ({
            hashtags: [
                { id: 1, code: 'ielts' },
                { id: 2, code: 'academic' },
            ],
        }),
    },
}));

describe('abtsSavePayload', () => {
    it('builds reading sections from the backend multi-part shape', () => {
        const content = {
            sections: [
                {
                    part: 1,
                    section: { passage_text: 'Part 1' },
                    questions: [{ question_number: 1 }, { question_number: 13 }],
                },
                {
                    part: 2,
                    section: { passage_text: 'Part 2' },
                    questions: [{ question_number: 14 }, { question_number: 26 }],
                },
            ],
        };

        const parts = buildPartsToSave(content, 'reading');

        expect(parts).toHaveLength(2);
        expect(parts[0]).toMatchObject({
            skill: 'reading',
            partNumber: 1,
            passageText: 'Part 1',
        });
        expect(parts[0].questions.map(q => q.question_number)).toEqual([1, 13]);
        expect(parts[1].questions.map(q => q.question_number)).toEqual([14, 26]);
    });

    it('builds a single-part reading section from the backend single-part shape', () => {
        const content = {
            section: { passage_text: 'Single passage' },
            questions: [{ question_number: 1 }],
        };

        const parts = buildPartsToSave(content, 'READING');

        expect(parts).toHaveLength(1);
        expect(parts[0].partNumber).toBe(1);
        expect(parts[0].passageText).toBe('Single passage');
    });

    it('builds listening sections with transcript + section_layout + audio urls', () => {
        const content = {
            sections: [
                {
                    part: 3,
                    transcript: 'Part 3 transcript',
                    section_layout: { blocks: [] },
                    questions: [{ question_number: 21 }],
                },
                {
                    part: 4,
                    transcript: 'Part 4 transcript',
                    section_layout: { blocks: [] },
                    questions: [{ question_number: 31 }],
                },
            ],
        };

        const parts = buildPartsToSave(content, 'LISTENING', {
            audioUrls: { 3: 'https://audio/part3.mp3' },
        });

        expect(parts[0].passageText).toBe('Part 3 transcript');
        expect(parts[0].audioUrl).toBe('https://audio/part3.mp3');
        expect(parts[0].sectionLayout).toEqual({ blocks: [] });
        expect(parts[1].audioUrl).toBeNull();
    });

    it('builds a writing section with section_layout metadata', () => {
        const content = {
            task_prompt: 'Write about charts',
            task_type: 'ACADEMIC_TASK_1',
            word_requirement: 150,
            chart_data: { type: 'pie' },
            sample_answer: 'Model answer',
        };

        const parts = buildPartsToSave(content, 'writing');

        expect(parts).toHaveLength(1);
        expect(parts[0].passageText).toBe('Write about charts');
        expect(parts[0].questions).toEqual([]);
        expect(parts[0].sectionLayout).toMatchObject({
            task_type: 'ACADEMIC_TASK_1',
            word_requirement: 150,
            chart_data: { type: 'pie' },
            sample_answer: 'Model answer',
        });
    });

    it('builds the backend save request shape with generation metadata + hashtag codes', () => {
        const request = buildABTSSaveRequest({
            content: {
                section: { passage_text: 'Passage' },
                questions: [{ question_number: 1 }],
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
                selectedParts: [1],
                scope: 'MULTI_PART',
                partNumber: 1,
                partConfigs: { 1: { topic: 'Part 1' } },
            },
            saveConfig: {
                setId: 10,
                existingTestId: 20,
                testName: 'AI Test',
                difficulty: 'ADVANCED',
                hashtagIds: [1, 2],
            },
        });

        expect(request).toMatchObject({
            setId: 10,
            testId: 20,
            testName: 'AI Test',
            difficulty: 'ADVANCED',
            hashtags: ['ielts', 'academic'],
        });
        expect(request.setCode).toBe('ai_generated');
        expect(request.sections).toHaveLength(1);
        expect(request.sections[0].skill).toBe('reading');
        expect(request.generationMetadata).toMatchObject({
            topic: 'Transport',
            facts: ['Fact A'],
            model: 'deepseek-chat',
            questionTypeCounts: { MATCHING_INFORMATION: 6 },
        });
    });
});
