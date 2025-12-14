-- Migration: 010_restructure_subscription_system.sql
-- Description: Complete restructure of subscription tiers for new ATTEMPT/ATTEMPT_AI system
-- Date: 2025-12-14
-- Author: AI Agent
--
-- CHANGES:
-- 1. Add new limit columns to subscription_tiers
-- 2. Remove Cramerous tier (soft delete)
-- 3. Update Cramerie (Free) with correct limits
-- 4. Update Cramerich (69,000đ) with ATTEMPT/ATTEMPT_AI system
-- 5. Create lua_packs table for Lúa pack definitions

-- ============================================================================
-- STEP 1: Add missing limit columns to subscription_tiers
-- ============================================================================

-- Global monthly limits for attempts
ALTER TABLE subscription_tiers 
ADD COLUMN IF NOT EXISTS monthly_attempt_limit INTEGER NOT NULL DEFAULT 0;

ALTER TABLE subscription_tiers 
ADD COLUMN IF NOT EXISTS monthly_attempt_ai_limit INTEGER NOT NULL DEFAULT 0;

-- Per-skill monthly limits
ALTER TABLE subscription_tiers 
ADD COLUMN IF NOT EXISTS per_skill_attempt_limit INTEGER NOT NULL DEFAULT 0;

ALTER TABLE subscription_tiers 
ADD COLUMN IF NOT EXISTS per_skill_attempt_ai_limit INTEGER NOT NULL DEFAULT 0;

-- Translation daily limit
ALTER TABLE subscription_tiers 
ADD COLUMN IF NOT EXISTS daily_translation_limit INTEGER NOT NULL DEFAULT 0;

-- Maximum vocabulary entries
ALTER TABLE subscription_tiers 
ADD COLUMN IF NOT EXISTS max_vocabulary_entries INTEGER NOT NULL DEFAULT 0;

-- Overage costs in Lúa
ALTER TABLE subscription_tiers 
ADD COLUMN IF NOT EXISTS attempt_overage_cost INTEGER NOT NULL DEFAULT 10;

ALTER TABLE subscription_tiers 
ADD COLUMN IF NOT EXISTS attempt_ai_overage_cost INTEGER NOT NULL DEFAULT 20;

-- Comments for clarity
COMMENT ON COLUMN subscription_tiers.monthly_attempt_limit IS 'Global monthly limit for test attempts (0 = unlimited for free tier)';
COMMENT ON COLUMN subscription_tiers.monthly_attempt_ai_limit IS 'Global monthly limit for AI-graded attempts';
COMMENT ON COLUMN subscription_tiers.per_skill_attempt_limit IS 'Per-skill monthly limit for test attempts (0 = unlimited)';
COMMENT ON COLUMN subscription_tiers.per_skill_attempt_ai_limit IS 'Per-skill monthly limit for AI-graded attempts';
COMMENT ON COLUMN subscription_tiers.daily_translation_limit IS 'Daily vocabulary AI translation limit';
COMMENT ON COLUMN subscription_tiers.max_vocabulary_entries IS 'Maximum vocabulary notebook entries allowed';
COMMENT ON COLUMN subscription_tiers.attempt_overage_cost IS 'Lúa cost per additional attempt beyond limit';
COMMENT ON COLUMN subscription_tiers.attempt_ai_overage_cost IS 'Lúa cost per additional AI attempt beyond limit';

-- ============================================================================
-- STEP 2: Soft delete Cramerous tier (reduce to 2 tiers only)
-- ============================================================================

-- First, migrate any Cramerous users to Cramerich (as a downgrade safety)
UPDATE user_subscriptions 
SET tier_id = (SELECT id FROM subscription_tiers WHERE code = 'cramerich')
WHERE tier_id = (SELECT id FROM subscription_tiers WHERE code = 'cramerous');

-- Soft delete Cramerous (keep record for audit, but hide from UI)
UPDATE subscription_tiers 
SET is_active = FALSE 
WHERE code = 'cramerous';

-- ============================================================================
-- STEP 3: Update Cramerie (Free Tier)
-- ============================================================================
-- Requirements:
-- - Free tier, no AI grading access
-- - Limited tests (some TOPICs only)
-- - Translation: 10/day
-- - Chatbot: 50/month (need Lúa for extra)
-- - Vocabulary: 250 max entries
-- - Initial Lúa: 50

