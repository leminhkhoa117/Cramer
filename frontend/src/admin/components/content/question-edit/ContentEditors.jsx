import React from 'react';
import { FiChevronDown, FiChevronUp, FiFileText, FiInfo, FiPlus, FiTrash2 } from 'react-icons/fi';
import { getNextLetter, optionsToStrings, parseOptionsFromStrings } from './questionParsers';

const TextContentEditor = ({ content, onChange, config, questionNumber }) => {
    const hasSectionTitle = config.contentFields.includes('sectionTitle');

    return (
        <div className="qem-editor-section">
            <div className="qem-editor-header">
                <FiFileText />
                <span>Nội dung câu hỏi</span>
            </div>

            {hasSectionTitle && (
                <div className="form-group">
                    <label>Tiêu đề phần (tùy chọn)</label>
                    <input
                        type="text"
                        className="form-input"
                        value={content.section_title || content.sectionTitle || ''}
                        onChange={(event) => onChange({
                            ...content,
                            section_title: event.target.value,
                        })}
                        placeholder="Ví dụ: Beach, Museum, etc."
                    />
                </div>
            )}

            <div className="form-group">
                <label>
                    Nội dung câu hỏi
                    {config.placeholder && (
                        <span className="qem-hint">
                            (Sử dụng {config.placeholder} để đánh dấu chỗ trống)
                        </span>
                    )}
                </label>
                <textarea
                    className="form-textarea"
                    rows={4}
                    value={content.text || ''}
                    onChange={(event) => onChange({ ...content, text: event.target.value })}
                    placeholder={`Nhập nội dung câu hỏi ${questionNumber}...`}
                />
            </div>

            <div className="qem-tip">
                <FiInfo size={14} />
                <span>
                    Có thể sử dụng HTML: &lt;b&gt;, &lt;i&gt;, &lt;br/&gt; để định dạng.
                    Dùng <code>____</code> để đánh dấu chỗ trống.
                </span>
            </div>
        </div>
    );
};

const BooleanContentEditor = ({ content, onChange, questionNumber }) => (
    <div className="qem-editor-section">
        <div className="qem-editor-header">
            <FiFileText />
            <span>Câu khẳng định</span>
        </div>

        <div className="form-group">
            <label>Nội dung câu khẳng định #{questionNumber}</label>
            <textarea
                className="form-textarea"
                rows={3}
                value={content.text || ''}
                onChange={(event) => onChange({ ...content, text: event.target.value })}
                placeholder="Nhập câu khẳng định để học sinh xác định True/False/Not Given..."
            />
        </div>

        <div className="qem-tip">
            <FiInfo size={14} />
            <span>
                Câu khẳng định phải rõ ràng, có thể đối chiếu với bài đọc/nghe.
            </span>
        </div>
    </div>
);

