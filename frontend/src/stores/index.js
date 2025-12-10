/**
 * Zustand Stores - Centralized State Management
 * 
 * This file exports all Zustand stores for easy importing throughout the app.
 * 
 * Usage:
 *   import { useAuthStore, useProfileStore, useTestStore } from '../stores';
 */

// Authentication & Profile
export { default as useAuthStore, selectUser, selectSession, selectLoading, selectError, selectIsAuthenticated } from './useAuthStore';
export { default as useProfileStore } from './useProfileStore';

// Test Taking
export { default as useTestStore } from './useTestStore';
export { default as useTestSessionStore } from './useTestSessionStore';

// Data Caching
export { default as useDashboardStore } from './useDashboardStore';
export { default as useCourseStore } from './useCourseStore';
