import React from 'react';
import { FiAlertTriangle, FiX, FiCheck, FiTrash, FiAlertCircle } from 'react-icons/fi';
import './ConfirmModal.css';

/**
 * ConfirmModal - Modal xác nhận cho các hành động quan trọng
 * 
 * @param {boolean} isOpen - Trạng thái hiển thị modal
 * @param {function} onClose - Callback khi đóng modal
 * @param {function} onConfirm - Callback khi xác nhận
 * @param {string} title - Tiêu đề modal
 * @param {string} message - Nội dung thông báo
 * @param {string} type - Loại modal: 'danger' | 'warning' | 'info'
 * @param {string} confirmText - Text nút xác nhận
 * @param {string} cancelText - Text nút hủy
 * @param {boolean} loading - Trạng thái loading
 */
export default function ConfirmModal({
    isOpen,
    onClose,
    onConfirm,
    title = 'Xác nhận',
    message = 'Bạn có chắc chắn muốn thực hiện hành động này?',
    type = 'warning',
    confirmText = 'Xác nhận',
    cancelText = 'Hủy',
    loading = false,
    children,
}) {
    if (!isOpen) return null;

    const icons = {
        danger: <FiTrash size={24} />,
        warning: <FiAlertTriangle size={24} />,
        info: <FiAlertCircle size={24} />,
    };

    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget && !loading) {
            onClose();
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Escape' && !loading) {
            onClose();
        }
    };

    React.useEffect(() => {
        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, [loading]);

    return (
        <div className="confirm-modal-overlay" onClick={handleOverlayClick}>
            <div className={`confirm-modal confirm-modal--${type}`}>
                <button
                    className="confirm-modal__close"
                    onClick={onClose}
                    disabled={loading}
                >
                    <FiX size={20} />
                </button>

                <div className={`confirm-modal__icon confirm-modal__icon--${type}`}>
                    {icons[type]}
                </div>

                <h2 className="confirm-modal__title">{title}</h2>
                <p className="confirm-modal__message">{message}</p>

                {children && (
                    <div className="confirm-modal__content">
                        {children}
                    </div>
                )}

                <div className="confirm-modal__actions">
                    <button
                        className="confirm-modal__btn confirm-modal__btn--cancel"
                        onClick={onClose}
                        disabled={loading}
                    >
                        {cancelText}
                    </button>
                    <button
                        className={`confirm-modal__btn confirm-modal__btn--confirm confirm-modal__btn--${type}`}
                        onClick={onConfirm}
                        disabled={loading}
                    >
                        {loading ? (
                            <span className="confirm-modal__loading"></span>
                        ) : (
                            <>
                                <FiCheck size={16} />
                                {confirmText}
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}

/**
 * DeleteConfirmModal - Preset cho xóa
 */
export function DeleteConfirmModal({ isOpen, onClose, onConfirm, itemName, loading }) {
    return (
        <ConfirmModal
            isOpen={isOpen}
            onClose={onClose}
            onConfirm={onConfirm}
            title="Xác nhận xóa"
            message={`Bạn có chắc chắn muốn xóa "${itemName}"? Hành động này không thể hoàn tác.`}
            type="danger"
            confirmText="Xóa"
            loading={loading}
        />
    );
}

/**
 * BanUserConfirmModal - Preset cho cấm người dùng
 */
export function BanUserConfirmModal({ isOpen, onClose, onConfirm, username, loading, children }) {
    return (
        <ConfirmModal
            isOpen={isOpen}
            onClose={onClose}
            onConfirm={onConfirm}
            title="Cấm người dùng"
            message={`Bạn có chắc chắn muốn cấm tài khoản "@${username}"?`}
            type="danger"
            confirmText="Cấm tài khoản"
            loading={loading}
        >
            {children}
        </ConfirmModal>
    );
}

/**
 * PublishConfirmModal - Preset cho xuất bản nội dung
 */
export function PublishConfirmModal({ isOpen, onClose, onConfirm, contentName, loading }) {
    return (
        <ConfirmModal
            isOpen={isOpen}
            onClose={onClose}
            onConfirm={onConfirm}
            title="Xuất bản nội dung"
            message={`Bạn có chắc chắn muốn xuất bản "${contentName}"? Nội dung sẽ hiển thị công khai cho tất cả người dùng.`}
            type="info"
            confirmText="Xuất bản"
            loading={loading}
        />
    );
}
