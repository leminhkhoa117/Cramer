import React from 'react';
import BaseModal from './common/BaseModal';

/**
 * StartTestModal - Confirmation before starting a Reading/Listening test
 * 
 * @param {boolean} isOpen - Modal visibility
 * @param {function} onClose - Close handler
 * @param {function} onConfirm - Start test handler
 * @param {string} skill - 'listening' | 'reading'
 */
const StartTestModal = ({ isOpen, onClose, onConfirm, skill }) => {
  const isListening = skill === 'listening';
  const title = `Bắt đầu bài thi ${isListening ? "Listening" : "Reading"}`;
  const description = isListening
    ? "Bài thi sẽ bắt đầu ngay khi bạn nhấn nút 'Bắt đầu'. Audio cho mỗi phần sẽ tự động phát theo trình tự. Bạn sẽ chỉ được nghe một lần duy nhất."
    : "Thời gian làm bài sẽ bắt đầu được tính ngay khi bạn nhấn nút. Hãy chắc chắn rằng bạn đã sẵn sàng.";

  return (
    <BaseModal
      isOpen={isOpen}
      onClose={onClose}
      title={title}
      showCloseButton={false}
      footer={
        <>
          <button 
            type="button" 
            className="cm-btn cm-btn--secondary" 
            onClick={onClose}
          >
            Để sau
          </button>
          <button 
            type="button" 
            className="cm-btn cm-btn--primary" 
            onClick={onConfirm}
          >
            Bắt đầu
          </button>
        </>
      }
    >
      <p className="cm-description">{description}</p>
    </BaseModal>
  );
};

export default StartTestModal;
