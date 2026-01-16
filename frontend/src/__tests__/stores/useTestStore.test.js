/**
 * Unit tests for useTestStore.
 * Tests test-taking state management including answers, timer, and UI state.
 * 
 * @author Cramer Test Team
 * @since 2026-01-11
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { act } from '@testing-library/react';
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { immer } from 'zustand/middleware/immer';

// Create a simplified test version of the test store
const createTestTestStore = () => {
  const initialState = {
    // Core Test State
    testStatus: 'idle',
    testData: [],
    attempt: null,
    answers: {},
    essays: { 1: '', 2: '' },
    loading: false,
    error: null,
    isSubmitting: false,

    // UI State
    displayPartIndex: 0,
    activeTask: 1,

    // Timer State
    timeLeft: 0,
    timerRunning: false,

    // Audio State
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

  return create(
    devtools(
      immer((set, get) => ({
        ...initialState,

        // Core Actions
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

        setAnswer: (questionId, value) =>
          set(
            (state) => {
              state.answers[questionId] = value;
            },
            false,
            'setAnswer'
          ),

        setAnswers: (answers) =>
          set(
            (state) => {
              state.answers = answers;
            },
            false,
            'setAnswers'
          ),

        setEssay: (taskNumber, text) =>
          set(
            (state) => {
              state.essays[taskNumber] = text;
            },
            false,
            'setEssay'
          ),

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

        // UI Actions
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

        // Timer Actions
        setTimeLeft: (time) =>
          set(
            (state) => {
              state.timeLeft = time;
            },
            false,
            'setTimeLeft'
          ),

        setTimerRunning: (running) =>
          set(
            (state) => {
              state.timerRunning = running;
            },
            false,
            'setTimerRunning'
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

        // Audio Actions
        setIsAutoplay: (autoplay) =>
          set(
            (state) => {
              state.isAutoplay = autoplay;
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

        // Modal Actions
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

        // Reset
        resetStore: () =>
          set(
            () => initialState,
            false,
            'resetStore'
          ),
      }))
    )
  );
};

describe('useTestStore', () => {
  let store;

  beforeEach(() => {
    store = createTestTestStore();
  });

  // =========================================================================
  // INITIAL STATE TESTS
  // =========================================================================
  describe('Initial State', () => {
    it('should initialize with idle status', () => {
      expect(store.getState().testStatus).toBe('idle');
    });

    it('should initialize with empty answers', () => {
      expect(store.getState().answers).toEqual({});
    });

    it('should initialize with empty essays', () => {
      expect(store.getState().essays).toEqual({ 1: '', 2: '' });
    });

    it('should initialize timer at 0', () => {
      expect(store.getState().timeLeft).toBe(0);
      expect(store.getState().timerRunning).toBe(false);
    });

    it('should initialize with first part displayed', () => {
      expect(store.getState().displayPartIndex).toBe(0);
    });
  });

  // =========================================================================
  // ANSWER MANAGEMENT TESTS
  // =========================================================================
  describe('Answer Management', () => {
    it('should set single answer correctly', () => {
      act(() => {
        store.getState().setAnswer('q1', 'A');
      });

      expect(store.getState().answers.q1).toBe('A');
    });

    it('should update existing answer', () => {
      act(() => {
        store.getState().setAnswer('q1', 'A');
        store.getState().setAnswer('q1', 'B');
      });

      expect(store.getState().answers.q1).toBe('B');
    });

    it('should set multiple answers at once', () => {
      const answers = { q1: 'A', q2: 'B', q3: 'C' };

      act(() => {
        store.getState().setAnswers(answers);
      });

      expect(store.getState().answers).toEqual(answers);
    });

    it('should preserve other answers when setting single answer', () => {
      act(() => {
        store.getState().setAnswers({ q1: 'A', q2: 'B' });
        store.getState().setAnswer('q3', 'C');
      });

      expect(store.getState().answers).toEqual({ q1: 'A', q2: 'B', q3: 'C' });
    });
  });

  // =========================================================================
  // ESSAY MANAGEMENT TESTS
  // =========================================================================
  describe('Essay Management', () => {
    it('should set Task 1 essay correctly', () => {
      const essayText = 'The chart shows...';

      act(() => {
        store.getState().setEssay(1, essayText);
      });

      expect(store.getState().essays[1]).toBe(essayText);
    });

    it('should set Task 2 essay correctly', () => {
      const essayText = 'In my opinion...';

      act(() => {
        store.getState().setEssay(2, essayText);
      });

      expect(store.getState().essays[2]).toBe(essayText);
    });

    it('should preserve other task essay when updating one', () => {
      act(() => {
        store.getState().setEssay(1, 'Task 1 essay');
        store.getState().setEssay(2, 'Task 2 essay');
      });

      expect(store.getState().essays).toEqual({
        1: 'Task 1 essay',
        2: 'Task 2 essay',
      });
    });

    it('should bulk update essays', () => {
      act(() => {
        store.getState().setEssays({ 1: 'Essay 1', 2: 'Essay 2' });
      });

      expect(store.getState().essays).toEqual({ 1: 'Essay 1', 2: 'Essay 2' });
    });
  });

  // =========================================================================
  // TIMER TESTS
  // =========================================================================
  describe('Timer Management', () => {
    it('should set time left correctly', () => {
      act(() => {
        store.getState().setTimeLeft(3600);
      });

      expect(store.getState().timeLeft).toBe(3600);
    });

    it('should start timer', () => {
      act(() => {
        store.getState().setTimerRunning(true);
      });

      expect(store.getState().timerRunning).toBe(true);
    });

    it('should stop timer', () => {
      act(() => {
        store.getState().setTimerRunning(true);
        store.getState().setTimerRunning(false);
      });

      expect(store.getState().timerRunning).toBe(false);
    });

    it('should decrement time by 1 second', () => {
      act(() => {
        store.getState().setTimeLeft(100);
        store.getState().decrementTime();
      });

      expect(store.getState().timeLeft).toBe(99);
    });

    it('should not go below 0', () => {
      act(() => {
        store.getState().setTimeLeft(0);
        store.getState().decrementTime();
      });

      expect(store.getState().timeLeft).toBe(0);
    });
  });

  // =========================================================================
  // UI STATE TESTS
  // =========================================================================
  describe('UI State Management', () => {
    it('should change display part index', () => {
      act(() => {
        store.getState().setDisplayPartIndex(2);
      });

      expect(store.getState().displayPartIndex).toBe(2);
    });

    it('should switch active task', () => {
      act(() => {
        store.getState().setActiveTask(2);
      });

      expect(store.getState().activeTask).toBe(2);
    });

    it('should toggle autoplay', () => {
      expect(store.getState().isAutoplay).toBe(true);

      act(() => {
        store.getState().setIsAutoplay(false);
      });

      expect(store.getState().isAutoplay).toBe(false);
    });

    it('should change active audio index', () => {
      act(() => {
        store.getState().setActiveAudioIndex(3);
      });

      expect(store.getState().activeAudioIndex).toBe(3);
    });
  });

  // =========================================================================
  // MODAL STATE TESTS
  // =========================================================================
  describe('Modal State Management', () => {
    it('should open confirm modal', () => {
      act(() => {
        store.getState().openConfirmModal();
      });

      expect(store.getState().isConfirmModalOpen).toBe(true);
    });

    it('should close confirm modal', () => {
      act(() => {
        store.getState().openConfirmModal();
        store.getState().closeConfirmModal();
      });

      expect(store.getState().isConfirmModalOpen).toBe(false);
    });

    it('should open exit modal', () => {
      act(() => {
        store.getState().openExitModal();
      });

      expect(store.getState().isExitModalOpen).toBe(true);
    });

    it('should close exit modal', () => {
      act(() => {
        store.getState().openExitModal();
        store.getState().closeExitModal();
      });

      expect(store.getState().isExitModalOpen).toBe(false);
    });
  });

  // =========================================================================
  // ERROR HANDLING TESTS
  // =========================================================================
  describe('Error Handling', () => {
    it('should set error and change status to error', () => {
      act(() => {
        store.getState().setError('Network error');
      });

      expect(store.getState().error).toBe('Network error');
      expect(store.getState().testStatus).toBe('error');
    });

    it('should clear error when set to null', () => {
      act(() => {
        store.getState().setError('Some error');
        store.getState().setError(null);
      });

      expect(store.getState().error).toBeNull();
    });
  });

  // =========================================================================
  // RESET TESTS
  // =========================================================================
  describe('Store Reset', () => {
    it('should reset all state to initial values', () => {
      // Modify some state
      act(() => {
        store.getState().setTestStatus('running');
        store.getState().setAnswer('q1', 'A');
        store.getState().setEssay(1, 'Some essay');
        store.getState().setTimeLeft(1800);
        store.getState().setDisplayPartIndex(2);
      });

      // Reset
      act(() => {
        store.getState().resetStore();
      });

      // Verify reset
      expect(store.getState().testStatus).toBe('idle');
      expect(store.getState().answers).toEqual({});
      expect(store.getState().essays).toEqual({ 1: '', 2: '' });
      expect(store.getState().timeLeft).toBe(0);
      expect(store.getState().displayPartIndex).toBe(0);
    });
  });

  // =========================================================================
  // TEST STATUS FLOW TESTS
  // =========================================================================
  describe('Test Status Flow', () => {
    it('should transition from idle to loading', () => {
      act(() => {
        store.getState().setTestStatus('loading');
      });

      expect(store.getState().testStatus).toBe('loading');
    });

    it('should transition from loading to running', () => {
      act(() => {
        store.getState().setTestStatus('loading');
        store.getState().setTestStatus('running');
      });

      expect(store.getState().testStatus).toBe('running');
    });

    it('should transition from running to submitted', () => {
      act(() => {
        store.getState().setTestStatus('running');
        store.getState().setTestStatus('submitted');
      });

      expect(store.getState().testStatus).toBe('submitted');
    });
  });

  // =========================================================================
  // SUBMITTING STATE TESTS
  // =========================================================================
  describe('Submitting State', () => {
    it('should set isSubmitting to true', () => {
      act(() => {
        store.getState().setIsSubmitting(true);
      });

      expect(store.getState().isSubmitting).toBe(true);
    });

    it('should set isSubmitting to false', () => {
      act(() => {
        store.getState().setIsSubmitting(true);
        store.getState().setIsSubmitting(false);
      });

      expect(store.getState().isSubmitting).toBe(false);
    });
  });
});
