# Cramer UI/UX Design System (v2.2)

This document outlines the official design language and UI guidelines for the Cramer application. Its purpose is to ensure a consistent, modern, and responsive user experience across the entire platform. This is a living document, intended to be updated as the platform evolves.

## 1. Core Philosophy

Our design is built on these core principles:

-   **Glassmorphism:** The primary visual style. We use blurred, semi-transparent backgrounds to create a sense of depth and hierarchy.
-   **Clarity & Focus:** UI elements are clean and well-spaced to guide the user's attention to what matters most.
-   **Fluidity & Animation:** Interactions are smooth and provide meaningful feedback through subtle, consistent animations.
-   **Responsiveness:** The interface is designed for a desktop-first experience and gracefully adapts to smaller screens.
-   **Single Scroll Context:** The browser's native scrollbar is the only scroll mechanism. Avoid nested scroll containers.
-   **Performance-First:** Render content only when visible; pause animations and intervals when off-screen.

---

## 2. Color Palette

The color scheme is based on a vibrant purple gradient, complemented by neutral tones for text and backgrounds.

| Role                      | Swatch                                                                                              | Value (CSS)                               | Notes                                                 |
| ------------------------- | --------------------------------------------------------------------------------------------------- | ----------------------------------------- | ----------------------------------------------------- |
| **Primary Accent**        | <div style="background-color: #7c3aed; width: 50px; height: 20px; border-radius: 4px;"></div>        | `#7c3aed`                                 | Main brand color, used for buttons, links, and highlights. |
| **Primary Accent (Hover)**| <div style="background-color: #6d28d9; width: 50px; height: 20px; border-radius: 4px;"></div>        | `#6d28d9`                                 | Darker shade for hover states.                        |
| **Primary Gradient**      | <div style="background: linear-gradient(135deg, #7c3aed, #6366f1); width: 50px; height: 20px; border-radius: 4px;"></div> | `linear-gradient(135deg, #7c3aed, #6366f1)` | Used for primary buttons and hero sections.           |
| **Glassmorphic BG**       | <div style="background-color: rgba(18, 10, 53, 0.75); width: 50px; height: 20px; border-radius: 4px;"></div> | `rgba(18, 10, 53, 0.75)`                  | Standard background for glass elements (e.g., scrolled header). |
| **Modal Glass BG**        | <div style="background-color: rgba(124, 120, 226, 0.69); width: 50px; height: 20px; border-radius: 4px;"></div> | `rgba(124, 120, 226, 0.69)`               | Background for modal dialogs.                         |
| **Glass Border**          | <div style="background-color: rgba(255, 255, 255, 0.18); width: 50px; height: 20px; border-radius: 4px;"></div> | `rgba(255, 255, 255, 0.18)`               | Subtle border for all glassmorphic elements.          |
| **Primary Text (Dark)**   | <div style="background-color: #1f2937; width: 50px; height: 20px; border-radius: 4px;"></div>        | `#1f2937`                                 | For text on light backgrounds.                        |
| **Primary Text (Light)**  | <div style="background-color: #ffffff; width: 50px; height: 20px; border-radius: 4px;"></div>        | `#ffffff`                                 | For text on dark/gradient backgrounds.                |
| **Secondary Text (Light)**| <div style="background-color: rgba(255, 255, 255, 0.9); width: 50px; height: 20px; border-radius: 4px;"></div> | `rgba(255, 255, 255, 0.9)`                | For subtitles and less important text on dark backgrounds. |
| **Error**                 | <div style="background-color: #b42318; width: 50px; height: 20px; border-radius: 4px;"></div>        | `#b42318`                                 | For error text.                                       |
| **Error Background**      | <div style="background-color: rgba(255, 235, 230, 0.95); width: 50px; height: 20px; border-radius: 4px;"></div> | `rgba(255, 235, 230, 0.95)`               | For the background of error messages/alerts.          |

---

## 3. Typography

The official font family is **Be Vietnam Pro**, imported from Google Fonts.

### Typographic Scale

