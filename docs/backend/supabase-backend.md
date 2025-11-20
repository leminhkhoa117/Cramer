# Cramer Project - Backend Documentation (Supabase)

## 1. Project Overview

**Cramer** is a web application designed for users to practice IELTS exam questions, specifically focusing initially on the Reading skill based on Cambridge IELTS materials. This document details the backend setup using Supabase.

## 2. Technology Stack

* **Backend as a Service (BaaS):** Supabase
* **Database:** PostgreSQL (managed by Supabase)
* **Authentication:** Supabase Auth

## 3. Authentication

User authentication and management are handled entirely by **Supabase Auth**.

* **User Table:** Supabase automatically manages users in the `auth.users` table (schema: `auth`). This table stores sensitive information like `id` (UUID), `email`, `encrypted_password`, etc., and is **not directly accessible** via public APIs for security.
* **Profile Creation Trigger:** A PostgreSQL trigger (`on_auth_user_created`) and function (`public.handle_new_user`) have been set up.
    * **Function:** `public.handle_new_user()` inserts a new row into the `public.profiles` table upon new user registration.
    * **Trigger:** `on_auth_user_created` fires `AFTER INSERT ON auth.users` for each new user, executing the `handle_new_user()` function.
    * **Default Username:** The trigger automatically sets the initial `username` in the `profiles` table to the part of the user's email address before the "@" symbol. This can be updated later by the user.

## 4. Database Schema (Schema: `public`)

The database consists of four main tables in the `public` schema.

---

### 4.1. `profiles` Table

* **Purpose:** Stores public-facing user information, linked to the `auth.users` table.
* **Columns:**
    * `id` (`uuid`, **Primary Key**, **Foreign Key** -> `auth.users.id`): Unique user identifier, mirrors the Supabase Auth user ID.
    * `username` (`text`, **Unique**): The user's chosen display name or account ID. Must be unique across all users.
    * `created_at` (`timestamptz`, default: `now()`): Timestamp when the profile was created.

---

### 4.2. `sections` Table

* **Purpose:** Stores the content and metadata for distinct sections of an IELTS test (e.g., a Reading Passage, a Listening Part).
* **Columns:**
    * `id` (`int8`/`serial`, **Primary Key**): Auto-incrementing unique identifier for the section.
    * `exam_source` (`text`): Identifier for the source material (e.g., `"cam17"`, `"cam18"`).
    * `test_number` (`text`): The test number within the source material (e.g., `"1"`, `"2"`, `"3"`, `"4"`). Stored as text, but always a simple numeral string.
    * `skill` (`text`): The IELTS skill being tested (e.g., `"reading"`, `"listening"`).
    * `part_number` (`int4`/`integer`): The part number within the skill test (e.g., 1, 2, 3 for Reading; 1, 2, 3, 4 for Listening).
    * `display_content_url` (`text`, nullable): Optional URL to an image or PDF displaying the original section content. Primarily relevant for older Reading content.
    * `passage_text` (`text`, nullable): Full text content. For Reading, this is the HTML-formatted passage. For Listening, this is the full audio transcript (used for review only).
    * `audio_url` (`text`, nullable): For Listening, the URL to the audio file for this part. `NULL` for Reading sections.
    * `section_layout` (`jsonb`, nullable): For Listening, a JSON object describing the visual layout in terms of "question blocks". For legacy Reading sections this is currently `NULL` and not used.
    * `created_at` (`timestamptz`, default: `now()`): Timestamp when the section was added (Default Supabase column).

**Reading vs Listening models**

- **Reading:** Uses a legacy model. The frontend renders from `passage_text` and per-question JSON only; `section_layout` is not used.
- **Listening:** Uses the newer "question block" model. The frontend relies on `audio_url` and `section_layout` to know how to display each group of questions (e.g., note completion, matching, instructions).

---

### 4.3. `questions` Table

