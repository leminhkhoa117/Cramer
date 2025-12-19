-- Sample Data for Testing - READY TO RUN
-- Using actual user IDs from profiles table
-- User 1: leminhkhoa597 (2d843ce6-99ed-4aff-baad-14db805f205c) - Admin
-- User 2: quochuu54 (7a2ea1a9-c043-4c64-aff2-31f789147f99)

-- ============================================================================
-- USER ACTIVITIES (Activity Timeline)
-- ============================================================================

-- Sample 1: User completed a reading test with good score
INSERT INTO user_activities (user_id, activity_type, title, description, metadata)
VALUES (
    '2d843ce6-99ed-4aff-baad-14db805f205c', -- leminhkhoa597
    'TEST_COMPLETED',
    'Hoàn thành bài kiểm tra Reading',
    'Đạt 8.0/9.0 trong bài test Reading Practice #15',
    jsonb_build_object(
        'test_id', '12345',
        'test_name', 'Reading Practice #15',
        'score', 8.0,
        'max_score', 9.0,
        'skill', 'reading',
        'duration_minutes', 45,
        'questions_total', 40,
        'questions_correct', 35
    )
);

-- Sample 2: User saved vocabulary words
INSERT INTO user_activities (user_id, activity_type, title, description, metadata)
VALUES (
    '2d843ce6-99ed-4aff-baad-14db805f205c', -- leminhkhoa597
    'VOCAB_SAVED',
    'Đã lưu 15 từ vựng mới',
    'Lưu từ vựng từ bài học "Academic Words - Unit 7"',
    jsonb_build_object(
        'vocab_count', 15,
        'lesson_id', 'vocab_unit_7',
        'lesson_name', 'Academic Words - Unit 7',
        'category', 'academic',
        'words', jsonb_build_array('hypothesis', 'analyze', 'concept', 'evidence', 'theory')
    )
);

-- Sample 3: User upgraded subscription
INSERT INTO user_activities (user_id, activity_type, title, description, metadata)
VALUES (
    '7a2ea1a9-c043-4c64-aff2-31f789147f99', -- quochuu54
    'SUBSCRIPTION_CHANGED',
    'Nâng cấp lên gói Premium',
    'Đã nâng cấp từ gói Free lên gói Premium (12 tháng)',
    jsonb_build_object(
        'plan_from', 'free',
        'plan_to', 'premium',
        'duration_months', 12,
        'amount_paid', 990000,
        'currency', 'VND',
        'payment_method', 'momo'
    )
);

-- ============================================================================
-- ADMIN AUDIT LOG (Admin Actions)
-- ============================================================================

-- Sample 1: Admin added credits to user
INSERT INTO admin_audit_log (
    admin_user_id, 
    admin_email, 
    action, 
    target_type, 
    target_id, 
    old_value, 
    new_value, 
    description, 
    ip_address, 
    user_agent
)
VALUES (
    '2d843ce6-99ed-4aff-baad-14db805f205c', -- leminhkhoa597 (Admin)
    'leminhkhoa597@gmail.com',
    'CREDITS_ADD',
    'CREDITS',
    '7a2ea1a9-c043-4c64-aff2-31f789147f99', -- Target: quochuu54
    jsonb_build_object('credits', 100),
    jsonb_build_object('credits', 150),
    'Thêm 50 credits cho người dùng do hỗ trợ khách hàng',
    '192.168.1.100',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
);

-- Sample 2: Admin changed user subscription
INSERT INTO admin_audit_log (
    admin_user_id, 
    admin_email, 
    action, 
    target_type, 
    target_id, 
    old_value, 
    new_value, 
    description, 
    ip_address, 
    user_agent
)
VALUES (
    '2d843ce6-99ed-4aff-baad-14db805f205c', -- leminhkhoa597 (Admin)
    'leminhkhoa597@gmail.com',
    'SUBSCRIPTION_CHANGE',
    'SUBSCRIPTION',
    'sub_12345',
    jsonb_build_object(
        'plan', 'basic',
        'status', 'active',
        'expires_at', '2025-12-31'
    ),
    jsonb_build_object(
        'plan', 'premium',
        'status', 'active',
        'expires_at', '2026-06-30'
    ),
    'Nâng cấp subscription cho VIP customer theo yêu cầu',
    '192.168.1.101',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)'
);

-- Sample 3: Admin updated user profile
INSERT INTO admin_audit_log (
    admin_user_id, 
    admin_email, 
    action, 
    target_type, 
    target_id, 
    old_value, 
    new_value, 
    description, 
    ip_address, 
    user_agent
)
VALUES (
    '2d843ce6-99ed-4aff-baad-14db805f205c', -- leminhkhoa597 (Admin)
    'leminhkhoa597@gmail.com',
    'PROFILE_UPDATE',
    'USER',
    '7a2ea1a9-c043-4c64-aff2-31f789147f99', -- Target: quochuu54
    jsonb_build_object(
        'target_score', 7.0,
        'study_hours_per_week', 10
    ),
    jsonb_build_object(
        'target_score', 8.5,
        'study_hours_per_week', 15
    ),
    'Cập nhật target score theo yêu cầu của học viên',
    '192.168.1.100',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0'
);

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================
-- Run these after inserting to verify the data:
--
-- SELECT * FROM user_activities ORDER BY created_at DESC LIMIT 10;
-- SELECT * FROM admin_audit_log ORDER BY created_at DESC LIMIT 10;
--
-- Count records:
-- SELECT COUNT(*) as total_activities FROM user_activities;
-- SELECT COUNT(*) as total_audit_logs FROM admin_audit_log;
