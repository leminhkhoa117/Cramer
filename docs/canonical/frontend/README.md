# Frontend Documentation

> **Last Updated:** 17/05/2026

This directory contains all frontend-specific documentation for the Cramer platform.

---

## Design System & Patterns

| Document | Description |
|----------|-------------|
| [UI_DESIGN_SYSTEM.md](./UI_DESIGN_SYSTEM.md) | Design tokens, CSS architecture, color palette, typography, glassmorphism |
| [PATTERNS/PAGE_PATTERNS.md](./PATTERNS/PAGE_PATTERNS.md) | Generic page structure, loading/error/empty states, BEM convention |
| [PATTERNS/COMPONENT_PATTERNS.md](./PATTERNS/COMPONENT_PATTERNS.md) | Shared component library (BaseModal, Pagination, FullPageLoader, etc.) |
| [PATTERNS/STORE_PATTERNS.md](./PATTERNS/STORE_PATTERNS.md) | Zustand store conventions, middleware, async actions, caching |
| [PATTERNS/ROUTE_PATTERNS.md](./PATTERNS/ROUTE_PATTERNS.md) | Routing architecture, lazy loading, protected routes |
| [PATTERNS/API_CLIENT_PATTERNS.md](./PATTERNS/API_CLIENT_PATTERNS.md) | Axios setup, JWT injection, API module organization |
| [PATTERNS/CSS_PATTERNS.md](./PATTERNS/CSS_PATTERNS.md) | CSS naming, glassmorphism tiers, responsive breakpoints, animations |

## Feature-Specific Docs

| Document | Description |
|----------|-------------|
| [PAGES.md](./PAGES.md) | Route overview and page component specifications |
| [COMPONENTS.md](./COMPONENTS.md) | React component library reference |
| [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md) | Zustand store architecture reference |
| [API_CLIENT.md](./API_CLIENT.md) | API client reference |

### Speaking Feature

| Document | Description |
|----------|-------------|
| [SPEAKING/SPEAKING_ARCHITECTURE.md](./SPEAKING/SPEAKING_ARCHITECTURE.md) | How Speaking fits into the frontend |
| [SPEAKING/SPEAKING_PAGES.md](./SPEAKING/SPEAKING_PAGES.md) | SpeakingSessionPage and SpeakingResultsPage specs |
| [SPEAKING/SPEAKING_COMPONENTS.md](./SPEAKING/SPEAKING_COMPONENTS.md) | SpeakingPartModal, PreBriefScreen, GeminiLiveSessionLayout, etc. |
| [SPEAKING/SPEAKING_STORE.md](./SPEAKING/SPEAKING_STORE.md) | useSpeakingStore specification |
| [SPEAKING/SPEAKING_API.md](./SPEAKING/SPEAKING_API.md) | speakingApi module and endpoint reference |
| [SPEAKING/SPEAKING_HOOKS.md](./SPEAKING/SPEAKING_HOOKS.md) | useGeminiLive, useAudioRecorder, useTimer specs |
| [SPEAKING/SPEAKING_CSS.md](./SPEAKING/SPEAKING_CSS.md) | CSS file structure and BEM prefix |

---

## How to Use These Docs

1. **New feature?** Start with `PATTERNS/` to understand conventions
2. **Need a component?** Check `COMPONENT_PATTERNS.md` first — it may already exist
3. **Building a page?** Follow `PAGE_PATTERNS.md` for structure, loading, error, empty states
4. **Adding state?** Follow `STORE_PATTERNS.md` for Zustand conventions
5. **Speaking-specific?** See `SPEAKING/` directory
