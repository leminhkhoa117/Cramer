# AI Agent Guide: Generating SQL for IELTS Listening Test Ingestion

## 1. Objective

This document provides a comprehensive guide for an AI agent to parse an IELTS Listening Test from a source (like a PDF and its accompanying audio) and generate the corresponding SQL `INSERT` statements. The goal is to accurately populate the PostgreSQL database (hosted on Supabase) used by the Cramer application with new test materials.

**Strict adherence to the JSON structures and `QuestionType` enums outlined below is critical for the application to function correctly.**

### Database Connection Details
- **Database:** PostgreSQL on Supabase
- **Connection:** Configured via environment variables in `.env` file
- **JDBC URL Format:** `jdbc:postgresql://db.xxx.supabase.co:6543/postgres?sslmode=require&prepareThreshold=0`
- **Important:** The `prepareThreshold=0` parameter is required to prevent prepared statement cache conflicts with HikariCP connection pooling.

## 2. Core Database Schema Redesign: The "Question Block" Model

To accommodate the complex and varied layouts of IELTS Listening tests, the data model has been redesigned around a "Question Block" concept. Instead of a single, optional image per section, each section's layout is now defined by an array of blocks, where each block represents a distinct visual or instructional part of the test.

### `sections` Table
The `display_content_url` column has been **replaced** with `section_layout`.

| Column          | Type    | Description                                                                                             |
|-----------------|---------|---------------------------------------------------------------------------------------------------------|
| `id`            | `bigint`| Primary Key. Auto-incrementing unique identifier for the section.                                       |
| `exam_source`   | `varchar`| The source of the exam, e.g., `cam17` for Cambridge 17.                                                |
| `test_number`   | `varchar`| The test number within the source, e.g., `'1'`, `'2'` (stored as string).                              |
| `skill`         | `varchar`| The skill being tested. For Listening, this will always be `listening`.                                 |
| `part_number`   | `integer`| The part number within the test, i.e., `1`, `2`, `3`, or `4`.                                           |
| `audio_url`     | `varchar`| **Required.** The URL for the part's audio file.                                                        |
| `passage_text`    | `text`  | The full transcript of the audio. Used for review purposes.                                             |
| `section_layout`| `jsonb` | **New.** A JSON object defining the layout as a series of "Question Blocks". See Section 4 for details. |

### `questions` Table
The structure of `question_content` is now dependent on the `block_type` of the block it belongs to.

| Column             | Type          | Description                                                                                             |
|--------------------|---------------|---------------------------------------------------------------------------------------------------------|
| `id`               | `bigint`      | Primary Key. Auto-incrementing unique identifier for the question.                                      |
| `section_id`       | `bigint`      | **Foreign Key** referencing `sections.id`.                                                              |
| `question_number`  | `integer`     | The overall question number in the test (1-40).                                                         |
| `question_uid`     | `varchar`     | Unique identifier: `{exam_source}-t{test_number}-l-q{question_number}`.                                 |
| `question_type`    | `varchar`     | An enum string representing the fundamental interaction type. See Section 3.                            |
| `question_content` | `jsonb`       | The specific text, options, and subtitles for the question. See Section 4.              |
| `correct_answer`   | `jsonb`       | The correct answer(s) for the question.                                                                 |
| `explanation`      | `text`        | Optional explanation for the answer.                                                                    |
| `word_limit`       | `varchar`     | Optional constraint on answer length, e.g., "ONE WORD AND/OR A NUMBER".                                 |

---

## 3. The `QuestionType` Enum for Listening

The `question_type` still defines the core *interaction* type for a single question.

- `FILL_IN_BLANK`: User types an answer into a simple text box.
- `MULTIPLE_CHOICE`: User selects one option from a list (radio buttons).
- `MULTIPLE_CHOICE_MULTIPLE_ANSWERS`: User selects multiple options from a list (checkboxes).
- `MATCHING`: User selects an option from a dropdown list. This is used for all matching-style questions.

---

## 4. Detailed JSON Structures: `section_layout` and `question_content`

This is the core of the new model. The `section_layout` column in the `sections` table holds a JSON object containing a `blocks` array.

### 4.1. The `section_layout` Object

