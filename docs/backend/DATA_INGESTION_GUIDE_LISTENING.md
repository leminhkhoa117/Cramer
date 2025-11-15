# AI Agent Guide: Generating SQL for IELTS Listening Test Ingestion

## 1. Objective

This document provides a comprehensive guide for an AI agent to parse an IELTS Listening Test from a source (like a PDF and its accompanying audio) and generate the corresponding SQL `INSERT` statements. The goal is to accurately populate the PostgreSQL database (hosted on Supabase) used by the Cramer application with new test materials.

**Strict adherence to the JSON structures and `QuestionType` enums outlined below is critical for the application to function correctly.**

### Database Connection Details
- **Database:** PostgreSQL on Supabase
- **Connection:** Configured via environment variables in `.env` file
- **JDBC URL Format:** `jdbc:postgresql://db.xxx.supabase.co:6543/postgres?sslmode=require&prepareThreshold=0`
- **Important:** The `prepareThreshold=0` parameter is required to prevent prepared statement cache conflicts with HikariCP connection pooling.

## 2. Core Database Schema

The test data is primarily stored in two tables: `sections` and `questions`.

### `sections` Table
This table stores the audio and visual context for each part of a Listening test.

| Column                | Type      | Description                                                                                             |
|-----------------------|-----------|---------------------------------------------------------------------------------------------------------|
| `id`                  | `bigint`  | Primary Key. Auto-incrementing unique identifier for the section.                                       |
| `exam_source`         | `varchar` | The source of the exam, e.g., `cam17` for Cambridge 17.                                                |
| `test_number`         | `varchar` | The test number within the source, e.g., `'1'`, `'2'` (stored as string).                              |
| `skill`               | `varchar` | The skill being tested. For Listening, this will always be `listening`.                                 |
| `part_number`         | `integer` | The part number within the test, i.e., `1`, `2`, `3`, or `4` for a Listening test.                       |
| `audio_url`           | `varchar` | **Required.** The URL for the part's audio file.                                                        |
| `transcript`          | `text`    | The full transcript of the audio. Used for review purposes.                                             |
| `display_content_url` | `varchar` | Optional URL for an image, table, or form to be displayed on the left panel during the test.            |

### `questions` Table
This table stores every individual question for all sections. The schema is identical to the one used for Reading questions.

| Column             | Type          | Description                                                                                             |
|--------------------|---------------|---------------------------------------------------------------------------------------------------------|
| `id`               | `bigint`      | Primary Key. Auto-incrementing unique identifier for the question.                                      |
| `section_id`       | `bigint`      | **Foreign Key** referencing `sections.id`. Links the question to its audio section.                     |
| `question_number`  | `integer`     | The overall question number in the test (1-40).                                                         |
| `question_uid`     | `varchar`     | Unique identifier following pattern: `{exam_source}-t{test_number}-l-q{question_number}`.               |
| `question_type`    | `varchar`     | An enum string representing the type of question. See Section 3 for the definitive list.                |
| `question_content` | `jsonb`       | The specific text and options for the question. The structure varies by `question_type`. See Section 4. |
| `correct_answer`   | `jsonb`       | The correct answer(s) for the question. The structure varies by `question_type`. See Section 4.         |
| `explanation`      | `text`        | Optional explanation for the answer (nullable, currently unused).                                        |
| `word_limit`       | `varchar`     | Optional constraint on answer length, e.g., "ONE WORD AND/OR A NUMBER".                                 |
| `image_url`        | `varchar`     | Optional URL for an image associated with the question (e.g., for diagram questions).                    |

---

## 3. The `QuestionType` Enum for Listening

The `question_type` column **must** be one of the following string values. While many overlap with Reading, their application in Listening is specific to audio cues.

- `FILL_IN_BLANK`
- `NOTE_COMPLETION`
- `FORM_COMPLETION`
- `TABLE_COMPLETION`
- `SENTENCE_COMPLETION`
- `MULTIPLE_CHOICE`
- `MULTIPLE_CHOICE_MULTIPLE_ANSWERS`
- `MATCHING_FEATURES`
- `PLAN_MAP_DIAGRAM_LABELING`

---

## 4. Detailed JSON Structures

This section details the required `jsonb` format for `question_content` and `correct_answer` for each `QuestionType`.

### `NOTE_COMPLETION` / `FORM_COMPLETION` / `FILL_IN_BLANK`
- **Description:** User types a word/phrase from the audio into a text box. These are often presented as notes, forms, or sentences with gaps. The `word_limit` (e.g., "ONE WORD AND/OR A NUMBER") is critical.
- **`question_content`**:
  ```json
  {
    "text": "Making sure the beach does not have 1 ____ on it"
  }
  ```
- **`correct_answer`**: An array of acceptable string answers. Case-insensitive matching is performed by the backend.
  ```json
  ["litter"]
  ```

### `MULTIPLE_CHOICE`
- **Description:** User selects one option from a list based on the audio.
- **`question_content`**:
  ```json
  {
    "text": "What is the maximum number of people who can stand on each side of the boat?",
    "options": [
      "A 9",
      "B 15",
      "C 18"
    ]
  }
  ```
- **`correct_answer`**: An array containing the single correct letter.
  ```json
  ["A"]
  ```

