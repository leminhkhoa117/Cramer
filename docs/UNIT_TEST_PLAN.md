# Unit Test Plan - Cramer Project

> **Status:** Project hiện chưa có bất kỳ unit test nào
> **Last Updated:** 2025-12-18

## 📋 Tổng Quan

Project Cramer gồm 2 phần chính:
- **Backend:** Spring Boot (Java 21) với 18 controllers, ~25 services
- **Frontend:** React + Vite với Zustand stores, ~60+ components
## 🎯 Mục Tiêu Của Test Script

### Mục Tiêu Chính

1. **🛡️ Đảm Bảo Chất Lượng Code**
   - Phát hiện bugs sớm trước khi deploy production
   - Ngăn chặn regressions khi refactor hoặc thêm features mới
   - Đảm bảo code hoạt động đúng theo specifications

2. **🔒 Bảo Mật & An Toàn**
   - Test các lỗ hổng bảo mật (XSS, IDOR, SQL Injection)
   - Verify JWT authentication & authorization
   - Kiểm tra RLS (Row Level Security) policies
   - Validate input sanitization

3. **🚀 Tăng Tốc Development**
   - Confidence khi refactor code mà không lo break features
   - Tự động hóa regression testing
   - Documentation thông qua test cases
   - Onboarding nhanh cho developers mới

4. **💰 Giảm Chi Phí Maintenance**
   - Phát hiện bugs sớm = chi phí fix thấp
   - Giảm thời gian debug trong production
   - Giảm technical debt

5. **📊 Code Quality Metrics**
   - Đo lường test coverage (target: 75%+)
   - Identify untested code paths
   - Track code quality trends over time

6. **🔄 CI/CD Integration**
   - Automated testing trong GitHub Actions
   - Prevent merging code với failing tests
   - Quality gates trước khi deploy

### Mục Tiêu Cụ Thể Theo Layers

#### Backend Tests
- ✅ **Service Layer:** Verify business logic correctness (80% coverage)
- ✅ **Controller Layer:** Test API contracts, auth, validation (70% coverage)
- ✅ **Repository Layer:** Test custom queries, data integrity (60% coverage)
- ✅ **Security:** Verify authentication/authorization cho mọi endpoint
- ✅ **Integration:** Test end-to-end flows (test attempts, writing submission)

#### Frontend Tests
- ✅ **Stores (Zustand):** Test state management logic (80% coverage)
- ✅ **Components:** Test user interactions, rendering, edge cases (60% coverage)
- ✅ **Utils:** Test helper functions, sanitization (90% coverage)
- ✅ **API Client:** Test API calls, error handling, retries

### Success Criteria

| Tiêu Chí | Mục Tiêu | Ý Nghĩa |
|----------|----------|---------|
| **Test Coverage** | 75%+ overall | Đa số code được test |
| **Critical Path Coverage** | 90%+ | Core features 100% reliable |
| **Test Execution Time** | <2 phút | Fast feedback loop |
| **Flaky Tests** | 0 | Tests luôn deterministic |
| **Build Pass Rate** | 95%+ | CI/CD stability |
| **Bug Detection Rate** | 70%+ bugs caught in tests | Prevent production bugs |

### Không Mục Tiêu (Out of Scope)

- ❌ E2E tests với browser automation (Playwright/Cypress) - separate plan
- ❌ Load/Performance testing - separate tools
- ❌ Manual testing - không thay thế QA manual
- ❌ 100% coverage - không practical và expensive

---
## 🎯 Mục Tiêu Test Coverage

| Category | Target Coverage | Priority |
|----------|----------------|----------|
| Backend Services | 80%+ | 🔴 Critical |
| Backend Controllers | 70%+ | 🟡 High |
| Backend Repositories | 60%+ | 🟢 Medium |
| Frontend Stores (Zustand) | 80%+ | 🔴 Critical |
| Frontend Components | 60%+ | 🟡 High |
| Frontend Utils | 90%+ | 🔴 Critical |

