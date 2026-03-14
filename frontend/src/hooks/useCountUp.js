import { useState, useEffect, useRef } from 'react';

/**
 * Animated count-up hook.
 * Animates from 0 to `target` over `duration` ms using easeOutExpo.
 *
 * @param {number} target - The final number to count up to
 * @param {number} duration - Animation duration in ms (default 2000)
 * @param {boolean} isActive - Whether to start counting
 * @returns {number} The current animated value (integer)
 */
export function useCountUp(target, duration = 2000, isActive = false) {
  const [value, setValue] = useState(0);
  const frameRef = useRef(null);
  const hasPlayedRef = useRef(false);
  const reducedMotion = useRef(
    typeof window !== 'undefined' &&
    window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  );

  useEffect(() => {
    if (!isActive || hasPlayedRef.current) return;
    hasPlayedRef.current = true;

    // Reduced motion: show final value immediately
    if (reducedMotion.current) {
      setValue(target);
      return;
    }

    let startTime = null;

    const easeOutExpo = (t) => (t === 1 ? 1 : 1 - Math.pow(2, -10 * t));

    const animate = (timestamp) => {
      if (!startTime) startTime = timestamp;
      const elapsed = timestamp - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const easedProgress = easeOutExpo(progress);

      setValue(Math.round(easedProgress * target));

      if (progress < 1) {
        frameRef.current = requestAnimationFrame(animate);
      } else {
        setValue(target); // ensure exact final value
      }
    };

    frameRef.current = requestAnimationFrame(animate);

    return () => {
      if (frameRef.current) cancelAnimationFrame(frameRef.current);
    };
  }, [isActive, target, duration]);

  return value;
}

export default useCountUp;
