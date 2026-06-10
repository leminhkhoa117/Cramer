import React, { useState } from 'react';
import { FiChevronDown, FiChevronUp, FiHelpCircle } from 'react-icons/fi';
import { explanationToString, parseExplanation } from './questionParsers';

export default function ExplanationEditor({ explanation, onChange }) {
    const [isExpanded, setIsExpanded] = useState(false);
    const structured = parseExplanation(explanation);

    const handleFieldChange = (field, value) => {
        const newStructured = { ...structured, [field]: value };
        onChange(explanationToString(newStructured));
    };

    return (
        <div className="qem-editor-section qem-editor-section--collapsible">
            <button
                type="button"
                className="qem-editor-header qem-editor-header--clickable"
                onClick={() => setIsExpanded(!isExpanded)}
            >
                <div className="qem-editor-header-left">
                    <FiHelpCircle />
                    <span>Giải thích (tùy chọn)</span>
                </div>
                {isExpanded ? <FiChevronUp /> : <FiChevronDown />}
            </button>

            {isExpanded && (
                <div className="qem-explanation-fields">
                    <div className="form-group">
                        <label>Giải thích chi tiết</label>
                        <textarea
                            className="form-textarea"
                            rows={4}
                            value={structured.detail}
                            onChange={(event) => handleFieldChange('detail', event.target.value)}
                            placeholder="Giải thích tại sao đáp án này đúng..."
                        />
                    </div>

                    <div className="form-group">
                        <label>Trích dẫn từ bài đọc/nghe</label>
                        <textarea
                            className="form-textarea"
                            rows={2}
                            value={structured.quote}
                            onChange={(event) => handleFieldChange('quote', event.target.value)}
                            placeholder="Trích dẫn câu văn hoặc đoạn liên quan (giữ nguyên tiếng Anh)..."
                        />
                    </div>

                    <div className="form-group">
                        <label>Chiến lược làm bài</label>
                        <textarea
                            className="form-textarea"
                            rows={2}
                            value={structured.strategy}
                            onChange={(event) => handleFieldChange('strategy', event.target.value)}
                            placeholder="Mẹo hoặc chiến lược để làm dạng bài này..."
                        />
                    </div>
                </div>
            )}
        </div>
    );
}