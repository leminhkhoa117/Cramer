import React, { useState, useEffect, useMemo } from 'react';
import { FiX, FiSave, FiFolder, FiType, FiBarChart2, FiHash, FiEye, FiPlus, FiAlertCircle } from 'react-icons/fi';
import useTestSetStore from '../../stores/useTestSetStore';
import { testsApi } from '../../api/adminApi';
import TagInput from './TagInput';
import '../../components/common/AdminModal.css';

/**
 * SaveAIContentModal - Simplified configuration modal for saving AI generated content.
 * 
 * Features:
 * - Preview section showing what will be saved
 * - Test set selection with inline creation
 * - Auto-incremented test number (editable)
 * - Required test name with validation
 * - 5-level difficulty selector
 * - Hashtags with max 20 tags
 * 
 * @since 2025-12-26
 * @updated 2026-01-03 - Simplified UI, added preview, fixed validation
 */
export default function SaveAIContentModal({
    isOpen,
    onClose,
    onSave,
    initialTopic = '',
    suggestedSkill = 'reading',
    partNumber = 1,
    selectedParts = [],
    questionCount = 0,
    passagePreview = '' // First 100 chars of passage/prompt
}) {
    const { testSets, fetchTestSets, isLoading } = useTestSetStore();

    const [formData, setFormData] = useState({
        setId: '',
        setCode: '',
        setName: '',
        testNumber: 1,
        testName: '',
        difficulty: 'INTERMEDIATE',
        hashtags: [],
        createNewSet: false
    });

    const [testsInSet, setTestsInSet] = useState([]);
    const [isLoadingTests, setIsLoadingTests] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errors, setErrors] = useState({});

    // Skill display name mapping
    const skillDisplayName = useMemo(() => {
        const names = { 
            reading: 'Reading', 
            listening: 'Listening', 
            writing: 'Writing', 
            speaking: 'Speaking' 
        };
        return names[suggestedSkill?.toLowerCase()] || suggestedSkill;
    }, [suggestedSkill]);

    // Parts display string
    const partsDisplay = useMemo(() => {
        if (selectedParts && selectedParts.length > 0) {
            return selectedParts.map(p => `Part ${p}`).join(', ');
        }
        return `Part ${partNumber}`;
    }, [selectedParts, partNumber]);

    // Difficulty options with Vietnamese labels
    const difficultyOptions = [
        { value: 'BEGINNER', label: 'Cơ bản', labelEn: 'Beginner' },
        { value: 'LOWER_INTERMEDIATE', label: 'Sơ trung cấp', labelEn: 'Lower Intermediate' },
        { value: 'INTERMEDIATE', label: 'Trung cấp', labelEn: 'Intermediate' },
        { value: 'UPPER_INTERMEDIATE', label: 'Trung cao cấp', labelEn: 'Upper Intermediate' },
        { value: 'ADVANCED', label: 'Nâng cao', labelEn: 'Advanced' }
    ];

    // Initialize form when modal opens
    useEffect(() => {
        if (isOpen) {
            fetchTestSets(true);
            setFormData({
                setId: '',
                setCode: '',
                setName: '',
                testNumber: 1,
                testName: '',
                difficulty: 'INTERMEDIATE',
                hashtags: [],
                createNewSet: false
            });
            setTestsInSet([]);
            setErrors({});
        }
    }, [isOpen, fetchTestSets]);

    // Fetch tests when a test set is selected & auto-increment test number
    useEffect(() => {
        const fetchTests = async () => {
            if (formData.setId && !formData.createNewSet) {
                setIsLoadingTests(true);
                try {
                    const tests = await testsApi.getBySetId(formData.setId);
                    setTestsInSet(tests || []);
                    // Auto-increment test number
                    const maxNumber = (tests || []).reduce((max, t) => 
                        Math.max(max, t.testNumber || 0), 0);
                    setFormData(prev => ({ ...prev, testNumber: maxNumber + 1 }));
                } catch (error) {
                    console.error('Error fetching tests:', error);
                    setTestsInSet([]);
                } finally {
                    setIsLoadingTests(false);
                }
            } else {
                setTestsInSet([]);
                setFormData(prev => ({ ...prev, testNumber: 1 }));
            }
        };
        fetchTests();
    }, [formData.setId, formData.createNewSet]);

    // Handle input changes
    const handleChange = (field, value) => {
        setFormData(prev => ({ ...prev, [field]: value }));
        // Clear error when user types
        if (errors[field]) {
            setErrors(prev => ({ ...prev, [field]: null }));
        }
    };

    // Toggle create new set mode
    const toggleCreateNewSet = (createNew) => {
        setFormData(prev => ({
            ...prev,
            createNewSet: createNew,
            setId: createNew ? '' : prev.setId,
            setCode: '',
            setName: ''
        }));
        setErrors(prev => ({ ...prev, setId: null, setCode: null, setName: null }));
    };

    // Validate form
    const validateForm = () => {
        const newErrors = {};

        // Validate test set
        if (formData.createNewSet) {
            if (!formData.setCode.trim()) {
                newErrors.setCode = 'Vui lòng nhập mã bộ đề';
            }
            if (!formData.setName.trim()) {
                newErrors.setName = 'Vui lòng nhập tên bộ đề';
            }
        } else {
            if (!formData.setId) {
                newErrors.setId = 'Vui lòng chọn bộ đề thi';
            }
        }

        // Validate test name - REQUIRED
        if (!formData.testName.trim()) {
            newErrors.testName = 'Vui lòng nhập tên đề thi';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    // Handle form submission
    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!validateForm()) {
            return;
        }

        setIsSubmitting(true);
        try {
            const saveConfig = {
                setId: formData.createNewSet ? null : (formData.setId || null),
                setCode: formData.createNewSet ? formData.setCode.trim() : null,
                setNameVi: formData.createNewSet ? formData.setName.trim() : null,
                testNumber: formData.testNumber,
                testName: formData.testName.trim(),
                difficulty: formData.difficulty,
                hashtagIds: formData.hashtags
            };
            await onSave(saveConfig);
        } finally {
            setIsSubmitting(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay">
            <div className="modal-content" style={{ maxWidth: '600px' }}>
                <div className="modal-header">
                    <h2 className="modal-title">
                        <FiSave className="title-icon" />
                        Lưu Đề Thi AI
                    </h2>
                    <button className="modal-close" onClick={onClose}>
                        <FiX />
                    </button>
                </div>

                <div className="modal-body">
                    {/* 1. PREVIEW SECTION */}
                    <div className="form-group">
                        <label className="form-label">
                            <FiEye className="form-icon" />
                            Xem trước nội dung
                        </label>
                        <div style={{
                            padding: '16px',
                            background: 'var(--surface-elevated, rgba(74, 144, 226, 0.08))',
                            borderRadius: 'var(--radius-md, 8px)',
                            border: '1px solid var(--border-color, rgba(255,255,255,0.1))'
                        }}>
                            <div style={{ 
                                display: 'grid', 
                                gridTemplateColumns: 'auto 1fr', 
                                gap: '8px 16px',
                                fontSize: '14px'
                            }}>
                                <span style={{ color: 'var(--text-secondary, rgba(255,255,255,0.6))' }}>Kỹ năng:</span>
                                <span style={{ fontWeight: 600, color: 'var(--primary, #4a90e2)' }}>{skillDisplayName}</span>
                                
                                <span style={{ color: 'var(--text-secondary, rgba(255,255,255,0.6))' }}>Phần:</span>
                                <span style={{ fontWeight: 600 }}>{partsDisplay}</span>
                                
                                <span style={{ color: 'var(--text-secondary, rgba(255,255,255,0.6))' }}>Số câu hỏi:</span>
                                <span style={{ fontWeight: 600 }}>{questionCount || 'N/A'}</span>
                                
                                {passagePreview && (
                                    <>
                                        <span style={{ color: 'var(--text-secondary, rgba(255,255,255,0.6))' }}>Tiêu đề:</span>
                                        <span style={{ 
                                            fontStyle: 'italic', 
                                            color: 'var(--text-primary, #fff)',
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            whiteSpace: 'nowrap'
                                        }}>
                                            {passagePreview.substring(0, 100)}{passagePreview.length > 100 ? '...' : ''}
                                        </span>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>

                    <form id="save-content-form" onSubmit={handleSubmit}>

                        {/* 2. TEST SET SELECTION */}
                        <div className="form-group">
                            <label className="form-label">
                                <FiFolder className="form-icon" />
                                Bộ đề thi <span className="required">*</span>
                            </label>

                            {!formData.createNewSet ? (
                                <>
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        <select
                                            className={`form-select ${errors.setId ? 'is-invalid' : ''}`}
                                            value={formData.setId}
                                            onChange={(e) => handleChange('setId', e.target.value)}
                                            style={{ flex: 1 }}
                                        >
                                            <option value="">-- Chọn bộ đề có sẵn --</option>
                                            {testSets.map(set => (
                                                <option key={set.id} value={set.id}>
                                                    {set.name} ({set.code})
                                                </option>
                                            ))}
                                        </select>
                                        <button
                                            type="button"
                                            className="btn btn-secondary"
                                            onClick={() => toggleCreateNewSet(true)}
                                            title="Tạo bộ đề mới"
                                        >
                                            <FiPlus />
                                        </button>
                                    </div>
                                    {errors.setId && (
                                        <div className="form-error">
                                            <FiAlertCircle /> {errors.setId}
                                        </div>
                                    )}
                                </>
                            ) : (
                                <div style={{ 
                                    padding: '12px', 
                                    background: 'var(--surface-card, rgba(255,255,255,0.03))',
                                    borderRadius: 'var(--radius-md, 8px)',
                                    border: '1px dashed var(--primary, #4a90e2)'
                                }}>
                                    <div style={{ marginBottom: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <span style={{ fontWeight: 600, fontSize: '13px', color: 'var(--primary, #4a90e2)' }}>
                                            Tạo bộ đề mới
                                        </span>
                                        <button
                                            type="button"
                                            className="btn btn-ghost btn-sm"
                                            onClick={() => toggleCreateNewSet(false)}
                                            style={{ fontSize: '12px', padding: '4px 8px' }}
                                        >
                                            Hủy
                                        </button>
                                    </div>
                                    <input
                                        type="text"
                                        className={`form-input ${errors.setCode ? 'is-invalid' : ''}`}
                                        placeholder="Mã bộ đề (VD: ielts_ai_2026)"
                                        value={formData.setCode}
                                        onChange={(e) => handleChange('setCode', e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, ''))}
                                        style={{ marginBottom: '8px' }}
                                    />
                                    {errors.setCode && (
                                        <div className="form-error" style={{ marginBottom: '8px' }}>
                                            <FiAlertCircle /> {errors.setCode}
                                        </div>
                                    )}
                                    <input
                                        type="text"
                                        className={`form-input ${errors.setName ? 'is-invalid' : ''}`}
                                        placeholder="Tên bộ đề (VD: IELTS AI Practice 2026)"
                                        value={formData.setName}
                                        onChange={(e) => handleChange('setName', e.target.value)}
                                    />
                                    {errors.setName && (
                                        <div className="form-error">
                                            <FiAlertCircle /> {errors.setName}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>

                        {/* 3. TEST NUMBER */}
                        <div className="form-group">
                            <label className="form-label">
                                Số thứ tự đề thi
                            </label>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                <input
                                    type="number"
                                    className="form-input"
                                    value={formData.testNumber}
                                    onChange={(e) => handleChange('testNumber', parseInt(e.target.value) || 1)}
                                    min={1}
                                    style={{ width: '100px' }}
                                />
                                <small style={{ color: 'var(--text-secondary, rgba(255,255,255,0.6))' }}>
                                    {formData.setId && !formData.createNewSet 
                                        ? `(${testsInSet.length} đề hiện có trong bộ)`
                                        : '(Tự động tăng)'
                                    }
                                </small>
                            </div>
                        </div>

                        {/* 4. TEST NAME - REQUIRED */}
                        <div className="form-group">
                            <label className="form-label">
                                <FiType className="form-icon" />
                                Tên đề thi <span className="required">*</span>
                            </label>
                            <input
                                type="text"
                                className={`form-input ${errors.testName ? 'is-invalid' : ''}`}
                                placeholder="VD: IELTS Academic Reading Test 1"
                                value={formData.testName}
                                onChange={(e) => handleChange('testName', e.target.value)}
                            />
                            {errors.testName && (
                                <div className="form-error">
                                    <FiAlertCircle /> {errors.testName}
                                </div>
                            )}
                        </div>

                        {/* 5. DIFFICULTY */}
                        <div className="form-group">
                            <label className="form-label">
                                <FiBarChart2 className="form-icon" />
                                Độ khó
                            </label>
                            <select
                                className="form-select"
                                value={formData.difficulty}
                                onChange={(e) => handleChange('difficulty', e.target.value)}
                            >
                                {difficultyOptions.map(opt => (
                                    <option key={opt.value} value={opt.value}>
                                        {opt.label} ({opt.labelEn})
                                    </option>
                                ))}
                            </select>
                        </div>

                        {/* 6. HASHTAGS - Max 20 */}
                        <div className="form-group">
                            <label className="form-label">
                                <FiHash className="form-icon" />
                                Hashtags
                                <span style={{ 
                                    marginLeft: '8px', 
                                    fontSize: '12px', 
                                    color: formData.hashtags.length >= 20 
                                        ? 'var(--danger, #f56565)' 
                                        : 'var(--text-secondary, rgba(255,255,255,0.6))'
                                }}>
                                    Đã chọn: {formData.hashtags.length}/20
                                </span>
                            </label>
                            <TagInput
                                mode="select"
                                value={formData.hashtags}
                                onChange={(val) => handleChange('hashtags', val)}
                                maxTags={20}
                                placeholder="Chọn hashtag..."
                            />
                            <small className="form-hint">
                                Gắn thẻ để dễ dàng tìm kiếm và phân loại sau này
                            </small>
                        </div>

                    </form>
                </div>

                <div className="modal-footer">
                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={onClose}
                        disabled={isSubmitting}
                    >
                        Hủy
                    </button>
                    <button
                        type="submit"
                        form="save-content-form"
                        className="btn btn-primary"
                        disabled={isSubmitting}
                    >
                        <FiSave style={{ marginRight: '6px' }} />
                        {isSubmitting ? 'Đang lưu...' : 'Lưu vào CSDL'}
                    </button>
                </div>
            </div>
        </div>
    );
}
