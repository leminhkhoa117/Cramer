import { get, post } from './client';
import { http } from './client';

/* ABTS — AI generation (SPEC-20..25). Admin-gated under /api/admin/abts. */

export const abtsApi = {
  generate: (skill, body) => post(`/admin/abts/generate/${skill}`, body),
  generateQuestions: (skill, body) => post('/admin/abts/generate/questions', body, { params: { skill } }),
  validate: (skill, content, { part = 1, taskType } = {}) =>
    post('/admin/abts/validate', content, { params: { skill, part, taskType } }),
  applyRefinement: (body) => post('/admin/abts/refine/apply', body),
  save: (body) => post('/admin/abts/save', body),
  models: () => get('/admin/abts/models'),
  modelCapabilities: (id) => get(`/admin/abts/models/capabilities/${id}`),
  templates: () => get('/admin/abts/templates'),
  templatesByCategory: (categoryId) => get(`/admin/abts/templates/${categoryId}`),
  status: () => get('/admin/abts/status'),
};

/**
 * Open an SSE stream (generation or refinement) via fetch + ReadableStream.
 * Calls onEvent(parsedJson) for each `data:` line. Returns an abort function.
 * Token is taken from the provided getToken() (pass () => session.access_token).
 */
export function openAbtsStream(path, body, { getToken, onEvent, onError, onDone } = {}) {
  const controller = new AbortController();
  const base = http.defaults.baseURL || '/api';
  const token = getToken?.();

  (async () => {
    try {
      const res = await fetch(`${base}${path}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(body),
        signal: controller.signal,
      });
      if (!res.ok || !res.body) {
        onError?.(new Error(`Stream failed: HTTP ${res.status}`));
        return;
      }
      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      for (;;) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const frames = buffer.split('\n\n');
        buffer = frames.pop() || '';
        for (const frame of frames) {
          const line = frame.split('\n').find((l) => l.startsWith('data:'));
          if (!line) continue;
          const json = line.slice(5).trim();
          if (!json || json === '[DONE]') continue;
          try { onEvent?.(JSON.parse(json)); } catch { /* ignore partial */ }
        }
      }
      onDone?.();
    } catch (err) {
      if (err?.name !== 'AbortError') onError?.(err);
    }
  })();

  return () => controller.abort();
}
