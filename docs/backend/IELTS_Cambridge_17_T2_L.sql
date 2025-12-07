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
        'https://jpocdgkrvohmjkejclpl.supabase.co/storage/v1/object/public/IELTS/LISTENING_AUDIOS/IELTS17_TEST2/IELTS17_T2_P1.mp3',
        'JANE: Hello, Jane Fairbanks speaking.
FRANK: Oh, good morning. My name''s Frank Pritchard. I''ve just retired and moved to Southoe. I''d like to become a volunteer, and I gather you co-ordinate voluntary work in the village.
JANE: That''s right.
FRANK: What sort of thing could I do?
JANE: Well, we need help with the village library. We borrow books from the town library, and individuals also donate them. So, one thing you could do is get involved in collecting them – if you''ve got a car, that is.
FRANK: Yes, that''s no problem.
JANE: The times are pretty flexible so we can arrange it to suit you. Another thing is the records that we keep of the books we''re given, and those we borrow and need to return to the town library. It would be very useful to have another person to help keep them up to date.
FRANK: Right. I''m used to working on a computer – I presume they''re computerised?
JANE: Oh yes.
FRANK: Is the library purpose-built? I haven''t noticed it when I''ve walked round the village.
JANE: No, we simply have the use of a room in the village hall, the West Room. It''s on the left as you go in.
FRANK: I must go and have a look inside the hall.
JANE: Yes, it''s a nice building.
FRANK: Do you run a lunch club in the village for elderly people? I know a lot of places do.
JANE: Yes, we have a very successful club.
FRANK: I could help with transport, if that''s of any use.
JANE: Ooo definitely. People come to the club from neighbouring villages, and we''re always in need of more drivers.
FRANK: And does the club have groups that focus on a particular hobby, too? I could get involved in one or two, particularly if there are any art groups.
JANE: Excellent. I''ll find out where we need help and get back to you.
FRANK: Fine. What about help for individual residents. Do you arrange that at all?
JANE: Yes, we do it as a one-off. In fact, there''s Mrs Carroll. She needs a lift to the hospital next week, and we''re struggling to find someone.
FRANK: When''s her appointment?
JANE: On Tuesday. It would take the whole morning.
FRANK: I could do that.
JANE: Oh, that would be great. Thank you. And also, next week, we''re arranging to have some work done to Mr Selsbury''s house before he moves, as he isn''t healthy enough to do it himself. We''ve got some people to decorate his kitchen, but if you could do some weeding in his garden, that would be wonderful.
FRANK: OK. I''d enjoy that. And presumably the day and time are flexible?
JANE: On yes. Just say when would suit you best, and we''ll let Mr Selsbury know.
FRANK: Good.
JANE: The volunteers group also organises monthly social events, which is a great way to meet other people, of course.
FRANK: Uhuh.
JANE: So next month, on the 19th of October, we''re holding a quiz – a couple of residents are great at planning unusual ones, and we''ve got all the village hall.
FRANK: That sounds like fun. Can I do anything to help?
JANE: Well, because of the number of people, we need plenty of refreshments for halfway through. So, if you could provide any, we''d be grateful.
FRANK: I''m sure I could. I''ll think about what to make, and let you know.
JANE: Thank you. Then on November the 18th, we''re holding a dance, also in the village hall. We''ve booked a band that specialises in music of the 1930s – they''ve been before, and we''ve had a lot of requests to bring them back.
FRANK: I''m not really a dancer, but I''d like to do something to help.
JANE: Well, we sell tickets in advance, and having an extra person to check them at the door, as people arrive, would be good – it can be quite a bottleneck if everyone arrives at once!
FRANK: OK. I''m happy with that.
JANE: We''re also arranging a New Year''s Eve party. We''re expecting that to be a really big event, so instead of the village hall, it''ll be held in the Mountfort Hotel.
FRANK: The ...?
JANE: Mountfort. M-O-U-N-T-F-O-R-T Hotel. It isn''t in Southoe itself, but it''s only a couple of miles away. The hotel will be providing dinner and we''ve booked a band. The one thing we haven''t got yet is a poster. That isn''t something you could do, by any chance, is it?
FRANK: Well actually, yes. Before I retired I was a graphic designer, so that''s right up my street.
JANE: Oh perfect! I''ll give you the details, and then perhaps you could send me a draft ...
FRANK: Of course.',
        '{
          "blocks": [
            {
              "block_type": "NOTE_COMPLETION",
              "content": {
                "title": "Questions 1-10",
                "instructions_text": "Questions 1–7<br/>Complete the notes below.<br/><br/>Questions 8–10<br/>Complete the table below.<br/><br/>Write ONE WORD ONLY for each answer.",
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
        'https://jpocdgkrvohmjkejclpl.supabase.co/storage/v1/object/public/IELTS/LISTENING_AUDIOS/IELTS17_TEST2/IELTS17_T2_P2.mp3',
        'Good morning, and welcome to Oniton Hall, one of the largest estates in the area. My name''s Nick, and I''m one of the guides. I''ll give you a brief introduction to the estate while we''re sitting down and then we''ll walk round. The estate consists of the house, gardens, parkland and farm, and it dates back to the fourteenth century. The original house was replaced in the late seventeenth century, and of course it has had a large number of owners. Almost all of them have left their mark, generally, by adding new rooms, like the ballroom and conservatory, or by demolishing others. The farm looks much as it''s always done, although the current owner has done a great deal of work to the flower beds. In the seventeenth century, the estate was owned by a very wealthy man called Sir Edward Downes. His intention was to escape from the world of politics, after years as an active politician, and to build a new house worthy of his big collection of books, paintings and sculptures. He chose to not contact with his former political allies. Instead, he hosted groups of creative and literary people, like painters and poets. Unusually for his time, he didn''t care whether his guests were rich or poor, as long as they had talent. Big houses like Oniton had dozens of servants until the 1920s or 30s, and we''ve tried to show what their working lives were like. Photographs of course don''t give much of an idea, so instead, as you go round the houses, you''ll see volunteers dressed up as nineteenth-century servants, going about their work. They''ll explain what they''re doing, and tell you their recipes, or what tools they''re using. We''ve just started to replace the audio guide we used to have available. I see there are a number of children here with you today. Well, we have several activities specially for children, like dressing up in the sorts of clothes that children wore in the past, and as it''s a really fine day, some of you will probably want to play in the adventure playground. Our latest addition is child-sized tractors, that you can drive around the grounds. We''ll also be going into the farm that''s part of the estate, where there''s plenty to do. Most of the buildings date from the eighteenth century, so you can really step back into an agricultural past. Until recently, the dairy was where milk from the cows was turned into cheese. It''s now the place to go for lunch, or afternoon tea, or just a cup of coffee and a slice of homemade cake. The big stone building that dominates the farm is the large barn, and in here is our collection of agricultural tools. These were used in the past to plough the earth, sow seeds, make gates, and much more. There''s a small barn, also made of stone, where you can groom the donkeys and horses, to keep their coats clean. They really seem to enjoy having it done, and children love grooming them. The horses no longer live in the stables, which instead is the place to go to buy gifts, books, our own jams and pickles, and clothes and blankets made of wool from our sheep. Outside the shed, which is the only brick building, you can climb into a horse-drawn carriage for a lovely, relaxing tour of the park and farm. The carriages are well over a hundred years old. And finally, the parkland, which was laid out in the eighteenth century, with a lake and trees that are now well established. You''ll see types of cattle and sheep, that are hardly ever found on farms these days. We''re helping to preserve them, to stop their numbers failing further. OK, well I''d like you to come with me ...',
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
        'https://jpocdgkrvohmjkejclpl.supabase.co/storage/v1/object/public/IELTS/LISTENING_AUDIOS/IELTS17_TEST2/IELTS17_T2_P3.mp3',
        'ED: Did you make notes while you were watching the performances of Romeo and Juliet, Gemma?
GEMMA: Yes, I did. I found it quite hard though. I kept getting too involved in the play.
ED: Me too. I ended up not taking notes. I wrote down my impressions when I got home. Do you mind if I check a few things with you? In case I''ve missed anything. And I''ve also got some questions about our assignment.
GEMMA: No, it''s good to talk things through. I may have missed things too.
ED: OK great. So first of all, I''m not sure how much information we should include in our reviews.
GEMMA: Right. Well, I don''t think we need to describe what happens. Especially as Romeo and Juliet is one of Shakespeare''s most well-known plays.
ED: Yeah, everyone knows the story. In an essay we''d focus on the poetry and Shakespeare''s use of imagery etc., but that isn''t really relevant in a review. We''re supposed to focus on how effective this particular production is.
GEMMA: Mmm. We should say what made it a success or a failure.
ED: And part of that means talking about the emotional impact the performance had on us. I think that''s important.
GEMMA: Yes. And we should definitely mention how well the director handled important bits of the play – like when Romeo climbs onto Juliet''s balcony.
ED: And the fight between Mercutio and Tybalt.
GEMMA: Yes. It would also be interesting to mention the theatre space and how the director used it but I don''t think we''ll have space in 800 words.
ED: No. OK. That all sounds quite straightforward.
ED: So what about The Emporium Theatre''s production of the play?
GEMMA: I thought some things worked really well but there were some problems too.
ED: Yeah. What about the set, for example?
GEMMA: I think it was visually stunning. I''d say that was probably the most memorable thing about this production.
ED: You''re right. The set design was really amazing, but actually I have seen similar ideas used in other productions.
GEMMA: What about the lighting? Some of the scenes were so dimly lit it was quite hard to see.
ED: I didn''t dislike it. It helped to change the mood of the quieter scenes.
GEMMA: That''s a good point.
ED: What did you think of the costumes?
GEMMA: I was a bit surprised by the contemporary dress. I must say, I was assumed it would be more conventional.
ED: Yeah – I think it worked well, but I had assumed it would be more conventional.
GEMMA: The music – I think the music at the beginning and I thought the musicians were brilliant, but I thought they were wasted because the music didn''t have much impact in Acts 2 and 3.
ED: Yes – that was a shame.
GEMMA: One problem with this production was that the actors didn''t deliver the lines that well. They were speaking too fast.
ED: It was a problem I agree, but I thought it was because they weren''t speaking loudly enough – especially at key points in the play.
GEMMA: I actually didn''t have a problem with that.
ED: It''s been an interesting experience watching different versions of Romeo and Juliet, hasn''t it?
GEMMA: Definitely. It''s made me realise how relevant the play still is.
ED: Right. I mean a lot''s changed since Shakespeare''s time, but in many ways nothing''s changed. There are always disagreements and tension between managers and their parents.
GEMMA: Yes, that''s something all young people can relate to – more than the violence and the extreme emotions in the play.
ED: How did you find watching it in translation?
GEMMA: Really interesting. I expected to find it more challenging, but I could follow the story well.
ED: I stopped worrying about not being able to understand all the words and focused on the actors'' expressions. The ending was pretty powerful.
GEMMA: Yes. That somehow intensified the emotion for me.
ED: Did you know Shakespeare''s been translated into more languages than any other writer?
GEMMA: What''s the reason for his international appeal, do you think?
ED: I was reading that it''s because his plays are about basic themes that people everywhere are familiar with.',
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
        'https://jpocdgkrvohmjkejclpl.supabase.co/storage/v1/object/public/IELTS/LISTENING_AUDIOS/IELTS17_TEST2/IELTS17_T2_P4.mp3',
        'Right, everyone, let''s make a start. Over the past few sessions, we''ve been considering the reasons why some world languages are in decline, and today I''m going to introduce another factor that affects languages, and the speakers of those languages, and that''s technology and, in particular, digital technology. In order to illustrate its effect, I''m going to focus on the Icelandic language, which is spoken by around 321,000 people, most of whom live in Iceland – an island in the North Atlantic Ocean. The problem for this language is not the number of speakers – even though this number is small. Nor is it about losing words to other languages, such as English. In fact, the vocabulary of Icelandic is continually increasing because when speakers need a new word for something, they tend to create one, rather than borrowing from another language. All this makes Icelandic quite a special language – it''s changed very little in the past millennium, yet it can handle twenty-first-century concepts related to the use of computers and digital technology. Take, for example, the word for web browser ... this is vafrir in Icelandic, which comes from the verb ''to wander''. I can''t think of a more appropriate term because that''s exactly what you do mentally when you browse the internet. Then there''s an Icelandic word for podcast – which is too hard to pronounce! And so on. Icelandic, then, is alive and growing, but – and it''s a big but – young Icelanders spend a great deal of time in the digital world and this world is predominantly English. Think about smartphones. They didn''t even exist until comparatively recently, but today young people use them all the time to read books, watch TV or films, play games, listen to music, and so on. Obviously, this is a good thing in many respects because it promotes their bilingual skills, but the extent of the influence of English in the virtual world is staggering and it''s all happening really fast. For their parents and grandparents, the change is less concerning because they already have native-speaker skills in Icelandic. But for young speakers – well, the outcome is a little troubling. For example, teachers have found that playground conversations in Icelandic secondary schools can be conducted entirely in English, while teachers of much younger children have reported situations where their classes find it easier to say what is in a picture using English, rather than Icelandic. The very real and worrying consequence of all this is that the young generation in Iceland is at risk of losing its mother tongue. Of course, this is happening to other European languages too, but while internet companies might be willing to offer, say, French options in their systems, it''s much harder for them to justify the expense of doing the same for a language that has a population the size of a French town, such as Nice. The other drawback is Icelandic is the grammar, which is significantly more complex than in most languages. At the moment, the tech giants are simply not interested in tackling this. So, what is the Icelandic government doing about this? Well, large sums of money are being allocated to a language technology fund that it is hoped will lead to the development of Icelandic-sourced apps and other social media and digital systems, but clearly this is going to be an uphill struggle.',
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
    (section_4_id, 40, 'cam17-t2-l-q40', 'FILL_IN_BLANK', '{"section_title": "The Icelandic government", "text": "• is worried about the consequences of children not being 40 ____ in either Icelandic or English"}', '["fluent"]', 'ONE WORD AND/OR A NUMBER');

END $$;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================
-- Verify the insertion of sections with transcript status
SELECT id, exam_source, test_number, skill, part_number,
       CASE WHEN passage_text IS NOT NULL AND passage_text <> '' THEN 'Present' ELSE 'Empty' END as has_transcript,
       audio_url
FROM public.sections
WHERE exam_source = 'cam17' AND test_number = '2' AND skill = 'listening'
ORDER BY part_number;

-- Verify total question count (should be 40)
SELECT COUNT(*) AS total_questions_count
FROM public.questions q
WHERE q.section_id IN (
    SELECT id FROM public.sections s
    WHERE s.exam_source = 'cam17' AND s.test_number = '2' AND s.skill = 'listening'
);

-- Verify question number coverage (should be 1-40 without gaps)
SELECT array_agg(question_number ORDER BY question_number) as all_question_numbers,
       COUNT(*) as total_count,
       COUNT(DISTINCT question_number) as unique_count
FROM public.questions q
WHERE q.section_id IN (
    SELECT id FROM public.sections s
    WHERE s.exam_source = 'cam17' AND s.test_number = '2' AND s.skill = 'listening'
);

