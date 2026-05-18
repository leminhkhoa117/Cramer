# Cramer UI Design System Documentation

> **Version:** 3.1
> **Last Updated:** 17/05/2026
> **Platform:** React + Vite
> **Branch:** `refactor/css-standardization`

This is the **authoritative source** for implementing consistent UI across Cramer. Every page must follow these conventions.

---

## Table of Contents

1. [CSS Architecture](#1-css-architecture)
2. [Design Tokens](#2-design-tokens)
3. [Page Layout Pattern](#3-page-layout-pattern)
4. [Shared Component Library](#4-shared-component-library)
5. [Color Palette](#5-color-palette)
6. [Typography System](#6-typography-system)
7. [Glassmorphism Effects](#7-glassmorphism-effects)
8. [Responsive Design](#8-responsive-design)
9. [Animation Patterns](#9-animation-patterns)
10. [CSS Naming Conventions](#10-css-naming-conventions)
11. [Page-Specific Guidelines](#11-page-specific-guidelines)
12. [Admin CSS](#12-admin-css)

---

## 1. CSS Architecture

### File Organization

```
frontend/src/
├── styles.css                        # Global entry: imports tokens.css, Tailwind, animations
└── css/
    ├── tokens.css                    # ★ SINGLE SOURCE OF TRUTH — all :root variables
    │
    ├── shared/                       # Reusable across ALL pages
    │   ├── layout.css               # .sl-page, .sl-sidebar, .sl-layout, .sl-card, .sl-btn (sidebar system)
    │   └── animations.css           # ALL @keyframes in one place (77 total)
    │
    ├── common/                       # Shared UI components (stay in place)
    │   ├── modal.css                # .cm-backdrop, .cm-content, .cm-header, .cm-btn (glass modal)
    │   ├── faq.css                  # .faq-section, .faq-list, .faq-item
    │   ├── testimonials.css          # .testimonial-card
    │   ├── grading-loader.css       # AI grading waiting screen
    │   └── passage-preview.css      # Dark-themed passage display
    │
    ├── components/                   # One CSS per non-shared component
    │   ├── header.css               # .header, .header--scrolled, .header--hidden
    │   ├── footer.css
    │   ├── pagination.css
    │   ├── full-page-loader.css
    │   ├── floating-assistant.css
    │   ├── progress-chart.css
    │   ├── skill-analysis.css
    │   ├── quota-display.css
    │   ├── quota-exceeded-modal.css
    │   ├── grading-quota-info.css
    │   ├── attempt-history-dropdown.css
    │   ├── change-password-modal.css
    │   ├── upload-image-modal.css
    │   ├── course-list.css
    │   └── ...
    │
    ├── test/                         # ★ All test-taking UI CSS (consolidated from 22 files → 8)
    │   ├── test-base.css            # Grid layout, passage container, questions column, resize handle
    │   ├── test-header-footer.css   # Header bar, timer, footer nav, question buttons
    │   ├── test-question.css        # Question renderer + question group styles
    │   ├── test-reading.css         # Reading-specific: highlight popup, submit info
    │   ├── test-listening.css       # Listening-specific: audio players, toggle switch, visual content
    │   ├── test-writing.css         # Writing-specific: prompt panel, editor panel, word counter
    │   ├── test-review.css          # Review page: merged 8 review CSS files into 1
    │   └── writing-result.css       # Writing result page (de-duplicated)
    │
    ├── speaking/                     # Speaking feature CSS
    │   ├── speaking-session.css
    │   ├── speaking-results.css
    │   ├── speaking-live.css
    │   ├── speaking-components.css
    │   └── examiner-waveform.css
    │
    └── pages/                        # Page-specific overrides
        ├── home.css                 # Merged 10 home section files into 1
        ├── about.css
        ├── pricing.css
        ├── courses.css
        ├── course-detail.css
        ├── dashboard.css
        ├── profile.css
        ├── vocabulary.css
        ├── subscription.css
        ├── payment.css
        └── login.css
```

### Import Strategy

**styles.css** — global entry point:
```css
@import './css/tokens.css';          /* Design tokens — MUST be first */
@tailwind base;
@tailwind components;
@tailwind utilities;
@import './css/shared/animations.css'; /* @keyframes — load last */
```

**JSX per-page imports** — import ONLY what the page needs:
```jsx
// Pages with sidebar layout
import '../css/shared/layout.css';    // sl-* classes
import '../css/pages/dashboard.css';  // page-specific

// Test pages
import '../css/test/test-base.css';
import '../css/test/test-header-footer.css';
import '../css/test/test-reading.css';
```

### Import Rules
- `styles.css` + `tokens.css` are loaded globally via `main.jsx` — NEVER import them per-page
- Sidebar pages (Dashboard, Profile, Courses, Subscription) MUST import `shared/layout.css`
- Test pages MUST import their specific `test/*.css` files in dependency order
- Speaking pages import `css/speaking/*.css` files
- Admin has its own separate `admin/css/tokens.css` with dark theme variables

---

## 2. Design Tokens

### Single Source of Truth: `css/tokens.css`

ALL CSS custom properties are defined in ONE file: `frontend/src/css/tokens.css`. Pages and components reference these via `var(--cr-*)` or legacy aliases.

### Canonical Tokens (use `--cr-*` for new code)

```css
:root {
  /* Layout */
  --cr-header-height: 58px;
  --cr-sidebar-width: 300px;

  /* Brand */
  --cr-primary: #7c3aed;
  --cr-primary-hover: #6d28d9;
  --cr-primary-light: #6366f1;
  --cr-primary-lighter: #8b5cf6;
  --cr-primary-rgb: 124, 58, 237;
  --cr-primary-gradient: linear-gradient(135deg, #7c3aed, #6366f1);
  --cr-hero-gradient: linear-gradient(135deg, #4c1d95, #5b21b6, #7c3aed);
  --cr-gold-gradient: linear-gradient(135deg, #f59e0b, #d97706);
  --cr-cyan: #27afdb;

  /* Text */
  --cr-text: #1f2937;
  --cr-text-secondary: #4b5563;
  --cr-text-muted: #6b7280;
  --cr-text-light: #ffffff;

  /* Surfaces */
  --cr-page-bg: #f8f5ff;
  --cr-card-bg: rgba(255, 255, 255, 0.96);
  --cr-card-border: rgba(124, 58, 237, 0.1);
  --cr-card-shadow: 0 4px 20px rgba(124, 58, 237, 0.06);
  --cr-card-shadow-hover: 0 15px 35px rgba(124, 58, 237, 0.1);

  /* Status */
  --cr-success: #10b981;
  --cr-warning: #f59e0b;
  --cr-danger: #ef4444;

  /* Glass */
  --cr-glass-bg: rgba(255, 255, 255, 0.96);
  --cr-glass-border: rgba(124, 58, 237, 0.1);
  --cr-glass-blur: blur(16px);
  --cr-overlay-bg: rgba(18, 10, 53, 0.25);

  /* Liquid Glass */
  --cr-liquid-bg: rgba(255, 255, 255, 0.85);
  --cr-liquid-border: rgba(255, 255, 255, 0.4);
  --cr-liquid-highlight: rgba(255, 255, 255, 0.6);
  --cr-liquid-shadow: 0 8px 32px rgba(124, 58, 237, 0.12);

  /* Modal */
  --cr-modal-backdrop: rgba(18, 10, 53, 0.65);
  --cr-modal-glass: rgba(124, 120, 226, 0.88);
  --cr-modal-text: #1f2937;
  --cr-modal-shadow: 0 25px 50px rgba(15, 23, 42, 0.35);

  /* Radius */
  --cr-radius-sm: 8px;
  --cr-radius-md: 10px;
  --cr-radius-lg: 16px;
  --cr-radius-xl: 20px;
  --cr-radius-2xl: 24px;
  --cr-radius-full: 9999px;

  /* Z-Index */
  --cr-z-content: 1;
  --cr-z-dropdown: 100;
  --cr-z-drawer: 200;
  --cr-z-header: 1020;
  --cr-z-modal-backdrop: 1000;
  --cr-z-modal-content: 1010;
  --cr-z-loader: 9999;
}
```

### Legacy Aliases (for backward compatibility)

Old variable names still work — they chain to `--cr-*` tokens:
- `--sl-primary` → `var(--cr-primary)`, `--sl-*` → `var(--cr-*)`
- `--modal-glass-bg` → `var(--cr-modal-glass)`
- `--header-clearance` → `var(--cr-header-height)`
- `--primary-accent` → `var(--cr-primary)`
- All page-specific prefixes (`--pricing-*`, `--vocab-*`, `--dash-*`, `--about-*`, etc.) are preserved as aliases

### Page-Scoped Tokens

Pages may define scoped CSS variables on class selectors (NOT `:root`):
```css
.cr-courses__card { --mouse-x: 50%; --mouse-y: 50%; }
```

---

## 3. Page Layout Pattern

### Every page MUST use this structure:

```jsx
<div className="{page-prefix}-page">
  {/* Fixed decorative background — optional, pointer-events: none */}
  <div className="{page-prefix}-page__bg">
    <div className="{page-prefix}-page__orb {page-prefix}-page__orb--1" />
  </div>

  {/* Main container */}
  <div className="{page-prefix}-page__container">
    {/* Hero / Header section */}
    <section className="{page-prefix}-hero">...</section>

    {/* Content */}
    <section className="{page-prefix}-content">...</section>
  </div>
</div>
```

### Key rules:
- Page root div gets `min-height: 100vh`, `background: var(--cr-page-bg)`, `font-family: 'Quicksand', sans-serif`
- Pages with sidebar layout use the shared `sl-` classes (Dashboard, Profile)
- Hero sections that absorb parent padding use `absorb-parent-padding` utility
- All pages get header clearance from `main.with-fixed-header` (58px)

---

## 4. Shared Component Library

### 4.1 Cards (`sl-card` / custom card)

```css
.card, .sl-card {
  background: var(--cr-card-bg);
  backdrop-filter: blur(10px);
  border: 1px solid var(--cr-card-border);
  border-radius: var(--cr-radius-xl); /* 20px */
  padding: 1.75rem;
  margin-bottom: 1.25rem;
  box-shadow: var(--cr-card-shadow);
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
}
.sl-card:hover {
  border-color: rgba(124, 58, 237, 0.2);
  box-shadow: var(--cr-card-shadow-hover);
}
```

### 4.2 Buttons

| Class | Use |
|-------|-----|
| `.sl-btn--primary` | Main action (purple gradient, white text) |
| `.sl-btn--secondary` | Secondary action (light purple bg) |
| `.sl-btn--danger` | Destructive action (red bg) |
| `.sl-btn--small` | Compact variant |

```css
.sl-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 0.65rem 1.25rem;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  text-decoration: none;
}
```

### 4.3 Form Inputs

```css
.sl-form-input {
  width: 100%;
  padding: 0.875rem 1rem;
  background: #fff;
  border: 1px solid rgba(124, 58, 237, 0.15);
  border-radius: 10px;
  color: var(--cr-text);
  font-size: 0.95rem;
  font-family: 'Quicksand', sans-serif;
  transition: all 0.3s ease;
}
.sl-form-input:focus {
  outline: none;
  border-color: var(--cr-primary);
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12);
}
```

### 4.4 Search Bar

```css
.sl-search-container {
  position: relative;
  flex-grow: 1;
  max-width: 300px;
}
.sl-search-input {
  width: 100%;
  padding: 0.65rem 1rem;
  border-radius: 10px;
  border: 1px solid var(--cr-card-border);
  background: #fff;
  font-family: 'Quicksand', sans-serif;
  font-size: 0.9rem;
}
```

### 4.5 Modals

Use `BaseModal` from `components/common/BaseModal.jsx`:

```jsx
import BaseModal from './common/BaseModal';

<BaseModal isOpen={open} onClose={handleClose} title="Title" size="md" footer={...}>
  {/* content */}
</BaseModal>
```

- On mobile (≤640px): bottom-sheet with `border-radius: 24px 24px 0 0`
- All modal content uses `cm-*` prefixed classes

### 4.6 Pagination

```jsx
import Pagination from '../components/Pagination';
<Pagination currentPage={page} totalPages={total} onPageChange={setPage} />
```

### 4.7 Empty / Loading / Error States

```css
.sl-empty { /* Dashed border, centered text, muted */ }
.sl-loading-overlay { /* Absolute overlay with spinner */ }
.sl-error { /* Red card, centered */ }
```

---

## 5. Color Palette

| Role | Hex | Usage |
|------|-----|-------|
| Primary | `#7c3aed` | Buttons, links, accents |
| Primary Hover | `#6d28d9` | Hover states |
| Page BG | `#f8f5ff` | Default page background |
| Card BG | `rgba(255, 255, 255, 0.96)` | Glass cards |
| Text Primary | `#1f2937` | Headings, body |
| Text Secondary | `#4b5563` | Paragraphs |
| Text Muted | `#6b7280` | Labels, metadata |
| Success | `#10b981` | Check marks, mastered |
| Warning | `#f59e0b` | Badges, alerts |
| Danger | `#ef4444` | Delete, errors |

### Gradients

```css
--cr-primary-gradient: linear-gradient(135deg, #7c3aed, #6366f1);
--cr-hero-gradient: linear-gradient(135deg, #4c1d95, #5b21b6, #7c3aed);
--cr-gold-gradient: linear-gradient(135deg, #f59e0b, #d97706);
```

---

## 6. Typography System

Font: **Quicksand** (single font family across the entire app)

| Element | Size | Weight | Usage |
|---------|------|--------|-------|
| Hero Title | `2.5-3rem` | 700-800 | Page hero |
| H1 | `1.75-2.25rem` | 700 | Section headers |
| H2 | `1.35-1.5rem` | 700 | Card titles |
| H3 | `1.1-1.25rem` | 600-700 | Sub-titles |
| Body | `0.95-1rem` | 400-500 | Content |
| Small | `0.8-0.85rem` | 400-500 | Metadata, labels |
| Label | `0.7-0.75rem` | 600-700 | Uppercase labels |

---

## 7. Glassmorphism Effects

Three tiers:

| Tier | Blur | Opacity | Use |
|------|------|---------|-----|
| Light | `blur(8px)` | `0.08` | Subtle hover states |
| Medium | `blur(10-16px)` | `0.96` | Cards, sidebars |
| Strong | `blur(20px)` | `0.88-0.95` | Modals, header |

---

## 8. Responsive Design

### Breakpoints (mobile-first)

| Name | Width | Target |
|------|-------|--------|
| Desktop base | `> 992px` | Laptops, desktops |
| Tablet | `≤ 992px` | Tablets, small laptops |
| Mobile | `≤ 640px` | Phones |
| Small phone | `≤ 480px` | Small phones |

### Mobile patterns every page must follow:

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

### Responsive CSS template:

```css
/* Desktop — base styles */

@media (max-width: 992px) {
  /* Tablet: grid → 2 columns, sidebar → top, font reductions */
}

@media (max-width: 640px) {
  /* Mobile: grid → 1 column, bottom-sheet modals, full-width buttons */
}

@media (max-width: 480px) {
  /* Small phone: further padding/font reductions, stack all actions */
}
```

---

## 9. Animation Patterns

### Standard transitions
```css
transition: all 0.3s ease;
transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
```

### Keyframe animations
- `float`: background orbs, 8-12s infinite
- `fadeIn` / `slideUp`: modals and page elements
- `spin`: loading spinners

### Framer Motion variants (React)
```js
const containerVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { staggerChildren: 0.05 } }
};
const itemVariants = {
  hidden: { y: 20, opacity: 0 },
  visible: { y: 0, opacity: 1, transition: { duration: 0.3 } }
};
```

### Hover effects
- Card lift: `transform: translateY(-4px)` + shadow increase
- Button lift: `transform: translateY(-2px)` + shadow
- 3D card tilt: mouse-tracking via `--mouse-x`/`--mouse-y` CSS custom properties

---

## 10. CSS Naming Conventions

### BEM with page-level prefix

Every page gets its own prefix. No page CSS should leak into another page.

```
.{page}__{element}
.{page}__{element}--{modifier}
.{page}--{modifier}
```

### Prefix reference

| Prefix | Page/System | CSS File |
|--------|-------------|----------|
| `header-`, `.header` | Navigation header | `components/header.css` |
| `sl-` | Shared sidebar layout | `shared/layout.css` |
| `cm-` | Shared modal system | `common/modal.css` |
| `dash-` | Dashboard-specific | `pages/dashboard.css` |
| `cr-courses-` | Courses page | `pages/courses.css` |
| `course-detail-` | Course detail page | `pages/course-detail.css` |
| `dash-course-` | Course list items | `components/course-list.css` |
| `pricing-` | Pricing page | `pages/pricing.css` |
| `vocab-` | Vocabulary page | `pages/vocabulary.css` |
| `profile-` | Profile page | `pages/profile.css` |
| `about-` | About page | `pages/about.css` |
| `sub-` | Subscription page | `pages/subscription.css` |
| `auth-` | Login page | `pages/login.css` |
| `test-` | Test UI (all skills) | `test/*.css` |
| `review-` | Review UI | `test/test-review.css` |
| `writing-` | Writing test UI | `test/test-writing.css` |
| `fa-` | Floating assistant | `components/floating-assistant.css` |
| `grading-quota-` | Grading quota | `components/grading-quota-info.css` |
| `speaking-` | Speaking feature | `speaking/*.css` |

### Examples

```css
.cr-courses__grid          /* Courses page grid */
.cr-courses__card          /* Course card */
.pricing-hero__title       /* Pricing hero title */
.vocab-page__controls      /* Vocabulary controls bar */
.profile-sidebar__cover    /* Profile sidebar cover image */
.test-page-wrapper         /* Test page grid container */
.review-page               /* Review page full-screen layout */
```

### Golden Rule

**NEVER rename a class selector** unless you also update ALL JSX files that use it. Class names are the contract between CSS and components.

---

## 11. Page-Specific Guidelines

### 11.1 Courses (`/courses`)

**Purpose**: Browse all test sets (Cambridge IELTS books, etc.) with search and filter.

**Layout**: Hero banner → Search bar + filter button → Card grid

**Key interactions**:
- Search/filter test sets by name/code
- Each card shows: cover image, name, description
- "Xem các bài test" button → navigates to `/courses/:code`

**Mobile**: Cards single column, hero text compact, filter button stays compact alongside search bar.

**BEM prefix**: `cr-courses-*`

### 11.2 Pricing (`/pricing`)

**Purpose**: Tier comparison, AI grading demo, FAQ. **Must reuse** the grading demo from the homepage (`InteractiveGradingDemo` component).

**Layout**: Hero → Tier cards (2 cols) → Interactive demo → How it works (4 steps) → Feature comparison table → FAQ → CTA banner → Support link

**Key interactions**:
- Tier cards with "Phổ biến nhất" badge on paid tier
- CTA links to `/login` (unauth) or `/subscription` (auth)

**Mobile**: Tier cards single column (popular first), demo single column, comparison table horizontal scroll, FAQ stacked, CTA edge-to-edge.

**BEM prefix**: `pricing-*`

### 11.3 Vocabulary (`/vocabulary`)

**Purpose**: Personal word collection with CRUD, mastery tracking, search, filter, grid/list toggle.

**Layout**: Header with stats → Controls (search + filter tabs + view toggle) → Grid/List of cards → Pagination → Add/Edit modal

**Key interactions**:
- Add word (opens modal)
- Edit/Delete word
- Toggle mastered (star)
- Search with 500ms debounce
- Filter: All / Unmastered / Mastered tabs
- Grid/List view toggle
- Pagination

**Mobile**: Controls stack vertically, cards full-width single column, modal bottom-sheet, "Thêm từ mới" button full-width.

**BEM prefix**: `vocab-*`

### 11.4 Profile (`/profile`)

**Purpose**: User profile management — personal info, page background, avatar/hero upload, security, sessions, login history.

**Layout**: Sidebar + Content using shared `sl-*` classes. Sidebar: Cover image → Avatar + Name → Nav tabs (Thông tin chung, Bảo mật). Content: Cards per tab.

**Key tabs**:
- **Thông tin chung**: Page Background card FIRST → Personal Info card (editable: name, phone, address). Upload modal for images.
- **Bảo mật**: Change password → 2FA (coming soon) → Active sessions → Login history → Linked accounts → Danger zone (delete account)

**Must be consistent with Dashboard** — same sidebar layout, same card styles, same mobile drawer pattern.

**Mobile**: Sidebar becomes slide-out drawer (same as Dashboard), hamburger in mobile header strip, cards full-width.

**BEM prefix**: `profile-*`

---

## Appendix: Quick Reference

### Shadow Scale

| Level | Value |
|-------|-------|
| Card | `0 4px 20px rgba(124, 58, 237, 0.06)` |
| Card hover | `0 15px 35px rgba(124, 58, 237, 0.1)` |
| Modal | `0 25px 50px rgba(15, 23, 42, 0.35)` |
| Header | `0 4px 24px rgba(0, 0, 0, 0.06)` |

### Z-Index Scale

| Value | Usage |
|-------|-------|
| 1-10 | Content, cards |
| 150-200 | Mobile hamburger, drawer, overlay |
| 1000 | Modal backdrop |
| 1010 | Modal content |
| 1020 | Header |

### Border Radius Scale

| Size | Value | Usage |
|------|-------|-------|
| sm | `8px` | Small elements |
| md | `10-12px` | Inputs, buttons |
| lg | `16px` | Cards, sidebars |
| xl | `20px` | Large cards |
| 2xl | `24-28px` | Modals |
| full | `9999px` | Pills, user dropdown |

---

## 12. Admin CSS

Admin uses a **separate dark theme** with its own token system at `admin/css/tokens.css`.

### Admin Structure

```
frontend/src/admin/
├── css/
│   ├── tokens.css                  # Dark-theme design tokens (--admin-*)
│   ├── admin.css                   # Main admin stylesheet
│   ├── common/
│   │   ├── modal.css              # Admin modal system
│   │   └── passage-preview.css    # Admin passage preview
│   ├── components/
│   │   └── admin-preview.css      # Dark-mode overrides for test UI preview
│   └── pages/
│       ├── content/               # Content management pages
│       ├── finance/               # Finance pages
│       ├── users/                 # User management pages
│       └── activity/              # Activity timeline
└── components/
    ├── abts/                      # AI Studio (co-located CSS)
    ├── common/                    # AdminModal.css
    ├── DataTable/
    ├── MetricCard/
    ├── StatusBadge/
    └── Toast/
```

### Admin Tokens

Admin dark theme tokens (`--admin-*`) are isolated from user-facing tokens:
- `--admin-primary: #8B5CF6` (slightly different purple)
- `--admin-bg-primary: #0F0F23` (dark backgrounds)
- `--admin-text-primary: #F8FAFC` (light text on dark)

### Admin Component Co-location

Some admin components have CSS co-located in their component directory (e.g., `admin/components/abts/AIStudio.css`). These stay IN PLACE — do NOT move them.

---

*Last updated: 10/05/2026. CSS structure refactored under branch `refactor/css-standardization`.*
