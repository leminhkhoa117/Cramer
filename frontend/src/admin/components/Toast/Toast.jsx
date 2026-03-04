import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { FiCheck, FiX, FiAlertTriangle, FiInfo } from 'react-icons/fi';
import { registerToastHandler, unregisterToastHandler } from '../../../utils/toast';
import './Toast.css';

// Toast Context
const ToastContext = createContext(null);

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
 * ToastProvider - Wrap ứng dụng để sử dụng toast
 */
export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([]);

    const addToast = useCallback((toast) => {
        const id = Date.now() + Math.random();
        const newToast = { id, ...toast };

        setToasts(prev => [...prev, newToast]);

        // Auto remove after duration
        const duration = toast.duration || 4000;
        setTimeout(() => {
            removeToast(id);
        }, duration);

        return id;
    }, []);

    const removeToast = useCallback((id) => {
        setToasts(prev => prev.filter(toast => toast.id !== id));
    }, []);

    const success = useCallback((message, title) => {
        return addToast({ type: 'success', message, title });
    }, [addToast]);

    const error = useCallback((message, title) => {
        return addToast({ type: 'error', message, title, duration: 6000 });
    }, [addToast]);

    const warning = useCallback((message, title) => {
        return addToast({ type: 'warning', message, title });
    }, [addToast]);

    const info = useCallback((message, title) => {
        return addToast({ type: 'info', message, title });
    }, [addToast]);

    // Register this provider as the global toast handler
    useEffect(() => {
        registerToastHandler({ success, error, warning, info });
        return () => unregisterToastHandler();
    }, [success, error, warning, info]);

    return (
        <ToastContext.Provider value={{ success, error, warning, info, addToast, removeToast }}>
            {children}

            {/* Toast Container */}
            <div className="toast-container">
                {toasts.map(toast => (
                    <ToastItem
                        key={toast.id}
                        {...toast}
                        onClose={removeToast}
                    />
                ))}
            </div>
        </ToastContext.Provider>
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
    const context = useContext(ToastContext);
    if (!context) {
        throw new Error('useToast must be used within a ToastProvider');
    }
    return context;
}

export default ToastProvider;
