# Cramerich Subscription System - Agentic Workflow (V2)

> **Supervisor Document for Multi-Agent Implementation**  
> **Last Updated:** 2025-12-14

---

## Executive Summary

This document coordinates **3 sub-agents** to implement the Cramerich subscription system with a "Lua" virtual currency economy and dual-quota billing logic. Each agent modifies an **exclusive, non-overlapping set of files** to prevent merge conflicts.

---

## Current State Analysis

### Existing Infrastructure (Already Implemented)
| Component | Status | Files/Tables |
|-----------|--------|--------------|
| `subscription_tiers` table | ✅ Complete | Migration 006, 008 |
| `user_credits` table (Lua balance) | ✅ Complete | Migration 006 |
| `credit_transactions` table | ✅ Complete | Migration 006 |
| `user_subscriptions` table | ✅ Complete | Migration 006 |
| `SubscriptionTier.java` entity | ✅ Complete | Backend entity |
| `UserCredit.java` entity | ✅ Complete | Backend entity |
| `PricingPage.jsx` | ✅ Exists | Frontend page |
| `SubscriptionPage.jsx` | ✅ Exists | Frontend page |

### What Needs Implementation
| Component | Status | Agent |
|-----------|--------|-------|
| Dual-Quota tracking tables | ❌ Missing | Agent 3 |
| Quota enforcement service | ❌ Missing | Agent 3 |
| Lua purchase flow (additional packages) | ❌ Missing | Agent 2 |
| Feature gating (Cramerie restrictions) | ❌ Missing | Agent 1 |

---

## Agent Assignments

### 🎯 Agent 1: Subscription Tiers & Feature Gating

**Objective:** Implement feature restrictions for Cramerie (free) tier and unlock logic for Cramerich (premium).

#### Files to Modify (EXCLUSIVE)
```
backend/
├── src/main/java/com/cramer/service/SubscriptionService.java    [NEW or MODIFY]
├── src/main/java/com/cramer/service/FeatureGatingService.java   [NEW]
├── src/main/java/com/cramer/controller/SubscriptionController.java [MODIFY]
├── src/main/java/com/cramer/dto/FeatureAccessDTO.java           [NEW]

frontend/
├── src/stores/useSubscriptionStore.js                           [NEW]
├── src/components/FeatureGate.jsx                               [NEW]
├── src/hooks/useFeatureAccess.js                                [NEW]
```

#### Task Specification

1. **Backend: Feature Gating Service**
   - Create `FeatureGatingService.java` with methods:
     ```java
     boolean canAccessTopic(UUID userId, String topicCode);
     boolean canAccessSkill(UUID userId, String skill);
     boolean canUseAIGrading(UUID userId, String gradingType);
     FeatureAccessDTO getAccessMap(UUID userId);
     ```
   - Cramerie restrictions:
     - Limited topics (first 2 per category)
     - No AI grading features
     - No Vocab AI, no Chatbot

2. **Frontend: Feature Gate Component**
   - Create `<FeatureGate feature="ai_grading">` wrapper component
   - Show upgrade prompt for locked features
   - Integrate with `useSubscriptionStore`

3. **Testing:** 
   - Unit test `FeatureGatingService` with mock subscriptions
   - Manual test by switching tier and verifying UI locks

