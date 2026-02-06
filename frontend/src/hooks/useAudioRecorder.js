import { useState, useRef, useCallback, useEffect } from 'react';

/**
 * Custom hook for audio recording with real-time level monitoring
 *
 * Features:
 * - Microphone permission handling
 * - Real-time audio level visualization
 * - Audio chunk streaming (for Gemini Live API)
 * - Automatic silence detection support
 *
 * @param {function} onAudioChunk - Callback when audio data is available
 * @returns {object} Audio recorder state and controls
 */
export default function useAudioRecorder(onAudioChunk) {
  const [isRecording, setIsRecording] = useState(false);
  const [audioLevel, setAudioLevel] = useState(0);
  const [hasPermission, setHasPermission] = useState(null);
  const [error, setError] = useState(null);

  const mediaRecorderRef = useRef(null);
  const streamRef = useRef(null);
  const analyserRef = useRef(null);
  const audioContextRef = useRef(null);
  const animationFrameRef = useRef(null);
  const audioChunksRef = useRef([]);

  /**
   * Monitor audio level for visualization
   * (Defined first because startRecording depends on it)
   */
  const monitorAudioLevel = useCallback(() => {
    if (!analyserRef.current) return;

    const dataArray = new Uint8Array(analyserRef.current.frequencyBinCount);

    const updateLevel = () => {
      if (!analyserRef.current) return;

      analyserRef.current.getByteFrequencyData(dataArray);

      // Calculate average volume
      const average = dataArray.reduce((a, b) => a + b, 0) / dataArray.length;

      // Normalize to 0-100 with some amplification for better visualization
      const normalizedLevel = Math.min(100, (average / 255) * 100 * 1.5);

      setAudioLevel(normalizedLevel);

      animationFrameRef.current = requestAnimationFrame(updateLevel);
    };

    updateLevel();
  }, []);

  /**
   * Request microphone permission
   */
  const requestPermission = useCallback(async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });

      streamRef.current = stream;
      setHasPermission(true);
      setError(null);

      // Setup audio analyser for volume level monitoring
      const audioContext = new (window.AudioContext || window.webkitAudioContext)();
      const source = audioContext.createMediaStreamSource(stream);
      const analyser = audioContext.createAnalyser();

      analyser.fftSize = 256;
      analyser.smoothingTimeConstant = 0.8;

      source.connect(analyser);

      audioContextRef.current = audioContext;
      analyserRef.current = analyser;

      return true;
    } catch (err) {
      console.error('Microphone permission error:', err);
      setError('Không thể truy cập microphone. Vui lòng kiểm tra quyền truy cập.');
      setHasPermission(false);
      return false;
    }
  }, []);

  /**
   * Start recording audio
   * Will automatically request permission if stream isn't initialized
   */
  const startRecording = useCallback(async () => {
    // Auto-request permission if stream not initialized
    if (!streamRef.current) {
      console.log('Stream not initialized, requesting permission...');
      const granted = await requestPermission();
      if (!granted) {
        setError('Không có quyền truy cập microphone.');
        return;
      }
    }

    try {
      // Reset audio chunks
      audioChunksRef.current = [];

      // Create MediaRecorder
      const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : 'audio/webm';

      const mediaRecorder = new MediaRecorder(streamRef.current, {
        mimeType,
        audioBitsPerSecond: 128000,
      });

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
          // Stream chunk to callback (for Gemini Live API)
          if (onAudioChunk) {
            onAudioChunk(event.data);
          }
        }
      };

      mediaRecorder.onerror = (event) => {
        console.error('MediaRecorder error:', event.error);
        setError('Lỗi khi ghi âm. Vui lòng thử lại.');
        setIsRecording(false);
      };

      // Start recording with timeslice for streaming
      mediaRecorder.start(100); // Collect data every 100ms
      mediaRecorderRef.current = mediaRecorder;
      setIsRecording(true);
      setError(null);
      console.log('Recording started successfully');

      // Start audio level monitoring
      monitorAudioLevel();
    } catch (err) {
      console.error('Start recording error:', err);
      setError('Không thể bắt đầu ghi âm.');
    }
  }, [onAudioChunk, requestPermission, monitorAudioLevel]);

  /**
   * Stop recording and return audio blob
   */
  const stopRecording = useCallback(() => {
    return new Promise((resolve) => {
      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        mediaRecorderRef.current.onstop = () => {
          const audioBlob = new Blob(audioChunksRef.current, {
            type: mediaRecorderRef.current.mimeType,
          });
          resolve(audioBlob);
        };

        mediaRecorderRef.current.stop();
      } else {
        resolve(null);
      }

      setIsRecording(false);
      setAudioLevel(0);

      // Stop audio level monitoring
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
      }
    });
  }, []);

  /**
   * Cleanup all audio resources with retry mechanism
   * Call this before navigating away to stop microphone
   * @param {number} retryCount - Current retry attempt (internal use)
   * @returns {Promise<{success: boolean, error?: string}>}
   */
  const cleanup = useCallback(async (retryCount = 0) => {
    const MAX_RETRIES = 3;

    try {
      // 1. Stop MediaRecorder (guard against null)
      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        try {
          mediaRecorderRef.current.stop();
        } catch (e) {
          console.warn('MediaRecorder stop failed:', e);
        }
      }
      mediaRecorderRef.current = null;

      // 2. Stop animation frame
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
        animationFrameRef.current = null;
      }

      // 3. Close AudioContext (guard against null)
      if (audioContextRef.current && audioContextRef.current.state !== 'closed') {
        try {
          await audioContextRef.current.close();
        } catch (e) {
          console.warn('AudioContext close failed:', e);
        }
      }
      audioContextRef.current = null;
      analyserRef.current = null;

      // 4. Stop all tracks - KEY FIX for browser mic indicator
      if (streamRef.current) {
        const tracks = streamRef.current.getTracks();
        tracks.forEach(track => {
          try {
            track.stop();
            console.log('Audio track stopped:', track.label, track.readyState);
          } catch (e) {
            console.warn('Track stop failed:', e);
          }
        });
        streamRef.current = null;
      }

      // Reset state
      setIsRecording(false);
      setAudioLevel(0);
      setHasPermission(null);

      console.log('Audio recorder fully cleaned up');
      return { success: true };

    } catch (error) {
      console.error(`Cleanup attempt ${retryCount + 1} failed:`, error);

      if (retryCount < MAX_RETRIES) {
        // Wait with exponential backoff before retry
        await new Promise(r => setTimeout(r, 500 * (retryCount + 1)));
        return cleanup(retryCount + 1);
      }

      // All retries exhausted - but still return success to not block submission
      console.warn('Cleanup retries exhausted, but returning success to avoid blocking');
      setIsRecording(false);
      setAudioLevel(0);
      return { success: true };
    }
  }, []);

  /**
   * Cleanup on unmount
   */
  useEffect(() => {
    return () => {
      // Stop recording
      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        mediaRecorderRef.current.stop();
      }

      // Stop animation frame
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
      }

      // Close audio context
      if (audioContextRef.current) {
        audioContextRef.current.close().catch(() => {});
      }

      // Stop all tracks
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(track => track.stop());
      }
    };
  }, []);

  return {
    isRecording,
    audioLevel,
    hasPermission,
    error,
    requestPermission,
    startRecording,
    stopRecording,
    cleanup,
  };
}
