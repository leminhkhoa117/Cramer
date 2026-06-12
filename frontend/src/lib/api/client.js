import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

/** Shared axios instance. */
export const http = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

// Token provider (set by useAuthStore on session change). JWT only — no X-User-Id.
let getAuthToken = () => null;
let onUnauthorized = null;

/** Wire the bearer-token provider (called from useAuthStore). */
export function setupApiClient(provider) {
  getAuthToken = provider || (() => null);
}

/** Register a callback invoked on any 401 (e.g. force sign-out). */
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn;
}

http.interceptors.request.use((config) => {
  const token = getAuthToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof onUnauthorized === 'function') {
      onUnauthorized();
    }
    return Promise.reject(error);
  }
);

/**
 * Normalize a backend error (ApiError shape, SPEC-04 §2) into a flat object.
 * @returns {{status:number, message:string, error:string, blockType:string|null, fieldErrors:object|null, raw:any}}
 */
export function getApiError(error) {
  const res = error?.response;
  const data = res?.data || {};
  return {
    status: res?.status ?? 0,
    error: data.error || (res ? `HTTP ${res.status}` : 'Network error'),
    message: data.message || error?.message || 'Đã có lỗi xảy ra. Vui lòng thử lại.',
    blockType: data.blockType ?? null,
    fieldErrors: data.fieldErrors ?? null,
    raw: data,
  };
}

// Thin verb helpers that unwrap `.data`.
export const get = (url, config) => http.get(url, config).then((r) => r.data);
export const post = (url, body, config) => http.post(url, body, config).then((r) => r.data);
export const put = (url, body, config) => http.put(url, body, config).then((r) => r.data);
export const patch = (url, body, config) => http.patch(url, body, config).then((r) => r.data);
export const del = (url, config) => http.delete(url, config).then((r) => r.data);

export default http;
