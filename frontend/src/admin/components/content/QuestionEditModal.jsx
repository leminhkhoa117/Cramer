import React, { useEffect, useMemo, useState } from 'react';
import { FiAlertTriangle, FiCheck, FiTrash2, FiX } from 'react-icons/fi';
import '../common/AdminModal.css';

const toJsonText = (value) => {
    if (value === null || value === undefined) return '';
    if (typeof value === 'string') {
        const trimmed = value.trim();
        if (!trimmed) return '';
        try {
            return JSON.stringify(JSON.parse(trimmed), null, 2);
        } catch (error) {
            return trimmed;
        }
    }
    try {
        return JSON.stringify(value, null, 2);
    } catch (error) {
        return String(value);
    }
};

const parseJsonField = (value, label, errors) => {
    const trimmed = value.trim();
    if (!trimmed) {
        errors[label] = 'Không được để trống';
        return null;
    }
    try {
        return JSON.parse(trimmed);
    } catch (error) {
        errors[label] = 'JSON không hợp lệ';
        return null;
    }
};

export default function QuestionEditModal({
    isOpen,
    onClose,
    onSave,
    onDelete,
    question,
    questionTypes = []
}) {
    const [form, setForm] = useState({
        questionType: '',
        questionContent: '',
        correctAnswer: '',
        explanation: '',
        wordLimit: '',
        imageUrl: ''
    });
    const [errors, setErrors] = useState({});

    useEffect(() => {
        if (!isOpen || !question) return;
        setForm({
            questionType: question.questionType || '',
            questionContent: toJsonText(question.questionContent),
            correctAnswer: toJsonText(question.correctAnswer),
            explanation: question.explanation || '',
            wordLimit: question.wordLimit || '',
            imageUrl: question.imageUrl || ''
        });
        setErrors({});
    }, [isOpen, question]);

    const questionTypeOptions = useMemo(() => {
        if (questionTypes.length > 0) return questionTypes;
        return [{ value: question?.questionType || 'UNKNOWN', label: question?.questionType || 'Unknown' }];
    }, [questionTypes, question]);

    if (!isOpen || !question) return null;

    const handleChange = (field, value) => {
        setForm(prev => ({ ...prev, [field]: value }));
        if (errors[field]) {
            setErrors(prev => ({ ...prev, [field]: null }));
        }
    };

    const handleSave = () => {
        const nextErrors = {};
        if (!form.questionType) {
            nextErrors.questionType = 'Vui lòng chọn loại câu hỏi';
        }

        const parsedContent = parseJsonField(form.questionContent, 'questionContent', nextErrors);
        const parsedAnswer = parseJsonField(form.correctAnswer, 'correctAnswer', nextErrors);

        if (Object.keys(nextErrors).length > 0) {
            setErrors(nextErrors);
            return;
        }

        onSave(question.id, {
            questionType: form.questionType,
            questionContent: parsedContent,
            correctAnswer: parsedAnswer,
            explanation: form.explanation.trim() || null,
            wordLimit: form.wordLimit.trim() || null,
            imageUrl: form.imageUrl.trim() || null
        });
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content modal-lg">
                <div className="modal-header">
                    <h3 className="modal-title">Chỉnh sửa câu hỏi {question.questionNumber}</h3>
                    <button className="modal-close" onClick={onClose}>
                        <FiX size={18} />
                    </button>
                </div>

                <div className="modal-body">
                    <div className="form-group">
                        <label>Question UID</label>
                        <input className="form-input" value={question.questionUid || ''} readOnly />
                    </div>

                    <div className="form-group">
                        <label>Question Type</label>
                        <select
                            className="form-select"
                            value={form.questionType}
                            onChange={(e) => handleChange('questionType', e.target.value)}
                        >
                            <option value="">-- Chọn loại câu hỏi --</option>
                            {questionTypeOptions.map(type => (
                                <option key={type.value} value={type.value}>
                                    {type.label}
                                </option>
                            ))}
                        </select>
                        {errors.questionType && <span className="error-text">{errors.questionType}</span>}
                    </div>

                    <div className="form-group">
                        <label>Question Content (JSON)</label>
                        <textarea
                            className="form-textarea"
                            rows={8}
                            value={form.questionContent}
                            onChange={(e) => handleChange('questionContent', e.target.value)}
                        />
                        {errors.questionContent && (
                            <span className="error-text">
                                <FiAlertTriangle size={14} /> {errors.questionContent}
                            </span>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Correct Answer (JSON)</label>
                        <textarea
                            className="form-textarea"
                            rows={4}
                            value={form.correctAnswer}
                            onChange={(e) => handleChange('correctAnswer', e.target.value)}
                        />
                        {errors.correctAnswer && (
                            <span className="error-text">
                                <FiAlertTriangle size={14} /> {errors.correctAnswer}
                            </span>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Explanation</label>
                        <textarea
                            className="form-textarea"
                            rows={4}
                            value={form.explanation}
                            onChange={(e) => handleChange('explanation', e.target.value)}
                        />
                    </div>

                    <div className="form-group">
                        <label>Word Limit</label>
                        <input
                            className="form-input"
                            value={form.wordLimit}
                            onChange={(e) => handleChange('wordLimit', e.target.value)}
                            placeholder="ONE WORD ONLY"
                        />
                    </div>

                    <div className="form-group">
                        <label>Image URL (Optional)</label>
                        <input
                            className="form-input"
                            value={form.imageUrl}
                            onChange={(e) => handleChange('imageUrl', e.target.value)}
                            placeholder="https://..."
                        />
                    </div>
                </div>

                <div className="modal-footer">
                    <button className="admin-btn admin-btn--secondary" onClick={onClose}>
                        Hủy
                    </button>
                    {onDelete && (
                        <button
                            className="admin-btn admin-btn--danger"
                            onClick={() => onDelete(question.id)}
                        >
                            <FiTrash2 size={16} />
                            Xóa
                        </button>
                    )}
                    <button className="admin-btn admin-btn--primary" onClick={handleSave}>
                        <FiCheck size={16} />
                        Lưu câu hỏi
                    </button>
                </div>
            </div>
        </div>
    );
}
