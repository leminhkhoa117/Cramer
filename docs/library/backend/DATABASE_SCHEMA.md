# Cramer IELTS Platform - Database Schema Documentation

> **Generated:** January 6, 2026  
> **Database:** Supabase PostgreSQL  
> **Total Tables:** 26  
> **Tables with RLS:** 22  
> **Applied Migrations:** 39

---

## Table of Contents

1. [Overview](#overview)
2. [Extensions](#extensions)
3. [Table Summary](#table-summary)
4. [Core Content Tables](#core-content-tables)
5. [User & Authentication Tables](#user--authentication-tables)
6. [Test & Learning Tables](#test--learning-tables)
7. [Subscription & Payment Tables](#subscription--payment-tables)
8. [Quota & Usage Tables](#quota--usage-tables)
9. [Admin & Audit Tables](#admin--audit-tables)
10. [AI & Templates Tables](#ai--templates-tables)
11. [Foreign Key Relationships](#foreign-key-relationships)
12. [Row Level Security (RLS) Policies](#row-level-security-rls-policies)
13. [Database Triggers](#database-triggers)
14. [Indexes](#indexes)
15. [Migration History](#migration-history)
16. [Entity Relationship Diagram](#entity-relationship-diagram)

---

## Overview

The Cramer IELTS platform uses Supabase (PostgreSQL) as its primary database. The schema is designed to support:

- **IELTS Test Management**: Reading, Listening, Writing sections with questions
- **User Progress Tracking**: Test attempts, answers, and AI grading
- **Subscription System**: Tiered plans (Cramerie, Cramerich, Cramerous)
- **Virtual Currency (Lúa)**: Credits for premium features
- **Vocabulary Notebook**: Personal word collections with AI translation
- **Admin Management**: User management, audit logs, activity tracking

---

## Extensions

### Installed Extensions

| Extension | Schema | Version | Description |
|-----------|--------|---------|-------------|
| `pg_graphql` | graphql | 1.5.11 | GraphQL support for Supabase |
| `supabase_vault` | vault | 0.3.1 | Secrets management |
| `pgcrypto` | extensions | 1.3 | Cryptographic functions |
| `pg_stat_statements` | extensions | 1.11 | Query performance tracking |
| `uuid-ossp` | extensions | 1.1 | UUID generation |
| `plpgsql` | pg_catalog | 1.0 | PL/pgSQL procedural language |

---

## Table Summary

| Table | Rows | RLS | Description |
|-------|------|-----|-------------|
| `profiles` | 4 | ✅ | User profile information |
| `target` | 3 | ✅ | User IELTS score targets |
| `test_sets` | 3 | ✅ | Test collections (e.g., Cambridge IELTS 17) |
| `tests` | 6 | ✅ | Individual tests within sets |
| `sections` | 34 | ❌ | Test sections (Reading passages, Listening parts) |
| `questions` | 360 | ❌ | IELTS questions with answers |
| `hashtags` | 24 | ✅ | Content categorization tags |
| `test_hashtags` | 2 | ✅ | Many-to-many: tests ↔ hashtags |
| `test_attempts` | 82 | ✅ | User test attempt records |
| `user_answers` | 93 | ✅ | User answers for questions |
| `writing_submissions` | 14 | ✅ | Writing essays with AI grading |
| `vocabulary` | 4 | ✅ | User vocabulary notebook |
| `subscription_tiers` | 3 | ❌ | Available subscription plans |
| `user_subscriptions` | 4 | ✅ | User subscription records |
| `user_credits` | 4 | ✅ | User Lúa balance |
| `credit_transactions` | 20 | ✅ | Lúa transaction history |
| `lua_packs` | 3 | ✅ | Lúa purchase packages |
| `payment_orders` | 15 | ✅ | PayOS payment records |
| `user_quotas` | 5 | ✅ | Global monthly quota tracking |
| `skill_quotas` | 8 | ✅ | Per-skill monthly quotas |
| `translation_usage` | 4 | ✅ | AI translation usage tracking |
| `chatbot_usage` | 3 | ✅ | Daily chatbot message usage |
| `chat_messages` | 8 | ✅ | Chat conversation history |
| `user_activities` | 15 | ✅ | User activity timeline |
| `admin_audit_log` | 13 | ✅ | Admin action audit trail |
| `abts_templates` | 8 | ✅ | AI test generation templates |

---

## Core Content Tables

### `test_sets`
> Top-level test collections/folders (e.g., Cambridge IELTS 17)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY | Primary key |
| `code` | varchar | NO | - | Unique identifier (e.g., cam17) |
| `name` | varchar | NO | - | Display name |
| `description` | text | YES | - | Description |
| `cover_image_url` | varchar | YES | - | Cover image |
| `source_type` | varchar | YES | 'custom' | Source type: cambridge, custom, ai_generated |
| `is_published` | boolean | YES | false | Publication status |
| `display_order` | integer | YES | 0 | Sort order |
| `created_by` | uuid | YES | - | FK → auth.users |
| `created_at` | timestamptz | YES | now() | - |
| `updated_at` | timestamptz | YES | now() | - |

**Indexes:** `test_sets_pkey`, `test_sets_code_key` (UNIQUE), `idx_test_sets_published`, `idx_test_sets_source_type`, `idx_test_sets_display_order`

---

### `tests`
> Individual IELTS tests within a test set

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY | Primary key |
| `set_id` | bigint | NO | - | FK → test_sets.id |
| `test_number` | integer | NO | - | Test number within set |
| `name` | varchar | YES | - | Test name |
| `description` | text | YES | - | Description |
| `difficulty` | varchar | YES | 'INTERMEDIATE' | BEGINNER, LOWER_INTERMEDIATE, INTERMEDIATE, UPPER_INTERMEDIATE, ADVANCED |
| `estimated_time_minutes` | integer | YES | 170 | Estimated completion time |
| `is_published` | boolean | YES | false | Publication status |
| `is_ai_generated` | boolean | YES | false | AI-generated flag |
| `generation_metadata` | jsonb | YES | - | ABTS generation inputs |
| `created_by` | uuid | YES | - | FK → auth.users |
| `created_at` | timestamptz | YES | now() | - |
| `updated_at` | timestamptz | YES | now() | - |

**Indexes:** `tests_pkey`, `tests_set_id_test_number_key` (UNIQUE), `idx_tests_set_id`, `idx_tests_published`, `idx_tests_ai_generated`

**Foreign Keys:**
- `set_id` → `test_sets.id`

---

### `sections`
> Test sections (Reading passages, Listening parts, Writing tasks)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY | Primary key |
| `test_id` | bigint | YES | - | FK → tests.id (NULL for legacy data) |
| `exam_source` | varchar | YES | - | Legacy: Source book (e.g., "IELTS Cambridge 17") |
| `test_number` | integer | YES | - | Legacy: Test number |
| `skill` | varchar | YES | - | READING, LISTENING, WRITING |
| `part_number` | integer | YES | - | Part/passage number |
| `display_content_url` | varchar | YES | - | Image URL for display |
| `passage_text` | text | YES | - | Full passage text |
| `audio_url` | varchar | YES | - | Audio URL for listening |
| `section_layout` | jsonb | YES | - | Layout configuration |
| `image_description` | text | YES | - | AI grading: text description of images |
| `status` | varchar | YES | 'PUBLISHED' | DRAFT, PUBLISHED, ARCHIVED |
| `created_at` | timestamptz | YES | now() | - |
| `updated_at` | timestamptz | YES | now() | - |

**Indexes:** `sections_pkey`, `sections_unique_exam_test_skill_part` (UNIQUE), `sections_exam_test_skill_idx`, `idx_sections_test_id`, `idx_sections_status`

**Foreign Keys:**
- `test_id` → `tests.id`

---

### `questions`
> IELTS questions with correct answers and explanations

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY | Primary key |
| `section_id` | bigint | YES | - | FK → sections.id |
| `question_number` | integer | YES | - | Question number within section |
| `question_uid` | varchar | YES | - | Unique identifier (UNIQUE) |
| `question_type` | varchar | YES | - | Type: MCQ, FILL_BLANK, TRUE_FALSE_NG, MATCHING, etc. |
| `question_content` | jsonb | YES | - | Question data (prompt, options, etc.) |
| `correct_answer` | jsonb | YES | - | Correct answer(s) |
| `image_url` | varchar | YES | - | Question image URL |
| `word_limit` | varchar | YES | - | Word limit for short answers |
| `explanation` | jsonb | YES | - | Structured: {detail, quote, strategy} |

**Indexes:** `questions_pkey`, `questions_question_uid_key` (UNIQUE), `questions_section_idx`

**Foreign Keys:**
- `section_id` → `sections.id`

---

### `hashtags`
> Categorization tags for tests (topics, themes, difficulty)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY | Primary key |
| `code` | varchar | NO | - | Unique code (UNIQUE) |
| `name` | varchar | YES | - | Display name |
| `category` | varchar | NO | - | topic, theme, difficulty, source, skill_focus |
| `icon` | varchar | YES | - | Emoji icon |
| `color` | varchar | YES | - | Display color |
| `use_count` | integer | YES | 0 | Usage count |
| `is_active` | boolean | YES | true | Active status |
| `created_at` | timestamptz | YES | now() | - |

**Indexes:** `hashtags_pkey`, `hashtags_code_key` (UNIQUE), `idx_hashtags_category`, `idx_hashtags_active`, `idx_hashtags_use_count`

---

### `test_hashtags`
> Many-to-many junction between tests and hashtags

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `test_id` | bigint | NO | - | FK → tests.id |
| `hashtag_id` | bigint | NO | - | FK → hashtags.id |
| `is_primary` | boolean | YES | false | Primary topic hashtag |
| `created_at` | timestamptz | YES | now() | - |

**Primary Key:** (`test_id`, `hashtag_id`)

**Foreign Keys:**
- `test_id` → `tests.id`
- `hashtag_id` → `hashtags.id`

---

## User & Authentication Tables

### `profiles`
> User profile information (extends Supabase auth.users)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | uuid | NO | gen_random_uuid() | Primary key (matches auth.users.id) |
| `username` | varchar | NO | '' | Username (UNIQUE) |
| `full_name` | varchar | YES | - | Display name |
| `phone_number` | varchar | YES | - | Phone number |
| `address` | varchar | YES | - | Address |
| `avatar_url` | varchar | YES | - | Profile picture URL |
| `hero_background_url` | varchar | YES | - | Hero background image |
| `page_background_url` | varchar | YES | - | Page background image |
| `llm_api_key` | varchar | YES | - | User's LLM API key (encrypted) |
| `llm_model` | varchar | YES | - | Selected LLM model |
| `llm_provider` | varchar | YES | 'deepseek' | LLM provider: deepseek, openai, gemini, anthropic |
| `is_admin` | boolean | YES | - | Admin flag |
| `account_status` | varchar | YES | 'ACTIVE' | ACTIVE, BANNED, DEACTIVATED, DELETED |
| `last_login_at` | timestamptz | YES | - | Last login timestamp |
| `status_reason` | text | YES | - | Reason for ban/deactivation |
| `created_at` | timestamptz | NO | now() | - |

**Indexes:** `profiles_pkey`, `profiles_username_key` (UNIQUE), `idx_profiles_account_status`

---

### `target`
> User IELTS score targets

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | uuid | NO | gen_random_uuid() | Primary key |
| `user_id` | uuid | NO | - | FK → auth.users.id (UNIQUE) |
| `exam_name` | varchar | NO | - | Target exam |
| `exam_date` | date | YES | - | Target exam date |
| `listening` | float8 | YES | - | Target band (0-9) |
| `reading` | float8 | YES | - | Target band (0-9) |
| `writing` | float8 | YES | - | Target band (0-9) |
| `speaking` | float8 | YES | - | Target band (0-9) |
| `created_at` | timestamptz | NO | now() | - |
| `updated_at` | timestamptz | NO | now() | - |

**Indexes:** `target_pkey`, `target_user_id_key` (UNIQUE)

**Check Constraints:**
- `listening >= 0 AND listening <= 9`
- `reading >= 0 AND reading <= 9`
- `writing >= 0 AND writing <= 9`
- `speaking >= 0 AND speaking <= 9`

---

### `vocabulary`
> User vocabulary notebook with translations and context

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY | Primary key |
| `user_id` | uuid | NO | - | FK → auth.users.id |
| `word` | varchar | NO | - | The vocabulary word |
| `translation` | text | YES | - | Translation |
| `phonetic` | varchar | YES | - | Phonetic transcription |
| `part_of_speech` | varchar | YES | - | Part of speech |
| `definition` | text | YES | - | Word definition |
| `example_sentence` | text | YES | - | Example usage |
| `source_context` | text | YES | - | Context where word was found |
| `source_test_id` | bigint | YES | - | FK → test_attempts.id |
| `source_section_id` | bigint | YES | - | FK → sections.id |
| `notes` | text | YES | - | User notes |
| `is_mastered` | boolean | YES | false | Mastery status |
| `review_count` | integer | YES | 0 | Review count |
| `last_reviewed_at` | timestamptz | YES | - | Last review date |
| `created_at` | timestamptz | YES | now() | - |
| `updated_at` | timestamptz | YES | now() | - |

**Indexes:** `vocabulary_pkey`, `idx_vocabulary_user_id`, `idx_vocabulary_word`, `idx_vocabulary_mastered`

---

## Test & Learning Tables

### `test_attempts`
> Tracks user test attempt sessions

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `user_id` | uuid | NO | - | FK → auth.users.id |
| `exam_source` | varchar | NO | - | Source book |
| `test_number` | varchar | NO | - | Test number |
| `skill` | varchar | NO | - | READING, LISTENING, WRITING |
| `status` | varchar | NO | 'IN_PROGRESS' | IN_PROGRESS, COMPLETED, CANCELLED |
| `score` | integer | YES | - | Final score |
| `current_part` | integer | YES | - | Current part progress |
| `time_left` | integer | YES | - | Remaining time (seconds) |
| `started_at` | timestamptz | NO | now() | - |
| `completed_at` | timestamptz | YES | - | - |

**Indexes:** `test_attempts_pkey`

---

### `user_answers`
> User answers to questions for test attempts

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `attempt_id` | bigint | NO | - | FK → test_attempts.id |
| `question_id` | bigint | NO | - | FK → questions.id |
| `user_id` | uuid | NO | - | User ID |
| `answer_content` | jsonb | NO | - | Answer data |
| `user_answer` | varchar | NO | - | Text answer |
| `is_correct` | boolean | YES | - | Correctness |
| `submitted_at` | timestamptz | NO | now() | - |
| `created_at` | timestamptz | NO | - | - |

**Indexes:** `user_answers_pkey`, `user_answers_attempt_id_question_id_key` (UNIQUE)

**Foreign Keys:**
- `attempt_id` → `test_attempts.id`
- `question_id` → `questions.id`

---

### `writing_submissions`
> Writing essays with AI grading results

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `attempt_id` | bigint | NO | - | FK → test_attempts.id |
| `user_id` | uuid | NO | - | FK → auth.users.id |
| `task_number` | integer | NO | - | Task 1 or 2 |
| `essay_text` | text | NO | - | Essay content |
| `word_count` | integer | NO | 0 | Word count |
| `grading_status` | varchar | NO | 'PENDING' | PENDING, GRADING, GRADED, FAILED |
| `overall_band` | numeric | YES | - | Overall band score |
| `band_scores` | jsonb | YES | - | Detailed band scores (TR, CC, LR, GRA) |
| `ai_feedback` | jsonb | YES | - | AI feedback and suggestions |
| `submitted_at` | timestamptz | NO | now() | - |
| `graded_at` | timestamptz | YES | - | - |
| `created_at` | timestamptz | NO | now() | - |

**Indexes:** `writing_submissions_pkey`, `writing_submissions_attempt_id_task_number_key` (UNIQUE), `idx_writing_submissions_user`, `idx_writing_submissions_attempt`, `idx_writing_submissions_status`

**Check Constraints:**
- `task_number IN (1, 2)`

---

### `chat_messages`
> Chat conversation history with AI assistant

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `user_id` | uuid | NO | - | FK → auth.users.id |
| `role` | varchar | NO | - | user, assistant, system |
| `content` | text | NO | - | Message content |
| `tokens_used` | integer | YES | 0 | Token count |
| `created_at` | timestamptz | YES | now() | - |

**Indexes:** `chat_messages_pkey`, `idx_chat_messages_user`, `idx_chat_messages_created`

**Check Constraints:**
- `role IN ('user', 'assistant', 'system')`

---

## Subscription & Payment Tables

### `subscription_tiers`
> Available subscription plans

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | nextval | Primary key |
| `code` | varchar | NO | - | Tier code (UNIQUE) |
| `name` | varchar | YES | - | Display name |
| `price_vnd` | integer | NO | 0 | Monthly price in VND |
| `included_ai_gradings` | integer | YES | 0 | AI gradings per month |
| `daily_chat_limit` | integer | YES | 20 | Daily chat limit |
| `chatbot_monthly_limit` | integer | NO | 0 | Monthly chatbot limit |
| `vocab_ai_daily_limit` | integer | NO | 0 | Daily vocab AI limit |
| `monthly_attempt_limit` | integer | NO | 0 | Global monthly attempt limit |
| `monthly_attempt_ai_limit` | integer | NO | 0 | Monthly AI attempt limit |
| `per_skill_attempt_limit` | integer | NO | 0 | Per-skill attempt limit |
| `per_skill_attempt_ai_limit` | integer | NO | 0 | Per-skill AI attempt limit |
| `monthly_translation_limit` | integer | NO | 0 | Monthly AI translation limit |
| `max_vocabulary_entries` | integer | NO | 0 | Max vocabulary entries |
| `attempt_overage_cost` | integer | NO | 10 | Lúa cost per extra attempt |
| `attempt_ai_overage_cost` | integer | NO | 20 | Lúa cost per extra AI attempt |
| `chatbot_overage_cost` | integer | NO | 2 | Lúa cost per chat message |
| `translation_overage_cost` | integer | NO | 1 | Lúa cost per translation |
| `initial_lua` | integer | NO | 50 | Initial Lúa on signup |
| `monthly_lua_bonus` | integer | YES | 0 | Monthly Lúa bonus |
| `features` | jsonb | YES | '{}' | Feature flags |
| `is_active` | boolean | YES | true | Active status |
| `sort_order` | integer | YES | 0 | Display order |
| `display_order` | integer | YES | - | Alternative display order |
| `created_at` | timestamptz | YES | now() | - |

**Indexes:** `subscription_tiers_pkey`, `subscription_tiers_code_key` (UNIQUE)

**Current Tiers:**
- 🌾 **Cramerie** (Free): 0 AI gradings, 20 chat/day, 50 initial Lúa
- 🌻 **Cramerich** (79,000đ): 5 AI gradings/mo, 100 chat/day, 100 initial Lúa
- 🌟 **Cramerous** (149,000đ): 10 AI gradings/mo, unlimited chat, 200 initial Lúa

---

### `user_subscriptions`
> User subscription records

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `user_id` | uuid | NO | - | FK → auth.users.id |
| `tier_id` | bigint | NO | - | FK → subscription_tiers.id |
| `status` | varchar | YES | 'ACTIVE' | ACTIVE, EXPIRED, CANCELLED, PENDING |
| `started_at` | timestamptz | NO | now() | - |
| `expires_at` | timestamptz | NO | - | Expiration date |
| `auto_renew` | boolean | YES | - | Auto-renewal flag |
| `attempts_used` | integer | NO | 0 | Monthly attempts used |
| `attempt_ais_used` | integer | NO | 0 | Monthly AI attempts used |
| `chatbot_used` | integer | NO | 0 | Monthly chatbot used |
| `ai_grading_enabled` | boolean | YES | true | AI grading toggle |
| `payment_reference` | varchar | YES | - | Payment reference |
| `created_at` | timestamptz | YES | now() | - |
| `updated_at` | timestamptz | YES | now() | - |

**Indexes:** `user_subscriptions_pkey`, `idx_user_subscriptions_user`, `idx_user_subscriptions_status`, `idx_user_subscriptions_usage`

**Check Constraints:**
- `status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'PENDING')`

---

### `user_credits`
> User Lúa (virtual currency) balance

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `user_id` | uuid | NO | - | FK → auth.users.id (UNIQUE) |
| `balance` | integer | YES | 50 | Current balance |
| `lifetime_earned` | integer | YES | 50 | Total earned |
| `lifetime_spent` | integer | YES | 0 | Total spent |
| `created_at` | timestamptz | YES | now() | - |
| `updated_at` | timestamptz | YES | now() | - |

**Indexes:** `user_credits_pkey`, `user_credits_user_id_key` (UNIQUE), `idx_user_credits_user`

**Check Constraints:**
- `balance >= 0`

---

### `credit_transactions`
> Lúa transaction history

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `user_id` | uuid | NO | - | FK → auth.users.id |
| `amount` | integer | NO | - | Transaction amount |
| `balance_after` | integer | NO | - | Balance after transaction |
| `type` | varchar | NO | - | EARN, SPEND, PURCHASE, REFUND, BONUS, ADMIN |
| `category` | varchar | YES | - | Transaction category |
| `description` | varchar | YES | - | Description |
| `reference_id` | varchar | YES | - | Reference ID |
| `created_at` | timestamptz | YES | now() | - |

**Indexes:** `credit_transactions_pkey`, `idx_credit_transactions_user`, `idx_credit_transactions_type`

**Check Constraints:**
- `type IN ('EARN', 'SPEND', 'PURCHASE', 'REFUND', 'BONUS', 'ADMIN')`

---

### `lua_packs`
> Virtual currency (Lúa) pack definitions for purchase

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `code` | varchar | NO | - | Pack code (UNIQUE) |
| `name` | varchar | YES | - | Display name |
| `emoji` | varchar | YES | '🌾' | Emoji icon |
| `lua_amount` | integer | NO | - | Base Lúa amount |
| `price_vnd` | integer | NO | - | Price in VND |
| `discount_percent` | integer | NO | 0 | Discount percentage |
| `bonus_lua` | integer | NO | 0 | Bonus Lúa |
| `description_vi` | varchar | YES | - | Vietnamese description |
| `description_en` | varchar | YES | - | English description |
| `is_active` | boolean | NO | true | Active status |
| `display_order` | integer | YES | 0 | Sort order |
| `created_at` | timestamptz | YES | now() | - |
| `updated_at` | timestamptz | YES | now() | - |

**Indexes:** `lua_packs_pkey`, `lua_packs_code_key` (UNIQUE)

---

### `payment_orders`
> PayOS payment order tracking

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | nextval | Primary key |
| `user_id` | uuid | NO | - | FK → profiles.id |
| `order_code` | bigint | NO | - | PayOS order code (UNIQUE) |
| `payment_link_id` | varchar | YES | - | PayOS payment link ID |
| `checkout_url` | varchar | YES | - | Checkout URL |
| `qr_code` | text | YES | - | QR code data |
| `type` | varchar | NO | - | SUBSCRIPTION or LUA_PACK |
| `tier_id` | bigint | YES | - | FK → subscription_tiers.id |
| `tier_code` | varchar | YES | - | Tier code |
| `lua_amount` | integer | YES | - | Lúa amount (for LUA_PACK) |
| `amount_vnd` | integer | NO | - | Payment amount |
| `description` | varchar | YES | - | Short description (max 25 chars) |
| `status` | varchar | NO | 'PENDING' | PENDING, PAID, CANCELLED, EXPIRED, FAILED |
| `transaction_datetime` | varchar | YES | - | Transaction datetime |
| `created_at` | timestamptz | NO | now() | - |
| `paid_at` | timestamptz | YES | - | Payment timestamp |
| `expires_at` | timestamptz | YES | - | Expiration timestamp |

**Indexes:** `payment_orders_pkey`, `payment_orders_order_code_key` (UNIQUE), `idx_payment_orders_user_id`, `idx_payment_orders_order_code`, `idx_payment_orders_status`, `idx_payment_orders_created_at`

**Check Constraints:**
- `type IN ('SUBSCRIPTION', 'LUA_PACK')`
- `status IN ('PENDING', 'PAID', 'CANCELLED', 'EXPIRED', 'FAILED')`

---

## Quota & Usage Tables

### `user_quotas`
> Global monthly quota tracking for Cramerie (free tier) users

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `user_id` | uuid | NO | - | FK → auth.users.id |
| `quota_month` | date | NO | - | First day of month |
| `attempt_count` | integer | NO | 0 | Regular attempts used |
| `attempt_ai_count` | integer | NO | 0 | AI attempts used |
| `created_at` | timestamptz | NO | now() | - |
| `updated_at` | timestamptz | YES | now() | - |

**Indexes:** `user_quotas_pkey`, `user_quotas_user_id_quota_month_key` (UNIQUE), `idx_user_quotas_user_id`, `idx_user_quotas_month`

---

### `skill_quotas`
> Per-skill monthly quota tracking (prevents grinding one skill)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `user_id` | uuid | NO | - | FK → auth.users.id |
| `skill` | varchar | NO | - | READING, LISTENING, WRITING, SPEAKING |
| `quota_month` | date | NO | - | First day of month |
| `attempt_count` | integer | NO | 0 | Skill attempts used |
| `attempt_ai_count` | integer | NO | 0 | Skill AI attempts used |
| `created_at` | timestamptz | NO | now() | - |
| `updated_at` | timestamptz | YES | now() | - |

**Indexes:** `skill_quotas_pkey`, `skill_quotas_user_id_skill_quota_month_key` (UNIQUE), `idx_skill_quotas_user_id`, `idx_skill_quotas_user_skill`, `idx_skill_quotas_month`, `idx_skill_quotas_full`

---

### `translation_usage`
> Monthly translation usage tracking for Vocabulary Notebook

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `user_id` | uuid | NO | - | FK → auth.users.id |
| `usage_month` | date | NO | - | First day of month |
| `translations_used` | integer | NO | 0 | Translations used |
| `created_at` | timestamptz | NO | now() | - |
| `updated_at` | timestamptz | YES | now() | - |

**Indexes:** `translation_usage_pkey`, `translation_usage_user_id_usage_month_key` (UNIQUE), `idx_translation_usage_user_month`

---

### `chatbot_usage`
> Daily chatbot usage tracking

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `user_id` | uuid | NO | - | FK → auth.users.id |
| `usage_date` | date | NO | CURRENT_DATE | Usage date |
| `messages_used` | integer | YES | 0 | Messages sent |

**Indexes:** `chatbot_usage_pkey`, `chatbot_usage_user_id_usage_date_key` (UNIQUE), `idx_chatbot_usage_user_date`

---

## Admin & Audit Tables

### `user_activities`
> User activity timeline for admin dashboard

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `user_id` | uuid | NO | - | FK → auth.users.id |
| `activity_type` | varchar | NO | - | Type: TEST_COMPLETED, VOCAB_SAVED, SUBSCRIPTION_CHANGED, etc. |
| `title` | varchar | NO | - | Activity title |
| `description` | text | YES | - | Description |
| `metadata` | jsonb | YES | - | JSON data (test_id, score, skill, etc.) |
| `created_at` | timestamptz | NO | now() | - |

**Indexes:** `user_activities_pkey`, `idx_user_activities_user_id`, `idx_user_activities_type`, `idx_user_activities_created_at`

---

### `admin_audit_log`
> Audit trail for all admin actions

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | bigint | NO | IDENTITY ALWAYS | Primary key |
| `admin_user_id` | uuid | NO | - | Admin user ID |
| `admin_email` | varchar | YES | - | Admin email |
| `action` | varchar | NO | - | STATUS_CHANGE, CREDITS_ADD, BAN, etc. |
| `target_type` | varchar | NO | - | USER, SUBSCRIPTION, CREDITS, CONTENT |
| `target_id` | varchar | NO | - | Target entity ID |
| `old_value` | jsonb | YES | - | Previous value |
| `new_value` | jsonb | YES | - | New value |
| `description` | text | YES | - | Description |
| `ip_address` | varchar | YES | - | Client IP |
| `user_agent` | text | YES | - | User agent |
| `created_at` | timestamptz | NO | now() | - |

**Indexes:** `admin_audit_log_pkey`, `idx_admin_audit_log_admin_id`, `idx_admin_audit_log_action`, `idx_admin_audit_log_target`, `idx_admin_audit_log_created_at`

---

## AI & Templates Tables

### `abts_templates`
> AI-Based Test Studio templates for test generation

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | uuid | NO | gen_random_uuid() | Primary key |
| `category` | varchar | NO | - | Category code |
| `category_label` | varchar | NO | - | Category display name |
| `category_icon` | varchar | YES | - | Category emoji |
| `topic` | varchar | NO | - | Topic name |
| `description` | text | YES | - | Description |
| `hashtags` | text[] | YES | '{}' | Associated hashtags |
| `facts` | text[] | YES | '{}' | Facts for generation |
| `skill` | varchar | NO | 'reading' | reading, listening, writing |
| `difficulty` | varchar | YES | 'INTERMEDIATE' | Difficulty level |
| `test_type` | varchar | YES | 'ACADEMIC' | ACADEMIC, GENERAL |
| `suggested_question_types` | text | YES | - | Recommended question types |
| `use_count` | integer | YES | 0 | Usage count |
| `last_used_at` | timestamptz | YES | - | Last usage |
| `is_active` | boolean | YES | true | Active status |
| `is_featured` | boolean | YES | false | Featured flag |
| `created_at` | timestamptz | YES | now() | - |
| `updated_at` | timestamptz | YES | now() | - |

**Indexes:** `abts_templates_pkey`, `idx_abts_templates_category`, `idx_abts_templates_skill`, `idx_abts_templates_active`, `idx_abts_templates_featured`

---

## Foreign Key Relationships

```
test_sets
    └── tests (set_id → test_sets.id)
            ├── test_hashtags (test_id → tests.id)
            │       └── hashtags (hashtag_id → hashtags.id)
            └── sections (test_id → tests.id)
                    └── questions (section_id → sections.id)
                            └── user_answers (question_id → questions.id)

auth.users
    ├── profiles (id → auth.users.id)
    │       └── payment_orders (user_id → profiles.id)
    ├── target (user_id → auth.users.id)
    ├── test_attempts (user_id → auth.users.id)
    │       ├── user_answers (attempt_id → test_attempts.id)
    │       ├── writing_submissions (attempt_id → test_attempts.id)
    │       └── vocabulary (source_test_id → test_attempts.id)
    ├── vocabulary (user_id → auth.users.id)
    ├── user_subscriptions (user_id → auth.users.id)
    │       └── subscription_tiers (tier_id → subscription_tiers.id)
    ├── user_credits (user_id → auth.users.id)
    ├── credit_transactions (user_id → auth.users.id)
    ├── chatbot_usage (user_id → auth.users.id)
    ├── chat_messages (user_id → auth.users.id)
    ├── user_quotas (user_id → auth.users.id)
    ├── skill_quotas (user_id → auth.users.id)
    ├── translation_usage (user_id → auth.users.id)
    └── user_activities (user_id → auth.users.id)
```

---

## Row Level Security (RLS) Policies

### Tables with RLS Enabled (22)

| Table | Policies |
|-------|----------|
| `profiles` | Users can manage own profile; Service role full access |
| `target` | Users can manage own targets; Service role full access |
| `test_attempts` | Users can manage own attempts; Service role full access + DELETE |
| `user_answers` | Users can manage answers for own attempts; Service role full access + DELETE |
| `writing_submissions` | Users can manage own submissions; Service role full access |
| `vocabulary` | Users CRUD own vocabulary; Service role full access |
| `user_subscriptions` | Users can view own; Service role full access |
| `user_credits` | Users can view own; Service role full access |
| `credit_transactions` | Users can view own; Service role full access |
| `chatbot_usage` | Users can view own; Service role full access |
| `chat_messages` | Users can view own; Service role full access |
| `user_quotas` | Users can view own; Service role full access |
| `skill_quotas` | Users can view own; Service role full access |
| `translation_usage` | Users can view own; Service role full access |
| `user_activities` | Users & Admins can view; Service role full access |
| `admin_audit_log` | Admins can view all; Service role full access |
| `payment_orders` | Users can manage own PENDING; Service role full access |
| `lua_packs` | Public read active; Service role full access |
| `test_sets` | Public read published; Service role full access |
| `tests` | Public read published; Service role full access |
| `hashtags` | Public read active; Service role full access |
| `test_hashtags` | Public read; Service role full access |
| `abts_templates` | Public read active; Admins insert/update |

### Tables without RLS (4)

| Table | Reason |
|-------|--------|
| `sections` | Public content, no user-specific data |
| `questions` | Public content, no user-specific data |
| `subscription_tiers` | Public reference data |

---

## Database Triggers

| Trigger | Table | Event | Function |
|---------|-------|-------|----------|
| `on_profile_created_init_credits` | profiles | INSERT | `initialize_user_credits()` |
| `set_timestamp` | target | UPDATE | `trigger_set_timestamp()` |
| `trigger_abts_templates_updated_at` | abts_templates | UPDATE | `update_abts_templates_updated_at()` |
| `trigger_skill_quotas_updated_at` | skill_quotas | UPDATE | `update_skill_quotas_updated_at()` |
| `trigger_test_sets_updated_at` | test_sets | UPDATE | `update_test_sets_updated_at()` |
| `update_test_sets_updated_at` | test_sets | UPDATE | `update_updated_at_column()` |
| `trigger_tests_updated_at` | tests | UPDATE | `update_tests_updated_at()` |
| `update_tests_updated_at` | tests | UPDATE | `update_updated_at_column()` |
| `trigger_user_quotas_updated_at` | user_quotas | UPDATE | `update_user_quotas_updated_at()` |

---

## Indexes

### Total Indexes: 100+

Key indexes by category:

**Primary Keys:** All tables have `{table}_pkey` indexes

**Unique Constraints:**
- `profiles_username_key`
- `questions_question_uid_key`
- `test_sets_code_key`
- `hashtags_code_key`
- `lua_packs_code_key`
- `subscription_tiers_code_key`
- `user_credits_user_id_key`
- `target_user_id_key`
- `payment_orders_order_code_key`
- `sections_unique_exam_test_skill_part`
- `tests_set_id_test_number_key`
- Composite unique indexes on quota tables

**Performance Indexes:**
- `idx_*_user_id` on all user-scoped tables
- `idx_*_status` on status columns
- `idx_*_created_at` for time-based queries
- Partial indexes for published/active content

---

## Migration History

**Total Migrations:** 39

| Version | Name | Description |
|---------|------|-------------|
| 20251210014215 | enable_rls_on_profiles | Enable RLS on profiles table |
| 20251210014216 | enable_rls_on_target | Enable RLS on target table |
| 20251210014415 | add_service_role_policies | Add service role policies |
| 20251212160355 | 004_add_image_description_column | Add image_description for AI grading |
| 20251213065028 | 005_vocabulary_table | Create vocabulary notebook |
| 20251213065106 | 006_subscription_and_credits | Subscription tiers and credits |
| 20251213065143 | 007_achievements_and_chat | Chat messages table |
| 20251213065215 | 008_user_initialization_triggers | User credit initialization |
| 20251213083301 | payos_payment_integration | PayOS payment orders |
| 20251213085704 | add_initial_lua_column | Initial Lúa for tiers |
| 20251214045941 | restructure_subscription_system | Quota-based subscription |
| 20251214051541 | add_attempt_tracking_columns | Attempt usage tracking |
| 20251214065713 | fix_cramerie_attempt_limits | Fix free tier limits |
| 20251214072921 | add_ai_grading_enabled | AI grading toggle |
| 20251215062812 | migrate_ai_grading_to_attempt_ais | Migrate grading column |
| 20251216045457 | add_is_admin_column | Add admin flag |
| 20251216053933 | remove_is_admin_column | Remove (moved) |
| 20251217071201 | add_account_status_and_last_login | Account status tracking |
| 20251219055613 | add_admin_rls_policies | Admin access policies |
| 20251219110431 | add_status_to_sections | Section status column |
| 20251226030119 | drop_unused_tables | Cleanup old tables |
| 20251226032322 | create_test_hierarchy_tables | New test_sets, tests structure |
| 20251226032403 | seed_test_hierarchy_data | Initial test data |
| 20251226034409 | create_test_hierarchy_tables | (duplicate) |
| 20251226034457 | seed_test_hierarchy_data | (duplicate) |
| 20251226055051 | sync_test_sets_and_tests | Sync existing data |
| 20251226055105 | link_sections_to_tests | Link sections to tests |
| 20251226080123 | change_questions_explanation_to_text | Explanation type change |
| 20251226082049 | add_cascade_delete_to_test_fkeys | Cascade delete FKs |
| 20251229072639 | merge_test_sets_name_columns | Merge name fields |
| 20251229074113 | merge_name_fields_all_tables | Merge all name fields |
| 20260101103515 | optimize_questions_column_types_v2 | Column type optimization |
| 20260101104246 | remove_is_system_from_test_sets | Remove is_system column |
| 20260103103923 | convert_explanation_to_jsonb | Convert explanation to JSONB |
| 20260103104456 | fix_explanation_remove_redundant_dapan | Remove redundant fields |
| 20260103105932 | cleanup_redundant_tables_and_columns | Cleanup |
| 20260103123837 | rename_explanation_keys_to_english | Rename keys to English |
| 20260105085454 | fix_matching_options_format | Fix matching format |

---

## Entity Relationship Diagram

```
┌──────────────────┐
│    test_sets     │
│  (Collections)   │
└────────┬─────────┘
         │ 1:N
         ▼
┌──────────────────┐       ┌──────────────────┐
│      tests       │◄─────►│  test_hashtags   │◄────►│   hashtags    │
│   (IELTS tests)  │  N:M  │   (junction)     │ N:1  │   (tags)      │
└────────┬─────────┘       └──────────────────┘      └───────────────┘
         │ 1:N
         ▼
┌──────────────────┐
│     sections     │
│ (Parts/Passages) │
└────────┬─────────┘
         │ 1:N
         ▼
┌──────────────────┐       ┌──────────────────┐
│    questions     │◄──────│   user_answers   │
│                  │  1:N  │                  │
└──────────────────┘       └────────▲─────────┘
                                    │ N:1
                           ┌────────┴─────────┐
                           │   test_attempts  │
                           │                  │
                           └────────┬─────────┘
                                    │ 1:N
         ┌──────────────────────────┼──────────────────────────┐
         │                          │                          │
         ▼                          ▼                          ▼
┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│writing_submissions│      │    vocabulary    │       │  chat_messages   │
│                  │       │                  │       │                  │
└──────────────────┘       └──────────────────┘       └──────────────────┘

┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│     profiles     │       │subscription_tiers│◄──────│user_subscriptions│
│                  │       │                  │  N:1  │                  │
└────────┬─────────┘       └──────────────────┘       └──────────────────┘
         │
         ├───────► user_credits
         ├───────► credit_transactions
         ├───────► payment_orders
         ├───────► user_quotas
         ├───────► skill_quotas
         ├───────► translation_usage
         ├───────► chatbot_usage
         └───────► user_activities
```

---

## Summary

| Metric | Value |
|--------|-------|
| **Total Tables** | 26 |
| **Tables with RLS** | 22 (85%) |
| **Tables without RLS** | 4 (public content) |
| **Total Migrations** | 39 |
| **Total Indexes** | 100+ |
| **Database Triggers** | 9 |
| **Installed Extensions** | 6 |
| **JSONB Columns** | 12 (across 8 tables) |

---

*Documentation auto-generated from Supabase MCP tools*
