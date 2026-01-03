import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { 
    FiAlertTriangle, FiCheck, FiChevronDown, FiChevronUp, FiEye, 
    FiPlus, FiTrash2, FiX, FiInfo, FiFileText, FiHelpCircle
} from 'react-icons/fi';
import '../common/AdminModal.css';
import './QuestionEditModal.css';

// ============================================================================
// QUESTION TYPES CONFIGURATION
// ============================================================================
const QUESTION_TYPE_CONFIG = {
    // Text input types (answer is typed word/phrase)
    FILL_IN_BLANK: {
        category: 'text',
        label: 'Fill in Blank',
        contentFields: ['sectionTitle', 'text'],
        answerType: 'text',
        placeholder: '____',
    },
    SUMMARY_COMPLETION: {
        category: 'text',
        label: 'Summary Completion',
        contentFields: ['text'],
        answerType: 'text',
    },
    NOTE_COMPLETION: {
        category: 'text',
        label: 'Note Completion',
        contentFields: ['sectionTitle', 'text'],
        answerType: 'text',
    },
    TABLE_COMPLETION: {
        category: 'text',
        label: 'Table Completion',
        contentFields: ['text'],
        answerType: 'text',
    },
    FLOW_CHART_COMPLETION: {
        category: 'text',
        label: 'Flow Chart Completion',
        contentFields: ['text'],
        answerType: 'text',
    },
    DIAGRAM_LABEL_COMPLETION: {
        category: 'text',
        label: 'Diagram Label',
        contentFields: ['text'],
        answerType: 'text',
    },
    SENTENCE_COMPLETION: {
        category: 'text',
        label: 'Sentence Completion',
        contentFields: ['text'],
        answerType: 'text',
    },
    SHORT_ANSWER: {
        category: 'text',
        label: 'Short Answer',
        contentFields: ['text'],
        answerType: 'text',
    },
    // Boolean types (TRUE/FALSE/NOT GIVEN or YES/NO/NOT GIVEN)
    TRUE_FALSE_NOT_GIVEN: {
        category: 'boolean',
        label: 'True/False/Not Given',
        contentFields: ['text'],
        answerType: 'tfng',
        options: ['TRUE', 'FALSE', 'NOT GIVEN'],
    },
    YES_NO_NOT_GIVEN: {
        category: 'boolean',
        label: 'Yes/No/Not Given',
        contentFields: ['text'],
        answerType: 'ynng',
        options: ['YES', 'NO', 'NOT GIVEN'],
    },
    // Multiple choice types
    MULTIPLE_CHOICE: {
        category: 'choice',
        label: 'Multiple Choice (Single)',
        contentFields: ['text', 'options'],
        answerType: 'single-select',
    },
    MULTIPLE_CHOICE_MULTIPLE_ANSWERS: {
        category: 'choice',
        label: 'Multiple Choice (Multiple)',
        contentFields: ['text', 'options'],
        answerType: 'multi-select',
    },
    // Matching types
    MATCHING: {
        category: 'matching',
        label: 'Matching',
        contentFields: ['text'],
        answerType: 'letter',
    },
    MATCHING_INFORMATION: {
        category: 'matching',
        label: 'Matching Information',
        contentFields: ['text'],
        answerType: 'letter',
    },
    MATCHING_HEADINGS: {
        category: 'matching',
        label: 'Matching Headings',
        contentFields: ['text'],
        answerType: 'letter',
    },
    MATCHING_FEATURES: {
        category: 'matching',
        label: 'Matching Features',
        contentFields: ['text'],
        answerType: 'letter',
    },
    MATCHING_SENTENCE_ENDINGS: {
        category: 'matching',
        label: 'Matching Sentence Endings',
        contentFields: ['text'],
        answerType: 'letter',
    },
    SUMMARY_COMPLETION_OPTIONS: {
        category: 'matching',
        label: 'Summary with Options',
        contentFields: ['text'],
        answerType: 'letter',
    },
    LIST_SELECTION: {
        category: 'matching',
        label: 'List Selection',
        contentFields: ['text'],
        answerType: 'letter',
    },
};

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Safely parse JSON content from various formats
 */
