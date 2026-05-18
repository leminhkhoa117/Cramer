# Cramer Component Patterns

> **Version:** 1.0
> **Last Updated:** 17/05/2026 (Round 1 fixes applied)

This document describes the generic component patterns used across all Cramer frontend. It is **role-based** (not path-based) so it survives file refactors.

---

## Table of Contents

1. [BaseModal](#1-basemodal)
2. [Specialized Modal Pattern](#2-specialized-modal-pattern)
3. [Pagination](#3-pagination)
4. [FullPageLoader](#4-fullpageloader)
5. [GradingQuotaInfo](#5-gradingquotainfo)
6. [ProtectedRoute](#6-protectedroute)
7. [PageWrapper](#7-pagewrapper)
8. [Header](#8-header)
9. [Shared Button Classes](#9-shared-button-classes)
10. [Shared Card Classes](#10-shared-card-classes)
11. [Shared Form Input Classes](#11-shared-form-input-classes)
12. [Shared Search Bar](#12-shared-search-bar)

---

## 1. BaseModal

Portal-based modal rendered to `document.body`.

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `isOpen` | bool | required | Controls visibility |
| `onClose` | func | required | Close handler |
| `title` | string | optional | Modal header title |
| `children` | node | optional | Modal body content |
| `footer` | node | optional | Modal footer content |
| `size` | 'sm'\|'md'\|'lg' | 'md' | Width variant |
| `showCloseButton` | bool | true | Show X button |
| `closeOnBackdropClick` | bool | true | Close on backdrop click |
| `className` | string | optional | Additional class |

### CSS classes

- `cm-backdrop` — semi-transparent overlay with blur
- `cm-content` — glass card with blur(20px)
- `cm-content--sm` (400px), default (480px), `--lg` (560px)
- `cm-header`, `cm-title`, `cm-close-btn`
- `cm-body`, `cm-footer`
- `cm-btn`, `cm-btn--primary`, `cm-btn--secondary`, `cm-btn--danger`

### Behaviour

- Returns `null` if `!isOpen`
- Renders via `createPortal` to `document.body`
- Glassmorphism: backdrop `blur(10px)`, content `blur(20px)`
- Animation: `cm-fadeIn` (backdrop), `cm-glitchIn` (content)
- Accessibility: `aria-modal`, `aria-labelledby`
- Mobile (≤640px): bottom-sheet with `border-radius: 24px 24px 0 0`

---

## 2. Specialized Modal Pattern

Specialized modals wrap `BaseModal`:

```jsx
const SpecializedModal = ({ isOpen, onClose, ...specificProps }) => (
  <BaseModal isOpen={isOpen} onClose={onClose} title="..." footer={...}>
    {content}
  </BaseModal>
);
```

- Pass `footer` with `cm-btn--primary` (confirm) and `cm-btn--secondary` (cancel)
- `ConfirmationModal` has `isConfirming` prop that disables buttons + shows spinner
- `StartTestModal` passes `showCloseButton={false}` and conditional content based on `skill`

---

## 3. Pagination

### Props

| Prop | Type | Description |
|------|------|-------------|
| `currentPage` | number | 0-indexed current page |
| `totalPages` | number | Total number of pages |
| `onPageChange` | func | Page change handler |

### Behaviour

- Returns `null` if `totalPages <= 1`
- Smart page number truncation (max 5 visible)
- Disabled state for first/last page buttons
- CSS: `pagination-container`, `pagination-btn`, `.active` modifier
- Accessibility: `aria-label`

---

## 4. FullPageLoader

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `message` | string | "Đang xử lý..." | Main loading text |
| `subMessage` | string | optional | Secondary loading text |

### Behaviour

- Full-screen backdrop with blur
- Centered card with loading GIF, title, optional subtext
- Uses `framer-motion` for enter/leave animations
- Designed to be used inside `<AnimatePresence>`
- CSS: `fullpage-loader-backdrop`, `fullpage-loader-card`, `fullpage-loader-gif`, `fullpage-loader-title`, `fullpage-loader-subtext`

---

## 5. GradingQuotaInfo

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | string | 'default' | 'default' or 'compact' |
| `showIcon` | bool | true | Show status icon |
| `className` | string | optional | Additional class |

### Behaviour

- Two variants: compact (inline text) and default (full card with progress bar)
- Status-driven CSS classes: `success`, `warning`, `error`, `low`
- Links to `/subscription` and `/pricing`
- Uses `PropTypes`

---

## 6. ProtectedRoute

### Props

| Prop | Type | Description |
|------|------|-------------|
| `children` | node | Optional children |

### Behaviour

- If no user → redirect to `/login`
- Otherwise renders `children ?? <Outlet />`
- Supports both wrapping children and layout-route pattern

---

## 7. PageWrapper

### Props

| Prop | Type | Description |
|------|------|-------------|
| `children` | node | Optional children |

### Behaviour

- Scrolls to top on route change
- Wraps children in `motion.div` with fade transition
- Uses `useLocation().pathname` for route change detection

---

## 8. Header

- No props (reads auth store directly)
- Scroll-aware hide/show with `requestAnimationFrame` throttling
- Responsive: custom mobile menu vs Bootstrap collapse
- Dropdown user menu with avatar
- CSS: `header`, `header--scrolled`, `header--hidden`, `header-nav-link`, etc.

---

## 9. Shared Button Classes

| Class | Style | Use |
|-------|-------|-----|
| `sl-btn--primary` | Purple gradient, white text, `translateY(-2px)` hover | Main action |
| `sl-btn--secondary` | Light purple bg | Secondary action |
| `sl-btn--danger` | Red gradient | Destructive action |
| `sl-btn--small` | Compact variant | Small buttons |
| `cm-btn--primary` | Same gradient as sl-btn--primary | Modal confirm |
| `cm-btn--secondary` | Outline style | Modal cancel |

Base: `border-radius: 10px`, `font-weight: 600`, `transition: all 0.3s ease`, `display: inline-flex`, `align-items: center`, `gap: 0.4rem`.

---

## 10. Shared Card Classes

- `sl-card`: Glass card with `border-radius: 20px`, `padding: 1.75rem`, `margin-bottom: 1.25rem`, hover elevation
- Custom page-specific cards follow same visual pattern but with their own BEM prefix

---

## 11. Shared Form Input Classes

- `sl-form-input`: Full-width, `padding: 0.875rem 1rem`, `border-radius: 10px`, focus ring with `box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12)`
- `sl-form-grid`: 2-column grid for form layouts (`grid-template-columns: repeat(2, 1fr); gap: 1.25rem`)

---

## 12. Shared Search Bar

- `sl-search-container`: Relative wrapper, `flex-grow: 1`, `max-width: 300px`
- `sl-search-input`: Full-width, `padding: 0.65rem 1rem`, `border-radius: 10px`, `border: 1px solid var(--cr-card-border)`
