# CURSOR.md

Cramer is an IELTS practice platform. Treat the repository as the source of truth; use docs as hints, not authority.

## Verification rules

1. Read the relevant files before explaining or changing anything.
2. Verify paths, symbols, and usage in the actual codebase.
3. If code and docs disagree, trust the code and mention the mismatch.
4. Prefer facts over inference. If uncertain, search more or ask.
5. Keep changes small, local, and consistent with existing patterns.

## Codebase shape

- Backend: Spring Boot + Maven in `backend/`
- Frontend: React + Vite in `frontend/`
- Data/auth/storage: Supabase PostgreSQL, Supabase JWTs, RLS
- Main areas: tests, questions, speaking, writing, chat, subscriptions, credits, payments, admin, ABTS

## Actual hotspots

### Backend

- Modules (one bounded context each, **no** global layers): `identity/`, `catalog/`, `assessment/`, `writing/`, `speaking/`, `billing/`, `engagement/`, `admin/`, `abts/`
- Each module owns: `web/` (controllers) + `web/dto/` (request/response records) · `service/` (+ cross-module `Port` interfaces) · `domain/` (Lombok JPA entities/enums) · `repository/` · some have `config/`
- Shared kernel: `platform/` — `web/` (GlobalExceptionHandler, WebConfig, HealthController), `security/` (`SecurityConfig` OAuth2 resource server, `SupabaseJwtConfig` NimbusJwtDecoder, `AdminAuthorizationService`, `CurrentUser`), `error/`, `integration/` (`llm`, `openrouter`, `supabase`), `ratelimit/`, `common/`, `config/`
- ABTS generation: `abts/` — `generation/` (+ `generation/prompt/`), `validation/`, `web/` (SSE streaming)
- Admin content CRUD: `catalog/web/admin/`; cross-domain admin console: `admin/`
- There is **no** global `controller/`, `service/implement/`, `entity/`, `dto/`, or `repository/` package anymore
- Architecture source of truth: `docs/specs/backend/` (SPEC-00…25)

### Frontend

- API client: `frontend/src/api/backendApi.js`
- Global state: `frontend/src/stores/`
- Public pages: `frontend/src/pages/`
- Admin module: `frontend/src/admin/`
- Shared UI: `frontend/src/components/`
- Styles: `frontend/src/css/`

## Key patterns

- Authentication uses Supabase JWTs with Spring Security.
- Frontend sends `Authorization: Bearer <token>` through the shared API client.
- Zustand is the primary global state layer.
- Delete repository methods often use `@Modifying(clearAutomatically = true, flushAutomatically = true)`.
- Prefer explicit DTOs over exposing entities directly through the API.
- Keep admin, speaking, content-generation, quota, and payment flows aligned with local patterns.

## Change discipline

- Keep changes as narrow as possible.
- Preserve existing layering unless a task explicitly requires a refactor.
- Treat schema, auth, quota, payment, grading, and websocket changes as high risk.
- Update docs or migrations only when the implementation change truly requires it.

## Useful commands

### Backend

```bash
cd backend && ./run-app.ps1
cd backend && ./mvnw clean package -DskipTests
cd backend && ./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend && npm install
cd frontend && npm run dev
cd frontend && npm run build
```

## Documentation and schema work

- Use backend docs and specs as references, but verify against implementation.
- If docs conflict with code, mention the conflict and proceed from the code.
- Reflect schema changes in `docs/ops/migrations/` when applicable.

## Google Workspace Copy Pack

For non-trivial tasks, keep concise copy-ready progress notes when useful.

### Output rules

- Use headings: `Workspace Copy Pack - START`, `Workspace Copy Pack - PROGRESS`, `Workspace Copy Pack - DONE`
- Use TSV rows for Sheets tabs
- Use Vietnamese wording unless the user asks otherwise
- Use `TBD` for unknown links, IDs, dates, or metrics
- Keep only the affected tabs
- Use `dd/mm/yyyy` for dates

### Workbook tabs

**A. Now-Next-Later**
- Columns: `Item | Bucket | Owner | Status | Main Link | Updated | Note`
- Bucket: `Now`, `Next`, `Later`
- Owner: `Jacob`, `Khoa`, `Both`
- Status: `Scoped`, `Doing`, `Review`, `Blocked`, `Done`

**B. Feature Pipeline**
- Columns: `Feature | Stage | Lead | Success Signal | Main Link | Next Step`
- Stage: `Discovery`, `Spec`, `Build`, `Validate`, `Shipped`
- Lead: `Jacob`, `Khoa`, `Both`

**C. Risks-Dependencies**
- Columns: `Type | Topic | Severity | Owner | Action Needed | State | Link`
- Type: `Risk`, `Dependency`
- Severity: `High`, `Medium`, `Low`
- Owner: `Jacob`, `Khoa`, `Both`
- State: `Open`, `Watching`, `Resolved`

**D. Release Log**
- Columns: `Date | What Shipped | Proof Links | Impact | Follow-up`
- Impact: `User-facing`, `Internal`, `Mixed`
- Follow-up: `None`, `Minor`, `Important`

### Companion docs

- `2 - Decision Log` for long-lived or hard-to-reverse decisions
- `3 - Weekly Notes` for `Done`, `In progress`, `Blocked`, `Next`
- `4 - Prompt Templates` for reusable prompting patterns

## Subagent Model Consistency Rule

When delegating tasks to subagents (via `runSubagent` or `Explore` agent), the **same model** running the current session must be used for all subagents. Specifically:

- If the current agent is **DeepSeek V4 Flash (opencodego-enhanced)**, all subagents must also use `DeepSeek V4 Flash (opencodego-enhanced)`.
- If the current agent is **Claude Haiku 4.5 (copilot)**, all subagents must use `Claude Haiku 4.5 (copilot)`.
- Never fall back to a different model family for subagents — the model parameter must match the current session's model exactly.
- The model string must match the exact name from the available models list (e.g., `"DeepSeek V4 Flash (opencodego-enhanced)"`, not `"DeepSeek V4 Flash"` or `"DeepSeek V4 Flash (opencode)"`).

This ensures consistent behavior and output quality across all parallel research tasks.

## Repo-specific note

If a task is non-trivial, keep updates short, structured, and grounded in evidence from the repo.
