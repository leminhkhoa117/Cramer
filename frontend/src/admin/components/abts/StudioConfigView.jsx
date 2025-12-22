import React, { useState, useEffect, useMemo } from 'react';
import useABTSStore from '../../stores/useABTSStore';
import {
    FiZap, FiBookOpen, FiHeadphones, FiEdit3,
    FiCpu, FiFileText, FiList, FiPlus, FiX, FiCheck,
    FiSettings, FiChevronDown, FiChevronUp, FiShuffle,
    FiThermometer, FiCode, FiSliders, FiHash, FiAlertCircle,
    FiMinus
} from 'react-icons/fi';
import { SKILL_TYPES, DIFFICULTY_LEVELS } from '../../services/abtsApi';
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
        { id: 'MULTIPLE_CHOICE', label: 'MCQ' },
        { id: 'FORM_COMPLETION', label: 'Form Completion' },
        { id: 'LABELLING_DIAGRAM', label: 'Label Diagram' },
        { id: 'MATCHING', label: 'Matching' },
        { id: 'SENTENCE_COMPLETION', label: 'Sentence Completion' },
        { id: 'SHORT_ANSWER_QUESTION', label: 'Short Answer' }
    ],
    WRITING: [
        { id: 'TASK_1', label: 'Task 1 (Chart/Graph)' },
        { id: 'TASK_2', label: 'Task 2 (Essay)' }
    ]
};

const LANGUAGES = [
    { value: 'VI', label: 'Tieng Viet' },
    { value: 'EN', label: 'English' }
];

const PASSAGE_LENGTHS = [
    { value: 'SHORT', label: 'Short (800-900w)' },
    { value: 'MEDIUM', label: 'Medium (900-1000w)' },
    { value: 'LONG', label: 'Long (1000-1200w)' }
];

// Reading Parts (1, 2, or 3 - each with different passage complexity)
const READING_PARTS = [
    { value: 1, label: 'Part 1 (Easier)' },
    { value: 2, label: 'Part 2 (Medium)' },
    { value: 3, label: 'Part 3 (Hardest)' }
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
        passageLength: formData.passageLength,
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
                            onClick={() => updateFormData({ generationMode: 'AUTO' })}
                        >
                            <FiCpu className="studio-mode__icon" />
                            <div className="studio-mode__text">
                                <h4>Auto Research</h4>
                                <p>AI finds facts and writes content</p>
                            </div>
                        </div>
                        <div
                            className={`studio-mode ${formData.generationMode === 'CUSTOM_FACTS' ? 'studio-mode--active' : ''}`}
                            onClick={() => updateFormData({ generationMode: 'CUSTOM_FACTS' })}
                        >
                            <FiFileText className="studio-mode__icon" />
                            <div className="studio-mode__text">
                                <h4>Custom Facts</h4>
                                <p>You provide specific details</p>
                            </div>
                        </div>
                    </div>

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

                    <div className="studio-form-row">
                        {/* Part Selector - only for Reading */}
                        {formData.skill === SKILL_TYPES.READING && (
                            <div className="studio-form-group">
                                <label className="studio-label">Part</label>
                                <select
                                    className="studio-select"
                                    value={formData.partNumber || 1}
                                    onChange={(e) => setFormField('partNumber', parseInt(e.target.value))}
                                >
                                    {READING_PARTS.map(part => (
                                        <option key={part.value} value={part.value}>{part.label}</option>
                                    ))}
                                </select>
                            </div>
                        )}
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
                    </div>

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
                                <span className="studio-label__value">{formData.maxTokens}</span>
                            </label>
                            <input
                                type="range"
                                className="studio-range"
                                min="4000"
                                max="16000"
                                step="1000"
                                value={formData.maxTokens}
                                onChange={(e) => setFormField('maxTokens', parseInt(e.target.value))}
                            />
                            <div className="studio-range-labels">
                                <span>4K</span>
                                <span>16K</span>
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
