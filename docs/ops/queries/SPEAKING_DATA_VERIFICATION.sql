-- Speaking data verification pack
-- Purpose:
-- 1) inventory legacy tables
-- 2) verify official/mock separation
-- 3) verify official bank contract 30 / 1 / 15
-- 4) verify Speaking question_content metadata contract

-- A. Legacy inventory
select 'speaking_topics_legacy' as table_name, count(*) as row_count from public.speaking_topics_legacy
union all
select 'speaking_tests_legacy', count(*) from public.speaking_tests_legacy
union all
select 'speaking_questions_legacy', count(*) from public.speaking_questions_legacy
union all
select 'speaking_fixed_questions_legacy', count(*) from public.speaking_fixed_questions_legacy
union all
select 'speaking_sessions_legacy', count(*) from public.speaking_sessions_legacy
union all
select 'speaking_transcripts_legacy', count(*) from public.speaking_transcripts_legacy;

-- B. Speaking test sets / tests overview
select
    ts.id as test_set_id,
    ts.code as test_set_code,
    ts.name as test_set_name,
    ts.is_published as test_set_published,
    t.id as test_id,
    t.test_number,
    t.name as test_name,
    t.is_published as test_published,
    count(*) filter (where lower(coalesce(s.skill, '')) = 'speaking') as speaking_sections
from public.test_sets ts
join public.tests t on t.set_id = ts.id
left join public.sections s on s.test_id = t.id
group by ts.id, ts.code, ts.name, ts.is_published, t.id, t.test_number, t.name, t.is_published
order by ts.id, t.test_number;

-- C. Official vs mock separation
select id, code, name, description, is_published, source_type
from public.test_sets
where code in ('cam17', 'speaking-mvp', 'ielts_ai_1')
order by id;

-- D. Official Speaking bank contract verification
select
    ts.code as test_set_code,
    t.id as test_id,
    t.name as test_name,
    t.is_published,
    max(case when s.part_number = 1 then cnt.prompt_count end) as part1_count,
    max(case when s.part_number = 2 then cnt.prompt_count end) as part2_count,
    max(case when s.part_number = 3 then cnt.prompt_count end) as part3_count
from public.tests t
join public.test_sets ts on ts.id = t.set_id
join public.sections s on s.test_id = t.id and lower(coalesce(s.skill, '')) = 'speaking'
join lateral (
    select count(*) as prompt_count
    from public.questions q
    where q.section_id = s.id
      and q.question_type in ('PART_1', 'PART_2', 'PART_3')
) cnt on true
where t.id = 1
group by ts.code, t.id, t.name, t.is_published;

-- E. Per-section speaking counts
select
    ts.code as test_set_code,
    t.id as test_id,
    t.name as test_name,
    s.id as section_id,
    s.part_number,
    s.status,
    count(q.id) as question_count
from public.test_sets ts
join public.tests t on t.set_id = ts.id
join public.sections s on s.test_id = t.id and lower(coalesce(s.skill, '')) = 'speaking'
left join public.questions q on q.section_id = s.id
where t.id = 1
group by ts.code, t.id, t.name, s.id, s.part_number, s.status
order by s.part_number;

-- F. Metadata contract verification
select
    ts.code as test_set_code,
    t.id as test_id,
    t.name as test_name,
    bool_and(
        case
            when s.part_number = 1 then q.question_type = 'PART_1'
            when s.part_number = 2 then q.question_type = 'PART_2'
            when s.part_number = 3 then q.question_type = 'PART_3'
            else false
        end
    ) as question_type_matches_part,
    count(*) filter (where q.question_content ? 'schemaVersion') as with_schema_version,
    count(*) filter (where q.question_content ? 'partType') as with_part_type,
    count(*) filter (where q.question_content ? 'promptText') as with_prompt_text,
    count(*) filter (where q.question_content ? 'topicLabel') as with_topic_label,
    count(*) filter (where s.part_number = 2 and q.question_content ? 'cueCardBullets') as part2_with_cue_bullets,
    count(*) filter (where s.part_number = 2 and q.question_content ? 'prepTimeSeconds') as part2_with_prep_time,
    count(*) filter (where s.part_number = 2 and q.question_content ? 'talkTimeSeconds') as part2_with_talk_time,
    count(*) as total_questions
from public.tests t
join public.test_sets ts on ts.id = t.set_id
join public.sections s on s.test_id = t.id and lower(coalesce(s.skill, '')) = 'speaking'
join public.questions q on q.section_id = s.id
where t.id = 1
group by ts.code, t.id, t.name;

-- G. Sample official Speaking rows for manual review
select
    q.question_uid,
    q.question_number,
    q.question_type,
    q.question_content ->> 'topicLabel' as topic_label,
    q.question_content ->> 'promptText' as prompt_text
from public.questions q
where q.section_id in (
    select s.id
    from public.sections s
    where s.test_id = 1
      and lower(coalesce(s.skill, '')) = 'speaking'
)
order by q.question_type, q.question_number
limit 20;
