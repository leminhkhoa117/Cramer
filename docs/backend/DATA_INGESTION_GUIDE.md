# AI Agent Guide: Generating SQL for IELTS Test Ingestion

## 1. Objective

This document provides a comprehensive guide for an AI agent to parse an IELTS Reading Test from a source (like a PDF) and generate the corresponding SQL `INSERT` statements. The goal is to accurately populate the PostgreSQL database (hosted on Supabase) used by the Cramer application with new test materials.

**Strict adherence to the JSON structures and `QuestionType` enums outlined below is critical for the application to function correctly.**

### Database Connection Details
- **Database:** PostgreSQL on Supabase
- **Connection:** Configured via environment variables in `.env` file
- **JDBC URL Format:** `jdbc:postgresql://db.xxx.supabase.co:6543/postgres?sslmode=require&prepareThreshold=0`
- **Important:** The `prepareThreshold=0` parameter is required to prevent prepared statement cache conflicts with HikariCP connection pooling

## 2. Core Database Schema

The test data is primarily stored in two tables: `sections` and `questions`.

### `sections` Table
This table stores the reading passage for each part of a test.

| Column                | Type      | Description                                                                                             |
|-----------------------|-----------|---------------------------------------------------------------------------------------------------------|
| `id`                  | `bigint`  | Primary Key. Auto-incrementing unique identifier for the section.                                       |
| `exam_source`         | `varchar` | The source of the exam, e.g., `cam17` for Cambridge 17.                                                |
| `test_number`         | `varchar` | The test number within the source, e.g., `'1'`, `'2'` (stored as string).                              |
| `skill`               | `varchar` | The skill being tested. For now, this will always be `reading`.                                        |
| `part_number`         | `integer` | The part number within the test, i.e., `1`, `2`, or `3` for a Reading test.                            |
| `display_content_url` | `varchar` | Optional URL for displaying content (nullable, currently unused).                                       |
| `passage_text`        | `text`    | The full HTML-formatted passage text with `<strong>` tags for titles and paragraph markers.            |

### `questions` Table
This table stores every individual question for all sections.

| Column             | Type          | Description                                                                                             |
|--------------------|---------------|---------------------------------------------------------------------------------------------------------|
| `id`               | `bigint`      | Primary Key. Auto-incrementing unique identifier for the question.                                      |
| `section_id`       | `bigint`      | **Foreign Key** referencing `sections.id`. Links the question to its reading passage.                   |
| `question_number`  | `integer`     | The overall question number in the test (e.g., 1, 14, 27).                                              |
| `question_uid`     | `varchar`     | Unique identifier following pattern: `{exam_source}-t{test_number}-{skill}-q{question_number}`.         |
| `question_type`    | `varchar`     | An enum string representing the type of question. See Section 3 for the definitive list.                |
| `question_content` | `jsonb`       | The specific text and options for the question. The structure varies by `question_type`. See Section 4. |
| `correct_answer`   | `jsonb`       | The correct answer(s) for the question. The structure varies by `question_type`. See Section 4.         |
| `explanation`      | `text`        | Optional explanation for the answer (nullable, currently unused).                                        |
| `word_limit`       | `varchar`     | Optional constraint on answer length, e.g., "NO MORE THAN TWO WORDS".                                    |
| `image_url`        | `varchar`     | Optional URL for an image associated with the question (e.g., for diagram questions).                    |

**Important Notes:** 
- The `question_uid` field provides a human-readable unique identifier (e.g., `cam17-t1-r-q1` for Cambridge 17, Test 1, Reading, Question 1).
- **Removed `instructions` field**: Instructions are now handled at the frontend level based on question type and grouping logic.
- Ensure each question has a unique `id` and avoid inserting duplicate questions.

---

## 3. The `QuestionType` Enum

The `question_type` column **must** be one of the following string values:

