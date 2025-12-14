import React from 'react';
import BaseModal from './common/BaseModal';

/**
 * ExitTestModal - Exit test with save/abort options
 * 
 * @param {boolean} isOpen - Modal visibility
 * @param {function} onClose - Close/back handler
 * @param {function} onAbort - Abort test handler
 * @param {function} onSaveAndExit - Save and exit handler
 * @param {boolean} isSaving - Loading state
 */
const ExitTestModal = ({ isOpen, onClose, onAbort, onSaveAndExit, isSaving }) => {
  return (
    <BaseModal
      isOpen={isOpen}
      onClose={onClose}
      title="Thoát bài thi"
      showCloseButton={false}
      size="lg"
      footer={
        <>
          <button 
            type="button"
            className="cm-btn cm-btn--secondary" 
            onClick={onClose} 
            disabled={isSaving}
          >
            Quay lại
          </button>
          <button 
            type="button"
            className="cm-btn cm-btn--danger" 
            onClick={onAbort} 
            disabled={isSaving}
          >
            Huỷ bài
          </button>
          <button 
            type="button"
            className="cm-btn cm-btn--primary" 
            onClick={onSaveAndExit} 
            disabled={isSaving}
          >
            {isSaving ? (
              <span className="cm-loading">Đang lưu...</span>
            ) : 'Lưu & Thoát'}
          </button>
        </>
      }
    >
      <p className="cm-description">
        Bạn có muốn lưu tiến trình làm bài để tiếp tục sau không?
      </p>
      <ul className="cm-info-list">
        <li>
          <strong>Lưu & Thoát:</strong> Bạn có thể quay lại và tiếp tục từ nơi đã dừng lại.
        </li>
        <li>
          <strong>Huỷ bài:</strong> Lần làm bài này sẽ bị xóa hoàn toàn.
        </li>
      </ul>
    </BaseModal>
  );
};

export default ExitTestModal;
