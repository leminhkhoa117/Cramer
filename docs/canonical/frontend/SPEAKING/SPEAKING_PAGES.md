# Speaking Pages

> **Version:** 1.0
> **Last Updated:** 17/05/2026

This document specifies the two Speaking pages and their integration into the existing page structure.

---

## 1. SpeakingSessionPage

**Route:** `/speaking/session/:sessionMode`
**BEM prefix:** `speaking-*`
**CSS:** `css/speaking/speaking-session.css`, `css/speaking/speaking-live.css`

### State machine

The page follows a state machine with these phases:

```
INIT → PRE_BRIEF → CREATING_SESSION → LIVE_SESSION → PROCESSING → RESULTS
```

| Phase | Component | Description |
|-------|-----------|-------------|
| `INIT` | Inline loading | Parse route params, prepare local data |
| `PRE_BRIEF` | `PreBriefScreen` | Request mic permission, choose accent/speed, show test info |
| `CREATING_SESSION` | Inline loading | Call `POST /api/speaking/sessions` |
| `LIVE_SESSION` | `GeminiLiveSessionLayout` | Main test flow with WebSocket + audio |
| `PROCESSING` | `ProcessingScreen` | Wait for grading to complete |
| `RESULTS` | Redirect | Navigate to `/speaking/results/{sessionId}` |

### Store integration

```js
import { useSpeakingStore } from '../../stores';
const phase = useSpeakingStore(state => state.phase);
const sessionId = useSpeakingStore(state => state.sessionId);
```

### Route registration

```jsx
// In router.jsx — protected route
{
  path: '/speaking/session/:sessionMode',
  element: <ProtectedRoute><SpeakingSessionPage /></ProtectedRoute>,
}
```

### RootLayout considerations

- During `LIVE_SESSION` phase, Header and Footer should be hidden (like test pages)
- Add to the conditional rendering check in RootLayout

---

## 2. SpeakingResultsPage

**Route:** `/speaking/results/:sessionId`
**BEM prefix:** `speaking-*`
**CSS:** `css/speaking/speaking-results.css`

### States

| State | Behaviour |
|-------|-----------|
| Loading | Show `FullPageLoader` while fetching results |
| Grading in progress | Show `ProcessingScreen` with polling |
| Graded | Show band scores, criteria feedback, improvement tips |
| Grading failed | Show error message with retry option |
| Not found | Show error message with link back to courses |

### Display sections

1. **Overall band** — Large badge with band score (color-coded: 9=green → 0=red)
2. **4 criteria** — Fluency & Coherence, Lexical Resource, Grammatical Range & Accuracy, Pronunciation — each with band score + feedback + highlights
3. **Per-part feedback** — Part 1/2/3 specific comments
4. **Improvement tips** — Bulleted list of actionable advice

### Polling

- Poll `GET /api/speaking/sessions/{id}/grading-status` every 3-5 seconds
- Backoff: 3→5→8→13→15s with ±15% jitter
- Hard timeout: 5 minutes → show "still grading" message
- Pause polling when `document.visibilitychange === 'hidden'`

### Route registration

```jsx
// In router.jsx — protected route
{
  path: '/speaking/results/:sessionId',
  element: <ProtectedRoute><SpeakingResultsPage /></ProtectedRoute>,
}
```
