-- Migration 011: Add ATTEMPT tracking columns to user_subscriptions
-- Date: 2025-12-14
-- Purpose: Track monthly ATTEMPT and ATTEMPT_AI usage per user

-- Add new usage tracking columns to user_subscriptions
ALTER TABLE user_subscriptions
ADD COLUMN IF NOT EXISTS attempts_used INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS attempt_ais_used INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS chatbot_used INTEGER NOT NULL DEFAULT 0;

-- Add comments for clarity
COMMENT ON COLUMN user_subscriptions.attempts_used IS 'Monthly ATTEMPT usage count (regular test attempts)';
COMMENT ON COLUMN user_subscriptions.attempt_ais_used IS 'Monthly ATTEMPT_AI usage count (AI graded attempts)';
COMMENT ON COLUMN user_subscriptions.chatbot_used IS 'Monthly chatbot messages used';

-- Create index for efficient usage queries
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_usage 
ON user_subscriptions (user_id, status, attempts_used, attempt_ais_used);
