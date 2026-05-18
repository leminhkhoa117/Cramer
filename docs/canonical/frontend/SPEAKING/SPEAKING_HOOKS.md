# Speaking Hooks

> **Version:** 1.0
> **Last Updated:** 17/05/2026

This document specifies the custom hooks needed for the Speaking feature.

---

## 1. useGeminiLive

**File:** `src/hooks/useGeminiLive.js`

### Purpose

Manages the WebSocket connection to the backend's `/ws/speaking/{sessionId}` endpoint, which bridges to Gemini Live API.

### API

```js
const {
  isConnected,           // boolean — WebSocket connected
  isReady,               // boolean — Gemini setup complete
  isExaminerSpeaking,    // boolean — AI is speaking
  examinerTranscript,    // string — real-time examiner text
  userTranscript,        // string — real-time user text (STT)
  error,                 // string | null

  connect,               // () => void — open WS connection
  disconnect,            // () => void — close WS connection
  sendStartQuestion,     // (turnIndex) => void — start a turn
  sendEndTurn,           // () => void — end current turn
  sendAudioChunk,        // (blob) => void — send audio chunk
} = useGeminiLive(sessionId, {
  onExaminerAudio: (audioData) => { /* play audio */ },
  onExaminerSpeaking: (isSpeaking) => { /* toggle mic */ },
  onTranscript: (text, source) => { /* update UI */ },
  onTurnComplete: () => { /* advance question */ },
  onError: (error) => { /* handle */ },
  onFallback: () => { /* switch to text mode */ },
});
```

### Internals

- Connects to `ws[s]://host/ws/speaking/{sessionId}`
- Handles message types: `status`, `examiner_audio` (binary), `transcript`, `examiner_speaking`, `turn_complete`, `error`
- Forwards binary audio chunks from mic
- Auto-reconnect on unexpected disconnect (max 3 retries)
- Falls back to text mode if Gemini Live unavailable

---

## 2. useAudioRecorder

**File:** `src/hooks/useAudioRecorder.js`

### Purpose

Manages microphone recording using the `MediaRecorder` API.

### API

```js
const {
  isRecording,           // boolean
  audioBlob,             // Blob | null — final blob after stop
  startRecording,        // () => void
  stopRecording,         // () => Promise<Blob>
  onDataAvailable,       // callback for streaming chunks
} = useAudioRecorder();
```

### Internals

- Uses `MediaRecorder` API
- Format: `audio/webm;codecs=opus` (with fallback for Safari: `audio/mp4`)
- `timeslice` param: 250ms chunks for streaming
- Constraints: `{ echoCancellation: true, noiseSuppression: true, autoGainControl: true, channelCount: 1, sampleRate: 16000 }`
- Runtime probe: `MediaRecorder.isTypeSupported()` to choose best format

---

## 3. useTimer

**File:** `src/hooks/useTimer.js`

### Purpose

Countdown timer for Part 2 preparation and turn time limits.

### API

```js
const {
  timeLeft,       // number — seconds remaining
  isRunning,      // boolean
  isExpired,      // boolean
  start,          // () => void
  pause,          // () => void
  reset,          // () => void
} = useTimer(durationSeconds);
```

### Usage

- Part 2 preparation: `useTimer(60)` — 1 minute prep
- Part 2 speaking: `useTimer(120)` — 2 minutes talk time
- TimeWarningBadge: reads `timeLeft` to show warnings at < 30s