| Element         | Font Size (rem/px) | Font Weight | Notes                               |
| --------------- | ------------------ | ----------- | ----------------------------------- |
| **Hero Title**  | `3.5rem` (56px)    | `700`       | Main page titles (e.g., Homepage).  |
| **H1 / Page Title** | `2.5rem` (40px)    | `700`       | Standard page titles (e.g., Dashboard). |
| **H2 / Section Title** | `1.8rem` (28.8px)  | `700`       | Titles for major sections.          |
| **H3 / Card Title** | `1.25rem` (20px)   | `600`       | Titles within components like cards. |
| **Body**        | `1rem` (16px)      | `400`       | Default paragraph and body text.    |
| **Body (Medium)**   | `1rem` (16px)      | `500`       | Emphasized body text.               |
| **Small / Meta**    | `0.875rem` (14px)  | `400`       | Helper text, metadata, labels.      |

---

## 4. Layout & Spacing

We use a consistent spacing scale based on a `4px` grid to maintain rhythm and alignment.

-   **Max Width:** `1280px` for main containers.
-   **Container Padding:** `2rem` (32px) on left and right.
-   **Box Sizing:** `border-box` is used for containers to ensure padding is included in the total width.
-   **Component Padding:** `1.25rem` (20px) to `1.5rem` (24px) inside cards and modals.
-   **Gaps between Elements:** `1rem` (16px) for small elements, `1.5rem` (24px) for larger components.
-   **Gaps between Sections:** `3rem` (48px) to `6rem` (96px) for vertical spacing between large page sections.

---

## 5. Elevation & Shadow

We use shadows to create elevation and separate layers.

| Level       | `box-shadow`                               | Use Case                               |
| ----------- | ------------------------------------------ | -------------------------------------- |
| **sm**      | `0 4px 15px rgba(0, 0, 0, 0.1)`            | Buttons, small interactive elements.   |
| **md (Lift)** | `0 10px 25px rgba(0, 0, 0, 0.1)`           | Standard cards, default state.         |
| **lg (Hover)**| `0 15px 30px rgba(0, 0, 0, 0.15)`          | Hovered cards, active elements.        |
| **xl (Glass)**| `0 25px 50px rgba(15, 23, 42, 0.28)`       | Large glassmorphic elements for depth. |

---

## 6. Z-Index Scale

To manage stacking context and prevent layering issues, adhere to the following z-index scale.

| Value  | Usage                               |
| ------ | ----------------------------------- |
| `1`    | Base elements, backgrounds.         |
| `10`   | Content elements, cards.            |
| `50`   | Sticky/fixed elements like Headers. |
| `100`  | Dropdown menus, popovers.           |
| `1000` | Modal backdrops/overlays.           |
| `1010` | Modal content.                      |

---

## 7. Core Components

### Buttons

| State       | Style                                                                                             |
| ----------- | ------------------------------------------------------------------------------------------------- |
| **Default** | `background: linear-gradient(135deg, #7c3aed, #6366f1); color: white;`                             |
| **Hover**   | `transform: translateY(-2px); box-shadow: var(--shadow-lg); filter: brightness(1.1);`              |
| **Active**  | `transform: translateY(0px); filter: brightness(1.0);`                                            |
| **Disabled**| `opacity: 0.5; cursor: not-allowed;`                                                              |

### Form Inputs

| State       | Style                                                                                             |
| ----------- | ------------------------------------------------------------------------------------------------- |
| **Default** | `background-color: rgba(255, 255, 255, 0.1); border: 1px solid rgba(255, 255, 255, 0.3);`           |
| **Focus**   | `border-color: #a78bfa; box-shadow: 0 0 0 3px rgba(167, 139, 250, 0.3);`                           |
| **Error**   | `border-color: #fca5a5; background-color: rgba(252, 165, 165, 0.1);`                               |
| **Disabled**| `background-color: rgba(255, 255, 255, 0.05); opacity: 0.6;`                                      |

---

## 8. Iconography

-   **Library:** `react-icons`. Primarily use Feather (`Fi`) for a modern, clean look (e.g., `FiEdit3`). Font Awesome (`Fa`) can be used for social icons or where `Fi` lacks an equivalent.
-   **Standard Size:** `20px` or `1.25rem`.
-   **Style:** Stroke-based, non-filled icons are preferred.
-   **Usage:** Icons should always be accompanied by a text label or have an `aria-label` for accessibility.

