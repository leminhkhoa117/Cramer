# Tech Specs: ABTS - AI-Based Test Generation System

> **Phiên bản:** 1.2.0  
> **Tác giả:** Quốc Hữu - Project Manager :)  
> **Ngày tạo:** 17/12/2025  
> **Cập nhật:** 17/12/2025 - DeepSeek API Reference + Q&A Enhancements
> **Loại tài liệu:** Extension cho CRAMER_CMS_ADMIN_SPECS.md  
> **Trạng thái:** Draft - Đang trong giai đoạn lên kế hoạch

---

## Mục lục

1. [Tổng quan Dự án](#1-tổng-quan-dự-án)
2. [Nguyên tắc Thiết kế Cốt lõi](#2-nguyên-tắc-thiết-kế-cốt-lõi)
3. [Kiến trúc Hệ thống](#3-kiến-trúc-hệ-thống)
4. [Giao diện Người dùng (Admin UI)](#4-giao-diện-người-dùng-admin-ui)
5. [Module Reading Generation](#5-module-reading-generation)
6. [Module Listening Generation](#6-module-listening-generation)
7. [Module Writing Generation](#7-module-writing-generation)
8. [Module Speaking Generation](#8-module-speaking-generation)
9. [AI Prompt Engineering & Structured Output](#9-ai-prompt-engineering--structured-output)
10. [JSON Schema & Data Validation](#10-json-schema--data-validation)
11. [Hệ thống Input: Topics, Facts & Templates](#11-hệ-thống-input-topics-facts--templates)
12. [Preview & Editing Workflow](#12-preview--editing-workflow)
13. [Backend API Design](#13-backend-api-design)
14. [Xử lý Lỗi & Edge Cases](#14-xử-lý-lỗi--edge-cases)
15. [Lộ trình Triển khai](#15-lộ-trình-triển-khai)
16. [Phụ lục](#16-phụ-lục)

**Appendices:**
- [Appendix A: DeepSeek API Reference](#appendix-a-deepseek-api-reference)
- [Appendix B: Chart Styling Guidelines](#appendix-b-chart-styling-guidelines)


---

## 1. Tổng quan Dự án

### 1.1 Bối cảnh

Hiện tại, việc tạo nội dung đề thi IELTS cho Cramer CMS đang gặp các vấn đề sau:

**Vấn đề 1: Tốn thời gian và công sức**
- Mỗi bài Reading mất 2-4 giờ để soạn thủ công
- Phải viết passage (850-1000 từ), 13-14 câu hỏi, và explanations
- Cần expertise cao về IELTS format và tiêu chuẩn

**Vấn đề 2: Khó đảm bảo chất lượng đồng đều**
- Độ khó không nhất quán giữa các bài
- Thiếu kiểm tra tính chính xác về mặt nội dung (factual accuracy)
- Format câu hỏi có thể sai lệch so với IELTS chuẩn

**Vấn đề 3: Giới hạn nguồn nội dung**
- Phụ thuộc vào sách Cambridge và các nguồn có sẵn
- Khó tạo nội dung mới với chủ đề đa dạng theo yêu cầu

### 1.2 Giải pháp: ABTS (AI-Based Test Generation System)

ABTS là một **extension của Module Quản lý Nội dung** trong Cramer CMS Admin, tích hợp AI (DeepSeek V3) để hỗ trợ việc tạo nội dung đề thi IELTS.

**ABTS KHÔNG phải là:**
- Hệ thống tự động hoàn toàn (fully automated)
- Thay thế hoàn toàn cho việc soạn thủ công
- Giải pháp "one-click" không cần kiểm duyệt

**ABTS LÀ:**
- Công cụ hỗ trợ (assistive tool) cho admins
- Tăng tốc quy trình soạn đề từ 2-4 giờ xuống còn 30-60 phút
- Đảm bảo format chuẩn IELTS với structured output
- Cho phép tùy chỉnh độ khó và chủ đề theo nhu cầu

### 1.3 Phạm vi & Các Skills Được Hỗ trợ

| Skill | Trạng thái | Mô tả |
|-------|-----------|-------|
| **Reading** | ✅ Đầy đủ | Generate passages, questions (13 types), answers, explanations |
| **Listening** | ✅ Đầy đủ | Generate transcripts, questions, answers, explanations, figure descriptions |
| **Writing** | ✅ Đầy đủ | Generate task prompts, chart/graph data, detailed figure descriptions |
| **Speaking** | 📋 Placeholder | Dự kiến phát triển sau khi Speaking module hoàn thiện |

### 1.4 Đối tượng Sử dụng

- **Admin Cramer CMS:** 2 tài khoản admin cố định
- **Yêu cầu:** Hiểu biết về format IELTS, khả năng review và chỉnh sửa nội dung AI generate

### 1.5 AI Model & API

**DeepSeek V3.2 - Non-thinking Mode**

| Thông số | Giá trị |
|----------|---------|
| **Provider** | DeepSeek |
| **Model ID** | `deepseek-chat` (DeepSeek V3.2 Non-thinking Mode) |
| **Base URL** | `https://api.deepseek.com` |
| **Alternative URL** | `https://api.deepseek.com/v1` (OpenAI-compatible) |
| **Context Length** | 128K tokens |
| **Max Output** | Default 4K, Maximum 8K tokens |
| **JSON Output** | ✅ Supported (`response_format: {type: "json_object"}`) |
| **Tool Calls** | ✅ Supported |
| **Chat Prefix** | ✅ Supported |

**Pricing (per 1M tokens):**

| Loại Token | Giá (USD) | Ghi chú |
|------------|-----------|----------|
| **Input (Cache Hit)** | $0.028 | Khi prompt được cache (~10x cheaper) |
| **Input (Cache Miss)** | $0.28 | Chi phí thông thường |
| **Output** | $0.42 | Chi phí output tokens |

**Ước tính chi phí cho ABTS:**

| Use Case | Input Tokens | Output Tokens | Est. Cost |
|----------|--------------|---------------|----------|
| 1 Reading passage + 13 questions | ~3K | ~5K | ~$0.003 |
| 1 Listening part (transcript + 10 questions) | ~2.5K | ~4K | ~$0.002 |
| Writing Task 1 + Task 2 | ~2K | ~3K | ~$0.002 |

> **Lưu ý về Cache:** DeepSeek có hệ thống caching tự động. Khi cùng một prompt (hoặc prefix của prompt) được gửi nhiều lần, cache sẽ hit và chi phí input giảm 10 lần. Templates và schemas sẽ thường xuyên được cache.

> **Tham khảo:** Chi tiết API tại [Appendix A: DeepSeek API Reference](#appendix-a-deepseek-api-reference)

---

## 2. Nguyên tắc Thiết kế Cốt lõi

### 2.1 Human-in-the-Loop (Bắt buộc Can thiệp Của Con người)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    HUMAN-IN-THE-LOOP WORKFLOW                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   [Admin Input]      [AI Generation]      [Human Review]     [Publish]  │
│        │                   │                    │                │      │
│        ▼                   ▼                    ▼                ▼      │
│   ┌─────────┐        ┌─────────┐          ┌─────────┐      ┌─────────┐  │
│   │ Topics  │──────▶│ DeepSeek│────────▶│ Preview │────▶│  Save   │  │
│   │ Facts   │        │   V3    │          │ & Edit  │      │ to DB   │  │
│   │ Settings│        │   API   │          │         │      │         │  │
│   └─────────┘        └─────────┘          └─────────┘      └─────────┘  │
│        │                   │                    │                │      │
│        │              AUTOMATED            MANDATORY          MANUAL    │
│        │                                                                │
│   ════════════════════════════════════════════════════════════════════  │
│   100% Admin         AI Assists          100% Admin         Admin       │
│   Control            Generation          Verification       Decision    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Nguyên tắc này đảm bảo:**
1. AI không bao giờ publish nội dung trực tiếp
2. Admin luôn có quyền chỉnh sửa trước khi lưu
3. Mỗi item generated đều phải qua bước review

### 2.2 Factual Accuracy (Độ Chính Xác Về Mặt Nội Dung)

**Vấn đề với AI hallucination:**
- AI có thể "bịa" thông tin nghe có vẻ đúng
- Especially nguy hiểm với IELTS vì students tin tưởng đề thi

**Giải pháp của ABTS:**
1. **Facts-based Generation:** Admin cung cấp 15-25 facts/truths làm nền tảng
2. **Source Attribution:** AI được yêu cầu chỉ sử dụng facts được cung cấp
3. **Verification Prompt:** AI được instruction để flag khi cần thêm thông tin

**Ví dụ Facts Input cho Reading về "Solar Energy":**
```
FACTS:
1. Solar panels convert sunlight into electricity using photovoltaic cells
2. The first practical solar cell was invented at Bell Labs in 1954
3. China is the world's largest producer of solar panels (as of 2023)
4. Solar energy is considered renewable because the sun's energy is inexhaustible
5. The efficiency of commercial solar panels ranges from 15-22%
6. Germany was the first country to reach 50% renewable electricity in 2020
... (15-25 facts recommended)
```

### 2.3 IELTS Standard Alignment (Đúng Chuẩn IELTS)

Mọi nội dung generated phải tuân thủ:

**A. Format chuẩn:**
- Reading: 3 passages, 13-14 questions mỗi passage, tổng 40 questions
- Listening: 4 parts, 10 questions mỗi part, tổng 40 questions  
- Writing: Task 1 (150+ words, 20 mins), Task 2 (250+ words, 40 mins)

**B. Độ khó theo Band Score:**

| Level | Band Range | Vocabulary | Grammar | Topic Complexity |
|-------|------------|------------|---------|------------------|
| Beginner | 4.0 - 5.0 | Basic (2000 words) | Simple sentences | Everyday topics |
| Lower-Intermediate | 5.0 - 6.0 | Intermediate | Compound sentences | General interest |
| Intermediate | 6.0 - 7.0 | Upper-intermediate | Complex sentences | Academic intro |
| Upper-Intermediate | 7.0 - 8.0 | Advanced | Varied structures | Academic depth |
| Advanced (IELTS-like) | 8.0 - 9.0 | Sophisticated | Full range | Complex academic |

**C. Question Type Distribution:**
- Tuân thủ tỷ lệ question types như trong Cambridge IELTS tests thực tế
- Không tập trung quá nhiều vào 1 loại câu hỏi

### 2.4 Explanation Language

**Target Market:** Vietnamese learners

**Quy tắc ngôn ngữ:**
- **Test Content:** 100% English (passages, questions, answers)
- **Explanations:** Vietnamese (mặc định) hoặc English (có thể chọn)
- **Admin UI:** Vietnamese

**Ví dụ Explanation:**
```json
{
  "question_number": 1,
  "correct_answer": ["population"],
  "explanation": "Đáp án là 'population' vì đoạn văn đề cập 'the population of London grew rapidly' ở câu thứ 2 của paragraph A. Từ khóa 'grew rapidly' tương ứng với 'increased' trong câu hỏi."
}
```

---

## 3. Kiến trúc Hệ thống

### 3.1 Tổng quan Kiến trúc

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ABTS SYSTEM ARCHITECTURE                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        FRONTEND (React)                              │   │
│  ├──────────────────────────────────────────────────────────────────────┤   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │   │
│  │  │ Generation  │  │   Preview   │  │   Editor    │  │   Import/   │  │   │
│  │  │   Wizard    │  │   Panel     │  │   Forms     │  │   Export    │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                      │                                      │
│                                      ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                     BACKEND (Spring Boot)                            │   │
│  ├──────────────────────────────────────────────────────────────────────┤   │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │  │                  AdminContentController                         │ │   │
│  │  │  (existing, extended with /generate endpoints)                  │ │   │
│  │  └─────────────────────────────────────────────────────────────────┘ │   │
│  │                                │                                     │   │
│  │  ┌─────────────────────────────┼─────────────────────────────────┐   │   │
│  │  │                             ▼                                 │   │   │
│  │  │  ┌─────────────┐  ┌─────────────────┐  ┌─────────────────┐    │   │   │
│  │  │  │  ABTS       │  │   Prompt        │  │   JSON          │    │   │   │
│  │  │  │  Service    │──│   Builder       │──│   Validator     │    │   │   │
│  │  │  │             │  │   Service       │  │   Service       │    │   │   │
│  │  │  └─────────────┘  └─────────────────┘  └─────────────────┘    │   │   │
│  │  │         │                                                     │   │   │
│  │  │         ▼                                                     │   │   │
│  │  │  ┌─────────────┐  ┌─────────────────┐  ┌─────────────────┐    │   │   │
│  │  │  │  DeepSeek   │  │   Content       │  │   Template      │    │   │   │
│  │  │  │  API Client │  │   Transformer   │  │   Repository    │    │   │   │
│  │  │  │             │  │                 │  │                 │    │   │   │
│  │  │  └─────────────┘  └─────────────────┘  └─────────────────┘    │   │   │
│  │  └───────────────────────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                      │                                      │
│                                      ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        EXTERNAL SERVICES                             │   │
│  ├──────────────────────────────────────────────────────────────────────┤   │
│  │  ┌─────────────────────┐        ┌─────────────────────┐              │   │
│  │  │   DeepSeek API      │        │   Supabase          │              │   │
│  │  │   (AI Generation)   │        │   (Database)        │              │   │
│  │  └─────────────────────┘        └─────────────────────┘              │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Cấu trúc Thư mục Mới

**Frontend (`frontend/src/admin/`):**
```
admin/
├── components/
│   └── abts/                        # ABTS-specific components
│       ├── GenerationWizard/        # Multi-step generation wizard
│       │   ├── GenerationWizard.jsx
│       │   ├── StepTopicInput.jsx
│       │   ├── StepFactsInput.jsx
│       │   ├── StepConfiguration.jsx
│       │   ├── StepGenerate.jsx
│       │   └── StepPreview.jsx
│       ├── InlineAssist/            # Inline AI assist buttons
│       │   ├── AIGenerateButton.jsx
│       │   └── AIRegenerateButton.jsx
│       ├── PreviewPanel/            # Preview generated content
│       │   ├── ReadingPreview.jsx
│       │   ├── ListeningPreview.jsx
│       │   ├── WritingPreview.jsx
│       │   └── ChartRenderer.jsx    # Render structured chart data
│       └── TemplateSelector/        # Topic template selection
│           ├── TemplateSelector.jsx
│           └── TemplateCard.jsx
│
├── pages/
│   └── content/
│       └── TestEditorPage.jsx       # Extended with ABTS integration
│
├── services/
│   └── abtsApi.js                   # ABTS API client
│
└── templates/                       # Pre-defined topic templates
    └── topicTemplates.js
```

**Backend (`backend/src/main/java/com/cramer/`):**
```
├── controller/
│   └── admin/
│       └── ABTSController.java      # ABTS REST endpoints
│
├── service/
│   └── abts/
│       ├── ABTSService.java         # Main orchestration service
│       ├── PromptBuilderService.java # Build AI prompts
│       ├── DeepSeekClient.java      # DeepSeek API client
│       ├── ContentTransformer.java  # Transform AI output to DB format
│       └── JsonValidatorService.java # Validate AI JSON output
│
├── dto/
│   └── abts/
│       ├── GenerationRequestDTO.java
│       ├── GenerationResponseDTO.java
│       ├── TopicConfigDTO.java
│       └── GeneratedContentDTO.java
│
└── config/
    └── ABTSConfig.java              # ABTS configuration
```

### 3.3 Data Flow: Generation Request

```
┌──────────────────────────────────────────────────────────────────────────┐
│                      GENERATION REQUEST FLOW                             │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. ADMIN INPUT                                                          │
│     ┌─────────────────────────────────────────────────────────────────┐  │
│     │ { skill: "reading", part: 1, difficulty: "intermediate",        │  │
│     │   topic: "Renewable Energy", hashtags: ["environment", "tech"], │  │
│     │   facts: ["fact1", "fact2", ...],                               │  │
│     │   questionTypes: ["TRUE_FALSE_NOT_GIVEN", "FILL_IN_BLANK", ...],│  │
│     │   wordCount: { min: 850, max: 1000 },                           │  │
│     │   explanationLanguage: "vi" }                                   │  │
│     └─────────────────────────────────────────────────────────────────┘  │
│                                      │                                   │
│                                      ▼                                   │
│  2. PROMPT BUILDING                                                      │
│     ┌─────────────────────────────────────────────────────────────────┐  │
│     │ PromptBuilderService.buildReadingPrompt(config)                 │  │
│     │ → Combines: System prompt + Facts + Schema + Examples           │  │
│     └─────────────────────────────────────────────────────────────────┘  │
│                                      │                                   │
│                                      ▼                                   │
│  3. AI GENERATION                                                        │
│     ┌─────────────────────────────────────────────────────────────────┐  │
│     │ DeepSeekClient.generate(prompt, { response_format: "json" })    │  │
│     │ → Returns raw JSON string                                       │  │
│     └─────────────────────────────────────────────────────────────────┘  │
│                                      │                                   │
│                                      ▼                                   │
│  4. VALIDATION & TRANSFORMATION                                          │
│     ┌─────────────────────────────────────────────────────────────────┐  │
│     │ JsonValidatorService.validate(json, ReadingSchema.class)        │  │
│     │ ContentTransformer.toSectionAndQuestions(validatedJson)         │  │
│     └─────────────────────────────────────────────────────────────────┘  │
│                                      │                                   │
│                                      ▼                                   │
│  5. RESPONSE TO FRONTEND (Preview, NOT saved to DB yet)                  │
│     ┌─────────────────────────────────────────────────────────────────┐  │
│     │ { status: "success", preview: { section: {...}, questions: [...]│  │
│     │   metadata: { wordCount: 923, generationTime: 15.3s } } }       │  │
│     └─────────────────────────────────────────────────────────────────┘  │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 3.4 Generation Metadata Storage

Khi content được AI generate và save vào database, chúng ta lưu thêm metadata để hỗ trợ **reproducibility** và **audit trail**.

**Database Schema Change:**

```sql
-- Thêm column vào sections table
ALTER TABLE sections
ADD COLUMN generation_metadata JSONB DEFAULT NULL;

-- Comment
COMMENT ON COLUMN sections.generation_metadata IS 
  'Stores AI generation inputs for reproducibility. NULL if manually created.';
```

**JSONB Structure:**

```json
{
  "generated_by": "ABTS",
  "generated_at": "2025-12-17T20:00:00Z",
  "version": "1.2.0",
  
  "model_config": {
    "model": "deepseek-reasoner",
    "model_version": "v3.2",
    "temperature": 1.0,
    "max_tokens": 8192
  },
  
  "generation_config": {
    "difficulty": "intermediate",
    "band_range": "6.0-7.0",
    "word_count_target": {"min": 850, "max": 1000},
    "question_types": ["TRUE_FALSE_NOT_GIVEN", "FILL_IN_BLANK", "MATCHING_HEADINGS"],
    "explanation_language": "vi",
    "test_type": "academic"
  },
  
  "input_data": {
    "topic": "Renewable Energy and Climate Change",
    "hashtags": ["environment", "technology", "sustainability"],
    "facts": [
      "Solar panels convert sunlight into electricity using photovoltaic cells",
      "The first practical solar cell was invented at Bell Labs in 1954",
      "China is the world's largest producer of solar panels (as of 2023)"
    ]
  },
  
  "chain_of_thought": "First, I need to create a passage about solar energy... The difficulty is intermediate, so I should use academic vocabulary but avoid overly complex structures... For the TRUE_FALSE_NOT_GIVEN questions, I'll base them on facts 1, 3, and 5...",
  
  "regeneration_history": [
    {
      "timestamp": "2025-12-17T20:15:00Z",
      "action": "regenerate_questions",
      "questions_affected": [5, 7],
      "model_used": "deepseek-chat"
    }
  ]
}
```

**Benefits:**

| Feature | Purpose |
|---------|---------|
| **Reproducibility** | "Regenerate with same inputs" button |
| **Audit Trail** | Track what inputs produced what output |
| **Debugging** | View Chain of Thought to understand AI decisions |
| **Continuous Improvement** | Analyze which facts/configs produce best results |
| **Version Tracking** | Know which ABTS version generated the content |

**Query Examples:**

```sql
-- Find all AI-generated sections
SELECT id, exam_source, test_number 
FROM sections 
WHERE generation_metadata IS NOT NULL;

-- Find sections using specific model
SELECT id 
FROM sections 
WHERE generation_metadata->>'model_config'->>'model' = 'deepseek-reasoner';

-- Find sections with regeneration history
SELECT id, generation_metadata->'regeneration_history' 
FROM sections 
WHERE jsonb_array_length(generation_metadata->'regeneration_history') > 0;
```

---

## 4. Giao diện Người dùng (Admin UI)

### 4.1 Hybrid Integration Approach

ABTS được tích hợp theo 2 cách:

**A. Generation Wizard (Bulk Generation):**
- Dùng khi muốn generate toàn bộ part/skill từ đầu
- Multi-step wizard với preview
- Phù hợp cho việc tạo nội dung mới

**B. Inline AI Assist (Individual Questions):**
- Nút "✨ Generate with AI" bên cạnh mỗi field trong editor
- Dùng khi muốn regenerate/edit câu hỏi đơn lẻ
- Phù hợp cho việc tinh chỉnh nội dung

### 4.2 Generation Wizard Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    GENERATION WIZARD - 5 STEPS                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌────────┐ │
│  │  STEP 1  │─▶│  STEP 2  │─▶│  STEP 3  │─▶│  STEP 4  │─▶│ STEP 5 │ │
│  │  Scope   │   │  Topic   │   │  Config  │   │ Generate │   │ Preview│ │
│  └──────────┘   └──────────┘   └──────────┘   └──────────┘   └────────┘ │
│                                                                         │
│  ────────────────────────────────────────────────────────────────────── │
│                                                                         │
│  STEP 1: Chọn phạm vi                                                   │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ Skill: ◉ Reading  ○ Listening  ○ Writing  ○ Speaking               │ │
│  │                                                                    │ │
│  │ Scope: ○ Full Skill (all 3 passages, 40 questions)                 │ │
│  │        ◉ Single Part (Passage 1: Q1-13)                            │ │
│  │        ○ Question Group (e.g., Q1-6 only)                          │ │
│  │                                                                    │ │
│  │ Target: Part 1 (Questions 1-13)                                    │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│  ────────────────────────────────────────────────────────────────────── │
│                                                                         │
│  STEP 2: Nhập Topic & Facts                                             │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ Topic: [Renewable Energy and Climate Change]                       │ │
│  │                                                                    │ │
│  │ Template: [Environment] [Technology] [Health] [Education]          | │
│  │           [History] [Science] [Business] [Arts]                    │ │
│  │                                                                    │ │
│  │ Hashtags: #environment #technology #sustainability                 │ │
│  │                                                                    │ │
│  │ Facts (15-25 required):                                            │ │
│  │ ┌────────────────────────────────────────────────────────────────┐ │ │
│  │ │ 1. Solar panels convert sunlight into electricity using PV...  │ │ │
│  │ │ 2. The first practical solar cell was invented at Bell Labs... │ │ │
│  │ │ 3. China is the world's largest producer of solar panels...    │ │ │
│  │ │ ...                                                            │ │ │
│  │ │                                                     [17/25] ✓  │ │ │
│  │ └────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                     │ │
│  │ [📋 Load from Template: "Solar Energy Basics"]                      │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ────────────────────────────────────────────────────────────────────── │
│                                                                          │
│  STEP 3: Cấu hình Generation                                            │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ Difficulty Level:                                                   │ │
│  │   ○ Beginner (Band 4-5)                                            │ │
│  │   ○ Lower-Intermediate (Band 5-6)                                  │ │
│  │   ◉ Intermediate (Band 6-7)                                        │ │
│  │   ○ Upper-Intermediate (Band 7-8)                                  │ │
│  │   ○ Advanced/IELTS-like (Band 8-9)                                 │ │
│  │                                                                     │ │
│  │ Question Types to Include:                                          │ │
│  │   ☑ TRUE_FALSE_NOT_GIVEN (4-5 questions)                           │ │
│  │   ☑ FILL_IN_BLANK (3-4 questions)                                  │ │
│  │   ☑ MATCHING_HEADINGS (5-6 questions)                              │ │
│  │   ☐ MULTIPLE_CHOICE                                                │ │
│  │                                                                     │ │
│  │ Word Count: Min [850] - Max [1000]                                 │ │
│  │                                                                     │ │
│  │ Explanation Language: ◉ Tiếng Việt  ○ English                      │ │
│  │                                                                     │ │
│  │ Test Type: ◉ Academic  ○ General Training                          │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ────────────────────────────────────────────────────────────────────── │
│                                                                          │
│  STEP 4: Generating...                                                   │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                                                                     │ │
│  │                    ⏳ Đang tạo nội dung...                          │ │
│  │                                                                     │ │
│  │     ████████████████████░░░░░░░░░░░░░░░░  65%                      │ │
│  │                                                                     │ │
│  │     ✓ Passage generated (923 words)                                │ │
│  │     ✓ Questions 1-6 generated (TRUE_FALSE_NOT_GIVEN)               │ │
│  │     ⏳ Generating questions 7-10 (FILL_IN_BLANK)...                 │ │
│  │     ○ Questions 11-13 pending (MATCHING_HEADINGS)                  │ │
│  │                                                                     │ │
│  │     Estimated time remaining: ~12 seconds                          │ │
│  │                                                                     │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ────────────────────────────────────────────────────────────────────── │
│                                                                          │
│  STEP 5: Preview & Edit                                                  │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ ┌──────────────────────────┐ ┌──────────────────────────────────┐ │ │
│  │ │      PASSAGE PREVIEW     │ │       QUESTIONS PREVIEW          │ │ │
│  │ │                          │ │                                  │ │ │
│  │ │ Word Count: 923 ✓        │ │ Q1. TRUE_FALSE_NOT_GIVEN         │ │ │
│  │ │                          │ │     "Solar panels were first..." │ │ │
│  │ │ The development of solar │ │     Answer: FALSE                │ │ │
│  │ │ energy has transformed   │ │     [🔄 Regenerate] [✏️ Edit]    │ │ │
│  │ │ the global energy land...│ │                                  │ │ │
│  │ │                          │ │ Q2. TRUE_FALSE_NOT_GIVEN         │ │ │
│  │ │ <A> In recent decades... │ │     "China currently leads..."   │ │ │
│  │ │ <B> The technology has...│ │     Answer: TRUE                 │ │ │
│  │ │ <C> However, challenges..│ │     [🔄 Regenerate] [✏️ Edit]    │ │ │
│  │ │                          │ │                                  │ │ │
│  │ │ [✏️ Edit Passage]        │ │ ... (13 questions total)         │ │ │
│  │ └──────────────────────────┘ └──────────────────────────────────┘ │ │
│  │                                                                     │ │
│  │ [🔄 Regenerate All]  [💾 Save to Editor]  [❌ Discard]              │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.3 Inline AI Assist Integration

Trong Question Editor hiện tại, thêm nút AI assist:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    QUESTION EDITOR WITH AI ASSIST                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Question 5: TRUE_FALSE_NOT_GIVEN                                        │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ Statement:                                                          │ │
│  │ ┌────────────────────────────────────────────────────────────────┐ │ │
│  │ │ The efficiency of solar panels has doubled since 2010.         │ │ │
│  │ └────────────────────────────────────────────────────────────────┘ │ │
│  │                                              [✨ AI Suggest]       │ │
│  │                                                                     │ │
│  │ Correct Answer: ◉ TRUE  ○ FALSE  ○ NOT GIVEN                       │ │
│  │                                                                     │ │
│  │ Explanation (Vietnamese):                                           │ │
│  │ ┌────────────────────────────────────────────────────────────────┐ │ │
│  │ │ Đáp án là TRUE vì đoạn văn nhắc đến...                         │ │ │
│  │ │                                                                 │ │ │
│  │ │                                                                 │ │ │
│  │ └────────────────────────────────────────────────────────────────┘ │ │
│  │                                 [✨ AI Generate Explanation]        │ │
│  │                                                                     │ │
│  │ [🔄 Regenerate This Question]  [💾 Save Changes]                   │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.4 Selective Regeneration

Admin có thể regenerate các phần cụ thể mà không ảnh hưởng phần khác:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    SELECTIVE REGENERATION OPTIONS                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Questions 1-13 for Part 1                                               │
│                                                                          │
│  ☑ Q1  TRUE_FALSE_NOT_GIVEN     ✓ Generated                            │
│  ☑ Q2  TRUE_FALSE_NOT_GIVEN     ✓ Generated                            │
│  ☐ Q3  TRUE_FALSE_NOT_GIVEN     ✓ Generated  ← Keep this               │
│  ☐ Q4  TRUE_FALSE_NOT_GIVEN     ✓ Generated  ← Keep this               │
│  ☑ Q5  TRUE_FALSE_NOT_GIVEN     ⚠️ Needs review  ← Regenerate          │
│  ☑ Q6  FILL_IN_BLANK            ✓ Generated                            │
│  ☐ Q7  FILL_IN_BLANK            ✓ Generated  ← Keep this               │
│  ...                                                                     │
│                                                                          │
│  Selected: 4 questions                                                   │
│                                                                          │
│  [🔄 Regenerate Selected (4)]  [Select All]  [Deselect All]             │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Module Reading Generation

### 5.1 Tổng quan Reading

| Thông số | Giá trị |
|----------|---------|
| **Số parts** | 3 (Passage 1, 2, 3) |
| **Số câu hỏi/part** | 13-14 |
| **Tổng câu hỏi** | 40 |
| **Thời gian thi** | 60 phút |
| **Word count/passage** | 850-1000 từ |
| **Loại test** | Academic (primary), General Training (future) |

### 5.2 Generation Scope Options

Admin có thể chọn generate ở 3 mức độ:

**A. Full Skill (All 3 passages):**
- Generate 3 passages liên tiếp
- Tổng 40 questions
- Thời gian generation: ~45-60 giây

**B. Single Part (1 passage):**
- Generate 1 passage với 13-14 questions
- Thời gian generation: ~15-20 giây

**C. Question Group:**
- Generate một nhóm câu hỏi cụ thể (e.g., Q1-6)
- Dựa trên passage đã có sẵn
- Thời gian generation: ~5-10 giây

### 5.3 Supported Question Types (13 Types)

| Question Type | Số câu thường gặp | Mô tả |
|---------------|-------------------|-------|
| `TRUE_FALSE_NOT_GIVEN` | 4-6 | Xác định tính đúng/sai của statement |
| `YES_NO_NOT_GIVEN` | 4-6 | Cho opinion-based passages |
| `FILL_IN_BLANK` | 3-5 | Điền từ vào chỗ trống |
| `SUMMARY_COMPLETION` | 4-6 | Điền từ vào summary |
| `SUMMARY_COMPLETION_OPTIONS` | 4-6 | Chọn từ từ word bank |
| `MATCHING_HEADINGS` | 5-8 | Match heading với paragraph |
| `MATCHING_INFORMATION` | 4-6 | Match statement với paragraph |
| `MATCHING_FEATURES` | 4-6 | Match items với features |
| `MATCHING_SENTENCE_ENDINGS` | 4-6 | Complete sentences |
| `MULTIPLE_CHOICE` | 3-4 | Single answer |
| `MULTIPLE_CHOICE_MULTIPLE_ANSWERS` | 2-3 | Multiple answers |
| `TABLE_COMPLETION` | 4-6 | Fill table blanks |
| `DIAGRAM_LABEL_COMPLETION` | 4-6 | Label diagram parts |

### 5.4 AI Output JSON Schema (Reading)

AI sẽ output JSON theo format có thể map trực tiếp vào database:

```json
{
  "section": {
    "passage_text": "<strong>The Future of Solar Energy</strong>\n\n<strong>A.</strong> In recent decades, the development of solar energy...\n\n<strong>B.</strong> The technology behind solar panels...",
    "word_count": 923
  },
  "questions": [
    {
      "question_number": 1,
      "question_type": "TRUE_FALSE_NOT_GIVEN",
      "question_content": {
        "text": "Solar panels were first developed in the 1950s."
      },
      "correct_answer": ["TRUE"],
      "explanation": "Đáp án là TRUE vì paragraph A đề cập 'the first practical solar cell was invented at Bell Labs in 1954', xác nhận solar panels được phát triển trong thập niên 1950.",
      "word_limit": null
    },
    {
      "question_number": 2,
      "question_type": "FILL_IN_BLANK",
      "question_content": {
        "text": "The efficiency of commercial solar panels ranges from 15% to ____."
      },
      "correct_answer": ["22%", "22 percent"],
      "explanation": "Đáp án là '22%' được nêu rõ trong paragraph B: 'commercial solar panels achieve efficiencies ranging from 15 to 22 percent'.",
      "word_limit": "ONE WORD AND/OR A NUMBER"
    },
    {
      "question_number": 3,
      "question_type": "MATCHING_HEADINGS",
      "question_content": {
        "text": "Paragraph C",
        "options": [
          { "letter": "i", "text": "Historical development of solar technology" },
          { "letter": "ii", "text": "Current challenges facing the industry" },
          { "letter": "iii", "text": "Government policies supporting renewable energy" },
          { "letter": "iv", "text": "Comparison with other energy sources" },
          { "letter": "v", "text": "Future predictions for solar power adoption" }
        ]
      },
      "correct_answer": ["ii"],
      "explanation": "Paragraph C thảo luận về 'challenges such as storage capacity and weather dependency', matching với heading 'Current challenges facing the industry'.",
      "word_limit": null
    }
  ],
  "metadata": {
    "topic": "Renewable Energy",
    "difficulty": "intermediate",
    "band_range": "6.0-7.0",
    "generated_at": "2025-12-17T16:00:00Z"
  }
}
```

### 5.5 Passage Formatting Rules

**HTML Tags được phép trong `passage_text`:**
- `<strong>Title</strong>` - Tiêu đề passage
- `<strong>A.</strong>` - Paragraph markers
- `\n\n` - Paragraph separation
- `''` (escaped single quotes)

**Ví dụ passage được format đúng:**
```html
<strong>The Evolution of Urban Transportation</strong>

<strong>A.</strong> In the early 19th century, cities around the world faced a common challenge: how to move growing populations efficiently across expanding urban landscapes. The horse-drawn omnibus, introduced in Paris in 1828, was among the first attempts to address this need...

<strong>B.</strong> The advent of steam power brought significant changes to urban transportation. In 1863, London opened the world''s first underground railway, the Metropolitan Railway, which used steam locomotives to transport passengers beneath the city streets...
```

### 5.6 Word Count Verification

AI được yêu cầu verify word count trong output:

```json
{
  "section": {
    "passage_text": "...",
    "word_count": 923,
    "word_count_valid": true,
    "word_count_message": "Word count (923) is within target range (850-1000)"
  }
}
```

**Validation rules:**
- Minimum: 850 words
- Maximum: 1000 words
- Frontend hiển thị warning nếu ngoài range

---

## 6. Module Listening Generation

### 6.1 Tổng quan Listening

| Thông số | Giá trị |
|----------|---------|
| **Số parts** | 4 |
| **Số câu hỏi/part** | 10 |
| **Tổng câu hỏi** | 40 |
| **Thời gian thi** | ~30 phút |
| **Output chính** | Transcript + Questions + Figure Descriptions |

### 6.2 Word Count theo Part

| Part | Word Count (Transcript) | Speakers | Context |
|------|------------------------|----------|---------|
| Part 1 | 400-500 | 2 people | Daily/social conversation |
| Part 2 | 550-700 | 1 person | Monologue on social topic |
| Part 3 | 500-700 | 2-4 people | Academic discussion |
| Part 4 | 750-900 | 1 person | Academic lecture |

### 6.3 Supported Question Types (Listening)

| Question Type | Usage | Mô tả |
|---------------|-------|-------|
| `FILL_IN_BLANK` | Common | Note/form/table completion |
| `MULTIPLE_CHOICE` | Common | Single answer |
| `MULTIPLE_CHOICE_MULTIPLE_ANSWERS` | Occasional | Multiple answers |
| `MATCHING` | Common | Dropdown matching |

### 6.4 Section Layout & Block Types

Theo `DATA_INGESTION_GUIDE_LISTENING.md`, mỗi part có `section_layout` với các block types:

| Block Type | Description | Usage |
|------------|-------------|-------|
| `INSTRUCTIONS_ONLY` | Instructions và title | Multiple choice groups |
| `NOTE_COMPLETION` | Notes với blanks | Form/note completion |
| `MATCHING_FEATURES` | Matching với options | Matching questions |
| `PLAN_MAP_DIAGRAM_LABELING` | Labeling với image | Map/diagram questions |

### 6.5 Figure/Image Description Generation

Khi Listening part có hình ảnh (maps, plans, diagrams), AI generate **extremely detailed description** để admin có thể recreate:

```json
{
  "figure_description": {
    "type": "floor_plan",
    "title": "University Library Ground Floor",
    "overall_description": "A rectangular floor plan approximately 40m x 30m, with the main entrance on the south side. The building is divided into distinct functional zones.",
    "elements": [
      {
        "label": "A",
        "name": "Main Entrance",
        "position": "center-south",
        "description": "Double glass doors, 3m wide, with accessibility ramp on the left side. A welcome desk is positioned 2m inside the doors."
      },
      {
        "label": "B", 
        "name": "Information Desk",
        "position": "5m north of entrance",
        "description": "Circular counter, approximately 4m diameter, with 3 computer terminals visible. Staff area behind counter."
      },
      {
        "label": "C",
        "name": "Study Area",
        "position": "northeast quadrant",
        "description": "Open area with 20 individual study desks arranged in 4 rows of 5. Each desk has a reading lamp. Windows on north and east walls."
      },
      {
        "label": "D",
        "name": "Computer Lab",
        "position": "northwest quadrant",
        "description": "Enclosed room with glass walls. Contains 30 computer workstations in 5 rows. Printer station in far corner."
      }
    ],
    "navigation": "Visitors entering from main entrance can go straight to reach Information Desk, turn left for Computer Lab, or turn right for Study Area.",
    "scale": "Approximate scale: 1cm = 2m",
    "recreation_notes": [
      "Use rounded corners for the information desk",
      "Study desks should be represented as small rectangles",
      "Add dotted lines to show suggested walking paths",
      "Windows should be indicated with thinner lines on outer walls"
    ]
  }
}
```

### 6.6 Transcript Formatting

**Speaker annotation format:**
```
SARAH: Good morning! I'm calling about the room available for rent.

LANDLORD: Ah yes, the room on Maple Street. Let me tell you about it.

SARAH: Great. First, could you tell me about the size?

LANDLORD: It's quite spacious - about 15 square metres. There's a single bed, a desk, and a built-in wardrobe.
```

**Part 4 (Single speaker/lecture) format:**
```
LECTURER: Today we're going to examine the history of labyrinths and their significance in various cultures throughout history.

Let me begin with a definition. A labyrinth, unlike a maze, has only one winding path leading to a centre. There are no dead ends or choices to make - you simply follow the path.

The oldest known labyrinth design dates back approximately 4,000 years...
```

### 6.7 Audio Placeholder

Vì audio được record/obtain separately, AI generate placeholder:

```json
{
  "audio_placeholder": {
    "duration_estimate": "6 minutes 30 seconds",
    "speaker_count": 2,
    "speaker_genders": ["female", "male"],
    "accent_recommendation": "British or Australian English, clear pronunciation",
    "pacing_notes": "Moderate pace with natural pauses between topic transitions. Slightly slower for numbers and specific terms.",
    "background_ambient": "Quiet indoor setting with occasional paper shuffling"
  }
}
```

### 6.8 AI Output JSON Schema (Listening Part)

```json
{
  "section": {
    "part_number": 1,
    "audio_url": null,
    "passage_text": "SARAH: Good morning! I'm calling about...\n\nLANDLORD: Ah yes, the room on...",
    "word_count": 467,
    "section_layout": {
      "blocks": [
        {
          "block_type": "NOTE_COMPLETION",
          "content": {
            "title": "Questions 1-10",
            "instructions_text": "<b>Room Rental Enquiry</b><br/><br/>Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.",
            "main_title": "Room Details"
          },
          "question_numbers": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        }
      ]
    }
  },
  "questions": [
    {
      "question_number": 1,
      "question_type": "FILL_IN_BLANK",
      "question_content": {
        "section_title": "Room Information",
        "text": "• Room size: 1 ____ square metres"
      },
      "correct_answer": ["15"],
      "explanation": "Landlord nói rõ 'It's quite spacious - about 15 square metres' khi mô tả kích thước phòng.",
      "word_limit": "ONE WORD AND/OR A NUMBER"
    }
  ],
  "audio_placeholder": {
    "duration_estimate": "5 minutes",
    "speaker_count": 2
  },
  "figure_description": null,
  "metadata": {
    "part_context": "daily_social",
    "topic": "Room Rental",
    "difficulty": "intermediate"
  }
}
```

---

## 7. Module Writing Generation

### 7.1 Tổng quan Writing

| Thông số | Giá trị |
|----------|---------|
| **Số tasks** | 2 |
| **Task 1** | 150+ từ, 20 phút recommended |
| **Task 2** | 250+ từ, 40 phút recommended |
| **Thời gian thi** | 60 phút tổng |

### 7.2 Writing Task Types

**Academic Task 1 - Chart/Graph Types (EXTREME DIVERSITY):**

AI sẽ random chọn từ library đa dạng các loại chart:

| Category | Chart Types | Weight |
|----------|-------------|--------|
| **Bar Charts** | Vertical, Horizontal, Stacked, Grouped, 100% Stacked | 25% |
| **Line Charts** | Single line, Multiple lines, Area chart, Step line | 20% |
| **Pie Charts** | Standard pie, Donut, Semi-circle | 10% |
| **Tables** | Comparison table, Data table with trends | 10% |
| **Process Diagrams** | Linear flow, Cyclic process, Branching flow | 12% |
| **Maps** | Town development (before/after), Building floor plan | 8% |
| **Combined Charts** | Bar + Line, Dual axis, Multiple panels | 10% |
| **Special** | Sankey diagram, Treemap, Timeline | 5% |

**Chart Type Distribution (Weighted Random):**

```javascript
// chartTypeDistribution.js
export const CHART_TYPE_WEIGHTS = {
  // BAR CHARTS (25%)
  "bar_vertical": 7,
  "bar_horizontal": 5,
  "bar_grouped": 7,
  "bar_stacked": 4,
  "bar_100_stacked": 2,
  
  // LINE CHARTS (20%)
  "line_single": 5,
  "line_multiple": 8,
  "line_area": 4,
  "line_step": 3,
  
  // PIE CHARTS (10%)
  "pie_standard": 5,
  "pie_donut": 3,
  "pie_semi": 2,
  
  // TABLES (10%)
  "table_comparison": 5,
  "table_data_trends": 5,
  
  // PROCESS DIAGRAMS (12%)
  "process_linear": 5,
  "process_cyclic": 4,
  "process_branching": 3,
  
  // MAPS (8%)
  "map_town_development": 5,
  "map_floor_plan": 3,
  
  // COMBINED (10%)
  "combined_bar_line": 5,
  "combined_dual_axis": 3,
  "combined_multi_panel": 2,
  
  // SPECIAL (5%)
  "sankey": 2,
  "treemap": 2,
  "timeline": 1
};

export function getRandomChartType() {
  const totalWeight = Object.values(CHART_TYPE_WEIGHTS).reduce((a, b) => a + b, 0);
  let random = Math.random() * totalWeight;
  
  for (const [type, weight] of Object.entries(CHART_TYPE_WEIGHTS)) {
    random -= weight;
    if (random <= 0) return type;
  }
  return "bar_grouped"; // fallback
}
```

**Academic Task 2:**
- Opinion essay
- Discussion essay
- Problem-solution essay
- Advantages-disadvantages essay
- Two-part question essay

**General Training Task 1 (Future):**
- Formal letter
- Semi-formal letter
- Informal letter

### 7.3 Task 1: Chart/Graph Data Generation

AI generate **structured data** có thể được render thành chart:

```json
{
  "task1": {
    "task_type": "bar_chart",
    "task_text": "<p>The chart below shows the number of electric vehicles sold in four countries between 2015 and 2023.</p>\n\n<p>Summarise the information by selecting and reporting the main features, and make comparisons where relevant.</p>\n\n<p><em>Write at least 150 words.</em></p>",
    
    "chart_data": {
      "chart_type": "grouped_bar",
      "title": "Electric Vehicle Sales (2015-2023)",
      "x_axis": {
        "label": "Year",
        "values": ["2015", "2017", "2019", "2021", "2023"]
      },
      "y_axis": {
        "label": "Number of vehicles (thousands)",
        "min": 0,
        "max": 800
      },
      "series": [
        {
          "name": "China",
          "color": "#e74c3c",
          "values": [120, 250, 380, 520, 720]
        },
        {
          "name": "USA",
          "color": "#3498db",
          "values": [100, 180, 270, 380, 450]
        },
        {
          "name": "Germany",
          "color": "#2ecc71",
          "values": [45, 95, 150, 220, 310]
        },
        {
          "name": "UK",
          "color": "#9b59b6",
          "values": [30, 60, 110, 170, 260]
        }
      ],
      "legend_position": "bottom"
    },
    
    "detailed_description": {
      "overview": "A grouped bar chart comparing electric vehicle sales across China, USA, Germany, and UK over five data points from 2015 to 2023. China consistently leads in sales volume, followed by USA, Germany, and UK.",
      "visual_elements": [
        "X-axis shows years at 2-year intervals: 2015, 2017, 2019, 2021, 2023",
        "Y-axis ranges from 0 to 800 thousand vehicles",
        "Four different colored bars for each year group",
        "Legend at bottom identifies each country by color"
      ],
      "key_trends": [
        "All countries show steady growth throughout the period",
        "China shows most dramatic increase (6x growth from 120k to 720k)",
        "Gap between China and other countries widens over time",
        "UK starts lowest but shows strong proportional growth"
      ],
      "recreation_notes": [
        "Ensure bars are clearly separated within each year group",
        "Use distinct colors that work for colorblind readers",
        "Grid lines on y-axis at 200k intervals",
        "Include exact values above each bar (optional)"
      ]
    }
  }
}
```

### 7.4 Task 1: Process Diagram/Map

Cho process diagrams và maps, AI generate detailed textual description:

```json
{
  "task1": {
    "task_type": "process_diagram",
    "task_text": "<p>The diagram below shows the process of recycling plastic bottles.</p>\n\n<p>Summarise the information by selecting and reporting the main features, and make comparisons where relevant.</p>\n\n<p><em>Write at least 150 words.</em></p>",
    
    "chart_data": null,
    
    "detailed_description": {
      "type": "linear_process",
      "title": "Plastic Bottle Recycling Process",
      "total_stages": 8,
      "stages": [
        {
          "number": 1,
          "name": "Collection",
          "description": "Plastic bottles are collected from households in recycling bins",
          "icon_suggestion": "Recycling bin with bottles",
          "arrow_to_next": true
        },
        {
          "number": 2,
          "name": "Sorting",
          "description": "Bottles are sorted by plastic type (PET, HDPE, etc.) at a facility",
          "icon_suggestion": "Conveyor belt with sorting mechanism",
          "arrow_to_next": true
        },
        {
          "number": 3,
          "name": "Cleaning",
          "description": "Sorted bottles are washed to remove labels, caps, and residue",
          "icon_suggestion": "Water spray cleaning station",
          "arrow_to_next": true
        },
        {
          "number": 4,
          "name": "Shredding",
          "description": "Clean bottles are shredded into small flakes",
          "icon_suggestion": "Industrial shredder machine",
          "arrow_to_next": true
        },
        {
          "number": 5,
          "name": "Separation",
          "description": "Flakes are separated by density in a water tank",
          "icon_suggestion": "Floatation tank with separated layers",
          "arrow_to_next": true
        },
        {
          "number": 6,
          "name": "Drying",
          "description": "Separated flakes are dried using hot air",
          "icon_suggestion": "Drying chamber with heat symbols",
          "arrow_to_next": true
        },
        {
          "number": 7,
          "name": "Melting",
          "description": "Dried flakes are melted and formed into pellets",
          "icon_suggestion": "Melting vat and pellet extruder",
          "arrow_to_next": true
        },
        {
          "number": 8,
          "name": "Manufacturing",
          "description": "Pellets are sold to manufacturers who create new plastic products",
          "icon_suggestion": "Factory with new products",
          "arrow_to_next": false
        }
      ],
      "layout_suggestion": "Horizontal flow from left to right, or circular with stages around the perimeter",
      "color_scheme": "Use green tones to emphasize eco-friendly process",
      "recreation_notes": [
        "Arrows should flow clearly between stages",
        "Include small icons for each stage",
        "Number each stage clearly",
        "Consider adding a small image of input (dirty bottles) and output (new products)"
      ]
    }
  }
}
```

### 7.5 Task 2: Essay Question Generation

```json
{
  "task2": {
    "essay_type": "opinion",
    "task_text": "<p>Some people believe that universities should focus on providing academic knowledge, while others think they should prepare students for practical work skills.</p>\n\n<p><strong>Discuss both views and give your own opinion.</strong></p>\n\n<p>Give reasons for your answer and include any relevant examples from your own knowledge or experience.</p>\n\n<p><em>Write at least 250 words.</em></p>",
    
    "topic_tags": ["education", "university", "employment"],
    
    "band_descriptors_focus": {
      "task_response": "Address both views clearly before stating own opinion",
      "coherence_cohesion": "Use clear paragraph structure with topic sentences",
      "lexical_resource": "Use education and employment vocabulary",
      "grammatical_range": "Mix simple and complex sentences effectively"
    },
    
    "sample_ideas": {
      "view_1_academic": [
        "Deep understanding of theoretical concepts",
        "Critical thinking and research skills",
        "Foundation for further academic study"
      ],
      "view_2_practical": [
        "Employability and job readiness",
        "Real-world problem solving",
        "Internship and work experience programs"
      ],
      "balanced_perspective": [
        "Universities should combine both approaches",
        "Different fields may require different emphasis",
        "Transferable skills bridge theory and practice"
      ]
    }
  }
}
```

### 7.6 Chart Rendering trong Preview

Frontend sử dụng `chart_data` để render chart preview:

```jsx
// ChartRenderer.jsx - Pseudo-code
import { Chart } from 'react-chartjs-2';

function ChartRenderer({ chartData }) {
  if (chartData.chart_type === 'grouped_bar') {
    return (
      <Chart
        type="bar"
        data={{
          labels: chartData.x_axis.values,
          datasets: chartData.series.map(s => ({
            label: s.name,
            data: s.values,
            backgroundColor: s.color
          }))
        }}
        options={{
          scales: {
            y: {
              title: { text: chartData.y_axis.label },
              min: chartData.y_axis.min,
              max: chartData.y_axis.max
            }
          }
        }}
      />
    );
  }
  // ... handle other chart types
}
```

**Lợi ích của structured chart data:**
1. Admin thấy preview giống như test thực
2. Có thể export chart as image
3. Dễ dàng chỉnh sửa giá trị data
4. Consistency trong format

### 7.7 Chart Rasterization (Export to Image)

Sau khi preview, admin có thể export chart thành image để sử dụng trong test thực:

**Rasterization Pipeline:**

```
┌──────────────────────────────────────────────────────────────────────────┐
│                      CHART RASTERIZATION PIPELINE                        │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  [AI generates chart_data JSON]                                          │
│              │                                                           │
│              ▼                                                           │
│  [ChartRenderer.jsx renders with Chart.js]                               │
│              │                                                           │
│              ▼                                                           │
│  [Admin previews & adjusts data if needed]                               │
│              │                                                           │
│              ▼                                                           │
│  [Click "Export as Image"]                                               │
│              │                                                           │
│              ▼                                                           │
│  [Canvas.toDataURL() → PNG blob]                                         │
│              │                                                           │
│              ▼                                                           │
│  [Upload to Supabase Storage]                                            │
│              │                                                           │
│              ▼                                                           │
│  [Store URL in section metadata]                                         │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

**Implementation:**

```javascript
// chartExport.js
export async function exportChartAsImage(chartRef, filename) {
  const canvas = chartRef.current.canvas;
  
  // Get high-resolution export (2x for retina)
  const scaleFactor = 2;
  const exportCanvas = document.createElement('canvas');
  exportCanvas.width = canvas.width * scaleFactor;
  exportCanvas.height = canvas.height * scaleFactor;
  
  const ctx = exportCanvas.getContext('2d');
  ctx.scale(scaleFactor, scaleFactor);
  ctx.drawImage(canvas, 0, 0);
  
  // Convert to blob
  const blob = await new Promise(resolve => 
    exportCanvas.toBlob(resolve, 'image/png', 1.0)
  );
  
  // Upload to Supabase Storage
  const { data, error } = await supabase.storage
    .from('chart-images')
    .upload(`writing/${filename}.png`, blob, {
      contentType: 'image/png',
      upsert: true
    });
  
  if (error) throw error;
  
  // Get public URL
  const { data: urlData } = supabase.storage
    .from('chart-images')
    .getPublicUrl(`writing/${filename}.png`);
  
  return urlData.publicUrl;
}
```

**Supported Export Formats:**

| Format | Use Case | Implementation |
|--------|----------|----------------|
| **PNG** | Standard charts (bar, line, pie) | `canvas.toDataURL('image/png')` |
| **SVG** | Process diagrams, maps | Export from D3.js or custom SVG |
| **PDF** | Print-ready | jsPDF + canvas |

### 7.8 Chart Styling Guidelines

> **IMPORTANT:** Charts phải đồng nhất với thiết kế chung của Cramer web.
> Tham khảo `docs/frontend/UI_GUIDELINES.md` cho design tokens.

**Chart Theme Integration:**

```javascript
// chartTheme.js - Based on UI_GUIDELINES.md
export const CHART_THEME = {
  // Primary colors from Cramer design system
  colors: {
    primary: '#7c3aed',      // Primary Accent
    primaryHover: '#6d28d9', // Primary Hover
    secondary: '#6366f1',    // Gradient end
  },
  
  // Chart-specific color palettes (diverse but harmonious)
  dataPalettes: {
    // For multi-series charts - each palette is cohesive
    warm: ['#e74c3c', '#f39c12', '#e67e22', '#d35400', '#c0392b'],
    cool: ['#3498db', '#2980b9', '#1abc9c', '#16a085', '#27ae60'],
    purple: ['#9b59b6', '#8e44ad', '#6c3483', '#5b2c6f', '#4a235a'],
    mixed: ['#e74c3c', '#3498db', '#2ecc71', '#9b59b6', '#f39c12', '#1abc9c'],
  },
  
  // Typography (Be Vietnam Pro from UI Guidelines)
  font: {
    family: "'Be Vietnam Pro', sans-serif",
    titleSize: 16,
    labelSize: 12,
    legendSize: 11,
  },
  
  // Styling
  borderRadius: 8,
  gridColor: 'rgba(0, 0, 0, 0.1)',
  backgroundColor: 'white',
  
  // Glassmorphism effects for chart container
  container: {
    background: 'rgba(255, 255, 255, 0.95)',
    backdropFilter: 'blur(10px)',
    border: '1px solid rgba(255, 255, 255, 0.18)',
    borderRadius: '12px',
    boxShadow: '0 10px 25px rgba(0, 0, 0, 0.1)',
  }
};

// Apply theme to Chart.js
Chart.defaults.font.family = CHART_THEME.font.family;
Chart.defaults.font.size = CHART_THEME.font.labelSize;
Chart.defaults.plugins.legend.labels.font.size = CHART_THEME.font.legendSize;
```

**Color Palette Selection:**

AI chọn palette dựa trên topic của Writing task:

| Topic Category | Suggested Palette | Example |
|----------------|-------------------|---------|
| Environment, Nature | Cool (blues, greens) | Climate data, emissions |
| Economy, Finance | Warm (reds, oranges) | GDP, inflation |
| Technology, Science | Purple | Innovation, research |
| Comparison (>3 series) | Mixed | Country comparisons |

**Process Diagram & Map Styling:**

```javascript
// For non-Chart.js visuals (using D3.js or custom SVG)
export const PROCESS_DIAGRAM_STYLE = {
  nodeRadius: 40,
  nodeColor: '#7c3aed',      // Primary accent
  arrowColor: '#6366f1',     // Secondary
  labelColor: '#1f2937',     // Dark text
  connectorWidth: 3,
  animation: {
    duration: 300,
    easing: 'ease-in-out',
  }
};
```

---

## 8. Module Speaking Generation

> **⚠️ PLACEHOLDER SECTION**
> 
> Module Speaking đang trong giai đoạn phát triển. Section này sẽ được cập nhật khi Speaking module của Cramer hoàn thiện.

### 8.1 Cấu trúc Speaking Test (Dự kiến)

| Part | Thời gian | Nội dung |
|------|-----------|----------|
| Part 1 | 4-5 phút | Introduction & Interview (12-15 questions) |
| Part 2 | 3-4 phút | Individual Long Turn (Cue Card) |
| Part 3 | 4-5 phút | Discussion (4-6 questions) |

### 8.2 Input Method: Keywords/Idea Outline (Speaking-specific)

**Khác biệt với Reading/Listening:**

| Skill | Input Method | Rationale |
|-------|--------------|-----------|
| Reading | **Facts** (verifiable, specific) | Cần factual accuracy |
| Listening | **Facts** (verifiable, specific) | Cần factual accuracy |
| Speaking | **Keywords/Idea Outline** (flexible) | Cần creative storytelling |

**Tại sao Speaking không dùng Facts?**

Speaking Part 2 (Cue Card) yêu cầu thí sinh **kể câu chuyện cá nhân** hoặc **mô tả trải nghiệm**. AI cần tự do sáng tạo nội dung phù hợp, không bị ràng buộc bởi facts cứng.

**Input Structure cho Speaking:**

```json
{
  "input_type": "keywords_outline",
  
  "topic": "Describe a memorable journey you have taken",
  
  "keywords": [
    "train", "countryside", "unexpected delay", 
    "kind stranger", "sunset", "hometown"
  ],
  
  "idea_outline": [
    "Where: From Hanoi to Hue by train",
    "When: Last summer, during monsoon season",
    "What happened: Train delayed, met interesting co-passenger",
    "Feelings: Initially frustrated, then grateful"
  ],
  
  "vocabulary_hints": [
    "scenic", "picturesque", "coincidence", 
    "hospitality", "memorable", "unexpected"
  ],
  
  "tone": "nostalgic, reflective",
  
  "difficulty": "intermediate"
}
```

**UI cho Speaking Input:**

```
┌────────────────────────────────────────────────────────────────────────┐
│  SPEAKING PART 2 - INPUT                                                │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Main Topic:                                                            │
│  ┌────────────────────────────────────────────────────────────────────┐│
│  │ Describe a memorable journey you have taken                        ││
│  └────────────────────────────────────────────────────────────────────┘│
│                                                                         │
│  Keywords (comma-separated):                                            │
│  ┌────────────────────────────────────────────────────────────────────┐│
│  │ train, countryside, unexpected delay, kind stranger, sunset        ││
│  └────────────────────────────────────────────────────────────────────┘│
│                                                                         │
│  Idea Outline (one per line):                                           │
│  ┌────────────────────────────────────────────────────────────────────┐│
│  │ Where: From Hanoi to Hue by train                                  ││
│  │ When: Last summer, during monsoon season                           ││
│  │ What happened: Train delayed, met interesting co-passenger         ││
│  │ Feelings: Initially frustrated, then grateful                      ││
│  └────────────────────────────────────────────────────────────────────┘│
│                                                                         │
│  Vocabulary Hints (optional):                                           │
│  ┌────────────────────────────────────────────────────────────────────┐│
│  │ scenic, picturesque, coincidence, hospitality                      ││
│  └────────────────────────────────────────────────────────────────────┘│
│                                                                         │
│  Tone: ◉ Nostalgic  ○ Excited  ○ Informative  ○ Humorous               │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

### 8.3 Part 1: Introduction & Interview (Dự kiến)

```json
{
  "part1": {
    "topics": ["Home", "Hobbies", "Weather"],
    "questions": [
      {
        "topic": "Home",
        "questions": [
          "Do you live in a house or an apartment?",
          "What do you like most about your home?",
          "How long have you lived there?",
          "Would you like to move to a different place in the future?"
        ]
      }
    ],
    "total_questions": 15,
    "questions_used": "8-12 (flexible selection)"
  }
}
```

### 8.4 Part 2: Cue Card (Dự kiến)

```json
{
  "part2": {
    "topic": "Describe a book that you have read recently.",
    "cue_card": {
      "main_topic": "Describe a book that you have read recently.",
      "bullet_points": [
        "what the book was about",
        "why you decided to read it",
        "how long it took you to finish it"
      ],
      "closing_question": "Explain why you would or would not recommend this book to others."
    },
    "preparation_time": "1 minute",
    "speaking_time": "1-2 minutes"
  }
}
```

### 8.5 Part 3: Discussion (Dự kiến)

```json
{
  "part3": {
    "theme": "Reading habits and literature",
    "questions": [
      "Do you think reading habits have changed in recent years?",
      "What are the benefits of reading books compared to watching movies?",
      "How important is it for children to develop reading habits early?",
      "Do you think physical books will become obsolete in the future?",
      "What role do libraries play in modern society?"
    ],
    "total_questions": 10,
    "questions_used": "4-6 (flexible selection)"
  }
}
```

### 8.6 Future Development Notes

- Speaking module sẽ cần tích hợp với audio recording/playback
- Có thể xem xét TTS cho sample answers
- AI grading cho Speaking sẽ là một extension riêng
- Keywords/Idea Outline input method cho phép AI "sáng tác" linh hoạt hơn Facts-based approach

---

## 9. AI Prompt Engineering & Structured Output

### 9.1 DeepSeek JSON Mode

DeepSeek V3 hỗ trợ JSON mode thông qua `response_format` parameter:

```java
// DeepSeekClient.java
public String generateWithJsonMode(String systemPrompt, String userPrompt) {
    Map<String, Object> requestBody = Map.of(
        "model", "deepseek-chat",
        "messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
        ),
        "response_format", Map.of("type", "json_object"),
        "temperature", 0.3,  // Lower for consistent structured output
        "max_tokens", 8192
    );
    
    // Make API call and return JSON string
    return httpClient.post(DEEPSEEK_API_URL, requestBody);
}
```

### 9.2 System Prompt Structure

**Prompt có 5 phần chính:**

```
┌────────────────────────────────────────────────────────────────────────┐
│                      SYSTEM PROMPT STRUCTURE                            │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. ROLE & CONTEXT                                                      │
│     "You are an expert IELTS test creator..."                          │
│                                                                         │
│  2. TASK SPECIFICATION                                                  │
│     "Create a Reading passage with 13 questions..."                    │
│                                                                         │
│  3. QUALITY REQUIREMENTS                                                │
│     "Difficulty level: Intermediate (Band 6-7)..."                     │
│                                                                         │
│  4. JSON SCHEMA                                                         │
│     "Your response MUST be valid JSON matching this schema..."         │
│                                                                         │
│  5. EXAMPLES                                                            │
│     "Here is an example of correct output..."                          │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

### 9.3 Example: Reading Generation Prompt

```text
### ROLE & CONTEXT
You are an expert IELTS Academic Reading test creator with 20 years of experience. Your passages are used in official Cambridge IELTS preparation materials. You create content that is:
- Factually accurate based ONLY on the provided facts
- Appropriate for the specified difficulty level
- Following exact IELTS format and standards

### TASK SPECIFICATION
Create a Reading passage for IELTS Academic with the following specifications:
- Topic: {topic}
- Difficulty: {difficulty} (Band {bandRange})
- Word count: {minWords}-{maxWords} words
- Number of paragraphs: 6-8 (labeled A, B, C, etc.)
- Question types to include: {questionTypes}
- Total questions: {questionCount}

### FACTS TO USE
Base your passage ONLY on these verified facts. Do not invent additional facts:
{facts}

### QUALITY REQUIREMENTS
1. Passage must be coherent and academic in tone
2. Each paragraph must have a clear main idea
3. Questions must be answerable from the passage text
4. Explanations must reference specific paragraph/sentence locations
5. Word count must be verified and reported

### OUTPUT FORMAT
Your response MUST be valid JSON matching this exact schema:
{jsonSchema}

### EXAMPLE OUTPUT
Here is an example of correct output format:
{exampleOutput}

Now generate the content based on the specifications above.
```

### 9.4 Prompt Templates per Skill

**PromptBuilderService.java:**

```java
@Service
public class PromptBuilderService {
    
    private final TemplateEngine templateEngine;
    
    public String buildReadingPrompt(GenerationRequestDTO request) {
        Map<String, Object> context = Map.of(
            "topic", request.getTopic(),
            "difficulty", request.getDifficulty().getDisplayName(),
            "bandRange", request.getDifficulty().getBandRange(),
            "minWords", 850,
            "maxWords", 1000,
            "questionTypes", formatQuestionTypes(request.getQuestionTypes()),
            "questionCount", request.getQuestionCount(),
            "facts", formatFacts(request.getFacts()),
            "jsonSchema", getReadingJsonSchema(),
            "exampleOutput", getReadingExampleOutput(),
            "explanationLanguage", request.getExplanationLanguage()
        );
        
        return templateEngine.render("prompts/reading_generation.txt", context);
    }
    
    public String buildListeningPrompt(GenerationRequestDTO request) {
        // Similar structure for Listening
    }
    
    public String buildWritingPrompt(GenerationRequestDTO request) {
        // Similar structure for Writing
    }
}
```

### 9.5 Model Selection Strategy

ABTS sử dụng **2 models khác nhau** tùy theo task:

| Model | Mode | Default Output | Max Output | Best For |
|-------|------|----------------|------------|----------|
| `deepseek-chat` | Non-thinking | 4K | 8K | Quick iterations, simple tasks |
| `deepseek-reasoner` | Thinking (CoT) | 32K | 64K | Complex generation, quality-critical |

**Model Selection per Task:**

| Task | Model | Rationale |
|------|-------|-----------|
| **Full passage + all questions** | `deepseek-reasoner` | Cần reasoning quality cao, CoT giúp AI "suy nghĩ" |
| **Regenerate single question** | `deepseek-chat` | Task đơn giản, cần nhanh |
| **Regenerate passage only** | `deepseek-reasoner` | Cần creative writing quality |
| **Generate explanation only** | `deepseek-chat` | Task nhỏ, straightforward |
| **JSON fix retry** | `deepseek-chat` | Deterministic, không cần reasoning |
| **Writing chart data** | `deepseek-reasoner` | Cần logical data patterns |

**Implementation:**

```java
@Component
public class ModelSelector {
    
    public String getModelForTask(GenerationTask task) {
        return switch (task) {
            case FULL_GENERATION, 
                 PASSAGE_REGENERATION,
                 WRITING_CHART_DATA -> "deepseek-reasoner";
            
            case QUESTION_REGENERATION, 
                 EXPLANATION_ONLY, 
                 JSON_FIX -> "deepseek-chat";
        };
    }
    
    public ModelConfig getConfigForTask(GenerationTask task) {
        String model = getModelForTask(task);
        return ModelConfig.builder()
            .model(model)
            .maxTokens(model.equals("deepseek-reasoner") ? 32768 : 8192)
            .temperature(getTemperatureForTask(task))
            .build();
    }
}
```

### 9.6 Chain of Thought Visibility

Khi sử dụng `deepseek-reasoner`, AI trả về **reasoning process** trong response. ABTS capture và hiển thị CoT cho admin:

**Response Structure từ deepseek-reasoner:**

```json
{
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "{...JSON output...}",
        "reasoning_content": "First, I need to analyze the facts provided...\n\nFor the difficulty level 'intermediate', I should use vocabulary appropriate for Band 6-7...\n\nI'll structure the passage with 7 paragraphs to cover all the key facts...\n\nFor TRUE_FALSE_NOT_GIVEN questions, I'll base them on facts 1, 3, and 5 because they contain verifiable claims..."
      }
    }
  ]
}
```

**UI cho Chain of Thought:**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  GENERATION COMPLETE                                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ▼ View AI Reasoning (Chain of Thought)                    [Collapse]   │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ 💭 AI Reasoning Process:                                           │ │
│  │                                                                     │ │
│  │ "First, I need to analyze the facts provided...                    │ │
│  │                                                                     │ │
│  │ For the difficulty level 'intermediate', I should use vocabulary   │ │
│  │ appropriate for Band 6-7...                                        │ │
│  │                                                                     │ │
│  │ I'll structure the passage with 7 paragraphs to cover all the      │ │
│  │ key facts...                                                       │ │
│  │                                                                     │ │
│  │ For TRUE_FALSE_NOT_GIVEN questions, I'll base them on facts 1, 3,  │ │
│  │ and 5 because they contain verifiable claims..."                   │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  [Preview Content]  [Edit]  [Save to Editor]                            │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**Storage:**

CoT được lưu trong `generation_metadata.chain_of_thought` (xem Section 3.4) để:
- Debugging khi output không như mong đợi
- Audit trail cho quality assurance
- Training insights (xem AI "suy nghĩ" như thế nào)

### 9.7 Selective Regeneration Context

Khi admin regenerate một câu hỏi đơn lẻ (e.g., Q5), **LUÔN gửi lại toàn bộ Passage** trong context:

**Rationale:**

| Approach | Pros | Cons |
|----------|------|------|
| **Send full passage** ✅ | Đảm bảo Q mới match passage | Tốn ~4K input tokens |
| **Send question only** | Tiết kiệm tokens | Q mới có thể không match passage |

**Decision: Luôn gửi full passage** vì:
1. Cost minimal (~$0.001 per regeneration)
2. Quality assurance quan trọng hơn cost
3. Avoid câu hỏi không có answer trong passage

**Request Structure cho Selective Regeneration:**

```json
{
  "task": "QUESTION_REGENERATION",
  "existing_passage": "The development of solar energy has transformed...",
  "questions_to_regenerate": [5, 7],
  "keep_questions": [1, 2, 3, 4, 6, 8, 9, 10, 11, 12, 13],
  "question_types": {
    "5": "TRUE_FALSE_NOT_GIVEN",
    "7": "FILL_IN_BLANK"
  }
}
```

### 9.8 Temperature Settings

**DeepSeek Official Recommendations:**

| Use Case | Recommended Temperature |
|----------|------------------------|
| Coding / Math | 0.0 |
| Data Cleaning / Data Analysis | 1.0 |
| General Conversation | 1.3 |
| Translation | 1.3 |
| Creative Writing / Poetry | 1.5 |

> **Default:** DeepSeek V3.2 có default temperature = **1.0**

**ABTS-specific Temperature Strategy:**

Vì ABTS cần balance giữa creativeness (cho passage) và precision (cho JSON structure), chúng ta sử dụng **2 separate API calls**:

| Task | Temperature | Rationale | DeepSeek Context |
|------|-------------|-----------|------------------|
| **Full content generation** | 1.0 (default) | Balance creativity & structure | Data Analysis/Cleaning |
| **Passage-only regeneration** | 1.3 | More varied vocabulary & style | General Conversation |
| **Questions-only regeneration** | 0.7 | Structured but varied phrasing | Below default for consistency |
| **JSON validation/fix** | 0.0 | Deterministic output needed | Coding/Math precision |

**Implementation Note:**
```java
// Trong DeepSeekClient.java
public Double getTemperatureForTask(GenerationTask task) {
    return switch (task) {
        case FULL_GENERATION -> 1.0;
        case PASSAGE_REGENERATION -> 1.3;
        case QUESTIONS_REGENERATION -> 0.7;
        case JSON_FIX -> 0.0;
    };
}
```


### 9.6 Retry Logic for JSON Errors

```java
public GeneratedContentDTO generateWithRetry(GenerationRequestDTO request) {
    int maxRetries = 3;
    
    for (int attempt = 0; attempt < maxRetries; attempt++) {
        try {
            String jsonResponse = deepSeekClient.generate(buildPrompt(request));
            return jsonValidator.validateAndParse(jsonResponse, GeneratedContentDTO.class);
        } catch (JsonParseException e) {
            log.warn("JSON parse error on attempt {}: {}", attempt + 1, e.getMessage());
            if (attempt == maxRetries - 1) {
                throw new ABTSGenerationException("Failed to generate valid JSON after " + maxRetries + " attempts");
            }
            // Optionally adjust prompt to be more specific about JSON format
        }
    }
    throw new ABTSGenerationException("Unexpected error in generation");
}
```

---

## 10. JSON Schema & Data Validation

### 10.1 Validation Layers

```
┌────────────────────────────────────────────────────────────────────────┐
│                      VALIDATION PIPELINE                                │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  AI OUTPUT → Layer 1 → Layer 2 → Layer 3 → Layer 4 → VALID OUTPUT     │
│              JSON      Schema    Content   Business                     │
│              Parse     Validate  Validate  Validate                     │
│                                                                         │
│  Layer 1: Is it valid JSON syntax?                                     │
│  Layer 2: Does it match expected schema (required fields, types)?      │
│  Layer 3: Is content valid (word count, placeholder count)?            │
│  Layer 4: Does it meet business rules (IELTS standards)?               │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

### 10.2 JsonValidatorService

```java
@Service
public class JsonValidatorService {
    
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;
    
    public <T> ValidationResult<T> validateAndParse(String json, Class<T> targetClass) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Layer 1: JSON Syntax
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return ValidationResult.error("Invalid JSON syntax: " + e.getMessage());
        }
        
        // Layer 2: Schema Validation
        JsonSchema schema = schemaFactory.getSchema(getSchemaFor(targetClass));
        Set<ValidationMessage> schemaErrors = schema.validate(rootNode);
        if (!schemaErrors.isEmpty()) {
            errors.addAll(schemaErrors.stream()
                .map(ValidationMessage::getMessage)
                .collect(Collectors.toList()));
        }
        
        // Parse to object
        T result = objectMapper.treeToValue(rootNode, targetClass);
        
        // Layer 3 & 4: Content and Business validation
        validateContent(result, errors, warnings);
        validateBusinessRules(result, errors, warnings);
        
        return new ValidationResult<>(result, errors, warnings);
    }
    
    private void validateContent(Object result, List<String> errors, List<String> warnings) {
        if (result instanceof ReadingGeneratedContent) {
            validateReadingContent((ReadingGeneratedContent) result, errors, warnings);
        }
        // ... other skill types
    }
    
    private void validateReadingContent(ReadingGeneratedContent content, 
                                         List<String> errors, 
                                         List<String> warnings) {
        // Check word count
        int wordCount = content.getSection().getWordCount();
        if (wordCount < 850) {
            errors.add("Passage word count (" + wordCount + ") is below minimum (850)");
        } else if (wordCount > 1000) {
            warnings.add("Passage word count (" + wordCount + ") exceeds maximum (1000)");
        }
        
        // Check question count
        int questionCount = content.getQuestions().size();
        if (questionCount < 13 || questionCount > 14) {
            warnings.add("Expected 13-14 questions, got " + questionCount);
        }
        
        // Check FILL_IN_BLANK has exactly one ____
        for (QuestionDTO q : content.getQuestions()) {
            if (q.getQuestionType().equals("FILL_IN_BLANK")) {
                String text = q.getQuestionContent().getText();
                int placeholderCount = countOccurrences(text, "____");
                if (placeholderCount != 1) {
                    errors.add("Q" + q.getQuestionNumber() + 
                        ": FILL_IN_BLANK must have exactly 1 ____ placeholder, found " + placeholderCount);
                }
            }
        }
    }
}
```

### 10.3 JSON Schema Example (Reading)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["section", "questions", "metadata"],
  "properties": {
    "section": {
      "type": "object",
      "required": ["passage_text", "word_count"],
      "properties": {
        "passage_text": { "type": "string", "minLength": 1000 },
        "word_count": { "type": "integer", "minimum": 800, "maximum": 1100 },
        "word_count_valid": { "type": "boolean" },
        "word_count_message": { "type": "string" }
      }
    },
    "questions": {
      "type": "array",
      "minItems": 13,
      "maxItems": 14,
      "items": {
        "type": "object",
        "required": ["question_number", "question_type", "question_content", "correct_answer"],
        "properties": {
          "question_number": { "type": "integer", "minimum": 1, "maximum": 40 },
          "question_type": { 
            "type": "string",
            "enum": [
              "TRUE_FALSE_NOT_GIVEN", "YES_NO_NOT_GIVEN", "FILL_IN_BLANK",
              "SUMMARY_COMPLETION", "SUMMARY_COMPLETION_OPTIONS",
              "MATCHING_HEADINGS", "MATCHING_INFORMATION", "MATCHING_FEATURES",
              "MATCHING_SENTENCE_ENDINGS", "MULTIPLE_CHOICE", 
              "MULTIPLE_CHOICE_MULTIPLE_ANSWERS", "TABLE_COMPLETION",
              "DIAGRAM_LABEL_COMPLETION"
            ]
          },
          "question_content": {
            "type": "object",
            "required": ["text"],
            "properties": {
              "text": { "type": "string" },
              "options": { "type": "array" }
            }
          },
          "correct_answer": {
            "type": "array",
            "minItems": 1,
            "items": { "type": "string" }
          },
          "explanation": { "type": "string" },
          "word_limit": { "type": ["string", "null"] }
        }
      }
    },
    "metadata": {
      "type": "object",
      "properties": {
        "topic": { "type": "string" },
        "difficulty": { "type": "string" },
        "band_range": { "type": "string" },
        "generated_at": { "type": "string", "format": "date-time" }
      }
    }
  }
}
```

---

## 11. Hệ thống Input: Topics, Facts & Templates

### 11.1 Topic Template Categories

```javascript
// frontend/src/admin/templates/topicTemplates.js

export const topicCategories = [
  {
    id: "environment",
    emoji: "🌱",
    name: "Environment",
    name_vi: "Môi trường",
    description: "Climate, pollution, conservation, sustainability"
  },
  {
    id: "technology",
    emoji: "💻",
    name: "Technology",
    name_vi: "Công nghệ",
    description: "AI, internet, digital transformation, gadgets"
  },
  {
    id: "health",
    emoji: "🏥",
    name: "Health",
    name_vi: "Sức khỏe",
    description: "Medicine, nutrition, mental health, fitness"
  },
  {
    id: "education",
    emoji: "📚",
    name: "Education",
    name_vi: "Giáo dục",
    description: "Schools, universities, online learning, skills"
  },
  {
    id: "history",
    emoji: "🏛️",
    name: "History",
    name_vi: "Lịch sử",
    description: "Ancient civilizations, historical events, archaeology"
  },
  {
    id: "science",
    emoji: "🔬",
    name: "Science",
    name_vi: "Khoa học",
    description: "Biology, physics, chemistry, astronomy"
  },
  {
    id: "business",
    emoji: "💼",
    name: "Business",
    name_vi: "Kinh doanh",
    description: "Economics, management, marketing, entrepreneurship"
  },
  {
    id: "arts",
    emoji: "🎨",
    name: "Arts & Culture",
    name_vi: "Nghệ thuật",
    description: "Music, literature, painting, architecture"
  },
  {
    id: "society",
    emoji: "👥",
    name: "Society",
    name_vi: "Xã hội",
    description: "Urban development, demographics, social issues"
  },
  {
    id: "travel",
    emoji: "✈️",
    name: "Travel & Tourism",
    name_vi: "Du lịch",
    description: "Destinations, cultural tourism, travel industry"
  }
];
```

### 11.2 Sample Topic Templates

```javascript
export const sampleTopicTemplates = {
  "environment": [
    {
      id: "solar_energy",
      name: "Solar Energy Development",
      hashtags: ["renewable", "technology", "sustainability"],
      facts: [
        "Solar panels convert sunlight into electricity using photovoltaic cells made primarily from silicon",
        "The first practical solar cell was invented at Bell Laboratories in 1954 with 6% efficiency",
        "Modern commercial solar panels achieve efficiencies between 15-22%",
        "China produces over 70% of the world's solar panels as of 2023",
        "Solar energy is the fastest-growing source of renewable electricity globally",
        "The cost of solar panels has decreased by 99% since 1977",
        "Solar farms can generate electricity for 25-30 years with minimal maintenance",
        "Floating solar panels (floatovoltaics) are installed on water bodies to save land",
        "Solar energy production varies significantly based on geographic location and weather",
        "Germany was the first country to exceed 50% renewable electricity in 2020",
        "Solar thermal technology uses mirrors to concentrate sunlight for power generation",
        "Rooftop solar installations can reduce household electricity bills by 50-90%",
        "The Mojave Desert in the US hosts some of the largest solar power plants",
        "Solar panel recycling is an emerging industry addressing end-of-life disposal",
        "Battery storage technology is crucial for managing solar energy's intermittency",
        "India aims to achieve 500 GW of renewable energy capacity by 2030"
      ]
    },
    {
      id: "ocean_pollution",
      name: "Ocean Plastic Pollution",
      hashtags: ["marine", "pollution", "sustainability"],
      facts: [
        // Similar structure with 15-20 facts
      ]
    }
  ],
  "technology": [
    {
      id: "ai_development",
      name: "Artificial Intelligence Development",
      hashtags: ["AI", "technology", "future"],
      facts: [
        // Facts about AI
      ]
    }
  ]
  // ... more categories
};
```

### 11.3 Facts Input UI

```
┌────────────────────────────────────────────────────────────────────────┐
│                      FACTS INPUT INTERFACE                              │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Topic: [Solar Energy and Climate Change                    ]           │
│                                                                         │
│  Quick Load: [🌱 Solar Energy] [🌡️ Climate Change] [⚡ Renewables]     │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Facts (enter one fact per line or paste multiple):               │  │
│  │ ────────────────────────────────────────────────────────────────│  │
│  │ 1. Solar panels convert sunlight into electricity using         │  │
│  │    photovoltaic cells made primarily from silicon.              │  │
│  │                                                                   │  │
│  │ 2. The first practical solar cell was invented at Bell          │  │
│  │    Laboratories in 1954 with 6% efficiency.                     │  │
│  │                                                                   │  │
│  │ 3. Modern commercial solar panels achieve efficiencies          │  │
│  │    between 15-22%.                                               │  │
│  │                                                                   │  │
│  │ 4. China produces over 70% of the world's solar panels          │  │
│  │    as of 2023.                                                   │  │
│  │                                                                   │  │
│  │ ... (more facts)                                                 │  │
│  │                                                                   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  Facts count: [17/25] ✓ (Recommended: 15-25)                           │
│                                                                         │
│  ⓘ Tips:                                                               │
│  • Include specific numbers, dates, and proper nouns                   │
│  • Each fact should be independently verifiable                        │
│  • Mix general concepts with specific examples                         │
│  • Include facts suitable for different question types                 │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 12. Preview & Editing Workflow

### 12.1 Preview Panel Layout

```
┌────────────────────────────────────────────────────────────────────────┐
│                      ABTS PREVIEW PANEL                                 │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────┬───────────────────────────────┐   │
│  │          PASSAGE/CONTENT         │         QUESTIONS             │   │
│  ├─────────────────────────────────┼───────────────────────────────┤   │
│  │                                  │                               │   │
│  │  📊 Generation Stats:            │  Show: ○ All  ◉ With Errors   │   │
│  │  • Word count: 923/1000 ✓       │                               │   │
│  │  • Paragraphs: 7                 │  ┌─────────────────────────┐ │   │
│  │  • Generated in: 15.3s           │  │ Q1 TRUE_FALSE_NOT_GIVEN │ │   │
│  │                                  │  │ ✓ Valid                  │ │   │
│  │  ──────────────────────────────  │  │                         │ │   │
│  │                                  │  │ Statement:              │ │   │
│  │  <strong>The Future of Solar     │  │ "Solar panels were      │ │   │
│  │  Energy</strong>                 │  │  first developed..."    │ │   │
│  │                                  │  │                         │ │   │
│  │  <strong>A.</strong> In recent   │  │ Answer: TRUE            │ │   │
│  │  decades, the development of     │  │                         │ │   │
│  │  solar energy has transformed    │  │ Explanation:            │ │   │
│  │  the global energy landscape...  │  │ "Đáp án là TRUE vì..."  │ │   │
│  │                                  │  │                         │ │   │
│  │  <strong>B.</strong> The         │  │ [✏️] [🔄] [❌]           │ │   │
│  │  technology behind solar panels  │  └─────────────────────────┘ │   │
│  │  has improved dramatically...    │                               │   │
│  │                                  │  ┌─────────────────────────┐ │   │
│  │  [▼ Show more]                   │  │ Q2 FILL_IN_BLANK        │ │   │
│  │                                  │  │ ⚠️ Warning: Check       │ │   │
│  │  ──────────────────────────────  │  │                         │ │   │
│  │                                  │  │ ...                     │ │   │
│  │  [✏️ Edit Passage]               │  └─────────────────────────┘ │   │
│  │                                  │                               │   │
│  └─────────────────────────────────┴───────────────────────────────┘   │
│                                                                         │
│  [🔄 Regenerate All]  [💾 Save to Editor]  [📥 Export JSON]  [❌ Discard]│
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

### 12.2 Inline Editing Modal

```
┌────────────────────────────────────────────────────────────────────────┐
│                      EDIT QUESTION                              [X]    │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Question 5: TRUE_FALSE_NOT_GIVEN                                       │
│                                                                         │
│  Statement:                                                             │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ The efficiency of solar panels has more than tripled since the  │  │
│  │ technology was first developed.                                  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  Correct Answer:                                                        │
│  ◉ TRUE    ○ FALSE    ○ NOT GIVEN                                      │
│                                                                         │
│  Explanation (Vietnamese):                                              │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Đáp án là TRUE. Paragraph B cho biết "the first practical solar │  │
│  │ cell achieved 6% efficiency in 1954" và "modern panels reach    │  │
│  │ 15-22%". Điều này cho thấy hiệu suất đã tăng hơn 3 lần.         │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  Validation:                                                            │
│  ✓ Statement is grammatically correct                                  │
│  ✓ Answer is consistent with passage content                           │
│  ✓ Explanation references specific paragraph                           │
│                                                                         │
│  [💾 Save Changes]  [🔄 Regenerate This Question]  [❌ Cancel]          │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

### 12.3 Save to Editor Flow

Khi admin click "Save to Editor":

1. **Transform to Database Format:**
   - Convert generated JSON to `SectionDTO` và `QuestionDTO[]`
   - Ensure all required fields are populated

2. **Load into Editor:**
   - Navigate to TestEditorPage
   - Populate section fields (passage_text, audio_url, etc.)
   - Populate each question in the question navigator

3. **Continue Manual Editing:**
   - Admin can make further changes using existing editor
   - Existing validation still applies

4. **Final Save to Database:**
   - Only when admin explicitly clicks "Save" in editor
   - Goes through normal save flow with audit logging

---

## 13. Backend API Design

### 13.1 ABTS API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/abts/generate/reading` | Generate Reading content |
| POST | `/api/admin/abts/generate/listening` | Generate Listening content |
| POST | `/api/admin/abts/generate/writing` | Generate Writing content |
| POST | `/api/admin/abts/generate/questions` | Regenerate specific questions |
| POST | `/api/admin/abts/validate` | Validate generated JSON |
| GET | `/api/admin/abts/templates` | Get topic templates |
| GET | `/api/admin/abts/templates/{category}` | Get templates by category |

### 13.2 Request/Response DTOs

**GenerationRequestDTO.java:**
```java
@Data
public class GenerationRequestDTO {
    @NotNull
    private SkillType skill;  // READING, LISTENING, WRITING
    
    @NotNull
    private GenerationScope scope;  // FULL_SKILL, SINGLE_PART, QUESTION_GROUP
    
    private Integer partNumber;  // Required if scope is SINGLE_PART
    
    @NotNull
    private String topic;
    
    private List<String> hashtags;
    
    @NotNull
    @Size(min = 15, max = 25)
    private List<String> facts;
    
    @NotNull
    private DifficultyLevel difficulty;
    
    private List<QuestionType> questionTypes;
    
    private WordCountRange wordCountRange;
    
    @NotNull
    private ExplanationLanguage explanationLanguage;  // VI, EN
    
    private TestType testType;  // ACADEMIC, GENERAL_TRAINING
    
    // For regeneration
    private String existingPassageText;
    private List<Integer> questionsToRegenerate;
}
```

**GenerationResponseDTO.java:**
```java
@Data
public class GenerationResponseDTO {
    private GenerationStatus status;  // SUCCESS, PARTIAL_SUCCESS, FAILED
    
    private GeneratedContentDTO content;
    
    private ValidationResultDTO validation;
    
    private GenerationMetadataDTO metadata;
    
    private List<String> errors;
    private List<String> warnings;
}

@Data
public class GenerationMetadataDTO {
    private String topic;
    private String difficulty;
    private String bandRange;
    private Integer wordCount;
    private Integer questionCount;
    private Double generationTimeSeconds;
    private String generatedAt;
}
```

### 13.3 ABTSController

```java
@RestController
@RequestMapping("/api/admin/abts")
@RequiredArgsConstructor
public class ABTSController {
    
    private final ABTSService abtsService;
    private final AdminAuthFilter adminAuthFilter;
    
    @PostMapping("/generate/reading")
    public ResponseEntity<GenerationResponseDTO> generateReading(
            @Valid @RequestBody GenerationRequestDTO request,
            @AuthenticationPrincipal UserDetails user) {
        
        // Verify admin access
        adminAuthFilter.verifyAdminAccess(user);
        
        request.setSkill(SkillType.READING);
        GenerationResponseDTO response = abtsService.generate(request);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/generate/listening")
    public ResponseEntity<GenerationResponseDTO> generateListening(
            @Valid @RequestBody GenerationRequestDTO request,
            @AuthenticationPrincipal UserDetails user) {
        
        adminAuthFilter.verifyAdminAccess(user);
        request.setSkill(SkillType.LISTENING);
        return ResponseEntity.ok(abtsService.generate(request));
    }
    
    @PostMapping("/generate/writing")
    public ResponseEntity<GenerationResponseDTO> generateWriting(
            @Valid @RequestBody GenerationRequestDTO request,
            @AuthenticationPrincipal UserDetails user) {
        
        adminAuthFilter.verifyAdminAccess(user);
        request.setSkill(SkillType.WRITING);
        return ResponseEntity.ok(abtsService.generate(request));
    }
    
    @PostMapping("/generate/questions")
    public ResponseEntity<GenerationResponseDTO> regenerateQuestions(
            @Valid @RequestBody RegenerateQuestionsRequestDTO request,
            @AuthenticationPrincipal UserDetails user) {
        
        adminAuthFilter.verifyAdminAccess(user);
        return ResponseEntity.ok(abtsService.regenerateQuestions(request));
    }
    
    @GetMapping("/templates")
    public ResponseEntity<List<TopicCategoryDTO>> getTemplateCategories() {
        return ResponseEntity.ok(abtsService.getTemplateCategories());
    }
    
    @GetMapping("/templates/{categoryId}")
    public ResponseEntity<List<TopicTemplateDTO>> getTemplatesByCategory(
            @PathVariable String categoryId) {
        return ResponseEntity.ok(abtsService.getTemplatesByCategory(categoryId));
    }
}
```

### 13.4 ABTSService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ABTSService {
    
    private final PromptBuilderService promptBuilder;
    private final DeepSeekClient deepSeekClient;
    private final JsonValidatorService jsonValidator;
    private final ContentTransformerService contentTransformer;
    
    public GenerationResponseDTO generate(GenerationRequestDTO request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Build prompt
            String prompt = promptBuilder.buildPrompt(request);
            
            // 2. Call DeepSeek API
            String jsonResponse = deepSeekClient.generateWithJsonMode(
                getSystemPrompt(request.getSkill()),
                prompt
            );
            
            // 3. Validate JSON
            ValidationResult<GeneratedContentDTO> validationResult = 
                jsonValidator.validateAndParse(jsonResponse, 
                    getContentClass(request.getSkill()));
            
            // 4. Build response
            double generationTime = (System.currentTimeMillis() - startTime) / 1000.0;
            
            return GenerationResponseDTO.builder()
                .status(validationResult.hasErrors() ? 
                    GenerationStatus.PARTIAL_SUCCESS : GenerationStatus.SUCCESS)
                .content(validationResult.getResult())
                .validation(validationResult.toDTO())
                .metadata(buildMetadata(request, validationResult.getResult(), generationTime))
                .errors(validationResult.getErrors())
                .warnings(validationResult.getWarnings())
                .build();
                
        } catch (Exception e) {
            log.error("ABTS generation failed", e);
            return GenerationResponseDTO.builder()
                .status(GenerationStatus.FAILED)
                .errors(List.of(e.getMessage()))
                .build();
        }
    }
    
    public GenerationResponseDTO regenerateQuestions(RegenerateQuestionsRequestDTO request) {
        // Similar flow but with existing passage context
        String prompt = promptBuilder.buildQuestionRegenerationPrompt(
            request.getExistingPassage(),
            request.getQuestionsToRegenerate(),
            request.getQuestionTypes()
        );
        
        // ... rest of the flow
    }
}
```

---

## 14. Xử lý Lỗi & Edge Cases

### 14.1 DeepSeek API Error Codes

**Theo tài liệu chính thức của DeepSeek:**

| HTTP Code | Error Type | Nguyên nhân | Xử lý trong ABTS |
|-----------|------------|-------------|------------------|
| **400** | Invalid Format | Request body format không hợp lệ | Log chi tiết, báo dev để fix prompt |
| **401** | Authentication Fails | API key sai hoặc hết hạn | Thông báo admin kiểm tra config |
| **402** | Insufficient Balance | Hết balance trong tài khoản DeepSeek | Thông báo admin top-up, disable ABTS tạm thời |
| **422** | Invalid Parameters | Params không hợp lệ (temperature, max_tokens, etc.) | Log chi tiết, báo dev để fix |
| **429** | Rate Limit Reached | Gửi requests quá nhanh | Wait và retry với exponential backoff |
| **500** | Server Error | DeepSeek server gặp lỗi | Retry sau 5-10 giây |
| **503** | Server Overloaded | High traffic on DeepSeek | Retry với delay dài hơn |

**Timeout Handling:**

> **Quan trọng:** DeepSeek API không có rate limit cứng, nhưng khi server quá tải:
> - **Non-streaming requests:** Trả về empty lines liên tục
> - **Streaming requests:** Trả về `: keep-alive` SSE comments
> - **Maximum wait time:** 30 phút trước khi server đóng connection
>
> **ABTS Timeout Strategy:** Đặt timeout 60s cho mỗi request, thông báo user nếu quá lâu.

### 14.2 ABTS Error Categories

| Category | HTTP Codes | Description | Handling |
|----------|------------|-------------|----------|
| **Authentication** | 401 | API key issues | Show config error, stop ABTS |
| **Balance** | 402 | Insufficient funds | Disable generations, notify admin |
| **Client Error** | 400, 422 | Invalid request | Log for debugging, show user-friendly error |
| **Rate Limit** | 429 | Too many requests | Exponential backoff retry |
| **Server Error** | 500, 503 | DeepSeek issues | Retry with delay |
| **Timeout** | N/A | Request takes too long | Cancel after 60s, allow retry |
| **JSON Parse** | N/A (200) | AI returned invalid JSON | Retry with stricter prompt |
| **Validation** | N/A (200) | JSON valid but content wrong | Show warnings, allow edit |

### 14.3 Error Handling Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                      ERROR HANDLING FLOW                                │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  [API Call] ──┬── 200 Success ────────────────────────▶ [Validate]     │
│               │                                                         │
│               ├── 400/422 Client Error ──▶ Log & Show Dev Error        │
│               │                                                         │
│               ├── 401 Auth Error ──▶ Stop & Notify Admin               │
│               │                                                         │
│               ├── 402 Balance Error ──▶ Disable ABTS, Alert Admin      │
│               │                                                         │
│               ├── 429 Rate Limit ──▶ Exponential Backoff Retry         │
│               │                                                         │
│               ├── 500/503 Server Error ──▶ Retry (max 3, 5s delay)     │
│               │                                                         │
│               └── Timeout (60s) ──▶ Cancel & Offer Retry               │
│                                                                         │
│  [Validate] ──┬── Valid Structure ──────────────────▶ [Return Success] │
│               │                                                         │
│               ├── JSON Parse Error ──▶ Retry with Stricter Prompt      │
│               │                                                         │
│               ├── Schema Error ──▶ Show Fields with Errors             │
│               │                                                         │
│               └── Content Warning ──▶ Return with Warnings (editable)  │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

### 14.4 Java Error Handler Implementation

```java
@Component
public class DeepSeekErrorHandler {
    
    public GenerationResponseDTO handleDeepSeekError(HttpClientErrorException ex) {
        int statusCode = ex.getStatusCode().value();
        String errorMessage = parseErrorMessage(ex.getResponseBodyAsString());
        
        return switch (statusCode) {
            case 400 -> GenerationResponseDTO.error(
                "INVALID_FORMAT",
                "Request không hợp lệ. Vui lòng liên hệ admin.",
                false  // Không retry
            );
            case 401 -> GenerationResponseDTO.error(
                "AUTH_FAILED",
                "API key không hợp lệ. ABTS tạm thời không khả dụng.",
                false
            );
            case 402 -> GenerationResponseDTO.error(
                "INSUFFICIENT_BALANCE",
                "Hết balance DeepSeek. Vui lòng liên hệ admin để top-up.",
                false
            );
            case 422 -> GenerationResponseDTO.error(
                "INVALID_PARAMS",
                "Parameters không hợp lệ: " + errorMessage,
                false
            );
            case 429 -> GenerationResponseDTO.error(
                "RATE_LIMITED",
                "Đang có quá nhiều requests. Vui lòng đợi vài giây.",
                true  // Có thể retry
            );
            case 500, 503 -> GenerationResponseDTO.error(
                "SERVER_ERROR",
                "DeepSeek server đang gặp vấn đề. Vui lòng thử lại sau.",
                true  // Retry với delay
            );
            default -> GenerationResponseDTO.error(
                "UNKNOWN_ERROR",
                "Lỗi không xác định: " + errorMessage,
                false
            );
        };
    }
}
```


### 14.3 User-Friendly Error Messages

```javascript
// frontend/src/admin/services/abtsErrorHandler.js

export const ABTS_ERROR_MESSAGES = {
  API_UNAVAILABLE: {
    title: "Không thể kết nối đến AI",
    message: "Vui lòng kiểm tra kết nối mạng và thử lại sau.",
    action: "Thử lại"
  },
  GENERATION_TIMEOUT: {
    title: "Quá thời gian chờ",
    message: "Việc tạo nội dung mất quá lâu. Vui lòng thử lại với ít câu hỏi hơn.",
    action: "Thử lại"
  },
  VALIDATION_FAILED: {
    title: "Nội dung cần kiểm tra",
    message: "AI đã tạo nội dung nhưng có một số vấn đề cần xem xét.",
    action: "Xem chi tiết"
  },
  RATE_LIMITED: {
    title: "Đang trong thời gian chờ",
    message: "Đã gửi quá nhiều yêu cầu. Vui lòng đợi {seconds} giây.",
    action: "Đợi"
  }
};
```

### 14.4 Partial Success Handling

Khi AI generate thành công một phần (e.g., passage OK but some questions invalid):

1. **Show what succeeded** with green checkmarks
2. **Highlight failed items** with error details
3. **Offer options:**
   - "Regenerate Failed Items Only"
   - "Edit Manually"
   - "Regenerate All"

### 14.5 Chiến lược Fail-Hard sau 3 Retries

**Scenario:** AI liên tục trả về JSON sai format dù đã retry.

**Decision:** Sau 3 lần retry thất bại → **FAIL HARD** và bắt admin làm lại từ đầu.

**Rationale:**
- ABTS là admin tool, không phải user-facing → có thể yêu cầu restart
- Continuing với broken JSON có thể gây data corruption
- Simple approach > complex recovery logic

**Flow:**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    3-RETRY FAIL STRATEGY                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  [API Call] → JSON Response                                              │
│       │                                                                  │
│       ▼                                                                  │
│  [JSON Parse Attempt 1] ─── Success ──▶ [Continue]                      │
│       │                                                                  │
│       ▼ Fail                                                             │
│  [Retry with stricter prompt, temp=0.0, model=deepseek-chat]            │
│       │                                                                  │
│       ▼                                                                  │
│  [JSON Parse Attempt 2] ─── Success ──▶ [Continue]                      │
│       │                                                                  │
│       ▼ Fail                                                             │
│  [Retry again with temp=0.0]                                            │
│       │                                                                  │
│       ▼                                                                  │
│  [JSON Parse Attempt 3] ─── Success ──▶ [Continue]                      │
│       │                                                                  │
│       ▼ Fail                                                             │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  ❌ GENERATION FAILED                                             │   │
│  │                                                                   │   │
│  │  "Không thể tạo nội dung sau 3 lần thử.                          │   │
│  │   Vui lòng thử lại với topic hoặc facts khác."                   │   │
│  │                                                                   │   │
│  │  [View Error Details]  [Thử lại từ đầu]  [Đóng]                   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**Implementation:**

```java
public GenerationResponseDTO generateWithRetry(GenerationRequestDTO request) {
    int maxRetries = 3;
    
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            // Each retry uses stricter settings
            double temperature = (attempt == 1) ? 1.0 : 0.0;
            String model = (attempt == 1) ? "deepseek-reasoner" : "deepseek-chat";
            
            String jsonResponse = deepSeekClient.generate(
                buildPrompt(request),
                model,
                temperature
            );
            
            return jsonValidator.validateAndParse(jsonResponse, GeneratedContentDTO.class);
            
        } catch (JsonParseException e) {
            log.warn("JSON parse error on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());
            
            if (attempt == maxRetries) {
                // FAIL HARD after 3 attempts
                return GenerationResponseDTO.builder()
                    .status(GenerationStatus.FAILED)
                    .errors(List.of(
                        "Không thể tạo nội dung hợp lệ sau " + maxRetries + " lần thử.",
                        "Chi tiết lỗi: " + e.getMessage()
                    ))
                    .metadata(GenerationMetadataDTO.builder()
                        .failedAttempts(maxRetries)
                        .lastError(e.getMessage())
                        .build())
                    .build();
            }
            
            // Small delay before retry
            Thread.sleep(1000);
        }
    }
    
    throw new IllegalStateException("Should not reach here");
}
```

---

## 15. Lộ trình Triển khai

### 15.1 Prerequisites

Trước khi bắt đầu ABTS, cần hoàn thành:
- [ ] CMS Admin shell (Phase 1 của CMS specs)
- [ ] Content Management UI cơ bản (Phase 4 của CMS specs)
- [ ] DeepSeek API integration đã test với chat feature

### 15.2 ABTS Development Phases

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    ABTS DEVELOPMENT TIMELINE                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Phase 1: Foundation          ███████░░░░░░░░░░░░░░░░░░░░  (1-2 tuần)   │
│  Phase 2: Reading Generation  ░░░░░░░████████░░░░░░░░░░░░  (2-3 tuần)   │
│  Phase 3: Listening Generation░░░░░░░░░░░░░░░█████░░░░░░░  (1-2 tuần)   │
│  Phase 4: Writing Generation  ░░░░░░░░░░░░░░░░░░░░█████░░  (1-2 tuần)   │
│  Phase 5: Polish & Testing    ░░░░░░░░░░░░░░░░░░░░░░░░░██  (1 tuần)     │
│                                                                          │
│  ────────────────────────────────────────────────────────────────────── │
│  TỔNG THỜI GIAN: ~7-10 tuần                                              │
│  ────────────────────────────────────────────────────────────────────── │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 15.3 Phase 1: Foundation (1-2 tuần)

| # | Task | Priority |
|---|------|----------|
| 1.1 | Setup ABTS folder structure | 🔴 Cao |
| 1.2 | Create DeepSeekClient with JSON mode | 🔴 Cao |
| 1.3 | Create PromptBuilderService | 🔴 Cao |
| 1.4 | Create JsonValidatorService | 🔴 Cao |
| 1.5 | Create ABTSController (stubs) | 🔴 Cao |
| 1.6 | Create ABTSService (stubs) | 🔴 Cao |
| 1.7 | Setup topic templates | 🟡 Trung bình |
| 1.8 | Create frontend ABTS API client | 🟡 Trung bình |

**Deliverables:**
- [ ] DeepSeek API can be called with JSON mode
- [ ] Basic prompt templates ready
- [ ] JSON validation framework in place

### 15.4 Phase 2: Reading Generation (2-3 tuần)

| # | Task | Priority |
|---|------|----------|
| 2.1 | Create Reading prompt template | 🔴 Cao |
| 2.2 | Implement Reading JSON schema | 🔴 Cao |
| 2.3 | Create GenerationWizard component | 🔴 Cao |
| 2.4 | Implement StepTopicInput | 🔴 Cao |
| 2.5 | Implement StepFactsInput | 🔴 Cao |
| 2.6 | Implement StepConfiguration | 🔴 Cao |
| 2.7 | Implement StepGenerate | 🔴 Cao |
| 2.8 | Implement StepPreview | 🔴 Cao |
| 2.9 | Create ReadingPreview component | 🟡 Trung bình |
| 2.10 | Implement inline AI assist buttons | 🟡 Trung bình |
| 2.11 | Implement selective regeneration | 🟡 Trung bình |
| 2.12 | Test with 10+ different topics | 🟡 Trung bình |

**Deliverables:**
- [ ] Full Reading generation wizard working
- [ ] Preview and edit flow working
- [ ] Can save to existing content editor

### 15.5 Phase 3: Listening Generation (1-2 tuần)

| # | Task | Priority |
|---|------|----------|
| 3.1 | Create Listening prompt template | 🔴 Cao |
| 3.2 | Implement Listening JSON schema | 🔴 Cao |
| 3.3 | Implement section_layout generation | 🔴 Cao |
| 3.4 | Create ListeningPreview component | 🔴 Cao |
| 3.5 | Implement figure description generation | 🟡 Trung bình |
| 3.6 | Implement audio placeholder | 🟡 Trung bình |
| 3.7 | Test with different part types | 🟡 Trung bình |

**Deliverables:**
- [ ] Listening generation working for all 4 part types
- [ ] Figure descriptions are detailed enough for recreation
- [ ] Transcript formatting is correct

### 15.6 Phase 4: Writing Generation (1-2 tuần)

| # | Task | Priority |
|---|------|----------|
| 4.1 | Create Writing prompt template | 🔴 Cao |
| 4.2 | Implement Writing JSON schema | 🔴 Cao |
| 4.3 | Create ChartRenderer component | 🔴 Cao |
| 4.4 | Implement chart data to Chart.js conversion | 🔴 Cao |
| 4.5 | Create WritingPreview component | 🟡 Trung bình |
| 4.6 | Implement Task 2 essay generation | 🟡 Trung bình |
| 4.7 | Test chart rendering accuracy | 🟡 Trung bình |

**Deliverables:**
- [ ] Task 1 with renderable charts working
- [ ] Task 2 essay questions working
- [ ] Chart preview matches expected output

### 15.7 Phase 5: Polish & Testing (1 tuần)

| # | Task | Priority |
|---|------|----------|
| 5.1 | Comprehensive error handling | 🔴 Cao |
| 5.2 | Loading states and progress bars | 🔴 Cao |
| 5.3 | User acceptance testing | 🔴 Cao |
| 5.4 | Performance optimization | 🟡 Trung bình |
| 5.5 | Documentation update | 🟡 Trung bình |

**Deliverables:**
- [ ] All edge cases handled gracefully
- [ ] Admin can use ABTS without technical issues
- [ ] Documentation complete

---

## 16. Phụ lục

### 16.1 Glossary

| Term | Definition |
|------|------------|
| **ABTS** | AI-Based Test Generation System |
| **Structured Output** | AI response constrained to specific JSON schema |
| **Facts-based Generation** | AI creates content only from provided facts |
| **Selective Regeneration** | Regenerate specific items while keeping others |
| **Human-in-the-Loop** | Human verification required before final save |

### 16.2 Related Documentation

**Internal:**
- `CRAMER_CMS_ADMIN_SPECS.md` - Parent CMS specifications
- `DATA_INGESTION_GUIDE_READING.md` - Reading JSON structure
- `DATA_INGESTION_GUIDE_LISTENING.md` - Listening JSON structure
- `DATABASE_SCHEMA.md` - Database table structures
- `IELTS_W_Band_Descriptors.md` - Writing marking criteria

**External:**
- `docs/backend/deepseek_api/` - DeepSeek API documentation (local copy)
- [DeepSeek API Official Docs](https://api-docs.deepseek.com/)

### 16.3 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 17/12/2025 | Initial draft |
| 1.1.0 | 17/12/2025 | Added DeepSeek API Reference appendix, updated error handling with official error codes, enhanced temperature settings |
| 1.2.0 | 17/12/2025 | Q&A Enhancements: Speaking Keywords/Idea Outline input, Generation Metadata storage, Model Selection Strategy (reasoner vs chat), Chain of Thought visibility, Chart diversity (20+ types) with rasterization, Fail-hard 3-retry strategy, Appendix B Chart Styling Guidelines |

---

## Appendix A: DeepSeek API Reference

> **Nguồn:** Tổng hợp từ `docs/backend/deepseek_api/` và tài liệu chính thức của DeepSeek

### A.1 API Overview

**DeepSeek API** tương thích với OpenAI API format. Có thể sử dụng OpenAI SDK hoặc bất kỳ client nào compatible với OpenAI API.

| Parameter | Value |
|-----------|-------|
| **Base URL** | `https://api.deepseek.com` |
| **Alternative URL** | `https://api.deepseek.com/v1` (OpenAI-compatible) |
| **Authentication** | Bearer token (`Authorization: Bearer ${DEEPSEEK_API_KEY}`) |
| **Content-Type** | `application/json` |

### A.2 Available Models

| Feature | deepseek-chat<br>(Non-thinking) | deepseek-reasoner<br>(Thinking Mode) |
|---------|--------------------------------|--------------------------------------|
| **Base Model** | DeepSeek-V3.2 | DeepSeek-V3.2 |
| **Context Length** | 128K | 128K |
| **Default Output** | 4K | 32K |
| **Maximum Output** | 8K | 64K |
| **JSON Output** | ✅ | ✅ |
| **Tool Calls** | ✅ | ✅ |
| **Chat Prefix** | ✅ | ✅ |
| **FIM Completion** | ✅ | ❌ |

> **Lưu ý:** ABTS sử dụng `deepseek-chat` vì:
> 1. Hỗ trợ JSON mode tốt
> 2. Default output 4K đủ cho hầu hết use cases
> 3. Thinking mode không cần thiết cho content generation

### A.3 Basic API Call Example

**cURL:**
```bash
curl https://api.deepseek.com/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${DEEPSEEK_API_KEY}" \
  -d '{
        "model": "deepseek-chat",
        "messages": [
          {"role": "system", "content": "You are a helpful assistant."},
          {"role": "user", "content": "Hello!"}
        ],
        "stream": false
      }'
```

**Java (RestTemplate):**
```java
@Service
public class DeepSeekClient {
    
    private static final String BASE_URL = "https://api.deepseek.com";
    private static final String CHAT_ENDPOINT = "/chat/completions";
    
    @Value("${deepseek.api-key}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    
    public String callChatCompletion(String systemPrompt, String userPrompt, 
                                      boolean jsonMode, double temperature) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
        ));
        requestBody.put("stream", false);
        requestBody.put("temperature", temperature);
        
        if (jsonMode) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<DeepSeekResponse> response = restTemplate.exchange(
            BASE_URL + CHAT_ENDPOINT,
            HttpMethod.POST,
            entity,
            DeepSeekResponse.class
        );
        
        return response.getBody().getChoices().get(0).getMessage().getContent();
    }
}
```

### A.4 JSON Mode

Để enable JSON mode, add `response_format` parameter:

```json
{
  "model": "deepseek-chat",
  "messages": [...],
  "response_format": {
    "type": "json_object"
  }
}
```

**Quan trọng khi dùng JSON mode:**
1. System prompt **PHẢI** yêu cầu AI trả về JSON
2. Nên cung cấp JSON schema trong prompt
3. Nên cung cấp ví dụ output

**Ví dụ System Prompt cho JSON mode:**
```
You are a helpful assistant that ALWAYS responds in valid JSON format.

Your response MUST be a valid JSON object matching this schema:
{
  "section": { "passage_text": "string", "word_count": number },
  "questions": [...]
}

Do NOT include any text outside the JSON object.
```

### A.5 Pricing

| Token Type | Price (USD per 1M tokens) |
|------------|---------------------------|
| **Input (Cache Hit)** | $0.028 |
| **Input (Cache Miss)** | $0.28 |
| **Output** | $0.42 |

**Cách tính chi phí:**
```
Cost = (Input Tokens × Input Price) + (Output Tokens × Output Price)
```

**Cache Hit Conditions:**
- Cùng prompt prefix được gửi nhiều lần
- Cache tự động, không cần configure
- Tiết kiệm ~10x cho input tokens

### A.6 Temperature Settings

| Use Case | Recommended Temperature |
|----------|------------------------|
| Coding / Math | 0.0 |
| Data Cleaning / Data Analysis | 1.0 |
| General Conversation | 1.3 |
| Translation | 1.3 |
| Creative Writing / Poetry | 1.5 |

> **Default temperature:** 1.0

### A.7 Error Codes

| HTTP Code | Error Type | Cause | Solution |
|-----------|------------|-------|----------|
| **400** | Invalid Format | Invalid request body | Check request format |
| **401** | Authentication Fails | Wrong API key | Verify API key |
| **402** | Insufficient Balance | Out of credits | Top up account |
| **422** | Invalid Parameters | Bad parameters | Check parameter values |
| **429** | Rate Limit Reached | Too many requests | Slow down requests |
| **500** | Server Error | Server issue | Retry after delay |
| **503** | Server Overloaded | High traffic | Retry later |

### A.8 Rate Limits & Keep-Alive

**DeepSeek không có rate limit cứng.** Tuy nhiên, khi server quá tải:

**Non-streaming requests:**
- Server trả về empty lines liên tục
- Client nên ignore empty lines

**Streaming requests:**
- Server trả về SSE keep-alive comments: `: keep-alive`
- OpenAI SDK tự động handle

**Timeout:**
- Sau 30 phút không complete, server đóng connection
- ABTS nên set timeout 60 giây và retry

### A.9 Best Practices cho ABTS

1. **Sử dụng JSON mode** cho tất cả content generation
2. **Cache prompt templates** bằng cách đặt schema/examples ở đầu prompt
3. **Set timeout 60s** và implement retry logic
4. **Log token usage** để monitor costs
5. **Validate JSON response** trước khi parse
6. **Sử dụng temperature phù hợp:**
   - 1.0 cho full generation
   - 1.3 cho regenerate passage only
   - 0.0 cho JSON fix
7. **Handle empty lines** trong non-streaming response
8. **Monitor HTTP 402** để alert về balance issues

### A.10 Sample Request for ABTS Reading Generation

```bash
curl https://api.deepseek.com/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${DEEPSEEK_API_KEY}" \
  -d '{
        "model": "deepseek-chat",
        "messages": [
          {
            "role": "system", 
            "content": "You are an expert IELTS Academic Reading test creator. You ALWAYS respond in valid JSON format matching the provided schema."
          },
          {
            "role": "user", 
            "content": "Create a Reading passage with 13 questions about Solar Energy. Use only these facts: [fact1, fact2, ...]. Output JSON with section and questions arrays."
          }
        ],
        "response_format": {"type": "json_object"},
        "temperature": 1.0,
        "max_tokens": 8192,
        "stream": false
      }'
```

---

## Appendix B: Chart Styling Guidelines

> **Nguồn:** Dựa trên `docs/frontend/UI_GUIDELINES.md` để đảm bảo chart theming nhất quán với design của Cramer web.

### B.1 Design Token Integration

Charts trong ABTS Writing phải tuân thủ design tokens từ UI Guidelines:

**Core Colors (từ UI_GUIDELINES.md):**

| Token | Value | Usage in Charts |
|-------|-------|-----------------|
| `--primary-accent` | `#7c3aed` | Chart borders, highlights |
| `--primary-hover` | `#6d28d9` | Hover states on chart elements |
| `--primary-gradient` | `linear-gradient(135deg, #7c3aed, #6366f1)` | Chart container headers |
| `--glass-bg` | `rgba(18, 10, 53, 0.75)` | Tooltip backgrounds |
| `--glass-border` | `rgba(255, 255, 255, 0.18)` | Chart container borders |
| `--text-primary` | `#1f2937` | Axis labels, chart titles |
| `--text-secondary` | `rgba(255, 255, 255, 0.9)` | Light mode legend text |

### B.2 Typography

Charts sử dụng **Be Vietnam Pro** (font chính của Cramer):

```css
/* Chart typography settings */
.chart-container {
  font-family: 'Be Vietnam Pro', sans-serif;
}

.chart-title {
  font-size: 1.25rem; /* 20px - H3/Card Title */
  font-weight: 600;
}

.axis-label {
  font-size: 0.875rem; /* 14px - Small/Meta */
  font-weight: 400;
}

.legend-text {
  font-size: 0.75rem; /* 12px */
  font-weight: 400;
}
```

### B.3 Color Palettes for Data Series

> **Lưu ý:** Mỗi chart sẽ có color palette riêng tùy thuộc vào topic và số series. Palette phải harmonious và accessible.

**Curated Data Palettes:**

```javascript
export const CHART_DATA_PALETTES = {
  // Primary palette (default) - based on Cramer purple theme
  cramer: [
    '#7c3aed', // Primary purple
    '#6366f1', // Secondary purple
    '#a78bfa', // Light purple
    '#4f46e5', // Deep indigo
    '#8b5cf6', // Violet
  ],
  
  // Warm palette - for economy, finance topics
  warm: [
    '#ef4444', // Red
    '#f97316', // Orange
    '#f59e0b', // Amber
    '#eab308', // Yellow
    '#84cc16', // Lime
  ],
  
  // Cool palette - for environment, nature topics
  cool: [
    '#06b6d4', // Cyan
    '#0ea5e9', // Sky blue
    '#3b82f6', // Blue
    '#22c55e', // Green
    '#14b8a6', // Teal
  ],
  
  // Neutral palette - for comparison charts
  neutral: [
    '#64748b', // Slate
    '#6b7280', // Gray
    '#78716c', // Stone
    '#71717a', // Zinc
    '#a1a1aa', // Light zinc
  ],
  
  // Mixed palette - for diverse country comparisons (colorblind-friendly)
  mixed: [
    '#e74c3c', // Red
    '#3498db', // Blue
    '#2ecc71', // Green
    '#f39c12', // Orange
    '#9b59b6', // Purple
    '#1abc9c', // Turquoise
  ]
};
```

### B.4 Glassmorphism Chart Containers

Áp dụng glassmorphism cho chart containers nhất quán với UI củapage:

```css
.chart-wrapper {
  /* Glassmorphism base */
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  
  /* Border */
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 12px;
  
  /* Shadow (md level from UI Guidelines) */
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  
  /* Padding */
  padding: 1.5rem;
  
  /* Animation on hover (optional) */
  transition: all 0.3s ease-in-out;
}

.chart-wrapper:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.15);
}
```

### B.5 Chart.js Default Configuration

```javascript
// chartDefaults.js - Apply globally
import { Chart } from 'chart.js';
import { CHART_THEME } from './chartTheme';

// Global font settings
Chart.defaults.font.family = "'Be Vietnam Pro', sans-serif";
Chart.defaults.font.size = 12;

// Global color settings
Chart.defaults.color = '#1f2937'; // Text color
Chart.defaults.borderColor = 'rgba(0, 0, 0, 0.1)'; // Grid lines

// Plugin defaults
Chart.defaults.plugins.legend.labels.font.size = 11;
Chart.defaults.plugins.legend.labels.font.family = "'Be Vietnam Pro', sans-serif";

Chart.defaults.plugins.tooltip.backgroundColor = 'rgba(18, 10, 53, 0.9)';
Chart.defaults.plugins.tooltip.titleFont.family = "'Be Vietnam Pro', sans-serif";
Chart.defaults.plugins.tooltip.bodyFont.family = "'Be Vietnam Pro', sans-serif";
Chart.defaults.plugins.tooltip.cornerRadius = 8;
Chart.defaults.plugins.tooltip.padding = 12;

// Animation defaults (matching UI Guidelines transitions)
Chart.defaults.animation.duration = 300;
Chart.defaults.animation.easing = 'easeInOutQuart';
```

### B.6 Responsive Chart Sizing

```javascript
// For charts in the Writing preview panel
export const CHART_SIZES = {
  preview: {
    width: '100%',
    height: 400,
    maintainAspectRatio: false,
  },
  export: {
    width: 800,
    height: 500,
    pixelRatio: 2, // Retina export
  },
  thumbnail: {
    width: 200,
    height: 140,
    maintainAspectRatio: true,
  }
};
```

### B.7 Accessibility Considerations

1. **Color Contrast:** Đảm bảo chart colors có contrast ratio ≥ 4.5:1
2. **Colorblind-friendly:** Sử dụng mixed palette với distinct hues
3. **Pattern fills:** Cho pie/bar charts, có option để thêm patterns
4. **Alt text:** AI generates `detailed_description` cho screen readers

```javascript
// Example accessible bar chart config
const accessibleOptions = {
  plugins: {
    legend: {
      labels: {
        generateLabels: (chart) => {
          // Add pattern indicators for colorblind users
          const original = Chart.defaults.plugins.legend.labels.generateLabels(chart);
          return original.map((label, i) => ({
            ...label,
            text: `${label.text} (Series ${i + 1})`,
          }));
        }
      }
    }
  }
};
```

---

**KẾT THÚC TÀI LIỆU ABTS SPECS**