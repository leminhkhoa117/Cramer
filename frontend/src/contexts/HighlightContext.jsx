import { create } from 'zustand';
import { v4 as uuidv4 } from 'uuid';

// Highlight store (was a React Context; now Zustand, same API).
// highlights: Map<contentId, Map<highlightId, highlightObject>>
const useHighlightStore = create((set, get) => ({
    highlights: new Map(),

    addHighlight: (contentId, startOffset, endOffset, text, style) => {
        const { highlights } = get();
        const contentHighlights = highlights.get(contentId);
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
                return; // Duplicate highlight, no change
            }
        }

        const newContentHighlights = new Map(contentHighlights); // New map for this contentId's highlights
        newContentHighlights.set(newHighlight.id, newHighlight);

        const newHighlights = new Map(highlights); // New map for overall highlights
        newHighlights.set(contentId, newContentHighlights); // Update only the changed contentId
        set({ highlights: newHighlights });
    },

    removeHighlight: (contentId, highlightId) => {
        const { highlights } = get();
        const contentHighlights = highlights.get(contentId);
        if (!contentHighlights || !contentHighlights.has(highlightId)) {
            return; // No change
        }

        const newContentHighlights = new Map(contentHighlights);
        newContentHighlights.delete(highlightId);

        const newHighlights = new Map(highlights);
        if (newContentHighlights.size === 0) {
            newHighlights.delete(contentId);
        } else {
            newHighlights.set(contentId, newContentHighlights);
        }
        set({ highlights: newHighlights });
    },

    getHighlightsForContent: (contentId) => {
        const contentHighlightsMap = get().highlights.get(contentId);
        // Always return a new array to keep consumers immutable.
        return contentHighlightsMap ? Array.from(contentHighlightsMap.values()) : [];
    },

    clearAllHighlights: () => set({ highlights: new Map() }),
}));

/** Provider kept for API compatibility; the store itself is global. */
export const HighlightProvider = ({ children }) => children;

export const useHighlights = () => ({
    highlights: useHighlightStore((s) => s.highlights),
    addHighlight: useHighlightStore((s) => s.addHighlight),
    removeHighlight: useHighlightStore((s) => s.removeHighlight),
    getHighlightsForContent: useHighlightStore((s) => s.getHighlightsForContent),
    clearAllHighlights: useHighlightStore((s) => s.clearAllHighlights),
});

export default HighlightProvider;
