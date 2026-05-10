# Cramer UI Design System Documentation

> **Version:** 2.0
> **Last Updated:** May 2026
> **Platform:** React + Vite

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

---

## 1. CSS Architecture

### File Organization

```
frontend/src/
├── styles.css                        # Global styles, Tailwind directives, reset, CSS variables
└── css/
    ├── common/                       # SHARED — reuse across all pages
    │   ├── sidebar-layout.css        # .sl-page, .sl-sidebar, .sl-layout, .sl-card, .sl-btn
    │   ├── modal.css                 # .cm-backdrop, .cm-content, .cm-header, .cm-title, .cm-body, .cm-footer
    │   ├── faq.css                   # .faq-section, .faq-list, .faq-item
    │   ├── testimonials.css           # .testimonial-card
    │   ├── grading-loader.css
    │   ├── passage-preview.css
    │   ├── panel-resize-handle.css
    │   ├── review-layout-base.css
    │   └── test-layout-base.css
    │
    └── page-specific/                # One file per page — namespaced with page prefix
        ├── header.css                # .header, .header--scrolled, .header--hidden
        ├── footer.css
        ├── dashboard.css             # .dash-mobile-header, .dash-hamburger, .dash-sidebar-overlay
        ├── pricing.css               # .pricing-hero, .pricing-tiers, .tier-card
        ├── courses.css               # .courses-hero, .courses-grid, .cr-card
        ├── vocabulary.css            # .vocab-page, .vocab-card, .vocab-controls
        ├── profile.css               # .profile-layout, .profile-card, .profile-sidebar
        ├── about.css
        ├── subscription.css
        ├── login.css
        └── ...
```

### Import Strategy

```css
/* styles.css — Global entry */
@import url('https://fonts.googleapis.com/css2?family=Quicksand:wght@400;500;600;700&display=swap');

@tailwind base;
@tailwind components;
@tailwind utilities;
```

```jsx
// Per-page imports in JSX
import '../css/page-specific/courses.css';
```

---

## 2. Design Tokens

### Global CSS Variables (in `:root` via `styles.css`)

```css
:root {
  /* Layout */
  --header-clearance: 58px;

  /* Primary Brand */
  --cr-primary: #7c3aed;
  --cr-primary-hover: #6d28d9;
  --cr-primary-light: #6366f1;
  --cr-primary-lighter: #8b5cf6;
  --cr-primary-gradient: linear-gradient(135deg, #7c3aed, #6366f1);

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
  --cr-cyan: #27afdb;

  /* Glass */
  --cr-glass-blur: blur(16px);
  --cr-overlay-bg: rgba(18, 10, 53, 0.25);

  /* Radius */
  --cr-radius-sm: 8px;
  --cr-radius-md: 12px;
  --cr-radius-lg: 16px;
  --cr-radius-xl: 20px;
  --cr-radius-2xl: 24px;
  --cr-radius-full: 9999px;
}
```

### Component Tokens (per-page, scoped)

Pages define their own `:root` / page-scoped CSS variables using the page prefix:

```css
/* courses.css */
.cr-courses { --courses-card-width: 320px; }
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

| Prefix | Page/System |
|--------|-------------|
| `header-`, `.header` | Navigation header |
| `sl-` | Shared sidebar layout (Dashboard, Profile) |
| `cm-` | Shared modal system |
| `dash-` | Dashboard-specific |
| `pricing-` | Pricing page |
| `courses-`, `cr-` | Courses page |
| `vocab-` | Vocabulary page |
| `profile-` | Profile page |
| `about-` | About page |

### Examples

```css
.cr-courses__grid          /* Courses page grid */
.cr-courses__card          /* Course card */
.pricing-hero__title       /* Pricing hero title */
.vocab-page__controls      /* Vocabulary controls bar */
.vocab-card__word          /* Vocabulary card word */
.profile-sidebar__cover    /* Profile sidebar cover image */
```

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

*Last updated: May 2026. For implementation questions, reference the actual source CSS files and existing pages for patterns.*
