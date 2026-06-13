# Cramer Canonical Documentation

> **Last Updated:** 17/05/2026  
> **Version:** 1.2.0  
> **Note:** This directory was renamed from `library/` to `canonical/` on 17/05/2026.

> ⚠️ **Backend rewrite (since 11/06/2026).** The backend has been rebuilt from a layered
> architecture into a **vertical-slice, 10-module** design under `com.cramer.*`
> (`platform`, `identity`, `catalog`, `assessment`, `writing`, `speaking`, `billing`,
> `engagement`, `admin`, `abts`). Each module owns its own `web/` · `service/` · `domain/` ·
> `repository/` · `config/`. The **source of truth** for the architecture is
> [`docs/specs/backend/`](../specs/backend/README.md) (SPEC-00…25). All 10 modules are built
> and tested (unit suite green; a `@SpringBootTest` boots the full context against the live
> Supabase schema with `ddl-auto=validate`). The detailed backend docs below
> (`ENTITIES.md`, `SERVICES.md`, `API_REFERENCE.md`) still describe the **legacy** layout and
> are pending regeneration; `DATABASE_SCHEMA.md` remains accurate (schema is frozen/unchanged).

---

## Overview

**Cramer** is a comprehensive IELTS learning platform designed to help students prepare for Reading, Listening, and Writing tests. The platform features:

- 📚 **Authentic Test Content** — Full Cambridge IELTS test banks
- ⏱️ **Timed Practice Sessions** — Real exam conditions with auto-save
- 🤖 **AI-Powered Grading** — DeepSeek-based writing assessment with detailed feedback
- 📊 **Progress Tracking** — Dashboard with attempt history and analytics
- 💎 **Subscription Tiers** — Cramerie (Free), Cramerich, and Cramerous plans
- 🌾 **Lúa Credit System** — Virtual currency for premium features

---

## Technology Stack

| Layer | Technologies |
|-------|-------------|
| **Frontend** | React 18, Vite, Tailwind CSS, Zustand, Framer Motion |
| **Backend** | Spring Boot 4.0, Java 25, Maven (vertical-slice modules) |
| **Database** | PostgreSQL (Supabase) with RLS |
| **Auth** | Supabase Auth with JWT |
| **AI** | OpenRouter API (400+ models), DeepSeek V3.2 |
| **Payments** | PayOS Gateway (Vietnam) |

---

## Quick Navigation

### 📦 Backend Documentation

| Document | Description | Lines |
|----------|-------------|-------|
| [API_REFERENCE.md](backend/API_REFERENCE.md) | Complete REST API documentation with endpoints, request/response formats, and authentication details | 1054 |
| [DATABASE_SCHEMA.md](backend/DATABASE_SCHEMA.md) | Canonical live Supabase schema reference with active tables, legacy archive notes, RLS policies, triggers, indexes, and ER diagrams | 1,050+ |
| [ENTITIES.md](backend/ENTITIES.md) | JPA entity reference covering all domain models with field definitions and relationships | 878 |
| [SERVICES.md](backend/SERVICES.md) | Service layer documentation with 38 services, ABTS system, and dependency diagrams | 1,279 |
| [DATA_INGESTION_READING.md](backend/DATA_INGESTION_READING.md) | Guide for parsing and ingesting IELTS Reading test content via SQL | 518 |
| [DATA_INGESTION_LISTENING.md](backend/DATA_INGESTION_LISTENING.md) | Guide for parsing and ingesting IELTS Listening test content with audio and question blocks | 376 |

### 🎨 Frontend Documentation

| Document | Description | Lines |
|----------|-------------|-------|
| [PAGES.md](frontend/PAGES.md) | Route overview and page component catalog with user flow diagrams | 725 |
| [COMPONENTS.md](frontend/COMPONENTS.md) | Reusable component library covering layout, navigation, forms, and test-taking UI | 1049 |
| [STATE_MANAGEMENT.md](frontend/STATE_MANAGEMENT.md) | Zustand store architecture with 11 stores including ABTS generation state | 831 |
| [API_CLIENT.md](frontend/API_CLIENT.md) | Axios-based API client with JWT injection, error handling, and domain-organized modules | 861 |
| [UI_DESIGN_SYSTEM.md](frontend/UI_DESIGN_SYSTEM.md) | Comprehensive design system with colors, typography, glassmorphism, and responsive patterns | 868 |
| [PATTERNS/PAGE_PATTERNS.md](frontend/PATTERNS/PAGE_PATTERNS.md) | Generic page structure, loading/error/empty states, BEM convention | ~200 |
| [PATTERNS/COMPONENT_PATTERNS.md](frontend/PATTERNS/COMPONENT_PATTERNS.md) | Shared component library (BaseModal, Pagination, FullPageLoader, etc.) | ~150 |
| [PATTERNS/STORE_PATTERNS.md](frontend/PATTERNS/STORE_PATTERNS.md) | Zustand store conventions, middleware, async actions, caching | ~200 |
| [PATTERNS/ROUTE_PATTERNS.md](frontend/PATTERNS/ROUTE_PATTERNS.md) | Routing architecture, lazy loading, protected routes | ~150 |
| [PATTERNS/API_CLIENT_PATTERNS.md](frontend/PATTERNS/API_CLIENT_PATTERNS.md) | Axios setup, JWT injection, API module organization | ~150 |
| [PATTERNS/CSS_PATTERNS.md](frontend/PATTERNS/CSS_PATTERNS.md) | CSS naming, glassmorphism tiers, responsive breakpoints, animations | ~200 |
| [SPEAKING/SPEAKING_ARCHITECTURE.md](frontend/SPEAKING/SPEAKING_ARCHITECTURE.md) | Speaking frontend architecture overview | ~100 |
| [SPEAKING/SPEAKING_PAGES.md](frontend/SPEAKING/SPEAKING_PAGES.md) | SpeakingSessionPage and SpeakingResultsPage specs | ~100 |
| [SPEAKING/SPEAKING_COMPONENTS.md](frontend/SPEAKING/SPEAKING_COMPONENTS.md) | Speaking component specs | ~150 |
| [SPEAKING/SPEAKING_STORE.md](frontend/SPEAKING/SPEAKING_STORE.md) | useSpeakingStore specification | ~100 |
| [SPEAKING/SPEAKING_API.md](frontend/SPEAKING/SPEAKING_API.md) | speakingApi module and endpoint reference | ~100 |
| [SPEAKING/SPEAKING_HOOKS.md](frontend/SPEAKING/SPEAKING_HOOKS.md) | useGeminiLive, useAudioRecorder, useTimer specs | ~100 |
| [SPEAKING/SPEAKING_CSS.md](frontend/SPEAKING/SPEAKING_CSS.md) | CSS file structure and BEM prefix | ~100 |

