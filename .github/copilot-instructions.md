## Quick context for AI code agents

This repository is a two-part web app: a Spring Boot backend (Maven, Java 21) and a React + Vite frontend. Primary data lives in a Supabase Postgres instance (see `docs/backend/supabase-backend.md`). The backend exposes REST APIs under `/api/**` and uses JWTs (Supabase tokens) for authentication.

Keep changes small, well-scoped and documented. If you modify DB schema, update `docs/backend/*` and coordinate with the Supabase project.

## Where to look first (high-value files)
- Root README: `README.md` — project overview and links to backend docs.
- Backend main: `backend/src/main/java/com/cramer/CramerBackendApplication.java`.
- Backend config: `backend/src/main/java/com/cramer/config/` — especially `SecurityConfig.java`, `JwtAuthFilter.java`, `ConditionalDataSourceConfig.java`, `WebConfig.java`.
- Backend env & run: `backend/.env` (example), `backend/run-local.cmd`, `backend/run-app.ps1`, `backend/BUILD_INSTRUCTIONS.md` and `backend/pom.xml`.
- Backend properties: `backend/src/main/resources/application.properties` (datasource, supabase keys, swagger path).
- API surface and client: `frontend/src/api/backendApi.js` — shows axios usage, token provider (`setupApiClient`) and endpoints.
- Frontend: `frontend/package.json`, `frontend/README.md`, Vite/Tailwind config files.
- Docker: `docker-compose.yml` and `docker-compose.dev.yml` for containerized runs.

## Runtime & build essentials (exact commands)
- Backend (Windows, preferred):
  - Start dev app (uses root `.env`): `backend\run-local.cmd` (CMD) or `backend\run-app.ps1` (PowerShell).
  - Build jar: `cd backend && .\mvnw.cmd clean package -DskipTests` then run `java -jar target\cramer-backend-0.0.1-SNAPSHOT.jar`.
  - Maven run: `cd backend && .\mvnw.cmd spring-boot:run`.
  - Swagger UI: http://localhost:8080/swagger-ui.html (use Authorize with a Supabase JWT).

- Frontend:
  - Install: `cd frontend && npm install`
  - Dev server: `npm run dev` (default port 5173)
  - Important env vars: `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`, optional `VITE_API_BASE_URL`.

- Docker: use `docker-compose.yml` or `docker-compose.dev.yml` at repo root.

## Authentication & API expectations
- All `/api/**` routes are guarded by Spring Security: `SecurityConfig` configures `.requestMatchers("/api/**").authenticated()` and inserts `JwtAuthFilter` before username/password filter.
- `JwtAuthFilter` extracts `userId` from Supabase JWTs and grants `ROLE_USER` to validated tokens — controllers expect authenticated requests with an `Authorization: Bearer <token>` header.
- Frontend sets Authorization with the `setupApiClient` token provider (see `frontend/src/api/backendApi.js`).
- JWT authentication is fully functional as of 2025-12-04.

## Data & DB patterns
- Schema lives in Supabase (Postgres). Backend uses `spring-data-jpa` + `jsonb` payloads for question content.
- **RLS (Row Level Security)**: Tables `test_attempts` and `user_answers` have RLS enabled. Service role policies have been added to allow backend operations (DELETE, etc.).
- Conditional DB wiring: `ConditionalDataSourceConfig` only creates the `DataSource` if `spring.datasource.url` is set.

## Test Attempt Flow
- `POST /api/test-attempts/start` — creates or resumes an IN_PROGRESS attempt.
- `POST /api/test-attempts/{id}/progress` — saves answers, timeLeft, currentPart.
- `POST /api/test-attempts/{id}/submit` — submits and scores the attempt (marks as COMPLETED).
- `POST /api/test-attempts/{id}/cancel` — **deletes** the attempt and all associated answers (fixed 2025-12-04).
- `DELETE /api/test-attempts/{id}` — alternative delete endpoint.
- Frontend uses `hasFetchedRef` to prevent duplicate API calls in React StrictMode.

## Writing Test Attempt Flow (with forceNew parameter)
- `POST /api/test-attempts/start?forceNew=false` (default):
  - If IN_PROGRESS exists → return it (resume)
  - If COMPLETED exists → return COMPLETED (frontend shows choice modal)
  - If CANCELLED exists → create new attempt
- `POST /api/test-attempts/start?forceNew=true`:
  - Cancel all IN_PROGRESS attempts and create new
