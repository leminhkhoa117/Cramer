# SPEC-F00 — Frontend Design System (Source of Truth)

> Status: **Authoritative** · Scope: the entire React frontend rebuild.
> Goal: one unified, dense, consistent, AI-slop-free system. Code that violates this spec is wrong.

---

## 0. Principles

1. **One system.** Tailwind 4 (`@theme` tokens) + a small set of React **primitives** in `src/ui/`.
   No Bootstrap. No per-page CSS reinvention. Custom CSS only for true globals + a few hard cases.
2. **Density first.** Default body text is **14px**, not 16px. Section rhythm is tight. Heroes are
   content-driven, never `min-height: 100vh`. More content per screen; less dead whitespace.
3. **Scale discipline.** Every spacing/size/color/radius value comes from a token/scale. **No magic
   numbers** (`0.9rem`, `1.75rem`, `7rem` are banned). If it's not on the scale, it's wrong.
4. **Restraint over slop.** Keep Cramer's violet brand, but apply it intentionally — solid surfaces,
   subtle borders, one reserved signature gradient for brand moments. No gradient-on-white spam.
5. **Primitives over classes.** Repeated UI (button/card/input/modal/…) is a React component with
   variant props, not a new BEM class per page.

---

## 1. Framework strategy

- **Tailwind 4** is the styling engine. Tokens live in `src/styles/theme.css` via `@theme`.
- **`src/ui/`** holds primitives (Button, Card, …) built with Tailwind classes + `cn()` (clsx).
- **`src/styles/globals.css`** holds: preflight tweaks, font-face, base element styles, a handful of
  utilities (`.glass`, scrollbar, `.cr-prose` for passages), and consolidated `@keyframes`.
- **DROP**: `bootstrap`, `react-bootstrap`, the ~50 `src/css/**` files + `-shards/` dirs,
  `scripts/split-large-css.mjs`, the 13 legacy token-alias layers, the separate admin token file.
- Class merge helper: `cn(...classes)` in `src/lib/cn.js` (clsx-style).

---

## 2. Color tokens (`@theme`)

Brand violet (primary) + indigo accent + restrained cyan; slate neutrals; semantic set.

```
--color-brand-50:#f5f3ff  --color-brand-100:#ede9fe  --color-brand-200:#ddd6fe
--color-brand-300:#c4b5fd  --color-brand-400:#a78bfa  --color-brand-500:#8b5cf6
--color-brand-600:#7c3aed  (PRIMARY)  --color-brand-700:#6d28d9 (hover)
--color-brand-800:#5b21b6  --color-brand-900:#4c1d95
--color-accent:#6366f1 (indigo)   --color-cyan:#27afdb (sparing)
Neutrals (slate): --color-ink:#0f172a (text strong) --color-ink-2:#334155 --color-muted:#64748b
--color-faint:#94a3b8  --color-line:#e2e8f0 (border) --color-line-2:#eef2f6
--color-surface:#ffffff  --color-surface-2:#f8f7fc  --color-page:#faf9ff (lavender-tinted)
Semantic: --color-success:#10b981 --color-warning:#f59e0b --color-danger:#ef4444 --color-info:#0ea5e9
(each also -soft bg: success-soft #ecfdf5, warning-soft #fffbeb, danger-soft #fef2f2, info-soft #eff6ff,
 brand-soft #f5f3ff)
```
Rules: text = ink/ink-2/muted; borders = line; page bg = page; cards = surface. Accent = brand-600.
Gradient reserved: `--gradient-brand: linear-gradient(135deg,#7c3aed,#6366f1)` — hero/brand only.

## 3. Spacing scale (4px base) — Tailwind default, DENSE usage

`0,1=4,2=8,3=12,4=16,5=20,6=24,8=32,10=40,12=48,16=64,20=80,24=96`.
Usage caps (enforced): section vertical padding ≤ **64px** desktop / **40px** mobile; card padding
16–24px; control gap 8–12px; page gutter 16–24px. Never 80–128px section padding.

## 4. Typography

Font: **Quicksand** (brand) for UI + headings; weights 400/500/600/700; default **500**.
Type scale (px / line-height), dense:
```
text-xs 12/16  text-sm 13/18  text-base 14/20 (DEFAULT)  text-md 16/24
text-lg 18/26  text-xl 20/28  text-2xl 24/32  text-3xl 30/38  text-4xl 36/44
display clamp(2.25rem,5vw,3.5rem)/1.08  (hero only)
```
Headings: weight 600–700, color ink, tracking -0.01em. Body: weight 500, color ink-2. Muted meta: muted.

