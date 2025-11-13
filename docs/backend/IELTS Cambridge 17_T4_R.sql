-- This file contains the complete and accurate SQL data for Cambridge IELTS 17, Test 4.
-- It follows the corrected structure for contextual summary and note completion questions.

-- =================================================================
-- == CAMBRIDGE 17, TEST 4, READING PASSAGE 1
-- =================================================================
DO $$
DECLARE
    section_1_id bigint;
BEGIN
    -- First, delete existing data for Cam17, Test 4, Reading
    DELETE FROM public.questions WHERE section_id IN (SELECT id FROM public.sections WHERE exam_source = 'cam17' AND test_number = '4' AND skill = 'reading');
    DELETE FROM public.sections WHERE exam_source = 'cam17' AND test_number = '4' AND skill = 'reading';

    -- Insert Passage 1 and capture its ID
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text)
    VALUES (
        'cam17',
        '4',
        'reading',
        1,
        '<strong>Bats to the rescue</strong>
How Madagascar''s bats are helping to save the rainforest

There are few places in the world where relations between agriculture and conservation are more strained. Madagascar''s forests are being converted to agricultural land at a rate of one percent every year. Much of this destruction is fuelled by the cultivation of the country''s main staple crop: rice. And a key reason for this destruction is that insect pests are destroying vast quantities of what is grown by local subsistence farmers, leading them to clear forest to create new paddy fields. The result is devastating habitat and biodiversity loss on the island, but not all species are suffering. In fact, some of the island''s insectivorous bats are currently thriving and this has important implications for farmers and conservationists alike.

Enter University of Cambridge zoologist Ricardo Rocha. He''s passionate about conservation, and bats. More specifically, he''s interested in how bats are responding to human activity and deforestation in particular. Rocha''s new study shows that several species of bats are giving Madagascar''s rice farmers a vital pest control service by feasting on plagues of insects. And this, he believes, can ease the financial pressure on farmers to turn forest into fields.

Bats comprise roughly one-fifth of all mammal species in Madagascar and thirty-six recorded bat species are native to the island, making it one of the most important regions for conservation of this animal group anywhere in the world.

Co-leading an international team of scientists, Rocha found that several species of indigenous bats are taking advantage of habitat modification to hunt insects swarming above the country''s rice fields. They include the Malagasy mouse-eared bat, Major''s long-fingered bat, the Malagasy white-bellied free-tailed bat and Peters'' wrinkle-lipped bat.

''These winner species are providing a valuable free service to Madagascar as biological pest suppressors,'' says Rocha. ‘We found that six species of bat are preying on rice pests, including the paddy swarming caterpillar and grass webworm. The damage which these insects cause puts the island''s farmers under huge financial pressure and that encourages deforestation.''

The study, now published in the journal Agriculture, Ecosystems and Environment, set out to investigate the feeding activity of insectivorous bats in the farmland bordering the Ranomafana National Park in the southeast of the country.

Rocha and his team used state-of-the-art ultrasonic recorders to record over a thousand bat ''feeding buzzes'' (echolocation sequences used by bats to target their prey) at 54 sites, in order to identify the favourite feeding spots of the bats. They next used DNA barcoding techniques to analyse droppings collected from bats at the different sites.

The recordings revealed that bat activity over rice fields was much higher than it was in continuous forest seven times higher over rice fields which were on flat ground, and sixteen times higher over fields on the sides of hills – leaving no doubt that the animals are preferentially foraging in these man-made ecosystems. The researchers suggest that the bats favour these fields because lack of water and nutrient run-off make these crops more susceptible to insect pest infestations. DNA analysis showed that all six species of bat had fed on economically important insect pests. While the findings indicated that rice farming benefits most from the bats, the scientists also found indications that the bats were consuming pests of other crops, including the black twig borer (which infests coffee plants), the sugarcane cicada, the macadamia nut-borer, and the sober tabby (a pest of citrus fruits).

''The effectiveness of bats as pest controllers has already been proven in the USA and Catalonia,'' said co-author James Kemp, from the University of Lisbon. ‘But our study is the first to show this happening in Madagascar, where the stakes for both farmers and conservationists are so high.''

Local people may have a further reason to be grateful to their bats. While the animal is often associated with spreading disease, Rocha and his team found evidence that Malagasy bats feed not just on crop pests but also on mosquitoes – carriers of malaria, Rift Valley fever virus and elephantiasis – as well as blackflies, which spread river blindness.

Rocha points out that the relationship is complicated. When food is scarce, bats become a crucial source of protein for local people. Even the children will hunt them. And as well as roosting in trees, the bats sometimes roost in buildings, but are not welcomed there because they make them unclean. At the same time, however, they are associated with sacred caves and the ancestors, so they can be viewed as beings between worlds, which makes them very significant in the culture of the people. And one potential problem is that while these bats are benefiting from farming, at the same time deforestation is reducing the places where they can roost, which could have long-term effects on their numbers. Rocha says, ‘With the right help, we hope that farmers can promote this mutually beneficial relationship by installing bat houses.''

Rocha and his colleagues believe that maximising bat populations can help to boost crop yields and promote sustainable livelihoods. The team is now calling for further research to quantify this contribution. ''I''m very optimistic,'' says Rocha. ‘If we give nature a hand, we can speed up the process of regeneration.''
    )
    RETURNING id INTO section_1_id;

    -- Insert Questions for Passage 1
    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit)
    VALUES
        -- TRUE_FALSE_NOT_GIVEN (Questions 1-6)
        (section_1_id, 1, 'cam17-t4-r-q1', 'TRUE_FALSE_NOT_GIVEN', '{"text": "Many Madagascan forests are being destroyed by attacks from insects."}', '["FALSE"]', NULL),
        (section_1_id, 2, 'cam17-t4-r-q2', 'TRUE_FALSE_NOT_GIVEN', '{"text": "Loss of habitat has badly affected insectivorous bats in Madagascar."}', '["FALSE"]', NULL),
        (section_1_id, 3, 'cam17-t4-r-q3', 'TRUE_FALSE_NOT_GIVEN', '{"text": "Ricardo Rocha has carried out studies of bats in different parts of the world."}', '["NOT GIVEN"]', NULL),
        (section_1_id, 4, 'cam17-t4-r-q4', 'TRUE_FALSE_NOT_GIVEN', '{"text": "Habitat modification has resulted in indigenous bats in Madagascar becoming useful to farmers."}', '["TRUE"]', NULL),
        (section_1_id, 5, 'cam17-t4-r-q5', 'TRUE_FALSE_NOT_GIVEN', '{"text": "The Malagasy mouse-eared bat is more common than other indigenous bat species in Madagascar."}', '["NOT GIVEN"]', NULL),
        (section_1_id, 6, 'cam17-t4-r-q6', 'TRUE_FALSE_NOT_GIVEN', '{"text": "Bats may feed on paddy swarming caterpillars and grass webworms."}', '["TRUE"]', NULL),

        -- TABLE_COMPLETION (Questions 7-13)
        (section_1_id, 7, 'cam17-t4-r-q7', 'TABLE_COMPLETION', '{"text": "<table class=\"question-table\"><thead><tr><th></th><th>The study carried out by Rocha''s team</th></tr></thead><tbody><tr><td><strong>Aim</strong></td><td>- to investigate the feeding habits of bats in farmland near the Ranomafana National Park</td></tr><tr><td><strong>Method</strong></td><td>- ultrasonic recording to identify favourite feeding spots<br/>- DNA analysis of bat <strong>7</strong> ____</td></tr><tr><td rowspan=\"3\"><strong>Findings</strong></td><td>- the bats<br/>  - were most active in rice fields located on hills<br/>  - ate pests of rice, <strong>8</strong> ____, sugarcane, nuts and fruit<br/>  - prevent the spread of disease by eating <strong>9</strong> ____ and blackflies</td></tr><tr><td>- local attitudes to bats are mixed:<br/>  - they provide food rich in <strong>10</strong> ____<br/>  - the buildings where they roost become <strong>11</strong> ____<br/>  - they play an important role in local <strong>12</strong> ____</td></tr><tr></tr><tr><td><strong>Recommendation</strong></td><td>- farmers should provide special <strong>13</strong> ____ to support the bat population</td></tr></tbody></table>"}', '["droppings"]', 'ONE WORD ONLY'),
        (section_1_id, 8, 'cam17-t4-r-q8', 'TABLE_COMPLETION', '{}', '["coffee"]', 'ONE WORD ONLY'),
        (section_1_id, 9, 'cam17-t4-r-q9', 'TABLE_COMPLETION', '{}', '["mosquitoes"]', 'ONE WORD ONLY'),
        (section_1_id, 10, 'cam17-t4-r-q10', 'TABLE_COMPLETION', '{}', '["protein"]', 'ONE WORD ONLY'),
        (section_1_id, 11, 'cam17-t4-r-q11', 'TABLE_COMPLETION', '{}', '["unclean"]', 'ONE WORD ONLY'),
        (section_1_id, 12, 'cam17-t4-r-q12', 'TABLE_COMPLETION', '{}', '["culture"]', 'ONE WORD ONLY'),
        (section_1_id, 13, 'cam17-t4-r-q13', 'TABLE_COMPLETION', '{}', '["houses"]', 'ONE WORD ONLY');
