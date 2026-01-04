/**
 * FlowchartRenderer - Renders a flowchart diagram from JSON structure
 * 
 * Supports vertical/horizontal layouts with labeled nodes and connections.
 * Used for DIAGRAM_LABEL_COMPLETION questions.
 * 
 * @since 2026-01-04
 */

import React from 'react';
import './FlowchartRenderer.css';

/**
 * FlowchartRenderer component
 * 
 * @param {Object} props
 * @param {Object} props.diagram - Flowchart JSON structure
 * @param {string} props.diagram.direction - 'vertical' or 'horizontal'
 * @param {Array} props.diagram.nodes - Array of node objects
 * @param {Array} props.diagram.connections - Array of connection objects
 * @param {Object} props.answers - User answers keyed by question number
 * @param {Function} props.onAnswerChange - Callback when answer changes (questionNumber, value)
 * @param {boolean} props.readOnly - If true, inputs are disabled
 * @param {Object} props.reviewData - For review mode: { correctAnswers, userAnswers }
 */
const FlowchartRenderer = ({
    diagram,
    answers = {},
    onAnswerChange,
    readOnly = false,
    reviewData = null
}) => {
    if (!diagram || !diagram.nodes || !Array.isArray(diagram.nodes)) {
        return (
            <div className="flowchart-error">
                Không có dữ liệu sơ đồ / No diagram data available
            </div>
        );
    }

    const { direction = 'vertical', nodes, connections = [] } = diagram;
    const isVertical = direction === 'vertical';

    /**
     * Renders the input/answer element for blank nodes
     */
    const renderInputOrAnswer = (question_number, statusClass) => {
        if (readOnly || reviewData) {
            return (
                <span className={`flowchart-node__inline-answer ${statusClass}`}>
                    {reviewData?.userAnswers?.[question_number] ||
                        answers[question_number] ||
                        '____'}
                </span>
            );
        }
        return (
            <input
                type="text"
                className="flowchart-node__inline-input"
                value={answers[question_number] || ''}
                onChange={(e) => onAnswerChange?.(question_number, e.target.value)}
                placeholder="..."
            />
        );
    };

    const renderNode = (node, index) => {
        const { id, label, type = 'step', question_number } = node;
        const isBlank = type === 'blank' || label?.includes('____');
        const hasLabelWithBlank = label && label.includes('____');

        // For review mode, determine correctness
        let statusClass = '';
        if (reviewData && question_number) {
            const userAns = reviewData.userAnswers?.[question_number];
            const correctAns = reviewData.correctAnswers?.[question_number];
            if (userAns !== undefined) {
                statusClass = userAns?.toLowerCase() === correctAns?.toLowerCase() ? 'correct' : 'incorrect';
            }
        }

        // CASE 1: Blank node WITH label containing ____ (IELTS-style hybrid)
        if (isBlank && hasLabelWithBlank) {
            const parts = label.split('____');
            return (
                <div
                    key={id || index}
                    className={`flowchart-node flowchart-node--blank ${statusClass}`}
                >
                    <div className="flowchart-node__content flowchart-node__hybrid">
                        <span className="flowchart-node__qnum">{question_number}</span>
                        <span className="flowchart-node__label-with-blank">
                            <span dangerouslySetInnerHTML={{ __html: parts[0] }} />
                            {renderInputOrAnswer(question_number, statusClass)}
                            {parts[1] && <span dangerouslySetInnerHTML={{ __html: parts[1] }} />}
                        </span>
                        {reviewData?.correctAnswers?.[question_number] && statusClass === 'incorrect' && (
                            <span className="flowchart-node__correct">
                                ✓ {reviewData.correctAnswers[question_number]}
                            </span>
                        )}
                    </div>
                </div>
            );
        }

        // CASE 2: Blank node WITHOUT label (legacy - just input box)
        if (isBlank && !hasLabelWithBlank) {
            return (
                <div
                    key={id || index}
                    className={`flowchart-node flowchart-node--blank ${statusClass}`}
                >
                    <div className="flowchart-node__content">
                        {question_number && (
                            <span className="flowchart-node__qnum">{question_number}</span>
                        )}
                        {readOnly || reviewData ? (
                            <span className={`flowchart-node__answer ${statusClass}`}>
                                {reviewData?.userAnswers?.[question_number] ||
                                    answers[question_number] ||
                                    '—'}
                            </span>
                        ) : (
                            <input
                                type="text"
                                className="flowchart-node__input"
                                value={answers[question_number] || ''}
                                onChange={(e) => onAnswerChange?.(question_number, e.target.value)}
                                placeholder={`${question_number}`}
                            />
                        )}
                        {reviewData?.correctAnswers?.[question_number] && statusClass === 'incorrect' && (
                            <span className="flowchart-node__correct">
                                ({reviewData.correctAnswers[question_number]})
                            </span>
                        )}
                    </div>
                </div>
            );
        }

        // CASE 3: Regular labeled node (step, start, end)
        return (
            <div
                key={id || index}
                className={`flowchart-node flowchart-node--${type} ${statusClass}`}
            >
                <div
                    className="flowchart-node__content"
                    dangerouslySetInnerHTML={{ __html: label || '' }}
                />
            </div>
        );
    };

    const renderArrow = (index, total) => {
        if (index >= total - 1) return null;

        return (
            <div className={`flowchart-arrow flowchart-arrow--${isVertical ? 'vertical' : 'horizontal'}`}>
                {isVertical ? '↓' : '→'}
            </div>
        );
    };

    return (
        <div className={`flowchart-container flowchart-container--${direction}`}>
            {nodes.map((node, index) => (
                <React.Fragment key={node.id || index}>
                    {renderNode(node, index)}
                    {renderArrow(index, nodes.length)}
                </React.Fragment>
            ))}
        </div>
    );
};

export default FlowchartRenderer;
