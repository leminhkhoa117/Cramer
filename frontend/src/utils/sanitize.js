import DOMPurify from 'dompurify';

/**
 * Sanitize HTML content to prevent XSS attacks
 * @param {string} dirty - Untrusted HTML string
 * @param {Object} options - DOMPurify configuration options
 * @returns {string} Sanitized HTML string
 */
export const sanitizeHtml = (dirty, options = {}) => {
  if (!dirty) return '';
  
  // Default config allows safe HTML tags for IELTS content
  const defaultConfig = {
    ALLOWED_TAGS: [
      'p', 'br', 'b', 'i', 'u', 'strong', 'em', 'span', 'div',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'ul', 'ol', 'li',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'blockquote', 'pre', 'code',
      'a', 'img', 'mark', 'sub', 'sup'
    ],
    ALLOWED_ATTR: [
      'href', 'src', 'alt', 'title', 'class', 'id', 'style',
      'target', 'rel', 'data-*'
    ],
    ALLOW_DATA_ATTR: true,
    ...options
  };
  
  return DOMPurify.sanitize(dirty, defaultConfig);
};

/**
 * Create a safe props object for dangerouslySetInnerHTML
 * @param {string} html - HTML content to sanitize
 * @param {Object} options - DOMPurify options
 * @returns {Object} Props object with __html property
 */
export const createSafeHtml = (html, options = {}) => ({
  __html: sanitizeHtml(html, options)
});

export default sanitizeHtml;
