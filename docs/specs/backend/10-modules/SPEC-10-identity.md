# SPEC-10 — Identity (Auth & Profile)

> Status: **Authoritative** · Module: `identity` · Depends on: SPEC-04
> Owns: email existence check, user profile self-service. Authentication itself
> (Supabase JWT) lives in `platform.security` (SPEC-04 §1).

---

## 1. Data model (existing tables)

- `profiles` — id (UUID, = Supabase `auth.users.id`), username, full_name, phone_number,
  address, avatar_url, hero_background_url, page_background_url, llm_api_key, llm_model,
  llm_provider, is_admin, account_status, status_reason, last_login_at, created_at.

`Profile` is the mirror of the Supabase auth user; the backend never creates/deletes it
(Supabase auth owns lifecycle).

## 2. API

| Method · Path | Auth | Purpose |
|---|---|---|
| `POST /api/auth/check-email` | public | does an account exist for an email |
| `GET /api/profiles/{id}` | self | read own profile |
| `PUT /api/profiles/{id}` | self | update own profile |

### 2.1 `check-email`
- Body `{ email }` (validated). Uses `EmailLookupService` → `SupabaseAdminClient`
  (service-role, paginated, case-insensitive).
- Response `{ exists: boolean }`.
- **Fix:** on upstream Supabase failure, return **503** — do **not** fabricate
  `exists:false` (the old code hid failures, risking duplicate-signup confusion).

### 2.2 Profile read/update
- **IDOR rule:** the authenticated UUID (from `CurrentUser`) must equal `{id}`, else **403**.
- **Fix:** missing profile → **404** (old code threw → 500).
- Updatable: full_name, phone_number, address, avatar_url, hero_background_url,
  page_background_url, llm_model, llm_provider, and `llm_api_key` (see §2.3).
- Read-only (returned, not updatable): username, is_admin, created_at.

### 2.3 LLM API key handling
- `llm_api_key = ""` → clear; non-empty → store; `null`/absent → unchanged.
- Responses **never** return the stored key; they return `hasLlmApiKey: boolean`.

> Product note: vocabulary translation and writing grading use the **server** key (SPEC-13,
> SPEC-16). Decide during implementation whether per-user `llm_*` fields still have product
> meaning; if not, deprecate them in a later spec (do not silently drop columns — schema is
> frozen).

## 3. Two-factor (TOTP)
- The build includes a TOTP library. **Verify** whether 2FA is wired in current code during
  implementation. If used, add a `SPEC-10a-2fa.md`; if unused, drop the dependency (SPEC-05 §2).

## 4. Removed
- `ProfileServiceOld` (dead). The old list/create/delete/username endpoints are **not**
  reintroduced — Supabase auth owns identity lifecycle.

## 5. Rules & edge cases
- All profile ops require authentication; only self access.
- `account_status` / `status_reason` are written by **admin** (SPEC-17), read here.
- `last_login_at` update is out of scope for this module (auth event; revisit if needed).

## 6. Ports
- Consumes: `platform.security.CurrentUser`, `SupabaseAdminClient`.
- Publishes: none (admin reads profiles via its own read models, SPEC-17).

## 7. Change log
| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
