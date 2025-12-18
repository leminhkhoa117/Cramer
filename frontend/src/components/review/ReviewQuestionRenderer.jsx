import React from 'react';
import { sanitizeHtml } from '../../utils/sanitize';
import '../../css/review-question-renderer.css';

/**
 * ReviewQuestionRenderer - Renders a question in review mode with answer highlighting
 * 
 * This component mimics the test-taking UI but displays:
 * - The user's submitted answer (pre-filled, read-only)
 * - Color highlighting based on correctness
 * - Click handler to scroll to explanation
 * 
 * @param {Object} props
 * @param {Object} props.question - Question data from review API
 * @param {Function} props.onQuestionClick - Handler when question is clicked (scrolls to explanation)
 * @param {boolean} props.isSelected - Whether this question is currently selected
 */
const ReviewQuestionRenderer = ({ question, onQuestionClick, isSelected }) => {
    const { 
        questionNumber, 
        questionUid, 
        questionType, 
        questionContent, 
        userAnswerContent, 
        correctAnswer, 
        isCorrect 
    } = question;

    // Normalize user answer to string for display
    const getUserAnswerValue = () => {
        if (!userAnswerContent) return '';
        if (typeof userAnswerContent === 'string') return userAnswerContent;
        if (userAnswerContent.value) return userAnswerContent.value;
        if (Array.isArray(userAnswerContent)) return userAnswerContent.join(', ');
        return JSON.stringify(userAnswerContent);
    };

    // Normalize correct answer for comparison
    const getCorrectAnswerValue = () => {
        if (!correctAnswer) return '';
        if (typeof correctAnswer === 'string') return correctAnswer;
        if (Array.isArray(correctAnswer)) return correctAnswer.join(' / ');
        return JSON.stringify(correctAnswer);
    };

    const userAnswer = getUserAnswerValue();
    const correctAnswerStr = getCorrectAnswerValue();

    // Get status class for highlighting
    const getStatusClass = () => {
        if (isCorrect === true) return 'correct';
        if (isCorrect === false) return 'incorrect';
        return 'unanswered';
    };

    const statusClass = getStatusClass();

    // Handle click - notify parent to scroll to explanation
    const handleClick = () => {
        if (onQuestionClick) {
            onQuestionClick(questionUid);
        }
    };

    // Render based on question type
    const renderQuestion = () => {
        const content = questionContent || {};
        const text = typeof content === 'string' ? content : content.text || content.prompt || '';
        const options = content.options || [];

        switch (questionType) {
            case 'FILL_IN_BLANK':
            case 'SUMMARY_COMPLETION':
            case 'TABLE_COMPLETION':
            case 'NOTE_COMPLETION':
                return renderFillInBlank(text);

            case 'MULTIPLE_CHOICE':
                return renderMultipleChoice(text, options);

            case 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS':
                return renderMultipleChoiceMultiple(text, options);

            case 'TRUE_FALSE_NOT_GIVEN':
                return renderTrueFalseNotGiven(text);

            case 'YES_NO_NOT_GIVEN':
                return renderYesNoNotGiven(text);

            case 'MATCHING':
            case 'MATCHING_INFORMATION':
            case 'MATCHING_FEATURES':
            case 'MATCHING_HEADINGS':
                return renderMatching(text, options);

            default:
                return renderFillInBlank(text);
        }
    };

    // Fill in blank with highlighted input
    const renderFillInBlank = (text) => {
        // Replace ____ with the user's answer (highlighted)
        const parts = text ? text.split(/____/g) : [''];
        
        return (
            <div className="review-question-content">
                <span className="question-number">{questionNumber}.</span>
                <span className="question-text">
                    {parts[0] && <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(parts[0]) }} />}
                    <span className={`review-answer-input ${statusClass}`}>
                        {userAnswer || <em className="no-answer">—</em>}
                    </span>
                    {parts[1] && <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(parts[1]) }} />}
                </span>
                {isCorrect === false && (
                    <span className="correct-answer-inline">
                        (Đáp án: <strong>{correctAnswerStr}</strong>)
                    </span>
                )}
            </div>
        );
    };

    // Multiple choice with highlighted selected option
    const renderMultipleChoice = (text, options) => {
        return (
            <div className="review-question-content">
                <p className="question-stem">
                    <span className="question-number">{questionNumber}.</span>
                    <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(text) }} />
                </p>
                <div className="review-mcq-options">
                    {options.map((opt, index) => {
                        const optLetter = typeof opt === 'string' ? opt.charAt(0) : opt.letter || String.fromCharCode(65 + index);
                        const optText = typeof opt === 'string' ? opt.substring(1).trim() : opt.text || opt;
                        const isUserAnswer = userAnswer === optLetter;
                        const isCorrectAnswer = correctAnswerStr.includes(optLetter);
                        
                        let optionClass = '';
                        if (isUserAnswer && isCorrect === true) optionClass = 'correct';
                        else if (isUserAnswer && isCorrect === false) optionClass = 'incorrect';
                        else if (isCorrectAnswer && isCorrect === false) optionClass = 'show-correct';
                        
                        return (
                            <label key={index} className={`review-option ${optionClass}`}>
                                <input 
                                    type="radio" 
                                    checked={isUserAnswer} 
                                    disabled 
                                    readOnly 
                                />
                                <strong>{optLetter}.</strong> {optText}
                            </label>
                        );
                    })}
                </div>
            </div>
        );
    };

    // Multiple choice with multiple answers
    const renderMultipleChoiceMultiple = (text, options) => {
        const userAnswers = Array.isArray(userAnswerContent) ? userAnswerContent : [];
        const correctAnswers = Array.isArray(correctAnswer) ? correctAnswer : [];

        return (
            <div className="review-question-content">
                <p className="question-stem">
                    <span className="question-number">{questionNumber}.</span>
                    <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(text) }} />
                </p>
                <div className="review-mcq-options">
                    {options.map((opt, index) => {
                        const optLetter = typeof opt === 'string' ? opt.charAt(0) : opt.letter || String.fromCharCode(65 + index);
                        const optText = typeof opt === 'string' ? opt.substring(1).trim() : opt.text || opt;
                        const isUserAnswer = userAnswers.includes(optLetter);
                        const isCorrectAnswer = correctAnswers.includes(optLetter);
                        
                        let optionClass = '';
                        if (isUserAnswer && isCorrectAnswer) optionClass = 'correct';
                        else if (isUserAnswer && !isCorrectAnswer) optionClass = 'incorrect';
                        else if (isCorrectAnswer && !isUserAnswer) optionClass = 'show-correct';
                        
                        return (
                            <label key={index} className={`review-option ${optionClass}`}>
                                <input 
                                    type="checkbox" 
                                    checked={isUserAnswer} 
                                    disabled 
                                    readOnly 
                                />
                                <strong>{optLetter}.</strong> {optText}
                            </label>
                        );
                    })}
                </div>
            </div>
        );
    };

    // True/False/Not Given
    const renderTrueFalseNotGiven = (text) => {
        const tfnOptions = ['TRUE', 'FALSE', 'NOT_GIVEN'];
        
        return (
            <div className="review-question-content">
                <p className="question-stem">
                    <span className="question-number">{questionNumber}.</span>
                    <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(text) }} />
                </p>
                <div className="review-tfn-options">
                    {tfnOptions.map(opt => {
                        const isUserAnswer = userAnswer === opt;
                        const isCorrectAnswer = correctAnswerStr === opt;
                        
                        let optionClass = '';
                        if (isUserAnswer && isCorrect === true) optionClass = 'correct';
                        else if (isUserAnswer && isCorrect === false) optionClass = 'incorrect';
                        else if (isCorrectAnswer && isCorrect === false) optionClass = 'show-correct';
                        
                        return (
                            <label key={opt} className={`review-option ${optionClass}`}>
                                <input type="radio" checked={isUserAnswer} disabled readOnly />
                                {opt.replace('_', ' ')}
                            </label>
                        );
                    })}
                </div>
            </div>
        );
    };

    // Yes/No/Not Given
    const renderYesNoNotGiven = (text) => {
        const ynnOptions = ['YES', 'NO', 'NOT_GIVEN'];
        
        return (
            <div className="review-question-content">
                <p className="question-stem">
                    <span className="question-number">{questionNumber}.</span>
                    <span dangerouslySetInnerHTML={{ __html: sanitizeHtml(text) }} />
                </p>
                <div className="review-tfn-options">
                    {ynnOptions.map(opt => {
                        const isUserAnswer = userAnswer === opt;
                        const isCorrectAnswer = correctAnswerStr === opt;
                        
                        let optionClass = '';
                        if (isUserAnswer && isCorrect === true) optionClass = 'correct';
                        else if (isUserAnswer && isCorrect === false) optionClass = 'incorrect';
                        else if (isCorrectAnswer && isCorrect === false) optionClass = 'show-correct';
                        
                        return (
                            <label key={opt} className={`review-option ${optionClass}`}>
                                <input type="radio" checked={isUserAnswer} disabled readOnly />
                                {opt.replace('_', ' ')}
                            </label>
                        );
                    })}
                </div>
            </div>
        );
    };

    // Matching with dropdown
    const renderMatching = (text, options) => {
        return (
            <div className="review-question-content">
                <span className="question-number">{questionNumber}.</span>
                <span className="question-text" dangerouslySetInnerHTML={{ __html: sanitizeHtml(text) }} />
                <span className={`review-answer-select ${statusClass}`}>
                    {userAnswer || <em className="no-answer">—</em>}
                </span>
                {isCorrect === false && (
                    <span className="correct-answer-inline">
                        (Đáp án: <strong>{correctAnswerStr}</strong>)
                    </span>
                )}
            </div>
        );
    };

    return (
        <div 
            className={`review-question-block ${statusClass} ${isSelected ? 'selected' : ''}`}
            onClick={handleClick}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && handleClick()}
        >
            {renderQuestion()}
        </div>
    );
};

export default ReviewQuestionRenderer;
