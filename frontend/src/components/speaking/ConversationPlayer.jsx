import React, { useState, useCallback, useRef, useEffect } from 'react';
import { FiPlay, FiPause, FiVolume2 } from 'react-icons/fi';
import '../../css/speaking/conversation-player.css';

/**
 * ConversationPlayer - Full Q&A Conversation Playback Component
 *
 * Plays back the complete conversation between examiner and user,
 * including TTS audio for examiner questions and user recordings.
 *
 * @param {Array} conversation - Array of { type, audioUrl, text, duration, questionId }
 * @param {function} onPlayStateChange - Callback when play state changes
 * @param {number} highlightedQuestionId - Optional: highlight specific question
 */
export default function ConversationPlayer({
  conversation = [],
  onPlayStateChange,
  highlightedQuestionId = null
}) {
  const [currentIndex, setCurrentIndex] = useState(-1);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isLoadingAudio, setIsLoadingAudio] = useState(false);
  const [audioDurations, setAudioDurations] = useState({});
  const [currentTime, setCurrentTime] = useState(0);
  const audioRef = useRef(null);
  const timeUpdateRef = useRef(null);

  // Notify parent of play state changes
  useEffect(() => {
    onPlayStateChange?.(isPlaying);
  }, [isPlaying, onPlayStateChange]);

  /**
   * Format duration from milliseconds to mm:ss
   */
  const formatDuration = (ms) => {
    if (!ms || isNaN(ms)) return '0:00';
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  /**
   * Format duration from seconds to mm:ss
   */
  const formatSeconds = (seconds) => {
    if (!seconds || isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  /**
   * Play a specific item in the conversation
   */
  const playItem = useCallback((index) => {
    const item = conversation[index];

    // Skip items without audio
    if (!item?.audioUrl) {
      // If no audio, try next item with slight delay
      if (index < conversation.length - 1) {
        setTimeout(() => playItem(index + 1), 500);
      } else {
        // End of conversation
        setIsPlaying(false);
        setCurrentIndex(-1);
      }
      return;
    }

    setCurrentIndex(index);
    setIsPlaying(true);
    setIsLoadingAudio(true);
    setCurrentTime(0);

    if (audioRef.current) {
      audioRef.current.src = item.audioUrl;

      audioRef.current.onloadedmetadata = () => {
        setIsLoadingAudio(false);
        // Store actual duration if not provided
        if (!item.duration) {
          setAudioDurations(prev => ({
            ...prev,
            [index]: audioRef.current.duration * 1000
          }));
        }
      };

      audioRef.current.onerror = () => {
        console.error('Failed to load audio:', item.audioUrl);
        setIsLoadingAudio(false);
        // Skip to next on error
        if (index < conversation.length - 1) {
          setTimeout(() => playItem(index + 1), 300);
        } else {
          setIsPlaying(false);
          setCurrentIndex(-1);
        }
      };

      audioRef.current.play().catch(err => {
        console.error('Failed to play audio:', err);
        setIsLoadingAudio(false);
      });
    }
  }, [conversation]);

  /**
   * Handle audio ended event
   */
  const handleAudioEnded = useCallback(() => {
    // Clear time update interval
    if (timeUpdateRef.current) {
      clearInterval(timeUpdateRef.current);
    }

    // Play next item if available
    if (currentIndex < conversation.length - 1) {
      setTimeout(() => playItem(currentIndex + 1), 300);
    } else {
      // End of conversation
      setIsPlaying(false);
      setCurrentIndex(-1);
      setCurrentTime(0);
    }
  }, [currentIndex, conversation.length, playItem]);

  /**
   * Handle time update for progress
   */
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    const handleTimeUpdate = () => {
      setCurrentTime(audio.currentTime);
    };

    audio.addEventListener('timeupdate', handleTimeUpdate);
    return () => audio.removeEventListener('timeupdate', handleTimeUpdate);
  }, []);

  /**
   * Toggle play all conversation
   */
  const handlePlayAll = () => {
    if (isPlaying) {
      audioRef.current?.pause();
      setIsPlaying(false);
    } else if (currentIndex >= 0) {
      // Resume from current position
      audioRef.current?.play().catch(console.error);
      setIsPlaying(true);
    } else {
      // Start from beginning
      playItem(0);
    }
  };

  /**
   * Play/pause single item
   */
  const handlePlaySingle = (index) => {
    if (currentIndex === index && isPlaying) {
      // Pause current
      audioRef.current?.pause();
      setIsPlaying(false);
    } else if (currentIndex === index && !isPlaying) {
      // Resume current
      audioRef.current?.play().catch(console.error);
      setIsPlaying(true);
    } else {
      // Play new item
      playItem(index);
    }
  };

  /**
   * Stop playback completely
   */
  const handleStop = () => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
    }
    setIsPlaying(false);
    setCurrentIndex(-1);
    setCurrentTime(0);
  };

  // Group conversation by question for better display
  const groupedConversation = conversation.reduce((acc, item, index) => {
    const questionId = item.questionId;
    if (!acc[questionId]) {
      acc[questionId] = { examiner: null, user: null, questionId };
    }
    if (item.type === 'examiner') {
      acc[questionId].examiner = { ...item, index };
    } else {
      acc[questionId].user = { ...item, index };
    }
    return acc;
  }, {});

  return (
    <div className="conversation-player">
      {/* Hidden audio element */}
      <audio
        ref={audioRef}
        onEnded={handleAudioEnded}
        preload="auto"
      />

      {/* Header with Play All button */}
      <div className="conversation-player__header">
        <h4 className="conversation-player__title">
          <FiVolume2 size={16} />
          Nghe lại hội thoại
        </h4>
        <div className="conversation-player__controls">
          <button
            className={`conversation-player__play-all ${isPlaying ? 'playing' : ''}`}
            onClick={handlePlayAll}
            disabled={conversation.length === 0}
          >
            {isPlaying ? <FiPause size={16} /> : <FiPlay size={16} />}
            <span>{isPlaying ? 'Tạm dừng' : 'Phát tất cả'}</span>
          </button>
          {isPlaying && (
            <button
              className="conversation-player__stop"
              onClick={handleStop}
            >
              Dừng
            </button>
          )}
        </div>
      </div>

      {/* Conversation Timeline */}
      <div className="conversation-player__timeline">
        {Object.values(groupedConversation).map(({ examiner, user, questionId }) => (
          <div
            key={questionId}
            className={`conversation-player__qa-group ${highlightedQuestionId === questionId ? 'highlighted' : ''}`}
          >
            {/* Examiner Question */}
            {examiner && (
              <div
                className={`conversation-player__item conversation-player__item--examiner ${
                  currentIndex === examiner.index ? 'active' : ''
                } ${currentIndex === examiner.index && isLoadingAudio ? 'loading' : ''}`}
              >
                <div className="conversation-player__item-header">
                  <span className="conversation-player__item-type">
                    <span className="conversation-player__type-icon">🎙️</span>
                    Examiner
                  </span>
                  <button
                    className="conversation-player__item-play"
                    onClick={() => handlePlaySingle(examiner.index)}
                    disabled={!examiner.audioUrl}
                    title={examiner.audioUrl ? 'Play/Pause' : 'No audio available'}
                  >
                    {currentIndex === examiner.index && isPlaying ? (
                      <FiPause size={14} />
                    ) : (
                      <FiPlay size={14} />
                    )}
                  </button>
                </div>
                <p className="conversation-player__item-text">
                  {examiner.text || '(Question text not available)'}
                </p>
                {examiner.audioUrl && (
                  <div className="conversation-player__item-duration">
                    {currentIndex === examiner.index ? (
                      <span className="conversation-player__progress">
                        {formatSeconds(currentTime)} / {formatDuration(examiner.duration || audioDurations[examiner.index])}
                      </span>
                    ) : (
                      <span>{formatDuration(examiner.duration || audioDurations[examiner.index])}</span>
                    )}
                  </div>
                )}
                {!examiner.audioUrl && (
                  <div className="conversation-player__no-audio">
                    Không có audio
                  </div>
                )}
              </div>
            )}

            {/* User Response */}
            {user && (
              <div
                className={`conversation-player__item conversation-player__item--user ${
                  currentIndex === user.index ? 'active' : ''
                } ${currentIndex === user.index && isLoadingAudio ? 'loading' : ''}`}
              >
                <div className="conversation-player__item-header">
                  <span className="conversation-player__item-type">
                    <span className="conversation-player__type-icon">👤</span>
                    Bạn
                  </span>
                  <button
                    className="conversation-player__item-play"
                    onClick={() => handlePlaySingle(user.index)}
                    disabled={!user.audioUrl}
                    title={user.audioUrl ? 'Play/Pause' : 'No audio available'}
                  >
                    {currentIndex === user.index && isPlaying ? (
                      <FiPause size={14} />
                    ) : (
                      <FiPlay size={14} />
                    )}
                  </button>
                </div>
                <p className="conversation-player__item-text">
                  {user.text || '(Transcript not available)'}
                </p>
                {user.audioUrl && (
                  <div className="conversation-player__item-duration">
                    {currentIndex === user.index ? (
                      <span className="conversation-player__progress">
                        {formatSeconds(currentTime)} / {formatDuration(user.duration || audioDurations[user.index])}
                      </span>
                    ) : (
                      <span>{formatDuration(user.duration || audioDurations[user.index])}</span>
                    )}
                  </div>
                )}
                {!user.audioUrl && (
                  <div className="conversation-player__no-audio">
                    Không có bản ghi
                  </div>
                )}
              </div>
            )}
          </div>
        ))}

        {conversation.length === 0 && (
          <div className="conversation-player__empty">
            <p>Chưa có dữ liệu hội thoại</p>
          </div>
        )}
      </div>
    </div>
  );
}