---

## 🔧 Backend Test Setup

### Dependencies Cần Thêm vào `pom.xml`

```xml
<!-- Test Dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>org.junit.vintage</groupId>
            <artifactId>junit-vintage-engine</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Mockito for mocking -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Security Test -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- H2 Database for testing -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>

<!-- AssertJ for fluent assertions -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers for integration tests (optional) -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

### Test Structure

```
backend/src/test/java/com/cramer/
├── controller/          # Controller layer tests (MockMvc)
│   ├── ProfileControllerTest.java
│   ├── TestAttemptControllerTest.java
│   ├── WritingControllerTest.java
│   └── ... (18 controllers)
├── service/            # Service layer tests (Unit + Integration)
│   ├── ProfileServiceTest.java
│   ├── TestAttemptServiceTest.java
│   ├── WritingSubmissionServiceTest.java
│   ├── LLMGradingServiceTest.java
│   ├── SubscriptionServiceTest.java
│   └── ... (~25 services)
├── repository/         # Repository tests (DataJpaTest)
│   ├── ProfileRepositoryTest.java
│   ├── TestAttemptRepositoryTest.java
│   └── ...
├── util/              # Utility tests
│   └── JwtUtilTest.java
├── config/            # Configuration tests
│   └── SecurityConfigTest.java
└── integration/       # End-to-end integration tests
    ├── TestAttemptFlowIntegrationTest.java
    └── WritingSubmissionFlowIntegrationTest.java
```

---

## 🔥 Backend: Priority 1 Tests (Critical Path)

### 1.1 Service Layer Tests

#### `TestAttemptServiceTest.java` 🔴 HIGHEST PRIORITY
Test toàn bộ flow: start → progress → submit → cancel

```java
@ExtendWith(MockitoExtension.class)
class TestAttemptServiceTest {
    @Mock private TestAttemptRepository testAttemptRepository;
    @Mock private UserAnswerRepository userAnswerRepository;
    @Mock private QuestionService questionService;
    @InjectMocks private TestAttemptService testAttemptService;
    
    @Test
    void startOrGetAttempt_ShouldCreateNewAttempt_WhenNoExistingAttempt() {
        // Given: No existing attempt
        when(testAttemptRepository.findLatest(...)).thenReturn(Optional.empty());
        
        // When: User starts test
        TestAttemptDTO result = testAttemptService.startOrGetAttempt(...);
        
        // Then: New attempt created with IN_PROGRESS status
        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
        verify(testAttemptRepository).save(any());
    }
    
    @Test
    void startOrGetAttempt_ShouldReturnExisting_WhenInProgressExists() {
        // Test resume logic
    }
    
    @Test
    void startOrGetAttempt_ShouldShowModal_WhenCompletedExists() {
        // Test forceNew=false with COMPLETED attempt
    }
    
    @Test
    void submitTestAttempt_ShouldCalculateCorrectScore() {
        // Test scoring logic
    }
    
    @Test
    void cancelTestAttempt_ShouldDeleteAttemptAndAnswers() {
        // Test cancellation with RLS
    }
}
```

#### `WritingSubmissionServiceTest.java` 🔴
```java
@ExtendWith(MockitoExtension.class)
class WritingSubmissionServiceTest {
    @Mock private WritingSubmissionRepository repository;
    @Mock private AsyncGradingService asyncGradingService;
    @InjectMocks private WritingSubmissionService service;
    
    @Test
    void submitWriting_ShouldSaveSubmission_AndTriggerAsyncGrading() {
        // Test async grading trigger
    }
    
    @Test
    void getReview_ShouldReturnGradedResult_WhenGradingComplete() {
        // Test review retrieval
    }
}
```

#### `LLMGradingServiceTest.java` 🔴
```java
class LLMGradingServiceTest {
    @Test
    void buildUserPrompt_ShouldIncludeImageDescription_ForTask1() {
        // Test prompt building with image descriptions
    }
    