#### Prompt for Agent 1
```
# Role: Backend & Frontend Developer
# Task: Implement Feature Gating for Cramer Subscription Tiers

## Context
Cramer has two subscription tiers:
- **Cramerie (Free):** Restricted access to topics/tests/skills. No AI features.
- **Cramerich (Premium - 69k VND/mo):** Full access to all content and AI features.

The subscription infrastructure already exists (see migration 006). Your job is to implement the gating logic.

## Your Exclusive Files
You are ONLY allowed to modify these files (create if they don't exist):
- `backend/src/main/java/com/cramer/service/FeatureGatingService.java` [NEW]
- `backend/src/main/java/com/cramer/dto/FeatureAccessDTO.java` [NEW]
- `frontend/src/stores/useSubscriptionStore.js` [NEW]
- `frontend/src/components/FeatureGate.jsx` [NEW]
- `frontend/src/hooks/useFeatureAccess.js` [NEW]

⚠️ DO NOT modify any other files. Other agents are working on different parts.

## Requirements

### Backend
1. Create `FeatureGatingService.java`:
   - Inject `UserSubscriptionRepository` (assume it exists)
   - Method: `boolean canAccessFeature(UUID userId, String featureCode)`
   - Method: `FeatureAccessDTO getFullAccessMap(UUID userId)`
   - Features to check: `all_tests`, `all_topics`, `ai_writing_grading`, `ai_reading_grading`, `vocab_ai`, `chatbot`
   - Logic: Parse the `features` JSONB from user's active subscription tier

2. Create `FeatureAccessDTO.java`:
   - Fields: `String tierCode`, `Map<String, Boolean> features`, `boolean isPremium`

### Frontend
3. Create `useSubscriptionStore.js` (Zustand):
   - State: `{ tier, features, loading, error }`
   - Action: `fetchSubscriptionStatus()`
   - Selector: `hasFeature(featureCode)`

4. Create `FeatureGate.jsx`:
   - Props: `feature` (string), `children`, `fallback` (optional JSX)
   - If user lacks feature, show fallback or upgrade prompt
   - Use `useSubscriptionStore`

5. Create `useFeatureAccess.js` hook:
   - Custom hook wrapping store logic for convenience

## Deliverables
After completion, report:
1. List of all files created/modified
2. Brief description of each file's purpose
3. Any assumptions made
4. Suggested integration points (where FeatureGate should be used)
```

---

### 🎯 Agent 2: Lua Economy (Virtual Currency)

**Objective:** Implement Lua purchase packages and transaction history UI.

#### Files to Modify (EXCLUSIVE)
```
backend/
├── src/main/java/com/cramer/service/LuaCreditService.java       [NEW or MODIFY]
├── src/main/java/com/cramer/controller/CreditController.java    [NEW or MODIFY]
├── src/main/java/com/cramer/dto/LuaPurchaseDTO.java             [NEW]
├── src/main/java/com/cramer/dto/CreditHistoryDTO.java           [NEW]

frontend/
├── src/pages/LuaStorePage.jsx                                   [NEW]
├── src/components/LuaPurchaseModal.jsx                          [NEW]
├── src/components/CreditHistoryList.jsx                         [NEW]
├── src/css/LuaStorePage.css                                     [NEW]
```

#### Task Specification

1. **Backend: Credit Service Enhancement**
   - Lua purchase packages:
     - 100 Lua = 10,000 VND
     - 500 Lua = 45,000 VND (10% bonus)
     - 2000 Lua = 150,000 VND (25% bonus)
   - Create pre-defined `LuaPackage` constants
   - Endpoint: `POST /api/credits/purchase` → initiates payment
   - Endpoint: `GET /api/credits/history` → paginated transactions

2. **Frontend: Lua Store Page**
   - Display current balance prominently
   - 3 package cards with pricing
   - Transaction history with filters (earn/spend)
   - Integration with PayOS (existing payment system)

3. **Security:**
   - Validate package codes server-side
   - Prevent double-spending with idempotency keys

