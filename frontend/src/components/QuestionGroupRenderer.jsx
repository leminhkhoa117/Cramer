import React from 'react';
import { motion } from 'framer-motion';
import { sanitizeHtml } from '../utils/sanitize';
import QuestionRenderer from './QuestionRenderer';
import HighlightableHtmlContent from './HighlightableHtmlContent'; // Import the new component
import '../css/question-group.css';

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
        return <span key={index} dangerouslySetInnerHTML={{ __html: sanitizeHtml(part) }} />;
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
    'TRUE_FALSE_NOT_GIVEN': (group) => `
        <p>Do the following statements agree with the information given in Reading Passage ${group.partNumber}?</p>
        <p>In boxes ${group.startNum}–${group.questions[group.questions.length - 1].questionNumber} on your answer sheet, select</p>
        <div class="ielts-instruction-list">
            <div class="ielts-instruction-item"><strong>TRUE</strong><span>if the statement agrees with the information</span></div>
            <div class="ielts-instruction-item"><strong>FALSE</strong><span>if the statement contradicts the information</span></div>
            <div class="ielts-instruction-item"><strong>NOT GIVEN</strong><span>if there is no information on this</span></div>
        </div>`,
    'YES_NO_NOT_GIVEN': (group) => `
        <p>Do the following statements agree with the claims of the writer in Reading Passage ${group.partNumber}?</p>
        <p>In boxes ${group.startNum}–${group.questions[group.questions.length - 1].questionNumber} on your answer sheet, select</p>
        <div class="ielts-instruction-list">
            <div class="ielts-instruction-item"><strong>YES</strong><span>if the statement agrees with the claims of the writer</span></div>
            <div class="ielts-instruction-item"><strong>NO</strong><span>if the statement contradicts the claims of the writer</span></div>
            <div class="ielts-instruction-item"><strong>NOT GIVEN</strong><span>if it is impossible to say what the writer thinks about this</span></div>
        </div>`,
    'MATCHING_INFORMATION': (group) => {
        const optionsCount = group.questions[0]?.questionContent?.options?.length || 0;
        const lastOptionLetter = optionsCount > 0 ? String.fromCharCode(64 + optionsCount) : 'G';
        return `
            <p>Reading Passage ${group.partNumber} has ${optionsCount || 'several'} sections, <strong>A–${lastOptionLetter}</strong>.</p>
            <p>Which section contains the following information?</p>
            <p>Write the correct letter, <strong>A–${lastOptionLetter}</strong>, in boxes ${group.startNum}–${group.questions[group.questions.length - 1].questionNumber} on your answer sheet.</p>
            <p class="ielts-instruction-note"><strong>NB</strong> You may use any letter more than once.</p>`;
    },
    'MULTIPLE_CHOICE': (group) => `
        <p>Choose the correct letter, <strong>A, B, C or D</strong>.</p>
        <p>Write the correct letter in boxes ${group.startNum}–${group.questions[group.questions.length - 1].questionNumber} on your answer sheet.</p>`,
    'MULTIPLE_CHOICE_MULTIPLE_ANSWERS': (group) => `
        <p>Choose <strong>TWO</strong> letters, <strong>A–E</strong>.</p>
        <p>Write the correct letters in boxes ${group.startNum} and ${group.questions[group.questions.length - 1].questionNumber} on your answer sheet.</p>`,
    'SUMMARY_COMPLETION_OPTIONS': (group) => `
        <p>Complete the summary using the list of phrases, <strong>A–J</strong>, below.</p>
        <p>Write the correct letter, <strong>A–J</strong>, in boxes ${group.startNum}–${group.questions[group.questions.length - 1].questionNumber} on your answer sheet.</p>`,
    'MATCHING_HEADINGS': (group) => {
        const optionsCount = group.questions[0]?.questionContent?.options?.length || group.questions[0]?.questionContent?.headings?.length || 0;
        const lastOptionNum = optionsCount > 0 ? optionsCount : 10;
        return `
            <p>Reading Passage ${group.partNumber} has ${group.questions.length} paragraphs, <strong>A–${String.fromCharCode(64 + group.questions.length)}</strong>.</p>
            <p>Choose the correct heading for each paragraph from the list of headings below.</p>
            <p>Write the correct number, <strong>i–${lastOptionNum <= 10 ? ['i', 'ii', 'iii', 'iv', 'v', 'vi', 'vii', 'viii', 'ix', 'x'][lastOptionNum - 1] : lastOptionNum}</strong>, in boxes ${group.startNum}–${group.questions[group.questions.length - 1].questionNumber} on your answer sheet.</p>`;
    },
    'DIAGRAM_LABEL_COMPLETION': (group) => {
        const wordLimit = group.questions[0]?.wordLimit;
        return `Complete the diagram below.<br/>Write <strong>${wordLimit || 'NO MORE THAN TWO WORDS'}</strong> from the passage for each answer.`;
    },
};



