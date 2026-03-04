import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import useTestSetStore from '../../stores/useTestSetStore';
import { DeleteConfirmModal } from '../../components/ConfirmModal/ConfirmModal';
import {
    FiPlus,
    FiEdit2,
    FiTrash2,
    FiSearch,
    FiFolder,
    FiCpu,
    FiBook,
    FiRefreshCw,
    FiX,
    FiImage,
    FiGlobe,
    FiZap
} from 'react-icons/fi';
import '../../css/pages/content/SetListPage.css';
import '../../css/pages/content/SetDetailPage.css'; // For unified modal styles

/**
 * SetListPage - Display and manage test sets in a grid layout
 * Part of Phase 5.1 of Test Storage Management System Overhaul
 * 
 * @since 2025-12-26
 */
export default function SetListPage() {
    const navigate = useNavigate();

    // Store state and actions
    const {
        testSets,
        isLoading,
        error,
        fetchTestSets,
        createTestSet,
        deleteTestSet,
        clearError
    } = useTestSetStore();

    // Local state
    const [searchQuery, setSearchQuery] = useState('');
    const [sourceFilter, setSourceFilter] = useState('all');
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [selectedSet, setSelectedSet] = useState(null);
    const [isDeleting, setIsDeleting] = useState(false);

    // Fetch test sets on mount
    useEffect(() => {
        fetchTestSets();
    }, [fetchTestSets]);

    // Filter logic
    const filteredSets = useMemo(() => {
        return testSets.filter(set => {
            const searchLower = searchQuery.toLowerCase();
            const matchesSearch =
                set.name?.toLowerCase().includes(searchLower) ||
                set.code?.toLowerCase().includes(searchLower);
            const matchesSource = sourceFilter === 'all' || set.sourceType === sourceFilter;
            return matchesSearch && matchesSource;
        });
    }, [testSets, searchQuery, sourceFilter]);

    // Source type icon mapping
    const getSourceIcon = (type) => {
        switch (type) {
            case 'cambridge':
                return <FiBook className="source-icon source-icon--cambridge" />;
            case 'ai_generated':
                return <FiCpu className="source-icon source-icon--ai" />;
            case 'custom':
            default:
                return <FiFolder className="source-icon source-icon--custom" />;
        }
    };

    // Source type label mapping
    const getSourceLabel = (type) => {
        switch (type) {
            case 'cambridge': return 'Cambridge';
            case 'ai_generated': return 'AI';
            case 'custom': return 'Custom';
            default: return type;
        }
    };

    // Handle refresh
    const handleRefresh = () => {
        fetchTestSets(true);
    };

    // Handle create
    const handleCreate = async (data) => {
        try {
            const newSet = await createTestSet(data);
            setShowCreateModal(false);
            // Navigate to the new set detail page
            navigate(`/admin/content/sets/${newSet.id}`);
        } catch (err) {
            console.error('Error creating test set:', err);
        }
    };

    // Handle edit
    const handleEdit = (set, e) => {
        e.stopPropagation();
        navigate(`/admin/content/sets/${set.id}?edit=true`);
    };

    // Confirm delete
    const confirmDelete = (set, e) => {
        e.stopPropagation();
        setSelectedSet(set);
        setShowDeleteModal(true);
    };

    // Handle delete
    const handleDelete = async () => {
        if (!selectedSet) return;
        setIsDeleting(true);
        try {
            await deleteTestSet(selectedSet.id);
            setShowDeleteModal(false);
            setSelectedSet(null);
        } catch (err) {
            console.error('Error deleting test set:', err);
        } finally {
            setIsDeleting(false);
        }
    };

    // Navigate to set detail
    const handleCardClick = (set) => {
        navigate(`/admin/content/sets/${set.id}`);
    };

    return (
        <div className="admin-page set-list-page">
            {/* Page Header */}
            <div className="admin-page__header">
                <div>
                    <h1 className="admin-page__title">Quản lý Bộ đề</h1>
                    <p className="admin-page__subtitle">
                        Tạo và quản lý các bộ đề thi IELTS
                    </p>
                </div>
                <div className="set-list-page__actions">
                    <button
                        className="admin-btn admin-btn--secondary"
                        onClick={handleRefresh}
                        disabled={isLoading}
                    >
                        <FiRefreshCw size={16} className={isLoading ? 'spinning' : ''} />
                        <span>Làm mới</span>
                    </button>
                    <button
                        className="admin-btn admin-btn--ai"
                        onClick={() => navigate('/admin/content/generate')}
                    >
                        <FiZap size={16} />
                        <span>Tạo bằng AI</span>
                    </button>
                    <button
                        className="admin-btn admin-btn--primary"
                        onClick={() => setShowCreateModal(true)}
                    >
                        <FiPlus size={16} />
                        <span>Tạo bộ đề mới</span>
                    </button>
                </div>
            </div>

            {/* Toolbar */}
            <div className="set-toolbar">
                <div className="set-toolbar__left">
                    <div className="search-box">
                        <FiSearch className="search-box__icon" />
                        <input
                            type="text"
                            placeholder="Tìm kiếm bộ đề..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="search-box__input"
                        />
                        {searchQuery && (
                            <button
                                className="search-box__clear"
                                onClick={() => setSearchQuery('')}
                            >
                                <FiX size={14} />
                            </button>
                        )}
                    </div>
                </div>

                <div className="set-toolbar__right">
                    <div className="filter-tabs">
                        <button
                            className={`filter-tab ${sourceFilter === 'all' ? 'filter-tab--active' : ''}`}
                            onClick={() => setSourceFilter('all')}
                        >
                            <FiGlobe size={14} />
                            Tất cả
                        </button>
                        <button
                            className={`filter-tab ${sourceFilter === 'cambridge' ? 'filter-tab--active' : ''}`}
                            onClick={() => setSourceFilter('cambridge')}
                        >
                            <FiBook size={14} />
                            Cambridge
                        </button>
                        <button
                            className={`filter-tab ${sourceFilter === 'custom' ? 'filter-tab--active' : ''}`}
                            onClick={() => setSourceFilter('custom')}
                        >
                            <FiFolder size={14} />
                            Tùy chỉnh
                        </button>
                        <button
                            className={`filter-tab ${sourceFilter === 'ai_generated' ? 'filter-tab--active' : ''}`}
                            onClick={() => setSourceFilter('ai_generated')}
                        >
                            <FiCpu size={14} />
                            AI tạo
                        </button>
                    </div>
                </div>
            </div>

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

            {/* Content Area */}
            <div className="content-area">
                {isLoading ? (
                    <div className="content-loading">
                        <div className="spinner"></div>
                        <p>Đang tải danh sách bộ đề...</p>
                    </div>
                ) : (
                    <div className="sets-grid">
                        {filteredSets.map(set => (
                            <div
                                key={set.id}
                                className="set-card"
                                onClick={() => handleCardClick(set)}
                            >
                                {/* Cover Image */}
                                <div className="set-card__cover">
                                    {set.coverImageUrl ? (
                                        <img src={set.coverImageUrl} alt={set.name} />
                                    ) : (
                                        <div className="set-card__cover-placeholder">
                                            {getSourceIcon(set.sourceType)}
                                        </div>
                                    )}
                                    {/* Source type badge */}
                                    <span className={`set-card__source-badge set-card__source-badge--${set.sourceType}`}>
                                        {getSourceIcon(set.sourceType)}
                                        {getSourceLabel(set.sourceType)}
                                    </span>
                                </div>

                                {/* Set Info */}
                                <div className="set-card__info">
                                    <h3 className="set-card__name">{set.name || set.code}</h3>
                                    <p className="set-card__code">{set.code}</p>

                                    <div className="set-card__stats">
                                        <span className="set-card__stat">
                                            <FiFolder size={12} />
                                            {set.testCount || 0} bài thi
                                        </span>
                                        <span className="set-card__separator">-</span>
                                        <span className={`set-card__status ${set.isPublished ? 'set-card__status--published' : 'set-card__status--draft'}`}>
                                            {set.isPublished ? 'Đã xuất bản' : 'Bản nháp'}
                                        </span>
                                    </div>
                                </div>

                                {/* Actions */}
                                <div className="set-card__actions" onClick={(e) => e.stopPropagation()}>
                                    <button
                                        className="set-card__action-btn"
                                        onClick={(e) => handleEdit(set, e)}
                                        title="Chỉnh sửa"
                                    >
                                        <FiEdit2 size={16} />
                                    </button>
                                    <button
                                        className="set-card__action-btn set-card__action-btn--delete"
                                        onClick={(e) => confirmDelete(set, e)}
                                        title="Xóa"
                                    >
                                        <FiTrash2 size={16} />
                                    </button>
                                </div>
                            </div>
                        ))}

                        {/* Empty State */}
                        {filteredSets.length === 0 && !isLoading && (
                            <div className="empty-state">
                                <FiFolder size={48} />
                                <p>Không tìm thấy bộ đề nào</p>
                                {searchQuery || sourceFilter !== 'all' ? (
                                    <button
                                        className="admin-btn admin-btn--secondary"
                                        onClick={() => { setSearchQuery(''); setSourceFilter('all'); }}
                                    >
                                        Xóa bộ lọc
                                    </button>
                                ) : (
                                    <button
                                        className="admin-btn admin-btn--primary"
                                        onClick={() => setShowCreateModal(true)}
                                    >
                                        <FiPlus size={16} />
                                        Tạo bộ đề đầu tiên
                                    </button>
                                )}
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Create Modal */}
            {showCreateModal && (
                <CreateSetModal
                    onClose={() => setShowCreateModal(false)}
                    onSubmit={handleCreate}
                />
            )}

            {/* Delete Confirmation Modal */}
            <DeleteConfirmModal
                isOpen={showDeleteModal}
                onClose={() => { setShowDeleteModal(false); setSelectedSet(null); }}
                onConfirm={handleDelete}
                itemName={selectedSet?.name || selectedSet?.code || ''}
                loading={isDeleting}
            />
        </div>
    );
}

/**
 * CreateSetModal - Modal for creating a new test set
 */
function CreateSetModal({ onClose, onSubmit }) {
    const [formData, setFormData] = useState({
        code: '',
        name: '',
        description: '',
        sourceType: 'custom',
        isPublished: false
    });
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errors, setErrors] = useState({});

    // Handle input change
    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
        // Clear error when user types
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: null }));
        }
    };

    // Validate form
    const validate = () => {
        const newErrors = {};
        if (!formData.code.trim()) {
            newErrors.code = 'Mã bộ đề là bắt buộc';
        } else if (!/^[a-z0-9_-]+$/.test(formData.code)) {
            newErrors.code = 'Mã chỉ chứa chữ thường, số, gạch ngang và gạch dưới';
        }
        if (!formData.name.trim()) {
            newErrors.name = 'Tên bộ đề là bắt buộc';
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
            console.error('Error creating set:', err);
            setErrors({ submit: err.message || 'Lỗi khi tạo bộ đề' });
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
            <div className="admin-edit-modal create-set-modal" onClick={e => e.stopPropagation()}>
                <div className="admin-edit-modal-header">
                    <h2>Tạo bộ đề mới</h2>
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
                        {/* Code */}
                        <div className="form-group">
                            <label htmlFor="code">Mã bộ đề *</label>
                            <input
                                type="text"
                                id="code"
                                name="code"
                                className={`form-input ${errors.code ? 'form-input--error' : ''}`}
                                placeholder="vd: cambridge_17, custom_set_1"
                                value={formData.code}
                                onChange={handleChange}
                                disabled={isSubmitting}
                            />
                            {errors.code && <span className="form-error">{errors.code}</span>}
                            <p className="form-hint">Mã duy nhất để nhận dạng bộ đề (không dấu, chữ thường)</p>
                        </div>

                        {/* Name */}
                        <div className="form-group">
                            <label htmlFor="name">Tên bộ đề *</label>
                            <input
                                type="text"
                                id="name"
                                name="name"
                                className={`form-input ${errors.name ? 'form-input--error' : ''}`}
                                placeholder="vd: Cambridge IELTS 17"
                                value={formData.name}
                                onChange={handleChange}
                                disabled={isSubmitting}
                            />
                            {errors.name && <span className="form-error">{errors.name}</span>}
                        </div>

                        {/* Source Type */}
                        <div className="form-group">
                            <label htmlFor="sourceType">Loại nguồn</label>
                            <select
                                id="sourceType"
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

                        {/* Description */}
                        <div className="form-group">
                            <label htmlFor="description">Mô tả</label>
                            <textarea
                                id="description"
                                name="description"
                                className="form-textarea"
                                placeholder="Mô tả ngắn về bộ đề..."
                                value={formData.description}
                                onChange={handleChange}
                                disabled={isSubmitting}
                                rows={3}
                            />
                        </div>

                        {/* Published */}
                        <div className="form-group form-group--checkbox">
                            <label className="checkbox-label">
                                <input
                                    type="checkbox"
                                    name="isPublished"
                                    checked={formData.isPublished}
                                    onChange={handleChange}
                                    disabled={isSubmitting}
                                />
                                <span className="checkbox-text">Xuất bản ngay</span>
                            </label>
                            <p className="form-hint">Bộ đề sẽ hiển thị cho người dùng nếu được xuất bản</p>
                        </div>

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
                                    Tạo bộ đề
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
