import React, { useMemo } from 'react';
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

            while (walker.nextNode()) {
                const node = walker.currentNode;
                const text = node.textContent || '';
                const nodeStart = currentOffset;
                const nodeEnd = nodeStart + text.length;

                // Skip nodes that are entirely before the highlight.
                if (nodeEnd <= startOffset) {
                    currentOffset = nodeEnd;
                    continue;
                }

                // Stop once we've passed the end of the highlight.
                if (nodeStart >= endOffset) {
                    break;
                }

                const highlightStartInNode = Math.max(startOffset, nodeStart) - nodeStart;
                const highlightEndInNode = Math.min(endOffset, nodeEnd) - nodeStart;

                if (highlightStartInNode >= highlightEndInNode) {
                    currentOffset = nodeEnd;
                    continue;
                }

                const range = document.createRange();
                range.setStart(node, highlightStartInNode);
                range.setEnd(node, highlightEndInNode);

                const span = document.createElement('span');
                span.setAttribute('data-highlight-id', id);
                span.classList.add('highlighted-text');

                if (style && typeof style === 'object') {
                    Object.entries(style).forEach(([key, value]) => {
                        try {
                            span.style[key] = value;
                        } catch {
                            // Ignore invalid style keys to avoid breaking highlights.
                        }
                    });
                }

                range.surroundContents(span);

                currentOffset = nodeEnd;
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
            dangerouslySetInnerHTML={{ __html: processedHtml }}
        />
    );
};

export default HighlightableHtmlContent;
