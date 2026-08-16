import React from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { FiX } from 'react-icons/fi';
import '../../css/common/modal.css';

/**
 * BaseModal - Reusable modal component with glassmorphic styling
 *
 * @param {boolean} isOpen - Controls modal visibility
 * @param {function} onClose - Callback when modal should close
 * @param {string} title - Modal title (optional)
 * @param {React.ReactNode} children - Modal body content
 * @param {React.ReactNode} footer - Modal footer content (buttons)
 * @param {string} size - Modal size: 'sm' | 'md' | 'lg' (default: 'md')
 * @param {boolean} showCloseButton - Show X button in header (default: true)
 * @param {boolean} closeOnBackdropClick - Close when clicking backdrop (default: true)
 * @param {string} className - Additional class for modal content
 */

const backdropVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1 },
};

const modalVariants = {
  hidden: {
    y: -30,
    opacity: 0,
    scale: 0.95
  },
  visible: {
    y: 0,
    opacity: 1,
    scale: 1,
    transition: {
      type: 'spring',
      stiffness: 300,
      damping: 28
    }
  },
  exit: {
    y: 30,
    opacity: 0,
    scale: 0.95,
    transition: {
      duration: 0.2
    }
  },
};

const BaseModal = ({
  isOpen,
  onClose,
  title,
  children,
  footer,
  size = 'md',
  showCloseButton = true,
  closeOnBackdropClick = true,
  className = '',
}) => {
  if (!isOpen) return null;

  const handleBackdropClick = (e) => {
    if (closeOnBackdropClick && e.target === e.currentTarget) {
      onClose?.();
    }
  };

  const sizeClass = size !== 'md' ? `cm-content--${size}` : '';

  return createPortal(
    <AnimatePresence>
      {isOpen && (
        <motion.div
          className="cm-backdrop"
          variants={backdropVariants}
          initial="hidden"
          animate="visible"
          exit="hidden"
          onClick={handleBackdropClick}
        >
          <motion.div
            className={`cm-content ${sizeClass} ${className}`.trim()}
            variants={modalVariants}
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby={title ? 'modal-title' : undefined}
          >
            {/* Header */}
            {(title || showCloseButton) && (
              <div className={`cm-header ${!title ? 'cm-header--no-border' : ''}`}>
                {title && (
                  <h2 id="modal-title" className="cm-title">{title}</h2>
                )}
                {showCloseButton && (
                  <button
                    type="button"
                    className="cm-close-btn"
                    onClick={onClose}
                    aria-label="Đóng"
                  >
                    <FiX />
                  </button>
                )}
              </div>
            )}

            {/* Body */}
            <div className="cm-body">
              {children}
            </div>

            {/* Footer */}
            {footer && (
              <div className="cm-footer">
                {footer}
              </div>
            )}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>,
    document.body
  );
};

export default BaseModal;
