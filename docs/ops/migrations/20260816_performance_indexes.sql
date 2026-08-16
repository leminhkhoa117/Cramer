-- =============================================================================
-- 20260816_performance_indexes.sql
-- Phase 3: hot-path indexes for test attempts, user answers, and the
-- speaking grading watchdog.
--
-- Hot paths:
-- - AttemptRepository.lockByKey / resume/cancel: (user_id, exam_source,
--   test_number, skill)
-- - DashboardService user stats: user_id filters on test_attempts / user_answers
-- - SpeakingGradingWorker.findStuck: status + updated_at
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_test_attempts_user_status
    ON public.test_attempts (user_id, status);

CREATE INDEX IF NOT EXISTS idx_test_attempts_lock_key
    ON public.test_attempts (user_id, exam_source, test_number, skill, status);

CREATE INDEX IF NOT EXISTS idx_user_answers_user
    ON public.user_answers (user_id);

CREATE INDEX IF NOT EXISTS idx_speaking_sessions_status_updated
    ON public.speaking_sessions (status, updated_at);