const ChoiceContentEditor = ({ content, onChange, questionNumber }) => {
    const options = parseOptionsFromStrings(content.options || []);

    const handleAddOption = () => {
        const newLetter = getNextLetter(options);
        const newOptions = [...options, { letter: newLetter, text: '' }];
        onChange({ ...content, options: optionsToStrings(newOptions) });
    };

    const handleRemoveOption = (index) => {
        const newOptions = options.filter((_, optionIndex) => optionIndex !== index);
        const reletteredOptions = newOptions.map((option, optionIndex) => ({
            ...option,
            letter: String.fromCharCode(65 + optionIndex),
        }));
        onChange({ ...content, options: optionsToStrings(reletteredOptions) });
    };

    const handleOptionChange = (index, text) => {
        const newOptions = [...options];
        newOptions[index] = { ...newOptions[index], text };
        onChange({ ...content, options: optionsToStrings(newOptions) });
    };

    const moveOption = (index, direction) => {
        const newIndex = index + direction;
        if (newIndex < 0 || newIndex >= options.length) return;

        const newOptions = [...options];
        [newOptions[index], newOptions[newIndex]] = [newOptions[newIndex], newOptions[index]];
        const reletteredOptions = newOptions.map((option, optionIndex) => ({
            ...option,
            letter: String.fromCharCode(65 + optionIndex),
        }));
        onChange({ ...content, options: optionsToStrings(reletteredOptions) });
    };

    return (
        <div className="qem-editor-section">
            <div className="qem-editor-header">
                <FiFileText />
                <span>Nội dung câu hỏi trắc nghiệm</span>
            </div>

            <div className="form-group">
                <label>Câu hỏi #{questionNumber}</label>
                <textarea
                    className="form-textarea"
                    rows={3}
                    value={content.text || ''}
                    onChange={(event) => onChange({ ...content, text: event.target.value })}
                    placeholder="Nhập nội dung câu hỏi..."
                />
            </div>

            <div className="form-group">
                <label>Các lựa chọn</label>
                <div className="qem-options-list">
                    {options.map((option, index) => (
                        <div key={index} className="qem-option-item">
                            <span className="qem-option-letter">{option.letter}</span>
                            <input
                                type="text"
                                className="form-input qem-option-input"
                                value={option.text}
                                onChange={(event) => handleOptionChange(index, event.target.value)}
                                placeholder={`Lựa chọn ${option.letter}...`}
                            />
                            <div className="qem-option-actions">
                                <button
                                    type="button"
                                    className="qem-icon-btn"
                                    onClick={() => moveOption(index, -1)}
                                    disabled={index === 0}
                                    title="Di chuyển lên"
                                >
                                    <FiChevronUp size={14} />
                                </button>
                                <button
                                    type="button"
                                    className="qem-icon-btn"
                                    onClick={() => moveOption(index, 1)}
                                    disabled={index === options.length - 1}
                                    title="Di chuyển xuống"
                                >
                                    <FiChevronDown size={14} />
                                </button>
                                <button
                                    type="button"
                                    className="qem-icon-btn qem-icon-btn--danger"
                                    onClick={() => handleRemoveOption(index)}
                                    disabled={options.length <= 2}
                                    title="Xóa lựa chọn"
                                >
                                    <FiTrash2 size={14} />
                                </button>
                            </div>
                        </div>
                    ))}
                </div>

                <button
                    type="button"
                    className="qem-add-btn"
                    onClick={handleAddOption}
                    disabled={options.length >= 8}
                >
                    <FiPlus size={14} />
                    Thêm lựa chọn
                </button>
            </div>
        </div>
    );
};

const MatchingContentEditor = ({ content, onChange, questionNumber }) => (
    <div className="qem-editor-section">
        <div className="qem-editor-header">
            <FiFileText />
            <span>Nội dung cần ghép nối</span>
        </div>

        <div className="form-group">
            <label>Mục cần ghép nối #{questionNumber}</label>
            <textarea
                className="form-textarea"
                rows={3}
                value={content.text || ''}
                onChange={(event) => onChange({ ...content, text: event.target.value })}
                placeholder="Nhập nội dung mục cần ghép nối (ví dụ: tên đoạn văn, đặc điểm, etc.)..."
            />
        </div>

        <div className="qem-tip qem-tip--warning">
            <FiInfo size={14} />
            <span>
                <strong>Lưu ý:</strong> Các lựa chọn ghép nối (A, B, C...) được định nghĩa
                ở cấp độ Section, không phải từng câu hỏi. Chỉnh sửa options tại Section Layout.
            </span>
        </div>
    </div>
);

export default function ContentEditors({ config, content, onChange, questionNumber }) {
    if (!config) {
        return (
            <div className="qem-editor-section">
                <div className="qem-empty-state">
                    Vui lòng chọn loại câu hỏi để hiện form chỉnh sửa.
                </div>
            </div>
        );
    }

    switch (config.category) {
        case 'text':
            return (
                <TextContentEditor
                    content={content}
                    onChange={onChange}
                    config={config}
                    questionNumber={questionNumber}
                />
            );
        case 'boolean':
            return (
                <BooleanContentEditor
                    content={content}
                    onChange={onChange}
                    questionNumber={questionNumber}
                />
            );
        case 'choice':
            return (
                <ChoiceContentEditor
                    content={content}
                    onChange={onChange}
                    questionNumber={questionNumber}
                />
            );
        case 'matching':
            return (
                <MatchingContentEditor
                    content={content}
                    onChange={onChange}
                    questionNumber={questionNumber}
                />
            );
        default:
            return (
                <TextContentEditor
                    content={content}
                    onChange={onChange}
                    config={config}
                    questionNumber={questionNumber}
                />
            );
    }
}