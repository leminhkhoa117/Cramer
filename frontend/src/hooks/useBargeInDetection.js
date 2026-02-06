import { useEffect, useRef, useCallback, useState } from 'react';

/**
 * Hook for detecting user barge-in (interrupting examiner speech)
 *
 * Monitors microphone audio level during examiner TTS playback.
 * If user speaks above threshold for sustained duration, triggers barge-in.
 *
 * @param {Object} options - Configuration options
 * @param {boolean} options.isExaminerSpeaking - Whether examiner TTS is playing
 * @param {number} options.audioThreshold - Min audio level to consider as speech (0-100)
 * @param {number} options.sustainedMs - Time user must speak above threshold (ms)
 * @param {function} options.onBargeIn - Callback when barge-in is detected
 */
export default function useBargeInDetection({
  isExaminerSpeaking = false,
  audioThreshold = 15,
  sustainedMs = 500,
  onBargeIn,
} = {}) {
  const [isMonitoring, setIsMonitoring] = useState(false);
  const [audioLevel, setAudioLevel] = useState(0);
  const [bargeInDetected, setBargeInDetected] = useState(false);

  // Refs for audio processing
  const streamRef = useRef(null);
  const audioContextRef = useRef(null);
  const analyserRef = useRef(null);
  const animationFrameRef = useRef(null);
  const sustainedStartRef = useRef(null);

  /**
   * Start monitoring microphone for barge-in
   */
  const startMonitoring = useCallback(async () => {
    if (isMonitoring || streamRef.current) return;

    try {
      // Get microphone access (may already be granted from recording)
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });

      streamRef.current = stream;

      // Setup audio analyser
      const audioContext = new (window.AudioContext || window.webkitAudioContext)();
      const source = audioContext.createMediaStreamSource(stream);
      const analyser = audioContext.createAnalyser();

      analyser.fftSize = 256;
      analyser.smoothingTimeConstant = 0.8;

      source.connect(analyser);

      audioContextRef.current = audioContext;
      analyserRef.current = analyser;

      setIsMonitoring(true);
      sustainedStartRef.current = null;

      // Start monitoring loop
      const dataArray = new Uint8Array(analyser.frequencyBinCount);

      const checkAudioLevel = () => {
        if (!analyserRef.current) return;

        analyserRef.current.getByteFrequencyData(dataArray);

        // Calculate average volume
        const average = dataArray.reduce((a, b) => a + b, 0) / dataArray.length;
        const normalizedLevel = Math.min(100, (average / 255) * 100 * 1.5);

        setAudioLevel(normalizedLevel);

        // Check for sustained speech above threshold
        if (normalizedLevel > audioThreshold) {
          if (!sustainedStartRef.current) {
            sustainedStartRef.current = Date.now();
          } else if (Date.now() - sustainedStartRef.current >= sustainedMs) {
            // Sustained speech detected - trigger barge-in
            console.log('Barge-in detected! User interrupted examiner.');
            setBargeInDetected(true);
            
            if (onBargeIn) {
              onBargeIn();
            }
            
            // Stop monitoring after barge-in
            stopMonitoring();
            return;
          }
        } else {
          // Reset sustained timer if volume drops
          sustainedStartRef.current = null;
        }

        animationFrameRef.current = requestAnimationFrame(checkAudioLevel);
      };

      checkAudioLevel();

    } catch (err) {
      console.error('Failed to start barge-in monitoring:', err);
    }
  }, [isMonitoring, audioThreshold, sustainedMs, onBargeIn]);

  /**
   * Stop monitoring
   */
  const stopMonitoring = useCallback(() => {
    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }

    // Don't stop the stream - it may be shared with recording
    // Just stop our analysis
    if (audioContextRef.current) {
      audioContextRef.current.close().catch(() => {});
      audioContextRef.current = null;
    }

    if (streamRef.current) {
      // Release tracks
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }

    analyserRef.current = null;
    sustainedStartRef.current = null;
    setIsMonitoring(false);
    setAudioLevel(0);
  }, []);

  /**
   * Reset barge-in state for next question
   */
  const resetBargeIn = useCallback(() => {
    setBargeInDetected(false);
    sustainedStartRef.current = null;
  }, []);

  /**
   * Start/stop monitoring based on examiner speaking state
   */
  useEffect(() => {
    if (isExaminerSpeaking && !bargeInDetected) {
      startMonitoring();
    } else {
      stopMonitoring();
    }

    return () => {
      stopMonitoring();
    };
  }, [isExaminerSpeaking, bargeInDetected, startMonitoring, stopMonitoring]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopMonitoring();
    };
  }, [stopMonitoring]);

  return {
    isMonitoring,
    audioLevel,
    bargeInDetected,
    resetBargeIn,
  };
}
