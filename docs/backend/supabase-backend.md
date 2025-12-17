# Cramer Project - Backend Documentation (Supabase)

> **Last Updated:** November 30, 2025

## 1. Project Overview

**Cramer** is a web application designed for users to practice IELTS exam questions, supporting both **Reading** and **Listening** skills based on Cambridge IELTS materials. The system uses a **hybrid architecture**:
- **Supabase** for database (PostgreSQL) and authentication
- **Spring Boot** backend API for business logic and data access

This document details the Supabase database schema and setup.

## 2. Technology Stack

| Component | Technology |
|-----------|------------|
| Database | PostgreSQL (managed by Supabase) |
| Authentication | Supabase Auth (JWT tokens) |
| Backend API | Spring Boot (Java 21, Maven) |
| API Gateway | REST via Spring Boot (`/api/**` endpoints) |

**Project URL:** `https://jpocdgkrvohmjkejclpl.supabase.co`

## 3. Authentication

User authentication is handled by **Supabase Auth**, with the Spring Boot backend validating JWT tokens.

* **User Table:** Supabase manages users in the `auth.users` table (schema: `auth`). This table stores sensitive information like `id` (UUID), `email`, `encrypted_password`, etc.
* **JWT Validation:** The Spring Boot backend uses `JwtAuthFilter` to validate Supabase-issued tokens on all `/api/**` routes.
* **Profile Creation Trigger:** A PostgreSQL trigger (`on_auth_user_created`) and function (`public.handle_new_user`) automatically create a profile when a new user registers.
    * **Function:** `public.handle_new_user()` — Creates a row in `public.profiles` with the user's ID and a default username (email prefix).
    * **Trigger:** `on_auth_user_created` — Fires `AFTER INSERT ON auth.users`.
    * **Security:** Function uses `SECURITY DEFINER` with `search_path = public` to prevent search_path injection attacks.

## 4. Database Schema (Schema: `public`)

The database consists of **six tables** in the `public` schema.

### Current Data Statistics

| Table | Rows | RLS Enabled |
|-------|------|-------------|
| `profiles` | 3 | ❌ No |
| `sections` | 16 | ❌ No |
| `questions` | 200 | ❌ No |
| `target` | 2 | ❌ No |
| `test_attempts` | 14 | ✅ Yes |
| `user_answers` | 9 | ✅ Yes |

---

### 4.1. `profiles` Table

* **Purpose:** Stores public-facing user information, linked to the `auth.users` table.
* **Columns:**

| Column | Type | Nullable | Default | Constraints |
|--------|------|----------|---------|-------------|
| `id` | `uuid` | NO | `gen_random_uuid()` | **PRIMARY KEY** |
| `username` | `varchar(255)` | NO | `''` | **UNIQUE** (`profiles_username_key`) |
| `full_name` | `varchar(255)` | YES | | |
| `phone_number` | `varchar(255)` | YES | | |
| `address` | `varchar(255)` | YES | | |
| `avatar_url` | `varchar(255)` | YES | | |
| `hero_background_url` | `varchar(255)` | YES | | |
| `page_background_url` | `varchar(255)` | YES | | |
| `llm_api_key` | `varchar(255)` | YES | | User's DeepSeek/LLM API key |
| `llm_model` | `varchar(255)` | YES | | Selected AI model (deepseek-chat, deepseek-reasoner) |
| `llm_provider` | `varchar(255)` | YES | `'deepseek'` | LLM provider identifier |
| `created_at` | `timestamptz` | NO | `now()` | |

---

### 4.2. `sections` Table

* **Purpose:** Stores content and metadata for IELTS test sections (Reading passages, Listening parts).
* **Columns:**

