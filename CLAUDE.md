# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
| Skill | Part 1 | Part 2 | Part 3 | Part 4 |
|-------|--------|--------|--------|--------|
| Reading | Q1-13 | Q14-26 | Q27-40 | - |
| Listening | Q1-10 | Q11-20 | Q21-30 | Q31-40 |
| Writing | Task 1 | Task 2 | - | - |

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
