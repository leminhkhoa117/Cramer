import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { testApi, testAttemptApi, writingApi } from '../api/backendApi';

// Cache TTL: 5 minutes in milliseconds
const CACHE_TTL = 5 * 60 * 1000;

/**
 * Test Session Store
 * 
 * This store handles API operations for test attempts, separate from UI state.
 * It manages:
 * - Test attempt lifecycle (start, submit, cancel)
 * - Test data caching
 * - Progress saving
 * - Writing submissions
 */
const useTestSessionStore = create(
  devtools(
    (set, get) => ({
      // ============================================
      // STATE
      // ============================================
      currentAttemptId: null,
      attemptStatus: 'idle', // 'idle' | 'loading' | 'ready' | 'error'
      lastSavedAt: null,
      autoSaveEnabled: true,

      // ============================================
      // CACHED DATA
      // ============================================
      // Structure: { [key: source-testNum-skill]: { data, fetchedAt } }
      testDataCache: {},

      // ============================================
      // ACTIONS
      // ============================================

      /**
       * Start or resume a test attempt
       * @param {string} source - Exam source (e.g., "cam17")
       * @param {number} testNum - Test number
       * @param {string} skill - Skill type (e.g., "reading", "writing")
       * @param {boolean} forceNew - Force create new attempt
       * @returns {Promise<object>} Attempt data from API
       */
      startOrResumeAttempt: async (source, testNum, skill, forceNew = false) => {
        set({ attemptStatus: 'loading' }, false, 'startOrResumeAttempt/pending');
        
        try {
          const response = await testAttemptApi.startAttempt(source, testNum, skill, forceNew);
          const attemptData = response.data;
          
          set({
            currentAttemptId: attemptData.id,
            attemptStatus: 'ready',
          }, false, 'startOrResumeAttempt/fulfilled');
          
          return attemptData;
        } catch (error) {
          console.error('Failed to start/resume attempt:', error);
          set({ attemptStatus: 'error' }, false, 'startOrResumeAttempt/rejected');
          throw error;
        }
      },

      /**
       * Load test data with caching
       * @param {string} source - Exam source
       * @param {number} testNum - Test number
       * @param {string} skill - Skill type
       * @returns {Promise<object>} Test data
       */
      loadTestData: async (source, testNum, skill) => {
        const cacheKey = `${source}-${testNum}-${skill}`;
        const cached = get().testDataCache[cacheKey];
        const now = Date.now();
        
        // Check if cache is valid (exists and not stale)
        if (cached && (now - cached.fetchedAt) < CACHE_TTL) {
          console.log(`📦 Using cached test data for ${cacheKey}`);
          return cached.data;
        }
        
        // Fetch fresh data
        console.log(`🔄 Fetching fresh test data for ${cacheKey}`);
        try {
          const testData = await testApi.getFullTest(source, testNum, skill);
          
          // Update cache
          set((state) => ({
            testDataCache: {
              ...state.testDataCache,
              [cacheKey]: {
                data: testData,
                fetchedAt: now,
              },
            },
          }), false, 'loadTestData/cached');
          
          return testData;
        } catch (error) {
          console.error(`Failed to load test data for ${cacheKey}:`, error);
          throw error;
        }
      },

      /**
       * Load answers for an attempt
       * @param {string} attemptId - Attempt ID
       * @returns {Promise<object>} Answers object
       */
      loadAnswers: async (attemptId) => {
        try {
          const response = await testAttemptApi.getAttemptAnswers(attemptId);
          return response.data;
        } catch (error) {
          console.error('Failed to load answers:', error);
          throw error;
        }
      },

      /**
       * Load essays/writing submissions for an attempt
       * @param {string} attemptId - Attempt ID
       * @returns {Promise<object>} Essays object
       */
      loadEssays: async (attemptId) => {
        try {
          const response = await writingApi.getSubmissions(attemptId);
          return response.data;
        } catch (error) {
          console.error('Failed to load essays:', error);
          throw error;
        }
      },

      /**
       * Save test progress
       * @param {string} attemptId - Attempt ID
       * @param {object} progressData - Progress data
       * @param {object} progressData.answers - User answers
       * @param {object} progressData.essays - Writing essays (for writing tests)
       * @param {number} progressData.timeLeft - Remaining time in seconds
       * @param {number} progressData.currentPart - Current part/section index
       * @returns {Promise<void>}
       */
      saveProgress: async (attemptId, { answers, essays, timeLeft, currentPart }) => {
        try {
          // Save standard progress (answers, time, part)
          await testAttemptApi.saveProgress(attemptId, {
            answers: answers || {},
            timeLeft,
            currentPart,
          });
          
          // For writing tests, save each essay draft
          if (essays && Object.keys(essays).length > 0) {
            const essayPromises = Object.entries(essays).map(([taskNumber, essayText]) => {
              if (essayText && essayText.trim()) {
                return writingApi.saveDraft(attemptId, parseInt(taskNumber, 10), essayText);
              }
              return Promise.resolve();
            });
            
            await Promise.all(essayPromises);
          }
          
          set({ lastSavedAt: new Date() }, false, 'saveProgress/fulfilled');
        } catch (error) {
          console.error('Failed to save progress:', error);
          throw error;
        }
      },

      /**
       * Submit a reading/listening test attempt
       * @param {string} attemptId - Attempt ID
       * @param {object} answers - Final answers
       * @returns {Promise<object>} Result with attemptId and score
       */
      submitAttempt: async (attemptId, answers) => {
        try {
          const response = await testAttemptApi.submitAttempt(attemptId, answers);
          
          set({
            currentAttemptId: null,
            attemptStatus: 'idle',
          }, false, 'submitAttempt/fulfilled');
          
          return response.data;
        } catch (error) {
          console.error('Failed to submit attempt:', error);
          throw error;
        }
      },

      /**
       * Submit writing test for AI grading
       * @param {string} attemptId - Attempt ID
       * @param {object} essays - Essays object { taskNumber: essayText }
       * @returns {Promise<object>} Submission result
       */
      submitWriting: async (attemptId, essays) => {
        try {
          const response = await writingApi.submitForGrading(attemptId, essays);
          
          set({
            currentAttemptId: null,
            attemptStatus: 'idle',
          }, false, 'submitWriting/fulfilled');
          
          return response.data;
        } catch (error) {
          console.error('Failed to submit writing:', error);
          throw error;
        }
      },

      /**
       * Cancel/delete a test attempt
       * @param {string} attemptId - Attempt ID
       * @returns {Promise<void>}
       */
      cancelAttempt: async (attemptId) => {
        try {
          await testAttemptApi.cancelAttempt(attemptId);
          
          set({
            currentAttemptId: null,
            attemptStatus: 'idle',
            lastSavedAt: null,
          }, false, 'cancelAttempt/fulfilled');
        } catch (error) {
          console.error('Failed to cancel attempt:', error);
          throw error;
        }
      },

      /**
       * Clear all cached test data
       */
      clearCache: () => {
        set({ testDataCache: {} }, false, 'clearCache');
      },

      /**
       * Enable/disable auto-save
       * @param {boolean} enabled - Whether auto-save is enabled
       */
      setAutoSave: (enabled) => {
        set({ autoSaveEnabled: enabled }, false, 'setAutoSave');
      },

      /**
       * Reset store to initial state
       */
      reset: () => {
        set({
          currentAttemptId: null,
          attemptStatus: 'idle',
          lastSavedAt: null,
          autoSaveEnabled: true,
          // Keep cache intact on reset
        }, false, 'reset');
      },
    }),
    {
      name: 'test-session-store',
      enabled: import.meta.env.DEV,
    }
  )
);

export default useTestSessionStore;
