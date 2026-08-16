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

import { FiRefreshCw } from 'react-icons/fi';
import QuestionRenderer from '../../../components/QuestionRenderer';
import { sanitizeHtml } from '../../utils/htmlSanitizer';
import DiagramUploadPanel from './DiagramUploadPanel';
import './AIStudio.css';

export default function QuestionGroupRenderer({
    group,
    allQuestions,
    userAnswers = {},
    onAnswerChange = () => { },
    onRegenerateQuestion,
    isGenerating = false,
    regeneratingQuestionId = null,
    questionIssues = null
}) {
    // Defensive: ensure group has required properties
    if (!group || typeof group !== 'object') {
        return <div className="studio-qgroup">Invalid question group data</div>;
    }

    const { type = 'UNKNOWN', questions = [], startNum = 1, endNum = 1 } = group;
    const blockContent = group.blockContent || group.content || {};

    // Defensive: ensure questions is an array
    const validQuestions = Array.isArray(questions) ? questions.filter(q => q && typeof q === 'object') : [];

    const resolveWordLimit = () => {
        if (!validQuestions || validQuestions.length === 0) return null;
        const match = validQuestions.find(q => q.wordLimit || q.word_limit);
        const limit = match?.wordLimit || match?.word_limit || null;
        return limit ? String(limit).toUpperCase() : null;
    };
    const wordLimit = resolveWordLimit();
    const hasBlockInstructions = Boolean(blockContent.instructions_text);

    // Render shared context (images, text, options)
    const renderSharedContext = () => {
        if (!validQuestions || validQuestions.length === 0) return null;
        const firstQ = validQuestions[0];
        const content = firstQ?.questionContent || {};

        if (blockContent.title || blockContent.main_title || blockContent.instructions_text) {
            return (
                <div className="studio-qgroup__context">
                    {blockContent.title && <div className="studio-qgroup__context-title">{blockContent.title}</div>}
                    {blockContent.main_title && <div className="studio-qgroup__context-title">{blockContent.main_title}</div>}
                    {blockContent.instructions_text && (
                        <div
                            className="studio-qgroup__context-text"
                            dangerouslySetInnerHTML={{ __html: sanitizeHtml(blockContent.instructions_text) }}
                        />
                    )}
                </div>
            );
        }

        const blockImageUrl = blockContent.image_url || blockContent.imageUrl;
        if (blockImageUrl) {
            return (
                <div className="studio-qgroup__context">
                    <img
                        src={blockImageUrl}
                        alt="Diagram"
                        style={{ maxWidth: '100%', maxHeight: '250px', borderRadius: '4px' }}
                    />
                </div>
            );
        }

        if (blockContent.options && Array.isArray(blockContent.options) && blockContent.options.length > 0) {
            return (
                <div className="studio-qgroup__context">
                    <div className="studio-qgroup__context-title">Options</div>
                    <div className="studio-qgroup__options-grid">
                        {blockContent.options.map((opt, i) => (
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

        // 2. TEXT CONTEXT (Consolidated into Question 1 for interactive rendering)
        // We no longer render static text/table headers for completion types 
        // because the first question now contains the interactive HTML.
        if (['SUMMARY_COMPLETION', 'TABLE_COMPLETION', 'FLOW_CHART_COMPLETION', 'NOTE_COMPLETION', 'FILL_IN_BLANK', 'DIAGRAM_LABEL_COMPLETION', 'SUMMARY_COMPLETION_OPTIONS'].includes(type)) {
            return null;
        }

        // 3. OPTIONS CONTEXT (Matching types)
        if (['MATCHING_HEADINGS', 'MATCHING_FEATURES', 'MATCHING_SENTENCE_ENDINGS',
            'SUMMARY_COMPLETION_OPTIONS', 'MATCHING_INFORMATION', 'MATCHING'].includes(type)) {
            const options = content.options || content.headings || content.items || content.categories;
            if (options && Array.isArray(options) && options.length > 0) {
                return (
                    <div className="studio-qgroup__context">
                        <div className="studio-qgroup__context-title">
                            {type === 'MATCHING_HEADINGS' ? 'List of Headings' :
                                type === 'MATCHING_SENTENCE_ENDINGS' ? 'Sentence Endings' : 'Options'}
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
        const limitText = wordLimit ? ` ${wordLimit}` : ' NO MORE THAN TWO WORDS';
        switch (type) {
            case 'TRUE_FALSE_NOT_GIVEN':
                return 'Do the following statements agree with the information given in the Reading Passage?';
            case 'YES_NO_NOT_GIVEN':
                return 'Do the following statements agree with the claims of the writer in the Reading Passage?';
            case 'MATCHING_HEADINGS':
                return 'Choose the correct heading for each paragraph from the list of headings below.';
            case 'SUMMARY_COMPLETION':
                return `Complete the summary below. Choose${limitText} from the passage for each answer.`;
            case 'SUMMARY_COMPLETION_OPTIONS':
                return 'Complete the summary below. Choose the correct letter, A, B, C, etc.';
            case 'MULTIPLE_CHOICE':
                return 'Choose the correct letter, A, B, C or D.';
            case 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS':
                return 'Choose TWO letters, A-E.';
            case 'DIAGRAM_LABEL_COMPLETION':
                return 'Label the diagram below.';
            case 'NOTE_COMPLETION':
            case 'TABLE_COMPLETION':
            case 'FLOW_CHART_COMPLETION':
            case 'FILL_IN_BLANK':
                return `Complete the notes below. Write${limitText}.`;
            case 'MATCHING_FEATURES':
                return 'Match each statement with the correct person or category.';
            case 'MATCHING_SENTENCE_ENDINGS':
                return 'Complete each sentence with the correct ending from the list below.';
            case 'MATCHING_INFORMATION':
                return 'Which paragraph contains the following information? You may use any letter more than once.';
            case 'MATCHING':
                return 'Match each item to the correct option from the list.';
            case 'PLAN_MAP_DIAGRAM_LABELING':
                return 'Label the map or plan using the options provided.';
            default:
                return 'Answer the questions below.';
        }
    };

    const groupInstructions = hasBlockInstructions ? null : getGroupInstructions(type);

    // Check if this is a BLOCK completion type where Q1 contains all content
    // ONLY TABLE_COMPLETION and FLOW_CHART_COMPLETION use Block style (Q1 has HTML, Q2+ empty)
    // SUMMARY_COMPLETION, FILL_IN_BLANK, NOTE_COMPLETION use Inline style (each Q has its own text)
    const isBlockCompletionType = [
        'TABLE_COMPLETION', 'FLOW_CHART_COMPLETION'
    ].includes(type);

    // Inline completion types render each question individually
    const isInlineCompletionType = [
        'SUMMARY_COMPLETION', 'FILL_IN_BLANK', 'NOTE_COMPLETION',
        'DIAGRAM_LABEL_COMPLETION', 'SUMMARY_COMPLETION_OPTIONS'
    ].includes(type);

    // Check if this is a DIAGRAM_LABEL_COMPLETION group (requires image upload)
    const isDiagramLabelCompletion = type === 'DIAGRAM_LABEL_COMPLETION';

    // Get answer for display - RESILIENT approach
    // Handles any format the AI produces without complex pattern-matching
    const getQuestionAnswer = (question) => {
        if (!question.correctAnswer) return 'N/A';

        if (Array.isArray(question.correctAnswer)) {
            // If single element, show just that
            if (question.correctAnswer.length === 1) {
                return question.correctAnswer[0];
            }
            // Multiple elements - show all (valid for some question types)
            return question.correctAnswer.join(', ');
        }
        return String(question.correctAnswer);
    };

    // Get explanation for display - RESILIENT approach
    // Handle both string and object formats (object has {quote, detail, strategy})
    const getQuestionExplanation = (question) => {
        if (!question.explanation) return null;
        if (typeof question.explanation === 'string') {
            return question.explanation;
        }
        // Object format - prioritize detail, then strategy, then quote
        const exp = question.explanation;
        return exp.detail || exp.strategy || exp.quote || JSON.stringify(exp);
    };

    // Render a simple completion question (just the number + blank indicator)
    const renderCompletionQuestion = (question, questionNum) => {
        // For completion types where context is shared, just show question number
        return (
            <div className="studio-question__completion-item">
                <span className="studio-question__number">{questionNum}.</span>
                <span className="studio-question__blank">
                    <input
                        type="text"
                        className="studio-question__input"
                        placeholder="Your answer"
                        readOnly
                    />
                </span>
            </div>
        );
    };

    return (
        <div className="studio-qgroup">
            {/* Group Header */}
            <div className="studio-qgroup__header">
                <h5 className="studio-qgroup__title">Questions {startNum}-{endNum}</h5>
                {groupInstructions && (
                    <p className="studio-qgroup__instructions">{groupInstructions}</p>
                )}
                {renderSharedContext()}
            </div>

            {/* Diagram Upload Panel for DIAGRAM_LABEL_COMPLETION */}
            {isDiagramLabelCompletion && validQuestions.length > 0 && (
                <DiagramUploadPanel
                    questions={validQuestions}
                    startNum={startNum}
                    endNum={endNum}
                />
            )}

            {/* Questions List */}
            <div className="studio-qgroup__questions">
                {(() => {
                    // BLOCK STRATEGY for TABLE_COMPLETION and FLOW_CHART_COMPLETION ONLY
                    // These types have Q1 with full HTML content, Q2+ are empty
                    if (isBlockCompletionType && validQuestions.length > 0) {
                        const masterQuestion = validQuestions[0];
                        const subQuestions = validQuestions.slice(1);

                        // Calculate IDs for the master question interactively
                        const absIndex = allQuestions ? allQuestions.findIndex(q => q === masterQuestion) : -1;
                        const questionNum = masterQuestion.questionNumber || (absIndex !== -1 ? absIndex + 1 : 1);
                        const questionId = absIndex !== -1 ? absIndex : `temp-0`;

                        // Fix subQuestions IDs for binding (needed for QuestionRenderer logic)
                        const preparedSubQuestions = subQuestions.map((sq, idx) => {
                            const sqAbsIndex = allQuestions ? allQuestions.findIndex(q => q === sq) : -1;
                            return {
                                ...sq,
                                id: sqAbsIndex !== -1 ? sqAbsIndex : `temp-${idx + 1}`,
                                questionNumber: sq.questionNumber || (sqAbsIndex !== -1 ? sqAbsIndex + 1 : questionNum + idx + 1)
                            };
                        });

                        return (
                            <div className="studio-question studio-question--master">
                                <QuestionRenderer
                                    question={{ ...masterQuestion, id: questionId, questionNumber: questionNum }}
                                    userAnswer={userAnswers[questionId]}
                                    onAnswerChange={onAnswerChange}
                                    partId="preview"
                                    groupOptions={blockContent.options}
                                    groupedQuestions={preparedSubQuestions}
                                    groupAnswers={userAnswers}
                                />

                                {/* Render Footers for ALL questions in the group (Answers/Explanations) */}
                                <div className="studio-question__master-footers">
                                    {validQuestions.map((q, idx) => {
                                        const qAbsIndex = allQuestions ? allQuestions.findIndex(obj => obj === q) : -1;
                                        const qId = qAbsIndex !== -1 ? qAbsIndex : `temp-${idx}`;
                                        const qNum = q.questionNumber || (qAbsIndex !== -1 ? qAbsIndex + 1 : questionNum + idx);
                                        const issues = questionIssues?.get?.(qNum) || [];

                                        return (
                                            <div key={idx} className="studio-question__footer-item">
                                                <div className="studio-question__answer">
                                                    <strong>{qNum}. Answer:</strong>
                                                    <span className="studio-question__answer-value">
                                                        {getQuestionAnswer(q)}
                                                    </span>
                                                </div>
                                                {(() => {
                                                    const explanation = getQuestionExplanation(q);
                                                    if (!explanation) return null;

                                                    return (
                                                        <div
                                                            className="studio-question__explanation"
                                                            dangerouslySetInnerHTML={{ __html: explanation }}
                                                        />
                                                    );
                                                })()}
                                                {/* Regeneration for individual questions */}
                                                {onRegenerateQuestion && (
                                                    <div style={{ marginTop: '4px' }}>
                                                        <button
                                                            className="studio-question__regen"
                                                            onClick={() => onRegenerateQuestion(qNum)}
                                                            disabled={isGenerating || regeneratingQuestionId === qNum}
                                                        >
                                                            <FiRefreshCw size={12} />
                                                            {regeneratingQuestionId === qNum ? '...' : 'Regen'}
                                                        </button>
                                                    </div>
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        );
                    }

                    // STANDARD STRATEGY for other types (MCQ, Matching)
                    return validQuestions.map((question, qIndex) => {
                        if (!question) return null;
                        const absIndex = allQuestions ? allQuestions.findIndex(q => q === question) : -1;
                        const questionNum = question.questionNumber || (absIndex !== -1 ? absIndex + 1 : qIndex + 1);
                        const questionId = absIndex !== -1 ? absIndex : `temp-${qIndex}`;
                        const issues = [...new Set(questionIssues?.get?.(questionNum) || [])];

                        return (
                            <div key={qIndex} className="studio-question">
                                <QuestionRenderer
                                    question={{ ...question, id: questionId, questionNumber: questionNum }}
                                    userAnswer={userAnswers[questionId]}
                                    onAnswerChange={onAnswerChange}
                                    partId="preview"
                                    groupOptions={blockContent.options}
                                />
                                <div className="studio-question__footer">
                                    <div className="studio-question__answer">
                                        <strong>Answer:</strong>
                                        <span className="studio-question__answer-value">
                                            {getQuestionAnswer(question)}
                                        </span>
                                    </div>
                                    {(() => {
                                        const explanation = getQuestionExplanation(question);
                                        if (!explanation) return null;

                                        return (
                                            <div
                                                className="studio-question__explanation"
                                                style={{
                                                    fontSize: '0.95rem',
                                                    lineHeight: '1.6',
                                                    marginTop: '8px'
                                                }}
                                                dangerouslySetInnerHTML={{ __html: explanation }}
                                            />
                                        );
                                    })()}
                                    {issues.length > 0 && (
                                        <div className="studio-question__issues">
                                            {issues.map((issue, idx) => (
                                                <div key={idx} className="studio-question__issue">{issue}</div>
                                            ))}
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
                    });
                })()}
            </div>
        </div>
    );
}
