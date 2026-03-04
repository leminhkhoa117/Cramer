import { useState, useCallback, useRef, useEffect } from 'react';
import { speakingApi } from '../api/speakingApi';

/**
 * Hook for real-time Automatic Speech Recognition (ASR)
 *
 * Push-based design: receives audio chunks from caller and transcribes them
 * periodically. Works with useAudioRecorder's onAudioChunk callback.
 *
 * Features:
 * - Configurable transcription interval (default: 3 seconds)
 * - Audio level gating to prevent hallucination on silence
 * - Cumulative transcript building
 * - Error handling with status tracking
 * - ASR availability checking
 *
 * @param {Object} options - Configuration options
 * @param {number} options.transcriptionInterval - Seconds between transcription requests (default: 3)
 * @param {number} options.minAudioLevel - Minimum audio level (0-100) to consider as speech (default: 10)
 * @param {function} options.onPartialTranscript - Callback when partial transcript is received
 * @param {function} options.onError - Callback when error occurs
 */
export default function useRealtimeASR({
  transcriptionInterval = 3,
  minAudioLevel = 10,
  onPartialTranscript,
  onError,
} = {}) {
  const [isTranscribing, setIsTranscribing] = useState(false);
  const [partialTranscript, setPartialTranscript] = useState('');
  const [fullTranscript, setFullTranscript] = useState('');
  const [error, setError] = useState(null);
  const [asrStatus, setAsrStatus] = useState(null);

  // Audio chunks accumulator
  const audioChunksRef = useRef([]);
  const transcriptionIntervalRef = useRef(null);
  const lastTranscriptRef = useRef('');
  const isTranscribingRef = useRef(false);

  // Audio level tracking to gate ASR (prevent hallucination on silence)
  const peakAudioLevelRef = useRef(0);
  const speechDetectedRef = useRef(false);

  /**
   * Check if ASR service is available
   */
  const checkASRStatus = useCallback(async () => {
    try {
      const response = await speakingApi.getASRStatus();
      const status = response.data.data;
      setAsrStatus(status);
      return status;
    } catch (err) {
      console.error('Failed to check ASR status:', err);
      setAsrStatus({ configured: false, provider: 'unknown', realtimeAvailable: false });
      return null;
    }
  }, []);

  /**
   * Transcribe accumulated audio chunks
   * Only runs if speech was detected (audio level exceeded threshold)
   */
  const transcribeAccumulatedChunks = useCallback(async () => {
    if (audioChunksRef.current.length === 0) return null;

    // CRITICAL: Skip transcription if no speech was detected
    // This prevents ASR hallucination on silence/ambient noise
    if (!speechDetectedRef.current) {
      console.log('No speech detected (peak level below threshold), skipping transcription');
      return null;
    }

    // Combine all chunks into one blob
    const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm;codecs=opus' });

    // Skip if blob is too small (likely silence)
    if (audioBlob.size < 2000) {
      console.log('Audio blob too small (' + audioBlob.size + ' bytes), skipping transcription');
      return null;
    }

    console.log('Transcribing audio: ' + audioBlob.size + ' bytes, peak level: ' + peakAudioLevelRef.current);

    try {
      const response = await speakingApi.transcribeAudio(audioBlob);

      if (response.data.success) {
        const transcript = response.data.data.transcript;

        if (transcript && transcript !== '[silence]' && transcript !== lastTranscriptRef.current) {
          lastTranscriptRef.current = transcript;

          // Append to full transcript
          setFullTranscript((prev) => {
            const newFull = prev ? `${prev} ${transcript}` : transcript;
            return newFull.trim();
          });

          setPartialTranscript(transcript);

          if (onPartialTranscript) {
            onPartialTranscript(transcript);
          }

          return transcript;
        }
      } else {
        console.warn('Transcription not successful:', response.data.error);
      }
    } catch (err) {
      console.error('Transcription request failed:', err);
      setError(err.message);
      if (onError) {
        onError(err);
      }
    }

    return null;
  }, [onPartialTranscript, onError]);

  /**
   * Push an audio chunk for transcription
   * Call this from useAudioRecorder's onAudioChunk callback
   *
   * @param {Blob} chunk - Audio chunk from MediaRecorder
   * @param {number} audioLevel - Current audio level (0-100) from the recorder
   */
  const pushAudioChunk = useCallback((chunk, audioLevel = 0) => {
    if (chunk && chunk.size > 0 && isTranscribingRef.current) {
      audioChunksRef.current.push(chunk);

      // Track peak audio level for this transcription period
      if (audioLevel > peakAudioLevelRef.current) {
        peakAudioLevelRef.current = audioLevel;
      }

      // Mark speech detected if audio level exceeds threshold
      if (audioLevel >= minAudioLevel) {
        speechDetectedRef.current = true;
      }
    }
  }, [minAudioLevel]);

  /**
   * Start ASR transcription
   * Sets up periodic transcription of accumulated chunks
   */
  const startTranscription = useCallback(async () => {
    // Check ASR status first
    const status = await checkASRStatus();
    if (!status?.configured) {
      console.warn('ASR not configured, transcription disabled');
      setError('ASR service not available');
      return false;
    }

    setIsTranscribing(true);
    isTranscribingRef.current = true;
    setError(null);
    audioChunksRef.current = [];
    lastTranscriptRef.current = '';
    setPartialTranscript('');
    setFullTranscript('');

    // Reset audio level tracking
    peakAudioLevelRef.current = 0;
    speechDetectedRef.current = false;

    // Set up interval for transcription
    transcriptionIntervalRef.current = setInterval(async () => {
      if (audioChunksRef.current.length > 0) {
        await transcribeAccumulatedChunks();
        // Clear chunks and reset audio level tracking for next period
        audioChunksRef.current = [];
        peakAudioLevelRef.current = 0;
        speechDetectedRef.current = false;
      }
    }, transcriptionInterval * 1000);

    console.log('Real-time ASR started with', transcriptionInterval, 'second intervals');
    return true;
  }, [transcriptionInterval, checkASRStatus, transcribeAccumulatedChunks]);

  /**
   * Stop ASR transcription
   * Transcribes remaining chunks and returns final transcript
   *
   * @returns {Promise<string>} Final full transcript
   */
  const stopTranscription = useCallback(async () => {
    setIsTranscribing(false);
    isTranscribingRef.current = false;

    // Clear interval
    if (transcriptionIntervalRef.current) {
      clearInterval(transcriptionIntervalRef.current);
      transcriptionIntervalRef.current = null;
    }

    // Final transcription of remaining chunks
    if (audioChunksRef.current.length > 0) {
      await transcribeAccumulatedChunks();
      audioChunksRef.current = [];
    }

    // Return current full transcript (use ref to get latest value)
    const finalTranscript = fullTranscript;
    console.log('Real-time ASR stopped. Final transcript:', finalTranscript);

    return finalTranscript;
  }, [transcribeAccumulatedChunks, fullTranscript]);

  /**
   * Reset all state
   */
  const reset = useCallback(() => {
    setIsTranscribing(false);
    isTranscribingRef.current = false;
    setPartialTranscript('');
    setFullTranscript('');
    setError(null);
    audioChunksRef.current = [];
    lastTranscriptRef.current = '';

    // Reset audio level tracking
    peakAudioLevelRef.current = 0;
    speechDetectedRef.current = false;

    if (transcriptionIntervalRef.current) {
      clearInterval(transcriptionIntervalRef.current);
      transcriptionIntervalRef.current = null;
    }
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (transcriptionIntervalRef.current) {
        clearInterval(transcriptionIntervalRef.current);
      }
    };
  }, []);

  return {
    // State
    isTranscribing,
    partialTranscript,
    fullTranscript,
    error,
    asrStatus,

    // Actions
    checkASRStatus,
    pushAudioChunk,
    startTranscription,
    stopTranscription,
    reset,
  };
}
