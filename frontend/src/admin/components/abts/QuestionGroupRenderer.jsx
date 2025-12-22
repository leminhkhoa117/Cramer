/**
 * QuestionGroupRenderer - Renders a group of related questions
 * 
 * V5.0: Uses unified AIStudio.css, no inline styles, no emojis
 * - Shared Context (headings, options, diagrams)
 * - Dark admin theme styling
 * - Uses actual QuestionRenderer for high-fidelity preview
 * 
 * @since 2025-12-22
 */

import React from 'react';
import { FiRefreshCw } from 'react-icons/fi';
import QuestionRenderer from '../../../components/QuestionRenderer';
import { sanitizeHtml } from '../../utils/htmlSanitizer';
import './AIStudio.css';

export default function QuestionGroupRenderer({
    group,
    allQuestions,
    userAnswers = {},
    onAnswerChange = () => { },
    onRegenerateQuestion,
    isGenerating = false,
    regeneratingQuestionId = null
}) {
    const { type, questions, startNum, endNum } = group;

    // Render shared context (images, text, options)
    const renderSharedContext = () => {
        if (!questions || questions.length === 0) return null;
        const firstQ = questions[0];
        const content = firstQ.questionContent || {};

        // 1. IMAGE CONTEXT (Diagrams)
        if (firstQ.imageUrl) {
            return (
                <div className="studio-qgroup__context">
                    <img
                        src={firstQ.imageUrl}
                        alt="Diagram"
                        style={{ maxWidth: '100%', maxHeight: '250px', borderRadius: '4px' }}
                    />
                </div>
            );
        }

        // 2. TEXT CONTEXT (Summaries, Tables, Flowcharts)
        if (['SUMMARY_COMPLETION', 'TABLE_COMPLETION', 'FLOW_CHART_COMPLETION', 'NOTE_COMPLETION'].includes(type)) {
            const textContent = content.text || content.summary || content.body ||
                content.main_statement || content.html || content.table || content.content;
            if (textContent && textContent.length > 0) {
                return (
                    <div className="studio-qgroup__context studio-qgroup__context--text">
                        <div
                            dangerouslySetInnerHTML={{
                                __html: sanitizeHtml(textContent.replace(/____/g, '<span style="display:inline-block;width:60px;border-bottom:1px solid rgba(255,255,255,0.4)">____</span>'))
                            }}
                            style={{ fontSize: '0.9rem', lineHeight: 1.7, color: 'var(--studio-text-primary)' }}
                        />
                    </div>
                );
            }
        }

        // 3. OPTIONS CONTEXT (Matching types)
        if (['MATCHING_HEADINGS', 'MATCHING_FEATURES', 'MATCHING_SENTENCE_ENDINGS',
            'SUMMARY_COMPLETION_OPTIONS', 'MATCHING_INFORMATION', 'MATCHING'].includes(type)) {
            const options = content.options || content.headings || content.items || content.categories;
            if (options && Array.isArray(options) && options.length > 0) {
                return (
                    <div className="studio-qgroup__context">
                        <div className="studio-qgroup__context-title">
                            {type === 'MATCHING_HEADINGS' ? 'List of Headings' : 'Options'}
                        </div>
                        <div className="studio-qgroup__options-grid">
                            {options.map((opt, i) => (
                                <div key={i} className="studio-qgroup__option">
                                    {typeof opt === 'object' ? (
                                        <>
                                            <strong>{opt.letter || opt.id || (i + 1)}.</strong> {opt.text || opt.content}
                                        </>
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

        return null;
    };

    // Get instructions based on type
    const getGroupInstructions = (type) => {
        switch (type) {
            case 'TRUE_FALSE_NOT_GIVEN':
                return 'Do the following statements agree with the information given in the Reading Passage?';
            case 'YES_NO_NOT_GIVEN':
                return 'Do the following statements agree with the claims of the writer in the Reading Passage?';
            case 'MATCHING_HEADINGS':
                return 'Choose the correct heading for each paragraph from the list of headings below.';
            case 'SUMMARY_COMPLETION':
                return 'Complete the summary below. Choose NO MORE THAN TWO WORDS from the passage for each answer.';
            case 'SUMMARY_COMPLETION_OPTIONS':
                return 'Complete the summary below. Choose the correct letter, A, B, C, etc.';
            case 'MULTIPLE_CHOICE':
                return 'Choose the correct letter, A, B, C or D.';
            case 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS':
                return 'Choose TWO letters, A-E.';
            case 'DIAGRAM_LABEL_COMPLETION':
                return 'Label the diagram below.';
            default:
                return 'Answer the questions below.';
        }
    };

    return (
        <div className="studio-qgroup">
            {/* Group Header */}
            <div className="studio-qgroup__header">
                <h5 className="studio-qgroup__title">Questions {startNum}-{endNum}</h5>
                <p className="studio-qgroup__instructions">{getGroupInstructions(type)}</p>
                {renderSharedContext()}
            </div>

            {/* Questions List */}
            <div>
                {questions.map((question, qIndex) => {
                    const absIndex = allQuestions ? allQuestions.findIndex(q => q === question) : -1;
                    const questionNum = question.questionNumber || (absIndex !== -1 ? absIndex + 1 : qIndex + 1);
                    const questionId = absIndex !== -1 ? absIndex : `temp-${qIndex}`;

                    return (
                        <div key={qIndex} className="studio-question">
                            {/* Question Renderer */}
                            <QuestionRenderer
                                question={{ ...question, id: questionId, questionNumber: questionNum }}
                                userAnswer={userAnswers[questionId]}
                                onAnswerChange={onAnswerChange}
                                partId="preview"
                            />

                            {/* Answer Footer */}
                            <div className="studio-question__footer">
                                <div className="studio-question__answer">
                                    <strong>Answer:</strong>
                                    <span className="studio-question__answer-value">
                                        {Array.isArray(question.correctAnswer)
                                            ? question.correctAnswer.join(', ')
                                            : question.correctAnswer}
                                    </span>
                                </div>

                                {question.explanation && (
                                    <div className="studio-question__explanation">
                                        <FiRefreshCw
                                            size={12}
                                            className="studio-question__explanation-icon"
                                            style={{ display: 'inline', marginRight: '4px' }}
                                        />
                                        {question.explanation}
                                    </div>
                                )}

                                {onRegenerateQuestion && (
                                    <button
                                        className="studio-question__regen"
                                        onClick={() => onRegenerateQuestion(questionNum)}
                                        disabled={isGenerating || regeneratingQuestionId === questionNum}
                                    >
                                        <FiRefreshCw size={12} />
                                        {regeneratingQuestionId === questionNum ? 'Regenerating...' : 'Regenerate'}
                                    </button>
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}
