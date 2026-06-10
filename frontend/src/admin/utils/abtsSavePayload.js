const QUESTION_RANGES = {
    READING: {
        1: [1, 13],
        2: [14, 26],
        3: [27, 40],
    },
    LISTENING: {
        1: [1, 10],
        2: [11, 20],
        3: [21, 30],
        4: [31, 40],
    },
};

const getQuestionNumber = (question) => question?.questionNumber ?? question?.question_number;

export function buildPartsToSave(content, skill, selectedParts) {
    if (!content?.sections?.length) return null;

    const skillUpper = String(skill || '').toUpperCase();
    const questions = Array.isArray(content.questions) ? content.questions : [];
    const selectedPartSet = Array.isArray(selectedParts) && selectedParts.length > 0
        ? new Set(selectedParts)
        : null;
    const { sections, questions: allQuestions, ...sharedContent } = content;

    return sections
        .filter((section) => {
            if (!selectedPartSet) return true;
            const partNumber = section.partNumber ?? section.part_number;
            return selectedPartSet.has(partNumber);
        })
        .map((section) => {
        const partNumber = section.partNumber ?? section.part_number;
        const range = QUESTION_RANGES[skillUpper]?.[partNumber];
        const filteredQuestions = range
            ? questions.filter((question) => {
                const questionNumber = getQuestionNumber(question);
                return questionNumber >= range[0] && questionNumber <= range[1];
            })
            : questions;

        return {
            partNumber,
            content: {
                ...sharedContent,
                section,
                questions: filteredQuestions,
            },
        };
    });
}

export function buildGenerationConfig(formData = {}) {
    const selectedParts = formData.selectedParts?.length ? formData.selectedParts : formData.partsToGenerate;

    return {
        scope: formData.scope,
        partNumber: formData.partNumber,
        topic: formData.topic,
        facts: formData.facts,
        difficulty: formData.difficulty,
        testType: formData.testType,
        questionTypes: formData.questionTypes,
        questionTypeCounts: formData.questionTypeCounts,
        model: formData.model,
        temperature: formData.temperature,
        writingEssayType: formData.writingEssayType || null,
        partsToGenerate: selectedParts,
        partConfigs: formData.partConfigs,
    };
}

export function buildABTSSaveRequest({
    content,
    formData = {},
    saveConfig = {},
    selectedSetId,
    selectedSetCode,
    selectedTestId,
} = {}) {
    if (!content) {
        throw new Error('No generated content to save');
    }

    const skill = formData.skill || content.skill || 'reading';
    const skillLower = String(skill).toLowerCase();
    const topic = formData.topic || content.metadata?.topic || 'AI Generated';

    return {
        examSource: saveConfig.examSource || 'AI-GEN',
        testNumber: saveConfig.testNumber || null,
        skill: skillLower,
        partNumber: formData.partNumber || content.section?.partNumber || 1,
        topic,
        difficulty: saveConfig.difficulty || formData.difficulty || 'INTERMEDIATE',
        content,
        setId: saveConfig.setId ?? selectedSetId,
        setCode: saveConfig.setCode ?? selectedSetCode ?? 'ai_generated',
        setName: saveConfig.setName || saveConfig.setNameVi || null,
        testId: saveConfig.existingTestId || saveConfig.testId || selectedTestId,
        testName: saveConfig.testName || saveConfig.testNameVi || null,
        hashtagCodes: saveConfig.hashtagCodes || formData.hashtags || [],
        hashtagIds: saveConfig.hashtagIds,
        generationConfig: buildGenerationConfig(formData),
        partsToSave: buildPartsToSave(content, skillLower, formData.selectedParts),
    };
}