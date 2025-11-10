# AI Agent Guide: Generating SQL for IELTS Test Ingestion

## 1. Objective

This document provides a comprehensive guide for an AI agent to parse an IELTS Reading Test from a source (like a PDF) and generate the corresponding SQL `INSERT` statements. The goal is to accurately populate the PostgreSQL database used by the Cramer application with new test materials.

**Strict adherence to the JSON structures and `QuestionType` enums outlined below is critical for the application to function correctly.**

## 2. Core Database Schema

The test data is primarily stored in two tables: `sections` and `questions`.

### `sections` Table
This table stores the reading passage for each part of a test.

| Column        | Type      | Description                                                                                             |
|---------------|-----------|---------------------------------------------------------------------------------------------------------|
| `id`          | `uuid`    | Primary Key. A unique identifier for the section.                                                       |
| `exam_source` | `varchar` | The source of the exam, e.g., `cam18` for Cambridge 18.                                                 |
| `test_number` | `varchar` | The test number within the source, e.g., `1`, `2`.                                                        |
| `part_number` | `integer` | The part number within the test, i.e., `1`, `2`, or `3` for a Reading test.                               ||
| `skill`       | `varchar` | The skill being tested. For now, this will always be `reading`.                                         ||
| `passage_text`| `jsonb`   | The full text of the reading passage. See Section 5 for the exact JSON structure.                       |

### `questions` Table
This table stores every individual question for all sections.

| Column             | Type          | Description                                                                                             |
|--------------------|---------------|---------------------------------------------------------------------------------------------------------|
| `id`               | `uuid`        | Primary Key. A unique identifier for the question.                                                      |
| `section_id`       | `uuid`        | **Foreign Key** referencing `sections.id`. Links the question to its reading passage.                   |
| `question_number`  | `integer`     | The overall question number in the test (e.g., 1, 14, 27).                                              |
| `question_type`    | `varchar`     | An enum string representing the type of question. See Section 3 for the definitive list.                |
| `instructions`     | `text`        | The instructions for a group of questions (e.g., "Choose ONE WORD ONLY...").                            |
| `question_content` | `jsonb`       | The specific text and options for the question. The structure varies by `question_type`. See Section 4. |
| `correct_answer`   | `jsonb`       | The correct answer(s) for the question. The structure varies by `question_type`. See Section 4.         |

---

## 3. The `QuestionType` Enum

The `question_type` column **must** be one of the following string values:

- `FILL_IN_BLANK`
- `SUMMARY_COMPLETION`
- `TRUE_FALSE_NOT_GIVEN`
- `YES_NO_NOT_GIVEN`
- `MATCHING_INFORMATION`
- `MULTIPLE_CHOICE`
- `MULTIPLE_CHOICE_MULTIPLE_ANSWERS`
- `SUMMARY_COMPLETION_OPTIONS`

---

## 4. Detailed JSON Structures

This section details the required `jsonb` format for `question_content` and `correct_answer` for each `QuestionType`.

### `FILL_IN_BLANK` and `SUMMARY_COMPLETION`
- **Description:** User types a word/phrase from the passage into a text box.
- **`question_content`**:
  ```json
  {
    "text": "The 1 ____ of London increased rapidly between 1800 and 1850"
  }
  ```
- **`correct_answer`**: An array of acceptable string answers. Case-insensitive matching is performed by the backend.
  ```json
  ["population"]
  ```

### `TRUE_FALSE_NOT_GIVEN` and `YES_NO_NOT_GIVEN`
- **Description:** User selects from three radio buttons.
- **`question_content`**:
  ```json
  {
    "text": "The London Underground is the oldest in the world."
  }
  ```
- **`correct_answer`**: An array containing a single string: "TRUE", "FALSE", "NOT GIVEN", "YES", or "NO".
  ```json
  ["TRUE"]
  ```

### `MULTIPLE_CHOICE`
- **Description:** User selects one option from a list. The renderer will automatically bold the letter and add a period.
- **`question_content`**:
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
- **`correct_answer`**: An array containing the single correct letter.
  ```json
  ["B"]
  ```

### `MULTIPLE_CHOICE_MULTIPLE_ANSWERS`
- **Description:** User selects multiple options from a list. The renderer will automatically bold the letter and add a period.
- **`question_content`**:
  ```json
  {
    "text": "Which TWO of the following are mentioned as benefits of the railway?",
    "options": [
      "A Reduced road traffic",
      "B Lower ticket prices",
      "C Faster journey times",
      "D A boost to the economy"
    ]
  }
  ```
