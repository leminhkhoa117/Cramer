-- SQL Ingestion Script for IELTS Listening Test
-- Source: Cambridge 18, Test 1, Listening
--
-- IMPORTANT: You must upload the audio file for this test to a storage provider
-- (like Supabase Storage) and replace the placeholder 'YOUR_AUDIO_URL_HERE'
-- with the actual public URL to the audio file.

DO $$
DECLARE
    section_1_id bigint;
    section_2_id bigint;
    section_3_id bigint;
    section_4_id bigint;
BEGIN
    -- To prevent duplicate data, delete existing data for this specific test first.
    DELETE FROM public.questions WHERE section_id IN (SELECT id FROM public.sections WHERE exam_source = 'cam18' AND test_number = '1' AND skill = 'listening');
    DELETE FROM public.sections WHERE exam_source = 'cam18' AND test_number = '1' AND skill = 'listening';

    -- PART 1
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text, audio_url)
    VALUES (
        'cam18',
        '1',
        'listening',
        1,
        '-- (Optional) Transcript for Part 1 can be placed here --',
        'https://jpocdgkrvohmjkejclpl.supabase.co/storage/v1/object/public/audio_files/Test%20ielts%20listening/Verse1_%20(1).mp3'
    )
    RETURNING id INTO section_1_id;

    -- Questions for Part 1 (Example: Fill in the blank)
    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit)
    VALUES
        (section_1_id, 1, 'cam18-t1-l-q1', 'FILL_IN_BLANK', '{"text": "Enquiry about joining the <strong>1</strong> ____"}', '["leisure club"]', 'NO MORE THAN TWO WORDS'),
        (section_1_id, 2, 'cam18-t1-l-q2', 'FILL_IN_BLANK', '{"text": "Name: Jack <strong>2</strong> ____"}', '["Cactus"]', 'NO MORE THAN TWO WORDS'),
        (section_1_id, 3, 'cam18-t1-l-q3', 'FILL_IN_BLANK', '{"text": "Membership type: <strong>3</strong> ____"}', '["Gold"]', 'NO MORE THAN TWO WORDS'),
        (section_1_id, 4, 'cam18-t1-l-q4', 'FILL_IN_BLANK', '{"text": "Contact number: <strong>4</strong> ____"}', '["0412 786 552"]', 'NO MORE THAN TWO WORDS AND/OR A NUMBER'),
        (section_1_id, 5, 'cam18-t1-l-q5', 'FILL_IN_BLANK', '{"text": "Email: jack.cactus@example.com<br/>Date of birth: 12th <strong>5</strong> ____"}', '["August"]', 'NO MORE THAN TWO WORDS'),
        (section_1_id, 6, 'cam18-t1-l-q6', 'FILL_IN_BLANK', '{"text": "Address: 24, <strong>6</strong> ____ Road, Northbridge"}', '["Manly"]', 'NO MORE THAN TWO WORDS'),
        (section_1_id, 7, 'cam18-t1-l-q7', 'FILL_IN_BLANK', '{"text": "Heard about us from a <strong>7</strong> ____"}', '["friend"]', 'NO MORE THAN TWO WORDS'),
        (section_1_id, 8, 'cam18-t1-l-q8', 'FILL_IN_BLANK', '{"text": "Medical conditions: suffers from <strong>8</strong> ____"}', '["allergies"]', 'NO MORE THAN TWO WORDS'),
        (section_1_id, 9, 'cam18-t1-l-q9', 'FILL_IN_BLANK', '{"text": "Interests: swimming and <strong>9</strong> ____"}', '["yoga"]', 'NO MORE THAN TWO WORDS'),
        (section_1_id, 10, 'cam18-t1-l-q10', 'FILL_IN_BLANK', '{"text": "Needs a card for the <strong>10</strong> ____"}', '["car park"]', 'NO MORE THAN TWO WORDS');

    -- PART 2
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text, audio_url)
    VALUES (
        'cam18',
        '1',
        'listening',
        2,
        '-- (Optional) Transcript for Part 2 can be placed here --',
        'https://jpocdgkrvohmjkejclpl.supabase.co/storage/v1/object/public/audio_files/Test%20ielts%20listening/Verse1_%20(1).mp3'
    )
    RETURNING id INTO section_2_id;

    -- Questions for Part 2 (Example: Multiple Choice)
    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer)
    VALUES
        (section_2_id, 11, 'cam18-t1-l-q11', 'MULTIPLE_CHOICE', '{"text": "The new climbing wall is popular with...", "options": ["A children", "B adults", "C both children and adults"]}', '["B"]'),
        (section_2_id, 12, 'cam18-t1-l-q12', 'MULTIPLE_CHOICE', '{"text": "The cafe is located near the...", "options": ["A main entrance", "B swimming pool", "C tennis courts"]}', '["A"]'),
        (section_2_id, 13, 'cam18-t1-l-q13', 'MULTIPLE_CHOICE', '{"text": "The new yoga classes will be held on...", "options": ["A Monday mornings", "B Wednesday evenings", "C Friday afternoons"]}', '["B"]'),
        (section_2_id, 14, 'cam18-t1-l-q14', 'MULTIPLE_CHOICE', '{"text": "Members should book classes...", "options": ["A by phone", "B in person", "C online"]}', '["C"]'),
        (section_2_id, 15, 'cam18-t1-l-q15', 'MULTIPLE_CHOICE', '{"text": "The speaker recommends the new sauna for...", "options": ["A post-workout recovery", "B relaxation", "C improving circulation"]}', '["A"]'),
        (section_2_id, 16, 'cam18-t1-l-q16', 'MULTIPLE_CHOICE', '{"text": "The family-friendly swim session is on...", "options": ["A Saturday mornings", "B Sunday afternoons", "C both Saturday and Sunday"]}', '["B"]'),
        (section_2_id, 17, 'cam18-t1-l-q17', 'MULTIPLE_CHOICE', '{"text": "The club is introducing a new type of...", "options": ["A dance class", "B martial art", "C team sport"]}', '["C"]'),
        (section_2_id, 18, 'cam18-t1-l-q18', 'MULTIPLE_CHOICE', '{"text": "The annual members'' party will be held in...", "options": ["A the main hall", "B the cafe", "C the outdoor area"]}', '["A"]'),
        (section_2_id, 19, 'cam18-t1-l-q19', 'MULTIPLE_CHOICE', '{"text": "What is new for children?", "options": ["A A smaller climbing wall", "B A new playground", "C A weekly activity club"]}', '["C"]'),
        (section_2_id, 20, 'cam18-t1-l-q20', 'MULTIPLE_CHOICE', '{"text": "Feedback is collected via...", "options": ["A a suggestion box", "B email", "C an online form"]}', '["A"]');

    -- PART 3
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text, audio_url)
    VALUES (
        'cam18',
        '1',
        'listening',
        3,
        '-- (Optional) Transcript for Part 3 can be placed here --',
        'https://jpocdgkrvohmjkejclpl.supabase.co/storage/v1/object/public/audio_files/Test%20ielts%20listening/Verse1_%20(1).mp3'
    )
    RETURNING id INTO section_3_id;

    -- Questions for Part 3 (Example: Matching Features)
    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer)
    VALUES
        (section_3_id, 21, 'cam18-t1-l-q21', 'MATCHING_FEATURES', '{"text": "The main advantage of the design", "options": [{"letter": "A", "text": "Spout"}, {"letter": "B", "text": "Handle"}, {"letter": "C", "text": "Lid"}]}', '["B"]'),
        (section_3_id, 22, 'cam18-t1-l-q22', 'MATCHING_FEATURES', '{"text": "The part that is most difficult to clean", "options": [{"letter": "A", "text": "Spout"}, {"letter": "B", "text": "Handle"}, {"letter": "C", "text": "Lid"}]}', '["A"]'),
        (section_3_id, 23, 'cam18-t1-l-q23', 'MATCHING_FEATURES', '{"text": "The element that adds the most weight", "options": [{"letter": "A", "text": "Spout"}, {"letter": "B", "text": "Handle"}, {"letter": "C", "text": "Lid"}]}', '["C"]'),
        (section_3_id, 24, 'cam18-t1-l-q24', 'MATCHING_FEATURES', '{"text": "The most fragile part of the product", "options": [{"letter": "A", "text": "Spout"}, {"letter": "B", "text": "Handle"}, {"letter": "C", "text": "Lid"}]}', '["A"]'),
        (section_3_id, 25, 'cam18-t1-l-q25', 'MATCHING_FEATURES', '{"text": "The part that is easiest to replace", "options": [{"letter": "A", "text": "Spout"}, {"letter": "B", "text": "Handle"}, {"letter": "C", "text": "Lid"}]}', '["C"]'),
        (section_3_id, 26, 'cam18-t1-l-q26', 'MULTIPLE_CHOICE', '{"text": "What was the initial feedback from the user group?", "options": ["A The product was too heavy", "B The handle was uncomfortable", "C The lid was difficult to open"]}', '["B"]'),
        (section_3_id, 27, 'cam18-t1-l-q27', 'MULTIPLE_CHOICE', '{"text": "The designers were surprised that users...", "options": ["A liked the color", "B did not use the spout as intended", "C found it easy to clean"]}', '["A"]'),
        (section_3_id, 28, 'cam18-t1-l-q28', 'MULTIPLE_CHOICE', '{"text": "The team decided to change the material of the...", "options": ["A body", "B handle", "C lid"]}', '["C"]'),
        (section_3_id, 29, 'cam18-t1-l-q29', 'MULTIPLE_CHOICE', '{"text": "The final product will be available in...", "options": ["A three colors", "B five colors", "C a single color"]}', '["A"]'),
        (section_3_id, 30, 'cam18-t1-l-q30', 'MULTIPLE_CHOICE', '{"text": "The marketing will focus on the product''s...", "options": ["A durability", "B unique design", "C ease of use"]}', '["A"]');

    -- PART 4
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text, audio_url)
    VALUES (
        'cam18',
        '1',
        'listening',
        4,
        '-- (Optional) Transcript for Part 4 can be placed here --',
        'https://jpocdgkrvohmjkejclpl.supabase.co/storage/v1/object/public/audio_files/Test%20ielts%20listening/Verse1_%20(1).mp3'
    )
    RETURNING id INTO section_4_id;

    -- Questions for Part 4 (Example: Fill in the blank)
    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit)
    VALUES
        (section_4_id, 31, 'cam18-t1-l-q31', 'FILL_IN_BLANK', '{"text": "The history of coffee<br/><br/>The coffee plant is an evergreen shrub that originated in <strong>31</strong> ____."}', '["Ethiopia"]', 'ONE WORD ONLY'),
        (section_4_id, 32, 'cam18-t1-l-q32', 'FILL_IN_BLANK', '{"text": "The first recorded use of coffee as a drink was in the <strong>32</strong> ____ century in Yemen."}', '["15th"]', 'ONE WORD AND/OR A NUMBER'),
        (section_4_id, 33, 'cam18-t1-l-q33', 'FILL_IN_BLANK', '{"text": "Coffee houses in the Ottoman Empire were important social and <strong>33</strong> ____ centers."}', '["political"]', 'ONE WORD ONLY'),
        (section_4_id, 34, 'cam18-t1-l-q34', 'FILL_IN_BLANK', '{"text": "Coffee was introduced to Europe in the 17th century via the port of <strong>34</strong> ____."}', '["Venice"]', 'ONE WORD ONLY'),
        (section_4_id, 35, 'cam18-t1-l-q35', 'FILL_IN_BLANK', '{"text": "The world''s first coffee house, or ''cafe'', opened in <strong>35</strong> ____ in 1683."}', '["Vienna"]', 'ONE WORD ONLY'),
        (section_4_id, 36, 'cam18-t1-l-q36', 'FILL_IN_BLANK', '{"text": "The addition of <strong>36</strong> ____ to coffee became popular in Europe."}', '["sugar"]', 'ONE WORD ONLY'),
        (section_4_id, 37, 'cam18-t1-l-q37', 'FILL_IN_BLANK', '{"text": "Brazil became a major coffee producer after plants were introduced by the <strong>37</strong> ____."}', '["French"]', 'ONE WORD ONLY'),
        (section_4_id, 38, 'cam18-t1-l-q38', 'FILL_IN_BLANK', '{"text": "The invention of <strong>38</strong> ____ coffee in the 20th century made preparation easier."}', '["instant"]', 'ONE WORD ONLY'),
        (section_4_id, 39, 'cam18-t1-l-q39', 'FILL_IN_BLANK', '{"text": "Modern coffee culture is often associated with large <strong>39</strong> ____."}', '["chains"]', 'ONE WORD ONLY'),
        (section_4_id, 40, 'cam18-t1-l-q40', 'FILL_IN_BLANK', '{"text": "The concept of ''fair trade'' aims to ensure better <strong>40</strong> ____ for farmers."}', '["prices"]', 'ONE WORD ONLY');

END $$;

-- Example of how to find the audio URL in Supabase Storage:
-- 1. Go to your Supabase project.
-- 2. Navigate to 'Storage' from the left sidebar.
-- 3. Create a new bucket (e.g., 'audio-files') and set it to be public.
-- 4. Upload your audio file into the bucket.
-- 5. Click on the uploaded file and select 'Get URL'.
-- 6. Copy the public URL and paste it in place of 'YOUR_AUDIO_URL_HERE' for all parts.
