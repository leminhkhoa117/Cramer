-- Migration: 20260416_speaking_mock_sections_draft
-- Purpose:
--   Ensure Speaking REST API never creates sessions from mock bank (5/2/4) on unpublished tests.
--   SpeakingContentServiceImpl.isPublishedSpeakingSection() only checks sections.status='PUBLISHED',
--   NOT tests.is_published. Mock tests #32-#41 had sections status=PUBLISHED while tests.is_published=false,
--   so a user could bypass UI and hit POST /api/speaking/sessions with testId=32,mode=PART_2 and get
--   the 2-cue-card mock data (violates official bank contract 30/1/15).
--
-- Policy decided for #12:
--   - Official Speaking test is test_id=1 (Cam17 "Bài thi 1") which meets 30/1/15 and is_published=true.
--   - Mock tests (test_set 'speaking-mvp') stay as DRAFT-only sandbox; no section listed PUBLISHED until
--     their bank is backfilled to the official contract.
--
-- Effect: SpeakingContentServiceImpl.loadSpeakingBanks() will throw
--   "Test X does not have published Speaking content" for mock tests.
--
-- Rollback: manually set sections.status back to 'PUBLISHED' for specific section_ids
--   after verifying their bank meets 30/1/15.

BEGIN;

UPDATE public.sections
SET status = 'DRAFT'
WHERE skill = 'speaking'
  AND test_id IN (
    SELECT id FROM public.tests WHERE is_published = false
  )
  AND status = 'PUBLISHED';

COMMIT;

-- Verification (expected: all rows for unpublished tests have status='DRAFT',
--  test_id=1 still PUBLISHED with 30/1/15):
-- select t.id, t.is_published, s.part_number, s.status, count(q.id) q_count
-- from public.tests t
-- join public.sections s on s.test_id=t.id
-- left join public.questions q on q.section_id=s.id
-- where s.skill='speaking'
-- group by t.id, t.is_published, s.part_number, s.status
-- order by t.id, s.part_number;
