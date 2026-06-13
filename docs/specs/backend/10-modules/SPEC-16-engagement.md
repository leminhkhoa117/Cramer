# SPEC-16 — Engagement (Chat, Vocabulary, Dashboard, Activity)

> Status: **Authoritative** · Module: `engagement` · Depends on: SPEC-11, SPEC-12, SPEC-13, SPEC-15

---

## 1. Data model (existing tables)

| Table | Key fields |
|-------|-----------|
| `chat_messages` | user_id, role, content, tokens_used, created_at |
| `chatbot_usage` | user_id, usage_date, messages_used; unique (user_id, usage_date) |
| `vocabulary` | user_id, word, translation, phonetic, part_of_speech, definition, example_sentence, source_context, source_test_id, source_section_id, notes, is_mastered, review_count, last_reviewed_at |
| `target` | user_id (unique), exam_name, exam_date, listening, reading, writing, speaking |
| `user_activities` | user_id, activity_type, title, description, metadata (JSONB), created_at |

## 2. AI chat

| Method · Path | Auth | Purpose |
|---|---|---|
| `POST /api/chat` | user | ask the IELTS assistant |
| `GET /api/chat/history?limit` | user | history (newest first, cap 100) |
| `GET /api/chat/remaining` | user | remaining monthly allowance |
| `DELETE /api/chat/history` | user | clear history |

- DeepSeek `deepseek-chat`, `max_tokens=500`, `temperature=0.7`, Vietnamese IELTS-assistant
  system prompt. Persist user + assistant messages.
- **Billing = `ChatBillingPort`, charged AFTER a successful reply** (SPEC-15 §6). Monthly
  allowance from the subscription tier; over-allowance charges overage Lúa
  (`CHAT_EXTENSION`). **Fix:** old code charged before the call and double-counted via the
  daily `chatbot_usage` table — the **monthly subscription counter is the source of truth**;
  `chatbot_usage` is analytics-only (or dropped).
- **Fix:** do not duplicate the current user message in the LLM context (save-then-fetch bug).

## 3. Vocabulary notebook

| Method · Path | Auth | Purpose |
|---|---|---|
| `GET /api/vocabulary?page&size&sortBy&sortDir&search&filter` | user | list/search/filter |
| `GET /api/vocabulary/{id}` | owner | one entry |
| `POST /api/vocabulary` | user | create (409 duplicate word/user) |
| `PUT /api/vocabulary/{id}` | owner | update |
| `DELETE /api/vocabulary/{id}` | owner | delete (204) |
| `PUT /api/vocabulary/{id}/toggle-mastered` | owner | toggle + review_count++ |
| `GET /api/vocabulary/stats` | user | totals/mastered/learning/% |
| `POST /api/vocabulary/translate` | user | AI translate a word |

- Duplicate prevention per `(user_id, word)`.
- **Translate** uses the **server** DeepSeek key; returns
  `{translation, phonetic, partOfSpeech, definition, exampleSentence}`. Billing =
  `TranslationBillingPort`, **charged after success** (SPEC-15 §6, category
  `VOCABULARY_TRANSLATION`).

## 4. Dashboard (read-model)

| Method · Path | Auth | Purpose |
|---|---|---|
| `GET /api/dashboard/summary?page&size&search` | user | aggregate |
| `GET /api/dashboard/course-history?examSource&testNumber&skill` | user | per-attempt history |
| `POST /api/dashboard/target` | user | upsert IELTS goal |

- **Summary** aggregates: profile, target, course progress, per-skill answer accuracy, user
  stats, recent activity, goals. Course progress groups attempts by
  `exam_source+test_number+skill` (excluding `CANCELLED`), takes the latest, computes
  answered/correct/completion, Reading/Listening band (`BandScale`), and weighted Writing band
  (Task1·⅓ + Task2·⅔).
- **Fixes:** correct the `UserStats` field mapping (tests-completed / questions-answered /
  correct-answers were mis-assigned); course progress no longer returns an empty `history`
  inside each item — history is the dedicated `course-history` endpoint.
- Dashboard reads attempts/answers/writing tables **read-only** as a projection; it does not
  write through other modules' tables.

## 5. Target
- One per user; bands 0–9 (DB check constraint); `exam_name`, `exam_date`. Upsert semantics.

## 6. Activity (`ActivityPort`)
- `ActivityService` logs `user_activities` for: test completion, vocab save, subscription
  change, login, achievement, credit change. Published as `ActivityPort` for other modules.
- Admin reads activities via SPEC-17.

## 7. Ports
- Consumes: `ChatBillingPort`, `TranslationBillingPort`, `FeatureAccessPort` (gate vocab AI /
  chatbot), `DeepSeekClient`.
- Publishes: `ActivityPort`.

## 8. Change log
| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
