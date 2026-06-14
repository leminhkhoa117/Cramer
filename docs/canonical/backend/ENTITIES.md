# Cramer Backend — JPA Entity Reference

> **Last Updated:** 14/06/2026
> **Version:** 2.0.0 (regenerated from vertical-slice code)
> **Tech:** Spring Boot 4.0.0 · Java 25 · Hibernate ORM · Hypersistence/`SqlTypes.JSON` JSONB · Lombok
> **Database:** PostgreSQL (Supabase), schema `public`

This document is regenerated **directly from the `.java` source** of the vertical-slice
backend (`backend/src/main/java/com/cramer/<module>/domain/`). Every class, table, column,
type, and enum below was verified by reading the source.

> **Supersedes the legacy `ENTITIES.md` (v1, 17/05/2026).** The old document described the
> deleted `com.cramer.entity` package and classes such as `IeltsTest`, `TestAttempt`, and
> `ChatbotUsage`, plus JPA association mappings (`@ManyToOne`/`@OneToMany`/`@ManyToMany`).
> **None of that exists anymore.** Do not trust the legacy file.

---

## Table of Contents

1. [Conventions](#conventions)
2. [Entity → Table Mapping (summary)](#entity--table-mapping-summary)
3. [Module: `identity`](#module-identity)
4. [Module: `catalog`](#module-catalog)
5. [Module: `assessment`](#module-assessment)
6. [Module: `writing`](#module-writing)
7. [Module: `speaking`](#module-speaking)
8. [Module: `billing`](#module-billing)
9. [Module: `engagement`](#module-engagement)
10. [Module: `admin`](#module-admin)
11. [Module: `platform` (shared enums)](#module-platform-shared-enums)
12. [Module: `abts` (no entities)](#module-abts-no-entities)
13. [Enum Catalogue](#enum-catalogue)
14. [Known Tables Without a JPA Entity](#known-tables-without-a-jpa-entity)
15. [Verification Notes](#verification-notes)

---

## Conventions

These patterns hold across (nearly) every entity and are **not** repeated in each table:

- **No JPA associations.** Verified: there are **zero** `@ManyToOne`, `@OneToMany`,
  `@OneToOne`, `@ManyToMany`, `@JoinColumn`, or `@JoinTable` annotations in the entire
  `com.cramer` tree. All relationships are modelled as **plain FK ID columns**
  (`Long`/`UUID`), the "FK-first" rule. Relationship semantics in this doc are inferred from
  column names, not from mapped associations.
- **Lombok.** Entities use `@Getter @Setter` (no `@Builder`/`@*Constructor` on the entities
  read). The composite-key `@Embeddable` adds `@NoArgsConstructor @AllArgsConstructor
  @EqualsAndHashCode`.
- **Primary keys.** Most use `@GeneratedValue(strategy = GenerationType.IDENTITY)` on a
  `Long id`. Exceptions are called out (`Profile`, `Target`, `TestHashtag`).
- **JSONB.** Stored via `@JdbcTypeCode(SqlTypes.JSON)` on a `com.fasterxml.jackson.databind.JsonNode`
  field with `columnDefinition = "jsonb"`. (No entity uses `@Type(JsonType.class)`.)
- **Timestamps.** `@CreationTimestamp` / `@UpdateTimestamp` (Hibernate) on `OffsetDateTime`
  columns (`created_at`, `updated_at`, etc.).
- **Schema.** Every `@Table` declares `schema = "public"`.
- **Enums.** Default mapping is `@Enumerated(EnumType.STRING)`. Two columns instead use a
  custom `AttributeConverter` to persist **lowercase** (`Section.skill`,
  `SpeakingSession.status`).

---

## Entity → Table Mapping (summary)

26 `@Entity` classes + 1 `@Embeddable` composite key.

| # | Module | Entity (class) | `@Table` | PK | Generation |
|---|--------|----------------|----------|----|------------|
| 1 | identity | `Profile` | `profiles` | `id` UUID | none (== `auth.users.id`) |
| 2 | catalog | `TestSet` | `test_sets` | `id` Long | IDENTITY |
| 3 | catalog | `Test` | `tests` | `id` Long | IDENTITY |
| 4 | catalog | `Section` | `sections` | `id` Long | IDENTITY |
| 5 | catalog | `Question` | `questions` | `id` Long | IDENTITY |
| 6 | catalog | `Hashtag` | `hashtags` | `id` Long | IDENTITY |
| 7 | catalog | `TestHashtag` | `test_hashtags` | `TestHashtagId` (composite) | `@EmbeddedId` |
| 8 | assessment | `Attempt` | `test_attempts` | `id` Long | IDENTITY |
| 9 | assessment | `UserAnswer` | `user_answers` | `id` Long | IDENTITY |
| 10 | writing | `WritingSubmission` | `writing_submissions` | `id` Long | IDENTITY |
| 11 | speaking | `SpeakingSession` | `speaking_sessions` | `id` Long | IDENTITY |
| 12 | speaking | `SpeakingTranscript` | `speaking_transcripts` | `id` Long | IDENTITY |
| 13 | billing | `SubscriptionTier` | `subscription_tiers` | `id` Long | IDENTITY |
| 14 | billing | `UserSubscription` | `user_subscriptions` | `id` Long | IDENTITY |
| 15 | billing | `UserCredit` | `user_credits` | `id` Long | IDENTITY |
| 16 | billing | `CreditTransaction` | `credit_transactions` | `id` Long | IDENTITY |
| 17 | billing | `PaymentOrder` | `payment_orders` | `id` Long | IDENTITY |
| 18 | billing | `LuaPack` | `lua_packs` | `id` Long | IDENTITY |
| 19 | billing | `UserQuota` | `user_quotas` | `id` Long | IDENTITY |
| 20 | billing | `SkillQuota` | `skill_quotas` | `id` Long | IDENTITY |
| 21 | billing | `TranslationUsage` | `translation_usage` | `id` Long | IDENTITY |
| 22 | engagement | `Target` | `target` | `id` UUID | none (DB default) |
| 23 | engagement | `ChatMessage` | `chat_messages` | `id` Long | IDENTITY |
| 24 | engagement | `UserActivity` | `user_activities` | `id` Long | IDENTITY |
| 25 | engagement | `Vocabulary` | `vocabulary` | `id` Long | IDENTITY |
| 26 | admin | `AdminAuditLog` | `admin_audit_log` | `id` Long | IDENTITY |

All 26 tables are present in the known live table set. No entity maps to an unknown table.

**Entity count per module:** identity 1 · catalog 6 · assessment 2 · writing 1 · speaking 2
· billing 9 · engagement 4 · admin 1 · platform 0 · abts 0 → **26 total**.

---

## Module: `identity`

`backend/src/main/java/com/cramer/identity/domain/`

### `Profile` → `profiles`

FQN: `com.cramer.identity.domain.Profile` · File: `identity/domain/Profile.java`
Mirror of the Supabase auth user; the backend never creates/deletes profiles.

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `UUID` | **PK**, not null, not updatable; `== auth.users.id` (no `@GeneratedValue`) |
| `username` | `username` | `String` | not null, **unique** |
| `fullName` | `full_name` | `String` | |
| `phoneNumber` | `phone_number` | `String` | |
| `address` | `address` | `String` | |
| `avatarUrl` | `avatar_url` | `String` | |
| `heroBackgroundUrl` | `hero_background_url` | `String` | |
| `pageBackgroundUrl` | `page_background_url` | `String` | |
| `llmApiKey` | `llm_api_key` | `String` | never exposed; `hasLlmApiKey()` helper |
| `llmModel` | `llm_model` | `String` | |
| `llmProvider` | `llm_provider` | `String` | default `"deepseek"` |
| `isAdmin` | `is_admin` | `Boolean` | default `false` |
| `accountStatus` | `account_status` | `AccountStatus` | `@Enumerated(STRING)`, default `ACTIVE` |
| `statusReason` | `status_reason` | `String` | |
| `lastLoginAt` | `last_login_at` | `OffsetDateTime` | |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |

**Enums:** `AccountStatus` { `ACTIVE`, `BANNED`, `DEACTIVATED`, `DELETED` }.

---

## Module: `catalog`

`backend/src/main/java/com/cramer/catalog/domain/`

Content hierarchy: `test_sets` → `tests` → `sections` → `questions`, plus `hashtags` and the
`test_hashtags` junction. All cross-table links are plain ID columns.

### `TestSet` → `test_sets`

FQN: `com.cramer.catalog.domain.TestSet` · File: `catalog/domain/TestSet.java`

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `code` | `code` | `String` | not null, **unique** |
| `name` | `name` | `String` | not null |
| `description` | `description` | `String` | `columnDefinition = "TEXT"` |
| `coverImageUrl` | `cover_image_url` | `String` | |
| `sourceType` | `source_type` | `String` | default `"custom"` (cambridge/custom/ai_generated) |
| `isPublished` | `is_published` | `Boolean` | default `false` |
| `displayOrder` | `display_order` | `Integer` | default `0` |
| `isSystem` | `is_system` | `Boolean` | |
| `createdBy` | `created_by` | `UUID` | |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp` |

### `Test` → `tests`

FQN: `com.cramer.catalog.domain.Test` · File: `catalog/domain/Test.java`
Unique `(set_id, test_number)`.

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `setId` | `set_id` | `Long` | not null — FK→`test_sets` (plain column) |
| `testNumber` | `test_number` | `Integer` | not null |
| `name` | `name` | `String` | |
| `description` | `description` | `String` | TEXT |
| `difficulty` | `difficulty` | `Difficulty` | `@Enumerated(STRING)`, default `INTERMEDIATE` |
| `estimatedTimeMinutes` | `estimated_time_minutes` | `Integer` | default `170` |
| `isPublished` | `is_published` | `Boolean` | default `false` |
| `isAiGenerated` | `is_ai_generated` | `Boolean` | default `false` |
| `generationMetadata` | `generation_metadata` | `JsonNode` | **JSONB** |
| `createdBy` | `created_by` | `UUID` | |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp` |

**Enums:** `Difficulty` { `BEGINNER`, `LOWER_INTERMEDIATE`, `INTERMEDIATE`, `UPPER_INTERMEDIATE`, `ADVANCED` } (stored uppercase, default `INTERMEDIATE`).

### `Section` → `sections`

FQN: `com.cramer.catalog.domain.Section` · File: `catalog/domain/Section.java`

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `testId` | `test_id` | `Long` | FK→`tests` (plain column, nullable for legacy rows) |
| `examSource` | `exam_source` | `String` | legacy lookup shim |
| `testNumber` | `test_number` | `Integer` | legacy lookup shim |
| `skill` | `skill` | `Skill` | **`@Convert(SkillConverter)`** → persists **lowercase** |
| `partNumber` | `part_number` | `Integer` | |
| `displayContentUrl` | `display_content_url` | `String` | |
| `passageText` | `passage_text` | `String` | TEXT |
| `audioUrl` | `audio_url` | `String` | |
| `sectionLayout` | `section_layout` | `JsonNode` | **JSONB** |
| `imageDescription` | `image_description` | `String` | TEXT |
| `status` | `status` | `SectionStatus` | `@Enumerated(STRING)`, default `PUBLISHED` |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp` |

**Enums:** `SectionStatus` { `DRAFT`, `PUBLISHED`, `ARCHIVED` } (default `PUBLISHED`).
**Converter:** `SkillConverter` maps `Skill` ↔ lowercase `sections.skill` (reading/listening/writing/speaking).

### `Question` → `questions`

FQN: `com.cramer.catalog.domain.Question` · File: `catalog/domain/Question.java`
`correct_answer` and `explanation` are answer-key material (never exposed by delivery reads).

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `sectionId` | `section_id` | `Long` | FK→`sections` (plain column) |
| `questionNumber` | `question_number` | `Integer` | |
| `questionUid` | `question_uid` | `String` | **unique** |
| `questionType` | `question_type` | `QuestionType` | `@Enumerated(STRING)` |
| `questionContent` | `question_content` | `JsonNode` | **JSONB** |
| `correctAnswer` | `correct_answer` | `JsonNode` | **JSONB** (answer key) |
| `explanation` | `explanation` | `JsonNode` | **JSONB** (answer key) |
| `imageUrl` | `image_url` | `String` | |
| `wordLimit` | `word_limit` | `String` | |

**Enums:** `QuestionType` — see [Enum Catalogue](#enum-catalogue). (Note: `Question` has **no**
created/updated timestamps.)

### `Hashtag` → `hashtags`

FQN: `com.cramer.catalog.domain.Hashtag` · File: `catalog/domain/Hashtag.java`
Soft-deleted via `is_active = false`.

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `code` | `code` | `String` | not null, **unique** |
| `name` | `name` | `String` | |
| `category` | `category` | `String` | not null (topic/theme/difficulty/source/skill_focus) |
| `icon` | `icon` | `String` | |
| `color` | `color` | `String` | |
| `useCount` | `use_count` | `Integer` | default `0` |
| `isActive` | `is_active` | `Boolean` | default `true` |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not updatable |

### `TestHashtag` → `test_hashtags`

FQN: `com.cramer.catalog.domain.TestHashtag` · File: `catalog/domain/TestHashtag.java`
Junction with an attribute, so modelled as an explicit entity (not a plain many-to-many).

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | (composite) | `TestHashtagId` | **`@EmbeddedId`** |
| `isPrimary` | `is_primary` | `Boolean` | default `false` |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not updatable |

**Composite key** `TestHashtagId` (`@Embeddable`, FQN `com.cramer.catalog.domain.TestHashtagId`,
`implements Serializable`):

| Field | Column | Java type |
|-------|--------|-----------|
| `testId` | `test_id` | `Long` |
| `hashtagId` | `hashtag_id` | `Long` |

---

## Module: `assessment`

`backend/src/main/java/com/cramer/assessment/domain/`

### `Attempt` → `test_attempts`

FQN: `com.cramer.assessment.domain.Attempt` · File: `assessment/domain/Attempt.java`
Keyed on legacy `exam_source` / `test_number` (varchar) / `skill` (lowercase).

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `userId` | `user_id` | `UUID` | not null |
| `examSource` | `exam_source` | `String` | not null |
| `testNumber` | `test_number` | `String` | not null (varchar) |
| `skill` | `skill` | `String` | not null, stored **lowercase** (plain String, no converter) |
| `status` | `status` | `AttemptStatus` | `@Enumerated(STRING)`, default `IN_PROGRESS` |
| `score` | `score` | `Integer` | |
| `currentPart` | `current_part` | `Integer` | |
| `timeLeft` | `time_left` | `Integer` | |
| `startedAt` | `started_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `completedAt` | `completed_at` | `OffsetDateTime` | |

**Enums:** `AttemptStatus` { `IN_PROGRESS`, `COMPLETED`, `CANCELLED` }.

### `UserAnswer` → `user_answers`

FQN: `com.cramer.assessment.domain.UserAnswer` · File: `assessment/domain/UserAnswer.java`

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `attemptId` | `attempt_id` | `Long` | not null — FK→`test_attempts` (plain column) |
| `questionId` | `question_id` | `Long` | not null — FK→`questions` (plain column) |
| `userId` | `user_id` | `UUID` | not null |
| `answerContent` | `answer_content` | `JsonNode` | **JSONB**, not null (`{"value": text}`) |
| `userAnswer` | `user_answer` | `String` | not null |
| `isCorrect` | `is_correct` | `Boolean` | |
| `submittedAt` | `submitted_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |

---

## Module: `writing`

`backend/src/main/java/com/cramer/writing/domain/`

### `WritingSubmission` → `writing_submissions`

FQN: `com.cramer.writing.domain.WritingSubmission` · File: `writing/domain/WritingSubmission.java`
One row per task (1 or 2) of an attempt.

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `attemptId` | `attempt_id` | `Long` | not null — FK→`test_attempts` (plain column) |
| `userId` | `user_id` | `UUID` | not null |
| `taskNumber` | `task_number` | `Integer` | not null (1 or 2) |
| `essayText` | `essay_text` | `String` | TEXT, not null |
| `wordCount` | `word_count` | `Integer` | not null, default `0` |
| `gradingStatus` | `grading_status` | `WritingStatus` | `@Enumerated(STRING)`, default `PENDING` |
| `overallBand` | `overall_band` | `BigDecimal` | |
| `bandScores` | `band_scores` | `JsonNode` | **JSONB** |
| `aiFeedback` | `ai_feedback` | `JsonNode` | **JSONB** |
| `submittedAt` | `submitted_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `gradedAt` | `graded_at` | `OffsetDateTime` | |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not updatable |

**Enums:** `WritingStatus` { `PENDING`, `GRADING`, `COMPLETED`, `FAILED` } (terminal success = `COMPLETED`, not `GRADED`).

---

## Module: `speaking`

`backend/src/main/java/com/cramer/speaking/domain/`

### `SpeakingSession` → `speaking_sessions`

FQN: `com.cramer.speaking.domain.SpeakingSession` · File: `speaking/domain/SpeakingSession.java`

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `userId` | `user_id` | `UUID` | not null |
| `testId` | `test_id` | `Long` | not null — FK→`tests` (plain column) |
| `sessionMode` | `session_mode` | `String` | not null |
| `status` | `status` | `SpeakingSessionStatus` | **`@Convert(SpeakingSessionStatusConverter)`** → lowercase; default `IN_PROGRESS` |
| `accent` | `accent` | `String` | not null |
| `speed` | `speed` | `BigDecimal` | not null, default `1.00` |
| `sessionBlueprint` | `session_blueprint` | `JsonNode` | **JSONB**, not null (frozen runtime plan) |
| `isFinalized` | `is_finalized` | `Boolean` | not null, default `false` |
| `totalDurationSeconds` | `total_duration_seconds` | `Integer` | |
| `overallBand` | `overall_band` | `BigDecimal` | |
| `fluencyBand` | `fluency_band` | `BigDecimal` | |
| `lexicalBand` | `lexical_band` | `BigDecimal` | |
| `grammarBand` | `grammar_band` | `BigDecimal` | |
| `pronunciationBand` | `pronunciation_band` | `BigDecimal` | |
| `gradingResult` | `grading_result` | `JsonNode` | **JSONB** |
| `luaCost` | `lua_cost` | `Integer` | not null, default `0` |
| `luaDeducted` | `lua_deducted` | `Boolean` | not null, default `false` |
| `startedAt` | `started_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `completedAt` | `completed_at` | `OffsetDateTime` | |
| `gradedAt` | `graded_at` | `OffsetDateTime` | |
| `gradingAttempts` | `grading_attempts` | `Integer` | not null, default `0` |
| `lastGradingError` | `last_grading_error` | `String` | TEXT |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp`, not null |

**Enums:** `SpeakingSessionStatus` { `IN_PROGRESS`, `COMPLETED`, `ABANDONED`, `EXPIRED`,
`GRADING`, `GRADED`, `GRADING_FAILED` } (persisted **lowercase**).
**Converter:** `SpeakingSessionStatusConverter` (enum ↔ lowercase DB string).
**Other domain types (non-persistent):** `SpeakingSessionStateMachine` (state-transition helper —
not an `@Entity`).

### `SpeakingTranscript` → `speaking_transcripts`

FQN: `com.cramer.speaking.domain.SpeakingTranscript` · File: `speaking/domain/SpeakingTranscript.java`
Unique `(session_id, turn_index)`.

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `sessionId` | `session_id` | `Long` | not null — FK→`speaking_sessions` (plain column) |
| `sourceQuestionId` | `source_question_id` | `Long` | FK→`questions` (plain column, nullable) |
| `partNumber` | `part_number` | `Integer` | not null |
| `turnIndex` | `turn_index` | `Integer` | not null |
| `questionSnapshot` | `question_snapshot` | `JsonNode` | **JSONB**, not null (frozen prompt) |
| `audioStoragePath` | `audio_storage_path` | `String` | TEXT |
| `audioDurationSeconds` | `audio_duration_seconds` | `Integer` | |
| `transcriptText` | `transcript_text` | `String` | TEXT |
| `transcriptConfidence` | `transcript_confidence` | `BigDecimal` | |
| `questionEvaluation` | `question_evaluation` | `JsonNode` | **JSONB** |
| `recordedAt` | `recorded_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp`, not null |

---

## Module: `billing`

`backend/src/main/java/com/cramer/billing/domain/`

### `SubscriptionTier` → `subscription_tiers`

FQN: `com.cramer.billing.domain.SubscriptionTier` · File: `billing/domain/SubscriptionTier.java`
Premium when `price_vnd > 0` (`isPremium()` helper).

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `code` | `code` | `String` | not null, **unique** |
| `name` | `name` | `String` | |
| `priceVnd` | `price_vnd` | `Integer` | not null, default `0` |
| `includedAiGradings` | `included_ai_gradings` | `Integer` | default `0` |
| `dailyChatLimit` | `daily_chat_limit` | `Integer` | |
| `chatbotMonthlyLimit` | `chatbot_monthly_limit` | `Integer` | not null, default `0` |
| `vocabAiDailyLimit` | `vocab_ai_daily_limit` | `Integer` | not null, default `0` |
| `monthlyAttemptLimit` | `monthly_attempt_limit` | `Integer` | not null, default `0` |
| `monthlyAttemptAiLimit` | `monthly_attempt_ai_limit` | `Integer` | not null, default `0` |
| `perSkillAttemptLimit` | `per_skill_attempt_limit` | `Integer` | not null, default `0` |
| `perSkillAttemptAiLimit` | `per_skill_attempt_ai_limit` | `Integer` | not null, default `0` |
| `monthlyTranslationLimit` | `monthly_translation_limit` | `Integer` | not null, default `0` |
| `maxVocabularyEntries` | `max_vocabulary_entries` | `Integer` | not null, default `0` |
| `attemptOverageCost` | `attempt_overage_cost` | `Integer` | not null, default `10` |
| `attemptAiOverageCost` | `attempt_ai_overage_cost` | `Integer` | not null, default `20` |
| `chatbotOverageCost` | `chatbot_overage_cost` | `Integer` | not null, default `2` |
| `translationOverageCost` | `translation_overage_cost` | `Integer` | not null, default `1` |
| `initialLua` | `initial_lua` | `Integer` | not null, default `50` |
| `monthlyLuaBonus` | `monthly_lua_bonus` | `Integer` | default `0` |
| `features` | `features` | `JsonNode` | **JSONB** |
| `isActive` | `is_active` | `Boolean` | default `true` |
| `sortOrder` | `sort_order` | `Integer` | default `0` |
| `displayOrder` | `display_order` | `Integer` | |

### `UserSubscription` → `user_subscriptions`

FQN: `com.cramer.billing.domain.UserSubscription` · File: `billing/domain/UserSubscription.java`

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `userId` | `user_id` | `UUID` | not null |
| `tierId` | `tier_id` | `Long` | not null — FK→`subscription_tiers` (plain column) |
| `status` | `status` | `String` | **plain String**, default `"ACTIVE"` (not an enum) |
| `startedAt` | `started_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `expiresAt` | `expires_at` | `OffsetDateTime` | |
| `autoRenew` | `auto_renew` | `Boolean` | default `false` |
| `attemptsUsed` | `attempts_used` | `Integer` | not null, default `0` |
| `attemptAisUsed` | `attempt_ais_used` | `Integer` | not null, default `0` |
| `chatbotUsed` | `chatbot_used` | `Integer` | not null, default `0` |
| `aiGradingEnabled` | `ai_grading_enabled` | `Boolean` | default `true` |
| `paymentReference` | `payment_reference` | `String` | |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp` |

### `UserCredit` → `user_credits`

FQN: `com.cramer.billing.domain.UserCredit` · File: `billing/domain/UserCredit.java`
One row per user; DB CHECK enforces `balance >= 0`.

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `userId` | `user_id` | `UUID` | not null, **unique** |
| `balance` | `balance` | `Integer` | not null, default `0` |
| `lifetimeEarned` | `lifetime_earned` | `Integer` | not null, default `0` |
| `lifetimeSpent` | `lifetime_spent` | `Integer` | not null, default `0` |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp` |

### `CreditTransaction` → `credit_transactions`

FQN: `com.cramer.billing.domain.CreditTransaction` · File: `billing/domain/CreditTransaction.java`
Immutable ledger row; `amount` is signed; `reference_id` backs idempotency.

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `userId` | `user_id` | `UUID` | not null |
| `amount` | `amount` | `Integer` | not null (signed) |
| `balanceAfter` | `balance_after` | `Integer` | not null |
| `type` | `type` | `TransactionType` | `@Enumerated(STRING)`, not null |
| `category` | `category` | `String` | **plain String** (free varchar; see `CreditCategory` enum used at service layer) |
| `description` | `description` | `String` | |
| `referenceId` | `reference_id` | `String` | idempotency key |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not updatable |

**Enums:** `TransactionType` { `EARN`, `SPEND`, `PURCHASE`, `REFUND`, `BONUS`, `ADMIN` }.
`CreditCategory` (see [Enum Catalogue](#enum-catalogue)) is **not** mapped on the entity — the
`category` column is a plain String; the enum is applied in service logic.

### `PaymentOrder` → `payment_orders`

FQN: `com.cramer.billing.domain.PaymentOrder` · File: `billing/domain/PaymentOrder.java`
PayOS order; PENDING→PAID claimed under a row lock for idempotent webhooks.

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `userId` | `user_id` | `UUID` | not null |
| `orderCode` | `order_code` | `Long` | not null, **unique** |
| `paymentLinkId` | `payment_link_id` | `String` | |
| `checkoutUrl` | `checkout_url` | `String` | |
| `qrCode` | `qr_code` | `String` | TEXT |
| `type` | `type` | `String` | **plain String** (SUBSCRIPTION / LUA_PACK), not null |
| `tierId` | `tier_id` | `Long` | |
| `tierCode` | `tier_code` | `String` | |
| `luaAmount` | `lua_amount` | `Integer` | |
| `amountVnd` | `amount_vnd` | `Integer` | not null |
| `description` | `description` | `String` | |
| `status` | `status` | `String` | **plain String**, default `"PENDING"` (PENDING/PAID/CANCELLED/EXPIRED/FAILED), not null |
| `transactionDatetime` | `transaction_datetime` | `String` | |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `paidAt` | `paid_at` | `OffsetDateTime` | |
| `expiresAt` | `expires_at` | `OffsetDateTime` | |

### `LuaPack` → `lua_packs`

FQN: `com.cramer.billing.domain.LuaPack` · File: `billing/domain/LuaPack.java`
Total granted on purchase = `lua_amount + bonus_lua` (`totalLua()` helper).

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `code` | `code` | `String` | not null, **unique** |
| `name` | `name` | `String` | |
| `emoji` | `emoji` | `String` | |
| `luaAmount` | `lua_amount` | `Integer` | not null |
| `priceVnd` | `price_vnd` | `Integer` | not null |
| `discountPercent` | `discount_percent` | `Integer` | not null, default `0` |
| `bonusLua` | `bonus_lua` | `Integer` | not null, default `0` |
| `descriptionVi` | `description_vi` | `String` | |
| `descriptionEn` | `description_en` | `String` | |
| `isActive` | `is_active` | `Boolean` | not null, default `true` |
| `displayOrder` | `display_order` | `Integer` | default `0` |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp` |

### `UserQuota` → `user_quotas`

FQN: `com.cramer.billing.domain.UserQuota` · File: `billing/domain/UserQuota.java`
Unique `(user_id, quota_month)`; `quota_month` = first day of month.

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `userId` | `user_id` | `UUID` | not null |
| `quotaMonth` | `quota_month` | `LocalDate` | not null |
| `attemptCount` | `attempt_count` | `Integer` | not null, default `0` |
| `attemptAiCount` | `attempt_ai_count` | `Integer` | not null, default `0` |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp` |

### `SkillQuota` → `skill_quotas`

FQN: `com.cramer.billing.domain.SkillQuota` · File: `billing/domain/SkillQuota.java`
Unique `(user_id, skill, quota_month)`; `skill` stored uppercase as a plain String.

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `userId` | `user_id` | `UUID` | not null |
| `skill` | `skill` | `String` | not null (plain String, uppercase) |
| `quotaMonth` | `quota_month` | `LocalDate` | not null |
| `attemptCount` | `attempt_count` | `Integer` | not null, default `0` |
| `attemptAiCount` | `attempt_ai_count` | `Integer` | not null, default `0` |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp` |

### `TranslationUsage` → `translation_usage`

FQN: `com.cramer.billing.domain.TranslationUsage` · File: `billing/domain/TranslationUsage.java`
Unique `(user_id, usage_month)`; `usage_month` = first day of month.

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `userId` | `user_id` | `UUID` | not null |
| `usageMonth` | `usage_month` | `LocalDate` | not null |
| `translationsUsed` | `translations_used` | `Integer` | not null, default `0` |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp` |

---

## Module: `engagement`

`backend/src/main/java/com/cramer/engagement/domain/`

### `Target` → `target`

FQN: `com.cramer.engagement.domain.Target` · File: `engagement/domain/Target.java`
One per user; bands 0–9 (DB check). PK is a DB-default-assigned UUID; service upserts by user.

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `UUID` | **PK**, not updatable (no `@GeneratedValue`; DB default) |
| `userId` | `user_id` | `UUID` | not null, **unique** |
| `examName` | `exam_name` | `String` | not null |
| `examDate` | `exam_date` | `LocalDate` | |
| `listening` | `listening` | `Double` | |
| `reading` | `reading` | `Double` | |
| `writing` | `writing` | `Double` | |
| `speaking` | `speaking` | `Double` | |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp`, not null |

### `ChatMessage` → `chat_messages`

FQN: `com.cramer.engagement.domain.ChatMessage` · File: `engagement/domain/ChatMessage.java`

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `userId` | `user_id` | `UUID` | not null |
| `role` | `role` | `String` | not null (user/assistant/system) |
| `content` | `content` | `String` | TEXT, not null |
| `tokensUsed` | `tokens_used` | `Integer` | default `0` |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not updatable |

### `UserActivity` → `user_activities`

FQN: `com.cramer.engagement.domain.UserActivity` · File: `engagement/domain/UserActivity.java`

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `userId` | `user_id` | `UUID` | not null |
| `activityType` | `activity_type` | `String` | not null |
| `title` | `title` | `String` | not null |
| `description` | `description` | `String` | TEXT |
| `metadata` | `metadata` | `JsonNode` | **JSONB** |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |

### `Vocabulary` → `vocabulary`

FQN: `com.cramer.engagement.domain.Vocabulary` · File: `engagement/domain/Vocabulary.java`
Unique per `(user_id, word)` (enforced in the service).

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `userId` | `user_id` | `UUID` | not null |
| `word` | `word` | `String` | not null |
| `translation` | `translation` | `String` | TEXT |
| `phonetic` | `phonetic` | `String` | |
| `partOfSpeech` | `part_of_speech` | `String` | |
| `definition` | `definition` | `String` | TEXT |
| `exampleSentence` | `example_sentence` | `String` | TEXT |
| `sourceContext` | `source_context` | `String` | TEXT |
| `sourceTestId` | `source_test_id` | `Long` | FK→`tests` (plain column) |
| `sourceSectionId` | `source_section_id` | `Long` | FK→`sections` (plain column) |
| `notes` | `notes` | `String` | TEXT |
| `isMastered` | `is_mastered` | `Boolean` | default `false` |
| `reviewCount` | `review_count` | `Integer` | default `0` |
| `lastReviewedAt` | `last_reviewed_at` | `OffsetDateTime` | |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not updatable |
| `updatedAt` | `updated_at` | `OffsetDateTime` | `@UpdateTimestamp` |

---

## Module: `admin`

`backend/src/main/java/com/cramer/admin/domain/`

### `AdminAuditLog` → `admin_audit_log`

FQN: `com.cramer.admin.domain.AdminAuditLog` · File: `admin/domain/AdminAuditLog.java`
Attribution comes from the authenticated admin principal (never a header).

| Field | Column | Java type | Notes |
|-------|--------|-----------|-------|
| `id` | `id` | `Long` | **PK**, IDENTITY |
| `adminUserId` | `admin_user_id` | `UUID` | not null |
| `adminEmail` | `admin_email` | `String` | |
| `action` | `action` | `String` | not null |
| `targetType` | `target_type` | `String` | not null |
| `targetId` | `target_id` | `String` | not null |
| `oldValue` | `old_value` | `JsonNode` | **JSONB** |
| `newValue` | `new_value` | `JsonNode` | **JSONB** |
| `description` | `description` | `String` | TEXT |
| `ipAddress` | `ip_address` | `String` | |
| `userAgent` | `user_agent` | `String` | TEXT |
| `createdAt` | `created_at` | `OffsetDateTime` | `@CreationTimestamp`, not null, not updatable |

---

## Module: `platform` (shared enums)

`backend/src/main/java/com/cramer/platform/common/ielts/`

No JPA entities. Houses the shared-kernel IELTS vocabulary referenced by entity columns:

- **`Skill`** (FQN `com.cramer.platform.common.ielts.Skill`) — used by `Section.skill` (via
  `SkillConverter`). Values: `READING`, `LISTENING`, `WRITING`, `SPEAKING`. DB value is
  **lowercase** (`dbValue()`).
- **`QuestionType`** (FQN `com.cramer.platform.common.ielts.QuestionType`) — used by
  `Question.questionType` (`@Enumerated(STRING)`). See [Enum Catalogue](#enum-catalogue).
- **`BandScale`** (FQN `com.cramer.platform.common.ielts.BandScale`) — present in the package;
  not referenced by any mapped entity column (its values were not enumerated here).

---

## Module: `abts` (no entities)

`backend/src/main/java/com/cramer/abts/domain/` — **no `@Entity` classes.** The ABTS pipeline
persists nothing through these types; the domain folder is value objects (records) + enums:

- **Records:** `GenerationResult`, `Hunk`, `TokenUsage`, `QuestionRange`, `StreamEvent`.
- **Enums:** `GenerationStatus`, `FactsMode`, `StreamEventType`.

(ABTS-generated content is persisted through the `catalog` entities — `tests`/`sections`/`questions`.)

---

## Enum Catalogue

Enums that are mapped onto entity columns (or persisted via converters):

| Enum | FQN | Mapped on | Storage | Values |
|------|-----|-----------|---------|--------|
| `AccountStatus` | `identity.domain.AccountStatus` | `Profile.accountStatus` | STRING (uppercase) | `ACTIVE`, `BANNED`, `DEACTIVATED`, `DELETED` |
| `Difficulty` | `catalog.domain.Difficulty` | `Test.difficulty` | STRING (uppercase) | `BEGINNER`, `LOWER_INTERMEDIATE`, `INTERMEDIATE`, `UPPER_INTERMEDIATE`, `ADVANCED` |
| `SectionStatus` | `catalog.domain.SectionStatus` | `Section.status` | STRING (uppercase) | `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| `Skill` | `platform.common.ielts.Skill` | `Section.skill` | Converter → **lowercase** | `READING`, `LISTENING`, `WRITING`, `SPEAKING` |
| `QuestionType` | `platform.common.ielts.QuestionType` | `Question.questionType` | STRING | `FILL_IN_BLANK`, `SUMMARY_COMPLETION`, `SUMMARY_COMPLETION_OPTIONS`, `TRUE_FALSE_NOT_GIVEN`, `YES_NO_NOT_GIVEN`, `MATCHING_INFORMATION`, `MATCHING_HEADINGS`, `MATCHING_FEATURES`, `MATCHING_SENTENCE_ENDINGS`, `MULTIPLE_CHOICE`, `MULTIPLE_CHOICE_MULTIPLE_ANSWERS`, `TABLE_COMPLETION`, `FLOW_CHART_COMPLETION`, `DIAGRAM_LABEL_COMPLETION`, `MATCHING`, `PART_1`, `PART_2`, `PART_3` |
| `AttemptStatus` | `assessment.domain.AttemptStatus` | `Attempt.status` | STRING (uppercase) | `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `WritingStatus` | `writing.domain.WritingStatus` | `WritingSubmission.gradingStatus` | STRING (uppercase) | `PENDING`, `GRADING`, `COMPLETED`, `FAILED` |
| `SpeakingSessionStatus` | `speaking.domain.SpeakingSessionStatus` | `SpeakingSession.status` | Converter → **lowercase** | `IN_PROGRESS`, `COMPLETED`, `ABANDONED`, `EXPIRED`, `GRADING`, `GRADED`, `GRADING_FAILED` |
| `TransactionType` | `billing.domain.TransactionType` | `CreditTransaction.type` | STRING | `EARN`, `SPEND`, `PURCHASE`, `REFUND`, `BONUS`, `ADMIN` |

Enum **not** mapped on an entity column (service-layer only):

| Enum | FQN | Values |
|------|-----|--------|
| `CreditCategory` | `billing.domain.CreditCategory` | `INITIAL_BONUS`, `TIER_BONUS`, `PURCHASE`, `AI_GRADING`, `ATTEMPT_OVERAGE`, `VOCABULARY_TRANSLATION`, `CHAT_EXTENSION`, `SPEAKING_SESSION`, `SPEAKING_REFUND`, `ADMIN_ADJUSTMENT` (each carries a default `TransactionType`) |

> Note: `UserSubscription.status`, `PaymentOrder.type`, `PaymentOrder.status`, `Attempt.skill`,
> and `SkillQuota.skill` are persisted as **plain `String`** columns (no `@Enumerated`/converter),
> even though their value sets are constrained at the application/DB level.

---

## Known Tables Without a JPA Entity

The following tables from the known live set have **no** corresponding `@Entity` (verified: no
references in `backend/src` except as noted). They are not mapped through Hibernate:

| Table | Status |
|-------|--------|
| `chatbot_usage` | **Legacy** — referenced only in a comment in `billing/service/ChatBillingPort.java` ("the legacy daily `chatbot_usage` table"). Monthly counters live on `user_subscriptions.chatbot_used`. No entity. |
| `abts_templates` | No entity and **no source reference** anywhere in `backend/src`. (ABTS templating is not persisted via JPA.) |
| `model_runtime_status` | No entity and **no source reference** anywhere in `backend/src`. |
| `speaking_*_legacy` (archived) | Archived legacy Speaking tables; intentionally unmapped. |

No `@Entity` maps to a table **outside** the known set — nothing to flag in that direction.

---

## Verification Notes

- **Sources read:** all 26 `@Entity` files + the `@Embeddable` (`TestHashtagId`) + every mapped
  enum + both `AttributeConverter`s + the two shared platform enums (`Skill`, `QuestionType`).
- **Cross-checks run:**
  - `@Entity` count = 26; `@Table(name=…)` count = 26 (1:1).
  - JPA-mapping annotations total = 27 (26 `@Entity` + 1 `@Embeddable`); **no `@MappedSuperclass`.**
  - Association annotations (`@ManyToOne`/`@OneToMany`/`@OneToOne`/`@ManyToMany`/`@JoinColumn`/
    `@JoinTable`) = **0** across `com.cramer`. All relationships are plain FK ID columns.
  - JSONB always via `@JdbcTypeCode(SqlTypes.JSON)` on `JsonNode` (no `@Type(JsonType.class)`).
- **Anti-hallucination:** anything not present in source was omitted. `BandScale` is listed as
  present but its values were not read, so they are not enumerated here.
