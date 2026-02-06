import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { useSpeakingStore } from '../../stores';
import useTimer from '../../hooks/useTimer';

// Import sub-components
import PreBriefScreen from '../../components/speaking/PreBriefScreen';
import GeminiLiveSessionLayout from '../../components/speaking/GeminiLiveSessionLayout';
import Part2PrepLayout from '../../components/speaking/Part2PrepLayout';
import FullPageLoader from '../../components/FullPageLoader';

// Import shared components
import TestHeader from '../../components/TestHeader';
import ConfirmationModal from '../../components/ConfirmationModal';

import '../../css/speaking/speaking-session.css';

/**
 * SpeakingSessionPage - Main session controller
 *
 * Manages the entire speaking test flow using a state machine:
 * IDLE → PRE_BRIEF → PART_1/2/3 → POST_PROCESSING → RESULTS
 *
 * Entry point: Via SpeakingPartModal on CourseDetailPage
 * Query params: ?source=cam13&test=1 (optional, for context display)
 */
export default function SpeakingSessionPage() {
  const { mode } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // Parse source context from query params
  const sourceContext = {
    courseName: searchParams.get('source'),
    testNumber: searchParams.get('test') ? parseInt(searchParams.get('test'), 10) : null,
  };

  // Local state for confirmation modals
  const [showSubmitConfirm, setShowSubmitConfirm] = useState(false);
  const [showExitConfirm, setShowExitConfirm] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showCleanupError, setShowCleanupError] = useState(false);

  // Zustand state
  const {
    status,
    sessionId,
    currentPart,
    sourceContext: storeSourceContext,
    globalTimer,
    error,
    isProcessing,
    cleanupError,
    startSession,
    resetSession,
    tickGlobalTimer,
    cleanupAudio,
    clearCleanupError,
    completeSessionOnBackend,
    abandonSessionOnBackend,
  } = useSpeakingStore();

  // Global timer (always running)
  const globalTimerControl = useTimer(0, false);

  /**
   * Cleanup audio resources when component unmounts
   * This ensures microphone is stopped even if user navigates away unexpectedly
   */
  useEffect(() => {
    return () => {
      cleanupAudio();
    };
  }, [cleanupAudio]);

  /**
   * Initialize session on mount
   * Note: Session may already be initialized by SpeakingPartModal
   * This handles direct URL access or page refresh
   */
  useEffect(() => {
    if (mode && status === 'IDLE') {
      // Parse context from query params for direct access
      const contextFromParams = sourceContext.courseName ? sourceContext : null;
      startSession(mode.toUpperCase(), contextFromParams);
    }
  }, [mode, status, sourceContext, startSession]);

  /**
   * Sync global timer with store
   */
  useEffect(() => {
    // Start timer when session is active (not in PRE_BRIEF or processing)
    const isActiveSession = ['PART_1', 'PART_2_PREP', 'PART_2_TALK', 'PART_3'].includes(status);
    if (isActiveSession && !globalTimerControl.isRunning) {
      globalTimerControl.start();
    }
  }, [status, globalTimerControl]);

  useEffect(() => {
    const interval = setInterval(() => {
      const isActiveSession = ['PART_1', 'PART_2_PREP', 'PART_2_TALK', 'PART_3'].includes(status);
      if (isActiveSession) {
        tickGlobalTimer();
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [status, tickGlobalTimer]);

  /**
   * Handler for errors (no longer needed for navigation)
   */
  useEffect(() => {
    if (error) {
      console.error('Speaking session error:', error);
    }
  }, [error]);

  // ============ HEADER HANDLERS ============

  /**
   * Handle submit button click - show confirmation modal
   */
  const handleSubmitClick = () => {
    setShowSubmitConfirm(true);
  };

  /**
   * Handle confirmed submit - cleanup mic FIRST, then end session
   * According to user requirement: Tắt mic TRƯỚC khi gọi API
   *
   * Note: Cleanup fail is BYPASSED to allow submission even if mic cleanup fails.
   * This ensures users can always submit their test even with browser microphone issues.
   *
   * Updated: Navigate directly to Results page after completion (no ProcessingScreen)
   * The Results page handles polling for grading status like Writing does.
   */
  const handleConfirmSubmit = async () => {
    setShowSubmitConfirm(false);
    setIsSubmitting(true);

    // 1. Attempt to cleanup audio (best effort - don't block on failure)
    try {
      const cleanupSuccess = await cleanupAudio();
      if (!cleanupSuccess) {
        console.warn('Mic cleanup failed, but proceeding with submission anyway');
        // Don't block submission - user can reload page later if needed
      }
    } catch (err) {
      console.warn('Mic cleanup error, proceeding anyway:', err);
    }

    // 2. Complete session on backend (triggers AI evaluation)
    try {
      await completeSessionOnBackend();

      // 3. Navigate directly to Results page (no fake ProcessingScreen)
      // Results page will poll for grading status like Writing does
      const resultSessionId = sessionId || Date.now();
      navigate(`/speaking/results/${resultSessionId}`);
    } catch (err) {
      console.error('Failed to complete session on backend:', err);
      // Still navigate to results - it will show error or mock data
      const resultSessionId = sessionId || Date.now();
      navigate(`/speaking/results/${resultSessionId}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  /**
   * Handle exit button click - show confirmation modal
   */
  const handleExitClick = () => {
    setShowExitConfirm(true);
  };

  /**
   * Handle confirmed exit - reset session and navigate back to courses
   */
  const handleConfirmExit = async () => {
    setShowExitConfirm(false);

    // Cleanup audio - stop microphone before navigating away
    await cleanupAudio();

    // Abandon session on backend (don't wait for response)
    abandonSessionOnBackend().catch(() => {});

    // Navigate back to the course page if we have context, otherwise to courses list
    const ctx = storeSourceContext || sourceContext;
    if (ctx?.courseName) {
      navigate(`/courses/${ctx.courseName}`);
    } else {
      navigate('/courses');
    }
    resetSession();
  };

  /**
   * Handle cleanup error - reload page to release microphone
   */
  const handleCleanupErrorReload = () => {
    setShowCleanupError(false);
    clearCleanupError();
    window.location.reload();
  };

  // ============ RENDER LOGIC ============

  /**
   * Determine if TestHeader should be shown
   * Show during active session parts only
   */
  const shouldShowHeader = ['PART_1', 'PART_2_PREP', 'PART_2_TALK', 'PART_3'].includes(status);

  /**
   * Get display name for current part
   */
  const getPartDisplayName = () => {
    switch (status) {
      case 'PART_1':
        return 'Part 1 - Introduction';
      case 'PART_2_PREP':
        return 'Part 2 - Preparation';
      case 'PART_2_TALK':
        return 'Part 2 - Speaking';
      case 'PART_3':
        return 'Part 3 - Discussion';
      default:
        return 'Speaking Test';
    }
  };

  /**
   * Render appropriate screen based on status
   */
  const renderScreen = () => {
    // Show loading overlay when submitting (like Writing does)
    if (isSubmitting) {
      return (
        <FullPageLoader
          message="Đang nộp bài và gửi chấm điểm..."
          subMessage="Vui lòng không đóng trang. AI đang phân tích bài nói của bạn."
        />
      );
    }

    switch (status) {
      case 'IDLE':
        return <div className="speaking-session__loading">Đang khởi tạo...</div>;

      case 'PRE_BRIEF':
        return <PreBriefScreen />;

      case 'PART_1':
      case 'PART_3':
        return <GeminiLiveSessionLayout part={currentPart} />;

      case 'PART_2_PREP':
        return <Part2PrepLayout />;

      case 'PART_2_TALK':
        return <GeminiLiveSessionLayout part={2} isTalking />;

      case 'ERROR':
        return (
          <div className="speaking-session__error">
            <h2>Đã xảy ra lỗi</h2>
            <p>{error || 'Vui lòng thử lại sau.'}</p>
            <button onClick={() => navigate('/courses')}>
              Quay về trang chủ
            </button>
          </div>
        );

      default:
        return <div className="speaking-session__loading">Đang tải...</div>;
    }
  };

  return (
    <div className="speaking-session">
      {/* TestHeader - shown during active session parts */}
      {shouldShowHeader && (
        <TestHeader
          testName={`IELTS Speaking - ${getPartDisplayName()}`}
          timeLeft={globalTimer}
          onSubmit={handleSubmitClick}
          onExit={handleExitClick}
          isSubmitting={isProcessing || isSubmitting}
        />
      )}

      {/* Main content */}
      {renderScreen()}

      {/* Submit Confirmation Modal */}
      <ConfirmationModal
        isOpen={showSubmitConfirm}
        onClose={() => setShowSubmitConfirm(false)}
        onConfirm={handleConfirmSubmit}
        title="Xác nhận nộp bài"
        confirmText="Nộp bài"
      >
        <p>Bạn có chắc muốn nộp bài Speaking?</p>
        <p style={{ fontSize: '0.875rem', color: '#6b7280', marginTop: '0.5rem' }}>
          Audio của bạn sẽ được gửi đi để AI đánh giá.
        </p>
      </ConfirmationModal>

      {/* Exit Confirmation Modal */}
      <ConfirmationModal
        isOpen={showExitConfirm}
        onClose={() => setShowExitConfirm(false)}
        onConfirm={handleConfirmExit}
        title="Thoát bài thi"
        confirmText="Thoát"
      >
        <p>Bạn có chắc muốn thoát?</p>
        <p style={{ fontSize: '0.875rem', color: '#6b7280', marginTop: '0.5rem' }}>
          Tiến trình hiện tại sẽ không được lưu.
        </p>
      </ConfirmationModal>

      {/* Cleanup Error Modal - shown when microphone cannot be stopped */}
      <ConfirmationModal
        isOpen={showCleanupError}
        onClose={() => setShowCleanupError(false)}
        onConfirm={handleCleanupErrorReload}
        title="Không thể tắt Microphone"
        confirmText="Tải lại trang"
      >
        <p>{cleanupError || 'Không thể tắt microphone. Vui lòng tải lại trang để đảm bảo quyền riêng tư.'}</p>
        <p style={{ fontSize: '0.875rem', color: '#dc2626', marginTop: '0.5rem' }}>
          ⚠️ Microphone có thể vẫn đang hoạt động.
        </p>
      </ConfirmationModal>
    </div>
  );
}
