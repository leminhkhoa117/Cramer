# Cramer Route Patterns

> **Version:** 1.0
> **Last Updated:** 17/05/2026 (Round 1 fixes applied)
> **Library:** React Router v6.4+

This document describes the routing patterns used in the Cramer frontend. It is **role-based** (not path-based) so it survives file refactors.

---

## Table of Contents

1. [Router Architecture](#1-router-architecture)
2. [Lazy Loading Helpers](#2-lazy-loading-helpers)
3. [Route Organization](#3-route-organization)
4. [ProtectedRoute Pattern](#4-protectedroute-pattern)
5. [RootLayout Conditional Rendering](#5-rootlayout-conditional-rendering)
6. [Error Boundary](#6-error-boundary)
7. [Test Route Pattern](#7-test-route-pattern)
8. [Adding a New Route](#8-adding-a-new-route)

---

## 1. Router Architecture

- Uses `createBrowserRouter` (React Router v6.4+ data router)
- Root layout component renders Header, AnimatedOutlet, Footer, FloatingAssistant conditionally
- Routes are organized in 3 sections: public, protected, admin

---

## 2. Lazy Loading Helpers

Three helpers exist:

```js
// Plain lazy — no wrapper (for pages without animation)
function lazyRoute(importFn) {
  return {
    async lazy() {
      const mod = await importFn();
      return { Component: mod.default };
    },
  };
}

// Wraps in PageWrapper for framer-motion page transitions
function lazyPage(importFn) {
  return {
    async lazy() {
      const mod = await importFn();
      const Page = mod.default;
      return {
        Component: () => <PageWrapper><Page /></PageWrapper>
      };
    },
  };
}

// Special case for TestPage only (hardcoded import, no PageWrapper)
function lazyTestPage() {
  return {
    async lazy() {
      const mod = await import('../pages/TestPage');
      return { Component: mod.default };
    },
  };
}
```

| Helper | When to Use |
|--------|-------------|
| `lazyPage` | Most public/protected pages (adds fade-in + scroll-to-top) |
| `lazyRoute` | Pages without animation (payment, admin) |
| `lazyTestPage` | TestPage only (hardcoded, no PageWrapper) |

---

## 3. Route Organization

```
Public routes:
  / → Home
  /login → Login
  /about → About
  /pricing → Pricing
  /payment/cancel → PaymentCancelPage

Protected routes (wrapped in ProtectedRoute):
  /dashboard → Dashboard
  /courses → Courses
  /courses/:courseName → CourseDetailPage
  /profile → Profile
  /vocabulary → VocabularyPage
  /subscription → SubscriptionPage
  /test/:source/:testNum/:skill → TestPage
  /test/writing/:source/:testNum → WritingTestPage
  /test/review/:attemptId → TestReviewPage
  /test/writing/review/:attemptId → WritingResultPage
  /payment/success → PaymentSuccessPage

Admin routes (wrapped in AdminRouteGuard + AdminLayout):
  /admin/* → Admin pages
```

---

## 4. ProtectedRoute Pattern

```jsx
const ProtectedRoute = ({ children }) => {
  const user = useAuthStore(state => state.user);
  if (!user) return <Navigate to="/login" replace />;
  return children ?? <Outlet />;
};
```

Supports both wrapping children and layout-route pattern via `Outlet`.

---

## 5. RootLayout Conditional Rendering

- Hides Header on test pages, review pages, and admin pages
- Hides Footer on test pages, review pages, and admin pages
- Hides FloatingAssistant on test pages and admin pages
- Shows SmallViewportWarning on test pages (min-width 768px)
- Shows navigation loader bar when `navigation.state === 'loading'`

---

## 6. Error Boundary

- `errorElement: <RouteError />` on the root route
- 404 catch-all: `{ path: '*', element: <NotFound /> }`

---

## 7. Test Route Pattern

Test routes use a specific param structure:

```
/test/:source/:testNum/:skill
```

Where `:source` is the test set code (e.g., "cam17"), `:testNum` is the test number, `:skill` is the skill name.

---

## 8. Adding a New Route

To add a new route:

1. Create the page component
2. Import it via `lazyPage(() => import('../pages/NewPage'))`
3. Add to the appropriate section (public/protected/admin) in `router.jsx`
4. If it needs a sidebar layout, import `shared/layout.css` in the page
5. If it's a test-like page (full viewport), add conditional rendering logic to RootLayout
