---
name: cramer-db-schema-truth
description: This skill should be used when the user asks to "verify schema", "check tables/columns", "audit the database", "update schema docs", or "find schema mismatches" in the Cramer project.
allowed-tools:
  - Read
  - Grep
  - Glob
  - Edit
  - mcp__supabase__*
---

# Cramer DB Schema Truth

## Purpose

Keep the documented schema and the live Supabase schema in sync.

## Workflow

1. Read docs first:
   - `docs/library/backend/DATABASE_SCHEMA.md`
   - `docs/library/backend/ENTITIES.md`
   - `docs/library/backend/API_REFERENCE.md`
2. Query the live schema via MCP `supabase`.
3. Compare docs vs live schema and list mismatches clearly.
4. Recommend a resolution path:
   - Update docs if the live schema is correct.
   - Update schema if docs are the source of truth.
5. Ask for confirmation before any destructive schema change.
6. Apply updates, then summarize what changed and why.

## Guardrails

- Always read docs before querying or changing schema.
- Never apply destructive schema changes without explicit user approval.
