import { useState } from 'react';
import { FiX } from 'react-icons/fi';

export default function BulkDifficultyModal({ count, onClose, onConfirm, loading }) {
    const [difficulty, setDifficulty] = useState('INTERMEDIATE');

    return (
        <div className="admin-modal-overlay-custom" onClick={(e) => e.target === e.currentTarget && !loading && onClose()}>
            <div className="admin-edit-modal" onClick={e => e.stopPropagation()}>
                <div className="admin-edit-modal-header">
                    <h2>Đổi độ khó ({count} bài thi)</h2>
                    <button className="admin-edit-modal-close" onClick={onClose} disabled={loading}>
                        <FiX size={20} />
                    </button>
                </div>

                <div className="admin-edit-modal-body">
                    <div className="form-group">
                        <label htmlFor="bulk-difficulty">Độ khó mới</label>
                        <select
                            id="bulk-difficulty"
                            className="form-select"
                            value={difficulty}
                            onChange={(e) => setDifficulty(e.target.value)}
                            disabled={loading}
                        >
                            <option value="BEGINNER">Cơ bản (Beginner)</option>
                            <option value="LOWER_INTERMEDIATE">Thấp-Trung bình (Lower-Intermediate)</option>
                            <option value="INTERMEDIATE">Trung bình (Intermediate)</option>
                            <option value="UPPER_INTERMEDIATE">Cao-Trung bình (Upper-Intermediate)</option>
                            <option value="ADVANCED">Nâng cao (Advanced)</option>
                        </select>
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
                        className="admin-btn admin-btn--primary"
                        onClick={() => onConfirm(difficulty)}
                        disabled={loading}
                    >
                        {loading ? 'Đang cập nhật...' : `Cập nhật ${count} bài thi`}
                    </button>
                </div>
            </div>
        </div>
    );
}