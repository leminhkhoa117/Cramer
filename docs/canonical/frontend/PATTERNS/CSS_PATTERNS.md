# Cramer CSS Patterns

> **Version:** 1.0
> **Last Updated:** 17/05/2026 (Round 1 fixes applied)
> **Architecture:** CSS Custom Properties + BEM + Tailwind

This document describes the CSS patterns used across all Cramer frontend. It is **role-based** (not path-based) so it survives file refactors.

---

## Table of Contents

1. [Token Architecture](#1-token-architecture)
2. [BEM Naming Convention](#2-bem-naming-convention)
3. [Glassmorphism Tiers](#3-glassmorphism-tiers)
4. [Responsive Breakpoints](#4-responsive-breakpoints)
5. [Sidebar Layout Pattern](#5-sidebar-layout-pattern)
6. [Card Patterns](#6-card-patterns)
7. [Button Patterns](#7-button-patterns)
8. [Animation Patterns](#8-animation-patterns)
9. [CSS Import Strategy](#9-css-import-strategy)
10. [Z-Index Scale](#10-z-index-scale)
11. [Shadow Scale](#11-shadow-scale)
12. [Golden Rule](#12-golden-rule)

---

## 1. Token Architecture

- Single source of truth: `tokens.css` defines ALL `--cr-*` custom properties
- Page-specific tokens are aliased back to `--cr-*` (e.g., `--vocab-primary: var(--cr-primary)`)
- Legacy aliases maintained for backward compatibility
- Tokens organized by: Brand, Text, Surfaces, Glass, Status, Radius, Layout, Modal, Z-Index

### Canonical tokens (use `--cr-*` for new code)

```css
--cr-primary: #7c3aed;
--cr-primary-hover: #6d28d9;
--cr-primary-gradient: linear-gradient(135deg, #7c3aed, #6366f1);
--cr-hero-gradient: linear-gradient(135deg, #4c1d95, #5b21b6, #7c3aed);
--cr-page-bg: #f8f5ff;
--cr-card-bg: rgba(255, 255, 255, 0.96);
--cr-card-border: rgba(124, 58, 237, 0.1);
--cr-text: #1f2937;
--cr-text-secondary: #4b5563;
--cr-text-muted: #6b7280;
--cr-glass-bg: rgba(255, 255, 255, 0.96);
--cr-glass-border: rgba(124, 58, 237, 0.1);
--cr-glass-blur: blur(16px);
--cr-radius-xl: 20px;
--cr-header-height: 58px;
```

---

## 2. BEM Naming Convention

Every page gets its own BEM prefix. Convention: `{prefix}__{element}--{modifier}`

| Prefix | System | CSS File Location |
|--------|--------|-------------------|
| `sl-` | Shared sidebar layout | `shared/layout.css` |
| `cm-` | Shared modal system | `common/modal.css` |
| `header-` | Navigation header | `components/header.css` |
| `pagination-` | Pagination | `components/pagination.css` |
| `fullpage-loader-` | Full page loader | `components/full-page-loader.css` |
| `grading-quota-` | Grading quota info | `components/grading-quota-info.css` |
| `test-` | Test UI (all skills) | `test/test-*.css` |
| `review-` | Review UI | `test/test-review.css` |
| `writing-` | Writing test UI | `test/test-writing.css` |
| `cr-courses-` | Courses page | `pages/courses.css` |
| `vocab-page-` | Vocabulary page | `pages/vocabulary.css` |
| `pricing-` | Pricing page | `pages/pricing.css` |
| `dash-` | Dashboard | `pages/dashboard.css` |
| `profile-` | Profile page | `pages/profile.css` |
| `course-detail-` | Course detail | `pages/course-detail.css` |
| `about-` | About page | `pages/about.css` |
| `sub-` | Subscription page | `pages/subscription.css` |
| `auth-` | Login page | `pages/login.css` |
| `fa-` | Floating assistant | `components/floating-assistant.css` |
| `dash-course-` | Course list items | `components/course-list.css` |
| `speaking-` | Speaking feature | `speaking/*.css` |
| `header-` | Navigation header | `components/header.css` |
| `pagination-` | Pagination | `components/pagination.css` |
| `fullpage-loader-` | Full page loader | `components/full-page-loader.css` |
| `grading-quota-` | Grading quota info | `components/grading-quota-info.css` |
| `fa-` | Floating assistant | `components/floating-assistant.css` |

---

## 3. Glassmorphism Tiers

| Tier | Blur | Opacity | Use |
|------|------|---------|-----|
| Light | `blur(8px)` | 0.08 | Subtle hover states |
| Medium | `blur(10-16px)` | 0.96 | Cards, sidebars |
| Strong | `blur(20px)` | 0.88-0.95 | Modals, header |

### Glass card pattern

```css
.card {
  background: var(--cr-card-bg);
  backdrop-filter: blur(10px);
  border: 1px solid var(--cr-card-border);
  border-radius: var(--cr-radius-xl);
  box-shadow: var(--cr-card-shadow);
}
```

---

## 4. Responsive Breakpoints

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

---

## 5. Sidebar Layout Pattern

- Desktop: `sl-layout` uses `display: flex; gap: 2rem`, sidebar is `position: sticky; top: calc(var(--header-clearance) + 1.5rem)`
- Mobile (≤992px): sidebar becomes fixed drawer (`left: -100%` → `left: 0`), hamburger visible, overlay behind drawer

---

## 6. Card Patterns

- Glass card: `border-radius: 20px`, `padding: 1.75rem`, hover `translateY(-4px)`, hover shadow elevation
- Course cards: mouse-follow radial gradient overlay via `--mouse-x`/`--mouse-y` CSS custom properties

---

## 7. Button Patterns

| Variant | Style |
|---------|-------|
| Primary | Purple gradient (`#7c3aed → #6366f1`), white text, `translateY(-2px)` on hover |
| Secondary | Light purple bg |
| Danger | Red gradient |
| All | `border-radius: 10px`, `font-weight: 600`, `transition: all 0.3s ease` |

---

## 8. Animation Patterns

- All `@keyframes` in one file: `shared/animations.css`
- Common: `fadeIn`, `fadeInUp`, `slideUp`, `float` (for decorative orbs)
- Modal: `cm-fadeIn` (backdrop), `cm-glitchIn` (content — uses clip-path + blur + hue-rotate)
- Loading: `spin` (6 variants for different contexts)

---

## 9. CSS Import Strategy

- `styles.css` imports `tokens.css` + Tailwind + `animations.css` — loaded globally via `main.jsx`
- Pages import only what they need: `shared/layout.css` (for sidebar) + `pages/{page}.css`
- Test pages import `test/test-base.css` + skill-specific CSS
- Admin has its own separate `admin/css/tokens.css` with dark theme

---

## 10. Z-Index Scale

| Value | Usage |
|-------|-------|
| 1-10 | Content, cards |
| 150-200 | Mobile hamburger, drawer, overlay |
| 1000 | Modal backdrop |
| 1010 | Modal content |
| 1020 | Header |

---

## 11. Shadow Scale

| Level | Value |
|-------|-------|
| Card | `0 4px 20px rgba(124, 58, 237, 0.06)` |
| Card hover | `0 15px 35px rgba(124, 58, 237, 0.1)` |
| Modal | `0 25px 50px rgba(15, 23, 42, 0.35)` |
| Header | `0 4px 24px rgba(0, 0, 0, 0.06)` |

---

## 12. Golden Rule

**NEVER rename a class selector** unless you also update ALL JSX files that use it. Class names are the contract between CSS and components.
