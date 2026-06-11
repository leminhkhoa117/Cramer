# SPEC-22 — ABTS Prompting & JSON Schema

> Status: **Authoritative** · Module: `abts` · Depends on: SPEC-20, SPEC-21

---

## 1. Principles

- Every generation call is **JSON-schema constrained**: OpenRouter `response_format` =
  `{ type: "json_schema", json_schema: <schema>, strict: true }`.
- Prompts enforce: **English** passages/questions/transcripts; structured explanations;
  IELTS-correct question-type formats; part-specific word ranges and numbering starts.
- Prompts are assembled from **reusable fragments** so rules live in one place.
- A `PromptBuilder` per skill composes: role/system framing → IELTS constraints →
  part/task constraints → question-type instructions → facts/strict block → output schema
  reminder.

## 2. Prompt builders

| Builder | Responsibility |
|---------|----------------|
| `ReadingPromptBuilder` | passage + question prompts; part word ranges; numbering starts; type formats; facts/strict. |
| `ListeningPromptBuilder` | transcript context + speaker patterns; allowed types; `section_layout.blocks`; `question_numbers`; audio placeholder; map/plan figure descriptions; phase separation (stems w/o answers, then answers). |
| `WritingPromptBuilder` | Academic T1 (chart/data), GT T1 (letter), T2 (essay); task / sample / band-breakdown phases. |
| `PromptFragments` | shared explanation + word-limit fragments. |
| `QuestionTypeInstructionBuilder` | per-type authoring rules (single source; **must be used**, not bypassed by inline text). |
| `PromptSchemaBuilder` | builds full + phase-specific JSON schemas. |

> **Fix:** consolidate question-type instructions into `QuestionTypeInstructionBuilder` and
> reference it from all skill builders (the old code embedded duplicate inline instructions).

## 3. Facts / strict mode

- **STRICT** (≥3 facts/part): instruct the model to construct content **only** from the
  supplied facts; do not invent contradicting data.
- **AUTO**: instruct the model to research/use plausible academic details.

## 4. Phase-specific prompts & schemas

| Skill | Phase | Prompt focus | Schema |
|-------|-------|--------------|--------|
| Reading | passage | passage only, word range | passage schema |
| Reading | questions | questions for range, types, answers, explanations | questions schema |
| Listening | transcript | transcript + audio placeholder | transcript schema |
| Listening | stems | stems + layout, **no answers** | stems schema |
| Listening | answers | `answers[]` keyed by number + explanations/evidence | answers schema |
| Writing | task | task prompt + requirement (+chart/letter/essay meta) | task schema |
| Writing | sample | sample answer | sample schema |
| Writing | band | band breakdown + key phrases + grading notes | band schema |

Full (non-streaming) schemas exist per skill for the synchronous path.

## 5. Content data shapes (align with catalog, SPEC-11)

Generated JSON maps to catalog JSONB columns on save (SPEC-24). Canonical shapes:

### 5.1 Reading section
```jsonc
{ "section": { "passage_text": "<html/text>" },
  "questions": [
    { "question_number": 1, "question_type": "FILL_IN_BLANK",
      "question_content": { "text": "... ____ ..." },
      "correct_answer": ["population"], "word_limit": "ONE WORD",
      "explanation": { "text": "...", "evidence": "..." } }
  ] }
```

### 5.2 Question-type content rules (selected)
- `FILL_IN_BLANK` / `*_COMPLETION`: `question_content.text` holds the blank `____`;
  `correct_answer` is an array of acceptable strings; `word_limit` set when constrained.
- `TABLE_COMPLETION` / `FLOW_CHART_COMPLETION`: the **first** question in the group carries
  the full table/chart HTML in `text`; subsequent group questions use `{ "text": "" }`.
- `TRUE_FALSE_NOT_GIVEN` / `YES_NO_NOT_GIVEN`: `correct_answer ∈ {TRUE,FALSE,NOT GIVEN}` /
  `{YES,NO,NOT GIVEN}`.
- `MULTIPLE_CHOICE`: `{ text, options[] }`, `correct_answer` = one letter.
- `MULTIPLE_CHOICE_MULTIPLE_ANSWERS`: same shape, `correct_answer` = **array of letters**
  (graded as a set, SPEC-12).
- `MATCHING_*`: `{ text, options[] }` (options may be `{letter,text}` objects);
  `correct_answer` = selected letter/roman numeral.

### 5.3 Listening
- `section_layout = { blocks: [ { type, instructions?, title?, question_numbers[],
  image_url?, options? } ] }`.
- `NOTE_COMPLETION` blocks wrap `FILL_IN_BLANK` questions; `PLAN_MAP_DIAGRAM_LABELING` and
  `MATCHING_FEATURES` carry shared `image_url`/`options` and group `MATCHING` questions.
- `audio_placeholder` metadata captured at transcript phase; **real `audio_url` is supplied at
  save time** (SPEC-24 — fix: persist Listening audio URLs).

### 5.4 Writing
- `task_prompt`, `word_requirement`, `task_type`.
- `ACADEMIC_TASK_1`: `chart_data` (axes/series, pie totals, table rows/headers, process/map
  figure descriptions).
- `GENERAL_TASK_1`: `letter_context`.
- `TASK_2`: `essay_metadata`.
- Plus `sample_answer`, `band_breakdown`, `key_phrases`, `grading_notes`.
  **Fix:** these must be persisted (SPEC-24), not previewed-then-dropped.

## 6. Output discipline

- Schemas use `strict: true`; the model must return only schema-conforming JSON.
- Explanations are objects (`{ text, evidence? }`), never bare strings, so review UI can show
  evidence.
- No Vietnamese in passages/questions; explanations follow `explanationLanguage` (a Vietnamese
  explanation when `en` is requested is a validation **warning**, SPEC-23).

## 7. Change log

| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring (target spec). |
