import React, { useEffect, useMemo, useRef, useState } from 'react';
import useABTSStore from '../../stores/useABTSStore';
import {
    FiBookOpen, FiCheck, FiChevronDown, FiCpu, FiEdit3, FiGlobe, FiHash,
    FiHeadphones, FiImage, FiInfo, FiLayers, FiPlus, FiRefreshCw,
    FiSettings, FiShuffle, FiSliders, FiThermometer, FiX
} from 'react-icons/fi';
import { SKILL_TYPES, DIFFICULTY_LEVELS } from '../../services/abtsApi';
import ModelSelector from './ModelSelector';
import ReasoningControls from './ReasoningControls';
import StudioMultiPartConfig from './StudioMultiPartConfig';
import { getAIStudioConfigReadiness } from './aiStudioStatus';
import './AIStudio.css';

const SKILLS = [
    { id: SKILL_TYPES.READING, name: 'Reading', icon: FiBookOpen },
    { id: SKILL_TYPES.LISTENING, name: 'Listening', icon: FiHeadphones },
    { id: SKILL_TYPES.WRITING, name: 'Writing', icon: FiEdit3 }
];

const QUESTION_TYPES = {
    READING: [
        { id: 'FILL_IN_BLANK', label: 'Fill in Blank' },
        { id: 'SUMMARY_COMPLETION', label: 'Summary Completion' },
        { id: 'SUMMARY_COMPLETION_OPTIONS', label: 'Summary w/ Options' },
        { id: 'TRUE_FALSE_NOT_GIVEN', label: 'T/F/NG' },
        { id: 'YES_NO_NOT_GIVEN', label: 'Y/N/NG' },
        { id: 'MATCHING_INFORMATION', label: 'Matching Info' },
        { id: 'MATCHING_HEADINGS', label: 'Matching Headings' },
        { id: 'MATCHING_FEATURES', label: 'Matching Features' },
        { id: 'MATCHING_SENTENCE_ENDINGS', label: 'Sentence Endings' },
        { id: 'MULTIPLE_CHOICE', label: 'MCQ' },
        { id: 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', label: 'MCQ (Multi)' },
        { id: 'DIAGRAM_LABEL_COMPLETION', label: 'Diagram Label' },
        { id: 'TABLE_COMPLETION', label: 'Table Completion' },
        { id: 'FLOW_CHART_COMPLETION', label: 'Flow Chart' }
    ],
    LISTENING: [
        { id: 'FILL_IN_BLANK', label: 'Note Completion' },
        { id: 'MULTIPLE_CHOICE', label: 'MCQ' },
        { id: 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', label: 'MCQ (Multi)' },
        { id: 'MATCHING', label: 'Matching' }
    ],
    WRITING: [
        { id: 'TASK_1', label: 'Task 1' },
        { id: 'TASK_2', label: 'Task 2' }
    ]
};

const ESSAY_TYPES = [
    { value: 'OPINION', label: 'Opinion' },
    { value: 'DISCUSSION', label: 'Discussion' },
    { value: 'PROBLEM_SOLUTION', label: 'Problem/Solution' },
    { value: 'TWO_PART', label: 'Two-Part' }
];

const LANGUAGES = [
    { value: 'VI', label: 'Tiếng Việt' },
    { value: 'EN', label: 'English' }
];

const PARTS_BY_SKILL = {
    READING: [
        { value: 1, label: 'Part 1', count: 13 },
        { value: 2, label: 'Part 2', count: 13 },
        { value: 3, label: 'Part 3', count: 14 }
    ],
    LISTENING: [
        { value: 1, label: 'Part 1', count: 10 },
        { value: 2, label: 'Part 2', count: 10 },
        { value: 3, label: 'Part 3', count: 10 },
        { value: 4, label: 'Part 4', count: 10 }
    ]
};

const WRITING_TASKS = [
    { id: 'TASK_1', label: 'Task 1', target: '150+ words' },
    { id: 'TASK_2', label: 'Task 2', target: '250+ words' }
];

const SECTION_IDS = {
    SETUP: 'setup',
    PARTS: 'parts',
    SOURCE: 'source',
    ADVANCED: 'advanced',
    JSON: 'json'
};

function formatTokens(value) {
    if (!value) return 'Auto';
    return value >= 1000 ? `${(value / 1000).toFixed(0)}K` : value;
}

function getSkillParts(skill) {
    return PARTS_BY_SKILL[skill] || [];
}

function getSelectedQuestionTotal(skill, selectedParts = []) {
    return selectedParts.reduce((total, partNumber) => {
        const part = getSkillParts(skill).find(item => item.value === partNumber);
        return total + (part?.count || 0);
    }, 0);
}

function getDifficultyLabel(value) {
    const difficulty = Object.values(DIFFICULTY_LEVELS).find(level => level.value === value);
    return difficulty ? `${difficulty.label} · ${difficulty.bandRange}` : 'Unset';
}

export default function StudioConfigView() {
    const {
        formData,
        setFormField,
        updateFormData,
        addFact,
        removeFact,
        togglePartSelection,
        randomizePartConfig,
        randomizeAllParts,
        togglePartQuestionType,
        setPartTopic,
        addPartFact,
        removePartFact,
        setPartPassageLength
    } = useABTSStore();
    const selectedModelCapabilities = useABTSStore(state => state.selectCapabilitiesForModel(formData.model));

    const [newFact, setNewFact] = useState('');
    const [openBySkill, setOpenBySkill] = useState({});
    const lastModeRef = useRef(formData.generationMode);

    const skill = formData.skill;
    const isWriting = skill === SKILL_TYPES.WRITING;
    const skillParts = getSkillParts(skill);
    const selectedParts = formData.selectedParts || [];
    const selectedWritingTasks = WRITING_TASKS.filter(task => formData.questionTypes?.includes(task.id));
    const configReadiness = useMemo(() => getAIStudioConfigReadiness(formData), [formData]);

    const openSection = openBySkill[skill] === undefined ? SECTION_IDS.SETUP : openBySkill[skill];
    const setOpen = (id) => setOpenBySkill(prev => ({
        ...prev,
        [skill]: prev[skill] === id ? null : id
    }));

    // Auto-open Source when switching into Custom Facts mode.
    useEffect(() => {
        if (formData.generationMode !== lastModeRef.current) {
            if (formData.generationMode === 'CUSTOM_FACTS') {
                setOpenBySkill(prev => ({ ...prev, [skill]: SECTION_IDS.SOURCE }));
            }
            lastModeRef.current = formData.generationMode;
        }
    }, [formData.generationMode, skill]);

    const expectedQuestionTotal = isWriting
        ? selectedWritingTasks.length
        : getSelectedQuestionTotal(skill, selectedParts);

    const requestPreview = useMemo(() => ({
        skill: formData.skill,
        difficulty: formData.difficulty,
        explanationLanguage: formData.explanationLanguage,
        selectedParts: formData.selectedParts,
        partConfigs: formData.partConfigs,
        writingTasks: formData.questionTypes,
        writingEssayType: formData.writingEssayType || null,
        topic: formData.topic,
        facts: formData.facts,
        generationMode: formData.generationMode,
        temperature: formData.temperature,
        maxTokens: formData.maxTokens,
        model: formData.model || 'default',
        enableReasoning: formData.enableReasoning,
        enableContextCaching: formData.enableContextCaching,
        customInstructions: formData.customInstructions || null
    }), [formData]);

    const handleSkillChange = (skillId) => {
        if (skillId === formData.skill) return;
        updateFormData({
            skill: skillId,
            selectedParts: [],
            partConfigs: {},
            questionTypes: [],
            questionTypeCounts: {},
            topic: '',
            facts: [],
            partNumber: skillId === SKILL_TYPES.WRITING ? 2 : 1,
            totalQuestions: skillId === SKILL_TYPES.LISTENING ? 10 : 13
        });
    };

    const handleWritingTaskToggle = (taskId) => {
        const currentTasks = formData.questionTypes || [];
        const nextTasks = currentTasks.includes(taskId)
            ? currentTasks.filter(task => task !== taskId)
            : [...currentTasks, taskId];
        const nextPartNumber = nextTasks.includes('TASK_2') && !nextTasks.includes('TASK_1') ? 2 : 1;
        updateFormData({
            questionTypes: nextTasks,
            questionTypeCounts: {},
            selectedParts: [],
            partConfigs: {},
            partNumber: nextPartNumber,
            writingEssayType: formData.writingEssayType || 'OPINION'
        });
    };

    const handleAddFact = () => {
        const lines = newFact.split('\n').map(line => line.trim()).filter(Boolean);
        if (lines.length === 0) return;
        lines.forEach(line => addFact(line));
        setNewFact('');
    };

    /* ---------- Section hint strings ---------- */
    const setupHint = `${SKILLS.find(s => s.id === skill)?.name || '—'} · ${getDifficultyLabel(formData.difficulty)}`;
    const partsHint = isWriting
        ? `${selectedWritingTasks.length}/2 tasks · ${expectedQuestionTotal} output${expectedQuestionTotal === 1 ? '' : 's'}`
        : `${selectedParts.length}/${skillParts.length} parts · ${expectedQuestionTotal}/40 questions`;
    const sourceHint = formData.generationMode === 'AUTO'
        ? 'AI web search'
        : isWriting
            ? `${formData.facts?.length || 0} facts`
            : 'Per-part facts';
    const advancedHint = `${formData.temperature}T · ${formatTokens(formData.maxTokens)} tok`;
    const readinessStatus = configReadiness.canGenerate
        ? { tone: 'ready', text: 'Ready' }
        : { tone: 'warn', text: `${configReadiness.issues.length || 1} issue${configReadiness.issues.length === 1 ? '' : 's'}` };

    /* ---------- Renderers ---------- */
    const renderSectionHeader = (id, label, hint, status) => {
        const isOpen = openSection === id;
        return (
            <button
                type="button"
                className={'studio-acc__head' + (isOpen ? ' is-open' : '')}
                onClick={() => setOpen(id)}
                aria-expanded={isOpen}
            >
                <span className="studio-acc__head-title">{label}</span>
                <span className="studio-acc__head-hint">{hint}</span>
                {status && <span className={`studio-acc__head-status studio-acc__head-status--${status.tone}`}>{status.text}</span>}
                <FiChevronDown className="studio-acc__head-chev" size={14} />
            </button>
        );
    };

    return (
        <div className="studio-config studio-config--accordion">
            {/* SETUP */}
            <section className="studio-acc">
                {renderSectionHeader(SECTION_IDS.SETUP, 'Setup', setupHint, readinessStatus)}
                {openSection === SECTION_IDS.SETUP && (
                    <div className="studio-acc__body">
                        <div className="studio-grid">
                            <div className="studio-cell studio-cell--12">
                                <span className="studio-lbl">Skill</span>
                                <div className="studio-skill-row">
                                    {SKILLS.map(s => {
                                        const Icon = s.icon;
                                        const active = formData.skill === s.id;
                                        return (
                                            <button
                                                key={s.id}
                                                type="button"
                                                className={'studio-skill-btn' + (active ? ' is-active' : '')}
                                                onClick={() => handleSkillChange(s.id)}
                                            >
                                                <Icon size={13} />
                                                <span>{s.name}</span>
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>

                            <div className="studio-cell studio-cell--6">
                                <span className="studio-lbl"><FiCpu size={11} /> Model</span>
                                <ModelSelector value={formData.model} onChange={(model) => setFormField('model', model)} />
                            </div>

                            <div className="studio-cell studio-cell--3">
                                <span className="studio-lbl"><FiSliders size={11} /> Difficulty</span>
                                <select
                                    className="studio-input"
                                    value={formData.difficulty}
                                    onChange={(event) => setFormField('difficulty', event.target.value)}
                                >
                                    {Object.values(DIFFICULTY_LEVELS).map(level => (
                                        <option key={level.value} value={level.value}>{level.label} ({level.bandRange})</option>
                                    ))}
                                </select>
                            </div>

                            <div className="studio-cell studio-cell--3">
                                <span className="studio-lbl"><FiBookOpen size={11} /> Explain</span>
                                <select
                                    className="studio-input"
                                    value={formData.explanationLanguage}
                                    onChange={(event) => setFormField('explanationLanguage', event.target.value)}
                                >
                                    {LANGUAGES.map(l => (
                                        <option key={l.value} value={l.value}>{l.label}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="studio-cell studio-cell--6">
                                <span className="studio-lbl">Generation mode</span>
                                <div className="studio-seg">
                                    <button
                                        type="button"
                                        className={formData.generationMode === 'AUTO' ? 'is-active' : ''}
                                        onClick={() => setFormField('generationMode', 'AUTO')}
                                    ><FiGlobe size={12} /> Auto · web search</button>
                                    <button
                                        type="button"
                                        className={formData.generationMode === 'CUSTOM_FACTS' ? 'is-active' : ''}
                                        onClick={() => setFormField('generationMode', 'CUSTOM_FACTS')}
                                    ><FiEdit3 size={12} /> Custom facts</button>
                                </div>
                            </div>

                            <div className="studio-cell studio-cell--6">
                                <span className="studio-lbl"><FiLayers size={11} /> {isWriting ? 'Tasks' : 'Parts'}</span>
                                {isWriting ? (
                                    <div className="studio-pillrow">
                                        {WRITING_TASKS.map(task => {
                                            const sel = formData.questionTypes?.includes(task.id);
                                            return (
                                                <button
                                                    key={task.id}
                                                    type="button"
                                                    className={'studio-pill' + (sel ? ' is-active' : '')}
                                                    onClick={() => handleWritingTaskToggle(task.id)}
                                                >
                                                    {sel && <FiCheck size={11} />}
                                                    <span>{task.label}</span>
                                                    <em>{task.target}</em>
                                                </button>
                                            );
                                        })}
                                    </div>
                                ) : (
                                    <div className="studio-pillrow">
                                        {skillParts.map(part => {
                                            const sel = selectedParts.includes(part.value);
                                            return (
                                                <button
                                                    key={part.value}
                                                    type="button"
                                                    className={'studio-pill' + (sel ? ' is-active' : '')}
                                                    onClick={() => togglePartSelection(part.value)}
                                                >
                                                    {sel && <FiCheck size={11} />}
                                                    <span>{part.label}</span>
                                                    <em>{part.count}Q</em>
                                                </button>
                                            );
                                        })}
                                    </div>
                                )}
                            </div>

                            {isWriting && (
                                <div className="studio-cell studio-cell--12">
                                    <span className="studio-lbl">Topic / prompt focus</span>
                                    <input
                                        type="text"
                                        className="studio-input"
                                        placeholder="e.g., public transport, remote work, climate policy"
                                        value={formData.topic || ''}
                                        onChange={(event) => setFormField('topic', event.target.value)}
                                    />
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </section>

            {/* PARTS / TASKS */}
            <section className="studio-acc">
                {renderSectionHeader(SECTION_IDS.PARTS, isWriting ? 'Tasks' : 'Parts', partsHint)}
                {openSection === SECTION_IDS.PARTS && (
                    <div className="studio-acc__body">
                        {!isWriting && selectedParts.length > 0 && (
                            <div className="studio-acc__actions">
                                <button
                                    type="button"
                                    className="studio-btn studio-btn--ghost studio-btn--sm"
                                    onClick={randomizeAllParts}
                                >
                                    <FiShuffle size={12} /> Randomize all
                                </button>
                            </div>
                        )}

                        {isWriting ? (
                            <div className="studio-grid">
                                {selectedWritingTasks.length === 0 ? (
                                    <div className="studio-cell studio-cell--12 studio-empty">
                                        Select Task 1, Task 2, or both in Setup to configure Writing.
                                    </div>
                                ) : selectedWritingTasks.map(task => (
                                    <React.Fragment key={task.id}>
                                        <div className="studio-cell studio-cell--2">
                                            <span className="studio-lbl">Task</span>
                                            <div className="studio-tag">{task.label}</div>
                                        </div>
                                        <div className="studio-cell studio-cell--4">
                                            <span className="studio-lbl">Target</span>
                                            <div className="studio-tag studio-tag--muted">{task.target}</div>
                                        </div>
                                        {task.id === 'TASK_2' ? (
                                            <div className="studio-cell studio-cell--6">
                                                <span className="studio-lbl">Essay type</span>
                                                <select
                                                    className="studio-input"
                                                    value={formData.writingEssayType || 'OPINION'}
                                                    onChange={(event) => setFormField('writingEssayType', event.target.value)}
                                                >
                                                    {ESSAY_TYPES.map(type => (
                                                        <option key={type.value} value={type.value}>{type.label}</option>
                                                    ))}
                                                </select>
                                            </div>
                                        ) : (
                                            <div className="studio-cell studio-cell--6">
                                                <span className="studio-lbl">Visual</span>
                                                <div className="studio-tag studio-tag--muted"><FiImage size={12} /> AI chooses chart/graph</div>
                                            </div>
                                        )}
                                    </React.Fragment>
                                ))}
                            </div>
                        ) : (
                            <StudioMultiPartConfig
                                formData={formData}
                                questionTypesBySkill={QUESTION_TYPES}
                                randomizePartConfig={randomizePartConfig}
                                setPartTopic={setPartTopic}
                                setPartPassageLength={setPartPassageLength}
                                addPartFact={addPartFact}
                                removePartFact={removePartFact}
                                togglePartQuestionType={togglePartQuestionType}
                            />
                        )}
                    </div>
                )}
            </section>

            {/* SOURCE & FACTS */}
            <section className={'studio-acc' + (formData.generationMode === 'AUTO' ? ' is-muted' : '')}>
                {renderSectionHeader(SECTION_IDS.SOURCE, 'Source & Facts', sourceHint)}
                {openSection === SECTION_IDS.SOURCE && (
                    <div className="studio-acc__body">
                        <div className="studio-strip">
                            {formData.generationMode === 'AUTO' ? <FiGlobe size={13} /> : <FiInfo size={13} />}
                            <span>
                                {formData.generationMode === 'AUTO'
                                    ? 'Auto mode: AI searches for source material from per-part topics.'
                                    : isWriting
                                        ? 'Custom Facts: add at least 3 Writing facts below.'
                                        : 'Custom Facts: configure each part\'s facts in the Parts section.'}
                            </span>
                        </div>

                        <div className="studio-grid">
                            {isWriting && formData.generationMode === 'CUSTOM_FACTS' && (
                                <>
                                    <div className="studio-cell studio-cell--12">
                                        <span className="studio-lbl">Writing facts / outline (one per line)</span>
                                        <textarea
                                            className="studio-input studio-textarea"
                                            rows={3}
                                            value={newFact}
                                            onChange={(event) => setNewFact(event.target.value)}
                                            placeholder="Paste one fact per line, then click Add."
                                        />
                                    </div>
                                    <div className="studio-cell studio-cell--12 studio-acc__actions">
                                        <button type="button" className="studio-btn studio-btn--sm" onClick={handleAddFact}>
                                            <FiPlus size={12} /> Add facts
                                        </button>
                                    </div>
                                    {(formData.facts || []).length > 0 && (
                                        <div className="studio-cell studio-cell--12">
                                            <div className="studio-chipwrap">
                                                {formData.facts.map((fact, index) => (
                                                    <span key={`${fact}-${index}`} className="studio-fact-chip">
                                                        <span>{fact}</span>
                                                        <button type="button" onClick={() => removeFact(index)} aria-label="Remove fact">
                                                            <FiX size={10} />
                                                        </button>
                                                    </span>
                                                ))}
                                            </div>
                                        </div>
                                    )}
                                </>
                            )}

                            <div className="studio-cell studio-cell--12">
                                <span className="studio-lbl"><FiHash size={11} /> Hashtags</span>
                                <input
                                    type="text"
                                    className="studio-input"
                                    placeholder="#cambridge17 #test1"
                                    value={formData.hashtags?.join(' ') || ''}
                                    onChange={(event) => {
                                        const tags = event.target.value
                                            .split(/\s+/)
                                            .filter(Boolean)
                                            .map(tag => tag.startsWith('#') ? tag : `#${tag}`);
                                        setFormField('hashtags', tags.filter(tag => tag.length > 1));
                                    }}
                                />
                            </div>
                        </div>
                    </div>
                )}
            </section>

            {/* ADVANCED */}
            <section className="studio-acc">
                {renderSectionHeader(SECTION_IDS.ADVANCED, 'Advanced', advancedHint)}
                {openSection === SECTION_IDS.ADVANCED && (
                    <div className="studio-acc__body">
                        <div className="studio-grid">
                            <div className="studio-cell studio-cell--6">
                                <span className="studio-lbl"><FiThermometer size={11} /> Temperature <strong>{formData.temperature}</strong></span>
                                <input
                                    type="range"
                                    className="studio-range"
                                    min="0"
                                    max="2"
                                    step="0.1"
                                    value={formData.temperature}
                                    onChange={(event) => setFormField('temperature', parseFloat(event.target.value))}
                                />
                            </div>

                            <div className="studio-cell studio-cell--6">
                                <span className="studio-lbl"><FiSettings size={11} /> Max tokens <strong>{formatTokens(formData.maxTokens)}</strong></span>
                                <input
                                    type="range"
                                    className="studio-range"
                                    min="4000"
                                    max="65000"
                                    step="5000"
                                    value={formData.maxTokens}
                                    onChange={(event) => setFormField('maxTokens', parseInt(event.target.value, 10))}
                                />
                            </div>

                            <div className="studio-cell studio-cell--6">
                                <span className="studio-lbl"><FiCpu size={11} /> Reasoning</span>
                                <ReasoningControls
                                    capabilities={selectedModelCapabilities}
                                    value={{
                                        enableReasoning: formData.enableReasoning,
                                        reasoningEffort: formData.reasoningEffort,
                                        reasoningBudget: formData.reasoningBudget
                                    }}
                                    onChange={(patch) => updateFormData(patch)}
                                />
                            </div>

                            <div className="studio-cell studio-cell--3">
                                <span className="studio-lbl"><FiRefreshCw size={11} /> Context cache</span>
                                <button
                                    type="button"
                                    className={'studio-toggle' + (formData.enableContextCaching ? ' is-active' : '')}
                                    onClick={() => setFormField('enableContextCaching', !formData.enableContextCaching)}
                                    aria-pressed={formData.enableContextCaching}
                                ><span /></button>
                            </div>

                            <div className="studio-cell studio-cell--12">
                                <span className="studio-lbl">Custom instructions</span>
                                <textarea
                                    className="studio-input studio-textarea"
                                    rows={2}
                                    placeholder="Focus on academic vocabulary, stricter IELTS phrasing, or specific distractor design."
                                    value={formData.customInstructions}
                                    onChange={(event) => setFormField('customInstructions', event.target.value)}
                                />
                            </div>
                        </div>
                    </div>
                )}
            </section>

            {/* JSON PREVIEW */}
            <section className="studio-acc">
                {renderSectionHeader(SECTION_IDS.JSON, 'Request preview', 'JSON payload')}
                {openSection === SECTION_IDS.JSON && (
                    <div className="studio-acc__body">
                        <pre className="studio-json">{JSON.stringify(requestPreview, null, 2)}</pre>
                    </div>
                )}
            </section>
        </div>
    );
}
