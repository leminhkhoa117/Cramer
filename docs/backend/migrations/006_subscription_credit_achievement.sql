-- Migration: 006_subscription_credit_achievement.sql
-- Description: Create tables for subscription tiers, user credits (Lúa), and achievements
-- Author: executionAgent
-- Date: 2025-12-13

-- ============================================================================
-- SUBSCRIPTION TIERS (Static tier definitions)
-- ============================================================================
CREATE TABLE IF NOT EXISTS subscription_tiers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_vi VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    price_vnd INTEGER NOT NULL DEFAULT 0,
    included_ai_gradings INTEGER NOT NULL DEFAULT 0,
    daily_chat_limit INTEGER NOT NULL DEFAULT 20,  -- -1 for unlimited
    monthly_lua_bonus INTEGER NOT NULL DEFAULT 0,
    initial_lua INTEGER NOT NULL DEFAULT 50,
    features JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER DEFAULT 0
);

-- Insert default tiers
INSERT INTO subscription_tiers (code, name_vi, name_en, price_vnd, included_ai_gradings, daily_chat_limit, monthly_lua_bonus, initial_lua, features, display_order)
VALUES 
    ('cramerie', 'Cramerie', 'Cramerie', 0, 0, 20, 0, 50, 
     '["limited_tests", "normal_grading", "vocabulary", "basic_progress"]'::jsonb, 1),
    ('cramerich', 'Cramerich', 'Cramerich', 79000, 5, 100, 20, 100, 
     '["all_tests", "normal_grading", "ai_writing_grading", "vocabulary", "full_progress", "email_support"]'::jsonb, 2),
    ('cramerous', 'Cramerous', 'Cramerous', 149000, 10, -1, 50, 200, 
     '["all_tests", "normal_grading", "ai_writing_grading", "ai_speaking_grading", "vocabulary", "full_progress", "analytics", "priority_support"]'::jsonb, 3)
ON CONFLICT (code) DO NOTHING;

-- ============================================================================
-- USER SUBSCRIPTIONS
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_subscriptions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    tier_id BIGINT NOT NULL REFERENCES subscription_tiers(id),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,  -- NULL for free tier (no expiry)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ai_gradings_used INTEGER NOT NULL DEFAULT 0,
    payment_reference VARCHAR(255),
    auto_renew BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_user_subscriptions_user_id ON user_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_status ON user_subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_expires_at ON user_subscriptions(expires_at);

-- RLS for user_subscriptions
ALTER TABLE user_subscriptions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own subscriptions" ON user_subscriptions
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Service role full access to subscriptions" ON user_subscriptions
    FOR ALL TO service_role
    USING (true) WITH CHECK (true);

