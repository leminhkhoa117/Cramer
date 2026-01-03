# Visual Question Editor - Component Architecture Design

> **Document Version:** 1.0  
> **Created:** January 3, 2026  
> **Status:** DESIGN SPECIFICATION

---

## 1. Overview

This document defines the component architecture for a Visual Question Editor that will replace the current JSON-only editing approach in `QuestionEditModal.jsx`. The goal is to provide a user-friendly interface for non-technical admins to edit IELTS question content.

---

## 2. Component Architecture Diagram

```
QuestionEditModal.jsx (main modal container)
│
├── 📋 QuestionMetaFields.jsx
│     ├── Question Type Selector (dropdown)
│     ├── Question Number (readonly display)
│     ├── Word Limit (text input)
│     └── Image URL (text input with preview)
│
├── 📝 QuestionContentEditor.jsx (dispatcher based on question type)
│     │
│     ├── 🅰️ TextInputEditors/
│     │     ├── FillInBlankEditor.jsx
│     │     ├── SummaryCompletionEditor.jsx
│     │     ├── NoteCompletionEditor.jsx
│     │     ├── TableCompletionEditor.jsx
│     │     ├── FlowChartCompletionEditor.jsx
│     │     └── DiagramLabelEditor.jsx
│     │
│     ├── 🔘 SelectionEditors/
│     │     ├── TrueFalseEditor.jsx
│     │     ├── YesNoEditor.jsx
│     │     ├── MultipleChoiceEditor.jsx
│     │     └── MultipleChoiceMultipleEditor.jsx
│     │
│     └── 🔗 MatchingEditors/
│           ├── MatchingInformationEditor.jsx
│           ├── MatchingHeadingsEditor.jsx
│           ├── MatchingFeaturesEditor.jsx
│           ├── MatchingSentenceEndingsEditor.jsx
│           └── SummaryCompletionOptionsEditor.jsx
│
├── ✅ AnswerEditor.jsx (dispatcher based on question type)
│     ├── TextAnswerEditor.jsx (single text input)
│     ├── ArrayTextAnswerEditor.jsx (multiple text inputs)
│     ├── SingleSelectionAnswerEditor.jsx (radio selection)
│     ├── MultiSelectionAnswerEditor.jsx (checkbox selection)
│     └── MatchingAnswerEditor.jsx (letter dropdown)
│
├── 💡 ExplanationEditor.jsx
│     ├── Tab: Simple (textarea)
│     └── Tab: Structured (JSON fields for explanation parts)
│
├── 👁️ QuestionPreview.jsx
│     ├── Renders question exactly as student sees it
│     └── Uses same render components as TestPage
│
└── 🎯 ActionButtons.jsx
      ├── Save Button
      ├── Cancel Button
      └── Delete Button (with confirmation)
```

---

## 3. Question Type Mapping

| Question Type | Content Editor | Answer Editor | Answer Format |
|---------------|----------------|---------------|---------------|
| `FILL_IN_BLANK` | FillInBlankEditor | TextAnswerEditor | `["word"]` |
| `SUMMARY_COMPLETION` | SummaryCompletionEditor | TextAnswerEditor | `["word"]` |
| `TRUE_FALSE_NOT_GIVEN` | TrueFalseEditor | SingleSelectionAnswerEditor | `["TRUE"]` / `["FALSE"]` / `["NOT GIVEN"]` |
| `YES_NO_NOT_GIVEN` | YesNoEditor | SingleSelectionAnswerEditor | `["YES"]` / `["NO"]` / `["NOT GIVEN"]` |
| `MULTIPLE_CHOICE` | MultipleChoiceEditor | SingleSelectionAnswerEditor | `["A"]` |
| `MULTIPLE_CHOICE_MULTIPLE_ANSWERS` | MultipleChoiceMultipleEditor | MultiSelectionAnswerEditor | `["A", "D"]` |
| `MATCHING_INFORMATION` | MatchingInformationEditor | MatchingAnswerEditor | `["A"]` |
| `MATCHING_HEADINGS` | MatchingHeadingsEditor | MatchingAnswerEditor | `["iv"]` |
| `MATCHING_FEATURES` | MatchingFeaturesEditor | MatchingAnswerEditor | `["A"]` |
| `MATCHING_SENTENCE_ENDINGS` | MatchingSentenceEndingsEditor | MatchingAnswerEditor | `["C"]` |
| `SUMMARY_COMPLETION_OPTIONS` | SummaryCompletionOptionsEditor | MatchingAnswerEditor | `["H"]` |
| `TABLE_COMPLETION` | TableCompletionEditor | TextAnswerEditor | `["word"]` |
| `FLOW_CHART_COMPLETION` | FlowChartCompletionEditor | TextAnswerEditor | `["word"]` |
| `DIAGRAM_LABEL_COMPLETION` | DiagramLabelEditor | TextAnswerEditor | `["word"]` |
| `NOTE_COMPLETION` | NoteCompletionEditor | TextAnswerEditor | `["word"]` |
| `MATCHING` | MatchingFeaturesEditor | MatchingAnswerEditor | `["A"]` |

