// A simple placeholder for toast notifications to prevent import errors.
// You can replace this with a real toast library like 'react-toastify' or 'sonner'.

/**
 * Displays an error message to the console.
 * @param {string} message The error message to display.
 */
export const showErrorToast = (message) => {
  console.error("Toast [Error]:", message);
};

/**
 * Displays a success message to the console.
 * @param {string} message The success message to display.
 */
export const showSuccessToast = (message) => {
  console.log("Toast [Success]:", message);
};
