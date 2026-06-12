import { create } from 'zustand';
import { AnimatePresence, motion } from 'framer-motion';
import { FiCheckCircle, FiAlertCircle, FiInfo, FiAlertTriangle, FiX } from 'react-icons/fi';
import { cn } from '../lib/cn';

let idSeq = 0;

const useToastStore = create((set, get) => ({
  toasts: [],
  push: (toast) => {
    const id = ++idSeq;
    const t = { id, duration: 4000, variant: 'info', ...toast };
    set((s) => ({ toasts: [...s.toasts, t] }));
    if (t.duration > 0) setTimeout(() => get().dismiss(id), t.duration);
    return id;
  },
  dismiss: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}));

/** Imperative toast API (SPEC-F00 §6). Replaces utils/toast.js. */
export const toast = {
  show: (message, opts = {}) => useToastStore.getState().push({ message, ...opts }),
  success: (message, opts = {}) => useToastStore.getState().push({ message, variant: 'success', ...opts }),
  error: (message, opts = {}) => useToastStore.getState().push({ message, variant: 'danger', duration: 6000, ...opts }),
  info: (message, opts = {}) => useToastStore.getState().push({ message, variant: 'info', ...opts }),
  warning: (message, opts = {}) => useToastStore.getState().push({ message, variant: 'warning', ...opts }),
};

const ICONS = { success: FiCheckCircle, danger: FiAlertCircle, warning: FiAlertTriangle, info: FiInfo };
const STYLES = {
  success: 'text-success',
  danger: 'text-danger',
  warning: 'text-warning',
  info: 'text-info',
};

/** Toaster — mount once near the app root. */
export function Toaster() {
  const toasts = useToastStore((s) => s.toasts);
  const dismiss = useToastStore((s) => s.dismiss);
  return (
    <div
      className="fixed bottom-4 right-4 flex flex-col gap-2 w-[min(360px,calc(100vw-2rem))]"
      style={{ zIndex: 'var(--z-toast)' }}
    >
      <AnimatePresence>
        {toasts.map((t) => {
          const Icon = ICONS[t.variant] || FiInfo;
          return (
            <motion.div
              key={t.id}
              layout
              initial={{ opacity: 0, x: 24, scale: 0.98 }}
              animate={{ opacity: 1, x: 0, scale: 1 }}
              exit={{ opacity: 0, x: 24, scale: 0.98 }}
              transition={{ duration: 0.18 }}
              className="flex items-start gap-3 rounded-lg border border-line bg-surface p-3 shadow-lg"
            >
              <Icon className={cn('mt-0.5 shrink-0', STYLES[t.variant])} size={18} />
              <div className="flex-1 min-w-0">
                {t.title && <div className="font-bold text-ink">{t.title}</div>}
                <div className="text-base text-ink-2 break-words">{t.message}</div>
              </div>
              <button aria-label="Dismiss" onClick={() => dismiss(t.id)} className="shrink-0 text-faint hover:text-ink-2">
                <FiX size={16} />
              </button>
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
}

export default toast;