| Column | Type | Nullable | Constraints |
|--------|------|----------|-------------|
| `id` | `bigint` | NO | **PRIMARY KEY** (identity) |
| `exam_source` | `varchar(255)` | YES | Part of **UNIQUE** composite |
| `test_number` | `integer` | YES | Part of **UNIQUE** composite |
| `skill` | `varchar(255)` | YES | Part of **UNIQUE** composite |
| `part_number` | `integer` | YES | Part of **UNIQUE** composite |
| `display_content_url` | `varchar(255)` | YES | |
| `passage_text` | `text` | YES | |
| `audio_url` | `varchar(255)` | YES | |
| `section_layout` | `jsonb` | YES | |
| `image_description` | `text` | YES | Detailed description for Writing Task 1 charts/maps (AI grading) |

* **Indexes:**
    - `sections_pkey` — Primary key on `id`
    - `sections_unique_exam_test_skill_part` — Unique composite on `(exam_source, test_number, skill, part_number)`
    - `sections_exam_test_skill_idx` — B-tree index for lookups

**Current Data Coverage:**
- Cambridge 17 Tests 1–4, Reading (parts 1–3 each)
- Cambridge 17 Test 1, Listening (parts 1–4)

**Reading vs Listening Models:**

| Aspect | Reading | Listening |
|--------|---------|-----------|
| Content | `passage_text` (HTML) | `audio_url` + transcript in `passage_text` |
| Layout | Not used (`section_layout` = NULL) | Uses `section_layout` (question blocks) |
| Parts | 3 passages per test | 4 parts per test |

**`section_layout` JSONB Structure (Listening):**
```json
{
  "blocks": [{
    "block_type": "NOTE_COMPLETION",
    "content": {
      "title": "Questions 31-40",
      "main_title": "Labyrinths",
      "instructions_text": "Complete the notes below..."
    },
    "question_numbers": [31, 32, 33, 34, 35, 36, 37, 38, 39, 40]
  }]
}
```

---

### 4.3. `questions` Table

* **Purpose:** Stores individual questions belonging to a section.
* **Columns:**

| Column | Type | Nullable | Constraints |
|--------|------|----------|-------------|
| `id` | `bigint` | NO | **PRIMARY KEY** (identity) |
| `section_id` | `bigint` | YES | **FK** → `sections.id` (`questions_section_fk`) |
| `question_number` | `integer` | YES | |
| `question_uid` | `varchar(255)` | YES | **UNIQUE** (`questions_question_uid_key`) |
| `question_type` | `varchar(255)` | YES | |
| `question_content` | `jsonb` | YES | |
| `correct_answer` | `jsonb` | YES | |
| `explanation` | `varchar(255)` | YES | |
| `word_limit` | `varchar(255)` | YES | |
| `image_url` | `varchar(255)` | YES | |

* **Indexes:**
    - `questions_section_idx` — B-tree index on `section_id` for faster joins

**Question Types (11 types currently in use):**

| Type | Count | Description |
|------|-------|-------------|
| `TRUE_FALSE_NOT_GIVEN` | 33 | T/F/NG statements |
| `SUMMARY_COMPLETION` | 29 | Fill-in-blank summaries |
| `MATCHING_INFORMATION` | 27 | Match statements to paragraphs |
| `FILL_IN_BLANK` | 27 | Simple gap-fill |
| `MULTIPLE_CHOICE` | 25 | Single-answer MCQ |
| `MULTIPLE_CHOICE_MULTIPLE_ANSWERS` | 16 | Multi-answer MCQ |
| `SUMMARY_COMPLETION_OPTIONS` | 14 | Summary with word bank |
| `YES_NO_NOT_GIVEN` | 13 | Y/N/NG opinion statements |
| `TABLE_COMPLETION` | 7 | Fill table cells |
| `MATCHING_FEATURES` | 5 | Match to features/people |
| `MATCHING` | 4 | Generic dropdown matching |

**`question_content` JSONB Examples:**