```json
{
  "blocks": [
    // ... one or more block objects go here ...
  ]
}
```

### 4.2. Block Type: `INSTRUCTIONS_ONLY`
- **Use Case:** For showing instructions, and optional main titles, for a group of standard questions (e.g., Multiple Choice).
- **Rendering Rules (CRITICAL):**
    - The `title` field (e.g., "Questions 11-14") is rendered as **plain text** inside `<strong>` tags. It does not support HTML.
    - The `instructions_text` field is rendered as **HTML**. Use this field for any formatted text, such as main titles for the question group, using tags like `<b>` and `<br/>`.
- **`section_layout` Block Example:**
  ```json
  {
    "block_type": "INSTRUCTIONS_ONLY",
    "content": {
      "title": "Questions 11-14",
      "instructions_text": "<b>Boat trip round Tasmania</b><br/><br/>Choose the correct letter, A, B or C."
    },
    "question_numbers": [11, 12, 13, 14]
  }
  ```
- **`question_content` (for Q11-14):** Standard `MULTIPLE_CHOICE` format.
  ```json
  {
    "text": "What is the maximum number of people who can stand on each side of the boat?",
    "options": ["A 9", "B 15", "C 18"]
  }
  ```

### 4.3. Block Type: `PLAN_MAP_DIAGRAM_LABELING`
- **Use Case:** For labeling specific points on a shared image.
- **`section_layout` Block:**
  ```json
  {
    "block_type": "PLAN_MAP_DIAGRAM_LABELING",
    "content": {
      "title": "Questions 15-20",
      "instructions_text": "Label the plan below. Write the correct letter, A-G, next to questions 15-20.",
      "image_url": "https://.../path/to/plan.png",
      "options": [
        { "letter": "A", "text": "Conference room" },
        { "letter": "B", "text": "New entrance" }
      ]
    },
    "question_numbers": [15, 16, 17, 18, 19, 20]
  }
  ```
- **`question_content` (for Q15-20):** The `text` is the label on the map, and the `question_type` is `MATCHING`.
  ```json
  {
    "text": "15. Reception"
  }
  ```

### 4.4. Block Type: `MATCHING_FEATURES`
- **Use Case:** For matching a list of items (questions) to a shared box of options or features.
- **`section_layout` Block:**
  ```json
  {
    "block_type": "MATCHING_FEATURES",
    "content": {
      "title": "Questions 21-25",
      "instructions_text": "What feature is available at the following hotels?",
      "options_title": "Hotel Features",
      "options": [
        { "letter": "A", "text": "outdoor swimming pool" },
        { "letter": "B", "text": "conference facilities" },
        { "letter": "C", "text": "gym" }
      ]
    },
    "question_numbers": [21, 22, 23, 24, 25]
  }
  ```
- **`question_content` (for Q21-25):** The `text` is the item to be matched, and the `question_type` is `MATCHING`.
  ```json
  {
    "text": "The Pearl"
  }
  ```

### 4.5. Block Type: `NOTE_COMPLETION` (New, Robust Structure)
- **Use Case:** For note, summary, or table completion where questions are presented as a list under various headings.
- **Rendering Rules (CRITICAL):**
    - The `main_title` field is rendered as **plain text** inside `<h3>` tags. It does not support HTML.
    - The `instructions_text` field is rendered as **HTML**. Use this for any preamble or definitions that appear before the main instructions.
    - The `section_title` in `question_content` is rendered as **plain text** inside `<h4>` tags. It does not support HTML.
    - The `text` field in `question_content` is rendered as **HTML**. Use this to include multi-line context or bullet points using `<br/>` tags.
- **`section_layout` Block Example:**
  ```json
  {
    "block_type": "NOTE_COMPLETION",
    "content": {
      "title": "Questions 31-40",
      "instructions_text": "<b>Definition</b><br/>• a winding spiral path leading to a central area<br/><br/>Complete the notes below. Write ONE WORD ONLY for each answer.",
      "main_title": "Labyrinths"
    },
    "question_numbers": [31, 32, 33, 34, 35, 36, 37, 38, 39, 40]
  }
  ```
