# Cramer Performance Improvement Plan

> **Ngày lập**: 28/04/2026
> **Trạng thái**: Plan — đã review, sẵn sàng implement
> **Sub-agent review**: PASS (6/6 tasks) — T2/T3/T6 có note nhỏ, T5 cần cẩn trọng deploy order
> **Mục đích**: Giảm thời gian load dữ liệu, đặc biệt là dashboard và subscription page

---

## Mục lục

1. [Bối cảnh & Phát hiện từ Audit](#1-bối-cảnh--phát-hiện-từ-audit)
2. [Đã hoàn thành (2 tasks)](#2-đã-hoàn-thành-2-tasks)
3. [Task 1 — `hibernate.default_batch_fetch_size=16`](#task-1--hibernatedefault_batch_fetch_size16)
4. [Task 2 — Cache ObjectMapper](#task-2--cache-objectmapper)
5. [Task 3 — Bỏ query `findById` dư thừa](#task-3--bỏ-query-findbyid-dư-thừa)
6. [Task 4 — Fix waterfall `profile?.id` → `user?.id`](#task-4--fix-waterfall-profileid--userid)
7. [Task 5 — Tách `recentPayments` ra endpoint riêng](#task-5--tách-recentpayments-ra-endpoint-riêng)
8. [Task 6 — Route-level code splitting](#task-6--route-level-code-splitting)
9. [Các thay đổi BỊ TỪ CHỐI (không làm)](#các-thay-đổi-bị-từ-chối-không-làm)
10. [Thứ tự triển khai](#thứ-tự-triển-khai)
11. [Cách test từng task](#cách-test-từng-task)

---

## 1. Bối cảnh & Phát hiện từ Audit

### 1.1 Tình trạng thực tế

| Metric | Giá trị |
|--------|---------|
| Kích thước DB | 18 MB |
| Users | 5 |
| `test_attempts` | 117 rows |
| `user_answers` | 143 rows |
| Connection latency tới Supabase | ~300ms/query (US East 2 ↔ VN) |
| PostgreSQL | 17.6 |

### 1.2 Bottleneck chính xác định

**Network latency ~300ms/query là bottleneck số 1.** Vấn đề không phải query chậm, mà do số lượng DB round-trips quá nhiều. Mỗi round-trip = ~300ms, gấp nhiều lần thời gian query thực tế.

### 1.3 Các endpoint nặng nhất (đã audit)

**`GET /api/subscriptions/my-status`** — `SubscriptionServiceImpl.getSubscriptionStatus()`:
```
7 sequential DB queries × 300ms = ~2.1 giây chỉ cho subscription
```

**`GET /api/credits/stats`** — `CreditServiceImpl.getUserStats()`:
```
5-8 sequential DB queries (tùy cold/warm user) × 300ms = ~1.5-2.4 giây
```

**Dashboard load** (Frontend):
```
Auth fires
  +-- Profile API (300ms)
  |     +-- [chờ] --> Dashboard summary API (300ms)  ← waterfall
  +-- 5 stats API calls song song (subscription ×3 + credits + chatbot)
  +-- 1 quota API call

Tổng: 15-20+ DB round-trips × 300ms = 4.5-6 giây để load dashboard
```

---

## 2. Đã hoàn thành (2 tasks)

### ✅ Batch load questions — `TestAttemptService.java`

| | Trước | Sau |
|---|---|---|
| Phương thức | `saveProgress()`, `submitAttempt()` | Same |
| Pattern | `findById()` trong vòng lặp | `findAllById()` trước vòng lặp |
| Queries cho bài 40 câu | 40 queries × 300ms = ~12s | 1 query × 300ms = ~0.3s |
| File | `backend/src/main/java/com/cramer/service/TestAttemptService.java:349, 410` | Dòng 342-347, 413-418 |

### ✅ Auto-save Reading/Listening/Writing

| File | Vai trò |
|------|--------|
| `frontend/src/hooks/useAutoSave.js` (NEW) | Custom hook: interval 30s, 6 lớp guard, dirty check, beforeunload |
| `frontend/src/stores/useTestStore.js` | `getAutoSavePayload(skill)` — payload + chuẩn hóa answers |
| `frontend/src/pages/TestPage.jsx` | Wire up hook, `setTestStatus('submitted')` trước submit |
| `frontend/src/pages/WritingTestPage.jsx` | Wire up hook, guard `isSavingProgress` trong handleSaveAndExit |
| `frontend/src/components/TestPageContent.jsx` | Guard `isSavingProgress` trong handleSaveAndExit |

---

## Task 1 — `hibernate.default_batch_fetch_size=16`

### Vấn đề

Hiện tại khi Hibernate lazy-load 1 entity, nó thực hiện 1 SELECT riêng. Nếu code duyệt qua 10 entity và mỗi entity truy cập 1 relationship lazy → 10 queries riêng lẻ.

Với `batch_fetch_size=16`, Hibernate gom các lazy-load thành `WHERE id IN (?, ?, ...)` — giảm 10 queries xuống 1.

### Code hiện tại

File: `backend/src/main/resources/application.properties` → **không có** dòng `default_batch_fetch_size` nào.

### Fix: Thêm 1 dòng

```properties
# Line mới thêm sau dòng JPA show-sql:
spring.jpa.properties.hibernate.default_batch_fetch_size=16
```

Vị trí: sau dòng `spring.jpa.show-sql=${SPRING_JPA_SHOW_SQL:false}` (dòng 46).

### Rủi ro

| | Đánh giá |
|---|---|
| **Behavior change** | Zero — transparent Hibernate optimization. Logic ứng dụng không đổi |
| **SQL generated** | `WHERE id IN (?, ...)` thay vì `WHERE id = ?` — functionally equivalent |
| **Memory** | Tăng nhẹ (16 entities trong 1 result set thay vì 1) — không đáng kể |
| **Regression** | Zero — Hibernate internal optimization, không ảnh hưởng API contract |
| **Trade-off** | Không có |

### Sub-agent review: **PASS**

---

## Task 2 — Cache ObjectMapper

### Vấn đề

`SubscriptionServiceImpl.getSubscriptionStatus()` tạo `new ObjectMapper()` mỗi lần được gọi. `ObjectMapper` là object nặng, có thể tái sử dụng (thread-safe).

Endpoint này được gọi mỗi khi user vào trang Subscription. ObjectMapper initialization tuy không phải DB query nhưng tốn CPU/GC không cần thiết.

### Code hiện tại

File: `backend/src/main/java/com/cramer/service/implement/SubscriptionServiceImpl.java:460`

```java
com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
String featuresJson = tier.getFeatures().trim();
```

Constructor hiện tại (dòng 65-80) KHÔNG có field `ObjectMapper`:
```java
@Autowired
public SubscriptionServiceImpl(
    SubscriptionTierRepository tierRepository,
    UserSubscriptionRepository subscriptionRepository,
    // ... các repository khác
    @Lazy CreditService creditService) {
    // ... gán fields
}
```

### Fix: Đưa thành `private static final`

```java
// Thêm vào phần field declarations của class (trước constructor, sau TIER_EMOJIS):
private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
    new com.fasterxml.jackson.databind.ObjectMapper();
```

```java
// Sửa dòng 460:
com.fasterxml.jackson.databind.ObjectMapper mapper = OBJECT_MAPPER;
```

### Rủi ro

| | Đánh giá |
|---|---|
| **Thread safety** | `ObjectMapper` là thread-safe sau khi cấu hình xong. Ở đây không có custom configuration → an toàn |
| **State leak** | Zero — `ObjectMapper` không giữ state giữa các lần serialize |
| **Trade-off** | Không có |

### Sub-agent review: **PASS WITH NOTES**

#### Scope mở rộng (optional, làm sau T5 nếu muốn)

Trong codebase còn **9 instances** `new ObjectMapper()` khác có thể cache luôn:

| File | Dòng | Pattern |
|------|------|---------|
| `ChatServiceImpl.java` | 83 | Instance field, không custom config |
| `VocabularyServiceImpl.java` | 56 | Instance field, không custom config |
| `RefinementService.java` | 57 | Instance field |
| `OpenRouterClient.java` | 43 | Instance field |
| `LLMGradingService.java` | 71 | Instance field |
| `ABTSService.java` | 95, 2064 | Instance field + local trong loop |
| `SpeakingSelectionPromptBuilder.java` | 36 | Instance field |
| `SupabaseAdminService.java` | 172 | Local trong method |

> **Note**: `JsonValidatorService.java:95-100` có custom config (ALLOW_TRAILING_COMMA...) → KHÔNG đưa vào `static final`, cần refactor riêng.

Codebase đã có sẵn pattern `private static final ObjectMapper` ở `JsonUtil.java:7`, `JsonPatcher.java:29` → pattern này đã proven.

---

## Task 3 — Bỏ query `findById` dư thừa

### Vấn đề

`getSubscriptionStatus()` gọi `initializeNewUser()` — method này đã INSERT 1 row mới vào `user_subscriptions`. Sau đó method `findById()` query lại chính row vừa tạo — pattern dư thừa không cần thiết.

> **Note kỹ thuật**: Thực tế JPA `EntityManager.find()` kiểm tra persistence context (PC) trước khi query DB. Vì entity vừa được `save()` trong cùng transaction → entity đã ở trong PC → `findById()` **có thể không** hit DB. Tuy nhiên `getReferenceById()` vẫn là pattern tốt hơn vì:
> 1. Semantic rõ ràng: "tôi biết entity tồn tại, cho tôi reference"
> 2. Không cần `Optional` boxing
> 3. Tránh edge case Hibernate version/config mà `find()` bypass PC

### Code hiện tại

File: `backend/src/main/java/com/cramer/service/implement/SubscriptionServiceImpl.java:310-320`

```java
UserSubscription subscription = subscriptionRepository.findActiveByUserId(userId)
    .orElse(null);                                                    // Query 1

if (subscription == null) {
    UserSubscriptionDTO created = initializeNewUser(userId);           // INSERT row mới
    subscription = subscriptionRepository.findById(created.getId())    // Query 2 — DƯ THỪA!
        .orElseThrow(() -> new IllegalStateException(
            "Just-created subscription disappeared for user " + userId));
}
```

### Fix: Dùng `getReferenceById` (proxy, không query DB)

```java
if (subscription == null) {
    UserSubscriptionDTO created = initializeNewUser(userId);
    subscription = subscriptionRepository.getReferenceById(created.getId());
    // getReferenceById trả về proxy — entity đã có trong persistence context
    // (vừa được INSERT bởi initializeNewUser) nên không cần query DB
}
```

`JpaRepository.getReferenceById()` trả về lazy proxy. Vì entity vừa được INSERT trong cùng transaction → đã ở trong persistence context → khi truy cập field (ví dụ `subscription.getTier()` ở dòng 322), Hibernate lấy từ PC, không cần query.

### Rủi ro

| | Đánh giá |
|---|---|
| **Entity không tồn tại** | Không thể — `initializeNewUser()` vừa INSERT và return DTO với ID hợp lệ |
| **Transaction scope** | Cùng `@Transactional` — persistence context shared |
| **`getReferenceById` behavior** | Khác `findById` ở chỗ throw `EntityNotFoundException` (unchecked) thay vì return `Optional.empty` nếu entity không tồn tại. Trong ngữ cảnh này, entity chắc chắn tồn tại |
| **Trade-off** | Không có |

### Sub-agent review: **PASS**

---

## Task 4 — Fix waterfall `profile?.id` → `user?.id`

### Vấn đề

Dashboard `useEffect` chờ `profile?.id` (từ `useProfileStore`) mới fetch dashboard data. Nhưng `profile?.id` chỉ có sau khi API profile trả về (~300ms). Trong khi `user?.id` từ `useAuthStore` đã có sẵn ngay khi auth resolve — không cần chờ.

Backend `GET /api/dashboard/summary` extract userId từ JWT (qua `SecurityContextHolder`) — không nhận userId từ request. Vậy profile data hoàn toàn không cần thiết cho việc fetch dashboard.

### Code hiện tại

File: `frontend/src/pages/Dashboard.jsx:78, 126-134`

```jsx
// Dòng 78: user đã được select từ useAuthStore
const user = useAuthStore(state => state.user);

// Dòng 126-134: nhưng useEffect lại chờ profile?.id
useEffect(() => {
    if (!profile?.id) {        // ← CHỜ profile API (~300ms)
      return;
    }
    fetchSummary(currentPage, 4, debouncedSearchQuery);
}, [profile?.id, currentPage, debouncedSearchQuery, fetchSummary]);
```

### Fix: 1 dòng

```jsx
useEffect(() => {
    if (!user?.id) {           // ← ĐỔI thành user?.id (có sẵn ngay)
      return;
    }
    fetchSummary(currentPage, 4, debouncedSearchQuery);
}, [user?.id, currentPage, debouncedSearchQuery, fetchSummary]);
```

### Rủi ro

| | Đánh giá |
|---|---|
| **user?.id undefined** | Zero — dashboard nằm trong `<ProtectedRoute>`, user chắc chắn có |
| **Backend cần profile data** | Zero — đã verify: `DashboardController` dùng `SecurityContextHolder.getContext().getAuthentication().getName()` để lấy userId |
| **Profile data cần cho render** | Profile data (avatar, tên) load song song và fill sau. Dashboard không dùng profile data trực tiếp trong fetch |
| **Trade-off** | Không có |

### Sub-agent review: **PASS**

---

## Task 5 — Tách `recentPayments` ra endpoint riêng

### Vấn đề

`getSubscriptionStatus()` luôn query `payment_orders` (5 recent payments) — mỗi lần endpoint này được gọi, dù frontend có hiển thị payment history hay không. Payment history chỉ hiển thị khi user click vào tab "Lịch sử giao dịch" trên Subscription page.

### Code hiện tại

File: `backend/src/main/java/com/cramer/service/implement/SubscriptionServiceImpl.java:486-498`

```java
// Luôn chạy — không có guard
List<SubscriptionStatusDTO.PaymentInfo> recentPayments = paymentOrderRepository
    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
    .stream()
    .map(order -> SubscriptionStatusDTO.PaymentInfo.builder()
        .orderCode(order.getOrderCode())
        .type(order.getType().name())
        // ... mapping
        .build())
    .collect(Collectors.toList());
```

Frontend chỉ dùng ở 1 chỗ: `SubscriptionPage.jsx` khi vào tab history.

### Fix — Đầy đủ 4 bước:

**Bước 1 — Thêm method vào interface `SubscriptionService.java`:**

```java
// Thêm vào SubscriptionService.java (sau getSubscriptionStatus):
/**
 * Get recent payment orders for the user (last 5).
 *
 * @param userId the user's UUID
 * @return list of PaymentInfo for the last 5 orders
 */
List<SubscriptionStatusDTO.PaymentInfo> getRecentPayments(UUID userId);
```

**Bước 2 — Trích logic thành method riêng trong `SubscriptionServiceImpl.java`:**

```java
// Thêm method mới với @Transactional(readOnly = true):
@Override
@Transactional(readOnly = true)
public List<SubscriptionStatusDTO.PaymentInfo> getRecentPayments(UUID userId) {
    return paymentOrderRepository
        .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
        .stream()
        .map(order -> SubscriptionStatusDTO.PaymentInfo.builder()
            .orderCode(order.getOrderCode())
            .type(order.getType().name())
            .amountVnd(order.getAmountVnd())
            .status(order.getStatus().name())
            .description(order.getDescription())
            .createdAt(order.getCreatedAt())
            .paidAt(order.getPaidAt())
            .build())
        .collect(Collectors.toList());
}
```

Xóa dòng 486-498 trong `getSubscriptionStatus()`, thay bằng:
```java
List<SubscriptionStatusDTO.PaymentInfo> recentPayments = null;
```

**Bước 3 — Thêm annotation vào DTO field (không serialize null):**

File: `backend/src/main/java/com/cramer/dto/SubscriptionStatusDTO.java`

```java
// Thêm import:
import com.fasterxml.jackson.annotation.JsonInclude;

// Sửa field (dòng 51):
@JsonInclude(JsonInclude.Include.NON_NULL)
private List<PaymentInfo> recentPayments;
```

**Bước 4 — Thêm endpoint mới trong `SubscriptionController.java`:**

```java
@Operation(summary = "Get recent payment history",
    description = "Retrieve the 5 most recent payment orders for the authenticated user")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Successfully retrieved payment history"),
    @ApiResponse(responseCode = "401", description = "Unauthorized")
})
@GetMapping("/my-payments")
public ResponseEntity<List<SubscriptionStatusDTO.PaymentInfo>> getMyPayments(
    Authentication authentication) {
    UUID userId = getCurrentUserId(authentication);
    logger.info("GET /api/subscriptions/my-payments - User: {}", userId);
    return ResponseEntity.ok(subscriptionService.getRecentPayments(userId));
}
```

**Bước 5 — Frontend: API client + SubscriptionPage**

Thêm vào `frontend/src/api/backendApi.js`:
```js
export const getMyPayments = () => api.get('/subscriptions/my-payments');
```

`frontend/src/pages/SubscriptionPage.jsx` — khi user vào tab "Lịch sử giao dịch":
```jsx
const [recentPayments, setRecentPayments] = useState(null);
const [isLoadingHistory, setIsLoadingHistory] = useState(false);

useEffect(() => {
  if (activeTab !== 'history' || recentPayments !== null) return;
  
  setIsLoadingHistory(true);
  backendApi.getMyPayments()
    .then(res => setRecentPayments(res.data))
    .catch(err => console.error('Failed to load payment history:', err))
    .finally(() => setIsLoadingHistory(false));
}, [activeTab]);
```

Thay `status?.recentPayments` → `recentPayments` trong phần render tab history.

> **QUAN TRỌNG — Deploy Order**: Backend PHẢI deploy trước (thêm endpoint `/my-payments`, set `recentPayments=null` trong `/my-status`). Frontend deploy sau (gọi endpoint mới). Nếu FE deploy trước → `/my-payments` trả 404 → tab history báo lỗi.

### Rủi ro

| | Đánh giá |
|---|---|
| **Deploy order** | **Critical** — Backend PHẢI deploy trước. Nếu FE deploy trước → `/my-payments` 404 → tab history lỗi |
| **API contract change** | Có — `recentPayments` trong response `/my-status` sẽ là `null` (sẽ bị `@JsonInclude(NON_NULL)` bỏ qua). Frontend cần update để gọi endpoint mới |
| **Backward compatibility** | Nếu FE chưa update, payment history sẽ trống → low impact, không crash (frontend dùng `status?.recentPayments` với optional chaining) |
| **Test gap** | `SubscriptionServiceImplTest.java` KHÔNG có test cho `getSubscriptionStatus()`. `SubscriptionControllerTest.java` chỉ verify HTTP 200, không verify field contents. **Khuyến nghị**: thêm test cho `/my-payments` và verify `recentPayments` null trong `/my-status` |
| **Trade-off** | 1 extra API call khi user vào tab history (~300ms), đổi lại `getSubscriptionStatus()` nhanh hơn ~300ms **mỗi lần gọi** (kể cả khi không xem history) |

### Sub-agent review: **PASS**

---

## Task 6 — Route-level code splitting

### Vấn đề

`App.jsx` static import **26 page components** (13 admin + 13 user). Tất cả — kể cả 13 admin pages — được bundle vào 1 file JS. User thường vào trang chủ phải download code cho admin panel không bao giờ dùng.

Ngoài ra, có **1 dead import** `TestEditorSelectPage` (dòng 39) — import nhưng không dùng trong bất kỳ `<Route>` nào → sẽ xóa luôn khi code splitting.

### Code hiện tại

File: `frontend/src/App.jsx:11-44`

```jsx
// 26 static imports — tất cả trong 1 bundle
import Home from './pages/Home';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import About from './pages/About';
import TestPage from './pages/TestPage';
import WritingTestPage from './pages/WritingTestPage';
import Courses from './pages/Courses';
import CourseDetailPage from './pages/CourseDetailPage';
import TestLayout from './components/TestLayout';
import TestReviewPage from './pages/TestReviewPage';
import WritingResultPage from './pages/WritingResultPage';
import Profile from './pages/Profile';
import VocabularyPage from './pages/VocabularyPage';
import PricingPage from './pages/PricingPage';
import SubscriptionPage from './pages/SubscriptionPage';
import PaymentSuccessPage from './pages/PaymentSuccessPage';
import PaymentCancelPage from './pages/PaymentCancelPage';

// Admin imports (13 pages)
import { AdminLayout } from './admin/components/layout';
import AdminRouteGuard from './admin/components/AdminRouteGuard';
import AdminDashboard from './admin/pages/AdminDashboard';
import UserListPage from './admin/pages/users/UserListPage';
import UserDetailPage from './admin/pages/users/UserDetailPage';
import FinanceDashboard from './admin/pages/finance/FinanceDashboard';
import TransactionHistoryPage from './admin/pages/finance/TransactionHistoryPage';
import ReportsPage from './admin/pages/finance/ReportsPage';
import ContentListPage from './admin/pages/content/ContentListPage';
import TestEditorSelectPage from './admin/pages/content/TestEditorSelectPage';  // ← DEAD IMPORT (không dùng trong Route nào)
import TestEditorPage from './admin/pages/content/TestEditorPage';
import AIGenerationPage from './admin/pages/content/AIGenerationPage';
import HashtagManagementPage from './admin/pages/content/HashtagManagementPage';
import SetListPage from './admin/pages/content/SetListPage';
import SetDetailPage from './admin/pages/content/SetDetailPage';
```

### Fix: `React.lazy()` + `<Suspense>`

```jsx
import React, { useEffect, lazy, Suspense } from 'react';

// Giữ static import cho components luôn render:
import Header from './components/Header';
import Footer from './components/Footer';
import PageWrapper from './components/PageWrapper';
import FloatingAssistant from './components/FloatingAssistant';
import FullPageLoader from './components/FullPageLoader';
import Home from './pages/Home';
import Login from './pages/Login';
import TestLayout from './components/TestLayout';  // ← GIỮ STATIC: lightweight wrapper (~64 dòng), tránh double-lazy flash với TestPage

// User pages — lazy
const Dashboard = lazy(() => import('./pages/Dashboard'));
const About = lazy(() => import('./pages/About'));
const TestPage = lazy(() => import('./pages/TestPage'));
const WritingTestPage = lazy(() => import('./pages/WritingTestPage'));
const Courses = lazy(() => import('./pages/Courses'));
const CourseDetailPage = lazy(() => import('./pages/CourseDetailPage'));
const TestReviewPage = lazy(() => import('./pages/TestReviewPage'));
const WritingResultPage = lazy(() => import('./pages/WritingResultPage'));
const Profile = lazy(() => import('./pages/Profile'));
const VocabularyPage = lazy(() => import('./pages/VocabularyPage'));
const PricingPage = lazy(() => import('./pages/PricingPage'));
const SubscriptionPage = lazy(() => import('./pages/SubscriptionPage'));
const PaymentSuccessPage = lazy(() => import('./pages/PaymentSuccessPage'));
const PaymentCancelPage = lazy(() => import('./pages/PaymentCancelPage'));

// Admin — tất cả lazy (13 pages)
const AdminLayout = lazy(() => import('./admin/components/layout/AdminLayout'));
//   ↑ Import trực tiếp AdminLayout.jsx (không qua barrel index.js vì barrel dùng named re-export)
const AdminRouteGuard = lazy(() => import('./admin/components/AdminRouteGuard'));
const AdminDashboard = lazy(() => import('./admin/pages/AdminDashboard'));
const UserListPage = lazy(() => import('./admin/pages/users/UserListPage'));
const UserDetailPage = lazy(() => import('./admin/pages/users/UserDetailPage'));
const FinanceDashboard = lazy(() => import('./admin/pages/finance/FinanceDashboard'));
const TransactionHistoryPage = lazy(() => import('./admin/pages/finance/TransactionHistoryPage'));
const ReportsPage = lazy(() => import('./admin/pages/finance/ReportsPage'));
const ContentListPage = lazy(() => import('./admin/pages/content/ContentListPage'));
// TestEditorSelectPage — XÓA (dead import, không dùng trong Route nào)
const TestEditorPage = lazy(() => import('./admin/pages/content/TestEditorPage'));
const AIGenerationPage = lazy(() => import('./admin/pages/content/AIGenerationPage'));
const HashtagManagementPage = lazy(() => import('./admin/pages/content/HashtagManagementPage'));
const SetListPage = lazy(() => import('./admin/pages/content/SetListPage'));
const SetDetailPage = lazy(() => import('./admin/pages/content/SetDetailPage'));
```

Wrap Routes với Suspense:
```jsx
<Suspense fallback={<FullPageLoader />}>
  <AnimatePresence mode="wait">
    <Routes location={location} key={location.pathname}>
      {/* ... existing routes giữ nguyên */}
    </Routes>
  </AnimatePresence>
</Suspense>
```

### Rủi ro

| | Đánh giá |
|---|---|
| **Default export** | Đã verify toàn bộ 26 pages + 2 admin components — **tất cả dùng `export default`**. Không có named export nào gây lỗi với `lazy()` |
| **AdminLayout barrel** | AdminLayout hiện import qua barrel (`index.js` dùng `export { default as AdminLayout }`). Phải import trực tiếp `./admin/components/layout/AdminLayout` trong `lazy()` call (đã reflect trong code trên) |
| **Loading flash** | User thấy `FullPageLoader` ~100-200ms lần đầu vào page. Lần sau browser cache → instant |
| **Double-lazy flash** | `TestLayout` được giữ STATIC (chỉ ~64 dòng, lightweight) để tránh flash 2 lần khi vào TestPage. Nếu lazy-load cả 2: Suspense hiện loader cho TestLayout → render → lại hiện loader cho TestPage |
| **AnimatePresence + Suspense** | Có thể xung đột nhẹ (exit animation delay). Nếu gặp vấn đề, bọc `<AnimatePresence>` BÊN TRONG `<Suspense>` |
| **AdminRouteGuard** | Có thể lazy-load như component render bình thường (dùng trong JSX). Plan cũ ghi "cần static import" là không chính xác |
| **Dead import** | `TestEditorSelectPage` (dòng 39) — import nhưng không dùng trong Route nào → **xóa** thay vì lazy-load |
| **Home.jsx precedent** | `Home.jsx` đã dùng `React.lazy()` cho 7 sub-component với `<Suspense>` → pattern đã proven trong codebase |
| **Trade-off** | Loading spinner lần đầu → đổi lại initial bundle giảm ~50% |

### Sub-agent review: **PASS WITH NOTES** — Đã verify export types (tất cả `export default`), xóa dead import, giữ TestLayout static, cập nhật AdminRouteGuard guidance, note double-lazy flash

---

## Các thay đổi BỊ TỪ CHỐI (không làm)

| Task | Lý do từ chối |
|------|---------------|
| `open-in-view=false` | Rủi ro regression quá cao — có thể gây `LazyInitializationException` ở mọi controller. Cần integration test coverage trước khi tắt. Lợi ích zero với 5 users hiện tại |
| Database indexes (`test_attempts` v.v.) | 117 rows — PostgreSQL bỏ qua index với bảng dưới ~1000 rows. Thêm index = write overhead không cần thiết |
| Dashboard aggregate queries | 143 answers — load vào memory xử lý <1ms. Refactor aggregation từ Java sang SQL rủi ro cao, lợi ích zero ở scale này |
| `@EntityGraph` annotations | Chỉ cần khi tắt `open-in-view`. Hiện tại lazy load hoạt động fine với `open-in-view=true` |
| `React.memo` toàn bộ components | <10 components trên màn hình dashboard — re-render <1ms. Premature optimization |
| `GET /api/init` gom tất cả stats | Vi phạm Single Responsibility. Khó cache/invalidate. Coupling cao |
| Connection pool `idle-timeout` | Đã tồn tại ở line 21 (`600000ms`). Không cần thay đổi |
| Response compression | Payload <10KB với 5 users. Overhead > benefit ở scale này |

---

## Thứ tự triển khai

```
Task 1 (2 phút, zero risk)
  └─→ Task 2 (5 phút, near-zero risk)
        └─→ Task 3 (5 phút, very low risk)
              └─→ Task 4 (5 phút, near-zero risk)
                    ├─→ Task 5 Backend (30 phút) ── deploy BE ──→ Task 5 Frontend (15 phút)
                    └─→ Task 6 (45 phút) ← Frontend, độc lập với T5
```

**Tổng thời gian: ~1 tiếng 50 phút**

### Deploy sequence cho T5 (quan trọng nhất)

```
1. Hoàn thành code T5 Backend (interface + impl + controller + DTO annotation)
2. BUILD & DEPLOY Backend (thêm endpoint /my-payments, recentPayments=null trong /my-status)
3. VERIFY: curl GET /api/subscriptions/my-payments → 200
4. VERIFY: curl GET /api/subscriptions/my-status → recentPayments is null (omitted)
5. Hoàn thành code T5 Frontend (API client + SubscriptionPage tab history)
6. BUILD & DEPLOY Frontend

NẾU LỠ DEPLOY FE TRƯỚC: /my-payments trả 404 → tab "Lịch sử giao dịch" hiển thị lỗi (có thể thêm guard try/catch)
```

T5 Frontend và T6 có thể làm song song (cả hai đều là Frontend, không conflict).

**Khuyến nghị**: Sau T5, thêm unit test cho:
- `GET /api/subscriptions/my-payments` → 200 + list payment
- `GET /api/subscriptions/my-status` → verify `recentPayments` là null

---

## Cách test từng task

### Task 1 — Batch fetch size
```bash
# Verify config được load
cd backend && ./mvnw spring-boot:run
# Check log: không có lỗi
# Smoke test: vào dashboard, subscription page — hoạt động bình thường
```

### Task 2 — Cache ObjectMapper
```bash
# Vào Subscription page → mở tab "Gói đăng ký"
# Kiểm tra features list hiển thị đúng (icon, text)
# Không được crash khi parse features JSON
```

### Task 3 — Bỏ query findById
```bash
# User MỚI (chưa có subscription) → vào trang Subscription lần đầu
# Phải tự động tạo free tier (Cramerie) — không crash, không lỗi EntityNotFoundException
# User đã có subscription → load bình thường
```

### Task 4 — Fix waterfall
```bash
# Mở DevTools → Network tab → Reload dashboard
# Dashboard summary request phải được gửi SONG SONG với profile request
# (không còn chờ profile trước)
# Tổng thời gian load dashboard giảm ~300ms
```

### Task 5 — Tách recentPayments
```bash
# Backend: gọi GET /api/subscriptions/my-payments → trả về 5 recent payments
# GET /api/subscriptions/my-status → recentPayments field empty/null — không crash
# Frontend: Vào Subscription page → tab "Lịch sử giao dịch" → hiển thị payments
# Đặc biệt test: user CHƯA deploy FE → vẫn vào Subscription page bình thường, tab history trống (không crash)
```

**Recommended unit test (thêm sau T5):**
```java
// SubscriptionControllerTest.java
@Test
void getMyPayments_ShouldReturnRecentPayments() {
    // verify HTTP 200 + non-null list
}

@Test  
void getMyStatus_ShouldNotIncludePayments() {
    // verify recentPayments is omitted from response body
}
```

### Task 6 — Code splitting
```bash
cd frontend && npm run build
# So sánh kích thước bundle trước/sau (dist/assets/)
# Navigate qua từng page — không crash, không white flash
# Admin pages — vẫn vào được bình thường
```

---

## Tổng kết

| # | Task | Effort | Risk | Impact |
|---|------|--------|------|--------|
| T1 | `batch_fetch_size=16` | 2 phút | Zero | Giảm N+1 lazy queries |
| T2 | Cache ObjectMapper | 5 phút | Near-Zero | Bớt GC/CPU waste |
| T3 | Bỏ query `findById` dư | 5 phút | Very Low | Cleaner semantics, tránh Optional boxing |
| T4 | Fix waterfall `user?.id` | 5 phút | Near-Zero | -1 round-trip (~300ms) |
| T5 | Tách `recentPayments` | 45 phút | **Low-Medium** ⚠️ | -1 DB query (~300ms)/lần gọi. Deploy BE trước FE |
| T6 | Code splitting | 45 phút | Low | -50% initial JS bundle |

### Risk ranking (thấp → cao)

1. **T1** — 1 dòng config, zero behavior change
2. **T2** — `static final` thay local variable, thread-safe proven
3. **T4** — 1 dòng đổi guard condition
4. **T3** — `getReferenceById` pattern mới nhưng entity chắc chắn trong PC
5. **T6** — 25+ files changed, nhưng pattern `lazy()` đã proven (Home.jsx)
6. **T5** ⚠️ — **Risk nhất**: API contract change, deploy order critical (BE trước FE), cần backend + frontend coordination, không có test cho method bị sửa

**Tổng impact ước tính**: Giảm thời gian load subscription page ~600ms, dashboard ~300ms, initial JS bundle ~50%. Tất cả task đều rủi ro thấp, T5 cần cẩn trọng deploy order.
