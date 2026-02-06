/**
 * Global Toast Notification System
 *
 * This module provides a simple global toast API that works both inside and outside
 * React components. For React components, prefer using the useToast hook from
 * the admin Toast component for full functionality.
 *
 * Usage:
 *   import toast from '../utils/toast';
 *   toast.success('Operation completed!');
 *   toast.error('Something went wrong');
 */

// Store for the toast context setter (set by ToastProvider)
let toastHandler = null;

/**
 * Register the toast handler from ToastProvider
 * Called internally by ToastProvider on mount
 */
export const registerToastHandler = (handler) => {
  toastHandler = handler;
};

/**
 * Unregister the toast handler
 * Called internally by ToastProvider on unmount
 */
export const unregisterToastHandler = () => {
  toastHandler = null;
};

/**
 * Show a success toast
 * @param {string} message - The message to display
 * @param {string} [title] - Optional title
 */
export const showSuccessToast = (message, title) => {
  if (toastHandler) {
    toastHandler.success(message, title);
  } else {
    console.log('✅ Toast [Success]:', title ? `${title}: ${message}` : message);
  }
};

/**
 * Show an error toast
 * @param {string} message - The message to display
 * @param {string} [title] - Optional title
 */
export const showErrorToast = (message, title) => {
  if (toastHandler) {
    toastHandler.error(message, title);
  } else {
    console.error('❌ Toast [Error]:', title ? `${title}: ${message}` : message);
  }
};

/**
 * Show a warning toast
 * @param {string} message - The message to display
 * @param {string} [title] - Optional title
 */
export const showWarningToast = (message, title) => {
  if (toastHandler) {
    toastHandler.warning(message, title);
  } else {
    console.warn('⚠️ Toast [Warning]:', title ? `${title}: ${message}` : message);
  }
};

/**
 * Show an info toast
 * @param {string} message - The message to display
 * @param {string} [title] - Optional title
 */
export const showInfoToast = (message, title) => {
  if (toastHandler) {
    toastHandler.info(message, title);
  } else {
    console.info('ℹ️ Toast [Info]:', title ? `${title}: ${message}` : message);
  }
};

// Default export as an object for convenient usage: toast.success(), toast.error(), etc.
const toast = {
  success: showSuccessToast,
  error: showErrorToast,
  warning: showWarningToast,
  info: showInfoToast,
};

export default toast;
