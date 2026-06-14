---
name: cramer-frontend-state-data
description: This skill should be used when the user asks about "Zustand", "state management", "stores", "data fetching", "API client", or "frontend data flow" in the Cramer project.
allowed-tools:
  - Read
  - Grep
  - Glob
  - Edit
---

# Cramer Frontend State and Data

## Purpose

Keep frontend state and data flows consistent with the project’s Zustand and API client patterns.

## Workflow

1. Read docs first:
   - `docs/canonical/frontend/STATE_MANAGEMENT.md`
   - `docs/canonical/frontend/API_CLIENT.md`
2. Review implementation:
   - `frontend/src/stores/` — Zustand stores
   - `frontend/src/api/backendApi.js` — Axios client
   - `frontend/src/hooks/` and `frontend/src/contexts/`
3. Understand CSS import patterns (stores may need CSS context):
   - `frontend/src/css/tokens.css` — ALL design variables (single source of truth)
   - `frontend/src/styles.css` — global entry (imports tokens + Tailwind + animations)
4. Propose changes with a clear rationale.
5. Apply edits after approval.
6. Summarize changes and any follow-up tasks.

## Guardrails

- Prefer existing store and API client patterns over new abstractions.
- Avoid UI changes unless they are necessary for data flow.
- If adding styles to a component, CSS goes in `css/components/` (not pages, not shared).
- Use `var(--cr-*)` tokens from `tokens.css` instead of hardcoded colors.
