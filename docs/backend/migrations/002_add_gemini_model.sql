-- Migration: Add gemini_model column to profiles table
-- Date: 2025-12-07
-- Description: Allow users to select their preferred Gemini model for AI grading

-- Add gemini_model column to profiles table
ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS gemini_model VARCHAR(50) DEFAULT 'gemini-2.5-flash';

-- Add comment for documentation
COMMENT ON COLUMN public.profiles.gemini_model IS 'User-selected Gemini model for AI grading (gemini-2.5-flash, gemini-2.5-flash-lite, gemini-2.5-pro, gemma-3-27b-it)';
