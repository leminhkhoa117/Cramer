import { useEffect, useCallback, useRef, useState } from 'react';
import { useParams, useNavigate, Navigate, useLocation } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import { useTestStore, useTestSessionStore } from '../stores';
import { HighlightProvider } from '../contexts/HighlightContext';
import TestPageContent from '../components/TestPageContent';
import FullPageLoader from '../components/FullPageLoader';
import ResumeConfirmationModal from '../components/ResumeConfirmationModal';
import QuotaExceededModal from '../components/QuotaExceededModal';
import useAutoSave from '../hooks/useAutoSave';

import '../css/test-page.css';

const INITIAL_READING_TIME = 3600;

const TestPage = () => {
    const { source, testNum, skill } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const normalizedSkill = skill?.toLowerCase();

    // Check if navigating from "Làm lại" button (forceNew flag)
    const forceNew = location.state?.forceNew || false;

    // ============ ZUSTAND STORE STATE ============
    const {
        // Core state
        testStatus,
        testData,
        attempt,
        answers,
        loading,
        error,
        isSubmitting,
        // Modal state
        isConfirmModalOpen,
        isResumeModalOpen,
        inProgressAttempt,
        isStartingNew,
        // UI state
        displayPartIndex,
        // Timer state
        timeLeft,
        // Audio state
        isAutoplay,
        activeAudioIndex,
        // Actions
        setTestStatus,
        setTestData,
        setAttempt,
        setAnswers,
        setLoading,
        setError,
        setIsSubmitting,
        setDisplayPartIndex,
        setTimeLeft,
        setIsAutoplay,
        setActiveAudioIndex,
        openResumeModal,
        closeResumeModal,
        closeConfirmModal,
        setIsStartingNew,
        resetTestState,
    } = useTestStore();

    // Session store for API operations
    const {
        startOrResumeAttempt,
        loadTestData,
        loadAnswers,
        submitAttempt: submitAttemptApi,
        cancelAttempt: cancelAttemptApi,
    } = useTestSessionStore();

    // ============ REFS (cannot be in store) ============
    const audioPlayerRefs = useRef([]);
    const isSubmittingRef = useRef(false);
    const hasFetchedRef = useRef(false);
    const answersRef = useRef(answers);
    const handleFinalSubmitRef = useRef(null);
    const [quotaBlock, setQuotaBlock] = useState(null);
    const [isResuming, setIsResuming] = useState(false);

    // Auto-save: periodically persist progress to backend
    useAutoSave(testStatus === 'running', normalizedSkill);

    // Keep answersRef in sync
    useEffect(() => {
        answersRef.current = answers;
    }, [answers]);

    // Reset store and ref on unmount
    useEffect(() => {
        return () => {
            hasFetchedRef.current = false;
            resetTestState();
        };
    }, [resetTestState]);

    // ============ DATA LOADING AND SETUP ============
    const setupTestState = useCallback(async (attemptData, fullTestData, abortSignal) => {
        if (abortSignal?.aborted) return;

        setAttempt(attemptData);
        setTestData(fullTestData);

        // Load answers if resuming an in-progress attempt
        if (attemptData.status === 'IN_PROGRESS' && attemptData.id) {
            try {
                const answersData = await loadAnswers(attemptData.id);
                if (abortSignal?.aborted) return;
                const loadedAnswers = answersData.reduce((acc, answer) => {
                    acc[answer.questionId] = answer.userAnswer;
                    return acc;
                }, {});
                setAnswers(loadedAnswers);
            } catch (err) {
                if (abortSignal?.aborted) return;
                console.error("Failed to load previous answers:", err);
            }
        } else {
            setAnswers({});
        }

        if (attemptData.timeLeft !== null && attemptData.timeLeft < INITIAL_READING_TIME) {
            setTimeLeft(attemptData.timeLeft);
        } else {
            setTimeLeft(INITIAL_READING_TIME);
        }

        if (skill === 'listening' && attemptData.currentPart !== null && attemptData.currentPart > 0) {
            setDisplayPartIndex(attemptData.currentPart);
            setActiveAudioIndex(attemptData.currentPart);
        } else if (skill === 'listening' && isAutoplay) {
            setActiveAudioIndex(0);
        }

        setLoading(false);
        closeResumeModal();
    }, [skill, isAutoplay, setAttempt, setTestData, setAnswers, setTimeLeft, setDisplayPartIndex, setActiveAudioIndex, setLoading, closeResumeModal, loadAnswers]);

    // Initial data fetch
    useEffect(() => {
        if (hasFetchedRef.current) return;
        hasFetchedRef.current = true;

        const abortController = new AbortController();

        const fetchAndStartTest = async () => {
            try {
                setLoading(true);
                setTestStatus('running');

                const attemptData = await startOrResumeAttempt(source, testNum, skill, forceNew);

                if (abortController.signal.aborted) return;

                // If backend returned a COMPLETED attempt, show choice modal
                if (attemptData.status === 'COMPLETED') {
                    openResumeModal(attemptData);
                    setLoading(false);
                    return;
                }

                // Check if attempt is "dirty" (has progress)
                let hasAnswers = false;
                if (attemptData.status === 'IN_PROGRESS' && attemptData.id) {
                    try {
                        const answersData = await loadAnswers(attemptData.id);
                        hasAnswers = answersData.length > 0;
                    } catch {
                        hasAnswers = false;
                    }
                }
                if (abortController.signal.aborted) return;

                const isDirty = (attemptData.timeLeft !== null && attemptData.timeLeft < INITIAL_READING_TIME) ||
                    (attemptData.currentPart !== null && attemptData.currentPart > 0) ||
                    hasAnswers;

                if (attemptData.status === 'IN_PROGRESS' && isDirty) {
                    openResumeModal(attemptData);
                    setLoading(false);
                } else {
                    const fullTestData = await loadTestData(source, testNum, skill);
                    if (abortController.signal.aborted) return;
                    await setupTestState(attemptData, fullTestData, abortController.signal);
                }
            } catch (err) {
                if (abortController.signal.aborted) return;
                // U2 (BUG_AUDIT): map 402 to QuotaExceededModal instead of generic error
                if (err?.response?.status === 402) {
                    setQuotaBlock(err.response.data);
                    setLoading(false);
                    return;
                }
                setError('Không thể tải đề thi. Vui lòng thử lại sau.');
                setLoading(false);
            }
        };

        fetchAndStartTest();

        return () => {
            abortController.abort();
            // U5 (BUG_AUDIT): removed hasFetchedRef.current = false — resetting in cleanup
            // causes StrictMode to re-run the fetch (double billing in dev).
            // On real navigation, the component unmounts fully and a new ref is created.
        };
    }, [source, testNum, skill, forceNew, setupTestState, startOrResumeAttempt, loadTestData, loadAnswers, setLoading, setTestStatus, setError, openResumeModal]);

    // ============ MODAL HANDLERS ============
    const handleResume = useCallback(async () => {
        if (inProgressAttempt?.status === 'COMPLETED') {
            navigate(`/test/review/${inProgressAttempt.id}`, { replace: true });
            return;
        }

        try {
            setIsResuming(true);
            setLoading(true);
            const fullTestData = await loadTestData(source, testNum, skill);
            await setupTestState(inProgressAttempt, fullTestData);
        } catch (err) {
            setError('Failed to load test data for resuming.');
            setLoading(false);
            setIsResuming(false);
        }
    }, [inProgressAttempt, navigate, source, testNum, skill, loadTestData, setupTestState, setLoading, setError]);

    const handleStartNew = useCallback(async () => {
        try {
            setIsStartingNew(true);

            // Note: The backend's startOrResumeAttempt with forceNew=true already handles
            // cancelling IN_PROGRESS attempts, so we don't need to cancel here first.
            // This also avoids race conditions where the attempt gets cancelled twice.

            const newAttemptData = await startOrResumeAttempt(source, testNum, skill, true);
            const fullTestData = await loadTestData(source, testNum, skill);

            // Reset state for new test
            setAnswers({});
            setTimeLeft(INITIAL_READING_TIME);
            setDisplayPartIndex(0);
            setActiveAudioIndex(-1);

            await setupTestState(newAttemptData, fullTestData);
        } catch (err) {
            setError('Failed to start a new test. Please try again.');
            setLoading(false);
        } finally {
            setIsStartingNew(false);
            closeResumeModal();
        }
    }, [source, testNum, skill, startOrResumeAttempt, loadTestData, setupTestState, setIsStartingNew, setAnswers, setTimeLeft, setDisplayPartIndex, setActiveAudioIndex, setError, setLoading, closeResumeModal]);

    // ============ SUBMISSION LOGIC ============
    const handleFinalSubmit = useCallback(async () => {
        if (!attempt || isSubmittingRef.current) return;
        isSubmittingRef.current = true;
        closeConfirmModal();

        try {
            setTestStatus('submitted'); // Stop auto-save before submission
            setIsSubmitting(true);
            const currentAnswers = answersRef.current;
            const normalizedAnswers = Object.entries(currentAnswers || {}).reduce((acc, [questionId, value]) => {
                acc[questionId] = Array.isArray(value) ? (value[0] || '') : value;
                return acc;
            }, {});

            const result = await submitAttemptApi(attempt.id, normalizedAnswers);
            navigate(`/test/review/${result.attemptId}`);
        } catch (err) {
            setError('Failed to submit test. Please try again.');
        } finally {
            setIsSubmitting(false);
            isSubmittingRef.current = false;
        }
    }, [attempt, navigate, closeConfirmModal, setTestStatus, setIsSubmitting, setError, submitAttemptApi]);

    // Keep handleFinalSubmitRef updated
    useEffect(() => {
        handleFinalSubmitRef.current = handleFinalSubmit;
    }, [handleFinalSubmit]);

    // ============ TIMER FOR READING TEST ============
    useEffect(() => {
        // Only run timer for reading tests when not loading and time is set
        if (testStatus !== 'running' || skill !== 'reading' || loading) return;

        // Check current timeLeft directly from store to ensure it's properly initialized
        const currentTimeLeft = useTestStore.getState().timeLeft;
        if (currentTimeLeft <= 0) {
            console.log('⏱️ Timer: Waiting for timeLeft to be initialized...');
            return;
        }

        // console.log('⏱️ Timer: Starting countdown from', currentTimeLeft, 'seconds');

        const timer = setInterval(() => {
            const currentTime = useTestStore.getState().timeLeft;
            if (currentTime <= 1) {
                clearInterval(timer);
                console.log('⏱️ Timer: Time up! Submitting...');
                handleFinalSubmitRef.current?.();
                setTimeLeft(0);
            } else {
                setTimeLeft(currentTime - 1);
            }
        }, 1000);

        return () => {
            // console.log('⏱️ Timer: Cleanup');
            clearInterval(timer);
        };
    }, [testStatus, skill, loading, setTimeLeft, timeLeft]);


    // ============ AUDIO AUTOPLAY LOGIC ============
    useEffect(() => {
        if (testStatus !== 'running' || skill !== 'listening' || activeAudioIndex === -1 || loading) return;
        const player = audioPlayerRefs.current[activeAudioIndex];
        if (player) {
            player.play();
        }
    }, [activeAudioIndex, testStatus, skill, loading]);

    // ============ SKILL ROUTE GUARDS ============
    if (!['reading', 'listening', 'writing'].includes(normalizedSkill)) {
        return <Navigate to="/courses" replace />;
    }

    if (normalizedSkill === 'writing') {
        return <Navigate to={`/test/writing/${source}/${testNum}`} replace />;
    }

    // ============ RENDER LOGIC ============
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
            <QuotaExceededModal
                isOpen={!!quotaBlock}
                billingResult={quotaBlock}
                onClose={() => { setQuotaBlock(null); navigate('/dashboard'); }}
                onBuyLua={() => navigate('/subscription?tab=lua')}
                onUpgrade={() => navigate('/subscription')}
            />

            <ResumeConfirmationModal
                isOpen={isResumeModalOpen}
                onResume={handleResume}
                onStartNew={handleStartNew}
                isStartingNew={isStartingNew}
                isResuming={isResuming}
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
                        source={source}
                        testNum={testNum}
                        skill={skill}
                        audioPlayerRefs={audioPlayerRefs}
                        handleFinalSubmit={handleFinalSubmit}
                    />
                </HighlightProvider>
            )}
        </>
    );
};

export default TestPage;
