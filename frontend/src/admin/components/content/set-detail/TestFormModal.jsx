import { useState } from 'react';
import { FiCheck, FiPlus, FiX } from 'react-icons/fi';

export default function TestFormModal({ existingTestNumbers, hashtags, onClose, onSubmit, editingTest = null }) {
    const topicHashtags = hashtags.filter(h => h.category === 'topic');
    const isEditing = !!editingTest;

    const [formData, setFormData] = useState({
        testNumber: editingTest?.testNumber ?? Math.max(...existingTestNumbers, 0) + 1,
        name: editingTest?.name ?? '',
        difficulty: editingTest?.difficulty ?? 'INTERMEDIATE',
        // For editing: extract codes from existing hashtags; for creating: start empty
        hashtagCodes: editingTest?.hashtags?.map(h => h.code) ?? []
    });
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errors, setErrors] = useState({});

    // Handle input change
    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: name === 'testNumber' ? parseInt(value) || 0 : value
        }));
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: null }));
        }
    };

    // Handle hashtag toggle - now uses codes instead of IDs
    const handleHashtagToggle = (hashtagCode) => {
        setFormData(prev => ({
            ...prev,
            hashtagCodes: prev.hashtagCodes.includes(hashtagCode)
                ? prev.hashtagCodes.filter(code => code !== hashtagCode)
                : [...prev.hashtagCodes, hashtagCode]
        }));
    };

    // Validate form
    const validate = () => {
        const newErrors = {};
        if (!formData.testNumber || formData.testNumber < 1) {
            newErrors.testNumber = 'Số bài thi phải lớn hơn 0';
        } else if (existingTestNumbers.includes(formData.testNumber)) {
            newErrors.testNumber = 'Số bài thi đã tồn tại';
        }
        if (!formData.name || formData.name.trim() === '') {
            newErrors.name = 'Vui lòng nhập tên đề thi';
        }
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    // Handle submit
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!validate()) return;

        setIsSubmitting(true);
        try {
            await onSubmit(formData);
        } catch (err) {
            console.error('Error creating test:', err);
            setErrors({ submit: err.message || 'Lỗi khi tạo bài thi' });
        } finally {
            setIsSubmitting(false);
        }
    };

    // Handle overlay click
    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget && !isSubmitting) {
            onClose();
        }
    };

    return (
        <div className="admin-modal-overlay-custom" onClick={handleOverlayClick}>
            <div className="admin-edit-modal create-test-modal" onClick={e => e.stopPropagation()}>
                <div className="admin-edit-modal-header">
                    <h2>{isEditing ? 'Chỉnh sửa thông tin đề thi' : 'Thêm bài thi mới'}</h2>
                    <button
                        className="admin-edit-modal-close"
                        onClick={onClose}
                        disabled={isSubmitting}
                    >
                        <FiX size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    <div className="admin-edit-modal-body">
                        {/* Test Number */}
                        <div className="form-group">
                            <label htmlFor="testNumber">Số bài thi *</label>
                            <input
                                type="number"
                                id="testNumber"
                                name="testNumber"
                                className={`form-input ${errors.testNumber ? 'form-input--error' : ''}`}
                                value={formData.testNumber}
                                onChange={handleChange}
                                disabled={isSubmitting}
                                min="1"
                            />
                            {errors.testNumber && <span className="form-error">{errors.testNumber}</span>}
                        </div>

                        {/* Test Name */}
                        <div className="form-group">
                            <label htmlFor="name">Tên đề thi *</label>
                            <input
                                type="text"
                                id="name"
                                name="name"
                                className={`form-input ${errors.name ? 'form-input--error' : ''}`}
                                placeholder="vd: Cambridge 17 Test 1"
                                value={formData.name}
                                onChange={handleChange}
                                disabled={isSubmitting}
                                required
                            />
                            {errors.name && <span className="form-error">{errors.name}</span>}
                        </div>

                        {/* Difficulty */}
                        <div className="form-group">
                            <label htmlFor="difficulty">Độ khó</label>
                            <select
                                id="difficulty"
                                name="difficulty"
                                className="form-select"
                                value={formData.difficulty}
                                onChange={handleChange}
                                disabled={isSubmitting}
                            >
                                <option value="BEGINNER">Cơ bản (Beginner)</option>
                                <option value="LOWER_INTERMEDIATE">Sơ trung cấp (Lower Intermediate)</option>
                                <option value="INTERMEDIATE">Trung cấp (Intermediate)</option>
                                <option value="UPPER_INTERMEDIATE">Trung cao cấp (Upper Intermediate)</option>
                                <option value="ADVANCED">Nâng cao (Advanced)</option>
                            </select>
                        </div>

                        {/* Hashtags */}
                        {topicHashtags.length > 0 && (
                            <div className="form-group">
                                <label>Chủ đề (Hashtags)</label>
                                <div className="hashtag-selector">
                                    {topicHashtags.map(tag => (
                                        <button
                                            key={tag.code}
                                            type="button"
                                            className={`hashtag-option ${formData.hashtagCodes.includes(tag.code) ? 'hashtag-option--selected' : ''}`}
                                            onClick={() => handleHashtagToggle(tag.code)}
                                            style={{
                                                borderColor: tag.color || '#8B5CF6',
                                                backgroundColor: formData.hashtagCodes.includes(tag.code)
                                                    ? (tag.color || '#8B5CF6') + '20'
                                                    : 'transparent'
                                            }}
                                        >
                                            {tag.icon} {tag.name}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Submit Error */}
                        {errors.submit && (
                            <div className="form-error-box">{errors.submit}</div>
                        )}
                    </div>

                    <div className="admin-edit-modal-footer">
                        <button
                            type="button"
                            className="admin-btn admin-btn--secondary"
                            onClick={onClose}
                            disabled={isSubmitting}
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            className="admin-btn admin-btn--primary"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? (
                                <>
                                    <span className="spinner small"></span>
                                    {isEditing ? 'Đang lưu...' : 'Đang tạo...'}
                                </>
                            ) : (
                                <>
                                    {isEditing ? <FiCheck size={16} /> : <FiPlus size={16} />}
                                    {isEditing ? 'Lưu thay đổi' : 'Tạo bài thi'}
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export function CreateTestModal(props) {
    return <TestFormModal {...props} />;
}

export function EditTestModal(props) {
    return <TestFormModal {...props} />;
}