-- =====================================================
-- Cambridge IELTS 17, Test 1 - WRITING DATA
-- Created: December 5, 2025
-- Description: Writing Task 1 and Task 2 for Cambridge 17 Test 1
-- =====================================================

DO $$
DECLARE
    section_task1_id bigint;
    section_task2_id bigint;
BEGIN
    -- First, delete existing Writing data for Cam17, Test 1
    DELETE FROM public.sections WHERE exam_source = 'cam17' AND test_number = '1' AND skill = 'writing';

    -- =========================================================
    -- WRITING TASK 1 - Map (Norbiton Industrial Area)
    -- =========================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text, display_content_url)
    VALUES (
        'cam17',
        '1',
        'writing',
        1,
        '<p>The two maps below show an industrial area in the town of Norbiton, and planned changes to it.</p>

<p>Summarise the information by selecting and reporting the main features, and make comparisons where relevant.</p>

<p><em>Write at least 150 words.</em></p>',
        'https://ieltsunlocked.com/wp-content/uploads/2023/08/cam17t1w1.jpg'
    )
    RETURNING id INTO section_task1_id;

    -- =========================================================
    -- WRITING TASK 2 - Essay (Risk-taking for young people)
    -- =========================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text)
    VALUES (
        'cam17',
        '1',
        'writing',
        2,
        '<p>Some people believe that it is a good idea for all young people to do unpaid voluntary work in their free time to help the local community.</p>

<p><strong>Do the advantages of this outweigh the disadvantages?</strong></p>

<p>Give reasons for your answer and include any relevant examples from your own knowledge or experience.</p>

<p><em>Write at least 250 words.</em></p>'
    )
    RETURNING id INTO section_task2_id;

    RAISE NOTICE 'Created Writing Task 1 with section_id: %', section_task1_id;
    RAISE NOTICE 'Created Writing Task 2 with section_id: %', section_task2_id;
END $$;

-- Verify the insertion
SELECT id, exam_source, test_number, skill, part_number, 
       LEFT(passage_text, 100) as passage_preview,
       display_content_url
FROM public.sections 
WHERE exam_source = 'cam17' AND test_number = '1' AND skill = 'writing'
ORDER BY part_number;
