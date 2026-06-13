import React from 'react';
import { FiAlertCircle, FiCheck, FiMic, FiPlus, FiShuffle, FiX } from 'react-icons/fi';
import { READING_PART_TYPES, LISTENING_PART_TYPES, QUESTION_COUNTS } from '../../stores/useABTSStore';

const PASSAGE_LENGTHS = [
    { value: 'SHORT', label: 'Short', detail: '900-1000w' },
    { value: 'MEDIUM', label: 'Medium', detail: '1000-1100w' },
    { value: 'LONG', label: 'Long', detail: '1100-1200w' }
];

function getTypePool(skill, partNumber) {
    if (skill === 'READING') return READING_PART_TYPES[partNumber] || [];
    if (skill === 'LISTENING') return LISTENING_PART_TYPES[partNumber] || [];
    return [];
}

function getQuestionTypeLabel(questionTypesBySkill, skill, typeId) {
    return questionTypesBySkill[skill]?.find(type => type.id === typeId)?.label || typeId;
}

export default function StudioMultiPartConfig({
    formData,
    questionTypesBySkill,
    randomizePartConfig,
    setPartTopic,
    setPartPassageLength,
    addPartFact,
    removePartFact,
    togglePartQuestionType
}) {
    const selectedParts = formData.selectedParts || [];

    if (selectedParts.length === 0) {
        return (
            <div className="studio-grid">
                <div className="studio-cell studio-cell--12 studio-empty">
                    Select parts in Setup to configure topics, question mix, and source facts.
                </div>
            </div>
        );
    }

    return (
        <div className="studio-part-list">
            {selectedParts.map(partNumber => {
                const partConfig = (formData.partConfigs || {})[partNumber] || {};
                const questionTypes = partConfig.questionTypes || [];
                const questionTypeCounts = partConfig.questionTypeCounts || {};
                const totalQuestions = QUESTION_COUNTS[formData.skill]?.[partNumber] || 13;
                const currentTotal = Object.values(questionTypeCounts).reduce((sum, count) => sum + count, 0);
                const typePool = getTypePool(formData.skill, partNumber);
                const isComplete = currentTotal === totalQuestions && questionTypes.length >= 2;

                return (
                    <div key={partNumber} className="studio-part">
                        <div className="studio-grid">
                            <div className="studio-cell studio-cell--2">
                                <span className="studio-lbl">Part</span>
                                <div className="studio-tag">P{partNumber}</div>
                            </div>
                            <div className="studio-cell studio-cell--2">
                                <span className="studio-lbl">Questions</span>
                                <div className={'studio-tag ' + (isComplete ? 'studio-tag--ok' : 'studio-tag--warn')}>
                                    {currentTotal}/{totalQuestions}
                                </div>
                            </div>

                            {formData.skill === 'READING' ? (
                                <div className="studio-cell studio-cell--3">
                                    <span className="studio-lbl">Length</span>
                                    <select
                                        className="studio-input"
                                        value={partConfig.passageLength || 'MEDIUM'}
                                        onChange={(event) => setPartPassageLength(partNumber, event.target.value)}
                                    >
                                        {PASSAGE_LENGTHS.map(pl => (
                                            <option key={pl.value} value={pl.value}>{pl.label} ({pl.detail})</option>
                                        ))}
                                    </select>
                                </div>
                            ) : (
                                <div className="studio-cell studio-cell--3">
                                    <span className="studio-lbl">Audio</span>
                                    <div className="studio-tag studio-tag--muted"><FiMic size={11} /> Source pending</div>
                                </div>
                            )}

                            <div className="studio-cell studio-cell--3 studio-cell--end">
                                <button
                                    type="button"
                                    className="studio-btn studio-btn--ghost studio-btn--sm"
                                    onClick={() => randomizePartConfig(partNumber)}
                                >
                                    <FiShuffle size={11} /> Random
                                </button>
                            </div>

                            <div className="studio-cell studio-cell--12">
                                <span className="studio-lbl">Topic / Chủ đề</span>
                                <input
                                    type="text"
                                    className="studio-input"
                                    placeholder={`Topic for Part ${partNumber}`}
                                    value={partConfig.topic || ''}
                                    onChange={(event) => setPartTopic(partNumber, event.target.value)}
                                />
                            </div>

                            <div className="studio-cell studio-cell--12">
                                <span className="studio-lbl">Question types ({questionTypes.length}/3)</span>
                                <div className="studio-chipwrap">
                                    {typePool.map(typeId => {
                                        const isSelected = questionTypes.includes(typeId);
                                        const isDisabled = !isSelected && questionTypes.length >= 3;
                                        const count = questionTypeCounts[typeId] || 0;
                                        const typeLabel = getQuestionTypeLabel(questionTypesBySkill, formData.skill, typeId);
                                        return (
                                            <button
                                                key={typeId}
                                                type="button"
                                                onClick={() => togglePartQuestionType(partNumber, typeId)}
                                                disabled={isDisabled}
                                                className={'studio-type-chip' + (isSelected ? ' is-active' : '')}
                                            >
                                                {isSelected && <FiCheck size={11} />}
                                                <span>{typeLabel}</span>
                                                {isSelected && <em>{count}</em>}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>

                            {questionTypes.length < 2 && (
                                <div className="studio-cell studio-cell--12">
                                    <div className="studio-strip studio-strip--warn">
                                        <FiAlertCircle size={12} />
                                        <span>Select at least 2 question types for Part {partNumber}.</span>
                                    </div>
                                </div>
                            )}

                            {formData.generationMode === 'CUSTOM_FACTS' && (
                                <>
                                    <div className="studio-cell studio-cell--12">
                                        <span className="studio-lbl">Key facts ({(partConfig.facts || []).length}/30)</span>
                                        <div className="studio-fact-input">
                                            <input
                                                type="text"
                                                className="studio-input"
                                                placeholder="Add fact and press Enter"
                                                onKeyDown={(event) => {
                                                    if (event.key === 'Enter' && event.target.value.trim()) {
                                                        addPartFact(partNumber, event.target.value);
                                                        event.target.value = '';
                                                    }
                                                }}
                                            />
                                            <button
                                                type="button"
                                                className="studio-btn studio-btn--ghost studio-btn--sm"
                                                onClick={(event) => {
                                                    const input = event.currentTarget.previousElementSibling;
                                                    if (input.value.trim()) {
                                                        addPartFact(partNumber, input.value);
                                                        input.value = '';
                                                    }
                                                }}
                                            >
                                                <FiPlus size={11} /> Add
                                            </button>
                                        </div>
                                    </div>
                                    {(partConfig.facts || []).length > 0 && (
                                        <div className="studio-cell studio-cell--12">
                                            <div className="studio-chipwrap">
                                                {(partConfig.facts || []).map((fact, index) => (
                                                    <span key={`${fact}-${index}`} className="studio-fact-chip">
                                                        <span>{fact}</span>
                                                        <button
                                                            type="button"
                                                            onClick={() => removePartFact(partNumber, index)}
                                                            aria-label="Remove fact"
                                                        >
                                                            <FiX size={10} />
                                                        </button>
                                                    </span>
                                                ))}
                                            </div>
                                        </div>
                                    )}
                                </>
                            )}
                        </div>
                    </div>
                );
            })}
        </div>
    );
}
