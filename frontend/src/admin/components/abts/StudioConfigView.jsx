import React, { useState, useEffect, useMemo } from 'react';
import useABTSStore, { READING_PART_TYPES, LISTENING_PART_TYPES, QUESTION_COUNTS } from '../../stores/useABTSStore';
import {
    FiZap, FiBookOpen, FiHeadphones, FiEdit3,
    FiCpu, FiFileText, FiList, FiPlus, FiX, FiCheck,
    FiSettings, FiChevronDown, FiChevronUp, FiShuffle,
    FiThermometer, FiCode, FiSliders, FiHash, FiAlertCircle,
    FiMinus, FiUpload, FiMic, FiImage, FiLayers, FiGrid,
    FiRefreshCw, FiGlobe, FiInfo
} from 'react-icons/fi';
import { SKILL_TYPES, DIFFICULTY_LEVELS, GENERATION_SCOPES } from '../../services/abtsApi';
import TagInput from './TagInput';
import ModelSelector from './ModelSelector';
import './AIStudio.css';

/**
 * StudioConfigView - Power-User Configuration Panel
 * 
 * V5.0: Dense full-width layout with all settings visible
 * - Per-question-type count controls
 * - Passage length control
 * - Custom instructions
 * - JSON preview panel
 * - Max tokens slider
 */

// --- Constants ---
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
        { id: 'TASK_1', label: 'Task 1 (Chart/Graph)' },
        { id: 'TASK_2', label: 'Task 2 (Essay)' }
    ]
};

// Writing Task 2 Essay Types
const ESSAY_TYPES = [
    { value: 'OPINION', label: 'Opinion (Agree/Disagree)', desc: 'To what extent do you agree or disagree?' },
    { value: 'DISCUSSION', label: 'Discussion (Both Views)', desc: 'Discuss both views and give your opinion' },
    { value: 'PROBLEM_SOLUTION', label: 'Problem & Solution', desc: 'What are the problems and how can they be solved?' },
    { value: 'TWO_PART', label: 'Two-Part Question', desc: 'Answer both parts of the question' }
];

const LANGUAGES = [
    { value: 'VI', label: 'Tieng Viet' },
    { value: 'EN', label: 'English' }
];

const PASSAGE_LENGTHS = [
    { value: 'SHORT', label: 'Short (900-1000w)' },
    { value: 'MEDIUM', label: 'Medium (1000-1100w)' },
    { value: 'LONG', label: 'Long (1100-1200w)' }
];

// Reading Parts (1, 2, or 3 - each with different passage complexity)
const READING_PARTS = [
    { value: 1, label: 'Part 1 (Easier)' },
    { value: 2, label: 'Part 2 (Medium)' },
    { value: 3, label: 'Part 3 (Hardest)' }
];

const LISTENING_PARTS = [
    { value: 1, label: 'Part 1 (Conversation)' },
    { value: 2, label: 'Part 2 (Monologue)' },
    { value: 3, label: 'Part 3 (Discussion)' },
    { value: 4, label: 'Part 4 (Lecture)' }
];

