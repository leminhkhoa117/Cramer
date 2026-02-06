import { useEffect, useRef, useCallback, useState } from 'react';

/**
 * Custom hook for silence detection in audio stream
 *
 * Automatically detects when user stops speaking by monitoring
 * audio level. Triggers callback when silence threshold is exceeded.
 *
 * Features:
 * - Real-time silence countdown (with state for UI updates)
 * - Configurable threshold and duration
 * - Warning callback at configurable seconds before trigger
 *
 * @param {number} audioLevel - Current audio level (0-100)
 * @param {boolean} isActive - Whether detection is active
 * @param {number} silenceThreshold - Audio level below which is considered silence (default: 5)
 * @param {number} silenceDuration - Duration in seconds before triggering (default: 5)
 * @param {number} warningAt - Seconds remaining when warning should show (default: 3)
 * @param {function} onSilenceDetected - Callback when silence duration reached
 * @param {function} onWarningStart - Callback when warning period starts
 * @returns {object} Silence detection state and controls
 */
export default function useSilenceDetection({
  audioLevel,
  isActive,
  silenceThreshold = 5,
  silenceDuration = 5,
  warningAt = 3,
  onSilenceDetected,
  onWarningStart,
}) {
  // Use state for UI reactivity
  const [silenceSeconds, setSilenceSeconds] = useState(0);
  const [isWarning, setIsWarning] = useState(false);
  
  const intervalRef = useRef(null);
  const hasDetectedRef = useRef(false);
  const hasWarnedRef = useRef(false);
  const audioLevelRef = useRef(audioLevel);

  // Keep audioLevel ref updated (to avoid stale closure in interval)
  useEffect(() => {
    audioLevelRef.current = audioLevel;
  }, [audioLevel]);

  /**
   * Reset silence timer
   */
  const resetSilenceTimer = useCallback(() => {
    setSilenceSeconds(0);
    setIsWarning(false);
    hasDetectedRef.current = false;
    hasWarnedRef.current = false;
  }, []);

  /**
   * Monitor audio level for silence
   */
  useEffect(() => {
    if (!isActive) {
      // Clear interval when not active
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
      resetSilenceTimer();
      return;
    }

    // Start monitoring every second
    intervalRef.current = setInterval(() => {
      const currentLevel = audioLevelRef.current;
      const isSilent = currentLevel < silenceThreshold;

      if (isSilent) {
        setSilenceSeconds((prev) => {
          const newCount = prev + 1;

          // Check for warning threshold
          const remainingSeconds = silenceDuration - newCount;
          if (remainingSeconds <= warningAt && !hasWarnedRef.current) {
            hasWarnedRef.current = true;
            setIsWarning(true);
            if (onWarningStart) {
              onWarningStart(remainingSeconds);
            }
          }

          // Check for silence detected
          if (newCount >= silenceDuration && !hasDetectedRef.current) {
            hasDetectedRef.current = true;
            if (onSilenceDetected) {
              // Use setTimeout to avoid state update during render
              setTimeout(() => onSilenceDetected(), 0);
            }
          }

          return newCount;
        });
      } else {
        // Voice detected - reset everything
        setSilenceSeconds(0);
        setIsWarning(false);
        hasDetectedRef.current = false;
        hasWarnedRef.current = false;
      }
    }, 1000);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [isActive, silenceThreshold, silenceDuration, warningAt, onSilenceDetected, onWarningStart, resetSilenceTimer]);

  /**
   * Cleanup on unmount
   */
  useEffect(() => {
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, []);

  return {
    silenceSeconds,        // Current seconds of silence (state - triggers re-renders)
    isWarning,             // Whether in warning period
    remainingSeconds: Math.max(0, silenceDuration - silenceSeconds), // Countdown
    resetSilenceTimer,
  };
}
