import { useEffect, useState } from 'react';
import { FiAlertTriangle, FiCheck, FiCode, FiX } from 'react-icons/fi';
import '../common/AdminModal.css';

const toLayoutText = (value) => {
    if (!value) return '';
    if (typeof value === 'string') return value;
    try {
        return JSON.stringify(value, null, 2);
    } catch (error) {
        return '';
    }
};

export default function SectionLayoutModal({
    isOpen,
    onClose,
    onSave,
    initialLayout = null
}) {
    const [layoutText, setLayoutText] = useState('');
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!isOpen) return;
        setLayoutText(toLayoutText(initialLayout));
        setError(null);
    }, [isOpen, initialLayout]);

    if (!isOpen) return null;

    const handleSave = () => {
        const trimmed = layoutText.trim();
        if (!trimmed) {
            onSave(null);
            return;
        }

        try {
            const parsed = JSON.parse(trimmed);
            onSave(parsed);
        } catch (err) {
            setError('JSON không hợp lệ. Vui lòng kiểm tra lại.');
        }
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content modal-xl">
                <div className="modal-header">
                    <h3 className="modal-title">
                        <FiCode className="title-icon" />
                        Section Layout JSON
                    </h3>
                    <button className="modal-close" onClick={onClose}>
                        <FiX size={18} />
                    </button>
                </div>

                <div className="modal-body">
                    <div className="form-group">
                        <label>Layout JSON</label>
                        <textarea
                            className="form-textarea"
                            rows={18}
                            value={layoutText}
                            onChange={(e) => {
                                setLayoutText(e.target.value);
                                setError(null);
                            }}
                            placeholder='{"blocks":[{"block_type":"NOTE_COMPLETION","question_numbers":[1,2,3],"content":{...}}]}'
                        />
                        {error && (
                            <span className="error-text">
                                <FiAlertTriangle size={14} /> {error}
                            </span>
                        )}
                    </div>
                </div>

                <div className="modal-footer">
                    <button className="admin-btn admin-btn--secondary" onClick={onClose}>
                        Hủy
                    </button>
                    <button className="admin-btn admin-btn--primary" onClick={handleSave}>
                        <FiCheck size={16} />
                        Lưu layout
                    </button>
                </div>
            </div>
        </div>
    );
}