---

## 9. Interactivity & Animations

Animations should be subtle and quick to provide feedback without being distracting.

-   **Standard Transition:** `transition: all 0.3s ease-in-out;`
-   **Hover Effect (Cards/Buttons):** A subtle "lift" effect using `transform` and the shadow system.
    -   `transform: translateY(-5px);`
    -   `box-shadow: var(--shadow-lg);`
-   **3D Parallax Cards:** Interactive cards with mouse-tracking 3D parallax effects.
    -   **Transition Duration:** `0.4s` for smooth, fluid motion
    -   **Easing Function:** `cubic-bezier(0.25, 0.46, 0.45, 0.94)` for natural acceleration/deceleration
    -   **Interpolation (Lerp):** Use linear interpolation with a factor of `0.15` for smooth following motion
    -   **Rotation Limits:** Maximum ±15° rotation on both X and Y axes
    -   **Implementation:** Use `useRef` to track current rotation values and apply smooth interpolation between current and target positions
    -   **Example:**
        ```javascript
        const currentRotationRef = useRef({ x: 0, y: 0 });
        const lerpFactor = 0.15;
        const smoothRotateX = currentRotationRef.current.x + (targetRotateX - currentRotationRef.current.x) * lerpFactor;
        ```
-   **Page Transitions:** Use simple fades or slide-ins. The `framer-motion` library is preferred for staggering animations.
    -   **Stagger Container:** `transition: { staggerChildren: 0.1 }`
    -   **Stagger Item:** `initial={{ y: 20, opacity: 0 }}`, `animate={{ y: 0, opacity: 1 }}`

---

## 10. Responsiveness

-   **Approach:** Desktop-first. Base styles are for desktop resolutions. Use `@media` queries to override styles for smaller screens.
-   **Breakpoints:**
    -   **Large Desktop:** `> 1200px` (No query needed, this is the base)
    -   **Tablet / Small Laptop:** `@media (max-width: 992px)`
    -   **Mobile:** `@media (max-width: 640px)`
-   **Units:** Use `rem` for typography and `rem` or `%` for layout dimensions to ensure scalability. Avoid fixed `px` values for layout containers where possible.
-   **Example:**
    ```css
    /* Base (Desktop) */
    .my-component {
      display: flex;
      gap: 2rem;
    }

    /* Tablet */
    @media (max-width: 992px) {
      .my-component {
        gap: 1rem;
      }
    }

    /* Mobile */
    @media (max-width: 640px) {
      .my-component {
        flex-direction: column;
      }
    }
    ```

---

## 11. Page Structure & Scrolling

To ensure smooth scrolling and prevent double scrollbar issues, follow these structural guidelines.

### Global Layout Architecture

The app uses a **flex-column layout** in `#root` to position Header, main content, and Footer:

```css
/* styles.css */
html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  overflow-x: hidden;
  scroll-behavior: smooth;
}

#root {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

#root > main {
  flex: 1 0 auto;
}

#root > .navbar,
#root > .site-footer {
  flex-shrink: 0;
}
```

### Preventing Double Scrollbars

**❌ Avoid:**
-   Setting `min-height: 100vh` on page containers inside `<main>` (creates nested scroll context)
-   Using `overflow-y: auto` or `overflow-y: scroll` on page-level elements
-   Combining `height: 100vh` with `overflow: auto` on inner containers

**✅ Do:**
-   Let the browser handle scrolling at the `<html>` level
-   Use `overflow: visible` on page containers (e.g., `.home-page`, `.dashboard-page`)
-   Use `overflow: hidden` only for clipping decorative elements (orbs, backgrounds)
-   Reserve `min-height: 100vh` / `100dvh` for hero sections only

### Page Container Pattern

```css
/* Correct page container styling */
.my-page {
  background: #f5f0ff;
  overflow: visible;  /* Don't create scroll context */
  position: relative;
  width: 100%;
}

/* Hero sections can use viewport height */
.my-page__hero {
  min-height: 100vh;
  min-height: 100dvh;  /* Dynamic viewport for mobile */
  overflow: hidden;    /* Clip decorative elements only */
}
```

