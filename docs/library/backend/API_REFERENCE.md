# Cramer Backend API Reference

> **Generated**: January 6, 2026  
> **Version**: 1.0.0  
> **Base URL**: `http://localhost:8080`

## Overview

The Cramer IELTS Learning Platform backend provides a comprehensive REST API for managing IELTS test content, user progress, AI-powered grading, subscriptions, and administrative functions.

### Authentication

All `/api/**` routes (except explicitly public endpoints) require JWT authentication via Supabase tokens.

**Header Format:**
```
Authorization: Bearer <supabase_jwt_token>
```

### Response Format

All endpoints return JSON responses. Successful responses typically return:
- `200 OK` - Successful request
- `201 Created` - Resource created successfully
- `204 No Content` - Successful deletion

Error responses include:
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Missing or invalid JWT
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `429 Too Many Requests` - Rate limit exceeded

---

## Table of Contents

1. [Public Endpoints](#1-public-endpoints)
2. [Authentication](#2-authentication)
3. [Test Management](#3-test-management)
4. [Test Attempts](#4-test-attempts)
5. [Writing & AI Grading](#5-writing--ai-grading)
6. [Dashboard](#6-dashboard)
7. [Profile Management](#7-profile-management)
8. [Vocabulary](#8-vocabulary)
9. [AI Chat](#9-ai-chat)
10. [Subscriptions](#10-subscriptions)
11. [Credits (Lúa)](#11-credits-lúa)
12. [Payments](#12-payments)
13. [Courses](#13-courses)
14. [Quotas](#14-quotas)
15. [Admin: Content Management](#15-admin-content-management)
16. [Admin: User Management](#16-admin-user-management)
17. [Admin: Finance](#17-admin-finance)
18. [Admin: ABTS (AI Test Generation)](#18-admin-abts-ai-test-generation)
19. [Admin: Test Hierarchy](#19-admin-test-hierarchy)
20. [Admin: Activities & Audit](#20-admin-activities--audit)
21. [Admin: Dashboard](#21-admin-dashboard)
22. [Speaking Runtime](#22-speaking-runtime)
23. [Debug & Diagnostics](#23-debug--diagnostics)

---

## 1. Public Endpoints

These endpoints do not require authentication.

### HelloController

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/ping` | Health check endpoint |
| GET | `/api/db-check` | Database connectivity check |

#### GET /api/ping

**Response:**
```json
{
  "message": "pong from Cramer backend"
}
```

#### GET /api/db-check

**Response:**
```json
{
  "db": "ok",
  "value": 1
}
```

---

## 2. Authentication

### AuthController

**Base Path:** `/api/auth`

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/check-email` | Check if email exists | No |

#### POST /api/auth/check-email

Check if an email is already registered.

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "exists": true
}
```

---

## 3. Test Management

### TestController

**Base Path:** `/api/tests`

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/data` | Get test data (passages + questions without answers) | Yes |

#### GET /api/tests/data

Fetches SAFE test data (without correct answers exposed).

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| source | string | Yes | Exam source (e.g., "cam17") |
| test | integer | Yes | Test number (e.g., 1, 2, 3, 4) |
| skill | string | Yes | Skill type: reading, listening, writing |

**Response:** `List<TestSectionDTO>`

---

### SectionController

**Base Path:** `/api/sections`

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/` | Get all sections | Yes |
| GET | `/{id}` | Get section by ID | Yes |
| GET | `/{id}/full` | Get full section with questions | Yes |
| GET | `/exam/{examSource}` | Get sections by exam source | Yes |
| GET | `/exam/{examSource}/test/{testNumber}` | Get sections by exam source and test | Yes |
| GET | `/exam/{examSource}/test/{testNumber}/skill/{skill}` | Get sections for specific test and skill | Yes |
| GET | `/skill/{skill}` | Get sections by skill | Yes |
| GET | `/specific` | Get specific section by all parameters | Yes |
| GET | `/count/exam/{examSource}` | Count sections by exam source | Yes |
| POST | `/` | Create a new section | Yes |
| PUT | `/{id}` | Update a section | Yes |
| DELETE | `/{id}` | Delete a section | Yes |

---

### QuestionController

**Base Path:** `/api/questions`

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/` | Get all questions | Yes |
| GET | `/{id}` | Get question by ID | Yes |
| GET | `/section/{sectionId}` | Get questions by section ID | Yes |
| GET | `/uid/{questionUid}` | Get question by UID | Yes |
| GET | `/type/{questionType}` | Get questions by type | Yes |
| GET | `/section/{sectionId}/type/{questionType}` | Get questions by section and type | Yes |
| GET | `/types` | Get all question types | Yes |
| GET | `/count` | Get total question count | Yes |
| GET | `/count/section/{sectionId}` | Count questions by section | Yes |
| POST | `/` | Create a question | Yes |
| PUT | `/{id}` | Update a question | Yes |
| DELETE | `/{id}` | Delete a question | Yes |

---

## 4. Test Attempts

### TestAttemptController

**Base Path:** `/api/test-attempts`

Manages user test sessions including starting, saving progress, submitting, and reviewing.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/start` | Start or resume a test attempt | Yes |
| POST | `/{id}/submit` | Submit test for grading | Yes |
| POST | `/{id}/progress` | Save progress (answers, time, part) | Yes |
| POST | `/{id}/cancel` | Cancel and delete attempt | Yes |
| POST | `/{id}/resume` | Mark attempt for resume | Yes |
| POST | `/{id}/regrade` | Re-grade a completed attempt | Yes |
| GET | `/{id}/answers` | Get saved answers for attempt | Yes |
| GET | `/{id}/review` | Get full test review with corrections | Yes |
| DELETE | `/{id}` | Delete a test attempt | Yes |

#### POST /api/test-attempts/start

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| source | string | Yes | - | Exam source (e.g., "cam17") |
| test | string | Yes | - | Test number |
| skill | string | Yes | - | Skill: reading, listening, writing |
| forceNew | boolean | No | false | Force create new attempt (cancel existing) |

**Response:** `TestAttempt` entity

#### POST /api/test-attempts/{id}/submit

**Request Body:** `AnswerSubmissionDTO`
```json
{
  "answers": {
    "q1": "answer1",
    "q2": "answer2"
  }
}
```

**Response:** `TestResultDTO`
```json
{
  "score": 35,
  "totalQuestions": 40,
  "bandScore": 7.5,
  "timeTaken": 3540
}
```

#### POST /api/test-attempts/{id}/progress

**Request Body:** `SaveProgressDTO`
```json
{
  "answers": { "q1": "answer" },
  "timeLeft": 1800,
  "currentPart": 2
}
```

---

## 5. Writing & AI Grading

### WritingController

**Base Path:** `/api/writing`

Handles IELTS Writing test submissions and AI-powered grading using DeepSeek.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/draft/{attemptId}` | Save essay draft | Yes |
| POST | `/submit/{attemptId}` | Submit essays for AI grading | Yes |
| GET | `/status/{attemptId}` | Get grading status | Yes |
| GET | `/review/{attemptId}` | Get full writing review with feedback | Yes |
| GET | `/submissions/{attemptId}` | Get all submissions for attempt | Yes |
| POST | `/validate-api-key` | Validate LLM API key | Yes |
| POST | `/regrade/{attemptId}` | Re-grade a writing attempt | Yes |

#### POST /api/writing/submit/{attemptId}

**Request Body:** `WritingSubmitDTO`
```json
{
  "essays": [
    {
      "taskNumber": 1,
      "essayText": "The chart shows..."
    },
    {
      "taskNumber": 2,
      "essayText": "In today's world..."
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Grading started",
  "gradingIds": [1, 2]
}
```

#### GET /api/writing/review/{attemptId}

**Response:** `WritingReviewDTO`
```json
{
  "attemptId": 123,
  "submissions": [
    {
      "taskNumber": 1,
      "essayText": "...",
      "status": "GRADED",
      "overallBand": 7.0,
      "taskAchievement": 7.0,
      "coherenceCohesion": 7.0,
      "lexicalResource": 7.0,
      "grammaticalAccuracy": 7.0,
      "feedback": "Your essay demonstrates..."
    }
  ]
}
```

---

## 6. Dashboard

### DashboardController

**Base Path:** `/api/dashboard`

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/summary` | Get user dashboard summary | Yes |
| POST | `/target` | Save user target score | Yes |

#### GET /api/dashboard/summary

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 3 | Items per page |
| search | string | - | Search term |

**Response:** `DashboardSummaryDTO`

#### POST /api/dashboard/target

**Request Body:** `TargetDTO`
```json
{
  "overallTarget": 7.5,
  "readingTarget": 7.0,
  "listeningTarget": 8.0,
  "writingTarget": 7.0,
  "speakingTarget": 7.0,
  "examDate": "2026-06-15"
}
```

---

## 7. Profile Management

### ProfileController

**Base Path:** `/api/profiles`

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/{id}` | Get profile by UUID | Yes |
| PUT | `/{id}` | Update profile | Yes |

**Note:** IDOR protection is enforced - users can only access their own profile.

#### PUT /api/profiles/{id}

**Request Body:** `ProfileDTO`
```json
{
  "username": "john_doe",
  "fullName": "John Doe",
  "avatarUrl": "https://...",
  "targetBand": 7.5,
  "llmApiKey": "sk-...",
  "llmModel": "deepseek-chat",
  "llmProvider": "deepseek"
}
```

---

## 8. Vocabulary

### VocabularyController

**Base Path:** `/api/vocabulary`

CRUD operations and AI-powered translation for vocabulary entries.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/` | List vocabulary with pagination/search | Yes |
| GET | `/{id}` | Get vocabulary by ID | Yes |
| GET | `/stats` | Get vocabulary statistics | Yes |
| POST | `/` | Create vocabulary entry | Yes |
| POST | `/translate` | Translate word using AI | Yes |
| PUT | `/{id}` | Update vocabulary entry | Yes |
| PUT | `/{id}/toggle-mastered` | Toggle mastered status | Yes |
| DELETE | `/{id}` | Delete vocabulary entry | Yes |

#### GET /api/vocabulary

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size |
| sortBy | string | createdAt | Sort field |
| sortDir | string | desc | Sort direction |
| search | string | - | Search term |
| filter | string | all | Filter: all, mastered, unmastered |

#### POST /api/vocabulary

**Request Body:** `VocabularyCreateDTO`
```json
{
  "word": "ubiquitous",
  "context": "Technology is ubiquitous in modern life",
  "autoTranslate": true
}
```

#### POST /api/vocabulary/translate

**Request Body:**
```json
{
  "word": "ubiquitous",
  "context": "optional context sentence"
}
```

**Response:**
```json
{
  "word": "ubiquitous",
  "translation": "phổ biến khắp nơi",
  "phonetic": "/juːˈbɪkwɪtəs/",
  "partOfSpeech": "adjective",
  "definition": "present, appearing, or found everywhere",
  "example": "Smartphones have become ubiquitous."
}
```

---

## 9. AI Chat

### ChatController

**Base Path:** `/api/chat`

AI chatbot functionality for IELTS learning assistant.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/` | Send message to AI assistant | Yes |
| GET | `/history` | Get chat history | Yes |
| GET | `/remaining` | Get remaining questions for today | Yes |
| DELETE | `/history` | Clear chat history | Yes |

**Rate Limits by Tier:**
- Cramerie (Free): 20 messages/day
- Cramerich: 100 messages/day
- Cramerous: Unlimited

#### POST /api/chat

**Request Body:** `ChatRequestDTO`
```json
{
  "message": "How do I improve my IELTS writing score?"
}
```

**Response:** `ChatResponseDTO`
```json
{
  "success": true,
  "message": "To improve your IELTS writing...",
  "remainingQuestions": 19
}
```

---

## 10. Subscriptions

### SubscriptionController

**Base Path:** `/api/subscriptions`

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/tiers` | Get all subscription tiers | Yes |
| GET | `/tiers/{code}` | Get tier by code | Yes |
| GET | `/current` | Get current user's subscription | Yes |
| GET | `/grading-status` | Check AI grading availability | Yes |
| GET | `/gradings-remaining` | Get remaining AI gradings | Yes |
| GET | `/chat-limit` | Get daily chat message limit | Yes |
| GET | `/my-status` | Get comprehensive subscription status | Yes |
| PUT | `/ai-grading` | Toggle AI grading preference | Yes |

#### GET /api/subscriptions/tiers

**Response:** `List<SubscriptionTierDTO>`
```json
[
  {
    "code": "cramerie",
    "name": "Cramerie",
    "priceVnd": 0,
    "monthlyGradings": 0,
    "dailyChatLimit": 20,
    "initialLua": 50
  },
  {
    "code": "cramerich",
    "name": "Cramerich",
    "priceVnd": 79000,
    "monthlyGradings": 5,
    "dailyChatLimit": 100,
    "initialLua": 100
  },
  {
    "code": "cramerous",
    "name": "Cramerous",
    "priceVnd": 149000,
    "monthlyGradings": 10,
    "dailyChatLimit": -1,
    "initialLua": 200
  }
]
```

#### GET /api/subscriptions/my-status

**Response:** `SubscriptionStatusDTO`
```json
{
  "tier": { "code": "cramerich", "name": "Cramerich", "...": "..." },
  "subscription": { "status": "ACTIVE", "expiresAt": "2026-02-15T..." },
  "usage": {
    "gradingsUsed": 2,
    "gradingsTotal": 5,
    "chatUsedToday": 15,
    "chatLimit": 100
  },
  "credits": { "balance": 150, "lifetimeEarned": 200 },
  "recentPayments": [...]
}
```

---

## 11. Credits (Lúa)

### CreditController

**Base Path:** `/api/credits`

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/` | Get user's credit balance | Yes |
| GET | `/check/{amount}` | Check if user has enough credits | Yes |
| GET | `/transactions` | Get transaction history | Yes |
| GET | `/stats` | Get aggregated user statistics | Yes |
| GET | `/packages` | Get available Lúa packages | Yes |
| GET | `/history` | Get transaction history with filter | Yes |
| POST | `/purchase` | Initiate Lúa package purchase | Yes |

#### GET /api/credits

**Response:** `UserCreditDTO`
```json
{
  "balance": 150,
  "lifetimeEarned": 200,
  "lifetimeSpent": 50
}
```

#### GET /api/credits/packages

**Response:** `List<LuaPackage>`
```json
[
  { "code": "lua_100", "amount": 100, "priceVnd": 10000, "name": "Túi Lúa", "bonus": 0 },
  { "code": "lua_500", "amount": 500, "priceVnd": 45000, "name": "Bao Lúa", "bonus": 10 },
  { "code": "lua_2000", "amount": 2000, "priceVnd": 150000, "name": "Xe Lúa", "bonus": 25 }
]
```

---

## 12. Payments

### PaymentController

**Base Path:** `/api/payments`

Payment processing via PayOS gateway.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/subscription` | Create subscription payment | Yes |
| POST | `/lua` | Create Lúa pack payment | Yes |
| POST | `/webhook` | PayOS webhook (public) | No |
| GET | `/status/{orderCode}` | Get payment status | Yes |
| GET | `/history` | Get payment history | Yes |
| GET | `/lua-packs` | Get Lúa pack options | No |
| GET | `/config-status` | Check PayOS configuration | No |

#### POST /api/payments/subscription

**Request Body:** `PaymentCreateDTO`
```json
{
  "type": "SUBSCRIPTION",
  "tierCode": "cramerich"
}
```

**Response:** `PaymentResponseDTO`
```json
{
  "success": true,
  "orderCode": 1234567890,
  "paymentUrl": "https://pay.payos.vn/...",
  "amount": 79000
}
```

#### POST /api/payments/webhook

**Note:** This endpoint is public (no auth) but verifies HMAC signature from PayOS.

**Request Body:** `PayOSWebhookDTO`
```json
{
  "code": "00",
  "data": {
    "orderCode": 1234567890,
    "amount": 79000,
    "status": "PAID"
  },
  "signature": "..."
}
```

---

## 13. Courses

### CourseController

**Base Path:** `/api/courses`

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/` | Get all courses (paginated) | Yes |
| GET | `/v2` | Get all test sets (full DTOs) | Yes |
| GET | `/{courseName}/tests` | Get tests for a course | Yes |
| GET | `/{courseCode}/details` | Get course details | Yes |

#### GET /api/courses

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 6 | Page size |
| search | string | - | Search term |

---

## 14. Quotas

### QuotaController

**Base Path:** `/api/quotas`

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/` | Get current quota status | Yes |
| GET | `/can-attempt` | Pre-check if attempt is allowed | Yes |
| GET | `/check` | Alternative check endpoint | Yes |

#### GET /api/quotas/can-attempt

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| skill | string | - | READING, LISTENING, WRITING, SPEAKING |
| ai | boolean | false | Whether this is an AI-graded attempt |

**Response:** `BillingResultDTO`
```json
{
  "allowed": true,
  "reason": null,
  "quotaRemaining": 5
}
```

---

## 15. Admin: Content Management

### AdminContentController

**Base Path:** `/api/admin/content`

**Required Header:** `X-User-Id: <admin_user_id>`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/topics` | Get topics with tests |
| GET | `/overview` | Get content statistics |
| GET | `/tests/{examSource}/{testNumber}` | Get test details |
| GET | `/tests/{examSource}/{testNumber}/{skill}/sections` | Get sections |
| GET | `/sections/{sectionId}` | Get section by ID |
| GET | `/sections/{sectionId}/questions` | Get questions for section |
| GET | `/questions/{questionId}` | Get question by ID |
| GET | `/activities` | Get recent activities |
| POST | `/tests` | Create test |
| POST | `/test-sets` | Create test set |
| POST | `/sections` | Create section |
| POST | `/sections/{sectionId}/questions` | Create question |
| PUT | `/tests/{testId}` | Update test |
| PUT | `/test-sets/{setId}` | Update test set |
| PUT | `/sections/{sectionId}` | Update section |
| PUT | `/questions/{questionId}` | Update question |
| PATCH | `/tests/{examSource}/{testNumber}/status` | Update test status |
| DELETE | `/tests/{testId}` | Delete test |
| DELETE | `/test-sets/{setId}` | Delete test set |
| DELETE | `/questions/{questionId}` | Delete question |

---

## 16. Admin: User Management

### AdminUserController

**Base Path:** `/api/admin/users`

**Required Header:** `X-User-Id: <admin_user_id>`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | List users (paginated, filterable) |
| GET | `/{id}` | Get user by ID |
| GET | `/stats` | Get user statistics |
| PATCH | `/{id}/status` | Update user status (ban/unban) |
| PATCH | `/{id}/credits` | Update user credits |
| PATCH | `/{id}/subscription` | Update user subscription tier |

#### GET /api/admin/users

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 25 | Page size |
| search | string | - | Search by username/email/name |
| status | string | - | ACTIVE, BANNED, DEACTIVATED |
| subscription | string | - | FREE, CRAMERICH |
| sortBy | string | createdAt | Sort field |
| sortOrder | string | desc | asc/desc |

---

## 17. Admin: Finance

### AdminFinanceController

**Base Path:** `/api/admin/finance`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/overview` | Get finance overview stats |
| GET | `/chart` | Get revenue chart data |
| GET | `/breakdown` | Get revenue breakdown by type |
| GET | `/transactions` | Get transactions list |

#### GET /api/admin/finance/overview

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| period | string | 30days | Time period: 7days, 30days, 90days, 1year |

---

## 18. Admin: ABTS (AI Test Generation)

### ABTSController

**Base Path:** `/api/admin/abts`

AI-Based Test Generation System for creating IELTS content.

**Required Header:** `X-User-Id: <admin_user_id>`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/generate/reading` | Generate Reading content |
| POST | `/generate/reading/stream` | Generate Reading with SSE streaming |
| POST | `/generate/listening` | Generate Listening content |
| POST | `/generate/listening/stream` | Generate Listening with SSE streaming |
| POST | `/generate/writing` | Generate Writing prompts |
| POST | `/generate/writing/stream` | Generate Writing with SSE streaming |
| POST | `/generate/questions` | Regenerate questions only |
| POST | `/validate` | Validate generated content |
| POST | `/save` | Save content to database |
| POST | `/refine/stream` | Refine content (SSE streaming) |
| GET | `/templates` | Get topic template categories |
| GET | `/templates/{categoryId}` | Get templates by category |
| GET | `/models` | Get available AI models |
| GET | `/status` | Get ABTS configuration status |

#### POST /api/admin/abts/generate/reading

**Request Body:** `GenerationRequestDTO`
```json
{
  "skill": "READING",
  "partConfigs": [
    {
      "partNumber": 1,
      "topic": "Climate Change",
      "passageLength": "MEDIUM",
      "questionTypes": ["TRUE_FALSE_NOT_GIVEN", "MATCHING_HEADINGS"]
    }
  ],
  "model": "deepseek-chat"
}
```

**Response:** `GenerationResponseDTO`

---

## 19. Admin: Test Hierarchy

### TestHierarchyController

**Base Path:** `/api/admin`

Manages TestSets, Tests, and Hashtags.

#### Test Sets

| Method | Path | Description |
|--------|------|-------------|
| GET | `/test-sets` | Get all test sets |
| GET | `/test-sets/{id}` | Get test set by ID |
| GET | `/test-sets/code/{code}` | Get test set by code |
| POST | `/test-sets` | Create test set |
| PUT | `/test-sets/{id}` | Update test set |
| DELETE | `/test-sets/{id}` | Delete test set |
| POST | `/test-sets/{id}/publish` | Publish test set |
| POST | `/test-sets/{id}/unpublish` | Unpublish test set |
| POST | `/test-sets/reorder` | Reorder test sets |
| GET | `/test-sets/{setId}/tests` | Get tests in a set |
| POST | `/test-sets/{setId}/tests` | Create test in set |

#### Tests

| Method | Path | Description |
|--------|------|-------------|
| GET | `/tests/{id}` | Get test by ID |
| GET | `/tests/lookup` | Get test by set code and number |
| PUT | `/tests/{id}` | Update test |
| DELETE | `/tests/{id}` | Delete test |
| POST | `/tests/{id}/publish` | Publish test |
| POST | `/tests/{id}/unpublish` | Unpublish test |
| PUT | `/tests/{id}/hashtags` | Update test hashtags |
| POST | `/tests/{id}/duplicate` | Duplicate test |
| GET | `/tests/{id}/sections` | Get test sections by skill |

#### Hashtags

| Method | Path | Description |
|--------|------|-------------|
| GET | `/hashtags` | Get all hashtags |
| GET | `/hashtags/category/{category}` | Get by category |
| GET | `/hashtags/search` | Search hashtags |
| GET | `/hashtags/popular` | Get popular hashtags |
| GET | `/hashtags/categories` | Get distinct categories |
| POST | `/hashtags` | Create hashtag |
| PUT | `/hashtags/{id}` | Update hashtag |
| DELETE | `/hashtags/{id}` | Delete hashtag (soft) |

---

## 20. Admin: Activities & Audit

### AdminActivityController

**Base Path:** `/api/admin/activities`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/users/{userId}` | Get user activities |
| GET | `/users/{userId}/recent` | Get recent user activities |
| GET | `/audit/users/{userId}` | Get audit logs for user |
| GET | `/audit` | Get all audit logs |

---

## 21. Admin: Dashboard

### AdminDashboardController

**Base Path:** `/api/admin/dashboard`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/stats` | Get dashboard statistics |
| GET | `/activities` | Get recent activities |
| GET | `/status` | Get system status |

#### GET /api/admin/dashboard/stats

**Response:**
```json
{
  "totalUsers": 1250,
  "activeUsers": 450,
  "newUsersThisMonth": 85,
  "totalTestAttempts": 5600,
  "totalQuestions": 2400,
  "publishedTests": 48,
  "totalVocabulary": 15000,
  "totalRevenue": 25000000,
  "changes": {
    "users": { "value": 12.5, "type": "up" },
    "revenue": { "value": 8.3, "type": "up" }
  }
}
```

---

## 22. Speaking Runtime

### SpeakingController

**Base Path:** `/api/speaking`

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/sessions` | Create a new Speaking session | Yes |
| GET | `/sessions/{id}` | Get session metadata and blueprint | Yes |
| POST | `/sessions/{id}/transcripts` | Retry-safe upsert for a turn transcript | Yes |
| POST | `/sessions/{id}/complete` | Finalize session and queue grading | Yes |
| POST | `/sessions/{id}/abandon` | Abandon session without deducting Lúa | Yes |
| GET | `/sessions/{id}/grading-status` | Poll grading status | Yes |
| GET | `/sessions/{id}/results` | Get graded Speaking result | Yes |
| GET | `/history` | Paginated history for the current user | Yes |

#### POST /api/speaking/sessions

**Request Body:**
```json
{
  "sessionMode": "FULL",
  "testId": 1,
  "accent": "british",
  "speed": 1.0
}
```

**Notes:**
- Requires an authenticated Supabase JWT
- Uses official Speaking authored content from the shared hierarchy
- Current official smoke-test target is `testId = 1`

#### POST /api/speaking/sessions/{id}/transcripts

**Request Body:**
```json
{
  "sourceQuestionId": 123,
  "partNumber": 1,
  "turnIndex": 1,
  "questionSnapshot": {
    "schemaVersion": 1,
    "partType": "PART_1",
    "promptText": "Do you like travelling?",
    "topicLabel": "Travel"
  },
  "audioStoragePath": "manual-tests/speaking/session-123/turn-001.webm",
  "transcriptText": "I enjoy travelling because it helps me relax.",
  "audioDurationSeconds": 25,
  "transcriptConfidence": 0.96
}
```

**Notes:**
- `turnIndex`, `sourceQuestionId`, and `questionSnapshot` must match the turn already frozen in `sessionBlueprint`
- For FULL mode, saving the Part 2 transcript may cause deferred Part 3 selection to be materialized

#### GET /api/speaking/history

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | integer | No | `0` | Page index |
| `size` | integer | No | `20` | Page size |
| `status` | string | No | - | Optional session status filter |

---

## 23. Debug & Diagnostics

### DatabaseTestController

**Base Path:** `/api/test`

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/db-full` | Comprehensive database test | Yes |

### DebugController

**Base Path:** `/api/debug`

**⚠️ Security:** Only enabled when `DEBUG_ENABLED=true` environment variable is set.

**Required Header:** `X-Debug-Key: <debug_secret_key>`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/activate-subscription` | Activate subscription (testing) |

---

## Summary

| Category | Controllers | Endpoints |
|----------|-------------|-----------|
| Public | 2 | 4 |
| Authentication | 1 | 1 |
| Test Management | 3 | 26 |
| Test Attempts | 1 | 9 |
| Writing & AI Grading | 1 | 7 |
| Dashboard | 1 | 2 |
| Profile | 1 | 2 |
| Vocabulary | 1 | 9 |
| AI Chat | 1 | 4 |
| Subscriptions | 1 | 8 |
| Credits | 1 | 7 |
| Payments | 1 | 7 |
| Courses | 1 | 4 |
| Quotas | 1 | 3 |
| Admin: Content | 1 | 18 |
| Admin: Users | 1 | 6 |
| Admin: Finance | 1 | 4 |
| Admin: ABTS | 1 | 14 |
| Admin: Test Hierarchy | 1 | 27 |
| Admin: Activities | 1 | 4 |
| Admin: Dashboard | 1 | 3 |
| Debug | 2 | 2 |

**Total Controllers Documented:** 25  
**Total Endpoints Documented:** 171

---

## Appendix: Common DTOs

### TestAttempt Status Enum
- `IN_PROGRESS` - Test is ongoing
- `COMPLETED` - Test submitted and graded
- `CANCELLED` - Test was cancelled

### Subscription Tier Codes
- `cramerie` - Free tier
- `cramerich` - Premium tier (79,000 VND/month)
- `cramerous` - Pro tier (149,000 VND/month)

### Question Types
- `MULTIPLE_CHOICE`
- `TRUE_FALSE_NOT_GIVEN`
- `YES_NO_NOT_GIVEN`
- `MATCHING_HEADINGS`
- `MATCHING_INFORMATION`
- `MATCHING_FEATURES`
- `MATCHING_SENTENCE_ENDINGS`
- `SENTENCE_COMPLETION`
- `SUMMARY_COMPLETION`
- `NOTE_COMPLETION`
- `TABLE_COMPLETION`
- `FLOW_CHART_COMPLETION`
- `DIAGRAM_LABELLING`
- `SHORT_ANSWER`

### Skill Types
- `READING`
- `LISTENING`
- `WRITING`
- `SPEAKING`

---

*Last updated: January 6, 2026*
