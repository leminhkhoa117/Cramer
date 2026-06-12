import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { testApi, attemptApi, writingApi } from '../lib/api';

const CACHE_TTL = 5 * 60 * 1000;

/** Normalize an answers map {questionId: value} or array to List<AnswerInput>{questionId,value}. */
function toAnswerList(answers) {
  if (!answers) return [];
  if (Array.isArray(answers)) {
    return answers
      .filter((a) => a && a.questionId != null)
      .map((a) => ({ questionId: Number(a.questionId), value: a.value ?? null }));
  }
  return Object.entries(answers).map(([qid, value]) => ({ questionId: Number(qid), value: value ?? null }));
}

/**
 * Test session store (SPEC-F12/F13): attempt lifecycle + test-data cache + progress/submit.
 * Talks to the new assessment/writing endpoints; converts the page's answer map to AnswerInput[].
 */
const useTestSessionStore = create(
  devtools(
    (set, get) => ({
      currentAttemptId: null,
      attemptStatus: 'idle',
      lastSavedAt: null,
      autoSaveEnabled: true,
      testDataCache: {},

      startOrResumeAttempt: async (source, testNum, skill, forceNew = false) => {
        set({ attemptStatus: 'loading' }, false, 'startOrResumeAttempt/pending');
        try {
          const attempt = await attemptApi.start(source, testNum, skill, forceNew);
          set({ currentAttemptId: attempt.id, attemptStatus: 'ready' }, false, 'startOrResumeAttempt/fulfilled');
          return attempt;
        } catch (error) {
          set({ attemptStatus: 'error' }, false, 'startOrResumeAttempt/rejected');
          throw error;
        }
      },

      loadTestData: async (source, testNum, skill) => {
        const key = `${source}-${testNum}-${skill}`;
        const cached = get().testDataCache[key];
        const now = Date.now();
        if (cached && now - cached.fetchedAt < CACHE_TTL) return cached.data;
        const data = await testApi.data(source, testNum, skill);
        set((s) => ({ testDataCache: { ...s.testDataCache, [key]: { data, fetchedAt: now } } }), false, 'loadTestData/cached');
        return data;
      },

      loadAnswers: (attemptId) => attemptApi.answers(attemptId),       // List<AnswerView>
      loadEssays: (attemptId) => writingApi.submissions(attemptId),    // List<WritingTaskReview>

      saveProgress: async (attemptId, { answers, essays, timeLeft, currentPart }) => {
        await attemptApi.saveProgress(attemptId, {
          currentPart: currentPart ?? null,
          timeLeft: timeLeft ?? null,
          answers: toAnswerList(answers),
        });
        if (essays && Object.keys(essays).length > 0) {
          await Promise.all(
            Object.entries(essays).map(([taskNumber, essayText]) =>
              essayText && essayText.trim()
                ? writingApi.saveDraft(attemptId, essayText, parseInt(taskNumber, 10))
                : Promise.resolve()
            )
          );
        }
        set({ lastSavedAt: new Date() }, false, 'saveProgress/fulfilled');
      },

      submitAttempt: async (attemptId, answers) => {
        const result = await attemptApi.submit(attemptId, { answers: toAnswerList(answers) });
        set({ currentAttemptId: null, attemptStatus: 'idle' }, false, 'submitAttempt/fulfilled');
        return result;
      },

      submitWriting: async (attemptId, essays) => {
        // essays: {taskNumber: text} -> [{taskNumber, essayText}]
        const list = Object.entries(essays || {}).map(([taskNumber, essayText]) => ({
          taskNumber: parseInt(taskNumber, 10),
          essayText,
        }));
        const result = await writingApi.submit(attemptId, list);
        set({ currentAttemptId: null, attemptStatus: 'idle' }, false, 'submitWriting/fulfilled');
        return result;
      },

      cancelAttempt: async (attemptId) => {
        await attemptApi.cancel(attemptId);
        set({ currentAttemptId: null, attemptStatus: 'idle', lastSavedAt: null }, false, 'cancelAttempt/fulfilled');
      },

      clearCache: () => set({ testDataCache: {} }, false, 'clearCache'),
      setAutoSave: (enabled) => set({ autoSaveEnabled: enabled }, false, 'setAutoSave'),
      reset: () => set({ currentAttemptId: null, attemptStatus: 'idle', lastSavedAt: null, autoSaveEnabled: true }, false, 'reset'),
    }),
    { name: 'TestSessionStore' }
  )
);

export default useTestSessionStore;
