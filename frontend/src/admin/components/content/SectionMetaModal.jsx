import React, { useEffect, useState } from 'react';
import { FiCheck, FiImage, FiX } from 'react-icons/fi';
import '../common/AdminModal.css';

export default function SectionMetaModal({
    isOpen,
    onClose,
    onSave,
    initialValues = {},
    skill = 'reading'
}) {
    const [displayContentUrl, setDisplayContentUrl] = useState('');
    const [imageDescription, setImageDescription] = useState('');

    useEffect(() => {
        if (!isOpen) return;
        setDisplayContentUrl(initialValues.displayContentUrl || '');
        setImageDescription(initialValues.imageDescription || '');
    }, [isOpen, initialValues]);

    if (!isOpen) return null;

    const handleSubmit = (e) => {
        e.preventDefault();
        onSave({
            displayContentUrl: displayContentUrl.trim() || null,
            imageDescription: imageDescription.trim() || null
        });
    };

    const isWriting = skill === 'writing';
    const title = isWriting ? 'Asset cho Writing Task' : 'Asset hiển thị';

    return (
        <div className="modal-overlay">
            <div className="modal-content modal-lg">
                <div className="modal-header">
                    <h3 className="modal-title">
                        <FiImage className="title-icon" />
                        {title}
                    </h3>
                    <button className="modal-close" onClick={onClose}>
                        <FiX size={18} />
                    </button>
                </div>

                <form className="modal-body" onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Display Content URL</label>
                        <input
                            className="form-input"
                            value={displayContentUrl}
                            onChange={(e) => setDisplayContentUrl(e.target.value)}
                            placeholder="https://... (image/PDF)"
                        />
                        <small className="form-hint">Dùng cho ảnh minh họa (Listening map, Writing Task 1, ...)</small>
                    </div>

                    <div className="form-group">
                        <label>Image Description (Optional)</label>
                        <textarea
                            className="form-textarea"
                            rows={4}
                            value={imageDescription}
                            onChange={(e) => setImageDescription(e.target.value)}
                            placeholder="Mô tả chi tiết hình ảnh để hỗ trợ AI/preview..."
                        />
                    </div>
                </form>

                <div className="modal-footer">
                    <button className="admin-btn admin-btn--secondary" onClick={onClose}>
                        Hủy
                    </button>
                    <button className="admin-btn admin-btn--primary" onClick={handleSubmit}>
                        <FiCheck size={16} />
                        Lưu asset
                    </button>
                </div>
            </div>
        </div>
    );
}
