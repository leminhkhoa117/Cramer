-- =====================================================
-- Cambridge IELTS 17, Test 2 - WRITING DATA
-- Created: December 7, 2025
-- Description: Writing Task 1 and Task 2 for Cambridge 17 Test 2
-- =====================================================

DO $$
DECLARE
    section_task1_id bigint;
    section_task2_id bigint;
BEGIN
    -- First, delete existing Writing data for Cam17, Test 2
    DELETE FROM public.sections WHERE exam_source = 'cam17' AND test_number = '2' AND skill = 'writing';

    -- =========================================================
    -- WRITING TASK 1 - Table & Pie Charts (Police Budget 2017-2018)
    -- =========================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text, display_content_url)
    VALUES (
        'cam17',
        '2',
        'writing',
        1,
        '<p>The table and charts below give information on the police budget for 2017 and 2018 in one area of Britain. The table shows where the money came from and the charts show how it was distributed.</p>

<p>Summarise the information by selecting and reporting the main features, and make comparisons where relevant.</p>

<p><em>Write at least 150 words.</em></p>',
        'docs/test_materials/IELTS CAM 17_WRIITING_IMG/IELTS17_T2_W1.png'
    )
    RETURNING id INTO section_task1_id;

    -- =========================================================
    -- WRITING TASK 2 - Essay (Smartphones & Children)
    -- =========================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text)
    VALUES (
        'cam17',
        '2',
        'writing',
        2,
        '<p>Some children spend hours every day on their smartphones.</p>

<p><strong>Why is this the case? Do you think this is a positive or a negative development?</strong></p>

<p>Give reasons for your answer and include any relevant examples from your own knowledge or experience.</p>

<p><em>Write at least 250 words.</em></p>'
    )
    RETURNING id INTO section_task2_id;

    RAISE NOTICE 'Created Writing Task 1 with section_id: %', section_task1_id;
    RAISE NOTICE 'Created Writing Task 2 with section_id: %', section_task2_id;
END $$;

-- Verify the insertion
SELECT id, exam_source, test_number, skill, part_number, 
       LEFT(passage_text, 120) as passage_preview,
       display_content_url
FROM public.sections 
WHERE exam_source = 'cam17' AND test_number = '2' AND skill = 'writing'
ORDER BY part_number;
