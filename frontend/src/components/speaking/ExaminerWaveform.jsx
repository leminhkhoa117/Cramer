import React from 'react';
import './../../css/speaking/examiner-waveform.css';

/**
 * ExaminerWaveform - Animated waveform indicator when examiner TTS is playing
 *
 * @param {boolean} isAnimating - Animation active state (examiner is speaking)
 * @param {boolean} isLoading - Loading state (audio is loading)
 */
export default function ExaminerWaveform({ isAnimating = false, isLoading = false }) {
  const barCount = 5;

  return (
    <div
      className={`examiner-waveform ${isAnimating ? 'examiner-waveform--animating' : ''} ${isLoading ? 'examiner-waveform--loading' : ''}`}
      role="img"
      aria-label={isLoading ? 'Đang tải âm thanh' : isAnimating ? 'Đang phát âm thanh' : 'Âm thanh'}
    >
      {[...Array(barCount)].map((_, i) => (
        <div
          key={i}
          className="examiner-waveform__bar"
          style={{ animationDelay: `${i * 0.1}s` }}
        />
      ))}
    </div>
  );
}
