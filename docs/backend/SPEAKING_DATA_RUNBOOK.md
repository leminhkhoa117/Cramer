# Speaking Data Runbook

## Mục tiêu

Runbook này mô tả cách vận hành, verify, và mở rộng dữ liệu Speaking sau đợt cleanup/backfill ban đầu.

## Current decisions

- `speaking_*_legacy` = archive-only
- `speaking-mvp` = mock/test only
- official smoke-test target = `cam17` / `tests.id = 1`

## Current official state

- shared hierarchy speaking đã có official test đầu tiên
- bank contract hiện tại cho official target:
  - Part 1 = 30
  - Part 2 = 1
  - Part 3 = 15

## Files to use

- Policy: `docs/library/backend/SPEAKING_DATA_POLICY.md`
- Migration artifact: `docs/backend/migrations/20260325_speaking_data_cleanup_backfill_v1.sql`
- SQL verification pack: `docs/backend/queries/SPEAKING_DATA_VERIFICATION.sql`
- Postman collection: `docs/backend/postman/Speaking_API.postman_collection.json`
- API smoke test: `docs/backend/SPEAKING_API_SMOKE_TEST.md`

## Operational checks before rollout

1. Confirm backend runtime does not read `speaking_*_legacy`
2. Run `SPEAKING_DATA_VERIFICATION.sql`
3. Verify official bank contract on the target test
4. Verify `speaking-mvp` is still unpublished and mock-labeled
5. Run Postman smoke test against `/api/speaking/sessions`

## How to expand official Speaking to more tests

Repeat the following for each new official test:

1. Create three `sections` rows with `skill = 'speaking'`, `part_number = 1/2/3`, `status = 'PUBLISHED'`
2. Insert `questions` rows with:
   - `PART_1` x 30
   - `PART_2` x 1
   - `PART_3` x 15
3. Ensure `question_content` includes the full metadata contract
4. Run the verification SQL pack against the new `test_id`
5. Run create-session smoke test on that `test_id`

## What not to do

- Do not reuse `speaking_*_legacy` directly in runtime
- Do not mark `speaking-mvp` as official acceptance evidence
- Do not publish mock speaking sets unintentionally
- Do not drop legacy tables without explicit backup/export approval

## Known limitation

`Complete session` will fail unless the client has uploaded transcripts for every selected turn in the frozen `sessionBlueprint`. This is expected behavior.
