-- This file contains the complete SQL data for Cambridge IELTS 17, Test 2, Listening.
-- It is structured to match the redesigned Listening data model used by the Cramer app.

DO $$
DECLARE
    section_1_id bigint;
    section_2_id bigint;
    section_3_id bigint;
    section_4_id bigint;
BEGIN
    -- First, delete existing data for Cam17, Test 2, Listening to avoid duplicates
    DELETE FROM public.questions
    WHERE section_id IN (
        SELECT id FROM public.sections
        WHERE exam_source = 'cam17' AND test_number = '2' AND skill = 'listening'
    );

    DELETE FROM public.sections
    WHERE exam_source = 'cam17' AND test_number = '2' AND skill = 'listening';

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 2, LISTENING PART 1
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '2', 'listening', 1,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-2-listening-part-1.mp3',
        '',
        '{
          "blocks": [
            {
              "block_type": "NOTE_COMPLETION",
              "content": {
                "title": "Questions 1-10",
                "instructions_text": "Questions 1\u20137\\nComplete the notes below.<br/><br/>Questions 8\u201310<br/>Complete the table below.<br/><br/>Write ONE WORD ONLY for each answer.",
                "main_title": "Opportunities for voluntary work in Southoe village"
              },
              "question_numbers": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
            }
          ]
        }'::jsonb
    )
    RETURNING id INTO section_1_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit) VALUES
    (section_1_id, 1, 'cam17-t2-l-q1', 'FILL_IN_BLANK',  '{"section_title": "Library", "text": "\u2022 Help with 1 ____ books (times to be arranged)"}',           '["collecting"]', 'ONE WORD ONLY'),
    (section_1_id, 2, 'cam17-t2-l-q2', 'FILL_IN_BLANK',  '{"section_title": "Library", "text": "\u2022 Help needed to keep 2 ____ of books up to date"}',         '["records"]',   'ONE WORD ONLY'),
    (section_1_id, 3, 'cam17-t2-l-q3', 'FILL_IN_BLANK',  '{"section_title": "Library", "text": "\u2022 Library is in the 3 ____ Room in the village hall"}',      '["West"]',      'ONE WORD ONLY'),
    (section_1_id, 4, 'cam17-t2-l-q4', 'FILL_IN_BLANK',  '{"section_title": "Lunch club", "text": "\u2022 Help by providing 4 ____"}',                            '["transport"]', 'ONE WORD ONLY'),
    (section_1_id, 5, 'cam17-t2-l-q5', 'FILL_IN_BLANK',  '{"section_title": "Lunch club", "text": "\u2022 Help with hobbies such as 5 ____"}',                    '["art"]',       'ONE WORD ONLY'),
    (section_1_id, 6, 'cam17-t2-l-q6', 'FILL_IN_BLANK',  '{"section_title": "Help for individuals", "text": "\u2022 Taking Mrs Carroll to 6 ____"}',              '["hospital"]',  'ONE WORD ONLY'),
    (section_1_id, 7, 'cam17-t2-l-q7', 'FILL_IN_BLANK',  '{"section_title": "Help for individuals", "text": "\u2022 Work in the 7 ____ at Mr Selsbury''s house"}','["garden"]',    'ONE WORD ONLY'),
    (section_1_id, 8, 'cam17-t2-l-q8', 'FILL_IN_BLANK',  '{"section_title": "Village social events", "text": "19 Oct 8 ____ \u2013 Village hall \u2013 providing refreshments"}', '["quiz"]',      'ONE WORD ONLY'),
    (section_1_id, 9, 'cam17-t2-l-q9', 'FILL_IN_BLANK',  '{"section_title": "Village social events", "text": "18 Nov dance \u2013 Village hall \u2013 checking 9 ____"}',          '["tickets"]',   'ONE WORD ONLY'),
    (section_1_id, 10,'cam17-t2-l-q10','FILL_IN_BLANK',  '{"section_title": "Village social events", "text": "31 Dec New Year''s Eve party \u2013 Mountfort Hotel \u2013 designing the 10 ____"}', '["poster"]', 'ONE WORD ONLY');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 2, LISTENING PART 2
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '2', 'listening', 2,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-2-listening-part-2.mp3',
        '',
        '{
          "blocks": [
            {
              "block_type": "INSTRUCTIONS_ONLY",
              "content": {
                "title": "Questions 11-14",
                "instructions_text": "<b>Oniton Hall</b><br/><br/>Choose the correct letter, A, B or C."
              },
              "question_numbers": [11, 12, 13, 14]
            },
            {
              "block_type": "MATCHING_FEATURES",
              "content": {
                "title": "Questions 15-20",
                "instructions_text": "Which activity is offered at each of the following locations on the farm?<br/><br/>Choose SIX answers from the box and write the correct letter, A\u2013H, next to Questions 15\u201320.",
                "options_title": "Activities",
                "options": [
                  { "letter": "A", "text": "shopping" },
                  { "letter": "B", "text": "watching cows being milked" },
                  { "letter": "C", "text": "seeing old farming equipment" },
                  { "letter": "D", "text": "eating and drinking" },
                  { "letter": "E", "text": "starting a trip" },
                  { "letter": "F", "text": "seeing rare breeds of animals" },
                  { "letter": "G", "text": "helping to look after animals" },
                  { "letter": "H", "text": "using farming tools" }
                ]
              },
              "question_numbers": [15, 16, 17, 18, 19, 20]
            }
          ]
        }'::jsonb
    )
    RETURNING id INTO section_2_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer) VALUES
    (section_2_id, 11, 'cam17-t2-l-q11', 'MULTIPLE_CHOICE', '{"text": "Many past owners made changes to", "options": ["A the gardens.", "B the house.", "C the farm."]}', '["B"]'),
    (section_2_id, 12, 'cam17-t2-l-q12', 'MULTIPLE_CHOICE', '{"text": "Sir Edward Downes built Oniton Hall because he wanted", "options": ["A a place for discussing politics.", "B a place to display his wealth.", "C a place for artists and writers."]}', '["C"]'),
    (section_2_id, 13, 'cam17-t2-l-q13', 'MULTIPLE_CHOICE', '{"text": "Visitors can learn about the work of servants in the past from", "options": ["A audio guides.", "B photographs.", "C people in costume."]}', '["C"]'),
    (section_2_id, 14, 'cam17-t2-l-q14', 'MULTIPLE_CHOICE', '{"text": "What is new for children at Oniton Hall?", "options": ["A clothes for dressing up", "B mini tractors", "C the adventure playground"]}', '["B"]'),
    (section_2_id, 15, 'cam17-t2-l-q15', 'MATCHING',        '{"text": "dairy"}',     '["D"]'),
    (section_2_id, 16, 'cam17-t2-l-q16', 'MATCHING',        '{"text": "large barn"}','["C"]'),
    (section_2_id, 17, 'cam17-t2-l-q17', 'MATCHING',        '{"text": "small barn"}','["G"]'),
    (section_2_id, 18, 'cam17-t2-l-q18', 'MATCHING',        '{"text": "stables"}',   '["A"]'),
    (section_2_id, 19, 'cam17-t2-l-q19', 'MATCHING',        '{"text": "shed"}',      '["E"]'),
    (section_2_id, 20, 'cam17-t2-l-q20', 'MATCHING',        '{"text": "parkland"}',  '["F"]');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 2, LISTENING PART 3
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '2', 'listening', 3,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-2-listening-part-3.mp3',
        '',
        '{
          "blocks": [
            {
              "block_type": "INSTRUCTIONS_ONLY",
              "content": {
                "title": "Questions 21 and 22",
                "instructions_text": "Choose TWO letters, A\u2013E."
              },
              "question_numbers": [21, 22]
            },
            {
              "block_type": "MATCHING_FEATURES",
              "content": {
                "title": "Questions 23-27",
                "instructions_text": "Which opinion do the speakers give about each of the following aspects of The Emporium''s production of Romeo and Juliet?<br/><br/>Choose FIVE answers from the box and write the correct letter, A\u2013G, next to Questions 23\u201327.",
                "options_title": "Opinions",
                "options": [
                  { "letter": "A", "text": "They both expected this to be more traditional." },
                  { "letter": "B", "text": "They both thought this was original." },
                  { "letter": "C", "text": "They agree this created the right atmosphere." },
                  { "letter": "D", "text": "They agree this was a major strength." },
                  { "letter": "E", "text": "They were both disappointed by this." },
                  { "letter": "F", "text": "They disagree about why this was an issue." },
                  { "letter": "G", "text": "They disagree about how this could be improved." }
                ]
              },
              "question_numbers": [23, 24, 25, 26, 27]
            },
            {
              "block_type": "INSTRUCTIONS_ONLY",
              "content": {
                "title": "Questions 28-30",
                "instructions_text": "Choose the correct letter, A, B or C."
              },
              "question_numbers": [28, 29, 30]
            }
          ]
        }'::jsonb
    )
    RETURNING id INTO section_3_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer) VALUES
    (section_3_id, 21, 'cam17-t2-l-q21', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO things do the students agree they need to include in their reviews of Romeo and Juliet?", "options": ["A analysis of the text", "B a summary of the plot", "C a description of the theatre", "D a personal reaction", "E a reference to particular scenes"]}', '["D", "E"]'),
    (section_3_id, 22, 'cam17-t2-l-q22', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO things do the students agree they need to include in their reviews of Romeo and Juliet?", "options": ["A analysis of the text", "B a summary of the plot", "C a description of the theatre", "D a personal reaction", "E a reference to particular scenes"]}', '["D", "E"]'),
    (section_3_id, 23, 'cam17-t2-l-q23', 'MATCHING', '{"text": "the set"}',             '["D"]'),
    (section_3_id, 24, 'cam17-t2-l-q24', 'MATCHING', '{"text": "the lighting"}',        '["C"]'),
    (section_3_id, 25, 'cam17-t2-l-q25', 'MATCHING', '{"text": "the costume design"}',  '["A"]'),
    (section_3_id, 26, 'cam17-t2-l-q26', 'MATCHING', '{"text": "the music"}',           '["E"]'),
    (section_3_id, 27, 'cam17-t2-l-q27', 'MATCHING', '{"text": "the actors'' delivery"}','["F"]'),
    (section_3_id, 28, 'cam17-t2-l-q28', 'MULTIPLE_CHOICE', '{"text": "The students think the story of Romeo and Juliet is still relevant for young people today because", "options": ["A it illustrates how easily conflict can start.", "B it deals with problems that families experience.", "C it teaches them about relationships."]}', '["B"]'),
    (section_3_id, 29, 'cam17-t2-l-q29', 'MULTIPLE_CHOICE', '{"text": "The students found watching Romeo and Juliet in another language", "options": ["A frustrating.", "B demanding.", "C moving."]}', '["C"]'),
    (section_3_id, 30, 'cam17-t2-l-q30', 'MULTIPLE_CHOICE', '{"text": "Why do the students think Shakespeare''s plays have such international appeal?", "options": ["A The stories are exciting.", "B There are recognisable characters.", "C They can be interpreted in many ways."]}', '["C"]');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 2, LISTENING PART 4
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '2', 'listening', 4,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-2-listening-part-4.mp3',
        '',
        '{
          "blocks": [
            {
              "block_type": "NOTE_COMPLETION",
              "content": {
                "title": "Questions 31-40",
                "instructions_text": "<b>The impact of digital technology on the Icelandic language</b><br/><br/>Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.",
                "main_title": "The Icelandic language"
              },
              "question_numbers": [31, 32, 33, 34, 35, 36, 37, 38, 39, 40]
            }
          ]
        }'::jsonb
    )
    RETURNING id INTO section_4_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit) VALUES
    (section_4_id, 31, 'cam17-t2-l-q31', 'FILL_IN_BLANK', '{"section_title": "The Icelandic language", "text": "\u2022 has approximately 31 ____ speakers"}', '["321,000"]', 'ONE WORD AND/OR A NUMBER'),
    (section_4_id, 32, 'cam17-t2-l-q32', 'FILL_IN_BLANK', '{"section_title": "The Icelandic language", "text": "\u2022 has a 32 ____ that is still growing"}', '["vocabulary"]', 'ONE WORD AND/OR A NUMBER'),
    (section_4_id, 33, 'cam17-t2-l-q33', 'FILL_IN_BLANK', '{"section_title": "The Icelandic language", "text": "\u2022 has its own words for computer-based concepts, such as web browser and 33 ____"}', '["podcast"]', 'ONE WORD AND/OR A NUMBER'),
    (section_4_id, 34, 'cam17-t2-l-q34', 'FILL_IN_BLANK', '{"section_title": "Young speakers", "text": "\u2022 are big users of digital technology, such as 34 ____"}', '["smartphones"]', 'ONE WORD AND/OR A NUMBER'),
    (section_4_id, 35, 'cam17-t2-l-q35', 'FILL_IN_BLANK', '{"section_title": "Young speakers", "text": "\u2022 are becoming 35 ____ very quickly"}', '["bilingual"]', 'ONE WORD AND/OR A NUMBER'),
    (section_4_id, 36, 'cam17-t2-l-q36', 'FILL_IN_BLANK', '{"section_title": "Young speakers", "text": "\u2022 are having discussions using only English while they are in the 36 ____ at school"}', '["playground"]', 'ONE WORD AND/OR A NUMBER'),
    (section_4_id, 37, 'cam17-t2-l-q37', 'FILL_IN_BLANK', '{"section_title": "Young speakers", "text": "\u2022 are better able to identify the content of a 37 ____ in English than Icelandic"}', '["picture"]', 'ONE WORD AND/OR A NUMBER'),
    (section_4_id, 38, 'cam17-t2-l-q38', 'FILL_IN_BLANK', '{"section_title": "Technology and internet companies", "text": "\u2022 write very little in Icelandic because of the small number of speakers and because of how complicated its 38 ____ is"}', '["grammar"]', 'ONE WORD AND/OR A NUMBER'),
    (section_4_id, 39, 'cam17-t2-l-q39', 'FILL_IN_BLANK', '{"section_title": "The Icelandic government", "text": "\u2022 is worried that young Icelanders may lose their 39 ____ as Icelanders"}', '["identity"]', 'ONE WORD AND/OR A NUMBER'),
    (section_4_id, 40, 'cam17-t2-l-q40', 'FILL_IN_BLANK', '{"section_title": "The Icelandic government", "text": "\u2022 is worried about the consequences of children not being 40 ____ in either Icelandic or English"}', '["fluent"]', 'ONE WORD AND/OR A NUMBER');

END $$;

