-- =====================================================
-- Writing Feature Database Migration
-- Created: December 5, 2025
-- Description: Add tables and columns for IELTS Writing test support
-- =====================================================

-- 1. Add gemini_api_key column to profiles table (encrypted storage)
ALTER TABLE profiles 
ADD COLUMN IF NOT EXISTS gemini_api_key TEXT;

COMMENT ON COLUMN profiles.gemini_api_key IS 'Encrypted Gemini API key for AI grading features';

-- 2. Create writing_submissions table for storing essays and AI feedback
CREATE TABLE IF NOT EXISTS writing_submissions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES test_attempts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    task_number INTEGER NOT NULL CHECK (task_number IN (1, 2)),
    essay_text TEXT NOT NULL,
    word_count INTEGER NOT NULL DEFAULT 0,
    
    -- AI Grading Results
    grading_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, GRADING, COMPLETED, FAILED
    overall_band DECIMAL(2,1),
    band_scores JSONB, -- {"task_response": 7.0, "coherence_cohesion": 6.5, "lexical_resource": 7.0, "grammatical_range": 6.5}
    
    -- Detailed AI Feedback
    ai_feedback JSONB, -- Contains sentence corrections, paragraph suggestions, sample essays, etc.
    
    -- Timestamps
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    graded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Ensure one submission per task per attempt
    UNIQUE(attempt_id, task_number)
);

-- Create indexes for writing_submissions
CREATE INDEX IF NOT EXISTS idx_writing_submissions_attempt ON writing_submissions(attempt_id);
CREATE INDEX IF NOT EXISTS idx_writing_submissions_user ON writing_submissions(user_id);
CREATE INDEX IF NOT EXISTS idx_writing_submissions_status ON writing_submissions(grading_status);

-- Enable RLS on writing_submissions
ALTER TABLE writing_submissions ENABLE ROW LEVEL SECURITY;

-- RLS policy for users to manage their own submissions
CREATE POLICY "Users can manage their own writing submissions"
ON writing_submissions
FOR ALL
USING (auth.uid() = user_id);

-- RLS policy for service_role (backend operations)
CREATE POLICY "Service role can manage all writing submissions"
ON writing_submissions
FOR ALL
TO service_role
USING (true)
WITH CHECK (true);

-- 3. Add Writing-specific question types (update existing question types)
-- These are informational - actual question types are stored as varchar
-- WRITING_TASK_1: Academic/GT Task 1 (describe chart/map/process OR write letter)
-- WRITING_TASK_2: Essay question (both Academic and GT)

COMMENT ON TABLE writing_submissions IS 'Stores user essay submissions for IELTS Writing tests with AI grading results';

-- 4. Create ai_feedback JSONB structure documentation
-- The ai_feedback column stores:
-- {
--   "sentence_corrections": [
--     {
--       "original": "This is orignal sentence.",
--       "corrected": "This is original sentence.",
--       "error_type": "spelling",
--       "explanation": "Corrected spelling of 'original'"
--     }
--   ],
--   "paragraph_rewrites": [
--     {
--       "paragraph_index": 0,
--       "original": "...",
--       "improved": "...",
--       "improvements_made": ["Better topic sentence", "Improved coherence"]
--     }
--   ],
--   "sample_essay_band_plus_one": "...",
--   "sample_essay_band_9": "...",
--   "feedback_summary": {
--     "strengths": ["Good vocabulary range", "Clear structure"],
--     "weaknesses": ["Some grammatical errors", "Limited complex sentences"],
--     "writing_approach": "Consider using more specific examples...",
--     "improvement_tips": "Focus on varying sentence structures..."
--   },
--   "word_definitions": {
--     "word": {
--       "definition": "...",
--       "context": "..."
--     }
--   }
-- }

-- 5. Update sections table to support Writing content
-- The existing sections table already supports writing with:
-- - skill = 'writing'
-- - passage_text = Task prompt/instructions
-- - display_content_url = Image for Task 1 (chart/map/diagram)
-- - part_number = Task number (1 or 2)

-- 6. Verify test_attempts supports writing skill
-- The existing test_attempts table already supports skill = 'writing'
