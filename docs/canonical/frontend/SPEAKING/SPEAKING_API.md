# Speaking API Module

> **Version:** 1.0
> **Last Updated:** 17/05/2026
> **Pattern Reference:** `PATTERNS/API_CLIENT_PATTERNS.md`

This document specifies the `speakingApi` module to be added to `backendApi.js`.

---

## Module Definition

```js
export const speakingApi = {
  // Sessions
  createSession: (data) => api.post('/speaking/sessions', data),
  getSession: (id) => api.get(`/speaking/sessions/${id}`),

  // Transcripts
  saveTranscript: (id, data) =>
    api.post(`/speaking/sessions/${id}/transcripts`, data),

  // Lifecycle
  completeSession: (id) => api.post(`/speaking/sessions/${id}/complete`),
  abandonSession: (id) => api.post(`/speaking/sessions/${id}/abandon`),

  // Grading
  getGradingStatus: (id) => api.get(`/speaking/sessions/${id}/grading-status`),
  getResults: (id) => api.get(`/speaking/sessions/${id}/results`),

  // History
  getHistory: (params) => api.get('/speaking/history', { params }),
};
```

---

## Endpoint Reference

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/speaking/sessions` | Create new session |
| GET | `/speaking/sessions/{id}` | Get session info |
| POST | `/speaking/sessions/{id}/transcripts` | Save a transcript turn |
| POST | `/speaking/sessions/{id}/complete` | Submit and trigger grading |
| POST | `/speaking/sessions/{id}/abandon` | Abandon session (no credit charge) |
| GET | `/speaking/sessions/{id}/grading-status` | Poll grading status |
| GET | `/speaking/sessions/{id}/results` | Get detailed results |
| GET | `/speaking/history` | Get user's session history |

---

## Key DTOs

### CreateSpeakingSessionDTO (request)

```json
{
  "sessionMode": "FULL",
  "testId": 123,
  "accent": "british",
  "speed": 1.0
}
```

### SaveSpeakingTranscriptDTO (request)

```json
{
  "sourceQuestionId": 501,
  "partNumber": 1,
  "turnIndex": 3,
  "questionSnapshot": { "schemaVersion": 1, "partType": "PART_1", "promptText": "..." },
  "audioStoragePath": "user-id/session-id/turn-003.webm",
  "transcriptText": "I think...",
  "audioDurationSeconds": 45,
  "transcriptConfidence": 0.91
}
```

### SpeakingSessionDTO (response)

```json
{
  "sessionId": 42,
  "sessionMode": "FULL",
  "testId": 123,
  "status": "in_progress",
  "isFinalized": false,
  "luaCost": 15,
  "accent": "british",
  "speed": 1.0,
  "startedAt": "2026-03-08T10:30:00Z",
  "sessionBlueprint": { ... },
  "turns": [ ... ]
}
```