-- ============================================================================
-- USER CREDITS (Lúa)
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_credits (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    balance INTEGER NOT NULL DEFAULT 0,
    lifetime_earned INTEGER NOT NULL DEFAULT 0,
    lifetime_spent INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_credits_user_id ON user_credits(user_id);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_user_credits_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_user_credits_updated_at ON user_credits;
CREATE TRIGGER trigger_user_credits_updated_at
    BEFORE UPDATE ON user_credits
    FOR EACH ROW
    EXECUTE FUNCTION update_user_credits_updated_at();

-- RLS for user_credits
ALTER TABLE user_credits ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own credits" ON user_credits
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Service role full access to credits" ON user_credits
    FOR ALL TO service_role
    USING (true) WITH CHECK (true);

-- ============================================================================
-- CREDIT TRANSACTIONS
-- ============================================================================
CREATE TABLE IF NOT EXISTS credit_transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    amount INTEGER NOT NULL,  -- Positive for earn, negative for spend
    balance_after INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL,  -- EARN, SPEND
    category VARCHAR(50) NOT NULL,  -- See CreditTransaction.Category enum
    description VARCHAR(500),
    reference_id VARCHAR(255),  -- External reference
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_credit_transactions_user_id ON credit_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_credit_transactions_created_at ON credit_transactions(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_credit_transactions_type ON credit_transactions(user_id, type);
CREATE INDEX IF NOT EXISTS idx_credit_transactions_category ON credit_transactions(user_id, category);

-- RLS for credit_transactions
ALTER TABLE credit_transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own transactions" ON credit_transactions
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Service role full access to transactions" ON credit_transactions
    FOR ALL TO service_role
    USING (true) WITH CHECK (true);

-- ============================================================================
-- ACHIEVEMENTS (Badge definitions)
-- ============================================================================
CREATE TABLE IF NOT EXISTS achievements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_vi VARCHAR(200) NOT NULL,
    name_en VARCHAR(200) NOT NULL,
    description_vi TEXT,
    description_en TEXT,
    icon_name VARCHAR(100),
    category VARCHAR(30) NOT NULL,  -- LEARNING, STREAK, MASTERY, SOCIAL, COLLECTION, SPECIAL
    lua_reward INTEGER NOT NULL DEFAULT 0,
    threshold INTEGER,  -- For milestone achievements
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER DEFAULT 0,
    is_secret BOOLEAN DEFAULT FALSE
);

-- Insert default achievements
INSERT INTO achievements (code, name_vi, name_en, description_vi, description_en, icon_name, category, lua_reward, threshold, display_order)
VALUES 
    -- Learning achievements
    ('first_reading', 'Bước chân đầu tiên (Reading)', 'First Steps (Reading)', 'Hoàn thành bài Reading đầu tiên', 'Complete your first Reading test', 'book-open', 'LEARNING', 10, NULL, 1),
    ('first_listening', 'Tai thính (Listening)', 'Sharp Ears (Listening)', 'Hoàn thành bài Listening đầu tiên', 'Complete your first Listening test', 'headphones', 'LEARNING', 10, NULL, 2),
    ('first_writing', 'Cây bút vàng (Writing)', 'Golden Pen (Writing)', 'Hoàn thành bài Writing đầu tiên', 'Complete your first Writing test', 'edit-3', 'LEARNING', 10, NULL, 3),
    ('test_10', 'Chăm chỉ luyện tập', 'Dedicated Learner', 'Hoàn thành 10 bài test', 'Complete 10 tests', 'award', 'LEARNING', 20, 10, 4),
    ('test_50', 'Bền bỉ không ngừng', 'Persistent Scholar', 'Hoàn thành 50 bài test', 'Complete 50 tests', 'medal', 'LEARNING', 50, 50, 5),
    
    -- Streak achievements
    ('streak_7', 'Tuần lễ chăm chỉ', 'Week of Dedication', 'Duy trì streak 7 ngày liên tiếp', 'Maintain a 7-day streak', 'flame', 'STREAK', 15, 7, 10),
    ('streak_30', 'Tháng rực rỡ', 'Glorious Month', 'Duy trì streak 30 ngày liên tiếp', 'Maintain a 30-day streak', 'fire', 'STREAK', 50, 30, 11),
    ('streak_100', 'Huyền thoại kiên trì', 'Legend of Persistence', 'Duy trì streak 100 ngày liên tiếp', 'Maintain a 100-day streak', 'zap', 'STREAK', 200, 100, 12),
    
    -- Mastery achievements
    ('band_6', 'Vượt ngưỡng 6.0', 'Breaking 6.0', 'Đạt Band 6.0+ trong bài Writing', 'Achieve Band 6.0+ in Writing', 'trending-up', 'MASTERY', 30, NULL, 20),
    ('band_7', 'Chinh phục 7.0', 'Conquering 7.0', 'Đạt Band 7.0+ trong bài Writing', 'Achieve Band 7.0+ in Writing', 'star', 'MASTERY', 50, NULL, 21),
    ('band_8', 'Đỉnh cao 8.0', 'Peak Performance', 'Đạt Band 8.0+ trong bài Writing', 'Achieve Band 8.0+ in Writing', 'crown', 'MASTERY', 100, NULL, 22),
    
    -- Collection achievements  
    ('vocab_50', 'Kho từ vựng nhỏ', 'Small Vocabulary Vault', 'Lưu 50 từ vựng vào sổ tay', 'Save 50 words to vocabulary notebook', 'book', 'COLLECTION', 20, 50, 30),
    ('vocab_200', 'Kho từ vựng lớn', 'Large Vocabulary Vault', 'Lưu 200 từ vựng vào sổ tay', 'Save 200 words to vocabulary notebook', 'library', 'COLLECTION', 50, 200, 31),
    ('vocab_500', 'Bậc thầy từ vựng', 'Vocabulary Master', 'Lưu 500 từ vựng vào sổ tay', 'Save 500 words to vocabulary notebook', 'graduation-cap', 'COLLECTION', 100, 500, 32),
    
    -- Social achievements
    ('referral_1', 'Người giới thiệu', 'Referrer', 'Giới thiệu 1 người bạn đăng ký', 'Refer 1 friend who signs up', 'users', 'SOCIAL', 100, 1, 40),
    ('referral_5', 'Đại sứ Cramer', 'Cramer Ambassador', 'Giới thiệu 5 người bạn đăng ký', 'Refer 5 friends who sign up', 'user-plus', 'SOCIAL', 300, 5, 41)
ON CONFLICT (code) DO NOTHING;

-- ============================================================================
-- USER ACHIEVEMENTS (Earned badges)
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_achievements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    achievement_id BIGINT NOT NULL REFERENCES achievements(id),
    earned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notified BOOLEAN DEFAULT FALSE,
    UNIQUE(user_id, achievement_id)
);