- `FILL_IN_BLANK`
- `SUMMARY_COMPLETION`
- `TRUE_FALSE_NOT_GIVEN`
- `YES_NO_NOT_GIVEN`
- `MATCHING_INFORMATION`
- `MATCHING_HEADINGS`
- `MATCHING_FEATURES`
- `MATCHING_SENTENCE_ENDINGS`
- `MULTIPLE_CHOICE`
- `MULTIPLE_CHOICE_MULTIPLE_ANSWERS`
- `SUMMARY_COMPLETION_OPTIONS`
- `DIAGRAM_LABEL_COMPLETION`
- `TABLE_COMPLETION`
- `FLOW_CHART_COMPLETION`

---

## 4. Detailed JSON Structures

This section details the required `jsonb` format for `question_content` and `correct_answer` for each `QuestionType`.

### `FILL_IN_BLANK`
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

### `SUMMARY_COMPLETION`
- **Description:** User completes a summary by typing words/phrases from the passage into text boxes.
- **Important Data Structure:** For `SUMMARY_COMPLETION` questions, the overall summary text is typically fragmented across multiple individual question entries in the database. Each question entry should contain a portion of the summary text, usually with a single blank (`____`). The frontend will combine these fragments to display a continuous summary.
- **`question_content`**:
  ```json
  {
    "text": "The database that Ogilvie and her team has compiled sheds light on the lives of a range of individuals, as well as those of their 19 ____ over a 300-year period."
  }
  ```
- **`correct_answer`**: An array containing the single correct word/phrase.
  ```json
  ["descendants"]
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
- **Special Note:** The frontend automatically adds "**NB** You may use any letter more than once." to the instructions.
- **`question_content`**: Includes both the text and the options array (even though options are predictable).
  ```json
  {
    "text": "a mention of negative attitudes towards stadium building projects",
    "options": ["A", "B", "C", "D", "E", "F", "G", "H"]
  }
  ```
- **`correct_answer`**: An array containing the single correct paragraph letter.
  ```json
  ["A"]
  ```

### `MATCHING_HEADINGS`, `MATCHING_FEATURES`, `MATCHING_SENTENCE_ENDINGS`
- **Description:** These types handle matching a question item (e.g., a paragraph, a statement, a sentence start) to an option from a shared list (e.g., a list of headings, researcher names, or sentence endings).
- **Special Note:** For a group of these questions, the `options` array should be identical for every question in the group. The `text` field will contain the specific item for that question (e.g., "Paragraph A", "The theory that...", "The experiment showed that...").
- **`question_content`**:
  ```json
  {
    "text": "The question text, e.g., 'Paragraph C'",
    "options": [
      { "letter": "i", "text": "Heading text one" },
      { "letter": "ii", "text": "Heading text two" },
      { "letter": "iii", "text": "Heading text three" }
    ]
  }
  ```
- **`correct_answer`**: An array containing the single correct letter/roman numeral.
  ```json
  ["ii"]
  ```

### `SUMMARY_COMPLETION_OPTIONS`
- **Description:** User completes a blank in a summary by choosing a letter corresponding to a phrase from a separate list of options. The renderer replaces the `____` with a dropdown.
- **`question_content`**: The `text` contains the summary with a blank. The `options` array contains objects, each with a `letter` and the full `text` of the choice.
  ```json
  {
    "text": "The railway was initially a 27 ____ success.",
    "options": [
      { "letter": "A", "text": "commercial" },
      { "letter": "B", "text": "financial" },
      { "letter": "C", "text": "popular" }
    ]
  }
  ```
- **`correct_answer`**: An array containing the single correct letter.
  ```json
  ["A"]
  ```

### `TABLE_COMPLETION` and `FLOW_CHART_COMPLETION`
- **Description:** User fills in blanks within a table or flow-chart.
- **`question_content`**: The `text` field contains the entire visual structure as an HTML string (e.g., using `<table>`). Blanks are marked with `____`. Each blank corresponds to a separate question entry in the database.
  ```json
  {
    "text": "<table><tr><td>Year</td><td>Event</td></tr><tr><td>1800</td><td>____</td></tr></table>"
  }
  ```
- **`correct_answer`**: An array containing the string for the blank.
  ```json
  ["Population growth"]
  ```

### `DIAGRAM_LABEL_COMPLETION`
- **Description:** User labels parts of a diagram based on an image.
- **Special Note:** The `image_url` field on the question record **must** be populated with the URL of the diagram image.
- **`question_content`**: The `text` field simply contains the label for the question, which corresponds to a number on the diagram.
  ```json
  {
    "text": "1. ____"
  }
  ```
- **`correct_answer`**: An array containing the string for the label.
  ```json
  ["Engine"]
  ```

---

## 5. The `passage_text` Structure

The `passage_text` column in the `sections` table is a `text` field that contains HTML-formatted text. Key formatting rules:

- **Title**: Use `<strong>Title text</strong>` for the passage title
- **Paragraphs**: Separate with `\n\n` (double newline)
- **Paragraph labels** (for MATCHING_INFORMATION questions): Use `<strong>A.</strong>`, `<strong>B.</strong>`, etc.
- **Special formatting**: Use `<strong>` for bold text, preserve single quotes as `''` (escaped)

**Example `passage_text`**:
```html
<strong>The development of the London underground railway</strong>