export default function StudioConfigView({ onGenerate }) {
    const {
        formData,
        setFormField,
        updateFormData,
        addFact,
        removeFact,
        isGenerating,
        toggleQuestionType,
        setQuestionTypeCount,
        // Multi-part actions
        togglePartSelection,
        setPartConfig,
        clearPartSelections,
        // Per-part question type actions
        randomizePartConfig,
        randomizeAllParts,
        togglePartQuestionType,
        // Per-part config actions (topic, facts, passageLength)
        setPartTopic,
        addPartFact,
        removePartFact,
        setPartPassageLength
    } = useABTSStore();

    const [newFact, setNewFact] = useState('');
    const [newPartFacts, setNewPartFacts] = useState({}); // Track new fact input per part

    // --- Computed ---
    const totalSelectedQuestions = useMemo(() => {
        if (!formData.questionTypeCounts) return 0;
        return Object.values(formData.questionTypeCounts).reduce((a, b) => a + b, 0);
    }, [formData.questionTypeCounts]);

    useEffect(() => {
        if (formData.skill === SKILL_TYPES.LISTENING && formData.totalQuestions !== 10) {
            setFormField('totalQuestions', 10);
        }
    }, [formData.skill, formData.totalQuestions, setFormField]);

    // --- Helpers ---
    const handleAddFact = () => {
        if (!newFact.trim()) return;
        const lines = newFact.split('\n').filter(l => l.trim().length > 0);
        lines.forEach(line => addFact(line));
        setNewFact('');
    };

    const handleRandomizeTypes = () => {
        if (!formData.skill || !QUESTION_TYPES[formData.skill]) return;
        const types = QUESTION_TYPES[formData.skill];
        const count = Math.floor(Math.random() * 3) + 2;
        const shuffled = [...types].sort(() => 0.5 - Math.random());
        const selected = shuffled.slice(0, count);

        const newCounts = {};
        selected.forEach((t, i) => {
            newCounts[t.id] = i === 0 ? 4 : 3; // First type gets 4, rest get 3
        });

        updateFormData({
            questionTypes: selected.map(t => t.id),
            questionTypeCounts: newCounts
        });
    };

    const handleClearTypes = () => {
        updateFormData({ questionTypes: [], questionTypeCounts: {} });
    };

    // Computes specific validation errors
    const validationErrors = useMemo(() => {
        const errors = [];
        if (!formData.skill) errors.push('Vui lòng chọn kỹ năng (Skill)');
        if (!formData.selectedParts?.length) errors.push('Vui lòng chọn ít nhất một phần thi (Part)');

        formData.selectedParts?.forEach(part => {
            const partConfig = formData.partConfigs?.[part];
            const partLabel = `Part ${part}`;

            // Each part must have a topic (at least 3 chars)
            if (!partConfig?.topic || partConfig.topic.length < 3) {
                errors.push(`${partLabel}: Nhập topic (tối thiểu 3 ký tự)`);
            }

            // Each part must have at least 2 question types
            if (!partConfig?.questionTypes || partConfig.questionTypes.length < 2) {
                errors.push(`${partLabel}: Chọn ít nhất 2 loại câu hỏi`);
            }

            // If CUSTOM_FACTS mode, each part must have at least 3 facts
            if (formData.generationMode === 'CUSTOM_FACTS') {
                if (!partConfig?.facts || partConfig.facts.length < 3) {
                    errors.push(`${partLabel}: Thêm ít nhất 3 facts`);
                }
            }
        });
        return errors;
    }, [formData]);

    const canGenerate = () => {
        return validationErrors.length === 0;
    };

    // Build request preview for JSON panel
    const requestPreview = useMemo(() => ({
        skill: formData.skill,
        topic: formData.topic,
        difficulty: formData.difficulty,
        questionTypes: formData.questionTypes.length > 0 ? formData.questionTypes : 'AI decides',
        questionTypeCounts: Object.keys(formData.questionTypeCounts).length > 0
            ? formData.questionTypeCounts : 'auto',
        passageLength: formData.skill === SKILL_TYPES.READING ? formData.passageLength : null,
        temperature: formData.temperature,
        maxTokens: formData.maxTokens,
        model: formData.model || 'default',
        customInstructions: formData.customInstructions || null
    }), [formData]);

    return (
        <div className="studio-config">
            <div className="studio-config__grid">

                {/* === ROW 1: Skill + Model (side by side) === */}
                <div className="studio-card">
                    <div className="studio-card__header">
                        <h3 className="studio-card__title">
                            <FiZap className="studio-card__title-icon" />
                            Skill
                        </h3>
                    </div>
                    <div className="studio-options studio-options--3col">
                        {SKILLS.map(skill => {
                            const Icon = skill.icon;
                            return (
                                <button
                                    key={skill.id}
                                    className={`studio-option ${formData.skill === skill.id ? 'studio-option--active' : ''}`}
                                    onClick={() => setFormField('skill', skill.id)}
                                >
                                    <Icon className="studio-option__icon" />
                                    <span className="studio-option__label">{skill.name}</span>
                                </button>
                            );
                        })}
                    </div>
                </div>

                <div className="studio-card" style={{ zIndex: 20, position: 'relative' }}>
                    <div className="studio-card__header">
                        <h3 className="studio-card__title">
                            <FiCpu className="studio-card__title-icon" />
                            AI Model
                        </h3>
                    </div>
                    <ModelSelector
                        value={formData.model}
                        onChange={(m) => setFormField('model', m)}
                    />
                </div>

                {/* === ROW 2: Generation Settings (full width) === */}
                <div className="studio-card studio-config__section--full">
                    <div className="studio-card__header">
                        <h3 className="studio-card__title">
                            <FiSliders className="studio-card__title-icon" />
                            Generation Settings
                        </h3>
                    </div>

                    <div className="studio-form-row">
                        <div className="studio-form-group">
                            <label className="studio-label">Difficulty</label>
                            <select
                                className="studio-select"
                                value={formData.difficulty}
                                onChange={(e) => setFormField('difficulty', e.target.value)}
                            >
                                {Object.values(DIFFICULTY_LEVELS).map(lvl => (
                                    <option key={lvl.value} value={lvl.value}>
                                        {lvl.label} ({lvl.bandRange})
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div className="studio-form-group">
                            <label className="studio-label">Explanation Language</label>
                            <select
                                className="studio-select"
                                value={formData.explanationLanguage}
                                onChange={(e) => setFormField('explanationLanguage', e.target.value)}
                            >
                                {LANGUAGES.map(lang => (
                                    <option key={lang.value} value={lang.value}>{lang.label}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {/* Multi-Part Selection - Reading/Listening only */}
                    {(formData.skill === SKILL_TYPES.READING || formData.skill === SKILL_TYPES.LISTENING) && (
                        <div className="studio-form-group" style={{ marginBottom: '16px' }}>
                            <label className="studio-label" style={{ marginBottom: '10px' }}>
                                <FiLayers style={{ marginRight: '6px' }} />
                                Chọn các Parts cần tạo
                            </label>
                            <div style={{
                                padding: '14px',
                                background: 'rgba(16, 185, 129, 0.08)',
                                border: '1px solid rgba(16, 185, 129, 0.3)',
                                borderRadius: '8px'
                            }}>
                                <div style={{
                                    fontSize: '0.8rem',
                                    color: '#6ee7b7',
                                    marginBottom: '10px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between'
                                }}>
                                    <span>
                                        <FiCheck style={{ marginRight: '6px' }} />
                                        Chọn các parts cần tạo:
                                    </span>
                                    {formData.selectedParts?.length > 0 && (
                                        <span style={{ color: '#a7f3d0', fontSize: '0.75rem' }}>
                                            ({formData.selectedParts.length} đã chọn)
                                        </span>
                                    )}
                                </div>
                                <div style={{
                                    display: 'grid',
                                    gridTemplateColumns: formData.skill === SKILL_TYPES.READING
                                        ? 'repeat(3, 1fr)'
                                        : 'repeat(4, 1fr)',
                                    gap: '8px'
                                }}>
                                    {(formData.skill === SKILL_TYPES.READING ? READING_PARTS : LISTENING_PARTS).map(part => {
                                        const isSelected = formData.selectedParts?.includes(part.value);
                                        return (
                                            <button
                                                key={part.value}
                                                type="button"
                                                onClick={() => togglePartSelection(part.value)}
                                                style={{
                                                    padding: '10px 12px',
                                                    borderRadius: '8px',
                                                    border: isSelected
                                                        ? '2px solid #10b981'
                                                        : '1px solid rgba(255,255,255,0.15)',
                                                    background: isSelected
                                                        ? 'rgba(16, 185, 129, 0.2)'
                                                        : 'rgba(255,255,255,0.05)',
                                                    color: isSelected ? '#6ee7b7' : 'rgba(255,255,255,0.7)',
                                                    cursor: 'pointer',
                                                    fontSize: '0.8rem',
                                                    fontWeight: isSelected ? 600 : 400,
                                                    textAlign: 'center',
                                                    transition: 'all 150ms ease',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    gap: '6px'
                                                }}
                                            >
                                                {isSelected && <FiCheck size={14} />}
                                                Part {part.value}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>
                    )}

                    <div className="studio-form-row">
                        {/* Essay Type Selector for Writing Task 2 */}
                        {formData.skill === SKILL_TYPES.WRITING && formData.questionTypes?.includes('TASK_2') && (
                            <div className="studio-form-group">
                                <label className="studio-label">Essay Type</label>
                                <select
                                    className="studio-select"
                                    value={formData.writingEssayType || 'OPINION'}
                                    onChange={(e) => setFormField('writingEssayType', e.target.value)}
                                >
                                    {ESSAY_TYPES.map(type => (
                                        <option key={type.value} value={type.value}>
                                            {type.label}
                                        </option>
                                    ))}
                                </select>
                                <div style={{
                                    fontSize: '0.75rem',
                                    color: 'rgba(255,255,255,0.5)',
                                    marginTop: '4px'
                                }}>
                                    {ESSAY_TYPES.find(t => t.value === (formData.writingEssayType || 'OPINION'))?.desc}
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Audio/Figure Upload Placeholder for Listening */}
                    {formData.skill === SKILL_TYPES.LISTENING && (
                        <div className="studio-form-row">
                            <div className="studio-form-group" style={{ flex: 1 }}>
                                <label className="studio-label" style={{ marginBottom: '8px' }}>
                                    <FiUpload style={{ marginRight: '6px' }} />
                                    Audio File
                                </label>
                                <div style={{
                                    padding: '20px',
                                    border: '2px dashed rgba(255,255,255,0.15)',
                                    borderRadius: '8px',
                                    textAlign: 'center',
                                    color: 'rgba(255,255,255,0.4)',
                                    cursor: 'not-allowed',
                                    background: 'rgba(255,255,255,0.02)'
                                }}>
                                    <FiMic size={24} style={{ marginBottom: '8px', opacity: 0.5 }} />
                                    <div style={{ fontSize: '0.8rem' }}>
                                        Audio URLs can be added after generation
                                    </div>
                                    <div style={{ fontSize: '0.7rem', marginTop: '4px', opacity: 0.6 }}>
                                        Paste URLs in the preview step
                                    </div>
                                </div>
                            </div>
                            <div className="studio-form-group" style={{ flex: 1 }}>
                                <label className="studio-label" style={{ marginBottom: '8px' }}>
                                    <FiImage style={{ marginRight: '6px' }} />
                                    Map/Diagram (Coming Soon)
                                </label>
                                <div style={{
                                    padding: '20px',
                                    border: '2px dashed rgba(255,255,255,0.15)',
                                    borderRadius: '8px',
                                    textAlign: 'center',
                                    color: 'rgba(255,255,255,0.4)',
                                    cursor: 'not-allowed',
                                    background: 'rgba(255,255,255,0.02)'
                                }}>
                                    <FiImage size={24} style={{ marginBottom: '8px', opacity: 0.5 }} />
                                    <div style={{ fontSize: '0.8rem' }}>
                                        Upload map or diagram
                                    </div>
                                    <div style={{ fontSize: '0.7rem', marginTop: '4px', opacity: 0.6 }}>
                                        PNG, JPG, SVG supported
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    <div className="studio-form-row">
                        <div className="studio-form-group">
                            <label className="studio-label">
                                <span><FiThermometer style={{ marginRight: '4px' }} />Temperature</span>
                                <span className="studio-label__value">{formData.temperature}</span>
                            </label>
                            <input
                                type="range"
                                className="studio-range"
                                min="0"
                                max="2"
                                step="0.1"
                                value={formData.temperature}
                                onChange={(e) => setFormField('temperature', parseFloat(e.target.value))}
                            />
                            <div className="studio-range-labels">
                                <span>Precise</span>
                                <span>Creative</span>
                            </div>
                        </div>
                        <div className="studio-form-group">
                            <label className="studio-label">
                                Max Tokens
                                <span className="studio-label__value">{formData.maxTokens >= 1000 ? `${(formData.maxTokens / 1000).toFixed(0)}K` : formData.maxTokens}</span>
                            </label>
                            <input
                                type="range"
                                className="studio-range"
                                min="4000"
                                max="65000"
                                step="5000"
                                value={formData.maxTokens}
                                onChange={(e) => setFormField('maxTokens', parseInt(e.target.value))}
                            />
                            <div className="studio-range-labels">
                                <span>4K</span>
                                <span>65K</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* === ROW 3: Question Types with Counts (full width) === */}
                {formData.skill && QUESTION_TYPES[formData.skill] && (
                    <div className="studio-card studio-config__section--full">
                        <div className="studio-card__header">
                            <h3 className="studio-card__title">
                                <FiList className="studio-card__title-icon" />
                                Question Types
                                {formData.scope === GENERATION_SCOPES.SINGLE_PART && totalSelectedQuestions > 0 && (
                                    <span style={{
                                        marginLeft: '8px',
                                        fontSize: '0.7rem',
                                        color: '#a78bfa',
                                        fontWeight: 'normal'
                                    }}>
                                        ({totalSelectedQuestions} questions selected)
                                    </span>
                                )}
                            </h3>
                            <div className="studio-card__actions">
                                {/* Single Part: Random/Clear buttons */}
                                {formData.scope === GENERATION_SCOPES.SINGLE_PART && (
                                    <>
                                        <button
                                            className="studio-btn studio-btn--ghost studio-btn--sm"
                                            onClick={handleRandomizeTypes}
                                            title="Pick Random Types"
                                        >
                                            <FiShuffle size={14} /> Random
                                        </button>
                                        <button
                                            className="studio-btn studio-btn--ghost studio-btn--sm"
                                            onClick={handleClearTypes}
                                            title="Clear All (AI Decides)"
                                        >
                                            <FiX size={14} /> Clear
                                        </button>
                                    </>
                                )}
                                {/* Multi Part: Randomize All button */}
                                {formData.scope === 'MULTI_PART' && formData.selectedParts?.length > 0 && (
                                    <button
                                        className="studio-btn studio-btn--ghost studio-btn--sm"
                                        onClick={randomizeAllParts}
                                        title="Randomize All Parts"
                                    >
                                        <FiShuffle size={14} /> Ngẫu nhiên tất cả
                                    </button>
                                )}
                            </div>
                        </div>

                        {/* === SINGLE_PART MODE: Global question type selector === */}
                        {formData.scope === GENERATION_SCOPES.SINGLE_PART && (
                            <>
                                {/* Empty state hint */}
                                {formData.questionTypes.length === 0 && (
                                    <div style={{
                                        padding: '10px 14px',
                                        marginBottom: '12px',
                                        background: 'rgba(139, 92, 246, 0.08)',
                                        border: '1px dashed rgba(139, 92, 246, 0.3)',
                                        borderRadius: '8px',
                                        color: '#c4b5fd',
                                        fontSize: '0.8rem',
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '8px'
                                    }}>
                                        <FiZap size={14} />
                                        No types selected - AI will choose optimal types automatically.
                                    </div>
                                )}

                                {/* Question Types Grid with Count Controls */}
                                <div className="studio-qtype-grid">
                                    {QUESTION_TYPES[formData.skill].map(type => {
                                        const isSelected = formData.questionTypes?.includes(type.id);
                                        const count = formData.questionTypeCounts?.[type.id] || 0;

                                        return (
                                            <div
                                                key={type.id}
                                                className={`studio-qtype-item ${isSelected ? 'studio-qtype-item--active' : ''}`}
                                            >
                                                <span
                                                    className="studio-qtype-label"
                                                    onClick={() => toggleQuestionType(type.id)}
                                                    style={{ cursor: 'pointer' }}
                                                >
                                                    {type.label}
                                                </span>

                                                {isSelected && (
                                                    <div className="studio-qtype-controls">
                                                        <button
                                                            className="studio-qtype-btn"
                                                            onClick={() => setQuestionTypeCount(type.id, count - 1)}
                                                            disabled={count <= 1}
                                                        >
                                                            <FiMinus size={10} />
                                                        </button>
                                                        <span className="studio-qtype-count">{count}</span>
                                                        <button
                                                            className="studio-qtype-btn"
                                                            onClick={() => setQuestionTypeCount(type.id, count + 1)}
                                                            disabled={count >= 10}
                                                        >
                                                            <FiPlus size={10} />
                                                        </button>
                                                    </div>
                                                )}

                                                {!isSelected && (
                                                    <button
                                                        className="studio-btn studio-btn--ghost studio-btn--sm"
                                                        onClick={() => toggleQuestionType(type.id)}
                                                        style={{ padding: '4px 8px' }}
                                                    >
                                                        <FiPlus size={12} />
                                                    </button>
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>
                            </>
                        )}

                        {/* === MULTI_PART MODE: Per-part configuration panels === */}
                        {formData.scope === 'MULTI_PART' && (
                            <>
                                {formData.selectedParts?.length === 0 && (
                                    <div style={{
                                        padding: '20px',
                                        textAlign: 'center',
                                        color: 'rgba(255,255,255,0.5)',
                                        fontSize: '0.85rem'
                                    }}>
                                        Chọn các parts ở trên để cấu hình loại câu hỏi
                                    </div>
                                )}

                                {formData.selectedParts?.length > 0 && (
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                                        {formData.selectedParts.map(partNumber => {
                                            const partConfig = formData.partConfigs[partNumber] || {};
                                            const questionTypes = partConfig.questionTypes || [];
                                            const questionTypeCounts = partConfig.questionTypeCounts || {};
                                            const totalQuestions = QUESTION_COUNTS[formData.skill]?.[partNumber] || 13;
                                            const currentTotal = Object.values(questionTypeCounts).reduce((a, b) => a + b, 0);
                                            const typePool = formData.skill === 'READING'
                                                ? READING_PART_TYPES[partNumber]
                                                : LISTENING_PART_TYPES[partNumber];

                                            return (
                                                <div key={partNumber} style={{
                                                    padding: '14px',
                                                    background: 'rgba(255, 255, 255, 0.03)',
                                                    border: '1px solid rgba(255, 255, 255, 0.1)',
                                                    borderRadius: '8px'
                                                }}>
                                                    {/* Part Header */}
                                                    <div style={{
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        justifyContent: 'space-between',
                                                        marginBottom: '12px'
                                                    }}>
                                                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                                            <span style={{ fontWeight: 600, color: '#a78bfa' }}>
                                                                Part {partNumber}
                                                            </span>
                                                            <span style={{
                                                                fontSize: '0.75rem',
                                                                color: currentTotal === totalQuestions ? '#6ee7b7' : '#fcd34d'
                                                            }}>
                                                                {currentTotal}/{totalQuestions} câu
                                                            </span>
                                                        </div>
                                                        <button
                                                            className="studio-btn studio-btn--ghost studio-btn--sm"
                                                            onClick={() => randomizePartConfig(partNumber)}
                                                            style={{ padding: '4px 10px' }}
                                                        >
                                                            <FiShuffle size={12} /> Random
                                                        </button>
                                                    </div>

                                                    {/* Per-Part Topic & Settings Row */}
                                                    <div style={{
                                                        display: 'flex',
                                                        gap: '12px',
                                                        marginBottom: '12px',
                                                        flexWrap: 'wrap'
                                                    }}>
                                                        {/* Topic Input */}
                                                        <div style={{ flex: '2 1 200px' }}>
                                                            <label style={{
                                                                fontSize: '0.7rem',
                                                                color: 'rgba(255,255,255,0.5)',
                                                                marginBottom: '4px',
                                                                display: 'block'
                                                            }}>
                                                                Topic / Chủ đề
                                                            </label>
                                                            <input
                                                                type="text"
                                                                className="studio-input"
                                                                placeholder={`Chủ đề Part ${partNumber}...`}
                                                                value={partConfig.topic || ''}
                                                                onChange={(e) => setPartTopic(partNumber, e.target.value)}
                                                                style={{
                                                                    fontSize: '0.8rem',
                                                                    padding: '8px 10px'
                                                                }}
                                                            />
                                                        </div>
                                                        {/* Passage Length (Reading only) */}
                                                        {formData.skill === 'READING' && (
                                                            <div style={{ flex: '1 1 140px' }}>
                                                                <label style={{
                                                                    fontSize: '0.7rem',
                                                                    color: 'rgba(255,255,255,0.5)',
                                                                    marginBottom: '4px',
                                                                    display: 'block'
                                                                }}>
                                                                    Độ dài Passage
                                                                </label>
                                                                <select
                                                                    className="studio-select"
                                                                    value={partConfig.passageLength || 'MEDIUM'}
                                                                    onChange={(e) => setPartPassageLength(partNumber, e.target.value)}
                                                                    style={{
                                                                        fontSize: '0.8rem',
                                                                        padding: '8px 10px'
                                                                    }}
                                                                >
                                                                    {PASSAGE_LENGTHS.map(pl => (
                                                                        <option key={pl.value} value={pl.value}>{pl.label}</option>
                                                                    ))}
                                                                </select>
                                                            </div>
                                                        )}
                                                    </div>

                                                    {/* Per-Part Facts (only when CUSTOM_FACTS mode) */}
                                                    {formData.generationMode === 'CUSTOM_FACTS' && (
                                                        <div style={{ marginBottom: '12px' }}>
                                                            <label style={{
                                                                fontSize: '0.7rem',
                                                                color: 'rgba(255,255,255,0.5)',
                                                                marginBottom: '4px',
                                                                display: 'flex',
                                                                justifyContent: 'space-between'
                                                            }}>
                                                                <span>Key Facts / Outline</span>
                                                                <span>{(partConfig.facts || []).length}/30</span>
                                                            </label>

                                                            {/* Multi-line textarea with Parse button */}
                                                            <div style={{ marginBottom: '8px' }}>
                                                                <textarea
                                                                    className="studio-input"
                                                                    placeholder="Nhập danh sách facts, mỗi dòng là 1 fact...&#10;Ví dụ:&#10;Fact 1&#10;Fact 2&#10;Fact 3"
                                                                    rows={3}
                                                                    style={{
                                                                        width: '100%',
                                                                        fontSize: '0.75rem',
                                                                        padding: '8px',
                                                                        resize: 'vertical',
                                                                        minHeight: '60px'
                                                                    }}
                                                                    id={`facts-textarea-${partNumber}`}
                                                                />
                                                                <button
                                                                    type="button"
                                                                    className="studio-btn studio-btn--ghost studio-btn--sm"
                                                                    onClick={() => {
                                                                        const textarea = document.getElementById(`facts-textarea-${partNumber}`);
                                                                        if (textarea && textarea.value.trim()) {
                                                                            const lines = textarea.value
                                                                                .split('\n')
                                                                                .map(line => line.trim())
                                                                                .filter(line => line.length > 0);
                                                                            lines.forEach(line => addPartFact(partNumber, line));
                                                                            textarea.value = '';
                                                                        }
                                                                    }}
                                                                    style={{ marginTop: '6px', width: '100%' }}
                                                                >
                                                                    <FiPlus size={12} /> Parse & Add Facts
                                                                </button>
                                                            </div>

                                                            {/* Single-line quick add */}
                                                            <div style={{ display: 'flex', gap: '6px', marginBottom: '6px' }}>
                                                                <input
                                                                    type="text"
                                                                    className="studio-input"
                                                                    placeholder="Hoặc thêm 1 fact..."
                                                                    onKeyDown={(e) => {
                                                                        if (e.key === 'Enter' && e.target.value.trim()) {
                                                                            addPartFact(partNumber, e.target.value);
                                                                            e.target.value = '';
                                                                        }
                                                                    }}
                                                                    style={{ flex: 1, fontSize: '0.75rem', padding: '6px 8px' }}
                                                                />
                                                                <button
                                                                    type="button"
                                                                    className="studio-btn studio-btn--ghost studio-btn--sm"
                                                                    onClick={(e) => {
                                                                        const input = e.target.closest('div').querySelector('input');
                                                                        if (input.value.trim()) {
                                                                            addPartFact(partNumber, input.value);
                                                                            input.value = '';
                                                                        }
                                                                    }}
                                                                    style={{ padding: '6px 10px' }}
                                                                >
                                                                    <FiPlus size={12} />
                                                                </button>
                                                            </div>
                                                            {(partConfig.facts || []).length > 0 && (
                                                                <div style={{
                                                                    display: 'flex',
                                                                    flexWrap: 'wrap',
                                                                    gap: '4px',
                                                                    maxHeight: '60px',
                                                                    overflowY: 'auto'
                                                                }}>
                                                                    {(partConfig.facts || []).map((fact, i) => (
                                                                        <span key={i} style={{
                                                                            fontSize: '0.7rem',
                                                                            padding: '3px 8px',
                                                                            background: 'rgba(167, 139, 250, 0.15)',
                                                                            border: '1px solid rgba(167, 139, 250, 0.3)',
                                                                            borderRadius: '4px',
                                                                            color: '#c4b5fd',
                                                                            display: 'flex',
                                                                            alignItems: 'center',
                                                                            gap: '4px'
                                                                        }}>
                                                                            <span style={{
                                                                                maxWidth: '120px',
                                                                                overflow: 'hidden',
                                                                                textOverflow: 'ellipsis',
                                                                                whiteSpace: 'nowrap'
                                                                            }}>
                                                                                {fact}
                                                                            </span>
                                                                            <button
                                                                                type="button"
                                                                                onClick={() => removePartFact(partNumber, i)}
                                                                                style={{
                                                                                    background: 'none',
                                                                                    border: 'none',
                                                                                    color: '#c4b5fd',
                                                                                    cursor: 'pointer',
                                                                                    padding: 0,
                                                                                    display: 'flex'
                                                                                }}
                                                                            >
                                                                                <FiX size={10} />
                                                                            </button>
                                                                        </span>
                                                                    ))}
                                                                </div>
                                                            )}
                                                        </div>
                                                    )}

                                                    {/* Question Type Chips */}
                                                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                                                        {typePool.map(typeId => {
                                                            const isSelected = questionTypes.includes(typeId);
                                                            const count = questionTypeCounts[typeId] || 0;
                                                            const typeLabel = QUESTION_TYPES[formData.skill]?.find(t => t.id === typeId)?.label || typeId;

                                                            return (
                                                                <button
                                                                    key={typeId}
                                                                    type="button"
                                                                    onClick={() => togglePartQuestionType(partNumber, typeId)}
                                                                    disabled={!isSelected && questionTypes.length >= 3}
                                                                    style={{
                                                                        padding: '6px 12px',
                                                                        borderRadius: '6px',
                                                                        fontSize: '0.75rem',
                                                                        cursor: (!isSelected && questionTypes.length >= 3) ? 'not-allowed' : 'pointer',
                                                                        border: isSelected
                                                                            ? '1px solid #10b981'
                                                                            : '1px solid rgba(255, 255, 255, 0.15)',
                                                                        background: isSelected
                                                                            ? 'rgba(16, 185, 129, 0.2)'
                                                                            : 'rgba(255, 255, 255, 0.05)',
                                                                        color: isSelected ? '#6ee7b7' : 'rgba(255, 255, 255, 0.7)',
                                                                        display: 'flex',
                                                                        alignItems: 'center',
                                                                        gap: '6px',
                                                                        transition: 'all 150ms ease',
                                                                        opacity: (!isSelected && questionTypes.length >= 3) ? 0.5 : 1
                                                                    }}
                                                                >
                                                                    {isSelected && <FiCheck size={12} />}
                                                                    <span>{typeLabel}</span>
                                                                    {isSelected && (
                                                                        <span style={{ fontSize: '0.65rem', opacity: 0.8 }}>
                                                                            ({count})
                                                                        </span>
                                                                    )}
                                                                </button>
                                                            );
                                                        })}
                                                    </div>

                                                    {/* Warning for insufficient types */}
                                                    {questionTypes.length < 2 && (
                                                        <div style={{
                                                            marginTop: '10px',
                                                            padding: '8px 12px',
                                                            background: 'rgba(245, 158, 11, 0.1)',
                                                            border: '1px solid rgba(245, 158, 11, 0.3)',
                                                            borderRadius: '6px',
                                                            color: '#fcd34d',
                                                            fontSize: '0.75rem',
                                                            display: 'flex',
                                                            alignItems: 'center',
                                                            gap: '6px'
                                                        }}>
                                                            <FiAlertCircle size={14} />
                                                            Chọn ít nhất 2 loại câu hỏi
                                                        </div>
                                                    )}
                                                </div>
                                            );
                                        })}
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                )}

                {/* === ROW 4: Context & Source (full width) === */}
                <div className="studio-card studio-config__section--full">
                    <div className="studio-card__header">
                        <h3 className="studio-card__title">
                            <FiBookOpen className="studio-card__title-icon" />
                            Context & Source
                        </h3>
                    </div>

                    {/* Generation Mode Toggle */}
                    <div className="studio-form-group" style={{ marginBottom: '16px' }}>
                        <label className="studio-label" style={{ marginBottom: '10px' }}>
                            <FiRefreshCw style={{ marginRight: '6px' }} />
                            Generation Mode
                        </label>
                        <div style={{ display: 'flex', gap: '8px' }}>
                            <button
                                type="button"
                                className={`studio-btn ${formData.generationMode === 'AUTO' ? 'studio-btn--primary' : 'studio-btn--ghost'}`}
                                onClick={() => setFormField('generationMode', 'AUTO')}
                                style={{ flex: 1 }}
                            >
                                <FiZap size={14} style={{ marginRight: '4px' }} />
                                Auto (AI decides)
                            </button>
                            <button
                                type="button"
                                className={`studio-btn ${formData.generationMode === 'CUSTOM_FACTS' ? 'studio-btn--primary' : 'studio-btn--ghost'}`}
                                onClick={() => setFormField('generationMode', 'CUSTOM_FACTS')}
                                style={{ flex: 1 }}
                            >
                                <FiEdit3 size={14} style={{ marginRight: '4px' }} />
                                Custom Facts
                            </button>
                        </div>
                        <div style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.5)', marginTop: '6px' }}>
                            {formData.generationMode === 'AUTO'
                                ? 'AI will search the web and generate topic-relevant passages'
                                : 'Provide specific facts for AI to incorporate into the passage'
                            }
                        </div>
                    </div>

                    {/* Web Search Info */}
                    {formData.generationMode === 'AUTO' && (
                        <div style={{
                            padding: '12px',
                            background: 'rgba(59, 130, 246, 0.1)',
                            border: '1px solid rgba(59, 130, 246, 0.3)',
                            borderRadius: '8px',
                            marginBottom: '16px'
                        }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#93c5fd', fontSize: '0.85rem' }}>
                                <FiGlobe size={16} />
                                <span>Web Search Enabled</span>
                            </div>
                            <div style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.6)', marginTop: '4px' }}>
                                AI will automatically search for relevant, up-to-date information based on the topic
                            </div>
                        </div>
                    )}

                    {/* Hashtags Input */}
                    <div className="studio-form-group">
                        <label className="studio-label">
                            <FiHash style={{ marginRight: '6px' }} />
                            Hashtags (Optional)
                        </label>
                        <input
                            type="text"
                            className="studio-input"
                            placeholder="Add tags to categorize, e.g., #cambridge17 #test1 #part1"
                            value={formData.hashtags?.join(' ') || ''}
                            onChange={(e) => {
                                const tags = e.target.value.split(/\s+/).filter(t => t.startsWith('#') || t.length > 0);
                                setFormField('hashtags', tags.map(t => t.startsWith('#') ? t : `#${t}`).filter(t => t.length > 1));
                            }}
                        />
                        <div style={{ fontSize: '0.7rem', color: 'rgba(255,255,255,0.4)', marginTop: '4px' }}>
                            Tags help organize and filter generated content
                        </div>
                    </div>

                    {/* Note about per-part configuration */}
                    {(formData.skill === SKILL_TYPES.READING || formData.skill === SKILL_TYPES.LISTENING) && formData.selectedParts?.length > 0 && (
                        <div style={{
                            padding: '12px',
                            background: 'rgba(16, 185, 129, 0.08)',
                            border: '1px solid rgba(16, 185, 129, 0.3)',
                            borderRadius: '8px',
                            marginTop: '16px'
                        }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#6ee7b7', fontSize: '0.85rem' }}>
                                <FiInfo size={16} />
                                <span>Per-Part Configuration</span>
                            </div>
                            <div style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.6)', marginTop: '4px' }}>
                                Topic, passage length, and custom facts can be configured for each selected part in the Question Types section above
                            </div>
                        </div>
                    )}
                </div>

                {/* Custom Instructions + JSON Preview */}
                <div className="studio-card">
                    <div className="studio-card__header">
                        <h3 className="studio-card__title">
                            <FiCode className="studio-card__title-icon" />
                            Advanced
                        </h3>
                        <button
                            className={`studio-btn studio-btn--ghost studio-btn--sm ${formData.showJsonPreview ? 'studio-option--active' : ''}`}
                            onClick={() => setFormField('showJsonPreview', !formData.showJsonPreview)}
                        >
                            <FiCode size={14} /> JSON
                        </button>
                    </div>

                    {/* Context Caching Toggle */}
                    <div className="studio-form-group">
                        <label className="studio-label" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                            <span>Context Caching</span>
                            <button
                                type="button"
                                className={`studio-toggle ${formData.enableContextCaching ? 'studio-toggle--active' : ''}`}
                                onClick={() => setFormField('enableContextCaching', !formData.enableContextCaching)}
                                style={{
                                    width: '44px',
                                    height: '24px',
                                    borderRadius: '12px',
                                    border: 'none',
                                    background: formData.enableContextCaching ? '#10B981' : 'rgba(255,255,255,0.2)',
                                    position: 'relative',
                                    cursor: 'pointer',
                                    transition: 'background 150ms ease'
                                }}
                            >
                                <span style={{
                                    position: 'absolute',
                                    top: '2px',
                                    left: formData.enableContextCaching ? '22px' : '2px',
                                    width: '20px',
                                    height: '20px',
                                    borderRadius: '50%',
                                    background: 'white',
                                    transition: 'left 150ms ease'
                                }} />
                            </button>
                        </label>
                        <div style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.5)' }}>
                            Enable to decrease response delay and cost for repeated prompts
                        </div>
                    </div>

                    <div className="studio-form-group">
                        <label className="studio-label">Custom Instructions (Optional)</label>
                        <textarea
                            className="studio-textarea"
                            rows={2}
                            placeholder="Add custom instructions for the AI, e.g., 'Focus on academic vocabulary' or 'Include complex sentence structures'"
                            value={formData.customInstructions}
                            onChange={(e) => setFormField('customInstructions', e.target.value)}
                        />
                    </div>

                    {formData.showJsonPreview && (
                        <div className="studio-json-panel" style={{ marginTop: '12px' }}>
                            <div className="studio-json-header">
                                <span className="studio-json-header__title">Request Preview</span>
                            </div>
                            <div className="studio-json-content">
                                <pre>{JSON.stringify(requestPreview, null, 2)}</pre>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* Generate Button */}
            <div className="studio-generate">
                <button
                    className="studio-btn studio-btn--primary studio-generate__btn"
                    onClick={onGenerate}
                    disabled={!canGenerate() || isGenerating}
                >
                    <FiZap size={18} />
                    {isGenerating ? 'Generating Content...' : 'Generate Content'}
                </button>

                {!canGenerate() && !isGenerating && validationErrors.length > 0 && (
                    <div className="studio-generate__error">
                        <div style={{ display: 'flex', alignItems: 'center', marginBottom: '4px', fontWeight: 600 }}>
                            <FiAlertCircle style={{ marginRight: '6px' }} />
                            Vui lòng hoàn thành các mục sau:
                        </div>
                        <ul style={{ margin: 0, paddingLeft: '24px', listStyleType: 'disc' }}>
                            {validationErrors.map((error, idx) => (
                                <li key={idx} style={{ marginTop: '2px' }}>{error}</li>
                            ))}
                        </ul>
                    </div>
                )}
            </div>
        </div >
    );
}
