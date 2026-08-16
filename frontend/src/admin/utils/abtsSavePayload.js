import useHashtagStore from '../stores/useHashtagStore';

const WRITING_META_KEYS = [
    'task_type',
    'word_requirement',
    'chart_data',
    'letter_context',
    'essay_metadata',
    'sample_answer',
    'band_breakdown',
    'key_phrases',
    'grading_notes',
];

const hasItems = (value) => Array.isArray(value) && value.length > 0;

/** Resolve hashtag IDs (TagInput select mode) to codes via the hashtag store cache. */
function resolveHashtagCodes(hashtagIds) {
    if (!hasItems(hashtagIds)) return [];
    const hashtags = useHashtagStore.getState().hashtags || [];
    const byId = new Map(hashtags.map((tag) => [String(tag.id), tag]));
    return hashtagIds
        .map((id) => byId.get(String(id))?.code)
        .filter(Boolean);
}

/** Reproducibility metadata recorded on the created test. */
export function buildGenerationConfig(formData = {}) {
    return {
        skill: formData.skill,
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
        partsToGenerate: formData.selectedParts,
        partConfigs: formData.partConfigs,
    };
}

const writingSectionLayout = (raw) => {
    const layout = {};
    WRITING_META_KEYS.forEach((key) => {
        if (raw && raw[key] !== undefined && raw[key] !== null) {
            layout[key] = raw[key];
        }
    });
    return Object.keys(layout).length > 0 ? layout : null;
};

/**
 * Convert one generated part (raw backend shape) into a SaveSectionInput.
 * Backend shapes:
 * - reading:  { section: { passage_text }, questions: [...] }
 * - listening: { transcript, audio_placeholder, section_layout, questions: [...] }
 * - writing:  { task_prompt, task_type, word_requirement, ... }
 * - multi-part entry: { part, ...<per-skill shape> }
 */
function toSectionInput(skillLower, partNumber, raw, audioUrls, imageUrls) {
    const base = {
        skill: skillLower,
        partNumber,
        passageText: null,
        audioUrl: audioUrls?.[partNumber] ?? null,
        sectionLayout: null,
        imageDescription: imageUrls?.[partNumber] ?? null,
        questions: [],
    };

    if (skillLower === 'reading') {
        base.passageText = raw?.section?.passage_text ?? raw?.passage_text ?? null;
        base.questions = hasItems(raw?.questions) ? raw.questions : [];
    } else if (skillLower === 'listening') {
        base.passageText = raw?.transcript ?? raw?.passage_text ?? null;
        base.sectionLayout = raw?.section_layout ?? null;
        base.questions = hasItems(raw?.questions) ? raw.questions : [];
    } else if (skillLower === 'writing') {
        base.passageText = raw?.task_prompt ?? null;
        base.sectionLayout = writingSectionLayout(raw);
        base.questions = [];
    }

    return base;
}

/**
 * Split raw generated content into per-part SaveSectionInputs.
 */
export function buildPartsToSave(content, skill, { audioUrls = {}, imageUrls = {} } = {}) {
    const skillLower = String(skill || 'reading').toLowerCase();
    const sections = [];

    if (Array.isArray(content?.sections)) {
        content.sections.forEach((section, idx) => {
            const partNumber = section.part ?? section.partNumber ?? idx + 1;
            sections.push(toSectionInput(skillLower, partNumber, section, audioUrls, imageUrls));
        });
        return sections;
    }

    const partNumber = content?.part ?? content?.section?.part ?? 1;
    sections.push(toSectionInput(skillLower, partNumber, content, audioUrls, imageUrls));
    return sections;
}

/**
 * Build the backend SaveContentRequest (SPEC-24 §4):
 * { setCode, setId, testNumber, testId, testName, difficulty, hashtags,
 *   generationMetadata, sections: [SaveSectionInput...] }
 */
export function buildABTSSaveRequest({
    content,
    formData = {},
    saveConfig = {},
    selectedSetId,
    selectedSetCode,
    selectedTestId,
    audioUrls = {},
    imageUrls = {},
} = {}) {
    if (!content) {
        throw new Error('No generated content to save');
    }

    const skill = String(formData.skill || 'reading').toLowerCase();
    const sections = buildPartsToSave(content, skill, {
        audioUrls: saveConfig.audioUrls || audioUrls,
        imageUrls,
    });
    if (sections.length === 0) {
        throw new Error('No sections to save');
    }

    const hashtagCodes = resolveHashtagCodes(saveConfig.hashtags || saveConfig.hashtagIds);

    return {
        setCode: saveConfig.setCode ?? selectedSetCode ?? 'ai_generated',
        setId: saveConfig.setId ?? selectedSetId ?? null,
        testNumber: saveConfig.testNumber ?? null,
        testId: saveConfig.existingTestId ?? saveConfig.testId ?? selectedTestId ?? null,
        testName: saveConfig.testName ?? null,
        difficulty: saveConfig.difficulty ?? formData.difficulty ?? 'INTERMEDIATE',
        hashtags: hashtagCodes,
        generationMetadata: buildGenerationConfig(formData),
        sections,
    };
}
