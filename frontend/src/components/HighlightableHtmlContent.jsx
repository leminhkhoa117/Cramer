import React, { useMemo } from 'react';
import { sanitizeHtml } from '../utils/sanitize';
import { useHighlights } from '../contexts/HighlightContext';

const HighlightableHtmlContent = ({ htmlString, contentId, className = '' }) => {
    const { getHighlightsForContent } = useHighlights();
    const highlights = getHighlightsForContent(contentId);

    const processedHtml = useMemo(() => {
        if (!htmlString) return '';

        // If no highlights, avoid extra work and return the original HTML.
        if (!highlights || highlights.length === 0) {
            return htmlString;
        }

        // Use a DOM-based approach so that offsets computed from text selection
        // (via Range.toString()) line up with the rendered content that includes HTML tags.
        if (typeof document === 'undefined') {
            // Fallback for non-browser environments (e.g., tests)
            return htmlString;
        }

        const container = document.createElement('div');
        container.innerHTML = htmlString;

        const applyHighlightToRange = (root, highlight) => {
            const { startOffset, endOffset, style, id } = highlight;
            if (startOffset >= endOffset) return;

            const walker = document.createTreeWalker(
                root,
                NodeFilter.SHOW_TEXT,
                null
            );

            let currentOffset = 0;
            const nodesToHighlight = [];

            // First pass: collect all text nodes that fall within the highlight range
            while (walker.nextNode()) {
                const node = walker.currentNode;
                const text = node.textContent || '';
                const nodeStart = currentOffset;
                const nodeEnd = nodeStart + text.length;

                // Skip nodes entirely before the highlight
                if (nodeEnd <= startOffset) {
                    currentOffset = nodeEnd;
                    continue;
                }

                // Stop once we've passed the end of the highlight
                if (nodeStart >= endOffset) {
                    break;
                }

                // Calculate the portion of this node to highlight
                const highlightStartInNode = Math.max(startOffset, nodeStart) - nodeStart;
                const highlightEndInNode = Math.min(endOffset, nodeEnd) - nodeStart;

                if (highlightStartInNode < highlightEndInNode) {
                    nodesToHighlight.push({
                        node,
                        start: highlightStartInNode,
                        end: highlightEndInNode
                    });
                }

                currentOffset = nodeEnd;
            }

            // Second pass: apply highlights in reverse order to preserve node positions
            for (let i = nodesToHighlight.length - 1; i >= 0; i--) {
                const { node, start, end } = nodesToHighlight[i];

                try {
                    const range = document.createRange();
                    range.setStart(node, start);
                    range.setEnd(node, end);

                    const span = document.createElement('span');
                    span.setAttribute('data-highlight-id', id);
                    span.classList.add('highlighted-text');

                    if (style && typeof style === 'object') {
                        Object.entries(style).forEach(([key, value]) => {
                            try {
                                span.style[key] = value;
                            } catch {
                                // Ignore invalid style keys
                            }
                        });
                    }

                    // extractContents + appendChild + insertNode handles cross-boundary ranges
                    // This is more robust than surroundContents which fails when ranges
                    // span multiple elements (e.g., bold text mixed with normal text)
                    const fragment = range.extractContents();
                    span.appendChild(fragment);
                    range.insertNode(span);
                } catch (err) {
                    console.warn('Failed to apply highlight to node:', err);
                }
            }
        };

        // Apply highlights in order of start offset so overlapping ranges
        // are processed deterministically.
        const sortedHighlights = [...highlights].sort(
            (a, b) => a.startOffset - b.startOffset
        );

        sortedHighlights.forEach(h => applyHighlightToRange(container, h));

        return container.innerHTML;
    }, [htmlString, highlights]);

    return (
        <span
            className={`highlightable-content ${className}`}
            data-content-id={contentId}
            dangerouslySetInnerHTML={{ __html: sanitizeHtml(processedHtml) }}
        />
    );
};

export default HighlightableHtmlContent;
