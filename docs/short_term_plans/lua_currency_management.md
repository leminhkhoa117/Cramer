# Lúa Currency Management

**Priority:** P2 Medium
**Status:** Planning (Improvement to existing system)
**Last Updated:** 2026-01-25

## Summary

Improve the existing Lúa credit system with better admin controls, purchase flow, balance UX, security, and reliability.

## Current State

- Basic credit system exists
- Floating bar showing Lúa balance already implemented
- Needs improvements in admin controls, purchase flow, and security

## Pricing Structure

### Per-Skill Costs

| Skill | Full Test | Per Part |
|-------|-----------|----------|
| Reading | 10 Lúa | 4 Lúa |
| Listening | 10 Lúa | 4 Lúa |
| Writing | 10 Lúa | 4 Lúa |
| Speaking | 30 Lúa | 10 Lúa per selection |

**Speaking Options:**
- Part 1: 10 Lúa
- Part 2: 10 Lúa
- Part 3: 10 Lúa
- Part 2+3: 10 Lúa
- Full Test: 30 Lúa

*(Note: Reevaluate Speaking costs based on actual AI costs)*

### Purchase Packages

| Package | Vietnamese Name | Lúa Amount | Price (VNĐ) |
|---------|-----------------|------------|-------------|
| Small | Gói Lúa | 50 Lúa | 15,000 |
| Medium | Bao Lúa | 200 Lúa | 60,000 |
| Large | Xe Lúa | 1,000 Lúa | 250,000 |

### Bonuses

- [ ] Volume bonus on larger purchases
- [ ] Promotional codes

## Security Requirements

- [ ] **Server-side validation** - Never trust client-side balance
- [ ] **Rate limiting** on credit operations
- [ ] Transaction logging for all credit changes

## Low Balance Handling

- [ ] Warning when balance drops below threshold
- [ ] Block feature if insufficient balance
- [ ] Show cost before each AI action
- [ ] Prompt to purchase when balance is low

## Admin Controls

- [ ] Set/adjust pricing per feature and part
- [ ] Create and manage promotional codes
- [ ] View user credit transactions
- [ ] Manually grant or deduct credits

## Technical Considerations

- Atomic transactions for credit deductions
- Balance check → Action → Deduct (in transaction)
- Audit log for all credit operations
- Admin audit log for manual adjustments

## Open Questions

1. Should credits expire? If so, after how long?
2. Refund policy for failed AI operations?
3. Should there be a minimum purchase amount?
4. How to handle promo code abuse?

## Related Docs

- `docs/canonical/backend/DATABASE_SCHEMA.md` - user_credits table
- `docs/short_term_plans/skill_part_selection.md` - Per-part pricing integration
- `docs/short_term_plans/ai_speaking_session.md` - Speaking cost integration
