-- This file contains the complete and accurate SQL data for Cambridge IELTS 17, Test 1, Listening.
-- It has been regenerated based on the source PDF to ensure data accuracy.

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
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '1', 'listening', 1,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-1-listening-part-1.mp3',
        'PETER: Hello?
JAN: Oh hello. My name''s Jan. Are you the right person to talk to about the Buckworth Conservation Group?
PETER: Yes, I''m Peter. I''m the secretary.
JAN: Good. I''ve just moved to this area, and I''m interested in getting involved. I was in a similar group where I used to live. Could you tell me something about your activities, please?
PETER: Of course. Well, we have a mixture of regular activities and special events. One of the regular ones is trying to keep the beach free of litter. A few of us spend a couple of hours a month on it, and it''s awful how much there is to clear. I wish people would be more responsible and take it home with them.
JAN: I totally agree. I''d be happy to help with that. Is it OK to take dogs?
PETER: I''m afraid not, as they''re banned from the beach itself. You can take them along the cliffs, though. And children are welcome.
JAN: Right.
PETER: We also manage a nature reserve, and there''s a lot to do there all year round. For example, because it''s a popular place to visit, we spend a lot of time looking after the paths and making sure they''re in good condition for walking.
JAN: I could certainly help with that.
PETER: Good. And we have a programme of creating new habitats there. We''ve just finished making and installing nesting boxes for birds to use, and next we''re going to work on encouraging insects – they''re important for the biodiversity of the reserve.
JAN: They certainly are.
PETER: Oh, and we''re also running a project to identify the different species of butterflies that visit the reserve. You might be interested in taking part in that.
JAN: Sure. I was involved in something similar where I used to live, counting all the species of moths. I''d enjoy that.
PETER: Another job we''re doing at the reserve is replacing the wall on the southern side, between the parking area and our woodshed. It was badly damaged in a storm last month.
JAN: OK.
PETER: Then as I said, we have a programme of events as well, both at the weekend, and during the week.
JAN: Right. I presume you have guided walks? I''d like to get to know the local countryside, as I''m new to the area.
PETER: Yes, we do. The next walk is to Ruston Island, a week on Saturday. We''ll be meeting in the car park at Dunsmore Beach at low tide – that''s when the sands are dry enough for us to walk to the island without getting wet.
JAN: Sounds good.',
        '{ "blocks": [ { "block_type": "NOTE_COMPLETION", "content": { "title": "Questions 1-10", "instructions_text": "Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.", "main_title": "Buckworth Conservation Group" }, "question_numbers": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] } ] }'::jsonb
    )
    RETURNING id INTO section_1_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit) VALUES
    (section_1_id, 1, 'cam17-t1-l-q1', 'FILL_IN_BLANK', '{"section_title": "Beach", "text": "• making sure the beach does not have 1 ____ on it"}', '["litter"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 2, 'cam17-t1-l-q2', 'FILL_IN_BLANK', '{"section_title": "Beach", "text": "• no 2 ____"}', '["dogs"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 3, 'cam17-t1-l-q3', 'FILL_IN_BLANK', '{"section_title": "Nature reserve", "text": "• maintaining paths<br/>• nesting boxes for birds installed<br/>• next task is taking action to attract 3 ____ to the place"}', '["insects"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 4, 'cam17-t1-l-q4', 'FILL_IN_BLANK', '{"section_title": "Nature reserve", "text": "• identifying types of 4 ____"}', '["butterflies"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 5, 'cam17-t1-l-q5', 'FILL_IN_BLANK', '{"section_title": "Nature reserve", "text": "• building a new 5 ____"}', '["wall"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 6, 'cam17-t1-l-q6', 'FILL_IN_BLANK', '{"section_title": "Forthcoming events", "text": "• walk across the sands and reach the 6 ____"}', '["island"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 7, 'cam17-t1-l-q7', 'FILL_IN_BLANK', '{"section_title": "Forthcoming events", "text": "• wear appropriate 7 ____"}', '["boots"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 8, 'cam17-t1-l-q8', 'FILL_IN_BLANK', '{"section_title": "Woodwork session", "text": "• suitable for 8 ____ to participate in"}', '["beginners"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 9, 'cam17-t1-l-q9', 'FILL_IN_BLANK', '{"section_title": "Woodwork session", "text": "• making 9 ____ out of wood"}', '["spoons"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 10, 'cam17-t1-l-q10', 'FILL_IN_BLANK', '{"section_title": "Woodwork session", "text": "• cost of session (no camping): 10 £____"}', '["35"]', 'ONE WORD AND/OR A NUMBER');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 1, LISTENING PART 2
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '1', 'listening', 2,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-1-listening-part-2.mp3',
        'So, hello everyone. My name''s Lou Miller and I''m going to be your tour guide today as we take this fantastic boat trip around the Tasmanian coast. Before we set off, I just want to tell you a few things about our journey. Our boats aren''t huge as you can see. We already have three staff members on board and on top of that, we can transport a further fifteen people - that''s you – around the coastline. But please note if there are more than nine people on either side of the boat, we''ll move some of you over, otherwise all eighteen of us will end up in the sea! We''ve recently upgraded all our boats. They used to be jet black, but our new ones now have these comfortable dark red seats and a light-green exterior in order to stand out from others and help promote our company. This gives our boats a rather unique appearance, don''t you think? We offer you a free lunchbox during the trip and we have three types. Lunchbox 1 contains ham and tomato sandwiches. Lunchbox 2 contains a cheddar cheese roll and Lunchbox 3 is salad-based and also contains eggs and tuna. All three lunchboxes also have a packet of crisps and chocolate bar inside. Please let staff know which lunchbox you prefer. I''m sure I don''t have to ask you not to throw anything into the sea. We don''t have any bins to put litter in, but Jess, myself or Ray, our other guide, will collect it from you after lunch and put it all in a large plastic sack. The engine on the boat makes quite a lot of noise so before we head off, let me tell you a few things about what you''re going to see. This area is famous for its ancient lighthouse, which you''ll see from the boat as we turn past the first little island. It was built in 1838 to protect sailors as a number of shipwrecks had led to significant loss of life. The construction itself was complicated as some of the original drawings kept by the local council show. It sits right on top of the cliffs in a very isolated spot. In the nineteenth century there were many jobs there, such as polishing the brass lamps, chopping firewood and cleaning windows, that kept lighthouse keepers busy. These workers were mainly prison convicts until the middle of that century when ordinary families willing to live in such circumstances took over. Some of you have asked me what creatures we can expect to see. I know everyone loves the penguins, but they''re very shy and, unfortunately, tend to hide from passing boats, but you might see birds in the distance, such as sea eagles, flying around the cliff edges where they nest. When we get to the rocky area inhabited by fur seals, we''ll stop and watch them swimming around the coast. They''re inquisitive creatures so don''t be surprised if one pops up right in front of you. Their predators, orca whales, hunt along the coastline too, but spotting one of these is rare. Dolphins, on the other hand, can sometimes approach on their own or in groups as they ride the waves beside us. Lastly, I want to mention the caves. Tasmania is famous for its caves and the ones we''ll pass by are so amazing that people are lost for words when they see them. They can only be approached by sea, but if you feel that you want to see more than we''re able to show you, then you can take a kayak into the area on another day and one of our staff will give you more information on that. What we''ll do is to go through a narrow channel, past some incredible rock formations and from there we''ll be able to see the openings to the caves, and at that point we''ll talk to you about what lies beyond.',
        '{ "blocks": [ { "block_type": "INSTRUCTIONS_ONLY", "content": { "title": "Questions 11-14", "instructions_text": "<b>Boat trip round Tasmania</b><br/><br/>Choose the correct letter, A, B or C." }, "question_numbers": [11, 12, 13, 14] }, { "block_type": "INSTRUCTIONS_ONLY", "content": { "title": "Questions 15 and 16", "instructions_text": "Choose TWO letters, A–E." }, "question_numbers": [15, 16] }, { "block_type": "INSTRUCTIONS_ONLY", "content": { "title": "Questions 17 and 18", "instructions_text": "Choose TWO letters, A–E." }, "question_numbers": [17, 18] }, { "block_type": "INSTRUCTIONS_ONLY", "content": { "title": "Questions 19 and 20", "instructions_text": "Choose TWO letters, A–E." }, "question_numbers": [19, 20] } ] }'::jsonb
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
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '1', 'listening', 3,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-1-listening-part-3.mp3',
        'DIANA: So, Tim, we have to do a short summary of our work experience on a farm.
TIM: Right. My farm was great, but arranging the work experience was hard. One problem was it was miles away and I don''t drive. And also, I''d really wanted a placement for a month, but I could only get one for two weeks.
DIANA: I was lucky, the farmer let me stay on the farm so I didn''t have to travel. But finding the right sort of farm to apply to wasn''t easy.
TIM: No, they don''t seem to have websites, do they. I found mine through a friend of my mother''s, but it wasn''t easy.
DIANA: No.
TIM: My farm was mostly livestock, especially sheep. I really enjoyed helping out with them. I was up most of one night helping a sheep deliver a lamb ...
DIANA: On your own?
TIM: No, the farmer was there, and he told me what to do. It wasn''t a straightforward birth, but I managed. It was a great feeling to see the lamb stagger to its feet and start feeding almost straightaway, and to know that it was OK.
DIANA: Mm.
TIM: Then another time a lamb had broken its leg, and they got the vet in to set it, and he talked me through what he was doing. That was really useful.
DIANA: Yes, my farm had sheep too. The farm was in a valley and they had a lowland breed called Suffolks, although the farmer said they''d had other breeds in the past.
TIM: So were they bred for their meat?
DIANA: Mostly, yes. They''re quite big and solid.
TIM: My farm was up in the hills and they had a different breed of sheep, they were Cheviots.
DIANA: Oh, I heard their wool''s really sought after.
TIM: Yes. It''s very hardwearing and they use it for carpets.
DIANA: Right.
TIM: I was interested in the amount of supplements they add to animals'' feed nowadays. Like, even the chickens got extra vitamins and electrolytes in their feed.
DIANA: Yes, I found that too. And they''re not cheap. But my farmer said some are overpriced for what they are. And he didn''t give them as a matter of routine, just at times when the chickens seemed to particularly require them.
TIM: Yes, mine said the same. He said certain breeds of chickens might need more supplements than the others, but the cheap and expensive ones are all basically the same.
DIANA: Mm.
TIM: So did your farm have any other livestock, Diana?
DIANA: Yes, dairy cows. I made a really embarrassing mistake when I was working in the milk shed. Some cows had been treated with antibiotics, so their milk wasn''t suitable for human consumption, and it had to be put in a separate container. But I got mixed up, and I poured some milk from the wrong cow in with the milk for humans, so the whole lot had to be thrown away. The farmer wasn''t too happy with me.
TIM: I asked my farmer how much he depended on the vet to deal with health problems. I''d read reports that the livestock''s health is being affected as farmers are under pressure to increase production. Well, he didn''t agree with that, but he said that actually some of the stuff the vets do, like minor operations, he''d be quite capable of doing himself.
DIANA: Yeah. My farmer said the same. But he reckons vets'' skills are still needed.
DIANA: Now we''ve got to give a bit of feedback about last term''s modules – just short comments, apparently. Shall we do that now?
TIM: OK. So medical terminology.
DIANA: Well, my heart sank when I saw that, especially right at the beginning of the course. And I did struggle with it.
TIM: I''d thought it''d be hard, but actually I found it all quite straightforward. What did you think about diet and nutrition?
DIANA: OK, I suppose.
TIM: Do you remember what they told us about pet food and the fact that there''s such limited checking into whether or not it''s contaminated? I mean in comparison with the checks on food for humans – I thought that was terrible.
DIANA: Mm. I think the module that really impressed me was the animal disease one, when we looked at domesticated animals in different parts of the world, like camels and water buffalo and alpaca. The economies of so many countries depend on these, but scientists don''t know much about the diseases that affect them.
TIM: Yes, I thought they''d know a lot about ways of controlling and eradicating those diseases, but that''s not the case at all.
DIANA: I loved the wildlife medication unit. Things like helping birds that have been caught in oil spills. That''s something I hadn''t thought about before.
TIM: Yeah, I thought I might write my dissertation on something connected with that.
DIANA: Right. So ...',
        '{ "blocks": [ { "block_type": "INSTRUCTIONS_ONLY", "content": { "title": "Questions 21-26", "instructions_text": "<b>Work experience for veterinary science students</b><br/><br/>Choose the correct letter, A, B or C." }, "question_numbers": [21, 22, 23, 24, 25, 26] }, { "block_type": "MATCHING_FEATURES", "content": { "title": "Questions 27-30", "instructions_text": "What opinion do the students give about each of the following modules on their veterinary science course?\n\nChoose FOUR answers from the box and write the correct letter, A–F, next to questions 27–30.", "options_title": "Opinions", "options": [ {"letter": "A", "text": "Tim found this easier than expected."}, {"letter": "B", "text": "Tim thought this was not very clearly organised."}, {"letter": "C", "text": "Diana may do some further study on this."}, {"letter": "D", "text": "They both found the reading required for this was difficult."}, {"letter": "E", "text": "Tim was shocked at something he learned on this module."}, {"letter": "F", "text": "They were both surprised how little is known about some aspects of this."} ] }, "question_numbers": [27, 28, 29, 30] } ] }'::jsonb
    )
    RETURNING id INTO section_3_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer) VALUES
    (section_3_id, 21, 'cam17-t1-l-q21', 'MULTIPLE_CHOICE', '{"text": "What problem did both Diana and Tim have when arranging their work experience?", "options": ["A making initial contact with suitable farms", "B organising transport to and from the farm", "C finding a placement for the required length of time"]}', '["A"]'),
    (section_3_id, 22, 'cam17-t1-l-q22', 'MULTIPLE_CHOICE', '{"text": "Tim was pleased to be able to help", "options": ["A a lamb that had a broken leg.", "B a sheep that was having difficulty giving birth.", "C a newly born lamb that was having trouble feeding."]}', '["B"]'),
    (section_3_id, 23, 'cam17-t1-l-q23', 'MULTIPLE_CHOICE', '{"text": "Diana says the sheep on her farm", "options": ["A were of various different varieties.", "B were mainly reared for their meat.", "C had better quality wool than sheep on the hills."]}', '["B"]'),
    (section_3_id, 24, 'cam17-t1-l-q24', 'MULTIPLE_CHOICE', '{"text": "What did the students learn about adding supplements to chicken feed?", "options": ["A These should only be given if specially needed.", "B It is worth paying extra for the most effective ones.", "C The amount given at one time should be limited."]}', '["A"]'),
    (section_3_id, 25, 'cam17-t1-l-q25', 'MULTIPLE_CHOICE', '{"text": "What happened when Diana was working with dairy cows?", "options": ["A She identified some cows incorrectly.", "B She accidentally threw some milk away.", "C She made a mistake when storing milk."]}', '["C"]'),
    (section_3_id, 26, 'cam17-t1-l-q26', 'MULTIPLE_CHOICE', '{"text": "What did both farmers mention about vets and farming?", "options": ["A Vets are failing to cope with some aspects of animal health.", "B There needs to be a fundamental change in the training of vets.", "C Some jobs could be done by the farmer rather than by a vet."]}', '["C"]'),
    (section_3_id, 27, 'cam17-t1-l-q27', 'MATCHING', '{"text": "Medical terminology"}', '["A"]'),
    (section_3_id, 28, 'cam17-t1-l-q28', 'MATCHING', '{"text": "Diet and nutrition"}', '["E"]'),
    (section_3_id, 29, 'cam17-t1-l-q29', 'MATCHING', '{"text": "Animal disease"}', '["F"]'),
    (section_3_id, 30, 'cam17-t1-l-q30', 'MATCHING', '{"text": "Wildlife medication"}', '["C"]');

    -- =================================================================
    -- == CAMBRIDGE 17, TEST 1, LISTENING PART 4
    -- =================================================================
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '1', 'listening', 4,
        'https://cramer-media.s3.ap-southeast-1.amazonaws.com/cambridge-17-test-1-listening-part-4.mp3',
        'Labyrinths have existed for well over 4,000 years. Labyrinths and labyrinthine symbols have been found in regions as diverse as modern-day Turkey, Ireland, Greece, and India. There are various designs of labyrinth but what they all have in common is a winding spiral path which leads to a central area. There is one starting point at the entrance and the goal is to reach the central area. Finding your way through a labyrinth involves many twists and turns, but it''s not possible to get lost as there is only one single path. In modern times, the word labyrinth has taken on a different meaning and is often used as a synonym for a maze. A maze is quite different as it is a kind of puzzle with an intricate network of paths. Mazes became fashionable in the 15th and 16th centuries in Europe, and can still be found in the gardens of great houses and palaces. The paths are usually surrounded by thick, high hedges so that it''s not possible to see over them. Entering a maze usually involves getting lost a few times before using logic to work out the pattern and find your way to the centre and then out again. There are lots of dead ends and paths which lead you back to where you started. The word ''maze'' is believed to come from a Scandinavian word for a state of confusion. This is where the word ''amazing'' comes from. Labyrinths, on the other hand, have a very different function. Although people now often refer to things they find complicated as labyrinths, this is not how they were seen in the past. The winding spiral of the labyrinth has been used for centuries as a metaphor for life''s journey. It served as a spiritual reminder that there is purpose and meaning to our lives and helped to give people a sense of direction. Labyrinths are thought to encourage a feeling of calm and have been used as a meditation and prayer tool in many cultures over many centuries. The earliest examples of the labyrinth spiral pattern have been found carved into stone, from Sardinia to Scandinavia, from Arizona to India to Africa. In Europe, these spiral carvings date from the late Bronze Age. The Native American Pima tribe wove baskets with a circular labyrinth design that depicted their own cosmology. In Ancient Greece, the labyrinth spiral was used on coins around four thousand years ago. Labyrinths made of mosaics were commonly found in bathhouses, villas and tombs throughout the Roman Empire. In Northern Europe, there were actual physical labyrinths designed for walking on. These were cut into the turf or grass, usually in a circular pattern. The origin of these walking labyrinths remains unclear, but they were probably used for fertility rites which may date back thousands of years. Eleven examples of turf labyrinths survive today, including the largest one at Saffron Walden, England, which used to have a large tree in the middle of it. More recently labyrinths have experienced something of a revival. Some believe that walking a labyrinth promotes healing and mindfulness, and there are those who believe in its emotional and physical benefits, which include slower breathing and a restored sense of balance and perspective. This idea has become so popular that labyrinths have been laid into the floors of spas, wellness centres and even prisons in recent years. A pamphlet at Colorado Children''s Hospital informs patients that ''walking a labyrinth can often calm people in the midst of a crisis''. And apparently, it''s not only patients who benefit. Many visitors find walking a labyrinth less stressful than sitting in a corridor or waiting room. Some doctors even walk the labyrinth during their breaks. In some hospitals, patients who can''t walk can have a paper ''finger labyrinth'' brought to their bed. The science behind the theory is a little sketchy, but there are dozens of small-scale studies which support claims about the benefits of labyrinths. For example, one study found that walking a labyrinth provided ''short-term calming, relaxation, and relief from anxiety'' for Alzheimer''s patients.',
        '{ "blocks": [ { "block_type": "NOTE_COMPLETION", "content": { "title": "Questions 31-40", "instructions_text": "<b>Definition</b><br/>• a winding spiral path leading to a central area<br/><br/>Complete the notes below. Write ONE WORD ONLY for each answer.", "main_title": "Labyrinths" }, "question_numbers": [31, 32, 33, 34, 35, 36, 37, 38, 39, 40] } ] }'::jsonb
    )
    RETURNING id INTO section_4_id;

    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit) VALUES
    (section_4_id, 31, 'cam17-t1-l-q31', 'FILL_IN_BLANK', '{"section_title": "Labyrinths compared with mazes", "text": "• Mazes are a type of 31 ____"}', '["puzzle"]', 'ONE WORD ONLY'),
    (section_4_id, 32, 'cam17-t1-l-q32', 'FILL_IN_BLANK', '{"section_title": "Labyrinths compared with mazes", "text": "– 32 ____ is needed to navigate through a maze"}', '["logic"]', 'ONE WORD ONLY'),
    (section_4_id, 33, 'cam17-t1-l-q33', 'FILL_IN_BLANK', '{"section_title": "Labyrinths compared with mazes", "text": "– the word ''maze'' is derived from a word meaning a feeling of 33 ____"}', '["confusion"]', 'ONE WORD ONLY'),
    (section_4_id, 34, 'cam17-t1-l-q34', 'FILL_IN_BLANK', '{"section_title": "Labyrinths compared with mazes", "text": "• Labyrinths represent a journey through life<br/>– they have frequently been used in 34 ____ and prayer"}', '["meditation"]', 'ONE WORD ONLY'),
    (section_4_id, 35, 'cam17-t1-l-q35', 'FILL_IN_BLANK', '{"section_title": "Early examples of the labyrinth spiral", "text": "• Ancient carvings on 35 ____ have been found across many cultures"}', '["stone"]', 'ONE WORD ONLY'),
    (section_4_id, 36, 'cam17-t1-l-q36', 'FILL_IN_BLANK', '{"section_title": "Early examples of the labyrinth spiral", "text": "• The Pima, a Native American tribe, wove the symbol on baskets<br/>• Ancient Greeks used the symbol on 36 ____"}', '["coins"]', 'ONE WORD ONLY'),
    (section_4_id, 37, 'cam17-t1-l-q37', 'FILL_IN_BLANK', '{"section_title": "Walking labyrinths", "text": "• The largest surviving example of a turf labyrinth once had a big 37 ____ at its centre"}', '["tree"]', 'ONE WORD ONLY'),
    (section_4_id, 38, 'cam17-t1-l-q38', 'FILL_IN_BLANK', '{"section_title": "Labyrinths nowadays", "text": "• Believed to have a beneficial impact on mental and physical health,<br/>e.g., walking a maze can reduce a person''s 38 ____ rate"}', '["breathing"]', 'ONE WORD ONLY'),
    (section_4_id, 39, 'cam17-t1-l-q39', 'FILL_IN_BLANK', '{"section_title": "Labyrinths nowadays", "text": "• Used in medical and health and fitness settings and also prisons<br/>• Popular with patients, visitors and staff in hospitals<br/>– patients who can''t walk can use ''finger labyrinths'' made from 39 ____"}', '["paper"]', 'ONE WORD ONLY'),
    (section_4_id, 40, 'cam17-t1-l-q40', 'FILL_IN_BLANK', '{"section_title": "Labyrinths nowadays", "text": "– research has shown that Alzheimer''s sufferers experience less 40 ____"}', '["anxiety"]', 'ONE WORD ONLY');

END $$