import React from 'react';
import { motion } from 'framer-motion';
import QuestionRenderer from './QuestionRenderer';
import '../css/QuestionGroup.css';

const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: { y: 0, opacity: 1 }
};

const QuestionGroupRenderer = ({ group, currentPart, onAnswerChange, answers }) => {

    const renderGroupInstructions = () => {
        const start = group.startNum;
        const end = group.questions[group.questions.length - 1].questionNumber;
        const range = start === end ? `Question ${start}` : `Questions ${start}-${end}`;
        const firstQuestion = group.questions[0];

        switch (group.type) {
            case 'FILL_IN_BLANK':
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Complete the notes below.</p>
                        <p>Choose <strong>{firstQuestion?.wordLimit?.toUpperCase() || 'ONE WORD ONLY'}</strong> from the passage for each answer.</p>
                    </>
                );
            case 'TRUE_FALSE_NOT_GIVEN':
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Do the following statements agree with the information given in Reading Passage {currentPart.partNumber}?</p>
                        <p><strong>TRUE</strong> if the statement agrees with the information</p>
                        <p><strong>FALSE</strong> if the statement contradicts the information</p>
                        <p><strong>NOT GIVEN</strong> if there is no information on this</p>
                    </>
                );
            case 'MATCHING_INFORMATION':
                const options = firstQuestion?.questionContent?.options || [];
                const rangeText = options.length > 0 ? `${options[0]}-${options[options.length - 1]}` : '';
                 return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Reading Passage {currentPart.partNumber} has {options.length} paragraphs, <strong>{rangeText}</strong>.</p>
                        <p>Which paragraph contains the following information?</p>
                        <p>Write the correct letter, <strong>{rangeText}</strong>, in boxes {start}-{end} on your answer sheet.</p>
                        <p><strong><em>NB</em></strong> You may use any letter more than once.</p>
                    </>
                );
            case 'MATCHING_HEADINGS':
                const headingOptions = firstQuestion?.questionContent?.options || [];
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Choose the correct heading for each paragraph from the list of headings below.</p>
                        <div className="options-list matching-options-box">
                            <h4>List of Headings</h4>
                            {headingOptions.map(opt => <p key={opt.letter}><strong>{opt.letter}</strong> {opt.text}</p>)}
                        </div>
                    </>
                );
            case 'MATCHING_FEATURES':
                const featureOptions = firstQuestion?.questionContent?.options || [];
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Match each statement with the correct feature.</p>
                        <div className="options-list matching-options-box">
                            <h4>List of Features</h4>
                            {featureOptions.map(opt => <p key={opt.letter}><strong>{opt.letter}</strong> {opt.text}</p>)}
                        </div>
                    </>
                );
            case 'MATCHING_SENTENCE_ENDINGS':
                const sentenceOptions = firstQuestion?.questionContent?.options || [];
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Complete each sentence with the correct ending.</p>
                        <div className="options-list matching-options-box">
                            {sentenceOptions.map(opt => <p key={opt.letter}><strong>{opt.letter}</strong> {opt.text}</p>)}
                        </div>
                    </>
                );
            case 'YES_NO_NOT_GIVEN':
                 return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Do the following statements agree with the claims of the writer in Reading Passage {currentPart.partNumber}?</p>
                        <p><strong>YES</strong> if the statement agrees with the claims of the writer</p>
                        <p><strong>NO</strong> if the statement contradicts the claims of the writer</p>
                        <p><strong>NOT GIVEN</strong> if it is impossible to say what the writer thinks about this</p>
                    </>
                );
            case 'SUMMARY_COMPLETION':
                return (
                     <>
                        <p><strong>{range}</strong></p>
                        <p>Complete the summary below.</p>
                        <p>Choose <strong>{firstQuestion?.wordLimit?.toUpperCase() || 'ONE WORD ONLY'}</strong> from the passage for each answer.</p>
                    </>
                );
            case 'SUMMARY_COMPLETION_OPTIONS':
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Complete the summary using the list of phrases below.</p>
                    </>
                );
            case 'MULTIPLE_CHOICE':
                 return (
                     <>
                        <p><strong>{range}</strong></p>
                        <p>Choose the correct letter, <strong>A, B, C or D</strong>.</p>
                    </>
                );
            case 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS':
                const mcmaOptions = firstQuestion?.questionContent?.options || [];
                const mcmaRangeText = mcmaOptions.length > 0 ? `${mcmaOptions[0].charAt(0)}-${mcmaOptions[mcmaOptions.length - 1].charAt(0)}` : '';
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Choose <strong>TWO</strong> letters, <strong>{mcmaRangeText}</strong>.</p>
                    </>
                );
            case 'DIAGRAM_LABEL_COMPLETION':
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Label the diagram below.</p>
                        <p>Choose <strong>{firstQuestion?.wordLimit?.toUpperCase() || 'ONE WORD ONLY'}</strong> from the passage for each answer.</p>
                    </>
                );
            case 'TABLE_COMPLETION':
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Complete the table below.</p>
                        <p>Choose <strong>{firstQuestion?.wordLimit?.toUpperCase() || 'ONE WORD ONLY'}</strong> from the passage for each answer.</p>
                    </>
                );
            default:
                return <p><strong>{range}</strong></p>;
        }
    };

    const renderGroupBody = () => {
        const firstQuestion = group.questions[0];

        switch (group.type) {
            case 'DIAGRAM_LABEL_COMPLETION':
                return (
                    <>
                        {firstQuestion.imageUrl && <img src={firstQuestion.imageUrl} alt="Diagram for questions" className="question-diagram" />}
                        {group.questions.map(q => (
                            <div id={`q-block-${q.id}`} key={q.id}>
                                <QuestionRenderer question={q} onAnswerChange={onAnswerChange} userAnswer={answers[q.id]} />
                            </div>
                        ))}
                    </>
                );
            
            case 'SUMMARY_COMPLETION':
                return (
                    <div className="summary-text-container">
                        {group.questions.map(q => (
                            <QuestionRenderer
                                key={q.id}
                                question={q}
                                onAnswerChange={onAnswerChange}
                                userAnswer={answers[q.id]}
                                wrapperTag="span" // Render as inline element
                            />
                        ))}
                    </div>
                );

            case 'TABLE_COMPLETION':
            case 'FLOW_CHART_COMPLETION':
                const firstQ = group.questions[0];
                if (!firstQ?.questionContent?.text) return null;

                return (
                    <div>
                        <div className="table-completion-container" dangerouslySetInnerHTML={{ __html: firstQ.questionContent.text }} />
                        <div className="table-completion-inputs">
                            {group.questions.map(q => (
                                <div id={`q-block-${q.id}`} key={q.id} className="table-input-row">
                                    <span className="question-number">{q.questionNumber}.</span>
                                    <QuestionRenderer 
                                        question={q} 
                                        onAnswerChange={onAnswerChange} 
                                        userAnswer={answers[q.id]} 
                                        typeOverride="FILL_IN_BLANK_INPUT_ONLY" 
                                    />
                                </div>
                            ))}
                        </div>
                    </div>
                );
            default:
                const matchingTypes = ['MATCHING_INFORMATION', 'MATCHING_HEADINGS', 'MATCHING_FEATURES', 'MATCHING_SENTENCE_ENDINGS'];
                if (matchingTypes.includes(group.type)) {
                    const groupOptions = group.questions[0]?.questionContent?.options || [];
                    return group.questions.map(q => (
                        <div id={`q-block-${q.id}`} key={q.id}>
                            <QuestionRenderer 
                                question={q} 
                                onAnswerChange={onAnswerChange} 
                                userAnswer={answers[q.id]} 
                                groupOptions={groupOptions}
                            />
                        </div>
                    ));
                }

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