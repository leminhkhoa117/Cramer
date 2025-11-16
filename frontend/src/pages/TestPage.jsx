import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { testApi, testAttemptApi } from '../api/backendApi';
import TestHeader from '../components/TestHeader';
import HighlightableText from '../components/HighlightableText';
import TestFooter from '../components/TestFooter';
import { motion } from 'framer-motion';
import QuestionGroupRenderer from '../components/QuestionGroupRenderer';
import ConfirmationModal from '../components/ConfirmationModal';
import StartTestModal from '../components/StartTestModal';
import AudioPlayer from '../components/AudioPlayer';
import ToggleSwitch from '../components/ToggleSwitch';
import TestLayout from '../components/TestLayout';
import '../css/TestPage.css';
import '../css/TestHeader.css';
import '../css/TestFooter.css';
import '../css/QuestionGroup.css';
import '../css/ToggleSwitch.css';
import '../css/StartTestModal.css';

const containerVariants = {
    hidden: { opacity: 0 },
    visible: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

const groupQuestionsFromLayout = (part) => {
    if (!part || !part.questions || part.questions.length === 0) return [];

    const questionsMap = new Map(part.questions.map(q => [q.questionNumber, q]));

    // New logic for Listening tests with sectionLayout
    if (part.sectionLayout && part.sectionLayout.blocks) {
        return part.sectionLayout.blocks.map(block => {
            const blockQuestions = block.question_numbers
                .map(num => questionsMap.get(num))
                .filter(Boolean); // Filter out any undefined questions

            return {
                ...block, // Spread all properties from the block (block_type, content, etc.)
                questions: blockQuestions,
                startNum: blockQuestions[0]?.questionNumber,
            };
        });
    }

    // Fallback logic for Reading tests or old data structure
    const uniqueQuestions = Array.from(new Map(part.questions.map(q => [q.id, q])).values());
    const groups = [];
    if (uniqueQuestions.length === 0) return groups;

    let currentGroup = { type: uniqueQuestions[0].questionType, questions: [uniqueQuestions[0]], startNum: uniqueQuestions[0].questionNumber };
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
    
    // --- Core State ---
    const [testStatus, setTestStatus] = useState('pending'); // pending, running, submitted
    const [testData, setTestData] = useState([]);
    const [attempt, setAttempt] = useState(null);
    const [answers, setAnswers] = useState({});
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);

    // --- UI State ---
    const [displayPartIndex, setDisplayPartIndex] = useState(0);
    
    // --- Reading Test State ---
    const [readingTimeLeft, setReadingTimeLeft] = useState(3600); 
    
    // --- Listening Test State ---
    const audioPlayerRefs = useRef([]);
    const [isAutoplay, setIsAutoplay] = useState(true);
    const [activeAudioIndex, setActiveAudioIndex] = useState(-1); // -1: none, 0-3: part 1-4

    const isListeningTest = skill === 'listening';

    // --- Submission Logic ---
    const handleFinalSubmit = useCallback(async () => {
        if (!attempt) return;
        setIsConfirmModalOpen(false);
        try {
            setLoading(true);
            const result = await testAttemptApi.submitAttempt(attempt.id, answers);
            navigate(`/test/review/${result.data.attemptId}`);
        } catch (err) {
            setError('Failed to submit test. Please try again.');
        } finally {
            setLoading(false);
        }
    }, [attempt, answers, navigate]);

    // --- Timer for Reading Test ---
    useEffect(() => {
        if (testStatus !== 'running' || isListeningTest) return;
        const timer = setInterval(() => {
            setReadingTimeLeft(prevTime => {
                if (prevTime <= 1) {
                    clearInterval(timer);
                    handleFinalSubmit();
                    return 0;
                }
                return prevTime - 1;
            });
        }, 1000);
        return () => clearInterval(timer);
    }, [testStatus, isListeningTest, handleFinalSubmit]);

    // --- Data Fetching ---
    useEffect(() => {
        if (testStatus !== 'running') return;

        const fetchAndStartTest = async () => {
            try {
                setLoading(true);
                const [data, attemptData] = await Promise.all([
                    testApi.getFullTest(source, testNum, skill),
                    testAttemptApi.startAttempt(source, testNum, skill)
                ]);
                setTestData(data);
                setAttempt(attemptData.data);
                if (isListeningTest && data.length > 0) {
                    audioPlayerRefs.current = data.map((_, i) => audioPlayerRefs.current[i] ?? React.createRef());
                }
                setError(null);
                if (isListeningTest && isAutoplay) {
                    setActiveAudioIndex(0); // Trigger first audio
                }
            } catch (err) {
                setError('Failed to load test data. Please try again later.');
            } finally {
                setLoading(false);
            }
        };
        fetchAndStartTest();
    }, [testStatus, source, testNum, skill, isAutoplay, isListeningTest]);

    // --- Audio Logic ---
    useEffect(() => {
        if (testStatus !== 'running' || !isListeningTest || activeAudioIndex === -1) return;
        
        const player = audioPlayerRefs.current[activeAudioIndex];
        if (player) {
            player.play();
        }
    }, [activeAudioIndex, testStatus, isListeningTest]);

    const handleAudioEnded = useCallback((endedIndex) => {
        if (isAutoplay && endedIndex < testData.length - 1) {
            setActiveAudioIndex(endedIndex + 1);
        }
    }, [isAutoplay, testData.length]);

    // --- Memoized Data ---
    const allQuestions = useMemo(() => testData.flatMap(part => part.questions), [testData]);
    const displayedPart = useMemo(() => testData[displayPartIndex] || null, [testData, displayPartIndex]);
    const questionGroups = useMemo(() => displayedPart ? groupQuestionsFromLayout(displayedPart) : [], [displayedPart]);

    // --- Dynamic Layout Logic ---
    const showLeftPanel = useMemo(() => {
        if (!isListeningTest) return true; // Always show for Reading
        if (displayedPart?.displayContentUrl) return true; // Show for listening if URL exists
        return false; // Default for listening is hidden
    }, [isListeningTest, displayedPart]);

    const leftPanelContent = useMemo(() => {
        if (isListeningTest) {
            if (displayedPart?.displayContentUrl) {
                return <img src={displayedPart.displayContentUrl} alt="Test visual aid" className="listening-visual-content" />;
            }
            // The panel is collapsed, so this won't be visible, but it's good practice to have a fallback.
            return null; 
        }
        // Reading test logic
        if (displayedPart) {
            return (
                <>
                    <h2 className="passage-title">{`Reading Passage ${displayedPart.partNumber}`}</h2>
                    <p className="passage-instructions">You should spend about 20 minutes on Questions {displayedPart.questions[0].questionNumber}–{displayedPart.questions[displayedPart.questions.length - 1].questionNumber}, which are based on Reading Passage {displayedPart.partNumber} below.</p>
                    <HighlightableText text={displayedPart.passageText} />
                </>
            );
        }
        return null;
    }, [isListeningTest, displayedPart]);

    // --- Handlers ---
    const handleAnswerChange = (questionId, answerValue) => {
        setAnswers(prev => ({ ...prev, [questionId]: answerValue }));
    };

    const handleQuestionSelect = (questionNumber) => {
        const question = allQuestions.find(q => q.questionNumber === questionNumber);
        if (!question) return;
        const partIndex = testData.findIndex(part => part.id === question.sectionId);
        if (partIndex !== -1 && partIndex !== displayPartIndex) {
            setDisplayPartIndex(partIndex);
        }
        setTimeout(() => {
            const element = document.getElementById(`q-block-${question.id}`);
            if (element) element.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 100);
    };

    // --- Render Logic ---
    if (testStatus === 'pending') {
        return (
            <StartTestModal
                isOpen={true}
                onConfirm={() => setTestStatus('running')}
                onClose={() => navigate('/courses')}
                skill={skill}
            />
        );
    }

    if (loading && !attempt) return <div className="loading-screen">Loading test...</div>;
    if (error) return <div className="error-message">{error}</div>;
    if (!displayedPart) return <div className="loading-screen">No test data found.</div>;

    return (
        <div className={`test-page-wrapper ${isListeningTest ? 'listening-test-active' : ''}`}>
            <TestHeader 
                testName={`IELTS ${skill.charAt(0).toUpperCase() + skill.slice(1)} Test - ${source.toUpperCase()} Test ${testNum}`}
                timeLeft={isListeningTest ? null : readingTimeLeft}
                onSubmit={() => setIsConfirmModalOpen(true)}
            />

            {isListeningTest && (
                <div className="listening-controls-container">
                    <div className="audio-players-wrapper">
                        {testData.map((part, index) => (
                            <AudioPlayer
                                key={part.id}
                                ref={el => audioPlayerRefs.current[index] = el}
                                index={index}
                                audioUrl={part.audioUrl}
                                onEnded={handleAudioEnded}
                            />
                        ))}
                    </div>
                    <ToggleSwitch
                        id="autoplay-toggle"
                        checked={isAutoplay}
                        onChange={setIsAutoplay}
                        label="Tự động phát"
                    />
                </div>
            )}

            <TestLayout showLeftPanel={showLeftPanel} leftPanelContent={leftPanelContent}>
                <div className="questions-column">
                    <motion.div
                        className="questions-container"
                        key={displayPartIndex}
                        variants={containerVariants}
                        initial="hidden"
                        animate="visible"
                    >
                        {questionGroups.map((group, index) => (
                            <QuestionGroupRenderer 
                                key={index}
                                group={group}
                                onAnswerChange={handleAnswerChange}
                                answers={answers}
                            />
                        ))}
                    </motion.div>
                </div>
            </TestLayout>
            <TestFooter
                testData={testData}
                answers={answers}
                onQuestionSelect={handleQuestionSelect}
                onPartSelect={setDisplayPartIndex}
                currentPartIndex={displayPartIndex}
            />

            <ConfirmationModal
                isOpen={isConfirmModalOpen}
                onClose={() => setIsConfirmModalOpen(false)}
                onConfirm={handleFinalSubmit}
                title="Xác nhận nộp bài"
            >
                <p>Bạn có chắc chắn muốn nộp bài không? Hành động này không thể hoàn tác.</p>
            </ConfirmationModal>
        </div>
    );
};

export default TestPage;
