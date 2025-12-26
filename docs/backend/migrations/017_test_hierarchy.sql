-- Migration 017: Test Hierarchy (TestSets, IeltsTests, Hashtags)
-- This migration creates the normalized test hierarchy structure

-- =====================================================
-- 1. Create test_sets table
-- =====================================================
CREATE TABLE IF NOT EXISTS public.test_sets (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    cover_image_url VARCHAR(2048),
    publisher VARCHAR(255),
    year INTEGER,
    is_published BOOLEAN NOT NULL DEFAULT false,
    is_system BOOLEAN NOT NULL DEFAULT false,
    display_order INTEGER DEFAULT 0,
    created_by UUID REFERENCES auth.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

-- Index for ordering and lookups
CREATE INDEX IF NOT EXISTS idx_test_sets_display_order ON public.test_sets(display_order);
CREATE INDEX IF NOT EXISTS idx_test_sets_is_published ON public.test_sets(is_published);

-- =====================================================
-- 2. Create tests table (IeltsTest entities)
-- =====================================================
CREATE TABLE IF NOT EXISTS public.tests (
    id BIGSERIAL PRIMARY KEY,
    set_id BIGINT NOT NULL REFERENCES public.test_sets(id) ON DELETE CASCADE,
    test_number INTEGER NOT NULL,
    name_vi VARCHAR(255),
    name_en VARCHAR(255),
    description TEXT,
    difficulty VARCHAR(30) DEFAULT 'INTERMEDIATE',
    estimated_time_minutes INTEGER DEFAULT 170,
    is_published BOOLEAN DEFAULT false,
    is_ai_generated BOOLEAN DEFAULT false,
    generation_metadata JSONB,
    created_by UUID REFERENCES auth.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT unique_test_in_set UNIQUE (set_id, test_number)
);

-- Indexes for tests
CREATE INDEX IF NOT EXISTS idx_tests_set_id ON public.tests(set_id);
CREATE INDEX IF NOT EXISTS idx_tests_is_published ON public.tests(is_published);

-- =====================================================
-- 3. Create hashtags table
-- =====================================================
CREATE TABLE IF NOT EXISTS public.hashtags (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_vi VARCHAR(100) NOT NULL,
    name_en VARCHAR(100),
    category VARCHAR(50) NOT NULL DEFAULT 'topic',
    icon VARCHAR(10),
    color VARCHAR(20),
    use_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for hashtags
CREATE INDEX IF NOT EXISTS idx_hashtags_category ON public.hashtags(category);
CREATE INDEX IF NOT EXISTS idx_hashtags_is_active ON public.hashtags(is_active);
CREATE INDEX IF NOT EXISTS idx_hashtags_use_count ON public.hashtags(use_count DESC);

-- =====================================================
-- 4. Create test_hashtags junction table
-- =====================================================
CREATE TABLE IF NOT EXISTS public.test_hashtags (
    test_id BIGINT NOT NULL REFERENCES public.tests(id) ON DELETE CASCADE,
    hashtag_id BIGINT NOT NULL REFERENCES public.hashtags(id) ON DELETE CASCADE,
    PRIMARY KEY (test_id, hashtag_id)
);

-- Indexes for junction table
CREATE INDEX IF NOT EXISTS idx_test_hashtags_hashtag ON public.test_hashtags(hashtag_id);

-- =====================================================
-- 5. Add test_id column to sections table (optional foreign key)
-- =====================================================
-- Note: This creates a soft reference - sections still work with exam_source/test_number
-- but can optionally be linked directly to a test entity
ALTER TABLE public.sections 
ADD COLUMN IF NOT EXISTS test_id BIGINT REFERENCES public.tests(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_sections_test_id ON public.sections(test_id);

-- =====================================================
-- 6. Insert initial test sets for existing Cambridge data
-- =====================================================
INSERT INTO public.test_sets (code, name, publisher, year, is_published, is_system, display_order)
VALUES 
    ('cam17', 'Cambridge IELTS 17', 'Cambridge University Press', 2022, true, true, 1),
    ('cam18', 'Cambridge IELTS 18', 'Cambridge University Press', 2023, true, true, 2)
ON CONFLICT (code) DO NOTHING;

-- =====================================================
-- 7. Insert tests for Cambridge 17 (based on existing sections)
-- =====================================================
INSERT INTO public.tests (set_id, test_number, name_vi, name_en, is_published, created_at)
SELECT 
    ts.id,
    test_num,
    'Bài thi ' || test_num,
    'Test ' || test_num,
    true,
    NOW()
FROM public.test_sets ts
CROSS JOIN generate_series(1, 4) AS test_num
WHERE ts.code = 'cam17'
AND NOT EXISTS (
    SELECT 1 FROM public.tests t 
    WHERE t.set_id = ts.id AND t.test_number = test_num
);

-- =====================================================
-- 8. Insert some initial hashtags
-- =====================================================
INSERT INTO public.hashtags (code, name_vi, name_en, category, icon, color)
VALUES
    ('environment', 'Môi trường', 'Environment', 'topic', NULL, '#22c55e'),
    ('technology', 'Công nghệ', 'Technology', 'topic', NULL, '#3b82f6'),
    ('education', 'Giáo dục', 'Education', 'topic', NULL, '#8b5cf6'),
    ('health', 'Sức khỏe', 'Health', 'topic', NULL, '#ef4444'),
    ('society', 'Xã hội', 'Society', 'topic', NULL, '#f97316'),
    ('business', 'Kinh doanh', 'Business', 'topic', NULL, '#eab308'),
    ('science', 'Khoa học', 'Science', 'topic', NULL, '#06b6d4'),
    ('history', 'Lịch sử', 'History', 'topic', NULL, '#a855f7'),
    ('culture', 'Văn hóa', 'Culture', 'topic', NULL, '#ec4899'),
    ('travel', 'Du lịch', 'Travel', 'topic', NULL, '#14b8a6')
ON CONFLICT (code) DO NOTHING;

-- =====================================================
-- 9. Enable RLS for new tables
-- =====================================================
ALTER TABLE public.test_sets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.hashtags ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.test_hashtags ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- 10. RLS Policies - Public read access, admin write access
-- =====================================================

-- Test sets: Anyone can read published, service_role can do everything
CREATE POLICY "test_sets_public_read" ON public.test_sets
    FOR SELECT USING (is_published = true);

CREATE POLICY "test_sets_service_all" ON public.test_sets
    FOR ALL TO service_role USING (true);

-- Tests: Anyone can read published, service_role can do everything
CREATE POLICY "tests_public_read" ON public.tests
    FOR SELECT USING (is_published = true);

CREATE POLICY "tests_service_all" ON public.tests
    FOR ALL TO service_role USING (true);

-- Hashtags: Anyone can read active, service_role can do everything
CREATE POLICY "hashtags_public_read" ON public.hashtags
    FOR SELECT USING (is_active = true);

CREATE POLICY "hashtags_service_all" ON public.hashtags
    FOR ALL TO service_role USING (true);

-- Test hashtags: Anyone can read, service_role can do everything
CREATE POLICY "test_hashtags_public_read" ON public.test_hashtags
    FOR SELECT USING (true);

CREATE POLICY "test_hashtags_service_all" ON public.test_hashtags
    FOR ALL TO service_role USING (true);

-- =====================================================
-- 11. Grant permissions
-- =====================================================
GRANT SELECT ON public.test_sets TO anon, authenticated;
GRANT SELECT ON public.tests TO anon, authenticated;
GRANT SELECT ON public.hashtags TO anon, authenticated;
GRANT SELECT ON public.test_hashtags TO anon, authenticated;

GRANT ALL ON public.test_sets TO service_role;
GRANT ALL ON public.tests TO service_role;
GRANT ALL ON public.hashtags TO service_role;
GRANT ALL ON public.test_hashtags TO service_role;

-- Grant sequence permissions
GRANT USAGE, SELECT ON SEQUENCE public.test_sets_id_seq TO service_role;
GRANT USAGE, SELECT ON SEQUENCE public.tests_id_seq TO service_role;
GRANT USAGE, SELECT ON SEQUENCE public.hashtags_id_seq TO service_role;
