import React, { useEffect, useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useParams, useNavigate, Link, useSearchParams } from 'react-router-dom';
import useTestSetStore from '../../stores/useTestSetStore';
import useHashtagStore from '../../stores/useHashtagStore';
import { DeleteConfirmModal } from '../../components/ConfirmModal/ConfirmModal';
import ConfirmModal from '../../components/ConfirmModal/ConfirmModal';
import {
    FiPlus,
    FiEdit2,
    FiTrash2,
    FiChevronRight,
    FiCheck,
    FiX,
    FiCopy,
    FiFolder,
    FiRefreshCw,
    FiArrowLeft,
    FiEye,
    FiEyeOff
} from 'react-icons/fi';
import '../../css/pages/content/SetDetailPage.css';

/**
 * SetDetailPage - Display and manage tests within a test set
 * Part of Phase 5.2 of Test Storage Management System Overhaul
 * 
 * @since 2025-12-26
 */
export default function SetDetailPage() {
    const { setId } = useParams();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    // Store state and actions
    const {
        selectedSet,
        selectedSetTests,
        isLoading,
        isLoadingTests,
        error,
        fetchTestSetById,
        createTest,
        deleteTest,
        publishTest,
        publishTestSet,
        duplicateTest,
        clearError
    } = useTestSetStore();

    const { hashtags, fetchHashtags } = useHashtagStore();

    // Local state
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [showEditSetModal, setShowEditSetModal] = useState(searchParams.get('edit') === 'true');
    const [showPublishModal, setShowPublishModal] = useState(false);
    const [selectedTest, setSelectedTest] = useState(null);
    const [isDeleting, setIsDeleting] = useState(false);
    const [isPublishing, setIsPublishing] = useState(false);

    // Fetch data on mount
    useEffect(() => {
        if (setId) {
            fetchTestSetById(Number(setId));
            fetchHashtags();
        }
    }, [setId, fetchTestSetById, fetchHashtags]);

    // Handle refresh
    const handleRefresh = () => {
        if (setId) {
            fetchTestSetById(Number(setId));
        }
    };

    // Handle edit set
    const handleEditSet = () => {
        setShowEditSetModal(true);
    };

    // Handle publish/unpublish set
    const handlePublishSet = async (publish) => {
        setIsPublishing(true);
        try {
            await publishTestSet(Number(setId), publish);
        } catch (err) {
            console.error('Error publishing test set:', err);
        } finally {
            setIsPublishing(false);
        }
    };

    // Handle create test
    const handleCreateTest = async (data) => {
        try {
            const newTest = await createTest(Number(setId), data);
            setShowCreateModal(false);
            // Navigate to the test editor
            navigate(`/admin/content/tests/${newTest.id}`);
        } catch (err) {
            console.error('Error creating test:', err);
        }
    };

    // Handle duplicate test
    const handleDuplicate = async (test) => {
        const existingNumbers = selectedSetTests.map(t => t.testNumber);
        const newNumber = Math.max(...existingNumbers, 0) + 1;
        try {
            await duplicateTest(test.id, newNumber);
        } catch (err) {
            console.error('Error duplicating test:', err);
        }
    };

    // Handle publish/unpublish test
    const handlePublishTest = async (testId, publish) => {
        try {
            await publishTest(testId, publish);
        } catch (err) {
            console.error('Error publishing test:', err);
        }
    };

    // Confirm delete test
    const confirmDeleteTest = (test) => {
        setSelectedTest(test);
        setShowDeleteModal(true);
    };

    // Handle delete test
    const handleDeleteTest = async () => {
        if (!selectedTest) return;
        setIsDeleting(true);
        try {
            await deleteTest(selectedTest.id);
            setShowDeleteModal(false);
            setSelectedTest(null);
        } catch (err) {
            console.error('Error deleting test:', err);
        } finally {
            setIsDeleting(false);
        }
    };

    // Navigate to test editor
    const handleTestClick = (test) => {
        navigate(`/admin/content/tests/${test.id}`);
    };

    // Existing test numbers for validation
    const existingTestNumbers = useMemo(() => {
        return selectedSetTests.map(t => t.testNumber);
    }, [selectedSetTests]);

    // Loading state
    if (isLoading || !selectedSet) {
        return (
            <div className="admin-page set-detail-page">
                <div className="content-loading">
                    <div className="spinner"></div>
                    <p>Đang tải thông tin bộ đề...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="admin-page set-detail-page">
            {/* Breadcrumb */}
            <nav className="breadcrumb">
                <Link to="/admin/content/sets" className="breadcrumb__link">
                    <FiFolder size={14} />
                    Quản lý Bộ đề
                </Link>
                <FiChevronRight size={14} className="breadcrumb__separator" />
                <span className="breadcrumb__current">{selectedSet.name || selectedSet.code}</span>
            </nav>

            {/* Set Header */}
            <header className="set-header">
                <div className="set-header__left">
                    <button
                        className="set-header__back"
                        onClick={() => navigate('/admin/content/sets')}
                        title="Quay lại"
                    >
                        <FiArrowLeft size={20} />
                    </button>
                    <div className="set-header__info">
                        <div className="set-header__title-row">
                            <h1 className="set-header__title">
                                {selectedSet.name || selectedSet.code}
                            </h1>
                            <span className={`status-badge ${selectedSet.isPublished ? 'status-badge--published' : 'status-badge--draft'}`}>
                                {selectedSet.isPublished ? 'Đã xuất bản' : 'Bản nháp'}
                            </span>
                        </div>
                        {selectedSet.description && (
                            <p className="set-header__description">{selectedSet.description}</p>
                        )}
                        <div className="set-header__meta">
                            <span className="set-header__meta-item">
                                Mã: <strong>{selectedSet.code}</strong>
                            </span>
                            <span className="set-header__meta-item">
                                {selectedSetTests.length} bài thi
                            </span>
                        </div>
                    </div>
                </div>

                <div className="set-header__actions">
                    <button
                        className="admin-btn admin-btn--secondary"
                        onClick={handleRefresh}
                        disabled={isLoading}
                    >
                        <FiRefreshCw size={16} className={isLoading ? 'spinning' : ''} />
                    </button>
                    <button
                        className="admin-btn admin-btn--secondary"
                        onClick={handleEditSet}
                    >
                        <FiEdit2 size={16} />
                        Chỉnh sửa
                    </button>
                    <button
                        className={`admin-btn ${selectedSet.isPublished ? 'admin-btn--warning' : 'admin-btn--success'}`}
                        onClick={() => handlePublishSet(!selectedSet.isPublished)}
                        disabled={isPublishing}
                    >
                        {selectedSet.isPublished ? (
                            <>
                                <FiEyeOff size={16} />
                                Gỡ xuất bản
                            </>
                        ) : (
                            <>
                                <FiEye size={16} />
                                Xuất bản
                            </>
                        )}
                    </button>
                    <button
                        className="admin-btn admin-btn--primary"
                        onClick={() => setShowCreateModal(true)}
                    >
                        <FiPlus size={16} />
                        Thêm bài thi
                    </button>
                </div>
            </header>

            {/* Error message */}
            {error && (
                <div className="content-error">
                    <p>{error}</p>
                    <button
                        className="admin-btn admin-btn--primary"
                        onClick={() => { clearError(); handleRefresh(); }}
                    >
                        Thử lại
                    </button>
                </div>
            )}

            {/* Tests Grid */}
            <div className="content-area">
                {isLoadingTests ? (
                    <div className="content-loading">
                        <div className="spinner"></div>
                        <p>Đang tải danh sách bài thi...</p>
                    </div>
                ) : (
                    <div className="tests-grid">
                        {selectedSetTests.map(test => (
                            <div
                                key={test.id}
                                className="test-card"
                                onClick={() => handleTestClick(test)}
                            >
                                {/* Test Header */}
                                <div className="test-card__header">
                                    <span className="test-card__number">Bài {test.testNumber}</span>
                                    <span className={`status-dot ${test.isPublished ? 'status-dot--published' : 'status-dot--draft'}`} />
                                </div>

                                {/* Test Name */}
                                <h3 className="test-card__title">
                                    {test.name || test.name || `Test ${test.testNumber}`}
                                </h3>

                                {/* Skill Indicators */}
                                <SkillIndicator skillCounts={test.skillSectionCounts} />

                                {/* Hashtags */}
                                {test.hashtags && test.hashtags.length > 0 && (
                                    <div className="test-card__hashtags">
                                        {test.hashtags.slice(0, 3).map(tag => (
                                            <span
                                                key={tag.id}
                                                className="hashtag"
                                                style={{
                                                    backgroundColor: (tag.color || '#8B5CF6') + '20',
                                                    color: tag.color || '#8B5CF6'
                                                }}
                                            >
                                                {tag.icon} {tag.name || tag.name}
                                            </span>
                                        ))}
                                        {test.hashtags.length > 3 && (
                                            <span className="hashtag hashtag--more">
                                                +{test.hashtags.length - 3}
                                            </span>
                                        )}
                                    </div>
                                )}

                                {/* Test Meta */}
                                <div className="test-card__meta">
                                    {test.difficulty && (
                                        <span className={`difficulty-badge difficulty-badge--${test.difficulty.toLowerCase()}`}>
                                            {test.difficulty}
                                        </span>
                                    )}
                                    {test.isAiGenerated && (
                                        <span className="ai-badge">AI</span>
                                    )}
                                </div>

                                {/* Test Actions */}
                                <div className="test-card__actions" onClick={(e) => e.stopPropagation()}>
                                    <button
                                        className="test-card__action-btn"
                                        onClick={() => handleDuplicate(test)}
                                        title="Nhân bản"
                                    >
                                        <FiCopy size={14} />
                                    </button>
                                    <button
                                        className="test-card__action-btn"
                                        onClick={() => handlePublishTest(test.id, !test.isPublished)}
                                        title={test.isPublished ? 'Gỡ xuất bản' : 'Xuất bản'}
                                    >
                                        {test.isPublished ? <FiEyeOff size={14} /> : <FiEye size={14} />}
                                    </button>
                                    <button
                                        className="test-card__action-btn test-card__action-btn--delete"
                                        onClick={() => confirmDeleteTest(test)}
                                        title="Xóa"
                                    >
                                        <FiTrash2 size={14} />
                                    </button>
                                </div>
                            </div>
                        ))}

                        {/* Add Test Card - Also serves as empty state prompt */}
                        <div
                            className="test-card test-card--add"
                            onClick={() => setShowCreateModal(true)}
                        >
                            <FiPlus size={32} />
                            <span>
                                {selectedSetTests.length === 0
                                    ? 'Thêm bài thi đầu tiên'
                                    : 'Thêm bài thi mới'}
                            </span>
                        </div>
                    </div>
                )}
            </div>

            {/* Create Test Modal */}
            {showCreateModal && createPortal(
                <CreateTestModal
                    setId={Number(setId)}
                    existingTestNumbers={existingTestNumbers}
                    hashtags={hashtags}
                    onClose={() => setShowCreateModal(false)}
                    onSubmit={handleCreateTest}
                />,
                document.body
            )}

            {/* Delete Confirmation Modal */}
            {showDeleteModal && createPortal(
                <DeleteConfirmModal
                    isOpen={showDeleteModal}
                    onClose={() => { setShowDeleteModal(false); setSelectedTest(null); }}
                    onConfirm={handleDeleteTest}
                    itemName={selectedTest ? `Bài ${selectedTest.testNumber}` : ''}
                    loading={isDeleting}
                />,
                document.body
            )}

            {/* Edit Set Modal */}
            {showEditSetModal && createPortal(
                <EditSetModal
                    testSet={selectedSet}
                    onClose={() => setShowEditSetModal(false)}
                />,
                document.body
            )}
        </div>
    );
}

/**
 * SkillIndicator - Shows skill completion status
 */
function SkillIndicator({ skillCounts }) {
    const skills = [
        { key: 'reading', label: 'R', color: '#3B82F6' },
        { key: 'listening', label: 'L', color: '#10B981' },
        { key: 'writing', label: 'W', color: '#F59E0B' },
        { key: 'speaking', label: 'S', color: '#EC4899' }
    ];

    return (
        <div className="skill-indicators">
            {skills.map(skill => {
                const count = skillCounts?.[skill.key] || 0;
                const hasContent = count > 0;
                return (
                    <span
                        key={skill.key}
                        className={`skill-badge ${hasContent ? 'skill-badge--complete' : 'skill-badge--empty'}`}
                        style={{
                            borderColor: skill.color,
                            backgroundColor: hasContent ? skill.color + '20' : 'transparent',
                            color: hasContent ? skill.color : 'var(--admin-text-muted)'
                        }}
                        title={`${skill.key}: ${count} sections`}
                    >
                        {skill.label}
                        {hasContent && <FiCheck size={10} />}
                    </span>
                );
            })}
        </div>
    );
}

/**
 * CreateTestModal - Modal for creating a new test
 */
function CreateTestModal({ setId, existingTestNumbers, hashtags, onClose, onSubmit }) {
    const topicHashtags = hashtags.filter(h => h.category === 'topic');

    const [formData, setFormData] = useState({
        testNumber: Math.max(...existingTestNumbers, 0) + 1,
        name: '',
        name: '',
        difficulty: 'INTERMEDIATE',
        hashtagIds: []
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

    // Handle hashtag toggle
    const handleHashtagToggle = (hashtagId) => {
        setFormData(prev => ({
            ...prev,
            hashtagIds: prev.hashtagIds.includes(hashtagId)
                ? prev.hashtagIds.filter(id => id !== hashtagId)
                : [...prev.hashtagIds, hashtagId]
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
                    <h2>Thêm bài thi mới</h2>
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

                        {/* Name Vietnamese */}
                        <div className="form-group">
                            <label htmlFor="name">Tên tiếng Việt</label>
                            <input
                                type="text"
                                id="name"
                                name="name"
                                className="form-input"
                                placeholder="vd: Bài thi 1 - Chủ đề Giáo dục"
                                value={formData.name}
                                onChange={handleChange}
                                disabled={isSubmitting}
                            />
                        </div>

                        {/* Name English */}
                        <div className="form-group">
                            <label htmlFor="name">Tên tiếng Anh</label>
                            <input
                                type="text"
                                id="name"
                                name="name"
                                className="form-input"
                                placeholder="e.g. Test 1 - Education Topic"
                                value={formData.name}
                                onChange={handleChange}
                                disabled={isSubmitting}
                            />
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
                                <option value="INTERMEDIATE">Trung bình (Intermediate)</option>
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
                                            key={tag.id}
                                            type="button"
                                            className={`hashtag-option ${formData.hashtagIds.includes(tag.id) ? 'hashtag-option--selected' : ''}`}
                                            onClick={() => handleHashtagToggle(tag.id)}
                                            style={{
                                                borderColor: tag.color || '#8B5CF6',
                                                backgroundColor: formData.hashtagIds.includes(tag.id)
                                                    ? (tag.color || '#8B5CF6') + '20'
                                                    : 'transparent'
                                            }}
                                        >
                                            {tag.icon} {tag.name || tag.name}
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
                                    Đang tạo...
                                </>
                            ) : (
                                <>
                                    <FiPlus size={16} />
                                    Tạo bài thi
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

/**
 * EditSetModal - Modal for editing test set details
 * Placeholder - full implementation would include all fields
 */
function EditSetModal({ testSet, onClose }) {
    const { updateTestSet } = useTestSetStore();
    const [formData, setFormData] = useState({
        name: testSet.name || '',
        description: testSet.description || '',
        sourceType: testSet.sourceType || 'custom'
    });
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        try {
            await updateTestSet(testSet.id, formData);
            onClose();
        } catch (err) {
            console.error('Error updating test set:', err);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="admin-modal-overlay-custom" onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="admin-edit-modal" onClick={e => e.stopPropagation()}>
                <div className="admin-edit-modal-header">
                    <h2>Chỉnh sửa bộ đề</h2>
                    <button className="admin-edit-modal-close" onClick={onClose} disabled={isSubmitting}>
                        <FiX size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    <div className="admin-edit-modal-body">
                        <div className="form-group">
                            <label htmlFor="edit-name">Tên bộ đề *</label>
                            <input
                                type="text"
                                id="edit-name"
                                name="name"
                                className="form-input"
                                value={formData.name}
                                onChange={handleChange}
                                disabled={isSubmitting}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="edit-description">Mô tả</label>
                            <textarea
                                id="edit-description"
                                name="description"
                                className="form-textarea"
                                value={formData.description}
                                onChange={handleChange}
                                disabled={isSubmitting}
                                rows={3}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="edit-sourceType">Loại nguồn</label>
                            <select
                                id="edit-sourceType"
                                name="sourceType"
                                className="form-select"
                                value={formData.sourceType}
                                onChange={handleChange}
                                disabled={isSubmitting}
                            >
                                <option value="custom">Tùy chỉnh</option>
                                <option value="cambridge">Cambridge</option>
                                <option value="ai_generated">AI tạo</option>
                            </select>
                        </div>
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
                            {isSubmitting ? 'Đang lưu...' : 'Lưu thay đổi'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
