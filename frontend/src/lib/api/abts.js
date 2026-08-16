import { get, post, currentAuthToken } from './client';
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
 * Parses `data:` frames and calls onEvent(parsedJson) for each.
 *
 * Guarantees: onEvent is never called after the stream closes; exactly one of
 * {terminal event, onError, onDone} finishes a stream. Abort (via the returned
 * function or an AbortError) triggers onError with an AbortError instance.
 *
 * @param {string} path SSE endpoint path (e.g. /admin/abts/generate/reading/stream)
 * @param {object} body JSON request body
 * @param {object} [opts]
 * @param {() => string|null} [opts.getToken] bearer-token provider (defaults to the shared client token)
 * @param {(event: object) => void} [opts.onEvent] one call per parsed SSE data frame
 * @param {(error: Error) => void} [opts.onError] transport/HTTP/abort errors
 * @param {() => void} [opts.onDone] called once when the stream closes cleanly (no terminal event)
 * @returns {() => void} abort function
 */
export function openAbtsStream(path, body, { getToken, onEvent, onError, onDone } = {}) {
  const controller = new AbortController();
  const base = http.defaults.baseURL || '/api';
  const token = (getToken ?? currentAuthToken)?.();
  let closed = false;

  const finish = (fn) => {
    if (closed) return;
    closed = true;
    fn?.();
  };

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
        const text = await res.text().catch(() => '');
        finish(() => onError?.(new Error(`Stream failed: HTTP ${res.status} ${text.slice(0, 200)}`)));
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
          try {
            onEvent?.(JSON.parse(json));
          } catch {
            /* ignore malformed frame */
          }
        }
      }
      finish(onDone);
    } catch (err) {
      finish(() => onError?.(err?.name === 'AbortError' ? err : (err instanceof Error ? err : new Error(String(err)))));
    }
  })();

  return () => controller.abort();
}
