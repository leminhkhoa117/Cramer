import { useRef, useState, useEffect, useCallback } from 'react';

/**
 * Custom hook to track element visibility in the viewport.
 * 
 * @param {Object} options - Configuration options
 * @param {number} options.threshold - Intersection threshold (0-1), default 0.1
 * @param {boolean} options.triggerOnce - If true, stays true after first intersection
 * @param {string} options.rootMargin - Margin around the root (e.g., "100px")
 * @returns {[React.RefObject, boolean]} - Ref to attach and visibility state
 */
export const useInView = (options = {}) => {
  const { 
    threshold = 0.1, 
    triggerOnce = false, 
    rootMargin = '0px' 
  } = options;
  
  const ref = useRef(null);
  const [isInView, setIsInView] = useState(false);

  useEffect(() => {
    const currentRef = ref.current;
    if (!currentRef) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsInView(true);
          if (triggerOnce) {
            observer.unobserve(entry.target);
          }
        } else if (!triggerOnce) {
          setIsInView(false);
        }
      },
      { threshold, rootMargin }
    );

    observer.observe(currentRef);

    return () => {
      if (currentRef) {
        observer.unobserve(currentRef);
      }
    };
  }, [threshold, triggerOnce, rootMargin]);

  return [ref, isInView];
};

/**
 * Hook for section-level visibility with placeholder support.
 * Returns inView state that toggles both ways (no triggerOnce).
 * Includes a buffer margin to preload content before it enters view.
 * 
 * @param {Object} options - Configuration options
 * @param {string} options.rootMargin - Preload buffer, default "200px"
 * @param {number} options.threshold - Intersection threshold, default 0
 * @returns {[React.RefObject, boolean]}
 */
export const useSectionInView = (options = {}) => {
  const { rootMargin = '200px', threshold = 0 } = options;
  return useInView({ threshold, rootMargin, triggerOnce: false });
};

export default useInView;
