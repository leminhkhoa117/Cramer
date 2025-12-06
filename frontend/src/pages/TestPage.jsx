import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate, Navigate, useLocation } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import { testApi, testAttemptApi } from '../api/backendApi';
import { HighlightProvider } from '../contexts/HighlightContext';
import TestPageContent from '../components/TestPageContent';
import FullPageLoader from '../components/FullPageLoader';
import ResumeConfirmationModal from '../components/ResumeConfirmationModal';

import '../css/TestPage.css';

const INITIAL_READING_TIME = 3600;

const TestPage = () => {
    const { source, testNum, skill } = useParams();
    const navigate = useNavigate();
    const location = useLocation();

    // Check if navigating from "Làm lại" button (forceNew flag)
    const forceNew = location.state?.forceNew || false;

    // --- Core State ---
    const [testStatus, setTestStatus] = useState('running'); // No longer need 'pending' state here
    const [testData, setTestData] = useState([]);
    const [attempt, setAttempt] = useState(null);
    const [answers, setAnswers] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    // --- Resume Modal State ---
    const [isResumeModalOpen, setIsResumeModalOpen] = useState(false);
    const [inProgressAttempt, setInProgressAttempt] = useState(null);
    const [isStartingNew, setIsStartingNew] = useState(false);

    // --- UI State ---
    const [displayPartIndex, setDisplayPartIndex] = useState(0);

    // --- Test-Specific State ---
    const [readingTimeLeft, setReadingTimeLeft] = useState(INITIAL_READING_TIME);
    const audioPlayerRefs = useRef([]);
    const [isAutoplay, setIsAutoplay] = useState(true);
    const [activeAudioIndex, setActiveAudioIndex] = useState(-1);

    const isSubmittingRef = useRef(false);
    const hasFetchedRef = useRef(false);

    // --- Data Loading and Setup ---
    const setupTestState = useCallback(async (attemptData, fullTestData) => {
        setAttempt(attemptData);
        setTestData(fullTestData);

        // Load answers if resuming an in-progress attempt
        if (attemptData.status === 'IN_PROGRESS' && attemptData.id) {
            try {
                const answersRes = await testAttemptApi.getAttemptAnswers(attemptData.id);
                const loadedAnswers = answersRes.data.reduce((acc, answer) => {
                    // Assuming userAnswer is the plain text answer
                    acc[answer.questionId] = answer.userAnswer;
                    return acc;
                }, {});
                setAnswers(loadedAnswers);
            } catch (error) {
                console.error("Failed to load previous answers:", error);
                // Continue without answers, or show an error to the user
            }
        } else {
            setAnswers({}); // Clear answers for new or completed attempts
        }

        if (attemptData.timeLeft !== null && attemptData.timeLeft < INITIAL_READING_TIME) {
            setReadingTimeLeft(attemptData.timeLeft);
        }
        if (skill === 'listening' && attemptData.currentPart !== null && attemptData.currentPart > 0) {
            setDisplayPartIndex(attemptData.currentPart);
            setActiveAudioIndex(attemptData.currentPart);
        } else if (skill === 'listening' && isAutoplay) {
            setActiveAudioIndex(0);
        }

        setLoading(false);
        setIsResumeModalOpen(false);
    }, [skill, isAutoplay]);

    useEffect(() => {
        // Prevent duplicate calls in React StrictMode
        if (hasFetchedRef.current) return;
        hasFetchedRef.current = true;

        const fetchAndStartTest = async () => {
            try {
                setLoading(true);
                // Pass forceNew to handle retake scenarios
                const attemptRes = await testAttemptApi.startAttempt(source, testNum, skill, forceNew);
                const attemptData = attemptRes.data;

                // If backend returned a COMPLETED attempt (forceNew was false), show choice modal
                if (attemptData.status === 'COMPLETED') {
                    setInProgressAttempt(attemptData);
                    setIsResumeModalOpen(true);
                    return;
                }

                const isDirty = (attemptData.timeLeft !== null && attemptData.timeLeft < INITIAL_READING_TIME) ||
                    (attemptData.currentPart !== null && attemptData.currentPart > 0) ||
                    (attemptData.status === 'IN_PROGRESS' && attemptData.id && (await testAttemptApi.getAttemptAnswers(attemptData.id)).data.length > 0); // Check if answers exist

                if (attemptData.status === 'IN_PROGRESS' && isDirty) {
                    setInProgressAttempt(attemptData);
                    setIsResumeModalOpen(true);
                } else {
                    const fullTestData = await testApi.getFullTest(source, testNum, skill);
                    setupTestState(attemptData, fullTestData);
                }
            } catch (err) {
                setError('Failed to load test data. Please try again later.');
                setLoading(false);
            }
        };
        fetchAndStartTest();
    }, [source, testNum, skill, setupTestState, forceNew]);


    // --- Modal Handlers ---
    const handleResume = async () => {
        // If the attempt is COMPLETED, redirect to review page
        if (inProgressAttempt?.status === 'COMPLETED') {
            navigate(`/test/review/${inProgressAttempt.id}`, { replace: true });
            return;
        }

        // Otherwise, resume IN_PROGRESS attempt
        try {
            setLoading(true);
            const fullTestData = await testApi.getFullTest(source, testNum, skill);
            await setupTestState(inProgressAttempt, fullTestData);
        } catch (err) {
            setError('Failed to load test data for resuming.');
            setLoading(false);
        }
    };

    const handleStartNew = async () => {
        try {
            setIsStartingNew(true);

            // Only cancel in-progress attempts (not COMPLETED ones)
            if (inProgressAttempt?.status === 'IN_PROGRESS') {
                try {
                    await testAttemptApi.cancelAttempt(inProgressAttempt.id);
                } catch (cancelError) {
                    // If attempt was already deleted (404), that's fine - continue
                    if (cancelError.response?.status !== 404) {
                        throw cancelError;
                    }
                    console.log('Previous attempt already deleted, proceeding with new test.');
                }
            }

            // Use forceNew=true to create a brand new attempt
            const newAttemptRes = await testAttemptApi.startAttempt(source, testNum, skill, true);
            const fullTestData = await testApi.getFullTest(source, testNum, skill);

            // Reset answers and other states for the new test
            setAnswers({});
            setReadingTimeLeft(INITIAL_READING_TIME);
            setDisplayPartIndex(0);
            setActiveAudioIndex(-1);

            await setupTestState(newAttemptRes.data, fullTestData); // Use await here
        } catch (err) {
            setError('Failed to start a new test. Please try again.');
            setLoading(false);
        } finally {
            setIsStartingNew(false);
            setIsResumeModalOpen(false);
        }
    };

    // --- Submission Logic ---
    const handleFinalSubmit = useCallback(async () => {
        if (!attempt || isSubmittingRef.current) return;
        isSubmittingRef.current = true;
        setIsConfirmModalOpen(false);
        try {
            setIsSubmitting(true);
            const normalizedAnswers = Object.entries(answers || {}).reduce((acc, [questionId, value]) => {
                acc[questionId] = Array.isArray(value) ? (value[0] || '') : value;
                return acc;
            }, {});

            const result = await testAttemptApi.submitAttempt(attempt.id, normalizedAnswers);
            navigate(`/test/review/${result.data.attemptId}`);
        } catch (err) {
            setError('Failed to submit test. Please try again.');
        } finally {
            setIsSubmitting(false);
            isSubmittingRef.current = false;
        }
    }, [attempt, answers, navigate]);

    // --- Timer for Reading Test ---
    useEffect(() => {
        if (testStatus !== 'running' || skill !== 'reading' || loading) return;
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
    }, [testStatus, skill, loading, handleFinalSubmit]);

    // --- Audio Logic ---
    useEffect(() => {
        if (testStatus !== 'running' || skill !== 'listening' || activeAudioIndex === -1 || loading) return;
        const player = audioPlayerRefs.current[activeAudioIndex];
        if (player) {
            player.play();
        }
    }, [activeAudioIndex, testStatus, skill, loading]);

    // --- Redirect Writing to dedicated page ---
    if (skill === 'writing') {
        return <Navigate to={`/test/writing/${source}/${testNum}`} replace />;
    }

    // --- Render Logic ---
    if (loading && !isResumeModalOpen) {
        return (
            <FullPageLoader
                key="loader"
                message="Đang tải đề thi và khởi tạo bài làm..."
                subMessage="Vui lòng chờ trong giây lát, hệ thống đang chuẩn bị bài test cho bạn."
            />
        );
    }

    if (error) return <div className="error-message">{error}</div>;

    return (
        <>
            <ResumeConfirmationModal
                isOpen={isResumeModalOpen}
                onResume={handleResume}
                onStartNew={handleStartNew}
                isStartingNew={isStartingNew}
                attemptStatus={inProgressAttempt?.status}
            />

            <AnimatePresence>
                {isSubmitting && (
                    <FullPageLoader
                        key="submitting-loader"
                        message="Đang nộp bài và chấm điểm..."
                        subMessage="Vui lòng không đóng trang trong khi hệ thống xử lý bài làm của bạn."
                    />
                )}
            </AnimatePresence>

            {attempt && testData.length > 0 && (
                <HighlightProvider>
                    <TestPageContent
                        testStatus={testStatus}
                        setTestStatus={setTestStatus}
                        testData={testData}
                        attempt={attempt}
                        answers={answers}
                        setAnswers={setAnswers}
                        loading={loading}
                        isSubmitting={isSubmitting}
                        error={error}
                        isConfirmModalOpen={isConfirmModalOpen}
                        setIsConfirmModalOpen={setIsConfirmModalOpen}
                        displayPartIndex={displayPartIndex}
                        setDisplayPartIndex={setDisplayPartIndex}
                        readingTimeLeft={readingTimeLeft}
                        audioPlayerRefs={audioPlayerRefs}
                        isAutoplay={isAutoplay}
                        setIsAutoplay={setIsAutoplay}
                        setActiveAudioIndex={setActiveAudioIndex}
                        source={source}
                        testNum={testNum}
                        skill={skill}
                        navigate={navigate}
                        handleFinalSubmit={handleFinalSubmit}
                    />
                </HighlightProvider>
            )}
        </>
    );
};

export default TestPage;
