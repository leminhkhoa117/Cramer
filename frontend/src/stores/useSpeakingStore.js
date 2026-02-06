import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { immer } from 'zustand/middleware/immer';
import { speakingApi } from '../api/speakingApi';

/**
 * Zustand store for AI Speaking Session state management
 *
 * Manages the entire speaking test flow including:
 * - Session lifecycle and state machine
 * - Audio recording and playback
 * - Question flow and timing
 * - Transcripts and evaluation results
 * - Backend API integration
 */

// ============================================================
// IELTS SPEAKING TIME LIMITS (in seconds)
// ============================================================
export const PART_TIME_LIMITS = {
  PART_1: 300,      // 5 minutes
  PART_2_PREP: 60,  // 1 minute preparation
  PART_2_TALK: 120, // 2 minutes speaking
  PART_3: 300,      // 5 minutes
};

// Warning thresholds (seconds remaining)
export const TIME_WARNING_THRESHOLDS = {
  SOFT_WARNING: 15, // Yellow indicator
  HARD_WARNING: 5,  // Red urgent warning
};

const initialState = {
  // ============ SESSION METADATA ============
  sessionId: null,
  mode: null, // 'FULL' | 'PART_1' | 'PART_2' | 'PART_3' | 'PART_2_3'
  status: 'IDLE', // State machine status
  sourceContext: null, // { courseName, testNumber } - track origin from course page
  topicId: null, // Selected topic ID
  luaCost: null, // Credit cost for this session

  // ============ CURRENT PART TRACKING ============
  currentPart: null, // 1, 2, or 3
  currentQuestionIndex: 0,
  currentQuestion: null,
  questionsBlueprint: [], // Pre-planned questions for the session

  // ============ TIMERS (seconds) ============
  globalTimer: 0,
  partTimer: 0,
  prepTimer: 60, // Part 2 preparation time
  silenceTimer: 0, // Track user silence (5s = auto end)

  // ============ TIME GUARD STATE ============
  partTimeLimit: null, // Current part's time limit
  timeWarningLevel: null, // null | 'SOFT' | 'HARD'
  timeRemaining: null, // Seconds remaining in current part
  isTimeUp: false, // True when part time has expired

  // ============ AUDIO STATE ============
  isRecording: false,
  isExaminerSpeaking: false,
  audioLevel: 0,
  hasAudioPermission: null,
  audioError: null,
  audioCleanupFn: null, // Function to cleanup audio resources
  cleanupError: null, // Error message if cleanup fails

  // ============ TTS CONVERSATION STATE ============
  conversationState: 'IDLE', // IDLE | EXAMINER_LOADING | EXAMINER_SPEAKING | USER_READY | USER_RECORDING | SUBMITTING
  examinerAudioUrl: null,
  examinerAudioDurationMs: null,
  isExaminerAudioLoading: false,

  // ============ TRANSCRIPT DATA ============
  transcripts: [], // Array of { questionId, text, audioBlob, timestamp, part }
  currentTranscript: '', // Current user answer being transcribed

  // ============ PART 2 NOTES ============
  userNotes: '',

  // ============ RESULTS ============
  evaluation: null, // Full evaluation object after session
  overallBand: null,
  criteriaScores: null,

  // ============ UI STATE ============
  showConsentModal: false,
  isProcessing: false,
  error: null,

  // ============ LOADING STATES ============
  isLoadingQuestions: false,
  isSubmitting: false,
};

