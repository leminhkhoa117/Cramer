import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { immer } from 'zustand/middleware/immer';

/**
 * Zustand store for ALL test-taking state management.
 * Single source of truth - eliminates 24+ prop drilling.
 */

const initialState = {
  // Core Test State
  testStatus: 'idle', // 'idle' | 'loading' | 'running' | 'submitted' | 'error'
  testData: [],
  attempt: null,
  answers: {}, // { [questionId]: answer } for Reading/Listening
  essays: { 1: '', 2: '' }, // For Writing Task 1 and Task 2
  loading: false,
  error: null,
  isSubmitting: false,

  // UI State
  displayPartIndex: 0,
  activeTask: 1, // 1 or 2 for writing

  // Timer State
  timeLeft: 0,
  timerRunning: false,

  // Audio State (Listening only)
  isAutoplay: true,
  activeAudioIndex: 0,

  // Modal State
  isConfirmModalOpen: false,
  isResumeModalOpen: false,
  isExitModalOpen: false,
  inProgressAttempt: null,
  isStartingNew: false,
  isSavingProgress: false,
};

const useTestStore = create(
  devtools(
    immer((set, get) => ({
      ...initialState,

      // ============ CORE STATE ACTIONS ============

      setTestStatus: (status) =>
        set(
          (state) => {
            state.testStatus = status;
          },
          false,
          'setTestStatus'
        ),

      setTestData: (data) =>
        set(
          (state) => {
            state.testData = data;
          },
          false,
          'setTestData'
        ),

      setAttempt: (attempt) =>
        set(
          (state) => {
            state.attempt = attempt;
          },
          false,
          'setAttempt'
        ),

      // Single answer update for Reading/Listening
      setAnswer: (questionId, value) =>
        set(
          (state) => {
            state.answers[questionId] = value;
          },
          false,
          'setAnswer'
        ),

      // Bulk set answers (e.g., when resuming)
      setAnswers: (answers) =>
        set(
          (state) => {
            state.answers = answers;
          },
          false,
          'setAnswers'
        ),

      // Single essay update for Writing
      setEssay: (taskNumber, text) =>
        set(
          (state) => {
            state.essays[taskNumber] = text;
          },
          false,
          'setEssay'
        ),

      // Bulk set essays (e.g., when resuming)
      setEssays: (essays) =>
        set(
          (state) => {
            state.essays = { ...state.essays, ...essays };
          },
          false,
          'setEssays'
        ),

      setLoading: (loading) =>
        set(
          (state) => {
            state.loading = loading;
          },
          false,
          'setLoading'
        ),

      setError: (error) =>
        set(
          (state) => {
            state.error = error;
            if (error) {
              state.testStatus = 'error';
            }
          },
          false,
          'setError'
        ),

      setIsSubmitting: (isSubmitting) =>
        set(
          (state) => {
            state.isSubmitting = isSubmitting;
          },
          false,
          'setIsSubmitting'
        ),

      // ============ UI STATE ACTIONS ============

      setDisplayPartIndex: (index) =>
        set(
          (state) => {
            state.displayPartIndex = index;
          },
          false,
          'setDisplayPartIndex'
        ),

      setActiveTask: (task) =>
        set(
          (state) => {
            state.activeTask = task;
          },
          false,
          'setActiveTask'
        ),

      // ============ TIMER ACTIONS ============

      setTimeLeft: (time) =>
        set(
          (state) => {
            state.timeLeft = time;
          },
          false,
          'setTimeLeft'
        ),

      decrementTime: () =>
        set(
          (state) => {
            if (state.timeLeft > 0) {
              state.timeLeft -= 1;
            }
          },
          false,
          'decrementTime'
        ),

      startTimer: () =>
        set(
          (state) => {
            state.timerRunning = true;
          },
          false,
          'startTimer'
        ),

      stopTimer: () =>
        set(
          (state) => {
            state.timerRunning = false;
          },
          false,
          'stopTimer'
        ),

      // ============ AUDIO STATE ACTIONS (Listening) ============

      setIsAutoplay: (isAutoplay) =>
        set(
          (state) => {
            state.isAutoplay = isAutoplay;
          },
          false,
          'setIsAutoplay'
        ),

      setActiveAudioIndex: (index) =>
        set(
          (state) => {
            state.activeAudioIndex = index;
          },
          false,
          'setActiveAudioIndex'
        ),

      // ============ MODAL ACTIONS ============

      openConfirmModal: () =>
        set(
          (state) => {
            state.isConfirmModalOpen = true;
          },
          false,
          'openConfirmModal'
        ),

      closeConfirmModal: () =>
        set(
          (state) => {
            state.isConfirmModalOpen = false;
          },
          false,
          'closeConfirmModal'
        ),

      openResumeModal: (attempt) =>
        set(
          (state) => {
            state.isResumeModalOpen = true;
            state.inProgressAttempt = attempt;
          },
          false,
          'openResumeModal'
        ),

      closeResumeModal: () =>
        set(
          (state) => {
            state.isResumeModalOpen = false;
            state.inProgressAttempt = null;
          },
          false,
          'closeResumeModal'
        ),

      openExitModal: () =>
        set(
          (state) => {
            state.isExitModalOpen = true;
          },
          false,
          'openExitModal'
        ),

      closeExitModal: () =>
        set(
          (state) => {
            state.isExitModalOpen = false;
          },
          false,
          'closeExitModal'
        ),

      setIsStartingNew: (isStartingNew) =>
        set(
          (state) => {
            state.isStartingNew = isStartingNew;
          },
          false,
          'setIsStartingNew'
        ),

      setIsSavingProgress: (isSavingProgress) =>
        set(
          (state) => {
            state.isSavingProgress = isSavingProgress;
          },
          false,
          'setIsSavingProgress'
        ),

      // ============ RESET ACTION ============

      resetTestState: () =>
        set(
          () => ({ ...initialState }),
          false,
          'resetTestState'
        ),

      // ============ COMPUTED / SELECTORS ============

      /**
       * Get word count for a specific writing task essay
       * @param {number} taskNumber - 1 or 2
       * @returns {number} Word count
       */
      getWordCount: (taskNumber) => {
        const essay = get().essays[taskNumber] || '';
        if (!essay.trim()) return 0;
        return essay.trim().split(/\s+/).filter(Boolean).length;
      },

      /**
       * Get total number of questions from testData
       * @returns {number} Total question count
       */
      getTotalQuestions: () => {
        const { testData } = get();
        if (!testData || !Array.isArray(testData)) return 0;

        return testData.reduce((total, part) => {
          if (part.questions && Array.isArray(part.questions)) {
            return total + part.questions.length;
          }
          // Handle nested question groups
          if (part.questionGroups && Array.isArray(part.questionGroups)) {
            return (
              total +
              part.questionGroups.reduce((groupTotal, group) => {
                return groupTotal + (group.questions?.length || 0);
              }, 0)
            );
          }
          return total;
        }, 0);
      },

      /**
       * Get count of answered questions (non-empty answers)
       * @returns {number} Answered question count
       */
      getAnsweredCount: () => {
        const { answers } = get();
        return Object.values(answers).filter(
          (answer) => answer !== null && answer !== undefined && answer !== ''
        ).length;
      },

      /**
       * Check if all questions are answered
       * @returns {boolean}
       */
      isAllAnswered: () => {
        const totalQuestions = get().getTotalQuestions();
        const answeredCount = get().getAnsweredCount();
        return totalQuestions > 0 && answeredCount >= totalQuestions;
      },

      /**
       * Get progress percentage
       * @returns {number} Percentage 0-100
       */
      getProgressPercentage: () => {
        const totalQuestions = get().getTotalQuestions();
        if (totalQuestions === 0) return 0;
        const answeredCount = get().getAnsweredCount();
        return Math.round((answeredCount / totalQuestions) * 100);
      },

      /**
       * Get current part/section data
       * @returns {object|null} Current part data
       */
      getCurrentPart: () => {
        const { testData, displayPartIndex } = get();
        if (!testData || !Array.isArray(testData)) return null;
        return testData[displayPartIndex] || null;
      },

      /**
       * Format time left as MM:SS string
       * @returns {string} Formatted time
       */
      getFormattedTime: () => {
        const { timeLeft } = get();
        const minutes = Math.floor(timeLeft / 60);
        const seconds = timeLeft % 60;
        return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
      },

      /**
       * Build auto-save payload for the current test session.
       * Mirrors the shape expected by useTestSessionStore.saveProgress().
       * @param {string} skill - 'reading', 'listening', or 'writing'
       * @returns {object} { answers, essays, timeLeft, currentPart }
       */
      getAutoSavePayload: (skill) => {
        const { answers, essays, timeLeft, displayPartIndex, activeTask } = get();

        if (skill === 'writing') {
          return {
            answers: {},
            essays: { ...essays },
            timeLeft,
            currentPart: activeTask,
          };
        }

        // Normalize array answers to string, matching submit normalization
        const normalized = {};
        if (answers) {
          Object.entries(answers).forEach(([qId, val]) => {
            normalized[qId] = Array.isArray(val) ? (val[0] || '') : val;
          });
        }

        return {
          answers: normalized,
          timeLeft: skill === 'reading' ? timeLeft : null,
          currentPart: displayPartIndex,
          essays: undefined,
        };
      },

      /**
       * Check if time is running low (under 5 minutes)
       * @returns {boolean}
       */
      isTimeLow: () => {
        return get().timeLeft < 300; // 5 minutes
      },

      /**
       * Check if time is critical (under 1 minute)
       * @returns {boolean}
       */
      isTimeCritical: () => {
        return get().timeLeft < 60; // 1 minute
      },
    })),
    {
      name: 'test-store',
      enabled: process.env.NODE_ENV === 'development',
    }
  )
);