---

## Document Statistics

| Category | Files | Total Lines |
|----------|-------|-------------|
| Backend | 6 | 5,044 |
| Frontend (existing) | 5 | 4,330 |
| Frontend (patterns) | 6 | ~1,100 |
| Frontend (speaking) | 7 | ~750 |
| **Total** | **24** | **~11,224** |

---

## Getting Started

### For Backend Developers
1. Start with [DATABASE_SCHEMA.md](backend/DATABASE_SCHEMA.md) to understand the data model
2. Review [ENTITIES.md](backend/ENTITIES.md) for JPA mappings
3. Check [API_REFERENCE.md](backend/API_REFERENCE.md) for endpoint contracts
4. See [SERVICES.md](backend/SERVICES.md) for business logic patterns
5. For ops tasks: see `docs/ops/` (migrations, queries, runbooks)

### For Frontend Developers
1. Start with [PATTERNS/PAGE_PATTERNS.md](frontend/PATTERNS/PAGE_PATTERNS.md) for page structure conventions
2. Read [PATTERNS/STORE_PATTERNS.md](frontend/PATTERNS/STORE_PATTERNS.md) for Zustand patterns
3. Browse [PATTERNS/COMPONENT_PATTERNS.md](frontend/PATTERNS/COMPONENT_PATTERNS.md) for available UI components
4. Check [PATTERNS/ROUTE_PATTERNS.md](frontend/PATTERNS/ROUTE_PATTERNS.md) for routing structure
5. Reference [UI_DESIGN_SYSTEM.md](frontend/UI_DESIGN_SYSTEM.md) for styling guidelines
6. For Speaking feature: see [SPEAKING/](frontend/SPEAKING/) directory

### For Content Editors
1. Use [DATA_INGESTION_READING.md](backend/DATA_INGESTION_READING.md) for Reading tests
2. Use [DATA_INGESTION_LISTENING.md](backend/DATA_INGESTION_LISTENING.md) for Listening tests

---

## Directory Structure

```
docs/
├── canonical/                         # ★ Single source of truth (was library/)
│   ├── README.md                      # This file
│   ├── backend/                       # Backend architecture docs
│   └── frontend/                      # Frontend architecture docs
│
├── ops/                               # ★ Operational docs
│   ├── migrations/                    # SQL migration files
│   ├── queries/                       # Data ingestion SQL queries
│   ├── postman/                       # Postman collections
│   ├── runbooks/                      # Operational runbooks
│   └── bug-reports/                   # Bug and vulnerability reports
│
├── plans/                             # ★ Feature plans (was short_term_plans/)
│   ├── speaking/                      # Speaking feature plans
│   ├── ai/                            # AI feature plans
│   └── ...                            # Other plans
│
├── architecture/                      # ★ System design docs
│   ├── userflow/                      # PlantUML user flow diagrams
│   └── decisions/                     # Architecture Decision Records
│
├── content/                           # ★ IELTS content (PDFs, audio, images)
│   ├── test_materials/                # Cambridge IELTS PDFs + audio
│   ├── speaking/                      # Speaking foundations
│   └── marking_criteria/              # IELTS band descriptors
│
├── test-reports/                      # ★ Consolidated test reports
│   ├── backend/
│   └── frontend/
│
└── archive/                           # ★ Obsolete but preserved
    ├── temp/
    └── plans/
```

---

## Related Resources

- **Root README:** [`../../README.md`](../../README.md) — Project overview and quick start
- **Backend Build:** [`../../backend/BUILD_INSTRUCTIONS.md`](../../backend/BUILD_INSTRUCTIONS.md) — Maven build guide
- **Frontend Setup:** [`../../frontend/README.md`](../../frontend/README.md) — npm setup instructions
- **Agent Instructions:** [`../../AGENTS.md`](../../AGENTS.md) — AI agent protocols and conventions

---

## Contributing

When adding new documentation:
1. Place backend docs in `backend/` and frontend docs in `frontend/`
2. Use consistent naming: `UPPERCASE_WITH_UNDERSCORES.md`
3. Include metadata headers (Last Updated, Version)
4. Update this README with the new document link and description

---

<div align="center">
  <sub>Built with ❤️ by the Cramer Team</sub>
</div>
