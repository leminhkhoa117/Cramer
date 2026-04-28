begin;

-- Widen session_mode to support PART_2_AND_3
alter table public.speaking_sessions
  drop constraint if exists chk_speaking_sessions_mode;
alter table public.speaking_sessions
  add constraint chk_speaking_sessions_mode
  check (session_mode in ('FULL', 'PART_1', 'PART_2', 'PART_3', 'PART_2_AND_3'));

-- Grading hardening columns
alter table public.speaking_sessions
  add column if not exists grading_attempts int not null default 0,
  add column if not exists last_grading_error text;

-- Half-step band constraints (0..9 step 0.5)
alter table public.speaking_sessions
  add constraint chk_speaking_sessions_overall_band_halfstep
    check (overall_band is null or
           (overall_band between 0 and 9 and (overall_band * 2)::int = overall_band * 2)),
  add constraint chk_speaking_sessions_fluency_band_halfstep
    check (fluency_band is null or
           (fluency_band between 0 and 9 and (fluency_band * 2)::int = fluency_band * 2)),
  add constraint chk_speaking_sessions_lexical_band_halfstep
    check (lexical_band is null or
           (lexical_band between 0 and 9 and (lexical_band * 2)::int = lexical_band * 2)),
  add constraint chk_speaking_sessions_grammar_band_halfstep
    check (grammar_band is null or
           (grammar_band between 0 and 9 and (grammar_band * 2)::int = grammar_band * 2)),
  add constraint chk_speaking_sessions_pronunciation_band_halfstep
    check (pronunciation_band is null or
           (pronunciation_band between 0 and 9 and (pronunciation_band * 2)::int = pronunciation_band * 2));

commit;
