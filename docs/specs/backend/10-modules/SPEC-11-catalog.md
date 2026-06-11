# SPEC-11 — Catalog (Content Hierarchy, Delivery, Admin, Browse)

> Status: **Authoritative** · Module: `catalog` · Depends on: SPEC-04
> Owns the IELTS content hierarchy, safe test delivery, course browsing, and **one** typed
> admin content API. Save target for ABTS (SPEC-24).

---

## 1. Data model (existing tables)

```
test_sets ──< tests ──< sections ──< questions
                 └──< test_hashtags >── hashtags
```

| Table | Key fields |
|-------|-----------|
| `test_sets` | code (unique), name, description, cover_image_url, source_type, is_published, display_order, created_by |
| `tests` | set_id, test_number, name, description, difficulty, estimated_time_minutes, is_published, is_ai_generated, generation_metadata (JSONB), created_by; unique (set_id, test_number) |
| `sections` | test_id, **exam_source**, **test_number** (legacy), skill, part_number, passage_text, audio_url, section_layout (JSONB), image_description, display_content_url, status (DRAFT/PUBLISHED/ARCHIVED) |
| `questions` | section_id, question_number, question_uid (unique), question_type, question_content (JSONB), correct_answer (JSONB), explanation (JSONB), word_limit, image_url |
| `hashtags` | code, name, category, icon, color, use_count, is_active |
| `test_hashtags` | test_id, hashtag_id, is_primary |

### 1.1 Content identity (target)
- **FK-first**: new code resolves content through `test_id`.
- Legacy `sections.exam_source/test_number` remain (schema frozen) and are used as a **lookup
  shim** by `assessment`/`speaking` runtime (which key on exam_source/test_number/skill). The
  catalog exposes both resolution paths via `ContentLookupPort` (§5) but treats `test_id` as
  canonical.

### 1.2 Skills & question types
- Skills: `READING`, `LISTENING`, `WRITING`, `SPEAKING` (Speaking authored content lives here:
  `sections.skill = speaking`, question types `PART_1/2/3`, `question_content` per
  SPEC-14 §3). The `Skill` and `QuestionType` enums are **shared-kernel vocabulary** in
  `platform.common.ielts` (not catalog-owned), so assessment/speaking/abts use them without
  depending on catalog's domain (SPEC-01 §3). Catalog persists `sections.skill` via a
  `SkillConverter` (lowercase in DB).
- Reading/Listening/Writing question types & JSONB shapes: SPEC-22 §5 (single source).

## 2. Test delivery (test-taking)

| Method · Path | Auth | Result |
|---|---|---|
| `GET /api/tests/data?source&test&skill` | user | `List<TestSectionView>` (answer-free) |

- Returns sections (ordered by `part_number`) + questions (ordered by `question_number`) as
  **`TestSectionView`/`TestQuestionView`** with **no `correct_answer`/`explanation`**.
- 404 if no sections match.
- **Answer-safety (hard rule, SPEC-04 §3):** there is **no** authenticated endpoint that
  returns answer keys outside review/admin. The old generic `GET /api/questions/**` and
  `GET /api/sections/{id}/full` answer-bearing endpoints are **removed**. Answers are
  available only via `assessment` review (owner) and `catalog` admin endpoints.

## 3. Course browsing

| Method · Path | Auth | Result |
|---|---|---|
| `GET /api/courses?page&size&search` | user | `PageResponse<String>` (published exam sources) |
| `GET /api/courses/v2` | user | published `TestSetView[]` |
| `GET /api/courses/{course}/tests` | user | published test numbers |
| `GET /api/courses/{code}/details` | user | `TestSetView` or 404 |

Only **published** sets/tests are visible to users.

## 4. Admin content API (one typed surface)

> **Fix:** the old dual API (typed `/api/admin/**` + raw-SQL `/api/admin/content/**`) is
> **collapsed into one** typed, DTO-based API under `/api/admin`. The raw-SQL CMS, its
> map payloads, local `200 {success:false}` handling, and the surprising "create test also
> creates a placeholder Reading section" behavior are **removed**.

Test sets: `GET/POST/PUT/DELETE /api/admin/test-sets[/{id}]`, `…/code/{code}`,
`POST …/{id}/publish|unpublish`, `POST …/reorder`.
Tests: `GET …/test-sets/{setId}/tests`, `GET/PUT/DELETE /api/admin/tests/{id}`,
`GET …/tests/lookup?setCode&testNumber`, `POST …/test-sets/{setId}/tests`,
`POST …/tests/{id}/publish|unpublish|duplicate`, `PUT …/tests/{id}/hashtags`,
`GET …/tests/{id}/sections?skill`.
Sections: `GET/POST/PUT/DELETE /api/admin/sections[/{id}]` (admin-gated; replaces generic CRUD).
Questions: `GET/POST/PUT/DELETE /api/admin/questions[/{id}]`.
Hashtags: `GET/POST/PUT/DELETE /api/admin/hashtags[/{id}]`, `…/category/{c}`, `…/search?q`,
`…/popular?limit`, `…/categories`.

### 4.1 Admin rules
- Test set: `code`+`name` required, max lengths, `code` unique. Defaults `source_type=custom`,
  `is_published=false`.
- Test: `test_number ≥ 1`, unique `(set_id, test_number)`; auto `max+1` when omitted.
  Defaults `difficulty=INTERMEDIATE`, `estimated_time_minutes=170`, unpublished, not AI.
- **Publish cascade:** publishing/unpublishing a test sets its sections to
  `PUBLISHED`/`DRAFT` (both FK-linked and legacy `exam_source/test_number` sections).
- **Duplicate** copies test metadata + hashtags, unpublished. (Section duplication is an
  explicit opt-in flag; default off — documented so callers aren't surprised.)
- Hashtags are **soft-deleted** (`is_active=false`); search/list return active only;
  `findOrCreateByCodes` creates missing as category `topic`; max 20 per test; codes match
  `^[a-z0-9_-]+$`. Use counts increment/decrement on attach/detach.
- **Delete** uses JPA cascade/explicit repository deletes covering dependents
  (questions, test_hashtags; attempts/answers/writing belong to other modules and are not
  silently destroyed — deletion of a test with user data is blocked with 409 unless forced).

## 5. Ports (published)

```java
interface ContentLookupPort {
  List<SectionRef> sectionsForTest(long testId, Skill skill);          // FK path
  List<SectionRef> sectionsForExam(String examSource, int testNumber, Skill skill); // legacy shim
  List<GradableQuestion> gradableQuestions(long sectionId);            // includes correct_answer (assessment only)
  List<SpeakingQuestionRef> speakingBank(long testId, SpeakingPart part); // for speaking blueprint
}
```
- Answer-bearing methods are for **server-side scoring/blueprint only**, never exposed over HTTP.

## 6. Change log
| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
| 11/06/2026 | `Skill`/`QuestionType` relocated to `platform.common.ielts` (shared kernel); catalog keeps `SkillConverter` for lowercase persistence. |
