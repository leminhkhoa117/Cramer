import { useState, useRef, useCallback, useEffect } from 'react';

/**
 * Hook for real-time conversation with Gemini Live API via WebSocket.
 * 
 * This hook manages:
 * - WebSocket connection to backend
 * - Audio streaming (send user audio, receive examiner audio)
 * - Transcription updates
 * - Turn-taking signals
 * 
 * Based on speaking_session_foundations_vi.md:
 * - Live turn-taking: gemini-live-2.5-flash-native-audio
 * - Native audio I/O (no separate TTS pipeline)
 * - Barge-in and turn detection
 * 
 * @param {Object} options Configuration options
 * @param {string} options.sessionId Speaking session ID
 * @param {Function} options.onExaminerAudio Callback when examiner audio received
 * @param {Function} options.onExaminerTranscript Callback when examiner text received
 * @param {Function} options.onUserTranscript Callback when user speech is transcribed
 * @param {Function} options.onExaminerSpeaking Callback when examiner speaking state changes
 * @param {Function} options.onError Callback when error occurs
 * @param {Function} options.onReady Callback when connection is ready
 */
export default function useGeminiLive({
  sessionId,
  onExaminerAudio,
  onExaminerTranscript,
  onUserTranscript,
  onExaminerSpeaking,
  onError,
  onReady,
}) {
  const [isConnected, setIsConnected] = useState(false);
  const [isReady, setIsReady] = useState(false);
  const [isExaminerSpeaking, setIsExaminerSpeaking] = useState(false);
  const [error, setError] = useState(null);

  const wsRef = useRef(null);
  const audioContextRef = useRef(null);
  const audioQueueRef = useRef([]);
  const isPlayingRef = useRef(false);
  const isConnectingRef = useRef(false); // Prevent multiple connection attempts

  /**
   * Connect to the Speaking WebSocket endpoint.
   */
  const connect = useCallback(() => {
    // Prevent multiple connection attempts
    if (isConnectingRef.current) {
      console.log('[GeminiLive] Connection already in progress');
      return;
    }
    
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      console.log('[GeminiLive] Already connected');
      return;
    }
    
    if (wsRef.current?.readyState === WebSocket.CONNECTING) {
      console.log('[GeminiLive] Already connecting');
      return;
    }

    isConnectingRef.current = true;
    
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = import.meta.env.VITE_WS_HOST || window.location.host;
    const wsUrl = `${protocol}//${host}/ws/speaking/${sessionId}`;

    console.log('[GeminiLive] Connecting to:', wsUrl);

    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      console.log('[GeminiLive] WebSocket connected');
      isConnectingRef.current = false;
      setIsConnected(true);
      setError(null);
    };

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        handleMessage(message);
      } catch (e) {
        console.error('[GeminiLive] Failed to parse message:', e);
      }
    };

    ws.onerror = (event) => {
      console.error('[GeminiLive] WebSocket error:', event);
      isConnectingRef.current = false;
      setError('WebSocket connection error');
      onError?.('WebSocket connection error');
    };

    ws.onclose = (event) => {
      console.log('[GeminiLive] WebSocket closed:', event.code, event.reason);
      isConnectingRef.current = false;
      setIsConnected(false);
      setIsReady(false);
      wsRef.current = null;
    };
  }, [sessionId, onError]);

  /**
   * Disconnect from WebSocket.
   */
  const disconnect = useCallback(() => {
    isConnectingRef.current = false;
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    setIsConnected(false);
    setIsReady(false);
  }, []);

  /**
   * Handle incoming WebSocket messages.
   */
  const handleMessage = useCallback((message) => {
    switch (message.type) {
      case 'status':
        console.log('[GeminiLive] Status:', message.status, message.message);
        if (message.status === 'ready') {
          setIsReady(true);
          onReady?.();
        }
        break;

      case 'examiner_audio':
        // Decode and queue audio for playback
        if (message.data) {
          const audioData = base64ToArrayBuffer(message.data);
          queueAudioForPlayback(audioData, message.format);
          onExaminerAudio?.(audioData);
        }
        break;

      case 'transcript':
        if (message.speaker === 'examiner') {
          onExaminerTranscript?.(message.text);
        } else if (message.speaker === 'user') {
          onUserTranscript?.(message.text);
        }
        break;

      case 'examiner_speaking':
        setIsExaminerSpeaking(message.speaking);
        onExaminerSpeaking?.(message.speaking);
        break;

      case 'error':
        console.error('[GeminiLive] Error:', message.message);
        setError(message.message);
        onError?.(message.message);
        break;

      default:
        console.log('[GeminiLive] Unknown message type:', message.type);
    }
  }, [onExaminerAudio, onExaminerTranscript, onUserTranscript, onExaminerSpeaking, onError, onReady]);

  /**
   * Send audio chunk to backend (which forwards to Gemini).
   * @param {ArrayBuffer|Blob} audioData Audio data to send
   */
  const sendAudio = useCallback((audioData) => {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) {
      console.warn('[GeminiLive] Cannot send audio: not connected');
      return;
    }

    if (audioData instanceof Blob) {
      audioData.arrayBuffer().then((buffer) => {
        wsRef.current.send(buffer);
      });
    } else if (audioData instanceof ArrayBuffer) {
      wsRef.current.send(audioData);
    } else {
      console.error('[GeminiLive] Invalid audio data type');
    }
  }, []);

  /**
   * Tell examiner to ask a specific question.
   * @param {string} questionText The question text
   */
  const startQuestion = useCallback((questionText) => {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) {
      console.warn('[GeminiLive] Cannot start question: not connected');
      return;
    }

    wsRef.current.send(JSON.stringify({
      type: 'start_question',
      questionText,
    }));
  }, []);

  /**
   * Signal that user has finished speaking.
   */
  const endTurn = useCallback(() => {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) {
      console.warn('[GeminiLive] Cannot end turn: not connected');
      return;
    }

    wsRef.current.send(JSON.stringify({ type: 'end_turn' }));
  }, []);

  /**
   * Initialize AudioContext for playback.
   */
  const initAudioContext = useCallback(() => {
    if (!audioContextRef.current) {
      audioContextRef.current = new (window.AudioContext || window.webkitAudioContext)({
        sampleRate: 24000, // Gemini outputs 24kHz audio
      });
    }
    return audioContextRef.current;
  }, []);

  /**
   * Queue audio data for sequential playback.
   */
  const queueAudioForPlayback = useCallback((audioData, format) => {
    audioQueueRef.current.push({ data: audioData, format });

    if (!isPlayingRef.current) {
      playNextInQueue();
    }
  }, []);

  /**
   * Play the next audio chunk in the queue.
   */
  const playNextInQueue = useCallback(async () => {
    if (audioQueueRef.current.length === 0) {
      isPlayingRef.current = false;
      return;
    }

    isPlayingRef.current = true;
    const { data } = audioQueueRef.current.shift();

    try {
      const audioContext = initAudioContext();

      // Convert PCM to AudioBuffer
      // Gemini outputs PCM 16-bit signed LE at 24kHz
      const pcmData = new Int16Array(data);
      const audioBuffer = audioContext.createBuffer(1, pcmData.length, 24000);
      const channelData = audioBuffer.getChannelData(0);

      // Convert Int16 to Float32 (-1.0 to 1.0)
      for (let i = 0; i < pcmData.length; i++) {
        channelData[i] = pcmData[i] / 32768;
      }

      // Play the audio
      const source = audioContext.createBufferSource();
      source.buffer = audioBuffer;
      source.connect(audioContext.destination);

      source.onended = () => {
        playNextInQueue();
      };

      source.start();
    } catch (e) {
      console.error('[GeminiLive] Audio playback error:', e);
      playNextInQueue(); // Try next chunk even if this one failed
    }
  }, [initAudioContext]);

  /**
   * Convert base64 string to ArrayBuffer.
   */
  const base64ToArrayBuffer = (base64) => {
    const binaryString = atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
  };

  /**
   * Auto-disconnect on unmount.
   */
  useEffect(() => {
    return () => {
      disconnect();
      if (audioContextRef.current) {
        audioContextRef.current.close();
      }
    };
  }, [disconnect]);

  return {
    // State
    isConnected,
    isReady,
    isExaminerSpeaking,
    error,

    // Actions
    connect,
    disconnect,
    sendAudio,
    startQuestion,
    endTurn,
  };
}