#### Prompt for Agent 2
```
# Role: Backend & Frontend Developer  
# Task: Implement Lua Economy Purchase System

## Context
Cramer uses a virtual currency called "Lua" (🌙) for overage billing and premium features.
The base infrastructure exists (`user_credits`, `credit_transactions` tables).

Your job is to implement:
1. Lua package purchase flow
2. Transaction history UI
3. Balance display components

## Lua Pricing Structure
| Package | Lua Amount | Price (VND) | Bonus |
|---------|------------|-------------|-------|
| Small   | 100        | 10,000      | -     |
| Medium  | 500        | 45,000      | 10%   |
| Large   | 2,000      | 150,000     | 25%   |

## Your Exclusive Files
You are ONLY allowed to modify these files:
- `backend/src/main/java/com/cramer/service/LuaCreditService.java` [NEW/MODIFY]
- `backend/src/main/java/com/cramer/controller/CreditController.java` [NEW/MODIFY]
- `backend/src/main/java/com/cramer/dto/LuaPurchaseDTO.java` [NEW]
- `backend/src/main/java/com/cramer/dto/LuaPurchaseResponseDTO.java` [NEW]
- `backend/src/main/java/com/cramer/dto/CreditHistoryDTO.java` [NEW]
- `frontend/src/pages/LuaStorePage.jsx` [NEW]
- `frontend/src/components/LuaPurchaseModal.jsx` [NEW]
- `frontend/src/components/CreditHistoryList.jsx` [NEW]
- `frontend/src/css/LuaStorePage.css` [NEW]

⚠️ DO NOT modify `UserCredit.java`, `CreditTransaction.java`, or any migration files.

## Requirements

### Backend
1. Create `LuaCreditService.java`:
   - Define package constants (code, luaAmount, priceVnd)
   - Method: `List<LuaPackage> getAvailablePackages()`
   - Method: `PaymentOrder initiatePurchase(UUID userId, String packageCode)`
   - Method: `void completePurchase(String orderId)` (called by payment webhook)
   - Method: `Page<CreditHistoryDTO> getTransactionHistory(UUID userId, Pageable pageable)`

2. Create `CreditController.java`:
   - `GET /api/credits/balance` → current balance
   - `GET /api/credits/packages` → available packages
   - `POST /api/credits/purchase` → initiate purchase
   - `GET /api/credits/history` → paginated history

### Frontend
3. Create `LuaStorePage.jsx`:
   - Hero section with current balance (large display)
   - 3 package cards (styled premium, highlight best value)
   - "Lịch sử giao dịch" section with `CreditHistoryList`
   - Route: `/store/lua`

4. Create `LuaPurchaseModal.jsx`:
   - Confirm purchase dialog
   - Show package details, price, bonus
   - Payment button (integrate with existing PayOS flow)

5. Create `CreditHistoryList.jsx`:
   - Paginated list of transactions
   - Filter: All / Earned / Spent
   - Each row: date, description, amount (+/-), balance after

## Styling Requirements
- Use existing design system tokens from `frontend/src/index.css`
- Premium feel with gradients and subtle animations
- Lua icon: 🌙 or moon icon from react-icons

## Deliverables
Report:
1. All files created/modified
2. API endpoints added
3. Any PayOS integration notes
4. Suggested improvements
```

---

### 🎯 Agent 3: Dual-Quota Billing Logic (CRITICAL)

**Objective:** Implement the Global + Local quota system with Lua billing.

#### Files to Modify (EXCLUSIVE)
```
docs/backend/migrations/
├── 009_dual_quota_system.sql                                    [NEW]

backend/
├── src/main/java/com/cramer/entity/UserQuota.java               [NEW]
├── src/main/java/com/cramer/entity/SkillQuota.java              [NEW]
├── src/main/java/com/cramer/repository/UserQuotaRepository.java [NEW]
├── src/main/java/com/cramer/repository/SkillQuotaRepository.java [NEW]
├── src/main/java/com/cramer/service/QuotaService.java           [NEW]
├── src/main/java/com/cramer/service/QuotaBillingService.java    [NEW]
├── src/main/java/com/cramer/dto/QuotaStatusDTO.java             [NEW]
├── src/main/java/com/cramer/controller/QuotaController.java     [NEW]

frontend/
├── src/components/QuotaDisplay.jsx                              [NEW]
├── src/components/QuotaExceededModal.jsx                        [NEW]
├── src/stores/useQuotaStore.js                                  [NEW]
```

#### Quota Logic Specification

**Global Caps (Monthly per user):**
- 60 `ATTEMPT` (standard practice attempts)
- 30 `ATTEMPT_AI` (AI-graded attempts)

**Local Caps (Per Skill per month):**
- 20 `ATTEMPT` per skill
- 3 `ATTEMPT_AI` per skill

**Billing Decision Tree:**
```
BEFORE allowing attempt:
  1. Check if user is Cramerich → Skip to step 5 (no caps for premium)
  2. Check Global_ATTEMPT >= 60 OR Local_Skill_ATTEMPT >= 20
     → YES: Charge 10 Lua, allow attempt
  3. Check Global_AI >= 30 OR Local_Skill_AI >= 3
     → YES: Charge 20 Lua, allow AI attempt
  4. If no cap hit → Increment free counters, allow
  5. If insufficient Lua → Block, show upgrade modal
```

