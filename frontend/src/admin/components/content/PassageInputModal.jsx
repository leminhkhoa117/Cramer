/**
 * PassageInputModal - Modal for inputting passage text.
 * Supports both text paste and file upload.
 * 
 * @since 2025-12-21 - Content Upload Feature
 */

import { useState, useRef, useEffect } from 'react';
import { FiUpload, FiClipboard, FiX, FiCheck, FiAlertTriangle } from 'react-icons/fi';
import '../../css/common/modal.css';

export default function PassageInputModal({
    isOpen,
    onClose,
    onSave,
    initialText = '',
    title = 'Add Passage',
    minWords = 600,
    maxWords = 1200
}) {
    const [text, setText] = useState(initialText);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const textareaRef = useRef(null);
    const fileInputRef = useRef(null);

    // Sync text state when initialText changes (different section selected) or modal opens
    useEffect(() => {
        if (isOpen) {
            setText(initialText);
            setError(null);
        }
    }, [isOpen, initialText]);

    if (!isOpen) return null;

    // Count words
    const wordCount = text.trim().split(/\s+/).filter(w => w.length > 0).length;
    const isValidLength = wordCount >= minWords && wordCount <= maxWords;

    // Handle paste from clipboard
    const handlePasteFromClipboard = async () => {
        try {
            const clipboardText = await navigator.clipboard.readText();
            if (clipboardText) {
                setText(clipboardText);
                setError(null);
            }
        } catch (err) {
            setError('Không thể đọc từ clipboard. Vui lòng paste thủ công (Ctrl+V)');
        }
    };

    // Handle file upload
    const handleFileUpload = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        // Check file type
        const validTypes = ['text/plain', 'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];

        if (!validTypes.includes(file.type) && !file.name.endsWith('.txt')) {
            setError('Chỉ hỗ trợ file .txt hoặc .doc/.docx');
            return;
        }

        setIsLoading(true);
        const reader = new FileReader();

        reader.onload = (event) => {
            setText(event.target.result);
            setIsLoading(false);
            setError(null);
        };

        reader.onerror = () => {
            setError('Không thể đọc file. Vui lòng thử lại.');
            setIsLoading(false);
        };

        reader.readAsText(file);
    };

    // Handle save
    const handleSave = () => {
        if (!text.trim()) {
            setError('Vui lòng nhập nội dung passage');
            return;
        }

        if (wordCount < minWords) {
            setError(`Passage quá ngắn. Cần ít nhất ${minWords} từ (hiện có ${wordCount} từ)`);
            return;
        }

        onSave(text);
        onClose();
    };

    return (
        <div className="admin-modal-overlay" onClick={onClose}>
            <div className="admin-modal admin-modal--large" onClick={e => e.stopPropagation()}>
                {/* Header */}
                <div className="admin-modal__header">
                    <h3>{title}</h3>
                    <button className="admin-modal__close" onClick={onClose}>
                        <FiX size={20} />
                    </button>
                </div>

                {/* Content */}
                <div className="admin-modal__content">
                    {/* Action buttons */}
                    <div className="passage-input-actions">
                        <button
                            className="admin-btn admin-btn--secondary"
                            onClick={handlePasteFromClipboard}
                        >
                            <FiClipboard size={16} />
                            <span>Paste từ Clipboard</span>
                        </button>
                        <button
                            className="admin-btn admin-btn--secondary"
                            onClick={() => fileInputRef.current?.click()}
                            disabled={isLoading}
                        >
                            <FiUpload size={16} />
                            <span>{isLoading ? 'Đang tải...' : 'Upload File'}</span>
                        </button>
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept=".txt,.doc,.docx"
                            onChange={handleFileUpload}
                            style={{ display: 'none' }}
                        />
                    </div>

                    {/* Textarea */}
                    <textarea
                        ref={textareaRef}
                        className="passage-input-textarea"
                        value={text}
                        onChange={(e) => {
                            setText(e.target.value);
                            setError(null);
                        }}
                        placeholder="Paste hoặc nhập nội dung passage tại đây...

Lưu ý:
• Passage nên có từ 600-1200 từ cho IELTS Reading
• Đảm bảo nội dung được định dạng đúng với các đoạn văn riêng biệt
• Có thể sử dụng HTML tags cho headers và emphasis"
                        rows={15}
                    />

                    {/* Word count */}
                    <div className="passage-input-stats">
                        <span className={`word-count ${isValidLength ? 'valid' : wordCount > maxWords ? 'invalid' : 'warning'}`}>
                            {isValidLength ? <FiCheck size={14} /> : <FiAlertTriangle size={14} />}
                            {wordCount} từ
                            {wordCount < minWords && ` (cần thêm ${minWords - wordCount} từ)`}
                            {wordCount > maxWords && ` (vượt ${wordCount - maxWords} từ)`}
                        </span>
                        <span className="target-range">
                            Mục tiêu: {minWords}-{maxWords} từ
                        </span>
                    </div>

                    {/* Error */}
                    {error && (
                        <div className="passage-input-error">
                            <FiAlertTriangle size={16} />
                            {error}
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="admin-modal__footer">
                    <button className="admin-btn admin-btn--secondary" onClick={onClose}>
                        Hủy
                    </button>
                    <button
                        className="admin-btn admin-btn--primary"
                        onClick={handleSave}
                        disabled={!text.trim()}
                    >
                        <FiCheck size={16} />
                        <span>Lưu Passage</span>
                    </button>
                </div>
            </div>
        </div>
    );
}