- **`question_content` (for Q1-10):** Each question is now self-contained.
  - `question_type` is `FILL_IN_BLANK`.
  - The `text` field contains the sentence with a `____` placeholder and can include HTML for line breaks.
  - An **optional** `section_title` key can be added. The frontend will render this as an `<h4>` heading above the question *only when it changes* from the previous question.
  ```json
  // For Question 1
  {
    "section_title": "Beach",
    "text": "• making sure the beach does not have 1 ____ on it"
  }

  // For Question 2 (no section_title needed as it's the same as Q1)
  {
    "text": "• no 2 ____"
  }

  // For Question 3 (new section_title and multi-line context in 'text')
  {
    "section_title": "Nature reserve",
    "text": "• maintaining paths<br/>• nesting boxes for birds installed<br/>• next task is taking action to attract 3 ____ to the place"
  }
  ```
- **`correct_answer` (for Q1):** `["litter"]`

---

## 5. Step-by-Step Workflow & SQL Generation

**Objective:** To process one full IELTS Listening Test and generate the corresponding SQL.

**Step 1: Add a Deletion Clause**
To prevent duplicate data, start by deleting existing test data for the target exam.
```sql
-- Inside your DO $$ block, before inserting
DELETE FROM public.questions WHERE section_id IN (SELECT id FROM public.sections WHERE exam_source = 'cam17' AND test_number = '1' AND skill = 'listening');
DELETE FROM public.sections WHERE exam_source = 'cam17' AND test_number = '1' AND skill = 'listening';
```

**Step 2: Generate `INSERT` for `sections` and `questions` Tables**
- For each part (1-4), generate one `INSERT` for the `sections` table.
- The `section_layout` column must contain the full, valid JSON structure with all blocks for that part.
- Immediately follow with `INSERT` statements for all questions belonging to that section.

**Example for a Part with a `NOTE_COMPLETION` block:**
```sql
-- Declare variables for each section ID
DO $$
DECLARE
    section_1_id bigint;
BEGIN
    -- (Deletion clause here)

    -- Insert Section 1 and capture its ID
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, audio_url, passage_text, section_layout)
    VALUES (
        'cam17', '1', 'listening', 1, 'https://.../part1.mp3', '...',
        '{
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
        }'::jsonb
    )
    RETURNING id INTO section_1_id;

    -- Insert all questions for Part 1
    INSERT INTO public.questions (section_id, question_number, question_uid, question_type, question_content, correct_answer, word_limit) VALUES
    (section_1_id, 1, 'cam17-t1-l-q1', 'FILL_IN_BLANK', '{"section_title": "Beach", "text": "• making sure the beach does not have 1 ____ on it"}', '["litter"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 2, 'cam17-t1-l-q2', 'FILL_IN_BLANK', '{"text": "• no 2 ____"}', '["dogs"]', 'ONE WORD AND/OR A NUMBER'),
    (section_1_id, 3, 'cam17-t1-l-q3', 'FILL_IN_BLANK', '{"section_title": "Nature reserve", "text": "• maintaining paths<br/>• nesting boxes for birds installed<br/>• next task is taking action to attract 3 ____ to the place"}', '["insects"]', 'ONE WORD AND/OR A NUMBER');
    -- ... and so on for questions 4-10

END $$;
```

---

## 6. Common Pitfalls & Solutions

- **JSON Validity:** The `section_layout` JSON must be perfectly formed. Use a JSON validator before inserting. Remember to cast it to `jsonb` in SQL using `::jsonb`.
- **`question_numbers` Array:** This array in each block is critical for the frontend to know which questions to render inside that block. Ensure it's accurate.
- **HTML in Wrong Fields:** The frontend renderer treats different JSON fields differently.
  - **Problem:** Titles or context appear with visible `<br/>` or `<b>` tags.
  - **Cause:** You placed HTML tags in a plain-text field.
  - **Solution:** Remember the rendering rules:
    - **Plain Text Fields:** `main_title`, `title`, `section_title`. Do not use HTML here.
    - **HTML Fields:** `instructions_text` (in the section block) and `text` (in a question's content). Use these fields for any formatted content, including line breaks (`<br/>`) and bolding (`<b>`).
- **`____` Placeholder:** For `FILL_IN_BLANK` questions, ensure the `text` in `question_content` contains exactly one `____` (four underscores) where the input box should appear.