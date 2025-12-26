import React from 'react';
import { FiAlertTriangle, FiX, FiCheck, FiTrash, FiAlertCircle } from 'react-icons/fi';
import '../common/AdminModal.css';

/**
 * ConfirmModal - Modal xác nhận cho các hành động quan trọng
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
        danger: <FiTrash />,
        warning: <FiAlertTriangle />,
        info: <FiAlertCircle />,
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
        <div className="modal-overlay" onClick={handleOverlayClick}>
            <div className={`modal-content modal-confirm confirm-${type}`}>
                <button
                    className="modal-close"
                    onClick={onClose}
                    disabled={loading}
                    style={{ position: 'absolute', top: '16px', right: '16px', zIndex: 10 }}
                >
                    <FiX size={20} />
                </button>

                <div className="modal-body">
                    <div className={`confirm-icon ${type}`}>
                        {icons[type]}
                    </div>

                    <h2 className="modal-title" style={{ justifyContent: 'center', fontSize: '1.5rem', marginBottom: '8px' }}>
                        {title}
                    </h2>

                    <p className="confirm-message">
                        {message}
                    </p>

                    {children && (
                        <div style={{
                            textAlign: 'left',
                            background: 'rgba(0,0,0,0.2)',
                            padding: '16px',
                            borderRadius: '8px',
                            marginTop: '16px'
                        }}>
                            {children}
                        </div>
                    )}
                </div>

                <div className="modal-footer">
                    <button
                        className="modal-btn modal-btn-secondary"
                        onClick={onClose}
                        disabled={loading}
                    >
                        {cancelText}
                    </button>
                    <button
                        className={`modal-btn modal-btn-${type === 'danger' ? 'danger' : (type === 'warning' ? 'warning' : 'primary')}`}
                        onClick={onConfirm}
                        disabled={loading}
                        style={type === 'warning' ? { background: 'var(--admin-warning)', color: '#1a1a2e' } : {}}
                    >
                        {loading ? (
                            <span className="modal-spinner"></span>
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