const parseQuestionContent = (content) => {
    if (!content) return { text: '' };
    if (typeof content === 'object') return content;
    try {
        return JSON.parse(content);
    } catch {
        return { text: String(content) };
    }
};

/**
 * Safely parse correct answer from various formats
 */
const parseCorrectAnswer = (answer) => {
    if (!answer) return [];
    if (Array.isArray(answer)) return answer;
    if (typeof answer === 'object') return [answer];
    try {
        const parsed = JSON.parse(answer);
        return Array.isArray(parsed) ? parsed : [parsed];
    } catch {
        return [String(answer)];
    }
};

/**
 * Parse options from "A option text" format to structured format
 */
const parseOptionsFromStrings = (options) => {
    if (!Array.isArray(options)) return [];
    return options.map((opt) => {
        if (typeof opt === 'object' && opt.letter) return opt;
        const str = String(opt);
        const match = str.match(/^([A-Za-z])\s+(.+)$/);
        if (match) {
            return { letter: match[1].toUpperCase(), text: match[2] };
        }
        // Try to extract just the letter at start
        const letterMatch = str.match(/^([A-Za-z])\s*[-.):]?\s*(.*)$/);
        if (letterMatch) {
            return { letter: letterMatch[1].toUpperCase(), text: letterMatch[2] || str };
        }
        return { letter: '', text: str };
    });
};

/**
 * Convert structured options back to string format
 */
const optionsToStrings = (options) => {
    if (!Array.isArray(options)) return [];
    return options.map((opt) => {
        if (typeof opt === 'string') return opt;
        return `${opt.letter} ${opt.text}`;
    });
};

/**
 * Generate next letter in sequence (A, B, C, ...)
 */
const getNextLetter = (options) => {
    if (!options || options.length === 0) return 'A';
    const letters = options.map(o => o.letter).filter(Boolean).sort();
    const lastLetter = letters[letters.length - 1] || '@';
    return String.fromCharCode(lastLetter.charCodeAt(0) + 1);
};

/**
 * Parse explanation into structured format (3 fields)
 * Backend schema: { detail, quote, strategy }
 */
const parseExplanation = (explanation) => {
    if (!explanation) {
        return { detail: '', quote: '', strategy: '' };
    }
    // Try to parse as JSON first (handle both object and JSON string)
    let parsed = explanation;
    if (typeof explanation === 'string') {
        try {
            parsed = JSON.parse(explanation);
        } catch {
            // Plain text - put it all in detail
            return { detail: explanation, quote: '', strategy: '' };
        }
    }
    if (typeof parsed === 'object') {
        return {
            // Support both new English keys and legacy Vietnamese keys for backwards compatibility
            detail: parsed.detail || parsed.giaiThich || parsed.giai_thich || '',
            quote: parsed.quote || parsed.trichDan || parsed.trich_dan || '',
            strategy: parsed.strategy || parsed.chienlược || parsed.chien_luoc || '',
        };
    }
    // Fallback
    return { detail: String(explanation), quote: '', strategy: '' };
};

/**
 * Convert structured explanation to JSON format for backend
 * Output schema: { detail, quote, strategy }
 */
const explanationToString = (structured) => {
    // Return as JSON object with English keys (backend expects JSONB)
    return {
        detail: structured.detail || '',
        quote: structured.quote || '',
        strategy: structured.strategy || '',
    };
};

// ============================================================================
// SUB-COMPONENTS: Content Editors
// ============================================================================