CREATE INDEX IF NOT EXISTS idx_user_achievements_user_id ON user_achievements(user_id);
CREATE INDEX IF NOT EXISTS idx_user_achievements_earned_at ON user_achievements(user_id, earned_at DESC);

-- RLS for user_achievements
ALTER TABLE user_achievements ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own achievements" ON user_achievements
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Service role full access to user_achievements" ON user_achievements
    FOR ALL TO service_role
    USING (true) WITH CHECK (true);

-- ============================================================================
-- USER STREAKS (Login streaks)
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_streaks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    last_login_date DATE,
    last_streak_bonus_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_streaks_user_id ON user_streaks(user_id);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_user_streaks_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_user_streaks_updated_at ON user_streaks;
CREATE TRIGGER trigger_user_streaks_updated_at
    BEFORE UPDATE ON user_streaks
    FOR EACH ROW
    EXECUTE FUNCTION update_user_streaks_updated_at();

-- RLS for user_streaks
ALTER TABLE user_streaks ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own streaks" ON user_streaks
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Service role full access to streaks" ON user_streaks
    FOR ALL TO service_role
    USING (true) WITH CHECK (true);

-- ============================================================================
-- CHATBOT USAGE (Daily message tracking)
-- ============================================================================
CREATE TABLE IF NOT EXISTS chatbot_usage (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    messages_used INTEGER NOT NULL DEFAULT 0,
    UNIQUE(user_id, usage_date)
);

CREATE INDEX IF NOT EXISTS idx_chatbot_usage_user_date ON chatbot_usage(user_id, usage_date);

-- RLS for chatbot_usage
ALTER TABLE chatbot_usage ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own chatbot usage" ON chatbot_usage
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Service role full access to chatbot_usage" ON chatbot_usage
    FOR ALL TO service_role
    USING (true) WITH CHECK (true);

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE subscription_tiers IS 'Subscription tier definitions (Cramerie, Cramerich, Cramerous)';
COMMENT ON TABLE user_subscriptions IS 'User subscription records with usage tracking';
COMMENT ON TABLE user_credits IS 'User Lúa (credit) balances';
COMMENT ON TABLE credit_transactions IS 'Transaction history for Lúa credits';
COMMENT ON TABLE achievements IS 'Badge/achievement definitions';
COMMENT ON TABLE user_achievements IS 'Badges earned by users';
COMMENT ON TABLE user_streaks IS 'User login streak tracking';
COMMENT ON TABLE chatbot_usage IS 'Daily chatbot message usage tracking';