### `MULTIPLE_CHOICE_MULTIPLE_ANSWERS`
- **Description:** User selects two or more options from a list.
- **`question_content`**:
  ```json
  {
    "text": "Which TWO features of the lighthouse does Lou mention?",
    "options": [
      "A why it was built",
      "B who built it",
      "C how long it took to build",
      "D who staffed it",
      "E what it was built with"
    ]
  }
  ```
- **`correct_answer`**: An array containing all correct letters.
  ```json
  ["A", "D"]
  ```

### `MATCHING_FEATURES`
- **Description:** User matches a set of questions (e.g., Items 27-30) to a list of options (e.g., Opinions A-F).
- **Special Note:** The `options` array should be identical for every question in the group. The `text` field will contain the specific item for that question.
- **`question_content`**:
  ```json
  {
    "text": "Medical terminology",
    "options": [
      { "letter": "A", "text": "Tim found this easier than expected." },
      { "letter": "B", "text": "Tim thought this was not very clearly organised." },
      { "letter": "C", "text": "Diana may do some further study on this." }
    ]
  }
  ```
- **`correct_answer`**: An array containing the single correct letter.
  ```json
  ["A"]
  ```

### `PLAN_MAP_DIAGRAM_LABELING`
- **Description:** User labels parts of a map, plan, or diagram based on audio directions.
- **Special Note:** The `image_url` field on the question record **must** be populated with the URL of the map/diagram image.
- **`question_content`**: The `text` field contains the label for the question, which corresponds to a number on the visual.
  ```json
  {
    "text": "15. ____"
  }
  ```
- **`correct_answer`**: An array containing the string for the label.
  ```json
  ["dairy"]
  ```

---

## 5. Listening Test Flow & UI Differences

The user experience for a Listening test has several key differences from a Reading test.

- **Structure:** The test is divided into **4 parts**, each with 10 questions (40 total).
- **Audio Player:**
    - A single audio player is displayed for each part, typically positioned below the main header.
    - The audio for each part is played **once only**.
    - The player should support two modes: **auto-play** (default, starts automatically) and **manual-play**.
- **Layout:**
    - The right panel contains the questions for the current part.
    - The left panel is used to display any visual materials relevant to the questions, such as maps, diagrams, forms, or tables. This content is loaded via the `display_content_url` in the `sections` table. If no visual is needed, this panel may be empty.
- **Timer:** There is **no timer** visible during the main 40-question test phase.
- **Final Review:** After the audio for Part 4 concludes, a **2-minute timer** starts, allowing the user to review and complete their answers across all 40 questions before final submission.

---

## 6. Step-by-Step Workflow & SQL Generation

**Objective:** To process one full IELTS Listening Test (e.g., Cambridge 17, Test 1) and generate all necessary SQL.

**Step 1: Add a Deletion Clause**
To prevent duplicate data, start by deleting existing test data for the target exam.
```sql
-- Inside your DO $$ block, before inserting
DELETE FROM public.questions WHERE section_id IN (SELECT id FROM public.sections WHERE exam_source = 'cam17' AND test_number = '1' AND skill = 'listening');
DELETE FROM public.sections WHERE exam_source = 'cam17' AND test_number = '1' AND skill = 'listening';
```

**Step 2: Generate `INSERT` for `sections` Table**
- For each part (1-4), generate an `INSERT` statement.
- Provide the `audio_url` and the full `transcript`.
- Use `RETURNING id` to capture the ID for each section.

**Example for Part 1:**
```sql
-- Declare variables for each section ID
DO $$
DECLARE
    section_1_id bigint;
    section_2_id bigint;
    section_3_id bigint;
    section_4_id bigint;
BEGIN
    -- (Deletion clause from Step 1 here)

    -- Insert Section 1 and capture its ID
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, transcript)
    VALUES (
        'cam17',
        '1',
        'listening',
        1,
        'https://example.com/audio/cam17_t1_p1.mp3',
        '...'
    )
    RETURNING id INTO section_1_id;

    -- Insert all questions for Part 1 using section_1_id
    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit) VALUES
    (section_1_id, 1, 'cam17-t1-l-q1', 'NOTE_COMPLETION', '{"text": "making sure the beach does not have 1 ____ on it"}', '["litter"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 2, 'cam17-t1-l-q2', 'NOTE_COMPLETION', '{"text": "no 2 ____"}', '["dogs"]', 'ONE WORD AND/OR A NUMBER');
    -- ... more questions for part 1

    -- Repeat for Part 2, 3, and 4, capturing and using their respective IDs
    -- ...

END $$;
```

**Step 3: Generate `INSERT` for `questions` Table**
- For every question associated with a section, generate an `INSERT` statement.
- Use the correct `section_id` variable for the `section_id` column.
- Meticulously follow the `QuestionType` enums and JSON structures.

---

## 7. Common Pitfalls & Solutions

- **Audio/Visual Sync:** Ensure the `audio_url` and any `display_content_url` are correct for the given section.
- **Question Numbering:** Maintain continuous numbering from 1 to 40 across all four parts.
- **`word_limit`:** This is especially important in Listening fill-in-the-blank tasks. Ensure it matches the instructions (e.g., "ONE WORD ONLY", "NO MORE THAN TWO WORDS AND/OR A NUMBER").
- **JSON Formatting:** Double-check that all `question_content` and `correct_answer` JSON is valid and correctly structured for the given `question_type`.