END $$;

-- =================================================================
-- == CAMBRIDGE 17, TEST 4, READING PASSAGE 2
-- =================================================================
DO $$
DECLARE
    section_2_id bigint;
BEGIN
    -- Insert Passage 2 and capture its ID
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text)
    VALUES (
        'cam17',
        '4',
        'reading',
        2,
        '<strong>Does education fuel economic growth?</strong>

<strong>A</strong> Over the last decade, a huge database about the lives of southwest German villagers between 1600 and 1900 has been compiled by a team led by Professor Sheilagh Ogilvie at Cambridge University''s Faculty of Economics. It includes court records, guild ledgers, parish registers, village censuses, tax lists and – the most recent addition – 9,000 handwritten inventories listing over a million personal possessions belonging to ordinary women and men across three centuries. Ogilvie, who discovered the inventories in the archives of two German communities 30 years ago, believes they may hold the answer to a conundrum that has long puzzled economists: the lack of evidence for a causal link between education and a country''s economic growth.

<strong>B</strong> As Ogilvie explains, ''Education helps us to work more productively, invent better technology, and earn more – surely it must be critical for economic growth? But, if you look back through history, there''s no evidence that having a high literacy rate made a country industrialise earlier.'' Between 1600 and 1900, England had only mediocre literacy rates by European standards, yet its economy grew fast and it was the first country to industrialise. During this period, Germany and Scandinavia had excellent literacy rates, but their economies grew slowly and they industrialised late. ‘Modern cross-country analyses have also struggled to find evidence that education causes economic growth, even though there is plenty of evidence that growth increases education,” she adds.

<strong>C</strong> In the handwritten inventories that Ogilvie is analysing are the belongings of women and men at marriage, remarriage and death. From badger skins to Bibles, sewing machines to scarlet bodices – the villagers'' entire worldly goods are included. Inventories of agricultural equipment and craft tools reveal economic activities; ownership of books and education-related objects like pens and slates suggests how people learned. In addition, the tax lists included in the database record the value of farms, workshops, assets and debts; signatures and people''s estimates of their age indicate literacy and numeracy levels; and court records reveal obstacles (such as the activities of the guilds*) that stifled industry.

<strong>D</strong> Ogilvie and her team have been building the vast database of material possessions on top of their full demographic reconstruction of the people who lived in these two German communities. ''We can follow the same people – and their descendants – across 300 years of educational and economic change,'' she says. Individual lives have unfolded before their eyes. Stories like that of the 24-year-olds Ana Regina and Magdalena Riethmüllerin, who were chastised in 1707 for reading books in church instead of listening to the sermon. ‘This tells us they were continuing to develop their reading skills at least a decade after leaving school,'' explains Ogilvie. The database also reveals the case of Juliana Schweickherdt, a 50-year-old spinster living in the small Black Forest community of Wildberg, who was reprimanded in 1752 by the local weavers'' guild for ‘weaving cloth and combing wool, counter to the guild ordinance''. When Juliana continued taking jobs reserved for male guild members, she was summoned before the guild court and told to pay a fine equivalent to one third of a servant''s annual wage. It was a small act of defiance by today''s standards, but it reflects a time when laws in Germany and elsewhere regulated people''s access to labour markets. The dominance of guilds not only prevented people from using their skills, but also held back even the simplest industrial innovation.

<strong>E</strong> The data-gathering phase of the project has been completed and now, according to Ogilvie, it is time ''to ask the big questions''. One way to look at whether education causes economic growth is to ''hold wealth constant''. This involves following the lives of different people with the same level of wealth over a period of time. If wealth is constant, it is possible to discover whether education was, for example, linked to the cultivation of new crops, or to the adoption of industrial innovations like sewing machines. The team will also ask what aspect of education helped people engage more with productive and innovative activities. Was it, for instance, literacy, numeracy, book ownership, years of schooling? Was there a threshold level – a tipping point – that needed to be reached to affect economic performance?

<strong>F</strong> Ogilvie hopes to start finding answers to these questions over the next few years. One thing is already clear, she says: the relationship between education and economic growth is far from straightforward. ‘German-speaking central Europe is an excellent laboratory for testing theories of economic growth,'' she explains. Between 1600 and 1900, literacy rates and book ownership were high and yet the region remained poor. It was also the case that local guilds and merchant associations were extremely powerful and legislated against anything that undermined their monopolies. In villages throughout the region, guilds blocked labour migration and resisted changes that might reduce their influence. ''Early findings suggest that the potential benefits of education for the economy can be held back by other barriers, and this has implications for today,'' says Ogilvie. ''Huge amounts are spent improving education in developing countries, but this spending can fail to deliver economic growth if restrictions block people – especially women and the poor – from using their education in economically productive ways. If economic institutions are poorly set up, for instance, education can''t lead to growth.'' 
    )
    RETURNING id INTO section_2_id;

    -- Insert Questions for Passage 2
    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit)
    VALUES
        -- MATCHING_INFORMATION (Questions 14-18)
        (section_2_id, 14, 'cam17-t4-r-q14', 'MATCHING_INFORMATION', '{"text": "an explanation of the need for research to focus on individuals with a fairly consistent income", "options": ["A", "B", "C", "D", "E", "F"]}', '["E"]', NULL),
        (section_2_id, 15, 'cam17-t4-r-q15', 'MATCHING_INFORMATION', '{"text": "examples of the sources the database has been compiled from", "options": ["A", "B", "C", "D", "E", "F"]}', '["A"]', NULL),
        (section_2_id, 16, 'cam17-t4-r-q16', 'MATCHING_INFORMATION', '{"text": "an account of one individual''s refusal to obey an order", "options": ["A", "B", "C", "D", "E", "F"]}', '["D"]', NULL),
        (section_2_id, 17, 'cam17-t4-r-q17', 'MATCHING_INFORMATION', '{"text": "a reference to a region being particularly suited to research into the link between education and economic growth", "options": ["A", "B", "C", "D", "E", "F"]}', '["F"]', NULL),
        (section_2_id, 18, 'cam17-t4-r-q18', 'MATCHING_INFORMATION', '{"text": "examples of the items included in a list of personal possessions", "options": ["A", "B", "C", "D", "E", "F"]}', '["C"]', NULL),

        -- SUMMARY_COMPLETION (Questions 19-22)
        (section_2_id, 19, 'cam17-t4-r-q19', 'SUMMARY_COMPLETION', '{"text": "<strong>Demographic reconstruction of two German communities</strong><br/><br/>The database that Ogilvie and her team has compiled sheds light on the lives of a range of individuals, as well as those of their <strong>19</strong> ____ over a 300-year period."}', '["descendants"]', 'ONE WORD ONLY'),
        (section_2_id, 20, 'cam17-t4-r-q20', 'SUMMARY_COMPLETION', '{"text": "For example, Ana Regina and Magdalena Riethmüllerin were reprimanded for reading while they should have been paying attention to a <strong>20</strong> ____."}', '["sermon"]', 'ONE WORD ONLY'),
        (section_2_id, 21, 'cam17-t4-r-q21', 'SUMMARY_COMPLETION', '{"text": "There was also Juliana Schweickherdt, who came to the notice of the weavers'' guild in the year 1752 for breaking guild rules. As a punishment, she was later given a <strong>21</strong> ____."}', '["fine"]', 'ONE WORD ONLY'),
        (section_2_id, 22, 'cam17-t4-r-q22', 'SUMMARY_COMPLETION', '{"text": "Cases like this illustrate how the guilds could prevent <strong>22</strong> ____ and stop skilled people from working."}', '["innovation"]', 'ONE WORD ONLY'),

        -- MULTIPLE_CHOICE_MULTIPLE_ANSWERS (Questions 23-26)
        (section_2_id, 23, 'cam17-t4-r-q23', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO of the following statements does the writer make about literacy rates in Section B?", "options": ["A Very little research has been done into the link between high literacy rates and improved earnings.", "B Literacy rates in Germany between 1600 and 1900 were very good.", "C There is strong evidence that high literacy rates in the modern world result in economic growth.", "D England is a good example of how high literacy rates helped a country industrialise.", "E Economic growth can help to improve literacy rates."]}', '["B", "E"]', NULL),
        (section_2_id, 24, 'cam17-t4-r-q24', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO of the following statements does the writer make about literacy rates in Section B?", "options": ["A Very little research has been done into the link between high literacy rates and improved earnings.", "B Literacy rates in Germany between 1600 and 1900 were very good.", "C There is strong evidence that high literacy rates in the modern world result in economic growth.", "D England is a good example of how high literacy rates helped a country industrialise.", "E Economic growth can help to improve literacy rates."]}', '["B", "E"]', NULL),
        (section_2_id, 25, 'cam17-t4-r-q25', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO of the following statements does the writer make in Section F about guilds in German-speaking Central Europe between 1600 and 1900?", "options": ["A They helped young people to learn a skill.", "B They were opposed to people moving to an area for work.", "C They kept better records than guilds in other parts of the world.", "D They opposed practices that threatened their control over a trade.", "E They predominantly consisted of wealthy merchants."]}', '["B", "D"]', NULL),
        (section_2_id, 26, 'cam17-t4-r-q26', 'MULTIPLE_CHOICE_MULTIPLE_ANSWERS', '{"text": "Which TWO of the following statements does the writer make in Section F about guilds in German-speaking Central Europe between 1600 and 1900?", "options": ["A They helped young people to learn a skill.", "B They were opposed to people moving to an area for work.", "C They kept better records than guilds in other parts of the world.", "D They opposed practices that threatened their control over a trade.", "E They predominantly consisted of wealthy merchants."]}', '["B", "D"]', NULL);
END $$;

-- =================================================================
-- == CAMBRIDGE 17, TEST 4, READING PASSAGE 3
-- =================================================================
DO $$
DECLARE
    section_3_id bigint;
BEGIN
    -- Insert Passage 3 and capture its ID
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text)
    VALUES (
        'cam17',
        '4',
        'reading',
        3,
        '<strong>Timur Gareyev – blindfold chess champion</strong>

<strong>A</strong> Next month, a chess player named Timur Gareyev will take on nearly 50 opponents at once. But that is not the hard part. While his challengers will play the games as normal, Gareyev himself will be blindfolded. Even by world record standards, it sets a high bar for human performance. The 28-year-old already stands out in the rarefied world of blindfold chess. He has a fondness for bright clothes and unusual hairstyles, and he gets his kicks from the adventure sport of BASE jumping. He has already proved himself a strong chess player, too. In a 10-hour chess marathon in 2013, Gareyev played 33 games in his head simultaneously. He won 29 and lost none. The skill has become his brand: he calls himself the Blindfold King.

<strong>B</strong> But Gareyev''s prowess has drawn interest from beyond the chess-playing community. In the hope of understanding how he and others like him can perform such mental feats, researchers at the University of California in Los Angeles (UCLA) called him in for tests. They now have their first results. ‘The ability to play a game of chess with your eyes closed is not a far reach for most accomplished players,'' said Jesse Rissman, who runs a memory lab at UCLA. ‘But the thing that''s so remarkable about Timur and a few other individuals is the number of games they can keep active at once. To me it is simply astonishing.''

<strong>C</strong> Gareyev learned to play chess in his native Uzbekistan when he was six years old. Tutored by his grandfather, he entered his first tournament aged eight and soon became obsessed with competitions. At 16, he was crowned Asia''s youngest ever chess grandmaster. He moved to the US soon after, and as a student helped his university win its first national chess championship. In 2013, Gareyev was ranked the third best chess player in the US.

<strong>D</strong> To the uninitiated, blindfold chess seems to call for superhuman skill. But displays of the feat go back centuries. The first recorded game in Europe was played in 13th-century Florence. In 1947, the Argentinian grandmaster Miguel Najdorf played 45 simultaneous games in his mind, winning 39 in the 24-hour session.

<strong>E</strong> Accomplished players can develop the skill of playing blind even without realising it. The nature of the game is to run through possible moves in the mind to see how they play out. From this, regular players develop a memory for the patterns the pieces make, the defences and attacks. ''You recreate it in your mind,'' said Gareyev. ''A lot of players are capable of doing what I''m doing.'' The real mental challenge comes from playing multiple games at once in the head. Not only must the positions of each piece on every board be memorised, they must be recalled faithfully when needed, updated with each player''s moves, and then reliably stored again, so the brain can move on to the next board. First moves can be tough to remember because they are fairly uninteresting. But the ends of games are taxing too, as exhaustion sets in. When Gareyev is tired, his recall can get patchy. He sometimes makes moves based on only a fragmented memory of the pieces'' positions.

<strong>F</strong> The scientists first had Gareyev perform some standard memory tests. These assessed his ability to hold numbers, pictures and words in mind. One classic test measures how many numbers a person can repeat, both forwards and backwards, soon after hearing them. Most people manage about seven. ‘He was not exceptional on any of these standard tests,'' said Rissman. ‘We didn''t find anything other than playing chess that he seems to be supremely gifted at.'' But next came the brain scans. With Gareyev lying down in the machine, Rissman looked at how well connected the various regions of the chess player''s brain were. Though the results are tentative and as yet unpublished, the scans found much greater than average communication between parts of Gareyev''s brain that make up what is called the frontoparietal control network. Of 63 people scanned alongside the chess player, only one or two scored more highly on the measure. ‘You use this network in almost any complex task. It helps you to allocate attention, keep rules in mind, and work out whether you should be responding or not,'' said Rissman. 

<strong>G</strong> It was not the only hint of something special in Gareyev''s brain. The scans also suggest that Gareyev''s visual network is more highly connected to other brain parts than usual. Initial results suggest that the areas of his brain that process visual images - such as chess boards – may have stronger links to other brain regions, and so be more powerful than normal. While the analyses are not finalised yet, they may hold the first clues to Gareyev''s extraordinary ability.

<strong>H</strong> For the world record attempt, Gareyev hopes to play 47 blindfold games at once in about 16 hours. He will need to win 80% to claim the title. ''I don''t worry too much about the winning percentage, that''s never been an issue for me,'' he said. ''The most important part of blindfold chess for me is that I have found the one thing that I can fully dedicate myself to. I miss having an obsession.'' 
' 
    )
    RETURNING id INTO section_3_id;

    -- Insert Questions for Passage 3
    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit)
    VALUES
        -- MATCHING_INFORMATION (Questions 27-32)
        (section_3_id, 27, 'cam17-t4-r-q27', 'MATCHING_INFORMATION', '{"text": "a reference to earlier examples of blindfold chess", "options": ["A", "B", "C", "D", "E", "F", "G", "H"]}', '["D"]', NULL),
        (section_3_id, 28, 'cam17-t4-r-q28', 'MATCHING_INFORMATION', '{"text": "an outline of what blindfold chess involves", "options": ["A", "B", "C", "D", "E", "F", "G", "H"]}', '["E"]', NULL),
        (section_3_id, 29, 'cam17-t4-r-q29', 'MATCHING_INFORMATION', '{"text": "a claim that Gareyev''s skill is limited to chess", "options": ["A", "B", "C", "D", "E", "F", "G", "H"]}', '["F"]', NULL),
        (section_3_id, 30, 'cam17-t4-r-q30', 'MATCHING_INFORMATION', '{"text": "why Gareyev''s skill is of interest to scientists", "options": ["A", "B", "C", "D", "E", "F", "G", "H"]}', '["B"]', NULL),
        (section_3_id, 31, 'cam17-t4-r-q31', 'MATCHING_INFORMATION', '{"text": "an outline of Gareyev''s priorities", "options": ["A", "B", "C", "D", "E", "F", "G", "H"]}', '["H"]', NULL),
        (section_3_id, 32, 'cam17-t4-r-q32', 'MATCHING_INFORMATION', '{"text": "a reason why the last part of a game may be difficult", "options": ["A", "B", "C", "D", "E", "F", "G", "H"]}', '["E"]', NULL),

        -- TRUE_FALSE_NOT_GIVEN (Questions 33-36)
        (section_3_id, 33, 'cam17-t4-r-q33', 'TRUE_FALSE_NOT_GIVEN', '{"text": "In the forthcoming games, all the participants will be blindfolded."}', '["FALSE"]', NULL),
        (section_3_id, 34, 'cam17-t4-r-q34', 'TRUE_FALSE_NOT_GIVEN', '{"text": "Gareyev has won competitions in BASE jumping."}', '["NOT GIVEN"]', NULL),
        (section_3_id, 35, 'cam17-t4-r-q35', 'TRUE_FALSE_NOT_GIVEN', '{"text": "UCLA is the first university to carry out research into blindfold chess players."}', '["NOT GIVEN"]', NULL),
        (section_3_id, 36, 'cam17-t4-r-q36', 'TRUE_FALSE_NOT_GIVEN', '{"text": "Good chess players are likely to be able to play blindfold chess."}', '["TRUE"]', NULL),

        -- SUMMARY_COMPLETION (Questions 37-40)
        (section_3_id, 37, 'cam17-t4-r-q37', 'SUMMARY_COMPLETION', '{"text": "<strong>How the research was carried out</strong><br/><br/>The researchers started by testing Gareyev''s <strong>37</strong> ____;"}', '["memory"]', 'ONE WORD ONLY'),
        (section_3_id, 38, 'cam17-t4-r-q38', 'SUMMARY_COMPLETION', '{"text": "for example, he was required to recall a string of <strong>38</strong> ____ in order and also in reverse order."}', '["numbers"]', 'ONE WORD ONLY'),
        (section_3_id, 39, 'cam17-t4-r-q39', 'SUMMARY_COMPLETION', '{"text": "Although his performance was normal, scans showed an unusual amount of <strong>39</strong> ____ within the areas of Gareyev''s brain that are concerned with directing attention."}', '["communication"]', 'ONE WORD ONLY'),
        (section_3_id, 40, 'cam17-t4-r-q40', 'SUMMARY_COMPLETION', '{"text": "In addition, the scans raised the possibility of unusual strength in the parts of his brain that deal with <strong>40</strong> ____ input."}', '["visual"]', 'ONE WORD ONLY');
END $$;