const useSpeakingStore = create(
  devtools(
    immer((set, get) => ({
      ...initialState,

      // ============================================================
      // SESSION LIFECYCLE ACTIONS
      // ============================================================

      /**
       * Start a new speaking session
       * @param {string} mode - Session mode (FULL, PART_1, etc.)
       * @param {object} sourceContext - Optional { courseName, testNumber } from course page
       */
      startSession: (mode, sourceContext = null) => set((state) => {
        state.mode = mode;
        state.sourceContext = sourceContext;
        state.status = 'PRE_BRIEF';
        state.showConsentModal = true;
      }, false, 'startSession'),

      /**
       * User accepts recording consent - directly starts the session
       */
      acceptConsent: () => set((state) => {
        state.showConsentModal = false;

        // Directly start the session based on mode
        const firstPart = getFirstPart(state.mode);
        state.currentPart = firstPart;
        state.currentQuestionIndex = 0;

        // Set initial status
        state.status = firstPart === 2 ? 'PART_2_PREP' : `PART_${firstPart}`;
      }, false, 'acceptConsent'),

      /**
       * Initialize session with questions blueprint (called after API fetch)
       * @param {array} questions - Questions array for the session
       */
      initializeQuestions: (questions) => set((state) => {
        state.questionsBlueprint = questions;

        // Set first question based on current part
        const partQuestions = questions.filter(q => q.part === state.currentPart);
        state.currentQuestion = partQuestions[0] || null;
      }, false, 'initializeQuestions'),

      /**
       * End the entire session
       */
      endSession: () => set((state) => {
        state.status = 'POST_PROCESSING';
      }, false, 'endSession'),

      /**
       * Complete session and move to results
       */
      completeSession: () => set((state) => {
        state.status = 'RESULTS';
      }, false, 'completeSession'),

      /**
       * Reset entire session to initial state
       */
      resetSession: () => set(initialState, false, 'resetSession'),

      // ============================================================
      // PART MANAGEMENT ACTIONS
      // ============================================================

      /**
       * Start a specific part
       * @param {number} partNumber - Part number (1, 2, or 3)
       */
      startPart: (partNumber) => set((state) => {
        state.currentPart = partNumber;
        state.partTimer = 0;
        state.currentQuestionIndex = 0;
        state.status = partNumber === 2 ? 'PART_2_PREP' : `PART_${partNumber}`;

        // Set time limit based on part
        if (partNumber === 2) {
          // Start with prep time limit
          state.partTimeLimit = PART_TIME_LIMITS.PART_2_PREP;
          state.timeRemaining = PART_TIME_LIMITS.PART_2_PREP;
        } else {
          state.partTimeLimit = partNumber === 1 ? PART_TIME_LIMITS.PART_1 : PART_TIME_LIMITS.PART_3;
          state.timeRemaining = state.partTimeLimit;
        }

        // Reset time warnings
        state.timeWarningLevel = null;
        state.isTimeUp = false;

        // Set first question of this part
        const partQuestions = state.questionsBlueprint.filter(q => q.part === partNumber);
        state.currentQuestion = partQuestions[0] || null;
      }, false, 'startPart'),

      /**
       * Finish Part 2 preparation and start speaking
       */
      finishPrep: () => set((state) => {
        state.status = 'PART_2_TALK';
        state.prepTimer = 0;
        state.partTimer = 0;

        // Set Part 2 talk time limit
        state.partTimeLimit = PART_TIME_LIMITS.PART_2_TALK;
        state.timeRemaining = PART_TIME_LIMITS.PART_2_TALK;
        state.timeWarningLevel = null;
        state.isTimeUp = false;
      }, false, 'finishPrep'),

      /**
       * Move to next question in current part
       */
      nextQuestion: () => set((state) => {
        const partQuestions = state.questionsBlueprint.filter(
          q => q.part === state.currentPart
        );

        if (state.currentQuestionIndex < partQuestions.length - 1) {
          state.currentQuestionIndex += 1;
          state.currentQuestion = partQuestions[state.currentQuestionIndex];
        } else {
          // No more questions, finish this part
          finishPartInternal(state);
        }
      }, false, 'nextQuestion'),

      /**
       * Finish current part and move to next or end session
       */
      finishPart: () => set((state) => {
        finishPartInternal(state);
      }, false, 'finishPart'),

      // ============================================================
      // AUDIO ACTIONS
      // ============================================================

      setRecording: (isRecording) => set((state) => {
        state.isRecording = isRecording;
        if (isRecording) {
          state.conversationState = 'USER_RECORDING';
        }
        if (!isRecording) {
          state.silenceTimer = 0;
        }
      }, false, 'setRecording'),

      setExaminerSpeaking: (isSpeaking) => set((state) => {
        state.isExaminerSpeaking = isSpeaking;
        if (isSpeaking && !state.isExaminerAudioLoading) {
          state.conversationState = 'EXAMINER_SPEAKING';
        } else if (!isSpeaking && !state.isRecording) {
          state.conversationState = 'USER_READY';
        }
      }, false, 'setExaminerSpeaking'),

      setAudioLevel: (level) => set((state) => {
        state.audioLevel = level;
      }, false, 'setAudioLevel'),

      setAudioPermission: (hasPermission) => set((state) => {
        state.hasAudioPermission = hasPermission;
      }, false, 'setAudioPermission'),

      setAudioError: (error) => set((state) => {
        state.audioError = error;
      }, false, 'setAudioError'),

      // ============================================================
      // TTS CONVERSATION STATE ACTIONS
      // ============================================================

      /**
       * Set conversation state for TTS flow
       */
      setConversationState: (newState) => set((state) => {
        state.conversationState = newState;
      }, false, 'setConversationState'),

      /**
       * Set examiner audio info from question data
       */
      setExaminerAudio: (url, durationMs) => set((state) => {
        state.examinerAudioUrl = url;
        state.examinerAudioDurationMs = durationMs;
      }, false, 'setExaminerAudio'),

      /**
       * Set examiner audio loading state
       */
      setExaminerAudioLoading: (isLoading) => set((state) => {
        state.isExaminerAudioLoading = isLoading;
        if (isLoading) {
          state.conversationState = 'EXAMINER_LOADING';
        }
      }, false, 'setExaminerAudioLoading'),

      // ============================================================
      // TIMER ACTIONS
      // ============================================================

      tickGlobalTimer: () => set((state) => {
        state.globalTimer += 1;
      }, false, 'tickGlobalTimer'),

      /**
       * Tick part timer and check for time warnings/expiry
       * Returns true if part should auto-finish
       */
      tickPartTimer: () => {
        const state = get();
        const { partTimer, partTimeLimit, status } = state;

        // Skip if no time limit or in prep mode (prep has its own timer)
        if (!partTimeLimit || status === 'PART_2_PREP') {
          set((s) => {
            s.partTimer += 1;
          }, false, 'tickPartTimer/noLimit');
          return false;
        }

        const newTimer = partTimer + 1;
        const remaining = Math.max(0, partTimeLimit - newTimer);

        set((s) => {
          s.partTimer = newTimer;
          s.timeRemaining = remaining;

          // Check for warning levels
          if (remaining <= TIME_WARNING_THRESHOLDS.HARD_WARNING && remaining > 0) {
            s.timeWarningLevel = 'HARD';
          } else if (remaining <= TIME_WARNING_THRESHOLDS.SOFT_WARNING && remaining > TIME_WARNING_THRESHOLDS.HARD_WARNING) {
            s.timeWarningLevel = 'SOFT';
          } else if (remaining <= 0) {
            s.isTimeUp = true;
            s.timeWarningLevel = null;
          }
        }, false, 'tickPartTimer');

        // Return true if time is up (caller should handle auto-finish)
        return remaining <= 0;
      },

      /**
       * Reset time warning state (e.g., when moving to next question)
       */
      resetTimeWarning: () => set((state) => {
        state.timeWarningLevel = null;
      }, false, 'resetTimeWarning'),

      tickPrepTimer: () => set((state) => {
        if (state.prepTimer > 0) {
          state.prepTimer -= 1;
        } else if (state.prepTimer === 0 && state.status === 'PART_2_PREP') {
          // Auto-transition when prep time ends
          state.status = 'PART_2_TALK';
        }
      }, false, 'tickPrepTimer'),

      tickSilenceTimer: () => set((state) => {
        state.silenceTimer += 1;
      }, false, 'tickSilenceTimer'),

      resetSilenceTimer: () => set((state) => {
        state.silenceTimer = 0;
      }, false, 'resetSilenceTimer'),

      // ============================================================
      // TRANSCRIPT ACTIONS
      // ============================================================

      /**
       * Add a completed transcript
       * @param {object} transcript - { questionId, text, audioBlob, timestamp, part }
       */
      addTranscript: (transcript) => set((state) => {
        state.transcripts.push({
          ...transcript,
          timestamp: new Date().toISOString(),
        });
      }, false, 'addTranscript'),

      /**
       * Update current transcript (real-time)
       * @param {string} text - Current transcript text
       */
      updateCurrentTranscript: (text) => set((state) => {
        state.currentTranscript = text;
      }, false, 'updateCurrentTranscript'),

      clearCurrentTranscript: () => set((state) => {
        state.currentTranscript = '';
      }, false, 'clearCurrentTranscript'),

      // ============================================================
      // NOTES ACTIONS (Part 2)
      // ============================================================

      setUserNotes: (notes) => set((state) => {
        state.userNotes = notes;
      }, false, 'setUserNotes'),

      // ============================================================
      // RESULTS ACTIONS
      // ============================================================

      /**
       * Set evaluation results
       * @param {object} evaluation - Full evaluation object
       */
      setEvaluation: (evaluation) => set((state) => {
        state.evaluation = evaluation;
        state.overallBand = evaluation.overallBand;
        state.criteriaScores = evaluation.criteria;
      }, false, 'setEvaluation'),

      // ============================================================
      // ERROR & LOADING ACTIONS
      // ============================================================

      setError: (error) => set((state) => {
        state.error = error;
        if (error) {
          state.status = 'ERROR';
        }
      }, false, 'setError'),

      clearError: () => set((state) => {
        state.error = null;
      }, false, 'clearError'),

      setProcessing: (isProcessing) => set((state) => {
        state.isProcessing = isProcessing;
      }, false, 'setProcessing'),

      setLoadingQuestions: (loading) => set((state) => {
        state.isLoadingQuestions = loading;
      }, false, 'setLoadingQuestions'),

      setSubmitting: (isSubmitting) => set((state) => {
        state.isSubmitting = isSubmitting;
      }, false, 'setSubmitting'),

      /**
       * Register audio cleanup function
       * Called by LiveSessionLayout when audio recorder is initialized
       */
      setAudioCleanup: (cleanupFn) => set((state) => {
        state.audioCleanupFn = cleanupFn;
      }, false, 'setAudioCleanup'),

      /**
       * Cleanup all audio resources
       * Call this before navigating away or ending session
       * @returns {Promise<boolean>} true if cleanup succeeded, false if failed
       */
      cleanupAudio: async () => {
        const { audioCleanupFn } = get();

        if (audioCleanupFn) {
          try {
            const result = await audioCleanupFn();

            if (!result?.success) {
              // Cleanup failed after retries - set error state for UI to display
              set((state) => {
                state.cleanupError = result?.error || 'Không thể tắt microphone. Vui lòng tải lại trang.';
              }, false, 'cleanupAudio/error');
              return false;
            }
          } catch (error) {
            console.error('Cleanup audio error:', error);
            set((state) => {
              state.cleanupError = 'Không thể tắt microphone. Vui lòng tải lại trang.';
            }, false, 'cleanupAudio/error');
            return false;
          }
        }

        set((state) => {
          state.audioCleanupFn = null;
          state.isRecording = false;
          state.audioLevel = 0;
          state.cleanupError = null;
        }, false, 'cleanupAudio');

        return true;
      },

      /**
       * Clear cleanup error state
       */
      clearCleanupError: () => set((state) => {
        state.cleanupError = null;
      }, false, 'clearCleanupError'),

      // ============================================================
      // API INTEGRATION ACTIONS
      // ============================================================

      /**
       * Create a new session on the backend
       * @param {string} mode - Session mode
       * @param {number} topicId - Topic ID
       * @returns {Promise<object>} Created session data
       */
      createSessionOnBackend: async (mode, topicId) => {
        set((state) => {
          state.isLoadingQuestions = true;
          state.error = null;
        }, false, 'createSessionOnBackend/start');

        try {
          const response = await speakingApi.createSession(mode, topicId);
          const sessionData = response.data.data;

          set((state) => {
            state.sessionId = sessionData.sessionId;
            state.topicId = topicId;
            state.luaCost = sessionData.luaCost;
            state.questionsBlueprint = sessionData.questions || [];
            state.isLoadingQuestions = false;

            // Set first question
            const firstPart = getFirstPart(mode);
            const partQuestions = state.questionsBlueprint.filter(q => q.part === firstPart);
            state.currentQuestion = partQuestions[0] || null;
          }, false, 'createSessionOnBackend/success');

          return sessionData;
        } catch (error) {
          console.error('Failed to create session:', error);
          set((state) => {
            state.error = error.response?.data?.message || 'Failed to create session';
            state.isLoadingQuestions = false;
            state.status = 'ERROR';
          }, false, 'createSessionOnBackend/error');
          throw error;
        }
      },

      /**
       * Fetch questions from backend
       * @param {number} topicId - Topic ID
       * @param {string} mode - Session mode
       */
      fetchQuestions: async (topicId, mode) => {
        set((state) => {
          state.isLoadingQuestions = true;
        }, false, 'fetchQuestions/start');

        try {
          const response = await speakingApi.getQuestions(topicId, mode);
          const questions = response.data.data || [];

          set((state) => {
            state.questionsBlueprint = questions;
            state.isLoadingQuestions = false;

            // Set first question based on current part
            if (state.currentPart) {
              const partQuestions = questions.filter(q => q.part === state.currentPart);
              state.currentQuestion = partQuestions[0] || null;
            }
          }, false, 'fetchQuestions/success');

          return questions;
        } catch (error) {
          console.error('Failed to fetch questions:', error);
          set((state) => {
            state.error = error.response?.data?.message || 'Failed to fetch questions';
            state.isLoadingQuestions = false;
          }, false, 'fetchQuestions/error');
          throw error;
        }
      },

      /**
       * Save transcript to backend
       * @param {object} transcriptData - Transcript data
       */
      saveTranscriptToBackend: async (transcriptData) => {
        const { sessionId } = get();
        if (!sessionId) {
          console.warn('No session ID, skipping transcript save');
          return null;
        }

        try {
          const response = await speakingApi.saveTranscript(sessionId, transcriptData);
          return response.data.data;
        } catch (error) {
          console.error('Failed to save transcript:', error);
          // Don't throw - allow session to continue even if save fails
          return null;
        }
      },

      /**
       * Complete session and trigger AI evaluation
       */
      completeSessionOnBackend: async () => {
        const { sessionId } = get();
        if (!sessionId) {
          console.warn('No session ID, using local completion');
          set((state) => {
            state.status = 'RESULTS';
          }, false, 'completeSession/noBackend');
          return;
        }

        set((state) => {
          state.isProcessing = true;
          state.status = 'POST_PROCESSING';
        }, false, 'completeSessionOnBackend/start');

        try {
          await speakingApi.completeSession(sessionId);
          // Session completed, evaluation is processing async on backend
          // Navigate to results page will fetch the results
          set((state) => {
            state.isProcessing = false;
            state.status = 'RESULTS';
          }, false, 'completeSessionOnBackend/success');
        } catch (error) {
          console.error('Failed to complete session:', error);
          set((state) => {
            state.error = error.response?.data?.message || 'Failed to complete session';
            state.isProcessing = false;
          }, false, 'completeSessionOnBackend/error');
          throw error;
        }
      },

      /**
       * Abandon session on backend
       */
      abandonSessionOnBackend: async () => {
        const { sessionId } = get();
        if (!sessionId) return;

        try {
          await speakingApi.abandonSession(sessionId);
        } catch (error) {
          console.error('Failed to abandon session:', error);
          // Don't throw - allow navigation even if abandon fails
        }
      },

      /**
       * Fetch evaluation results from backend
       * @param {number} [overrideSessionId] - Optional session ID (used by results page)
       */
      fetchResults: async (overrideSessionId = null) => {
        const sessionId = overrideSessionId || get().sessionId;
        if (!sessionId) {
          console.warn('No session ID for fetching results');
          return null;
        }

        set((state) => {
          state.isProcessing = true;
        }, false, 'fetchResults/start');

        try {
          const response = await speakingApi.getResults(sessionId);
          const results = response.data.data;

          set((state) => {
            state.evaluation = results;
            state.overallBand = results.overallBand;
            state.criteriaScores = {
              fluency: results.fluency,
              lexical: results.lexical,
              grammar: results.grammar,
              pronunciation: results.pronunciation,
            };
            state.transcripts = results.transcripts || state.transcripts;
            state.isProcessing = false;
          }, false, 'fetchResults/success');

          return results;
        } catch (error) {
          console.error('Failed to fetch results:', error);
          set((state) => {
            state.error = error.response?.data?.message || 'Failed to fetch results';
            state.isProcessing = false;
          }, false, 'fetchResults/error');
          throw error;
        }
      },

      /**
       * Set the backend session ID (used when session is created externally)
       */
      setSessionId: (sessionId) => set((state) => {
        state.sessionId = sessionId;
      }, false, 'setSessionId'),

      /**
       * Set topic ID
       */
      setTopicId: (topicId) => set((state) => {
        state.topicId = topicId;
      }, false, 'setTopicId'),

      // ============================================================
      // DYNAMIC FOLLOW-UP ACTIONS (Phase 2 - speaking_session_foundations)
      // ============================================================

      /**
       * Request a follow-up question with TTS from backend.
       * If enabled, uses AI to select contextual follow-up question.
       * Falls back to blueprint if API fails or disabled.
       * 
       * @param {object} context - Context for follow-up selection
       * @param {string} context.previousQuestion - Previous question text
       * @param {string} context.candidateAnswer - User's transcript
       * @returns {Promise<boolean>} True if follow-up was found, false otherwise
       */
      requestFollowUp: async (context = {}) => {
        const state = get();
        const { topicId, currentPart, questionsBlueprint, transcripts, currentQuestionIndex } = state;

        // Collect asked question IDs
        const askedQuestionIds = transcripts
          .filter(t => t.part === currentPart)
          .map(t => t.questionId)
          .filter(Boolean);

        const previousQuestion = context.previousQuestion || state.currentQuestion?.text || '';
        const candidateAnswer = context.candidateAnswer || '';

        try {
          // Call follow-up API with TTS
          const response = await speakingApi.selectFollowUpWithTTS({
            topicId,
            part: currentPart,
            previousQuestion,
            candidateAnswer,
            askedQuestionIds,
          });

          if (response.data.success && response.data.data?.question) {
            const { question, audioUrl, durationMs, hasAudio } = response.data.data;
            
            // Set the follow-up as current question
            set((state) => {
              state.currentQuestion = {
                ...question,
                examinerAudioUrl: audioUrl,
                examinerAudioDurationMs: durationMs,
              };
              state.currentQuestionIndex += 1;
            }, false, 'requestFollowUp/success');

            console.log('Follow-up selected:', question.id, 'hasAudio:', hasAudio);
            return true;
          }

          // No follow-up available - fall back to blueprint
          console.log('No follow-up available, using blueprint');
          return get().nextQuestionFromBlueprint();

        } catch (error) {
          console.error('Follow-up request failed:', error);
          // Fall back to blueprint
          return get().nextQuestionFromBlueprint();
        }
      },

      /**
       * Move to next question from static blueprint
       * (Fallback when dynamic follow-up is disabled or fails)
       */
      nextQuestionFromBlueprint: () => {
        const state = get();
        const partQuestions = state.questionsBlueprint.filter(
          q => q.part === state.currentPart
        );

        if (state.currentQuestionIndex < partQuestions.length - 1) {
          set((s) => {
            s.currentQuestionIndex += 1;
            s.currentQuestion = partQuestions[s.currentQuestionIndex];
          }, false, 'nextQuestionFromBlueprint');
          return true;
        } else {
          // No more questions in blueprint, finish part
          set((s) => {
            finishPartInternal(s);
          }, false, 'nextQuestionFromBlueprint/finishPart');
          return false;
        }
      },
    })),
    { name: 'speaking-store' }
  )
);

