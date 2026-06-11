# SPEC-14 — Speaking (Sessions, Realtime, Grading)

> Status: **Authoritative** · Module: `speaking` · Depends on: SPEC-11, SPEC-15
> Large module — if it exceeds ~400 lines, split into `SPEC-14a-session`, `-14b-realtime`,
> `-14c-grading`. Authored content comes from `catalog` (sections.skill=speaking); runtime
> truth is `speaking_sessions` + `speaking_transcripts`. **Never** read `speaking_*_legacy`.

---

## 1. Data model (existing tables)

| Table | Key fields |
|-------|-----------|
| `speaking_sessions` | user_id, test_id, session_mode, status, accent, speed, session_blueprint (JSONB), is_finalized, duration, band columns, grading_result (JSONB), lua_cost, lua_deducted, grading_attempts, last_grading_error, graded_at |
| `speaking_transcripts` | session_id, source_question_id, part_number, turn_index, question_snapshot (JSONB), audio_storage_path, audio_duration, transcript_text, confidence, question_evaluation; unique (session_id, turn_index) |

Authored content: `sections.skill=speaking`, question types `PART_1/2/3`, `question_content`
= `{ schemaVersion, partType, promptText, topicLabel, … }`; Part 2 adds cue-card bullets +
prep/talk timing.

## 2. Session lifecycle

`SpeakingSessionStatus` ∈ `in_progress`, `completed`, `abandoned`, `expired`, `grading`,
`graded`, `grading_failed`.

**State machine** (`SpeakingSessionStateMachine`):
- `in_progress → completed | abandoned | expired`
- `completed → grading`
- `grading → graded | grading_failed`
- `grading_failed → grading`
- `graded → completed` **only via admin regrade** (see §6 — this is the fix that makes
  regrade actually work).

Create (`POST /api/speaking/sessions`): normalize `session_mode` (`FULL`, `PART_1`, `PART_2`,
`PART_3`, `PART_2_AND_3`), `accent` (`british|american|australian|neutral`), `speed`
(`0.85|1.00|1.15`); build blueprint (§3); **check** Lúa (not deduct) if
`speaking.session.lua-check-on-create`; persist `status=in_progress`, `lua_cost`,
`lua_deducted=false`, `session_blueprint`. Requires a **published** test with published (or
null-status) speaking sections.

Defaults (`SpeakingSessionProperties`): lua-cost 15; check-on-create true; charge-on-complete
true; Part1 bank 30/select 8–12; Part2 bank 1/select 1; Part3 bank 15/select 3–6,
defer-until-context true.

**Complete** (`POST …/{id}/complete`): require no pending deferred Part 3 and a transcript for
every selected turn; finalize; **deduct** Lúa if not already (`SpeakingBillingPort`); save
duration; **dispatch grading after commit**. **Abandon** finalizes without charge.

## 3. Blueprint & question selection

- The **frozen blueprint** = `{ schemaVersion, testId, sessionMode, accent, speed, parts[] }`;
  each part has `partNumber, bankSize, selectionStatus, selectionStrategy, targetTurnCount,
  selectedTurnCount, turns[]`; each turn = `{ turnIndex, sourceQuestionId, questionSnapshot }`.
  Deferred Part 3 candidate banks live under `_internal` and are **stripped** from API
  responses.
- **`SelectionPlanner`** (interface, 2 impls):
  - `HeuristicSelectionPlanner`: Part 1 random count across 2–3 topic clusters; Part 2 single
    shuffled cue card; Part 3 coherent cluster; follow-up Part 3 scored by topic overlap with
    the chosen Part 2.
  - `LlmSelectionPlanner` (`speaking.selection.provider=llm` + model + key): LLM picks
    existing question IDs only; validates exact count, no duplicates, all in bank; **any**
    failure falls back to heuristic. Part 2 always heuristic.
- **Deferred Part 3** (FULL mode, defer-until-context): Part 3 starts `pending_after_part_2`;
  after the Part 2 transcript is saved, Part 3 materializes from the chosen Part 2 cue card +
  its transcript text. `PART_2_AND_3` does not defer.

## 4. Realtime (Gemini Live)

WS `/ws/speaking/{sessionId}` (registered in `speaking.web.ws`). Spring Security permits
`/ws/**`, but the **handshake interceptor enforces JWT** (from `Authorization: Bearer`,
`access_token`, or legacy `token` query). The handler verifies ownership and requires
`status=in_progress` & not finalized, then connects to Gemini Live or enters **fallback text
mode** (Gemini disabled / no key).

Client→server: `{type:start_question, turnIndex}` (validates turn; sends frozen prompt),
`{type:end_turn}` (Gemini `audioStreamEnd`), binary PCM audio (forwarded as base64 realtime
input; ignored in fallback).
Server→client: text `status` / `error` / `transcript {source: examiner|user}` /
`examiner_speaking` / `turn_complete`; binary examiner audio.
Gemini config (`SpeakingGeminiProperties`): model `gemini-2.0-flash-live-preview-04-09`,
input PCM 16000 Hz, output PCM 24000 Hz, voice `Puck`, one frozen prompt per turn (Part 2
includes cue-card bullets + timing), transcription enabled, text reassembly capped at 1 MiB.