### PageWrapper Component

All routes should be wrapped with `PageWrapper` for consistent animations and scroll behavior:

```jsx
// components/PageWrapper.jsx
import { motion } from 'framer-motion';
import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

const PageWrapper = ({ children }) => {
  const { pathname } = useLocation();

  // Reset scroll position on route change
  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
  }, [pathname]);

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.25 }}
      style={{ width: '100%' }}
    >
      {children}
    </motion.div>
  );
};
```

**Key points:**
-   Use `behavior: 'instant'` for scroll reset to prevent visual jank
-   Avoid `y` transforms in page animations (can cause initial scroll offset)
-   Keep animations opacity-only for page transitions

### Mobile Viewport Units

For full-viewport sections on mobile (where address bars affect height):

| Unit | Description | Use Case |
|------|-------------|----------|
| `100vh` | Fixed viewport height | Desktop layouts |
| `100dvh` | Dynamic viewport height | Mobile hero sections (accounts for address bar) |
| `100svh` | Small viewport height | When you want minimum height regardless of address bar |
| `100lvh` | Large viewport height | When you want maximum height with address bar hidden |

```css
.hero-section {
  min-height: 100vh;
  min-height: 100dvh;  /* Fallback for browsers supporting dvh */
}
```

---

## 12. File Organization

### CSS File Structure

```
frontend/src/
├── styles.css          # Global styles, resets, #root layout
├── css/
│   ├── Header.css      # Header/navbar styles
│   ├── Footer.css      # Footer styles
│   ├── Home.css        # Homepage-specific styles
│   ├── Dashboard.css   # Dashboard page styles
│   ├── Profile.css     # Profile page styles
│   └── [Component].css # Component-specific styles
```

### Naming Conventions

-   **Page classes:** `.{page-name}-page` (e.g., `.home-page`, `.dashboard-page`)
-   **Section classes:** `.{page-name}__{section}` (e.g., `.home-page__hero`, `.dashboard__stats`)
-   **Component classes:** `.{component-name}` (e.g., `.course-card`, `.stat-badge`)
-   **Modifiers:** `.{class}--{modifier}` (e.g., `.btn--primary`, `.card--highlighted`)

---

## 13. Performance & Visibility Optimization

Heavy pages (3D scenes, carousels, animations) should optimize rendering by only activating content when it is visible in the viewport. This reduces GPU/CPU load and improves battery life on mobile devices.

### Shared `useInView` Hook

Use the shared hook at `src/hooks/useInView.js` for visibility tracking:

```javascript
import { useInView, useSectionInView } from '../hooks/useInView';

// For animation triggers (fires once, stays true)
const [ref, isInView] = useInView({ threshold: 0.1, triggerOnce: true });

// For section-level optimization (toggles both ways with preload buffer)
const [sectionRef, sectionInView] = useSectionInView({ rootMargin: '200px' });
```

| Hook | Behavior | Use Case |
|------|----------|----------|
| `useInView` | Configurable; can be `triggerOnce` or toggle | Scroll-triggered animations |
| `useSectionInView` | Toggles visibility; 200px preload buffer | Pausing heavy sections |

### Pausing Heavy Components

#### 3D Scenes (Three.js / React Three Fiber)

Pass an `isActive` prop to pause the render loop when off-screen:

```jsx
// Parent component
const [heroRef, heroInView] = useSectionInView({ rootMargin: '100px' });

<section ref={heroRef}>
  <Scene3D isActive={heroInView} />
</section>

// Scene component
<Canvas frameloop={isActive ? 'always' : 'demand'}>
  {/* ... */}
</Canvas>
```

#### Auto-Rotating Carousels / Intervals

Gate intervals with the section's visibility state:

```jsx
const [sectionRef, sectionInView] = useSectionInView();

useEffect(() => {
  if (!sectionInView) return; // Don't start interval if not visible
  
  const interval = setInterval(() => {
    setActiveSlide((prev) => (prev + 1) % slides.length);
  }, 5000);
  
  return () => clearInterval(interval);
}, [sectionInView]);
```