// ============================================================
// HELPER FUNCTIONS
// ============================================================

/**
 * Get the first part number based on session mode
 */
function getFirstPart(mode) {
  switch (mode) {
    case 'FULL': return 1;
    case 'PART_1': return 1;
    case 'PART_2': return 2;
    case 'PART_3': return 3;
    case 'PART_2_3': return 2;
    default: return 1;
  }
}

/**
 * Internal function to handle part completion
 * Mutates state directly (used inside immer)
 */
function finishPartInternal(state) {
  const transitions = {
    'FULL': { 1: 2, 2: 3, 3: null },
    'PART_1': { 1: null },
    'PART_2': { 2: null },
    'PART_3': { 3: null },
    'PART_2_3': { 2: 3, 3: null },
  };

  const nextPart = transitions[state.mode]?.[state.currentPart] || null;

  if (nextPart) {
    // Move to next part
    state.currentPart = nextPart;
    state.status = nextPart === 2 ? 'PART_2_PREP' : `PART_${nextPart}`;
    state.currentQuestionIndex = 0;
    state.partTimer = 0;
    state.prepTimer = nextPart === 2 ? 60 : 0;

    // Reset and set new time limit
    state.timeWarningLevel = null;
    state.isTimeUp = false;
    if (nextPart === 2) {
      // Start with prep time limit
      state.partTimeLimit = PART_TIME_LIMITS.PART_2_PREP;
      state.timeRemaining = PART_TIME_LIMITS.PART_2_PREP;
    } else {
      state.partTimeLimit = nextPart === 1 ? PART_TIME_LIMITS.PART_1 : PART_TIME_LIMITS.PART_3;
      state.timeRemaining = state.partTimeLimit;
    }

    // Set first question of next part
    const partQuestions = state.questionsBlueprint.filter(q => q.part === nextPart);
    state.currentQuestion = partQuestions[0] || null;
  } else {
    // Session complete
    state.status = 'POST_PROCESSING';
  }
}

export default useSpeakingStore;
