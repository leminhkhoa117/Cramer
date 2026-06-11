# SPEC-15 — Billing (Subscription, Lúa, Quota, Payment, Gating)

> Status: **Authoritative** · Module: `billing` · Depends on: SPEC-04
> Owns all paid access and paid consumption. **Money correctness is the top priority** here:
> idempotency, atomicity, and charge-after-success are hard rules.

---

## 1. Data model (existing tables)

| Table | Purpose |
|-------|---------|
| `subscription_tiers` | code, price_vnd, attempt/AI/per-skill limits, overage costs, chat/translation/vocab limits, Lúa bonus fields, features (JSONB) |
| `user_subscriptions` | user_id, tier_id, status, started_at, expires_at, attempts_used, attempt_ais_used, chatbot_used, ai_grading_enabled, payment_reference, auto_renew |
| `user_credits` | user_id, balance, lifetime_earned, lifetime_spent |
| `credit_transactions` | user_id, signed amount, balance_after, type, category, description, reference_id |
| `lua_packs` | code, name, lua_amount, price_vnd, discount_percent, bonus_lua, active, display_order |
| `user_quotas` / `skill_quotas` | monthly global / per-skill attempt + AI counters (quota_month) |
| `payment_orders` | order_code, payment_link_id, checkout_url, type, tier_id/code, lua_amount, amount_vnd, status, transaction_datetime, paid_at, expires_at |
| `translation_usage` | user_id, usage_month, translations_used |

## 2. Subscriptions

- Tiers `cramerie` (free), `cramerich`, `cramerous`; values are **DB-driven**
  (`subscription_tiers`), ordered by `display_order`. `cramerich`/`cramerous` are **premium**.
- **Lifecycle**:
  - `GET /current` auto-creates a free Cramerie subscription if none (status `ACTIVE`,
    `expires_at=null`, counters 0, `auto_renew=false`). Free **initial Lúa** is granted only
    if the user has no `user_credits` row yet.
  - Paid: PayOS order → on verified webhook, create/update active subscription, **reset
    counters**, `started_at=now`, `expires_at=now+1 month`, `payment_reference=link`; grant
    tier `initial_lua` as `TIER_BONUS`.
  - Daily `SubscriptionExpiryScheduler` (00:05 Asia/Ho_Chi_Minh) flips expired `ACTIVE` →
    `EXPIRED`. Active = `status=ACTIVE` AND (`expires_at` null or future).
- **Fix:** define **monthly resets** for `attempts_used`/`attempt_ais_used`/`chatbot_used`
  (calendar-month, like quota rows) so free lifetime subscriptions don't accumulate forever;
  and a monthly **`monthly_lua_bonus`** grant job (was never granted).
- `ai_grading_enabled`: stored; enabling is allowed for premium tiers only (enforced), and the
  flag is **read by the grading path** (the old flag was unused/inconsistent).

## 3. Lúa credits

- Balance state: `balance`, `lifetime_earned`, `lifetime_spent`. Every mutation writes a
  signed `credit_transactions` row (`EARN`+, `SPEND`−, `REFUND`+).
- Categories: `INITIAL_BONUS`, `TIER_BONUS`, `PURCHASE`, `AI_GRADING`, `ATTEMPT_OVERAGE`,
  `VOCABULARY_TRANSLATION`, `CHAT_EXTENSION`, `SPEAKING_SESSION`, `SPEAKING_REFUND`,
  **`ADMIN_ADJUSTMENT`** (**fix:** add to the enum — the old SQL used it but the enum lacked
  it, breaking JPA reads). `ATTEMPT_OVERAGE` covers Reading/Listening over-cap attempt charges
  (the §3 list previously had no attempt category — added here; `category` is a free varchar).
- **Fix — idempotency & atomicity:**
  - `earn`/`spend`/`refund` are **idempotent by `(user_id, reference_id, category)`**: a repeat
    with the same reference is a no-op returning the prior result.
  - Mutations use **atomic repository updates** (or row lock), not read-modify-write on
    detached entities. `spend` rejects when `balance < amount` (checked atomically).

## 4. Lúa packs (DB-driven only)

- `GET /api/credits/packages` and `GET /api/payments/lua-packs` both read **active `lua_packs`
  rows**. **Fix:** remove the hardcoded pack tables in the payment path; `purchase` and webhook
  validation both use the DB pack's `lua_amount + bonus_lua` (`totalLua`) and `price_vnd`.

## 5. Quota

- Monthly (calendar `quota_month`). Free caps: global **60** regular / **30** AI per month;
  per-skill **20** regular / **3** AI (skills R/L/W/S). New month → new rows; row creation
  handles insert races (`REQUIRES_NEW` + retry).
