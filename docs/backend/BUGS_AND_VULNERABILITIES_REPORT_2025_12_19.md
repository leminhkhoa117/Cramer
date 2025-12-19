# Bugs and Vulnerabilities Report - 2025-12-19

## 1. Mismatch in Payment Packs (Revenue Loss) [RESOLVED]
**Severity:** Critical
**Status:** Fixed (2025-12-19)
**Location:** `backend/src/main/java/com/cramer/service/implement/PaymentServiceImpl.java` vs `PaymentController` vs Frontend UI.

**Description:**
The payment service had hardcoded validation logic that only accepted specific deprecated Lúa pack amounts (100, 550, 2500), while the controller and frontend were advertising different packs (50, 100, 300, 500, 1000). This caused legitimate purchase attempts for most packs to fail with "Invalid Lúa pack".

**Fix Applied:**
- Synchronized `PaymentServiceImpl.java` to accept the correct packs: 100 (10k), 500 (45k), 2000 (150k).
- Updated `PaymentController.java` to advertise these exact packs.
- Verified database `lua_packs` table matches these values.

---

## 2. Infinite Chat Quota (Resource Drain)
**Severity:** High
**Status:** Open
**Location:** `backend/src/main/java/com/cramer/service/implement/CreditServiceImpl.java` (Line 187)

**Description:**
The `getUserStats` method contains a TODO comment: `// TODO: Calculate actual remaining from chatbot_usage table`. It currently returns the full daily limit without subtracting usage.
```java
// Current code
int dailyChatLimit = subscription.getTier() != null ? subscription.getTier().getDailyChatLimit() : 20;
// TODO: Calculate actual remaining from chatbot_usage table
return UserFullStatsDTO.builder().dailyChatRemaining(dailyChatLimit)...
```
**Impact:** Users have effectively infinite chat quota, which incurs cost for the platform owner.

**Recommendation:**
Implement `ChatbotUsageRepository` count logic and subtract from the limit.

---

## 3. Answer Leakage (Cheating Risk) [ADDRESSED]
**Severity:** Critical
**Status:** Addressed (Pending Deployment/Verification)
**Location:** `backend/src/main/java/com/cramer/controller/TestController.java`

**Description:**
The `/api/tests/data` endpoint previously returned `FullSectionDTO`, which included `QuestionDTO` objects containing the `correctAnswer` and `explanation` fields. This allowed users to inspect the network response and see all answers before starting the test.

**Fix (Implemented in Codebase):**
- Created `TestQuestionDTO` (safe, no answers) and `TestSectionDTO`.
- Updated `TestController` to use `testService.getSafeTest(...)` which maps to these safe DTOs.
- **Note:** This change was written to disk but should be verified in the running application.

---

## 4. Security Configuration "Fail Open"
**Severity:** High
**Status:** Addressed (Pending Deployment/Verification)
**Location:** `backend/src/main/java/com/cramer/config/SecurityConfig.java`

**Description:**
The security chain ended with `.anyRequest().permitAll()`. This violates the "Secure Default" principle. Any new endpoint added to the system would be publicly accessible by default unless explicitly secured.

**Fix (Implemented in Codebase):**
- Changed `.anyRequest().permitAll()` to `.anyRequest().authenticated()`.
- Explicitly permitted public endpoints (auth, webhooks, swagger).

---

## 5. Infinite Time Cheat
**Severity:** Medium
**Status:** Open
**Location:** `backend/src/main/java/com/cramer/service/implement/TestAttemptService.java`

**Description:**
The `saveProgress` method trusts the `timeLeft` sent by the client.
```java
attempt.setTimeLeft(saveProgressDTO.getTimeLeft());
```
**Impact:** A malicious user can manipulate the API request to reset their timer or freeze it, giving themselves unlimited time.

**Recommendation:**
The backend should calculate time remaining based on `startedAt` and the test duration, acting as the source of truth, rather than trusting the client.

---

## 6. User Enumeration Vulnerability
**Severity:** Medium
**Status:** Open
**Location:** `backend/src/main/java/com/cramer/controller/AuthController.java`

**Description:**
The `/api/auth/check-email` endpoint allows unauthenticated users to check if an email exists in the database.

**Impact:** Allows attackers to scrape the user base or verify leaked email lists.

**Recommendation:**
Rate limit this endpoint heavily or remove it if not strictly necessary for the signup flow (e.g., handle "email already exists" error during signup submission instead of pre-check).

---

## 7. Technical Debt: Frontend CSS Conflict
**Severity:** Low (Maintenance/Quality)
**Status:** Open
**Location:** `frontend/package.json`

**Description:**
The project uses both `Bootstrap` (via react-bootstrap) and `Tailwind CSS`. This causes style conflicts, increases bundle size, and complicates maintenance.

**Recommendation:**
Migrate all Bootstrap components to Tailwind and remove the Bootstrap dependency.

---

## 8. Potential XSS via `dangerouslySetInnerHTML`
**Severity:** Medium
**Status:** Open
**Location:** Multiple frontend components (22 instances)

**Description:**
Heavy reliance on `dangerouslySetInnerHTML`. While `sanitizeHtml` is used, this pattern is fragile.

**Recommendation:**
Replace raw HTML injection with structured React components or a Markdown renderer where possible.

---

## 9. Unused Rate Limiting
**Severity:** Medium
**Status:** Open
**Location:** `pom.xml`

**Description:**
`Bucket4j` is included in dependencies but not used. The API is vulnerable to brute-force and DDoS.

**Recommendation:**
Implement rate limiting filters, especially on Auth and Payment endpoints.
