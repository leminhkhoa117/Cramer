begin;

do $$
declare
    target_test_exists boolean;
    part1_count integer;
    part2_count integer;
    part3_count integer;
    invalid_part1_metadata integer;
    invalid_part2_metadata integer;
    invalid_part3_metadata integer;
begin
    select exists(
        select 1
        from public.tests t
        where t.id = 1
    ) into target_test_exists;

    if not target_test_exists then
        raise exception 'Official Speaking backfill target tests.id = 1 does not exist.';
    end if;

    select count(*)
    into part1_count
    from public.test_sets ts
    join public.tests t on t.set_id = ts.id
    join public.sections s on s.test_id = t.id
    join public.questions q on q.section_id = s.id
    where ts.code = 'speaking-mvp'
      and t.id in (32, 33, 34, 35, 36, 37)
      and lower(coalesce(s.skill, '')) = 'speaking'
      and s.part_number = 1
      and q.question_type = 'PART_1';

    if part1_count < 30 then
        raise exception 'Speaking mock source does not contain enough PART_1 prompts for official backfill. Expected at least 30, found %.', part1_count;
    end if;

    select count(*)
    into part2_count
    from public.questions q
    where q.question_uid = 'speaking-mvp-t1-s-p2-q1';

    if part2_count < 1 then
        raise exception 'Speaking mock source cue card speaking-mvp-t1-s-p2-q1 is missing.';
    end if;

    select count(*)
    into part3_count
    from public.test_sets ts
    join public.tests t on t.set_id = ts.id
    join public.sections s on s.test_id = t.id
    join public.questions q on q.section_id = s.id
    where ts.code = 'speaking-mvp'
      and t.id in (32, 33, 34, 37)
      and lower(coalesce(s.skill, '')) = 'speaking'
      and s.part_number = 3
      and q.question_type = 'PART_3';

    if part3_count < 15 then
        raise exception 'Speaking mock source does not contain enough PART_3 prompts for official backfill. Expected at least 15, found %.', part3_count;
    end if;

    select count(*)
    into invalid_part1_metadata
    from (
        select q.question_content,
               row_number() over (order by t.test_number, q.question_number, q.id) as rn
        from public.test_sets ts
        join public.tests t on t.set_id = ts.id
        join public.sections s on s.test_id = t.id
        join public.questions q on q.section_id = s.id
        where ts.code = 'speaking-mvp'
          and t.id in (32, 33, 34, 35, 36, 37)
          and lower(coalesce(s.skill, '')) = 'speaking'
          and s.part_number = 1
          and q.question_type = 'PART_1'
    ) source_part1
    where source_part1.rn <= 30
      and not (
          source_part1.question_content ? 'schemaVersion'
          and source_part1.question_content ? 'partType'
          and source_part1.question_content ? 'promptText'
          and source_part1.question_content ? 'topicLabel'
      );

    if invalid_part1_metadata > 0 then
        raise exception 'PART_1 source prompts contain % rows with incomplete question_content metadata.', invalid_part1_metadata;
    end if;

    select count(*)
    into invalid_part2_metadata
    from public.questions q
    where q.question_uid = 'speaking-mvp-t1-s-p2-q1'
      and not (
          q.question_content ? 'schemaVersion'
          and q.question_content ? 'partType'
          and q.question_content ? 'promptText'
          and q.question_content ? 'topicLabel'
          and q.question_content ? 'cueCardBullets'
          and q.question_content ? 'prepTimeSeconds'
          and q.question_content ? 'talkTimeSeconds'
      );

    if invalid_part2_metadata > 0 then
        raise exception 'PART_2 source cue card has incomplete question_content metadata.';
    end if;

    select count(*)
    into invalid_part3_metadata
    from (
        select q.question_content,
               row_number() over (
                   order by
                       case t.id
                           when 32 then 1
                           when 33 then 2
                           when 34 then 3
                           when 37 then 4
                           else 99
                       end,
                       q.question_number,
                       q.id
               ) as rn
        from public.test_sets ts
        join public.tests t on t.set_id = ts.id
        join public.sections s on s.test_id = t.id
        join public.questions q on q.section_id = s.id
        where ts.code = 'speaking-mvp'
          and t.id in (32, 33, 34, 37)
          and lower(coalesce(s.skill, '')) = 'speaking'
          and s.part_number = 3
          and q.question_type = 'PART_3'
    ) source_part3
    where source_part3.rn <= 15
      and not (
          source_part3.question_content ? 'schemaVersion'
          and source_part3.question_content ? 'partType'
          and source_part3.question_content ? 'promptText'
          and source_part3.question_content ? 'topicLabel'
      );

    if invalid_part3_metadata > 0 then
        raise exception 'PART_3 source prompts contain % rows with incomplete question_content metadata.', invalid_part3_metadata;
    end if;
end $$;

-- Mark the retained MVP set as mock-only.
update public.test_sets
set name = 'Speaking MVP Mock Set',
    description = 'Mock/test speaking dataset only. Retained for seed and regression use. Not official production speaking content.',
    is_published = false,
    source_type = 'custom'
