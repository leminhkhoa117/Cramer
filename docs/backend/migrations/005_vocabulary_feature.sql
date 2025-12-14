-- Migration: 005_vocabulary_feature.sql
-- Description: Create vocabulary table for the Sổ tay Từ vựng (Vocabulary Notebook) feature
-- Author: executionAgent
-- Date: 2025-12-13

-- Create vocabulary table
CREATE TABLE IF NOT EXISTS vocabulary (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    word VARCHAR(200) NOT NULL,
    translation TEXT,
    phonetic VARCHAR(100),
    part_of_speech VARCHAR(50),
    definition TEXT,
    example_sentence TEXT,
    source_context TEXT,
    source_test_id BIGINT,
    source_section_id BIGINT,
    notes TEXT,
    is_mastered BOOLEAN DEFAULT FALSE,
    review_count INTEGER DEFAULT 0,
    last_reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_vocabulary_user_id ON vocabulary(user_id);
CREATE INDEX IF NOT EXISTS idx_vocabulary_user_id_created_at ON vocabulary(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vocabulary_user_id_is_mastered ON vocabulary(user_id, is_mastered);
CREATE INDEX IF NOT EXISTS idx_vocabulary_user_id_word ON vocabulary(user_id, LOWER(word));

-- Enable Row Level Security
ALTER TABLE vocabulary ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see their own vocabulary
CREATE POLICY "Users can view own vocabulary" ON vocabulary
    FOR SELECT
    USING (auth.uid() = user_id);

-- Policy: Users can insert their own vocabulary
CREATE POLICY "Users can insert own vocabulary" ON vocabulary
    FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Policy: Users can update their own vocabulary
CREATE POLICY "Users can update own vocabulary" ON vocabulary
    FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Policy: Users can delete their own vocabulary
CREATE POLICY "Users can delete own vocabulary" ON vocabulary
    FOR DELETE
    USING (auth.uid() = user_id);

-- Policy: Service role has full access (for backend operations)
CREATE POLICY "Service role has full access to vocabulary" ON vocabulary
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_vocabulary_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_vocabulary_updated_at
    BEFORE UPDATE ON vocabulary
    FOR EACH ROW
    EXECUTE FUNCTION update_vocabulary_updated_at();

-- Add comments for documentation
COMMENT ON TABLE vocabulary IS 'User vocabulary notebook for learning words from IELTS tests';
COMMENT ON COLUMN vocabulary.user_id IS 'UUID of the user who owns this vocabulary entry';
COMMENT ON COLUMN vocabulary.word IS 'The English word being learned';
COMMENT ON COLUMN vocabulary.translation IS 'Vietnamese translation of the word';
COMMENT ON COLUMN vocabulary.phonetic IS 'IPA phonetic notation';
COMMENT ON COLUMN vocabulary.part_of_speech IS 'Part of speech (noun, verb, etc.)';
COMMENT ON COLUMN vocabulary.definition IS 'English definition of the word';
COMMENT ON COLUMN vocabulary.example_sentence IS 'Example sentence demonstrating word usage';
COMMENT ON COLUMN vocabulary.source_context IS 'Original context where the word was found';
COMMENT ON COLUMN vocabulary.source_test_id IS 'ID of the test where the word was found';
COMMENT ON COLUMN vocabulary.source_section_id IS 'ID of the section where the word was found';
COMMENT ON COLUMN vocabulary.notes IS 'User notes about the word';
COMMENT ON COLUMN vocabulary.is_mastered IS 'Whether the user has mastered this word';
COMMENT ON COLUMN vocabulary.review_count IS 'Number of times the word has been reviewed';
COMMENT ON COLUMN vocabulary.last_reviewed_at IS 'Timestamp of the last review';
