import React from 'react';
import './../css/QuestionNavBar.css';

const QuestionNavBar = ({ allQuestions, answers, onQuestionSelect }) => {
    if (!allQuestions || allQuestions.length === 0) {
        return null;
    }

    return (
        <div className="question-nav-bar">
            <h3 className="question-nav-title">Question Navigator</h3>
            <div className="question-nav-grid">
                {allQuestions.map(question => {
                    const isAnswered = answers[question.id] && answers[question.id].value;
                    const buttonClass = isAnswered
                        ? 'question-nav-btn answered'
                        : 'question-nav-btn';

                    return (
                        <button
                            key={question.id}
                            className={buttonClass}
                            onClick={() => onQuestionSelect(question.questionNumber)}
                        >
                            {question.questionNumber}
                        </button>
                    );
                })}
            </div>
        </div>
    );
};

export default QuestionNavBar;
