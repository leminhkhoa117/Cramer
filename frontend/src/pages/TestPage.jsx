import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { testApi, testAttemptApi } from '../api/backendApi';
import TestHeader from '../components/TestHeader';
import QuestionRenderer from '../components/QuestionRenderer';
import TestFooter from '../components/TestFooter';
import HighlightableText from '../components/HighlightableText';
import { motion } from 'framer-motion';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';
import '../css/TestPage.css';
import '../css/TestHeader.css';
import '../css/TestFooter.css';

// Animation variants for staggering children
const containerVariants = {
    hidden: { opacity: 0 },
    visible: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: { y: 0, opacity: 1 }
};

// Helper to group questions for rendering
const groupQuestions = (questions) => {
    if (!questions || questions.length === 0) return [];
    
    // First, deduplicate questions by ID to prevent rendering duplicates
    const uniqueQuestions = Array.from(
        new Map(questions.map(q => [q.id, q])).values()
    );
    
    // Debug log
    if (questions.length !== uniqueQuestions.length) {
        console.warn(`⚠️ Removed ${questions.length - uniqueQuestions.length} duplicate questions`);
    }
    
    const groups = [];
    let currentGroup = {
        type: uniqueQuestions[0].questionType,
        questions: [uniqueQuestions[0]],
        startNum: uniqueQuestions[0].questionNumber,
        instructions: uniqueQuestions[0].instructions
    };
    
    for (let i = 1; i < uniqueQuestions.length; i++) {
        const q = uniqueQuestions[i];
        if (q.questionType === currentGroup.type) {
            currentGroup.questions.push(q);
        } else {
            groups.push(currentGroup);
            currentGroup = { type: q.questionType, questions: [q], startNum: q.questionNumber, instructions: q.instructions };
        }
    }
    groups.push(currentGroup);
    return groups;
};

