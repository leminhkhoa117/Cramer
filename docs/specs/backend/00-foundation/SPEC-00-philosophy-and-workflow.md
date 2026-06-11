# SPEC-00 — Philosophy & Workflow

> Status: **Authoritative** · Scope: whole backend rewrite · Owner: Both
> These specs are the **source of truth**. Code that disagrees with a spec is wrong code.

---

## 1. Why this document set exists

The Cramer backend grew organically into ~327 files / ~43k LOC with overlapping
services, two content-identity systems, two admin content APIs, ~90 flat DTOs, and
several money-correctness bugs. The rewrite restructures the backend into **feature-based
vertical slices** with consistent naming, granular files, and a single source of truth for
behavior: **these specs**.

## 2. The golden rule

1. **Spec is truth.** If code does something the spec does not describe, the code is wrong
   and must be changed to match the spec.
2. **If the spec is wrong, fix the spec first**, then the code. Never silently diverge.
3. **Behavior changes require a spec edit in the same change.** No "temporary" undocumented
   behavior.

## 3. Specs-driven workflow (per slice)

For every module/feature we follow this loop:

1. **Read the spec** for the module (`10-modules/SPEC-1x-*.md`) plus the foundation specs.
2. **Write/adjust tests** that encode the spec's observable behavior (API contract, rules,
   edge cases).
3. **Implement** the slice to satisfy the spec and tests.
4. **Validate**: module tests green, then full build green.
5. **Reconcile**: if reality forces a deviation, update the spec, note it in the
   `Change log` section of that spec, and re-run.

## 4. Granularity rules (files & specs)

- **Specs**: one concern per file. A module spec may split into sub-files when it exceeds
  ~400 lines (e.g. `SPEC-14-speaking.md` → `SPEC-14a-speaking-session.md`, `…-grading.md`).
- **Code**: one public type per file. Target ≤ ~250 lines per class; extract collaborators
  when larger. No "god services".
- Prefer **many small, well-named files** over few large ones. Navigation > brevity.

## 5. Non-negotiable constraints

- **Database is live Supabase.** The rewrite **must not** change the database schema. Map
  the new domain types onto the **existing tables/columns** documented in
  `docs/canonical/backend/DATABASE_SCHEMA.md`. Schema changes, if ever needed, are a
  separate, explicitly-approved migration effort.
- **Tech stack is fixed for this pass**: Spring Boot 4.0, Java 25 (see `SPEC-05`).
- **API contracts may be redesigned** (frontend is rewritten later). We optimize for a clean,
  consistent HTTP surface, not backward compatibility — but every endpoint's *capability*
  must be preserved unless a spec explicitly drops it.

## 6. What we deliberately fix (carried into module specs)

The rewrite corrects known defects. Each is restated as a rule in the relevant module spec:

- Answer scoring supports true multi-select set comparison (SPEC-12).
- Answer keys/explanations are never exposed outside review/admin surfaces (SPEC-11/12).
- One canonical AI-grading overage price (SPEC-15).
- Lúa packs are DB-driven everywhere; no hardcoded pack tables (SPEC-15).
- Credit mutations are idempotent by reference; quota check+increment is atomic; payment
  webhooks are idempotent under concurrency (SPEC-15).
- Usage-based features (chat, translation) charge **after** a successful AI call, or refund
  on failure (SPEC-15/16).
- Speaking admin regrade actually re-grades (state machine fix, SPEC-14).
- Speaking grading result is one schema-driven contract, not 24 fragmented DTOs (SPEC-14).
- Content identity is FK-first (`test_id`); legacy `exam_source/test_number` is a lookup
  shim only (SPEC-11/12).
- One typed admin content API; the raw-SQL CMS path is removed (SPEC-11/17).
- Dead/dev-only code is dropped; a real health endpoint replaces ad-hoc debug controllers
  (SPEC-18).

## 7. Definition of done (per module)

- Module spec exists and is current.
- All endpoints in the spec implemented with the documented contract.
- Unit + slice tests cover happy path, validation, authz, and listed edge cases.
- `./mvnw test` green for the module; full build green.
- No references to the old package tree remain for that module.

## 8. Change log

| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
