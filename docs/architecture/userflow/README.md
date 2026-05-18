# Cramer CMS User Flow Diagrams

> **Generated**: 2026-01-10  
> **Format**: PlantUML  
> **Total Diagrams**: 14

---

## User Journey Maps (Business-Focused)

These diagrams show step-by-step user experiences from a business perspective.

| File | Description | User Type |
|------|-------------|-----------|
| `journey_01_new_student.puml` | Discovery to first test completion | New visitor |
| `journey_02_free_to_paid.puml` | Conversion journey with friction points | Free user |
| `journey_03_writing_practice.puml` | Complete Writing test cycle with AI grading | Student |
| `journey_04_regular_practice.puml` | Weekly study routine and progress | Regular user |
| `journey_05_admin_content.puml` | AI content creation to publishing | Admin |

**Key Elements in Journey Maps:**
- User goals and motivations
- Decision points and friction
- Emotional moments (aha moments, frustrations)
- Conversion triggers
- Time estimates per phase

---

## Technical Architecture Diagrams


| File | Description | Lines |
|------|-------------|-------|
| `cramer_userflow.puml` | Complete unified diagram (may be slow to render) | ~750 |
| `01_authentication_flow.puml` | Login, OAuth, OTP verification | ~80 |
| `02_test_taking_flow.puml` | Reading/Listening test flow | ~120 |
| `03_writing_grading_flow.puml` | Writing test + DeepSeek AI grading | ~130 |
| `04_subscription_payment_flow.puml` | Subscription + PayOS payments | ~120 |
| `05_quota_billing_flow.puml` | Quota checking + Lua billing | ~130 |
| `06_abts_generation_flow.puml` | AI content generation (ABTS) | ~170 |
| `07_admin_content_flow.puml` | Admin content management | ~150 |

## How to Preview

### Option 1: PlantUML Online Server (Recommended)

1. Visit [https://www.plantuml.com/plantuml/uml/](https://www.plantuml.com/plantuml/uml/)
2. Copy the contents of any `.puml` file
3. Paste and click "Submit"

### Option 2: VS Code Extension

1. Install extension: `jebbs.plantuml`
2. Open any `.puml` file
3. Press `Alt+D` to preview
4. **If seeing ServiceWorker errors**: Try the smaller files (01-07)

### Option 3: Command Line (Java required)

```bash
# Install PlantUML
# Download: https://plantuml.com/download

# Generate PNG
java -jar plantuml.jar 01_authentication_flow.puml

# Generate SVG (better for large diagrams)
java -jar plantuml.jar -tsvg 02_test_taking_flow.puml

# Generate all diagrams
java -jar plantuml.jar docs/architecture/userflow/*.puml
```

### Option 4: Docker

```bash
docker run -v $(pwd):/data plantuml/plantuml:latest \
  /data/docs/architecture/userflow/01_authentication_flow.puml
```

## Diagram Overview

### 01. Authentication Flow
- Email/Password sign up/sign in
- Google/Facebook OAuth
- OTP verification
- Session management with Supabase Auth

### 02. Test-Taking Flow (Reading/Listening)
- Course selection → Test page → Review
- 60-minute timer with auto-save
- Resume/retake logic
- Part navigation

### 03. Writing & AI Grading Flow
- Writing test with Task 1/Task 2
- Async AI grading via DeepSeek
- Polling for results
- Detailed feedback display

### 04. Subscription & Payment Flow
- Tier comparison (cramerie/cramerich/cramerous)
- PayOS integration
- Lua pack purchases
- Webhook handling

### 05. Quota & Billing Flow
- Pre-check before test start
- Premium bypass logic
- Lua overage charges (10/20 Lua)
- Quota increment on attempt

### 06. ABTS AI Generation Flow
- Two-pass generation strategy
- SSE streaming with reasoning tokens
- Validation layers
- Agent 2 refinement with patches

### 07. Admin Content Management Flow
- Test set → Test → Section → Question hierarchy
- Test editor with preview
- Publish/unpublish workflow
- Audio/image upload

## Theme

All diagrams use the `blueprint` theme with:
- Dark background (#1a1a2e)
- Blue arrows (#64b5f6)
- Green actors (#4caf50)
- Coral admin actors

## Verified Against Source Code

These diagrams were created by reading:
- `App.jsx` (routes)
- `useTestStore.js`, `useAuthStore.js`, `useABTSStore.js` (state)
- `TestAttemptController.java`, `WritingController.java`, `ABTSController.java`
- `TestAttemptService.java`, `ABTSService.java`, `LLMGradingService.java`
- `PaymentService.java`, `SubscriptionService.java`, `QuotaBillingService.java`
