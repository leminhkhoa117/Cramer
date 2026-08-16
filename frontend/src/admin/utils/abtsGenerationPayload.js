const hasItems = (value) => Array.isArray(value) && value.length > 0;
const hasKeys = (value) => value && Object.keys(value).length > 0;

const resolveWritingParts = (formData) => {
    const types = formData.questionTypes || [];
    const hasTask1 = types.includes('TASK_1');
    const hasTask2 = types.includes('TASK_2');
    const parts = [];
    if (hasTask1) parts.push(1);
    if (hasTask2) parts.push(2);
    return parts.length > 0 ? parts : [1];
};

const resolveSelectedParts = (formData) => {
    if (hasItems(formData.selectedParts)) return formData.selectedParts;
    if (formData.skill === 'WRITING') return resolveWritingParts(formData);
    return [formData.partNumber || 1];
};

const writingTaskType = (formData, part) => {
    if (part === 2) return 'TASK_2';
    return formData.testType === 'GENERAL_TRAINING' ? 'GENERAL_TASK_1' : 'ACADEMIC_TASK_1';
};

const toModelConfig = (formData = {}) => ({
    model: formData.model || null,
    temperature: formData.temperature ?? null,
    maxTokens: formData.maxTokens || null,
    enableReasoning: formData.enableReasoning ?? null,
    reasoningEffort: formData.reasoningEffort || null,
    reasoningBudget: formData.reasoningBudget ?? null,
    contextCache: null,
});

/**
 * Build the backend GenerationRequest (SPEC-21 §1):
 * { partsToGenerate, parts: {part: PartConfig}, model: ModelConfig,
 *   explanationLanguage, customInstructions, existingPassageText }
 */
export function buildABTSGenerationRequest(formData = {}) {
    const skill = formData.skill;
    const parts = resolveSelectedParts(formData);
    const partConfigs = formData.partConfigs || {};
    const isWriting = skill === 'WRITING';

    const partsMap = {};
    parts.forEach((part) => {
        const cfg = partConfigs[part] || {};
        partsMap[String(part)] = {
            topic: cfg.topic ?? formData.topic ?? null,
            factsMode: formData.generationMode === 'CUSTOM_FACTS' ? 'STRICT' : 'AUTO',
            facts: cfg.facts ?? formData.facts ?? [],
            questionTypes: isWriting ? null : (hasItems(cfg.questionTypes) ? cfg.questionTypes : hasItems(formData.questionTypes) ? formData.questionTypes : null),
            questionTypeCounts: isWriting ? null : (hasKeys(cfg.questionTypeCounts) ? cfg.questionTypeCounts : hasKeys(formData.questionTypeCounts) ? formData.questionTypeCounts : null),
            totalQuestions: isWriting ? null : (cfg.totalQuestions ?? formData.totalQuestions ?? null),
            passageLength: cfg.passageLength ?? formData.passageLength ?? null,
            difficulty: cfg.difficulty ?? formData.difficulty ?? null,
            taskType: isWriting ? writingTaskType(formData, part) : null,
        };
    });

    return {
        partsToGenerate: parts,
        parts: partsMap,
        model: toModelConfig(formData),
        explanationLanguage: String(formData.explanationLanguage || 'en').toLowerCase(),
        customInstructions: formData.customInstructions || null,
        existingPassageText: formData.existingPassageText || null,
    };
}