- **Fix:** `QuotaService.getQuotaStatus` is **tier-aware** — premium users see premium/unlimited
  status (the old status endpoint always showed free caps even though billing bypassed them).
- **Fix:** quota **check + increment is atomic** (single locked/conditional update) so
  concurrent attempts cannot overrun caps.

## 6. Billing (consumption)

**Attempt billing** (`AttemptBillingService` / `AttemptBillingPort`):
- premium → allowed, no charge, counter++.
- non-premium within caps → allowed, no charge, counter++.
- non-premium over cap → **regular 10 Lúa, AI 20 Lúa**; insufficient Lúa → `allowed=false`
  (402); sufficient → spend then counter++. `preCheck` decides without spending.
- **Single canonical AI-grading overage = 20 Lúa** everywhere (writing copy/DTOs included).
  **Fix:** remove all "10 Lúa AI grading" references.

**Chat billing** (`ChatBillingPort`) & **Translation billing** (`TranslationBillingPort`):
- require active subscription + tier; monthly limit from tier (`< 0` = unlimited); within
  quota → counter++; over quota → charge tier overage (chat default **2** Lúa category
  `CHAT_EXTENSION`; translation default **1** Lúa category `VOCABULARY_TRANSLATION`).
  **Fix:** correct categories (old code mis-tagged both as `AI_GRADING`).
- **Fix — charge after success:** the charge/counter is committed **after** the LLM call
  succeeds; if the AI call fails, **no charge** (or refund). (Old code charged before the call
  with no refund path.)

**Speaking billing** (`SpeakingBillingPort`): check on create; deduct once on complete via
`lua_deducted`; refund idempotently on grading failure/watchdog (`refund_session_{id}`).
Default cost 15 Lúa.

**Writing AI grading** (`UsageBillingPort`): `chargeAiGrading(userId, ref)` after grading
succeeds; `refund(userId, ref)` available; idempotent by `ref`.

## 7. Feature gating

- `FeatureAccessService` (+ `FeatureAccessPort`) resolves the active tier and parses
  `subscription_tiers.features` (supports array **and** object JSON shapes — unify the two
  inconsistent parsers). Premium = `price_vnd > 0`. Default Cramerie features:
  `limited_tests, normal_grading, vocabulary, basic_progress`.
- **Fix:** the port is **wired into request paths** that need gating (catalog access, vocab AI,
  chatbot) — the old service existed but was never called.

## 8. Payments (PayOS)

- **Create**: subscription needs `tierId|tierCode` (reject free tier); Lúa needs a DB pack.
  Order saved `PENDING`, expires 24 h; if PayOS unconfigured → mock checkout URL (signature
  skipped); PayOS failure → order `FAILED`.
- **Webhook** `POST /api/payments/webhook` (public): signature required unless PayOS
  unconfigured; success = `code=="00"` && `success==true`. Grants subscription or Lúa
  (`PURCHASE`). **Fix — concurrency-safe idempotency:** claim the order with a row
  lock/version (status `PENDING → PAID`) so duplicate concurrent webhooks **cannot** grant
  twice. Already-`PAID` → skip. Non-success/unknown order → no state change.
- Webhook always returns 200 with `{code}` to stop PayOS retries; signature mismatch → 403.
- Owner-checked `GET /status/{orderCode}` (403 for others), `GET /history`.
- **Fix:** add a reconciliation path for `CANCELLED`/`EXPIRED` (or document the explicit gap)
  so those statuses aren't dead.

## 9. API surface (summary)

Subscriptions: `GET /api/subscriptions/{tiers,tiers/{code},current,my-status,grading-status,
gradings-remaining,chat-limit}`, `PUT /api/subscriptions/ai-grading`.
Credits: `GET /api/credits`, `/check/{amount}`, `/transactions`, `/stats`, `/packages`,
`/history`; `POST /api/credits/purchase`.
Quotas: `GET /api/quotas`, `/can-attempt?skill&ai`, `/check` (alias).
Payments: `POST /subscription`, `/lua`, `/webhook` (public); `GET /status/{orderCode}`,
`/history`, `/lua-packs` (public), `/config-status` (public).

## 10. Ports (published)
`AttemptBillingPort`, `UsageBillingPort`, `SpeakingBillingPort`, `ChatBillingPort`,
`TranslationBillingPort`, `FeatureAccessPort`. Each exposes records/primitives only.

## 11. Change log
| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
| 11/06/2026 | Added `ATTEMPT_OVERAGE` credit category for Reading/Listening over-cap charges (the §3 list had no attempt category). |
