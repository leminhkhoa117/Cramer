import React from 'react';
import { FaCheck, FaTimes } from 'react-icons/fa';
import '../css/ReviewedQuestion.css';

const getUserAnswerText = (answerContent) => {
    if (!answerContent || answerContent.value == null) {
        return <span className="answer-empty">Không trả lời</span>;
    }

    if (answerContent.value === '') {
        return <span className="answer-empty">(đã bỏ trống)</span>;
    }

    // Handles both plain text and object values like { value: "A" }
    const value = typeof answerContent.value === 'object' ? answerContent.value.value : answerContent.value;
    return String(value).replace(/_/g, ' ');
};

const getCorrectAnswerText = (correctAnswer) => {
    // The correct answer from the backend is a JSON array of strings, e.g., ["innovation"] or ["TRUE"]
    if (!correctAnswer || !Array.isArray(correctAnswer) || correctAnswer.length === 0) {
        return <span className="answer-empty">N/A</span>; 
    }
    
    // If there are multiple correct answers, join them with "or"
    return correctAnswer.map(ans => String(ans).replace(/_/g, ' ')).join(' hoặc ');
};

const ReviewedQuestion = ({ questionReview }) => {
    const {
        questionNumber,
        questionContent,
        userAnswerContent,
        correctAnswer,
        isCorrect,
        explanation
    } = questionReview;

    const userAnswerText = getUserAnswerText(userAnswerContent);
    const correctAnswerText = getCorrectAnswerText(correctAnswer);

    return (
        <div className={`reviewed-question-card ${isCorrect ? 'correct' : 'incorrect'}`}>
            <div className="reviewed-question-header">
                <span className="question-number">Câu {questionNumber}</span>
                <span className="status-icon">
                    {isCorrect ? <FaCheck /> : <FaTimes />}
                </span>
            </div>
            <div className="reviewed-question-body">
                <p className="question-text" dangerouslySetInnerHTML={{ __html: questionContent.text }} />
                
                <div className="answers-comparison">
                    <div className="answer-item your-answer">
                        <strong className="answer-label">Câu trả lời của bạn:</strong>
                        <span className="answer-text">{userAnswerText}</span>
                    </div>
                    <div className="answer-item correct-answer">
                        <strong className="answer-label">Đáp án đúng:</strong>
                        <span className="answer-text">{correctAnswerText}</span>
                    </div>
                </div>

                {explanation && (
                    <div className="explanation-section">
                        <h4>Giải thích</h4>
                        <p>{explanation}</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default ReviewedQuestion;
