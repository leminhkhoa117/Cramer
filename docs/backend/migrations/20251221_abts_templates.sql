-- =============================================================================
-- ABTS Topic Templates Table Migration
-- Creates a database table for storing topic templates used in AI generation
-- 
-- @since 2025-12-21 - ABTS Database Templates
-- =============================================================================

-- Create templates table
CREATE TABLE IF NOT EXISTS public.abts_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Category information
    category VARCHAR(50) NOT NULL,  -- e.g., 'environment', 'technology', 'health'
    category_label VARCHAR(100) NOT NULL,  -- Display name: 'Environment & Nature'
    category_icon VARCHAR(10),  -- Emoji icon: '🌍'
    
    -- Template content
    topic VARCHAR(500) NOT NULL,
    description TEXT,
    hashtags TEXT[] DEFAULT '{}',  -- Array of hashtags
    facts TEXT[] DEFAULT '{}',  -- Array of facts for CUSTOM_FACTS mode
    
    -- Template metadata
    skill VARCHAR(20) NOT NULL DEFAULT 'reading',  -- 'reading', 'listening', 'writing'
    difficulty VARCHAR(20) DEFAULT 'INTERMEDIATE',  -- 'EASY', 'INTERMEDIATE', 'ADVANCED'
    test_type VARCHAR(20) DEFAULT 'ACADEMIC',  -- 'ACADEMIC', 'GENERAL'
    
    -- Suggested question types (comma-separated)
    suggested_question_types TEXT,
    
    -- Template stats
    use_count INTEGER DEFAULT 0,
    last_used_at TIMESTAMP WITH TIME ZONE,
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    is_featured BOOLEAN DEFAULT false,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_abts_templates_category ON public.abts_templates(category);
CREATE INDEX IF NOT EXISTS idx_abts_templates_skill ON public.abts_templates(skill);
CREATE INDEX IF NOT EXISTS idx_abts_templates_active ON public.abts_templates(is_active);
CREATE INDEX IF NOT EXISTS idx_abts_templates_featured ON public.abts_templates(is_featured);

-- Create trigger to update updated_at
CREATE OR REPLACE FUNCTION update_abts_templates_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_abts_templates_updated_at ON public.abts_templates;
CREATE TRIGGER trigger_abts_templates_updated_at
    BEFORE UPDATE ON public.abts_templates
    FOR EACH ROW
    EXECUTE FUNCTION update_abts_templates_updated_at();

-- =============================================================================
-- SEED DATA: Initial topic templates
-- =============================================================================

-- Environment & Nature
INSERT INTO public.abts_templates (category, category_label, category_icon, topic, description, skill, hashtags, facts, is_featured) VALUES
('environment', 'Môi trường & Thiên nhiên', '🌍', 
 'Climate Change and Its Global Impact',
 'The effects of climate change on ecosystems, weather patterns, and human societies',
 'reading',
 ARRAY['climate_change', 'environment', 'global_warming'],
 ARRAY[
     'Global temperatures have risen by approximately 1.1°C since pre-industrial times',
     'Arctic sea ice is declining at a rate of 13% per decade',
     'Extreme weather events have increased by 46% since 2000',
     'Ocean acidification has increased by 30% since the industrial revolution',
     'Approximately 1 million species are at risk of extinction due to climate change'
 ],
 true),

('environment', 'Môi trường & Thiên nhiên', '🌍',
 'Sustainable Urban Development',
 'How cities are adapting to become more environmentally sustainable',
 'reading',
 ARRAY['sustainability', 'urban_planning', 'green_cities'],
 ARRAY[
     'Green buildings can reduce energy consumption by up to 50%',
     'Urban trees can lower city temperatures by 2-8°C',
     'Bike-sharing programs have reduced car usage by 15% in major cities',
     'Vertical gardens can improve air quality by filtering 70% of pollutants',
     'Smart traffic systems reduce congestion and emissions by 25%'
 ],
 false);

-- Technology
INSERT INTO public.abts_templates (category, category_label, category_icon, topic, description, skill, hashtags, facts, is_featured) VALUES
('technology', 'Công nghệ', '💻',
 'Artificial Intelligence in Healthcare',
 'How AI is transforming medical diagnosis and treatment',
 'reading',
 ARRAY['AI', 'healthcare', 'technology', 'medicine'],
 ARRAY[
     'AI can detect certain cancers with 94% accuracy, surpassing human doctors',
     'Machine learning algorithms can predict heart attacks 5 hours in advance',
     'AI-powered robots assist in over 1 million surgeries annually',
     'Natural language processing helps analyze medical records 60 times faster',
     'Telemedicine with AI support has increased access to healthcare by 40%'
 ],
 true),

