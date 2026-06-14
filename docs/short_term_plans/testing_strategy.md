# Testing Strategy

**Priority:** In Progress (Friend's Branch)
**Status:** Setup Phase
**Last Updated:** 2026-01-25
**Owner:** Friend (with handoff to main maintainer)

## Summary

Establish a testing foundation for Cramer with focus on critical paths. Friend is setting up testing infrastructure on a separate branch; main maintainer will continue after handoff.

## Test Types

- [ ] **Unit Tests** - Individual functions and services
- [ ] **E2E Tests** - Complete user flows
- *(Integration and performance tests not in initial scope)*

## Frameworks (TBD)

| Layer | Options to Consider |
|-------|---------------------|
| Backend (Spring Boot) | JUnit 5, Mockito, TestContainers |
| Frontend (React) | To be determined by friend |

## Critical Areas to Test

### Priority 1: Auth Flows
- User registration and login
- JWT token validation
- Session management
- Admin access control

### Priority 2: Payment/Credits
- Lúa balance operations
- Credit deduction atomicity
- Purchase flow
- Edge cases (insufficient balance, concurrent operations)

### Priority 3: Test Flows
- Test session creation
- Answer submission
- Scoring and results
- Part selection logic

## Coverage Approach

- **Goal:** Focus on critical paths (no specific % target)
- **Philosophy:** Better to have thorough tests on critical flows than shallow coverage everywhere

## Test Data

- Approach TBD by friend
- Options: fixtures, dynamic generation, or test DB with seeds

## CI/CD Integration

- TBD - friend will evaluate options
- Potential: run tests on PRs, main branch, or manual

## Documentation Needed

- [ ] **Contributor Guide** - How to write tests for Cramer
- [ ] **Local Run Guide** - How to run tests locally

## Collaboration Model

1. Friend sets up testing infrastructure
2. Friend creates initial test suite for critical areas
3. Handoff to main maintainer
4. Main maintainer continues test maintenance

## Open Questions (For Friend to Decide)

1. Backend testing framework choice?
2. Frontend testing framework choice?
3. CI/CD integration approach?
4. Test data management strategy?
5. Branch naming convention for test PRs?

## Notes

- Testing work is on a separate branch
- Coordinate with main branch before merging
- Consider mocking external services (Supabase, AI providers)

## Related Docs

- Friend's testing branch documentation (TBD)
- `docs/canonical/backend/SERVICES.md` - Services to test
