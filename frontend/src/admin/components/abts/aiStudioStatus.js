const STEP_IDS = ['configure', 'generate', 'review', 'save'];

export const AI_STUDIO_STEPS = [
    { id: 'configure', label: 'Configure' },
    { id: 'generate', label: 'Generate' },
    { id: 'review', label: 'Review' },
    { id: 'save', label: 'Save' },
];

const issueGroups = [
    { key: 'schemaErrors', idPrefix: 'schema', label: 'Schema errors', severity: 'error' },
    { key: 'contentErrors', idPrefix: 'content', label: 'Content errors', severity: 'error' },
    { key: 'businessRuleErrors', idPrefix: 'business', label: 'Business rule errors', severity: 'error' },
];

function formatIssueMessage(issue) {
    if (typeof issue === 'string') return issue;
    if (issue?.message) return issue.message;
    if (issue?.error) return issue.error;
    if (issue == null) return '';
    return JSON.stringify(issue);
}

function toIssueList(items, { idPrefix, label, severity }) {
    if (!Array.isArray(items)) return [];

    return items
        .map((item, index) => ({
            id: item?.id || `${idPrefix}-${index}`,
            message: formatIssueMessage(item),
            label,
            severity,
            sourceIndex: index,
            original: item,
        }))
        .filter(issue => issue.message);
}

export function getAIStudioValidationBuckets(generationResult) {
    const validation = generationResult?.validation || {};
    const buckets = issueGroups.reduce((acc, group) => {
        acc[group.key] = toIssueList(validation[group.key], group);
        return acc;
    }, {});

    buckets.warnings = toIssueList(generationResult?.warnings, {
        idPrefix: 'warn',
        label: 'Warnings',
        severity: 'warning',
    }).map((issue) => ({ ...issue, id: `warn-${issue.sourceIndex}`, type: 'WARNING' }));

    return buckets;
}

export function getAIStudioIssues(generationResult) {
    const buckets = getAIStudioValidationBuckets(generationResult);
    return [
        ...buckets.schemaErrors,
        ...buckets.contentErrors,
        ...buckets.businessRuleErrors,
        ...buckets.warnings,
    ];
}

export function getAIStudioIssueCounts(generationResult) {
    const buckets = getAIStudioValidationBuckets(generationResult);
    const errorCount = buckets.schemaErrors.length + buckets.contentErrors.length + buckets.businessRuleErrors.length;
    const warningCount = buckets.warnings.length;

    return {
        errorCount,
        warningCount,
        total: errorCount + warningCount,
    };
}

export function getAIStudioConfigReadiness(formData = {}) {
    const issues = [];

    if (!formData.skill) {
        issues.push('Select a skill');
    }

    if (formData.skill === 'WRITING') {
        const writingTasks = (formData.questionTypes || []).filter(type => type === 'TASK_1' || type === 'TASK_2');

        if (writingTasks.length === 0) {
            issues.push('Select at least one Writing task');
        }

        if (!formData.topic || formData.topic.length < 3) {
            issues.push('Writing topic needs at least 3 characters');
        }

        if (formData.generationMode === 'CUSTOM_FACTS' && (!formData.facts || formData.facts.length < 3)) {
            issues.push('Writing: add at least 3 facts');
        }

        return {
            canGenerate: issues.length === 0,
            issues,
        };
    }

    if (!formData.selectedParts?.length) {
        issues.push('Select at least one part');
    }

    (formData.selectedParts || []).forEach((partNumber) => {
        const partConfig = formData.partConfigs?.[partNumber] || {};

        if (!partConfig.topic || partConfig.topic.length < 3) {
            issues.push(`Part ${partNumber}: topic needs at least 3 characters`);
        }

        if (!partConfig.questionTypes || partConfig.questionTypes.length < 2) {
            issues.push(`Part ${partNumber}: select at least 2 question types`);
        }

        if (formData.generationMode === 'CUSTOM_FACTS' && (!partConfig.facts || partConfig.facts.length < 3)) {
            issues.push(`Part ${partNumber}: add at least 3 facts`);
        }
    });

    return {
        canGenerate: issues.length === 0,
        issues,
    };
}

export function getAIStudioStepState({ view, isGenerating, generationResult, isSaving, isSaveModalOpen, canGenerate }) {
    const hasResult = Boolean(generationResult?.content);
    const activeStep = isSaving || isSaveModalOpen
        ? 'save'
        : isGenerating
            ? 'generate'
            : hasResult && view === 'preview'
                ? 'review'
                : 'configure';

    const completedSteps = new Set();
    if (canGenerate || isGenerating || hasResult) completedSteps.add('configure');
    if (hasResult) completedSteps.add('generate');
    if (isSaving || isSaveModalOpen) completedSteps.add('review');

    return AI_STUDIO_STEPS.map((step) => ({
        ...step,
        isActive: step.id === activeStep,
        isComplete: completedSteps.has(step.id),
        isPending: STEP_IDS.indexOf(step.id) > STEP_IDS.indexOf(activeStep) && !completedSteps.has(step.id),
    }));
}

function formatSkill(skill) {
    if (!skill) return 'Skill pending';
    const value = String(skill).toLowerCase();
    return value.charAt(0).toUpperCase() + value.slice(1);
}

export function getAIStudioSaveTargetSummary({ formData = {}, generationResult } = {}) {
    const content = generationResult?.content;
    const selectedParts = Array.isArray(formData.selectedParts) ? formData.selectedParts : [];
    const contentPart = content?.section?.partNumber || content?.section?.part_number;
    const partsLabel = selectedParts.length > 0
        ? selectedParts.map(part => `Part ${part}`).join(', ')
        : `Part ${formData.partNumber || contentPart || 1}`;
    const questionCount = Array.isArray(content?.questions) ? content.questions.length : 0;

    return {
        examSource: 'AI-GEN',
        skillLabel: formatSkill(formData.skill || content?.skill),
        partsLabel,
        questionCount,
        targetLabel: content ? 'Choose set/test in save dialog' : 'Available after review',
        isReadyToSave: Boolean(content),
    };
}