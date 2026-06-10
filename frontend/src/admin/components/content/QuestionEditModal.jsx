import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { FiAlertTriangle, FiCheck, FiEye, FiTrash2, FiX } from 'react-icons/fi';
import '../common/AdminModal.css';
import './QuestionEditModal.css';
import AnswerEditors from './question-edit/AnswerEditors';
import ContentEditors from './question-edit/ContentEditors';
import ExplanationEditor from './question-edit/ExplanationEditor';
import QuestionPreview from './question-edit/QuestionPreview';
import {
    QUESTION_TYPE_CONFIG,
    TYPES_ALLOWING_EMPTY_CONTENT,
    getQuestionTypeOptions,
} from './question-edit/questionTypeConfig';
import {
    parseCorrectAnswer,
    parseOptionsFromStrings,
    parseQuestionContent,
} from './question-edit/questionParsers';

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
        return getQuestionTypeOptions(questionTypes);
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

        // Allow empty content for matching/table types (content comes from shared section layout)
        const allowEmptyContent = TYPES_ALLOWING_EMPTY_CONTENT.includes(questionType);

        if (!allowEmptyContent && (!content.text || !content.text.trim())) {
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
            // Handle explanation (can be string or object)
            let finalExplanation = explanation;
            if (typeof explanation === 'string') {
                finalExplanation = explanation.trim() || null;
            } else if (typeof explanation === 'object' && explanation !== null) {
                // Check if object is effectively empty
                const hasContent = (explanation.detail && explanation.detail.trim()) ||
                    (explanation.quote && explanation.quote.trim()) ||
                    (explanation.strategy && explanation.strategy.trim());
                if (!hasContent) {
                    finalExplanation = null;
                }
            }

            // Build the data object
            const data = {
                questionType,
                questionContent: content,
                correctAnswer: answer,
                explanation: finalExplanation,
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
                            <ContentEditors
                                config={config}
                                content={content}
                                onChange={setContent}
                                questionNumber={question?.questionNumber}
                            />
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
                            <AnswerEditors
                                config={config}
                                answer={answer}
                                onChange={setAnswer}
                                contentOptions={content.options || []}
                                matchingOptions={matchingOptions}
                            />
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
                </div>

                {/* Global Error - Outside modal-body for visibility */}
                {errors.save && (
                    <div className="qem-global-error" style={{ margin: '0 24px 16px' }}>
                        <FiAlertTriangle size={16} />
                        {errors.save}
                    </div>
                )}

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