```json
// FILL_IN_BLANK
{"text": "An undesirable trait such as loss of <strong>24</strong> ____ may be caused by..."}

// MULTIPLE_CHOICE
{
  "text": "What is the reviewer's main purpose?",
  "options": ["A to describe...", "B to give an account...", "C to provide...", "D to compare..."]
}

// MATCHING_FEATURES
{
  "text": "Domestication of certain plants could...",
  "options": [
    {"letter": "A", "text": "Jorg Kudla"},
    {"letter": "B", "text": "Caixia Gao"},
    {"letter": "C", "text": "Joyce Van Eck"},
    {"letter": "D", "text": "Jonathan Jones"}
  ]
}
```

**`correct_answer` JSONB Examples:**
```json
["flavour"]           // Single text answer
["B"]                 // Single letter answer
["C", "D"]            // Multiple answers
```

---

### 4.4. `target` Table

* **Purpose:** Stores user IELTS target scores and exam information.
* **Columns:**

| Column | Type | Nullable | Default | Constraints |
|--------|------|----------|---------|-------------|
| `id` | `uuid` | NO | `gen_random_uuid()` | **PRIMARY KEY** |
| `user_id` | `uuid` | NO | | **FK** → `auth.users.id` |
| `exam_name` | `varchar(255)` | NO | | |
| `exam_date` | `date` | YES | | |
| `listening` | `float8` | YES | | CHECK: 0–9 |
| `reading` | `float8` | YES | | CHECK: 0–9 |
| `writing` | `float8` | YES | | CHECK: 0–9 |
| `speaking` | `float8` | YES | | CHECK: 0–9 |
| `created_at` | `timestamptz` | NO | `now()` | |
| `updated_at` | `timestamptz` | NO | `now()` | Auto-updated via trigger |

* **Triggers:**
    - `set_timestamp` — `BEFORE UPDATE` → calls `trigger_set_timestamp()` to auto-update `updated_at`

---

### 4.5. `test_attempts` Table *(RLS Enabled)*

* **Purpose:** Tracks a user's attempt at a specific test (exam_source + test_number + skill).
* **Columns:**

| Column | Type | Nullable | Default | Constraints |
|--------|------|----------|---------|-------------|
| `id` | `bigint` | NO | identity (ALWAYS) | **PRIMARY KEY** |
| `user_id` | `uuid` | NO | | **FK** → `auth.users.id` |
| `exam_source` | `varchar(255)` | NO | | |
| `test_number` | `varchar(255)` | NO | | |
| `skill` | `varchar(255)` | NO | | |
| `status` | `varchar(255)` | NO | `'IN_PROGRESS'` | |
| `score` | `integer` | YES | | |
| `started_at` | `timestamptz` | NO | `now()` | |
| `completed_at` | `timestamptz` | YES | | |
| `current_part` | `integer` | YES | | |
| `time_left` | `integer` | YES | | Seconds remaining |

**Status Values:** `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

* **RLS Policy:** `Users can manage their own test attempts`
    - Command: `ALL`
    - Condition: `auth.uid() = user_id`

---

### 4.6. `user_answers` Table *(RLS Enabled)*

* **Purpose:** Stores user answers for each question within a test attempt.
* **Columns:**

| Column | Type | Nullable | Constraints |
|--------|------|----------|-------------|
| `id` | `bigint` | NO | **PRIMARY KEY** (identity ALWAYS) |
| `user_id` | `uuid` | NO | |
| `attempt_id` | `bigint` | NO | **FK** → `test_attempts.id`, part of **UNIQUE** composite |
| `question_id` | `bigint` | NO | **FK** → `questions.id`, part of **UNIQUE** composite |
| `answer_content` | `jsonb` | NO | |
| `user_answer` | `varchar(255)` | NO | Plain text version |
| `is_correct` | `boolean` | YES | |
| `submitted_at` | `timestamptz` | NO | `now()` |
| `created_at` | `timestamptz` | NO | |

* **Unique Constraint:** `user_answers_attempt_id_question_id_key` ensures one answer per question per attempt.
* **RLS Policy:** `Users can manage answers for their own attempts`
    - Command: `ALL`
    - Condition: Checks that the `attempt_id` belongs to the current user via subquery

---

## 5. Entity Relationships

```
auth.users (Supabase Auth)
    │
    ├──→ profiles.id (1:1, auto-created via trigger)
    │
    ├──→ target.user_id (1:N)
    │
    └──→ test_attempts.user_id (1:N)
              │
              └──→ user_answers.attempt_id (1:N)
                        │
                        └──→ questions.id ←── sections.id
