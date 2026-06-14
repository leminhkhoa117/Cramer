# Skill Part Selection

**Priority:** P0 Critical
**Status:** Planning
**Last Updated:** 2026-01-25

## Summary

Allow users to choose specific parts of any skill (Reading, Listening, Writing, Speaking) to take instead of requiring full tests. Enables focused practice on weak areas.

## Scope

| Skill | Parts Available |
|-------|-----------------|
| Reading | Part 1, Part 2, Part 3 |
| Listening | Part 1, Part 2, Part 3, Part 4 |
| Writing | Task 1, Task 2 |
| Speaking | Part 1, Part 2, Part 3 |

## Selection Options

- **Full Test** - All parts
- **Single Part** - Any one part
- **Random** - System picks (Speaking only)

## UX Flow

1. User clicks on a skill to enter test
2. **Modal popup appears** with part selection
3. Popup displays:
   - [ ] Lúa credit cost for selected parts
   - [ ] Estimated time for selected parts
   - [ ] Note about partial band scoring
   - [ ] Warning: cannot change selection mid-test
4. User confirms selection
5. Test begins with selected parts only

## Timing

- **Approach:** Sum of selected parts (fixed time per part)
- Timer adjusts to total of selected parts only

## Scoring

- Partial score on parts taken
- Band estimate provided even for partial tests
- AI evaluation tailored to the specific parts taken

## Credit Pricing

- **Model:** Per-part pricing
- Cost = sum of individual part costs
- *(Consider: full test bundle discount in future)*

## Progress Tracking

- Track both full and partial attempts
- Per-part completion status
- History shows which parts were taken

## Technical Considerations

- Test session needs `selected_parts` field
- Timer calculation based on part selection
- Credit calculation service needs part-level pricing
- Results display adapts to partial tests

## Open Questions

1. Should users be able to save "favorite" part combinations?
2. Minimum credit balance check - per part or total?
3. How to handle mid-test abandonment for partial tests?

## Related Docs

- `docs/short_term_plans/lua_currency_management.md` - Credit pricing
- `docs/canonical/frontend/PAGES.md` - Test entry UX
