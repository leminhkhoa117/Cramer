-- Migration: Add AI Grading Enabled toggle to user_subscriptions
-- Date: 2025-12-14
-- Description: 
--   Adds a toggle for users to enable/disable AI grading (ATTEMPT_AI).
--   - Cramerich users: Can toggle ON/OFF (default ON)
--   - Cramerie users: Always OFF (cannot enable)
--   When OFF: Writing submissions are saved but not AI-graded

-- Add the column with default TRUE (enabled)
ALTER TABLE user_subscriptions 
ADD COLUMN IF NOT EXISTS ai_grading_enabled BOOLEAN DEFAULT true;

-- Update existing Cramerie subscriptions to have AI grading disabled
-- (since they don't have access to this feature anyway)
UPDATE user_subscriptions us
SET ai_grading_enabled = false
FROM subscription_tiers st
WHERE us.tier_id = st.id 
  AND st.code = 'cramerie';

-- Add a comment explaining the column
COMMENT ON COLUMN user_subscriptions.ai_grading_enabled IS 
  'Whether AI grading is enabled for this user. Only applicable for Cramerich+ tiers. When disabled, Writing is saved but not AI-graded.';
