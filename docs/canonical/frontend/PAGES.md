# Cramer Frontend Pages Documentation

> **Last Updated:** 17/05/2026  
> **Framework:** React 18 + Vite + React Router v6.4+ (createBrowserRouter)  
> **State Management:** Zustand

This document catalogs all page components in the Cramer IELTS learning platform.

---

## Table of Contents

1. [Route Overview](#route-overview)
2. [Public Pages](#public-pages)
3. [Protected Pages](#protected-pages)
4. [Admin Pages](#admin-pages)
5. [User Flows](#user-flows)

---

## Route Overview

| Route | Page Component | Auth Required | Description |
|-------|---------------|---------------|-------------|
| `/` | `Home` | ❌ | Landing page with feature showcase |
| `/login` | `Login` | ❌ | Authentication (login/register/forgot password) |
| `/about` | `About` | ❌ | About page with 3D scene |
| `/pricing` | `PricingPage` | ❌ | Subscription tiers and Lúa packs |
| `/dashboard` | `Dashboard` | ✅ | User's test history and analytics |
| `/courses` | `Courses` | ✅ | Browse available test sets |
| `/courses/:courseName` | `CourseDetailPage` | ✅ | Individual course/test set details |
| `/test/:source/:testNum/:skill` | `TestPage` | ✅ | Reading/Listening test interface |
| `/test/writing/:source/:testNum` | `WritingTestPage` | ✅ | Writing test interface |
| `/test/review/:attemptId` | `TestReviewPage` | ✅ | R/L test results review |
| `/test/writing/review/:attemptId` | `WritingResultPage` | ✅ | Writing AI grading results |
| `/profile` | `Profile` | ✅ | User settings and security |
| `/vocabulary` | `VocabularyPage` | ✅ | Personal vocabulary notebook |
| `/subscription` | `SubscriptionPage` | ✅ | Manage subscription & usage |
| `/payment/success` | `PaymentSuccessPage` | ✅ | Payment confirmation |
| `/payment/cancel` | `PaymentCancelPage` | ❌ | Payment cancellation |
| `/admin/*` | Admin Routes | ✅ + Admin Role | Admin panel |

---

## Public Pages

### 1. Home (`/`)

**File:** [src/pages/Home.jsx](../../../frontend/src/pages/Home.jsx)

**Purpose:** Landing page that showcases platform features and converts visitors to users.

**Key Features:**
- Lazy-loaded sections for performance
- Hero section with CTA buttons
- Features, testimonials, FAQ sections
- Demo section showing platform capabilities

**Sections (lazy-loaded):**
- `HeroSection` - Main banner with signup CTA
- `FeaturesSection` - Platform features grid
- `GuideSection` - How-to-use guide
- `TestimonialsSection` - User testimonials
- `DemoSection` - Interactive demo
- `FAQSection` - Frequently asked questions
- `SignupSection` - Final CTA

**Dependencies:**
- Components from `components/home/`

---

### 2. Login (`/login`)

**File:** [src/pages/Login.jsx](../../../frontend/src/pages/Login.jsx) (~540 lines)

**Purpose:** Unified authentication page handling login, registration, and password recovery.

**Key Features:**
- Email/Password authentication
- Google OAuth integration
- OTP verification flow
- Password recovery with email verification
- Form validation with Vietnamese error messages

**Forms:**
1. `ForgotPasswordForm` - Email → New Password → OTP verification
2. Main login/register form with toggle

**Stores Used:**
- `useAuthStore` - Login/logout, OAuth
- `useProfileStore` - Profile sync after login

**API Calls:**
- `authApi.checkEmail()` - Email existence check
- `authHelpers.requestPasswordReset()` - Send OTP
- `authHelpers.verifyRecoveryOtp()` - Verify OTP
- `authHelpers.updatePassword()` - Set new password

---

### 3. About (`/about`)

**File:** [src/pages/About.jsx](../../../frontend/src/pages/About.jsx)

**Purpose:** Information about Cramer platform and team.

**Key Features:**
- 3D scene background (`Scene3DAbout`)
- Team introduction
- Mission statement

---

### 4. Pricing (`/pricing`)

**File:** [src/pages/PricingPage.jsx](../../../frontend/src/pages/PricingPage.jsx) (~510 lines)

**Purpose:** Display subscription tiers and Lúa (credit) packages.

**Key Features:**
- Three-tier comparison (Cramerie, Cramerich, Cramerous)
- Lúa pack purchase options
- Interactive AI grading demo
- FAQ accordion
- PayOS payment integration

**Stores Used:**
- `useAuthStore` - Check if user logged in

**Components:**
- `TierCard` - Subscription tier card
- `LuaPackCard` - Credit package card
- `InteractiveGradingDemo` - AI grading showcase
- `FAQ` - Common questions

**Constants:**
- `TIERS`, `TIER_INFO`, `LIMITS` from `constants/subscription.js`

---

## Protected Pages

All routes below require authentication via `ProtectedRoute` wrapper.

### 5. Dashboard (`/dashboard`)

**File:** [src/pages/Dashboard.jsx](../../../frontend/src/pages/Dashboard.jsx) (~691 lines)

**Purpose:** User's personal learning hub with test history and analytics.

**Layout:** Sidebar navigation with three tabs:
1. **Lịch sử làm bài** - Test attempt history with pagination
2. **Biểu đồ tiến độ** - Progress charts over time
3. **Phân tích Kỹ năng** - Skill-by-skill breakdown

**Stores Used:**
- `useAuthStore` - User data
- `useProfileStore` - Profile (avatar, goals)
- `useDashboardStore` - Summary, attempts, pagination

**Components:**
- `GoalModal` - Set learning goals
- `FilterModal` - Filter test history
- `ProgressChart` - Score trends visualization
- `SkillAnalysis` - Reading/Listening/Writing breakdown
- `Pagination` - Page navigation
- `AttemptHistoryDropdown` - Per-attempt actions

**Key Features:**
- Welcome message with user avatar
- Quick stats: total attempts, average score, trend
- Attempt cards with status badges
- "Làm lại" button with `forceNew: true` navigation state
- Score history chart (by month)

---

### 6. Courses (`/courses`)

**File:** [src/pages/Courses.jsx](../../../frontend/src/pages/Courses.jsx) (~176 lines)

**Purpose:** Browse and select test sets (Cambridge IELTS books).

**Stores Used:**
- `useCourseStore` - Courses list, search, caching

**Features:**
- Search bar with debounce
- Filter by exam source
- Hover effects on course cards
- Glassmorphic card design

**API:**
- `fetchCoursesV2()` - Load courses with caching

---

### 7. CourseDetailPage (`/courses/:courseName`)

**File:** [src/pages/CourseDetailPage.jsx](../../../frontend/src/pages/CourseDetailPage.jsx)

**Purpose:** Display tests within a course/exam source.

**URL Params:**
- `:courseName` - Exam source code (e.g., "cam17")

**Features:**
- List of tests (Test 1, 2, 3, 4)
- Skills available per test (Reading, Listening, Writing)
- Start/resume test buttons

---

### 8. TestPage (`/test/:source/:testNum/:skill`)

**File:** [src/pages/TestPage.jsx](../../../frontend/src/pages/TestPage.jsx) (~365 lines)

**Purpose:** Interactive Reading/Listening test-taking interface.

**URL Params:**
- `:source` - Exam source (cam17)
- `:testNum` - Test number (1-4)
- `:skill` - "reading" or "listening"

**Stores Used:**
- `useTestStore` - UI state (answers, timer, modals, navigation)
- `useTestSessionStore` - API operations with caching

**Key Features:**
- 60-minute countdown timer (auto-save every 5 min)
- Part navigation (Part 1, 2, 3)
- Resume in-progress attempts
- `forceNew` flag for retaking completed tests
- Question highlighting (HighlightContext)
- Audio player for Listening (autoplay toggle)

**Components:**
- `TestPageContent` - Main test content
- `TestLayout` - Full-screen layout wrapper
- `ResumeConfirmationModal` - Resume or start new
- `ConfirmationModal` - Submit confirmation

**Layout:**
- Left panel: Passage/Audio
- Right panel: Questions
- Fixed header with timer
- Fixed footer with navigation

---

### 9. WritingTestPage (`/test/writing/:source/:testNum`)

**File:** [src/pages/WritingTestPage.jsx](../../../frontend/src/pages/WritingTestPage.jsx) (~514 lines)

**Purpose:** Writing test interface with Task 1 and Task 2.

**URL Params:**
- `:source` - Exam source
- `:testNum` - Test number

**Stores Used:**
- `useTestStore` - Essays, timer, task switching
- `useTestSessionStore` - Save progress, submit

**Key Features:**
- 60-minute timer for both tasks combined
- Task 1 (150 words min) + Task 2 (250 words min)
- Word count display with color coding
- Resizable panels (task prompt | essay editor)
- Auto-save progress every 5 minutes
- AI grading quota check before submit

**Components:**
- `TestHeader` - Timer and submit button
- `TestFooter` - Task navigation
- `GradingQuotaInfo` - Show remaining AI gradings
- `ExitTestModal` - Exit confirmation

---

### 10. TestReviewPage (`/test/review/:attemptId`)

**File:** [src/pages/TestReviewPage.jsx](../../../frontend/src/pages/TestReviewPage.jsx) (~451 lines)

**Purpose:** Review completed Reading/Listening test results.

**URL Params:**
- `:attemptId` - Test attempt UUID

**API Calls:**
- `testAttemptApi.getTestReview(attemptId)`
- `testAttemptApi.regradeAttempt(attemptId)` - Re-score answers

**Key Features:**
- Band score display with IELTS conversion
- Correct/Wrong/Unattempted breakdown
- Part-by-part navigation
- Scroll sync between answers and questions
- Passage view with question highlights
- Retake button with `forceNew: true`

**Components:**
- `ReviewAnswerColumn` - Answer comparison grid
- `ReviewQuestionGroup` - Question group display

---

### 11. WritingResultPage (`/test/writing/review/:attemptId`)

**File:** [src/pages/WritingResultPage.jsx](../../../frontend/src/pages/WritingResultPage.jsx) (~1231 lines)

**Purpose:** AI-graded Writing test results with detailed feedback.

**URL Params:**
- `:attemptId` - Writing attempt UUID

**Stores Used:**
- `useUserStatsStore` - Quota for regrading
- `useAuthStore` - User info

**API Calls:**
- `writingApi.getWritingReview(attemptId)`
- `writingApi.requestRegrade(attemptId, taskNumber, type)`

**Key Features:**
- Overall band score + 4 criteria scores
- Task 1 / Task 2 toggle
- Collapsible score analysis sections
- Error highlights in essay (grammar, vocabulary, etc.)
- Essay comparison view (original vs sample)
- Paragraph-by-paragraph analysis
- Word-level vocabulary analysis
- Sample Band 7+ / Band 9 essays
- Polling for grading status (every 3s)

**Error Types Highlighted:**
- Grammar (red)
- Spelling (yellow)
- Vocabulary (purple)
- Punctuation (blue)
- Coherence (green)
- Style (yellow)

---

### 12. Profile (`/profile`)

**File:** [src/pages/Profile.jsx](../../../frontend/src/pages/Profile.jsx) (~908 lines)

**Purpose:** User profile settings and account security.

**Tabs:**
1. **Thông tin cá nhân** - Personal info editing
2. **Cài đặt AI** - DeepSeek API key and model settings
3. **Bảo mật** - Sessions, password change, linked accounts

**Stores Used:**
- `useAuthStore` - User data
- `useProfileStore` - Profile CRUD

**API Calls:**
- `profileApi.updateProfile()` - Save changes

**Components:**
- `UploadImageModal` - Avatar/cover image upload
- `ChangePasswordModal` - Password change form
- `ConfirmationModal` - Delete confirmations

**Key Features:**
- Avatar with crop functionality
- Cover image upload
- Session management (logout other devices)
- Link Google/Facebook accounts
- LLM provider settings (DeepSeek API key, model selection)

---

### 13. VocabularyPage (`/vocabulary`)

**File:** [src/pages/VocabularyPage.jsx](../../../frontend/src/pages/VocabularyPage.jsx) (~363 lines)

**Purpose:** Personal vocabulary notebook with flashcard-style learning.

**Stores Used:**
- `useVocabularyStore` - CRUD, pagination, search

**Key Features:**
- Grid/List view toggle
- Search and filter (all/starred/mastered)
- Add/Edit/Delete words
- Toggle mastered status
- Pagination

**Components:**
- `VocabularyCard` - Word card with definition
- `VocabularyModal` - Add/Edit form

---

### 14. SubscriptionPage (`/subscription`)

**File:** [src/pages/SubscriptionPage.jsx](../../../frontend/src/pages/SubscriptionPage.jsx) (~1068 lines)

**Purpose:** Comprehensive subscription management hub.

**Tabs:**
1. **Hạn mức** - Current usage limits
2. **Các gói khác** - Upgrade options
3. **Lịch sử** - Payment and credit history

**Stores Used:**
- `subscriptionApi.getMyStatus()` - Full subscription status

**API Calls:**
- `paymentApi.createSubscriptionPayment()` - Upgrade
- `paymentApi.createLuaPayment()` - Buy Lúa

**Key Features:**
- Tier badge with tier-specific styling
- Usage progress bars (AI gradings, attempts, chat)
- Lúa balance display
- Session management
- Credit transaction history

**Components:**
- `CreditHistoryList` - Transaction history table
- `LuaPurchaseModal` - Buy Lúa modal
- `ConfirmationModal` - Various confirmations

---

### 15. PaymentSuccessPage (`/payment/success`)

**File:** [src/pages/PaymentSuccessPage.jsx](../../../frontend/src/pages/PaymentSuccessPage.jsx)

**Purpose:** Payment confirmation after successful PayOS payment.

**Query Params:**
- `orderCode` - PayOS order reference

**Features:**
- Order details display
- Redirect to subscription page

---

### 16. PaymentCancelPage (`/payment/cancel`)

**File:** [src/pages/PaymentCancelPage.jsx](../../../frontend/src/pages/PaymentCancelPage.jsx)

**Purpose:** Handle cancelled/failed payments.

**Features:**
- Retry payment button
- Return to pricing page

---

## Admin Pages

All admin routes are protected by `AdminRouteGuard` and use `AdminLayout` wrapper.

### Admin Layout Structure

```
/admin
├── AdminLayout (wrapper with sidebar)
│   ├── AdminSidebar - Navigation sidebar
│   ├── AdminHeader - Top bar with user menu
│   └── <Outlet /> - Child route content
```

### Admin Dashboard (`/admin` or `/admin/dashboard`)

**File:** [src/admin/pages/AdminDashboard.jsx](../../../frontend/src/admin/pages/AdminDashboard.jsx) (~377 lines)

**Purpose:** Admin overview with key metrics.

**Stores Used:**
- `useAdminDashboardStore` - Stats, activities, system status

**Metrics:**
- Total users
- Monthly revenue
- Published tests
- Growth percentage

**Features:**
- Metric cards with trend indicators
- Recent activity timeline
- Quick action buttons
- System status indicators

---

### User Management

#### UserListPage (`/admin/users`)
**File:** [src/admin/pages/users/UserListPage.jsx](../../../frontend/src/admin/pages/users/UserListPage.jsx)

**Purpose:** View and manage all users.

**Features:**
- User table with search/filter
- Role badges
- Subscription tier display
- Click to view details

#### UserDetailPage (`/admin/users/:userId`)
**File:** [src/admin/pages/users/UserDetailPage.jsx](../../../frontend/src/admin/pages/users/UserDetailPage.jsx)

**Purpose:** Individual user management.

**Features:**
- Profile editing
- Subscription management
- Activity history
- Credit adjustments

---

### Finance Management

#### FinanceDashboard (`/admin/finance`)
**File:** [src/admin/pages/finance/FinanceDashboard.jsx](../../../frontend/src/admin/pages/finance/FinanceDashboard.jsx)

**Purpose:** Financial overview and metrics.

#### TransactionHistoryPage (`/admin/finance/transactions`)
**File:** [src/admin/pages/finance/TransactionHistoryPage.jsx](../../../frontend/src/admin/pages/finance/TransactionHistoryPage.jsx)

**Purpose:** All payment transactions.

#### ReportsPage (`/admin/finance/reports`)
**File:** [src/admin/pages/finance/ReportsPage.jsx](../../../frontend/src/admin/pages/finance/ReportsPage.jsx)

**Purpose:** Financial reports and exports.

---

### Content Management

#### ContentListPage (`/admin/content`)
**File:** [src/admin/pages/content/ContentListPage.jsx](../../../frontend/src/admin/pages/content/ContentListPage.jsx)

**Purpose:** Redirects to SetListPage (legacy).

#### SetListPage (`/admin/content/sets`)
**File:** [src/admin/pages/content/SetListPage.jsx](../../../frontend/src/admin/pages/content/SetListPage.jsx)

**Purpose:** Manage test sets (Cambridge books).

**Stores Used:**
- `useTestSetStore` - Sets CRUD

#### SetDetailPage (`/admin/content/sets/:setId`)
**File:** [src/admin/pages/content/SetDetailPage.jsx](../../../frontend/src/admin/pages/content/SetDetailPage.jsx)

**Purpose:** Manage individual tests within a set.

#### TestEditorPage (`/admin/content/editor/:testId`)
**File:** [src/admin/pages/content/TestEditorPage.jsx](../../../frontend/src/admin/pages/content/TestEditorPage.jsx) (~667 lines)

**Purpose:** Full test content editor.

**Stores Used:**
- `useTestEditorStore` - Editor state

**Features:**
- Section/Part management
- Question editing
- Passage input
- Audio upload (Listening)
- Preview mode
- Status management (Draft/Review/Published)

**Components:**
- `SectionMetaModal` - Edit section metadata
- `SectionLayoutModal` - Block-based layout editor
- `QuestionEditModal` - Question content editor
- `PassageInputModal` - Reading passage input
- `AudioUploadModal` - Audio file management
- `AdminPreviewContent` - Test preview
- `StudioModal` - AI generation quick access

#### AIGenerationPage (`/admin/content/generate`)
**File:** [src/admin/pages/content/AIGenerationPage.jsx](../../../frontend/src/admin/pages/content/AIGenerationPage.jsx) (~249 lines)

**Purpose:** AI-powered test content generation (ABTS).

**Stores Used:**
- `useABTSStore` - Generation config and streaming

**Views:**
1. **Config View** - Set generation parameters
2. **Preview View** - Review generated content

**Components:**
- `StudioConfigView` - Generation configuration
- `StepPreview` - Preview generated questions
- `SaveAIContentModal` - Save to database

#### HashtagManagementPage (`/admin/content/hashtags`)
**File:** [src/admin/pages/content/HashtagManagementPage.jsx](../../../frontend/src/admin/pages/content/HashtagManagementPage.jsx)

**Purpose:** Manage content hashtags/tags.

**Stores Used:**
- `useHashtagStore` - Hashtags CRUD

---

## User Flows

### 1. Test-Taking Flow (Reading/Listening)

```
Dashboard → Course → CourseDetail → TestPage
    │                                   │
    │ "Làm lại" (forceNew=true)         │ Resume/New modal
    └───────────────────────────────────┘
                    │
                    ▼
            TestPageContent
                    │
                    ▼ Submit
            TestReviewPage
```

**Key State:** `useTestStore` + `useTestSessionStore`

### 2. Writing Test Flow

```
Dashboard → Course → CourseDetail → WritingTestPage
                                        │
                                        │ Submit
                                        ▼
                                WritingResultPage
                                        │
                                        │ Polling for grading
                                        ▼
                                AI Grading Results
```

**Key State:** `useTestStore` (essays) + `useUserStatsStore` (quota)

### 3. Authentication Flow

```
Home/Any Page → Login
        │
        ├── Email/Password
        ├── Google OAuth
        └── Forgot Password → OTP → Reset
                │
                ▼
        Dashboard (redirect)
```

**Key State:** `useAuthStore` → `useProfileStore` (auto-sync)

### 4. Subscription & Payment Flow

```
Pricing → Select Tier/Lúa Pack → PayOS Payment
                                        │
                    ┌───────────────────┼───────────────────┐
                    ▼                                       ▼
            PaymentSuccessPage                      PaymentCancelPage
                    │                                       │
                    ▼                                       ▼
            SubscriptionPage                          Pricing (retry)
```

**Key State:** `useSubscriptionStore` + `useUserStatsStore`

### 5. Admin Content Creation Flow

```
AdminDashboard → SetListPage → SetDetailPage → TestEditorPage
                                                      │
                                    ┌─────────────────┼─────────────────┐
                                    ▼                 ▼                 ▼
                            Manual Edit        AI Generation       Audio Upload
                                    │                 │
                                    └─────────────────┘
                                            │
                                            ▼
                                    Publish/Archive
```

**Key State:** `useTestEditorStore` + `useABTSStore`

---

## Page Component Summary

| Category | Count | Key Files |
|----------|-------|-----------|
| **Public Pages** | 4 | Home, Login, About, Pricing |
| **Protected Pages** | 12 | Dashboard, TestPage, WritingTestPage, Profile, etc. |
| **Admin Pages** | 12 | AdminDashboard, UserList, UserDetail, FinanceDashboard, TransactionHistory, Reports, ContentList, HashtagManagement, AIGeneration, SetList, SetDetail, TestEditor, TestEditorSelect |
| **Total** | **28** | - |

---

## CSS Files by Page

| Page | CSS File(s) |
|------|-------------|
| Home | `pages/home.css` |
| Login | `pages/login.css` |
| About | `pages/about.css` |
| Dashboard | `shared/layout.css`, `pages/dashboard.css`, `components/course-list.css`, `components/progress-chart.css`, `components/skill-analysis.css` |
| Courses | `shared/layout.css`, `pages/courses.css` |
| CourseDetail | `pages/course-detail.css` |
| TestPage | `test/test-base.css`, `test/test-header-footer.css`, `test/test-question.css`, `test/test-reading.css` or `test/test-listening.css` |
| WritingTestPage | `test/test-base.css`, `test/test-header-footer.css`, `test/test-writing.css` |
| WritingResultPage | `test/writing-result.css` |
| TestReviewPage | `test/test-review.css` |
| Profile | `shared/layout.css`, `pages/profile.css` |
| VocabularyPage | `pages/vocabulary.css` |
| SubscriptionPage | `shared/layout.css`, `pages/subscription.css` |
| PricingPage | `pages/pricing.css` |
| Payment pages | `pages/payment.css` |
| Admin Pages | `admin/css/admin.css` → `admin/css/tokens.css` + page-specific under `admin/css/pages/` |
