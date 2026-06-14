---
name: cramer-supabase-ops
description: This skill should be used when the user asks to "run a Supabase query", "insert records", "update rows", "bulk update", "delete rows", "truncate", "change schema", "fix data", or "verify database data" in the Cramer project.
allowed-tools:
  - Read
  - Grep
  - Glob
  - Edit
  - mcp__supabase__*
---

# Cramer Supabase Ops

## Purpose

Enable safe, fast data operations in Supabase while keeping changes aligned with Cramer backend documentation.

## Workflow

1. Read docs first:
   - `docs/canonical/backend/DATABASE_SCHEMA.md`
   - `docs/canonical/backend/ENTITIES.md`
   - `docs/canonical/backend/SERVICES.md`
   - `docs/canonical/backend/API_REFERENCE.md`
2. Verify schema live using MCP `supabase` before writing.
3. Prefer direct table operations for routine edits.
4. For any destructive operation, ask for explicit confirmation before executing:
   - Deletes
   - Truncations
   - Schema changes
   - Bulk updates (affecting many rows)
5. When a write is needed, show the intended change, the target table(s), and the criteria.
6. After changes, summarize what was modified and note any follow-up updates needed in docs.

## Guardrails

- Always ask before destructive operations (delete/truncate/schema/bulk updates).
- If the docs and the live schema disagree, pause and report the mismatch before writing.
- When unsure, run a read-only query first to validate scope.

## Project Structure Notes

- Backend code lives in `backend/src/main/java/com/cramer/...`.
- Keep data operations consistent with documented entities and services.