#### Prompt for Agent 3
```markdown
# Role: Backend Developer (Database + Services)
# Task: Implement Dual-Quota Billing System

## Context
Cramer needs usage-based billing for Cramerie (free) users via a dual-quota system:
- **Global Quota:** Monthly limits across all activities
- **Local Quota:** Per-skill monthly limits (prevents grinding one skill)

Premium (Cramerich) users have UNLIMITED access.

## Quota Caps (Cramerie Only)
| Quota Type | Global Cap | Local Cap (per skill) |
|------------|------------|----------------------|
| ATTEMPT    | 60/month   | 20/month             |
| ATTEMPT_AI | 30/month   | 3/month              |

## Billing Rates
- Exceeding ATTEMPT cap: **10 Lua** per attempt
- Exceeding ATTEMPT_AI cap: **20 Lua** per attempt

## Your Exclusive Files
- `docs/backend/migrations/009_dual_quota_system.sql` [NEW]
- `backend/src/main/java/com/cramer/entity/UserQuota.java` [NEW]
- `backend/src/main/java/com/cramer/entity/SkillQuota.java` [NEW]
- `backend/src/main/java/com/cramer/repository/UserQuotaRepository.java` [NEW]
- `backend/src/main/java/com/cramer/repository/SkillQuotaRepository.java` [NEW]
- `backend/src/main/java/com/cramer/service/QuotaService.java` [NEW]
- `backend/src/main/java/com/cramer/service/QuotaBillingService.java` [NEW]
- `backend/src/main/java/com/cramer/dto/QuotaStatusDTO.java` [NEW]
- `backend/src/main/java/com/cramer/controller/QuotaController.java` [NEW]
- `frontend/src/components/QuotaDisplay.jsx` [NEW]
- `frontend/src/components/QuotaExceededModal.jsx` [NEW]
- `frontend/src/stores/useQuotaStore.js` [NEW]

⚠️ DO NOT modify existing entities or services. Inject them as dependencies.

## Requirements

### Database Migration (009)

CREATE TABLE user_quotas (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    quota_month DATE NOT NULL,  -- First day of month (e.g., 2025-12-01)
    attempt_count INTEGER NOT NULL DEFAULT 0,
    attempt_ai_count INTEGER NOT NULL DEFAULT 0,
    UNIQUE(user_id, quota_month)
);

CREATE TABLE skill_quotas (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    skill VARCHAR(20) NOT NULL,  -- READING, LISTENING, WRITING, SPEAKING
    quota_month DATE NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    attempt_ai_count INTEGER NOT NULL DEFAULT 0,
    UNIQUE(user_id, skill, quota_month)
);


### Backend Services
1. `QuotaService.java`:
   - `QuotaStatusDTO getQuotaStatus(UUID userId)`
   - `void incrementAttempt(UUID userId, String skill, boolean isAI)`
   - `boolean canAttempt(UUID userId, String skill, boolean isAI)`
   - Auto-create quota rows for current month if missing

2. `QuotaBillingService.java`:
   - `BillingResult processAttemptBilling(UUID userId, String skill, boolean isAI)`
   - Returns: `{ allowed: boolean, luaCharged: int, reason: string }`
   - Inject `UserCreditService` for Lua deduction
   - Inject `SubscriptionService` to check Cramerich status

3. `QuotaController.java`:
   - `GET /api/quotas` → current quota status
   - `GET /api/quotas/can-attempt?skill=WRITING&ai=true` → pre-check

### Frontend
4. `QuotaDisplay.jsx`:
   - Progress bars for global/local quotas
   - Show "X of 60 attempts used"
   - Color coding (green/yellow/red)

5. `QuotaExceededModal.jsx`:
   - Explain which cap was hit
   - Show Lua cost to continue
   - Buttons: "Mua Lua" / "Nâng cấp Cramerich"

6. `useQuotaStore.js`:
   - Cache quota status
   - Pre-check before starting attempts

## Billing Pseudocode

public BillingResult processAttemptBilling(UUID userId, String skill, boolean isAI) {
    // Step 1: Cramerich = free pass
    if (subscriptionService.isCramerich(userId)) {
        incrementAttempt(userId, skill, isAI);
        return BillingResult.allowed();
    }
    
    // Step 2: Check global and local caps
    QuotaStatus status = getQuotaStatus(userId);
    boolean globalCapHit = isAI 
        ? status.globalAI >= 30 
        : status.globalAttempt >= 60;
    boolean localCapHit = isAI
        ? status.getSkill(skill).ai >= 3
        : status.getSkill(skill).attempt >= 20;
    
    // Step 3: Determine billing
    if (globalCapHit || localCapHit) {
        int cost = isAI ? 20 : 10;
        if (!creditService.hasEnough(userId, cost)) {
            return BillingResult.blocked("Không đủ Lua");
        }
        creditService.deduct(userId, cost, "Quota overage: " + skill);
        incrementAttempt(userId, skill, isAI);
        return BillingResult.charged(cost);
    }
    
    // Step 4: Within free quota
    incrementAttempt(userId, skill, isAI);
    return BillingResult.allowed();
}


## Deliverables
Report:
1. All files created
2. SQL migration content
3. API endpoints
4. Edge cases handled
5. Integration points (where to call QuotaBillingService)
```

