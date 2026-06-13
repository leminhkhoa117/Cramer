import { cn } from '../lib/cn';
import { FiInfo, FiCheckCircle, FiAlertTriangle, FiAlertCircle, FiX } from 'react-icons/fi';

const VARIANTS = {
  info: { wrap: 'bg-info-soft border-info/20 text-info', Icon: FiInfo },
  success: { wrap: 'bg-success-soft border-success/20 text-success', Icon: FiCheckCircle },
  warning: { wrap: 'bg-warning-soft border-warning/20 text-warning', Icon: FiAlertTriangle },
  danger: { wrap: 'bg-danger-soft border-danger/20 text-danger', Icon: FiAlertCircle },
};

/** Alert / inline message primitive (SPEC-F00 §6). */
export function Alert({ variant = 'info', title, children, dismissible = false, onDismiss, className }) {
  const { wrap, Icon } = VARIANTS[variant] || VARIANTS.info;
  return (
    <div role="alert" className={cn('flex gap-3 rounded-lg border p-3', wrap, className)}>
      <Icon className="mt-0.5 shrink-0" size={18} />
      <div className="flex-1 min-w-0">
        {title && <div className="font-bold text-ink">{title}</div>}
        {children && <div className={cn('text-base text-ink-2', title && 'mt-0.5')}>{children}</div>}
      </div>
      {dismissible && (
        <button type="button" aria-label="Dismiss" onClick={onDismiss} className="shrink-0 text-current opacity-70 hover:opacity-100">
          <FiX size={16} />
        </button>
      )}
    </div>
  );
}

export default Alert;
