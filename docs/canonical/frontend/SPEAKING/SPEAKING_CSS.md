# Speaking CSS

> **Version:** 1.0
> **Last Updated:** 17/05/2026
> **Pattern Reference:** `PATTERNS/CSS_PATTERNS.md`

This document specifies the CSS files needed for the Speaking feature.

---

## File Structure

```
frontend/src/css/speaking/
├── speaking-session.css       # Session page layout
├── speaking-results.css       # Results page styles
├── speaking-live.css          # Live session layout
├── speaking-components.css    # Shared component styles
└── examiner-waveform.css      # Waveform animation
```

---

## BEM Prefix

All Speaking CSS uses the prefix `speaking-*`.

### Class naming examples

```css
.speaking-page              /* Page root */
.speaking-session           /* Session container */
.speaking-session__header   /* Session header */
.speaking-session__content  /* Main content area */
.speaking-turn              /* Single turn display */
.speaking-turn--active      /* Active turn modifier */
.speaking-mic-btn           /* Microphone button */
.speaking-mic-btn--recording /* Recording state */
.speaking-mic-btn--disabled  /* Disabled state */
.speaking-timer             /* Timer display */
.speaking-timer--warning    /* < 30 seconds */
.speaking-timer--critical   /* < 10 seconds */
.speaking-waveform          /* Examiner waveform */
.speaking-waveform--active  /* Waveform animating */
.speaking-transcript        /* Real-time transcript */
.speaking-transcript--examiner /* Examiner text */
.speaking-transcript--user  /* User text */
.speaking-results           /* Results page */
.speaking-results__band     /* Overall band badge */
.speaking-results__criteria /* Criteria section */
.speaking-results__feedback /* Feedback section */
```

---

## Design Tokens

Speaking should define page-scoped tokens on the `.speaking-page` class:

```css
.speaking-page {
  --speaking-primary: var(--cr-primary);
  --speaking-bg: var(--cr-page-bg);
  --speaking-card-bg: var(--cr-card-bg);
  --speaking-glass-border: var(--cr-glass-border);
  --speaking-success: var(--cr-success);
  --speaking-warning: var(--cr-warning);
  --speaking-danger: var(--cr-danger);
}
```

---

## Key Patterns

### Session layout

The live session should use a full-viewport layout similar to test pages:

```css
.speaking-session {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
```

### Results layout

The results page should follow the standard page pattern with hero + content sections.

### Waveform animation

```css
@keyframes speaking-wave-pulse {
  0%, 100% { transform: scaleY(0.5); }
  50% { transform: scaleY(1); }
}

.speaking-waveform--active .speaking-waveform__bar {
  animation: speaking-wave-pulse 0.5s ease-in-out infinite;
}
```

### Mic button states

```css
.speaking-mic-btn {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--cr-primary-gradient);
  color: white;
  transition: all 0.3s ease;
}

.speaking-mic-btn--recording {
  background: var(--cr-danger);
  animation: pulse 1.5s ease-in-out infinite;
}

.speaking-mic-btn--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
```

---

## Import Pattern

```jsx
// SpeakingSessionPage
import '../css/speaking/speaking-session.css';
import '../css/speaking/speaking-live.css';
import '../css/speaking/speaking-components.css';
import '../css/speaking/examiner-waveform.css';

// SpeakingResultsPage
import '../css/speaking/speaking-results.css';
import '../css/speaking/speaking-components.css';
```
