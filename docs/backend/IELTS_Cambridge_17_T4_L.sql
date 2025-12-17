-- This file contains the complete SQL data for Cambridge IELTS 17, Test 4, Listening.
-- It is structured to match the redesigned Listening data model used by the Cramer app.

DO $$
DECLARE
    section_1_id bigint;
    section_2_id bigint;
    section_3_id bigint;
    section_4_id bigint;
BEGIN
    -- First, delete existing data for Cam17, Test 4, Listening to avoid duplicates
    DELETE FROM public.questions
    WHERE section_id IN (
        SELECT id FROM public.sections
        WHERE exam_source = 'cam17' AND test_number = '4' AND skill = 'listening'
    );

    DELETE FROM public.sections
    WHERE exam_source = 'cam17' AND test_number = '4' AND skill = 'listening';

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 4, LISTENING PART 1
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '4', 'listening', 1,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-4-listening-part-1.mp3',
        '',
        '{
          "blocks": [
            {
              "block_type": "NOTE_COMPLETION",
              "content": {
                "title": "Questions 1-10",
                "instructions_text": "Complete the notes below. Write ONE WORD for each answer.",
                "main_title": "Easy Life Cleaning Services"
              },
              "question_numbers": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
            }
          ]
        }'::jsonb
    )
    RETURNING id INTO section_1_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit) VALUES
    (section_1_id, 1,  'cam17-t4-l-q1',  'FILL_IN_BLANK', '{"section_title": "Basic cleaning package", "text": "\u2022 Cleaning the 1 ____ throughout the apartment"}',                  '["floors"]',      'ONE WORD'),
    (section_1_id, 2,  'cam17-t4-l-q2',  'FILL_IN_BLANK', '{"section_title": "Additional services", "text": "\u2022 Every week \u2013 Cleaning the 2 ____"}',                            '["fridge"]',      'ONE WORD'),
    (section_1_id, 3,  'cam17-t4-l-q3',  'FILL_IN_BLANK', '{"section_title": "Additional services", "text": "\u2022 Every week \u2013 Ironing clothes \u2013 3 ____ only"}',          '["shirts"]',      'ONE WORD'),
    (section_1_id, 4,  'cam17-t4-l-q4',  'FILL_IN_BLANK', '{"section_title": "Additional services", "text": "\u2022 Every month \u2013 Cleaning all the 4 ____ from the inside"}',     '["windows"]',     'ONE WORD'),
    (section_1_id, 5,  'cam17-t4-l-q5',  'FILL_IN_BLANK', '{"section_title": "Additional services", "text": "\u2022 Every month \u2013 Washing down the 5 ____"}',                     '["balcony"]',     'ONE WORD'),
    (section_1_id, 6,  'cam17-t4-l-q6',  'FILL_IN_BLANK', '{"section_title": "Other possibilities", "text": "\u2022 They can organise a plumber or an 6 ____ if necessary."}',        '["electrician"]', 'ONE WORD'),
    (section_1_id, 7,  'cam17-t4-l-q7',  'FILL_IN_BLANK', '{"section_title": "Other possibilities", "text": "\u2022 A special cleaning service is available for customers who are allergic to 7 ____."}', '["dust"]', 'ONE WORD'),
    (section_1_id, 8,  'cam17-t4-l-q8',  'FILL_IN_BLANK', '{"section_title": "Information on the cleaners", "text": "\u2022 Before being hired, all cleaners have a background check carried out by the 8 ____."}', '["police"]', 'ONE WORD'),
    (section_1_id, 9,  'cam17-t4-l-q9',  'FILL_IN_BLANK', '{"section_title": "Information on the cleaners", "text": "\u2022 All cleaners are given 9 ____ for two weeks."}',           '["training"]',    'ONE WORD'),
    (section_1_id, 10, 'cam17-t4-l-q10', 'FILL_IN_BLANK', '{"section_title": "Information on the cleaners", "text": "\u2022 Customers send a 10 ____ after each visit."}',            '["review"]',      'ONE WORD');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 4, LISTENING PART 2
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '4', 'listening', 2,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-4-listening-part-2.mp3',
        '',
        '{
          "blocks": [
            {
              "block_type": "INSTRUCTIONS_ONLY",
              "content": {
                "title": "Questions 11-14",
                "instructions_text": "Choose the correct letter, A, B or C."
              },
              "question_numbers": [11, 12, 13, 14]
            },
            {
              "block_type": "MATCHING_FEATURES",
              "content": {
                "title": "Questions 15-20",
                "instructions_text": "Which way of reducing staff turnover was used in each of the following hotels?<br/><br/>Write the correct letter, A, B or C, next to Questions 15\u201320.",
                "options_title": "Ways of reducing staff turnover",
                "options": [
                  { "letter": "A", "text": "improving relationships and teamwork" },
                  { "letter": "B", "text": "offering incentives and financial benefits" },
                  { "letter": "C", "text": "providing career opportunities" }
                ]
              },
              "question_numbers": [15, 16, 17, 18, 19, 20]
            }
          ]
        }'::jsonb
    )
    RETURNING id INTO section_2_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer) VALUES
    (section_2_id, 11, 'cam17-t4-l-q11', 'MULTIPLE_CHOICE', '{"text": "Many hotel managers are unaware that their staff often leave because of", "options": ["A a lack of training.", "B long hours.", "C low pay."]}', '["A"]'),
    (section_2_id, 12, 'cam17-t4-l-q12', 'MULTIPLE_CHOICE', '{"text": "What is the impact of high staff turnover on managers?", "options": ["A an increased workload", "B low morale", "C an inability to meet targets"]}', '["A"]'),
    (section_2_id, 13, 'cam17-t4-l-q13', 'MULTIPLE_CHOICE', '{"text": "What mistake should managers always avoid?", "options": ["A failing to treat staff equally", "B reorganising shifts without warning", "C neglecting to have enough staff during busy periods"]}', '["A"]'),
    (section_2_id, 14, 'cam17-t4-l-q14', 'MULTIPLE_CHOICE', '{"text": "What unexpected benefit did Dunwich Hotel notice after improving staff retention rates?", "options": ["A a fall in customer complaints", "B an increase in loyalty club membership", "C a rise in spending per customer"]}', '["C"]'),
    (section_2_id, 15, 'cam17-t4-l-q15', 'MATCHING', '{"text": "The Sun Club"}',   '["A"]'),
    (section_2_id, 16, 'cam17-t4-l-q16', 'MATCHING', '{"text": "The Portland"}',   '["C"]'),
    (section_2_id, 17, 'cam17-t4-l-q17', 'MATCHING', '{"text": "Bluewater Hotels"}','["B"]'),
    (section_2_id, 18, 'cam17-t4-l-q18', 'MATCHING', '{"text": "Pentlow Hotels"}', '["C"]'),
    (section_2_id, 19, 'cam17-t4-l-q19', 'MATCHING', '{"text": "Green Planet"}',   '["B"]'),
    (section_2_id, 20, 'cam17-t4-l-q20', 'MATCHING', '{"text": "The Amesbury"}',   '["A"]');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 4, LISTENING PART 3
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '4', 'listening', 3,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-4-listening-part-3.mp3',
        '',
        '{
          "blocks": [
            {
              "block_type": "INSTRUCTIONS_ONLY",
              "content": {
                "title": "Questions 21-22",
                "instructions_text": "Choose TWO letters, A\u2013E."
              },
              "question_numbers": [21, 22]
            },
            {
              "block_type": "INSTRUCTIONS_ONLY",
              "content": {
                "title": "Questions 23 and 24",
                "instructions_text": "Choose TWO letters, A\u2013E."
              },
              "question_numbers": [23, 24]
            },
            {
              "block_type": "MATCHING_FEATURES",
              "content": {
                "title": "Questions 25-30",
                "instructions_text": "What comment do the students make about the development of each of the following items of sporting equipment?<br/><br/>Choose SIX answers from the box and write the correct letter, A\u2013H, next to Questions 25\u201330.",
                "options_title": "Comments about the development of the equipment",
                "options": [
                  { "letter": "A", "text": "It could cause excessive sweating." },
                  { "letter": "B", "text": "The material was being mass produced for another purpose." },
                  { "letter": "C", "text": "People often needed to make their own." },
                  { "letter": "D", "text": "It often had to be replaced." },
                  { "letter": "E", "text": "The material was expensive." },
                  { "letter": "F", "text": "It was unpopular among spectators." },
                  { "letter": "G", "text": "It caused injuries." },
                  { "letter": "H", "text": "No one using it liked it at first." }
                ]
              },
              "question_numbers": [25, 26, 27, 28, 29, 30]
            }
          ]
        }'::jsonb
    )
    RETURNING id INTO section_3_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer) VALUES
    (section_3_id, 21, 'cam17-t4-l-q21', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO points do Thomas and Jeanne make about Thomas''s sporting activities at school?", "options": ["A He should have felt more positive about them.", "B The training was too challenging for him.", "C He could have worked harder at them.", "D His parents were disappointed in him.", "E His fellow students admired him."]}', '["C", "E"]'),
    (section_3_id, 22, 'cam17-t4-l-q22', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO points do Thomas and Jeanne make about Thomas''s sporting activities at school?", "options": ["A He should have felt more positive about them.", "B The training was too challenging for him.", "C He could have worked harder at them.", "D His parents were disappointed in him.", "E His fellow students admired him."]}', '["C", "E"]'),
    (section_3_id, 23, 'cam17-t4-l-q23', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO feelings did Thomas experience when he was in Kenya?", "options": ["A disbelief", "B relief", "C stress", "D gratitude", "E homesickness"]}', '["A", "D"]'),
    (section_3_id, 24, 'cam17-t4-l-q24', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO feelings did Thomas experience when he was in Kenya?", "options": ["A disbelief", "B relief", "C stress", "D gratitude", "E homesickness"]}', '["A", "D"]'),
    (section_3_id, 25, 'cam17-t4-l-q25', 'MATCHING', '{"text": "the table tennis bat"}', '["B"]'),
    (section_3_id, 26, 'cam17-t4-l-q26', 'MATCHING', '{"text": "the cricket helmet"}',  '["F"]'),
    (section_3_id, 27, 'cam17-t4-l-q27', 'MATCHING', '{"text": "the cycle helmet"}',    '["A"]'),
    (section_3_id, 28, 'cam17-t4-l-q28', 'MATCHING', '{"text": "the golf club"}',       '["D"]'),
    (section_3_id, 29, 'cam17-t4-l-q29', 'MATCHING', '{"text": "the hockey stick"}',    '["C"]'),
    (section_3_id, 30, 'cam17-t4-l-q30', 'MATCHING', '{"text": "the football"}',        '["G"]');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 4, LISTENING PART 4
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '4', 'listening', 4,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-4-listening-part-4.mp3',
        '',
        '{
          "blocks": [
            {
              "block_type": "NOTE_COMPLETION",
              "content": {
                "title": "Questions 31-40",
                "instructions_text": "<b>Maple syrup</b><br/><br/>Complete the notes below. Write ONE WORD ONLY for each answer.",
                "main_title": "Maple syrup"
              },
              "question_numbers": [31, 32, 33, 34, 35, 36, 37, 38, 39, 40]
            }
          ]
        }'::jsonb
    )
    RETURNING id INTO section_4_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit) VALUES
    (section_4_id, 31, 'cam17-t4-l-q31', 'FILL_IN_BLANK', '{"section_title": "What is maple syrup?", "text": "\u2022 colour described as 31 ____"}', '["golden"]', 'ONE WORD ONLY'),
    (section_4_id, 32, 'cam17-t4-l-q32', 'FILL_IN_BLANK', '{"section_title": "What is maple syrup?", "text": "\u2022 very 32 ____ compared to refined sugar"}', '["healthy"]', 'ONE WORD ONLY'),
    (section_4_id, 33, 'cam17-t4-l-q33', 'FILL_IN_BLANK', '{"section_title": "The maple tree", "text": "\u2022 Best growing conditions and 33 ____ are in Canada and North America"}', '["climate"]', 'ONE WORD ONLY'),
    (section_4_id, 34, 'cam17-t4-l-q34', 'FILL_IN_BLANK', '{"section_title": "Early maple sugar producers", "text": "\u2022 used hot 34 ____ to heat the sap"}', '["rocks"]', 'ONE WORD ONLY'),
    (section_4_id, 35, 'cam17-t4-l-q35', 'FILL_IN_BLANK', '{"section_title": "Today''s maple syrup \u2013 The trees", "text": "\u2022 Tree trunks may not have the correct 35 ____ until they have been growing for 40 years."}', '["diameter"]', 'ONE WORD ONLY'),
    (section_4_id, 36, 'cam17-t4-l-q36', 'FILL_IN_BLANK', '{"section_title": "Today''s maple syrup \u2013 The production", "text": "\u2022 A tap is drilled into the trunk and a 36 ____ carries the sap into a bucket."}', '["tube"]', 'ONE WORD ONLY'),
    (section_4_id, 37, 'cam17-t4-l-q37', 'FILL_IN_BLANK', '{"section_title": "Today''s maple syrup \u2013 The production", "text": "\u2022 Large pans of sap called evaporators are heated by means of a 37 ____."}', '["fire"]', 'ONE WORD ONLY'),
    (section_4_id, 38, 'cam17-t4-l-q38', 'FILL_IN_BLANK', '{"section_title": "Today''s maple syrup \u2013 The production", "text": "\u2022 A lot of 38 ____ is produced during the evaporation process."}', '["steam"]', 'ONE WORD ONLY'),
    (section_4_id, 39, 'cam17-t4-l-q39', 'FILL_IN_BLANK', '{"section_title": "Today''s maple syrup \u2013 The production", "text": "\u2022 \"Sugar sand\" is removed because it makes the syrup look 39 ____ and affects the taste."}', '["cloudy"]', 'ONE WORD ONLY'),
    (section_4_id, 40, 'cam17-t4-l-q40', 'FILL_IN_BLANK', '{"section_title": "Today''s maple syrup \u2013 The production", "text": "\u2022 A huge quantity of sap is needed to make a 40 ____ of maple syrup."}', '["litre"]', 'ONE WORD ONLY');

END $$;

