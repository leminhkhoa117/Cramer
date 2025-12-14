-- Migration: Add image_description column to sections table
-- Date: 2025-12-12
-- Description: Add text-based image descriptions for Writing Task 1 to support DeepSeek AI grading (no image input support)
-- Status: APPLIED

ALTER TABLE public.sections 
ADD COLUMN IF NOT EXISTS image_description TEXT;

COMMENT ON COLUMN public.sections.image_description IS 'Detailed text description of Task 1 charts/maps/diagrams for AI grading when image input is not supported';

-- ============================================================================
-- Example: Add description for Cambridge 17 Test 1 Writing Task 1
-- ============================================================================
-- UPDATE public.sections
-- SET image_description = 'The diagrams illustrate the current layout of Norbiton industrial area...'
-- WHERE exam_source = 'cam17' 
--   AND test_number = 1 
--   AND skill = 'writing' 
--   AND part_number = 1;

-- ============================================================================
-- Verification query
-- ============================================================================
-- SELECT id, exam_source, test_number, skill, part_number,
--        CASE 
--          WHEN image_description IS NOT NULL THEN 'Description exists (' || LENGTH(image_description) || ' chars)'
--          ELSE 'No description'
--        END as description_status
-- FROM sections
-- WHERE skill = 'writing'
-- ORDER BY exam_source, test_number, part_number;
