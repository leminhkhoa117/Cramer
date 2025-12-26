import React from 'react';
import HighlightableHtmlContent from './HighlightableHtmlContent'; // Import the new component
import '../css/question-renderer.css';

const QuestionRenderer = ({ question, onAnswerChange, userAnswer, typeOverride, groupOptions, partId, groupedQuestions = [], groupAnswers = {} }) => {
    const { id, questionType, questionContent, questionNumber } = question;

    // Create a unique content ID prefix that includes partId to prevent cross-part collisions
    const contentIdPrefix = partId ? `p${partId}-q${id}` : `q${id}`;

    // Helper to extract text from various backend field names
    const getQuestionText = () => {
        if (!questionContent) return '';
        if (typeof questionContent === 'string') return questionContent;

        return questionContent.text ||
            questionContent.statement ||
            questionContent.question ||
            questionContent.sentence ||
            questionContent.incomplete_sentence ||
            questionContent.prompt ||
            questionContent.item ||
            questionContent.paragraph ||
            questionContent.heading ||
            '';
    };

    const questionText = getQuestionText();

    const handleSingleValueChange = (e) => {
        onAnswerChange(id, e.target.value);
    };

    const handleMultiValueChange = (e) => {
        const { value, checked } = e.target;
        const currentAnswers = userAnswer ? [...userAnswer] : [];
        if (checked) {
            onAnswerChange(id, [...currentAnswers, value].sort());
        } else {
            onAnswerChange(id, currentAnswers.filter(ans => ans !== value));
        }
    };

    const renderTextWithInput = (text) => {
        if (!text) return null;
        const parts = text.split(/____/g);

        return (
            <div className="question-text-interactive">
                {parts.map((part, index) => {
                    const isLast = index === parts.length - 1;
                    let targetQId = null;
                    let currentValue = '';
                    let showInput = !isLast;

                    if (!isLast) {
                        if (index === 0) {
                            targetQId = id;
                            currentValue = typeof userAnswer !== 'undefined' ? userAnswer : '';
                        } else {
                            const subQ = groupedQuestions && groupedQuestions[index - 1];
                            if (subQ) {
                                targetQId = subQ.id !== undefined ? subQ.id : `temp-sub-${index}`;
                                currentValue = groupAnswers && groupAnswers[targetQId] !== undefined ? groupAnswers[targetQId] : '';
                            } else {
                                showInput = false;
                            }
                        }
                    }

                    return (
                        <React.Fragment key={index}>
                            <HighlightableHtmlContent
                                htmlString={part.replace(/(\b\d+\b)/g, '<strong>$1</strong>')}
                                contentId={`${contentIdPrefix}-part${index}`}
                            />
                            {showInput && (
                                <input
                                    type="text"
                                    className="fill-in-blank-input"
                                    value={currentValue}
                                    onChange={(e) => onAnswerChange(targetQId, e.target.value)}
                                    placeholder={index === 0 ? (questionNumber ? `${questionNumber}` : '') : (groupedQuestions?.[index - 1]?.questionNumber ? `${groupedQuestions[index - 1].questionNumber}` : '')}
                                />
                            )}
                        </React.Fragment>
                    );
                })}
            </div>
        );
    };

    const renderTextWithSelect = (text, options) => {
        if (!text) return null;
        // Safety check: if options is not an array, use empty array or show text input instead
        const safeOptions = Array.isArray(options) ? options : [];
        const parts = text.split(/____/g);

        return (
            <div className="question-text-interactive">
                {parts.map((part, index) => {
                    const isLast = index === parts.length - 1;

                    let targetQId = null;
                    let currentValue = '';
                    let showInput = !isLast;

                    if (!isLast) {
                        if (index === 0) {
                            targetQId = id;
                            currentValue = userAnswer || '';
                        } else {
                            const subQ = groupedQuestions && groupedQuestions[index - 1];
                            if (subQ) {
                                targetQId = subQ.id !== undefined ? subQ.id : `temp-sub-${index}`;
                                currentValue = groupAnswers && groupAnswers[targetQId] !== undefined ? groupAnswers[targetQId] : '';
                            } else {
                                showInput = false;
                            }
                        }
                    }

                    return (
                        <React.Fragment key={index}>
                            <HighlightableHtmlContent
                                htmlString={part.replace(/(\b\d+\b)/g, '<strong>$1</strong>')}
                                contentId={`${contentIdPrefix}-part${index}`}
                            />
                            {showInput && safeOptions.length > 0 && (
                                <select
                                    value={currentValue}
                                    onChange={(e) => onAnswerChange(targetQId, e.target.value)}
                                    className="fill-in-blank-select"
                                >
                                    <option value="">Select...</option>
                                    {safeOptions.map((opt) => (
                                        <option key={opt.letter} value={opt.letter}>{opt.letter}. {opt.text.replace(/^[A-Z]\.?\s*/, '')}</option>
                                    ))}
                                </select>
                            )}
                            {showInput && safeOptions.length === 0 && (
                                <input
                                    type="text"
                                    className="fill-in-blank-input"
                                    value={currentValue}
                                    onChange={(e) => onAnswerChange(targetQId, e.target.value)}
                                    placeholder="Type answer..."
                                />
                            )}
                        </React.Fragment>
                    );
                })}
            </div>
        );
    };

    const renderQuestion = () => {
        const effectiveType = typeOverride || questionType;

        switch (effectiveType) {
            case 'FILL_IN_BLANK_INPUT_ONLY':
                return (
                    <input
                        type="text"
                        className="fill-in-blank-input"
                        value={userAnswer || ''}
                        onChange={handleSingleValueChange}
                    />
                );

            case 'FILL_IN_BLANK':
                return renderTextWithInput(questionText);

            case 'MATCHING':
                return (
                    <div className="matching-question-container">
                        <p><span className="question-number">{questionNumber}.</span> <HighlightableHtmlContent htmlString={questionText} contentId={`${contentIdPrefix}-text`} /></p>
                        <select value={userAnswer || ''} onChange={handleSingleValueChange} className="matching-select">
                            <option value="">Select...</option>
                            {Array.isArray(groupOptions) && groupOptions.map((opt, index) => {
                                const isObject = typeof opt === 'object' && opt !== null;
                                const letter = isObject ? opt.letter : String(opt).charAt(0);
                                const text = isObject ? opt.text : String(opt);
                                const value = letter || text || String(index);
                                return (
                                    <option key={value} value={value}>
                                        {letter ? `${letter}. ` : ''}{text}
                                    </option>
                                );
                            })}
                        </select>
                    </div>
                );

            case 'MULTIPLE_CHOICE':
                return (
                    <div>
                        <p><span className="question-number">{questionNumber}.</span> <HighlightableHtmlContent htmlString={questionText} contentId={`${contentIdPrefix}-text`} /></p>
                        <div className="mcq-options">
                            {questionContent.options && questionContent.options.map((opt, index) => {
                                const letter = String.fromCharCode(65 + index);
                                const text = opt.replace(/^[A-Z]\.?\s*/, '');
                                return (
                                    <label key={index}>
                                        <input type="radio" name={`q_${id}`} value={letter} checked={userAnswer === letter} onChange={handleSingleValueChange} />
                                        <strong>{letter}.</strong> {text}
                                    </label>
                                );
                            })}
                        </div>
                    </div>
                );

            case 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS':
                const currentAnswers = userAnswer || [];
                return (
                    <div>
                        <p><span className="question-number">{questionNumber}.</span> <HighlightableHtmlContent htmlString={questionText} contentId={`${contentIdPrefix}-text`} /></p>
                        <div className="mcq-options">
                            {questionContent.options && questionContent.options.map((opt, index) => {
                                const letter = String.fromCharCode(65 + index);
                                const text = opt.replace(/^[A-Z]\.?\s*/, '');
                                return (
                                    <label key={index}>
                                        <input type="checkbox" name={`q_${id}`} value={letter} checked={currentAnswers.includes(letter)} onChange={handleMultiValueChange} />
                                        <strong>{letter}.</strong> {text}
                                    </label>
                                );
                            })}
                        </div>
                    </div>
                );

            default:
                // Fallback for old Reading types
                switch (questionType) {
                    case 'TRUE_FALSE_NOT_GIVEN':
                        return (
                            <div>
                                <p><span className="question-number">{questionNumber}.</span> <HighlightableHtmlContent htmlString={questionText} contentId={`${contentIdPrefix}-text`} /></p>
                                <div className="tfn-options">
                                    <label><input type="radio" name={`q_${id}`} value="TRUE" checked={userAnswer === 'TRUE'} onChange={handleSingleValueChange} /> True</label>
                                    <label><input type="radio" name={`q_${id}`} value="FALSE" checked={userAnswer === 'FALSE'} onChange={handleSingleValueChange} /> False</label>
                                    <label><input type="radio" name={`q_${id}`} value="NOT_GIVEN" checked={userAnswer === 'NOT_GIVEN'} onChange={handleSingleValueChange} /> Not Given</label>
                                </div>
                            </div>
                        );
                    case 'YES_NO_NOT_GIVEN':
                        return (
                            <div>
                                <p><span className="question-number">{questionNumber}.</span> <HighlightableHtmlContent htmlString={questionText} contentId={`${contentIdPrefix}-text`} /></p>
                                <div className="tfn-options">
                                    <label><input type="radio" name={`q_${id}`} value="YES" checked={userAnswer === 'YES'} onChange={handleSingleValueChange} /> Yes</label>
                                    <label><input type="radio" name={`q_${id}`} value="NO" checked={userAnswer === 'NO'} onChange={handleSingleValueChange} /> No</label>
                                    <label><input type="radio" name={`q_${id}`} value="NOT_GIVEN" checked={userAnswer === 'NOT_GIVEN'} onChange={handleSingleValueChange} /> Not Given</label>
                                </div>
                            </div>
                        );
                    case 'SUMMARY_COMPLETION':
                        return renderTextWithInput(questionText);
                    case 'MATCHING_INFORMATION':
                    case 'MATCHING_FEATURES':
                    case 'MATCHING_HEADINGS':
                    case 'MATCHING_SENTENCE_ENDINGS':
                        const options = questionContent.options || [];
                        return (
                            <div>
                                <p><span className="question-number">{questionNumber}.</span> <HighlightableHtmlContent htmlString={questionText} contentId={`${contentIdPrefix}-text`} /></p>
                                <select value={userAnswer || ''} onChange={handleSingleValueChange} className="matching-select">
                                    <option value="">Select...</option>
                                    {options.map((opt, index) => {
                                        if (typeof opt === 'object' && opt !== null) {
                                            return <option key={opt.letter || index} value={opt.letter}>{`${opt.letter}. ${opt.text}`}</option>;
                                        }
                                        return <option key={index} value={opt}>{opt}</option>;
                                    })}
                                </select>
                            </div>
                        );
                    case 'SUMMARY_COMPLETION_OPTIONS':
                        return renderTextWithSelect(questionText, questionContent.options);
                    case 'TABLE_COMPLETION':
                    case 'FLOW_CHART_COMPLETION':
                    case 'DIAGRAM_LABEL_COMPLETION':
                    case 'NOTE_COMPLETION':
                        return (
                            <p className="question-text-interactive">
                                <span className="question-number"><strong>{questionNumber}</strong></span>
                                <input
                                    type="text"
                                    className="fill-in-blank-input"
                                    value={userAnswer || ''}
                                    onChange={handleSingleValueChange}
                                />
                            </p>
                        );
                    default:
                        return <p>Unsupported question type: {questionType}</p>;
                }
        }
    };

    const effectiveType = typeOverride || questionType;

    // For inline inputs, we must return the raw input without the wrapper div.
    if (effectiveType === 'FILL_IN_BLANK_INPUT_ONLY') {
        return renderQuestion();
    }

    return (
        <div className="question-block">
            {renderQuestion()}
        </div>
    );
};

export default QuestionRenderer;