In the first half of the 1800s, London''s population grew at an astonishing rate, and the central area became increasingly congested.

Amongst the most vocal advocates for a solution to London''s traffic problems was Charles Pearson, who worked as a solicitor for the City of London.
```

**Important Notes:**
- The frontend `HighlightableText` component parses this HTML to enable text selection and highlighting.
- Single quotes must be escaped as `''` (double single quotes) in SQL INSERT statements.
- For passages with labeled paragraphs (like stadium passage), each paragraph should start with `<strong>{Letter}.</strong>`.

---

## 6. Step-by-Step Workflow & SQL Generation

**Objective:** To process one full IELTS Reading Test (e.g., Cambridge 18, Test 1) and generate all necessary SQL.

**Step 1: Declare a Variable for the Section ID**
For each passage/section, you must first insert the section and retrieve its generated `id`. A good practice is to declare a variable to hold this ID.

**Step 2: Generate `INSERT` for `sections` Table**
- Parse the first passage (Part 1).
- Extract its full text with HTML formatting.
- Generate the `INSERT` statement for the `sections` table.
- Use `RETURNING id` to capture the auto-incremented ID of the new row.

**Example for Passage 1:**
```sql
-- Declare a variable to hold the ID of the section we are about to create
DO $$
DECLARE
    section_1_id bigint;
BEGIN
    -- Insert Passage 1 and capture its ID
    INSERT INTO sections (test_id, test_number, part_number, section_type, passage_text, created_at, updated_at)
    VALUES (
        1,
        'Test 1',
        1,
        'READING',
        '<strong>The development of the London underground railway</strong>

In the first half of the 1800s, London''s population grew at an astonishing rate, and the central area became increasingly congested.

Amongst the most vocal advocates for a solution to London''s traffic problems was Charles Pearson, who worked as a solicitor for the City of London.',
        NOW(),
        NOW()
    )
    RETURNING id INTO section_1_id;

    -- Now, insert all questions for Passage 1 using the captured section_1_id

END $$;
```
**Note:** 
- Remember to escape single quotes in the text string by doubling them (`''`).
- `passage_text` must include HTML formatting with `<strong>` tags for titles.
- `test_number` is varchar (e.g., 'Test 1', 'Test 2').
- `id` is auto-generated (bigint), do NOT include it in INSERT.

**Step 3: Generate `INSERT` for `questions` Table**
- For every question associated with the passage from Step 2, generate an `INSERT` statement.
- Use the `section_1_id` variable for the `section_id` column.
- Meticulously follow the `QuestionType` enums and JSON structures from Sections 3 & 4.

**Example for Questions in Passage 1:**
```sql
-- (This code would be inside the DO block from the previous step)

