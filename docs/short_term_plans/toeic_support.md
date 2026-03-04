# TOEIC Test Support

**Priority:** P3 Low
**Status:** Planning (Future Expansion)
**Last Updated:** 2026-01-25

## Summary

Expand Cramer to support TOEIC Listening & Reading tests using the same infrastructure as IELTS. Maximum reuse of existing schema and UI.

## Scope

- **Test Format:** L&R Test first (standard 2-hour test)
- **S&W Test:** Not in initial scope
- **MVP:** Full L&R support (all parts)

## Test Structure

### Listening (45 min, 100 questions)

| Part | Name | Questions | Description |
|------|------|-----------|-------------|
| 1 | Photographs | 6 | Look at photo, choose best description |
| 2 | Question-Response | 25 | Hear question, choose best response |
| 3 | Conversations | 39 | Listen to conversations, answer questions |
| 4 | Talks | 30 | Listen to talks, answer questions |

### Reading (75 min, 100 questions)

| Part | Name | Questions | Description |
|------|------|-----------|-------------|
| 5 | Incomplete Sentences | 30 | Fill in the blank |
| 6 | Text Completion | 16 | Complete passages |
| 7 | Reading Comprehension | 54 | Single + multiple passages |

## Infrastructure Approach

- **Schema:** Same tables with `test_type` flag (IELTS/TOEIC)
- **UI:** Unified interface with test type toggle
- **Audio:** TBD after researching TOEIC audio requirements
- **ABTS:** Research TOEIC format first, then create prompts

## Content Creation

- **Hybrid approach:**
  - AI generation (after TOEIC-specific prompts created)
  - Import existing TOEIC materials
  - Manual admin creation

## Scoring

- Raw score (correct answers out of 200)
- Scaled TOEIC score (10-990 range)
- Both displayed to user

## Pricing

- Same Lúa pricing as IELTS equivalents
- Full test: 10 Lúa
- Per section: 4 Lúa

## Technical Considerations

- Add `test_type` enum to test-related tables
- Question types need TOEIC-specific variants
- Audio timing differs from IELTS (research needed)
- Scaled score conversion table required

## Pre-Implementation Research Needed

1. TOEIC audio timing and replay rules
2. Exact question type formats for all parts
3. Official scaled score conversion methodology
4. TOEIC-specific ABTS prompt requirements

## Open Questions

1. Should TOEIC have its own branded landing page?
2. How to handle TOEIC's different timing (no per-part limits)?
3. TOEIC allows going back - how does this affect our test flow?

## Related Docs

- `docs/library/backend/DATABASE_SCHEMA.md` - Schema extension
- `docs/CRAMER_ABTS_SPECS.md` - ABTS prompt patterns
