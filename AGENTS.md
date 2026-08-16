# AGENTS.md

This file provides guidance when working with code in this repository.

## Communication Standard: ASD-STE100 (Issue 9)

All assistant communication with the user and all project documentation must follow ASD-STE100 (Simplified Technical English), Issue 9. The same rules apply to every language the assistant uses, for example Vietnamese.

Core rules applied:

- Write short sentences. Use 20 words maximum for instructions. Use 25 words maximum for descriptions.
- Use one word for one meaning. Prefer the approved STE dictionary over synonyms. For example, use "start", not "initiate", "begin", or "commence".
- Use the active voice. Do not use gerunds in instructions ("turn the knob", not "turning the knob").
- Use only simple verb tenses (past, present, future).
- Use "must" for requirements. Use "can" for possibilities. Use "do not" for prohibitions. In Vietnamese, use "phải", "có thể", and "không được".
- Keep noun clusters short. Use 3 nouns maximum in one cluster.
- Do not omit articles (a, an, the) or helper verbs.
- Explain technical terms at first use.

This rule applies to chat responses, documentation, and commit messages. Code and code comments keep existing project conventions.

## Verification Protocol (Anti-Hallucination)

1. **Verify Paths**: Never assume a file exists based on common patterns. detailed directory structures often vary. Use `ls` or `find` to confirm.
2. **Read Before Explaining**: logical assumptions are often wrong. Read the code file before explaining 'how it works'.
3. **Check Usage**: Before stating a library or feature is used, `grep` the codebase to prove it.
4. **Admit Unknowns**: It is better to stop and ask or search than to invent a plausible-sounding answer.

## Project Overview

Cramer is an IELTS practice platform with a Spring Boot backend and React frontend. It uses Supabase for PostgreSQL database, authentication, and storage. The platform includes an AI-Based Test Generation System (ABTS) for creating IELTS content.

## Build & Run Commands

### Backend (Spring Boot + Maven)

```bash
# Development (Linux/macOS) - loads .env from root
cd backend && ./run-app.sh

# Development (Windows)
cd backend && ./run-app.ps1

# Manual build
cd backend && ./mvnw clean package -DskipTests
java -jar target/cramer-backend-0.0.1-SNAPSHOT.jar

# Maven run directly
cd backend && ./mvnw spring-boot:run
```

### Frontend (React + Vite)

```bash
cd frontend && npm install
npm run dev          # Dev server on port 5173
npm run build        # Production build
npm run preview      # Preview production build
```

### Swagger UI

http://localhost:8080/swagger-ui.html (requires JWT token via Authorize button)

## Architecture

### Tech Stack

- **Backend**: Spring Boot 4.0.0, Java 25, Spring Data JPA, Spring Security (OAuth2 resource server)
- **Frontend**: React 19, Vite 8, Zustand 5 (state), Tailwind CSS 4, React Bootstrap
- **Database**: Supabase PostgreSQL with RLS policies
- **AI Integration**: DeepSeek (`deepseek-reasoner` default, via OpenAI-compatible API at `api.deepseek.com`) for writing grading; OpenRouter (`deepseek/deepseek-v4-flash` default) for ABTS generation; Google Gemini for speaking evaluation

### Backend Structure (`backend/src/main/java/com/cramer/`)

Vertical-slice modules (one bounded context each); **no** global controller/service/entity layers. Source of truth: `docs/specs/backend/` (SPEC-00…25).