- Frontend `location.state.forceNew` controls this behavior:
  - Dashboard "Làm lại" button → `forceNew=true`
  - Direct URL access → `forceNew=false` → shows modal if COMPLETED
- `ResumeConfirmationModal` handles both IN_PROGRESS ("Tiếp tục") and COMPLETED ("Xem kết quả") states.

## Conventions & patterns to follow
- API routes: controllers are under `backend/src/main/java/com/cramer/controller/` and use `/api/{resource}` conventions.
- Use Springdoc OpenAPI (Swagger) for new endpoints.
- Repository delete methods should use explicit `@Query` annotations with `@Modifying(clearAutomatically = true, flushAutomatically = true)` for reliable deletion.

## Edit & testing rules for agents
- Small PRs only: change one component at a time.
- After making backend changes, always rebuild: `cd backend && .\mvnw.cmd clean package -DskipTests` and restart.
- Don't commit secrets: `.env` at repo root is local only.

## Known Issues (Resolved)
- ✅ Test cancellation now properly deletes attempts (fixed 2025-12-04).
- ✅ RLS policies added for service_role on test_attempts and user_answers tables.
- ✅ Duplicate API calls in React StrictMode prevented with refs.

## Recent Changes (2025-12-05)
When adding new features, especially those that touch both the backend and frontend, please document the changes in the repo docs and update `GEMINI.md`/`AGENTS.md` accordingly.

- **DeepSeek V3.2 Migration (2025-12-12) ✅ COMPLETED:**
  - Migrated from Google Gemini to DeepSeek V3.2 for AI writing grading
  - Created `LLMGradingService.java` (new) - uses OpenAI-compatible API format
  - Renamed entity fields: `geminiApiKey` → `llmApiKey`, `geminiModel` → `llmModel`
  - Added new field: `llmProvider` (default: "deepseek")
  - Updated `Profile.java`, `ProfileDTO.java`, `ProfileServiceImpl.java`
  - Updated `WritingSubmissionService.java`, `AsyncGradingService.java` to use `LLMGradingService`
  - Updated `Profile.jsx` UI: DeepSeek API key input, model selector (deepseek-chat, deepseek-reasoner)
  - **Image Description Feature:** Added `image_description` column to `sections` table to store detailed text descriptions of Task 1 charts/maps/diagrams. DeepSeek doesn't support image input, so AI grading uses text descriptions instead.
  - DB Migrations: 
    - `docs/backend/migrations/003_deepseek_migration.sql` (column renames)
    - `docs/backend/migrations/004_add_image_description_column.sql` (image description support)
  - Added descriptions for Cambridge 17 Test 1 (Norbiton maps) and Test 2 (Police Budget charts)
  - Updated `Section.java` entity with `imageDescription` field
  - Modified `LLMGradingService.buildUserPrompt()` to include image description in AI prompt
  - **Status:** Fully deployed and functional

