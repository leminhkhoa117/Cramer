import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { testApi, testAttemptApi } from '../api/backendApi';
import TestHeader from '../components/TestHeader';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';
import HighlightableText from '../components/HighlightableText';
import TestFooter from '../components/TestFooter';
import { motion } from 'framer-motion';
import QuestionGroupRenderer from '../components/QuestionGroupRenderer';
import ConfirmationModal from '../components/ConfirmationModal';
import AudioPlayer from '../components/AudioPlayer';
import ToggleSwitch from '../components/ToggleSwitch';
import '../css/TestPage.css';
import '../css/TestHeader.css';
import '../css/TestFooter.css';
import '../css/QuestionGroup.css';
import '../css/ToggleSwitch.css';

const SILENT_REVIEW_SECONDS = 120;
const FINAL_REVIEW_SECONDS = 120;

const containerVariants = {
    hidden: { opacity: 0 },
    visible: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

const groupQuestions = (questions) => {
    if (!questions || questions.length === 0) return [];
    const uniqueQuestions = Array.from(new Map(questions.map(q => [q.id, q])).values());
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
    const audioPlayerRef = useRef(null);
    const silentTimerRef = useRef(null);

    const [testData, setTestData] = useState([]);
    const [attempt, setAttempt] = useState(null);
    const [answers, setAnswers] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);

    // --- State Management ---
    const isListeningTest = skill === 'listening';
    const [displayPartIndex, setDisplayPartIndex] = useState(0);
    const [audioPartIndex, setAudioPartIndex] = useState(0);
    const [isAutoplay, setIsAutoplay] = useState(true);
    const [playTrigger, setPlayTrigger] = useState(0); // New trigger for playing audio
    
    const [readingTimeLeft, setReadingTimeLeft] = useState(3600); 
    
    const [testPhase, setTestPhase] = useState('PLAYING'); // PLAYING, FINAL_REVIEW
    const [finalReviewTime, setFinalReviewTime] = useState(FINAL_REVIEW_SECONDS);

    // --- Submission Logic ---
    const handleFinalSubmit = useCallback(async () => {
        if (!attempt) return;
        setIsConfirmModalOpen(false);
        if (silentTimerRef.current) clearTimeout(silentTimerRef.current);
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

    // --- Timers & Test Flow ---

    // Timer for Reading tests
    useEffect(() => {
        if (isListeningTest) return;
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
    }, [isListeningTest, handleFinalSubmit]);

    // Timer for FINAL review in Listening tests
    useEffect(() => {
        if (!isListeningTest || testPhase !== 'FINAL_REVIEW') return;
        const timer = setInterval(() => {
            setFinalReviewTime(prevTime => {
                if (prevTime <= 1) {
                    clearInterval(timer);
                    handleFinalSubmit();
                    return 0;
                }
                return prevTime - 1;
            });
        }, 1000);
        return () => clearInterval(timer);
    }, [isListeningTest, testPhase, handleFinalSubmit]);

    // --- Data Fetching ---
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
            } finally {
                setLoading(false);
                if (skill === 'listening' && isAutoplay) {
                    // Initial play trigger
                    setPlayTrigger(v => v + 1);
                }
            }
        };
        fetchAndStartTest();
        return () => {
            if (silentTimerRef.current) clearTimeout(silentTimerRef.current);
        };
    }, [source, testNum, skill, isAutoplay]);

    // --- Audio Logic ---

    // New dedicated effect for playing audio when triggered
    useEffect(() => {
        if (playTrigger > 0 && audioPlayerRef.current) {
            audioPlayerRef.current.play();
        }
    }, [playTrigger]);

    const handleAudioEnded = useCallback(() => {
        if (audioPartIndex < testData.length - 1) {
            // 1. Immediately load the next track
            setAudioPartIndex(prev => prev + 1);
            
            // 2. Wait for the silent period
            silentTimerRef.current = setTimeout(() => {
                // 3. After waiting, trigger the play effect
                if (isAutoplay) {
                    setPlayTrigger(v => v + 1);
                }
            }, SILENT_REVIEW_SECONDS * 1000);
        } else {
            setTestPhase('FINAL_REVIEW');
        }
    }, [audioPartIndex, testData.length, isAutoplay]);


    // --- Memoized Data ---
    const allQuestions = useMemo(() => testData.flatMap(part => part.questions), [testData]);
    
    const displayedPart = useMemo(() => {
        if (!testData || testData.length === 0) return null;
        return testData[displayPartIndex];
    }, [testData, displayPartIndex]);

    const audioPart = useMemo(() => {
        if (!testData || testData.length === 0) return null;
        return testData[audioPartIndex];
    }, [testData, audioPartIndex]);

    const questionGroups = useMemo(() => {
        if (!displayedPart) return [];
        return groupQuestions(displayedPart.questions);
    }, [displayedPart]);

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

    if (loading && !attempt) return <div className="loading-screen">Loading test...</div>;
    if (error) return <div className="error-message">{error}</div>;
    if (!displayedPart || !audioPart) return <div className="loading-screen">No test data found.</div>;

    return (
        <div className={`test-page-wrapper ${isListeningTest ? 'listening-test-active' : ''}`}>
            <TestHeader 
                testName={`IELTS ${skill.charAt(0).toUpperCase() + skill.slice(1)} Test - ${source.toUpperCase()} Test ${testNum}`}
                timeLeft={isListeningTest ? (testPhase === 'FINAL_REVIEW' ? finalReviewTime : null) : readingTimeLeft}
                onSubmit={() => setIsConfirmModalOpen(true)}
            />

            {isListeningTest && (
                <>
                    {testPhase === 'FINAL_REVIEW' && (
                        <div className="review-banner">
                            <p>Thời gian xem lại bắt đầu!</p>
                        </div>
                    )}
                    <div className="listening-controls-container">
                        <AudioPlayer
                            ref={audioPlayerRef}
                            audioUrl={audioPart.audioUrl}
                            onEnded={handleAudioEnded}
                        />
                        <ToggleSwitch
                            id="autoplay-toggle"
                            checked={isAutoplay}
                            onChange={setIsAutoplay}
                            label="Tự động phát"
                        />
                    </div>
                </>
            )}

            <PanelGroup direction="horizontal" className="test-page-container">
                <Panel defaultSize={50} minSize={30}>
                    <div className="passage-container">
                        {isListeningTest ? (
                            <>
                                {displayedPart.displayContentUrl ? (
                                    <img src={displayedPart.displayContentUrl} alt={`Visual for Part ${displayedPart.partNumber}`} className="listening-visual-content" />
                                ) : (
                                    <div className="listening-visual-placeholder">
                                        <p>Không có nội dung hình ảnh cho phần này.</p>
                                    </div>
                                )}
                            </>
                        ) : (
                            <>
                                <h2 className="passage-title">{`Reading Passage ${displayedPart.partNumber}`}</h2>
                                <p className="passage-instructions">You should spend about 20 minutes on Questions {displayedPart.questions[0].questionNumber}–{displayedPart.questions[displayedPart.questions.length - 1].questionNumber}, which are based on Reading Passage {displayedPart.partNumber} below.</p>
                                <HighlightableText text={displayedPart.passageText} />
                            </>
                        )}
                    </div>
                </Panel>
                <PanelResizeHandle className="resize-handle">
                    <div className="resize-handle-icon">
                        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M6 10L2 10M2 10L4 8M2 10L4 12M14 10L18 10M18 10L16 8M18 10L16 12" stroke="#6b7280" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                    </div>
                </PanelResizeHandle>
                <Panel defaultSize={50} minSize={30}>
                    <div className="questions-column">
                        <motion.div
                            className="questions-container"
                            key={displayPartIndex}
                            variants={containerVariants}
                            initial="hidden"
                            animate="visible"
                        >
                            <h2>Questions {displayedPart.questions[0]?.questionNumber} - {displayedPart.questions[displayedPart.questions.length - 1]?.questionNumber}</h2>
                            {questionGroups.map((group, index) => (
                                <QuestionGroupRenderer 
                                    key={index}
                                    group={group}
                                    currentPart={displayedPart}
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
                onPartSelect={setDisplayPartIndex}
                currentPartIndex={displayPartIndex}
            />

            <ConfirmationModal
                isOpen={isConfirmModalOpen}
                onClose={() => setIsConfirmModalModalOpen(false)}
                onConfirm={handleFinalSubmit}
                title="Xác nhận nộp bài"
            >
                <p>Bạn có chắc chắn muốn nộp bài không? Hành động này không thể hoàn tác.</p>
            </ConfirmationModal>
        </div>
    );
};

export default TestPage;
