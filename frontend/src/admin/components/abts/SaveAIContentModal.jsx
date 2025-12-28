import React, { useState, useEffect, useMemo } from 'react';
import { FiX, FiSave, FiFolder, FiType, FiBarChart2, FiFileText, FiLayers } from 'react-icons/fi';
import useTestSetStore from '../../stores/useTestSetStore';
import { testsApi } from '../../api/adminApi';
import TagInput from './TagInput';
import '../../components/common/AdminModal.css';

/**
 * SaveAIContentModal - Configuration modal for saving AI generated content.
 * Updated to support adding parts to existing tests.
 * 
 * @since 2025-12-26
 * @updated 2025-12-28 - Added test selector and part display
 */
export default function SaveAIContentModal({
    isOpen,
    onClose,
    onSave,
    initialTopic = '',
    suggestedSkill = 'reading',
    partNumber = 1 // Current part being saved
}) {
    const { testSets, fetchTestSets, isLoading } = useTestSetStore();

    const [formData, setFormData] = useState({
        setId: '', // Selected test set ID
        setCode: '', // Only used if creating new
        setNameVi: '', // Name for new test set
        testId: '', // Existing test ID (if adding to existing)
        testName: '', // Test name (only for new test)
        difficulty: 'INTERMEDIATE',
        hashtags: [], // Array of hashtag IDs
        createNewSet: false,
        addToExistingTest: false
    });

    const [testsInSet, setTestsInSet] = useState([]);
    const [isLoadingTests, setIsLoadingTests] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    // Skill display name
    const skillDisplayName = useMemo(() => {
        const names = { reading: 'Reading', listening: 'Listening', writing: 'Writing', speaking: 'Speaking' };
        return names[suggestedSkill?.toLowerCase()] || suggestedSkill;
    }, [suggestedSkill]);

    // Initialize form when modal opens
    useEffect(() => {
        if (isOpen) {
            fetchTestSets(true);
            const topicName = initialTopic || 'AI Generated';
            setFormData(prev => ({
                ...prev,
                testName: topicName,
                setNameVi: topicName,
                setCode: generateCode(topicName),
                testId: '',
                addToExistingTest: false
            }));
            setTestsInSet([]);
        }
    }, [isOpen, initialTopic, fetchTestSets]);

    // Fetch tests when a test set is selected
    useEffect(() => {
        const fetchTests = async () => {
            if (formData.setId && !formData.createNewSet) {
                setIsLoadingTests(true);
                try {
                    const tests = await testsApi.getBySetId(formData.setId);
                    setTestsInSet(tests || []);
                } catch (error) {
                    console.error('Error fetching tests:', error);
                    setTestsInSet([]);
                } finally {
                    setIsLoadingTests(false);
                }
            } else {
                setTestsInSet([]);
            }
        };
        fetchTests();
    }, [formData.setId, formData.createNewSet]);

    // Handle input changes
    const handleChange = (field, value) => {
        setFormData(prev => ({
            ...prev,
            [field]: value
        }));

        // Reset test selection when changing set or mode
        if (field === 'setId' || field === 'createNewSet') {
            setFormData(prev => ({
                ...prev,
                [field]: value,
                testId: '',
                addToExistingTest: false
            }));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        try {
            // Prepare data for callback
            const saveConfig = {
                // Test Set identification
                setId: formData.createNewSet ? null : (formData.setId || null),
                setCode: formData.createNewSet ? formData.setCode : null,
                setNameVi: formData.createNewSet ? formData.setNameVi : null,
                // Test identification
                testId: formData.addToExistingTest ? Number(formData.testId) : null,
                testName: formData.addToExistingTest ? null : formData.testName,
                // Other metadata
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
                    {/* Part Info Display */}
                    <div className="form-group">
                        <div className="info-badge" style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '12px',
                            padding: '12px 16px',
                            background: 'var(--surface-elevated, rgba(74, 144, 226, 0.1))',
                            borderRadius: 'var(--radius-md, 8px)',
                            border: '1px solid var(--border-color, rgba(255,255,255,0.1))'
                        }}>
                            <FiLayers style={{ color: 'var(--primary, #4a90e2)', fontSize: '20px' }} />
                            <div>
                                <div style={{ fontWeight: 600, color: 'var(--text-primary, #fff)' }}>
                                    {skillDisplayName} - Part {partNumber}
                                </div>
                                <small style={{ color: 'var(--text-secondary, rgba(255,255,255,0.6))' }}>
                                    Nội dung AI đã tạo sẽ được lưu vào phần này
                                </small>
                            </div>
                        </div>
                    </div>

                    <form id="save-content-form" onSubmit={handleSubmit}>

                        {/* 1. Test Set Selection */}
                        <div className="form-group">
                            <label className="form-label">
                                <FiFolder className="form-icon" />
                                Bộ đề thi (Folder)
                            </label>

                            {!formData.createNewSet ? (
                                <div style={{ display: 'flex', gap: '8px' }}>
                                    <select
                                        className="form-select"
                                        value={formData.setId}
                                        onChange={(e) => handleChange('setId', e.target.value)}
                                        style={{ flex: 1 }}
                                    >
                                        <option value="">-- Chọn bộ đề có sẵn --</option>
                                        {testSets.map(set => (
                                            <option key={set.id} value={set.id}>
                                                {set.nameVi} ({set.code})
                                            </option>
                                        ))}
                                    </select>
                                    <button
                                        type="button"
                                        className="btn btn-secondary"
                                        onClick={() => handleChange('createNewSet', true)}
                                    >
                                        Tạo mới
                                    </button>
                                </div>
                            ) : (
                                <div style={{ display: 'flex', gap: '8px' }}>
                                    <input
                                        type="text"
                                        className="form-input"
                                        placeholder="Nhập mã bộ đề mới (VD: speaking_2025)"
                                        value={formData.setCode}
                                        onChange={(e) => handleChange('setCode', e.target.value)}
                                        style={{ flex: 1 }}
                                        required
                                    />
                                    <button
                                        type="button"
                                        className="btn btn-secondary"
                                        onClick={() => handleChange('createNewSet', false)}
                                    >
                                        Hủy
                                    </button>
                                </div>
                            )}
                            <small className="form-hint">
                                {formData.createNewSet
                                    ? "Nhập mã cho bộ đề mới. Tên sẽ được tạo tự động."
                                    : "Chọn một bộ đề có sẵn để thêm bài tập này vào."}
                            </small>
                        </div>

                        {/* 2. Test Selection - Only show if set is selected */}
                        {formData.setId && !formData.createNewSet && (
                            <div className="form-group">
                                <label className="form-label">
                                    <FiFileText className="form-icon" />
                                    Bài thi
                                </label>

                                {/* Toggle between new and existing test */}
                                <div style={{
                                    display: 'flex',
                                    gap: '8px',
                                    marginBottom: '8px'
                                }}>
                                    <button
                                        type="button"
                                        className={`btn ${!formData.addToExistingTest ? 'btn-primary' : 'btn-secondary'}`}
                                        onClick={() => setFormData(prev => ({ ...prev, addToExistingTest: false, testId: '' }))}
                                        style={{ flex: 1 }}
                                    >
                                        Tạo bài mới
                                    </button>
                                    <button
                                        type="button"
                                        className={`btn ${formData.addToExistingTest ? 'btn-primary' : 'btn-secondary'}`}
                                        onClick={() => setFormData(prev => ({ ...prev, addToExistingTest: true }))}
                                        disabled={testsInSet.length === 0}
                                        style={{ flex: 1 }}
                                    >
                                        Thêm vào bài có sẵn {testsInSet.length > 0 && `(${testsInSet.length})`}
                                    </button>
                                </div>

                                {formData.addToExistingTest ? (
                                    <select
                                        className="form-select"
                                        value={formData.testId}
                                        onChange={(e) => handleChange('testId', e.target.value)}
                                        required
                                        disabled={isLoadingTests}
                                    >
                                        <option value="">
                                            {isLoadingTests ? 'Đang tải...' : '-- Chọn bài thi --'}
                                        </option>
                                        {testsInSet.map(test => (
                                            <option key={test.id} value={test.id}>
                                                {test.nameVi || `Test ${test.testNumber}`}
                                            </option>
                                        ))}
                                    </select>
                                ) : (
                                    <input
                                        type="text"
                                        className="form-input"
                                        placeholder="Nhập tên bài thi..."
                                        value={formData.testName}
                                        onChange={(e) => handleChange('testName', e.target.value)}
                                        required
                                    />
                                )}

                                <small className="form-hint">
                                    {formData.addToExistingTest
                                        ? `Part ${partNumber} sẽ được thêm vào bài thi đã chọn.`
                                        : "Bài thi mới sẽ được tạo trong bộ đề này."}
                                </small>
                            </div>
                        )}

                        {/* 3. Test Name - Only for new test without set selected */}
                        {(!formData.setId || formData.createNewSet) && (
                            <div className="form-group">
                                <label className="form-label">
                                    <FiType className="form-icon" />
                                    Tên bài thi <span className="required">*</span>
                                </label>
                                <input
                                    type="text"
                                    className="form-input"
                                    value={formData.testName}
                                    onChange={(e) => handleChange('testName', e.target.value)}
                                    required
                                />
                            </div>
                        )}

                        {/* 4. Difficulty Level */}
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
                                <option value="BEGINNER">Dễ (Beginner)</option>
                                <option value="INTERMEDIATE">Trung bình (Intermediate)</option>
                                <option value="ADVANCED">Khó (Advanced)</option>
                            </select>
                        </div>

                        {/* 5. Hashtags */}
                        <div className="form-group">
                            <TagInput
                                label="Hashtags"
                                mode="select"
                                value={formData.hashtags}
                                onChange={(val) => handleChange('hashtags', val)}
                                maxTags={5}
                                placeholder="Chọn hashtag (VD: #reading, #ielts)..."
                                helperText="Gắn thẻ để dễ dàng tìm kiếm sau này."
                            />
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
                        {isSubmitting ? 'Đang lưu...' : 'Lưu vào CSDL'}
                    </button>
                </div>
            </div>
        </div>
    );
}

function generateCode(name) {
    if (!name) return '';
    return String(name)
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '')
        .substring(0, 30);
}