    @Test
    void gradeEssay_ShouldHandleDeepSeekApiResponse() {
        // Mock DeepSeek API response
    }
    
    @Test
    void parseGradingResult_ShouldExtractBandScores() {
        // Test JSON parsing
    }
}
```

#### `SubscriptionServiceTest.java` 🟡
```java
class SubscriptionServiceTest {
    @Test
    void checkGradingAvailability_ShouldReturnTrue_WhenQuotaAvailable() {}
    
    @Test
    void incrementAttemptUsage_ShouldDeductQuota() {}
    
    @Test
    void checkGradingAvailability_ShouldReturnFalse_WhenQuotaExceeded() {}
}
```

#### `ProfileServiceTest.java` 🟢
```java
class ProfileServiceTest {
    @Test
    void getProfile_ShouldThrowException_WhenUserNotOwner() {
        // Test IDOR protection
    }
    
    @Test
    void updateProfile_ShouldSanitizeInput() {
        // Test XSS prevention
    }
}
```

### 1.2 Controller Layer Tests

#### `TestAttemptControllerTest.java` 🔴
```java
@WebMvcTest(TestAttemptController.class)
@Import(SecurityConfig.class)
class TestAttemptControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TestAttemptService testAttemptService;
    
    @Test
    @WithMockUser(username = "user123")
    void startTestAttempt_ShouldReturn201_WithValidRequest() throws Exception {
        mockMvc.perform(post("/api/test-attempts/start")
                .param("userId", "user123")
                .param("examSource", "Cambridge 17")
                .param("testNumber", "1")
                .param("skill", "reading"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
    
    @Test
    void startTestAttempt_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/test-attempts/start"))
            .andExpect(status().isUnauthorized());
    }
}
```

#### `WritingControllerTest.java` 🔴
#### `ProfileControllerTest.java` 🟡
#### `SubscriptionControllerTest.java` 🟡

### 1.3 Repository Layer Tests

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class TestAttemptRepositoryTest {
    @Autowired private TestAttemptRepository repository;
    @Autowired private TestEntityManager entityManager;
    
    @Test
    void findLatestByUserIdAndExamSourceAndTestNumberAndSkill_ShouldReturnMostRecent() {
        // Test custom query
    }
    
    @Test
    void deleteByIdAndUserId_ShouldDeleteOnlyOwnedAttempts() {
        // Test RLS-like behavior in code
    }
}
```

---

## ⚛️ Frontend Test Setup

### Dependencies Cần Thêm vào `package.json`

```json
{
  "devDependencies": {
    "vitest": "^2.0.5",
    "@testing-library/react": "^16.2.0",
    "@testing-library/jest-dom": "^6.6.3",
    "@testing-library/user-event": "^14.5.2",
    "jsdom": "^25.0.1",
    "@vitest/ui": "^2.0.5",
    "msw": "^2.6.6"
  },
  "scripts": {
    "test": "vitest",
    "test:ui": "vitest --ui",
    "test:coverage": "vitest --coverage"
  }
}
```

### Test Configuration Files Cần Tạo

#### `frontend/vitest.config.js`
```js
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: ['node_modules/', 'src/test/']
    }
  }
})
```

#### `frontend/src/test/setup.js`
```js
import '@testing-library/jest-dom'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(() => {
  cleanup()
})
```

### Test Structure

```
frontend/src/
├── stores/
│   ├── __tests__/
│   │   ├── useAuthStore.test.js
│   │   ├── useTestStore.test.js
│   │   ├── useProfileStore.test.js
│   │   ├── useDashboardStore.test.js
│   │   └── ... (11 stores)
├── components/
│   ├── __tests__/
│   │   ├── TestPageContent.test.jsx
│   │   ├── QuestionRenderer.test.jsx
│   │   ├── ResumeConfirmationModal.test.jsx
│   │   └── ... (critical components)
├── utils/
│   ├── __tests__/
│   │   ├── sanitize.test.js
│   │   └── helpers.test.js
├── api/
│   ├── __tests__/
│   │   └── backendApi.test.js
└── test/
    ├── setup.js
    ├── mocks/
    │   ├── handlers.js (MSW handlers)
    │   └── supabase.js
    └── fixtures/
        ├── testData.js
        └── userFixtures.js
```

---

## 🚀 Frontend: Priority 1 Tests

### 2.1 Zustand Stores Tests 🔴 HIGHEST PRIORITY

#### `useAuthStore.test.js`
```js
import { describe, it, expect, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useAuthStore } from '../useAuthStore'

describe('useAuthStore', () => {
  beforeEach(() => {
    // Reset store before each test
    useAuthStore.setState({ user: null, session: null })
  })

  it('should initialize with null user and session', () => {
    const { result } = renderHook(() => useAuthStore())
    expect(result.current.user).toBeNull()
    expect(result.current.session).toBeNull()
  })

  it('should update user on signIn', async () => {
    const { result } = renderHook(() => useAuthStore())
    
    await act(async () => {
      await result.current.signIn('test@example.com', 'password123')
    })
    
    expect(result.current.user).toBeDefined()
    expect(result.current.user.email).toBe('test@example.com')
  })

  it('should clear user on signOut', async () => {
    const { result } = renderHook(() => useAuthStore())
    
    // First sign in
    await act(async () => {
      await result.current.signIn('test@example.com', 'password123')
    })
    
    // Then sign out
    await act(async () => {
      await result.current.signOut()
    })
    
    expect(result.current.user).toBeNull()
  })
})
```

#### `useTestStore.test.js` 🔴
```js
describe('useTestStore', () => {
  it('should save answer when saveAnswer is called', () => {
    const { result } = renderHook(() => useTestStore())
    
    act(() => {
      result.current.saveAnswer('1', 'A')
    })
    
    expect(result.current.answers['1']).toBe('A')
  })

  it('should calculate completion percentage correctly', () => {
    const { result } = renderHook(() => useTestStore())
    
    act(() => {
      result.current.setTotalQuestions(10)
      result.current.saveAnswer('1', 'A')
      result.current.saveAnswer('2', 'B')
    })
    
    expect(result.current.getCompletionPercentage()).toBe(20)
  })

  it('should track time correctly', () => {
    const { result } = renderHook(() => useTestStore())
    
    act(() => {
      result.current.startTimer(3600) // 60 minutes
    })
    
    expect(result.current.timeLeft).toBe(3600)
    
    // Simulate 1 second passing
    act(() => {
      result.current.decrementTime()
    })
    
    expect(result.current.timeLeft).toBe(3599)
  })
})
```

#### `useTestSessionStore.test.js` 🔴
```js
import { vi } from 'vitest'

describe('useTestSessionStore - API caching', () => {
  it('should cache test attempts for 5 minutes', async () => {
    const { result } = renderHook(() => useTestSessionStore())
    
    // First call - should hit API
    await act(async () => {
      await result.current.startTestAttempt('Cambridge 17', 1, 'reading')
    })
    
    const firstCallTime = Date.now()
    
    // Second call within 5 min - should use cache
    await act(async () => {
      await result.current.startTestAttempt('Cambridge 17', 1, 'reading')
    })
    
    // Verify API only called once
    expect(mockApi.startAttempt).toHaveBeenCalledTimes(1)
  })
})
```

### 2.2 Component Tests 🟡

#### `TestPageContent.test.jsx`
```jsx
import { render, screen, fireEvent } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import TestPageContent from '../TestPageContent'

describe('TestPageContent', () => {
  it('should render questions correctly', () => {
    const mockQuestions = [
      { id: 1, question_text: 'What is React?' }
    ]
    
    render(
      <BrowserRouter>
        <TestPageContent questions={mockQuestions} />
      </BrowserRouter>
    )
    
    expect(screen.getByText('What is React?')).toBeInTheDocument()
  })

  it('should disable navigation when modal is open', () => {
    // Test modal blocking logic
  })
})
```

#### `ResumeConfirmationModal.test.jsx` 🔴
```jsx
describe('ResumeConfirmationModal', () => {
  it('should show correct message for IN_PROGRESS attempts', () => {
    render(<ResumeConfirmationModal attemptStatus="IN_PROGRESS" />)
    expect(screen.getByText(/Tiếp tục/i)).toBeInTheDocument()
  })

  it('should show correct message for COMPLETED attempts', () => {
    render(<ResumeConfirmationModal attemptStatus="COMPLETED" />)
    expect(screen.getByText(/Xem kết quả/i)).toBeInTheDocument()
  })
})
```

### 2.3 Utils Tests 🔴

#### `sanitize.test.js` (XSS Prevention)
```js
import { sanitizeHtml } from '../sanitize'

describe('sanitizeHtml', () => {
  it('should remove script tags', () => {
    const dirty = '<p>Hello</p><script>alert("XSS")</script>'
    const clean = sanitizeHtml(dirty)
    expect(clean).not.toContain('<script>')
    expect(clean).toContain('<p>Hello</p>')
  })

  it('should preserve safe HTML', () => {
    const safe = '<p><strong>Bold</strong> text</p>'
    const result = sanitizeHtml(safe)
    expect(result).toBe(safe)
  })
})
```

---

## 📊 Test Implementation Roadmap

### Phase 1: Critical Path (Week 1-2) 🔴
**Goal:** 50% coverage on critical business logic

1. ✅ Setup test infrastructure (dependencies, configs)
2. 🧪 Backend Service Tests:
   - `TestAttemptServiceTest.java` (10 tests)
   - `WritingSubmissionServiceTest.java` (6 tests)
   - `LLMGradingServiceTest.java` (5 tests)
3. 🧪 Frontend Store Tests:
   - `useAuthStore.test.js` (8 tests)
   - `useTestStore.test.js` (12 tests)
   - `useTestSessionStore.test.js` (6 tests)
4. 🧪 Security Tests:
   - `sanitize.test.js` (XSS prevention)
   - `ProfileControllerTest` (IDOR protection)

**Deliverable:** 42 tests, critical flows covered

### Phase 2: Controller & Repository Layer (Week 3) 🟡
**Goal:** 65% overall coverage

1. 🧪 Controller Tests (MockMvc):
   - All 18 controllers với auth tests
   - Happy path + error handling
2. 🧪 Repository Tests (DataJpaTest):
   - Custom query tests
   - RLS behavior tests

**Deliverable:** +60 tests

### Phase 3: Component & Integration Tests (Week 4) 🟢
**Goal:** 75% overall coverage

1. 🧪 React Component Tests:
   - Critical components (~15 components)
   - Modal behaviors
   - User interactions
2. 🧪 Integration Tests:
   - End-to-end flows
   - Test attempt lifecycle
   - Writing submission flow

**Deliverable:** +40 tests

### Phase 4: Comprehensive Coverage (Week 5+) ⚪
**Goal:** 80%+ overall coverage

1. 🧪 Remaining Services
2. 🧪 Utility functions
3. 🧪 Edge cases
4. 🧪 Performance tests

---

## 🛠️ Testing Tools & Best Practices

### Backend Testing Stack
- **JUnit 5** - Test framework
- **Mockito** - Mocking framework
- **MockMvc** - Controller testing
- **AssertJ** - Fluent assertions
- **@DataJpaTest** - Repository testing
- **H2/Testcontainers** - Test databases

### Frontend Testing Stack
- **Vitest** - Test runner (faster than Jest)
- **React Testing Library** - Component testing
- **MSW (Mock Service Worker)** - API mocking
- **@testing-library/user-event** - User interaction simulation

### Best Practices

#### Backend
1. **Arrange-Act-Assert (AAA)** pattern
2. **Mock external dependencies** (Supabase, DeepSeek API)
3. **Test both happy & failure paths**
4. **Use @WithMockUser** for auth tests
5. **Test transactional behavior** explicitly
6. **Verify RLS policies** in repository tests

#### Frontend
1. **Test user behavior, not implementation**
2. **Use `screen` queries** for accessibility
3. **Mock stores** when testing components in isolation
4. **Use `waitFor`** for async operations
5. **Test error states** and loading states
6. **Avoid testing internal state** of Zustand stores

---

## 🔍 Example Test Commands

### Backend
```bash
# Run all tests
cd backend && .\mvnw.cmd test

# Run specific test class
.\mvnw.cmd test -Dtest=TestAttemptServiceTest

# Run with coverage
.\mvnw.cmd test jacoco:report
# View: target/site/jacoco/index.html
```

### Frontend
```bash
# Run all tests
cd frontend && npm test

# Run in watch mode
npm test -- --watch

# Run with UI
npm run test:ui

# Generate coverage report
npm run test:coverage
# View: coverage/index.html
```

---

## 📝 Test Writing Guidelines

### Naming Convention
```java
// Backend (JUnit 5)
@Test
void methodName_ShouldExpectedBehavior_WhenCondition() {
    // Example: submitTestAttempt_ShouldCalculateCorrectScore_WhenAllAnswersProvided
}
```

```js
// Frontend (Vitest)
it('should expected behavior when condition', () => {
    // Example: 'should calculate completion percentage when answers are saved'
})
```

### Test Structure Template

```java
// Backend
@Test
void testName() {
    // Given: Setup test data and mocks
    User user = new User("user123");
    when(repository.findById("user123")).thenReturn(Optional.of(user));
    
    // When: Execute the method under test
    UserDTO result = service.getUser("user123");
    
    // Then: Verify expected outcomes
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo("user123");
    verify(repository, times(1)).findById("user123");
}
```

```js
// Frontend
it('should update state when action is called', () => {
  // Arrange: Setup initial state
  const { result } = renderHook(() => useAuthStore())
  
  // Act: Perform action
  act(() => {
    result.current.setUser({ id: '123' })
  })
  
  // Assert: Verify outcome
  expect(result.current.user.id).toBe('123')
})
```

---

## 🎯 Success Metrics

| Metric | Target | Current |
|--------|--------|---------|
| Backend Test Coverage | 75%+ | 0% ❌ |
| Frontend Test Coverage | 70%+ | 0% ❌ |
| Critical Path Coverage | 90%+ | 0% ❌ |
| Test Execution Time | <2 min | N/A |
| Flaky Tests | 0 | N/A |

---

## 📚 Additional Resources

- [Spring Boot Testing Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Testing Library - Guiding Principles](https://testing-library.com/docs/guiding-principles/)
- [Vitest Documentation](https://vitest.dev/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)

---

## 🚀 Getting Started

1. **Backend Setup:**
   ```bash
   # Add test dependencies to pom.xml
   # Create test directory structure
   mkdir -p backend/src/test/java/com/cramer/{controller,service,repository}
   ```

2. **Frontend Setup:**
   ```bash
   cd frontend
   npm install --save-dev vitest @testing-library/react @testing-library/jest-dom jsdom
   # Create vitest.config.js and setup.js
   ```

3. **Write First Test:**
   - Backend: Start with `TestAttemptServiceTest.java`
   - Frontend: Start with `useAuthStore.test.js`

4. **Run Tests & Iterate:**
   ```bash
   # Backend
   cd backend && .\mvnw.cmd test
   
   # Frontend
   cd frontend && npm test
   ```

---

**Next Steps:** Begin Phase 1 implementation focusing on critical business logic tests.
