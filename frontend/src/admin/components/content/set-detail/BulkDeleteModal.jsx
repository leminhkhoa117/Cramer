import { useState } from 'react';
import { FiTrash2, FiX } from 'react-icons/fi';

const CONFIRM_PHRASE = 'I CONFIRM THE DELETION';

export default function BulkDeleteModal({ count, onClose, onConfirm, loading }) {
    const [confirmText, setConfirmText] = useState('');
    const isConfirmValid = confirmText === CONFIRM_PHRASE;

    return (
        <div className="admin-modal-overlay-custom" onClick={(e) => e.target === e.currentTarget && !loading && onClose()}>
            <div className="admin-edit-modal bulk-delete-modal" onClick={e => e.stopPropagation()}>
                <div className="admin-edit-modal-header admin-edit-modal-header--danger">
                    <h2>⚠️ Xóa {count} bài thi</h2>
                    <button className="admin-edit-modal-close" onClick={onClose} disabled={loading}>
                        <FiX size={20} />
                    </button>
                </div>

                <div className="admin-edit-modal-body">
                    <p className="bulk-delete-warning">
                        Hành động này <strong>không thể hoàn tác</strong>. Tất cả {count} bài thi đã chọn
                        và dữ liệu liên quan sẽ bị xóa vĩnh viễn.
                    </p>

                    <div className="form-group">
                        <label>Nhập "<strong>{CONFIRM_PHRASE}</strong>" để xác nhận:</label>
                        <input
                            type="text"
                            className={`form-input ${confirmText && !isConfirmValid ? 'form-input--error' : ''}`}
                            value={confirmText}
                            onChange={(e) => setConfirmText(e.target.value)}
                            placeholder="I CONFIRM THE DELETION"
                            disabled={loading}
                            autoFocus
                        />
                    </div>
                </div>

                <div className="admin-edit-modal-footer">
                    <button
                        className="admin-btn admin-btn--secondary"
                        onClick={onClose}
                        disabled={loading}
                    >
                        Hủy
                    </button>
                    <button
                        className="admin-btn admin-btn--danger"
                        onClick={onConfirm}
                        disabled={loading || !isConfirmValid}
                    >
                        {loading ? (
                            <>
                                <span className="spinner small"></span>
                                Đang xóa...
                            </>
                        ) : (
                            <>
                                <FiTrash2 size={14} />
                                Xóa {count} bài thi
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}