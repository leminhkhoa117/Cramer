const hasItems = (value) => Array.isArray(value) && value.length > 0;
const hasKeys = (value) => value && Object.keys(value).length > 0;

const resolveWritingPartNumber = (formData) => {
    const types = formData.questionTypes || [];
    const hasTask1 = types.includes('TASK_1');
    const hasTask2 = types.includes('TASK_2');
    if (hasTask2 && !hasTask1) return 2;
    if (hasTask1 && !hasTask2) return 1;
    return formData.partNumber;
};

export function buildABTSGenerationRequest(formData = {}) {
    const selectedParts = hasItems(formData.selectedParts) ? formData.selectedParts : [];
    const isSingleSelectedPart = selectedParts.length === 1;
    const partNumber = isSingleSelectedPart
        ? selectedParts[0]
        : formData.skill === 'WRITING'
            ? resolveWritingPartNumber(formData)
            : formData.partNumber;
    const partConfig = isSingleSelectedPart ? formData.partConfigs?.[partNumber] || {} : {};

    const questionTypes = partConfig.questionTypes ?? formData.questionTypes ?? [];
    const questionTypeCounts = partConfig.questionTypeCounts ?? formData.questionTypeCounts ?? {};
    const totalQuestions = partConfig.totalQuestions ?? formData.totalQuestions;
    const hasCustomCounts = hasKeys(questionTypeCounts);
    const shouldSendTotalQuestions = hasCustomCounts
        || questionTypes.length > 0
        || (typeof totalQuestions === 'number' && totalQuestions !== 13);

    return {
        skill: formData.skill,
        scope: isSingleSelectedPart ? 'SINGLE_PART' : formData.scope,
        partNumber,
        topic: partConfig.topic ?? formData.topic,
        hashtags: partConfig.hashtags ?? formData.hashtags,
        facts: partConfig.facts ?? formData.facts,
        difficulty: partConfig.difficulty ?? formData.difficulty,
        explanationLanguage: formData.explanationLanguage,
        testType: formData.testType,
        questionTypes: questionTypes.length > 0 ? questionTypes : null,
        model: formData.model,
        enableReasoning: formData.enableReasoning,
        reasoningEffort: formData.reasoningEffort,
        reasoningBudget: formData.reasoningBudget ?? null,
        temperature: formData.temperature,
        questionTypeCounts: hasCustomCounts ? questionTypeCounts : null,
        passageLength: partConfig.passageLength ?? formData.passageLength,
        customInstructions: (partConfig.customInstructions ?? formData.customInstructions) || null,
        maxTokens: formData.maxTokens,
        totalQuestions: shouldSendTotalQuestions ? totalQuestions : null,
        writingEssayType: formData.writingEssayType || null,
        partsToGenerate: selectedParts.length > 1 ? selectedParts : null,
        partConfigs: selectedParts.length > 1 && hasKeys(formData.partConfigs) ? formData.partConfigs : null,
    };
}