## 5. Transcripts

`POST …/{id}/transcripts` upserts by `(session_id, turn_index)`. The service **pessimistically
locks** the owned session, requires `in_progress`, finds the expected turn in the blueprint,
and **rejects** mismatched `partNumber`/`sourceQuestionId`/`questionSnapshot` (deep equality —
a client cannot alter the prompt). `audioStoragePath` must be a **relative object key** (no
empty/absolute/`..`/backslash/scheme/colon). `transcript_text` trimmed-nullable; `confidence`
∈ [0,1]. Saving the Part 2 transcript also **materializes deferred Part 3**.
> Realtime transcript events are transient; grading depends on these **REST-persisted**
> transcripts/audio paths.

## 6. Grading

Completion dispatches async grading (`speakingGradingExecutor`). The worker **claims** a
session by transitioning `completed → grading`, stores progress, loads transcripts/turns and
the strict grading schema, and calls OpenRouter (default `google/gemini-2.5-flash`).

**Audio prep** (`SpeakingAudioPreparer`): download each `audio_storage_path` from Supabase
bucket `speaking-audio`; `mp3`/`wav` as-is, else `ffmpeg` → mono 16 kHz 64 kbps MP3 (30 s
timeout); per-turn max 180 s; total base64 cap 18 MB. Download/transcode failures skip that
turn; payload-too-large → retry/fail.

**Retry** (max 2): attempt 1 multimodal text+audio; later attempts text-only + instruction
that pronunciation confidence is low and pronunciation arrays empty. On success: validate
overall + criterion bands non-null, 0–9, half-steps; persist band columns + full
`grading_result` JSON; on total failure: `grading_failed`, `graded_at`, `last_grading_error`,
**refund** Lúa if deducted (idempotent `refund_session_{id}`).

**Watchdog** (`@Scheduled`, 60 s, flag `speaking.evaluation.enabled`): fail+refund `grading`
sessions stale 10 min; re-enqueue `completed` sessions stale 5 min.

### 6.1 Grading result (schema-driven — fix)
> **Fix:** the result is **one** schema-driven typed object (`SpeakingGradingResult`), not 24
> fragmented DTOs. The JSON schema in `resources/speaking/grading-schema.json` is the contract;
> the typed record mirrors it. Reconcile the prompt, schema, and type so success cases never
> fail strict output (the old `gradingMode/degradedReason` mismatch is removed). Persisted
> `grading_result` deserializes back into the type tolerantly (ignore `_worker` metadata).

Root: `schemaVersion, overallBand, gradingMode, degradedReason?, criteria, perPartFeedback[],
perTurnFeedback[], improvementTips[]`. Criteria: fluency/coherence (hesitations, repetitions,
self-corrections, topic development), lexical (good usage, weak/improvable, inaccurate,
idiomatic), grammar (inaccurate structures, problematic sentences), pronunciation (confidence,
stress/intonation/pronunciation issues, connected speech, intelligibility). Per-part =
`{partNumber, feedback}`; per-turn = `{turnIndex, partNumber, shortNote, sampleAnswer}`.

## 7. Admin regrade
`POST /api/admin/speaking/sessions/{id}/regrade?mode&force` (admin; non-blank `reason`).
Allowed from `grading_failed`, or `graded` **with `force=true`**. **Fix:** regrade resets the
session to a **claimable** state (`completed`), clears attempts/last-error, writes audit
`SPEAKING_REGRADE` (`AuditPort`), then dispatches — so the worker (which claims `completed`)
actually grades it. (The old path set `grading` directly and the worker skipped it.)

## 8. API surface

| Method · Path / WS | Auth | Purpose |
|---|---|---|
| `POST /api/speaking/sessions` | user | create (201) |
| `GET /api/speaking/sessions/{id}` | owner | session + blueprint |
| `POST /api/speaking/sessions/{id}/transcripts` | owner | upsert transcript |
| `POST /api/speaking/sessions/{id}/complete` | owner | finalize + queue grading |
| `POST /api/speaking/sessions/{id}/abandon` | owner | finalize, no charge |
| `GET /api/speaking/sessions/{id}/grading-status` | owner | poll grading |
| `GET /api/speaking/sessions/{id}/results` | owner | result (409 if not graded) |
| `GET /api/speaking/history?page&size&status` | user | paged history |
| `POST /api/admin/speaking/sessions/{id}/regrade` | admin | regrade + audit |
| `WS /ws/speaking/{sessionId}` | JWT handshake | realtime examiner proxy |

Errors per SPEC-04 (`IllegalState→409`, `ResourceNotFound→404`, `QuotaExceeded→402`,
`AccessDenied→403`). WS: handshake 401; close 1008 policy / 1011 transport.

## 9. Ports
- Consumes: `ContentLookupPort.speakingBank` (catalog), `SpeakingBillingPort` (billing),
  `AuditPort` (admin), `SupabaseStorageClient` + `OpenRouterClient` (platform).
- Publishes: `AdminSpeakingService` (used by admin module via the regrade endpoint here).

## 10. Change log
| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
