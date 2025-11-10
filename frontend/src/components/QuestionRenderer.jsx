import React from 'react';

const QuestionRenderer = ({ question, onAnswerChange, userAnswer }) => {
    const { id, questionType, questionContent, questionNumber } = question;

    const handleInputChange = (e) => {
        const { value } = e.target;
        onAnswerChange(id, { value });
    };

    // Helper to bold the number within a question's text
    const renderPartWithBoldNumber = (part) => {
        return part.replace(/(\b\d+\b)/g, '<strong>$1</strong>');
    };

    // Renders text with a fill-in-the-blank <input>
    const renderTextWithInput = (text) => {
        const parts = text.split('____');
        return (
            <p className="question-text-interactive">
                {parts.map((part, index) => (
                    <React.Fragment key={index}>
                        <span dangerouslySetInnerHTML={{ __html: renderPartWithBoldNumber(part) }} />
                        {index < parts.length - 1 && (
                            <input
                                type="text"
                                className="fill-in-blank-input"
                                value={userAnswer?.value || ''}
                                onChange={handleInputChange}
                            />
                        )}
                    </React.Fragment>
                ))}
            </p>
        );
    };

    // Renders text with a <select> dropdown in the blank
    const renderTextWithSelect = (text, options) => {
        const parts = text.split('____');
        return (
            <p className="question-text-interactive">
                {parts.map((part, index) => (
                    <React.Fragment key={index}>
                        <span dangerouslySetInnerHTML={{ __html: renderPartWithBoldNumber(part) }} />
                        {index < parts.length - 1 && (
                            <select value={userAnswer?.value || ''} onChange={handleInputChange} className="fill-in-blank-select">
                                <option value="">Select...</option>
                                {options.map(opt => <option key={opt} value={opt}>{opt}</option>)}
                            </select>
                        )}
                    </React.Fragment>
                ))}
            </p>
        );
    };

    // Renders a multiple choice option with bolded letter and period
    const renderMcqOption = (optionText) => {
        const letter = optionText.charAt(0);
        const rest = optionText.substring(1).trim();
        return (
            <>
                <strong>{letter}.</strong> {rest}
            </>
        );
    };

    const renderQuestion = () => {
        const currentAnswer = userAnswer?.value || '';

        switch (questionType) {
            case 'FILL_IN_BLANK':
            case 'SUMMARY_COMPLETION':
                return renderTextWithInput(questionContent.text);

            case 'TRUE_FALSE_NOT_GIVEN':
                return (
                    <div>
                        <p>{questionContent.text}</p>
                        <div className="tfn-options">
                            <label><input type="radio" name={`q_${id}`} value="TRUE" checked={currentAnswer === 'TRUE'} onChange={handleInputChange} /> True</label>
                            <label><input type="radio" name={`q_${id}`} value="FALSE" checked={currentAnswer === 'FALSE'} onChange={handleInputChange} /> False</label>
                            <label><input type="radio" name={`q_${id}`} value="NOT_GIVEN" checked={currentAnswer === 'NOT_GIVEN'} onChange={handleInputChange} /> Not Given</label>
                        </div>
                    </div>
                );
            
            case 'YES_NO_NOT_GIVEN':
                return (
                    <div>
                        <p>{questionContent.text}</p>
                        <div className="tfn-options">
                            <label><input type="radio" name={`q_${id}`} value="YES" checked={currentAnswer === 'YES'} onChange={handleInputChange} /> Yes</label>
                            <label><input type="radio" name={`q_${id}`} value="NO" checked={currentAnswer === 'NO'} onChange={handleInputChange} /> No</label>
                            <label><input type="radio" name={`q_${id}`} value="NOT_GIVEN" checked={currentAnswer === 'NOT_GIVEN'} onChange={handleInputChange} /> Not Given</label>
                        </div>
                    </div>
                );

            case 'MATCHING_INFORMATION':
                return (
                    <div>
                        <p>{questionContent.text}</p>
                        <select value={currentAnswer} onChange={handleInputChange} className="matching-select">
                            <option value="">Select Paragraph...</option>
                            {questionContent.options.map(opt => <option key={opt} value={opt}>{opt}</option>)}
                        </select>
                    </div>
                );

            case 'MULTIPLE_CHOICE':
            case 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS':
                return (
                    <div>
                        <p>{questionContent.text}</p>
                        <div className="mcq-options">
                            {questionContent.options.map((opt, index) => (
                                <label key={index}>
                                    <input type="radio" name={`q_${id}`} value={opt.charAt(0)} checked={currentAnswer === opt.charAt(0)} onChange={handleInputChange} />
                                    {renderMcqOption(opt)}
                                </label>
                            ))}
                        </div>
                    </div>
                );

            case 'SUMMARY_COMPLETION_OPTIONS':
                return renderTextWithSelect(questionContent.text, questionContent.options);

            default:
                return <pre>{JSON.stringify(questionContent, null, 2)}</pre>;
        }
    };

    return (
        <div className="question-block">
            {renderQuestion()}
        </div>
    );
};

export default QuestionRenderer;
