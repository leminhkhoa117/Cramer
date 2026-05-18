# Cramer Page Patterns

> **Version:** 1.0
> **Last Updated:** 17/05/2026 (Round 1 fixes applied)
> **Framework:** React 18 + Vite
> **UI Libraries:** Framer Motion, Zustand, React Icons

This document describes the generic page structure patterns used across all Cramer frontend pages. It is **role-based** (not path-based) so it survives file refactors.

---

## Table of Contents

1. [Page Structure Template](#1-page-structure-template)
2. [Loading State Patterns](#2-loading-state-patterns)
3. [Error State Patterns](#3-error-state-patterns)
4. [Empty State Patterns](#4-empty-state-patterns)
5. [BEM Prefix Convention](#5-bem-prefix-convention)
6. [Responsive Breakpoints](#6-responsive-breakpoints)
7. [Animation Patterns](#7-animation-patterns)
8. [Store Import Pattern](#8-store-import-pattern)
9. [CSS Import Pattern](#9-css-import-pattern)
10. [Common Page Sections](#10-common-page-sections)

---

## 1. Page Structure Template

Every page follows this structure:

```jsx
<div className="{prefix}-page">
  {/* Fixed decorative background — optional, pointer-events: none */}
  <div className="{prefix}-page__bg">
    <div className="{prefix}-page__orb {prefix}-page__orb--1" />
    <div className="{prefix}-page__orb {prefix}-page__orb--2" />
  </div>

  {/* Main container */}
  <div className="{prefix}-page__container">
    {/* Hero / Header section */}
    <section className="{prefix}-hero">
      <div className="{prefix}-hero__content">
        <h1 className="{prefix}-hero__title">...</h1>
        <p className="{prefix}-hero__description">...</p>
      </div>
    </section>

    {/* Content section */}
    <section className="{prefix}-content">...</section>
  </div>
</div>
```

### Key rules

| Rule | Details |
|------|---------|
| **Page root** | `min-height: 100vh`, `background: var(--cr-page-bg)`, `font-family: 'Quicksand', sans-serif`, `overflow-x: hidden` |
| **Sidebar pages** | Pages with sidebar layout (Dashboard, Profile) use shared `sl-` classes from `shared/layout.css` |
| **Hero bleed** | Hero sections that absorb parent padding use `margin-top: calc(-1 * var(--header-clearance))` + `padding-top: calc(var(--header-clearance) + Xrem)` to bleed behind the fixed header |
| **Header clearance** | All pages get header clearance from `main.with-fixed-header` in `styles.css`: `padding-top: var(--header-clearance, 90px)` |
| **Background orbs** | Decorative `position: fixed; inset: 0; pointer-events: none` div with floating blurred circles |

### Example: Hero bleed pattern

```css
.page-hero {
  margin-top: calc(-1 * var(--header-clearance, 58px));
  padding-top: calc(var(--header-clearance, 58px) + 3rem);
}
```

---

## 2. Loading State Patterns

Two patterns exist:

### Full-page loader

Used when the **entire page** depends on critical data. Wraps `<FullPageLoader>` inside `<AnimatePresence>`.

**Condition:** `loading && !error && data.length === 0`

```jsx
import FullPageLoader from '../components/FullPageLoader';
import { AnimatePresence } from 'framer-motion';

<AnimatePresence>
  {loading && !error && items.length === 0 && (
    <FullPageLoader message="Đang tải..." />
  )}
</AnimatePresence>
```

### Inline loader

Used for **partial/background loads** (e.g., when data already exists but is being refreshed).

```jsx
<div className="sl-loading-overlay">Đang tải...</div>
```

---

## 3. Error State Patterns

### Full-page error

Used when data is **critical** and the page cannot render without it. Returns early.

```jsx
if (error) {
  return <div className="sl-error">{error}</div>;
}
```

### Inline error banner

Used for **non-critical errors**. Dismissible banner with icon + message + close button.

```jsx
{error && (
  <div className="sl-error" style={{ marginBottom: '1rem' }}>
    <FiAlertCircle />
    <span>{error}</span>
    <button onClick={clearError}>×</button>
  </div>
)}
```

---

## 4. Empty State Patterns

### Shared empty state

For sidebar-layout pages, use the `.sl-empty` class:

```jsx
<div className="sl-empty">Không tìm thấy dữ liệu.</div>
```

### Page-specific empty states

Some pages define custom empty states with illustrations and CTAs (e.g., VocabularyPage has two variants: search-no-results vs no-data).

---

## 5. BEM Prefix Convention

Every page gets its own BEM prefix. Convention: `{page-name}-*` in **lowercase-kebab**.

| Page Role | Prefix | Example Classes |
|-----------|--------|-----------------|
| Courses listing | `cr-courses-*` | `cr-courses-page`, `cr-courses-hero` |
| Vocabulary | `vocab-page-*` | `vocab-page`, `vocab-page__bg` |
| Pricing | `pricing-*` | `pricing-page`, `pricing-hero` |
| Dashboard | `dash-*` | `dash-page`, `dash-sidebar` |
| Profile | `profile-*` | `profile-page`, `profile-tabs` |
| Course Detail | `course-detail-*` | `course-detail-page` |
| About | `about-*` | `about-page`, `about-hero` |
| Subscription | `sub-*` | `sub-page`, `sub-hero` |
| Login/Auth | `auth-*` | `auth-page`, `auth-form` |
| Speaking | `speaking-*` | `speaking-page`, `speaking-session` |

### Rules

- Prefixes are defined in the page's CSS file on the root class selector
- No page CSS should leak into another page
- Shared `sl-*` classes from `layout.css` are exempt (they are designed to be reused)
- Page-scoped CSS variables are defined on the page class, **never** on `:root`

---

## 6. Responsive Breakpoints

| Name | Width | Behaviour |
|------|-------|-----------|
| **Desktop** | `> 992px` | Full layout, multi-column grids |
| **Tablet** | `≤ 992px` | Sidebar → drawer overlay, grids → 2 columns |
| **Mobile** | `≤ 640px` | Grids → 1 column, bottom-sheet modals, full-width buttons |
| **Small phone** | `≤ 480px` | Further padding/font reductions, stack all actions |

### CSS template

```css
/* Desktop — base styles (no media query) */

@media (max-width: 992px) {
  /* Tablet: grid → 2 columns, sidebar → drawer, font reductions */
}

@media (max-width: 640px) {
  /* Mobile: grid → 1 column, bottom-sheet modals, full-width buttons */
}

@media (max-width: 480px) {
  /* Small phone: further padding/font reductions, stack all actions */
}
```

### Mobile patterns every page must follow

1. **Cards**: single column at ≤640px
2. **Modals**: bottom-sheet at ≤640px (`border-radius: 24px 24px 0 0`)
3. **Sidebars**: slide-out drawer with overlay at ≤992px
4. **Buttons**: full width at ≤480px
5. **Grids**: `repeat(auto-fill, minmax(280px, 1fr))` or explicit single-column at ≤640px
6. **Font sizes**: reduce hero/section titles by ~20% at ≤640px
7. **Padding**: reduce container padding to `1rem` at ≤640px, `0.75rem` at ≤480px
8. **Mobile header strip**: `.{page}-mobile-header` with hamburger for pages with sidebars
9. **Touch targets**: minimum 40×40px for interactive elements
10. **No horizontal overflow** — ALL pages must pass `overflow-x: hidden` check

---

## 7. Animation Patterns

### Framer Motion

All pages use `framer-motion` for animations:

```jsx
import { motion, AnimatePresence } from 'framer-motion';
```

### Staggered children (most common)

```jsx
const containerVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { staggerChildren: 0.05 } }
};

const itemVariants = {
  hidden: { y: 20, opacity: 0 },
  visible: { y: 0, opacity: 1, transition: { duration: 0.3 } }
};

<motion.div variants={containerVariants} initial="hidden" animate="visible">
  {items.map(item => (
    <motion.div key={item.id} variants={itemVariants}>{item.content}</motion.div>
  ))}
</motion.div>
```

### Tab panel transitions

```jsx
<AnimatePresence mode="wait">
  <motion.div
    key={activeTab}
    initial={{ opacity: 0, y: 10 }}
    animate={{ opacity: 1, y: 0 }}
    exit={{ opacity: 0, y: -10 }}
    transition={{ duration: 0.2 }}
  >
    {tabContent}
  </motion.div>
</AnimatePresence>
```

### Scroll-triggered animations

```jsx
<motion.div
  whileInView={{ opacity: 1, y: 0 }}
  initial={{ opacity: 0, y: 30 }}
  viewport={{ once: true }}
>
  {content}
</motion.div>
```

---

## 8. Store Import Pattern

All global state uses Zustand stores:

```jsx
import { useXStore } from '../stores';
const x = useXStore(state => state.x);
```

### Common stores by page role

| Page Role | Stores Used |
|-----------|-------------|
| Public (Pricing, Home) | `useAuthStore` |
| Dashboard | `useAuthStore`, `useProfileStore`, `useDashboardStore` |
| Courses | `useCourseStore` |
| Test taking | `useTestStore`, `useTestSessionStore` |
| Profile | `useAuthStore`, `useProfileStore` |
| Vocabulary | `useVocabularyStore` |
| Subscription | `useUserStatsStore`, `useAuthStore` |

---

## 9. CSS Import Pattern

Pages import only the CSS they need:

```jsx
// Pages with sidebar layout
import '../css/shared/layout.css';    // sl-* classes
import '../css/pages/{page}.css';     // page-specific styles

// Standalone pages (no sidebar)
import '../css/pages/{page}.css';

// Test pages
import '../css/test/test-base.css';
import '../css/test/test-header-footer.css';
import '../css/test/test-{skill}.css';
```

### Import rules

- `styles.css` + `tokens.css` are loaded globally via `main.jsx` — **never** import them per-page
- Sidebar pages **must** import `shared/layout.css`
- Test pages **must** import their specific `test/*.css` files in dependency order
- Component CSS (from `css/components/`) is imported by the component itself, not the page

---

## 10. Common Page Sections

### Hero

- Gradient background (typically `--cr-hero-gradient` or page-specific gradient)
- Title (`h1`) + subtitle/description (`p`)
- Optional CTA buttons
- Decorative orbs with `float` animation
- May bleed behind the fixed header via negative `margin-top`

### Controls bar

- Search bar (`.sl-search-container` + `.sl-search-input`)
- Filter button (opens `FilterModal`)
- View toggle (grid/list)
- Wrapped in a glass card with `display: flex; align-items: center; gap: 1rem`

### Content area

- Card grid: `display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1.5rem;`
- Or list layout for tabular data
- Wrapped in a container with `max-width: 1280px; margin: 0 auto; padding: 2rem;`

### Pagination

```jsx
import Pagination from '../components/Pagination';
<Pagination currentPage={page} totalPages={total} onPageChange={setPage} />
```

### Tab panels (sidebar or inline)

Pages with multiple views use either:
- **Sidebar tabs** (Dashboard, Profile): vertical tab list on the left, content on the right
- **Inline tabs** (SubscriptionPage): horizontal tab bar at the top of the content section