('technology', 'Công nghệ', '💻',
 'The Future of Transportation',
 'Emerging technologies in transportation including autonomous vehicles',
 'reading',
 ARRAY['transportation', 'technology', 'autonomous_vehicles'],
 ARRAY[
     'Autonomous vehicles could reduce traffic accidents by 90%',
     'Electric vehicles are expected to reach price parity with gas cars by 2025',
     'Hyperloop technology could enable travel at speeds over 1000 km/h',
     'Flying taxis are projected to be commercially available by 2030',
     'Smart traffic management can reduce commute times by 20%'
 ],
 false);

-- Education
INSERT INTO public.abts_templates (category, category_label, category_icon, topic, description, skill, hashtags, facts, is_featured) VALUES
('education', 'Giáo dục', '📚',
 'Digital Learning and Online Education',
 'The transformation of education through technology and virtual classrooms',
 'reading',
 ARRAY['education', 'e_learning', 'technology'],
 ARRAY[
     'Online learning has grown by 900% since 2000',
     'Students retain 25-60% more information in online courses',
     'Virtual reality can improve learning outcomes by 30%',
     'MOOCs have enrolled over 180 million students worldwide',
     'Personalized learning algorithms can improve test scores by 40%'
 ],
 true);

-- Health & Wellness
INSERT INTO public.abts_templates (category, category_label, category_icon, topic, description, skill, hashtags, facts, is_featured) VALUES
('health', 'Sức khỏe & Đời sống', '🏥',
 'Mental Health in Modern Society',
 'The growing awareness and treatment of mental health issues',
 'reading',
 ARRAY['mental_health', 'wellness', 'psychology'],
 ARRAY[
     'One in four people will experience a mental health problem each year',
     'Meditation has been shown to reduce anxiety by 60%',
     'Regular exercise can be as effective as antidepressants for mild depression',
     'Workplace mental health programs reduce absenteeism by 30%',
     'Therapy apps have made mental health support accessible to 50 million users'
 ],
 true);

-- Culture & Society
INSERT INTO public.abts_templates (category, category_label, category_icon, topic, description, skill, hashtags, facts, is_featured) VALUES
('culture', 'Văn hóa & Xã hội', '🎭',
 'Globalization and Cultural Identity',
 'How globalization affects local cultures and traditions',
 'reading',
 ARRAY['globalization', 'culture', 'society'],
 ARRAY[
     'Over 7000 languages exist today, but 40% are endangered',
     'Cultural tourism generates $1.5 trillion annually',
     'Social media connects 4.5 billion people across cultures',
     'Traditional crafts provide livelihoods for 200 million artisans',
     'UNESCO protects 1154 World Heritage Sites preserving cultural identity'
 ],
 false);

-- Economics
INSERT INTO public.abts_templates (category, category_label, category_icon, topic, description, skill, hashtags, facts, is_featured) VALUES
('economics', 'Kinh tế', '📈',
 'The Gig Economy and Future of Work',
 'How freelancing and gig work are reshaping employment',
 'reading',
 ARRAY['economy', 'work', 'freelancing'],
 ARRAY[
     'Gig workers now make up 36% of the global workforce',
     'Remote work has increased productivity by 13% on average',
     'Freelancing platforms have grown by 78% since 2019',
     'The gig economy is expected to reach $455 billion by 2025',
     'Flexible work arrangements improve work-life balance for 85% of employees'
 ],
 false);

-- Enable RLS (Row Level Security)
ALTER TABLE public.abts_templates ENABLE ROW LEVEL SECURITY;

-- Create policy for public read access (templates are public)
CREATE POLICY "Templates are viewable by everyone"
    ON public.abts_templates
    FOR SELECT
    USING (is_active = true);

-- Create policy for admin write access (requires admin role)
CREATE POLICY "Templates are insertable by admins"
    ON public.abts_templates
    FOR INSERT
    WITH CHECK (true);  -- Adjust based on your admin check

CREATE POLICY "Templates are updatable by admins"
    ON public.abts_templates
    FOR UPDATE
    USING (true);

-- Grant permissions
GRANT SELECT ON public.abts_templates TO authenticated;
GRANT SELECT ON public.abts_templates TO anon;

-- =============================================================================
-- END OF MIGRATION
-- =============================================================================
