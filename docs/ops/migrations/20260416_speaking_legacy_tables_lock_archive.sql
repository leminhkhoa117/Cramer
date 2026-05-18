-- Migration: 20260416_speaking_legacy_tables_lock_archive
-- Purpose:
--   Finalize the "archive" decision for speaking_*_legacy tables per SUB-ISSUE 3 (#12).
--   Inventory (run 2026-04-16):
--     speaking_topics_legacy       - 10 rows
--     speaking_tests_legacy        - 10 rows
--     speaking_questions_legacy    - 117 rows
--     speaking_fixed_questions_legacy - 4 rows
--     speaking_sessions_legacy     - 60 rows
--     speaking_transcripts_legacy  - 82 rows
--
--   Code search in backend/ and frontend/ for references to these table names (excluding the
--   migration files themselves): 0 matches. No runtime path reads/writes them.
--
-- Policy chosen: KEEP as archive (not drop). Reason:
--   - Audit/traceability value (past user sessions recorded under old schema).
--   - Cost is negligible vs. legal/operational safety of having the data.
--   - Dropping is a one-way destructive operation; archive keeps the option open.
--
-- Effect of this migration:
--   1. RLS enabled on all legacy tables (fail-closed).
--   2. All user/anon policies removed -> only service_role can read.
--   3. COMMENT marks the table as ARCHIVE-ONLY so future contributors know.

BEGIN;

ALTER TABLE public.speaking_topics_legacy ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.speaking_tests_legacy ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.speaking_questions_legacy ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.speaking_fixed_questions_legacy ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.speaking_sessions_legacy ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.speaking_transcripts_legacy ENABLE ROW LEVEL SECURITY;

DO $$
DECLARE r record;
BEGIN
  FOR r IN
    SELECT schemaname, tablename, policyname
    FROM pg_policies
    WHERE schemaname='public'
      AND tablename IN (
        'speaking_topics_legacy',
        'speaking_tests_legacy',
        'speaking_questions_legacy',
        'speaking_fixed_questions_legacy',
        'speaking_sessions_legacy',
        'speaking_transcripts_legacy'
      )
  LOOP
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I.%I', r.policyname, r.schemaname, r.tablename);
  END LOOP;
END $$;

COMMENT ON TABLE public.speaking_topics_legacy IS 'ARCHIVE-ONLY (2026-04-16): no active runtime references. See docs/short_term_plans/speaking_github_issue.md SUB-ISSUE 3.';
COMMENT ON TABLE public.speaking_tests_legacy IS 'ARCHIVE-ONLY (2026-04-16): no active runtime references.';
COMMENT ON TABLE public.speaking_questions_legacy IS 'ARCHIVE-ONLY (2026-04-16): no active runtime references.';
COMMENT ON TABLE public.speaking_fixed_questions_legacy IS 'ARCHIVE-ONLY (2026-04-16): no active runtime references.';
COMMENT ON TABLE public.speaking_sessions_legacy IS 'ARCHIVE-ONLY (2026-04-16): no active runtime references. Preserved for audit.';
COMMENT ON TABLE public.speaking_transcripts_legacy IS 'ARCHIVE-ONLY (2026-04-16): no active runtime references. Preserved for audit.';

COMMIT;