#### Framer Motion Animations

Conditionally animate based on visibility:

```jsx
<motion.div
  animate={isInView ? 'visible' : 'hidden'}
  variants={{
    visible: { opacity: 1, y: 0 },
    hidden: { opacity: 0, y: 20 }
  }}
/>
```

### Root Margin (Preload Buffer)

Use `rootMargin` to start loading/rendering content *before* it enters the viewport to avoid visible pop-in:

| Margin | Use Case |
|--------|----------|
| `"100px"` | Heavy 3D scenes (start GPU work early) |
| `"200px"` | Standard sections with animations |
| `"50px"` | Lightweight content |

### Reduced Motion Support

Always respect the user's `prefers-reduced-motion` preference:

```javascript
const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

if (prefersReducedMotion) {
  // Return static fallback instead of animated content
  return <div className="static-fallback" />;
}
```

### Performance Checklist

When building a new page with heavy visuals:

- [ ] Lazy-load heavy components with `React.lazy()` + `<Suspense>`
- [ ] Use `useSectionInView` to track section visibility
- [ ] Pass `isActive` prop to 3D scenes to pause render loops
- [ ] Gate `setInterval`/`setTimeout` with visibility state
- [ ] Add `rootMargin` buffer to prevent visible pop-in
- [ ] Provide static fallbacks for `prefers-reduced-motion`
- [ ] Use `loading="lazy"` on images below the fold

---

## 14. Full-Viewport Layouts (Writing Review Pattern)

For pages that require fitting all content within the viewport with internal scrolling (e.g., Writing Review, Test Layout), follow this pattern:

### Container Setup

```css
.full-viewport-page {
    height: 100vh;
    max-height: 100vh;
    display: flex;
    flex-direction: column;
    overflow: hidden; /* Prevent page-level scroll */
}
```

### Resizable Panel Layout

Use `react-resizable-panels` for multi-column layouts with user-adjustable widths:

```jsx
import { PanelGroup, Panel, PanelResizeHandle } from 'react-resizable-panels';

<PanelGroup direction="horizontal">
    <Panel defaultSize={25} minSize={15} maxSize={40}>
        {/* Left column */}
    </Panel>
    <PanelResizeHandle className="resize-handle" />
    <Panel defaultSize={40} minSize={25}>
        {/* Middle column */}
    </Panel>
    <PanelResizeHandle className="resize-handle" />
    <Panel defaultSize={35} minSize={20}>
        {/* Right column */}
    </Panel>
</PanelGroup>
```

### Column Scrolling

Each column should have its own scroll context:

```css
.column-content {
    flex: 1;
    overflow-y: auto;
    min-height: 0; /* Critical for flex overflow to work */
    padding: 0.75rem;
}

/* Custom scrollbar styling */
.column-content::-webkit-scrollbar {
    width: 8px;
}
.column-content::-webkit-scrollbar-track {
    background: #f1f5f9;
    border-radius: 4px;
}
.column-content::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 4px;
}
```

### Resize Handle Styling

```css
.resize-handle {
    background-color: #f3f4f6;
    width: 15px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-left: 1px solid #e5e7eb;
    border-right: 1px solid #e5e7eb;
    position: relative;
    z-index: 10; /* Above panel content */
}

.resize-handle-icon-container {
    width: 28px;
    height: 28px;
    background-color: white;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 11;
}
```

### Collapsible Sections

For sections that can be toggled (e.g., scores bar):

```css
.collapsible-section-toggle {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.5rem 1rem;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    cursor: pointer;
    font-size: 0.85rem;
}
```

### Panel Border Radius

Apply rounded corners to first and last panels:

```css
/* First column (left) */
.first-column {
    border-radius: 12px 0 0 12px;
}

/* Last column (right) */
.last-column {
    border-radius: 0 12px 12px 0;
}

/* Column headers inherit parent radius */
.first-column .column-header {
    border-radius: 11px 0 0 0;
}
.last-column .column-header {
    border-radius: 0 11px 0 0;
}
```
