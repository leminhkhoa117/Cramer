# DeepSeek V3.2 Migration Guide

> **Migration Date:** December 12, 2025  
> **Status:** ✅ Completed and Deployed  
> **Migration Files:** `003_deepseek_migration.sql`, `004_add_image_description_column.sql`

---

## Overview

This document describes the migration from Google Gemini AI to DeepSeek V3.2 for IELTS Writing grading, including the implementation of text-based image descriptions for Task 1 charts/maps/diagrams.

## Motivation

### Why DeepSeek?

| Aspect | Gemini 2.5 | DeepSeek V3.2 | Winner |
|--------|-----------|---------------|--------|
| **Cost** | $0.075-0.30/1M input, $1.20/1M output | $0.028-0.28/1M input, $0.42/1M output | 🏆 DeepSeek (3-6x cheaper) |
| **API Format** | Google-specific | OpenAI-compatible | 🏆 DeepSeek (industry standard) |
| **Rate Limits** | 1,500 RPM (Pro), 10 RPM (Flash) | No documented limits | 🏆 DeepSeek |
| **Image Support** | ✅ Multimodal | ❌ Text-only | Gemini |
| **Output Length** | 8K tokens | 8K-64K tokens (reasoner) | 🏆 DeepSeek |

**Decision:** DeepSeek is significantly more cost-effective for our use case. The lack of image support is solved by storing text descriptions of charts/maps.

---

## Architecture Changes

### Database Schema

#### Migration 003: Column Renames

```sql
-- Rename gemini_api_key → llm_api_key
ALTER TABLE public.profiles 
RENAME COLUMN gemini_api_key TO llm_api_key;

-- Rename gemini_model → llm_model
ALTER TABLE public.profiles 
RENAME COLUMN gemini_model TO llm_model;

-- Add provider column
ALTER TABLE public.profiles 
ADD COLUMN IF NOT EXISTS llm_provider VARCHAR(50) DEFAULT 'deepseek';

-- Update existing model values
UPDATE public.profiles 
SET llm_model = 'deepseek-chat'
WHERE llm_model IN ('gemini-2.5-flash', 'gemini-2.5-flash-lite', 'gemini-2.5-pro');
```

#### Migration 004: Image Description Support

```sql
-- Add image_description column to sections table
ALTER TABLE public.sections 
ADD COLUMN IF NOT EXISTS image_description TEXT;

COMMENT ON COLUMN public.sections.image_description IS 
  'Detailed text description of Task 1 charts/maps/diagrams for AI grading when image input is not supported';
```

### Backend Changes

| Component | Old (Gemini) | New (DeepSeek) |
|-----------|-------------|----------------|
| Service | `GeminiGradingService.java` | `LLMGradingService.java` |
| API Endpoint | `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` | `https://api.deepseek.com/chat/completions` |
| Auth Method | API key in URL query param | Bearer token in Authorization header |
| Request Format | Google-specific with `systemInstruction` + `contents` | OpenAI-compatible with `messages` array |
| Response Format | `candidates[0].content.parts[0].text` | `choices[0].message.content` |

### Entity Changes

**Profile.java:**
```java
// Before
@Column(name = "gemini_api_key")
private String geminiApiKey;

@Column(name = "gemini_model")
private String geminiModel;

// After
@Column(name = "llm_api_key")
private String llmApiKey;

@Column(name = "llm_model")
private String llmModel;

@Column(name = "llm_provider")
private String llmProvider = "deepseek";
```

**Section.java:**
```java
// New field added
@Column(name = "image_description", columnDefinition = "TEXT")
private String imageDescription;
```

### API Request Format

**Gemini (Old):**
```json
{
  "systemInstruction": {
    "parts": [{"text": "system prompt"}]
  },
  "contents": [
    {"parts": [{"text": "user prompt"}]}
  ],
  "generationConfig": {
    "temperature": 0.4,
    "maxOutputTokens": 8192
  }
}
```

**DeepSeek (New):**
```json
{
  "model": "deepseek-chat",
  "messages": [
    {"role": "system", "content": "system prompt"},
    {"role": "user", "content": "user prompt"}
  ],
  "temperature": 0.4,
  "max_tokens": 16384,
  "response_format": {"type": "json_object"},
  "stream": false
}
```

---

## Image Description Feature

### Problem Statement

DeepSeek V3.2 does not support multimodal input (images). IELTS Writing Task 1 often requires describing charts, maps, or diagrams shown in an image.

### Solution

Store **detailed text descriptions** of Task 1 images in the database (`sections.image_description` column). The AI grading service includes this description in the prompt.

### Implementation Flow

1. **Database:** `image_description` column stores detailed text (1,500-2,500 chars)
2. **Backend:** `Section` entity includes `imageDescription` field
3. **Service:** `AsyncGradingService` retrieves `section.getImageDescription()`
4. **Grading:** `LLMGradingService.buildUserPrompt()` adds description to prompt:

```
## Đề bài (Task Prompt):
[original task prompt]

## Mô tả chi tiết hình ảnh/biểu đồ (Image/Chart Description):
[detailed text description from database]

**Lưu ý:** Đánh giá bài viết dựa trên việc thí sinh có mô tả chính xác 
các thông tin trong biểu đồ/bản đồ/sơ đồ này hay không.

## Bài viết của thí sinh:
[student's essay]
```

### Example Descriptions Added

