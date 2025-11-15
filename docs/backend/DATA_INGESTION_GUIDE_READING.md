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
- **Important Data Structure & Frontend Logic:**
  - For a group of `SUMMARY_COMPLETION` questions, the frontend renders each question's `text` content sequentially as an inline element.
  - **Rule 1:** Each question entry **must** contain its own `text` and exactly **one** `____` placeholder to ensure its input box is rendered.
  - **Rule 2:** To provide contextual titles or headings, include them in the `text` field of the first question in the group.
  - **CRITICAL:** To maintain the inline flow and prevent layout breaks, **do not use block-level HTML tags** (like `<p>`, `<h4>`, or `<ul>`). Use inline tags such as `<strong>` for emphasis and `<br />` for line breaks.
- **`question_content` Example (Group of 2 questions):**
  ```json
  // Question 18
  {
    "text": "<strong>Roman amphitheatres</strong><br/><br/>The Roman stadiums of Europe have proved very versatile. The amphitheatre of Arles, for example, was converted first into a <strong>18</strong> ____, then into a residential area..."
  }
  // Question 19
  {
    "text": "...and finally into an arena where spectators could watch <strong>19</strong> ____."
  }
  ```
- **`correct_answer`**: An array containing the single correct word/phrase.
  ```json
  ["fortress"]
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
- **Important Data Structure & Frontend Logic:**
  - **CRITICAL:** For a group of `SUMMARY_COMPLETION_OPTIONS` questions, the `options` array **must be present and identical** for every single question in the group. The frontend does not share options from one question with others in its group.
  - Each question entry must contain its own `text` and exactly **one** `____` placeholder.
  - To avoid layout issues, use only inline HTML tags (`<strong>`, `<br />`) within the `text` field.
- **`question_content`**: The `text` contains the summary with a blank. The `options` array contains objects, each with a `letter` and the full `text` of the choice.
  ```json
  // For Question 27
  {
    "text": "Charles II then formed a <strong>27</strong> ____ with the Scots...",
    "options": [
      { "letter": "A", "text": "military innovation" },
      { "letter": "B", "text": "large reward" }
      // ... and so on
    ]
  }
  // For Question 28
  {
    "text": "...he abandoned an important <strong>28</strong> ____ that was held by his father...",
    "options": [
      { "letter": "A", "text": "military innovation" },
      { "letter": "B", "text": "large reward" }
      // ... and so on (MUST be repeated)
    ]
  }
  ```
- **`correct_answer`**: An array containing the single correct letter.
  ```json
  ["H"]
  ```

### `TABLE_COMPLETION` and `FLOW_CHART_COMPLETION`
- **Description:** User fills in blanks within a table or flow-chart.
- **Frontend Rendering Logic:** This type has a unique rendering behavior.
  1.  The HTML structure (e.g., the `<table>...</table>`) is taken **only** from the `text` field of the **first question** in the group and rendered once. Any `____` placeholders in this HTML are for author reference only and are **not** replaced with inputs.
  2.  The frontend then loops through **all** questions in the group and renders a numbered list of text input boxes, ignoring the `text` field of subsequent questions.
- **`question_content`**:
  ```json
  // For the FIRST question in the group
  {
    "text": "<table><tr><td>Year</td><td>Event</td></tr><tr><td>1800</td><td><strong>1.</strong></td></tr></table>"
  }
  // For subsequent questions in the group
  {
    "text": "" // This field is ignored
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

**Step 1: Add a Deletion Clause**
To prevent duplicate data when re-running a script, start your `DO` block by deleting the existing test data.
```sql
-- Inside your DO $$ block, before inserting
DELETE FROM public.questions WHERE section_id IN (SELECT id FROM public.sections WHERE exam_source = 'cam17' AND test_number = '1' AND skill = 'reading');
DELETE FROM public.sections WHERE exam_source = 'cam17' AND test_number = '1' AND skill = 'reading';
```

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
    -- (Deletion clause from Step 1 here)

    -- Insert Passage 1 and capture its ID
    INSERT INTO public.sections (exam_source, test_number, skill, part_number, passage_text)
    VALUES (
        'cam17',
        '1',
        'reading',
        1,
        '<strong>The development of the London underground railway</strong> ...'
    )
    RETURNING id INTO section_1_id;

    -- Now, insert all questions for Passage 1 using the captured section_1_id
    -- ...

END $$;
```

**Step 3: Generate `INSERT` for `questions` Table**
- For every question associated with the passage from Step 2, generate an `INSERT` statement.
- Use the `section_1_id` variable for the `section_id` column.
- Meticulously follow the `QuestionType` enums and JSON structures from Section 4. **Pay close attention to the rules for summary and matching questions.**

**Step 4: Repeat for All Passages**
- Repeat Steps 1-3 for each subsequent passage (Part 2 and Part 3) in the test, creating a new `DO` block and a new `section_id` variable for each.
- Ensure `part_number` is updated accordingly (`2`, `3`).
- Ensure `question_number` continues to increment correctly across all parts (1-40).

---

## 7. Common Pitfalls & Solutions

This section highlights critical data authoring rules that are required for the frontend to render correctly.

### 1. Incorrect Text Fragmentation in Summaries
- **Problem:** An input box or dropdown is missing for a question in a summary group.
- **Cause:** A question's `question_content.text` field is either empty or does not contain a `____` placeholder. This often happens when multiple blanks from a single sentence are incorrectly grouped into one question's text.
- **Solution:** Every question in the group **must** have its own `text` field with exactly one `____` placeholder. If one sentence in the source material contains multiple blanks, you must split the sentence itself across multiple question entries.
  - **Example:** If the source is "...regarded as **37** ____, especially when compared to **38** ____.", the data should be:
    - Question 37 `text`: `"...regarded as <strong>37</strong> ____, "`
    - Question 38 `text`: `"especially when compared to <strong>38</strong> ____."`

### 2. Missing `options` Array in Grouped Questions
- **Problem:** The application crashes with a `TypeError: can't access property "map", options is undefined` when viewing a `SUMMARY_COMPLETION_OPTIONS` question.
- **Cause:** The `question_content.options` array is missing from one or more questions in the group.
- **Solution:** For any grouped question type that uses a shared list of options (`SUMMARY_COMPLETION_OPTIONS`, `MATCHING_HEADINGS`, etc.), the **entire `options` array must be repeated** in the `question_content` for every single question within that group.

### 3. Invalid HTML in Inline Summaries (`SUMMARY_COMPLETION`)
- **Problem:** The layout of a summary appears broken, with text and input boxes on incorrect newlines.
- **Cause:** Using block-level HTML tags (e.g., `<p>`, `<h1>`, `<ul>`) inside the `text` field of a `SUMMARY_COMPLETION` question. The frontend renderer wraps this content in a `<span>`, and nesting block elements inside an inline element is invalid HTML that browsers render unpredictably.
- **Solution:** Use only inline HTML tags like `<strong>` for emphasis and `<br />` for manual line breaks to format context and headings.

---

## 8. Frontend Rendering Considerations

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

## 9. Common Issues and Solutions

### Duplicate Questions
**Problem:** Questions appearing twice in the UI
**Root Cause:** Database contains duplicate rows with same question data
**Solution:** 
- Ensure unique constraints on question insertions
- Frontend has deduplication as a safety net but the data should be fixed at the database level.
- Add a `DELETE` clause to the start of your SQL script to clear old data before inserting new data.

### Prepared Statement Errors
**Problem:** `ERROR: prepared statement "S_1" does not exist/already exists`
**Root Cause:** PostgreSQL JDBC driver caching prepared statements with HikariCP connection reuse
**Solution:** Add `prepareThreshold=0` to JDBC URL to disable server-side prepared statements

### Layout/Scrolling Issues
**Problem:** Unwanted scrollbars or content not filling viewport
**Root Cause:** CSS conflicts between fixed positioning and flexbox layout
**Solution:** Use proper viewport height calculations accounting for fixed header/footer