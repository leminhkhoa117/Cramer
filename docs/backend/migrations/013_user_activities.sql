-- Migration: 013_user_activities.sql
-- Description: Create user activities and admin audit log tables
-- Author: Cramer Team
-- Date: 2025-12-18

-- ============================================================================
-- USER ACTIVITIES (Activity timeline)
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_activities (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_user_activities_user_id ON user_activities(user_id);
CREATE INDEX IF NOT EXISTS idx_user_activities_created_at ON user_activities(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_activities_type ON user_activities(activity_type);

-- RLS
ALTER TABLE user_activities ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own activities" ON user_activities;
CREATE POLICY "Users can view own activities" ON user_activities
    FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Service role full access to activities" ON user_activities;
CREATE POLICY "Service role full access to activities" ON user_activities
    FOR ALL TO service_role
    USING (true) WITH CHECK (true);

-- ============================================================================
-- ADMIN AUDIT LOG
-- ============================================================================
CREATE TABLE IF NOT EXISTS admin_audit_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    admin_user_id UUID NOT NULL,
    admin_email VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    description TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_admin_audit_log_admin_id ON admin_audit_log(admin_user_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_log_target ON admin_audit_log(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_log_created_at ON admin_audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_log_action ON admin_audit_log(action);

-- RLS (Only service role can access)
ALTER TABLE admin_audit_log ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Service role full access to audit log" ON admin_audit_log;
CREATE POLICY "Service role full access to audit log" ON admin_audit_log
    FOR ALL TO service_role
    USING (true) WITH CHECK (true);

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE user_activities IS 'User activity timeline for admin dashboard';
COMMENT ON COLUMN user_activities.activity_type IS 'Type: TEST_COMPLETED, VOCAB_SAVED, SUBSCRIPTION_CHANGED, LOGIN, ACHIEVEMENT_EARNED, PROFILE_UPDATED, CREDITS_CHANGED';
COMMENT ON COLUMN user_activities.metadata IS 'JSON data like {test_id, score, skill} or {vocab_count}';

COMMENT ON TABLE admin_audit_log IS 'Audit trail for all admin actions';
COMMENT ON COLUMN admin_audit_log.action IS 'Action: STATUS_CHANGE, CREDITS_ADD, CREDITS_SUBTRACT, SUBSCRIPTION_CHANGE, PROFILE_UPDATE, BAN, UNBAN';
COMMENT ON COLUMN admin_audit_log.target_type IS 'Target: USER, SUBSCRIPTION, CREDITS, CONTENT';
