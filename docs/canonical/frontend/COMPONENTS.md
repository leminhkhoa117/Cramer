# Cramer Frontend Components Documentation

> **Last Updated:** 17/05/2026  
> **Framework:** React 19 + Vite 8  
> **UI Libraries:** Framer Motion, React Bootstrap, React Icons

This document catalogs all reusable components in the Cramer IELTS learning platform.

---

## Table of Contents

1. [Component Categories](#component-categories)
2. [Layout Components](#layout-components)
3. [Navigation Components](#navigation-components)
4. [Test-Taking Components](#test-taking-components)
5. [Question Rendering](#question-rendering)
6. [Modal Components](#modal-components)
7. [Form Components](#form-components)
8. [Display Components](#display-components)
9. [Admin Components](#admin-components)
10. [Utility Components](#utility-components)

---

## Component Categories

| Category | Count | Location |
|----------|-------|----------|
| Layout | 4 | `components/` |
| Navigation | 3 | `components/` |
| Test-Taking | 8 | `components/` |
| Question Rendering | 4 | `components/` |
| Modals | 12 | `components/` |
| Forms | 4 | `components/` |
| Display/UI | 15 | `components/` |
| Common (shared) | 5 | `components/common/` |
| Home Sections | 7 | `components/home/` |
| Pricing | 1 | `components/pricing/` |
| Review | 6 | `components/review/` |
| 3D | 2 | `components/3d/` |
| **Admin Total** | 30+ | `admin/components/` |

---

## Layout Components

### Header

**File:** [src/components/Header.jsx](../../../frontend/src/components/Header.jsx)

**Purpose:** Main navigation header for public/protected pages.

```jsx
import Header from './components/Header';

// Used in App.jsx, conditionally hidden on test pages
{showHeader && <Header />}
```

**Features:**
- Fixed position with scroll effect
- Logo and navigation links
- User dropdown with avatar
- Responsive hamburger menu

**Stores Used:**
- `useAuthStore` - User state
- `useProfileStore` - Avatar, display name

**Visibility:** Hidden on `/test/*`, `/admin/*`, and review pages

---

### Footer

**File:** [src/components/Footer.jsx](../../../frontend/src/components/Footer.jsx)

**Purpose:** Site footer with links and copyright.

**Visibility:** Hidden on test-taking and admin pages.

---

### PageWrapper

**File:** [src/components/PageWrapper.jsx](../../../frontend/src/components/PageWrapper.jsx)

**Purpose:** Wraps pages with Framer Motion animations.

```jsx
import PageWrapper from './components/PageWrapper';

<PageWrapper>
  <Dashboard />
</PageWrapper>
```

**Props:**
| Prop | Type | Description |
|------|------|-------------|
| `children` | ReactNode | Page content |

---

### TestLayout

**File:** [src/components/TestLayout.jsx](../../../frontend/src/components/TestLayout.jsx)

**Purpose:** Full-screen wrapper for test-taking interface.

```jsx
<TestLayout>
  <TestPage />
</TestLayout>
```

**Features:**
- Full viewport height
- No scroll on body
- Dark overlay background

---

## Navigation Components

### TestHeader

**File:** [src/components/TestHeader.jsx](../../../frontend/src/components/TestHeader.jsx)

**Purpose:** Header bar during test-taking with timer and actions.

```jsx
<TestHeader
  testTitle="IELTS Reading Test 1"
  timeLeft={3450}
  onSubmit={handleSubmit}
  isSubmitting={false}
/>
```

**Props:**
| Prop | Type | Description |
|------|------|-------------|
| `testTitle` | string | Display title |
| `timeLeft` | number | Seconds remaining |
| `onSubmit` | function | Submit handler |
| `isSubmitting` | boolean | Disable button state |
| `onExit` | function | Exit test handler |

---

### TestFooter

**File:** [src/components/TestFooter.jsx](../../../frontend/src/components/TestFooter.jsx)

**Purpose:** Navigation footer for part/task switching.

```jsx
<TestFooter
  parts={[1, 2, 3]}
  activePart={0}
  onPartChange={setActivePart}
  answeredQuestions={answeredMap}
/>
```

**Props:**
| Prop | Type | Description |
|------|------|-------------|
| `parts` | array | Part numbers |
| `activePart` | number | Current index |
| `onPartChange` | function | Part switch handler |
| `answeredQuestions` | object | Map of answered question IDs |

---

### QuestionNavBar

**File:** [src/components/QuestionNavBar.jsx](../../../frontend/src/components/QuestionNavBar.jsx)

**Purpose:** Question number grid for quick navigation.

**Features:**
- Color-coded by status (answered, flagged, current)
- Click to scroll to question
- Compact grid layout

---

## Test-Taking Components

### TestPageContent

**File:** [src/components/TestPageContent.jsx](../../../frontend/src/components/TestPageContent.jsx) (~351 lines)

**Purpose:** Main content area for Reading/Listening tests.

**Layout:**
```
┌─────────────────────────────────────────────┐
│ TestHeader                                  │
├─────────────────────┬───────────────────────┤
│ Passage/Audio       │ Questions             │
│ (Scrollable)        │ (Scrollable)          │
├─────────────────────┴───────────────────────┤
│ TestFooter                                  │
└─────────────────────────────────────────────┘
```

**Stores Used:**
- `useTestStore` - Answers, part index, modal states
- `useTestSessionStore` - Save progress, submit

**Key Features:**
- Part-based question grouping
- Text highlighting with popup
- Audio player for Listening
- Autoplay toggle
- Confirmation modal on submit

**Components Used:**
- `TestHeader`, `TestFooter`
- `QuestionGroupRenderer`
- `HighlightableText`, `HighlightPopup`
- `AudioPlayer`, `ToggleSwitch`
- `ConfirmationModal`, `ExitTestModal`

---

### AudioPlayer

**File:** [src/components/AudioPlayer.jsx](../../../frontend/src/components/AudioPlayer.jsx)

**Purpose:** Custom audio player for Listening tests.

```jsx
<AudioPlayer
  ref={audioRef}
  src={audioUrl}
  autoplay={isAutoplay}
  onEnded={handleNextAudio}
/>
```

**Props:**
| Prop | Type | Description |
|------|------|-------------|
| `src` | string | Audio URL |
| `autoplay` | boolean | Auto-start playback |
| `onEnded` | function | Callback when audio ends |

**Features:**
- Play/pause controls
- Progress bar with seek
- Volume control
- Time display

---

### HighlightableText

**File:** [src/components/HighlightableText.jsx](../../../frontend/src/components/HighlightableText.jsx)

**Purpose:** Text container that supports user highlighting.

```jsx
<HighlightableText
  content={passageHtml}
  onHighlight={handleHighlight}
/>
```

**Hook:** Uses `useTextHighlighter` custom hook

---

### HighlightableHtmlContent

**File:** [src/components/HighlightableHtmlContent.jsx](../../../frontend/src/components/HighlightableHtmlContent.jsx)

**Purpose:** Renders HTML content with highlighting support.

---

### HighlightPopup

**File:** [src/components/HighlightPopup.jsx](../../../frontend/src/components/HighlightPopup.jsx)

**Purpose:** Tooltip popup for highlighted text with actions.

**Actions:**
- Add to vocabulary
- Remove highlight
- Translate (AI)

---

### ToggleSwitch

**File:** [src/components/ToggleSwitch.jsx](../../../frontend/src/components/ToggleSwitch.jsx)

**Purpose:** iOS-style toggle switch component.

```jsx
<ToggleSwitch
  checked={isAutoplay}
  onChange={setIsAutoplay}
  label="Tự động phát"
/>
```

---

### FullPageLoader

**File:** [src/components/FullPageLoader.jsx](../../../frontend/src/components/FullPageLoader.jsx)

**Purpose:** Full-screen loading overlay with spinner.

```jsx
<FullPageLoader
  message="Đang tải bài thi..."
  subMessage="Vui lòng chờ trong giây lát"
/>
```

**Props:**
| Prop | Type | Description |
|------|------|-------------|
| `message` | string | Primary loading text |
| `subMessage` | string | Secondary text |

---

### GradingQuotaInfo

**File:** [src/components/GradingQuotaInfo.jsx](../../../frontend/src/components/GradingQuotaInfo.jsx)

**Purpose:** Display AI grading quota before submitting Writing.

**Shows:**
- Remaining gradings this month
- Lúa cost if over limit
- Warning if insufficient

---

## Question Rendering

### QuestionGroupRenderer

**File:** [src/components/QuestionGroupRenderer.jsx](../../../frontend/src/components/QuestionGroupRenderer.jsx)

**Purpose:** Renders a group of questions with shared context.

```jsx
<QuestionGroupRenderer
  group={questionGroup}
  answers={answers}
  onAnswerChange={handleAnswer}
  partId={part.id}
/>
```

**Handles:**
- Group instructions/context
- Block-based layout (Listening)
- Question type routing

---

### QuestionRenderer

**File:** [src/components/QuestionRenderer.jsx](../../../frontend/src/components/QuestionRenderer.jsx) (~452 lines)

**Purpose:** Renders individual question by type.

**Supported Question Types:**
| Type | Render Style |
|------|--------------|
| `TRUE_FALSE_NOT_GIVEN` | 3 radio buttons |
| `YES_NO_NOT_GIVEN` | 3 radio buttons |
| `MULTIPLE_CHOICE_SINGLE` | Radio buttons |
| `MULTIPLE_CHOICE_MULTIPLE` | Checkboxes |
| `FILL_IN_BLANK` | Text input |
| `SENTENCE_COMPLETION` | Text input |
| `SUMMARY_COMPLETION` | Inline inputs in HTML |
| `MATCHING_HEADINGS` | Dropdown select |
| `MATCHING_INFORMATION` | Dropdown select |
| `MATCHING_FEATURES` | Dropdown select |
| `TABLE_COMPLETION` | Inputs in table HTML |
| `FLOW_CHART_COMPLETION` | Inputs in flowchart |
| `NOTE_COMPLETION` | Inline inputs |

**Props:**
| Prop | Type | Description |
|------|------|-------------|
| `question` | object | Question data |
| `onAnswerChange` | function | `(questionId, value)` |
| `userAnswer` | any | Current answer value |
| `typeOverride` | string | Force question type |
| `groupOptions` | array | Options for matching |
| `partId` | number | Parent part ID |

---

### FlowchartRenderer

**File:** [src/components/FlowchartRenderer.jsx](../../../frontend/src/components/FlowchartRenderer.jsx)

**Purpose:** Renders flowchart HTML with interactive inputs.

**CSS:** [FlowchartRenderer.css](../../../frontend/src/components/FlowchartRenderer.css)

---

### ReviewedQuestion

**File:** [src/components/ReviewedQuestion.jsx](../../../frontend/src/components/ReviewedQuestion.jsx)

**Purpose:** Display question with correct/wrong indicators.

**Used in:** TestReviewPage

---

## Modal Components

### ConfirmationModal

**File:** [src/components/ConfirmationModal.jsx](../../../frontend/src/components/ConfirmationModal.jsx)

**Purpose:** Generic confirmation dialog.

```jsx
<ConfirmationModal
  isOpen={showModal}
  onClose={() => setShowModal(false)}
  onConfirm={handleConfirm}
  title="Xác nhận nộp bài"
  message="Bạn có chắc muốn nộp bài?"
  confirmText="Nộp bài"
  cancelText="Hủy"
  variant="warning"
/>
```

**Props:**
| Prop | Type | Description |
|------|------|-------------|
| `isOpen` | boolean | Visibility |
| `onClose` | function | Close handler |
| `onConfirm` | function | Confirm handler |
| `title` | string | Modal title |
| `message` | string | Body text |
| `confirmText` | string | Confirm button label |
| `cancelText` | string | Cancel button label |
| `variant` | string | `"default"` \| `"warning"` \| `"danger"` |

---

### ResumeConfirmationModal

**File:** [src/components/ResumeConfirmationModal.jsx](../../../frontend/src/components/ResumeConfirmationModal.jsx)

**Purpose:** Modal for resuming in-progress or viewing completed attempts.

**States Handled:**
- `IN_PROGRESS` → "Tiếp tục" or "Làm lại"
- `COMPLETED` → "Xem kết quả" or "Làm lại"

---

### ExitTestModal

**File:** [src/components/ExitTestModal.jsx](../../../frontend/src/components/ExitTestModal.jsx)

**Purpose:** Confirm exit from test-taking (saves progress).

---

### StartTestModal

**File:** [src/components/StartTestModal.jsx](../../../frontend/src/components/StartTestModal.jsx)

**Purpose:** Pre-test instructions and start confirmation.

---

### GoalModal

**File:** [src/components/GoalModal.jsx](../../../frontend/src/components/GoalModal.jsx)

**Purpose:** Set learning goals (target band score, study hours).

**Used in:** Dashboard

---

### FilterModal

**File:** [src/components/FilterModal.jsx](../../../frontend/src/components/FilterModal.jsx)

**Purpose:** Filter options for test history/courses.

---

### UploadImageModal

**File:** [src/components/UploadImageModal.jsx](../../../frontend/src/components/UploadImageModal.jsx)

**Purpose:** Image upload with crop functionality.

**Used in:** Profile (avatar, cover image)

**Features:**
- Drag & drop upload
- Image cropping
- Supabase storage upload

---

### ChangePasswordModal

**File:** [src/components/ChangePasswordModal.jsx](../../../frontend/src/components/ChangePasswordModal.jsx)

**Purpose:** Password change form with validation.

---

### VocabularyModal

**File:** [src/components/VocabularyModal.jsx](../../../frontend/src/components/VocabularyModal.jsx)

**Purpose:** Add/Edit vocabulary word form.

---

### LuaPurchaseModal

**File:** [src/components/LuaPurchaseModal.jsx](../../../frontend/src/components/LuaPurchaseModal.jsx)

**Purpose:** Quick Lúa (credit) purchase interface.

---

### QuotaExceededModal

**File:** [src/components/QuotaExceededModal.jsx](../../../frontend/src/components/QuotaExceededModal.jsx)

**Purpose:** Alert when AI grading quota is exceeded.

**Actions:**
- Buy Lúa
- Upgrade subscription
- Use Lúa balance

---

## Form Components

### OTPVerification

**File:** [src/components/OTPVerification.jsx](../../../frontend/src/components/OTPVerification.jsx)

**Purpose:** 6-digit OTP input for email verification.

```jsx
<OTPVerification
  onVerify={handleOtpSubmit}
  onResend={handleResendOtp}
  email={email}
/>
```

**Features:**
- Auto-focus next input
- Paste support
- Resend countdown timer

---

### CroppableImageTab

**File:** [src/components/CroppableImageTab.jsx](../../../frontend/src/components/CroppableImageTab.jsx)

**Purpose:** Image cropping interface using react-image-crop.

---

## Display Components

### CourseCard

**File:** [src/components/CourseCard.jsx](../../../frontend/src/components/CourseCard.jsx)

**Purpose:** Card displaying a test set/course.

**Props:**
| Prop | Type | Description |
|------|------|-------------|
| `course` | object | Course data |
| `onClick` | function | Click handler |

---

### VocabularyCard

**File:** [src/components/VocabularyCard.jsx](../../../frontend/src/components/VocabularyCard.jsx)

**Purpose:** Vocabulary word card with flip animation.

**Features:**
- Word and definition
- Star/Mastered toggle
- Edit/Delete actions

---

### ProgressChart

**File:** [src/components/ProgressChart.jsx](../../../frontend/src/components/ProgressChart.jsx)

**Purpose:** Line chart showing score progress over time.

**CSS:** [progress-chart.css](../../../frontend/src/css/progress-chart.css)

---

### SkillAnalysis

**File:** [src/components/SkillAnalysis.jsx](../../../frontend/src/components/SkillAnalysis.jsx)

**Purpose:** Radar/bar chart for skill breakdown.

**CSS:** [skill-analysis.css](../../../frontend/src/css/skill-analysis.css)

---

### TierCard

**File:** [src/components/TierCard.jsx](../../../frontend/src/components/TierCard.jsx)

**Purpose:** Subscription tier display card.

**Used in:** PricingPage

---

### LuaPackCard

**File:** [src/components/LuaPackCard.jsx](../../../frontend/src/components/LuaPackCard.jsx)

**Purpose:** Lúa credit pack purchase card.

---

### Pagination

**File:** [src/components/Pagination.jsx](../../../frontend/src/components/Pagination.jsx)

**Purpose:** Page navigation with first/prev/next/last.

```jsx
<Pagination
  currentPage={page}
  totalPages={totalPages}
  onPageChange={setPage}
/>
```

---

### AttemptHistoryDropdown

**File:** [src/components/AttemptHistoryDropdown.jsx](../../../frontend/src/components/AttemptHistoryDropdown.jsx)

**Purpose:** Dropdown actions for test attempt (view, retake, delete).

---

### CreditHistoryList

**File:** [src/components/CreditHistoryList.jsx](../../../frontend/src/components/CreditHistoryList.jsx)

**Purpose:** Transaction history table for Lúa credits.

---

### QuotaDisplay

**File:** [src/components/QuotaDisplay.jsx](../../../frontend/src/components/QuotaDisplay.jsx)

**Purpose:** Usage quota progress bar.

---

### FeatureGate

**File:** [src/components/FeatureGate.jsx](../../../frontend/src/components/FeatureGate.jsx)

**Purpose:** Conditional render based on feature flag or subscription feature.

```jsx
<FeatureGate feature="ai_writing_grading" fallback={<UpgradePrompt />}>
  <PremiumFeature />
</FeatureGate>
```

**Props:** `feature` (string, required) — feature code to check, `fallback` (node, optional) — rendered when access denied.

---

### WordPopup

**File:** [src/components/WordPopup.jsx](../../../frontend/src/components/WordPopup.jsx)

**Purpose:** Popup for word definition and translation.

---

### ChatBubble

**File:** [src/components/ChatBubble.jsx](../../../frontend/src/components/ChatBubble.jsx)

**Purpose:** Chat message bubble for FloatingAssistant.

---

### EssayComparison

**File:** [src/components/EssayComparison.jsx](../../../frontend/src/components/EssayComparison.jsx)

**Purpose:** Side-by-side essay comparison view.

**Used in:** WritingResultPage

---

### FloatingAssistant

**File:** [src/components/FloatingAssistant.jsx](../../../frontend/src/components/FloatingAssistant.jsx) (~356 lines)

**Purpose:** Floating AI chatbot widget.

**Features:**
- Lúa balance display
- AI chat interface
- Quick support access
- Collapsible/minimizable

**Stores Used:**
- `useAuthStore` - User check
- `useUserStatsStore` - Credits, chat usage

**API:**
- `chatApi.sendMessage()` - AI chat

**Visibility:** Hidden on `/`, `/login`, `/about`, test pages, admin

---

## Common Components

**Location:** [src/components/common/](../../../frontend/src/components/common/)

### BaseModal

**File:** [src/components/common/BaseModal.jsx](../../../frontend/src/components/common/BaseModal.jsx)

**Purpose:** Base modal wrapper with backdrop and close handling.

---

### FAQ

**File:** [src/components/common/FAQ.jsx](../../../frontend/src/components/common/FAQ.jsx)

**Purpose:** Accordion-style FAQ section.

---

### GradingLoader

**File:** [src/components/common/GradingLoader.jsx](../../../frontend/src/components/common/GradingLoader.jsx)

**Purpose:** Loading animation while AI grading in progress.

---

### Testimonials

**File:** [src/components/common/Testimonials.jsx](../../../frontend/src/components/common/Testimonials.jsx)

**Purpose:** Testimonials carousel.

---

## Home Section Components

**Location:** [src/components/home/](../../../frontend/src/components/home/)

| Component | Purpose |
|-----------|---------|
| `HeroSection` | Landing banner with CTA |
| `FeaturesSection` | Feature grid showcase |
| `GuideSection` | How-to-use guide |
| `TestimonialsSection` | User testimonials |
| `DemoSection` | Interactive demo |
| `FAQSection` | FAQ accordion |
| `SignupSection` | Final signup CTA |

All lazy-loaded in Home.jsx for performance.

---

## Review Components

**Location:** [src/components/review/](../../../frontend/src/components/review/)

| Component | Purpose |
|-----------|---------|
| `ReviewLayout` | Review page layout wrapper |
| `ReviewHeader` | Header with scores and stats |
| `ReviewColumn` | Passage/content column |
| `ReviewAnswerColumn` | Answer comparison column |
| `ReviewQuestionGroup` | Question group with answers |
| `ReviewQuestionRenderer` | Individual reviewed question |

---

## 3D Components

**Location:** [src/components/3d/](../../../frontend/src/components/3d/)

| Component | Purpose |
|-----------|---------|
| `Scene3DAbout` | 3D scene for About page |
| `Scene3DProfile` | 3D scene for Profile page |

Uses Three.js / React Three Fiber.

---

## Admin Components

**Location:** [src/admin/components/](../../../frontend/src/admin/components/)

### Layout Components

| Component | File | Purpose |
|-----------|------|---------|
| `AdminLayout` | `layout/AdminLayout.jsx` | Main admin layout wrapper |
| `AdminHeader` | `layout/AdminHeader.jsx` | Admin top bar |
| `AdminSidebar` | `AdminSidebar.jsx` | Side navigation |
| `AdminRouteGuard` | `AdminRouteGuard.jsx` | Role-based access control |

### Common Admin Components

| Component | Purpose |
|-----------|---------|
| `MetricCard` | Dashboard metric display |
| `StatusBadge` | Status indicator badge |
| `DataTable` | Sortable/filterable table |
| `ActivityTimeline` | Activity log display |
| `ConfirmModal` | Admin confirmation dialog |
| `Toast` | Toast notification system |

### Content Editor Components

**Location:** [src/admin/components/content/](../../../frontend/src/admin/components/content/)

| Component | Purpose |
|-----------|---------|
| `AdminPreviewContent` | Test preview in editor |
| `PassageInputModal` | Reading passage editor |
| `AudioUploadModal` | Audio file upload |
| `SectionMetaModal` | Section metadata editor |
| `SectionLayoutModal` | Block-based layout editor |
| `QuestionEditModal` | Question content editor |

### ABTS (AI Generation) Components

**Location:** [src/admin/components/abts/](../../../frontend/src/admin/components/abts/)

| Component | Purpose |
|-----------|---------|
| `StudioConfigView` | AI generation configuration |
| `StepPreview` | Preview generated content |
| `StudioModal` | Quick generation modal |
| `SaveAIContentModal` | Save to database modal |
| `StreamingDisplay` | Real-time streaming output |
| `ModelSelector` | LLM model selection |
| `TagInput` | Tag input with chips |
| `QuestionPreviewRenderer` | Preview question types |
| `DiagramUploadPanel` | Upload diagrams for generation |
| `RefinementModal` | Refine generated content |
| `IssueSelector` | Select issues for refinement |
| `Tooltip` | Info tooltip |
| `Skeleton` | Loading skeleton |

---

## Component Hierarchy

```
App
├── AuthInitializer
│   └── AppContent
│       ├── Header (conditional)
│       ├── Routes
│       │   ├── PageWrapper
│       │   │   └── [Page Components]
│       │   ├── TestLayout
│       │   │   └── TestPage/WritingTestPage
│       │   │       ├── TestHeader
│       │   │       ├── TestPageContent/WritingContent
│       │   │       │   ├── QuestionGroupRenderer
│       │   │       │   │   └── QuestionRenderer
│       │   │       │   ├── AudioPlayer
│       │   │       │   └── HighlightableText
│       │   │       └── TestFooter
│       │   └── AdminLayout
│       │       ├── AdminHeader
│       │       ├── AdminSidebar
│       │       └── [Admin Pages]
│       ├── Footer (conditional)
│       └── FloatingAssistant (conditional)
```

---

## Component Dependencies Map

### Core Stores Usage

| Store | Components Using It |
|-------|---------------------|
| `useAuthStore` | Header, FloatingAssistant, AdminRouteGuard, Login, all protected pages |
| `useProfileStore` | Header, Dashboard, Profile, FloatingAssistant |
| `useTestStore` | TestPage, WritingTestPage, TestPageContent |
| `useTestSessionStore` | TestPage, WritingTestPage, TestPageContent |
| `useDashboardStore` | Dashboard |
| `useCourseStore` | Courses |
| `useVocabularyStore` | VocabularyPage, VocabularyModal |
| `useUserStatsStore` | FloatingAssistant, WritingResultPage, GradingQuotaInfo |
| `useSubscriptionStore` | SubscriptionPage, FeatureGate |

### Admin Stores

| Store | Components Using It |
|-------|---------------------|
| `useAdminDashboardStore` | AdminDashboard |
| `useAdminContentStore` | ContentListPage, SetListPage |
| `useTestEditorStore` | TestEditorPage, all editor modals |
| `useABTSStore` | AIGenerationPage, StudioConfigView, StepPreview |
| `useHashtagStore` | HashtagManagementPage |
| `useTestSetStore` | SetListPage, SetDetailPage |
| `useAdminUsersStore` | UserListPage, UserDetailPage |
| `useAdminFinanceStore` | FinanceDashboard, TransactionHistoryPage |

---

## CSS Organization

### Global Styles
- `src/styles.css` - Base styles, Tailwind imports

### Component CSS
- `src/css/header.css`
- `src/css/test-page.css`
- `src/css/test-header.css`
- `src/css/test-footer.css`
- `src/css/question-renderer.css`
- `src/css/question-group.css`
- `src/css/toggle-switch.css`
- `src/css/highlight-popup.css`
- `src/css/floating-assistant.css`
- `src/css/writing-test-page.css`
- `src/css/writing-result-page.css`
- `src/css/test-review-page.css`
- etc.

### Common CSS
- `src/css/common/sidebar-layout.css` - Shared sidebar layout
- `src/css/common/modal.css` - Modal base styles

### Admin CSS
- `src/admin/css/` - Admin-specific styles

---

## Component Count Summary

| Category | Count |
|----------|-------|
| Main Components | 48 |
| Common Components | 5 |
| Home Components | 7 |
| Review Components | 6 |
| 3D Components | 2 |
| Admin Layout | 4 |
| Admin Common | 6 |
| Admin Content | 6 |
| Admin ABTS | 14 |
| **Total** | **~104** |

---

### Additional Components (not in categories above)

| Component | File | Purpose |
|-----------|------|---------|
| `CourseListItem` | `components/CourseListItem.jsx` | Compact course card for dashboard list view |
| `SmallViewportWarning` | `components/SmallViewportWarning.jsx` | Full-screen warning for very small viewports |
| `InButtonSpinner` | `components/common/InButtonSpinner.jsx` | Inline spinner for button loading states |
| `FeatureVisualHost` | `components/home/features/FeatureVisualHost.jsx` | Host component for home page feature visuals |
| `useStackedReveal` | `components/home/features/useStackedReveal.js` | Scroll-triggered stacked reveal hook |
| `useReducedMotion` | `components/home/hooks/useReducedMotion.js` | Detects prefers-reduced-motion setting |

---

## Best Practices

### Creating New Components

1. **Location**: Place in appropriate subdirectory
2. **Naming**: PascalCase, descriptive name
3. **CSS**: Create matching `.css` file if needed
4. **Props**: Document with PropTypes or TypeScript
5. **Stores**: Use Zustand selectors for performance

### Component Template

```jsx
import React from 'react';
import { useAuthStore } from '../stores';
import './MyComponent.css';

/**
 * MyComponent - Brief description
 * 
 * @param {Object} props
 * @param {string} props.title - The title to display
 */
export default function MyComponent({ title }) {
  const user = useAuthStore(state => state.user);
  
  return (
    <div className="my-component">
      {title}
    </div>
  );
}
```

### Performance Tips

1. Use Zustand selectors: `const user = useAuthStore(state => state.user)`
2. Memoize expensive computations with `useMemo`
3. Lazy load heavy components with `React.lazy`
4. Avoid inline object/array props where possible
