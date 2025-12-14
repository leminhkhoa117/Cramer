-- Migration: DeepSeek V3.2 Migration - Rename Gemini columns to generic LLM columns
-- Date: 2025-12-12
-- Description: Migrate from Gemini-specific naming to generic LLM naming to support DeepSeek and other providers
-- 
-- Changes:
--   1. Rename gemini_api_key → llm_api_key
--   2. Rename gemini_model → llm_model
--   3. Add llm_provider column with default 'deepseek'
--   4. Update existing model values to 'deepseek-chat'

-- ============================================================================
-- Step 1: Rename gemini_api_key to llm_api_key
-- ============================================================================
ALTER TABLE public.profiles 
RENAME COLUMN gemini_api_key TO llm_api_key;

-- ============================================================================
-- Step 2: Rename gemini_model to llm_model
-- ============================================================================
ALTER TABLE public.profiles 
RENAME COLUMN gemini_model TO llm_model;

-- ============================================================================
-- Step 3: Add llm_provider column
-- ============================================================================
ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS llm_provider VARCHAR(50) DEFAULT 'deepseek';

-- ============================================================================
-- Step 4: Update existing Gemini model values to DeepSeek default
-- ============================================================================
UPDATE public.profiles 
SET llm_model = 'deepseek-chat'
WHERE llm_model IS NULL 
   OR llm_model IN (
       'gemini-2.5-flash', 
       'gemini-2.5-flash-lite', 
       'gemini-2.5-pro', 
       'gemma-3-27b-it'
   );

-- Set provider for all existing rows (in case any have NULL)
UPDATE public.profiles 
SET llm_provider = 'deepseek'
WHERE llm_provider IS NULL;

-- ============================================================================
-- Step 5: Add column comments for documentation
-- ============================================================================
COMMENT ON COLUMN public.profiles.llm_api_key IS 'User API key for LLM provider (DeepSeek, OpenAI, etc.) - encrypted at rest';

COMMENT ON COLUMN public.profiles.llm_model IS 'User-selected LLM model for AI grading. DeepSeek: deepseek-chat, deepseek-reasoner. OpenAI-compatible format.';

COMMENT ON COLUMN public.profiles.llm_provider IS 'LLM provider identifier: deepseek (default), openai, gemini, anthropic';

-- ============================================================================
-- Verification query (run manually to verify migration)
-- ============================================================================
-- SELECT id, llm_provider, llm_model, 
--        CASE WHEN llm_api_key IS NOT NULL THEN 'SET' ELSE 'NULL' END as api_key_status
-- FROM public.profiles 
-- LIMIT 10;
