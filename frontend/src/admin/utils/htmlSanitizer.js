/**
 * HTML Sanitization Utility
 * 
 * Provides XSS-safe HTML rendering for user-generated content.
 * Uses a simple whitelist approach without requiring external libs.
 * 
 * @since 2025-12-21 - XSS Prevention
 */

// Allowed HTML tags for passage content
const ALLOWED_TAGS = [
    'p', 'br', 'b', 'strong', 'i', 'em', 'u', 'sub', 'sup',
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'ul', 'ol', 'li',
    'blockquote', 'pre', 'code',
    'table', 'thead', 'tbody', 'tr', 'th', 'td',
    'span', 'div',
    'a' // Will have href sanitized
];

// Allowed attributes per tag
const ALLOWED_ATTRS = {
    'a': ['href', 'title'],
    'img': ['src', 'alt', 'title', 'width', 'height'],
    'span': ['class'],
    'div': ['class'],
    'p': ['class'],
    'table': ['class'],
    'td': ['colspan', 'rowspan'],
    'th': ['colspan', 'rowspan']
};

// Dangerous protocols in href/src
const DANGEROUS_PROTOCOLS = ['javascript:', 'data:', 'vbscript:'];

/**
 * Sanitize HTML string for safe rendering.
 * Removes dangerous tags and attributes while keeping content.
 * Also converts newlines to <br> if no paragraph tags present.
 * 
 * @param {string} html - Raw HTML string
 * @returns {string} Sanitized HTML safe for dangerouslySetInnerHTML
 */
export function sanitizeHtml(html) {
    if (!html || typeof html !== 'string') {
        return '';
    }

    // Pre-process: convert newlines to <br> if content lacks paragraph structure
    let processedHtml = html;
    const hasParagraphTags = /<(p|div|br)\b/i.test(html);

    if (!hasParagraphTags) {
        // Convert double newlines to paragraph breaks, single newlines to <br>
        processedHtml = html
            .replace(/\r\n/g, '\n')               // Normalize line endings
            .replace(/\n{2,}/g, '</p><p>')        // Double newlines become paragraphs
            .replace(/\n/g, '<br>')               // Single newlines become <br>
            .replace(/^/, '<p>')                   // Wrap in paragraphs
            .replace(/$/, '</p>')
            .replace(/<p><\/p>/g, '');            // Remove empty paragraphs
    }

    // Also normalize underscore sequences for fill-in-blank questions
    // AI often generates 5-10 underscores when we need exactly 4
    processedHtml = processedHtml.replace(/_{3,}/g, '____');

    // Create a temporary DOM element
    const doc = new DOMParser().parseFromString(processedHtml, 'text/html');

    // Recursively sanitize
    sanitizeNode(doc.body);

    return doc.body.innerHTML;
}

/**
 * Recursively sanitize a DOM node.
 */
function sanitizeNode(node) {
    // Get list of child nodes (convert to array to avoid live collection issues)
    const children = Array.from(node.childNodes);

    for (const child of children) {
        if (child.nodeType === Node.ELEMENT_NODE) {
            const tagName = child.tagName.toLowerCase();

            // Remove disallowed tags but keep their content
            if (!ALLOWED_TAGS.includes(tagName)) {
                // Replace with text content
                const textNode = document.createTextNode(child.textContent || '');
                node.replaceChild(textNode, child);
                continue;
            }

            // Remove disallowed attributes
            const allowedAttrs = ALLOWED_ATTRS[tagName] || [];
            const attrs = Array.from(child.attributes);

            for (const attr of attrs) {
                if (!allowedAttrs.includes(attr.name)) {
                    child.removeAttribute(attr.name);
                } else {
                    // Sanitize href/src values
                    if (attr.name === 'href' || attr.name === 'src') {
                        const value = attr.value.toLowerCase().trim();
                        if (DANGEROUS_PROTOCOLS.some(p => value.startsWith(p))) {
                            child.removeAttribute(attr.name);
                        }
                    }
                }
            }

            // Recursively sanitize children
            sanitizeNode(child);
        } else if (child.nodeType === Node.COMMENT_NODE) {
            // Remove comments
            node.removeChild(child);
        }
    }
}

/**
 * Strip all HTML tags and return plain text.
 * 
 * @param {string} html - HTML string
 * @returns {string} Plain text content
 */
export function stripHtml(html) {
    if (!html || typeof html !== 'string') {
        return '';
    }

    const doc = new DOMParser().parseFromString(html, 'text/html');
    return doc.body.textContent || '';
}

/**
 * Truncate HTML while preserving tag integrity.
 * 
 * @param {string} html - HTML string
 * @param {number} maxLength - Maximum text length
 * @param {string} suffix - Suffix to add if truncated (default: '...')
 * @returns {string} Truncated HTML
 */
export function truncateHtml(html, maxLength = 300, suffix = '...') {
    if (!html || typeof html !== 'string') {
        return '';
    }

    const text = stripHtml(html);

    if (text.length <= maxLength) {
        return sanitizeHtml(html);
    }

    // Find a good breakpoint
    let breakpoint = maxLength;
    const breakChars = [' ', '.', ',', '!', '?'];

    for (let i = maxLength; i > maxLength - 50 && i > 0; i--) {
        if (breakChars.includes(text[i])) {
            breakpoint = i;
            break;
        }
    }

    // Truncate text and wrap in paragraph
    const truncatedText = text.substring(0, breakpoint).trim() + suffix;
    return `<p>${escapeHtml(truncatedText)}</p>`;
}

/**
 * Escape HTML special characters.
 * 
 * @param {string} str - Plain text string
 * @returns {string} HTML-escaped string
 */
export function escapeHtml(str) {
    if (!str || typeof str !== 'string') {
        return '';
    }

    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

export default {
    sanitizeHtml,
    stripHtml,
    truncateHtml,
    escapeHtml
};
