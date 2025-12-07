# Comprehensive Styling Refactor Plan

**Created:** 2025-12-07  
**Status:** Planned  
**Priority:** Medium  

---

## Goals

1. **Consistency** - Unified design language across all pages
2. **Reusability** - Shared components and CSS variables
3. **Maintainability** - Organized folder structure
4. **Scalability** - Easy to add new features/pages

---

## Phase 1: Design System Foundation

**Create `frontend/src/css/design-system/`**

| File | Purpose |
|------|---------|
| `variables.css` | CSS custom properties (colors, spacing, typography, shadows, etc.) |
| `typography.css` | Font families, sizes, weights, line heights |
| `colors.css` | Color palette with semantic naming (primary, success, error, etc.) |
| `spacing.css` | Margin/padding scale (xs, sm, md, lg, xl) |
| `shadows.css` | Box-shadow presets |
| `animations.css` | Keyframe animations and transitions |
| `breakpoints.css` | Media query mixins/variables |

### Example `variables.css`:

```css
:root {
  /* Primary Colors */
  --color-primary: #7c3aed;
  --color-primary-light: #a78bfa;
  --color-primary-dark: #5b21b6;
  --color-primary-bg: #f5f3ff;
  --color-primary-border: #ede9fe;

  /* Semantic Colors */
  --color-success: #22c55e;
  --color-success-bg: #dcfce7;
  --color-error: #ef4444;
  --color-error-bg: #fee2e2;
  --color-warning: #f59e0b;
  --color-warning-bg: #fef3c7;

  /* Neutrals */
  --color-text-primary: #1f2937;
  --color-text-secondary: #6b7280;
  --color-text-muted: #9ca3af;
  --color-border: #e5e7eb;
  --color-bg-primary: #ffffff;
  --color-bg-secondary: #f9fafb;
  --color-bg-tertiary: #f3f4f6;

  /* Spacing */
  --space-xs: 0.25rem;
  --space-sm: 0.5rem;
  --space-md: 1rem;
  --space-lg: 1.5rem;
  --space-xl: 2rem;
  --space-2xl: 3rem;

  /* Border Radius */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-full: 9999px;

  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px rgba(0, 0, 0, 0.07);
  --shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.1);
  --shadow-purple: 0 4px 20px rgba(102, 97, 241, 0.3);

  /* Transitions */
  --transition-fast: 0.15s ease;
  --transition-normal: 0.2s ease;
  --transition-slow: 0.3s ease;
}
```

---

## Phase 2: Shared Component Styles

**Reorganize `frontend/src/css/common/`**

| File | Purpose |
|------|---------|
| `buttons.css` | All button variants (primary, secondary, danger, ghost, etc.) |
| `inputs.css` | Form inputs, selects, textareas |
| `cards.css` | Card containers with variants |
| `badges.css` | Status badges, tags |
| `modals.css` | Modal dialogs |
| `panels.css` | Resizable panel styles |
| `headers.css` | Page headers (purple glassmorphism, white, etc.) |
| `scrollbars.css` | Custom scrollbar styles |
| `loaders.css` | Loading spinners, skeletons |

### Example `buttons.css`:

```css
/* Base Button */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  padding: 0.4rem 0.75rem;
  border-radius: var(--radius-md);
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all var(--transition-normal);
  border: 1px solid transparent;
  text-decoration: none;
}

/* Primary Button */
.btn-primary {
  background: var(--color-primary);
  color: white;
}

.btn-primary:hover {
  background: var(--color-primary-dark);
  transform: translateY(-1px);
}

/* Secondary Button */
.btn-secondary {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
  border-color: var(--color-border);
}

.btn-secondary:hover {
  background: var(--color-bg-tertiary);
}

/* Ghost Button (for dark backgrounds) */
.btn-ghost {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  border-color: rgba(255, 255, 255, 0.3);
}

.btn-ghost:hover {
  background: rgba(255, 255, 255, 0.25);
}
```

---

## Phase 3: Page-Specific Styles

**Organize by feature in `frontend/src/css/pages/`**

```
pages/
├── dashboard/
│   ├── Dashboard.css
│   └── DashboardCards.css
├── test/
│   ├── TestPage.css
│   ├── TestHeader.css
│   ├── TestFooter.css
│   ├── QuestionGroup.css
│   └── QuestionRenderer.css
├── review/
│   ├── TestReviewPage.css
│   ├── ReviewAnswerColumn.css
│   └── ReviewQuestionRenderer.css
├── writing/
│   ├── WritingTestPage.css
│   └── WritingResultPage.css
├── auth/
│   └── AuthPages.css
└── profile/
    └── ProfilePage.css
```

