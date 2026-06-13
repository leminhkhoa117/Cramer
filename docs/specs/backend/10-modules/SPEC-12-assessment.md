# SPEC-12 — Assessment (Attempts, Answers, Scoring, Review)

> Status: **Authoritative** · Module: `assessment` · Depends on: SPEC-11, SPEC-15
> Owns the test-taking runtime for objective skills (Reading/Listening) and the attempt
> lifecycle shared by all skills. Writing essay grading is `writing` (SPEC-13); Speaking is
> `speaking` (SPEC-14).

---

## 1. Data model (existing tables)

| Table | Key fields |
|-------|-----------|
| `test_attempts` | user_id, exam_source, test_number (string), skill, status, score, current_part, time_left, started_at, completed_at |
| `user_answers` | attempt_id, question_id, user_id, answer_content (JSONB `{value}`), user_answer, is_correct, submitted_at |

`AttemptStatus` ∈ `IN_PROGRESS`, `COMPLETED`, `CANCELLED`. Attempts key on legacy
`exam_source/test_number/skill` (resolved via `ContentLookupPort` legacy shim, SPEC-11 §5).

## 2. API

| Method · Path | Auth | Purpose |
|---|---|---|
| `POST /api/test-attempts/start?source&test&skill&forceNew` | user | start/resume attempt |
| `POST /api/test-attempts/{id}/progress` | owner | save progress (in-progress only) |
| `POST /api/test-attempts/{id}/submit` | owner | grade + complete |
| `POST /api/test-attempts/{id}/cancel` | owner | cancel (idempotent) |
| `POST /api/test-attempts/{id}/resume` | owner | resume (in-progress only) |
| `GET /api/test-attempts/{id}/answers` | owner | saved answers |
| `GET /api/test-attempts/{id}/review` | owner | full review (with answers) |
| `DELETE /api/test-attempts/{id}` | owner | delete attempt |
| `POST /api/test-attempts/{id}/regrade` | owner | re-grade completed attempt |

## 3. Attempt lifecycle

- **Start** acquires a `PESSIMISTIC_WRITE` lock over the user's attempts for
  `source+test+skill`:
  - `forceNew=true` → cancel all in-progress, create new.
  - multiple in-progress → keep most recent, cancel others.
  - latest completed & `!forceNew` → return it (read-only resume of result).
  - latest in-progress → return it (resume).
  - else → create new.
- Reading/Listening **start** charges attempt quota via `AttemptBillingPort` (SPEC-15);
  Writing **does not** (it bills at grading time, SPEC-13). 402 on quota block.
- **Save progress** (in-progress only): update `time_left`/`current_part`; if `answers`
  present, replace all `user_answers` (delete + reinsert non-empty), stored as
  `answer_content = {"value": text}`. `is_correct` not set here.
- **Submit**: delete existing answers, re-grade (§4), reinsert with correctness, set
  `status=COMPLETED`, `completed_at`, `score = raw correct count`. Resubmission allowed.
- **Cancel** idempotent: missing/already-cancelled → success; completed → success without
  deletion; in-progress → delete answers (+ writing submissions) then the attempt.
- **Regrade** (completed only): re-compare stored answers vs current `correct_answer`;
  update `is_correct` + `score`.

## 4. Scoring (`ScoringService`)

Normalization for both sides: read `answer_content.value`; replace `_`→space; trim; lowercase.

- `correct_answer` is an **array** of acceptable values:
  - **Single-answer types** → user value matches **any** array element.
  - **`MULTIPLE_CHOICE_MULTIPLE_ANSWERS`** → **true set equality**: the user's selected set
    equals the correct set (order-independent, de-duplicated). **Fix:** the old code did a
    scalar equality against array elements, which mis-scored multi-select.
- Scalar `correct_answer` → exact normalized equality.
- Empty user answers are skipped (no row), counted wrong only via total (§4.1).

### 4.1 Band & totals
- Total questions = count of all questions in the test skill (`ContentLookupPort`), not the
  number answered.
- Reading/Listening band via `platform.common.ielts.BandScale` (40→9.0 … <4→0.0).
- **Fix:** `AttemptResultResponse` includes `bandScore` (Reading/Listening). The old
  `TestResultDTO` omitted it.

## 5. Review

`AttemptReviewView`: attempt metadata, score, total, timestamps, duration, `bandScore?`,
a flat `questions[]` and grouped `sections[]`. Each question review includes number, uid,
type, content, **user answer**, **correct answer**, **correctness**, **explanation**. Each
section review includes part number, passage_text, display/audio url, section_layout.
Owner-only; this is the **only** user-facing surface that exposes answer keys (SPEC-04 §3).

## 6. Edge cases
- `submit` on an already-completed attempt re-grades (delete+regrade+complete) — intentional.
- Numeric/synonym/article tolerance is **not** in scope (documented limitation; future spec
  may add normalization rules).
- Attempt history is served by `engagement` dashboard (SPEC-16), reading these tables.

## 7. Ports
- Consumes: `ContentLookupPort` (catalog), `AttemptBillingPort` (billing),
  `ActivityPort` (engagement — log test completion).
- Publishes: none (review is HTTP-only).

## 8. Change log
| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
