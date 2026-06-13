# Cramer Backend — Rewrite Specs

> **Source of truth** for the backend rewrite (feature-based vertical slices, Spring Boot 4 /
> Java 25). Code that disagrees with these specs is wrong code (SPEC-00). The live Supabase
> schema is **frozen** — new domain types map onto existing tables.

Branch: `rewrite/backend-vertical-slice` · Backup: commit `2f0ed64` on `dev/experiment-refactor`.

## Foundation (`00-foundation/`)
| Spec | Topic |
|------|-------|
| [SPEC-00](00-foundation/SPEC-00-philosophy-and-workflow.md) | Philosophy, specs-as-truth, per-slice workflow, fix-list |
| [SPEC-01](00-foundation/SPEC-01-architecture.md) | Vertical slices, slice internals, dependency/port rules |
| [SPEC-02](00-foundation/SPEC-02-naming-conventions.md) | Role-suffix naming, old→new renames |
| [SPEC-03](00-foundation/SPEC-03-package-structure.md) | Full `com.cramer.*` target tree |
| [SPEC-04](00-foundation/SPEC-04-cross-cutting.md) | Auth, error model, response conventions, port catalog |
| [SPEC-05](00-foundation/SPEC-05-tech-stack.md) | Boot 4 / Java 25, dependencies, build, testing |

## Modules (`10-modules/`)
| Spec | Module |
|------|--------|
| [SPEC-10](10-modules/SPEC-10-identity.md) | identity — auth, profile |
| [SPEC-11](10-modules/SPEC-11-catalog.md) | catalog — content hierarchy, delivery, admin, browse |
| [SPEC-12](10-modules/SPEC-12-assessment.md) | assessment — attempts, scoring, review |
| [SPEC-13](10-modules/SPEC-13-writing.md) | writing — submission + AI grading |
| [SPEC-14](10-modules/SPEC-14-speaking.md) | speaking — sessions, realtime, grading |
| [SPEC-15](10-modules/SPEC-15-billing.md) | billing — subscription, Lúa, quota, payment, gating |
| [SPEC-16](10-modules/SPEC-16-engagement.md) | engagement — chat, vocabulary, dashboard, activity |
| [SPEC-17](10-modules/SPEC-17-admin.md) | admin — users, audit, dashboard, finance |
| [SPEC-18](10-modules/SPEC-18-platform.md) | platform — shared kernel |

## AI generation (`20-ai-generation/`) — brand-new functional spec
| Spec | Topic |
|------|-------|
| [SPEC-20](20-ai-generation/SPEC-20-abts-overview.md) | Overview & IELTS domain model |
| [SPEC-21](20-ai-generation/SPEC-21-abts-generation.md) | Generation & streaming |
| [SPEC-22](20-ai-generation/SPEC-22-abts-prompting-schema.md) | Prompting & JSON schema |
| [SPEC-23](20-ai-generation/SPEC-23-abts-validation-refinement.md) | Validation & refinement |
| [SPEC-24](20-ai-generation/SPEC-24-abts-models-persistence.md) | Models & persistence |
| [SPEC-25](20-ai-generation/SPEC-25-abts-api-ux.md) | HTTP/SSE API & Admin UX |

## How to use
1. Read SPEC-00 → SPEC-04 once. They govern everything.
2. For a module, read its `10-modules` spec + any ports it consumes (SPEC-04 §4).
3. Write tests from the spec, implement, validate (SPEC-00 §7).
4. If reality forces a deviation, **edit the spec first**, note it in that spec's Change log.

## Implementation status
- Specs: **complete** (this set).
- Code: in progress on `rewrite/backend-vertical-slice` (platform kernel + proof slice first).
