# Cramer

## Overview

Cramer is an English practice platform that delivers curated IELTS-style tests in a modern web experience. Learners sign up, log in, and work through Reading, Listening, Writing, and Speaking materials, with AI-assisted grading and progress tracking. Content is built from trusted sources, starting with Cambridge IELTS.

## Architecture

- **Backend:** Spring Boot 4.0.0 (Java 25, Maven) — vertical-slice modules under `com.cramer.*` (`platform`, `identity`, `catalog`, `assessment`, `writing`, `speaking`, `billing`, `engagement`, `admin`, `abts`).
- **Frontend:** React 19 + Vite 8, Zustand 5 for state, Tailwind CSS 4, React Bootstrap.
- **Database / Auth / Storage:** Supabase (managed PostgreSQL with Row Level Security, Supabase Auth, storage).
- **AI:** DeepSeek (writing grading, OpenAI-compatible API), OpenRouter (ABTS content generation), Google Gemini (speaking evaluation).
- **Payments:** PayOS (Vietnam).

## Features

- Authentication via Supabase Auth (JWT validated by the backend OAuth2 resource server).
- IELTS practice across Reading, Listening, Writing, and Speaking, with timed sessions and auto-save.
- AI-powered grading for Writing (DeepSeek) and Speaking (Gemini), plus attempt history and dashboards.
- Subscription tiers (Cramerie / Cramerich / Cramerous) and the Lúa virtual-credit system.
- Admin console and the AI-Based Test Generation System (ABTS) for authoring IELTS content.

## Getting Started

1. Clone the repository and obtain access to the Supabase project credentials and required API keys.
2. Configure environment variables:
   - Root `.env` (backend): `SPRING_DATASOURCE_*`, `SUPABASE_URL`, `SUPABASE_JWT_SECRET`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, `DEEPSEEK_API_KEY`, `OPENROUTER_API_KEY`, `PAYOS_*`.
   - `frontend/.env`: `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`, `VITE_API_BASE_URL` (optional).
3. Run the backend: `cd backend && ./run-app.ps1` (Windows) or `./run-app.sh` (Linux/macOS). API at http://localhost:8080 (Swagger at `/swagger-ui.html`).
4. Run the frontend: `cd frontend && npm install && npm run dev` (dev server on port 5173).

See `AGENTS.md` for contributor/agent conventions, `docs/specs/backend/` for the authoritative backend architecture, and `docs/canonical/` for backend + frontend reference docs.

### Browser Compatibility

**Recommended browsers:**
- ✅ Google Chrome
- ✅ Mozilla Firefox
- ✅ Zen Browser
- ⚠️ Microsoft Edge (requires cookie settings adjustment)

## Roadmap

- Expand content coverage to additional IELTS materials and full Cambridge test banks.
- Deepen analytics dashboards for learner performance.
- Introduce additional exams (IELTS General, TOEIC, and more) and broaden AI-based marking accuracy.

## Contributing

Contributions are welcome as the project matures. Open an issue or submit a pull request with a clear description of the problem or enhancement. Keep PRs small and well-scoped, and coordinate schema changes with the team to keep the Supabase environment in sync (migrations live in `docs/ops/migrations/`).
