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
   - `docs/library/frontend/STATE_MANAGEMENT.md`
   - `docs/library/frontend/API_CLIENT.md`
2. Review implementation:
   - `frontend/src/stores/`
   - `frontend/src/api/`
   - `frontend/src/hooks/` and `frontend/src/contexts/` (if used)
3. Propose changes with a clear rationale.
4. Apply edits after approval.
5. Summarize changes and any follow-up tasks.

## Guardrails

- Prefer existing store and API client patterns over new abstractions.
- Avoid UI changes unless they are necessary for data flow.
