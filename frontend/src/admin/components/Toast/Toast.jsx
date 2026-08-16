import { create } from 'zustand';
import { FiCheck, FiX, FiAlertTriangle, FiInfo } from 'react-icons/fi';
import './Toast.css';

// Admin toast store (was a React Context; now Zustand, same API).
const useToastStore = create((set, get) => ({
    toasts: [],
    addToast: (toast) => {
        const id = Date.now() + Math.random();
        const newToast = { id, ...toast };
        set((state) => ({ toasts: [...state.toasts, newToast] }));
        const duration = toast.duration || 4000;
        setTimeout(() => get().removeToast(id), duration);
        return id;
    },
    removeToast: (id) => set((state) => ({
        toasts: state.toasts.filter((toast) => toast.id !== id),
    })),
    success: (message, title) => get().addToast({ type: 'success', message, title }),
    error: (message, title) => get().addToast({ type: 'error', message, title, duration: 6000 }),
    warning: (message, title) => get().addToast({ type: 'warning', message, title }),
    info: (message, title) => get().addToast({ type: 'info', message, title }),
}));

/**
 * Toast item component
 */
function ToastItem({ id, type, message, title, onClose }) {
    const icons = {
        success: <FiCheck size={18} />,
        error: <FiX size={18} />,
        warning: <FiAlertTriangle size={18} />,
        info: <FiInfo size={18} />,
    };

    return (
        <div className={`toast toast--${type}`}>
            <span className={`toast__icon toast__icon--${type}`}>
                {icons[type]}
            </span>
            <div className="toast__content">
                {title && <span className="toast__title">{title}</span>}
                <span className="toast__message">{message}</span>
            </div>
            <button className="toast__close" onClick={() => onClose(id)}>
                <FiX size={16} />
            </button>
        </div>
    );
}

/**
 * ToastProvider - Mount once near the admin app root. Renders the toast container.
 */
export function ToastProvider({ children }) {
    const toasts = useToastStore((state) => state.toasts);
    const removeToast = useToastStore((state) => state.removeToast);

    return (
        <>
            {children}

            {/* Toast Container */}
            <div className="toast-container">
                {toasts.map((toast) => (
                    <ToastItem
                        key={toast.id}
                        {...toast}
                        onClose={removeToast}
                    />
                ))}
            </div>
        </>
    );
}

/**
 * useToast - Hook để sử dụng toast
 *
 * @example
 * const toast = useToast();
 * toast.success('Đã lưu thành công!');
 * toast.error('Có lỗi xảy ra!');
 */
export function useToast() {
    return {
        success: useToastStore((s) => s.success),
        error: useToastStore((s) => s.error),
        warning: useToastStore((s) => s.warning),
        info: useToastStore((s) => s.info),
        addToast: useToastStore((s) => s.addToast),
        removeToast: useToastStore((s) => s.removeToast),
    };
}

export default ToastProvider;
