import { useState, useEffect, useCallback, useRef } from 'react';

/**
 * Custom hook for timer management (countdown or count-up)
 *
 * Features:
 * - Count up or countdown modes
 * - Auto-stop at 0 for countdown
 * - Warning states (< 30s, < 10s)
 * - Format helper for MM:SS display
 *
 * @param {number} initialTime - Initial time in seconds
 * @param {boolean} countDown - If true, counts down; else counts up
 * @returns {object} Timer state and controls
 */
export default function useTimer(initialTime = 0, countDown = false) {
  const [time, setTime] = useState(initialTime);
  const [isRunning, setIsRunning] = useState(false);
  const intervalRef = useRef(null);

  /**
   * Start the timer
   */
  const start = useCallback(() => {
    setIsRunning(true);
  }, []);

  /**
   * Stop the timer
   */
  const stop = useCallback(() => {
    setIsRunning(false);
  }, []);

  /**
   * Reset timer to initial or specified time
   * @param {number} newTime - New time to reset to (optional)
   */
  const reset = useCallback((newTime = initialTime) => {
    setTime(newTime);
    setIsRunning(false);
  }, [initialTime]);

  /**
   * Set time directly (useful for syncing with server)
   * @param {number} newTime - New time value
   */
  const setTimeDirect = useCallback((newTime) => {
    setTime(newTime);
  }, []);

  /**
   * Timer tick effect
   */
  useEffect(() => {
    if (isRunning) {
      intervalRef.current = setInterval(() => {
        setTime((prev) => {
          const newTime = countDown ? prev - 1 : prev + 1;

          // Auto-stop at 0 for countdown
          if (countDown && newTime <= 0) {
            setIsRunning(false);
            return 0;
          }

          return newTime;
        });
      }, 1000);
    } else {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [isRunning, countDown]);

  /**
   * Format time as MM:SS
   * @param {number} seconds - Time in seconds
   * @returns {string} Formatted time string
   */
  const formatTime = useCallback((seconds) => {
    const mins = Math.floor(Math.abs(seconds) / 60);
    const secs = Math.abs(seconds) % 60;
    const formatted = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    return seconds < 0 ? `-${formatted}` : formatted;
  }, []);

  /**
   * Get warning states
   */
  const isWarning = countDown && time > 0 && time <= 30;
  const isCritical = countDown && time > 0 && time <= 10;

  return {
    time,
    isRunning,
    formattedTime: formatTime(time),
    start,
    stop,
    reset,
    setTime: setTimeDirect,
    isWarning,
    isCritical,
    formatTime,
  };
}