const QuestionGroupRenderer = ({ group, onAnswerChange, answers, skill }) => {
    // Defensive: ensure group is a valid object
    if (!group || typeof group !== 'object') {
        return <div className="question-group">Invalid question group data</div>;
    }

    const renderGroupInstructions = () => {
        // Use uniqueGroupId for guaranteed uniqueness across parts
        const groupId = group.uniqueGroupId || group.id || 'unknown';

        // Safety: check if questions exist
        const hasQuestions = group.questions && group.questions.length > 0;
        const lastQuestion = hasQuestions ? group.questions[group.questions.length - 1] : null;
        const lastQuestionNumber = lastQuestion?.questionNumber ?? group.startNum;

        // New data-driven instructions for Listening
        if (group.content?.title || group.content?.instructions_text) {
            return (
                <>
                    {group.content?.title && <p><strong>{group.content.title}</strong></p>}
                    {group.content?.instructions_text &&
                        <HighlightableHtmlContent
                            htmlString={group.content.instructions_text}
                            contentId={`instruction-${groupId}`}
                        />
                    }
                </>
            );
        }

        // Dynamic instructions for Reading tests
        if (skill === 'reading' && readingInstructionsMap[group.type] && hasQuestions) {
            const instructionText = readingInstructionsMap[group.type](group);
            return (
                <>
                    <p><strong>Questions {group.startNum}-{lastQuestionNumber}</strong></p>
                    <HighlightableHtmlContent
                        htmlString={instructionText}
                        contentId={`instruction-${groupId}`}
                    />
                </>
            );
        }

        // Fallback for old Reading Test structure or if no specific instruction is found
        return <p><strong>Questions {group.startNum}-{lastQuestionNumber}</strong></p>;
    };

    const renderGroupBody = () => {
        let lastSectionTitle = null;

        // Defensive: ensure questions array exists
        const questions = Array.isArray(group.questions) ? group.questions : [];
        if (questions.length === 0) {
            return <div className="no-questions">No questions in this group</div>;
        }

        // Switch based on the new block_type for Listening
        switch (group.block_type) {
            case 'NOTE_COMPLETION':
                return (
                    <div className="note-completion-wrapper">
                        {group.content?.main_title && <h3>{group.content.main_title}</h3>}
                        {questions.map(q => {
                            const showSectionTitle = q.questionContent?.section_title && q.questionContent.section_title !== lastSectionTitle;
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
                                            partId={group.partId}
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
                        {group.content?.image_url && <img src={group.content.image_url} alt="Diagram for questions" className="question-diagram" />}
                        <div className="options-list matching-options-box">
                            {group.content?.options?.map(opt => <p key={opt.letter || opt}><strong>{opt.letter || opt}</strong> {opt.text || ''}</p>)}
                        </div>
                        {questions.map(q => (
                            <div id={`q-block-${q.id}`} key={q.id}>
                                <QuestionRenderer
                                    question={q}
                                    onAnswerChange={onAnswerChange}
                                    userAnswer={answers[q.id]}
                                    groupOptions={group.content?.options}
                                    partId={group.partId}
                                />
                            </div>
                        ))}
                    </>
                );

            case 'MATCHING_FEATURES':
                return (
                    <>
                        <div className="options-list matching-options-box">
                            <h4>{group.content?.options_title || 'Options'}</h4>
                            {group.content?.options?.map(opt => <p key={opt.letter}><strong>{opt.letter}</strong> {opt.text}</p>)}
                        </div>
                        {questions.map(q => (
                            <div id={`q-block-${q.id}`} key={q.id}>
                                <QuestionRenderer
                                    question={q}
                                    onAnswerChange={onAnswerChange}
                                    userAnswer={answers[q.id]}
                                    groupOptions={group.content?.options}
                                    partId={group.partId}
                                />
                            </div>
                        ))}
                    </>
                );

            case 'INSTRUCTIONS_ONLY': {
                // Check if this is a MATCHING group
                const isMatchingGroup = questions[0]?.questionType === 'MATCHING';

                // Try to get options from block content, or fallback to first question's content
                let groupOptions = group.content?.options;
                if ((!groupOptions || groupOptions.length === 0) && isMatchingGroup) {
                    // Fallback: extract from first question's questionContent.options
                    groupOptions = questions[0]?.questionContent?.options;
                }

                const hasOptions = Array.isArray(groupOptions) && groupOptions.length > 0;

                if (hasOptions && isMatchingGroup) {
                    // Render options box for MATCHING questions within INSTRUCTIONS_ONLY
                    return (
                        <>
                            <div className="options-list matching-options-box">
                                <h4>{group.content?.options_title || 'Options'}</h4>
                                {groupOptions.map(opt => {
                                    const letter = typeof opt === 'object' ? opt.letter : opt;
                                    const text = typeof opt === 'object' ? opt.text : '';
                                    return <p key={letter}><strong>{letter}</strong> {text}</p>;
                                })}
                            </div>
                            {questions.map(q => (
                                <div id={`q-block-${q.id}`} key={q.id}>
                                    <QuestionRenderer
                                        question={q}
                                        onAnswerChange={onAnswerChange}
                                        userAnswer={answers[q.id]}
                                        groupOptions={groupOptions}
                                        partId={group.partId}
                                    />
                                </div>
                            ))}
                        </>
                    );
                }

                // Default INSTRUCTIONS_ONLY: no options box
                return questions.map(q => (
                    <div id={`q-block-${q.id}`} key={q.id}>
                        <QuestionRenderer question={q} onAnswerChange={onAnswerChange} userAnswer={answers[q.id]} partId={group.partId} />
                    </div>
                ));
            }

            // IMPORTANT: Fallback for backward compatibility with Reading tests
            default: {
                // For TABLE_COMPLETION: first question contains full HTML, subsequent questions are empty
                // We render only first question with table, skip others
                const isTableCompletion = questions[0]?.questionType === 'TABLE_COMPLETION';

                if (isTableCompletion) {
                    const firstQ = questions[0];
                    return (
                        <div id={`q-block-${firstQ.id}`} key={firstQ.id}>
                            <QuestionRenderer
                                question={firstQ}
                                onAnswerChange={onAnswerChange}
                                userAnswer={answers[firstQ.id]}
                                partId={group.partId}
                                groupedQuestions={questions}
                                groupAnswers={answers}
                            />
                        </div>
                    );
                }

                // Check if this is a MATCHING group (fallback for old data or AI preview)
                const isMatchingType = questions[0]?.questionType === 'MATCHING' ||
                    questions[0]?.questionType?.startsWith('MATCHING_') ||
                    group.type === 'MATCHING';

                // Try to get options from block content, or fallback to first question's content
                let groupOptions = group.content?.options;
                if ((!groupOptions || groupOptions.length === 0) && isMatchingType) {
                    // Fallback: extract from first question's questionContent.options
                    groupOptions = questions[0]?.questionContent?.options;
                }

                const hasOptions = Array.isArray(groupOptions) && groupOptions.length > 0;

                if (hasOptions && isMatchingType) {
                    return (
                        <>
                            <div className="options-list matching-options-box">
                                <h4>{group.content?.options_title || 'Options'}</h4>
                                {groupOptions.map(opt => {
                                    const letter = typeof opt === 'object' ? opt.letter : opt;
                                    const text = typeof opt === 'object' ? opt.text : '';
                                    return <p key={letter}><strong>{letter}</strong> {text}</p>;
                                })}
                            </div>
                            {questions.map(q => (
                                <div id={`q-block-${q.id}`} key={q.id}>
                                    <QuestionRenderer
                                        question={q}
                                        onAnswerChange={onAnswerChange}
                                        userAnswer={answers[q.id]}
                                        groupOptions={groupOptions}
                                        partId={group.partId}
                                    />
                                </div>
                            ))}
                        </>
                    );
                }

                // Default rendering for other question types
                return questions.map(q => (
                    <div id={`q-block-${q.id}`} key={q.id}>
                        <QuestionRenderer question={q} onAnswerChange={onAnswerChange} userAnswer={answers[q.id]} partId={group.partId} />
                    </div>
                ));
            }
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