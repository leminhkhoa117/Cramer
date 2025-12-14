-- Migration: 009_dual_quota_system.sql
-- Description: Create tables for dual-quota billing system (global + per-skill monthly limits)
-- Author: executionAgent
-- Date: 2025-12-14

-- ============================================================================
-- USER QUOTAS (Global monthly usage tracking)
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_quotas (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    quota_month DATE NOT NULL,  -- First day of month (e.g., 2025-12-01)
    attempt_count INTEGER NOT NULL DEFAULT 0,
    attempt_ai_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, quota_month)
);

CREATE INDEX IF NOT EXISTS idx_user_quotas_user_id ON user_quotas(user_id);
CREATE INDEX IF NOT EXISTS idx_user_quotas_month ON user_quotas(user_id, quota_month);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_user_quotas_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_user_quotas_updated_at ON user_quotas;
CREATE TRIGGER trigger_user_quotas_updated_at
    BEFORE UPDATE ON user_quotas
    FOR EACH ROW
    EXECUTE FUNCTION update_user_quotas_updated_at();

-- RLS for user_quotas
ALTER TABLE user_quotas ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own quotas" ON user_quotas
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Service role full access to user_quotas" ON user_quotas
    FOR ALL TO service_role
    USING (true) WITH CHECK (true);

-- ============================================================================
-- SKILL QUOTAS (Per-skill monthly usage tracking)
-- ============================================================================
CREATE TABLE IF NOT EXISTS skill_quotas (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    skill VARCHAR(20) NOT NULL,  -- READING, LISTENING, WRITING, SPEAKING
    quota_month DATE NOT NULL,  -- First day of month (e.g., 2025-12-01)
    attempt_count INTEGER NOT NULL DEFAULT 0,
    attempt_ai_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, skill, quota_month)
);

CREATE INDEX IF NOT EXISTS idx_skill_quotas_user_id ON skill_quotas(user_id);
CREATE INDEX IF NOT EXISTS idx_skill_quotas_user_skill ON skill_quotas(user_id, skill);
CREATE INDEX IF NOT EXISTS idx_skill_quotas_month ON skill_quotas(user_id, quota_month);
CREATE INDEX IF NOT EXISTS idx_skill_quotas_full ON skill_quotas(user_id, skill, quota_month);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_skill_quotas_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_skill_quotas_updated_at ON skill_quotas;
CREATE TRIGGER trigger_skill_quotas_updated_at
    BEFORE UPDATE ON skill_quotas
    FOR EACH ROW
    EXECUTE FUNCTION update_skill_quotas_updated_at();

-- RLS for skill_quotas
ALTER TABLE skill_quotas ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own skill quotas" ON skill_quotas
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Service role full access to skill_quotas" ON skill_quotas
    FOR ALL TO service_role
    USING (true) WITH CHECK (true);

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE user_quotas IS 'Global monthly quota tracking for Cramerie (free tier) users';
COMMENT ON TABLE skill_quotas IS 'Per-skill monthly quota tracking (prevents grinding one skill)';
COMMENT ON COLUMN user_quotas.quota_month IS 'First day of the month for quota period (e.g., 2025-12-01)';
COMMENT ON COLUMN skill_quotas.skill IS 'Skill type: READING, LISTENING, WRITING, SPEAKING';
