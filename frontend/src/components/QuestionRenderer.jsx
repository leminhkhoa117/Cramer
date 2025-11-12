import React from 'react';

const QuestionRenderer = ({ question, onAnswerChange, userAnswer, typeOverride, groupOptions, wrapperTag = 'div' }) => {
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
        const TextWrapper = wrapperTag === 'span' ? 'span' : 'p'; // Use span for inline, p for block
        return (
            <TextWrapper className="question-text-interactive">
                <span dangerouslySetInnerHTML={{ __html: renderPartWithBoldNumber(parts[0]) }} />
                {parts.length > 1 && (
                    <input
                        type="text"
                        className="fill-in-blank-input"
                        value={userAnswer?.value || ''}
                        onChange={handleInputChange}
                    />
                )}
                {parts.length > 1 && <span dangerouslySetInnerHTML={{ __html: renderPartWithBoldNumber(parts[1]) }} />}
            </TextWrapper>
        );
    };
    
    const renderInputOnly = () => {
        return (
             <input
                type="text"
                className="fill-in-blank-input"
                value={userAnswer?.value || ''}
                onChange={handleInputChange}
            />
        )
    }

    // Renders text with a <select> dropdown in the blank
    const renderTextWithSelect = (text, options) => {
        const parts = text.split('____');
        const TextWrapper = wrapperTag === 'span' ? 'span' : 'p'; // Use span for inline, p for block
        return (
            <TextWrapper className="question-text-interactive">
                <span dangerouslySetInnerHTML={{ __html: renderPartWithBoldNumber(parts[0]) }} />
                {parts.length > 1 && (
                    <select value={userAnswer?.value || ''} onChange={handleInputChange} className="fill-in-blank-select">
                        <option value="">Select...</option>
                        {options.map((opt, index) => {
                            // Check if opt is an object (for SUMMARY_COMPLETION_OPTIONS) or a string (for other matching types)
                            const letter = typeof opt === 'object' ? opt.letter : opt.charAt(0);
                            const displayText = typeof opt === 'object' ? `${opt.letter}. ${opt.text}` : opt;
                            return <option key={letter || index} value={letter}>{displayText}</option>;
                        })}
                    </select>
                )}
                {parts.length > 1 && <span dangerouslySetInnerHTML={{ __html: renderPartWithBoldNumber(parts[1]) }} />}
            </TextWrapper>
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
        const effectiveType = typeOverride || questionType;
        const currentAnswer = userAnswer?.value || '';

        switch (effectiveType) {
            case 'FILL_IN_BLANK_INPUT_ONLY':
                return renderInputOnly();

            case 'FILL_IN_BLANK':
            case 'SUMMARY_COMPLETION':
                return renderTextWithInput(questionContent.text);

            case 'TRUE_FALSE_NOT_GIVEN':
                return (
                    <div>
                        <p><span className="question-number">{questionNumber}.</span> {questionContent.text}</p>
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
                        <p><span className="question-number">{questionNumber}.</span> {questionContent.text}</p>
                        <div className="tfn-options">
                            <label><input type="radio" name={`q_${id}`} value="YES" checked={currentAnswer === 'YES'} onChange={handleInputChange} /> Yes</label>
                            <label><input type="radio" name={`q_${id}`} value="NO" checked={currentAnswer === 'NO'} onChange={handleInputChange} /> No</label>
                            <label><input type="radio" name={`q_${id}`} value="NOT_GIVEN" checked={currentAnswer === 'NOT_GIVEN'} onChange={handleInputChange} /> Not Given</label>
                        </div>
                    </div>
                );

            case 'MATCHING_INFORMATION':
            case 'MATCHING_HEADINGS':
            case 'MATCHING_FEATURES':
            case 'MATCHING_SENTENCE_ENDINGS':
                const options = groupOptions || questionContent.options || [];
                return (
                    <div>
                        <p><span className="question-number">{questionNumber}.</span> {questionContent.text}</p>
                        <select value={currentAnswer} onChange={handleInputChange} className="matching-select">
                            <option value="">Select...</option>
                            {options.map(opt => <option key={opt.letter || opt} value={opt.letter || opt}>{opt.text ? `${opt.letter}. ${opt.text}` : opt}</option>)}
                        </select>
                    </div>
                );

            case 'MULTIPLE_CHOICE':
            case 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS':
                return (
                    <div>
                        <p><span className="question-number">{questionNumber}.</span> {questionContent.text}</p>
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
