import React from 'react';

const QuestionRenderer = ({ question }) => {
    const { questionType, questionContent, questionNumber } = question;

    // A helper to render the question text, replacing blanks with inputs
    const renderTextWithInput = (text) => {
        const parts = text.split('____');
        return (
            <p>
                {parts.map((part, index) => (
                    <React.Fragment key={index}>
                        {part}
                        {index < parts.length - 1 && (
                            <input type="text" className="fill-in-blank-input" />
                        )}
                    </React.Fragment>
                ))}
            </p>
        );
    };

    const renderQuestion = () => {
        switch (questionType) {
            case 'FILL_IN_BLANK':
            case 'SUMMARY_COMPLETION':
                return renderTextWithInput(questionContent.text);

            case 'TRUE_FALSE_NOT_GIVEN':
                return (
                    <div>
                        <p>{questionContent.text}</p>
                        <div className="tfn-options">
                            <label><input type="radio" name={`q_${question.id}`} value="TRUE" /> True</label>
                            <label><input type="radio" name={`q_${question.id}`} value="FALSE" /> False</label>
                            <label><input type="radio" name={`q_${question.id}`} value="NOT_GIVEN" /> Not Given</label>
                        </div>
                    </div>
                );
            
            case 'YES_NO_NOT_GIVEN':
                return (
                    <div>
                        <p>{questionContent.text}</p>
                        <div className="tfn-options">
                            <label><input type="radio" name={`q_${question.id}`} value="YES" /> Yes</label>
                            <label><input type="radio" name={`q_${question.id}`} value="NO" /> No</label>
                            <label><input type="radio" name={`q_${question.id}`} value="NOT_GIVEN" /> Not Given</label>
                        </div>
                    </div>
                );

            case 'MATCHING_INFORMATION':
                return (
                    <div>
                        <p>{questionContent.text}</p>
                        <select>
                            <option value="">Select...</option>
                            {questionContent.options.map(opt => <option key={opt} value={opt}>{opt}</option>)}
                        </select>
                    </div>
                );

            case 'MULTIPLE_CHOICE':
                return (
                    <div>
                        <p>{questionContent.text}</p>
                        <div className="mcq-options">
                            {questionContent.options.map((opt, index) => (
                                <label key={index}>
                                    <input type="radio" name={`q_${question.id}`} value={opt.charAt(0)} />
                                    {opt}
                                </label>
                            ))}
                        </div>
                    </div>
                );

            case 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS':
                 return (
                    <div>
                        <p>{questionContent.text}</p>
                        <div className="mcq-options">
                            {questionContent.options.map((opt, index) => (
                                <label key={index}>
                                    <input type="checkbox" name={`q_${question.id}`} value={opt.charAt(0)} />
                                    {opt}
                                </label>
                            ))}
                        </div>
                    </div>
                );
            
            case 'SUMMARY_COMPLETION_OPTIONS':
                 return (
                    <div>
                        {/* The options box should be rendered once for the group */}
                        {renderTextWithInput(questionContent.text)}
                    </div>
                )

            default:
                return <pre>{JSON.stringify(questionContent, null, 2)}</pre>;
        }
    };

    return (
        <div className="question-block">
            <strong>Question {questionNumber}</strong>
            {renderQuestion()}
        </div>
    );
};

export default QuestionRenderer;