---

## 4. Detailed Field Layouts for Top 5 Question Types

### 4.1 FILL_IN_BLANK

**Frequency:** Most common in Listening, common in Reading  
**Use Case:** Single blank in a sentence that student fills with a word/number

#### Content Editor Fields

```
┌────────────────────────────────────────────────────────────┐
│ FILL_IN_BLANK Content Editor                               │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Section Title (optional)                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [text input] e.g., "Beach"                           │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  Question Text (with blank placeholder)                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [rich text editor with ____ button]                  │  │
│  │                                                      │  │
│  │ "• making sure the beach does not have ____ on it"   │  │
│  │                                                      │  │
│  │ [Insert Blank] [Bold] [Italic] [Bullet]              │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  💡 Tip: Use ____ or {BLANK} to indicate where the        │
│     student should type their answer                       │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

#### Answer Editor Fields

```
┌────────────────────────────────────────────────────────────┐
│ FILL_IN_BLANK Answer Editor                                │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Correct Answer(s)                                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Primary Answer: [litter                            ] │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  ☑ Accept alternative spellings/answers                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Alt 1: [rubbish                                    ] │  │
│  │ Alt 2: [trash                                      ] │  │
│  │ [+ Add Alternative]                                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  ☐ Case sensitive                                          │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

#### Sample Data Structure

```javascript
// question_content
{
  "section_title": "Beach",
  "text": "• making sure the beach does not have 1 ____ on it"
}

// correct_answer
["litter"]

// With alternatives (future enhancement)
["litter", "rubbish"]
```

---

### 4.2 TRUE_FALSE_NOT_GIVEN

**Frequency:** Very common in Reading  
**Use Case:** Student determines if statement is TRUE, FALSE, or NOT GIVEN

#### Content Editor Fields

```
┌────────────────────────────────────────────────────────────┐
│ TRUE_FALSE_NOT_GIVEN Content Editor                        │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Statement Text                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [textarea]                                           │  │
│  │                                                      │  │
│  │ "Other countries had built underground railways      │  │
│  │  before the Metropolitan line opened."               │  │
│  │                                                      │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  📋 Preview                                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 7. Other countries had built underground railways    │  │
│  │    before the Metropolitan line opened.              │  │
│  │                                                      │  │
│  │    ○ TRUE   ○ FALSE   ○ NOT GIVEN                    │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

#### Answer Editor Fields

```
┌────────────────────────────────────────────────────────────┐
│ TRUE_FALSE_NOT_GIVEN Answer Editor                         │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Correct Answer                                            │
│                                                            │
│    ○ TRUE                                                  │
│    ● FALSE    ← selected                                   │
│    ○ NOT GIVEN                                             │
│                                                            │
│  ───────────────────────────────────────────────────────   │
│                                                            │
│  Passage Reference (optional - for explanation)            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Paragraph: [3    ] Line: [from: 2  ] [to: 4  ]       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

#### Sample Data Structure

```javascript
// question_content
{
  "text": "Other countries had built underground railways before the Metropolitan line opened."
}

// correct_answer
["FALSE"]
```

---

### 4.3 MULTIPLE_CHOICE

**Frequency:** Common in both Reading and Listening  
**Use Case:** Student selects one option from A, B, C, D

#### Content Editor Fields