-- Question 1: FILL_IN_BLANK
INSERT INTO questions (section_id, question_uid, question_number, question_type, question_content, correct_answer, created_at, updated_at)
VALUES (
    section_1_id,
    'cam17-t1-r-q1',
    1,
    'FILL_IN_BLANK',
    '{"text": "The 1 ____ of London increased rapidly between 1800 and 1850"}',
    '["population"]',
    NOW(),
    NOW()
);

-- Question 8: TRUE_FALSE_NOT_GIVEN
INSERT INTO questions (section_id, question_uid, question_number, question_type, question_content, correct_answer, created_at, updated_at)
VALUES (
    section_1_id,
    'cam17-t1-r-q8',
    8,
    'TRUE_FALSE_NOT_GIVEN',
    '{"text": "The London Underground is the oldest in the world."}',
    '["TRUE"]',
    NOW(),
    NOW()
);
```

**Important Notes:**
- `question_uid` follows pattern: `{exam_source}-t{test_number}-{skill}-q{question_number}` (e.g., 'cam17-t1-r-q1')
- `instructions` field removed - instructions are handled by frontend based on `question_type`
- `id` is auto-generated (bigint), do NOT include it in INSERT
- Optional `explanation` field can be added for answer explanations

**Step 4: Repeat for All Passages**
- Repeat Steps 1-3 for each subsequent passage (Part 2 and Part 3) in the test, creating a new `DO` block and a new `section_id` variable for each.
- Ensure `part_number` is updated accordingly (`2`, `3`).
- Ensure `question_number` continues to increment correctly across all parts (1-40).

---

## 7. Frontend Rendering Considerations

### Question Grouping
- The frontend automatically groups **consecutive questions of the same type** for display
- Each group shows instructions once at the top (e.g., "Questions 1-6")
- Questions are deduplicated by ID to prevent rendering duplicates (safety net for data quality issues)

### UI Layout
- **Split-panel layout** with resizable divider:
  - Left panel: Reading passage with `HighlightableText` component
  - Right panel: Questions with automatic grouping
  - Resize handle: 6px gray bar with white square button containing ⟷ icon
- **Full viewport height** layout with fixed header (60px) and footer (100px)
- Each panel scrolls independently

### Special Instructions Rendering
- **MATCHING_INFORMATION** automatically appends: "**NB** You may use any letter more than once."
- **MULTIPLE_CHOICE** options automatically have letters bolded (A., B., C., etc.)
- Instructions are rendered with proper formatting (bold, emphasis)

### `SUMMARY_COMPLETION` Rendering
- For `SUMMARY_COMPLETION` questions, the frontend combines the `text` from all questions within a group to form a continuous summary paragraph.
- Each individual question's text (containing a single blank) is rendered inline, with an input field replacing the `____` placeholder. This creates a cohesive summary where users can fill in multiple blanks sequentially.

### Performance Considerations
- Backend uses `prepareThreshold=0` to avoid PostgreSQL prepared statement cache conflicts
- Frontend deduplicates questions by ID using `Map` data structure
- HikariCP connection pool: max 10, min idle 5 connections


---

## 8. Common Issues and Solutions

### Duplicate Questions
**Problem:** Questions appearing twice in the UI
**Root Cause:** Database contains duplicate rows with same question data
**Solution:** 
- Ensure unique constraints on question insertions
- Frontend has deduplication as safety net but fix at database level

### Prepared Statement Errors
**Problem:** `ERROR: prepared statement "S_1" does not exist/already exists`
**Root Cause:** PostgreSQL JDBC driver caching prepared statements with HikariCP connection reuse
**Solution:** Add `prepareThreshold=0` to JDBC URL to disable server-side prepared statements

### Layout/Scrolling Issues
**Problem:** Unwanted scrollbars or content not filling viewport
**Root Cause:** CSS conflicts between fixed positioning and flexbox layout
**Solution:** Use proper viewport height calculations accounting for fixed header/footer

```