```

---

## 6. Functions & Triggers

### 6.1. `handle_new_user()` Function

Creates a profile automatically when a new user registers.

```sql
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.profiles (id, username)
  VALUES (new.id, split_part(new.email, '@', 1));
  RETURN new;
END;
$$;
```

**Trigger:** `on_auth_user_created` on `auth.users` (AFTER INSERT)

### 6.2. `trigger_set_timestamp()` Function

Auto-updates `updated_at` column on row updates.

```sql
CREATE OR REPLACE FUNCTION public.trigger_set_timestamp()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;
```

**Trigger:** `set_timestamp` on `target` (BEFORE UPDATE)

---

## 7. Installed Extensions

| Extension | Version | Purpose |
|-----------|---------|---------|
| `pg_graphql` | 1.5.11 | GraphQL support |
| `pgcrypto` | 1.3 | Cryptographic functions |
| `pg_stat_statements` | 1.11 | Query statistics |
| `uuid-ossp` | 1.1 | UUID generation |
| `supabase_vault` | 0.3.1 | Secrets management |
| `plpgsql` | 1.0 | PL/pgSQL procedural language |

---

## 8. Security Notes

### Row Level Security (RLS)

| Table | RLS Status | Policy |
|-------|------------|--------|
| `profiles` | ❌ Disabled | — |
| `sections` | ❌ Disabled | — |
| `questions` | ❌ Disabled | — |
| `target` | ❌ Disabled | — |
| `test_attempts` | ✅ Enabled | Users can manage their own attempts |
| `user_answers` | ✅ Enabled | Users can manage answers for their own attempts |

> **Note:** RLS is partially implemented. Before production launch, enable RLS on `profiles` and `target` tables with appropriate policies.

### Security Advisories

- ⚠️ **Leaked Password Protection** is currently disabled. Consider enabling it in Supabase Auth settings to check passwords against HaveIBeenPwned.org.

### API Keys

- **`anon` key:** Use for client-side (frontend) requests.
- **`service_role` key:** **Never expose on client-side.** Used only by Spring Boot backend.

---

## 9. Backend Integration (Spring Boot)

The Spring Boot backend connects to Supabase PostgreSQL and validates Supabase JWT tokens.

### Key Configuration Files

- `backend/src/main/resources/application.properties` — Database URL, Supabase keys
- `backend/src/main/java/com/cramer/config/JwtAuthFilter.java` — JWT validation
- `backend/src/main/java/com/cramer/config/SecurityConfig.java` — Route protection

### Entity Classes

| Entity | Table | Package |
|--------|-------|---------|
| `Profile` | `profiles` | `com.cramer.entity` |
| `Section` | `sections` | `com.cramer.entity` |
| `Question` | `questions` | `com.cramer.entity` |
| `Target` | `target` | `com.cramer.entity` |
| `TestAttempt` | `test_attempts` | `com.cramer.entity` |
| `UserAnswer` | `user_answers` | `com.cramer.entity` |

### Running the Backend

```powershell
cd backend
./run-app.ps1
```

Swagger UI available at: `http://localhost:8080/swagger-ui.html`

---

## 10. Data Insertion

- **Exam Content:** Inserted via SQL scripts in `docs/backend/*.sql` or Supabase Table Editor.
- **User Profiles:** Created automatically via `handle_new_user` trigger.
- **Test Data:** Created by Spring Boot API when users start/complete tests.
