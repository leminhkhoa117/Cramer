import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { testApi, testAttemptApi } from '../api/backendApi';
import TestHeader from '../components/TestHeader';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';
import HighlightableText from '../components/HighlightableText';
import TestFooter from '../components/TestFooter';
import { motion } from 'framer-motion';
import QuestionGroupRenderer from '../components/QuestionGroupRenderer';
import ConfirmationModal from '../components/ConfirmationModal'; // Import the modal
import '../css/TestPage.css';
import '../css/TestHeader.css';
import '../css/TestFooter.css';
import '../css/QuestionGroup.css';

// Animation variants for staggering children
const containerVariants = {
    hidden: { opacity: 0 },
    visible: { opacity: 1, transition: { staggerChildren: 0.1 } }
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
    if (uniqueQuestions.length === 0) return groups;

    let currentGroup = {
        type: uniqueQuestions[0].questionType,
        questions: [uniqueQuestions[0]],
        startNum: uniqueQuestions[0].questionNumber,
    };
    
    for (let i = 1; i < uniqueQuestions.length; i++) {
        const q = uniqueQuestions[i];
        if (q.questionType === currentGroup.type) {
            currentGroup.questions.push(q);
        } else {
            groups.push(currentGroup);
            currentGroup = { type: q.questionType, questions: [q], startNum: q.questionNumber };
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
    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);

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

    // This function will now just open the confirmation modal
    const handleSubmit = () => {
        setIsConfirmModalOpen(true);
    };

    // This function contains the actual submission logic
    const handleConfirmSubmit = async () => {
        setIsConfirmModalOpen(false); // Close the modal
        try {
            setLoading(true);
            const result = await testAttemptApi.submitAttempt(attempt.id, answers);
            // Navigate to the new review page on success
            navigate(`/test/review/${result.data.attemptId}`);
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
                            {questionGroups.map((group, index) => (
                                <QuestionGroupRenderer 
                                    key={index}
                                    group={group}
                                    currentPart={currentPart}
                                    onAnswerChange={handleAnswerChange}
                                    answers={answers}
                                />
                            ))}
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

            <ConfirmationModal
                isOpen={isConfirmModalOpen}
                onClose={() => setIsConfirmModalOpen(false)}
                onConfirm={handleConfirmSubmit}
                title="Xác nhận nộp bài"
            >
                <p>Bạn có chắc chắn muốn nộp bài không? Hành động này không thể hoàn tác.</p>
            </ConfirmationModal>
        </div>
    );
};

export default TestPage;
