import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { sanitizeHtml } from '../utils/sanitize';
import { AnimatePresence, motion } from 'framer-motion';
import { testApi, testAttemptApi, writingApi } from '../api/backendApi';
import TestHeader from '../components/TestHeader';
import TestFooter from '../components/TestFooter';
import FullPageLoader from '../components/FullPageLoader';
import ResumeConfirmationModal from '../components/ResumeConfirmationModal';
import ExitTestModal from '../components/ExitTestModal';
import ConfirmationModal from '../components/ConfirmationModal';
import { PanelGroup, Panel, PanelResizeHandle } from 'react-resizable-panels';

// Reuse TestPage styles
import '../css/TestPage.css';
import '../css/TestHeader.css';
import '../css/TestFooter.css';
import '../css/WritingTestPage.css';

// Initial time: 60 minutes for writing test (combined Task 1 + Task 2)
const INITIAL_WRITING_TIME = 3600;

const WritingTestPage = () => {
    const { source, testNum } = useParams();
    const navigate = useNavigate();
    const location = useLocation();

    // Check if navigating from "Làm lại" button (forceNew flag)
    const forceNew = location.state?.forceNew || false;

    // --- Core State ---
    const [testStatus, setTestStatus] = useState('running');
    const [testData, setTestData] = useState([]);
    const [attempt, setAttempt] = useState(null);
    const [essays, setEssays] = useState({ 1: '', 2: '' });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    // --- Resume Modal State ---
    const [isResumeModalOpen, setIsResumeModalOpen] = useState(false);
    const [inProgressAttempt, setInProgressAttempt] = useState(null);
    const [isStartingNew, setIsStartingNew] = useState(false);

    // --- Exit Modal State ---
    const [isExitModalOpen, setIsExitModalOpen] = useState(false);
    const [isSavingProgress, setIsSavingProgress] = useState(false);

    // --- UI State ---
    const [activeTask, setActiveTask] = useState(1); // 1 or 2
    const [timeLeft, setTimeLeft] = useState(INITIAL_WRITING_TIME);

    const isSubmittingRef = useRef(false);
    const hasFetchedRef = useRef(false);

    // Ref to fix stale closure in timer
    const handleFinalSubmitRef = useRef(null);

    // --- Word Count ---
    const wordCount = useMemo(() => {
        const text = essays[activeTask] || '';
        if (!text.trim()) return 0;
        return text.trim().split(/\s+/).length;
    }, [essays, activeTask]);

    const minWords = activeTask === 1 ? 150 : 250;

    // Computed word counts for TestFooter
    const wordCounts = useMemo(() => ({
        1: {
            current: essays[1]?.trim().split(/\s+/).filter(Boolean).length || 0,
            min: 150
        },
        2: {
            current: essays[2]?.trim().split(/\s+/).filter(Boolean).length || 0,
            min: 250
        }
    }), [essays]);

    // Reset hasFetchedRef on unmount
    useEffect(() => {
        return () => {
            hasFetchedRef.current = false;
        };
    }, []);

    // --- Data Loading and Setup ---
    const setupTestState = useCallback(async (attemptData, fullTestData, abortSignal) => {
        if (abortSignal?.aborted) return;
        
        setAttempt(attemptData);
        setTestData(fullTestData);

        // Load essays if resuming an in-progress attempt
        if (attemptData.status === 'IN_PROGRESS' && attemptData.id) {
            try {
                const submissionsRes = await writingApi.getSubmissions(attemptData.id);
                if (abortSignal?.aborted) return;
                const loadedEssays = { 1: '', 2: '' };
                submissionsRes.data.forEach(sub => {
                    loadedEssays[sub.taskNumber] = sub.essayText || '';
                });
                setEssays(loadedEssays);
            } catch (error) {
                if (abortSignal?.aborted) return;
                console.error("Failed to load previous essays:", error);
            }
        } else {
            setEssays({ 1: '', 2: '' });
        }

        if (attemptData.timeLeft !== null && attemptData.timeLeft < INITIAL_WRITING_TIME) {
            setTimeLeft(attemptData.timeLeft);
        }

        setLoading(false);
        setIsResumeModalOpen(false);
    }, []);

    useEffect(() => {
        // Prevent duplicate calls in React StrictMode
        if (hasFetchedRef.current) return;
        hasFetchedRef.current = true;

        const abortController = new AbortController();

        const fetchAndStartTest = async () => {
            try {
                setLoading(true);
                // Pass forceNew to cancel old IN_PROGRESS attempts and start fresh
                const attemptRes = await testAttemptApi.startAttempt(source, testNum, 'writing', forceNew);
                
                if (abortController.signal.aborted) return;
                
                const attemptData = attemptRes.data;

                // If backend returned a COMPLETED attempt (forceNew was false), show choice modal
                if (attemptData.status === 'COMPLETED') {
                    setInProgressAttempt(attemptData);
                    setIsResumeModalOpen(true);
                    return;
                }

                // If forceNew is true, skip the resume modal since we're starting fresh
                if (!forceNew) {
                    // Only consider an attempt "dirty" if time has been consumed
                    // A fresh new attempt will have timeLeft === null or timeLeft === INITIAL_WRITING_TIME
                    const isDirty = attemptData.timeLeft !== null && attemptData.timeLeft < INITIAL_WRITING_TIME;

                    if (attemptData.status === 'IN_PROGRESS' && isDirty) {
                        // Check if there are any saved essays
                        try {
                            const submissionsRes = await writingApi.getSubmissions(attemptData.id);
                            if (abortController.signal.aborted) return;
                            const hasEssays = submissionsRes.data.some(sub => sub.essayText && sub.essayText.trim());
                            if (hasEssays || attemptData.timeLeft < INITIAL_WRITING_TIME) {
                                setInProgressAttempt(attemptData);
                                setIsResumeModalOpen(true);
                                return;
                            }
                        } catch (e) {
                            // Continue with normal flow if can't check submissions
                        }
                    }
                }

                if (abortController.signal.aborted) return;

                const fullTestData = await testApi.getFullTest(source, testNum, 'writing');
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
    }, [source, testNum, setupTestState, forceNew]);

    // --- Modal Handlers ---
    const handleResume = async () => {
        // If the attempt is COMPLETED, redirect to result page
        if (inProgressAttempt?.status === 'COMPLETED') {
            navigate(`/test/writing/review/${inProgressAttempt.id}`, { replace: true });
            return;
        }

        // Otherwise, resume IN_PROGRESS attempt
        try {
            setLoading(true);
            const fullTestData = await testApi.getFullTest(source, testNum, 'writing');
            await setupTestState(inProgressAttempt, fullTestData);
        } catch (err) {
            setError('Không thể tải dữ liệu bài làm trước đó.');
            setLoading(false);
        }
    };

    const handleStartNew = async () => {
        try {
            setIsStartingNew(true);

            // Use forceNew=true to cancel all IN_PROGRESS and create new attempt
            const newAttemptRes = await testAttemptApi.startAttempt(source, testNum, 'writing', true);
            const fullTestData = await testApi.getFullTest(source, testNum, 'writing');

            setEssays({ 1: '', 2: '' });
            setTimeLeft(INITIAL_WRITING_TIME);
            setActiveTask(1);

            await setupTestState(newAttemptRes.data, fullTestData);
        } catch (err) {
            setError('Không thể bắt đầu bài làm mới. Vui lòng thử lại.');
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

            // Submit essays for grading
            await writingApi.submitForGrading(attempt.id, essays);

            // Navigate to review page
            navigate(`/test/writing/review/${attempt.id}`);
        } catch (err) {
            console.error('Error submitting writing test:', err);
            setError('Không thể nộp bài. Vui lòng thử lại.');
        } finally {
            setIsSubmitting(false);
            isSubmittingRef.current = false;
        }
    }, [attempt, essays, navigate]);

    // Keep handleFinalSubmitRef updated
    useEffect(() => {
        handleFinalSubmitRef.current = handleFinalSubmit;
    }, [handleFinalSubmit]);

    // --- Timer ---
    useEffect(() => {
        if (testStatus !== 'running' || loading) return;

        const timer = setInterval(() => {
            setTimeLeft(prevTime => {
                if (prevTime <= 1) {
                    clearInterval(timer);
                    // Use ref to avoid stale closure
                    handleFinalSubmitRef.current?.();
                    return 0;
                }
                return prevTime - 1;
            });
        }, 1000);

        return () => clearInterval(timer);
    }, [testStatus, loading]); // Removed handleFinalSubmit from deps

    // --- Essay Change Handler ---
    const handleEssayChange = useCallback((taskNumber, text) => {
        setEssays(prev => ({ ...prev, [taskNumber]: text }));
    }, []);

    // --- Exit Handlers ---
    const handleExitRequest = useCallback(() => setIsExitModalOpen(true), []);

    const handleAbort = useCallback(async () => {
        if (!attempt) return;
        try {
            setIsSavingProgress(true);
            await testAttemptApi.cancelAttempt(attempt.id);
            setIsExitModalOpen(false);
            navigate('/dashboard', { state: { refreshData: true } });
        } catch (error) {
            console.error('Failed to cancel attempt:', error);
            if (error.response?.status === 404) {
                setIsExitModalOpen(false);
                navigate('/dashboard', { state: { refreshData: true } });
            } else {
                alert('Không thể huỷ lần làm bài. Vui lòng thử lại.');
            }
        } finally {
            setIsSavingProgress(false);
        }
    }, [attempt, navigate]);

    const handleSaveAndExit = useCallback(async () => {
        if (!attempt) return;

        try {
            setIsSavingProgress(true);

            // Save essays as drafts
            for (const [taskNum, essayText] of Object.entries(essays)) {
                if (essayText.trim()) {
                    await writingApi.saveDraft(attempt.id, parseInt(taskNum), essayText);
                }
            }

            // Save progress
            await testAttemptApi.saveProgress(attempt.id, {
                timeLeft,
                currentPart: activeTask,
                answers: {}
            });

            setIsExitModalOpen(false);
            navigate('/dashboard');
        } catch (error) {
            console.error('Failed to save progress:', error);
            alert('Không thể lưu tiến trình. Vui lòng thử lại.');
        } finally {
            setIsSavingProgress(false);
        }
    }, [attempt, essays, timeLeft, activeTask, navigate]);

    // --- Get Task Data ---
    const getTaskData = useCallback((taskNumber) => {
        if (!testData || testData.length === 0) return null;
        return testData.find(section => section.partNumber === taskNumber);
    }, [testData]);

    const currentTask = useMemo(() => getTaskData(activeTask), [getTaskData, activeTask]);

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
                onClose={() => setIsExitModalOpen(false)}
                onSaveAndExit={handleSaveAndExit}
                onAbort={handleAbort}
                isSaving={isSavingProgress}
            />

            <ConfirmationModal
                isOpen={isConfirmModalOpen}
                onClose={() => setIsConfirmModalOpen(false)}
                onConfirm={handleFinalSubmit}
                title="Xác nhận nộp bài"
                confirmText="Nộp bài"
            >
                <p>Bạn có chắc chắn muốn nộp bài Writing?</p>
                <div className="submit-summary">
                    <div className="submit-summary-item">
                        <span>Task 1:</span>
                        <span>{essays[1]?.trim().split(/\s+/).filter(Boolean).length || 0} từ</span>
                    </div>
                    <div className="submit-summary-item">
                        <span>Task 2:</span>
                        <span>{essays[2]?.trim().split(/\s+/).filter(Boolean).length || 0} từ</span>
                    </div>
                </div>
                <p className="submit-warning">
                    Sau khi nộp, bài làm sẽ được chấm điểm bởi AI. Bạn sẽ nhận kết quả trong vài phút.
                </p>
            </ConfirmationModal>

            {attempt && testData.length > 0 && (
                <div className="test-page-wrapper writing-test-wrapper">
                    <TestHeader
                        testName={`${source.toUpperCase()} Test ${testNum} - Writing`}
                        timeLeft={timeLeft}
                        onSubmit={() => setIsConfirmModalOpen(true)}
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
