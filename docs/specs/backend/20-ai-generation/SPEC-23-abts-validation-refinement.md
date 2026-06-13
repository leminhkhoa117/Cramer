# SPEC-23 — ABTS Validation & Refinement

> Status: **Authoritative** · Module: `abts` · Depends on: SPEC-20–22

---

## 1. Validation pipeline

Validation runs in layers; each layer can add findings without aborting the next:

1. **Lenient JSON parse** — tolerate minor model formatting noise.
2. **Schema checks** — required fields/types per skill+phase (SPEC-22).
3. **Content checks** — counts, sequences, duplicates, placeholders.
4. **IELTS/business rules** — per-skill authenticity rules.

### 1.1 ValidationResult

```jsonc
{ "valid": false,
  "schemaErrors": ["..."], "contentErrors": ["..."],
  "businessRuleErrors": ["..."], "warnings": ["..."],
  "issues": [ { "id": "rd-q5-missing-answer", "severity": "ERROR",
                "path": "/questions/4/correct_answer", "message": "..." } ] }
```

- `issues[]` are **structured** with a **stable id** and best-effort **JSON-pointer path** so
  the UI can target a specific finding for refinement.
- `valid = (no schema/content/business errors)`. Warnings do not block.

## 2. Reading rules

Require `section.passage_text` and `questions[]`. Check: required question fields; allowed
question types; question count + sequence for the part range; **no duplicate numbers**;
completion `word_limit` present where needed; blank placeholders present; MC/matching answer
validity; group option consistency; table/flow first-question shared content; requested
type counts satisfied; **at least two question types** per part. Passage word count =
**warning** only. Vietnamese explanation when `en` requested = **warning**.

## 3. Listening rules

Require `transcript`, `questions`, `section_layout`, `audio_placeholder`. Transcript word
count outside the part range = **content error**. Speaker labels required. Each part = exactly
the correct count (10 per part). Allowed interaction types only (SPEC-20 §4.2). Layout blocks
must be allowed types with non-empty `question_numbers`. Map/plan labeling requires
image/options in the layout and a `figure_description` for Part 2.

## 4. Writing rules

Require `task_prompt` and `word_requirement`. Warn on missing `task_type`. Require
`chart_data` for Academic Task 1, `letter_context` for GT Task 1, `essay_metadata` for
Task 2. Chart data: validate axes/series, pie totals, table rows/headers, process/map figure
descriptions. Sample answer min word count = **warning**.

## 5. Refinement (C4) — single, coherent patch flow

Refinement makes **targeted fixes** to selected issues; it never regenerates the whole part.

> **Fix:** the old system had two overlapping patch mechanisms (Agent-2 `patches` and
> diff `hunks`). The target is **one** flow: model proposes patches → patches applied →
> result diffed into hunks → author accepts/rejects hunks → accepted hunks applied. Both
> errors **and** warnings (selected `issue.id`s) are eligible for refinement.

### 5.1 Flow

1. Author selects one or more `issue.id`s in the review UI.
2. Client sends a **refinement request**: `originalJson`, selected issue ids, `skill`,
   `part`, `model`, caching/reasoning flags, `round`, and the current `validation` result.
3. Backend rejects an empty selection and enforces `abts.max-refinement-rounds`.
4. The refinement prompt asks the model to return `{ "patches": [ { "op":
   "replace|insert|append", "questionNumber"?, "path", "value" } ] }`.
5. Patches are applied to `originalJson` (resolving `questionNumber + path` to the matching
   `questions[index]` path).
6. The refined JSON is parsed for validity, then **diffed** into RFC-6902-style **hunks**.
7. Stream `REFINEMENT_COMPLETED` with the hunks; the round counter increments.

### 5.2 Apply

- `POST /refine/apply` applies only the **accepted** hunks to the original content.
- Per-hunk failures are **skipped** (reported in `skipped[]`), not fatal.
- After apply, the client re-runs `/validate` on the patched content.
- Malformed `originalJson` → 400; unexpected apply failure → 500; partial skips → 200 with
  `skipped` details.

## 6. Edge cases

- Refinement on already-valid content is allowed (e.g. to act on warnings).
- Hunks default to **accepted** in the UI; the author can deselect.
- Round cap reached → refinement rejected with a clear message.

## 7. Change log

| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring (target spec). |
