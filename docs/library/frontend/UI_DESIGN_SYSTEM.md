# Cramer UI Design System Documentation

> **Version:** 1.0  
> **Last Updated:** January 2026  
> **Platform:** React + Vite + Tailwind CSS

This document provides a comprehensive reference of the Cramer IELTS platform's design system, CSS architecture, and styling patterns. Use this as the authoritative source for implementing consistent UI across the application.

---

## Table of Contents

1. [CSS Architecture](#1-css-architecture)
2. [Tailwind Configuration](#2-tailwind-configuration)
3. [Design Tokens](#3-design-tokens)
4. [Color Palette](#4-color-palette)
5. [Typography System](#5-typography-system)
6. [Glassmorphism Effects](#6-glassmorphism-effects)
7. [Component Styling Patterns](#7-component-styling-patterns)
8. [Responsive Design](#8-responsive-design)
9. [Animation Patterns](#9-animation-patterns)
10. [CSS Naming Conventions](#10-css-naming-conventions)

---

## 1. CSS Architecture

### File Organization

The CSS is organized in a **component-based architecture** under `frontend/src/css/`:

```
frontend/src/
├── styles.css              # Global styles, Tailwind directives, base resets
└── css/
    ├── common/             # Shared reusable styles
    │   ├── faq.css
    │   ├── grading-loader.css
    │   ├── modal.css           # Unified modal system
    │   ├── panel-resize-handle.css
    │   ├── passage-preview.css
    │   ├── review-layout-base.css
    │   ├── sidebar-layout.css  # Shared sidebar patterns
    │   ├── test-layout-base.css
    │   └── testimonials.css
    │
    ├── # Page-specific styles (43+ files)
    ├── about.css
    ├── audio-player.css
    ├── courses.css
    ├── dashboard.css
    ├── floating-assistant.css
    ├── footer.css
    ├── header.css
    ├── home.css            # Homepage (4000+ lines)
    ├── login.css
    ├── pricing-page.css
    ├── profile-page.css
    ├── subscription-page.css
    ├── test-page.css
    ├── writing-test-page.css
    └── ... (40+ more component files)
```

**Total CSS Files:** 52 files (9 in `common/`, 43 page/component-specific)

### CSS Import Strategy

```css
/* styles.css - Entry point */
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap');

@tailwind base;
@tailwind components;
@tailwind utilities;

/* Component CSS files import shared modules */
/* Example: test-page.css */
@import './common/test-layout-base.css';
```

---

## 2. Tailwind Configuration

The project uses a **minimal Tailwind configuration**, relying primarily on custom CSS for complex styling.

### tailwind.config.js

```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Be Vietnam Pro"', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
```

### PostCSS Configuration

```javascript
// postcss.config.js
import tailwindcss from '@tailwindcss/postcss';
import autoprefixer from 'autoprefixer';

export default {
  plugins: [tailwindcss(), autoprefixer()],
};
```

### Key Tailwind Usage

The project uses Tailwind for:
- **Utility classes** in JSX (flex, grid, padding, margin)
- **Responsive prefixes** (`md:`, `lg:`)
- **Custom utilities** defined in `@layer utilities`

```css
/* Custom Tailwind utility */
@layer utilities {
  .all-\[unset\] {
    all: unset;
  }
}
```

---

## 3. Design Tokens

### CSS Custom Properties (Root Variables)

The design system uses CSS custom properties for consistent theming:

```css
:root {
  /* === Page Layout === */
  --header-clearance: 90px;

  /* === Primary Colors === */
  --primary-accent: #7c3aed;
  --primary-accent-hover: #6d28d9;
  --primary-gradient: linear-gradient(135deg, #7c3aed, #6366f1);
  --primary-color: #7c3aed;
  --primary-rgb: 124, 58, 237;

  /* === Text Colors === */
  --text-primary: #1f2937;
  --text-secondary: #4b5563;
  --text-light: #ffffff;

  /* === Glassmorphism Tokens === */
  --glass-bg: rgba(255, 255, 255, 0.05);
  --glass-border: rgba(255, 255, 255, 0.1);
  --glass-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);

  /* === Liquid Glass Tokens === */
  --liquid-glass-bg: rgba(255, 255, 255, 0.1);
  --liquid-glass-border: rgba(255, 255, 255, 0.2);
  --liquid-glass-highlight: rgba(255, 255, 255, 0.3);
  --liquid-glass-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}
```

### Component-Specific Tokens

Different components define their own CSS variables for scoped theming:

#### Sidebar Layout (`sidebar-layout.css`)
```css
:root {
  --sl-primary: #7c3aed;
  --sl-primary-hover: #6d28d9;
  --sl-primary-light: #6366f1;
  --sl-cyan: #27afdb;
  --sl-text: #1f2937;
  --sl-text-muted: #6b7280;
  --sl-glass-bg: rgba(255, 255, 255, 0.96);
  --sl-glass-border: rgba(124, 58, 237, 0.1);
  --sl-glass-shadow: 0 4px 20px rgba(124, 58, 237, 0.06);
  --sl-page-bg: #f8f5ff;
  --sl-success: #10b981;
  --sl-warning: #f59e0b;
  --sl-danger: #ef4444;
}
```

#### Modal System (`modal.css`)
```css
:root {
  --modal-backdrop-bg: rgba(18, 10, 53, 0.65);
  --modal-glass-bg: rgba(124, 120, 226, 0.88);
  --modal-glass-border: rgba(255, 255, 255, 0.18);
  --modal-text: #ffffff;
  --modal-shadow: 0 25px 50px rgba(15, 23, 42, 0.35);
  --modal-border-radius: 28px;
  --modal-padding: 2rem;
  --modal-max-width-sm: 400px;
  --modal-max-width-md: 480px;
  --modal-max-width-lg: 560px;
  --modal-backdrop-z: 1000;
  --modal-content-z: 1010;
}
```

#### Floating Assistant (`floating-assistant.css`)
```css
:root {
  --fa-primary: #7c3aed;
  --fa-gradient: linear-gradient(135deg, #7c3aed, #6366f1);
  --fa-glass-bg: rgba(255, 255, 255, 0.95);
  --fa-glass-bg-dark: rgba(18, 10, 53, 0.85);
  --fa-glass-shadow: 0 25px 50px rgba(124, 58, 237, 0.25);
  --fa-user-bubble: linear-gradient(135deg, #7c3aed, #6366f1);
  --fa-width: 360px;
  --fa-border-radius: 16px;
}
```

---

## 4. Color Palette

### Primary Brand Colors

| Role | Hex | RGB | Usage |
|------|-----|-----|-------|
| **Primary** | `#7c3aed` | `124, 58, 237` | Buttons, links, accents |
| **Primary Hover** | `#6d28d9` | `109, 40, 217` | Hover states |
| **Primary Light** | `#6366f1` | `99, 102, 241` | Gradients, secondary |
| **Primary Lighter** | `#8b5cf6` | `139, 92, 246` | Highlights |
| **Accent Cyan** | `#27afdb` | `39, 175, 219` | Dashboard accents |

### Gradient Palette

```css
/* Primary Gradient */
background: linear-gradient(135deg, #7c3aed 0%, #6366f1 100%);

/* Purple-Pink Gradient */
background: linear-gradient(135deg, #8b5cf6 0%, #ec4899 100%);

/* Blue Gradient */
background: linear-gradient(135deg, #6366f1 0%, #3b82f6 100%);

/* Teal Gradient */
background: linear-gradient(135deg, #14b8a6 0%, #06b6d4 100%);

/* Gold Gradient (Pricing) */
background: linear-gradient(135deg, #f59e0b, #d97706);

/* Green Gradient */
background: linear-gradient(135deg, #22c55e, #16a34a);
```

### Text Colors

| Role | Value | Usage |
|------|-------|-------|
| **Primary Dark** | `#1f2937` | Headings, main text on light bg |
| **Secondary** | `#4b5563` | Body text, paragraphs |
| **Muted** | `#6b7280` | Labels, metadata |
| **Light** | `#9ca3af` | Disabled, placeholder |
| **White** | `#ffffff` | Text on dark/gradient backgrounds |

### Status Colors

| Status | Color | Background |
|--------|-------|------------|
| **Success** | `#10b981` | `rgba(34, 197, 94, 0.12)` |
| **Warning** | `#f59e0b` | `rgba(245, 158, 11, 0.12)` |
| **Danger** | `#ef4444` | `rgba(239, 68, 68, 0.12)` |
| **Error Text** | `#b42318` | `rgba(255, 235, 230, 0.95)` |

### Background Colors

| Surface | Value | Usage |
|---------|-------|-------|
| **Page Background** | `#f8f5ff` / `#f5f0ff` | Light purple tinted background |
| **Card Background** | `rgba(255, 255, 255, 0.96)` | Glass cards |
| **Modal Backdrop** | `rgba(18, 10, 53, 0.65)` | Dark overlay |
| **Footer** | `linear-gradient(135deg, #120a35, #111827)` | Dark gradient |

---

## 5. Typography System

### Font Family

The primary (and only) font is **Be Vietnam Pro**, a modern Vietnamese-friendly sans-serif.

```css
* {
  font-family: 'Be Vietnam Pro', system-ui, -apple-system, 'Segoe UI', 
               Roboto, 'Helvetica Neue', Arial, sans-serif;
}
```

### Type Scale

| Element | Size | Weight | Line Height |
|---------|------|--------|-------------|
| **Hero Title** | `3.5rem` (56px) | 700-800 | 1.2 |
| **H1 / Page Title** | `2.5rem` (40px) | 700 | 1.2 |
| **H2 / Section Title** | `1.8rem` (28.8px) | 700 | 1.3 |
| **H3 / Card Title** | `1.25rem` (20px) | 600-700 | 1.3 |
| **Body** | `1rem` (16px) | 400 | 1.6 |
| **Body Medium** | `1rem` (16px) | 500 | 1.6 |
| **Small / Meta** | `0.875rem` (14px) | 400 | 1.5 |
| **Label** | `0.75rem` (12px) | 600-700 | 1.4 |

### Typography CSS

```css
h1, h2, h3, h4, h5, h6 {
  font-weight: 600;
  color: #1f2937;
}

p {
  line-height: 1.6;
  color: #4b5563;
}

a {
  color: #7c3aed;
  text-decoration: none;
  transition: color 0.3s ease;
}

a:hover {
  color: #6366f1;
}
```

---

## 6. Glassmorphism Effects

Glassmorphism is the **primary visual style** of Cramer. It creates depth through blurred, semi-transparent backgrounds.

### Standard Glass Effect

```css
.glass-liquid {
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.18);
}
```

### Glass Variations

#### Light Glass (Cards)
```css
.sl-card {
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(124, 58, 237, 0.1);
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(124, 58, 237, 0.06);
}
```

#### Modal Glass (Dark)
```css
.cm-content {
  background: rgba(124, 120, 226, 0.88);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 28px;
  box-shadow: 0 25px 50px rgba(15, 23, 42, 0.35);
}
```

#### Floating Navbar (Purple Glass)
```css
.navbar {
  background: rgba(124, 58, 237, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 9999px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 
    0 4px 24px rgba(124, 58, 237, 0.25),
    0 2px 8px rgba(0, 0, 0, 0.1);
}

/* Scrolled state - transitions to white */
.navbar.scrolled {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(124, 58, 237, 0.12);
}
```

### Backdrop Blur Values

| Effect | Blur Amount | Usage |
|--------|-------------|-------|
| Light | `blur(8px)` | Subtle overlay |
| Medium | `blur(10-16px)` | Standard cards |
| Strong | `blur(20px)` | Modals, header |

---

## 7. Component Styling Patterns

### Buttons

#### Primary Button
```css
.btn-primary,
.sl-btn--primary {
  background: linear-gradient(135deg, #7c3aed 0%, #6366f1 100%);
  color: white;
  border: none;
  border-radius: 10px;
  padding: 0.65rem 1.25rem;
  font-weight: 600;
  box-shadow: 0 8px 18px rgba(124, 58, 237, 0.28);
  transition: all 0.3s ease;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 25px rgba(124, 58, 237, 0.35);
}
```

#### Secondary Button
```css
.sl-btn--secondary {
  background: rgba(124, 58, 237, 0.08);
  color: var(--sl-text);
  border: 1px solid rgba(124, 58, 237, 0.1);
}

.sl-btn--secondary:hover {
  background: rgba(124, 58, 237, 0.15);
  border-color: #7c3aed;
  color: #7c3aed;
}
```

#### Danger Button
```css
.sl-btn--danger {
  background: linear-gradient(135deg, #fee2e2, #fecaca);
  color: #dc2626;
  border: 1px solid rgba(220, 38, 38, 0.2);
}
```

### Cards

```css
.card,
.sl-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(124, 58, 237, 0.1);
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(124, 58, 237, 0.06);
  transition: all 0.3s ease;
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: 0 15px 35px rgba(124, 58, 237, 0.1);
  border-color: rgba(124, 58, 237, 0.2);
}
```

### Form Inputs

```css
.form-control {
  border-radius: 8px;
  border: 2px solid #e5e7eb;
  padding: 0.8rem;
  transition: border-color 0.3s ease;
}

.form-control:focus {
  border-color: #7c3aed;
  box-shadow: 0 0 0 0.2rem rgba(124, 58, 237, 0.15);
  outline: none;
}
```

### Toggle Switch

```css
.toggle-switch-slider {
  background-color: #d1d5db;
  border-radius: 22px;
  transition: background-color 0.2s ease;
}

.toggle-switch-slider::before {
  width: 16px;
  height: 16px;
  background-color: white;
  border-radius: 50%;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.toggle-switch-checkbox:checked + .toggle-switch-slider {
  background-color: #7c3aed;
}
```

### Modal Structure

```css
/* Backdrop */
.cm-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(18, 10, 53, 0.65);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* Content */
.cm-content {
  background: rgba(124, 120, 226, 0.88);
  backdrop-filter: blur(20px);
  border-radius: 28px;
  padding: 2rem;
  max-width: 480px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
}

/* Size variants */
.cm-content--sm { max-width: 400px; }
.cm-content--lg { max-width: 560px; }
```

---

## 8. Responsive Design

### Breakpoints

| Name | Media Query | Target Devices |
|------|-------------|----------------|
| **Desktop (Base)** | `> 1200px` | Large monitors |
| **Laptop** | `max-width: 992px` | Small laptops, tablets landscape |
| **Tablet** | `max-width: 768px` | Tablets portrait |
| **Mobile** | `max-width: 640px` | Smartphones |
| **Small Mobile** | `max-width: 480px` | Small phones |

### Responsive Pattern Examples

```css
/* Base (Desktop) */
.dashboard-course-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
}

/* Tablet */
@media (max-width: 992px) {
  .dashboard-course-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
  
  .sl-layout {
    flex-direction: column;
  }
  
  .sl-sidebar {
    width: 100%;
    position: relative;
    top: 0;
  }
}

/* Mobile */
@media (max-width: 640px) {
  .sl-page .container {
    padding: 0 1rem;
  }
  
  .hero-section {
    padding: 2rem 1rem;
  }
}
```

### Header Clearance

All pages account for the fixed floating header:

```css
:root {
  --header-clearance: 90px;
}

.sl-page {
  padding-top: calc(var(--header-clearance) + 1.5rem);
}

.login-page {
  padding-top: calc(var(--header-clearance) + 5rem);
}
```

---

## 9. Animation Patterns

### Standard Transitions

```css
/* Default timing */
transition: all 0.3s ease;
transition: all 0.3s ease-in-out;

/* Quick interactions */
transition: all 0.2s ease;

/* Smooth movements */
transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

/* 3D card transitions */
transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
```

### Keyframe Animations

#### Float Animation (Background Orbs)
```css
@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-30px) scale(1.05); }
}

.pricing-hero__orb--1 {
  animation: float 8s ease-in-out infinite;
}
```

#### Fade In/Out
```css
@keyframes cm-fadeIn {
  from { opacity: 0; backdrop-filter: blur(0px); }
  to { opacity: 1; backdrop-filter: blur(10px); }
}

@keyframes sl-fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
```

#### Glitch Effect (Modals)
```css
@keyframes cm-glitchIn {
  0% {
    opacity: 0;
    transform: translateY(-20px);
    filter: blur(10px) hue-rotate(60deg);
    clip-path: polygon(0 0, 100% 0, 100% 0, 0 0);
  }
  50% {
    opacity: 0.8;
    transform: translateY(-5px);
    filter: blur(2px) hue-rotate(-20deg);
    clip-path: polygon(0 0, 100% 0, 100% 70%, 0 65%);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0px) hue-rotate(0deg);
    clip-path: polygon(0 0, 100% 0, 100% 100%, 0 100%);
  }
}
```

#### Spinning Loader
```css
@keyframes spin {
  to { transform: rotate(360deg); }
}

.loader {
  animation: spin 1s linear infinite;
}
```

#### Scroll Reveal
```css
.animated-item {
  opacity: 0;
  transform: translateY(30px);
  transition: opacity 0.6s ease-out, transform 0.6s ease-out;
}

.animated-item.in-view {
  opacity: 1;
  transform: translateY(0);
}
```

### Hover Effects

#### Lift Effect
```css
.card:hover,
.dash-course-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(124, 58, 237, 0.25);
}
```

#### Button Lift
```css
.btn-primary:hover,
.cta-button:hover {
  transform: translateY(-4px) scale(1.05);
  box-shadow: 0 15px 30px rgba(124, 58, 237, 0.4);
}
```

#### 3D Card Tilt (JavaScript-driven)
```javascript
// Mouse tracking for 3D parallax cards
const lerpFactor = 0.15;
const maxRotation = 15; // degrees

// Smooth interpolation between current and target rotation
const smoothRotateX = currentRotation.x + (targetRotateX - currentRotation.x) * lerpFactor;
const smoothRotateY = currentRotation.y + (targetRotateY - currentRotation.y) * lerpFactor;

element.style.transform = `perspective(1000px) rotateX(${smoothRotateX}deg) rotateY(${smoothRotateY}deg)`;
```

---

## 10. CSS Naming Conventions

### BEM-like Naming

The project follows a **modified BEM convention**:

```
.component-name
.component-name__element
.component-name__element--modifier
.component-name--modifier
```

### Examples

```css
/* Modal system */
.cm-backdrop          /* Component: cramer-modal backdrop */
.cm-content           /* Content container */
.cm-content--sm       /* Size modifier */
.cm-header            /* Element: header */
.cm-title             /* Element: title */
.cm-close-btn         /* Element: close button */

/* Sidebar layout */
.sl-page              /* Component: sidebar-layout page */
.sl-sidebar           /* Element: sidebar */
.sl-sidebar__cover    /* Sub-element: cover */
.sl-sidebar__nav-btn  /* Sub-element: nav button */
.sl-content           /* Element: content area */
.sl-card              /* Reusable card */
.sl-btn--primary      /* Button modifier */

/* Dashboard */
.dashboard-course-grid
.dash-course-card
.dash-course-card__image-container
.dashboard-goal-meta__item
```

### Prefix Conventions

| Prefix | Meaning | Example |
|--------|---------|---------|
| `cm-` | Cramer Modal | `.cm-backdrop` |
| `sl-` | Sidebar Layout | `.sl-sidebar` |
| `fa-` | Floating Assistant | `.fa-widget` |
| `dash-` | Dashboard-specific | `.dash-course-card` |

### Utility Classes

Global utility classes follow simpler naming:

```css
.glass-liquid          /* Glassmorphism effect */
.btn                   /* Base button */
.btn-primary           /* Primary button */
.btn-light             /* Light button */
.card                  /* Base card */
.rounded-lg            /* Border radius */
.shadow-lg             /* Box shadow */
.text-center           /* Text alignment */
.animated-item         /* Scroll animation target */
.highlighted-text      /* Text highlighting */
```

---

## Appendix: Quick Reference

### Shadow Scale

| Level | Value | Usage |
|-------|-------|-------|
| **sm** | `0 4px 15px rgba(0, 0, 0, 0.1)` | Buttons |
| **md** | `0 10px 25px rgba(0, 0, 0, 0.1)` | Cards default |
| **lg** | `0 15px 30px rgba(0, 0, 0, 0.15)` | Cards hover |
| **xl** | `0 25px 50px rgba(15, 23, 42, 0.28)` | Glass elements |

### Z-Index Scale

| Value | Usage |
|-------|-------|
| `1` | Base elements |
| `10` | Content, cards |
| `50` | Fixed header |
| `100` | Dropdowns, popovers |
| `999` | Sticky banners |
| `1000` | Modal backdrop |
| `1010` | Modal content |
| `1020` | Floating header (highest) |

### Border Radius Scale

| Size | Value | Usage |
|------|-------|-------|
| **sm** | `4px` | Small elements |
| **md** | `8px` | Inputs, buttons |
| **lg** | `12-16px` | Cards |
| **xl** | `20-24px` | Large cards, modals |
| **2xl** | `28px` | Modals |
| **full** | `9999px` | Pills, navbar |

---

*This documentation is auto-generated from the Cramer codebase. For updates, see the source CSS files in `frontend/src/css/`.*
