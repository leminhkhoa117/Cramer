import { useState, useEffect, useCallback, useRef } from 'react';
import { useHighlights } from '../contexts/HighlightContext';

const POPUP_OFFSET = 40;

const useTextHighlighter = (containerRef) => {
    const { addHighlight, removeHighlight, getHighlightsForContent } = useHighlights();
    const [selectedText, setSelectedText] = useState(null);
    const [selectionRange, setSelectionRange] = useState(null);
    const [popupPosition, setPopupPosition] = useState({ x: 0, y: 0, visible: false });
    const contentIdRef = useRef(null); // To store the ID of the content being highlighted

    const hidePopup = useCallback(() => {
        setPopupPosition(prev => ({ ...prev, visible: false }));
        setSelectedText(null);
        setSelectionRange(null);
        contentIdRef.current = null;
    }, []);

    const getSelectionInfo = useCallback(() => {
        const selection = window.getSelection();
        if (!selection || selection.rangeCount === 0) {
            return null;
        }

        const range = selection.getRangeAt(0);
        const parentElement = range.commonAncestorContainer.nodeType === Node.TEXT_NODE
            ? range.commonAncestorContainer.parentElement
            : range.commonAncestorContainer;

        // Find the closest ancestor with a data-content-id attribute
        let currentHighlightableContainer = parentElement;
        while (currentHighlightableContainer && !currentHighlightableContainer.dataset.contentId) {
            currentHighlightableContainer = currentHighlightableContainer.parentElement;
        }

        if (!currentHighlightableContainer || !containerRef.current || !containerRef.current.contains(currentHighlightableContainer)) {
            return null; // Selection is outside the main highlightable area or not in a content block
        }

        const contentId = currentHighlightableContainer.dataset.contentId;
        contentIdRef.current = contentId; // Store for later use

        const preSelectionRange = range.cloneRange();
        preSelectionRange.selectNodeContents(currentHighlightableContainer);
        preSelectionRange.setEnd(range.startContainer, range.startOffset);
        const startOffset = preSelectionRange.toString().length;
        const endOffset = startOffset + range.toString().length;

        const info = {
            text: range.toString(),
            startOffset,
            endOffset,
            contentId,
            rect: range.getBoundingClientRect(), // This gives viewport-relative coordinates
        };
        return info;
    }, [containerRef]);

    const handleMouseUp = useCallback(() => {
        const selectionInfo = getSelectionInfo();

        if (selectionInfo && selectionInfo.text.length > 0) {
            setSelectedText(selectionInfo.text);
            setSelectionRange({
                startOffset: selectionInfo.startOffset,
                endOffset: selectionInfo.endOffset,
                contentId: selectionInfo.contentId,
                highlightId: null,
            });
            // Use viewport coordinates directly for fixed positioning
            setPopupPosition({
                x: selectionInfo.rect.left,
                y: selectionInfo.rect.top - POPUP_OFFSET,
                visible: true,
            });
        } else {
            hidePopup();
        }
    }, [getSelectionInfo, hidePopup]);

    const handleHighlightClick = useCallback((event) => {
        const container = containerRef.current;
        if (!container) {
            return;
        }

        const highlightElement = event.target.closest('[data-highlight-id]');
        if (!highlightElement || !container.contains(highlightElement)) {
            return;
        }

        const contentElement = highlightElement.closest('[data-content-id]');
        if (!contentElement) {
            return;
        }

        const contentId = contentElement.dataset.contentId;
        const highlightId = highlightElement.getAttribute('data-highlight-id');
        const targetHighlights = getHighlightsForContent(contentId);
        const targetHighlight = targetHighlights.find(h => h.id === highlightId);

        if (!targetHighlight) {
            return;
        }

        contentIdRef.current = contentId;
        setSelectedText(targetHighlight.text);
        setSelectionRange({
            startOffset: targetHighlight.startOffset,
            endOffset: targetHighlight.endOffset,
            contentId,
            highlightId,
        });

        const rect = highlightElement.getBoundingClientRect();
        // Use viewport coordinates directly for fixed positioning
        setPopupPosition({
            x: rect.left,
            y: rect.top - POPUP_OFFSET,
            visible: true,
        });

        event.preventDefault();
        event.stopPropagation();
        window.getSelection().removeAllRanges();
    }, [containerRef, getHighlightsForContent]);

    useEffect(() => {
        const container = containerRef.current;
        if (container) {
            container.addEventListener('mouseup', handleMouseUp);
            container.addEventListener('click', handleHighlightClick);
            return () => {
                container.removeEventListener('mouseup', handleMouseUp);
                container.removeEventListener('click', handleHighlightClick);
            };
        }
    }, [containerRef, handleMouseUp, handleHighlightClick]);

    const applyHighlight = useCallback((style) => {
        if (selectionRange && selectedText) {
            if (selectionRange.highlightId) {
                removeHighlight(selectionRange.contentId, selectionRange.highlightId);
            }
            addHighlight(
                selectionRange.contentId,
                selectionRange.startOffset,
                selectionRange.endOffset,
                selectedText,
                style
            );
            window.getSelection().removeAllRanges(); // Clear selection after applying highlight
            hidePopup();
        }
    }, [addHighlight, hidePopup, removeHighlight, selectionRange, selectedText]);

    const clearHighlight = useCallback(() => {
        if (selectionRange) {
            if (selectionRange.highlightId) {
                removeHighlight(selectionRange.contentId, selectionRange.highlightId);
            } else {
                // Find existing highlights that overlap with the selection
                const existingHighlights = getHighlightsForContent(selectionRange.contentId);
                existingHighlights.forEach(h => {
                    // Simple overlap check: if selection starts before highlight ends AND selection ends after highlight starts
                    if (selectionRange.startOffset < h.endOffset && selectionRange.endOffset > h.startOffset) {
                        removeHighlight(selectionRange.contentId, h.id);
                    }
                });
            }
            window.getSelection().removeAllRanges();
            hidePopup();
        }
    }, [getHighlightsForContent, hidePopup, removeHighlight, selectionRange]);

    return {
        popupPosition,
        applyHighlight,
        clearHighlight,
        selectedText, // Can be used by popup to show selected text
        currentContentId: contentIdRef.current, // ID of the content block where selection occurred
    };
};

export default useTextHighlighter;
