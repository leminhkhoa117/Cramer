# AI Speaking Session

**Priority:** P0 Critical
**Status:** Planning
**Target Timeline:** ~2 months
**Last Updated:** 2026-01-25

## Summary

Full-fledged AI-assisted IELTS Speaking simulation with live audio conversation and AI-powered grading. Uses Gemini Live API for real-time turn-taking.

## Dependencies (Must Complete First)

- [ ] Admin Provider Control (for Gemini API configuration)
- [ ] Question Bank infrastructure (Speaking-specific schema)
- [ ] Lúa Credit system (all AI features require credits)

## Scope

- **Implementation:** Full (all 3 parts + live audio)
- **Session Modes:** Full test, Single part, Part 2+3
- **Audio Tech:** Gemini Live API only (no fallback to simpler options)
- **Examiner Voice:** Gemini native voice output

## Session Flow

1. **Entry:** Courses page (or system random selection)
2. **Pre-brief:** Consent + recording notice
3. **Mode selection:** Full / Single part / Part 2+3
4. **Topic planning:** Deterministic selection from question bank
5. **Live test:** Real-time audio with Gemini Live API
6. **Post-processing:** Transcript + scoring (batch at end)
7. **Results:** Full report with sample answers

## Question Bank

- **Population:** Build bank first (before implementing flow)
- **Schema:** As defined in `docs/ielts_specific/speaking_session_foundations.md`
- **Selection:** Deterministic from pre-approved bank per topic/part

## Evaluation

| Criterion | Method |
|-----------|--------|
| Fluency & Coherence | Gemini analysis |
| Lexical Resources | Gemini analysis |
| Grammar Range & Accuracy | Gemini analysis |
| Pronunciation | Gemini 3 Pro prompt-based |

- **Scoring Timing:** Batch at end of session
- **Sample Answers:** AI-generated post-session

## Failure Handling

- **Gemini Live failure:** Fail hard (show error, end session)
- **No text fallback** for MVP

## Data Storage

- Store both raw audio and transcripts
- Retention policy required (define duration)
- Audit trail for model versions and prompts used

## Lúa Credit Integration

- All AI-based Speaking features consume Lúa credits
- Cannot start session without sufficient balance
- Credit check before session, deduction after completion

## Technical Stack

| Component | Technology |
|-----------|------------|
| Live turn-taking | `gemini-live-2.5-flash-native-audio` |
| Transcript | `gemini-2.5-flash-lite` |
| Pronunciation | `gemini-3-pro` |
| Reasoning | DeepSeek Reasoner |

## Open Questions

1. Audio file storage location (Supabase Storage vs external)?
2. Retention policy duration for audio recordings?
3. Credit cost calculation (per session vs per minute)?
4. Handling timezone for session scheduling?

## Related Docs

- `docs/ielts_specific/speaking_session_foundations.md` - Full specification
- `docs/short_term_plans/admin_provider_control.md` - Provider dependency
- `docs/short_term_plans/lua_currency_management.md` - Credit dependency
