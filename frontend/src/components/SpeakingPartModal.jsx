import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiMic, FiClock, FiMessageSquare } from 'react-icons/fi';
import BaseModal from './common/BaseModal';
import { useSpeakingStore } from '../stores';
import './SpeakingPartModal.css';

/**
 * SpeakingPartModal - Modal for selecting Speaking test part
 *
 * Displayed when user clicks Speaking card on CourseDetailPage.
 * Allows selection of Full Test, Part 1, Part 2, Part 3, or Part 2+3.
 *
 * @param {boolean} isOpen - Controls modal visibility
 * @param {function} onClose - Callback when modal closes
 * @param {string} courseName - Course name (e.g., 'cam13')
 * @param {number} testNumber - Test number (e.g., 1)
 */

const PART_OPTIONS = [
  {
    id: 'FULL',
    title: 'Full Test',
    subtitle: '11-14 phút',
    icon: '🎯',
    description: 'Part 1 + Part 2 + Part 3',
    duration: '11-14',
  },
  {
    id: 'PART_1',
    title: 'Part 1',
    subtitle: '4-5 phút',
    icon: '👋',
    description: 'Introduction & Interview',
    duration: '4-5',
  },
  {
    id: 'PART_2',
    title: 'Part 2',
    subtitle: '3-4 phút',
    icon: '📝',
    description: 'Long Turn (Cue Card)',
    duration: '3-4',
  },
  {
    id: 'PART_3',
    title: 'Part 3',
    subtitle: '4-5 phút',
    icon: '💬',
    description: 'Discussion',
    duration: '4-5',
  },
  {
    id: 'PART_2_3',
    title: 'Part 2 + 3',
    subtitle: '7-9 phút',
    icon: '🔄',
    description: 'Long Turn + Discussion',
    duration: '7-9',
  },
];

export default function SpeakingPartModal({
  isOpen,
  onClose,
  courseName,
  testNumber,
}) {
  const navigate = useNavigate();
  const [selectedPart, setSelectedPart] = useState(null);
  const [isStarting, setIsStarting] = useState(false);

  const startSession = useSpeakingStore(state => state.startSession);

  /**
   * Format course name for display
   */
  const formatCourseName = (name) => {
    if (!name) return '';
    // Convert 'cam13' to 'CAM 13'
    const match = name.match(/^([a-zA-Z]+)(\d+)$/);
    if (match) {
      return `${match[1].toUpperCase()} ${match[2]}`;
    }
    return name.toUpperCase();
  };

  /**
   * Handle part selection
   */
  const handleSelectPart = (partId) => {
    setSelectedPart(partId);
  };

  /**
   * Handle start button click
   */
  const handleStart = () => {
    if (!selectedPart) return;

    setIsStarting(true);

    // Store source context and navigate
    const sourceContext = { courseName, testNumber };

    // Initialize the speaking store with mode and context
    startSession(selectedPart, sourceContext);

    // Navigate to speaking session with query params for context
    const searchParams = new URLSearchParams({
      source: courseName,
      test: testNumber.toString(),
    });

    navigate(`/speaking/session/${selectedPart.toLowerCase()}?${searchParams.toString()}`);
  };

  /**
   * Handle modal close
   */
  const handleClose = () => {
    setSelectedPart(null);
    setIsStarting(false);
    onClose();
  };

  const modalTitle = `IELTS Speaking - ${formatCourseName(courseName)} Test ${testNumber}`;

  const footer = (
    <>
      <button
        type="button"
        className="cm-btn cm-btn--secondary"
        onClick={handleClose}
        disabled={isStarting}
      >
        Hủy
      </button>
      <button
        type="button"
        className="cm-btn cm-btn--primary"
        onClick={handleStart}
        disabled={!selectedPart || isStarting}
      >
        {isStarting ? (
          <span className="cm-loading">Đang khởi tạo...</span>
        ) : (
          <>
            <FiMic />
            <span>Bắt đầu</span>
          </>
        )}
      </button>
    </>
  );

  return (
    <BaseModal
      isOpen={isOpen}
      onClose={handleClose}
      title={modalTitle}
      footer={footer}
      size="lg"
      className="speaking-part-modal"
      closeOnBackdropClick={!isStarting}
    >
      <p className="speaking-part-modal__description">
        Chọn phần bạn muốn luyện tập. AI Examiner sẽ tương tác với bạn như một giám khảo thật.
      </p>

      <div className="speaking-part-modal__grid">
        {PART_OPTIONS.map((option) => (
          <button
            key={option.id}
            type="button"
            className={`speaking-part-card ${selectedPart === option.id ? 'selected' : ''}`}
            onClick={() => handleSelectPart(option.id)}
            disabled={isStarting}
          >
            <span className="speaking-part-card__icon">{option.icon}</span>
            <span className="speaking-part-card__title">{option.title}</span>
            <span className="speaking-part-card__subtitle">{option.subtitle}</span>
            <span className="speaking-part-card__description">{option.description}</span>
          </button>
        ))}
      </div>

      <div className="speaking-part-modal__info">
        <div className="speaking-part-modal__info-item">
          <FiMic className="speaking-part-modal__info-icon" />
          <span>Yêu cầu microphone</span>
        </div>
        <div className="speaking-part-modal__info-item">
          <FiClock className="speaking-part-modal__info-icon" />
          <span>Thời gian thực như thi thật</span>
        </div>
        <div className="speaking-part-modal__info-item">
          <FiMessageSquare className="speaking-part-modal__info-icon" />
          <span>AI phản hồi chi tiết sau khi hoàn thành</span>
        </div>
      </div>
    </BaseModal>
  );
}
