import React, { useState, useEffect, useMemo } from 'react';
import useABTSStore from '../../stores/useABTSStore';
import {
    FiZap, FiBookOpen, FiHeadphones, FiEdit3,
    FiCpu, FiFileText, FiList, FiPlus, FiX, FiCheck,
    FiSettings, FiChevronDown, FiChevronUp, FiShuffle,
    FiThermometer, FiCode, FiSliders, FiHash, FiAlertCircle,
    FiMinus, FiUpload, FiMic, FiImage, FiLayers, FiGrid
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
        setQuestionTypeCount
    } = useABTSStore();

    const [newFact, setNewFact] = useState('');

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

    const canGenerate = () => {
        if (!formData.skill) return false;
        if (!formData.topic || formData.topic.length < 3) return false;
        if (formData.generationMode === 'CUSTOM_FACTS' && formData.facts.length < 3) return false;
        return true;
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

                {/* === ROW 2: Context & Source (full width) === */}
                <div className="studio-card studio-config__section--full">
                    <div className="studio-card__header">
                        <h3 className="studio-card__title">
                            <FiBookOpen className="studio-card__title-icon" />
                            Context & Source
                        </h3>
                    </div>

                    {/* Mode Toggle */}
                    <div className="studio-modes">
                        <div
                            className={`studio-mode ${formData.generationMode === 'AUTO' ? 'studio-mode--active' : ''}`}
                            onClick={() => updateFormData({ generationMode: 'AUTO', enableWebSearch: true })}
                        >
                            <FiCpu className="studio-mode__icon" />
                            <div className="studio-mode__text">
                                <h4>Auto Research</h4>
                                <p>AI uses web search to find facts</p>
                            </div>
                        </div>
                        <div
                            className={`studio-mode ${formData.generationMode === 'CUSTOM_FACTS' ? 'studio-mode--active' : ''}`}
                            onClick={() => updateFormData({ generationMode: 'CUSTOM_FACTS', enableWebSearch: false })}
                        >
                            <FiFileText className="studio-mode__icon" />
                            <div className="studio-mode__text">
                                <h4>Custom Facts</h4>
                                <p>You provide specific details</p>
                            </div>
                        </div>
                    </div>

                    {/* Web Search Info for Auto Mode */}
                    {formData.generationMode === 'AUTO' && (
                        <div style={{
                            padding: '10px 14px',
                            marginTop: '12px',
                            background: 'rgba(59, 130, 246, 0.1)',
                            border: '1px solid rgba(59, 130, 246, 0.3)',
                            borderRadius: '8px',
                            color: '#93c5fd',
                            fontSize: '0.8rem',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px'
                        }}>
                            <FiZap size={14} />
                            <span>
                                <strong>Web Search Enabled:</strong> AI will search the internet for accurate, up-to-date facts about your topic.
                            </span>
                        </div>
                    )}

                    {/* Topic + Hashtags Row */}
                    <div className="studio-form-row" style={{ marginTop: '16px' }}>
                        <div className="studio-form-group" style={{ flex: 2 }}>
                            <label className="studio-label">Topic / Title</label>
                            <input
                                type="text"
                                className="studio-input"
                                placeholder={formData.generationMode === 'AUTO'
                                    ? "e.g. The benefits of electric cars"
                                    : "e.g. Analysis of renewable energy trends"}
                                value={formData.topic}
                                onChange={(e) => setFormField('topic', e.target.value)}
                            />
                        </div>
                        <div className="studio-form-group" style={{ flex: 1 }}>
                            <TagInput
                                value={formData.hashtags}
                                onChange={(tags) => updateFormData({ hashtags: tags })}
                                placeholder="Add hashtags..."
                                label="Hashtags"
                            />
                        </div>
                    </div>

                    {/* Custom Facts Section */}
                    {formData.generationMode === 'CUSTOM_FACTS' && (
                        <div className="studio-form-group" style={{ marginTop: '12px' }}>
                            <label className="studio-label">
                                Key Facts / Outline
                                <span className="studio-label__value">{formData.facts.length}/30</span>
                            </label>
                            <div style={{ display: 'flex', gap: '8px', marginBottom: '10px' }}>
                                <textarea
                                    className="studio-textarea"
                                    rows={3}
                                    placeholder="Paste facts here (one per line)..."
                                    value={newFact}
                                    onChange={(e) => setNewFact(e.target.value)}
                                    style={{ flex: 1 }}
                                />
                                <button
                                    className="studio-btn studio-btn--primary"
                                    onClick={handleAddFact}
                                    style={{ alignSelf: 'flex-end' }}
                                >
                                    <FiPlus /> Add
                                </button>
                            </div>

                            <div className="studio-facts">
                                {formData.facts.length === 0 ? (
                                    <div className="studio-facts-empty">
                                        No facts added yet. Add at least 3 for best results.
                                    </div>
                                ) : (
                                    formData.facts.map((fact, i) => (
                                        <div key={i} className="studio-fact">
                                            <span className="studio-fact__num">{i + 1}.</span>
                                            <span className="studio-fact__text">{fact}</span>
                                            <button
                                                className="studio-fact__remove"
                                                onClick={() => removeFact(i)}
                                            >
                                                <FiX size={14} />
                                            </button>
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>
                    )}
                </div>

                {/* === ROW 3: Question Types with Counts (full width) === */}
                {formData.skill && QUESTION_TYPES[formData.skill] && (
                    <div className="studio-card studio-config__section--full">
                        <div className="studio-card__header">
                            <h3 className="studio-card__title">
                                <FiList className="studio-card__title-icon" />
                                Question Types
                                {totalSelectedQuestions > 0 && (
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
                            </div>
                        </div>

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
                    </div>
                )}

                {/* === ROW 4: Advanced Settings (2 columns) === */}
                <div className="studio-card">
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

                    {/* Generation Scope Toggle - Only for Reading/Listening */}
                    {(formData.skill === SKILL_TYPES.READING || formData.skill === SKILL_TYPES.LISTENING) && (
                        <div className="studio-form-group" style={{ marginBottom: '16px' }}>
                            <label className="studio-label" style={{ marginBottom: '10px' }}>
                                <FiLayers style={{ marginRight: '6px' }} />
                                Phạm vi tạo nội dung
                            </label>
                            <div className="studio-modes">
                                <div
                                    className={`studio-mode ${formData.scope === GENERATION_SCOPES.SINGLE_PART ? 'studio-mode--active' : ''}`}
                                    onClick={() => setFormField('scope', GENERATION_SCOPES.SINGLE_PART)}
                                >
                                    <FiFileText className="studio-mode__icon" />
                                    <div className="studio-mode__text">
                                        <h4>Tạo từng phần</h4>
                                        <p>Tạo 1 part mỗi lần</p>
                                    </div>
                                </div>
                                <div
                                    className={`studio-mode ${formData.scope === GENERATION_SCOPES.FULL_SKILL ? 'studio-mode--active' : ''}`}
                                    onClick={() => setFormField('scope', GENERATION_SCOPES.FULL_SKILL)}
                                    style={{ position: 'relative' }}
                                >
                                    <FiGrid className="studio-mode__icon" />
                                    <div className="studio-mode__text">
                                        <h4>Tạo toàn bộ</h4>
                                        <p>{formData.skill === SKILL_TYPES.READING ? '3 parts (40 câu)' : '4 parts (40 câu)'}</p>
                                    </div>
                                    <span style={{
                                        position: 'absolute',
                                        top: '4px',
                                        right: '4px',
                                        fontSize: '0.6rem',
                                        padding: '2px 6px',
                                        background: 'linear-gradient(135deg, #f59e0b, #d97706)',
                                        borderRadius: '4px',
                                        fontWeight: 600,
                                        color: '#fff',
                                        textTransform: 'uppercase'
                                    }}>Beta</span>
                                </div>
                            </div>
                            {formData.scope === GENERATION_SCOPES.FULL_SKILL && (
                                <div style={{
                                    marginTop: '10px',
                                    padding: '10px 14px',
                                    background: 'rgba(245, 158, 11, 0.1)',
                                    border: '1px solid rgba(245, 158, 11, 0.3)',
                                    borderRadius: '8px',
                                    color: '#fcd34d',
                                    fontSize: '0.8rem',
                                    display: 'flex',
                                    alignItems: 'flex-start',
                                    gap: '8px'
                                }}>
                                    <FiAlertCircle size={16} style={{ marginTop: '2px', flexShrink: 0 }} />
                                    <div>
                                        <strong>Lưu ý:</strong> Chế độ này tạo {formData.skill === SKILL_TYPES.READING ? '3 bài đọc' : '4 phần nghe'} cùng lúc.
                                        Quá trình sẽ mất nhiều thời gian và token hơn. AI sẽ tạo lần lượt từng part.
                                    </div>
                                </div>
                            )}
                        </div>
                    )}

                    <div className="studio-form-row">
                        {/* Part Selector - Reading or Listening (only when SINGLE_PART scope) */}
                        {(formData.skill === SKILL_TYPES.READING || formData.skill === SKILL_TYPES.LISTENING) && 
                         formData.scope === GENERATION_SCOPES.SINGLE_PART && (
                            <div className="studio-form-group">
                                <label className="studio-label">Part</label>
                                <select
                                    className="studio-select"
                                    value={formData.partNumber || 1}
                                    onChange={(e) => setFormField('partNumber', parseInt(e.target.value))}
                                >
                                    {(formData.skill === SKILL_TYPES.READING ? READING_PARTS : LISTENING_PARTS).map(part => (
                                        <option key={part.value} value={part.value}>{part.label}</option>
                                    ))}
                                </select>
                            </div>
                        )}
                        {/* Passage length is only meaningful for Reading */}
                        {formData.skill === SKILL_TYPES.READING && formData.scope === GENERATION_SCOPES.SINGLE_PART && (
                            <div className="studio-form-group">
                                <label className="studio-label">Passage Length</label>
                                <select
                                    className="studio-select"
                                    value={formData.passageLength}
                                    onChange={(e) => setFormField('passageLength', e.target.value)}
                                >
                                    {PASSAGE_LENGTHS.map(pl => (
                                        <option key={pl.value} value={pl.value}>{pl.label}</option>
                                    ))}
                                </select>
                            </div>
                        )}

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

                    {formData.skill === SKILL_TYPES.READING && (
                        <div className="studio-form-row">
                            <div className="studio-form-group">
                                <label className="studio-label">
                                    Total Questions Target
                                    <span className="studio-label__value">{formData.totalQuestions}</span>
                                </label>
                                <input
                                    type="range"
                                    className="studio-range"
                                    min="8"
                                    max="20"
                                    step="1"
                                    value={formData.totalQuestions}
                                    onChange={(e) => setFormField('totalQuestions', parseInt(e.target.value))}
                                />
                                <div className="studio-range-labels">
                                    <span>8</span>
                                    <span>20</span>
                                </div>
                            </div>
                        </div>
                    )}
                    {formData.skill === SKILL_TYPES.LISTENING && (
                        <div className="studio-form-row">
                            <div className="studio-form-group">
                                <label className="studio-label">
                                    Total Questions
                                    <span className="studio-label__value">10</span>
                                </label>
                                <input
                                    type="range"
                                    className="studio-range"
                                    min="10"
                                    max="10"
                                    step="1"
                                    value={10}
                                    onChange={() => { }}
                                    disabled
                                />
                                <div className="studio-range-labels">
                                    <span>10</span>
                                    <span>10</span>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Audio/Figure Upload Placeholder for Listening */}
                    {formData.skill === SKILL_TYPES.LISTENING && (
                        <div className="studio-form-row">
                            <div className="studio-form-group" style={{ flex: 1 }}>
                                <label className="studio-label" style={{ marginBottom: '8px' }}>
                                    <FiUpload style={{ marginRight: '6px' }} />
                                    Audio File (Coming Soon)
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
                                        Upload audio for transcription
                                    </div>
                                    <div style={{ fontSize: '0.7rem', marginTop: '4px', opacity: 0.6 }}>
                                        MP3, WAV, M4A supported
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

                {!canGenerate() && !isGenerating && (
                    <div className="studio-generate__error">
                        <FiAlertCircle style={{ marginRight: '6px' }} />
                        Please select a skill, enter a topic (min 3 chars)
                        {formData.generationMode === 'CUSTOM_FACTS' && ', and add at least 3 facts'}
                    </div>
                )}
            </div>
        </div >
    );
}
