import React, { useState } from 'react';
import { FiX, FiPlus, FiFolder, FiFileText, FiTag } from 'react-icons/fi';
import TagInput from '../abts/TagInput';
import '../common/AdminModal.css';

/**
 * CreateTestModal - Modal tạo đề thi mới
 * 
 * @param {boolean} isOpen - Trạng thái mở modal
 * @param {function} onClose - Callback đóng modal
 * @param {function} onSubmit - Callback khi submit form
 * @param {Array} topics - Danh sách topics
 */
export default function CreateTestModal({ isOpen, onClose, onSubmit, topics = [], testToEdit = null }) {
    const [step, setStep] = useState(testToEdit ? 2 : 1); // If editing, start at step 2
    const [mode, setMode] = useState('existing'); // 'existing' | 'new'

    // Initialize form data
    const [formData, setFormData] = useState({
        topicId: '',
        newTopicName: '',
        newTopicSource: '',
        testName: '',
        testNumber: '',
        hashtagIds: []
    });

    const [errors, setErrors] = useState({});

    // Effect to populate data when opening for edit or new
    React.useEffect(() => {
        if (isOpen) {
            if (testToEdit) {
                // Edit Mode
                setStep(2);
                setMode('existing');

                // Find topic based on test's topic ID or similar linkage
                // Note: testToEdit might need to contain topicId or we search for it
                const parentTopic = topics.find(t => t.tests.some(test => test.id === testToEdit.id));
                const topicId = parentTopic ? parentTopic.id : '';

                setFormData({
                    topicId: topicId,
                    newTopicName: '',
                    newTopicSource: '',
                    testName: testToEdit.nameEn || testToEdit.nameVi || '',
                    testNumber: testToEdit.testNumber || '',
                    hashtagIds: testToEdit.hashtags ? testToEdit.hashtags.map(h => h.id) : []
                });
            } else {
                // Create Mode
                setStep(1);
                setMode('existing');
                setFormData({
                    topicId: '',
                    newTopicName: '',
                    newTopicSource: '',
                    testName: '',
                    testNumber: '',
                    hashtagIds: []
                });
            }
            setErrors({});
        }
    }, [isOpen, testToEdit, topics]);

    if (!isOpen) return null;

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
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
            // If editing, we just submit the updated data
            if (testToEdit) {
                onSubmit({
                    id: testToEdit.id,
                    testName: formData.testName,
                    testNumber: formData.testNumber,
                    hashtagIds: formData.hashtagIds,
                    // We might allow moving topics later, but for now focus on metadata
                    // topicId: formData.topicId 
                });
                return;
            }

            // ... (Creation logic remains the same)
            let examSource = '';

            if (mode === 'existing') {
                const selectedTopic = topics.find(t => t.id === parseInt(formData.topicId));
                examSource = selectedTopic?.source || '';
            } else {
                examSource = formData.newTopicSource
                    .trim()
                    .toLowerCase()
                    .replace(/[^a-z0-9]+/g, '-')
                    .replace(/^-+|-+$/g, '');
            }

            onSubmit({
                examSource: examSource,
                testNumber: formData.testNumber || (Math.floor(Math.random() * 1000) + 1),
                testName: formData.testName,
                hashtagIds: formData.hashtagIds
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
            hashtagIds: []
        });
        setErrors({});
        onClose();
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content create-test-modal">
                <div className="modal-header">
                    <h3 className="modal-title">
                        <FiFolder className="title-icon" />
                        Tạo đề thi mới
                    </h3>
                    <button className="modal-close" onClick={handleClose}>
                        <FiX size={20} />
                    </button>
                </div>

                {/* Progress Steps */}
                <div className="modal-steps">
                    <div className={`modal-step ${step >= 1 ? 'active' : ''}`}>
                        <div className="modal-step-number">1</div>
                        <div className="modal-step-label">Chọn Topic</div>
                    </div>
                    <div className="modal-step-connector" />
                    <div className={`modal-step ${step >= 2 ? 'active' : ''}`}>
                        <div className="modal-step-number">2</div>
                        <div className="modal-step-label">Thông tin Test</div>
                    </div>
                </div>

                <div className="modal-body">
                    {step === 1 && (
                        <div className="step-content">
                            <p className="step-description" style={{ marginBottom: '20px', color: 'var(--admin-text-secondary)' }}>
                                Chọn Topic có sẵn hoặc tạo Topic mới để thêm đề thi
                            </p>

                            {/* Mode Toggle */}
                            <div className="modal-toggle-group">
                                <button
                                    className={`modal-toggle-btn ${mode === 'existing' ? 'active' : ''}`}
                                    onClick={() => setMode('existing')}
                                >
                                    <FiFolder size={18} />
                                    Topic có sẵn
                                </button>
                                <button
                                    className={`modal-toggle-btn ${mode === 'new' ? 'active' : ''}`}
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
                                        className={`form-select ${errors.topicId ? 'error' : ''}`}
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
                                            className={`form-input ${errors.newTopicSource ? 'error' : ''}`}
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
                                            className={`form-input ${errors.newTopicName ? 'error' : ''}`}
                                        />
                                        {errors.newTopicName && <span className="error-text">{errors.newTopicName}</span>}
                                    </div>
                                </>
                            )}
                        </div>
                    )}

                    {step === 2 && (
                        <div className="step-content">
                            <p className="step-description" style={{ marginBottom: '20px', color: 'var(--admin-text-secondary)' }}>
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
                                    className={`form-input ${errors.testName ? 'error' : ''}`}
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
                                    className="form-input"
                                />
                                <span className="help-text">Dùng để sắp xếp thứ tự hiển thị</span>
                            </div>

                            <div className="form-group">
                                <label>Hashtags</label>
                                <TagInput
                                    value={formData.hashtagIds}
                                    onChange={(ids) => setFormData(prev => ({ ...prev, hashtagIds: ids }))}
                                    mode="select"
                                />
                            </div>

                            <div className="modal-info-box">
                                <FiFileText size={16} />
                                <p>
                                    Đề thi mới sẽ được tạo với trạng thái <strong>DRAFT</strong>.
                                    Bạn có thể thêm câu hỏi và xuất bản sau.
                                </p>
                            </div>
                        </div>
                    )}
                </div>

                <div className="modal-footer">
                    {step === 1 ? (
                        <>
                            <button className="modal-btn modal-btn-secondary" onClick={handleClose}>
                                Hủy
                            </button>
                            <button className="modal-btn modal-btn-primary" onClick={handleNext}>
                                Tiếp theo
                            </button>
                        </>
                    ) : (
                        <>
                            <button className="modal-btn modal-btn-secondary" onClick={handleBack}>
                                Quay lại
                            </button>
                            <button className="modal-btn modal-btn-primary" onClick={handleSubmit}>
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