```
┌────────────────────────────────────────────────────────────┐
│ MULTIPLE_CHOICE Content Editor                             │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Question Text                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [textarea]                                           │  │
│  │                                                      │  │
│  │ "What is the maximum number of people who can stand  │  │
│  │  on each side of the boat?"                          │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  Answer Options                                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ A: [9                                              ] │  │
│  │ B: [15                                             ] │  │
│  │ C: [18                                             ] │  │
│  │ D: [                                        ] [🗑️]  │  │
│  │                                                      │  │
│  │ [+ Add Option]                                       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  💡 Options auto-labeled A, B, C, D...                     │
│                                                            │
│  📋 Preview                                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 11. What is the maximum number of people who can     │  │
│  │     stand on each side of the boat?                  │  │
│  │                                                      │  │
│  │     ○ A  9                                           │  │
│  │     ○ B  15                                          │  │
│  │     ○ C  18                                          │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

#### Answer Editor Fields

```
┌────────────────────────────────────────────────────────────┐
│ MULTIPLE_CHOICE Answer Editor                              │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Select Correct Answer                                     │
│                                                            │
│    ● A - 9        ← selected                               │
│    ○ B - 15                                                │
│    ○ C - 18                                                │
│                                                            │
│  ───────────────────────────────────────────────────────   │
│                                                            │
│  Why is this correct? (optional explanation)               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [textarea - brief explanation for the student]       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

#### Sample Data Structure

```javascript
// question_content
{
  "text": "What is the maximum number of people who can stand on each side of the boat?",
  "options": [
    "A 9",
    "B 15", 
    "C 18"
  ]
}

// correct_answer
["A"]
```

---

### 4.4 MATCHING_HEADINGS (applies to MATCHING_FEATURES, MATCHING_INFORMATION)

**Frequency:** Common in Reading and Listening  
**Use Case:** Match paragraph/item to a heading/feature from a list

#### Content Editor Fields

```
┌────────────────────────────────────────────────────────────┐
│ MATCHING Content Editor                                    │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Item to Match                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [text input]                                         │  │
│  │ "Medical terminology"                                │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  ─── OR use "text" for display ───                        │
│                                                            │
│  📋 Preview                                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 27. Medical terminology                              │  │
│  │                                                      │  │
│  │     [Select ▼]                                       │  │
│  │     ┌─────────────────────────────────────────────┐  │  │
│  │     │ A - Tim found this easier than expected.   │  │  │
│  │     │ B - Tim thought this was not very clearly..│  │  │
│  │     │ C - Diana may do some further study on this│  │  │
│  │     │ D - They both found the reading required...│  │  │
│  │     │ E - Tim was shocked at something he learned│  │  │
│  │     │ F - They were both surprised how little... │  │  │
│  │     └─────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  ⚠️ Note: Matching options are defined at SECTION level    │
│     in section_layout.blocks[].content.options             │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

#### Answer Editor Fields

```
┌────────────────────────────────────────────────────────────┐
│ MATCHING Answer Editor                                     │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Select Correct Match                                      │
│                                                            │
│  Available Options (from section):                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ ○ A - Tim found this easier than expected.           │  │
│  │ ○ B - Tim thought this was not very clearly org...   │  │
│  │ ○ C - Diana may do some further study on this.       │  │
│  │ ○ D - They both found the reading required for...    │  │
│  │ ○ E - Tim was shocked at something he learned on...  │  │
│  │ ○ F - They were both surprised how little is known.. │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  Selected: [A ▼]                                           │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

#### Sample Data Structure

```javascript
// question_content
{
  "text": "Medical terminology"
}

// correct_answer
["A"]

// Note: The matching options come from section_layout.blocks[].content.options
// Example section_layout block:
{
  "block_type": "MATCHING_FEATURES",
  "content": {
    "title": "Questions 27-30",
    "instructions_text": "What opinion do the students give...",
    "options_title": "Opinions",
    "options": [
      {"letter": "A", "text": "Tim found this easier than expected."},
      {"letter": "B", "text": "Tim thought this was not very clearly organised."},
      {"letter": "C", "text": "Diana may do some further study on this."},
      {"letter": "D", "text": "They both found the reading required for this was difficult."},
      {"letter": "E", "text": "Tim was shocked at something he learned on this module."},
      {"letter": "F", "text": "They were both surprised how little is known about some aspects of this."}
    ]
  },
  "question_numbers": [27, 28, 29, 30]
}
```

---

### 4.5 SUMMARY_COMPLETION

**Frequency:** Common in Reading  
**Use Case:** Fill in blanks within a summary paragraph (word from passage)

#### Content Editor Fields

