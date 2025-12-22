-- Migration: 015_sample_data_for_reports.sql
-- Description: Sample data for Finance Reports (Subscription Analysis, Lua Economy, User Acquisition)
-- Date: 2025-12-22
-- Author: AI Agent
--
-- NOTE: Run this in Supabase SQL Editor to add sample data for testing reports

-- ============================================================================
-- STEP 1: Update user_subscriptions table structure if needed
-- ============================================================================

-- Add end_date column if not exists (for subscription tracking)
ALTER TABLE user_subscriptions 
ADD COLUMN IF NOT EXISTS end_date TIMESTAMPTZ;

-- Update expires_at to end_date for expired subscriptions logic
UPDATE user_subscriptions 
SET end_date = expires_at 
WHERE end_date IS NULL AND expires_at IS NOT NULL;

-- ============================================================================
-- STEP 2: Fix credit_transactions table structure
-- ============================================================================

-- Add missing columns if they don't exist
ALTER TABLE credit_transactions 
ADD COLUMN IF NOT EXISTS credits_gained INTEGER DEFAULT 0;

ALTER TABLE credit_transactions 
ADD COLUMN IF NOT EXISTS credits_spent INTEGER DEFAULT 0;

ALTER TABLE credit_transactions 
ADD COLUMN IF NOT EXISTS transaction_date TIMESTAMPTZ DEFAULT NOW();

-- Migrate old data format to new format
UPDATE credit_transactions 
SET 
    credits_gained = CASE WHEN amount > 0 THEN amount ELSE 0 END,
    credits_spent = CASE WHEN amount < 0 THEN amount ELSE 0 END,
    transaction_date = COALESCE(transaction_date, created_at)
WHERE credits_gained = 0 AND credits_spent = 0;

-- ============================================================================
-- STEP 3: Sample User Subscriptions (Active and Expired)
-- ============================================================================

-- Get tier IDs
DO $$
DECLARE
    cramerie_tier_id BIGINT;
    cramerich_tier_id BIGINT;
    sample_users UUID[];
BEGIN
    -- Get tier IDs
    SELECT id INTO cramerie_tier_id FROM subscription_tiers WHERE code = 'cramerie' LIMIT 1;
    SELECT id INTO cramerich_tier_id FROM subscription_tiers WHERE code = 'cramerich' LIMIT 1;
    
    -- Get some existing users
    SELECT ARRAY_AGG(id) INTO sample_users 
    FROM (SELECT id FROM profiles ORDER BY created_at LIMIT 10) t;
    
    -- Only proceed if we have users and tiers
    IF sample_users IS NOT NULL AND array_length(sample_users, 1) > 0 
       AND cramerich_tier_id IS NOT NULL THEN
        
        -- Insert sample active subscriptions for first 3 users
        INSERT INTO user_subscriptions (user_id, tier_id, started_at, expires_at, end_date, status)
        SELECT 
            sample_users[i],
            cramerich_tier_id,
            NOW() - INTERVAL '1 month' * (i - 1),
            NOW() + INTERVAL '1 month',
            NOW() + INTERVAL '1 month',
            'active'
        FROM generate_series(1, LEAST(3, array_length(sample_users, 1))) AS i
        ON CONFLICT DO NOTHING;
        
        -- Insert sample expired subscriptions for next 2 users
        IF array_length(sample_users, 1) > 3 THEN
            INSERT INTO user_subscriptions (user_id, tier_id, started_at, expires_at, end_date, status)
            SELECT 
                sample_users[i],
                cramerich_tier_id,
                NOW() - INTERVAL '2 month',
                NOW() - INTERVAL '1 week' * (i - 3),
                NOW() - INTERVAL '1 week' * (i - 3),
                'expired'
            FROM generate_series(4, LEAST(5, array_length(sample_users, 1))) AS i
            ON CONFLICT DO NOTHING;
        END IF;
    END IF;
END $$;

-- ============================================================================
-- STEP 4: Sample Credit Transactions (Purchases and Bonuses)
-- ============================================================================

DO $$
DECLARE
    sample_users UUID[];
    i INTEGER;
    current_user_id UUID;
BEGIN
    -- Get users
    SELECT ARRAY_AGG(id) INTO sample_users 
    FROM (SELECT id FROM profiles ORDER BY created_at LIMIT 10) t;
    
    IF sample_users IS NOT NULL AND array_length(sample_users, 1) > 0 THEN
        FOR i IN 1..LEAST(5, array_length(sample_users, 1)) LOOP
            current_user_id := sample_users[i];
            
            -- Insert purchase transactions
            INSERT INTO credit_transactions (user_id, amount, balance_after, type, category, description, credits_gained, credits_spent, transaction_date, created_at)
            VALUES 
                (current_user_id, 100, 100, 'EARN', 'PURCHASE', 'Mua gói Lúa 100', 100, 0, NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
                (current_user_id, 50, 150, 'EARN', 'BONUS', 'Thưởng đăng ký mới', 50, 0, NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days'),
                (current_user_id, -20, 130, 'SPEND', 'AI_GRADING', 'AI chấm Writing Task 2', 0, -20, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'),
                (current_user_id, -10, 120, 'SPEND', 'EXTRA_ATTEMPT', 'Làm thêm bài Reading', 0, -10, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days')
            ON CONFLICT DO NOTHING;
        END LOOP;
        
        -- Add some bonus transactions for variety
        FOR i IN 1..LEAST(3, array_length(sample_users, 1)) LOOP
            current_user_id := sample_users[i];
            
            INSERT INTO credit_transactions (user_id, amount, balance_after, type, category, description, credits_gained, credits_spent, transaction_date, created_at)
            VALUES 
                (current_user_id, 20, 140, 'EARN', 'BONUS', 'Thưởng streak 7 ngày', 20, 0, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
                (current_user_id, 10, 150, 'EARN', 'BONUS', 'Thưởng hoàn thành bài test', 10, 0, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days')
            ON CONFLICT DO NOTHING;
        END LOOP;
    END IF;
END $$;

-- ============================================================================
-- STEP 5: Update profiles with sample credits
-- ============================================================================

-- Ensure profiles have credits column
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS credits INTEGER DEFAULT 50;

-- Update credits for users with transactions
UPDATE profiles p
SET credits = COALESCE((
    SELECT SUM(
        CASE 
            WHEN type = 'EARN' THEN ABS(amount)
            WHEN type = 'SPEND' THEN -ABS(amount)
            ELSE 0 
        END
    )
    FROM credit_transactions ct
    WHERE ct.user_id = p.id
), 50)
WHERE EXISTS (SELECT 1 FROM credit_transactions ct WHERE ct.user_id = p.id);

-- ============================================================================
-- STEP 6: Verify the data
-- ============================================================================

-- Check subscription data
-- SELECT 
--     us.status, 
--     COUNT(*) as count,
--     st.code as tier
-- FROM user_subscriptions us
-- JOIN subscription_tiers st ON us.tier_id = st.id
-- GROUP BY us.status, st.code;

-- Check credit transaction types
-- SELECT 
--     type, 
--     category,
--     COUNT(*) as count,
--     SUM(ABS(amount)) as total_amount
-- FROM credit_transactions
-- GROUP BY type, category
-- ORDER BY type, category;

-- Check user credits
-- SELECT 
--     COUNT(*) as users_with_credits,
--     AVG(credits) as avg_credits,
--     SUM(credits) as total_credits
-- FROM profiles
-- WHERE credits > 0;

COMMENT ON TABLE credit_transactions IS 'Credit transactions with updated columns for reports';
