# SPEC-20 — ABTS Overview & IELTS Domain Model

> Status: **Authoritative** · Module: `abts` · Audience: backend + admin frontend
> This is a **brand-new functional spec** for AI-Based Test generation. It describes the
> **target** feature set, UX, and behavior — independent of any current file layout. Where the
> current implementation falls short, this spec states the **required** behavior; code follows.

---

## 1. Purpose

ABTS lets an **admin content author** generate authentic-style IELTS **Reading, Listening,
and Writing** content with AI, review/validate/refine it, and save it into the Cramer content
catalog as **draft** material. Speaking generation is **out of scope** (requests for Speaking
return `NOT_IMPLEMENTED`).

## 2. Actors & access

- **Admin author** — the only actor. All ABTS endpoints require an authenticated admin
  (`profiles.is_admin = true`); they live under `/api/admin/abts/**` (SPEC-04 §1).
- No end-user ever calls ABTS.

## 3. Capability map

| # | Capability | Summary |
|---|-----------|---------|
| C1 | **Configure** | Choose skill, parts/tasks, topic, difficulty, facts mode, question types/counts, model, reasoning, language. |
| C2 | **Generate** | Produce content, synchronously or via **SSE streaming**, for one part or many. |
| C3 | **Validate** | Check generated JSON against schema + IELTS business rules; surface structured issues. |
| C4 | **Refine** | Targeted, patch-based fixes to selected issues (not full regeneration), with diff review. |
| C5 | **Preview/Edit** | Render and hand-edit content before saving. |
| C6 | **Save** | Persist into the catalog hierarchy as **draft**, into an existing or new set/test. |
| C7 | **Catalog/status** | List models + capabilities, templates, and service status. |

The end-to-end studio flow is **Configure → Generate → Review (validate + refine + edit) →
Save** (SPEC-25 covers the UX in detail).

## 4. IELTS domain model

### 4.1 Skills & parts

| Skill | Parts | Question numbering |
|-------|-------|--------------------|
| Reading | Part 1, 2, 3 | P1 Q1–13, P2 Q14–26, P3 Q27–40 |
| Listening | Part 1, 2, 3, 4 | P1 Q1–10, P2 Q11–20, P3 Q21–30, P4 Q31–40 |
| Writing | Task 1, Task 2 | task-numbered (no question range) |

These ranges are the **single source of truth** for numbering and are used to renumber
questions during multi-part merges (SPEC-21 §4). They must exist in exactly **one** place in
code (a `QuestionRange` type), not duplicated across prompt/save/frontend.

### 4.2 Question types

**Reading** (generation + validation):
`FILL_IN_BLANK`, `SUMMARY_COMPLETION`, `SUMMARY_COMPLETION_OPTIONS`, `TRUE_FALSE_NOT_GIVEN`,
`YES_NO_NOT_GIVEN`, `MATCHING_INFORMATION`, `MATCHING_HEADINGS`, `MATCHING_FEATURES`,
`MATCHING_SENTENCE_ENDINGS`, `MULTIPLE_CHOICE`, `MULTIPLE_CHOICE_MULTIPLE_ANSWERS`,
`TABLE_COMPLETION`, `FLOW_CHART_COMPLETION`, `DIAGRAM_LABEL_COMPLETION`.

**Listening** — interaction types: `FILL_IN_BLANK`, `MULTIPLE_CHOICE`,
`MULTIPLE_CHOICE_MULTIPLE_ANSWERS`, `MATCHING`. Layout block types (UI grouping):
`NOTE_COMPLETION`, `INSTRUCTIONS_ONLY`, `MATCHING_FEATURES`, `PLAN_MAP_DIAGRAM_LABELING`.

**Writing** — task types: `ACADEMIC_TASK_1` (chart/data), `GENERAL_TASK_1` (letter),
`TASK_2` (essay).

> Question-type data shapes (JSONB `question_content`/`correct_answer`) are defined in
> SPEC-22 §5 and align with the catalog contract in SPEC-11.

### 4.3 Content modes

- **Strict / facts mode** — when the author supplies ≥3 facts per part, the prompt operates in
  "Strict Mode": the model must build content around the supplied facts only.
- **Auto / research mode** — fewer facts: the model invents plausible academic details.

### 4.4 Reasoning

Models with reasoning support can run with `enableReasoning`, `reasoningEffort`, and
`reasoningBudget`. Reasoning deltas stream as `AI_THINKING`; final reasoning is attached to
the generation result.

## 5. Glossary

| Term | Meaning |
|------|---------|
| **Part** | A Reading/Listening part or a Writing task. |
| **Phase** | A sub-step of generating one part (e.g. Reading: passage phase → questions phase). |
| **Issue** | A structured validation finding with a stable id and JSON-pointer path. |
| **Hunk** | An RFC-6902-style before/after diff segment the author accepts/rejects during refine. |
| **Strict mode** | Fact-constrained generation. |
| **Draft** | Saved content that is unpublished (`is_published=false`, section `status=DRAFT`). |

## 6. End-to-end pipeline (target)

```mermaid
flowchart LR
  CFG[Configure] --> GEN[Generate (sync/stream, 1..N parts)]
  GEN --> VAL[Validate -> issues]
  VAL -->|issues| REF[Refine (patch + hunk review)]
  REF --> VAL
  VAL -->|ok / accepted| EDIT[Preview & manual edit]
  EDIT --> SAVE[Save as draft into catalog]
```

## 7. Cross-references

- Generation & streaming: SPEC-21
- Prompting & JSON schema: SPEC-22
- Validation & refinement: SPEC-23
- Models & persistence: SPEC-24
- HTTP/SSE API & Admin UX: SPEC-25
- Catalog data contract (save target): SPEC-11

## 8. Change log

| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring (target spec). |