```
┌────────────────────────────────────────────────────────────┐
│ SUMMARY_COMPLETION Content Editor                          │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Summary Text with Blanks                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [rich text editor]                                   │  │
│  │                                                      │  │
│  │ "The <b>1</b> ____ of London increased rapidly       │  │
│  │  between 1800 and 1850"                              │  │
│  │                                                      │  │
│  │ [Insert Blank #] [Bold] [Italic]                     │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  💡 Tips:                                                  │
│  • Use <b>1</b> ____ format for numbered blanks            │
│  • Use <strong> for bold headings                          │
│  • Use <br/> for line breaks                               │
│                                                            │
│  📋 Preview                                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ The **1** [________] of London increased rapidly     │  │
│  │ between 1800 and 1850                                │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

#### Answer Editor Fields

```
┌────────────────────────────────────────────────────────────┐
│ SUMMARY_COMPLETION Answer Editor                           │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Correct Answer (from passage)                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Answer: [population                                ] │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  Alternative Answers (optional)                            │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ [                                                  ] │  │
│  │ [+ Add Alternative]                                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  Passage Location (for explanation)                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Paragraph: [1  ] Sentence: [1  ]                     │  │
│  │ Key phrase: [London's population grew at an         │  │
│  │              astonishing rate                      ] │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

#### Sample Data Structure

```javascript
// question_content
{
  "text": "<strong>The London underground railway</strong><br/><br/><strong>The problem</strong><br/>The <strong>1</strong> ____ of London increased rapidly between 1800 and 1850"
}

// correct_answer
["population"]
```

---

## 5. Shared Components

### 5.1 RichTextEditor (sub-component)

Used across multiple content editors for formatting question text.

```
┌────────────────────────────────────────────────────────────┐
│ RichTextEditor Toolbar                                     │
├────────────────────────────────────────────────────────────┤
│ [B] [I] [U] │ [•] [1.] │ [____] │ [</>] │ [Preview]        │
│ Bold Italic  Bullets    Insert   Raw     Toggle            │
│ Underline    Numbers    Blank    HTML    Preview           │
└────────────────────────────────────────────────────────────┘

Features:
• WYSIWYG editing with HTML output
• Insert blank placeholder button
• Toggle between visual and raw HTML modes
• Support for IELTS-specific formatting (bold numbers, bullets)
```

### 5.2 OptionsListEditor (sub-component)

Used for Multiple Choice and Matching question types.

```
┌────────────────────────────────────────────────────────────┐
│ OptionsListEditor                                          │
├────────────────────────────────────────────────────────────┤
│ [A] [Option text here                            ] [🗑️] [↕] │
│ [B] [Option text here                            ] [🗑️] [↕] │
│ [C] [Option text here                            ] [🗑️] [↕] │
│                                                            │
│ [+ Add Option]                                             │
│                                                            │
│ Props:                                                     │
│ • labelType: 'letter' | 'roman' | 'number'                 │
│ • options: string[]                                        │
│ • onChange: (options) => void                              │
│ • maxOptions?: number                                      │
│ • minOptions?: number                                      │
└────────────────────────────────────────────────────────────┘
```

### 5.3 AnswerSelector (sub-component)

Used for selecting correct answers.

```
┌────────────────────────────────────────────────────────────┐
│ AnswerSelector                                             │
├────────────────────────────────────────────────────────────┤
│                                                            │
│ Mode: Single Selection                                     │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ ○ A - Option text                                      │ │
│ │ ● B - Option text (selected)                           │ │
│ │ ○ C - Option text                                      │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ Mode: Multiple Selection                                   │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ ☑ A - Option text (selected)                           │ │
│ │ ☐ B - Option text                                      │ │
│ │ ☑ C - Option text (selected)                           │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ Props:                                                     │
│ • mode: 'single' | 'multiple'                              │
│ • options: {letter: string, text: string}[]                │
│ • selected: string[]                                       │
│ • onChange: (selected) => void                             │
└────────────────────────────────────────────────────────────┘
```

---

## 6. State Management

### 6.1 Form State Structure

```typescript
interface QuestionFormState {
  // Meta fields
  questionType: QuestionType;
  questionNumber: number;  // readonly
  wordLimit: string | null;
  imageUrl: string | null;
  
  // Content (varies by type - visual representation)
  content: {
    text?: string;
    sectionTitle?: string;
    options?: OptionItem[];
  };
  
  // Answer (varies by type)
  answer: string[];
  
  // Explanation
  explanation: string | ExplanationStructure | null;
  
  // Validation
  errors: Record<string, string>;
  isDirty: boolean;
}

interface OptionItem {
  letter: string;
  text: string;
}

interface ExplanationStructure {
  summary: string;
  passageRef?: {
    paragraph: number;
    lines?: [number, number];
  };
  keyPhrase?: string;
}
```

