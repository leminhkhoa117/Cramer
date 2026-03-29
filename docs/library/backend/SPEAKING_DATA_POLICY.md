# Cramer Speaking Data Policy

> Last Updated: 25/03/2026
> Scope: live Speaking data cleanup, mock/official separation, legacy retention

---

## 1. Source-of-truth policy

### Active runtime path

The active Speaking runtime uses only:

- authored content from the shared hierarchy: `test_sets -> tests -> sections -> questions`
- runtime truth from: `speaking_sessions`, `speaking_transcripts`

Active backend runtime code reads Speaking authored content from the shared hierarchy via:

- `SpeakingContentServiceImpl`
- `SectionRepository.findByIeltsTestId(...)`
- `QuestionRepository.findBySectionId(...)`

No active backend runtime path should read from `speaking_*_legacy` tables.

### Runtime truth

- `speaking_sessions.session_blueprint` = session-level runtime truth
- `speaking_transcripts.question_snapshot` = turn-level runtime truth

---

## 2. Legacy table retention policy

`speaking_*_legacy` tables are retained as **archive-only** tables for traceability and migration reference.

They are:

- **not** part of the active runtime path
- **not** the source of authored content for REST Speaking session creation
- retained for audit / traceability until a separate backup-export-and-drop decision is approved

### Legacy inventory snapshot

| Table | Live Rows | Runtime Dependency | Decision |
|---|---:|---|---|
| `speaking_topics_legacy` | 10 | None found in active runtime | Keep archive-only |
| `speaking_tests_legacy` | 10 | None found in active runtime | Keep archive-only |
| `speaking_questions_legacy` | 117 | None found in active runtime | Keep archive-only |
| `speaking_fixed_questions_legacy` | 4 | None found in active runtime | Keep archive-only |
| `speaking_sessions_legacy` | 60 | None found in active runtime | Keep archive-only |
| `speaking_transcripts_legacy` | 82 | None found in active runtime | Keep archive-only |

---

## 3. Official vs mock Speaking data

### Official Speaking data

Official Speaking content must live in the shared hierarchy and satisfy the REST/planner contract:

- `sections.skill = 'speaking'`
- `questions.question_type IN ('PART_1', 'PART_2', 'PART_3')`
- official bank contract per test:
  - Part 1 = 30 prompts
  - Part 2 = 1 cue card
  - Part 3 = 15 prompts

### Mock / test Speaking data

`speaking-mvp` is retained as **mock/test data only**.

Policy:

- keep the test set unpublished
- do not use it as official acceptance evidence
- keep it available as seed / mock / regression dataset during early rollout

Current mock identifiers:

- `test_sets.code = 'speaking-mvp'`
- set should remain unpublished and clearly labeled mock-only

---

## 4. Official backfill target (initial rollout)

Initial official rollout target:

- `cam17` / test 1 (`tests.id = 1`)

Backfill strategy:

- create Speaking sections for parts 1/2/3 under `cam17` test 1
- copy / normalize prompt payloads from vetted shared-hierarchy Speaking seed data
- ensure the official bank satisfies `30 / 1 / 15`

This creates one official published Speaking test that can be used to verify:

- `POST /api/speaking/sessions`
- session blueprint generation
- planner selection behavior against live data

---

## 5. Metadata contract for `questions.question_content`

Every Speaking question must include:

- `schemaVersion`
- `partType`
- `promptText`
- `topicLabel`

Part 2 also requires:

- `cueCardBullets`
- `prepTimeSeconds`
- `talkTimeSeconds`

This contract is required so REST session creation and planner selection remain stable.

---

## 6. Verification checklist

- Legacy inventory documented and archived policy recorded
- No active runtime/code path depends on `speaking_*_legacy`
- Official shared-hierarchy Speaking test passes `30 / 1 / 15`
- Speaking `question_content` metadata is complete
- Mock/test data remains clearly separated from official data
- `POST /api/speaking/sessions` can be tested against official published Speaking content
