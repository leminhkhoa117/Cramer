# Speaking Components

> **Version:** 1.0
> **Last Updated:** 17/05/2026

This document specifies the Speaking-specific components and their patterns.

---

## 1. SpeakingPartModal (entry point)

**File:** `src/components/SpeakingPartModal.jsx`
**Pattern:** Specialized modal wrapping `BaseModal` (see `PATTERNS/COMPONENT_PATTERNS.md`)

### Props

| Prop | Type | Description |
|------|------|-------------|
| `isOpen` | bool | Modal visibility |
| `onClose` | func | Close handler |
| `testId` | number | Selected test ID |
| `testNumber` | number | Test number for display |
| `testSetCode` | string | Test set code |

### Content

- Title: "Chọn chế độ thi Speaking"
- Mode options: Full Test, Part 1, Part 2, Part 3
- Each mode shows estimated duration and question count
- Disabled modes show reason (e.g., "Part 3 cần hoàn thành Part 2 trước")
- On select: navigate to `/speaking/session/{mode}?testId={testId}`

### Integration

- Opened from `CourseDetailPage` when user clicks Speaking skill
- Receives `testId` from the course data

---

## 2. PreBriefScreen

**File:** `src/components/speaking/PreBriefScreen.jsx`

### Content

- Test info display (name, mode, estimated duration)
- Microphone permission request (`navigator.mediaDevices.getUserMedia`)
- Accent selector: British, American, Australian, Neutral
- Speed selector: Slow (0.85), Normal (1.00), Fast (1.15)
- "Bắt đầu buổi thi" button (disabled until mic permission granted)
- Warning if mic permission denied

### State

- Reads/writes `accent` and `speed` from `useSpeakingStore`
- Persists preferences to `localStorage` (keys: `speaking_setup_accent`, `speaking_setup_speed`)
- Optionally mirrors to `profiles.preferred_accent`, `profiles.preferred_speed`

---

## 3. GeminiLiveSessionLayout

**File:** `src/components/speaking/GeminiLiveSessionLayout.jsx`

### Content

- Current question display (text, always visible)
- Examiner audio player (auto-play when audio received from WS)
- `ExaminerWaveform` — visualization when AI is speaking
- Real-time transcript (updates from WS)
- Microphone button (lit = recording, dim = idle)
- `TimeWarningBadge` — warning when time is low
- "Next" / "Submit" buttons

### Turn flow

1. Show question from `sessionBlueprint`
2. Examiner speaks (audio from WS) → waveform animates
3. Examiner stops → mic enables → user speaks
4. User stops → audio uploaded to storage
5. Transcript saved via `POST /transcripts`
6. Advance to next turn

---

## 4. Part2PrepLayout

**File:** `src/components/speaking/Part2PrepLayout.jsx`

### Content

- Cue card display with bullet points
- 60-second countdown timer
- "Bắt đầu nói" button (or auto-advance when timer expires)
- 3-2-1 countdown overlay when time is up

---

## 5. ProcessingScreen

**File:** `src/components/speaking/ProcessingScreen.jsx`

### Content

- Loading animation (reuse `FullPageLoader` pattern)
- Progress steps: "Đang upload audio..." → "Đang phân tích..." → "Đang chấm điểm..."
- Auto-redirect to results when `gradingStatus === 'graded'`
- Retry button if grading fails

---

## 6. ExaminerWaveform

**File:** `src/components/speaking/ExaminerWaveform.jsx`

### Behaviour

- Animated waveform bars when examiner is speaking
- Hides/stops when `examiner_speaking = false`
- CSS animation in `css/speaking/examiner-waveform.css`

---

## 7. TimeWarningBadge

**File:** `src/components/speaking/TimeWarningBadge.jsx`

### Behaviour

- Shows warning badge when < 30 seconds remaining
- Uses `useTimer` hook
- Color transitions: normal → yellow → red