---

## Integration Checklist

After all agents complete, the **Supervisor** (you) must verify:

### Agent 1 Verification
- [ ] `FeatureGatingService` correctly reads subscription tier features
- [ ] Cramerie users see locked icons on premium features
- [ ] Cramerich users have full access

### Agent 2 Verification
- [ ] Lua packages display correctly on `/store/lua`
- [ ] Purchase flow initiates PayOS payment
- [ ] Transaction history shows paginated results

### Agent 3 Verification
- [ ] Quota tables created with correct constraints
- [ ] Monthly reset logic works (new month = new quota row)
- [ ] Billing correctly deducts Lua on cap exceedance
- [ ] Modal shows when user lacks Lua to proceed

### Cross-Agent Integration Points
```
TestAttemptService.startOrGetAttempt()
    └── MUST call QuotaBillingService.processAttemptBilling() before creating attempt

WritingSubmissionService.submitWriting()
    └── MUST call QuotaBillingService.processAttemptBilling(userId, "WRITING", true)

VocabAIService.generateMeaning()
    └── MUST check FeatureGatingService.canAccessFeature(userId, "vocab_ai")
```

---

## Agent Completion Tracking

| Agent | Status | Files Created/Modified | Confirmation |
|-------|--------|------------------------|--------------|
| Agent 1 (Feature Gating) | ✅ Complete | 5 files (FeatureGatingService, FeatureAccessDTO, useSubscriptionStore, FeatureGate, useFeatureAccess) | Verified 2025-12-14 |
| Agent 2 (Lua Economy) | ✅ Complete | 10 files (LuaCreditService*, DTOs, LuaStorePage*, CreditHistoryList*) | Verified 2025-12-14 |
| Agent 3 (Dual-Quota) | ✅ Complete | 14 files (009 migration, UserQuota, SkillQuota, QuotaService*, QuotaBillingService*, QuotaController, frontend components) | Verified 2025-12-14 |

> **Build Status:** ✅ Backend compiles successfully (verified 2025-12-14 11:20 ICT)

---

## Appendix: Existing File References

### Backend Entities (DO NOT MODIFY)
- `backend/src/main/java/com/cramer/entity/SubscriptionTier.java`
- `backend/src/main/java/com/cramer/entity/UserSubscription.java`
- `backend/src/main/java/com/cramer/entity/UserCredit.java`
- `backend/src/main/java/com/cramer/entity/CreditTransaction.java`

### Frontend Pages (Reference Only)
- `frontend/src/pages/PricingPage.jsx`
- `frontend/src/pages/SubscriptionPage.jsx`
- `frontend/src/pages/PaymentSuccessPage.jsx`

### Existing Migrations (DO NOT MODIFY)
- `docs/backend/migrations/006_subscription_credit_achievement.sql`
- `docs/backend/migrations/007_payos_payment_integration.sql`
- `docs/backend/migrations/008_update_cramerich_tier.sql`
