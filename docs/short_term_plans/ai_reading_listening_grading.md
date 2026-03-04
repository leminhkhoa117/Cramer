# AI Reading & Listening Grading

**Priority:** P1 High
**Status:** Planning
**Last Updated:** 2026-01-25

## Summary

Implement AI-powered grading for Reading and Listening tests. AI grades all answers but validates against answer keys. Mimics the comprehensive feedback approach used in Writing evaluation.

## Grading Approach

- **Method:** AI grades everything + validates against answer key
- **Question Types Covered:** All (objective, completion, short answer, matching)
- **Flexibility:**
  - Accept spelling variants (colour/color)
  - Accept synonyms when meaning preserved
  - *(Not included: typo tolerance, grammar variants)*

## Processing

- **Timing:** Batch grading at end of test (not real-time)
- **Architecture:** Shared core service with skill-specific extensions

## Feedback Features

### Reading
- [ ] Passage highlighting (show answer location)
- [ ] Context snippet (relevant paragraph/sentence)
- [ ] Vocabulary explanations for difficult words
- [ ] Comprehensive feedback (why wrong + correct answer + tips)

### Listening
- [ ] Transcript display alongside answers
- [ ] Comprehensive feedback

### Shared
- [ ] Error categorization (vocabulary, comprehension, inference)
- [ ] Pattern tracking across attempts
- [ ] Practice suggestions based on error types
- [ ] Estimated IELTS band score
- [ ] Detailed breakdown by question type

## Scoring Output

| Component | Description |
|-----------|-------------|
| Raw Score | e.g., 32/40 |
| Band Estimate | Converted IELTS band |
| Breakdown | Score per question type |
| Error Analysis | Categories + patterns |
| Suggestions | Targeted practice areas |

## MVP (Must-Have)

- [ ] AI grading with spelling/synonym tolerance
- [ ] Basic feedback (correct answer + explanation)
- [ ] Band score estimation
- [ ] Passage highlighting for Reading

## Post-MVP

- [ ] Vocabulary help
- [ ] Audio timestamps for Listening
- [ ] Progress tracking (compare to previous attempts)
- [ ] Advanced pattern analysis

## Technical Considerations

- Leverage existing Writing evaluation patterns
- Core grading service + Reading/Listening extensions
- Answer key validation as safety net
- Highlight coordinates need passage text indexing

## Open Questions

1. How to handle answers that AI accepts but key rejects (edge cases)?
2. Storage format for highlight coordinates in passages?
3. How to weight different question types in band calculation?

## Related Docs

- `docs/library/backend/SERVICES.md` - Existing grading services
- Writing evaluation implementation (reference for feedback style)
