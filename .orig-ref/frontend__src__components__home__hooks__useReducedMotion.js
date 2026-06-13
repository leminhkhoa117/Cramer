import { useEffect, useState } from 'react';

/**
 * Hook that returns whether the user prefers reduced motion.
 * Tracks the media query live.
 */
export function useReducedMotion() {
    const [reduced, setReduced] = useState(() => {
        if (typeof window === 'undefined') return false;
        return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    });

    useEffect(() => {
        if (typeof window === 'undefined') return undefined;
        const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
        const handler = (event) => setReduced(event.matches);
        if (mq.addEventListener) {
            mq.addEventListener('change', handler);
            return () => mq.removeEventListener('change', handler);
        }
        mq.addListener(handler);
        return () => mq.removeListener(handler);
    }, []);

    return reduced;
}