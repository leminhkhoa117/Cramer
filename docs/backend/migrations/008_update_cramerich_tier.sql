-- Migration: 008_update_cramerich_tier.sql
-- Description: Update subscription tier definitions with new pricing and AI feature limits
-- Author: Backend Agent
-- Date: 2025-12-14

-- ============================================================================
-- ADD NEW COLUMNS FOR VOCAB AI AND CHATBOT LIMITS
-- ============================================================================

-- Add vocab_ai_daily_limit column (daily uses for Vocab AI feature)
ALTER TABLE subscription_tiers 
ADD COLUMN IF NOT EXISTS vocab_ai_daily_limit INTEGER NOT NULL DEFAULT 0;

-- Add chatbot_monthly_limit column (monthly messages for chatbot)
ALTER TABLE subscription_tiers 
ADD COLUMN IF NOT EXISTS chatbot_monthly_limit INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN subscription_tiers.vocab_ai_daily_limit IS 'Daily limit for Vocab AI feature uses';
COMMENT ON COLUMN subscription_tiers.chatbot_monthly_limit IS 'Monthly limit for chatbot messages';

-- ============================================================================
-- UPDATE CRAMERICH TIER
-- ============================================================================

-- Update Cramerich tier with new pricing and limits
UPDATE subscription_tiers
SET 
    price_vnd = 69000,
    vocab_ai_daily_limit = 50,
    chatbot_monthly_limit = 500,
    features = '[
        "all_tests",
        "all_topics",
        "all_skills",
        "ai_writing_grading",
        "ai_reading_grading",
        "ai_listening_grading",
        "vocabulary",
        "vocab_ai",
        "chatbot",
        "full_progress",
        "email_support"
    ]'::jsonb
WHERE code = 'cramerich';

-- ============================================================================
-- UPDATE CRAMERIE (FREE) TIER
-- ============================================================================

-- Update Cramerie tier to clarify restricted access and no AI features
UPDATE subscription_tiers
SET 
    vocab_ai_daily_limit = 0,
    chatbot_monthly_limit = 0,
    features = '[
        "restricted_topics",
        "normal_grading",
        "vocabulary",
        "basic_progress"
    ]'::jsonb
WHERE code = 'cramerie';

-- ============================================================================
-- UPDATE CRAMEROUS TIER (if exists) - maintain higher limits
-- ============================================================================

UPDATE subscription_tiers
SET 
    vocab_ai_daily_limit = -1,  -- Unlimited
    chatbot_monthly_limit = -1  -- Unlimited
WHERE code = 'cramerous';

-- ============================================================================
-- VERIFICATION QUERY (for manual verification)
-- ============================================================================
-- Run this query to verify the updates:
-- SELECT code, name_en, price_vnd, vocab_ai_daily_limit, chatbot_monthly_limit, features 
-- FROM subscription_tiers ORDER BY display_order;