// ============ STANDALONE SELECTORS FOR PERFORMANCE ============
// Use these for components that only need specific slices of state

export const selectTestStatus = (state) => state.testStatus;
export const selectTestData = (state) => state.testData;
export const selectAttempt = (state) => state.attempt;
export const selectAnswers = (state) => state.answers;
export const selectEssays = (state) => state.essays;
export const selectLoading = (state) => state.loading;
export const selectError = (state) => state.error;
export const selectIsSubmitting = (state) => state.isSubmitting;
export const selectDisplayPartIndex = (state) => state.displayPartIndex;
export const selectActiveTask = (state) => state.activeTask;
export const selectTimeLeft = (state) => state.timeLeft;
export const selectTimerRunning = (state) => state.timerRunning;
export const selectIsAutoplay = (state) => state.isAutoplay;
export const selectActiveAudioIndex = (state) => state.activeAudioIndex;
export const selectIsConfirmModalOpen = (state) => state.isConfirmModalOpen;
export const selectIsResumeModalOpen = (state) => state.isResumeModalOpen;
export const selectIsExitModalOpen = (state) => state.isExitModalOpen;
export const selectInProgressAttempt = (state) => state.inProgressAttempt;
export const selectIsStartingNew = (state) => state.isStartingNew;
export const selectIsSavingProgress = (state) => state.isSavingProgress;

// Compound selectors
export const selectModalState = (state) => ({
  isConfirmModalOpen: state.isConfirmModalOpen,
  isResumeModalOpen: state.isResumeModalOpen,
  isExitModalOpen: state.isExitModalOpen,
  inProgressAttempt: state.inProgressAttempt,
});

export const selectTimerState = (state) => ({
  timeLeft: state.timeLeft,
  timerRunning: state.timerRunning,
});

export const selectAudioState = (state) => ({
  isAutoplay: state.isAutoplay,
  activeAudioIndex: state.activeAudioIndex,
});

export default useTestStore;
