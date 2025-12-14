import React, { useMemo } from 'react';
import ReviewQuestionGroup from './ReviewQuestionGroup';
import '../../css/ReviewAnswerColumn.css';

/**
 * ReviewAnswerColumn - Renders the answer field column for review pages
 * Displays questions in test-taking style with highlighted answers
 * 
 * @param {Object} props
 * @param {Object} props.section - Section data with questions and sectionLayout
 * @param {Function} props.onQuestionClick - Handler when a question is clicked
 * @param {string} props.selectedQuestionId - Currently selected question ID
 * @param {string} props.skill - 'reading' or 'listening'
 */
const ReviewAnswerColumn = ({ section, onQuestionClick, selectedQuestionId, skill }) => {
    // Group questions similar to test-taking page logic
    const questionGroups = useMemo(() => {
        if (!section?.questions || section.questions.length === 0) return [];

        const questionsMap = new Map(section.questions.map(q => [q.questionNumber, q]));

        // For Listening tests with sectionLayout
        if (section.sectionLayout?.blocks) {
            return section.sectionLayout.blocks.map(block => {
                const blockQuestions = block.question_numbers
                    .map(num => questionsMap.get(num))
                    .filter(Boolean);

                return {
                    ...block,
                    questions: blockQuestions,
                    startNum: blockQuestions[0]?.questionNumber,
                };
            });
        }

        // Fallback: Group by question type (for Reading or old data)
        const questions = section.questions;
        if (questions.length === 0) return [];

        const groups = [];
        let currentGroup = {
            type: questions[0].questionType,
            questions: [questions[0]],
            startNum: questions[0].questionNumber,
            partNumber: section.partNumber
        };

        for (let i = 1; i < questions.length; i++) {
            const q = questions[i];
            if (q.questionType === currentGroup.type) {
                currentGroup.questions.push(q);
            } else {
                groups.push(currentGroup);
                currentGroup = {
                    type: q.questionType,
                    questions: [q],
                    startNum: q.questionNumber,
                    partNumber: section.partNumber
                };
            }
        }
        groups.push(currentGroup);
        return groups;
    }, [section]);

    if (!section || questionGroups.length === 0) {
        return (
            <div className="review-answer-column-empty">
                <p>Không có câu hỏi cho phần này.</p>
            </div>
        );
    }

    return (
        <div className="review-answer-column-content">
            {/* Question groups */}
            <div className="review-question-groups">
                {questionGroups.map((group, index) => (
                    <ReviewQuestionGroup
                        key={index}
                        group={group}
                        onQuestionClick={onQuestionClick}
                        selectedQuestionId={selectedQuestionId}
                        skill={skill}
                    />
                ))}
            </div>
        </div>
    );
};

export default ReviewAnswerColumn;
