# Admin Provider & Cost Control

**Priority:** P1 High
**Status:** Planning
**Last Updated:** 2026-01-25

## Summary

Upgrade Cramer's admin dashboard to provide control over AI providers and cost breakdowns. Admins can configure which models/providers are used for AI grading and test generation separately.

## Scope

- **Coverage:** Both AI grading and test generation, with **separate control panels**
- **Granularity:** Global settings (one config applies to all skills)
- **Providers:** DeepSeek, Trollllm, OpenRouter, Google (Vertex, Gemini Live API)

## Features

### MVP (Must-Have)
- [ ] Provider toggle (enable/disable per provider)
- [ ] Model selection via dynamic fetch from provider APIs
- [ ] API key management (secure storage in admin panel)

### Post-MVP
- [ ] Cost breakdown by task type (grading vs generation)
- [ ] Budget auto-pause when threshold exceeded
- [ ] Rate limiting per provider
- [ ] Health checks for provider connectivity
- [ ] Request logs (which provider/model used)
- [ ] Token usage tracking
- [ ] Latency metrics per provider

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Scope separation | Both grading + generation, separate panels | Clear separation of concerns |
| Granularity | Global only | Simplicity for MVP |
| Model selection | Dynamic fetch | Always up-to-date model lists |
| Fallback behavior | No fallback (show error) | Explicit control, no surprise costs |
| Budget control | Auto-pause | Prevent runaway costs |
| Access control | All admins | No role complexity for now |

## UI/UX

- **Location:** New dedicated page in admin dashboard
- **Access:** All admins can view and modify

## Audit & Logging

- Request logs (provider/model per request)
- Token usage per request
- Latency metrics per provider
- *(Config change history - post-MVP)*

## Technical Considerations

- Secure API key storage (encrypted in database or environment)
- Provider API integration for model list fetching
- Budget tracking requires token/cost logging per request
- Auto-pause needs graceful degradation messaging to users

## Open Questions

1. How to handle in-flight requests when auto-pause triggers?
2. Should cost estimates be shown before grading requests?
3. Notification method when budget threshold approached?

## Related Docs

- `docs/canonical/backend/SERVICES.md` - Current AI service implementations
- `docs/specs/backend/20-ai-generation/` - ABTS generation specs (SPEC-20…25)