- `platform/` - Shared kernel: `web/` (GlobalExceptionHandler, WebConfig, HealthController), `security/` (SupabaseJwtConfig, SecurityConfig OAuth2 resource server, AdminAuthorizationService, CurrentUser), `error/`, `integration/` (openrouter, llm, supabase), `ratelimit/`, `config/`, `common/`
- `identity/` - Supabase-JWT auth + profile
- `catalog/` - test_sets/tests/sections/questions/hashtags; admin content CRUD; course browse; ContentDraftPort
- `assessment/` - test_attempts, user_answers, scoring, review
- `writing/` - writing_submissions + async DeepSeek grading
- `speaking/` - sessions, blueprint, transcripts, async grading worker/watchdog, admin regrade
- `billing/` - subscription, credit (Lúa), quota, payment (PayOS), feature gating, schedulers
- `engagement/` - chat, vocabulary/translation, dashboard read-models, activity
- `admin/` - cross-domain console (users, audit, dashboard, finance)
- `abts/` - AI generation (OpenRouter): prompting, generation pipelines, validation, refinement, SSE streaming
- Each module: `web/` (Controller + web/dto records) · `service/` (+ cross-module Ports) · `domain/` (Lombok entities/enums) · `repository/` · `config/`

### Frontend Structure (`frontend/src/`)

- `stores/` - Zustand stores (useAuthStore, useTestStore, useProfileStore, etc.)
- `api/backendApi.js` - Axios client with JWT token injection
- `pages/` - Public user-facing pages
- `admin/` - Admin dashboard module
  - `admin/pages/` - Admin pages (ContentListPage, AIGenerationPage, etc.)
  - `admin/components/` - Admin UI components (some with co-located CSS)
  - `admin/css/` - Admin stylesheets (dark theme, separate tokens)
  - `admin/stores/useABTSStore.js` - ABTS frontend state
- `components/` - Shared React components
- `css/` - Stylesheets (refactored May 2026)
  - `tokens.css` - **Single source of truth** for ALL CSS custom properties
  - `shared/` - Reusable: `layout.css` (sl-* sidebar), `animations.css` (all @keyframes)
  - `common/` - Shared UI: `modal.css`, `faq.css`, `testimonials.css`, `grading-loader.css`
  - `components/` - Per-component CSS (header, footer, pagination, etc.)
  - `test/` - **Consolidated test UI** (8 files from original 22): `test-base.css`, `test-header-footer.css`, `test-question.css`, `test-reading.css`, `test-listening.css`, `test-writing.css`, `test-review.css`, `writing-result.css`
  - `pages/` - Page-specific overrides (home, about, pricing, courses, etc.)

### CSS Import Patterns

```
styles.css  →  tokens.css  (global, via main.jsx)
pages       →  shared/layout.css + pages/{page}.css
test pages  →  test/test-base.css + test/test-header-footer.css + test/{skill}.css + test/test-question.css
admin       →  admin/css/admin.css → admin/css/tokens.css
```

### Database Schema (Supabase)

Schema is **frozen** (JPA `ddl-auto=validate`). ~30 active tables; full reference in `docs/canonical/backend/DATABASE_SCHEMA.md`.

- `profiles` - User profile data linked to Supabase auth
- `test_sets` -> `tests` -> `sections` -> `questions` - Content hierarchy (+ `hashtags`, `test_hashtags`)
- `test_attempts`, `user_answers` - User test progress (RLS enabled)
- `writing_submissions` - Writing tasks + async grading
- `speaking_sessions`, `speaking_transcripts` - Speaking (legacy `speaking_*_legacy` archived)
- `subscription_tiers`, `user_subscriptions`, `user_credits`, `credit_transactions`, `lua_packs` - Monetization (Lúa)
- `user_quotas`, `skill_quotas`, `chatbot_usage`, `translation_usage` - Quota/usage tracking
- `chat_messages`, `vocabulary`, `user_activities` - Engagement
- `payment_orders` - PayOS payment tracking
- `admin_audit_log` - admin action audit; `abts_templates` - ABTS templates; `model_runtime_status` - AI model health

## Key Patterns

### Authentication

- Spring Security **OAuth2 resource server** validates Supabase HS256 JWTs via `NimbusJwtDecoder` (`platform/security/SupabaseJwtConfig`, wired in `platform/security/SecurityConfig`). There is **no** custom `JwtAuthFilter`.
- Extract user ID (JWT subject): `SecurityContextHolder.getContext().getAuthentication().getName()`, or inject via `platform/security/CurrentUser`
- Admin authorization via `platform/security/AdminAuthorizationService`
- Frontend sets `Authorization: Bearer <token>` via the Axios client in `frontend/src/api/backendApi.js`

