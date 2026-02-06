# Short-Term Development Plans

This folder contains planning documents for Cramer's upcoming features and improvements. Each doc was created through iterative questioning to ensure alignment with project goals.

**Created:** 2026-01-25

## Priority Overview

| Priority | Plan | Status |
|----------|------|--------|
| P0 Critical | [AI Speaking Session](ai_speaking_session.md) | Planning |
| P0 Critical | [Skill Part Selection](skill_part_selection.md) | Planning |
| P1 High | [Admin Provider Control](admin_provider_control.md) | Planning |
| P1 High | [AI Reading/Listening Grading](ai_reading_listening_grading.md) | Planning |
| P2 Medium | [Lúa Currency Management](lua_currency_management.md) | Planning |
| P3 Low | [TOEIC Support](toeic_support.md) | Planning |
| In Progress | [Testing Strategy](testing_strategy.md) | Friend's Branch |

## Dependency Graph

```
                    ┌─────────────────────┐
                    │  Admin Provider     │
                    │  Control (P1)       │
                    └─────────┬───────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐   ┌─────────────────┐   ┌─────────────────┐
│ AI Speaking   │   │ AI R/L Grading  │   │ Lúa Currency    │
│ Session (P0)  │   │ (P1)            │   │ Management (P2) │
└───────┬───────┘   └─────────────────┘   └────────┬────────┘
        │                                          │
        └──────────────────┬───────────────────────┘
                           │
                           ▼
                 ┌─────────────────┐
                 │ Skill Part      │
                 │ Selection (P0)  │
                 └─────────────────┘
                           │
                           ▼
                 ┌─────────────────┐
                 │ TOEIC Support   │
                 │ (P3 - Future)   │
                 └─────────────────┘
```

## Quick Reference

### Lúa Pricing Summary

| Skill | Full Test | Per Part |
|-------|-----------|----------|
| Reading | 10 Lúa | 4 Lúa |
| Listening | 10 Lúa | 4 Lúa |
| Writing | 10 Lúa | 4 Lúa |
| Speaking | 30 Lúa | 10 Lúa/selection |

### Purchase Packages

| Package | Name | Amount | Price |
|---------|------|--------|-------|
| Small | Gói Lúa | 50 Lúa | 15,000 VNĐ |
| Medium | Bao Lúa | 200 Lúa | 60,000 VNĐ |
| Large | Xe Lúa | 1,000 Lúa | 250,000 VNĐ |

## Notes

- All AI-based features require Lúa credits
- Speaking session has 3 dependencies: Provider Control, Question Bank, Lúa Credits
- Testing is being handled separately by a friend on a different branch
