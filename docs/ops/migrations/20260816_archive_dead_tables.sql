-- =============================================================================
-- 20260816_archive_dead_tables.sql
-- Phase 3: move dead tables out of public into an archive schema.
--
-- Dead tables (zero code references as of 16/08/2026):
-- - chatbot_usage         superseded by user_subscriptions.chatbot_used
-- - abts_templates        templates are served from hardcoded Java
-- - model_runtime_status  auxiliary, no reader/writer
-- - speaking_*_legacy x6  archived legacy speaking hierarchy
--
-- Data is fully preserved. Restore = ALTER TABLE archive.<t> SET SCHEMA public.
-- Foreign keys reference tables by OID, so moving keeps every constraint.
-- RLS policies move with their tables and stay enforced.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS archive;

ALTER TABLE public.chatbot_usage SET SCHEMA archive;
ALTER TABLE public.abts_templates SET SCHEMA archive;
ALTER TABLE public.model_runtime_status SET SCHEMA archive;
ALTER TABLE public.speaking_topics_legacy SET SCHEMA archive;
ALTER TABLE public.speaking_tests_legacy SET SCHEMA archive;
ALTER TABLE public.speaking_questions_legacy SET SCHEMA archive;
ALTER TABLE public.speaking_fixed_questions_legacy SET SCHEMA archive;
ALTER TABLE public.speaking_sessions_legacy SET SCHEMA archive;
ALTER TABLE public.speaking_transcripts_legacy SET SCHEMA archive;

ALTER SEQUENCE IF EXISTS public.chatbot_usage_id_seq SET SCHEMA archive;
ALTER SEQUENCE IF EXISTS public.speaking_topics_id_seq SET SCHEMA archive;
ALTER SEQUENCE IF EXISTS public.speaking_questions_id_seq SET SCHEMA archive;
ALTER SEQUENCE IF EXISTS public.speaking_sessions_id_seq SET SCHEMA archive;
ALTER SEQUENCE IF EXISTS public.speaking_transcripts_id_seq SET SCHEMA archive;
