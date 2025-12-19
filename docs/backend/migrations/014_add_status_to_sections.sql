-- Migration: Add status column to sections table
-- Created: 2024-12-19
-- Description: Add status column to support Draft/Published workflow for content management

-- Add status column to sections table
ALTER TABLE public.sections 
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PUBLISHED';

-- Add created_at and updated_at for tracking
ALTER TABLE public.sections 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

ALTER TABLE public.sections 
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- Update existing records to PUBLISHED status
UPDATE public.sections SET status = 'PUBLISHED' WHERE status IS NULL;

-- Add comment
COMMENT ON COLUMN public.sections.status IS 'Content status: DRAFT, PUBLISHED, ARCHIVED';
