import React, { useEffect, useState, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FiMic, FiStopCircle, FiWifi, FiWifiOff } from 'react-icons/fi';
import { useSpeakingStore, useAuthStore } from '../../stores';
import { speakingApi } from '../../api/speakingApi';
import useAudioRecorder from '../../hooks/useAudioRecorder';
import useSilenceDetection from '../../hooks/useSilenceDetection';
import useGeminiLive from '../../hooks/useGeminiLive';
import ExaminerWaveform from './ExaminerWaveform';
import TimeWarningBadge from './TimeWarningBadge';
import '../../css/speaking/speaking-live.css';

/**
 * GeminiLiveSessionLayout - Real-time conversation with Gemini Live API
 * 
 * This component implements the architecture from speaking_session_foundations_vi.md:
 * - Live turn-taking via WebSocket to Gemini Live API
 * - Native audio I/O (no pre-generated TTS)
 * - Barge-in detection (handled by Gemini)
 * - Real-time transcription
 * 
 * Flow:
 * 1. Component mounts → Connect to WebSocket
 * 2. WebSocket connects to Gemini Live API
 * 3. Send question text → Gemini speaks it (native audio output)
 * 4. User speaks → Audio streams to Gemini → Gemini processes
 * 5. Gemini responds with follow-up (dynamic conversation)
 */
