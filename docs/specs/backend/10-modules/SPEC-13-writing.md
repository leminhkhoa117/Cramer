# SPEC-13 — Writing (Submission & AI Grading)

> Status: **Authoritative** · Module: `writing` · Depends on: SPEC-11, SPEC-12, SPEC-15

---

## 1. Data model (existing table)

`writing_submissions` — attempt_id, user_id, task_number (1|2), essay_text, word_count,
grading_status, overall_band, band_scores (JSONB), ai_feedback (JSONB), submitted_at,
graded_at. `WritingStatus` ∈ `PENDING`, `GRADING`, `COMPLETED`, `FAILED` (code's terminal
success state is **COMPLETED**, not `GRADED`).

## 2. API

| Method · Path | Auth | Purpose |
|---|---|---|
| `POST /api/writing/draft/{attemptId}?taskNumber=1\|2` | owner | save/update a draft essay |
| `POST /api/writing/submit/{attemptId}` | owner | submit 1–2 essays for grading (rate-limited) |
| `GET /api/writing/status/{attemptId}` | owner | grading status (poll) |
| `GET /api/writing/review/{attemptId}` | owner | full review |
| `GET /api/writing/submissions/{attemptId}` | owner | raw submissions |
| `POST /api/writing/regrade/{attemptId}` | owner | re-grade completed (rate-limited) |
| `POST /api/writing/validate-api-key` | user | validate a DeepSeek key |

## 3. Submission flow

- **Draft**: save `essay_text`, recompute `word_count`, status `PENDING`. Ownership via parent
  attempt.
- **Submit** (`SubmitEssayRequest { essays: {1:.., 2:..} }`, size 1–2):
  - rate-limit key `grading` → 429 on exceed.
  - cancel other in-progress attempts for the same user/source/test/skill.
  - mark the attempt `COMPLETED` immediately (attempt completion ≠ grading completion).
  - save submissions `PENDING`, then **dispatch async grading after commit** (SPEC-04 §5).
- AI-grading usage is charged **after** grading succeeds (SPEC-15 `UsageBillingPort`), not at
  submit. On grading failure, no charge / refund.

## 4. Async grading (`WritingGradingDispatcher` + `WritingGradingService`)

- Requires the **server** `DEEPSEEK_API_KEY`; if missing → all submissions `FAILED`.
- Grade tasks in parallel (`CompletableFuture`), overall wait **20 min**.
- Per task: status `GRADING` → DeepSeek call → save result.
- If **any** task fails → all submissions `FAILED`, **skip** billing.
- If all succeed → charge AI grading once via `UsageBillingPort.chargeAiGrading(userId,
  "attempt_{attemptId}")`. If the post-grading charge fails, results stay visible and a
  `BILLING_RECONCILIATION_REQUIRED` event is logged (never hide a successful grade).

### 4.1 DeepSeek call
- OpenAI-compatible `/chat/completions`; model: explicit → `llm.gradingModel` → `deepseek-chat`.
- `temperature=0.4`, `max_tokens=8192`, `response_format={type:json_object}`, no streaming.
- **Text-only**: images unsupported; `image_description` is passed in the prompt;
  `display_content_url` is logged as unsupported.
- **Local shortcuts** (no API): empty essay → band 0; `< 20` words → band 1.
- Guidance: Task 1 ≥150 words, Task 2 ≥250 words.

### 4.2 Result handling
- Model JSON: `band_scores`, `overall_band`, `sentence_corrections`, `paragraph_rewrites`,
  `vocabulary_highlights`, `error_analysis`, `sample_essay_band_plus_one`,
  `sample_essay_band_9`, `feedback_summary`, `word_analysis`, `criteria_comments`.
- `band_scores` stored as JSONB; **overall band recomputed** = average of band-score values
  rounded to nearest 0.5 (do not trust the model's `overall_band`).
- `ai_feedback` stores the feedback fields; status `COMPLETED`, `graded_at` set.

## 5. Status & review

- **Status** aggregate: `COMPLETED` (all completed, none failed), `PARTIAL_FAILURE` (all
  terminal, ≥1 failed), `GRADING`, `PENDING`; plus per-task statuses + counts.
- **Review**: weighted **overall band = Task1·⅓ + Task2·⅔**, rounded 0.5 (single task → its
  band). Task reviews (essay, word count, status, band, band_scores, feedback, timestamps) +
  task prompts (from writing `sections`: part_number→task, passage_text→prompt,
  display_content_url→image). **Fix:** populate `WritingReviewView.averageBandScores`
  (the old DTO field was left empty).

## 6. Regrade
- Completed attempt only; submissions must exist. Re-charge AI grading via `UsageBillingPort`;
  on block → `FAILED` + cleared bands; else reset to `PENDING`, clear bands/feedback, dispatch
  after commit. Response `REGRADING_STARTED`.

## 7. Money consistency (with SPEC-15)
- **One** canonical AI-grading overage price (defined in SPEC-15). All writing copy/DTOs use
  it. **Fix:** remove the stale "10 Lúa" references; the AI-grading overage is the SPEC-15
  value (20 Lúa).

## 8. Ports
- Consumes: `UsageBillingPort` (billing), `ContentLookupPort` (catalog — writing sections),
  `RateLimiter` (platform), `DeepSeekClient` (platform), `ActivityPort` (engagement).

## 9. Change log
| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
