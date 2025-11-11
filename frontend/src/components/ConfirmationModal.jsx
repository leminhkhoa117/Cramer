import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import '../css/ConfirmationModal.css';

const ConfirmationModal = ({ isOpen, onClose, onConfirm, title, children }) => {
  if (!isOpen) {
    return null;
  }

  return (
    <AnimatePresence>
      <div className="confirmation-modal-backdrop" onClick={onClose}>
        <motion.div
          className="confirmation-modal-content"
          onClick={(e) => e.stopPropagation()}
          initial={{ y: -50, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: 50, opacity: 0 }}
          transition={{ type: 'spring', stiffness: 300, damping: 30 }}
        >
          <h2 className="confirmation-modal-title">{title}</h2>
          <div className="confirmation-modal-body">
            {children}
          </div>
          <div className="confirmation-modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Hủy
            </button>
            <button type="button" className="btn btn-primary" onClick={onConfirm}>
              Xác nhận
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};

export default ConfirmationModal;
