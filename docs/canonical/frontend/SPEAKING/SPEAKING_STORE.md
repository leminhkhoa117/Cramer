# Speaking Store

> **Version:** 1.0
> **Last Updated:** 17/05/2026
> **Pattern Reference:** `PATTERNS/STORE_PATTERNS.md`

This document specifies the `useSpeakingStore` Zustand store.

---

## State Shape

```js
{
  // Session
  sessionId: null,           // number | null
  sessionMode: null,         // 'FULL' | 'PART_1' | 'PART_2' | 'PART_3' | null
  testId: null,              // number | null
  sessionBlueprint: null,    // object | null (from backend)
  turns: [],                 // flattened turns from blueprint
  currentTurnIndex: null,    // number | null

  // Phase
  phase: 'idle',             // 'idle' | 'init' | 'pre_brief' | 'creating_session' | 'live' | 'processing' | 'completed'

  // Config
  accent: 'neutral',         // 'british' | 'american' | 'australian' | 'neutral'
  speed: 1.0,                // 0.85 | 1.00 | 1.15

  // Transcript data
  transcripts: {},           // { [turnIndex]: { partNumber, sourceQuestionId, questionSnapshot, transcriptText, audioStoragePath, audioDurationSeconds, transcriptConfidence } }
  currentTranscript: '',     // live transcript text

  // Results
  results: null,             // SpeakingResultDTO | null
  gradingStatus: null,       // 'completed' | 'grading' | 'graded' | 'grading_failed' | null

  // UI
  loading: false,
  error: null,
}
```

---

## Actions

### Session lifecycle

| Action | Description |
|--------|-------------|
| `prepareSession(sessionMode, testId)` | Parse route params, set initial state |
| `createSession({ accent, speed })` | `POST /api/speaking/sessions` |
| `startTurn(turnIndex)` | Advance to specified turn |
| `saveTranscript(data)` | `POST /api/speaking/sessions/{id}/transcripts` |
| `completeSession()` | `POST /api/speaking/sessions/{id}/complete` |
| `abandonSession()` | `POST /api/speaking/sessions/{id}/abandon` |

### Results

| Action | Description |
|--------|-------------|
| `pollGradingStatus()` | `GET /api/speaking/sessions/{id}/grading-status` |
| `getResults()` | `GET /api/speaking/sessions/{id}/results` |

### Utility

| Action | Description |
|--------|-------------|
| `reset()` | Clear all state |
| `setAccent(accent)` | Update accent |
| `setSpeed(speed)` | Update speed |

---

## Middleware

- `devtools` — for debugging (all stores use this)
- `persist` — with `sessionStorage`, partialize `{ sessionId, phase, sessionMode, testId }` only (exclude transcripts/audio blobs)

---

## API Layer

All API calls go through `speakingApi` module in `backendApi.js`:

```js
import { speakingApi } from '../api/backendApi';
```

---

## Selectors

```js
export const selectSessionId = (state) => state.sessionId;
export const selectPhase = (state) => state.phase;
export const selectCurrentTurn = (state) =>
  state.turns.find(t => t.turnIndex === state.currentTurnIndex);
export const selectIsInSession = (state) =>
  ['pre_brief', 'creating_session', 'live', 'processing'].includes(state.phase);
```
