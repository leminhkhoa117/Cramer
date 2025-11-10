import React, { useRef, useEffect, useCallback } from 'react';

const HighlightableText = ({ text }) => {
    const contentRef = useRef(null);

    const handleHighlight = useCallback(() => {
        const selection = window.getSelection();
        if (!selection.rangeCount || selection.isCollapsed) return;

        const range = selection.getRangeAt(0);
        
        // Prevent highlighting if the selection is already inside a highlight
        if (range.startContainer.parentElement.classList.contains('highlighted-text') ||
            range.endContainer.parentElement.classList.contains('highlighted-text')) {
            selection.removeAllRanges();
            return;
        }

        const selectedText = range.toString();
        if (selectedText.trim() === '') return;

        const newSpan = document.createElement('span');
        newSpan.className = 'highlighted-text';
        newSpan.onclick = () => unwrapHighlight(newSpan);

        try {
            // A more robust way to wrap a selection that might span multiple elements
            const fragment = range.extractContents();
            newSpan.appendChild(fragment);
            range.insertNode(newSpan);
        } catch (e) {
            console.error("Highlighting failed:", e);
        }

        selection.removeAllRanges();
    }, []);

    const unwrapHighlight = (span) => {
        const parent = span.parentNode;
        while (span.firstChild) {
            parent.insertBefore(span.firstChild, span);
        }
        parent.removeChild(span);
        parent.normalize(); // Clean up adjacent text nodes
    };

    useEffect(() => {
        const contentDiv = contentRef.current;
        if (contentDiv) {
            // Set up initial state and event listeners
            contentDiv.innerHTML = text.replace(/\n/g, '<br />');
            contentDiv.addEventListener('mouseup', handleHighlight);

            // Add click listeners to any pre-existing highlights (if any)
            const existingHighlights = contentDiv.querySelectorAll('.highlighted-text');
            existingHighlights.forEach(span => {
                span.onclick = () => unwrapHighlight(span);
            });
        }

        return () => {
            if (contentDiv) {
                contentDiv.removeEventListener('mouseup', handleHighlight);
            }
        };
    }, [text, handleHighlight]);

    return (
        <div
            ref={contentRef}
            className="passage-content"
        />
    );
};

export default HighlightableText;