UPDATE subscription_tiers
SET 
    name_vi = 'Cramerie',
    name_en = 'Cramerie',
    price_vnd = 0,
    -- Attempts: Cramerie users get 0 free attempts (they can only view limited tests)
    -- They need to upgrade to Cramerich for full test access
    monthly_attempt_limit = 0,        -- No free attempts
    monthly_attempt_ai_limit = 0,     -- No AI grading at all
    per_skill_attempt_limit = 0,      -- No per-skill attempts
    per_skill_attempt_ai_limit = 0,   -- No per-skill AI
    attempt_overage_cost = 10,        -- 10 Lúa per extra attempt
    attempt_ai_overage_cost = 20,     -- 20 Lúa per extra AI attempt
    -- Other limits
    daily_chat_limit = 0,             -- Legacy field, using chatbot_monthly_limit instead
    chatbot_monthly_limit = 50,       -- 50 questions/month
    daily_translation_limit = 10,     -- 10 translations/day
    max_vocabulary_entries = 250,     -- 250 max vocab entries
    vocab_ai_daily_limit = 10,        -- Same as translation limit
    included_ai_gradings = 0,         -- No AI gradings
    initial_lua = 50,
    monthly_lua_bonus = 0,
    features = '[
        "limited_tests",
        "basic_grading",
        "vocabulary_basic",
        "basic_progress",
        "community_access"
    ]'::jsonb,
    display_order = 1
WHERE code = 'cramerie';

-- ============================================================================
-- STEP 4: Update Cramerich (Paid Tier - 69,000đ/month)
-- ============================================================================
-- Requirements:
-- - Price: 69,000đ/month
-- - Access to ALL tests (full TOPIC/TEST/SKILL library)
-- - ATTEMPTs: 60 total/month, 20 per skill
-- - ATTEMPT_AIs: 30 total/month, 3 per skill  
-- - After limits: 10 Lúa/ATTEMPT, 20 Lúa/ATTEMPT_AI
-- - Translation: 50/day
-- - Chatbot: 500/month
-- - Vocabulary: 1000 max entries

UPDATE subscription_tiers
SET 
    name_vi = 'Cramerich',
    name_en = 'Cramerich',
    price_vnd = 69000,
    -- ATTEMPT limits (the core new feature)
    monthly_attempt_limit = 60,       -- 60 total ATTEMPTs/month
    monthly_attempt_ai_limit = 30,    -- 30 total ATTEMPT_AIs/month
    per_skill_attempt_limit = 20,     -- 20 ATTEMPTs per skill/month
    per_skill_attempt_ai_limit = 3,   -- 3 ATTEMPT_AIs per skill/month
    attempt_overage_cost = 10,        -- 10 Lúa per extra attempt
    attempt_ai_overage_cost = 20,     -- 20 Lúa per extra AI attempt
    -- Other limits
    daily_chat_limit = -1,            -- Unlimited daily (deprecated, use monthly)
    chatbot_monthly_limit = 500,      -- 500 questions/month
    daily_translation_limit = 50,     -- 50 translations/day
    max_vocabulary_entries = 1000,    -- 1000 max vocab entries
    vocab_ai_daily_limit = 50,        -- Same as translation
    included_ai_gradings = 30,        -- Align with monthly_attempt_ai_limit
    initial_lua = 100,
    monthly_lua_bonus = 20,           -- Bonus 20 Lúa/month for subscribers
    features = '[
        "all_tests",
        "all_skills",
        "ai_grading",
        "personalized_feedback",
        "vocabulary_full",
        "vocab_ai",
        "chatbot_full",
        "full_progress",
        "priority_support"
    ]'::jsonb,
    display_order = 2
WHERE code = 'cramerich';

-- ============================================================================
-- STEP 5: Create lua_packs table
-- ============================================================================

CREATE TABLE IF NOT EXISTS lua_packs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_vi VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    emoji VARCHAR(10) DEFAULT '🌾',
    lua_amount INTEGER NOT NULL,
    price_vnd INTEGER NOT NULL,
    discount_percent INTEGER NOT NULL DEFAULT 0,
    bonus_lua INTEGER NOT NULL DEFAULT 0,
    description_vi TEXT,
    description_en TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Comments for clarity
