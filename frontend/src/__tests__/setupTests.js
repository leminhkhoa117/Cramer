/**
 * Vitest Test Setup
 * 
 * This file runs before each test file.
 * Sets up testing-library, mocks, and global test utilities.
 * 
 * @author Cramer Test Team
 * @since 2026-01-11
 */

import { expect, afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import * as matchers from '@testing-library/jest-dom/matchers';

// Extend Vitest's expect with testing-library matchers
expect.extend(matchers);

// Cleanup after each test to prevent memory leaks
afterEach(() => {
  cleanup();
});

// =============================================================================
// MOCK: window.matchMedia (required for responsive components)
// =============================================================================
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(), // deprecated
    removeListener: vi.fn(), // deprecated
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// =============================================================================
// MOCK: ResizeObserver (required for some UI components)
// =============================================================================
global.ResizeObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}));

// =============================================================================
// MOCK: IntersectionObserver (required for lazy loading)
// =============================================================================
global.IntersectionObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}));

// =============================================================================
// MOCK: Supabase Client
// =============================================================================
vi.mock('@supabase/supabase-js', () => ({
  createClient: vi.fn(() => ({
    auth: {
      getSession: vi.fn().mockResolvedValue({ data: { session: null }, error: null }),
      getUser: vi.fn().mockResolvedValue({ data: { user: null }, error: null }),
      signInWithPassword: vi.fn(),
      signInWithOAuth: vi.fn(),
      signOut: vi.fn(),
      onAuthStateChange: vi.fn(() => ({
        data: { subscription: { unsubscribe: vi.fn() } },
      })),
    },
    from: vi.fn(() => ({
      select: vi.fn().mockReturnThis(),
      insert: vi.fn().mockReturnThis(),
      update: vi.fn().mockReturnThis(),
      delete: vi.fn().mockReturnThis(),
      eq: vi.fn().mockReturnThis(),
      single: vi.fn(),
    })),
  })),
}));

// =============================================================================
// MOCK: localStorage
// =============================================================================
const localStorageMock = {
  store: {},
  getItem: vi.fn((key) => localStorageMock.store[key] || null),
  setItem: vi.fn((key, value) => {
    localStorageMock.store[key] = value.toString();
  }),
  removeItem: vi.fn((key) => {
    delete localStorageMock.store[key];
  }),
  clear: vi.fn(() => {
    localStorageMock.store = {};
  }),
};

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
});

// =============================================================================
// MOCK: sessionStorage
// =============================================================================
const sessionStorageMock = {
  store: {},
  getItem: vi.fn((key) => sessionStorageMock.store[key] || null),
  setItem: vi.fn((key, value) => {
    sessionStorageMock.store[key] = value.toString();
  }),
  removeItem: vi.fn((key) => {
    delete sessionStorageMock.store[key];
  }),
  clear: vi.fn(() => {
    sessionStorageMock.store = {};
  }),
};

Object.defineProperty(window, 'sessionStorage', {
  value: sessionStorageMock,
});

// =============================================================================
// Clear mocks between tests
// =============================================================================
afterEach(() => {
  localStorageMock.store = {};
  sessionStorageMock.store = {};
  vi.clearAllMocks();
});

// =============================================================================
// Suppress console errors/warnings in tests (optional)
// =============================================================================
// Uncomment to silence console output during tests:
// vi.spyOn(console, 'error').mockImplementation(() => {});
// vi.spyOn(console, 'warn').mockImplementation(() => {});