---

## Phase 4: Import Structure

**Create `frontend/src/css/index.css`** (main entry point)

```css
/* Design System */
@import './design-system/variables.css';
@import './design-system/typography.css';
@import './design-system/colors.css';
@import './design-system/animations.css';

/* Common Components */
@import './common/buttons.css';
@import './common/inputs.css';
@import './common/cards.css';
@import './common/panels.css';
@import './common/scrollbars.css';

/* Base Reset */
@import './base/reset.css';
@import './base/global.css';
```

Then in `main.jsx` or `App.jsx`:

```jsx
import './css/index.css';
```

---

## Phase 5: Migration Steps

### Step 1: Audit Existing Styles
- [ ] Document all CSS files and their usage
- [ ] Identify duplicate patterns
- [ ] List all hardcoded colors/spacing values

### Step 2: Extract Variables
- [ ] Create `design-system/variables.css`
- [ ] Replace hardcoded values with CSS variables
- [ ] Test each page after variable replacement

### Step 3: Create Shared Components
- [ ] Extract button styles to `common/buttons.css`
- [ ] Extract input styles to `common/inputs.css`
- [ ] Extract card styles to `common/cards.css`
- [ ] Extract modal styles to `common/modals.css`

### Step 4: Refactor Page by Page
- [ ] Dashboard pages
- [ ] Test-taking pages (Reading/Listening)
- [ ] Review pages
- [ ] Writing pages
- [ ] Auth pages
- [ ] Profile pages

### Step 5: Test Thoroughly
- [ ] Visual regression testing on all pages
- [ ] Test responsive breakpoints
- [ ] Test dark mode compatibility (if applicable)

### Step 6: Cleanup
- [ ] Remove deprecated/unused CSS files
- [ ] Remove duplicate class definitions
- [ ] Update import statements

---

## Phase 6: Naming Convention

**BEM-inspired with `crm-` prefix:**

```css
/* Block */
.crm-card { }

/* Element */
.crm-card__header { }
.crm-card__body { }
.crm-card__footer { }

/* Modifier */
.crm-card--elevated { }
.crm-card--compact { }
.crm-card--highlighted { }
```

### Naming Examples:

| Component | Class Name |
|-----------|------------|
| Primary button | `.crm-btn`, `.crm-btn--primary` |
| Card header | `.crm-card__header` |
| Input with error | `.crm-input`, `.crm-input--error` |
| Badge success | `.crm-badge`, `.crm-badge--success` |

---

## Estimated Effort

| Phase | Effort | Priority |
|-------|--------|----------|
| Phase 1: Design System | 2-3 hours | High |
| Phase 2: Shared Components | 3-4 hours | High |
| Phase 3: Page Reorganization | 2-3 hours | Medium |
| Phase 4-5: Migration | 4-6 hours | Medium |
| Phase 6: Testing & Cleanup | 2-3 hours | High |
| **Total** | **13-19 hours** | |

---

## Current CSS File Inventory

### Common Styles (`/css/common/`)
- `SidebarLayout.css` - Sidebar and layout
- `TestLayoutBase.css` - Test page layout
- `ReviewLayoutBase.css` - Review page layout

### Component Styles (`/css/`)
- `QuestionGroup.css` - Question grouping
- `QuestionRenderer.css` - Individual questions
- `TestHeader.css` - Test page header
- `TestFooter.css` - Test page footer
- `ToggleSwitch.css` - Toggle component
- `HighlightPopup.css` - Text highlight popup

### Page Styles (`/css/`)
- `TestPage.css` - Reading/Listening test
- `TestReviewPage.css` - Review page
- `WritingTestPage.css` - Writing test
- `WritingResultPage.css` - Writing results

### Review Components (`/css/`)
- `ReviewQuestionRenderer.css`
- `ReviewQuestionGroup.css`
- `ReviewAnswerColumn.css`

---

## Notes

- Consider using CSS Modules or styled-components for component-scoped styles in the future
- Purple glassmorphism header is a key design element - ensure consistency
- The review page aesthetic (clean, card-based, purple accents) should be the baseline

---

## Related Files

- `AGENTS.md` - Repository context for AI agents
- `frontend/README.md` - Frontend documentation
