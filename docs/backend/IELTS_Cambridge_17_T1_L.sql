-- This file contains the complete and accurate SQL data for Cambridge IELTS 17, Test 1, Listening.
-- It follows the structure outlined in the DATA_INGESTION_GUIDE_LISTENING.md document.

DO $$
DECLARE
    section_1_id bigint;
    section_2_id bigint;
    section_3_id bigint;
    section_4_id bigint;
BEGIN
    -- First, delete existing data for Cam17, Test 1, Listening to avoid duplicates
    DELETE FROM public.questions WHERE section_id IN (SELECT id FROM public.sections WHERE exam_source = 'cam17' AND test_number = '1' AND skill = 'listening');
    DELETE FROM public.sections WHERE exam_source = 'cam17' AND test_number = '1' AND skill = 'listening';

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 1, LISTENING PART 1
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text)
    VALUES (
        'cam17',
        '1',
        'listening',
        1,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-1-listening-part-1.mp3',
        '...'
    )
    RETURNING id INTO section_1_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit) VALUES
    (section_1_id, 1, 'cam17-t1-l-q1', 'NOTE_COMPLETION', '{"text": "making sure the beach does not have 1 ____ on it"}', '["litter"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 2, 'cam17-t1-l-q2', 'NOTE_COMPLETION', '{"text": "no 2 ____"}', '["dogs"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 3, 'cam17-t1-l-q3', 'NOTE_COMPLETION', '{"text": "next task is taking action to attract 3 ____ to the place"}', '["insects"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 4, 'cam17-t1-l-q4', 'NOTE_COMPLETION', '{"text": "identifying types of 4 ____"}', '["butterflies"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 5, 'cam17-t1-l-q5', 'NOTE_COMPLETION', '{"text": "building a new 5 ____"}', '["wall"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 6, 'cam17-t1-l-q6', 'NOTE_COMPLETION', '{"text": "walk across the sands and reach the 6 ____"}', '["island"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 7, 'cam17-t1-l-q7', 'NOTE_COMPLETION', '{"text": "wear appropriate 7 ____"}', '["boots"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 8, 'cam17-t1-l-q8', 'NOTE_COMPLETION', '{"text": "suitable for 8 ____ to participate in"}', '["beginners"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 9, 'cam17-t1-l-q9', 'NOTE_COMPLETION', '{"text": "making 9 ____ out of wood"}', '["spoons"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 10, 'cam17-t1-l-q10', 'NOTE_COMPLETION', '{"text": "cost of session (no camping): 10 £ ____"}', '["35"]', 'ONE WORD AND/OR A NUMBER');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 1, LISTENING PART 2
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text)
    VALUES (
        'cam17',
        '1',
        'listening',
        2,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-1-listening-part-2.mp3',
        '...'
    )
    RETURNING id INTO section_2_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer) VALUES
    (section_2_id, 11, 'cam17-t1-l-q11', 'MULTIPLE_CHOICE', '{"text": "What is the maximum number of people who can stand on each side of the boat?", "options": ["A 9", "B 15", "C 18"]}', '["A"]'),
    (section_2_id, 12, 'cam17-t1-l-q12', 'MULTIPLE_CHOICE', '{"text": "What colour are the tour boats?", "options": ["A dark red", "B jet black", "C light green"]}', '["C"]'),
    (section_2_id, 13, 'cam17-t1-l-q13', 'MULTIPLE_CHOICE', '{"text": "Which lunchbox is suitable for someone who doesn''t eat meat or fish?", "options": ["A Lunchbox 1", "B Lunchbox 2", "C Lunchbox 3"]}', '["B"]'),
    (section_2_id, 14, 'cam17-t1-l-q14', 'MULTIPLE_CHOICE', '{"text": "What should people do with their litter?", "options": ["A take it home", "B hand it to a member of staff", "C put it in the bins provided on the boat"]}', '["B"]'),
    (section_2_id, 15, 'cam17-t1-l-q15', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO features of the lighthouse does Lou mention?", "options": ["A why it was built", "B who built it", "C how long it took to build", "D who staffed it", "E what it was built with"]}', '["A", "D"]'),
    (section_2_id, 16, 'cam17-t1-l-q16', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO features of the lighthouse does Lou mention?", "options": ["A why it was built", "B who built it", "C how long it took to build", "D who staffed it", "E what it was built with"]}', '["A", "D"]'),
    (section_2_id, 17, 'cam17-t1-l-q17', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO types of creature might come close to the boat?", "options": ["A sea eagles", "B fur seals", "C dolphins", "D whales", "E penguins"]}', '["B", "C"]'),
    (section_2_id, 18, 'cam17-t1-l-q18', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO types of creature might come close to the boat?", "options": ["A sea eagles", "B fur seals", "C dolphins", "D whales", "E penguins"]}', '["B", "C"]'),
    (section_2_id, 19, 'cam17-t1-l-q19', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO points does Lou make about the caves?", "options": ["A Only large tourist boats can visit them.", "B The entrances to them are often blocked.", "C It is too dangerous for individuals to go near them.", "D Someone will explain what is inside them.", "E They cannot be reached on foot."]}', '["D", "E"]'),
    (section_2_id, 20, 'cam17-t1-l-q20', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO points does Lou make about the caves?", "options": ["A Only large tourist boats can visit them.", "B The entrances to them are often blocked.", "C It is too dangerous for individuals to go near them.", "D Someone will explain what is inside them.", "E They cannot be reached on foot."]}', '["D", "E"]');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 1, LISTENING PART 3
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text)
    VALUES (
        'cam17',
        '1',
        'listening',
        3,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-1-listening-part-3.mp3',
        '...'
    )
    RETURNING id INTO section_3_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer) VALUES
    (section_3_id, 21, 'cam17-t1-l-q21', 'MULTIPLE_CHOICE', '{"text": "What problem did both Diana and Tim have when arranging their work experience?", "options": ["A making initial contact with suitable farms", "B organising transport to and from the farm", "C finding a placement for the required length of time"]}', '["A"]'),
    (section_3_id, 22, 'cam17-t1-l-q22', 'MULTIPLE_CHOICE', '{"text": "Tim was pleased to be able to help", "options": ["A a lamb that had a broken leg.", "B a sheep that was having difficulty giving birth.", "C a newly born lamb that was having trouble feeding."]}', '["B"]'),
    (section_3_id, 23, 'cam17-t1-l-q23', 'MULTIPLE_CHOICE', '{"text": "Diana says the sheep on her farm", "options": ["A were of various different varieties.", "B were mainly reared for their meat.", "C had better quality wool than sheep on the hills."]}', '["B"]'),
    (section_3_id, 24, 'cam17-t1-l-q24', 'MULTIPLE_CHOICE', '{"text": "What did the students learn about adding supplements to chicken feed?", "options": ["A These should only be given if specially needed.", "B It is worth paying extra for the most effective ones.", "C The amount given at one time should be limited."]}', '["A"]'),
    (section_3_id, 25, 'cam17-t1-l-q25', 'MULTIPLE_CHOICE', '{"text": "What happened when Diana was working with dairy cows?", "options": ["A She identified some cows incorrectly.", "B She accidentally threw some milk away.", "C She made a mistake when storing milk."]}', '["C"]'),
    (section_3_id, 26, 'cam17-t1-l-q26', 'MULTIPLE_CHOICE', '{"text": "What did both farmers mention about vets and farming?", "options": ["A Vets are failing to cope with some aspects of animal health.", "B There needs to be a fundamental change in the training of vets.", "C Some jobs could be done by the farmer rather than by a vet."]}', '["C"]'),
    (section_3_id, 27, 'cam17-t1-l-q27', 'MATCHING_FEATURES', '{"text": "Medical terminology", "options": [{"letter": "A", "text": "Tim found this easier than expected."}, {"letter": "B", "text": "Tim thought this was not very clearly organised."}, {"letter": "C", "text": "Diana may do some further study on this."}, {"letter": "D", "text": "They both found the reading required for this was difficult."}, {"letter": "E", "text": "Tim was shocked at something he learned on this module."}, {"letter": "F", "text": "They were both surprised how little is known about some aspects of this."}]}', '["A"]'),
    (section_3_id, 28, 'cam17-t1-l-q28', 'MATCHING_FEATURES', '{"text": "Diet and nutrition", "options": [{"letter": "A", "text": "Tim found this easier than expected."}, {"letter": "B", "text": "Tim thought this was not very clearly organised."}, {"letter": "C", "text": "Diana may do some further study on this."}, {"letter": "D", "text": "They both found the reading required for this was difficult."}, {"letter": "E", "text": "Tim was shocked at something he learned on this module."}, {"letter": "F", "text": "They were both surprised how little is known about some aspects of this."}]}', '["E"]'),
    (section_3_id, 29, 'cam17-t1-l-q29', 'MATCHING_FEATURES', '{"text": "Animal disease", "options": [{"letter": "A", "text": "Tim found this easier than expected."}, {"letter": "B", "text": "Tim thought this was not very clearly organised."}, {"letter": "C", "text": "Diana may do some further study on this."}, {"letter": "D", "text": "They both found the reading required for this was difficult."}, {"letter": "E", "text": "Tim was shocked at something he learned on this module."}, {"letter": "F", "text": "They were both surprised how little is known about some aspects of this."}]}', '["F"]'),
    (section_3_id, 30, 'cam17-t1-l-q30', 'MATCHING_FEATURES', '{"text": "Wildlife medication", "options": [{"letter": "A", "text": "Tim found this easier than expected."}, {"letter": "B", "text": "Tim thought this was not very clearly organised."}, {"letter": "C", "text": "Diana may do some further study on this."}, {"letter": "D", "text": "They both found the reading required for this was difficult."}, {"letter": "E", "text": "Tim was shocked at something he learned on this module."}, {"letter": "F", "text": "They were both surprised how little is known about some aspects of this."}]}', '["C"]');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 1, LISTENING PART 4
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text)
    VALUES (
        'cam17',
        '1',
        'listening',
        4,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-1-listening-part-4.mp3',
        '...'
    )
    RETURNING id INTO section_4_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit) VALUES
    (section_4_id, 31, 'cam17-t1-l-q31', 'NOTE_COMPLETION', '{"text": "Mazes are a type of 31 ____"}', '["puzzle"]', 'ONE WORD ONLY'),
    (section_4_id, 32, 'cam17-t1-l-q32', 'NOTE_COMPLETION', '{"text": "____ is needed to navigate through a maze"}', '["logic"]', 'ONE WORD ONLY'),
    (section_4_id, 33, 'cam17-t1-l-q33', 'NOTE_COMPLETION', '{"text": "the word ''maze'' is derived from a word meaning a feeling of 33 ____"}', '["confusion"]', 'ONE WORD ONLY'),
    (section_4_id, 34, 'cam17-t1-l-q34', 'NOTE_COMPLETION', '{"text": "they have frequently been used in 34 ____ and prayer"}', '["meditation"]', 'ONE WORD ONLY'),
    (section_4_id, 35, 'cam17-t1-l-q35', 'NOTE_COMPLETION', '{"text": "Ancient carvings on 35 ____ have been found across many cultures"}', '["stone"]', 'ONE WORD ONLY'),
    (section_4_id, 36, 'cam17-t1-l-q36', 'NOTE_COMPLETION', '{"text": "Ancient Greeks used the symbol on 36 ____"}', '["coins"]', 'ONE WORD ONLY'),
    (section_4_id, 37, 'cam17-t1-l-q37', 'NOTE_COMPLETION', '{"text": "The largest surviving example of a turf labyrinth once had a big 37 ____ at its centre"}', '["tree"]', 'ONE WORD ONLY'),
    (section_4_id, 38, 'cam17-t1-l-q38', 'NOTE_COMPLETION', '{"text": "e.g., walking a maze can reduce a person''s 38 ____ rate"}', '["breathing"]', 'ONE WORD ONLY'),
    (section_4_id, 39, 'cam17-t1-l-q39', 'NOTE_COMPLETION', '{"text": "patients who can''t walk can use ''finger labyrinths'' made from 39 ____"}', '["paper"]', 'ONE WORD ONLY'),
    (section_4_id, 40, 'cam17-t1-l-q40', 'NOTE_COMPLETION', '{"text": "research has shown that Alzheimer''s sufferers experience less 40 ____"}', '["anxiety"]', 'ONE WORD ONLY');

END $$;
