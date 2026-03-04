# Cramer Documentation Library

> **Last Updated:** January 10, 2026  
> **Version:** 1.1.0

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
| **Backend** | Spring Boot 3.x, Java 21, Maven |
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
| [DATABASE_SCHEMA.md](backend/DATABASE_SCHEMA.md) | PostgreSQL schema with 26 tables, RLS policies, triggers, indexes, and ER diagrams | 944 |
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

---

## Document Statistics

| Category | Files | Total Lines |
|----------|-------|-------------|
| Backend | 6 | 5,044 |
| Frontend | 5 | 4,330 |
| **Total** | **11** | **9,374** |

---

## Getting Started

### For Backend Developers
1. Start with [DATABASE_SCHEMA.md](backend/DATABASE_SCHEMA.md) to understand the data model
2. Review [ENTITIES.md](backend/ENTITIES.md) for JPA mappings
3. Check [API_REFERENCE.md](backend/API_REFERENCE.md) for endpoint contracts
4. See [SERVICES.md](backend/SERVICES.md) for business logic patterns

### For Frontend Developers
1. Read [STATE_MANAGEMENT.md](frontend/STATE_MANAGEMENT.md) for Zustand patterns
2. Browse [COMPONENTS.md](frontend/COMPONENTS.md) for available UI components
3. Check [PAGES.md](frontend/PAGES.md) for routing structure
4. Reference [UI_DESIGN_SYSTEM.md](frontend/UI_DESIGN_SYSTEM.md) for styling guidelines

### For Content Editors
1. Use [DATA_INGESTION_READING.md](backend/DATA_INGESTION_READING.md) for Reading tests
2. Use [DATA_INGESTION_LISTENING.md](backend/DATA_INGESTION_LISTENING.md) for Listening tests

---

## Directory Structure

```
docs/library/
├── README.md                           # This file
├── backend/
│   ├── API_REFERENCE.md               # REST API documentation
│   ├── DATABASE_SCHEMA.md             # PostgreSQL schema reference
│   ├── DATA_INGESTION_LISTENING.md    # Listening test SQL generation
│   ├── DATA_INGESTION_READING.md      # Reading test SQL generation
│   ├── ENTITIES.md                    # JPA entity reference
│   └── SERVICES.md                    # Service layer documentation
└── frontend/
    ├── API_CLIENT.md                  # Axios API client
    ├── COMPONENTS.md                  # React component library
    ├── PAGES.md                       # Page components & routes
    ├── STATE_MANAGEMENT.md            # Zustand stores
    └── UI_DESIGN_SYSTEM.md            # Design tokens & patterns
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