**Cambridge 17 Test 1 - Norbiton Industrial Area (2,173 chars):**
- Current layout: 7 factories around roundabout, river to north, farmland beyond
- Planned development: Housing replacing factories, playground, school, shopping center, medical centre, bridge to farmland

**Cambridge 17 Test 2 - Police Budget 2017-2018 (1,603 chars):**
- Budget sources table with 3 categories (National Gov, Local Taxes, Other)
- Spending pie charts showing percentages for Salaries (75%→69%), Technology (8%→14%), Buildings (17%→17%)
- Key trends: Technology investment doubled, local taxes increased £11.1m

---

## Frontend Changes

### Profile.jsx AI Settings

**Before (Gemini):**
```jsx
const GEMINI_MODELS = [
  { value: 'gemini-2.5-flash', label: 'Gemini 2.5 Flash' },
  { value: 'gemini-2.5-pro', label: 'Gemini 2.5 Pro' }
];
```

**After (DeepSeek):**
```jsx
const LLM_MODELS = [
  { 
    value: 'deepseek-chat', 
    label: 'DeepSeek V3.2 (Non-thinking)', 
    description: 'Nhanh, giá rẻ ($0.028-$0.28/1M input, $0.42/1M output)' 
  },
  { 
    value: 'deepseek-reasoner', 
    label: 'DeepSeek V3.2 (Thinking)', 
    description: 'Chính xác hơn, output lên đến 64K tokens' 
  }
];
```

### UI Text Updates

| Element | Old | New |
|---------|-----|-----|
| Section Title | "Gemini API Key" | "DeepSeek API Key" |
| Link Text | "Lấy API key từ Google AI Studio" | "Lấy API key từ DeepSeek Platform" |
| Link URL | `https://aistudio.google.com/apikey` | `https://platform.deepseek.com/api_keys` |
| Placeholder | `AIza...` | `sk-...` |

---

## Testing Checklist

- [x] Database migrations applied successfully
- [x] Backend compiles without errors
- [x] Profile API returns `llmApiKey`, `llmModel`, `llmProvider` correctly
- [x] Frontend Profile page displays DeepSeek settings
- [x] API key validation works with DeepSeek keys
- [x] Writing Task 1 grading uses image description
- [x] Writing Task 2 grading works (no image needed)
- [x] No references to Gemini in active code paths

---

## Breaking Changes

### For Users

**⚠️ Users must re-enter their API keys:**
- Old Gemini API keys (format: `AIza...`) will not work
- New DeepSeek API keys required (format: `sk-...`)
- Get keys from: https://platform.deepseek.com/api_keys

### For Developers

**Legacy Code:**
- `GeminiGradingService.java` is **kept for reference** but no longer used
- All active services now use `LLMGradingService`
- Database columns renamed: `gemini_*` → `llm_*`

---

## Future Enhancements

### Multi-Provider Support

The current architecture supports adding more LLM providers:

```java
// Profile.java
private String llmProvider; // "deepseek", "openai", "anthropic"
```

Future providers could include:
- OpenAI GPT-4o (has image support)
- Anthropic Claude 3.5 Sonnet (has image support)
- Google Gemini (legacy support)

### Image Description Management

**Manual Process (Current):**
1. Analyze image manually
2. Write detailed description (1,500-2,500 chars)
3. Insert via SQL: `UPDATE sections SET image_description = '...' WHERE id = X;`

**Future Automation:**
- Admin UI for adding/editing descriptions
- OCR + GPT-4o Vision to auto-generate descriptions
- Batch processing for Cambridge tests

---

## Rollback Plan

If issues arise, rollback is straightforward:

1. **Revert database columns:**
   ```sql
   ALTER TABLE profiles RENAME COLUMN llm_api_key TO gemini_api_key;
   ALTER TABLE profiles RENAME COLUMN llm_model TO gemini_model;
   ALTER TABLE profiles DROP COLUMN llm_provider;
   ```

2. **Revert code:**
   - Restore `GeminiGradingService` references
   - Update `WritingSubmissionService`, `AsyncGradingService`

3. **Frontend:**
   - Revert `Profile.jsx` to Gemini UI

**Note:** The `image_description` column can remain (harmless if unused).

---

## Cost Analysis

### Estimated Savings

**Assumptions:**
- Average essay: 300 words = ~450 tokens input
- System prompt: ~3,000 tokens
- AI response: ~2,000 tokens output
- **Total per grading:** ~5,500 tokens

| Provider | Cost per Grading | Cost per 100 Gradings |
|----------|------------------|-----------------------|
| Gemini 2.5 Flash | ~$0.011 | $1.10 |
| DeepSeek V3.2 | ~$0.003 | $0.30 |
| **Savings** | **~73%** | **$0.80** |

**At scale (1,000 gradings/month):**
- Gemini: $11/month
- DeepSeek: $3/month
- **Savings: $8/month = $96/year**

---

## References

- DeepSeek API Docs: https://platform.deepseek.com/api-docs/
- Migration Files: `docs/backend/migrations/003_deepseek_migration.sql`, `004_add_image_description_column.sql`
- Service Implementation: `backend/src/main/java/com/cramer/service/LLMGradingService.java`
- Frontend Changes: `frontend/src/pages/Profile.jsx`

---

**Migration Status:** ✅ **COMPLETED AND DEPLOYED**  
**Next Priority:** Dashboard Completion (Phase 1.2)
