import React from 'react';
import { motion } from 'framer-motion';
import QuestionRenderer from './QuestionRenderer';
import HighlightableHtmlContent from './HighlightableHtmlContent'; // Import the new component
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

// Map for dynamically generating Reading instructions
const readingInstructionsMap = {
    'SUMMARY_COMPLETION': (group) => {
        const wordLimit = group.questions[0]?.wordLimit;
        return `Complete the notes below.<br/>Write <strong>${wordLimit || 'ONE WORD ONLY'}</strong> for each answer.`;
    },
    'FILL_IN_BLANK': (group) => {
        const wordLimit = group.questions[0]?.wordLimit;
        return `Complete the sentences below.<br/>Choose <strong>${wordLimit || 'ONE WORD ONLY'}</strong> from the passage for each answer.`;
    },
    'TABLE_COMPLETION': (group) => {
        const wordLimit = group.questions[0]?.wordLimit;
        return `Complete the table below.<br/>Choose <strong>${wordLimit || 'ONE WORD ONLY'}</strong> from the passage for each answer.`;
    },
    'TRUE_FALSE_NOT_GIVEN': (group) => `Do the following statements agree with the information given in Reading Passage ${group.partNumber}?<br/>In boxes ${group.startNum}–${group.questions[group.questions.length - 1].questionNumber} on your answer sheet, write<br/><strong>TRUE</strong> &nbsp;&nbsp;&nbsp;&nbsp; if the statement agrees with the information<br/><strong>FALSE</strong> &nbsp;&nbsp;&nbsp; if the statement contradicts the information<br/><strong>NOT GIVEN</strong> if there is no information on this`,
    'YES_NO_NOT_GIVEN': (group) => `Do the following statements agree with the claims of the writer in Reading Passage ${group.partNumber}?<br/>In boxes ${group.startNum}–${group.questions[group.questions.length - 1].questionNumber} on your answer sheet, write<br/><strong>YES</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; if the statement agrees with the claims of the writer<br/><strong>NO</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; if the statement contradicts the claims of the writer<br/><strong>NOT GIVEN</strong> if it is impossible to say what the writer thinks about this`,
    'MATCHING_INFORMATION': (group) => {
        const optionsCount = group.questions[0]?.questionContent?.options?.length || 0;
        const lastOptionLetter = optionsCount > 0 ? String.fromCharCode(64 + optionsCount) : 'G'; // Default to G if no options
        return `Reading Passage ${group.partNumber} has ${optionsCount || 'several'} sections, <strong>A–${lastOptionLetter}</strong>. Which section contains the following information?<br/>Write the correct letter, <strong>A–${lastOptionLetter}</strong>, in boxes ${group.startNum}–${group.questions[group.questions.length - 1].questionNumber} on your answer sheet.<br/><strong>NB</strong> You may use any letter more than once.`;
    },
    'MULTIPLE_CHOICE': (group) => `Choose the correct letter, <strong>A, B, C or D</strong>.<br/>Write the correct letter in boxes ${group.startNum}–${group.questions[group.questions.length - 1].questionNumber} on your answer sheet.`,
    'MULTIPLE_CHOICE_MULTIPLE_ANSWERS': (group) => `Choose <strong>TWO</strong> letters, <strong>A–E</strong>.<br/>Write the correct letters in boxes ${group.startNum} and ${group.questions[group.questions.length - 1].questionNumber} on your answer sheet.`,
    'SUMMARY_COMPLETION_OPTIONS': (group) => `Complete the summary using the list of phrases, <strong>A–J</strong>, below.<br/>Write the correct letter, <strong>A–J</strong>, in boxes ${group.startNum}–${group.questions[group.questions.length - 1].questionNumber} on your answer sheet.`,
    // Add other types as needed
};


const QuestionGroupRenderer = ({ group, onAnswerChange, answers, skill }) => {

    const renderGroupInstructions = () => {
        // New data-driven instructions for Listening
        if (group.content?.title || group.content?.instructions_text) {
            return (
                <>
                    {group.content.title && <p><strong>{group.content.title}</strong></p>}
                    {group.content.instructions_text && 
                        <HighlightableHtmlContent 
                            htmlString={group.content.instructions_text} 
                            contentId={`group-instruction-${group.id}-listening`} 
                        />
                    }
                </>
            );
        }

        // Dynamic instructions for Reading tests
        if (skill === 'reading' && readingInstructionsMap[group.type]) {
            const instructionText = readingInstructionsMap[group.type](group);
            return (
                <>
                    <p><strong>Questions {group.startNum}-{group.questions[group.questions.length - 1].questionNumber}</strong></p>
                    <HighlightableHtmlContent 
                        htmlString={instructionText} 
                        contentId={`group-instruction-${group.id}-reading`} 
                    />
                </>
            );
        }
        
        // Fallback for old Reading Test structure or if no specific instruction is found
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
                            <HighlightableHtmlContent 
                                htmlString={tableHtml} 
                                contentId={`table-completion-${group.id}`} 
                                className="table-completion-container" 
                            />
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