* **Purpose:** Stores individual questions belonging to a specific section. This is the core table for practice content.
* **Columns:**
    * `id` (`int8`/`serial`, **Primary Key**): Auto-incrementing unique identifier for the question.
        * *Note:* The sequence `public.questions_id_seq` has been manually reset to ensure contiguous numbering after initial insertions.
    * `section_id` (`int4`/`integer`, **Foreign Key** -> `public.sections.id`): Links the question to its parent section.
    * `question_number` (`int4`/`integer`): The Cambridge question number (1–40) within the test. For Listening this is global across all four parts; for Reading it is global across the three passages.
    * `question_uid` (`text`, **Should be Unique**): A unique string identifier for the question across the entire system (e.g., `"cam17-t1-r-q1"` for Reading, `"cam17-t1-l-q1"` for Listening). A unique constraint should be added later.
    * `question_type` (`text`): Defines how the question should be rendered and validated on the frontend. Examples (non‑exhaustive):
        * `FILL_IN_BLANK`
        * `SUMMARY_COMPLETION`
        * `SUMMARY_COMPLETION_OPTIONS`
        * `TRUE_FALSE_NOT_GIVEN`
        * `YES_NO_NOT_GIVEN`
        * `MATCHING_INFORMATION`
        * `MATCHING_HEADINGS`
        * `MATCHING_FEATURES`
        * `MATCHING_SENTENCE_ENDINGS`
        * `MULTIPLE_CHOICE`
        * `MULTIPLE_CHOICE_MULTIPLE_ANSWERS`
        * `MATCHING` (generic dropdown-based matching, primarily for Listening)
        * `DIAGRAM_LABEL_COMPLETION`
    * `question_content` (`jsonb`): Stores the question text and any associated options (like multiple-choice answers, matching lists) in a structured JSON format. The exact shape depends on `question_type` and, for Listening, the block type in `section_layout`.
        * *Example (Fill in blank):* `{"text": "The 1 ____ of London..."}`
        * *Example (Multiple Choice):* `{"text": "Why does X happen?", "options": ["A...", "B...", "C...", "D..."]}`
    * `correct_answer` (`jsonb`): Stores the correct answer(s). Using `jsonb` allows flexibility for single answers, multiple answers (e.g., Choose TWO), or ordered answers if needed.
        * *Example (Single):* `["population"]` or `["B"]`
        * *Example (Multiple):* `["C", "D"]`
    * `word_limit` (`text`, nullable): Optional constraint string for completion questions, e.g., `"ONE WORD ONLY"`, `"ONE WORD AND/OR A NUMBER"`. Empty for question types that do not use it.
    * `image_url` (`text`, nullable): Optional URL to an image required for rendering the question (e.g., diagram label completion in Reading).
    * `created_at` (`timestamptz`, default: `now()`): Timestamp when the question was added (Default Supabase column).

---

### 4.4. `user_answers` Table

* **Purpose:** Records each attempt a user makes on a specific question. Tracks user progress and performance.
* **Columns:**
    * `id` (`int8`/`serial`, **Primary Key**): Auto-incrementing unique identifier for the answer attempt.
    * `user_id` (`uuid`, **Foreign Key** -> `auth.users.id`): Identifies the user who submitted the answer. Links to the authenticated user.
    * `question_id` (`int4`/`integer`, **Foreign Key** -> `public.questions.id`): Identifies the question being answered.
    * `user_answer` (`text`): The actual answer provided by the user.
    * `submitted_at` (`timestamptz`, default: `now()`): Timestamp when the answer was submitted.
    * `is_correct` (`bool`, nullable): Boolean flag indicating if the `user_answer` matched the `correct_answer` for the `question_id` at the time of submission. Can be updated by a backend process or function after submission.
    * `created_at` (`timestamptz`, default: `now()`): Timestamp when the answer record was created (Default Supabase column).

---

## 5. Key Concepts

* **`jsonb`:** A binary JSON data type in PostgreSQL. Allows storing structured data within a single column, offering flexibility for varied question types and answers. It is efficient for querying.
* **`uuid`:** Universally Unique Identifier used by Supabase Auth for user IDs. Ensures unique identification across systems.
* **Foreign Keys:** Enforce referential integrity between tables (e.g., an answer must belong to a real user and a real question).
* **Triggers:** Database procedures automatically executed in response to specific events (like inserting a new user).

## 6. Setup & Important Notes

* **Row Level Security (RLS):** RLS is **currently disabled** on all tables in the `public` schema (`profiles`, `sections`, `questions`, `user_answers`) for ease of development. **RLS must be enabled and appropriate policies must be written** before launching the application to ensure data security (e.g., users can only see their own answers, users cannot modify questions).
* **Profile Trigger:** The `handle_new_user` function and `on_auth_user_created` trigger are essential for linking `auth.users` with `public.profiles`. Do not delete them without understanding the implications.
* **API Keys:** Use the `anon` key (public key) from Supabase Project Settings -> API for client-side (frontend) requests. The `service_role` key should **never** be exposed on the client-side.

## 7. Data Insertion

* **Initial Data:** Exam content (sections and questions) is currently being inserted via the Supabase Table Editor or SQL Editor using `INSERT` statements.
* **User Data:** User profiles are created automatically via the trigger. User answers are inserted by the application logic when a user submits an answer.
