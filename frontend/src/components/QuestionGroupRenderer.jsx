import React from 'react';
import { motion } from 'framer-motion';
import QuestionRenderer from './QuestionRenderer';
import '../css/QuestionGroup.css';

const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: { y: 0, opacity: 1 }
};

// Helper to parse HTML and inject question components
const renderHtmlWithQuestions = (html, questions, onAnswerChange, answers) => {
    const questionMap = new Map(questions.map(q => [q.questionNumber, q]));
    const parts = html.split(/({{\d+}})/g);

    return parts.map((part, index) => {
        const match = part.match(/{{\s*(\d+)\s*}}/);
        if (match) {
            const qNum = parseInt(match[1], 10);
            const question = questionMap.get(qNum);
            if (question) {
                return (
                    <QuestionRenderer
                        key={question.id}
                        question={question}
                        onAnswerChange={onAnswerChange}
                        userAnswer={answers[question.id]}
                        typeOverride="FILL_IN_BLANK_INPUT_ONLY"
                    />
                );
            }
        }
        // Use dangerouslySetInnerHTML for non-placeholder parts
        return <span key={index} dangerouslySetInnerHTML={{ __html: part }} />;
    });
};


const QuestionGroupRenderer = ({ group, onAnswerChange, answers }) => {

    const renderGroupInstructions = () => {
        // New data-driven instructions
        if (group.content?.title || group.content?.instructions_text) {
            return (
                <>
                    {group.content.title && <p><strong>{group.content.title}</strong></p>}
                    {group.content.instructions_text && <p dangerouslySetInnerHTML={{ __html: group.content.instructions_text }} />}
                </>
            );
        }
        // Fallback for old Reading Test structure
        return <p><strong>Questions {group.startNum}-{group.questions[group.questions.length - 1].questionNumber}</strong></p>;
    };

    const renderGroupBody = () => {
        let lastSectionTitle = null;

        // Switch based on the new block_type for Listening
        switch (group.block_type) {
            case 'NOTE_COMPLETION':
                return (
                    <div className="note-completion-wrapper">
                        {group.content.main_title && <h3>{group.content.main_title}</h3>}
                        {group.questions.map(q => {
                            const showSectionTitle = q.questionContent.section_title && q.questionContent.section_title !== lastSectionTitle;
                            if (showSectionTitle) {
                                lastSectionTitle = q.questionContent.section_title;
                            }
                            return (
                                <React.Fragment key={q.id}>
                                    {showSectionTitle && <h4 className="note-section-title">{lastSectionTitle}</h4>}
                                    <div id={`q-block-${q.id}`}>
                                        <QuestionRenderer 
                                            question={q} 
                                            onAnswerChange={onAnswerChange} 
                                            userAnswer={answers[q.id]} 
                                        />
                                    </div>
                                </React.Fragment>
                            );
                        })}
                    </div>
                );

            case 'PLAN_MAP_DIAGRAM_LABELING':
                return (
                    <>
                        {group.content.image_url && <img src={group.content.image_url} alt="Diagram for questions" className="question-diagram" />}
                        <div className="options-list matching-options-box">
                            {group.content.options.map(opt => <p key={opt.letter || opt}><strong>{opt.letter || opt}</strong> {opt.text || ''}</p>)}
                        </div>
                        {group.questions.map(q => (
                            <div id={`q-block-${q.id}`} key={q.id}>
                                <QuestionRenderer 
                                    question={q} 
                                    onAnswerChange={onAnswerChange} 
                                    userAnswer={answers[q.id]} 
                                    groupOptions={group.content.options}
                                />
                            </div>
                        ))}
                    </>
                );
            
            case 'MATCHING_FEATURES':
                return (
                    <>
                        <div className="options-list matching-options-box">
                            <h4>{group.content.options_title || 'Options'}</h4>
                            {group.content.options.map(opt => <p key={opt.letter}><strong>{opt.letter}</strong> {opt.text}</p>)}
                        </div>
                        {group.questions.map(q => (
                            <div id={`q-block-${q.id}`} key={q.id}>
                                <QuestionRenderer 
                                    question={q} 
                                    onAnswerChange={onAnswerChange} 
                                    userAnswer={answers[q.id]} 
                                    groupOptions={group.content.options}
                                />
                            </div>
                        ))}
                    </>
                );

            case 'INSTRUCTIONS_ONLY':
                return group.questions.map(q => (
                    <div id={`q-block-${q.id}`} key={q.id}>
                        <QuestionRenderer question={q} onAnswerChange={onAnswerChange} userAnswer={answers[q.id]} />
                    </div>
                ));

            // IMPORTANT: Fallback for backward compatibility with Reading tests
            default:
                // Special handling for legacy Reading TABLE_COMPLETION
                if (group.questions[0]?.questionType === 'TABLE_COMPLETION') {
                    const tableHtml = group.questions[0].questionContent.text;

                    return (
                        <>
                            <div className="table-completion-container" dangerouslySetInnerHTML={{ __html: tableHtml }} />
                            <div className="summary-completion-inputs">
                                {group.questions.map(q => (
                                    <div id={`q-block-${q.id}`} key={q.id}>
                                        <QuestionRenderer 
                                            question={q} 
                                            onAnswerChange={onAnswerChange} 
                                            userAnswer={answers[q.id]} 
                                        />
                                    </div>
                                ))}
                            </div>
                        </>
                    );
                }

                // Default rendering for all other legacy question groups
                return group.questions.map(q => (
                    <div id={`q-block-${q.id}`} key={q.id}>
                        <QuestionRenderer question={q} onAnswerChange={onAnswerChange} userAnswer={answers[q.id]} />
                    </div>
                ));
        }
    };

    return (
        <motion.div className="question-group" variants={itemVariants}>
            <div className="group-instructions">
                {renderGroupInstructions()}
            </div>
            {renderGroupBody()}
        </motion.div>
    );
};

export default QuestionGroupRenderer;