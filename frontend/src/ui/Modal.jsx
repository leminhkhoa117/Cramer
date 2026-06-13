import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { FiX } from 'react-icons/fi';
import { cn } from '../lib/cn';
import { IconButton } from './IconButton';

const SIZES = { sm: 'max-w-sm', md: 'max-w-md', lg: 'max-w-2xl', xl: 'max-w-4xl' };

/**
 * Modal primitive (SPEC-F00 §6) — portal + framer-motion, focus/esc handling.
 * Replaces the legacy BaseModal. Compose with Modal.Footer for actions.
 */
export function Modal({
  open,
  onClose,
  title,
  size = 'md',
  closeOnBackdrop = true,
  showClose = true,
  footer,
  className,
  children,
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e) => e.key === 'Escape' && onClose?.();
    document.addEventListener('keydown', onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = prev;
    };
  }, [open, onClose]);

  return createPortal(
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 flex items-center justify-center p-4"
          style={{ zIndex: 'var(--z-modal)' }}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.15 }}
        >
          <div
            className="absolute inset-0 bg-ink/50 backdrop-blur-sm"
            onClick={closeOnBackdrop ? onClose : undefined}
          />
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-label={typeof title === 'string' ? title : undefined}
            className={cn(
              'relative w-full rounded-2xl bg-surface shadow-xl border border-line flex flex-col max-h-[90vh]',
              SIZES[size],
              className
            )}
            initial={{ opacity: 0, scale: 0.96, y: 8 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: 8 }}
            transition={{ duration: 0.18, ease: 'easeOut' }}
          >
            {(title || showClose) && (
              <div className="flex items-center justify-between gap-3 px-5 py-3.5 border-b border-line">
                {typeof title === 'string' ? <h2 className="text-lg font-bold text-ink">{title}</h2> : title}
                {showClose && (
                  <IconButton aria-label="Close" size="sm" onClick={onClose}>
                    <FiX size={18} />
                  </IconButton>
                )}
              </div>
            )}
            <div className="px-5 py-4 overflow-y-auto cr-scroll">{children}</div>
            {footer && <div className="px-5 py-3.5 border-t border-line flex items-center justify-end gap-2">{footer}</div>}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>,
    document.body
  );
}

export default Modal;
