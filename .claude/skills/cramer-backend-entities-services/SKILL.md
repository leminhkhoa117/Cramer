---
name: cramer-backend-entities-services
description: This skill should be used when the user asks to "add an endpoint", "change a service", "update an entity", "refactor backend logic", or "adjust API behavior" in the Cramer project.
allowed-tools:
  - Read
  - Grep
  - Glob
  - Edit
---

# Cramer Backend Entities and Services

## Purpose

Align backend changes with documented entities, services, and API behavior.

## Workflow

1. Read docs first:
   - `docs/canonical/backend/ENTITIES.md`
   - `docs/canonical/backend/SERVICES.md`
   - `docs/canonical/backend/API_REFERENCE.md`
2. Locate code under `backend/src/main/java/com/cramer/<module>/` where `<module>` is one of: identity, catalog, assessment, writing, speaking, billing, engagement, admin, abts (plus shared `platform/`). Each module owns `web/` (controllers + `web/dto` records), `service/` (+ cross-module `Port` interfaces), `domain/` (Lombok JPA entities/enums), `repository/`, and some `config/`.
3. Propose changes with a clear rationale.
4. Edit code and update docs where needed.
5. Summarize updates and any follow-up tasks.

## Guardrails

- Keep docs consistent with implementation.
- Avoid speculative changes without a concrete user request.
