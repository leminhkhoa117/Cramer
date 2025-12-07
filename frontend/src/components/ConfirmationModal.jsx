import React from 'react';
import BaseModal from './common/BaseModal';

/**
 * ConfirmationModal - Simple confirmation dialog
 * 
 * @param {boolean} isOpen - Modal visibility
 * @param {function} onClose - Close handler
 * @param {function} onConfirm - Confirm action handler
 * @param {string} title - Modal title
 * @param {React.ReactNode} children - Body content
 * @param {string} confirmText - Confirm button text
 * @param {boolean} isConfirming - Loading state
 */
const ConfirmationModal = ({ 
  isOpen, 
  onClose, 
  onConfirm, 
  title, 
  children, 
  confirmText = "Xác nhận", 
  isConfirming = false 
}) => {
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
            disabled={isConfirming}
          >
            Hủy
          </button>
          <button 
            type="button" 
            className="cm-btn cm-btn--primary" 
            onClick={onConfirm} 
            disabled={isConfirming}
          >
            {isConfirming ? (
              <span className="cm-loading">{confirmText}</span>
            ) : confirmText}
          </button>
        </>
      }
    >
      {children}
    </BaseModal>
  );
};

export default ConfirmationModal;
