import React from 'react';
import './../css/TestFooter.css';

const QuestionButton = ({ question, isAnswered, onSelect }) => (
    <button
        className={`question-nav-btn ${isAnswered ? 'answered' : ''}`}
        onClick={() => onSelect(question.questionNumber)}
    >
        {question.questionNumber}
    </button>
);

export default function TestFooter({ testData, answers, onQuestionSelect, onPartSelect, currentPartIndex }) {
    if (!testData || testData.length === 0) {
        return null;
    }

    return (
        <footer className="test-page-footer">
            {testData.map((part, index) => (
                <div key={part.id} className={`footer-part-section ${index === currentPartIndex ? 'active' : ''}`}>
                    <h3 className="footer-part-title" onClick={() => onPartSelect(index)}>
                        Part {part.partNumber}
                    </h3>
                    <div className="footer-question-grid">
                        {part.questions.map(q => (
                            <QuestionButton
                                key={q.id}
                                question={q}
                                isAnswered={!!answers[q.id]}
                                onSelect={onQuestionSelect}
                            />
                        ))}
                    </div>
                </div>
            ))}
        </footer>
    );
}
