# Cramer Database Schema Documentation

> **Comprehensive documentation of the Supabase PostgreSQL database for the Cramer IELTS Learning Platform**
>
> **Project ID:** `jpocdgkrvohmjkejclpl`
>
> **Last Updated:** 2025-12-26

---

## Table of Contents

1. [Overview](#1-overview)
2. [Database Connection](#2-database-connection)
3. [Tables Reference](#3-tables-reference)
   - [User Management](#31-user-management)
   - [IELTS Content](#32-ielts-content)
   - [Test Attempts & Answers](#33-test-attempts--answers)
   - [Subscription & Billing](#34-subscription--billing)
   - [AI Features](#35-ai-features)
   - [Vocabulary & Learning](#36-vocabulary--learning)
4. [Relationships & ERD](#4-relationships--erd)
5. [Indexes](#5-indexes)
6. [Row Level Security (RLS)](#6-row-level-security-rls)
7. [Database Functions](#7-database-functions)
8. [Triggers](#8-triggers)
9. [Migrations History](#9-migrations-history)
10. [Extensions](#10-extensions)

---

## 1. Overview

The Cramer database is hosted on **Supabase** (PostgreSQL) and serves as the backend for an IELTS learning platform. The schema is organized around several core domains:

| Domain | Description | Key Tables |
|--------|-------------|------------|
| **User Management** | User profiles and authentication | `profiles` |
| **IELTS Content** | Test sections and questions for Reading, Listening, Writing | `sections`, `questions` |
| **Test Attempts** | User test progress and answers | `test_attempts`, `user_answers`, `writing_submissions` |
| **Subscription & Billing** | Subscription tiers, payments, and virtual currency | `subscription_tiers`, `user_subscriptions`, `payment_orders`, `user_credits`, `credit_transactions`, `lua_packs` |
| **AI Features** | AI chatbot and usage tracking | `chat_messages`, `chatbot_usage` |
| **Vocabulary** | Vocabulary notebook with AI translations | `vocabulary`, `translation_usage` |
| **Quotas** | Usage limits and quota tracking | `user_quotas`, `skill_quotas` |

### Quick Statistics

| Metric | Value |
|--------|-------|
| Total Tables | 22 |
| Tables with RLS | 17 |
| Database Functions | 3 |
| Active Triggers | 3 |
| Applied Migrations | 15 |

---

## 2. Database Connection

### Connection Details

```properties
# JDBC Connection String (for Java/Spring Boot)
jdbc:postgresql://db.jpocdgkrvohmjkejclpl.supabase.co:6543/postgres?sslmode=require&prepareThreshold=0
```

### Important Configuration Notes

| Parameter | Value | Purpose |
|-----------|-------|---------|
| `sslmode` | `require` | Enforces SSL encryption for all connections |
| `prepareThreshold` | `0` | **Critical:** Prevents prepared statement cache conflicts with HikariCP connection pooling |
| Port | `6543` | Supabase connection pooler (Transaction mode) |
| Port | `5432` | Direct database connection (Session mode) |

### Recommended HikariCP Settings

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
```

---

## 3. Tables Reference

### 3.1 User Management

#### `profiles`

> **Description:** Stores public profile information for each user.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `uuid` | NO | `gen_random_uuid()` | **Primary Key.** Links to Supabase Auth user. |
| `username` | `varchar` | NO | `''` | Unique username for display. |
| `full_name` | `varchar` | YES | - | User's full display name. |
| `phone_number` | `varchar` | YES | - | Contact phone number. |
| `address` | `varchar` | YES | - | User's address. |
| `avatar_url` | `varchar` | YES | - | URL to profile avatar image. |
| `hero_background_url` | `varchar` | YES | - | URL to custom hero background. |
| `page_background_url` | `varchar` | YES | - | URL to custom page background. |
| `llm_api_key` | `varchar` | YES | - | User's API key for LLM provider (encrypted). |
| `llm_model` | `varchar` | YES | - | Selected LLM model (e.g., `deepseek-chat`, `deepseek-reasoner`). |
| `llm_provider` | `varchar` | YES | `'deepseek'` | LLM provider: `deepseek`, `openai`, `gemini`, `anthropic`. |
| `created_at` | `timestamptz` | NO | `now()` | Account creation timestamp. |

**Constraints:**
- Primary Key: `profiles_pkey` on `id`
- Unique: `profiles_username_key` on `username`

**RLS Policies:**
- `Enable read access for all users` - Users can read all profiles
- `Users can update own profile` - Users can only update their own profile

---

### 3.2 IELTS Content

#### `test_sets`

> **Description:** Top-level collections of tests (e.g., "Cambridge IELTS 17", "Road to IELTS").

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `code` | `varchar(50)` | NO | - | Unique code (e.g., `cam17`). |
| `name_vi` | `varchar(255)` | NO | - | Vietnamese name. |
| `name_en` | `varchar(255)` | NO | - | English name. |
| `description` | `text` | YES | - | Set description. |
| `cover_image_url` | `varchar(255)` | YES | - | URL to cover image. |
| `source_type` | `varchar(50)` | NO | `'CAMBRIDGE'` | Type: `CAMBRIDGE`, `IELTS_TRAINER`, `ROAD_TO_IELTS`, `ACTUAL_TEST`, `AI_GENERATED`. |
| `is_published` | `boolean` | NO | `false` | Whether set is visible to users. |
| `is_system` | `boolean` | NO | `false` | System sets cannot be deleted. |
| `display_order` | `integer` | YES | `0` | Sort order. |
| `created_by` | `uuid` | YES | - | Reference to creator. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |
| `updated_at` | `timestamptz` | YES | `now()` | Last modification. |

**Constraints:**
- Primary Key: `test_sets_pkey` on `id`
- Unique: `test_sets_code_key` on `code`

**Indexes:**
- `idx_test_sets_source_type` on (`source_type`)
- `idx_test_sets_is_published` on (`is_published`)

---

#### `tests`

> **Description:** Individual tests within a set (e.g., "Test 1" inside "Cambridge 17").

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `set_id` | `bigint` | NO | - | **Foreign Key** to `test_sets.id`. |
| `test_number` | `integer` | NO | - | Test number within set (1, 2, 3...). |
| `name_vi` | `varchar(255)` | YES | - | Custom Vietnamese name (optional). |
| `name_en` | `varchar(255)` | YES | - | Custom English name (optional). |
| `description` | `text` | YES | - | Test description. |
| `difficulty` | `varchar(20)` | YES | `'MEDIUM'` | Difficulty: `EASY`, `MEDIUM`, `HARD`, `EXPERT`. |
| `estimated_time_minutes` | `integer` | YES | - | Estimated duration. |
| `is_published` | `boolean` | NO | `false` | Whether test is visible. |
| `is_ai_generated` | `boolean` | NO | `false` | Whether created by AI. |
| `generation_metadata` | `jsonb` | YES | - | Metadata if AI generated (prompt, model, etc.). |
| `created_by` | `uuid` | YES | - | Reference to creator. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |
| `updated_at` | `timestamptz` | YES | `now()` | Last modification. |

**Constraints:**
- Primary Key: `tests_pkey` on `id`
- Foreign Key: `tests_set_id_fkey` references `test_sets(id)`
- Unique: `tests_set_id_test_number_key` on (`set_id`, `test_number`)

**Indexes:**
- `idx_tests_set_id` on (`set_id`)

---

#### `hashtags`

> **Description:** Categorization tags for tests (topics, themes, difficulty).

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `code` | `varchar(50)` | NO | - | Unique tag code (e.g., `topic_science`). |
| `name_vi` | `varchar(100)` | NO | - | Vietnamese display name. |
| `name_en` | `varchar(100)` | NO | - | English display name. |
| `category` | `varchar(50)` | NO | - | Category: `TOPIC`, `THEME`, `DIFFICULTY`, `SOURCE`. |
| `icon` | `varchar(10)` | YES | - | Emoji or icon code. |
| `color` | `varchar(20)` | YES | - | Display color hex or name. |
| `use_count` | `integer` | NO | `0` | Number of tests using this tag. |
| `is_active` | `boolean` | NO | `true` | Whether tag is available. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |

**Constraints:**
- Primary Key: `hashtags_pkey` on `id`
- Unique: `hashtags_code_key` on `code`

**Indexes:**
- `idx_hashtags_category` on (`category`)

---

#### `test_hashtags`

> **Description:** Junction table linking tests to hashtags (Many-to-Many).

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `test_id` | `bigint` | NO | - | **Foreign Key** to `tests.id`. |
| `hashtag_id` | `bigint` | NO | - | **Foreign Key** to `hashtags.id`. |
| `is_primary` | `boolean` | NO | `false` | Whether this is a primary tag. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |

**Constraints:**
- Primary Key: `test_hashtags_pkey` on (`test_id`, `hashtag_id`)
- Foreign Key: `test_hashtags_test_id_fkey` references `tests(id)`
- Foreign Key: `test_hashtags_hashtag_id_fkey` references `hashtags(id)`

---

#### `sections`

> **Description:** Stores test sections (passages for Reading, parts for Listening, tasks for Writing).

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `exam_source` | `varchar` | NO | - | Source identifier (e.g., `cam17`, `cam18`). |
| `test_number` | `varchar` | NO | - | Test number as string (e.g., `'1'`, `'2'`). |
| `skill` | `varchar` | NO | - | Skill type: `reading`, `listening`, `writing`. |
| `part_number` | `integer` | NO | - | Part/section number within test. |
| `display_content_url` | `varchar` | YES | - | URL for external display content. |
| `passage_text` | `text` | YES | - | Full HTML-formatted passage/transcript. |
| `audio_url` | `varchar` | YES | - | Audio file URL (for Listening). |
| `instruction` | `text` | YES | - | General instructions for the section. |
| `section_layout` | `jsonb` | YES | - | JSON defining question block layouts (Listening). |
| `task_type` | `varchar` | YES | - | Task type for Writing (e.g., `task1`, `task2`). |
| `task_title` | `text` | YES | - | Title for Writing tasks. |
| `time_limit_minutes` | `integer` | YES | - | Time limit for the section. |
| `recommended_word_count` | `integer` | YES | - | Recommended word count (Writing). |
| `minimum_word_count` | `integer` | YES | - | Minimum word count requirement. |
| `image_url` | `varchar` | YES | - | Image for Writing Task 1 (charts, graphs). |
| `image_description` | `text` | YES | - | Accessibility description of the image. |
| `test_id` | `bigint` | YES | - | **Foreign Key** to `tests.id`. |
| `status` | `varchar(20)` | YES | `'PUBLISHED'` | Status: `DRAFT`, `PUBLISHED`, `ARCHIVED`. |

**Constraints:**
- Primary Key: `sections_pkey` on `id`
- Foreign Key: `sections_test_id_fkey` references `tests(id)`
- Unique: `sections_exam_source_test_number_skill_part_number_key` on (`exam_source`, `test_number`, `skill`, `part_number`) (Legacy)

**Indexes:**
- `idx_sections_skill` on (`skill`)
- `idx_sections_exam` on (`exam_source`, `test_number`)
- `idx_sections_test_id` on (`test_id`)

---

#### `questions`

> **Description:** Stores individual questions for test sections.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `section_id` | `bigint` | NO | - | **Foreign Key** to `sections.id`. |
| `question_number` | `integer` | NO | - | Question number (1-40 for R/L). |
| `question_uid` | `varchar` | NO | - | Unique ID: `{exam}-t{test}-{skill}-q{num}`. |
| `question_type` | `varchar` | NO | - | Question type enum (see below). |
| `question_content` | `jsonb` | NO | - | Question text, options in JSON format. |
| `correct_answer` | `jsonb` | NO | - | Correct answer(s) as JSON array. |
| `explanation` | `text` | YES | - | Optional answer explanation. |
| `word_limit` | `varchar` | YES | - | Answer length constraint. |
| `image_url` | `varchar` | YES | - | Associated image URL. |

**Question Types (Enum Values):**

| Type | Description |
|------|-------------|
| `FILL_IN_BLANK` | Text input for word/phrase |
| `SUMMARY_COMPLETION` | Fill blanks in summary text |
| `TRUE_FALSE_NOT_GIVEN` | T/F/NG selection |
| `YES_NO_NOT_GIVEN` | Y/N/NG selection |
| `MATCHING_INFORMATION` | Match statements to paragraphs |
| `MATCHING_HEADINGS` | Match headings to paragraphs |
| `MATCHING_FEATURES` | Match items to features |
| `MATCHING_SENTENCE_ENDINGS` | Complete sentence endings |
| `MULTIPLE_CHOICE` | Single answer selection |
| `MULTIPLE_CHOICE_MULTIPLE_ANSWERS` | Multiple answer selection |
| `SUMMARY_COMPLETION_OPTIONS` | Fill blanks from word list |
| `DIAGRAM_LABEL_COMPLETION` | Label diagram parts |
| `TABLE_COMPLETION` | Fill blanks in table |
| `FLOW_CHART_COMPLETION` | Fill blanks in flowchart |
| `MATCHING` | Generic matching (Listening) |

**Constraints:**
- Primary Key: `questions_pkey` on `id`
- Foreign Key: `questions_section_fk` references `sections(id)`
- Unique: `questions_question_uid_key` on `question_uid`

**Indexes:**
- `idx_questions_section` on (`section_id`)
- `idx_questions_type` on (`question_type`)

---

### 3.3 Test Attempts & Answers

#### `test_attempts`

> **Description:** Tracks a user's attempt at a specific test section.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | Reference to user. |
| `section_id` | `bigint` | NO | - | Reference to test section. |
| `skill` | `varchar(50)` | NO | - | Skill type for quick filtering. |
| `score` | `integer` | YES | - | Raw score achieved. |
| `band_score` | `numeric(3,1)` | YES | - | IELTS band score (0.0-9.0). |
| `total_questions` | `integer` | YES | - | Total questions in section. |
| `time_spent_seconds` | `integer` | YES | - | Time spent on attempt. |
| `status` | `varchar(50)` | NO | `'in_progress'` | Status: `in_progress`, `completed`. |
| `started_at` | `timestamptz` | NO | `now()` | Attempt start time. |
| `completed_at` | `timestamptz` | YES | - | Attempt completion time. |
| `is_ai_graded` | `boolean` | NO | `false` | Whether AI grading was used. |
| `ai_grading_enabled` | `boolean` | YES | `false` | Whether AI grading is enabled. |

**Constraints:**
- Primary Key: `test_attempts_pkey` on `id`

**Indexes:**
- `idx_test_attempts_user` on (`user_id`)
- `idx_test_attempts_section` on (`section_id`)
- `idx_test_attempts_status` on (`status`)
- `idx_test_attempts_skill` on (`skill`)

**RLS Policies:**
- `Service role full access` - Backend can manage all attempts
- `Users can view own attempts` - Users can only read their own attempts

---

#### `user_answers`

> **Description:** Stores a user's answer to a question for a given test attempt.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `attempt_id` | `bigint` | NO | - | **Foreign Key** to `test_attempts.id`. |
| `question_id` | `bigint` | NO | - | **Foreign Key** to `questions.id`. |
| `user_answer` | `jsonb` | YES | - | User's submitted answer(s). |
| `is_correct` | `boolean` | YES | - | Whether answer was correct. |
| `answered_at` | `timestamptz` | YES | `now()` | When answer was submitted. |

**Constraints:**
- Primary Key: `user_answers_pkey` on `id`
- Foreign Key: `user_answers_attempt_id_fkey` references `test_attempts(id)`
- Foreign Key: `user_answers_question_id_fkey` references `questions(id)`
- Unique: `user_answers_attempt_id_question_id_key` on (`attempt_id`, `question_id`)

**Indexes:**
- `idx_user_answers_attempt` on (`attempt_id`)
- `idx_user_answers_question` on (`question_id`)

---

#### `writing_submissions`

> **Description:** Stores user writing submissions for IELTS Writing tasks.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | Reference to user. |
| `attempt_id` | `bigint` | NO | - | **Foreign Key** to `test_attempts.id`. |
| `task_number` | `integer` | NO | - | Task number (1 or 2). |
| `essay_text` | `text` | NO | - | User's submitted essay. |
| `word_count` | `integer` | NO | `0` | Essay word count. |
| `grading_status` | `varchar(255)` | NO | `'PENDING'` | Status: `PENDING`, `GRADING`, `COMPLETED`, `ERROR`. |
| `overall_band` | `numeric` | YES | - | Overall band score. |
| `band_scores` | `jsonb` | YES | - | Detailed band scores by criteria. |
| `ai_feedback` | `jsonb` | YES | - | AI-generated feedback and suggestions. |
| `submitted_at` | `timestamptz` | NO | `now()` | Submission timestamp. |
| `graded_at` | `timestamptz` | YES | - | When grading completed. |
| `created_at` | `timestamptz` | NO | `now()` | Record creation timestamp. |

**Constraints:**
- Primary Key: `writing_submissions_pkey` on `id`
- Foreign Key: `writing_submissions_attempt_id_fkey` references `test_attempts(id)`
- Unique: `writing_submissions_attempt_id_task_number_key` on (`attempt_id`, `task_number`)

**Indexes:**
- `idx_writing_submissions_user` on (`user_id`)
- `idx_writing_submissions_attempt` on (`attempt_id`)
- `idx_writing_submissions_status` on (`grading_status`)

**RLS Policies:**
- `Service role can manage all writing submissions` - Backend full access
- `Users can manage their own writing submissions` - Users can CRUD their own

---

### 3.4 Subscription & Billing

#### `subscription_tiers`

> **Description:** Available subscription plans with features and limits.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `code` | `varchar(50)` | NO | - | Unique tier code (e.g., `cramerie`, `cramerich`). |
| `name_vi` | `varchar(100)` | NO | - | Vietnamese display name. |
| `name_en` | `varchar(100)` | NO | - | English display name. |
| `price_vnd` | `integer` | NO | `0` | Monthly price in VND. |
| `features` | `jsonb` | NO | - | Feature flags and capabilities. |
| `is_active` | `boolean` | NO | `true` | Whether tier is available for purchase. |
| `display_order` | `integer` | YES | - | Sort order for display. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |
| **Quota Limits** ||||
| `monthly_attempt_limit` | `integer` | YES | `0` | Monthly test attempts allowed. |
| `monthly_attempt_ai_limit` | `integer` | YES | `0` | Monthly AI-graded attempts. |
| `monthly_translation_limit` | `integer` | YES | `0` | Monthly translations (Vocabulary AI). |
| `chatbot_monthly_limit` | `integer` | YES | `0` | Monthly chatbot messages. |
| `max_vocabulary_entries` | `integer` | YES | `0` | Maximum saved vocabulary. |
| **Overage Costs (Lúa)** ||||
| `attempt_overage_cost` | `integer` | YES | `10` | Lúa cost per extra attempt. |
| `attempt_ai_overage_cost` | `integer` | YES | `20` | Lúa cost per extra AI grading. |
| `chatbot_overage_cost` | `integer` | YES | `2` | Lúa cost per extra chat message. |
| `translation_overage_cost` | `integer` | YES | `1` | Lúa cost per extra translation. |
| **Bonuses** ||||
| `initial_lua` | `integer` | YES | `0` | Lúa given on first subscription. |
| `monthly_lua_bonus` | `integer` | YES | `0` | Monthly Lúa bonus. |

**Current Subscription Tiers:**

| Code | Name | Price (VND) | Monthly Attempts | AI Gradings | Translations |
|------|------|-------------|------------------|-------------|--------------|
| `cramerie` | Cramerie | 0 (Free) | 20 | 3 | 150 |
| `cramerich` | Cramerich | 69,000 | 40 | 20 | 500 |
| `cramerous` | Cramerous | 149,000 | ∞ | ∞ | ∞ | *(Deprecated)* |

---

#### `user_subscriptions`

> **Description:** User subscription records linking users to their active tier.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | Reference to user. |
| `tier_id` | `bigint` | NO | - | **Foreign Key** to `subscription_tiers.id`. |
| `status` | `varchar(20)` | YES | `'ACTIVE'` | Status: `ACTIVE`, `EXPIRED`, `CANCELLED`. |
| `started_at` | `timestamptz` | NO | `now()` | Subscription start date. |
| `expires_at` | `timestamptz` | YES | - | Subscription expiration date. |
| `auto_renew` | `boolean` | YES | - | Whether subscription auto-renews. |
| `payment_reference` | `varchar(255)` | YES | - | Payment transaction reference. |
| **Usage Tracking** ||||
| `attempts_used` | `integer` | NO | `0` | Test attempts used this period. |
| `attempt_ais_used` | `integer` | NO | `0` | AI grading attempts used. |
| `ai_gradings_used` | `integer` | YES | `0` | Legacy: AI gradings used. |
| `chatbot_used` | `integer` | NO | `0` | Chatbot messages used. |
| `ai_grading_enabled` | `boolean` | YES | `true` | Whether AI grading is enabled. |
| `created_at` | `timestamptz` | YES | `now()` | Record creation timestamp. |
| `updated_at` | `timestamptz` | YES | `now()` | Last modification timestamp. |

**Constraints:**
- Foreign Key: `user_subscriptions_tier_id_fkey` references `subscription_tiers(id)`

**Usage Notes:**
- The `attempts_used`, `attempt_ais_used`, and `chatbot_used` columns track monthly usage against tier limits
- When `expires_at` is reached, these counters should be reset for the new billing period

---

#### `user_credits`

> **Description:** User Lúa (virtual currency) credit balance.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | Reference to user (unique). |
| `balance` | `integer` | NO | `0` | Current Lúa balance. |
| `lifetime_earned` | `integer` | NO | `0` | Total Lúa ever earned. |
| `lifetime_spent` | `integer` | NO | `0` | Total Lúa ever spent. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |
| `updated_at` | `timestamptz` | YES | - | Last modification timestamp. |

**Constraints:**
- Unique: `user_credits_user_id_key` on `user_id`

---

#### `credit_transactions`

> **Description:** Lúa transaction history for audit and tracking.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | Reference to user. |
| `amount` | `integer` | NO | - | Transaction amount (+/-). |
| `balance_after` | `integer` | NO | - | Balance after transaction. |
| `type` | `varchar(50)` | NO | - | Type: `EARN`, `SPEND`, `PURCHASE`, `BONUS`. |
| `category` | `varchar(50)` | NO | - | Category: `achievement`, `purchase`, `overage`, etc. |
| `description` | `text` | YES | - | Human-readable description. |
| `reference_id` | `varchar(100)` | YES | - | Related entity ID (order, attempt, etc.). |
| `created_at` | `timestamptz` | YES | `now()` | Transaction timestamp. |

**Indexes:**
- `idx_credit_transactions_user` on (`user_id`)
- `idx_credit_transactions_type` on (`user_id`, `type`)

---

#### `lua_packs`

> **Description:** Virtual currency (Lúa) pack definitions for purchase.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `code` | `varchar(50)` | NO | - | Unique pack code (e.g., `lua_100`). |
| `name_vi` | `varchar(100)` | NO | - | Vietnamese name. |
| `name_en` | `varchar(100)` | YES | - | English name. |
| `emoji` | `varchar(10)` | YES | - | Display emoji. |
| `lua_amount` | `integer` | NO | - | Base Lúa in pack. |
| `price_vnd` | `integer` | NO | - | Price in VND. |
| `discount_percent` | `integer` | YES | `0` | Discount percentage. |
| `bonus_lua` | `integer` | YES | `0` | Bonus Lúa included. |
| `description_vi` | `text` | YES | - | Vietnamese description. |
| `description_en` | `text` | YES | - | English description. |
| `is_active` | `boolean` | YES | `true` | Whether available for purchase. |
| `display_order` | `integer` | YES | - | Sort order. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |
| `updated_at` | `timestamptz` | YES | `now()` | Last modification. |

**Current Lúa Packs:**

| Code | Name | Lúa | Price (VND) | Discount |
|------|------|-----|-------------|----------|
| `lua_100` | Túi Lúa | 100 | 10,000 | 0% |
| `lua_500` | Bao Lúa | 500 | 45,000 | 10% |
| `lua_2000` | Xe Lúa | 2,000 | 150,000 | 25% |

---

#### `payment_orders`

> **Description:** Tracks all payment attempts via PayOS payment gateway.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | **Foreign Key** to `profiles.id`. |
| `tier_id` | `bigint` | YES | - | **Foreign Key** to `subscription_tiers.id`. |
| `lua_pack_id` | `bigint` | YES | - | Reference to Lúa pack. |
| `order_code` | `bigint` | NO | - | Unique PayOS order code. |
| `amount_vnd` | `integer` | NO | - | Order amount in VND. |
| `order_type` | `varchar(50)` | NO | - | Type: `subscription`, `lua_purchase`. |
| `status` | `varchar(50)` | NO | `'pending'` | Status: `pending`, `completed`, `cancelled`, `failed`. |
| `payment_link` | `text` | YES | - | PayOS checkout URL. |
| `description` | `text` | YES | - | Order description. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |
| `completed_at` | `timestamptz` | YES | - | Completion timestamp. |

**Constraints:**
- Foreign Key: `payment_orders_user_id_fkey` references `profiles(id)`
- Foreign Key: `payment_orders_tier_id_fkey` references `subscription_tiers(id)`
- Unique: `payment_orders_order_code_key` on `order_code`

---

### 3.5 AI Features

#### `chat_messages`

> **Description:** Chat conversation history for AI chatbot.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | Reference to user. |
| `role` | `varchar(20)` | NO | - | Role: `user`, `assistant`, `system`. |
| `content` | `text` | NO | - | Message content. |
| `tokens_used` | `integer` | YES | `0` | Token count for billing. |
| `created_at` | `timestamptz` | YES | `now()` | Message timestamp. |

**Indexes:**
- `idx_chat_messages_user` on (`user_id`)
- `idx_chat_messages_created` on (`user_id`, `created_at DESC`)

---

#### `chatbot_usage`

> **Description:** Daily chatbot usage tracking for quota enforcement.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | Reference to user. |
| `usage_date` | `date` | NO | `CURRENT_DATE` | Date of usage. |
| `messages_count` | `integer` | NO | `0` | Messages sent on this date. |
| `tokens_used` | `integer` | NO | `0` | Total tokens used. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |
| `updated_at` | `timestamptz` | YES | - | Last modification. |

**Constraints:**
- Unique: `chatbot_usage_user_id_usage_date_key` on (`user_id`, `usage_date`)

---

### 3.6 Vocabulary & Learning

#### `vocabulary`

> **Description:** User vocabulary notebook - stores saved words with translations and context.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | Reference to user. |
| `word` | `varchar(255)` | NO | - | The vocabulary word. |
| `vietnamese_meaning` | `text` | YES | - | Vietnamese translation. |
| `example_sentence` | `text` | YES | - | Example usage sentence. |
| `context` | `text` | YES | - | Context where word was found. |
| `is_mastered` | `boolean` | NO | `false` | Whether word is mastered. |
| `source_test_id` | `bigint` | YES | - | **Foreign Key** to `test_attempts.id`. |
| `source_section_id` | `bigint` | YES | - | **Foreign Key** to `sections.id`. |
| `notes` | `text` | YES | - | User's personal notes. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |
| `updated_at` | `timestamptz` | YES | - | Last modification. |

**Indexes:**
- `idx_vocabulary_user_id` on (`user_id`)
- `idx_vocabulary_word` on (`word`)
- `idx_vocabulary_mastered` on (`user_id`, `is_mastered`)

---

#### `translation_usage`

> **Description:** Monthly translation usage tracking for Vocabulary Notebook AI translations.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | Reference to user. |
| `usage_month` | `date` | NO | - | First day of the usage month. |
| `translations_used` | `integer` | NO | `0` | Translations used this month. |
| `created_at` | `timestamptz` | NO | `now()` | Creation timestamp. |
| `updated_at` | `timestamptz` | YES | `now()` | Last modification. |

**Constraints:**
- Unique: `translation_usage_user_id_usage_month_key` on (`user_id`, `usage_month`)

---

### 3.7 Quota Management

#### `user_quotas`

> **Description:** Global monthly quota tracking for Cramerie (free tier) users.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | Reference to user. |
| `quota_month` | `date` | NO | - | First day of quota month. |
| `attempts_used` | `integer` | NO | `0` | Test attempts used. |
| `ai_gradings_used` | `integer` | NO | `0` | AI gradings used. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |
| `updated_at` | `timestamptz` | YES | `now()` | Last modification. |

**Constraints:**
- Unique: `user_quotas_user_id_quota_month_key` on (`user_id`, `quota_month`)

---

#### `skill_quotas`

> **Description:** Per-skill monthly quota tracking (prevents grinding one skill).

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | Reference to user. |
| `skill` | `varchar(50)` | NO | - | Skill: `reading`, `listening`, `writing`, `speaking`. |
| `quota_month` | `date` | NO | - | First day of quota month. |
| `attempts_used` | `integer` | NO | `0` | Attempts for this skill. |
| `ai_gradings_used` | `integer` | NO | `0` | AI gradings for this skill. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |
| `updated_at` | `timestamptz` | YES | `now()` | Last modification. |

**Constraints:**
- Unique: `skill_quotas_user_id_skill_quota_month_key` on (`user_id`, `skill`, `quota_month`)

---

#### `target`

> **Description:** User learning targets and goals.

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | Identity | **Primary Key.** Auto-incrementing. |
| `user_id` | `uuid` | NO | - | Reference to user. |
| `exam_type` | `varchar` | YES | - | Target exam type. |
| `test_date` | `date` | YES | - | Planned test date. |
| `target_score` | `numeric` | YES | - | Target band score. |
| `created_at` | `timestamptz` | YES | `now()` | Creation timestamp. |
| `updated_at` | `timestamptz` | YES | `now()` | Last modification. |

---

## 4. Relationships & ERD

### Entity Relationship Diagram

```
┌─────────────────┐
│    profiles     │◄────────────────────────────────────────────────┐
│  (auth users)   │                                                  │
└────────┬────────┘                                                  │
         │                                                           │
         │ 1:1                                                       │
         ▼                                                           │
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐│
│  user_credits   │     │ user_subscriptions│────►│subscription_tiers││
└─────────────────┘     └──────────────────┘     └─────────────────┘│
         │                                                           │
         │ 1:N                                                       │
         ▼                                                           │
┌─────────────────┐     ┌──────────────────┐                        │
│credit_transactions│   │   payment_orders  │────────────────────────┘
└─────────────────┘     └──────────────────┘

┌─────────────────┐
│    sections     │◄────────────────────────────────────────────────┐
│ (test content)  │                                                  │
└────────┬────────┘                                                  │
         │                                                           │
         │ 1:N                                                       │
         ▼                                                           │
┌─────────────────┐                                                  │
│    questions    │                                                  │
└────────┬────────┘                                                  │
         │                                                           │
         │                    ┌──────────────────┐                   │
         │                    │   test_attempts   │◄──────────────────┘
         │                    └────────┬─────────┘
         │                             │
         │               ┌─────────────┼──────────────┐
         │               │             │              │
         │               ▼             ▼              ▼
         │    ┌─────────────────┐ ┌───────────┐ ┌───────────────┐
         └───►│   user_answers  │ │ vocabulary│ │writing_submissions│
              └─────────────────┘ └───────────┘ └───────────────┘
```

### Foreign Key Relationships

| Source Table | Source Column | Target Table | Target Column |
|--------------|---------------|--------------|---------------|
| `payment_orders` | `user_id` | `profiles` | `id` |
| `payment_orders` | `tier_id` | `subscription_tiers` | `id` |
| `questions` | `section_id` | `sections` | `id` |
| `user_answers` | `attempt_id` | `test_attempts` | `id` |
| `user_answers` | `question_id` | `questions` | `id` |
| `user_subscriptions` | `tier_id` | `subscription_tiers` | `id` |
| `vocabulary` | `source_test_id` | `test_attempts` | `id` |
| `vocabulary` | `source_section_id` | `sections` | `id` |
| `writing_submissions` | `attempt_id` | `test_attempts` | `id` |

---

## 5. Indexes

### Performance Indexes

| Table | Index Name | Columns | Type |
|-------|------------|---------|------|
| `chat_messages` | `idx_chat_messages_user` | `user_id` | B-tree |
| `chat_messages` | `idx_chat_messages_created` | `user_id, created_at DESC` | B-tree |
| `chatbot_usage` | `idx_chatbot_usage_user_date` | `user_id, usage_date` | B-tree |
| `credit_transactions` | `idx_credit_transactions_user` | `user_id` | B-tree |
| `credit_transactions` | `idx_credit_transactions_type` | `user_id, type` | B-tree |
| `questions` | `idx_questions_section` | `section_id` | B-tree |
| `sections` | `idx_sections_skill` | `skill` | B-tree |
| `test_attempts` | `idx_test_attempts_user` | `user_id` | B-tree |
| `test_attempts` | `idx_test_attempts_section` | `section_id` | B-tree |
| `test_attempts` | `idx_test_attempts_skill` | `skill` | B-tree |
| `user_answers` | `idx_user_answers_attempt` | `attempt_id` | B-tree |
| `vocabulary` | `idx_vocabulary_user_id` | `user_id` | B-tree |
| `vocabulary` | `idx_vocabulary_word` | `word` | B-tree |
| `vocabulary` | `idx_vocabulary_mastered` | `user_id, is_mastered` | B-tree |
| `writing_submissions` | `idx_writing_submissions_user` | `user_id` | B-tree |
| `writing_submissions` | `idx_writing_submissions_status` | `grading_status` | B-tree |

---

## 6. Row Level Security (RLS)

Row Level Security is enabled on most tables to ensure users can only access their own data.

### RLS Policy Summary

| Table | Policy Name | Command | Rule |
|-------|-------------|---------|------|
| **chat_messages** | Service role full access | ALL | `auth.jwt() ->> 'role' = 'service_role'` |
| | Users can view own chat messages | SELECT | `auth.uid() = user_id` |
| **chatbot_usage** | Service role full access | ALL | `auth.jwt() ->> 'role' = 'service_role'` |
| | Users can view own chatbot usage | SELECT | `auth.uid() = user_id` |
| **credit_transactions** | Service role full access | ALL | `auth.jwt() ->> 'role' = 'service_role'` |
| | Users can view own transactions | SELECT | `auth.uid() = user_id` |
| **lua_packs** | lua_packs_public_read | SELECT | `is_active = true` |
| **profiles** | Enable read access for all users | SELECT | `true` |
| | Users can update own profile | UPDATE | `auth.uid() = id` |
| **questions** | Public read access | SELECT | `true` |
| **sections** | Public read access | SELECT | `true` |
| **subscription_tiers** | Public read access | SELECT | `true` |
| **test_attempts** | Service role full access | ALL | `auth.jwt() ->> 'role' = 'service_role'` |
| | Users can view own attempts | SELECT | `auth.uid() = user_id` |
| **translation_usage** | Service role full access | ALL | `auth.jwt() ->> 'role' = 'service_role'` |
| | Users can view own translation usage | SELECT | `auth.uid() = user_id` |
| **user_credits** | Service role full access | ALL | `auth.jwt() ->> 'role' = 'service_role'` |
| | Users can view own credits | SELECT | `auth.uid() = user_id` |
| **user_quotas** | Service role full access | ALL | `auth.jwt() ->> 'role' = 'service_role'` |
| | Users can view own quotas | SELECT | `auth.uid() = user_id` |
| **user_subscriptions** | Service role full access | ALL | `auth.jwt() ->> 'role' = 'service_role'` |
| | Users can view own subscriptions | SELECT | `auth.uid() = user_id` |
| **vocabulary** | Service role full access | ALL | `auth.jwt() ->> 'role' = 'service_role'` |
| | Users can view own vocabulary | SELECT | `auth.uid() = user_id` |
| | Users can insert own vocabulary | INSERT | `auth.uid() = user_id` |
| | Users can update own vocabulary | UPDATE | `auth.uid() = user_id` |
| | Users can delete own vocabulary | DELETE | `auth.uid() = user_id` |
| **writing_submissions** | Service role can manage all | ALL | `true` (service_role only) |
| | Users can manage their own | ALL | `auth.uid() = user_id` |

---

## 7. Database Functions

### `initialize_user_credits()`

> **Purpose:** Automatically creates a user_credits record when a new profile is created.

```sql
CREATE OR REPLACE FUNCTION public.initialize_user_credits()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $function$
BEGIN
    INSERT INTO public.user_credits (user_id, balance, lifetime_earned, lifetime_spent)
    VALUES (NEW.id, 0, 0, 0);
    RETURN NEW;
END;
$function$;
```

---

### `check_and_award_achievement(p_user_id, p_achievement_code)`

> **Purpose:** Award achievement to user if not already earned.

| Parameter | Type | Description |
|-----------|------|-------------|
| `p_user_id` | `uuid` | User to award achievement to |
| `p_achievement_code` | `varchar` | Achievement code to check |

**Returns:** `boolean` - Whether achievement was newly awarded

---

### `trigger_set_timestamp()`

> **Purpose:** Automatically updates the `updated_at` column on update.

```sql
CREATE OR REPLACE FUNCTION public.trigger_set_timestamp()
RETURNS trigger
LANGUAGE plpgsql
AS $function$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$function$;
```

---

### `update_skill_quotas_updated_at()`

> **Purpose:** Trigger function to update `updated_at` on skill_quotas.

---

### `update_user_quotas_updated_at()`

> **Purpose:** Trigger function to update `updated_at` on user_quotas.

---

## 8. Triggers

| Trigger Name | Table | Event | Timing | Function |
|--------------|-------|-------|--------|----------|
| `on_profile_created_init_credits` | `profiles` | INSERT | AFTER | `initialize_user_credits()` |
| `set_timestamp` | `target` | UPDATE | BEFORE | `trigger_set_timestamp()` |
| `trigger_skill_quotas_updated_at` | `skill_quotas` | UPDATE | BEFORE | `update_skill_quotas_updated_at()` |
| `trigger_user_quotas_updated_at` | `user_quotas` | UPDATE | BEFORE | `update_user_quotas_updated_at()` |

---

## 9. Migrations History

| Version | Name | Description |
|---------|------|-------------|
| 20251210014215 | `enable_rls_on_profiles` | Enable RLS on profiles table |
| 20251210014216 | `enable_rls_on_target` | Enable RLS on target table |
| 20251210014415 | `add_service_role_policies` | Add service role bypass policies |
| 20251212160355 | `004_add_image_description_column` | Add image description to sections |
| 20251213065028 | `005_vocabulary_table` | Create vocabulary table |
| 20251213065106 | `006_subscription_and_credits` | Create subscription system |
| 20251213065143 | `007_achievements_and_chat` | Create chat and achievements |
| 20251213065215 | `008_user_initialization_triggers` | Add user init triggers |
| 20251213083301 | `payos_payment_integration` | Add PayOS payment orders |
| 20251213085704 | `add_initial_lua_column` | Add initial Lúa to tiers |
| 20251214045941 | `restructure_subscription_system` | Major subscription refactor |
| 20251214051541 | `add_attempt_tracking_columns` | Add quota tracking columns |
| 20251214065713 | `fix_cramerie_attempt_limits` | Fix free tier limits |
| 20251214072921 | `add_ai_grading_enabled` | Add AI grading toggle |
| 20251226000001 | `test_storage_overhaul` | Add test_sets, tests, hashtags tables and update sections |

---

## 10. Extensions

### Installed Extensions

| Extension | Schema | Version | Description |
|-----------|--------|---------|-------------|
| `pg_graphql` | graphql | 1.5.11 | GraphQL support for Supabase |
| `supabase_vault` | vault | 0.3.1 | Encrypted secrets storage |
| `pgcrypto` | extensions | 1.3 | Cryptographic functions |
| `uuid-ossp` | extensions | 1.1 | UUID generation functions |
| `pgjwt` | extensions | 0.2.0 | JWT creation and verification |

### Available (Not Installed) Extensions

Notable extensions available for future use:
- `vector` (0.8.0) - Vector similarity search for AI/ML features
- `pg_cron` (1.6) - Job scheduling
- `http` (1.6) - HTTP client for external APIs
- `postgis` (3.3.7) - Geospatial data types

---

## Appendix A: Common Queries

### Get user's current subscription tier

```sql
SELECT 
    us.id,
    us.status,
    st.code,
    st.name_vi,
    st.monthly_attempt_limit,
    st.monthly_attempt_ai_limit
FROM user_subscriptions us
JOIN subscription_tiers st ON us.tier_id = st.id
WHERE us.user_id = :userId
    AND us.status = 'active'
ORDER BY us.started_at DESC
LIMIT 1;
```

### Get user's current quota usage

```sql
SELECT 
    attempts_used,
    ai_gradings_used,
    quota_month
FROM user_quotas
WHERE user_id = :userId
    AND quota_month = date_trunc('month', CURRENT_DATE)::date;
```

### Get test sections with question counts

```sql
SELECT 
    s.id,
    s.exam_source,
    s.test_number,
    s.skill,
    s.part_number,
    COUNT(q.id) as question_count
FROM sections s
LEFT JOIN questions q ON q.section_id = s.id
GROUP BY s.id
ORDER BY s.exam_source, s.test_number, s.skill, s.part_number;
```

---

## Appendix B: JSON Schema Examples

### `question_content` for Multiple Choice

```json
{
    "text": "What was the main purpose of the first underground line?",
    "options": [
        "A To transport goods",
        "B To connect mainline stations",
        "C To serve the financial district"
    ]
}
```

### `section_layout` for Listening

```json
{
    "blocks": [
        {
            "block_type": "NOTE_COMPLETION",
            "content": {
                "title": "Questions 1-10",
                "instructions_text": "Complete the notes below...",
                "main_title": "Buckworth Conservation Group"
            },
            "question_numbers": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        }
    ]
}
```

### `ai_feedback` for Writing Submissions

```json
{
    "task_achievement": {
        "band": 6.5,
        "feedback": "The response addresses all parts of the task..."
    },
    "coherence_cohesion": {
        "band": 7.0,
        "feedback": "Ideas are arranged coherently..."
    },
    "lexical_resource": {
        "band": 6.0,
        "feedback": "Vocabulary is adequate for the task..."
    },
    "grammatical_range": {
        "band": 6.5,
        "feedback": "A mix of simple and complex sentences..."
    },
    "overall_comments": "This is a well-structured essay...",
    "suggestions": [
        "Consider using more academic vocabulary",
        "Vary sentence structures more"
    ]
}
```

---

*This documentation was auto-generated on 2025-12-15 based on the live Supabase database schema.*