/**
 * Text-based content editor (FILL_IN_BLANK, SUMMARY_COMPLETION, etc.)
 */
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
                        onChange={(e) => onChange({ 
                            ...content, 
                            section_title: e.target.value 
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
                    onChange={(e) => onChange({ ...content, text: e.target.value })}
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

/**
 * Boolean content editor (TRUE_FALSE_NOT_GIVEN, YES_NO_NOT_GIVEN)
 */
const BooleanContentEditor = ({ content, onChange, config, questionNumber }) => {
    return (
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
                    onChange={(e) => onChange({ ...content, text: e.target.value })}
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
};

/**
 * Multiple choice content editor
 */
const ChoiceContentEditor = ({ content, onChange, config, questionNumber }) => {
    const options = parseOptionsFromStrings(content.options || []);
    
    const handleAddOption = () => {
        const newLetter = getNextLetter(options);
        const newOptions = [...options, { letter: newLetter, text: '' }];
        onChange({ ...content, options: optionsToStrings(newOptions) });
    };
    
    const handleRemoveOption = (index) => {
        const newOptions = options.filter((_, i) => i !== index);
        // Re-letter remaining options
        const reletteredOptions = newOptions.map((opt, i) => ({
            ...opt,
            letter: String.fromCharCode(65 + i) // A, B, C, ...
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
        // Re-letter
        const reletteredOptions = newOptions.map((opt, i) => ({
            ...opt,
            letter: String.fromCharCode(65 + i)
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
                    onChange={(e) => onChange({ ...content, text: e.target.value })}
                    placeholder="Nhập nội dung câu hỏi..."
                />
            </div>
            
            <div className="form-group">
                <label>Các lựa chọn</label>
                <div className="qem-options-list">
                    {options.map((opt, index) => (
                        <div key={index} className="qem-option-item">
                            <span className="qem-option-letter">{opt.letter}</span>
                            <input
                                type="text"
                                className="form-input qem-option-input"
                                value={opt.text}
                                onChange={(e) => handleOptionChange(index, e.target.value)}
                                placeholder={`Lựa chọn ${opt.letter}...`}
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

/**
 * Matching content editor
 */
const MatchingContentEditor = ({ content, onChange, config, questionNumber }) => {
    return (
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
                    onChange={(e) => onChange({ ...content, text: e.target.value })}
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
};

// ============================================================================
// SUB-COMPONENTS: Answer Editors
// ============================================================================

/**
 * Text answer editor (for FILL_IN_BLANK, etc.)
 */
const TextAnswerEditor = ({ answer, onChange, questionNumber }) => {
    const primaryAnswer = answer[0] || '';
    const alternatives = answer.slice(1);
    
    const handlePrimaryChange = (value) => {
        onChange([value, ...alternatives]);
    };
    
    const handleAltChange = (index, value) => {
        const newAlts = [...alternatives];
        newAlts[index] = value;
        onChange([primaryAnswer, ...newAlts]);
    };
    
    const handleAddAlt = () => {
        onChange([primaryAnswer, ...alternatives, '']);
    };
    
    const handleRemoveAlt = (index) => {
        const newAlts = alternatives.filter((_, i) => i !== index);
        onChange([primaryAnswer, ...newAlts]);
    };
    
    return (
        <div className="qem-editor-section">
            <div className="qem-editor-header">
                <FiCheck />
                <span>Đáp án đúng</span>
            </div>
            
            <div className="form-group">
                <label>Đáp án chính</label>
                <input
                    type="text"
                    className="form-input"
                    value={primaryAnswer}
                    onChange={(e) => handlePrimaryChange(e.target.value)}
                    placeholder="Nhập đáp án đúng..."
                />
            </div>
            
            {alternatives.length > 0 && (
                <div className="form-group">
                    <label>Đáp án thay thế (chấp nhận)</label>
                    {alternatives.map((alt, index) => (
                        <div key={index} className="qem-alt-answer">
                            <input
                                type="text"
                                className="form-input"
                                value={alt}
                                onChange={(e) => handleAltChange(index, e.target.value)}
                                placeholder="Đáp án thay thế..."
                            />
                            <button 
                                type="button"
                                className="qem-icon-btn qem-icon-btn--danger"
                                onClick={() => handleRemoveAlt(index)}
                            >
                                <FiTrash2 size={14} />
                            </button>
                        </div>
                    ))}
                </div>
            )}
            
            <button 
                type="button"
                className="qem-add-btn qem-add-btn--small"
                onClick={handleAddAlt}
            >
                <FiPlus size={12} />
                Thêm đáp án thay thế
            </button>
        </div>
    );
};

/**
 * Boolean answer editor (TRUE/FALSE/NOT GIVEN, YES/NO/NOT GIVEN)
 */
const BooleanAnswerEditor = ({ answer, onChange, options }) => {
    const selectedAnswer = answer[0] || '';
    
    return (
        <div className="qem-editor-section">
            <div className="qem-editor-header">
                <FiCheck />
                <span>Đáp án đúng</span>
            </div>
            
            <div className="qem-radio-group">
                {options.map((opt) => (
                    <label key={opt} className="qem-radio-option">
                        <input
                            type="radio"
                            name="booleanAnswer"
                            value={opt}
                            checked={selectedAnswer === opt}
                            onChange={(e) => onChange([e.target.value])}
                        />
                        <span className={`qem-radio-label qem-radio-label--${opt.toLowerCase().replace(' ', '-')}`}>
                            {opt}
                        </span>
                    </label>
                ))}
            </div>
        </div>
    );
};

/**
 * Single selection answer editor (for MULTIPLE_CHOICE)
 */
const SingleSelectAnswerEditor = ({ answer, onChange, options }) => {
    const selectedAnswer = answer[0] || '';
    const parsedOptions = parseOptionsFromStrings(options);
    
    return (
        <div className="qem-editor-section">
            <div className="qem-editor-header">
                <FiCheck />
                <span>Chọn đáp án đúng</span>
            </div>
            
            <div className="qem-radio-group qem-radio-group--options">
                {parsedOptions.map((opt) => (
                    <label key={opt.letter} className="qem-radio-option qem-radio-option--full">
                        <input
                            type="radio"
                            name="singleAnswer"
                            value={opt.letter}
                            checked={selectedAnswer === opt.letter}
                            onChange={(e) => onChange([e.target.value])}
                        />
                        <span className="qem-option-letter">{opt.letter}</span>
                        <span className="qem-option-text">{opt.text}</span>
                    </label>
                ))}
            </div>
            
            {parsedOptions.length === 0 && (
                <div className="qem-empty-state">
                    Chưa có lựa chọn nào. Vui lòng thêm options ở phần Nội dung câu hỏi.
                </div>
            )}
        </div>
    );
};

/**
 * Multiple selection answer editor (for MULTIPLE_CHOICE_MULTIPLE_ANSWERS)
 */
const MultiSelectAnswerEditor = ({ answer, onChange, options }) => {
    const selectedAnswers = answer || [];
    const parsedOptions = parseOptionsFromStrings(options);
    
    const handleToggle = (letter) => {
        if (selectedAnswers.includes(letter)) {
            onChange(selectedAnswers.filter(a => a !== letter));
        } else {
            onChange([...selectedAnswers, letter].sort());
        }
    };
    
    return (
        <div className="qem-editor-section">
            <div className="qem-editor-header">
                <FiCheck />
                <span>Chọn tất cả đáp án đúng</span>
            </div>
            
            <div className="qem-checkbox-group">
                {parsedOptions.map((opt) => (
                    <label key={opt.letter} className="qem-checkbox-option">
                        <input
                            type="checkbox"
                            checked={selectedAnswers.includes(opt.letter)}
                            onChange={() => handleToggle(opt.letter)}
                        />
                        <span className="qem-option-letter">{opt.letter}</span>
                        <span className="qem-option-text">{opt.text}</span>
                    </label>
                ))}
            </div>
            
            {selectedAnswers.length > 0 && (
                <div className="qem-selected-summary">
                    Đã chọn: <strong>{selectedAnswers.join(', ')}</strong>
                </div>
            )}
        </div>
    );
};

/**
 * Letter answer editor (for MATCHING types)
 */
const LetterAnswerEditor = ({ answer, onChange, matchingOptions = [] }) => {
    const selectedAnswer = answer[0] || '';
    
    // Generate available letters if no matching options provided
    const availableLetters = matchingOptions.length > 0 
        ? matchingOptions 
        : ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'];
    
    return (
        <div className="qem-editor-section">
            <div className="qem-editor-header">
                <FiCheck />
                <span>Đáp án ghép nối</span>
            </div>
            
            <div className="form-group">
                <label>Chọn lựa chọn đúng</label>
                <select
                    className="form-select"
                    value={selectedAnswer}
                    onChange={(e) => onChange([e.target.value])}
                >
                    <option value="">-- Chọn đáp án --</option>
                    {availableLetters.map((letter) => (
                        <option key={letter} value={letter}>
                            {letter}
                        </option>
                    ))}
                </select>
            </div>
            
            <div className="qem-tip">
                <FiInfo size={14} />
                <span>
                    Đáp án phải khớp với một trong các options được định nghĩa ở Section Layout.
                </span>
            </div>
        </div>
    );
};

// ============================================================================
// SUB-COMPONENT: Explanation Editor
// ============================================================================

const ExplanationEditor = ({ explanation, onChange }) => {
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
                            onChange={(e) => handleFieldChange('detail', e.target.value)}
                            placeholder="Giải thích tại sao đáp án này đúng..."
                        />
                    </div>
                    
                    <div className="form-group">
                        <label>Trích dẫn từ bài đọc/nghe</label>
                        <textarea
                            className="form-textarea"
                            rows={2}
                            value={structured.quote}
                            onChange={(e) => handleFieldChange('quote', e.target.value)}
                            placeholder="Trích dẫn câu văn hoặc đoạn liên quan (giữ nguyên tiếng Anh)..."
                        />
                    </div>
                    
                    <div className="form-group">
                        <label>Chiến lược làm bài</label>
                        <textarea
                            className="form-textarea"
                            rows={2}
                            value={structured.strategy}
                            onChange={(e) => handleFieldChange('strategy', e.target.value)}
                            placeholder="Mẹo hoặc chiến lược để làm dạng bài này..."
                        />
                    </div>
                </div>
            )}
        </div>
    );
};

// ============================================================================
// SUB-COMPONENT: Live Preview
// ============================================================================

const QuestionPreview = ({ questionNumber, questionType, content, answer, config }) => {
    const renderContent = () => {
        const text = content.text || 'Chưa có nội dung';
        const displayText = text
            .replace(/____/g, '<span class="qem-preview-blank">________</span>')
            .replace(/<b>(\d+)<\/b>/g, '<strong class="qem-preview-number">$1</strong>');
        
        return (
            <div 
                className="qem-preview-text"
                dangerouslySetInnerHTML={{ __html: displayText }}
            />
        );
    };
    
    const renderOptions = () => {
        if (!config) return null;
        
        if (config.category === 'boolean') {
            return (
                <div className="qem-preview-options">
                    {config.options.map((opt) => (
                        <label key={opt} className="qem-preview-option">
                            <span className={`qem-preview-radio ${answer[0] === opt ? 'qem-preview-radio--selected' : ''}`}>
                                {answer[0] === opt ? '●' : '○'}
                            </span>
                            <span>{opt}</span>
                        </label>
                    ))}
                </div>
            );
        }
        
        if (config.category === 'choice' && content.options) {
            const parsedOptions = parseOptionsFromStrings(content.options);
            return (
                <div className="qem-preview-options">
                    {parsedOptions.map((opt) => (
                        <label key={opt.letter} className="qem-preview-option">
                            <span className={`qem-preview-radio ${answer.includes(opt.letter) ? 'qem-preview-radio--selected' : ''}`}>
                                {answer.includes(opt.letter) ? '●' : '○'}
                            </span>
                            <span><strong>{opt.letter}</strong> {opt.text}</span>
                        </label>
                    ))}
                </div>
            );
        }
        
        if (config.category === 'matching') {
            return (
                <div className="qem-preview-matching">
                    <span className="qem-preview-answer-box">
                        {answer[0] || '?'}
                    </span>
                </div>
            );
        }
        
        if (config.category === 'text') {
            return (
                <div className="qem-preview-text-answer">
                    Đáp án: <strong>{answer.join(' / ') || '____'}</strong>
                </div>
            );
        }
        
        return null;
    };
    
    return (
        <div className="qem-preview">
            <div className="qem-preview-header">
                <FiEye />
                <span>Xem trước</span>
            </div>
            
            <div className="qem-preview-content">
                <div className="qem-preview-question">
                    <span className="qem-preview-qnum">{questionNumber}.</span>
                    {content.section_title && (
                        <div className="qem-preview-section-title">
                            {content.section_title}
                        </div>
                    )}
                    {renderContent()}
                </div>
                
                {renderOptions()}
            </div>
            
            <div className="qem-preview-type-badge">
                {config?.label || questionType}
            </div>
        </div>
    );
};

// ============================================================================
// MAIN COMPONENT: QuestionEditModal
// ============================================================================

export default function QuestionEditModal({
    isOpen,
    onClose,
    onSave,
    onDelete,
    question,
    questionTypes = [],
    matchingOptions = [] // Options from section layout for matching questions
}) {
    // Form state
    const [questionType, setQuestionType] = useState('');
    const [content, setContent] = useState({ text: '' });
    const [answer, setAnswer] = useState([]);
    const [explanation, setExplanation] = useState('');
    const [wordLimit, setWordLimit] = useState('');
    const [imageUrl, setImageUrl] = useState('');
    
    // UI state
    const [errors, setErrors] = useState({});
    const [showPreview, setShowPreview] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    
    // Get type configuration
    const config = QUESTION_TYPE_CONFIG[questionType] || null;
    
    // Build question type options
    const questionTypeOptions = useMemo(() => {
        if (questionTypes.length > 0) return questionTypes;
        // Fallback to current type if no types provided
        return Object.entries(QUESTION_TYPE_CONFIG).map(([value, cfg]) => ({
            value,
            label: cfg.label
        }));
    }, [questionTypes]);
    
    // Initialize form when modal opens or question changes
    useEffect(() => {
        if (!isOpen || !question) return;
        
        setQuestionType(question.questionType || '');
        setContent(parseQuestionContent(question.questionContent));
        setAnswer(parseCorrectAnswer(question.correctAnswer));
        setExplanation(question.explanation || '');
        setWordLimit(question.wordLimit || '');
        setImageUrl(question.imageUrl || '');
        setErrors({});
        setIsSaving(false);
    }, [isOpen, question]);
    
    // Handle question type change
    const handleTypeChange = useCallback((newType) => {
        setQuestionType(newType);
        // Reset answer when type changes (different answer format)
        setAnswer([]);
        setErrors({});
    }, []);
    
    // Validate form
    const validate = useCallback(() => {
        const newErrors = {};
        
        if (!questionType) {
            newErrors.questionType = 'Vui lòng chọn loại câu hỏi';
        }
        
        if (!content.text || !content.text.trim()) {
            newErrors.content = 'Nội dung câu hỏi không được để trống';
        }
        
        if (!answer || answer.length === 0 || !answer[0]) {
            newErrors.answer = 'Vui lòng nhập đáp án';
        }
        
        // Type-specific validations
        if (config) {
            if (config.category === 'text' && config.placeholder) {
                if (content.text && !content.text.includes('____')) {
                    newErrors.content = 'Nội dung phải chứa chỗ trống (____) để học sinh điền';
                }
            }
            
            if (config.category === 'choice') {
                const options = parseOptionsFromStrings(content.options || []);
                if (options.length < 2) {
                    newErrors.options = 'Cần ít nhất 2 lựa chọn';
                }
                if (answer[0] && !options.find(o => o.letter === answer[0])) {
                    newErrors.answer = 'Đáp án phải là một trong các lựa chọn';
                }
            }
            
            if (config.category === 'boolean') {
                if (answer[0] && !config.options.includes(answer[0])) {
                    newErrors.answer = `Đáp án phải là: ${config.options.join(', ')}`;
                }
            }
        }
        
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    }, [questionType, content, answer, config]);
    
    // Handle save
    const handleSave = useCallback(async () => {
        if (!validate()) return;
        
        setIsSaving(true);
        
        try {
            // Build the data object
            const data = {
                questionType,
                questionContent: content,
                correctAnswer: answer,
                explanation: explanation.trim() || null,
                wordLimit: wordLimit.trim() || null,
                imageUrl: imageUrl.trim() || null,
            };
            
            await onSave(question.id, data);
        } catch (error) {
            console.error('Save error:', error);
            setErrors({ save: 'Lỗi khi lưu câu hỏi. Vui lòng thử lại.' });
        } finally {
            setIsSaving(false);
        }
    }, [questionType, content, answer, explanation, wordLimit, imageUrl, question, onSave, validate]);
    
    // Render content editor based on question type
    const renderContentEditor = () => {
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
                        onChange={setContent}
                        config={config}
                        questionNumber={question?.questionNumber}
                    />
                );
            case 'boolean':
                return (
                    <BooleanContentEditor
                        content={content}
                        onChange={setContent}
                        config={config}
                        questionNumber={question?.questionNumber}
                    />
                );
            case 'choice':
                return (
                    <ChoiceContentEditor
                        content={content}
                        onChange={setContent}
                        config={config}
                        questionNumber={question?.questionNumber}
                    />
                );
            case 'matching':
                return (
                    <MatchingContentEditor
                        content={content}
                        onChange={setContent}
                        config={config}
                        questionNumber={question?.questionNumber}
                    />
                );
            default:
                return (
                    <TextContentEditor
                        content={content}
                        onChange={setContent}
                        config={config}
                        questionNumber={question?.questionNumber}
                    />
                );
        }
    };
    
    // Render answer editor based on question type
    const renderAnswerEditor = () => {
        if (!config) return null;
        
        switch (config.answerType) {
            case 'text':
                return (
                    <TextAnswerEditor
                        answer={answer}
                        onChange={setAnswer}
                        questionNumber={question?.questionNumber}
                    />
                );
            case 'tfng':
            case 'ynng':
                return (
                    <BooleanAnswerEditor
                        answer={answer}
                        onChange={setAnswer}
                        options={config.options}
                    />
                );
            case 'single-select':
                return (
                    <SingleSelectAnswerEditor
                        answer={answer}
                        onChange={setAnswer}
                        options={content.options || []}
                    />
                );
            case 'multi-select':
                return (
                    <MultiSelectAnswerEditor
                        answer={answer}
                        onChange={setAnswer}
                        options={content.options || []}
                    />
                );
            case 'letter':
                return (
                    <LetterAnswerEditor
                        answer={answer}
                        onChange={setAnswer}
                        matchingOptions={matchingOptions}
                    />
                );
            default:
                return (
                    <TextAnswerEditor
                        answer={answer}
                        onChange={setAnswer}
                        questionNumber={question?.questionNumber}
                    />
                );
        }
    };
    
    if (!isOpen || !question) return null;
    
    return (
        <div className="modal-overlay">
            <div className="modal-content modal-xl qem-modal">
                {/* Header */}
                <div className="modal-header">
                    <h3 className="modal-title">
                        Chỉnh sửa câu hỏi #{question.questionNumber}
                        {question.questionUid && (
                            <span className="qem-uid">({question.questionUid})</span>
                        )}
                    </h3>
                    <div className="qem-header-actions">
                        <button 
                            type="button"
                            className={`qem-preview-toggle ${showPreview ? 'active' : ''}`}
                            onClick={() => setShowPreview(!showPreview)}
                            title="Bật/tắt xem trước"
                        >
                            <FiEye size={16} />
                        </button>
                        <button className="modal-close" onClick={onClose}>
                            <FiX size={18} />
                        </button>
                    </div>
                </div>
                
                {/* Body */}
                <div className="modal-body qem-body">
                    <div className={`qem-layout ${showPreview ? 'qem-layout--with-preview' : ''}`}>
                        {/* Left: Form */}
                        <div className="qem-form">
                            {/* Question Type Selector */}
                            <div className="form-group">
                                <label>Loại câu hỏi</label>
                                <select
                                    className="form-select"
                                    value={questionType}
                                    onChange={(e) => handleTypeChange(e.target.value)}
                                >
                                    <option value="">-- Chọn loại câu hỏi --</option>
                                    {questionTypeOptions.map((type) => (
                                        <option key={type.value} value={type.value}>
                                            {type.label}
                                        </option>
                                    ))}
                                </select>
                                {errors.questionType && (
                                    <span className="error-text">
                                        <FiAlertTriangle size={14} /> {errors.questionType}
                                    </span>
                                )}
                            </div>
                            
                            {/* Content Editor */}
                            {renderContentEditor()}
                            {errors.content && (
                                <span className="error-text">
                                    <FiAlertTriangle size={14} /> {errors.content}
                                </span>
                            )}
                            {errors.options && (
                                <span className="error-text">
                                    <FiAlertTriangle size={14} /> {errors.options}
                                </span>
                            )}
                            
                            {/* Answer Editor */}
                            {renderAnswerEditor()}
                            {errors.answer && (
                                <span className="error-text">
                                    <FiAlertTriangle size={14} /> {errors.answer}
                                </span>
                            )}
                            
                            {/* Explanation Editor (3 fields: giải thích, trích dẫn, chiến lược) */}
                            <ExplanationEditor
                                explanation={explanation}
                                onChange={setExplanation}
                            />
                            
                            {/* Optional Fields */}
                            <div className="qem-optional-fields">
                                <div className="form-group qem-half">
                                    <label>Giới hạn từ (Word Limit)</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        value={wordLimit}
                                        onChange={(e) => setWordLimit(e.target.value)}
                                        placeholder="ONE WORD ONLY"
                                    />
                                </div>
                                
                                <div className="form-group qem-half">
                                    <label>URL Hình ảnh (tùy chọn)</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        value={imageUrl}
                                        onChange={(e) => setImageUrl(e.target.value)}
                                        placeholder="https://..."
                                    />
                                </div>
                            </div>
                        </div>
                        
                        {/* Right: Preview */}
                        {showPreview && (
                            <div className="qem-preview-panel">
                                <QuestionPreview
                                    questionNumber={question.questionNumber}
                                    questionType={questionType}
                                    content={content}
                                    answer={answer}
                                    config={config}
                                />
                            </div>
                        )}
                    </div>
                    
                    {/* Global Error */}
                    {errors.save && (
                        <div className="qem-global-error">
                            <FiAlertTriangle size={16} />
                            {errors.save}
                        </div>
                    )}
                </div>
                
                {/* Footer */}
                <div className="modal-footer">
                    <button 
                        type="button"
                        className="admin-btn admin-btn--secondary" 
                        onClick={onClose}
                        disabled={isSaving}
                    >
                        Hủy
                    </button>
                    
                    {onDelete && (
                        <button
                            type="button"
                            className="admin-btn admin-btn--danger"
                            onClick={() => onDelete(question.id)}
                            disabled={isSaving}
                        >
                            <FiTrash2 size={16} />
                            Xóa câu hỏi
                        </button>
                    )}
                    
                    <button 
                        type="button"
                        className="admin-btn admin-btn--primary" 
                        onClick={handleSave}
                        disabled={isSaving}
                    >
                        {isSaving ? (
                            <span className="modal-spinner" />
                        ) : (
                            <FiCheck size={16} />
                        )}
                        {isSaving ? 'Đang lưu...' : 'Lưu câu hỏi'}
                    </button>
                </div>
            </div>
        </div>
    );
}