### State Management

- All global state uses Zustand stores (AuthContext is deprecated)
- Import pattern: `import { useAuthStore, useProfileStore } from '../stores'`
- Use selectors: `const user = useAuthStore(state => state.user)`

### Repository Operations

- Use `@Query` with `@Modifying(clearAutomatically = true, flushAutomatically = true)` for DELETE operations
- RLS requires service_role policies for backend operations

### ABTS Question Ranges

| Skill     | Part 1 | Part 2 | Part 3 | Part 4 |
| --------- | ------ | ------ | ------ | ------ |
| Reading   | Q1-13  | Q14-26 | Q27-40 | -      |
| Listening | Q1-10  | Q11-20 | Q21-30 | Q31-40 |
| Writing   | Task 1 | Task 2 | -      | -      |

## Environment Variables

Root `.env` file (loaded by run scripts):

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SUPABASE_URL`, `SUPABASE_JWT_SECRET`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`
- `DEEPSEEK_API_KEY`, `DEEPSEEK_MODEL`, `OPENROUTER_API_KEY`
- `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`, `PAYOS_RETURN_URL`, `PAYOS_CANCEL_URL`

Frontend `.env`:

- `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`
- `VITE_API_BASE_URL` (optional, defaults to localhost:8080)

## Development Notes

- After backend changes: rebuild with `./mvnw clean package -DskipTests` and restart
- Convert .env line endings if needed: `dos2unix .env` or `sed -i 's/\r$//' .env`
- DB schema changes require migration files in `docs/ops/migrations/`
- Keep PRs small and well-scoped; update docs when modifying schema

## Key Documentation

- `docs/specs/backend/README.md` - Backend architecture **source of truth** (SPEC-00…25)
- `docs/canonical/README.md` - Canonical docs hub (backend + frontend references)
- `docs/canonical/backend/DATABASE_SCHEMA.md` - Live Supabase schema reference
- `docs/canonical/backend/{API_REFERENCE,SERVICES,ENTITIES}.md` - REST API, services, JPA entities
- `docs/specs/backend/20-ai-generation/` - ABTS specification (SPEC-20…25)
- `docs/specs/backend/10-modules/SPEC-17-admin.md` - Admin module spec
- `docs/canonical/frontend/UI_DESIGN_SYSTEM.md` - UI design system, tokens, naming conventions
- `backend/BUILD_INSTRUCTIONS.md` - Detailed build troubleshooting

## Google Workspace Sync Protocol

When handling any non-trivial task, include a **Workspace Copy Pack** so Jacob and Khoa can paste updates quickly into Google Workspace.

### Scope and timing

- Provide update drafts at relevant checkpoints: **START**, **PROGRESS**, **DONE**.
- Keep this lightweight: only include tabs affected by the task.
- Never invent Issue/PR links, IDs, dates, or metrics. Use `TBD` if unknown.

### Output format requirements

- Use fenced code blocks for copy-ready snippets.
- For Google Sheets tabs, output **TSV rows** (tab-separated) for easy paste.
- Prefer Vietnamese wording for row text unless user asks for another language.
- Use date format `dd/mm/yyyy`.

### `0 - Worksheet` schemas and allowed dropdown values

1) **A. Now-Next-Later**

- Columns: `Item | Bucket | Owner | Status | Main Link | Updated | Note`
- Bucket: `Now`, `Next`, `Later`
- Owner: `Jacob`, `Khoa`, `Both`
- Status: `Scoped`, `Doing`, `Review`, `Blocked`, `Done`

2) **B. Feature Pipeline**

- Columns: `Feature | Stage | Lead | Success Signal | Main Link | Next Step`
- Stage: `Discovery`, `Spec`, `Build`, `Validate`, `Shipped`
- Lead: `Jacob`, `Khoa`, `Both`

3) **C. Risks-Dependencies**

