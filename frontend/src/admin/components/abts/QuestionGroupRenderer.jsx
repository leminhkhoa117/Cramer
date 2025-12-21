/**
 * QuestionGroupRenderer - Renders a group of related questions with their shared context.
 * 
 * "Shared Context" includes:
 * - List of Headings (for Matching Headings)
 * - Options Box (for matching features/sentence endings)
 * - Diagram Images (for label completion)
 * - Summary Text/Tables (for completion tasks)
 * 
 * Uses the actual QuestionRenderer for high-fidelity preview.
 * 
 * @since 2025-12-21
 */

import React from 'react';
import QuestionRenderer from '../../../components/QuestionRenderer'; // Use REAL renderer
import { sanitizeHtml } from '../../utils/htmlSanitizer';

export default function QuestionGroupRenderer({
    group,
    allQuestions, // Needed for absolute indexing
    selectedQuestionIndex, // Absolute index of currently selected question
    onSelectQuestion,
    onEditQuestion,
    onRegenerateQuestion,
    isGenerating = false,
    regeneratingQuestionId = null,
    // New props for preview interactivity
    userAnswers = {},
    onAnswerChange = () => { }
}) {
    const { type, questions, startNum, endNum } = group;

    // Helper: Determine if this group needs a "Shared Context" box
    const renderSharedContext = () => {
        if (!questions || questions.length === 0) return null;
        const firstQ = questions[0];
        const content = firstQ.questionContent || {};

        // 1. IMAGE CONTEXT (Diagrams)
        if (firstQ.imageUrl) {
            return (
                <div className="admin-shared-context-media">
                    <img src={firstQ.imageUrl} alt="Diagram" className="context-image" />
                </div>
            );
        }

        // 2. TEXT CONTEXT (Summaries, Tables, Flowcharts) - Exceptions first
        // SUMMARY_COMPLETION_OPTIONS typically has shared options, but might have text too.
        if (['SUMMARY_COMPLETION', 'TABLE_COMPLETION', 'FLOW_CHART_COMPLETION', 'NOTE_COMPLETION'].includes(type)) {
            const textContent = content.text || content.summary || content.body || content.main_statement;
            if (textContent && textContent.length > 0) {
                return (
                    <div className="admin-shared-context-text">
                        <div
                            className="context-html-content"
                            dangerouslySetInnerHTML={{
                                __html: sanitizeHtml(textContent.replace(/____/g, '<span class="context-blank">____</span>'))
                            }}
                        />
                    </div>
                );
            }
        }

        // 3. OPTIONS CONTEXT (Matching Headings, Matching Features, Box Completion)
        if (['MATCHING_HEADINGS', 'MATCHING_FEATURES', 'MATCHING_SENTENCE_ENDINGS', 'SUMMARY_COMPLETION_OPTIONS', 'MATCHING_INFORMATION', 'MATCHING'].includes(type)) {
            const options = content.options || content.headings || content.items;
            if (options && Array.isArray(options) && options.length > 0) {
                return (
                    <div className="admin-shared-context-box">
                        <div className="context-title">
                            {type === 'MATCHING_HEADINGS' ? 'List of Headings' : 'Options'}
                        </div>
                        <div className="context-options-grid">
                            {options.map((opt, i) => (
                                <div key={i} className="context-option-item">
                                    {typeof opt === 'object' ? (
                                        <><strong>{opt.letter || opt.id || (i + 1)}.</strong> {opt.text || opt.content}</>
                                    ) : (
                                        <>{opt}</>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>
                );
            }
        }

        return null; // No shared context found
    };

    // Helper: Get instructions based on type
    const getGroupInstructions = (type) => {
        switch (type) {
            case 'TRUE_FALSE_NOT_GIVEN': return 'Do the following statements agree with the information given in the Reading Passage?';
            case 'YES_NO_NOT_GIVEN': return 'Do the following statements agree with the claims of the writer in the Reading Passage?';
            case 'MATCHING_HEADINGS': return 'Choose the correct heading for each paragraph from the list of headings below.';
            case 'SUMMARY_COMPLETION': return 'Complete the summary below. Choose NO MORE THAN TWO WORDS from the passage for each answer.';
            case 'SUMMARY_COMPLETION_OPTIONS': return 'Complete the summary below. Choose the correct letter, A, B, C, etc.';
            case 'MULTIPLE_CHOICE': return 'Choose the correct letter, A, B, C or D.';
            case 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS': return 'Choose TWO letters, A-E.';
            case 'DIAGRAM_LABEL_COMPLETION': return 'Label the diagram below.';
            default: return 'Answer the questions below.';
        }
    };

    return (
        <div className="admin-question-group">
            {/* 1. Group Header & Instructions */}
            <div className="admin-question-group-header">
                <h5>Questions {startNum}-{endNum}</h5>
                <p className="admin-question-group-instructions">{getGroupInstructions(type)}</p>

                {/* 2. Shared Context (Image, Text, or Options) */}
                {renderSharedContext()}
            </div>

            {/* 2. List of Question Renderers */}
            <div className="admin-group-questions-list">
                {questions.map((question, qIndex) => {
                    const absIndex = allQuestions ? allQuestions.findIndex(q => q === question) : -1;
                    const questionNum = question.questionNumber || (absIndex !== -1 ? absIndex + 1 : qIndex + 1);
                    // Use question absolute ID/index as key for answers
                    const questionId = absIndex !== -1 ? absIndex : `temp-${qIndex}`;

                    return (
                        <div key={qIndex} className="preview-question-wrapper">
                            {/* The Real Renderer */}
                            <QuestionRenderer
                                question={{ ...question, id: questionId, questionNumber: questionNum }}
                                userAnswer={userAnswers[questionId]}
                                onAnswerChange={onAnswerChange}
                                partId="preview"
                            />

                            {/* Admin Footer: Answer Key & Explanation */}
                            <div className="admin-question-footer">
                                <div className="answer-key">
                                    <strong>Answer:</strong>
                                    <span className="answer-value">
                                        {Array.isArray(question.correctAnswer)
                                            ? question.correctAnswer.join(', ')
                                            : question.correctAnswer}
                                    </span>
                                </div>
                                {question.explanation && (
                                    <div className="explanation-text">
                                        <small>💡 {question.explanation}</small>
                                    </div>
                                )}

                                {/* Only show regenerate if callback provided and selected */}
                                {onRegenerateQuestion && (
                                    <button
                                        className="btn-regenerate-question tiny"
                                        onClick={() => onRegenerateQuestion(questionNum)}
                                        disabled={isGenerating || regeneratingQuestionId === questionNum}
                                        style={{ marginTop: '8px' }}
                                    >
                                        {regeneratingQuestionId === questionNum ? '⏳' : '🔄'} Regenerate
                                    </button>
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>

            <style>{`
                .admin-question-group {
                    margin-bottom: 24px;
                    background: rgba(255, 255, 255, 0.02);
                    border-radius: 12px;
                    border: 1px solid rgba(255, 255, 255, 0.05);
                    overflow: hidden;
                }
                .admin-question-group-header {
                    padding: 16px 20px;
                    background: rgba(255, 255, 255, 0.03);
                    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
                }
                .admin-question-group-header h5 {
                    margin: 0 0 4px 0;
                    font-size: 1rem;
                    color: white;
                }
                .admin-question-group-instructions {
                    margin: 0;
                    font-size: 0.85rem;
                    color: rgba(255, 255, 255, 0.6);
                    font-style: italic;
                }

                /* Context Boxes reused from previous implementation... */
                .admin-shared-context-box {
                    margin-top: 16px;
                    padding: 16px;
                    background: rgba(0, 0, 0, 0.2);
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    border-radius: 8px;
                }
                .context-title {
                    font-size: 0.75rem;
                    color: rgba(255, 255, 255, 0.5);
                    text-transform: uppercase;
                    margin-bottom: 12px;
                    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
                    padding-bottom: 4px;
                }
                .context-options-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 12px;
                }
                .context-option-item, .context-html-content {
                    font-size: 0.9rem;
                    color: rgba(255, 255, 255, 0.9);
                    line-height: 1.5;
                }
                .context-option-item strong { color: #a855f7; }

                .admin-shared-context-text {
                    margin-top: 16px;
                    padding: 16px;
                    background: rgba(255, 255, 255, 0.03);
                    border-left: 3px solid #a855f7;
                    border-radius: 4px;
                }
                .context-blank {
                    display: inline-block;
                    width: 60px;
                    border-bottom: 1px solid rgba(255, 255, 255, 0.3);
                }

                .admin-shared-context-media {
                    margin-top: 16px;
                    text-align: center;
                }
                .context-image {
                    max-width: 100%;
                    max-height: 300px;
                    border-radius: 8px;
                    border: 1px solid rgba(255, 255, 255, 0.1);
                }
                
                /* Question Wrapper inside group */
                .preview-question-wrapper {
                    padding: 16px 20px;
                    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
                }
                .preview-question-wrapper:last-child { border-bottom: none; }
                
                .admin-question-footer {
                    margin-top: 12px;
                    padding-top: 12px;
                    border-top: 1px dashed rgba(255, 255, 255, 0.1);
                    font-size: 0.9rem;
                }
                .answer-key { color: #86efac; margin-bottom: 4px; }
                .answer-value { font-weight: bold; margin-left: 6px; }
                .explanation-text { color: rgba(255, 255, 255, 0.6); line-height: 1.4; }
                
                .btn-regenerate-question.tiny {
                    padding: 4px 8px;
                    font-size: 0.7rem;
                    background: rgba(255, 255, 255, 0.05);
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    color: white;
                    border-radius: 4px;
                    cursor: pointer;
                }
                .btn-regenerate-question.tiny:hover { background: rgba(255, 255, 255, 0.1); }

                /* --- DARK MODE OVERRIDES FOR QuestionRenderer.css --- */
                /* 1. Transparent Question Container */
                .preview-question-wrapper .question-block {
                    background: transparent !important;
                    border: none !important;
                    box-shadow: none !important;
                    padding: 0 !important;
                    margin: 0 !important;
                    color: rgba(255, 255, 255, 0.9) !important;
                }

                /* 2. Text Color Overrides */
                .preview-question-wrapper .question-text-interactive,
                .preview-question-wrapper p,
                .preview-question-wrapper span:not(.question-number):not(.highlight-span) {
                    color: rgba(255, 255, 255, 0.9) !important;
                }
                
                .preview-question-wrapper .question-number {
                    color: #d8b4fe !important; /* Lighter purple */
                }

                /* 3. Input & Select Overrides */
                .preview-question-wrapper input[type="text"],
                .preview-question-wrapper select, 
                .fill-in-blank-input,
                .matching-select,
                .fill-in-blank-select {
                    background: rgba(0, 0, 0, 0.4) !important;
                    border: 1px solid rgba(255, 255, 255, 0.2) !important;
                    color: rgba(255, 255, 255, 0.9) !important;
                    border-radius: 4px;
                    padding: 4px 8px;
                }
                
                .preview-question-wrapper input[type="text"]:focus,
                .preview-question-wrapper select:focus {
                    border-color: #a855f7 !important;
                    outline: none;
                    box-shadow: 0 0 0 2px rgba(168, 85, 247, 0.2);
                }

                .preview-question-wrapper option {
                    background: #1f2937; /* Dark hex for options */
                    color: white;
                }

                /* 4. MCQ/Option Labels */
                .mcq-options label,
                .tfn-options label {
                    color: rgba(255, 255, 255, 0.8) !important;
                    border-color: transparent !important;
                }
                
                .mcq-options label:hover,
                .tfn-options label:hover {
                    background-color: rgba(255, 255, 255, 0.05) !important;
                    border-color: rgba(255, 255, 255, 0.1) !important;
                }

                .mcq-options label:has(input:checked),
                .tfn-options label:has(input:checked) {
                    background-color: rgba(168, 85, 247, 0.2) !important;
                    border-color: #a855f7 !important;
                    color: #e9d5ff !important;
                }
                
                .fill-in-blank-input::placeholder {
                    color: rgba(255, 255, 255, 0.3) !important;
                }

                /* Fix inline input display */
                .fill-in-blank-input {
                    display: inline-block;
                    min-width: 120px;
                    margin: 0 4px;
                }
            `}</style>
        </div>
    );
}
