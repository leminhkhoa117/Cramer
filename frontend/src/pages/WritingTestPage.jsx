import React, { useEffect, useCallback, useRef, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
// Note: shallow import removed - using direct destructuring pattern for stable references
import { sanitizeHtml } from '../utils/sanitize';
import { AnimatePresence } from 'framer-motion';
import { useTestStore, useTestSessionStore } from '../stores';
import TestHeader from '../components/TestHeader';
import TestFooter from '../components/TestFooter';
import FullPageLoader from '../components/FullPageLoader';
import ResumeConfirmationModal from '../components/ResumeConfirmationModal';
import ExitTestModal from '../components/ExitTestModal';
import ConfirmationModal from '../components/ConfirmationModal';
import GradingQuotaInfo from '../components/GradingQuotaInfo';
import { PanelGroup, Panel, PanelResizeHandle } from 'react-resizable-panels';

// Reuse TestPage styles
import '../css/test-page.css';
import '../css/test-header.css';
import '../css/test-footer.css';
import '../css/writing-test-page.css';

// Initial time: 60 minutes for writing test (combined Task 1 + Task 2)
const INITIAL_WRITING_TIME = 3600;

const WritingTestPage = () => {
    const { source, testNum } = useParams();
    const navigate = useNavigate();
    const location = useLocation();

    // Check if navigating from "Làm lại" button (forceNew flag)
    const forceNew = location.state?.forceNew || false;

    // --- Zustand Store (direct destructuring for stable references) ---
    const {
        // State
        testStatus,
        testData,
        attempt,
        essays,
        loading,
        error,
        isSubmitting,
        isConfirmModalOpen,
        isResumeModalOpen,
        isExitModalOpen,
        inProgressAttempt,
        isStartingNew,
        isSavingProgress,
        activeTask,
        timeLeft,
        // Actions
        setTestStatus,
        setTestData,
        setAttempt,
        setEssay,
        setEssays,
        setLoading,
        setError,
        setIsSubmitting,
        setActiveTask,
        setTimeLeft,
        decrementTime,
        openConfirmModal,
        closeConfirmModal,
        openResumeModal,
        closeResumeModal,
        openExitModal,
        closeExitModal,
        setIsStartingNew,
        setIsSavingProgress,
        getWordCount,
        resetTestState,
    } = useTestStore();

    // --- Session Store Actions (direct destructuring for stable references) ---
    const {
        startOrResumeAttempt,
        loadTestData,
        loadEssays,
        saveProgress,
        submitWriting,
        cancelAttempt
    } = useTestSessionStore();

    // --- Refs (minimal local state) ---
    const isSubmittingRef = useRef(false);
    const hasFetchedRef = useRef(false);
    const handleFinalSubmitRef = useRef(null);

    // --- Computed Values ---
    const wordCount = getWordCount(activeTask);
    const minWords = activeTask === 1 ? 150 : 250;

    // Compute word counts directly from essays with stable primitive dependencies
    const wordCounts = useMemo(() => {
        const countWords = (text) => {
            if (!text || !text.trim()) return 0;
            return text.trim().split(/\s+/).filter(Boolean).length;
        };
        return {
            1: { current: countWords(essays?.[1] || ''), min: 150 },
            2: { current: countWords(essays?.[2] || ''), min: 250 },
        };
    }, [essays?.[1], essays?.[2]]);

    // --- Reset store on unmount ---
    useEffect(() => {
        // Initialize with proper state
        setLoading(true);
        setTestStatus('running');
        setTimeLeft(INITIAL_WRITING_TIME);
        setActiveTask(1);
        setEssays({ 1: '', 2: '' });

        return () => {
            hasFetchedRef.current = false;
            resetTestState();
        };
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    // --- Data Loading and Setup ---
    const setupTestState = useCallback(async (attemptData, fullTestData, abortSignal) => {
        if (abortSignal?.aborted) return;

        setAttempt(attemptData);
        setTestData(fullTestData);

        // Load essays if resuming an in-progress attempt
        if (attemptData.status === 'IN_PROGRESS' && attemptData.id) {
            try {
                const submissions = await loadEssays(attemptData.id);
                if (abortSignal?.aborted) return;
                const loadedEssays = { 1: '', 2: '' };
                submissions.forEach(sub => {
                    loadedEssays[sub.taskNumber] = sub.essayText || '';
                });
                setEssays(loadedEssays);
            } catch (err) {
                if (abortSignal?.aborted) return;
                console.error("Failed to load previous essays:", err);
            }
        } else {
            setEssays({ 1: '', 2: '' });
        }

        if (attemptData.timeLeft !== null && attemptData.timeLeft < INITIAL_WRITING_TIME) {
            setTimeLeft(attemptData.timeLeft);
        }

        setLoading(false);
        closeResumeModal();
    }, [setAttempt, setTestData, setEssays, setTimeLeft, setLoading, closeResumeModal, loadEssays]);

    // --- Initial Load Effect ---
    useEffect(() => {
        if (hasFetchedRef.current) return;
        hasFetchedRef.current = true;

        const abortController = new AbortController();

        const fetchAndStartTest = async () => {
            try {
                setLoading(true);
                const attemptData = await startOrResumeAttempt(source, testNum, 'writing', forceNew);

                if (abortController.signal.aborted) return;

                // If backend returned a COMPLETED attempt, show choice modal
                if (attemptData.status === 'COMPLETED') {
                    openResumeModal(attemptData);
                    return;
                }

                // If forceNew is true, skip the resume modal
                if (!forceNew) {
                    const isDirty = attemptData.timeLeft !== null && attemptData.timeLeft < INITIAL_WRITING_TIME;

                    if (attemptData.status === 'IN_PROGRESS' && isDirty) {
                        try {
                            const submissions = await loadEssays(attemptData.id);
                            if (abortController.signal.aborted) return;
                            const hasEssays = submissions.some(sub => sub.essayText && sub.essayText.trim());
                            if (hasEssays || attemptData.timeLeft < INITIAL_WRITING_TIME) {
                                openResumeModal(attemptData);
                                return;
                            }
                        } catch (e) {
                            // Continue with normal flow
                        }
                    }
                }

                if (abortController.signal.aborted) return;

                const fullTestData = await loadTestData(source, testNum, 'writing');
                if (abortController.signal.aborted) return;
                setupTestState(attemptData, fullTestData, abortController.signal);
            } catch (err) {
                if (abortController.signal.aborted) return;
                console.error('Error starting writing test:', err);
                setError('Không thể tải đề Writing. Vui lòng thử lại sau.');
                setLoading(false);
            }
        };
        fetchAndStartTest();

        return () => {
            abortController.abort();
            hasFetchedRef.current = false;
        };
    }, [source, testNum, forceNew, setupTestState, startOrResumeAttempt, loadTestData, loadEssays, openResumeModal, setError, setLoading]);

    // --- Modal Handlers ---
    const handleResume = useCallback(async () => {
        if (inProgressAttempt?.status === 'COMPLETED') {
            navigate(`/test/writing/review/${inProgressAttempt.id}`, { replace: true });
            return;
        }

        try {
            setLoading(true);
            const fullTestData = await loadTestData(source, testNum, 'writing');
            await setupTestState(inProgressAttempt, fullTestData);
        } catch (err) {
            setError('Không thể tải dữ liệu bài làm trước đó.');
            setLoading(false);
        }
    }, [inProgressAttempt, navigate, setLoading, loadTestData, setupTestState, source, testNum, setError]);

    const handleStartNew = useCallback(async () => {
        try {
            setIsStartingNew(true);

            // Use forceNew=true to cancel all IN_PROGRESS and create new attempt
            const newAttemptData = await startOrResumeAttempt(source, testNum, 'writing', true);
            const fullTestData = await loadTestData(source, testNum, 'writing');

            setEssays({ 1: '', 2: '' });
            setTimeLeft(INITIAL_WRITING_TIME);
            setActiveTask(1);

            await setupTestState(newAttemptData, fullTestData);
        } catch (err) {
            setError('Không thể bắt đầu bài làm mới. Vui lòng thử lại.');
            setLoading(false);
        } finally {
            setIsStartingNew(false);
            closeResumeModal();
        }
    }, [source, testNum, startOrResumeAttempt, loadTestData, setupTestState, setEssays, setTimeLeft, setActiveTask, setError, setLoading, setIsStartingNew, closeResumeModal]);

    // --- Submission Logic ---
    const handleFinalSubmit = useCallback(async () => {
        if (!attempt || isSubmittingRef.current) return;
        isSubmittingRef.current = true;
        closeConfirmModal();

        try {
            setIsSubmitting(true);
            await submitWriting(attempt.id, essays);
            navigate(`/test/writing/review/${attempt.id}`);
        } catch (err) {
            console.error('Error submitting writing test:', err);
            setError('Không thể nộp bài. Vui lòng thử lại.');
        } finally {
            setIsSubmitting(false);
            isSubmittingRef.current = false;
        }
    }, [attempt, essays, navigate, closeConfirmModal, setIsSubmitting, submitWriting, setError]);

    // Keep handleFinalSubmitRef updated
    useEffect(() => {
        handleFinalSubmitRef.current = handleFinalSubmit;
    }, [handleFinalSubmit]);

    // --- Timer ---
    useEffect(() => {
        if (testStatus !== 'running' || loading) return;

        const timer = setInterval(() => {
            const currentTime = useTestStore.getState().timeLeft;
            if (currentTime <= 1) {
                clearInterval(timer);
                handleFinalSubmitRef.current?.();
                return;
            }
            decrementTime();
        }, 1000);

        return () => clearInterval(timer);
    }, [testStatus, loading, decrementTime]);

    // --- Essay Change Handler ---
    const handleEssayChange = useCallback((taskNumber, text) => {
        setEssay(taskNumber, text);
    }, [setEssay]);

    // --- Exit Handlers ---
    const handleExitRequest = useCallback(() => openExitModal(), [openExitModal]);

    const handleAbort = useCallback(async () => {
        if (!attempt) return;
        try {
            setIsSavingProgress(true);
            await cancelAttempt(attempt.id);
            closeExitModal();
            navigate('/dashboard', { state: { refreshData: true } });
        } catch (err) {
            console.error('Failed to cancel attempt:', err);
            // Handle already deleted (404) or already completed/cancelled (400) gracefully
            const status = err.response?.status;
            if (status === 404 || status === 400) {
                // Attempt was already deleted, cancelled, or completed - just navigate away
                console.log('Attempt already processed, navigating to dashboard');
                closeExitModal();
                navigate('/dashboard', { state: { refreshData: true } });
            } else {
                alert('Không thể huỷ lần làm bài. Vui lòng thử lại.');
            }
        } finally {
            setIsSavingProgress(false);
        }
    }, [attempt, navigate, cancelAttempt, closeExitModal, setIsSavingProgress]);

    const handleSaveAndExit = useCallback(async () => {
        if (!attempt) return;

        try {
            setIsSavingProgress(true);
            await saveProgress(attempt.id, {
                essays,
                timeLeft,
                currentPart: activeTask,
                answers: {},
            });
            closeExitModal();
            navigate('/dashboard');
        } catch (err) {
            console.error('Failed to save progress:', err);
            alert('Không thể lưu tiến trình. Vui lòng thử lại.');
        } finally {
            setIsSavingProgress(false);
        }
    }, [attempt, essays, timeLeft, activeTask, navigate, saveProgress, closeExitModal, setIsSavingProgress]);

    // --- Get Task Data ---
    const currentTask = useMemo(() => {
        if (!testData || testData.length === 0) return null;
        return testData.find(section => section.partNumber === activeTask);
    }, [testData, activeTask]);

    // --- Render Logic ---
    if (loading && !isResumeModalOpen) {
        return (
            <FullPageLoader
                key="loader"
                message="Đang tải đề Writing..."
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
                        message="Đang nộp bài và gửi chấm điểm..."
                        subMessage="Vui lòng không đóng trang. AI đang phân tích bài viết của bạn."
                    />
                )}
            </AnimatePresence>

            <ExitTestModal
                isOpen={isExitModalOpen}
                onClose={closeExitModal}
                onSaveAndExit={handleSaveAndExit}
                onAbort={handleAbort}
                isSaving={isSavingProgress}
            />

            <ConfirmationModal
                isOpen={isConfirmModalOpen}
                onClose={closeConfirmModal}
                onConfirm={handleFinalSubmit}
                title="Xác nhận nộp bài"
                confirmText="Nộp bài"
            >
                <p>Bạn có chắc chắn muốn nộp bài Writing?</p>
                <div className="submit-summary">
                    <div className="submit-summary-item">
                        <span>Task 1:</span>
                        <span>{getWordCount(1)} từ</span>
                    </div>
                    <div className="submit-summary-item">
                        <span>Task 2:</span>
                        <span>{getWordCount(2)} từ</span>
                    </div>
                </div>
                <GradingQuotaInfo />
                <div className="submit-info">
                    <p className="submit-info__ai">
                        🤖 Bài làm sẽ được chấm điểm chi tiết bởi AI theo 4 tiêu chí IELTS.
                    </p>
                    <p className="submit-info__billing">
                        💡 <strong>Lưu ý:</strong> Bạn chỉ bị trừ lượt chấm nâng cao khi AI
                        trả về kết quả thành công. Nếu có lỗi xảy ra, lượt chấm sẽ được hoàn lại.
                    </p>
                </div>
            </ConfirmationModal>

            {attempt && testData.length > 0 && (
                <div className="test-page-wrapper writing-test-wrapper">
                    <TestHeader
                        testName={`${source.toUpperCase()} Test ${testNum} - Writing`}
                        timeLeft={timeLeft}
                        onSubmit={openConfirmModal}
                        onExit={handleExitRequest}
                        isSubmitting={isSubmitting}
                    />

                    {/* Main Content - Reusing test-page-container structure */}
                    <div className="test-page-container">
                        <PanelGroup direction="horizontal">
                            {/* Left Panel - Task Prompt (like passage-container) */}
                            <Panel defaultSize={50} minSize={30}>
                                <div className="passage-container writing-prompt-panel">
                                    <div className="task-header">
                                        <h2 className="passage-title">WRITING TASK {activeTask}</h2>
                                        <p className="passage-instructions">
                                            {activeTask === 1
                                                ? 'You should spend about 20 minutes on this task.'
                                                : 'You should spend about 40 minutes on this task.'}
                                        </p>
                                    </div>

                                    <div className="task-prompt">
                                        {currentTask?.passageText && (
                                            <div
                                                className="prompt-text"
                                                dangerouslySetInnerHTML={{ __html: sanitizeHtml(currentTask.passageText) }}
                                            />
                                        )}
                                    </div>

                                    {activeTask === 1 && currentTask?.displayContentUrl && (
                                        <div className="task-image">
                                            <img
                                                src={currentTask.displayContentUrl}
                                                alt="Task 1 Figure"
                                            />
                                        </div>
                                    )}
                                </div>
                            </Panel>

                            <PanelResizeHandle className="resize-handle">
                                <div className="resize-handle-icon-container">
                                    <span className="resize-handle-icon">↔</span>
                                </div>
                            </PanelResizeHandle>

                            {/* Right Panel - Writing Area (like questions-column) */}
                            <Panel defaultSize={50} minSize={30}>
                                <div className="questions-column writing-editor-panel">
                                    <div className="editor-header">
                                        <h3>Your Response</h3>
                                        <div className={`word-counter ${wordCount < minWords ? 'warning' : 'success'}`}>
                                            <span className="count">{wordCount}</span>
                                            <span className="separator">/</span>
                                            <span className="min">{minWords} words</span>
                                        </div>
                                    </div>

                                    <textarea
                                        className="writing-textarea"
                                        value={essays[activeTask]}
                                        onChange={(e) => handleEssayChange(activeTask, e.target.value)}
                                        placeholder={`Start writing your Task ${activeTask} response here...`}
                                        spellCheck="true"
                                    />
                                </div>
                            </Panel>
                        </PanelGroup>
                    </div>

                    {/* Footer with Task Navigation - using TestFooter component */}
                    <TestFooter
                        testData={testData}
                        currentPartIndex={activeTask - 1}
                        onPartSelect={(index) => setActiveTask(index + 1)}
                        mode="wordCount"
                        wordCounts={wordCounts}
                        partLabel="Task"
                    />
                </div>
            )}
        </>
    );
};

export default WritingTestPage;