- **Subscription, Credit (Lúa), and Achievement Backend (2025-12-13) ✅ COMPLETED:**
  - Complete backend infrastructure for monetization and gamification
  - **New Entities** (in `backend/src/main/java/com/cramer/entity/`):
    - `SubscriptionTier.java` — Tier definitions (Cramerie, Cramerich, Cramerous)
    - `UserSubscription.java` — User's active subscription with usage tracking
    - `UserCredit.java` — User's Lúa balance and lifetime stats
    - `CreditTransaction.java` — Transaction history (earn/spend)
    - `Achievement.java` — Badge definitions with categories and rewards
    - `UserAchievement.java` — Badges earned by users
    - `UserStreak.java` — Login streak tracking
    - `ChatbotUsage.java` — Daily AI chat usage limits
  - **New DTOs** (in `backend/src/main/java/com/cramer/dto/`):
    - `SubscriptionTierDTO.java`, `UserSubscriptionDTO.java`, `UserCreditDTO.java`
    - `CreditTransactionDTO.java`, `AchievementDTO.java`, `UserAchievementDTO.java`
    - `UserFullStatsDTO.java` — Aggregated user stats (sub + credits + streak + achievements)
    - `GradingStatusDTO.java` — AI grading availability check
  - **New Repositories** (in `backend/src/main/java/com/cramer/repository/`):
    - `SubscriptionTierRepository.java`, `UserSubscriptionRepository.java`
    - `UserCreditRepository.java`, `CreditTransactionRepository.java`
    - `AchievementRepository.java`, `UserAchievementRepository.java`
    - `UserStreakRepository.java`, `ChatbotUsageRepository.java`
  - **New Services** (interfaces in `service/`, implementations in `service/implement/`):
    - `SubscriptionService.java` / `SubscriptionServiceImpl.java` — Tier management, AI grading limits
    - `CreditService.java` / `CreditServiceImpl.java` — Lúa balance, earn/spend transactions
    - `AchievementService.java` / `AchievementServiceImpl.java` — Badge awarding, milestone checks
  - **New Controllers**:
    - `SubscriptionController.java` — `/api/subscriptions/tiers`, `/current`, `/grading-status`
    - `CreditController.java` — `/api/credits`, `/transactions`, `/stats`
    - `AchievementController.java` — `/api/achievements`, `/mine`, `/unnotified`
  - **DB Migration**: `docs/backend/migrations/006_subscription_credit_achievement.sql`
    - Tables: `subscription_tiers`, `user_subscriptions`, `user_credits`, `credit_transactions`
    - Tables: `achievements`, `user_achievements`, `user_streaks`, `chatbot_usage`
    - Pre-populated tiers (Cramerie/Cramerich/Cramerous) and default achievements
    - RLS policies for all tables
  - **Pricing Tiers**:
    - 🌾 Cramerie (Free): 0 AI gradings, 20 chat/day, 50 initial Lúa
    - 🌻 Cramerich (79,000đ): 5 AI gradings/mo, 100 chat/day, 100 initial Lúa
    - 🌟 Cramerous (149,000đ): 10 AI gradings/mo, unlimited chat, 200 initial Lúa
  - **Status:** Backend complete, ready for frontend integration