## 5. Radii / shadows / z / breakpoints / motion

- Radii: `sm 6, md 8, lg 12, xl 16, 2xl 20, full 9999`. Default control/card radius = lg(12).
- Shadows (violet-tinted, soft): `xs 0 1px 2px rgba(15,23,42,.06); sm 0 2px 6px rgba(15,23,42,.07);
  md 0 4px 14px rgba(15,23,42,.08); lg 0 10px 30px rgba(15,23,42,.10); xl 0 20px 48px rgba(15,23,42,.14)`.
  Focus ring: `0 0 0 3px rgba(124,58,237,.35)`.
- Z: content 1, dropdown 100, header 1020, drawer 1030, backdrop 1040, modal 1050, popover 1060, toast 1080.
- Breakpoints: Tailwind defaults `sm 640, md 768, lg 1024, xl 1280, 2xl 1536`. Stop inventing 992/1200/1400.
- Glass tiers: `.glass` (surface .92 + blur 14 + line border + sm shadow), `.glass-strong` (blur 20),
  `.glass-dark` (modal/overlay). Use sparingly for sidebars/sticky/overlays — not every card.
- Motion: framer-motion for page/element reveals (staggered, ≤300ms); Tailwind transitions for hover/
  focus (150ms). One consolidated `@keyframes` set in globals (spin, fadeIn, slideUp, pulse, shimmer).

## 6. Primitives (`src/ui/`) — the only way to build these

Button(variant: primary|secondary|outline|ghost|danger|link; size sm|md|lg; loading; iconLeft/iconRight;
fullWidth) · IconButton · Card(+Header/Body/Footer; variant solid|glass|outline; interactive) · Input ·
Textarea · Select · Checkbox · Radio · Switch · Badge(variant; size) · Chip · Alert(info|success|warning|
danger; title; dismissible) · Modal(size sm|md|lg|xl; title; footer; closeOnBackdrop — wraps the old
BaseModal behavior: portal + framer-motion) · ConfirmDialog · Tabs(variant underline|pill) · Pagination ·
Spinner · Skeleton · EmptyState(icon,title,description,action) · Avatar · Tooltip · Dropdown/Menu ·
Progress · Stat/StatCard · Toast + Toaster(replaces utils/toast).
Layout: Page · Container(max-w 1200, gutter) · PageHeader(title,subtitle,actions,breadcrumbs) ·
Section · SidebarLayout(replaces `.sl-*`: sticky sidebar 280–300px + content).
Control sizing: sm 32px, md 38px(default), lg 44px height. Inputs: 38px, radius lg, line border, focus ring.

## 7. Conventions

- Tailwind utilities for layout/spacing/color; primitives for repeated patterns; `cn()` to merge.
- Icons: `react-icons` (Fi/Lu set preferred for consistency).
- Vietnamese copy preserved (audience). Keep brand terms: Lúa, Cramerie/Cramerich/Cramerous.
- A11y: focus-visible rings on all interactives; modals trap focus + Esc close; buttons have aria-label
  when icon-only; color-contrast ≥ 4.5 for text.
- Files: primitives `src/ui/<Name>.jsx` + barrel `src/ui/index.js`. Data layer `src/lib/api/*`. Keep
  `src/pages/`, `src/components/` (feature components), `src/stores/`.

## 8. Density / anti-slop checklist (every screen must pass)

- [ ] No `min-height:100vh` hero; hero ≤ ~80vh and content-driven.
- [ ] Section padding ≤ 64px desktop. No 5–8rem gaps.
- [ ] Body text 14px; headings from the scale; no random font sizes.
- [ ] All spacing from the 4px scale; no magic numbers.
- [ ] One button/card/input system; no page-local button/card classes.
- [ ] Accent used sparingly; surfaces are solid; ≤1 gradient per view.
- [ ] Lists/tables are dense (rows 44–52px); more content per screen.
- [ ] Consistent radius (lg default) + shadow scale; no hardcoded shadows.

## 9. Change log
| Date | Change |
|------|--------|
| 13/06/2026 | Initial authoring (target system). |