where code = 'speaking-mvp';

with target as (
    select 1::bigint as test_id, 'cam17'::varchar as exam_source, 1::integer as test_number
),
existing_sections as (
    select s.part_number, s.id
    from public.sections s
    join target t on t.test_id = s.test_id
    where lower(coalesce(s.skill, '')) = 'speaking'
),
inserted_sections as (
    insert into public.sections (
        exam_source,
        test_number,
        skill,
        part_number,
        status,
        test_id
    )
    select
        t.exam_source,
        t.test_number,
        'speaking',
        part_numbers.part_number,
        'PUBLISHED',
        t.test_id
    from target t
    cross join (values (1), (2), (3)) as part_numbers(part_number)
    left join existing_sections es on es.part_number = part_numbers.part_number
    where es.id is null
    returning id, part_number
),
sections_final as (
    select part_number, id from existing_sections
    union all
    select part_number, id from inserted_sections
),
part1_source as (
    select
        q.question_content,
        q.image_url,
        q.word_limit,
        q.explanation,
        row_number() over (
            order by t.test_number, q.question_number, q.id
        ) as rn
    from public.test_sets ts
    join public.tests t on t.set_id = ts.id
    join public.sections s on s.test_id = t.id
    join public.questions q on q.section_id = s.id
    where ts.code = 'speaking-mvp'
      and t.id in (32, 33, 34, 35, 36, 37)
      and lower(coalesce(s.skill, '')) = 'speaking'
      and s.part_number = 1
      and q.question_type = 'PART_1'
),
upsert_part1 as (
    insert into public.questions (
        section_id,
        question_number,
        question_uid,
        question_type,
        question_content,
        image_url,
        word_limit,
        explanation
    )
    select
        sf.id,
        ps.rn,
        'cam17-t1-s-p1-q' || ps.rn,
        'PART_1',
        ps.question_content,
        ps.image_url,
        ps.word_limit,
        ps.explanation
    from part1_source ps
    join sections_final sf on sf.part_number = 1
    where ps.rn <= 30
    on conflict (question_uid) do update
    set section_id = excluded.section_id,
        question_number = excluded.question_number,
        question_type = excluded.question_type,
        question_content = excluded.question_content,
        image_url = excluded.image_url,
        word_limit = excluded.word_limit,
        explanation = excluded.explanation
    returning question_uid
),
part2_source as (
    select
        q.question_content,
        q.image_url,
        q.word_limit,
        q.explanation
    from public.questions q
    where q.question_uid = 'speaking-mvp-t1-s-p2-q1'
),
upsert_part2 as (
    insert into public.questions (
        section_id,
        question_number,
        question_uid,
        question_type,
        question_content,
        image_url,
        word_limit,
        explanation
    )
    select
        sf.id,
        1,
        'cam17-t1-s-p2-q1',
        'PART_2',
        ps.question_content,
        ps.image_url,
        ps.word_limit,
        ps.explanation
    from part2_source ps
    join sections_final sf on sf.part_number = 2
    on conflict (question_uid) do update
    set section_id = excluded.section_id,
        question_number = excluded.question_number,
        question_type = excluded.question_type,
        question_content = excluded.question_content,
        image_url = excluded.image_url,
        word_limit = excluded.word_limit,
        explanation = excluded.explanation
    returning question_uid
),
part3_source as (
    select
        q.question_content,
        q.image_url,
        q.word_limit,
        q.explanation,
        row_number() over (
            order by
                case t.id
                    when 32 then 1
                    when 33 then 2
                    when 34 then 3
                    when 37 then 4
                    else 99
                end,
                q.question_number,
                q.id
        ) as rn
    from public.test_sets ts
    join public.tests t on t.set_id = ts.id
    join public.sections s on s.test_id = t.id
    join public.questions q on q.section_id = s.id
    where ts.code = 'speaking-mvp'
      and t.id in (32, 33, 34, 37)
      and lower(coalesce(s.skill, '')) = 'speaking'
      and s.part_number = 3
      and q.question_type = 'PART_3'
),
upsert_part3 as (
    insert into public.questions (
        section_id,
        question_number,
        question_uid,
        question_type,
        question_content,
        image_url,
        word_limit,
        explanation
    )
    select
        sf.id,
        ps.rn,
        'cam17-t1-s-p3-q' || ps.rn,
        'PART_3',
        ps.question_content,
        ps.image_url,
        ps.word_limit,
        ps.explanation
    from part3_source ps
    join sections_final sf on sf.part_number = 3
    where ps.rn <= 15
    on conflict (question_uid) do update
    set section_id = excluded.section_id,
        question_number = excluded.question_number,
        question_type = excluded.question_type,
        question_content = excluded.question_content,
        image_url = excluded.image_url,
        word_limit = excluded.word_limit,
        explanation = excluded.explanation
    returning question_uid
)
select
    (select count(*) from upsert_part1) as part1_upserts,
    (select count(*) from upsert_part2) as part2_upserts,
    (select count(*) from upsert_part3) as part3_upserts;

commit;
