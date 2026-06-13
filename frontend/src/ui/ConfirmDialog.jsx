import { Modal } from './Modal';
import { Button } from './Button';

/**
 * Confirmation dialog (SPEC-F00 §6). Generic confirm/cancel built on Modal.
 */
export function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title = 'Xác nhận',
  message,
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Huỷ',
  variant = 'primary',
  loading = false,
  children,
}) {
  return (
    <Modal
      open={open}
      onClose={loading ? undefined : onClose}
      title={title}
      size="sm"
      closeOnBackdrop={!loading}
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={loading}>
            {cancelLabel}
          </Button>
          <Button variant={variant} onClick={onConfirm} loading={loading}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      {message && <p className="text-base text-ink-2 leading-relaxed">{message}</p>}
      {children}
    </Modal>
  );
}

export default ConfirmDialog;