### 6.2 Type-Specific Content Schemas

```typescript
// FILL_IN_BLANK, SUMMARY_COMPLETION, NOTE_COMPLETION
interface TextBlankContent {
  section_title?: string;
  text: string;  // contains ____ placeholder
}

// TRUE_FALSE_NOT_GIVEN, YES_NO_NOT_GIVEN
interface StatementContent {
  text: string;
}

// MULTIPLE_CHOICE, MULTIPLE_CHOICE_MULTIPLE_ANSWERS
interface MultipleChoiceContent {
  text: string;
  options: string[];  // ["A option", "B option", "C option"]
}

// MATCHING_*, SUMMARY_COMPLETION_OPTIONS
interface MatchingContent {
  text: string;
  // options come from section_layout, not question_content
}
```

---

## 7. Conversion Functions

### 7.1 Visual Form → JSON

```typescript
function contentToJson(type: QuestionType, visualContent: VisualContent): object {
  switch(type) {
    case 'FILL_IN_BLANK':
    case 'SUMMARY_COMPLETION':
      return {
        section_title: visualContent.sectionTitle || undefined,
        text: visualContent.text
      };
    
    case 'MULTIPLE_CHOICE':
      return {
        text: visualContent.text,
        options: visualContent.options.map(o => `${o.letter} ${o.text}`)
      };
    
    case 'TRUE_FALSE_NOT_GIVEN':
    case 'YES_NO_NOT_GIVEN':
    case 'MATCHING':
      return {
        text: visualContent.text
      };
    
    // ... other types
  }
}

function answerToJson(type: QuestionType, answer: string[]): string[] {
  // Most types just return the array as-is
  return answer;
}
```

### 7.2 JSON → Visual Form

```typescript
function jsonToContent(type: QuestionType, json: object): VisualContent {
  switch(type) {
    case 'MULTIPLE_CHOICE':
      return {
        text: json.text,
        options: parseOptions(json.options)  // "A option" → {letter: "A", text: "option"}
      };
    
    // ... other types
  }
}

function parseOptions(options: string[]): OptionItem[] {
  return options.map(opt => {
    const match = opt.match(/^([A-Z])\s+(.+)$/);
    return match 
      ? { letter: match[1], text: match[2] }
      : { letter: '', text: opt };
  });
}
```

---

## 8. Validation Rules

| Question Type | Content Validation | Answer Validation |
|---------------|-------------------|-------------------|
| FILL_IN_BLANK | Text must contain `____` | At least 1 answer |
| TRUE_FALSE_NOT_GIVEN | Text cannot be empty | Must be TRUE/FALSE/NOT GIVEN |
| MULTIPLE_CHOICE | At least 2 options, text required | Must match an option letter |
| MATCHING_* | Text required | Must be valid letter |
| SUMMARY_COMPLETION | Text must contain blank placeholder | At least 1 answer |

---

## 9. UI/UX Considerations

### 9.1 Layout

```
┌─────────────────────────────────────────────────────────────────────┐
│ Edit Question #7                                              [X]   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────┐  ┌──────────────────────────────┐  │
│  │ META FIELDS                 │  │ LIVE PREVIEW                 │  │
│  │                             │  │                              │  │
│  │ Type: [TRUE_FALSE_NOT_GI ▼] │  │ 7. Other countries had built │  │
│  │ Word Limit: [             ] │  │    underground railways...   │  │
│  │ Image URL: [              ] │  │                              │  │
│  │                             │  │    ○ TRUE                    │  │
│  ├─────────────────────────────┤  │    ○ FALSE                   │  │
│  │ CONTENT EDITOR              │  │    ○ NOT GIVEN               │  │
│  │                             │  │                              │  │
│  │ Statement Text:             │  │                              │  │
│  │ ┌─────────────────────────┐ │  │                              │  │
│  │ │ Other countries had...  │ │  │                              │  │
│  │ └─────────────────────────┘ │  │                              │  │
│  │                             │  │                              │  │
│  ├─────────────────────────────┤  │                              │  │
│  │ ANSWER EDITOR               │  │                              │  │
│  │                             │  │                              │  │
│  │  ○ TRUE                     │  │                              │  │
│  │  ● FALSE                    │  │                              │  │
│  │  ○ NOT GIVEN                │  │                              │  │
│  │                             │  │                              │  │
│  ├─────────────────────────────┤  │                              │  │
│  │ EXPLANATION (optional)      │  │                              │  │
│  │ ┌─────────────────────────┐ │  │                              │  │
│  │ │ The passage states...   │ │  │                              │  │
│  │ └─────────────────────────┘ │  │                              │  │
│  └─────────────────────────────┘  └──────────────────────────────┘  │
│                                                                     │
│  [Cancel]                              [Delete Question] [💾 Save]  │
└─────────────────────────────────────────────────────────────────────┘
```

