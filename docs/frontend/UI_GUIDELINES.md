# Cramer UI/UX Design System (v2)

This document outlines the official design language and UI guidelines for the Cramer application. Its purpose is to ensure a consistent, modern, and responsive user experience across the entire platform. This is a living document, intended to be updated as the platform evolves.

## 1. Core Philosophy

Our design is built on these core principles:

-   **Glassmorphism:** The primary visual style. We use blurred, semi-transparent backgrounds to create a sense of depth and hierarchy.
-   **Clarity & Focus:** UI elements are clean and well-spaced to guide the user's attention to what matters most.
-   **Fluidity & Animation:** Interactions are smooth and provide meaningful feedback through subtle, consistent animations.
-   **Responsiveness:** The interface is designed for a desktop-first experience and gracefully adapts to smaller screens.

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

-   **Container Padding:** `1.5rem` (24px) on left and right.
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
