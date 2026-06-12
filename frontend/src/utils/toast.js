/**
 * Toast compat shim — delegates to the unified toast system in `src/ui/toast`.
 * Existing imports (showSuccessToast/showErrorToast/…) keep working.
 */
import { toast as uiToast } from '../ui/toast';

export const showSuccessToast = (message, title) => uiToast.success(message, title ? { title } : undefined);
export const showErrorToast = (message, title) => uiToast.error(message, title ? { title } : undefined);
export const showWarningToast = (message, title) => uiToast.warning(message, title ? { title } : undefined);
export const showInfoToast = (message, title) => uiToast.info(message, title ? { title } : undefined);

// No-ops kept for backward compatibility (old ToastProvider registration).
export const registerToastHandler = () => {};
export const unregisterToastHandler = () => {};

const toast = {
  success: showSuccessToast,
  error: showErrorToast,
  warning: showWarningToast,
  info: showInfoToast,
};

export default toast;