export default function GeminiLiveSessionLayout({ part, isTalking = false }) {
  const {
    sessionId,
    currentQuestion,
    isRecording,
    currentPart,
    questionsBlueprint,
    timeWarningLevel,
    timeRemaining,
    isTimeUp,
    setRecording,
    setAudioLevel,
    setExaminerSpeaking,
    addTranscript,
    saveTranscriptToBackend,
    nextQuestion,
    finishPart,
    tickPartTimer,
    setAudioCleanup,
  } = useSpeakingStore();

  const user = useAuthStore(state => state.user);

  // Local state
  const [isReadyToSpeak, setIsReadyToSpeak] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [examinerText, setExaminerText] = useState('');
  const [connectionStatus, setConnectionStatus] = useState('disconnected');
  const [userTranscript, setUserTranscript] = useState(''); // Live transcript display

  // Refs
  const previousQuestionIdRef = useRef(null);
  const userTranscriptRef = useRef('');
  const fallbackTimerRef = useRef(null); // Fallback timer for text-only mode
  const connectionAttemptedRef = useRef(false); // Prevent reconnection loop

  /**
   * Gemini Live WebSocket connection
   */
  const {
    isConnected,
    isReady,
    isExaminerSpeaking: geminiExaminerSpeaking,
    error: geminiError,
    connect: connectGemini,
    disconnect: disconnectGemini,
    sendAudio,
    startQuestion,
    endTurn,
  } = useGeminiLive({
    sessionId,
    onExaminerAudio: (audioData) => {
      // Audio playback is handled inside the hook
      console.log('[GeminiLive] Received examiner audio chunk');
    },
    onExaminerTranscript: (text) => {
      setExaminerText(prev => prev + text);
      console.log('[GeminiLive] Examiner said:', text);
    },
    onUserTranscript: (text) => {
      // Update user transcript display (from Gemini's ASR)
      setUserTranscript(prev => prev + ' ' + text);
      userTranscriptRef.current = (userTranscriptRef.current || '') + ' ' + text;
      console.log('[GeminiLive] User said:', text);
    },
    onExaminerSpeaking: (speaking) => {
      setExaminerSpeaking(speaking);
      
      // Clear fallback timer since Gemini is responding
      if (speaking && fallbackTimerRef.current) {
        clearTimeout(fallbackTimerRef.current);
        fallbackTimerRef.current = null;
      }
      
      // When examiner stops speaking, enable user recording
      if (!speaking) {
        setTimeout(() => {
          setIsReadyToSpeak(true);
          startRecording();
        }, 300);
      }
    },
    onError: (error) => {
      console.error('[GeminiLive] Error:', error);
      setConnectionStatus('error');
      // Enable fallback text-only mode immediately on error
      setIsReadyToSpeak(true);
    },
    onReady: () => {
      console.log('[GeminiLive] Ready to start conversation');
      setConnectionStatus('ready');
      
      // If there's a current question, ask it
      if (currentQuestion?.text) {
        askCurrentQuestion();
      }
    },
  });

  /**
   * Audio recorder setup - sends chunks to Gemini via WebSocket
   */
  const {
    isRecording: recorderIsRecording,
    audioLevel: recorderAudioLevel,
    hasPermission,
    error: audioError,
    requestPermission,
    startRecording: startRecordingRaw,
    stopRecording: stopRecordingRaw,
    cleanup: cleanupAudioRecorder,
  } = useAudioRecorder((audioChunk) => {
    // Send audio chunks to Gemini via WebSocket
    if (isReady) {
      sendAudio(audioChunk);
    }
  });

  /**
   * Wrapped startRecording
   */
  const startRecording = useCallback(async () => {
    await startRecordingRaw();
  }, [startRecordingRaw]);

  /**
   * Wrapped stopRecording
   */
  const stopRecording = useCallback(async () => {
    const audioBlob = await stopRecordingRaw();
    // Signal end of turn to Gemini
    endTurn();
    return audioBlob;
  }, [stopRecordingRaw, endTurn]);

  /**
   * Silence detection (auto-end after 5s of silence)
   */
  const { isWarning, remainingSeconds, resetSilenceTimer } = useSilenceDetection({
    audioLevel: recorderAudioLevel,
    isActive: recorderIsRecording && !geminiExaminerSpeaking,
    silenceThreshold: 5,
    silenceDuration: 5,
    warningAt: 3,
    onSilenceDetected: handleSilenceDetected,
  });

  /**
   * Ask the current question via Gemini
   */
  const askCurrentQuestion = useCallback(() => {
    if (!currentQuestion?.text) return;
    
    setExaminerText(''); // Clear previous text
    setExaminerSpeaking(true);
    startQuestion(currentQuestion.text);
  }, [currentQuestion?.text, startQuestion, setExaminerSpeaking]);

  /**
   * Connect to Gemini when session starts (only once)
   */
  useEffect(() => {
    // Only attempt connection once per session
    if (sessionId && !connectionAttemptedRef.current) {
      connectionAttemptedRef.current = true;
      setConnectionStatus('connecting');
      connectGemini();
    }
    
    return () => {
      disconnectGemini();
    };
  }, [sessionId, connectGemini, disconnectGemini]);

  /**
   * Handle question changes - ask new question via Gemini
   * With fallback to text-only mode if Gemini doesn't respond within 3 seconds
   */
  useEffect(() => {
    if (!currentQuestion || currentQuestion.id === previousQuestionIdRef.current) {
      return;
    }
    
    previousQuestionIdRef.current = currentQuestion.id;
    setIsReadyToSpeak(false);
    setExaminerText('');
    setUserTranscript('');
    userTranscriptRef.current = '';
    
    // Clear any existing fallback timer
    if (fallbackTimerRef.current) {
      clearTimeout(fallbackTimerRef.current);
    }
    
    if (isReady) {
      askCurrentQuestion();
    }
    
    // Fallback: Enable recording after 3s even if Gemini doesn't respond
    // This allows text-only mode where user sees question and can record
    fallbackTimerRef.current = setTimeout(() => {
      console.log('[GeminiLive] Fallback: Enabling recording without Gemini audio');
      setIsReadyToSpeak(true);
      // Don't auto-start recording - let user click button
    }, 3000);
    
    return () => {
      if (fallbackTimerRef.current) {
        clearTimeout(fallbackTimerRef.current);
      }
    };
  }, [currentQuestion?.id, isReady, askCurrentQuestion]);

  /**
   * Register audio cleanup
   */
  useEffect(() => {
    if (cleanupAudioRecorder) {
      setAudioCleanup(cleanupAudioRecorder);
    }
    return () => setAudioCleanup(null);
  }, [cleanupAudioRecorder, setAudioCleanup]);

  /**
   * Sync state with store
   */
  useEffect(() => {
    setAudioLevel(recorderAudioLevel);
    setRecording(recorderIsRecording);
  }, [recorderAudioLevel, recorderIsRecording, setAudioLevel, setRecording]);

  /**
   * Part timer
   */
  useEffect(() => {
    const interval = setInterval(() => {
      const isTimeExpired = tickPartTimer();
      if (isTimeExpired) {
        if (recorderIsRecording) {
          handleFinishAnswer();
        } else {
          finishPart();
        }
      }
    }, 1000);
    
    return () => clearInterval(interval);
  }, [tickPartTimer, recorderIsRecording, finishPart]);

  /**
   * Handle time up
   */
  useEffect(() => {
    if (isTimeUp && !isUploading) {
      if (recorderIsRecording) {
        handleFinishAnswer();
      } else {
        finishPart();
      }
    }
  }, [isTimeUp, recorderIsRecording, isUploading, finishPart]);

  /**
   * Request mic permission on mount
   */
  useEffect(() => {
    if (hasPermission === null) {
      requestPermission();
    }
  }, [hasPermission, requestPermission]);

  /**
   * Handle silence detected
   */
  function handleSilenceDetected() {
    console.log('Silence detected, auto-ending answer');
    handleFinishAnswer();
  }

  /**
   * Handle user finishing their answer
   */
  const handleFinishAnswer = useCallback(async () => {
    if (!recorderIsRecording || isUploading) return;
    
    setIsUploading(true);
    
    const audioBlob = await stopRecording();
    
    // Save transcript to backend
    if (audioBlob && currentQuestion && sessionId && user?.id) {
      try {
        const audioUrl = await speakingApi.uploadAudio(
          user.id,
          sessionId,
          currentQuestion.id,
          audioBlob
        );
        
        addTranscript({
          questionId: currentQuestion.id,
          text: userTranscriptRef.current || '',
          audioBlob,
          audioUrl,
          part: currentPart,
          questionText: currentQuestion.text,
          examinerText: examinerText,
        });
        
        await saveTranscriptToBackend({
          questionId: currentQuestion.id,
          part: currentPart,
          audioUrl,
          duration: Math.round(audioBlob.size / 16000),
          transcriptText: userTranscriptRef.current || '',
        });
      } catch (err) {
        console.error('Failed to save transcript:', err);
      }
    }
    
    setIsUploading(false);
    setIsReadyToSpeak(false);
    resetSilenceTimer();
    
    // Move to next question or finish part
    if (part === 2 && isTalking) {
      finishPart();
    } else {
      // Gemini will automatically continue the conversation
      // Just wait for next examiner turn
      setTimeout(() => {
        nextQuestion();
      }, 1000);
    }
  }, [
    recorderIsRecording,
    isUploading,
    stopRecording,
    currentQuestion,
    sessionId,
    user,
    currentPart,
    examinerText,
    part,
    isTalking,
    addTranscript,
    saveTranscriptToBackend,
    resetSilenceTimer,
    nextQuestion,
    finishPart,
  ]);

  // Error states
  if (audioError) {
    return (
      <div className="speaking-session-error">
        <h2>Lỗi Microphone</h2>
        <p>{audioError}</p>
        <button onClick={requestPermission}>Thử lại</button>
      </div>
    );
  }

  if (hasPermission === false) {
    return (
      <div className="speaking-session-permission">
        <h2>Yêu cầu quyền truy cập Microphone</h2>
        <p>Vui lòng cho phép truy cập microphone để tiếp tục.</p>
        <button onClick={requestPermission}>Cho phép</button>
      </div>
    );
  }

  return (
    <div className="speaking-live-session">
      {/* Time Warning Badge */}
      <TimeWarningBadge
        warningLevel={timeWarningLevel}
        remainingSeconds={timeRemaining}
      />

      {/* Connection Status */}
      <div className="speaking-connection-status">
        {connectionStatus === 'connecting' && (
          <span className="speaking-connection-status--connecting">
            <FiWifi className="animate-pulse" /> Đang kết nối Gemini Live...
          </span>
        )}
        {connectionStatus === 'ready' && (
          <span className="speaking-connection-status--ready">
            <FiWifi /> Gemini Live sẵn sàng
          </span>
        )}
        {connectionStatus === 'error' && (
          <span className="speaking-connection-status--error">
            <FiWifiOff /> Lỗi kết nối: {geminiError}
          </span>
        )}
      </div>

      {/* Main Content */}
      <div className="speaking-live-session__content">
        {/* Examiner Panel */}
        <motion.div
          className={`speaking-examiner ${geminiExaminerSpeaking ? 'speaking' : ''}`}
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <div className="speaking-examiner__avatar">
            <span className="speaking-examiner__avatar-icon">🤖</span>
            {geminiExaminerSpeaking && (
              <motion.div
                className="speaking-examiner__pulse"
                animate={{ scale: [1, 1.2, 1] }}
                transition={{ repeat: Infinity, duration: 1 }}
              />
            )}
          </div>

          <div className="speaking-examiner__bubble">
            <p className="speaking-examiner__question">
              {examinerText || currentQuestion?.text || 'Đang chờ câu hỏi...'}
            </p>

            {geminiExaminerSpeaking && (
              <div className="speaking-examiner__speaking">
                <ExaminerWaveform isAnimating={true} />
                <span>Đang nói...</span>
              </div>
            )}
          </div>
        </motion.div>

        {/* User Panel */}
        <AnimatePresence mode="wait">
          {(isReadyToSpeak || (currentQuestion && !geminiExaminerSpeaking)) && (
            <motion.div
              className="speaking-user"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
            >
              {/* User Transcript Display */}
              {userTranscript && (
                <div className="speaking-user__transcript">
                  <span className="speaking-user__transcript-label">Bạn nói:</span>
                  <p className="speaking-user__transcript-text">{userTranscript}</p>
                </div>
              )}

              {/* Audio Level Bar */}
              <div className="speaking-user__level-container">
                <span className="speaking-user__level-label">Mức âm thanh</span>
                <div className="speaking-user__level-bar">
                  <motion.div
                    className="speaking-user__level-fill"
                    style={{ width: `${recorderAudioLevel}%` }}
                    animate={{ width: `${recorderAudioLevel}%` }}
                    transition={{ duration: 0.1 }}
                  />
                </div>
              </div>

              {/* Mic Button */}
              <div className="speaking-user__controls">
                <motion.button
                  className={`speaking-user__mic-btn ${recorderIsRecording ? 'recording' : ''} ${isUploading ? 'uploading' : ''}`}
                  onClick={recorderIsRecording ? handleFinishAnswer : startRecording}
                  disabled={isUploading}
                  whileHover={{ scale: isUploading ? 1 : 1.05 }}
                  whileTap={{ scale: isUploading ? 1 : 0.95 }}
                >
                  {isUploading ? (
                    <span className="speaking-user__spinner" />
                  ) : recorderIsRecording ? (
                    <FiStopCircle />
                  ) : (
                    <FiMic />
                  )}
                </motion.button>

                <div className="speaking-user__status">
                  {isUploading ? (
                    <span className="speaking-user__status--uploading">
                      ⏳ Đang lưu câu trả lời...
                    </span>
                  ) : recorderIsRecording ? (
                    <span className="speaking-user__status--recording">
                      🎤 Đang ghi âm... Nhấn để hoàn thành {isWarning && `(${remainingSeconds}s)`}
                    </span>
                  ) : (
                    <span>Nhấn mic để bắt đầu trả lời</span>
                  )}
                </div>
              </div>

              {/* Silence Warning */}
              {isWarning && recorderIsRecording && (
                <motion.div
                  className="speaking-user__silence-warning"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                >
                  ⏱️ Bạn đang im lặng... Sẽ tự động chuyển câu sau {remainingSeconds} giây
                </motion.div>
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