### 9.2 Responsive Behavior

- **Desktop (>1024px):** Side-by-side layout (editor left, preview right)
- **Tablet (768-1024px):** Stacked with collapsible preview
- **Mobile (<768px):** Full-width stacked, preview in modal

### 9.3 Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+S` | Save question |
| `Ctrl+P` | Toggle preview |
| `Ctrl+B` | Bold text (in rich editor) |
| `Escape` | Close modal |

---

## 10. Implementation Priority

### Phase 1 (MVP)
1. ✅ QuestionTypeSelector
2. ✅ FillInBlankEditor + TextAnswerEditor
3. ✅ TrueFalseEditor + SingleSelectionAnswerEditor
4. ✅ MultipleChoiceEditor
5. ✅ Basic QuestionPreview

### Phase 2
6. MatchingEditors (all variants)
7. SummaryCompletionEditor
8. SummaryCompletionOptionsEditor
9. ExplanationEditor (structured)

### Phase 3
10. TableCompletionEditor (complex layout)
11. FlowChartCompletionEditor
12. DiagramLabelEditor
13. NoteCompletionEditor

### Phase 4
14. Batch editing support
15. AI-assisted content generation
16. Version history

---

## 11. File Structure

```
frontend/src/admin/components/content/
├── QuestionEditModal.jsx (updated - uses new components)
├── QuestionEditModal.css
│
├── editors/
│   ├── index.js (re-exports)
│   │
│   ├── meta/
│   │   ├── QuestionTypeSelector.jsx
│   │   └── QuestionMetaFields.jsx
│   │
│   ├── content/
│   │   ├── ContentEditorDispatcher.jsx
│   │   ├── FillInBlankEditor.jsx
│   │   ├── TrueFalseEditor.jsx
│   │   ├── YesNoEditor.jsx
│   │   ├── MultipleChoiceEditor.jsx
│   │   ├── MultipleChoiceMultipleEditor.jsx
│   │   ├── SummaryCompletionEditor.jsx
│   │   ├── SummaryCompletionOptionsEditor.jsx
│   │   ├── MatchingEditor.jsx (shared by all matching types)
│   │   ├── TableCompletionEditor.jsx
│   │   ├── FlowChartEditor.jsx
│   │   ├── DiagramLabelEditor.jsx
│   │   └── NoteCompletionEditor.jsx
│   │
│   ├── answer/
│   │   ├── AnswerEditorDispatcher.jsx
│   │   ├── TextAnswerEditor.jsx
│   │   ├── SingleSelectionAnswerEditor.jsx
│   │   ├── MultiSelectionAnswerEditor.jsx
│   │   └── MatchingAnswerEditor.jsx
│   │
│   ├── shared/
│   │   ├── RichTextEditor.jsx
│   │   ├── OptionsListEditor.jsx
│   │   ├── AnswerSelector.jsx
│   │   └── BlankPlaceholder.jsx
│   │
│   └── preview/
│       └── QuestionPreview.jsx
│
└── ExplanationEditor.jsx
```

---

## 12. Dependencies

### Required New Packages

```json
{
  "dependencies": {
    "react-quill": "^2.0.0",     // or "@tiptap/react" for rich text
    "dnd-kit": "^6.0.0"          // for drag-and-drop option reordering
  }
}
```

### Existing Components to Reuse

- `AdminModal.css` - Modal styling
- Question rendering components from `frontend/src/components/test/` - For preview

---

## 13. Next Steps

1. **Review this design** with stakeholders
2. **Create ticket breakdown** for implementation phases
3. **Build shared components first** (RichTextEditor, OptionsListEditor)
4. **Implement Phase 1 editors** (FILL_IN_BLANK, TRUE_FALSE, MULTIPLE_CHOICE)
5. **Integration testing** with actual question data
6. **Iterate based on admin feedback**

---

*End of Design Document*
