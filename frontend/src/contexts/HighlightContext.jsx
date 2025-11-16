import React, { createContext, useState, useContext, useCallback } from 'react';
import { v4 as uuidv4 } from 'uuid';

const HighlightContext = createContext();

export const HighlightProvider = ({ children }) => {
    // highlights: Map<contentId, Map<highlightId, highlightObject>>
    const [highlights, setHighlights] = useState(new Map());

    const addHighlight = useCallback((contentId, startOffset, endOffset, text, style) => {
        setHighlights(prevHighlights => {
            const contentHighlights = prevHighlights.get(contentId);
            const newHighlight = { id: uuidv4(), startOffset, endOffset, text, style };

            // Check if a highlight with the same properties already exists
            if (contentHighlights) {
                const existingHighlight = Array.from(contentHighlights.values()).find(
                    h => h.startOffset === newHighlight.startOffset &&
                         h.endOffset === newHighlight.endOffset &&
                         h.text === newHighlight.text &&
                         JSON.stringify(h.style) === JSON.stringify(newHighlight.style)
                );
                if (existingHighlight) {
                    console.log("Duplicate highlight, not adding:", newHighlight);
                    return prevHighlights; // No change, return previous state
                }
            }

            const newContentHighlights = new Map(contentHighlights); // Create new map for this contentId's highlights
            newContentHighlights.set(newHighlight.id, newHighlight);

            const newHighlights = new Map(prevHighlights); // Create new map for overall highlights
            newHighlights.set(contentId, newContentHighlights); // Update only the changed contentId
            
            console.log("Added Highlight:", newHighlight, "Current Highlights:", newHighlights);
            return newHighlights;
        });
    }, []);

    const removeHighlight = useCallback((contentId, highlightId) => {
        setHighlights(prevHighlights => {
            const contentHighlights = prevHighlights.get(contentId);
            if (!contentHighlights || !contentHighlights.has(highlightId)) {
                return prevHighlights; // No change, return previous state
            }

            const newContentHighlights = new Map(contentHighlights);
            newContentHighlights.delete(highlightId);

            const newHighlights = new Map(prevHighlights);
            if (newContentHighlights.size === 0) {
                newHighlights.delete(contentId);
            } else {
                newHighlights.set(contentId, newContentHighlights);
            }
            console.log("Removed Highlight:", highlightId, "Current Highlights:", newHighlights);
            return newHighlights;
        });
    }, []);

    const getHighlightsForContent = useCallback((contentId) => {
        const contentHighlightsMap = highlights.get(contentId);
        // Always return a new array to keep consumers immutable.
        const result = contentHighlightsMap ? Array.from(contentHighlightsMap.values()) : [];
        return result;
    }, [highlights]);

    const clearAllHighlights = useCallback(() => {
        setHighlights(new Map());
        console.log("Cleared all highlights."); // Log clear all
    }, []);

    const value = {
        highlights,
        addHighlight,
        removeHighlight,
        getHighlightsForContent,
        clearAllHighlights,
    };

    return (
        <HighlightContext.Provider value={value}>
            {children}
        </HighlightContext.Provider>
    );
};

export const useHighlights = () => {
    const context = useContext(HighlightContext);
    if (!context) {
        throw new Error('useHighlights must be used within a HighlightProvider');
    }
    return context;
};