const TestPage = () => {
    const { source, testNum, skill } = useParams();
    const navigate = useNavigate();

    const [testData, setTestData] = useState([]); // Array of FullSectionDTO
    const [attempt, setAttempt] = useState(null);
    const [currentPartIndex, setCurrentPartIndex] = useState(0);
    const [answers, setAnswers] = useState({}); // { questionId: { value: '...' } }
    const [timeLeft, setTimeLeft] = useState(3600); // 60 minutes in seconds
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const timer = setInterval(() => {
            setTimeLeft(prevTime => {
                if (prevTime <= 1) {
                    clearInterval(timer);
                    // Auto-submit logic can be added here
                    return 0;
                }
                return prevTime - 1;
            });
        }, 1000);
        return () => clearInterval(timer);
    }, []);

    useEffect(() => {
        const fetchAndStartTest = async () => {
            try {
                setLoading(true);
                const [data, attemptData] = await Promise.all([
                    testApi.getFullTest(source, testNum, skill),
                    testAttemptApi.startAttempt(source, testNum, skill)
                ]);
                setTestData(data);
                setAttempt(attemptData.data);
                setError(null);
            } catch (err) {
                setError('Failed to load test data. Please try again later.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchAndStartTest();
    }, [source, testNum, skill]);

    const allQuestions = useMemo(() => {
        return testData.flatMap(part => part.questions);
    }, [testData]);

    const handleAnswerChange = (questionId, answerValue) => {
        setAnswers(prev => ({
            ...prev,
            [questionId]: answerValue
        }));
    };

    const handleQuestionSelect = (questionNumber) => {
        const question = allQuestions.find(q => q.questionNumber === questionNumber);
        if (!question) return;

        const partIndex = testData.findIndex(part => part.id === question.sectionId);
        if (partIndex !== -1 && partIndex !== currentPartIndex) {
            setCurrentPartIndex(partIndex);
        }

        setTimeout(() => {
            const element = document.getElementById(`q-block-${question.id}`);
            if (element) {
                element.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }, 100);
    };

    const handleSubmit = async () => {
        if (!window.confirm('Are you sure you want to submit your answers? This action cannot be undone.')) {
            return;
        }
        try {
            setLoading(true);
            const result = await testAttemptApi.submitAttempt(attempt.id, answers);
            alert(`Test submitted! Your score: ${result.data.score}/${result.data.totalQuestions}`);
            navigate('/dashboard');
        } catch (err) {
            setError('Failed to submit test. Please try again.');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const currentPart = useMemo(() => {
        if (!testData || testData.length === 0) return null;
        return testData[currentPartIndex];
    }, [testData, currentPartIndex]);

    const questionGroups = useMemo(() => {
        if (!currentPart) return [];
        return groupQuestions(currentPart.questions);
    }, [currentPart]);

    const renderGroupInstructions = (group) => {
        const start = group.startNum;
        const end = group.questions[group.questions.length - 1].questionNumber;
        const range = start === end ? `Question ${start}` : `Questions ${start}-${end}`;

        switch (group.type) {
            case 'FILL_IN_BLANK':
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Complete the notes below.</p>
                        <p>Choose <strong>ONE WORD ONLY</strong> from the passage for each answer.</p>
                    </>
                );
            case 'TRUE_FALSE_NOT_GIVEN':
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Do the following statements agree with the information given in Reading Passage {currentPart.partNumber}?</p>
                        <p>In boxes {start}-{end} on your answer sheet, write</p>
                        <p><strong>TRUE</strong> if the statement agrees with the information</p>
                        <p><strong>FALSE</strong> if the statement contradicts the information</p>
                        <p><strong>NOT GIVEN</strong> if there is no information on this</p>
                    </>
                );
            case 'MATCHING_INFORMATION':
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Reading Passage {currentPart.partNumber} has several paragraphs, A-F.</p>
                        <p>Which paragraph contains the following information?</p>
                        <p>Write the correct letter, <strong>A-F</strong>, in boxes {start}-{end} on your answer sheet.</p>
                        <p><strong><em>NB</em></strong> You may use any letter more than once.</p>
                    </>
                );
            case 'YES_NO_NOT_GIVEN':
                 return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Do the following statements agree with the claims of the writer in Reading Passage {currentPart.partNumber}?</p>
                        <p>In boxes {start}-{end} on your answer sheet, write</p>
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
                        <p>Choose <strong>NO MORE THAN TWO WORDS</strong> from the passage for each answer.</p>
                        <p>Write your answers in boxes {start}-{end} on your answer sheet.</p>
                    </>
                );
            case 'SUMMARY_COMPLETION_OPTIONS':
                return (
                    <>
                        <p><strong>{range}</strong></p>
                        <p>Complete the summary using the list of phrases, A–J, below.</p>
                        <p>Write the correct letter, <strong>A–J</strong>, in boxes {start}-{end} on your answer sheet.</p>
                    </>
                );
            case 'MULTIPLE_CHOICE':
                 return (
                     <>
                        <p><strong>{range}</strong></p>
                        <p>Choose the correct letter, <strong>A, B, C or D</strong>.</p>
                        <p>Write the correct letter in boxes {start}-{end} on your answer sheet.</p>
                    </>
                );
            // Add other cases here as needed
            default:
                return <p><strong>{range}</strong>: {group.instructions}</p>;
        }
    };

    if (loading) return <div className="loading-screen">Loading test...</div>;
    if (error) return <div className="error-message">{error}</div>;
    if (!currentPart) return <div className="loading-screen">No test data found.</div>;

    return (
        <div className="test-page-wrapper">
            <TestHeader 
                testName={`IELTS Reading Test - ${source.toUpperCase()} Test ${testNum}`}
                timeLeft={timeLeft}
                onSubmit={handleSubmit}
            />
            <PanelGroup direction="horizontal" className="test-page-container">
                <Panel defaultSize={50} minSize={30}>
                    <div className="passage-container">
                        <h2 className="passage-title">{`Reading Passage ${currentPart.partNumber}`}</h2>
                        <p className="passage-instructions">You should spend about 20 minutes on Questions {currentPart.questions[0].questionNumber}–{currentPart.questions[currentPart.questions.length - 1].questionNumber}, which are based on Reading Passage {currentPart.partNumber} below.</p>
                        <HighlightableText text={currentPart.passageText} />
                    </div>
                </Panel>
                <PanelResizeHandle className="resize-handle">
                    <div className="resize-handle-icon">
                        <svg 
                            width="20" 
                            height="20" 
                            viewBox="0 0 20 20" 
                            fill="none" 
                            xmlns="http://www.w3.org/2000/svg"
                        >
                            <path 
                                d="M6 10L2 10M2 10L4 8M2 10L4 12M14 10L18 10M18 10L16 8M18 10L16 12" 
                                stroke="#6b7280" 
                                strokeWidth="1.5" 
                                strokeLinecap="round" 
                                strokeLinejoin="round"
                            />
                        </svg>
                    </div>
                </PanelResizeHandle>
                <Panel defaultSize={50} minSize={30}>
                    <div className="questions-column">
                        <motion.div
                            className="questions-container"
                            key={currentPartIndex}
                            variants={containerVariants}
                            initial="hidden"
                            animate="visible"
                        >
                            <h2>Questions</h2>
                            {questionGroups.map((group, index) => {
                                // Only show instructions if this is the first group or type changed from previous group
                                const showInstructions = index === 0 || group.type !== questionGroups[index - 1].type;
                                
                                return (
                                    <motion.div key={index} className="question-group" variants={itemVariants}>
                                        {showInstructions && (
                                            <div className="group-instructions">
                                                {renderGroupInstructions(group)}
                                            </div>
                                        )}
                                        {group.questions.map(q => (
                                            <div id={`q-block-${q.id}`} key={q.id}>
                                                <QuestionRenderer
                                                    question={q}
                                                    onAnswerChange={handleAnswerChange}
                                                    userAnswer={answers[q.id]}
                                                />
                                            </div>
                                        ))}
                                    </motion.div>
                                );
                            })}
                        </motion.div>
                    </div>
                </Panel>
            </PanelGroup>
            <TestFooter
                testData={testData}
                answers={answers}
                onQuestionSelect={handleQuestionSelect}
                onPartSelect={setCurrentPartIndex}
                currentPartIndex={currentPartIndex}
            />
        </div>
    );
};

export default TestPage;
