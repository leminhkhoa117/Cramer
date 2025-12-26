/**
 * QuestionPreviewRenderer - Read-only preview of questions for admin.
 * Mirrors the QuestionRenderer display format but without interactivity.
 * Uses shared admin passage-preview CSS classes.
 * 
 * @since 2025-12-20 - ABTS v2.0
 * @updated 2025-12-21 - Added inline editing support and new CSS classes
 */

import React, { useState } from 'react';
import '../../css/common/passage-preview.css';

export default function QuestionPreviewRenderer({
    question,
    showAnswer = false,
    onEdit = null,  // Callback for editing: (questionNumber, updatedFields) => void
    isEditable = false,
    showType = true // New prop to control type visibility
}) {
    const { questionNumber, questionType, questionContent, correctAnswer, explanation } = question;

    // State for inline editing
    const [isEditing, setIsEditing] = useState(false);
    const [editedAnswer, setEditedAnswer] = useState(correctAnswer || '');
    const [editedExplanation, setEditedExplanation] = useState(explanation || '');

    // Get the question text from various possible locations
    const getQuestionText = () => {
        if (!questionContent) {
            // Try direct properties
            if (question.text) return question.text;
            if (question.statement) return question.statement;
            return null;
        }

        if (typeof questionContent === 'string') {
            return questionContent;
        }

        // Try common properties
        return questionContent.text ||
            questionContent.statement ||
            questionContent.question ||
            questionContent.sentence ||
            questionContent.incomplete_sentence ||
            questionContent.prompt ||
            questionContent.item ||       // Common for matching
            questionContent.paragraph ||  // Common for matching headings
            questionContent.heading ||
            null;
    };

    const questionText = getQuestionText();

    // Get options if available
    const getOptions = () => {
        if (!questionContent) return [];
        if (Array.isArray(questionContent.options)) return questionContent.options;
        return [];
    };

    const options = getOptions();

    // Handle save edit
    const handleSaveEdit = () => {
        if (onEdit) {
            onEdit(questionNumber, {
                correctAnswer: editedAnswer,
                explanation: editedExplanation
            });
        }
        setIsEditing(false);
    };

    // Handle cancel edit
    const handleCancelEdit = () => {
        setEditedAnswer(correctAnswer || '');
        setEditedExplanation(explanation || '');
        setIsEditing(false);
    };

    // Render based on question type
    const renderQuestionByType = () => {
        switch (questionType) {
            case 'TRUE_FALSE_NOT_GIVEN':
                return (
                    <>
                        <div className="admin-question-text">{questionText || 'No statement provided'}</div>
                        <div className="admin-question-options tfng">
                            <span className="admin-question-option">True</span>
                            <span className="admin-question-option">False</span>
                            <span className="admin-question-option">Not Given</span>
                        </div>
                    </>
                );

            case 'YES_NO_NOT_GIVEN':
                return (
                    <>
                        <div className="admin-question-text">{questionText || 'No statement provided'}</div>
                        <div className="admin-question-options tfng">
                            <span className="admin-question-option">Yes</span>
                            <span className="admin-question-option">No</span>
                            <span className="admin-question-option">Not Given</span>
                        </div>
                    </>
                );

            case 'MULTIPLE_CHOICE':
            case 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS':
                return (
                    <>
                        <div className="admin-question-text">{questionText || 'No question text'}</div>
                        {options.length > 0 && (
                            <div className="admin-question-options mcq">
                                {options.map((opt, i) => {
                                    const letter = typeof opt === 'object' ? opt.letter : opt.charAt?.(0) || String.fromCharCode(65 + i);
                                    const text = typeof opt === 'object' ? opt.text : (opt.substring?.(1)?.trim() || opt);
                                    return (
                                        <div key={i} className="admin-question-option">
                                            <strong>{letter}.</strong> {text}
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </>
                );

            case 'MATCHING_INFORMATION':
            case 'MATCHING_FEATURES':
            case 'MATCHING_HEADINGS':
            case 'MATCHING_SENTENCE_ENDINGS':
            case 'MATCHING':
                return (
                    <>
                        <div className="admin-question-text">{questionText || 'Match the following'}</div>
                        <div className="admin-matching-placeholder">
                            Select from list...
                        </div>
                    </>
                );

            case 'SUMMARY_COMPLETION':
            case 'FILL_IN_BLANK':
            case 'NOTE_COMPLETION':
                return (
                    <>
                        <div className="admin-question-text">
                            {questionText?.replace(/____/g, '_______') || 'Complete the blank'}
                        </div>
                        <div className="admin-input-placeholder">Type answer here</div>
                    </>
                );

            case 'SUMMARY_COMPLETION_OPTIONS':
                return (
                    <>
                        <div className="admin-question-text">
                            {questionText?.replace(/____/g, '[_____]') || 'Select from options'}
                        </div>
                        {options.length > 0 && (
                            <div className="admin-question-options">
                                {options.map((opt, i) => (
                                    <span key={i} className="admin-question-option">
                                        {typeof opt === 'object' ? `${opt.letter}. ${opt.text}` : opt}
                                    </span>
                                ))}
                            </div>
                        )}
                    </>
                );

            case 'TABLE_COMPLETION':
            case 'DIAGRAM_LABEL_COMPLETION':
            case 'FLOW_CHART_COMPLETION':
                return (
                    <>
                        <div className="admin-question-text">{questionText || 'Complete the diagram/table'}</div>
                        <div className="admin-input-placeholder">Fill in the blank</div>
                    </>
                );

            case 'SHORT_ANSWER':
                return (
                    <>
                        <div className="admin-question-text">{questionText || 'Answer the question'}</div>
                        <div className="admin-input-placeholder">Short answer</div>
                    </>
                );

            default:
                return (
                    <>
                        <div className="admin-question-text">{questionText || 'Question content not available'}</div>
                        {questionType && <div className="admin-question-type">{questionType}</div>}
                    </>
                );
        }
    };

    // Format correct answer for display
    const formatAnswer = () => {
        if (!correctAnswer) return 'N/A';
        if (Array.isArray(correctAnswer)) {
            return correctAnswer.join(', ');
        }
        return correctAnswer;
    };

    // Render edit mode
    const renderEditMode = () => (
        <div className="admin-question-edit-mode">
            <div className="admin-question-edit-field">
                <label>Correct Answer</label>
                <input
                    type="text"
                    value={editedAnswer}
                    onChange={(e) => setEditedAnswer(e.target.value)}
                    placeholder="Enter correct answer..."
                />
            </div>
            <div className="admin-question-edit-field">
                <label>Explanation</label>
                <textarea
                    value={editedExplanation}
                    onChange={(e) => setEditedExplanation(e.target.value)}
                    placeholder="Enter explanation..."
                    rows={3}
                />
            </div>
            <div className="admin-question-edit-actions">
                <button
                    className="admin-passage-btn"
                    onClick={handleCancelEdit}
                >
                    Cancel
                </button>
                <button
                    className="admin-passage-btn btn-success"
                    onClick={handleSaveEdit}
                >
                    Save
                </button>
            </div>
        </div>
    );

    return (
        <div className="admin-question-item">
            <div className="admin-question-header">
                <span className="admin-question-number">{questionNumber}</span>
                {showType && <span className="admin-question-type">{questionType?.replace(/_/g, ' ')}</span>}
                {isEditable && !isEditing && (
                    <button
                        className="admin-passage-btn"
                        onClick={() => setIsEditing(true)}
                        style={{ marginLeft: 'auto' }}
                    >
                        Edit
                    </button>
                )}
            </div>

            {renderQuestionByType()}

            {showAnswer && !isEditing && (
                <div className="admin-question-answer-section">
                    <div className="admin-question-answer">
                        <strong>Answer:</strong> <span>{formatAnswer()}</span>
                    </div>
                    {explanation && (
                        <div className="admin-question-explanation">
                            <strong>Explanation:</strong> <span>{explanation}</span>
                        </div>
                    )}
                </div>
            )}

            {isEditing && renderEditMode()}
        </div>
    );
}
