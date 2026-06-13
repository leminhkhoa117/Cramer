# SPEC-21 — ABTS Generation & Streaming

> Status: **Authoritative** · Module: `abts` · Depends on: SPEC-20, SPEC-22

---

## 1. Generation request (C2 input)

A generation request carries:

- `skill` — `READING | LISTENING | WRITING` (`SPEAKING` → `NOT_IMPLEMENTED`).
- `partsToGenerate` — list of parts/tasks (e.g. `[1,3]`, `[TASK_1]`).
- Per-part config: `topic`, `factsMode` (`AUTO|STRICT`), `facts[]`, `questionTypes[]`,
  `questionTypeCounts{type→n}`, `totalQuestions`, `passageLength` (reading),
  `difficulty`.
- Model config: `model`, `temperature`, `maxTokens`, `enableReasoning`, `reasoningEffort`,
  `reasoningBudget`, `contextCache`.
- `explanationLanguage` (e.g. `vi`/`en`), `customInstructions`.

> **Fix (was lost):** multi-part generation **must** propagate per-part `questionTypeCounts`
> and `totalQuestions` into each part request. The old code copied only `questionTypes`.

## 2. Two execution modes

### 2.1 Synchronous (`/generate/{skill}`)

- One JSON-schema-constrained call per skill (SPEC-22).
- Up to **3 attempts**. On a validation failure, retry while attempts remain.
- If the final content is parseable but still has non-fatal issues → `PARTIAL_SUCCESS`
  (warnings/errors attached). If unparseable/empty after retries → `FAILED` with retryability.
- OpenRouter transport errors map to `FAILED` with an error code (SPEC-24 §3).

### 2.2 Streaming (`/generate/{skill}/stream`, SSE)

The streaming path is the primary UX. Lifecycle:

1. Emit `STARTED`.
2. Verify OpenRouter key; if missing → `FAILED` (clean SSE failure, connection already open).
3. Choose path by `partsToGenerate`:
   - **single part** → single-part pipeline (§3),
   - **multiple parts** → multi-part loop (§4).

## 3. Per-skill phase pipelines (streaming)

Each part is generated in **phases**; phase outputs are cached across retries so a passing
phase is never re-billed.

### 3.1 Reading — 2 phases

1. **Passage** phase → `section.passage_text`.
2. **Questions** phase → `questions[]` for the part's range.
3. Merge passage + questions → validate (SPEC-23) → parse → `COMPLETED`.

### 3.2 Listening — 3 phases

1. **Transcript** phase → transcript text + `audio_placeholder` metadata (cached).
2. **Stems/layout** phase → question stems + `section_layout.blocks` + `question_numbers`
   (no answers) (cached).
3. **Answers** phase → `answers[]` keyed by `question_number`, with explanations/evidence.
4. Merge → validate → parse → `COMPLETED`.

### 3.3 Writing — 3 phases

1. **Task** phase → `task_prompt` + `word_requirement` (+ `chart_data`/`letter_context`/
   `essay_metadata` per task type) (cached).
2. **Sample answer** phase → model sample answer (cached).
3. **Band breakdown** phase → band-by-criterion + key phrases + grading notes.
4. Merge → validate → `COMPLETED`.

## 4. Multi-part generation

- Iterate the requested parts; for each, run the single-part pipeline.
- **Renumber** questions to the part's canonical range (SPEC-20 §4.1).
- **Merge** successful sections/questions into one result; aggregate token/cost metadata.
- **Partial success**: if some parts fail, return `PARTIAL_SUCCESS` with `partErrors{part→error}`
  and the successful parts. If **all** parts fail → `FAILED` with the full error map.
- A failed part never aborts the others.

## 5. SSE event model

All stream payloads are a single `StreamEvent` shape. The **`type` field inside the payload**
is authoritative (clients read `data.type`, not the SSE event name — keep them consistent
anyway).

| `type` | When | Key fields |
|--------|------|-----------|
| `STARTED` | stream opened | `message`, `timestamp` |
| `PROMPT_BUILT` | prompt assembled | `message` |
| `AI_THINKING` | reasoning delta | `data` (delta), `partNumber` |
| `AI_CHUNK` | content delta | `data` (delta), `partNumber` |
| `PROGRESS` | phase/part progress | `progress` (0–100), `partNumber`, `totalParts`, `message` |
| `RETRY` | attempt retried | `attempt`, `maxAttempts`, `message` |
| `COMPLETED` | success / partial | `data` (result), `progress=100` |
| `FAILED` | hard failure | `message`, error code |
| `ABORTED` | client cancelled / queue saturated | `message` |
| `REFINEMENT_COMPLETED` | refine stream done | `data` (hunks) — see SPEC-23 |

Common fields on every event: `type`, `message?`, `progress?`, `attempt?`, `maxAttempts?`,
`data?`, `partNumber?`, `totalParts?`, `timestamp`.

## 6. Concurrency, timeout, cancellation

- Streaming runs on a **bounded executor** (`abtsStreamingExecutor`, default 8 threads, queue
  4). Queue saturation → emit `ABORTED` (never silently drop).
- **Emitter timeout** default 30 min; **per-part timeout** default 10 min (`PartTimeoutException`).
- **Client abort**: closing the SSE fetch sets a shared cancellation flag; the OpenRouter
  streaming client disconnects its upstream HTTP connection so no further tokens are billed.
- Cancellation is cooperative and checked between phases and during upstream streaming.

## 7. Reasoning behavior

- `ModelCapabilityRegistry` (SPEC-24) maps a model slug → the correct OpenRouter reasoning
  payload. Requests that enable reasoning on a non-reasoning model degrade gracefully (no
  reasoning, content still generated).
- Reasoning deltas → `AI_THINKING`; final reasoning text is attached to the result.

## 8. Result object

A generation result carries: `status` (`SUCCESS|PARTIAL_SUCCESS|FAILED|NOT_IMPLEMENTED`),
`skill`, `content` (the merged JSON), `validation` (SPEC-23), `partErrors?`, `reasoning?`,
`usage` (tokens/cost), `model`, `attempts`.

## 9. Edge cases / rules

- `POST /generate/questions` regenerates questions against an existing passage/transcript;
  Reading/Listening require `existingPassageText` (400 if missing); Writing delegates to
  writing generation; Speaking → `NOT_IMPLEMENTED`.
- Listening phase-3 missing answers or wrong answer cardinality is a hard retry/fail.
- All generated text is English; explanations follow `explanationLanguage`.

## 10. Change log

| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring (target spec). |