- **`correct_answer`**: An array containing all correct letters.
  ```json
  ["A", "C"]
  ```

### `MATCHING_INFORMATION`
- **Description:** User matches a statement to a paragraph in the passage.
- **`question_content`**:
  ```json
  {
    "text": "a reference to a problem that had not been foreseen"
  }
  ```
- **`correct_answer`**: An array containing the single correct paragraph letter.
  ```json
  ["D"]
  ```

### `SUMMARY_COMPLETION_OPTIONS`
- **Description:** User completes a blank in a summary by choosing a letter corresponding to a phrase from a separate list. The renderer replaces the `____` with a dropdown.
- **`question_content`**: The `text` contains the summary with a blank. The `options` contain the list of choices the user selects from.
  ```json
  {
    "text": "The railway was initially a 27 ____ success.",
    "options": ["A", "B", "C", "D", "E", "F", "G", "H"]
  }
  ```
- **`correct_answer`**: An array containing the single correct letter.
  ```json
  ["A"]
  ```

---

## 5. The `passage_text` JSON Structure

The `passage_text` column in the `sections` table is a `jsonb` field that must contain a single object with a "text" key. Paragraphs from the source PDF must be separated by `\n`.

- **Example `passage_text`**:
  ```json
  {
    "text": "The first section of the London Underground railway opened on 10 January 1863, between Paddington and Farringdon Street.\n\nIt was the world's first underground railway and was a major engineering achievement. The line was built using the 'cut-and-cover' method, where a trench was dug, the railway lines laid, and then the trench was covered over."
  }
  ```

---

## 6. Step-by-Step Workflow & SQL Generation

**Objective:** To process one full IELTS Reading Test (e.g., Cambridge 18, Test 1) and generate all necessary SQL.

**Step 1: Declare a Variable for the Section ID**
For each passage/section, you must first insert the section and retrieve its generated `id`. A good practice is to declare a variable to hold this ID.

**Step 2: Generate `INSERT` for `sections` Table**
- Parse the first passage (Part 1).
- Extract its full text.
- Generate the `INSERT` statement for the `sections` table.
- Use `RETURNING id` to capture the UUID of the new row.

**Example for Passage 1:**
```sql
-- Declare a variable to hold the UUID of the section we are about to create
DO $$
DECLARE
    section_1_id uuid;
BEGIN
    -- Insert Passage 1 and capture its ID
    INSERT INTO sections (exam_source, test_number, part_number, skill, passage_text)
    VALUES (
        'cam18',
        '1',
        1,
        'reading',
        '{"text": "The first section of the London Underground railway opened on 10 January 1863...\n\nIt was the world''s first underground railway..."}'
    )
    RETURNING id INTO section_1_id;

    -- Now, insert all questions for Passage 1 using the captured section_1_id

END $$;
```
**Note:** Remember to escape single quotes in the JSON string by doubling them (`''`).

**Step 3: Generate `INSERT` for `questions` Table**
- For every question associated with the passage from Step 2, generate an `INSERT` statement.
- Use the `section_1_id` variable for the `section_id` column.
- Meticulously follow the `QuestionType` enums and JSON structures from Sections 3 & 4.

**Example for Questions in Passage 1:**
```sql
-- (This code would be inside the DO block from the previous step)

-- Question 1: FILL_IN_BLANK
INSERT INTO questions (section_id, question_number, question_type, instructions, question_content, correct_answer)
VALUES (
    section_1_id,
    1,
    'FILL_IN_BLANK',
    'Complete the notes below. Choose ONE WORD ONLY from the passage for each answer.',
    '{"text": "The 1 ____ of London increased rapidly between 1800 and 1850"}',
    '["population"]'
);

-- Question 8: TRUE_FALSE_NOT_GIVEN
INSERT INTO questions (section_id, question_number, question_type, instructions, correct_answer, question_content)
VALUES (
    section_1_id,
    8,
    'TRUE_FALSE_NOT_GIVEN',
    'Do the following statements agree with the information given in Reading Passage 1? Write TRUE, FALSE, or NOT GIVEN.',
    '["TRUE"]',
    '{"text": "The London Underground is the oldest in the world."}'
);
```

**Step 4: Repeat for All Passages**
- Repeat Steps 1-3 for each subsequent passage (Part 2 and Part 3) in the test, creating a new `DO` block and a new `section_id` variable for each.
- Ensure `part_number` is updated accordingly (`2`, `3`).
- Ensure `question_number` continues to increment correctly across all parts (1-40).

```