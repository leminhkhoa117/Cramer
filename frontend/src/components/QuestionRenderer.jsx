import React from 'react';
import '../css/QuestionRenderer.css';

const QuestionRenderer = ({ question, onAnswerChange, userAnswer, typeOverride, groupOptions }) => {
    const { id, questionType, questionContent, questionNumber } = question;

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
        const parts = text.split(/____/g);
        return (
            <p className="question-text-interactive">
                <span dangerouslySetInnerHTML={{ __html: parts[0].replace(/(\b\d+\b)/g, '<strong>$1</strong>') }} />
                {parts.length > 1 && (
                    <input
                        type="text"
                        className="fill-in-blank-input"
                        value={userAnswer || ''}
                        onChange={handleSingleValueChange}
                    />
                )}
                {parts.length > 1 && parts[1] && <span dangerouslySetInnerHTML={{ __html: parts[1] }} />}
            </p>
        );
    };

    const renderTextWithSelect = (text, options) => {
        const parts = text.split('____');
        return (
            <p className="question-text-interactive">
                <span dangerouslySetInnerHTML={{ __html: parts[0].replace(/(\b\d+\b)/g, '<strong>$1</strong>') }} />
                {parts.length > 1 && (
                    <select value={userAnswer || ''} onChange={handleSingleValueChange} className="fill-in-blank-select">
                        <option value="">Select...</option>
                        {options.map((opt) => (
                            <option key={opt.letter} value={opt.letter}>{opt.letter}. {opt.text}</option>
                        ))}
                    </select>
                )}
                {parts.length > 1 && parts[1] && <span dangerouslySetInnerHTML={{ __html: parts[1] }} />}
            </p>
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
                return renderTextWithInput(questionContent.text);

            case 'MATCHING':
                return (
                    <div className="matching-question-container">
                        <p><span className="question-number">{questionNumber}.</span> {questionContent.text}</p>
                        <select value={userAnswer || ''} onChange={handleSingleValueChange} className="matching-select">
                            <option value="">Select...</option>
                            {groupOptions && groupOptions.map(opt => (
                                <option key={opt.letter} value={opt.letter}>{opt.letter}. {opt.text}</option>
                            ))}
                        </select>
                    </div>
                );

            case 'MULTIPLE_CHOICE':
                return (
                    <div>
                        <p><span className="question-number">{questionNumber}.</span> {questionContent.text}</p>
                        <div className="mcq-options">
                            {questionContent.options.map((opt, index) => (
                                <label key={index}>
                                    <input type="radio" name={`q_${id}`} value={opt.charAt(0)} checked={userAnswer === opt.charAt(0)} onChange={handleSingleValueChange} />
                                    <strong>{opt.charAt(0)}.</strong> {opt.substring(1).trim()}
                                </label>
                            ))}
                        </div>
                    </div>
                );
            
            case 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS':
                const currentAnswers = userAnswer || [];
                return (
                    <div>
                        <p><span className="question-number">{questionNumber}.</span> {questionContent.text}</p>
                        <div className="mcq-options">
                            {questionContent.options.map((opt, index) => (
                                <label key={index}>
                                    <input type="checkbox" name={`q_${id}`} value={opt.charAt(0)} checked={currentAnswers.includes(opt.charAt(0))} onChange={handleMultiValueChange} />
                                    <strong>{opt.charAt(0)}.</strong> {opt.substring(1).trim()}
                                </label>
                            ))}
                        </div>
                    </div>
                );

            default:
                // Fallback for old Reading types
                switch(questionType) {
                    case 'TRUE_FALSE_NOT_GIVEN':
                        return (
                            <div>
                                <p><span className="question-number">{questionNumber}.</span> {questionContent.text}</p>
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
                                <p><span className="question-number">{questionNumber}.</span> {questionContent.text}</p>
                                <div className="tfn-options">
                                    <label><input type="radio" name={`q_${id}`} value="YES" checked={userAnswer === 'YES'} onChange={handleSingleValueChange} /> Yes</label>
                                    <label><input type="radio" name={`q_${id}`} value="NO" checked={userAnswer === 'NO'} onChange={handleSingleValueChange} /> No</label>
                                    <label><input type="radio" name={`q_${id}`} value="NOT_GIVEN" checked={userAnswer === 'NOT_GIVEN'} onChange={handleSingleValueChange} /> Not Given</label>
                                </div>
                            </div>
                        );
                    case 'SUMMARY_COMPLETION':
                        return renderTextWithInput(questionContent.text);
                    case 'MATCHING_INFORMATION':
                    case 'MATCHING_FEATURES':
                    case 'MATCHING_HEADINGS':
                        const options = questionContent.options || [];
                        return (
                            <div>
                                <p><span className="question-number">{questionNumber}.</span> {questionContent.text}</p>
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
                        return renderTextWithSelect(questionContent.text, questionContent.options);
                    case 'TABLE_COMPLETION':
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
