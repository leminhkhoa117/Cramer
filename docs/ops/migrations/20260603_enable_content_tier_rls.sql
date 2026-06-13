-- Enable RLS on the remaining public content/tier tables.
-- Scope: sections, questions, subscription_tiers.
-- This migration is intentionally read-oriented for anon/authenticated clients:
-- - public clients can read published content and active tiers
-- - public clients cannot write these tables
-- - questions.correct_answer and questions.explanation are not granted to anon/authenticated roles
-- - backend/service_role access remains available for admin and server-side workflows

BEGIN;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename IN ('sections', 'questions', 'subscription_tiers')
      AND policyname NOT IN (
        'sections_select_public_content',
        'questions_select_public_content_safe_columns',
        'subscription_tiers_select_active'
      )
  ) THEN
    RAISE EXCEPTION 'Unexpected existing RLS policies on sections/questions/subscription_tiers. Audit pg_policies before applying.';
  END IF;
END $$;

CREATE SCHEMA IF NOT EXISTS private;

CREATE OR REPLACE FUNCTION private.cramer_is_public_section(p_section_id bigint)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.sections s
    LEFT JOIN public.tests t ON t.id = s.test_id
    LEFT JOIN public.test_sets ts ON ts.id = t.set_id
    WHERE s.id = p_section_id
      AND upper(coalesce(s.status, 'PUBLISHED')) = 'PUBLISHED'
      AND (
        s.test_id IS NULL
        OR (
          coalesce(t.is_published, false) = true
          AND coalesce(ts.is_published, false) = true
        )
      )
  );
$$;

REVOKE ALL ON FUNCTION private.cramer_is_public_section(bigint) FROM public;
GRANT USAGE ON SCHEMA private TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION private.cramer_is_public_section(bigint) TO anon, authenticated, service_role;

ALTER TABLE public.sections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscription_tiers ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.sections NO FORCE ROW LEVEL SECURITY;
ALTER TABLE public.questions NO FORCE ROW LEVEL SECURITY;
ALTER TABLE public.subscription_tiers NO FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS sections_select_public_content ON public.sections;
CREATE POLICY sections_select_public_content
ON public.sections
FOR SELECT
TO anon, authenticated
USING (private.cramer_is_public_section(id));

DROP POLICY IF EXISTS questions_select_public_content_safe_columns ON public.questions;
CREATE POLICY questions_select_public_content_safe_columns
ON public.questions
FOR SELECT
TO anon, authenticated
USING (private.cramer_is_public_section(section_id));

DROP POLICY IF EXISTS subscription_tiers_select_active ON public.subscription_tiers;
CREATE POLICY subscription_tiers_select_active
ON public.subscription_tiers
FOR SELECT
TO anon, authenticated
USING (is_active IS true);

REVOKE ALL PRIVILEGES ON public.sections FROM anon, authenticated;
REVOKE ALL PRIVILEGES ON public.questions FROM anon, authenticated;
REVOKE ALL PRIVILEGES ON public.subscription_tiers FROM anon, authenticated;

GRANT SELECT ON public.sections TO anon, authenticated;

GRANT SELECT (
  id,
  section_id,
  question_number,
  question_uid,
  question_type,
  question_content,
  image_url,
  word_limit
) ON public.questions TO anon, authenticated;

GRANT SELECT ON public.subscription_tiers TO anon, authenticated;

GRANT SELECT, INSERT, UPDATE, DELETE ON public.sections TO service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.questions TO service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.subscription_tiers TO service_role;

DO $$
DECLARE
  seq_name text;
BEGIN
  FOREACH seq_name IN ARRAY ARRAY[
    pg_get_serial_sequence('public.sections', 'id'),
    pg_get_serial_sequence('public.questions', 'id'),
    pg_get_serial_sequence('public.subscription_tiers', 'id')
  ]
  LOOP
    IF seq_name IS NOT NULL THEN
      EXECUTE format('GRANT USAGE, SELECT ON SEQUENCE %s TO service_role', seq_name);
    END IF;
  END LOOP;
END $$;

COMMIT;