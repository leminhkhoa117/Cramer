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
   - `docs/library/frontend/UI_DESIGN_SYSTEM.md` — design tokens, naming conventions, layout patterns
   - `docs/library/frontend/COMPONENTS.md` (if exists)
   - `docs/library/frontend/STATE_MANAGEMENT.md`
   - `docs/userflow/` (if relevant)
2. Understand CSS structure (critical before any change):
   - `frontend/src/css/tokens.css` — ALL design tokens (single source of truth)
   - `frontend/src/css/shared/layout.css` — sl-* sidebar system
   - `frontend/src/css/shared/animations.css` — ALL @keyframes
   - `frontend/src/css/test/` — test-taking UI (8 files, do NOT rename class selectors)
   - `frontend/src/css/pages/` — page-specific overrides
   - `frontend/src/css/components/` — per-component CSS
   - `frontend/src/css/common/` — modal, faq, testimonials, grading-loader
   - `frontend/src/admin/css/tokens.css` — admin dark-theme tokens (separate system)
3. Inspect relevant UI code:
   - `frontend/src/pages/` (user screens)
   - `frontend/src/admin/` (admin screens)
   - `frontend/src/components/`
   - `frontend/src/css/` and `frontend/src/styles.css`
4. Perform an audit, present 2-3 options.
5. Recommend one option with rationale.
6. Ask for approval before editing.
7. Apply edits and summarize changes.

## Guardrails

- NEVER rename a CSS class selector unless you update ALL JSX references
- Do not change colors unless necessary — use `var(--cr-*)` tokens
- Keep admin and user UX consistent while respecting context differences
- Test UI (under `css/test/`) is fragile — class name changes WILL break the test-taking experience
- All `@keyframes` live in `css/shared/animations.css` — add new ones there
