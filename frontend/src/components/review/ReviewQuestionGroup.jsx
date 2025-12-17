import React, { useMemo } from 'react';
import { sanitizeHtml } from '../../utils/sanitize';
import ReviewQuestionRenderer from './ReviewQuestionRenderer';
import '../../css/ReviewQuestionGroup.css';

/**
 * ReviewQuestionGroup - Groups and renders questions in review mode
 * Mimics the test-taking QuestionGroupRenderer but for review with highlights
 * 
 * @param {Object} props
 * @param {Object} props.group - Question group with block_type, content, and questions
 * @param {Function} props.onQuestionClick - Handler when a question is clicked
 * @param {string} props.selectedQuestionId - Currently selected question ID
 * @param {string} props.skill - 'reading' or 'listening'
 */
const ReviewQuestionGroup = ({ group, onQuestionClick, selectedQuestionId, skill }) => {
    const { block_type, content, questions, type, startNum, partNumber } = group;

    // Render group instructions/title
    const renderGroupInstructions = () => {
        // New data-driven instructions for Listening
        if (content?.title || content?.instructions_text) {
            return (
                <div className="review-group-instructions">
                    {content.title && <p><strong>{content.title}</strong></p>}
                    {content.instructions_text && (
                        <div dangerouslySetInnerHTML={{ __html: sanitizeHtml(content.instructions_text) }} />
                    )}
                </div>
            );
        }

        // For Reading tests - generate instructions based on type
        if (skill === 'reading' && startNum && questions?.length > 0) {
            const endNum = questions[questions.length - 1].questionNumber;
            return (
                <div className="review-group-instructions">
                    <p><strong>Questions {startNum}-{endNum}</strong></p>
                </div>
            );
        }

        return null;
    };

    // Render options box for matching types
    const renderOptionsBox = () => {
        if (!content?.options || content.options.length === 0) return null;

        return (
            <div className="review-options-box">
                {content.options_title && <h4>{content.options_title}</h4>}
                {content.options.map((opt, idx) => (
                    <p key={opt.letter || idx}>
                        <strong>{opt.letter || String.fromCharCode(65 + idx)}</strong> {opt.text || opt}
                    </p>
                ))}
            </div>
        );
    };

    // Render image if present
    const renderImage = () => {
        if (!content?.image_url) return null;
        return (
            <div className="review-group-image">
                <img src={content.image_url} alt="Question diagram" />
            </div>
        );
    };

    // Render questions list
    const renderQuestions = () => {
        if (!questions || questions.length === 0) return null;

        return (
            <div className="review-questions-list">
                {questions.map((q, idx) => (
                    <ReviewQuestionRenderer
                        key={q.questionUid || idx}
                        question={q}
                        onQuestionClick={onQuestionClick}
                        isSelected={selectedQuestionId === q.questionUid}
                    />
                ))}
            </div>
        );
    };

    // Main title for note completion
    const renderMainTitle = () => {
        if (!content?.main_title) return null;
        return <h3 className="review-group-main-title">{content.main_title}</h3>;
    };

    return (
        <div className="review-question-group">
            {renderGroupInstructions()}
            {renderMainTitle()}
            {renderImage()}
            {renderOptionsBox()}
            {renderQuestions()}
        </div>
    );
};

export default ReviewQuestionGroup;
