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
            console.log("No selection or empty range.");
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
            console.log("Selection is outside the main highlightable area or not in a content block.");
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
            rect: range.getBoundingClientRect(),
        };
        console.log("Selection Info:", info);
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
            setPopupPosition({
                x: selectionInfo.rect.left + window.scrollX,
                y: selectionInfo.rect.top + window.scrollY - POPUP_OFFSET, // Position above selection
                visible: true,
            });
            console.log("Popup visible with selection:", selectionInfo.text);
        } else {
            hidePopup();
            console.log("No valid selection, hiding popup.");
        }
    }, [getSelectionInfo, hidePopup]);

    const handleHighlightClick = useCallback((event) => {
        const container = containerRef.current;
        if (!container) {
            console.log("Container ref is null, cannot detect highlight clicks.");
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
            console.log("Clicked highlight not found in context.");
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
        setPopupPosition({
            x: rect.left + window.scrollX,
            y: rect.top + window.scrollY - POPUP_OFFSET,
            visible: true,
        });

        event.preventDefault();
        event.stopPropagation();
        window.getSelection().removeAllRanges();
        console.log("Popup opened from highlight click:", targetHighlight.id);
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
        } else {
            console.log("Container ref is null, cannot attach listeners.");
        }
    }, [containerRef, handleMouseUp, handleHighlightClick]);

    const applyHighlight = useCallback((style) => {
        console.log("Attempting to apply highlight with style:", style, "for selection:", selectionRange);
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
            console.log("Highlight applied.");
        } else {
            console.log("No valid selection to apply highlight.");
        }
    }, [addHighlight, hidePopup, removeHighlight, selectionRange, selectedText]);

    const clearHighlight = useCallback(() => {
        console.log("Attempting to clear highlight for selection:", selectionRange);
        if (selectionRange) {
            if (selectionRange.highlightId) {
                removeHighlight(selectionRange.contentId, selectionRange.highlightId);
                console.log("Removed highlight by id:", selectionRange.highlightId);
            } else {
                // Find existing highlights that overlap with the selection
                const existingHighlights = getHighlightsForContent(selectionRange.contentId);
                existingHighlights.forEach(h => {
                    // Simple overlap check: if selection starts before highlight ends AND selection ends after highlight starts
                    if (selectionRange.startOffset < h.endOffset && selectionRange.endOffset > h.startOffset) {
                        removeHighlight(selectionRange.contentId, h.id);
                        console.log("Removed highlight:", h.id);
                    }
                });
            }
            window.getSelection().removeAllRanges();
            hidePopup();
            console.log("Highlight cleared.");
        } else {
            console.log("No valid selection to clear highlight.");
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
