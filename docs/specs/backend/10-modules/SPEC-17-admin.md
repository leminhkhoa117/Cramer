# SPEC-17 — Admin Console (Users, Audit, Dashboard, Finance)

> Status: **Authoritative** · Module: `admin` · Depends on: SPEC-15, SPEC-16
> Cross-domain admin-only console. Orchestrates existing capabilities via **ports**; uses
> read-only projections for reporting. All endpoints under `/api/admin/**` (admin-gated,
> SPEC-04 §1). **Admin identity comes from the JWT principal — `X-User-Id` is never trusted.**

---

## 1. Data model
- `admin_audit_log` — admin_user_id, admin_email, action, target_type, target_id,
  old_value (JSONB), new_value (JSONB), description, ip_address, user_agent, created_at.
- Reads (projections) across `profiles`, `auth.users`, `payment_orders`, `user_subscriptions`,
  `subscription_tiers`, `user_credits`, `credit_transactions`, attempts/answers/writing.

## 2. User management

| Method · Path | Purpose |
|---|---|
| `GET /api/admin/users?page&size&search&status&subscription&sortBy&sortOrder` | list/filter/sort |
| `GET /api/admin/users/{id}` | detail |
| `GET /api/admin/users/stats` | totals (users/active/premium/new-this-month) |
| `PATCH /api/admin/users/{id}/status` | set `account_status` + `status_reason` |
| `PATCH /api/admin/users/{id}/credits` | adjust Lúa (`ADMIN_ADJUSTMENT`) |
| `PATCH /api/admin/users/{id}/subscription` | set tier (`cramerie`/`cramerich`; 1/3/6 mo) |

- Credit adjustment goes through `billing.CreditService` (idempotent, writes
  `credit_transactions` with category `ADMIN_ADJUSTMENT`) + logs `user_activities`
  (`ActivityPort`) + writes audit (§4). **Fix:** no raw SQL that bypasses credit invariants.
- Subscription change goes through `billing.SubscriptionService` (resets counters, sets
  expiry) + audit.
- **Fix:** admin id is the authenticated principal; remove the `X-User-Id` requirement and
  its spoofable-attribution risk.

## 3. Admin dashboard

| Method · Path | Purpose |
|---|---|
| `GET /api/admin/dashboard/stats` | top-level counts |
| `GET /api/admin/dashboard/activities?limit` | recent activity |
| `GET /api/admin/dashboard/status` | system status (db/api/payment/ai) |

- **Fix:** replace hard-coded placeholder metrics with **real** queries where feasible; any
  genuinely unavailable metric is explicitly labelled (not silently faked).

## 4. Audit (`AuditPort`)

`AuditService.write(adminId, action, targetType, targetId, oldValue, newValue, description,
ip, userAgent)`. Actions: `BAN`, `UNBAN`, `STATUS_CHANGE`, `CREDITS_ADD`, `CREDITS_SUBTRACT`,
`SUBSCRIPTION_CHANGE`, `SPEAKING_REGRADE`, plus future actions. Published as `AuditPort` so
`speaking`/`billing`/`catalog` admin actions log consistently.

| Method · Path | Purpose |
|---|---|
| `GET /api/admin/activities/users/{userId}?page&size&type` | user activity |
| `GET /api/admin/activities/users/{userId}/recent?limit` | recent user activity |
| `GET /api/admin/activities/audit?page&size` | audit log |
| `GET /api/admin/activities/audit/users/{userId}?page&size` | per-user audit |

## 5. Finance

| Method · Path | Purpose |
|---|---|
| `GET /api/admin/finance/overview?period` | revenue overview |
| `GET /api/admin/finance/chart?period` | time series |
| `GET /api/admin/finance/breakdown?period` | revenue breakdown |
| `GET /api/admin/finance/transactions?page&size&status&type` | paged transactions |
| `GET /api/admin/finance/top-spenders?limit` | ranked spenders |
| `GET /api/admin/finance/export?dateFrom&dateTo&status` | export rows |
| `GET /api/admin/finance/reports?dateFrom&dateTo&granularity` | report bundle |
| `GET /api/admin/finance/reports/{subscriptions,lua-economy,acquisition}` | sub-reports |

- **Fix — consistent data source:** all finance queries use `payment_orders.status = 'PAID'`
  and `amount_vnd` (remove the stale `status='completed'`/`amount` path the dashboard used).
- **Fix — consistent contracts:** endpoints either return data or a real error; they do **not**
  silently return `200 []` for some failures and `500` for others. Reporting reads are
  read-only projections.

## 6. Ports
- Consumes: `billing.CreditService`/`SubscriptionService` (or admin-facing methods),
  `ActivityPort`, profile reads.
- Publishes: `AuditPort`.

## 7. Change log
| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
