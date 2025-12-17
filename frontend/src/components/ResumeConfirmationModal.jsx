import React from 'react';
import BaseModal from './common/BaseModal';

/**
 * ResumeConfirmationModal - Resume or start new test attempt
 * 
 * @param {boolean} isOpen - Modal visibility
 * @param {function} onResume - Resume/view result handler
 * @param {function} onStartNew - Start new attempt handler
 * @param {boolean} isStartingNew - Loading state
 * @param {string} attemptStatus - 'IN_PROGRESS' | 'COMPLETED'
 */
const ResumeConfirmationModal = ({ 
  isOpen, 
  onResume, 
  onStartNew, 
  isStartingNew, 
  attemptStatus = 'IN_PROGRESS' 
}) => {
  const isCompleted = attemptStatus === 'COMPLETED';
  const title = isCompleted ? 'Đã có bài làm trước đó' : 'Bài làm đang dang dở';
  const message = isCompleted 
    ? 'Bạn đã hoàn thành bài test này trước đó. Bạn muốn xem kết quả hay làm bài mới?'
    : 'Chúng tôi tìm thấy một lần làm bài chưa hoàn thành cho bài test này. Bạn muốn tiếp tục hay bắt đầu một bài mới?';
  const resumeButtonText = isCompleted ? 'Xem kết quả' : 'Tiếp tục làm bài';

  return (
    <BaseModal
      isOpen={isOpen}
      onClose={() => {}} // No close on backdrop - user must choose
      title={title}
      showCloseButton={false}
      closeOnBackdropClick={false}
      footer={
        <>
          <button 
            type="button"
            className="cm-btn cm-btn--secondary" 
            onClick={onStartNew}
            disabled={isStartingNew}
          >
            {isStartingNew ? (
              <span className="cm-loading">Đang tạo...</span>
            ) : 'Làm bài mới'}
          </button>
          <button 
            type="button"
            className="cm-btn cm-btn--primary" 
            onClick={onResume}
            disabled={isStartingNew}
          >
            {resumeButtonText}
          </button>
        </>
      }
    >
      <p className="cm-description">{message}</p>
    </BaseModal>
  );
};

export default ResumeConfirmationModal;
