---
name: cramer-ui-ux-refinement
description: This skill should be used when the user asks to "improve UI", "refine UX", "polish design", "audit usability", "make screens consistent", or "review UI/UX" in the Cramer project.
allowed-tools:
  - Read
  - Grep
  - Glob
  - Edit
---

# Cramer UI/UX Refinement

## Purpose

Deliver consistent UX and UI improvements across user and admin screens.

## Workflow

1. Read docs first:
   - `docs/canonical/frontend/UI_DESIGN_SYSTEM.md`
   - `docs/canonical/frontend/COMPONENTS.md`
   - `docs/canonical/frontend/PAGES.md`
   - `docs/canonical/frontend/STATE_MANAGEMENT.md`
   - `docs/canonical/frontend/API_CLIENT.md`
   - `docs/userflow/` (if relevant)
2. Inspect relevant UI code:
   - `frontend/src/pages/` (user screens)
   - `frontend/src/admin/` (admin screens)
   - `frontend/src/components/`
   - `frontend/src/css/` and `frontend/src/styles.css`
3. Perform an audit, then present 2-3 options.
4. Recommend one option with rationale.
5. Ask for approval before editing.
6. Apply edits and summarize changes.

## Guardrails

- Do not change colors unless necessary.
- Keep admin and user UX consistent while respecting context differences.