- **PayOS Payment Gateway Integration (2025-12-14) ✅ COMPLETED:**
  - Vietnamese payment gateway integration for subscription and Lúa purchases
  - **Backend Configuration** (`backend/src/main/java/com/cramer/config/`):
    - `PayOSConfig.java` — PayOS client configuration (clientId, apiKey, checksumKey)
  - **Environment Variables Required**:
    - `PAYOS_CLIENT_ID` — Client ID from PayOS Merchant Dashboard
    - `PAYOS_API_KEY` — API Key from PayOS Merchant Dashboard
    - `PAYOS_CHECKSUM_KEY` — Checksum Key for HMAC-SHA256 signatures
    - `PAYOS_RETURN_URL` — Redirect URL after successful payment (default: http://localhost:5173/payment/success)
    - `PAYOS_CANCEL_URL` — Redirect URL after cancelled payment (default: http://localhost:5173/payment/cancel)
  - **New Entity**: `PaymentOrder.java` — Tracks payment attempts with status (PENDING/PAID/CANCELLED/EXPIRED)
  - **New Repository**: `PaymentOrderRepository.java`
  - **New Service**: `PaymentService.java` / `PaymentServiceImpl.java`
    - `createSubscriptionPayment()` — Creates PayOS payment link for tier upgrades
    - `createLuaPackPayment()` — Creates PayOS payment link for Lúa purchases
    - `handleWebhook()` — Processes PayOS webhook, updates order, grants subscription/credits
    - HMAC-SHA256 signature generation and verification
  - **New Controller**: `PaymentController.java`
    - `POST /api/payments/subscription` — Create subscription payment
    - `POST /api/payments/lua` — Create Lúa pack payment
    - `POST /api/payments/webhook` — PayOS webhook (public, no auth)
    - `GET /api/payments/status/{orderCode}` — Check payment status
    - `GET /api/payments/history` — User payment history
    - `GET /api/payments/lua-packs` — Available Lúa packs (public)
    - `GET /api/payments/config-status` — Check if PayOS is configured (public)
  - **Frontend**:
    - `PaymentSuccessPage.jsx` — Success page with order details
    - `PaymentCancelPage.jsx` — Cancel page with retry options
    - `PaymentPage.css` — Glassmorphic styling for payment pages
    - Updated `PricingPage.jsx` — Calls payment API on tier/Lúa pack click
    - Updated `TierCard.jsx`, `LuaPackCard.jsx` — Loading state during payment processing
    - Added `paymentApi` to `backendApi.js`
  - **Routes**: `/payment/success`, `/payment/cancel`
  - **DB Migration**: `docs/backend/migrations/007_payos_payment_integration.sql`
    - Table: `payment_orders` with RLS policies
  - **Security**: Webhook endpoint is public (no JWT required) but verifies HMAC signature
  - **Status:** Fully deployed, pending PayOS production credentials

- **Subscription Management Page (2025-12-13) ✅ COMPLETED:**
  - Comprehensive subscription status page at `/subscription`
  - **Backend**:
    - `SubscriptionStatusDTO.java` — Comprehensive DTO with nested classes for tier, usage, credits, payments
    - `GET /api/subscriptions/my-status` — Returns full subscription status
    - Updated `SubscriptionService.java` and `SubscriptionServiceImpl.java` with `getSubscriptionStatus()` method
  - **Frontend**:
    - `SubscriptionPage.jsx` — Full subscription management page with:
      - Hero section with tier badge and status
      - Subscription details card (dates, progress, upgrade CTA)
      - Usage tracking cards (AI gradings, daily chat)
      - Lúa balance card with buy more link
      - Features list from tier
      - Payment history section
    - `SubscriptionPage.css` — Glassmorphic styling with tier-specific colors
    - Added route `/subscription` in `App.jsx`
    - Added "Gói đăng ký" link in Header dropdown menu
    - Added `getMyStatus()` to `subscriptionApi` in `backendApi.js`
  - **UI Features**:
    - Tier-specific colors: Cramerie (green), Cramerich (gold), Cramerous (purple)
    - Animated progress bars for usage tracking
    - Responsive design for mobile

- **State Management Migration to Zustand (2025-12-10):**
  - Migrated from React Context to Zustand for all global state management
  - New stores in `frontend/src/stores/`:
    - `useAuthStore.js` — Auth user, session, login/logout/OAuth actions
    - `useProfileStore.js` — User profile with auto-sync to auth changes
    - `useTestStore.js` — Test-taking UI state (answers, timer, modals, navigation)
    - `useTestSessionStore.js` — Test API operations with 5-min caching TTL
    - `useDashboardStore.js` — Dashboard data with pagination (sessionStorage persisted)
    - `useCourseStore.js` — Courses list with caching + pagination
    - `index.js` — Clean re-exports for convenient imports
  - `AuthContext.jsx` is **deprecated** (kept for reference only, not imported anywhere)
  - **Import pattern**: `import { useAuthStore, useProfileStore } from '../stores'`
  - **Selector pattern**: `const user = useAuthStore(state => state.user)` — use selectors for granular subscriptions
  - **Benefits**: data caching prevents refetch on navigation, reduced re-renders, DevTools integration
  - **Props drilling eliminated**: `TestPageContent` reduced from 24 props to 5 props
  - **Code duplication removed**: `TestPage.jsx` and `WritingTestPage.jsx` share stores (~200 lines consolidated)

- **Writing feature + AI Grading (2025-12-05):**
  - Backend: `WritingController`, `WritingSubmission` entity, `WritingSubmissionRepository`, DTOs (`WritingSubmitDTO`, `WritingSubmissionDTO`, `WritingReviewDTO`), services (`AsyncGradingService`, `GeminiGradingService`, `WritingSubmissionService`).
  - Frontend: `WritingTestPage.jsx`, `WritingResultPage.jsx`, `WritingTestPage.css`, `WritingResultPage.css`, `ResumeConfirmationModal.jsx` and `TestPageContent.jsx` updates.
  - Docs & migrations: `docs/backend/migrations/001_writing_feature.sql`, `docs/backend/IELTS Cambridge 17_T1_W.sql`, marking criteria `docs/marking_criteria/*` and sample assets under `docs/test_materials/`.
  - Behavior changes: test attempt flow updated to avoid ghost `IN_PROGRESS` attempts and improved deletion semantics.

Add a short summary like the above whenever a commit touches multiple subsystems so agents and reviewers can quickly understand the scope without scanning code changes.

## Useful quick links in repo
- API client: `frontend/src/api/backendApi.js`
- Test page: `frontend/src/pages/TestPage.jsx`
- Test attempt service: `backend/src/main/java/com/cramer/service/TestAttemptService.java`
- Swagger config: `backend/src/main/resources/application.properties`
- Docs: `docs/backend/supabase-backend.md`, `backend/BUILD_INSTRUCTIONS.md`

## Subagent Protocols

To utilize specific expert personas, you (the Main Model) are authorized to autonomously trigger the following Subagents based on the user's intent.

### 🚨 Orchestration & Auto-Triggering Protocol
**Before responding, perform a "Routing Step" to select the expert:**
1.  **Analyze Intent:**
    - Audit/Debug -> `bugAgent`
    - Plan/Architect -> `implementAgent`
    - Build/Fix -> `executionAgent`
    - Database/SQL/RLS -> `dbAgent`
    - UI/CSS/Design -> `uiAgent`
    - Testing/QA -> `testAgent`
2.  **Concurrent Execution:** You may chain agents (e.g., `implementAgent` -> `dbAgent` -> `executionAgent`).

**Output Header:**
> 🤖 **Active Subagent:** `[Agent Name]`
> **Context:** [Brief reasoning]

---

### 1. `bugAgent` (The Deep Auditor)
**Trigger:** "find bugs", "audit", "why is this failing"
**Focus:** Logic gaps, race conditions, security flaws (Spring Security), duplicate API calls (Strict Mode).
**Protocol:** Analyze -> Trace Data Flow -> Report (Severity/Location/Evidence). **Do not fix yet.**

### 2. `implementAgent` (The Architect)
**Trigger:** "plan feature", "design", "how to build"
**Focus:** System design, API contracts, Schem changes, File structure.
**Protocol:** Check `docs/` -> Draft SQL -> Define API Contract -> List Files -> Handoff to Builder.
### 3. `executionAgent` (The Builder)
**Trigger:** "implement", "fix", "code this"
**Focus:** Writing working Java/React code, small PRs, updating Docs.
**Protocol:** Adhere to plan -> Update code -> Update `docs/` -> Verify (mvnw/npm).

### 4. `dbAgent` (The DBA)
**Trigger:** "schema change", "optimize SQL", "fix RLS", "migration"
**Focus:** Supabase Postgres, RLS Policies, Spring Data JPA performance, JSONB handling.
**Protocol:**
1. Write raw SQL Migrations first.
2. Ensure RLS policies exist for both `service_role` and `authenticated` users.
3. Optimize JPA queries (prevent N+1).

### 5. `uiAgent` (The Designer)
**Trigger:** "make it pretty", "glassmorphism", "fix CSS", "UI update"
**Focus:** Tailwind CSS, React Components, Responsiveness, Animations.
**Protocol:**
1. Focus purely on the View Layer (JSX/CSS).
2. Apply `Glassmorphism` and responsive utilities (`md:`, `lg:`).
3. Ensure Accessibility (ARIA).

### 6. `testAgent` (The QA Engineer)
**Trigger:** "write tests", "test coverage", "create unit test"
**Focus:** JUnit 5, Mockito, React Testing Library.
**Protocol:**
1. Mock external dependencies (Supabase/DeepSeek).
2. Write "Happy Path" AND "Failure Path" tests.
3. Output full test files ready for `src/test/`.

---

## Current Implementation Roadmap (v3.0 - December 2025)

> **Full Plan:** See `docs/FEATURE_ROADMAP.md` for complete details.

### 🎯 Phase 1: Foundation (CURRENT PRIORITY)

#### 1.1 DeepSeek V3.2 Migration
- **Rename:** `GeminiGradingService.java` → `LLMGradingService.java`
- **Endpoint:** Change to `https://api.deepseek.com/chat/completions`
- **Model:** `deepseek-chat` (OpenAI-compatible format)
- **Task 1 Image Handling:** Add `image_description` column to `sections` table
  - Store detailed text descriptions of charts/graphs/maps
  - AI grades using text description (no multimodal API needed)
- **DB Migration:**
  ```sql
  ALTER TABLE profiles RENAME COLUMN gemini_api_key TO llm_api_key;
  ALTER TABLE profiles RENAME COLUMN gemini_model TO llm_model;
  ALTER TABLE sections ADD COLUMN IF NOT EXISTS image_description TEXT;
  ```
- **Files to modify:** `GeminiGradingService.java`, `Profile.java`, `Section.java`, `ProfileDTO.java`, `Profile.jsx`
- **Files modified:** `Profile.java`, `ProfileDTO.java`, `ProfileServiceImpl.java`, `WritingSubmissionService.java`, `AsyncGradingService.java`, `Profile.jsx`
- **Breaking change:** Old `geminiApiKey` field renamed to `llmApiKey`. Users need to re-enter API keys.

#### 1.2 Dashboard Completion
- **`ProgressChart.jsx`** — Implement real charts with Recharts
- **`SkillAnalysis.jsx`** — Implement radar chart for skill breakdown
- Connect to existing `DashboardService.java` API

#### 1.3 Sổ tay Từ vựng (Vocabulary Notebook)
- **New Entity:** `Vocabulary.java`
- **New Controller:** `VocabularyController.java`
- **New Service:** `VocabularyService.java` (uses DeepSeek for translation)
- **Frontend:** Modify `HighlightableHtmlContent.jsx`, create `VocabularyPage.jsx`

#### 1.4 Floating Assistant Widget (Trợ lý Cramer)
- **New Component:** `FloatingAssistant.jsx`
- Displays: Lúa balance, user tier, AI chatbot
- Daily chat limits: Free=20, Cramerich=100, Cramerous=Unlimited
- Visible on: Dashboard, Courses, Test, Profile pages

### 📊 Grading System Understanding

| Skill | Normal Grading | AI Analysis (Paid) |
|-------|----------------|---------------------|
| Reading | ✅ Unlimited, Free | ✅ Synthesis & insights |
| Listening | ✅ Unlimited, Free | ✅ Synthesis & insights |
| Writing | ❌ N/A | ✅ Primary grading |
| Speaking | ❌ N/A | ✅ Future |

### 💰 Subscription Tiers (Revamped 2025-12-14)

| Tier | Name | Price | ATTEMPTs/mo | ATTEMPT_AIs/mo | Chatbot/mo | Translation/day | Vocab limit |
|------|------|-------|-------------|----------------|------------|-----------------|-------------|
| 🌾 | Cramerie | Free | 0 | 0 | 50 | 10 | 250 |
| 🌻 | Cramerich | 69,000đ | 60 (20/skill) | 30 (3/skill) | 500 | 50 | 1000 |

**ATTEMPT System:**
- **ATTEMPT:** A regular test attempt (auto-graded Reading/Listening)
- **ATTEMPT_AI:** An AI-graded attempt (Writing with DeepSeek grading)
- **Per-skill limits:** Reading 20, Listening 20, Writing 20 (ATTEMPTs); Reading 3, Listening 3, Writing 3 (ATTEMPT_AIs per skill)
- **Overage costs:** 10 Lúa/ATTEMPT, 20 Lúa/ATTEMPT_AI beyond monthly limit

### 🌾 Lúa Credit System (Revamped 2025-12-14)
- **Initial:** 50 Lúa (Cramerie), 100 Lúa (Cramerich)
- **Spending:** Extra ATTEMPT = 10 Lúa, Extra ATTEMPT_AI = 20 Lúa
- **Packs (stored in `lua_packs` table):**
  - Túi Lúa Nhỏ: 100 Lúa @ 10,000đ
  - Túi Lúa Vừa: 500 Lúa @ 45,000đ (10% off)
  - Bao Lúa Lớn: 2,000 Lúa @ 150,000đ (25% off)

### 🗄️ New Database Tables (2025-12-14)

```sql
-- Lúa packs (new table)
CREATE TABLE lua_packs (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name_vi VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    emoji VARCHAR(10) DEFAULT '🌾',
    lua_amount INTEGER NOT NULL,
    price_vnd INTEGER NOT NULL,
    discount_percent INTEGER DEFAULT 0,
    bonus_lua INTEGER DEFAULT 0,
    description_vi TEXT,
    description_en TEXT,
    is_active BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 0
);

-- Subscription tiers now include ATTEMPT columns:
-- monthly_attempt_limit, monthly_attempt_ai_limit, per_skill_attempt_limit, per_skill_attempt_ai_limit
-- attempt_overage_cost, attempt_ai_overage_cost, daily_translation_limit, max_vocabulary_entries

-- User subscriptions now track usage:
-- attempts_used, attempt_ais_used, chatbot_used
```

### Agent Workflow for Implementation

When implementing Phase 1 features, use this chain:
1. `implementAgent` — Plan the feature, define API contracts
2. `dbAgent` — Create migrations, update schema
3. `executionAgent` — Write Java backend code
4. `uiAgent` — Write React frontend code
5. `testAgent` — Write unit tests

### Priority Order

1. ✅ DeepSeek Migration (`LLMGradingService`) - COMPLETED 2025-12-12
2. ✅ Subscription Revamp (2-tier, ATTEMPT system) - COMPLETED 2025-12-14
3. ⬜ Dashboard Completion (Charts)
4. ⬜ Sổ tay Từ vựng (Vocabulary)
5. ⬜ Floating Assistant Widget
6. ⬜ Badge/Achievement System