- Columns: `Type | Topic | Severity | Owner | Action Needed | State | Link`
- Type: `Risk`, `Dependency`
- Severity: `High`, `Medium`, `Low`
- Owner: `Jacob`, `Khoa`, `Both`
- State: `Open`, `Watching`, `Resolved`

4) **D. Release Log**

- Columns: `Date | What Shipped | Proof Links | Impact | Follow-up`
- Impact: `User-facing`, `Internal`, `Mixed`
- Follow-up: `None`, `Minor`, `Important`

### Companion drafts for Google Docs (when relevant)

- **`2 - Decision Log`**: include ADR-lite draft if decision is long-lived or expensive to reverse (architecture, auth/security, schema, AI behavior, billing/quota).
- **`3 - Weekly Notes`**: include concise bullets split into `Done`, `In progress`, `Blocked`, `Next`.
- **`4 - Prompt Templates`**: if a useful prompt pattern emerges, include reusable template fields: `Name`, `When to use`, `Inputs`, `Prompt`, `Expected output`.

### Response skeleton

Use these headings when returning workspace snippets:

- `Workspace Copy Pack - START`
- `Workspace Copy Pack - PROGRESS`
- `Workspace Copy Pack - DONE`

Only include affected tabs; omit unchanged tabs.

### Quick prompts (copy-paste)

Use these ultra-short prompts in any AI tool (OpenCode, Antigravity, Copilot Chat, Claude Code). AI should infer context from the current session and return copy-ready output.

**Ultra-short aliases (recommended)**

- `ws s` or `ws start` -> Generate only `Workspace Copy Pack - START`
- `ws p` or `ws progress` -> Generate only `Workspace Copy Pack - PROGRESS`
- `ws d` or `ws done` -> Generate only `Workspace Copy Pack - DONE`
- `ws all` -> Generate relevant checkpoints among START/PROGRESS/DONE
- Optional scope: `ws d A,D` (only include tabs A and D)

**Alias interpretation rules**

- Infer task summary, progress, and evidence from the current session.
- Use `TBD` for missing links/IDs/metrics instead of guessing.
- Keep only affected tabs; omit unchanged tabs.
- Return copy-ready blocks only.

If you need stricter control, use the extended prompts below.

1) **START prompt**

```text
Task: <short task summary>
Generate only "Workspace Copy Pack - START".
Requirements:
- Include only affected tabs from "0 - Worksheet".
- Use TSV rows for Sheets output.
- Use Vietnamese wording for row content.
- Date format: dd/mm/yyyy.
- Do not invent issue/PR IDs, links, dates, or metrics; use TBD if unknown.
- Return copy-ready blocks only.
```

2) **PROGRESS prompt**

```text
Task: <short task summary>
Current progress: <what is completed / what remains>
Generate only "Workspace Copy Pack - PROGRESS".
Requirements:
- Include only tabs affected right now.
- Use TSV rows for Sheets output.
- Keep each row concise and factual.
- Date format: dd/mm/yyyy.
- Use TBD for unknown values.
- Return copy-ready blocks only.
```

3) **DONE prompt**

```text
Task completed: <short task summary>
Evidence: <files changed, test command results, PR/issue links if available>
Generate only "Workspace Copy Pack - DONE".
Requirements:
- Include only tabs affected by this completed task.
- Use TSV rows for Sheets output.
- Use Vietnamese wording for row content.
- Date format: dd/mm/yyyy.
- Use TBD where information is missing.
- Return copy-ready blocks only.
```

4) **Full-cycle prompt (START + PROGRESS + DONE)**

```text
Task: <short task summary>
Context: <optional branch/issue/doc links>
Progress status: <start|in-progress|done>
Generate Workspace copy packs for all relevant checkpoints among START, PROGRESS, DONE.
Requirements:
- Follow "0 - Worksheet" schemas exactly.
- Use TSV rows for Sheets output.
- Keep outputs concise and copy-ready.
- Date format: dd/mm/yyyy.
- Use TBD for unknown values.
- Omit unchanged tabs.
```


