# AGENTS.md

This file provides guidance when working with code in this repository.

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

- **Backend**: Spring Boot 3.3, Java 17, Spring Data JPA, Spring Security
- **Frontend**: React 18, Vite, Zustand (state), Tailwind CSS, React Bootstrap
- **Database**: Supabase PostgreSQL with RLS policies
- **AI Integration**: DeepSeek V3.2 (via OpenAI-compatible API) for writing grading, OpenRouter for ABTS

### Backend Structure (`backend/src/main/java/com/cramer/`)

- `controller/` - REST endpoints (`/api/**` are authenticated)
- `controller/admin/` - Admin-only endpoints (ABTSController, TestHierarchyController, etc.)
- `service/` - Business logic
- `service/abts/` - AI test generation (ABTSService, PromptBuilderService)
- `service/implement/` - Service implementations
- `entity/` - JPA entities with JSONB support (Hypersistence Utils)
- `repository/` - Spring Data repositories
- `config/` - SecurityConfig, JwtAuthFilter, WebConfig, PayOSConfig
- `dto/` - Data transfer objects

### Frontend Structure (`frontend/src/`)

- `stores/` - Zustand stores (useAuthStore, useTestStore, useProfileStore, etc.)
- `api/backendApi.js` - Axios client with JWT token injection
- `pages/` - Public user-facing pages
- `admin/` - Admin dashboard module
  - `admin/pages/` - Admin pages (ContentListPage, AIGenerationPage, etc.)
  - `admin/components/` - Admin UI components
  - `admin/stores/useABTSStore.js` - ABTS frontend state
- `components/` - Shared React components
- `css/` - Stylesheets

### Database Schema (Supabase)

- `profiles` - User profile data linked to Supabase auth
- `test_sets` -> `tests` -> `sections` -> `questions` - Content hierarchy
- `test_attempts`, `user_answers` - User test progress (RLS enabled)
- `subscription_tiers`, `user_subscriptions`, `user_credits` - Monetization
- `payment_orders` - PayOS payment tracking

## Key Patterns

### Authentication

- JwtAuthFilter validates Supabase JWTs before UsernamePasswordAuthFilter
- Extract user ID: `SecurityContextHolder.getContext().getAuthentication().getName()`
- Frontend sets `Authorization: Bearer <token>` via setupApiClient in backendApi.js

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
- `SUPABASE_JWT_SECRET`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`
- `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`

Frontend `.env`:

- `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`
- `VITE_API_BASE_URL` (optional, defaults to localhost:8080)

## Development Notes

- After backend changes: rebuild with `./mvnw clean package -DskipTests` and restart
- Convert .env line endings if needed: `dos2unix .env` or `sed -i 's/\r$//' .env`
- DB schema changes require migration files in `docs/backend/migrations/`
- Keep PRs small and well-scoped; update docs when modifying schema

## Key Documentation

- `docs/backend/supabase-backend.md` - Schema and operational guidance
- `docs/CRAMER_ABTS_SPECS.md` - ABTS specification
- `docs/CRAMER_CMS_ADMIN_SPECS.md` - Admin panel specs
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


