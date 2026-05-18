# Speaking Feature — Frontend Architecture

> **Version:** 1.0
> **Last Updated:** 17/05/2026
> **Status:** Planned (Issues #18, #19)

This document describes how the Speaking feature fits into the Cramer frontend architecture. It references the generic patterns in `PATTERNS/` and adds Speaking-specific details.

---

## Overview

The Speaking feature adds a new skill to the platform. Users can take an IELTS Speaking test with an AI examiner, receive real-time audio interaction, and get graded results.

### What's needed

| Layer | Files | Pattern Reference |
|-------|-------|-------------------|
| Routes | 2 new routes in `router.jsx` | `PATTERNS/ROUTE_PATTERNS.md` |
| Store | `useSpeakingStore.js` | `PATTERNS/STORE_PATTERNS.md` |
| API client | `speakingApi` module in `backendApi.js` | `PATTERNS/API_CLIENT_PATTERNS.md` |
| Pages | `SpeakingSessionPage`, `SpeakingResultsPage` | `PATTERNS/PAGE_PATTERNS.md` |
| Components | 6+ new components | `PATTERNS/COMPONENT_PATTERNS.md` |
| Hooks | `useGeminiLive`, `useAudioRecorder`, `useTimer` | Custom hooks |
| CSS | `css/speaking/` directory | `PATTERNS/CSS_PATTERNS.md` |

### Integration points

1. **CourseDetailPage** — Add Speaking to the skills list, open `SpeakingPartModal` on click
2. **Router** — Add `/speaking/session/:sessionMode` and `/speaking/results/:sessionId`
3. **RootLayout** — Add Speaking session pages to the conditional rendering (hide Header/Footer during session)
4. **Stores index** — Export `useSpeakingStore`
5. **Backend API** — All 8 REST endpoints + WebSocket already built

### BEM prefix

Speaking uses the prefix `speaking-*` for all CSS classes.

---

## File Map

```
frontend/src/
├── api/
│   └── backendApi.js          # + speakingApi module
├── stores/
│   ├── index.js               # + useSpeakingStore export
│   └── useSpeakingStore.js    # NEW
├── pages/
│   └── speaking/
│       ├── SpeakingSessionPage.jsx   # NEW
│       └── SpeakingResultsPage.jsx   # NEW
├── components/
│   ├── SpeakingPartModal.jsx         # NEW
│   └── speaking/
│       ├── GeminiLiveSessionLayout.jsx  # NEW
│       ├── PreBriefScreen.jsx           # NEW
│       ├── Part2PrepLayout.jsx          # NEW
│       ├── ProcessingScreen.jsx         # NEW
│       └── ExaminerWaveform.jsx         # NEW
├── hooks/
│   ├── useGeminiLive.js       # NEW
│   ├── useAudioRecorder.js    # NEW
│   └── useTimer.js            # NEW
├── css/
│   └── speaking/
│       ├── speaking-session.css    # NEW
│       ├── speaking-results.css    # NEW
│       ├── speaking-live.css       # NEW
│       ├── speaking-components.css # NEW
│       └── examiner-waveform.css   # NEW
└── router.jsx                 # + 2 new routes
```
