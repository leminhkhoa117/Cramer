import React, { useState } from 'react';
import { FiX, FiPlus, FiFolder, FiFileText } from 'react-icons/fi';
import './CreateTestModal.css';

/**
 * CreateTestModal - Modal tạo đề thi mới
 * 
 * @param {boolean} isOpen - Trạng thái mở modal
 * @param {function} onClose - Callback đóng modal
 * @param {function} onSubmit - Callback khi submit form
 * @param {Array} topics - Danh sách topics
 */
export default function CreateTestModal({ isOpen, onClose, onSubmit, topics = [] }) {
    const [step, setStep] = useState(1); // 1 = Chọn/Tạo topic, 2 = Thông tin test
    const [mode, setMode] = useState('existing'); // 'existing' | 'new'
    const [formData, setFormData] = useState({
        topicId: '',
        newTopicName: '',
        newTopicSource: '',
        testName: '',
        testNumber: '',
    });
    const [errors, setErrors] = useState({});

    if (!isOpen) return null;

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        // Clear error when user types
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: null }));
        }
    };

    const validateStep1 = () => {
        const newErrors = {};
        if (mode === 'existing' && !formData.topicId) {
            newErrors.topicId = 'Vui lòng chọn Topic';
        }
        if (mode === 'new') {
            if (!formData.newTopicName.trim()) {
                newErrors.newTopicName = 'Vui lòng nhập tên Topic';
            }
            if (!formData.newTopicSource.trim()) {
                newErrors.newTopicSource = 'Vui lòng nhập nguồn (vd: Cambridge 19)';
            }
        }
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const validateStep2 = () => {
        const newErrors = {};
        if (!formData.testName.trim()) {
            newErrors.testName = 'Vui lòng nhập tên Test';
        }
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleNext = () => {
        if (step === 1 && validateStep1()) {
            setStep(2);
        }
    };

    const handleBack = () => {
        setStep(1);
        setErrors({});
    };

    const handleSubmit = () => {
        if (validateStep2()) {
            onSubmit({
                mode,
                topicId: mode === 'existing' ? formData.topicId : null,
                newTopic: mode === 'new' ? {
                    name: formData.newTopicName,
                    source: formData.newTopicSource,
                } : null,
                test: {
                    name: formData.testName,
                    number: formData.testNumber || null,
                },
            });
            // Reset form
            setStep(1);
            setMode('existing');
            setFormData({
                topicId: '',
                newTopicName: '',
                newTopicSource: '',
                testName: '',
                testNumber: '',
            });
        }
    };

    const handleClose = () => {
        setStep(1);
        setFormData({
            topicId: '',
            newTopicName: '',
            newTopicSource: '',
            testName: '',
            testNumber: '',
        });
        setErrors({});
        onClose();
    };

    return (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && handleClose()}>
            <div className="create-test-modal">
                <div className="create-test-modal__header">
                    <h2>Tạo đề thi mới</h2>
                    <button className="close-btn" onClick={handleClose}>
                        <FiX size={20} />
                    </button>
                </div>

                {/* Progress Steps */}
                <div className="create-test-modal__steps">
                    <div className={`step ${step >= 1 ? 'step--active' : ''}`}>
                        <span className="step__number">1</span>
                        <span className="step__label">Chọn Topic</span>
                    </div>
                    <div className="step__connector" />
                    <div className={`step ${step >= 2 ? 'step--active' : ''}`}>
                        <span className="step__number">2</span>
                        <span className="step__label">Thông tin Test</span>
                    </div>
                </div>

                <div className="create-test-modal__content">
                    {step === 1 && (
                        <div className="step-content">
                            <p className="step-description">
                                Chọn Topic có sẵn hoặc tạo Topic mới để thêm đề thi
                            </p>

                            {/* Mode Toggle */}
                            <div className="mode-toggle">
                                <button
                                    className={`mode-toggle__btn ${mode === 'existing' ? 'mode-toggle__btn--active' : ''}`}
                                    onClick={() => setMode('existing')}
                                >
                                    <FiFolder size={18} />
                                    Topic có sẵn
                                </button>
                                <button
                                    className={`mode-toggle__btn ${mode === 'new' ? 'mode-toggle__btn--active' : ''}`}
                                    onClick={() => setMode('new')}
                                >
                                    <FiPlus size={18} />
                                    Tạo Topic mới
                                </button>
                            </div>

                            {mode === 'existing' && (
                                <div className="form-group">
                                    <label>Chọn Topic</label>
                                    <select
                                        name="topicId"
                                        value={formData.topicId}
                                        onChange={handleInputChange}
                                        className={errors.topicId ? 'error' : ''}
                                    >
                                        <option value="">-- Chọn Topic --</option>
                                        {topics.map(topic => (
                                            <option key={topic.id} value={topic.id}>
                                                {topic.displayName} ({topic.testsCount} tests)
                                            </option>
                                        ))}
                                    </select>
                                    {errors.topicId && <span className="error-text">{errors.topicId}</span>}
                                </div>
                            )}

                            {mode === 'new' && (
                                <>
                                    <div className="form-group">
                                        <label>Nguồn đề thi (Source)</label>
                                        <input
                                            type="text"
                                            name="newTopicSource"
                                            value={formData.newTopicSource}
                                            onChange={handleInputChange}
                                            placeholder="vd: Cambridge 19, Barron's, Real Test"
                                            className={errors.newTopicSource ? 'error' : ''}
                                        />
                                        {errors.newTopicSource && <span className="error-text">{errors.newTopicSource}</span>}
                                    </div>
                                    <div className="form-group">
                                        <label>Tên hiển thị</label>
                                        <input
                                            type="text"
                                            name="newTopicName"
                                            value={formData.newTopicName}
                                            onChange={handleInputChange}
                                            placeholder="vd: Cambridge IELTS 19"
                                            className={errors.newTopicName ? 'error' : ''}
                                        />
                                        {errors.newTopicName && <span className="error-text">{errors.newTopicName}</span>}
                                    </div>
                                </>
                            )}
                        </div>
                    )}

                    {step === 2 && (
                        <div className="step-content">
                            <p className="step-description">
                                Nhập thông tin cho đề thi mới
                            </p>

                            <div className="selected-topic">
                                <FiFolder size={16} />
                                <span>
                                    {mode === 'existing'
                                        ? topics.find(t => t.id === parseInt(formData.topicId))?.displayName
                                        : formData.newTopicName
                                    }
                                </span>
                            </div>

                            <div className="form-group">
                                <label>Tên Test</label>
                                <input
                                    type="text"
                                    name="testName"
                                    value={formData.testName}
                                    onChange={handleInputChange}
                                    placeholder="vd: Test 1, Academic Test 1"
                                    className={errors.testName ? 'error' : ''}
                                />
                                {errors.testName && <span className="error-text">{errors.testName}</span>}
                            </div>

                            <div className="form-group">
                                <label>Số thứ tự (tùy chọn)</label>
                                <input
                                    type="number"
                                    name="testNumber"
                                    value={formData.testNumber}
                                    onChange={handleInputChange}
                                    placeholder="vd: 1, 2, 3..."
                                    min="1"
                                />
                                <span className="help-text">Dùng để sắp xếp thứ tự hiển thị</span>
                            </div>

                            <div className="info-box">
                                <FiFileText size={16} />
                                <p>
                                    Đề thi mới sẽ được tạo với trạng thái <strong>DRAFT</strong>.
                                    Bạn có thể thêm câu hỏi và xuất bản sau.
                                </p>
                            </div>
                        </div>
                    )}
                </div>

                <div className="create-test-modal__footer">
                    {step === 1 ? (
                        <>
                            <button className="modal-btn modal-btn--secondary" onClick={handleClose}>
                                Hủy
                            </button>
                            <button className="modal-btn modal-btn--primary" onClick={handleNext}>
                                Tiếp theo
                            </button>
                        </>
                    ) : (
                        <>
                            <button className="modal-btn modal-btn--secondary" onClick={handleBack}>
                                Quay lại
                            </button>
                            <button className="modal-btn modal-btn--primary" onClick={handleSubmit}>
                                <FiPlus size={16} />
                                Tạo đề thi
                            </button>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}