COMMENT ON TABLE lua_packs IS 'Virtual currency (Lúa) pack definitions for purchase';
COMMENT ON COLUMN lua_packs.lua_amount IS 'Base amount of Lúa in pack';
COMMENT ON COLUMN lua_packs.price_vnd IS 'Price in Vietnamese Dong';
COMMENT ON COLUMN lua_packs.discount_percent IS 'Discount percentage from base rate (100 Lúa = 10,000đ)';
COMMENT ON COLUMN lua_packs.bonus_lua IS 'Extra bonus Lúa included (for display purposes)';

-- Insert the 3 Lúa packs as per requirements
-- Base rate: 100 Lúa = 10,000đ (no discount)
-- 500 Lúa = 45,000đ (10% off from 50,000đ)
-- 2000 Lúa = 150,000đ (25% off from 200,000đ)

INSERT INTO lua_packs (code, name_vi, name_en, emoji, lua_amount, price_vnd, discount_percent, bonus_lua, description_vi, description_en, display_order)
VALUES 
    ('lua_100', 'Túi Lúa Nhỏ', 'Small Lúa Pack', '🌾', 100, 10000, 0, 0, 
     'Gói khởi đầu hoàn hảo', 'Perfect starter pack', 1),
    ('lua_500', 'Túi Lúa Vừa', 'Medium Lúa Pack', '🌻', 500, 45000, 10, 0, 
     'Tiết kiệm 10% - Phổ biến nhất!', 'Save 10% - Most Popular!', 2),
    ('lua_2000', 'Bao Lúa Lớn', 'Large Lúa Pack', '🌟', 2000, 150000, 25, 0, 
     'Tiết kiệm 25% - Giá trị tốt nhất!', 'Save 25% - Best Value!', 3)
ON CONFLICT (code) DO UPDATE SET
    name_vi = EXCLUDED.name_vi,
    name_en = EXCLUDED.name_en,
    emoji = EXCLUDED.emoji,
    lua_amount = EXCLUDED.lua_amount,
    price_vnd = EXCLUDED.price_vnd,
    discount_percent = EXCLUDED.discount_percent,
    bonus_lua = EXCLUDED.bonus_lua,
    description_vi = EXCLUDED.description_vi,
    description_en = EXCLUDED.description_en,
    display_order = EXCLUDED.display_order,
    updated_at = NOW();

-- ============================================================================
-- STEP 6: Enable RLS on lua_packs (public read, admin write)
-- ============================================================================

ALTER TABLE lua_packs ENABLE ROW LEVEL SECURITY;

-- Anyone can read active packs
CREATE POLICY "lua_packs_public_read" ON lua_packs
    FOR SELECT
    USING (is_active = TRUE);

-- Service role can do anything
CREATE POLICY "lua_packs_service_all" ON lua_packs
    FOR ALL
    TO service_role
    USING (TRUE)
    WITH CHECK (TRUE);

-- ============================================================================
-- VERIFICATION QUERIES (uncomment to test)
-- ============================================================================

-- Check tier configuration:
-- SELECT code, name_en, price_vnd, is_active,
--        monthly_attempt_limit, monthly_attempt_ai_limit,
--        per_skill_attempt_limit, per_skill_attempt_ai_limit,
--        attempt_overage_cost, attempt_ai_overage_cost,
--        daily_translation_limit, max_vocabulary_entries,
--        chatbot_monthly_limit, initial_lua
-- FROM subscription_tiers 
-- ORDER BY display_order;

-- Check Lúa packs:
-- SELECT code, name_en, lua_amount, price_vnd, discount_percent,
--        ROUND(price_vnd::numeric / lua_amount * 100, 0) as price_per_100_lua
-- FROM lua_packs 
-- WHERE is_active = TRUE
-- ORDER BY display_order;

-- Expected output for Lúa packs:
-- lua_100  | 100  | 10000  | 0%  | 10000đ per 100
-- lua_500  | 500  | 45000  | 10% | 9000đ per 100  
-- lua_2000 | 2000 | 150000 | 25% | 7500đ per 100
