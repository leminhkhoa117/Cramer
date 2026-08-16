const STEP_IDS = ['configure', 'generate', 'review', 'save'];

export const AI_STUDIO_STEPS = [
    { id: 'configure', label: 'Configure' },
    { id: 'generate', label: 'Generate' },
    { id: 'review', label: 'Review' },
    { id: 'save', label: 'Save' },
];

function formatIssueMessage(issue) {
    if (typeof issue === 'string') return issue;
    if (issue?.message) return issue.message;
    if (issue?.error) return issue.error;
    if (issue == null) return '';
    return JSON.stringify(issue);
}

/**
 * Bucket the backend ValidationView (SPEC-23 §1.1) into the rail's display
 * groups. Backend shape: { valid, issues:[{id,severity,path,message}],
 * errors[], warnings[], errorCount, warningCount }.
 */
function toIssueList(items, { idPrefix, label, severity }) {
    if (!Array.isArray(items)) return [];

    return items
        .map((item, index) => ({
            id: item?.id || `${idPrefix}-${index}`,
            message: formatIssueMessage(item),
            path: item?.path ?? null,
            label,
            severity,
            sourceIndex: index,
            original: item,
        }))
        .filter(issue => issue.message);
}

export function getAIStudioValidationBuckets(generationResult) {
    const validation = generationResult?.validation || {};
    const issues = Array.isArray(validation.issues) ? validation.issues : [];

    const errorIssues = issues.filter((issue) => String(issue?.severity).toUpperCase() !== 'WARNING');
    const warningIssues = issues.filter((issue) => String(issue?.severity).toUpperCase() === 'WARNING');

    const buckets = {};
    buckets.contentErrors = toIssueList(errorIssues, {
        idPrefix: 'error',
        label: 'Content errors',
        severity: 'error',
    });
    buckets.warnings = toIssueList(warningIssues, {
        idPrefix: 'warn',
        label: 'Warnings',
        severity: 'warning',
    });

    return buckets;
}

export function getAIStudioIssues(generationResult) {
    const buckets = getAIStudioValidationBuckets(generationResult);
    return [
        ...buckets.contentErrors,
        ...buckets.warnings,
    ];
}

export function getAIStudioIssueCounts(generationResult) {
    const buckets = getAIStudioValidationBuckets(generationResult);
    const errorCount = buckets.contentErrors.length;
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
    const rawSections = Array.isArray(content?.sections) ? content.sections : [content];
    const contentPart = rawSections[0]?.part ?? content?.section?.part ?? content?.part;
    const partsLabel = selectedParts.length > 0
        ? selectedParts.map(part => `Part ${part}`).join(', ')
        : `Part ${formData.partNumber || contentPart || 1}`;
    const questionCount = Array.isArray(content?.questions)
        ? content.questions.length
        : Array.isArray(content?.sections)
            ? content.sections.reduce((sum, sec) => sum + (sec.questions?.length || 0), 0)
            : 0;

    return {
        examSource: 'AI-GEN',
        skillLabel: formatSkill(formData.skill || content?.skill),
        partsLabel,
        questionCount,
        targetLabel: content ? 'Choose set/test in save dialog' : 'Available after review',
        isReadyToSave: Boolean(content),
    };
}