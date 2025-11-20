-- This file contains the complete SQL data for Cambridge IELTS 17, Test 3, Listening.
-- It is structured to match the redesigned Listening data model used by the Cramer app.

DO $$
DECLARE
    section_1_id bigint;
    section_2_id bigint;
    section_3_id bigint;
    section_4_id bigint;
BEGIN
    -- First, delete existing data for Cam17, Test 3, Listening to avoid duplicates
    DELETE FROM public.questions
    WHERE section_id IN (
        SELECT id FROM public.sections
        WHERE exam_source = 'cam17' AND test_number = '3' AND skill = 'listening'
    );

    DELETE FROM public.sections
    WHERE exam_source = 'cam17' AND test_number = '3' AND skill = 'listening';

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 3, LISTENING PART 1
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '3', 'listening', 1,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-3-listening-part-1.mp3',
        '',
        '{
          "blocks": [
            {
              "block_type": "NOTE_COMPLETION",
              "content": {
                "title": "Questions 1-10",
                "instructions_text": "Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.",
                "main_title": "Advice on surfing holidays"
              },
              "question_numbers": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
            }
          ]
        }'::jsonb
    )
    RETURNING id INTO section_1_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit) VALUES
    (section_1_id, 1,  'cam17-t3-l-q1',  'FILL_IN_BLANK', '{"section_title": "Jack''s advice", "text": "\u2022 Recommends surfing for 1 ____ holidays in the summer"}',        '["family"]',     'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 2,  'cam17-t3-l-q2',  'FILL_IN_BLANK', '{"section_title": "Jack''s advice", "text": "\u2022 Need to be quite 2 ____"}',                                  '["fit"]',        'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 3,  'cam17-t3-l-q3',  'FILL_IN_BLANK', '{"section_title": "Irish surfing locations", "text": "\u2022 County Clare \u2013 Lahinch has some good quality 3 ____ and surf schools"}', '["hotels"]',     'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 4,  'cam17-t3-l-q4',  'FILL_IN_BLANK', '{"section_title": "Irish surfing locations", "text": "\u2022 County Mayo \u2013 Good surf school at 4 ____ beach"}',                        '["Carrowniskey"]','ONE WORD AND/OR A NUMBER'),
    (section_1_id, 5,  'cam17-t3-l-q5',  'FILL_IN_BLANK', '{"section_title": "Irish surfing locations", "text": "\u2022 County Mayo \u2013 Surf camp lasts for one 5 ____"}',                         '["week"]',       'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 6,  'cam17-t3-l-q6',  'FILL_IN_BLANK', '{"section_title": "Irish surfing locations", "text": "\u2022 County Mayo \u2013 Can also explore the local 6 ____ by kayak"}',             '["bay"]',        'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 7,  'cam17-t3-l-q7',  'FILL_IN_BLANK', '{"section_title": "Weather", "text": "\u2022 Best month to go: 7 ____"}',                                        '["September"]',  'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 8,  'cam17-t3-l-q8',  'FILL_IN_BLANK', '{"section_title": "Weather", "text": "\u2022 Average temperature in summer: approx. 8 ____ degrees"}',           '["19"]',         'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 9,  'cam17-t3-l-q9',  'FILL_IN_BLANK', '{"section_title": "Costs", "text": "\u2022 Equipment \u2013 Wetsuit and surfboard: 9 ____ euros per day"}',        '["30"]',         'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 10, 'cam17-t3-l-q10', 'FILL_IN_BLANK', '{"section_title": "Costs", "text": "\u2022 Equipment \u2013 Also advisable to hire 10 ____ for warmth"}',          '["boots"]',      'ONE WORD AND/OR A NUMBER');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 3, LISTENING PART 2
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '3', 'listening', 2,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-3-listening-part-2.mp3',
        '',
        '{
          "blocks": [
            {
              "block_type": "INSTRUCTIONS_ONLY",
              "content": {
                "title": "Questions 11 and 12",
                "instructions_text": "<b>Extended hours childcare service</b><br/><br/>Choose TWO letters, A\u2013E."
              },
              "question_numbers": [11, 12]
            },
            {
              "block_type": "INSTRUCTIONS_ONLY",
              "content": {
                "title": "Questions 13-15",
                "instructions_text": "Choose the correct letter, A, B or C."
              },
              "question_numbers": [13, 14, 15]
            },
            {
              "block_type": "MATCHING_FEATURES",
              "content": {
                "title": "Questions 16-20",
                "instructions_text": "What information is given about each of the following activities on offer?<br/><br/>Choose FIVE answers from the box and write the correct letter, A\u2013G, next to Questions 16\u201320.",
                "options_title": "Information",
                "options": [
                  { "letter": "A", "text": "has limited availability" },
                  { "letter": "B", "text": "is no longer available" },
                  { "letter": "C", "text": "is for over 8s only" },
                  { "letter": "D", "text": "requires help from parents" },
                  { "letter": "E", "text": "involves an additional fee" },
                  { "letter": "F", "text": "is a new activity" },
                  { "letter": "G", "text": "was requested by children" }
                ]
              },
              "question_numbers": [16, 17, 18, 19, 20]
            }
          ]
        }'::jsonb
    )
    RETURNING id INTO section_2_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer) VALUES
    (section_2_id, 11, 'cam17-t3-l-q11', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO facts are given about the school''s extended hours childcare service?", "options": ["A It started recently.", "B More children attend after school than before school.", "C An average of 50 children attend in the mornings.", "D A child cannot attend both the before and after school sessions.", "E The maximum number of children who can attend is 70."]}', '["B", "E"]'),
    (section_2_id, 12, 'cam17-t3-l-q12', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO facts are given about the school''s extended hours childcare service?", "options": ["A It started recently.", "B More children attend after school than before school.", "C An average of 50 children attend in the mornings.", "D A child cannot attend both the before and after school sessions.", "E The maximum number of children who can attend is 70."]}', '["B", "E"]'),
    (section_2_id, 13, 'cam17-t3-l-q13', 'MULTIPLE_CHOICE', '{"text": "How much does childcare cost for a complete afternoon session per child?", "options": ["A \u00a33.50", "B \u00a35.70", "C \u00a37.20"]}', '["C"]'),
    (section_2_id, 14, 'cam17-t3-l-q14', 'MULTIPLE_CHOICE', '{"text": "What does the manager say about food?", "options": ["A Children with allergies should bring their own food.", "B Children may bring healthy snacks with them.", "C Children are given a proper meal at 5 p.m."]}', '["C"]'),
    (section_2_id, 15, 'cam17-t3-l-q15', 'MULTIPLE_CHOICE', '{"text": "What is different about arrangements in the school holidays?", "options": ["A Children from other schools can attend.", "B Older children can attend.", "C A greater number of children can attend."]}', '["A"]'),
    (section_2_id, 16, 'cam17-t3-l-q16', 'MATCHING', '{"text": "Spanish"}', '["E"]'),
    (section_2_id, 17, 'cam17-t3-l-q17', 'MATCHING', '{"text": "Music"}',   '["D"]'),
    (section_2_id, 18, 'cam17-t3-l-q18', 'MATCHING', '{"text": "Painting"}','["G"]'),
    (section_2_id, 19, 'cam17-t3-l-q19', 'MATCHING', '{"text": "Yoga"}',    '["F"]'),
    (section_2_id, 20, 'cam17-t3-l-q20', 'MATCHING', '{"text": "Cooking"}', '["C"]');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 3, LISTENING PART 3
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '3', 'listening', 3,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-3-listening-part-3.mp3',
        '',
        '{
          "blocks": [
            {
              "block_type": "INSTRUCTIONS_ONLY",
              "content": {
                "title": "Questions 21-24",
                "instructions_text": "<b>Holly''s Work Placement Tutorial</b><br/><br/>Choose the correct letter, A, B or C."
              },
              "question_numbers": [21, 22, 23, 24]
            },
            {
              "block_type": "MATCHING_FEATURES",
              "content": {
                "title": "Questions 25-30",
                "instructions_text": "What do Holly and her tutor agree is an important aspect of each of the following events management skills?<br/><br/>Choose SIX answers from the box and write the correct letter, A\u2013H, next to Questions 25\u201330.",
                "options_title": "Important aspects",
                "options": [
                  { "letter": "A", "text": "being flexible" },
                  { "letter": "B", "text": "focusing on details" },
                  { "letter": "C", "text": "having a smart appearance" },
                  { "letter": "D", "text": "hiding your emotions" },
                  { "letter": "E", "text": "relying on experts" },
                  { "letter": "F", "text": "trusting your own views" },
                  { "letter": "G", "text": "doing one thing at a time" },
                  { "letter": "H", "text": "thinking of the future" }
                ]
              },
              "question_numbers": [25, 26, 27, 28, 29, 30]
            }
          ]
        }'::jsonb
    )
    RETURNING id INTO section_3_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer) VALUES
    (section_3_id, 21, 'cam17-t3-l-q21', 'MULTIPLE_CHOICE', '{"text": "Holly has chosen the Orion Stadium placement because", "options": ["A it involves children.", "B it is outdoors.", "C it sounds like fun."]}', '["B"]'),
    (section_3_id, 22, 'cam17-t3-l-q22', 'MULTIPLE_CHOICE', '{"text": "Which aspect of safety does Dr Green emphasise most?", "options": ["A ensuring children stay in the stadium", "B checking the equipment children will use", "C removing obstacles in changing rooms"]}', '["A"]'),
    (section_3_id, 23, 'cam17-t3-l-q23', 'MULTIPLE_CHOICE', '{"text": "What does Dr Green say about the spectators?", "options": ["A They can be hard to manage.", "B They make useful volunteers.", "C They shouldn''t take photographs."]}', '["A"]'),
    (section_3_id, 24, 'cam17-t3-l-q24', 'MULTIPLE_CHOICE', '{"text": "What has affected the schedule in the past?", "options": ["A bad weather", "B an injury", "C extra time"]}', '["B"]'),
    (section_3_id, 25, 'cam17-t3-l-q25', 'MATCHING', '{"text": "Communication"}', '["C"]'),
    (section_3_id, 26, 'cam17-t3-l-q26', 'MATCHING', '{"text": "Organisation"}',  '["A"]'),
    (section_3_id, 27, 'cam17-t3-l-q27', 'MATCHING', '{"text": "Time management"}','["D"]'),
    (section_3_id, 28, 'cam17-t3-l-q28', 'MATCHING', '{"text": "Creativity"}',    '["B"]'),
    (section_3_id, 29, 'cam17-t3-l-q29', 'MATCHING', '{"text": "Leadership"}',    '["F"]'),
    (section_3_id, 30, 'cam17-t3-l-q30', 'MATCHING', '{"text": "Networking"}',    '["H"]');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 3, LISTENING PART 4
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '3', 'listening', 4,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-3-listening-part-4.mp3',
        '',
        '{
          "blocks": [
            {
              "block_type": "NOTE_COMPLETION",
              "content": {
                "title": "Questions 31-40",
                "instructions_text": "<b>Bird Migration Theory</b><br/><br/>Complete the notes below. Write ONE WORD ONLY for each answer.",
                "main_title": "Bird Migration Theory"
              },
              "question_numbers": [31, 32, 33, 34, 35, 36, 37, 38, 39, 40]
            }
          ]
        }'::jsonb
    )
    RETURNING id INTO section_4_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit) VALUES
    (section_4_id, 31, 'cam17-t3-l-q31', 'FILL_IN_BLANK', '{"section_title": "Hibernation theory", "text": "\u2022 It was believed that birds hibernated underwater or buried themselves in 31 ____."}', '["mud"]',        'ONE WORD ONLY'),
    (section_4_id, 32, 'cam17-t3-l-q32', 'FILL_IN_BLANK', '{"section_title": "Transmutation theory", "text": "\u2022 In autumn Aristotle observed that redstarts experience the loss of 32 ____ and thought they then turned into robins."}', '["feathers"]',  'ONE WORD ONLY'),
    (section_4_id, 33, 'cam17-t3-l-q33', 'FILL_IN_BLANK', '{"section_title": "Transmutation theory", "text": "\u2022 Aristotle''s assumptions were logical because the two species of birds had a similar 33 ____."}', '["shape"]',     'ONE WORD ONLY'),
    (section_4_id, 34, 'cam17-t3-l-q34', 'FILL_IN_BLANK', '{"section_title": "17th century", "text": "\u2022 Charles Morton popularised the idea that birds fly to the 34 ____ in winter."}', '["moon"]',       'ONE WORD ONLY'),
    (section_4_id, 35, 'cam17-t3-l-q35', 'FILL_IN_BLANK', '{"section_title": "Scientific developments", "text": "\u2022 In 1822, a stork killed in Germany had an African spear in its 35 ____."}', '["neck"]',      'ONE WORD ONLY'),
    (section_4_id, 36, 'cam17-t3-l-q36', 'FILL_IN_BLANK', '{"section_title": "Scientific developments", "text": "\u2022 Previously there had been no 36 ____ that storks migrate to Africa."}', '["evidence"]',  'ONE WORD ONLY'),
    (section_4_id, 37, 'cam17-t3-l-q37', 'FILL_IN_BLANK', '{"section_title": "Scientific developments", "text": "\u2022 Little was known about the 37 ____ and journeys of migrating birds until the practice of ringing was established."}', '["destinations"]','ONE WORD ONLY'),
    (section_4_id, 38, 'cam17-t3-l-q38', 'FILL_IN_BLANK', '{"section_title": "Scientific developments", "text": "\u2022 It was thought large birds carried small birds because they were considered incapable of travelling across huge 38 ____."}', '["oceans"]',    'ONE WORD ONLY'),
    (section_4_id, 39, 'cam17-t3-l-q39', 'FILL_IN_BLANK', '{"section_title": "Scientific developments", "text": "\u2022 Ringing depended on what is called the 39 ''____'' of dead birds."}', '["recovery"]', 'ONE WORD ONLY'),
    (section_4_id, 40, 'cam17-t3-l-q40', 'FILL_IN_BLANK', '{"section_title": "Scientific developments", "text": "\u2022 In 1931, the first 40 ____ to show the migration of European birds was printed."}', '["atlas"]', 'ONE WORD ONLY');

END $$;

