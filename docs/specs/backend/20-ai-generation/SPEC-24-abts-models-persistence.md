# SPEC-24 — ABTS Models & Persistence

> Status: **Authoritative** · Module: `abts` · Depends on: SPEC-20–23, SPEC-11

---

## 1. Model catalog (C7)

`ModelCatalogService`:
- Fetches OpenRouter `/models`; caches for **5 minutes**.
- Falls back to a **curated model list** when the live catalog is unavailable.
- Enriches each model with **capability descriptors** (reasoning support, context window,
  modality, pricing hints).
- Validates configured default models at **startup**; if the configured default generation
  model is invalid in the live catalog, it recovers via **fallback candidates**.

`ModelCapabilityRegistry` maps a model slug → the correct OpenRouter **reasoning payload**
shape (effort/budget/none), so reasoning requests are model-appropriate (SPEC-21 §7).

## 2. OpenRouter integration

`OpenRouterClient` (in `platform.integration.openrouter`, reused by ABTS) supports:
- non-streaming **JSON-schema** chat calls,
- feature calls with **web search** / **context caching**,
- true **SSE streaming**,
- model listing.

Error mapping (HTTP → code): `AUTH_FAILED` (401/403), `INSUFFICIENT_CREDITS` (402),
`RATE_LIMITED` (429), `MODEL_UNAVAILABLE` / `NO_PROVIDERS` (404/503), else `UPSTREAM_ERROR`.
Each carries a `retryable` flag used by the generation retry logic (SPEC-21 §2).

## 3. Configuration (`abts.*` + `openrouter.*`)

| Key | Default | Purpose |
|-----|---------|---------|
| `openrouter.api-key` | env | credential |
| `openrouter.base-url` | OpenRouter URL | endpoint |
| `openrouter.default-generation-model` | `deepseek/deepseek-v4-flash`* | sync/stream default |
| `openrouter.api-timeout` | — | per-call timeout |
| `abts.streaming.emitter-timeout` | 30m | SSE emitter timeout |
| `abts.streaming.part-timeout` | 10m | per-part timeout |
| `abts.streaming.pool-size` / `queue` | 8 / 4 | bounded executor |
| `abts.max-refinement-rounds` | configurable | refine cap |

\* configurable; the catalog validates/recovers it at startup.

## 4. Saving generated content (C6) → catalog (draft)

Save target is the **catalog hierarchy** (SPEC-11). Schema is frozen; ABTS writes into the
existing tables.

### 4.1 Resolve/create set & test

- **Test set**: by `setId` or `setCode`; default code `ai_generated`. Newly created sets are
  **unpublished**, `source_type = ai_generated`.
- **Test**: by `testId` or `testNumber`; if no number, use **max+1** within the set. Newly
  created tests are **unpublished**, `is_ai_generated = true`, with `generation_metadata`
  (model, prompt config, usage) recorded.

### 4.2 Sections & questions

For each generated section (single or multi-part):
- Find existing `sections` row by `(test_id, skill, part_number)`.
  - **Found** → update `passage_text` / `section_layout` / `audio_url` / `image_description`,
    and **replace** that section's questions.
  - **Absent** → create a **DRAFT** section (`status = DRAFT`).
- Insert `questions` with JSONB `question_content`, `correct_answer`, `explanation`,
  plus `word_limit` / `image_url` where present.
- `question_uid` = `{setCode}_{testNumber}_{skillInitial}_q{number}` (unique).

### 4.3 Draft discipline

- Save **never publishes**. New sets/tests are `is_published = false`; sections are `DRAFT`.
- Publishing is a separate catalog admin action (SPEC-11).

### 4.4 Persist everything generated (fixes)

The save contract **must** persist the full generated payload (the old saver dropped some):
- **Listening `audio_url`** — accepted in the save request and written to `sections.audio_url`.
- **Writing** `chart_data` / `letter_context` / `essay_metadata` → `section_layout` JSONB;
  `sample_answer` / `band_breakdown` / `key_phrases` / `grading_notes` → a dedicated JSONB
  field on the writing section (`section_layout.writing_meta`), so review/grading can use them.
- `figure_description` → `sections.image_description`.

## 5. Save response

`{ success, setId, setCode, testId, testNumber, sectionIds[], questionCount, message }`.
Failures propagate to the global handler (SPEC-04) — **no `200 {success:false}`** wrapping.

## 6. Change log

| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring (target spec). |
