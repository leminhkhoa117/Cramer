# SPEC-25 — ABTS HTTP/SSE API & Admin UX

> Status: **Authoritative** · Module: `abts` · Depends on: SPEC-20–24

---

## 1. API surface

All endpoints are under `/api/admin/abts`, **admin-only** (SPEC-04 §1). User id comes from
the token; **no `X-User-Id` header**. Errors use the global model (SPEC-04 §2) — ABTS does
**not** return `200 {success:false}`.

| Method · Path | Purpose | Body | Result |
|---|---|---|---|
| `POST /generate/{skill}` | sync generate (`reading\|listening\|writing`) | `GenerationRequest` | `GenerationResponse` |
| `POST /generate/{skill}/stream` | streaming generate | `GenerationRequest` | **SSE** `StreamEvent` |
| `POST /generate/questions` | regenerate questions vs existing passage/transcript | `GenerationRequest` (+`existingPassageText`) | `GenerationResponse` / 400 |
| `POST /validate` | validate arbitrary generated JSON | content JSON | `ValidationResult` |
| `POST /refine/stream` | streaming refine selected issues | `RefinementRequest` | **SSE** `StreamEvent` (`REFINEMENT_COMPLETED`) |
| `POST /refine/apply` | apply accepted hunks | `RefinementApplyRequest` | `RefinementApplyResponse` |
| `POST /save` | save content as draft | `SaveContentRequest` | `SaveContentResponse` |
| `GET /models` | model catalog + capabilities | — | model list |
| `GET /models/capabilities/{id}` | one capability descriptor | — | descriptor |
| `GET /templates` | template categories | — | categories |
| `GET /templates/{categoryId}` | active templates | — | templates |
| `GET /status` | key status, defaults, timeouts, round cap, version | — | status |

`{skill}` ∈ `reading|listening|writing`; `speaking` → `NOT_IMPLEMENTED`.

### 1.1 Streaming transport

- `*/stream` endpoints return `text/event-stream`. The HTTP connection is accepted first;
  capacity rejection / failure / completion / abort are all delivered as `StreamEvent`s
  (SPEC-21 §5), then the stream closes.
- The client reads `data.type` to drive UI state and may add client-only states
  (`CONNECTING`, `SENDING`, `CONNECTED`, `TIMEOUT_WARNING`) that the backend never sends.

## 2. Admin UX — AI Studio

Top-level flow: **Configure → Generate → Review → Save**.

### 2.1 Configure

Controls: skill; model; difficulty; explanation language; facts mode (auto/custom);
selected parts/tasks; per-part topic; passage length; question types; balanced/random counts;
hashtags; temperature; max tokens; reasoning controls; context cache; custom instructions;
live request-JSON preview.

**Readiness rules** (generate disabled until met):
- Reading/Listening: ≥1 part; each selected part has a topic and **≥2 question types**;
  custom-facts mode requires **≥3 facts per part**.
- Writing: ≥1 task and a topic; custom-facts mode requires **≥3 global facts**.

### 2.2 Generate

Live streaming display shows: response chunks, progress, recent events, reasoning/model
trace, partial part errors, and a **Stop** control (triggers client abort → backend
cancellation, SPEC-21 §6).

### 2.3 Review

- Reading/Listening render in a preview with **answer toggles** and inline **question
  editing**; Listening adds transcript copy/view and map/image helpers.
- Writing renders task prompt + details.
- **Issue rail**: validation counts (errors/warnings), issue selection for refine, **hunk
  diff review** (accept/reject, skipped-hunk reasons), **Apply Accepted**, **Refine Again**.

### 2.4 Save

Save modal: select/create a test set; choose/auto-increment a test number; name the test;
difficulty; hashtags; append to an existing test missing the current skill; **Listening audio
URLs** (now persisted, SPEC-24 §4.4). Saved content is **draft**.

## 3. Business rules (consolidated)

- Admin-only; OpenRouter key required.
- Reading/Listening/Writing only.
- Max 3 generation attempts; refinement bounded by `abts.max-refinement-rounds`.
- Max 20 hashtags; hashtag codes lowercase alphanumeric/`_`/`-`.
- Save content is draft/unpublished; saving replaces the questions of an existing
  same-`(test,skill,part)` section.
- Cancellation stops upstream token usage (SPEC-21 §6).

## 4. Change log

| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring (target spec). |
