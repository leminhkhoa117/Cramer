/**
 * HashtagManagementPage - Admin page for managing hashtags
 * 
 * Features:
 * - List all hashtags grouped by category
 * - Filter by category, search by name/code
 * - Create, edit, delete (soft delete) hashtags
 * - Color picker and emoji selector
 * 
 * @since 2025-12-26 - Phase 5.5: Test Storage Management System Overhaul
 */

import React, { useEffect, useState, useMemo } from 'react';
import useHashtagStore from '../../stores/useHashtagStore';
import { FiPlus, FiEdit2, FiTrash2, FiSearch, FiTag, FiCheck, FiX, FiRefreshCw } from 'react-icons/fi';
import '../../css/pages/content/HashtagManagementPage.css';

// Category display mapping
const CATEGORIES = {
    topic: { label: 'Chủ đề', icon: '\u{1F4DA}' },
    theme: { label: 'Chủ điểm', icon: '\u{1F3AF}' },
    difficulty: { label: 'Độ khó', icon: '\u{1F4CA}' },
    source: { label: 'Nguồn', icon: '\u{1F4C1}' },
    skill_focus: { label: 'Kỹ năng', icon: '\u{1F393}' }
};

// Common emojis for quick selection
const COMMON_EMOJIS = [
    '\u{1F4DA}', '\u{1F4BB}', '\u{1F30D}', '\u{1F3E5}', '\u{1F4BC}', '\u{1F52C}', '\u{1F4DC}', '\u{1F465}', '\u{1F3A8}', '\u{1F981}',
    '\u{2708}', '\u{26BD}', '\u{1F37D}', '\u{1F3DB}', '\u{1F9E0}', '\u{1F4F0}', '\u{1F393}', '\u{1F9EA}', '\u{1F91D}', '\u{1F331}',
    '\u{1F333}', '\u{1F3D4}', '\u{1F3AF}', '\u{1F4CA}', '\u{1F4C1}', '\u{2B50}', '\u{1F4A1}', '\u{1F3C6}', '\u{1F4DD}', '\u{1F50D}'
];

// Preset colors for quick selection
const PRESET_COLORS = [
    '#6366F1', // Indigo
    '#8B5CF6', // Purple
    '#EC4899', // Pink
    '#EF4444', // Red
    '#F59E0B', // Amber
    '#10B981', // Emerald
    '#14B8A6', // Teal
    '#3B82F6', // Blue
    '#6B7280', // Gray
    '#1F2937', // Dark
];

const HashtagManagementPage = () => {
    const {
        hashtags,
        byCategory,
        isLoading,
        error,
        fetchHashtags,
        createHashtag,
        updateHashtag,
        deleteHashtag,
        clearError
    } = useHashtagStore();

    const [searchQuery, setSearchQuery] = useState('');
    const [categoryFilter, setCategoryFilter] = useState('all');
    const [showModal, setShowModal] = useState(false);
    const [editingHashtag, setEditingHashtag] = useState(null);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        fetchHashtags(true); // Force refresh on mount
    }, [fetchHashtags]);

    // Filter logic
    const filteredHashtags = useMemo(() => {
        return hashtags.filter(h => {
            const matchesSearch =
                h.nameVi?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                h.nameEn?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                h.code?.toLowerCase().includes(searchQuery.toLowerCase());
            const matchesCategory = categoryFilter === 'all' || h.category === categoryFilter;
            return matchesSearch && matchesCategory;
        });
    }, [hashtags, searchQuery, categoryFilter]);

    // Group by category for display
    const groupedHashtags = useMemo(() => {
        return filteredHashtags.reduce((acc, h) => {
            const cat = h.category || 'other';
            if (!acc[cat]) acc[cat] = [];
            acc[cat].push(h);
            return acc;
        }, {});
    }, [filteredHashtags]);

    // Stats
    const stats = useMemo(() => ({
        total: hashtags.length,
        active: hashtags.filter(h => h.isActive !== false).length,
        categories: Object.keys(byCategory).length
    }), [hashtags, byCategory]);

    const handleCreate = async (data) => {
        setIsSubmitting(true);
        try {
            await createHashtag(data);
            setShowModal(false);
        } catch (err) {
            console.error('Failed to create hashtag:', err);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleUpdate = async (id, data) => {
        setIsSubmitting(true);
        try {
            await updateHashtag(id, data);
            setEditingHashtag(null);
        } catch (err) {
            console.error('Failed to update hashtag:', err);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleDelete = async (id) => {
        setIsSubmitting(true);
        try {
            await deleteHashtag(id);
            setShowDeleteConfirm(null);
        } catch (err) {
            console.error('Failed to delete hashtag:', err);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleRefresh = () => {
        fetchHashtags(true);
    };

    return (
        <div className="admin-page hashtag-management-page">
            <header className="admin-page__header">
                <div>
                    <h1 className="admin-page__title">Quản lý Hashtag</h1>
                    <p className="admin-page__subtitle">Phân loại và đánh dấu các bài thi</p>
                </div>
                <div className="hashtag-page__actions">
                    <button
                        className="admin-btn admin-btn--secondary"
                        onClick={handleRefresh}
                        disabled={isLoading}
                    >
                        <FiRefreshCw size={16} className={isLoading ? 'spinning' : ''} />
                        <span>Làm mới</span>
                    </button>
                    <button
                        className="admin-btn admin-btn--primary"
                        onClick={() => setShowModal(true)}
                    >
                        <FiPlus size={16} />
                        <span>Tạo hashtag mới</span>
                    </button>
                </div>
            </header>

            {/* Error message */}
            {error && (
                <div className="hashtag-error">
                    <p>{error}</p>
                    <button onClick={clearError}><FiX size={16} /></button>
                </div>
            )}

            {/* Stats Row */}
            <div className="hashtag-stats-row">
                <div className="hashtag-stat-card">
                    <span className="hashtag-stat-value">{stats.total}</span>
                    <span className="hashtag-stat-label">Tổng hashtag</span>
                </div>
                <div className="hashtag-stat-card">
                    <span className="hashtag-stat-value">{stats.active}</span>
                    <span className="hashtag-stat-label">Đang hoạt động</span>
                </div>
                <div className="hashtag-stat-card">
                    <span className="hashtag-stat-value">{stats.categories}</span>
                    <span className="hashtag-stat-label">Danh mục</span>
                </div>
            </div>

            {/* Toolbar */}
            <div className="hashtag-toolbar">
                <div className="hashtag-search-box">
                    <FiSearch className="hashtag-search-icon" />
                    <input
                        type="text"
                        placeholder="Tìm hashtag..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="hashtag-search-input"
                    />
                    {searchQuery && (
                        <button
                            className="hashtag-search-clear"
                            onClick={() => setSearchQuery('')}
                        >
                            <FiX size={14} />
                        </button>
                    )}
                </div>

                <div className="hashtag-filter-tabs">
                    <button
                        className={categoryFilter === 'all' ? 'active' : ''}
                        onClick={() => setCategoryFilter('all')}
                    >
                        Tất cả
                    </button>
                    {Object.entries(CATEGORIES).map(([key, { label, icon }]) => (
                        <button
                            key={key}
                            className={categoryFilter === key ? 'active' : ''}
                            onClick={() => setCategoryFilter(key)}
                        >
                            {icon} {label}
                        </button>
                    ))}
                </div>
            </div>

            {/* Hashtags by Category */}
            {isLoading ? (
                <div className="hashtag-loading">
                    <div className="spinner"></div>
                    <p>Đang tải hashtag...</p>
                </div>
            ) : (
                <div className="hashtags-container">
                    {Object.entries(groupedHashtags).map(([category, tags]) => (
                        <div key={category} className="hashtag-category-section">
                            <h2 className="hashtag-category-header">
                                <span className="hashtag-category-icon">
                                    {CATEGORIES[category]?.icon || '\u{1F3F7}'}
                                </span>
                                <span className="hashtag-category-name">
                                    {CATEGORIES[category]?.label || category}
                                </span>
                                <span className="hashtag-category-count">({tags.length})</span>
                            </h2>

                            <div className="hashtags-grid">
                                {tags.map(hashtag => (
                                    <div
                                        key={hashtag.id}
                                        className={`hashtag-card ${hashtag.isActive === false ? 'inactive' : ''}`}
                                    >
                                        <div
                                            className="hashtag-card-preview"
                                            style={{
                                                backgroundColor: hashtag.color ? `${hashtag.color}20` : '#f3f4f6',
                                                borderLeft: `4px solid ${hashtag.color || '#6b7280'}`
                                            }}
                                        >
                                            <span className="hashtag-card-icon">{hashtag.icon || '\u{1F3F7}'}</span>
                                            <div className="hashtag-card-names">
                                                <span className="hashtag-name-vi">{hashtag.nameVi}</span>
                                                {hashtag.nameEn && (
                                                    <span className="hashtag-name-en">{hashtag.nameEn}</span>
                                                )}
                                            </div>
                                        </div>

                                        <div className="hashtag-card-meta">
                                            <span className="hashtag-code">#{hashtag.code}</span>
                                            <span className="hashtag-use-count">
                                                {hashtag.useCount || 0} lần sử dụng
                                            </span>
                                        </div>

                                        <div className="hashtag-card-actions">
                                            <button
                                                onClick={() => setEditingHashtag(hashtag)}
                                                title="Chỉnh sửa"
                                                className="hashtag-action-btn"
                                            >
                                                <FiEdit2 size={14} />
                                            </button>
                                            <button
                                                onClick={() => setShowDeleteConfirm(hashtag)}
                                                title="Xóa"
                                                className="hashtag-action-btn delete"
                                            >
                                                <FiTrash2 size={14} />
                                            </button>
                                        </div>

                                        {hashtag.isActive === false && (
                                            <span className="hashtag-inactive-badge">Đã ẩn</span>
                                        )}
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}

                    {Object.keys(groupedHashtags).length === 0 && (
                        <div className="hashtag-empty-state">
                            <FiTag size={48} />
                            <p>Không tìm thấy hashtag nào</p>
                            {searchQuery && (
                                <button
                                    className="admin-btn admin-btn--secondary"
                                    onClick={() => setSearchQuery('')}
                                >
                                    Xóa bộ lọc
                                </button>
                            )}
                        </div>
                    )}
                </div>
            )}

            {/* Create/Edit Modal */}
            {(showModal || editingHashtag) && (
                <HashtagModal
                    hashtag={editingHashtag}
                    onClose={() => {
                        setShowModal(false);
                        setEditingHashtag(null);
                    }}
                    onSubmit={editingHashtag
                        ? (data) => handleUpdate(editingHashtag.id, data)
                        : handleCreate
                    }
                    isSubmitting={isSubmitting}
                />
            )}

            {/* Delete Confirmation */}
            {showDeleteConfirm && (
                <div className="hashtag-modal-overlay" onClick={() => setShowDeleteConfirm(null)}>
                    <div className="hashtag-modal hashtag-confirm-modal" onClick={e => e.stopPropagation()}>
                        <h3>Xóa hashtag</h3>
                        <p>
                            Bạn có chắc muốn ẩn hashtag "<strong>{showDeleteConfirm.nameVi}</strong>"?
                            Hashtag sẽ không hiển thị nhưng vẫn được giữ lại trong hệ thống.
                        </p>
                        <div className="hashtag-modal-actions">
                            <button
                                className="admin-btn admin-btn--secondary"
                                onClick={() => setShowDeleteConfirm(null)}
                                disabled={isSubmitting}
                            >
                                Hủy
                            </button>
                            <button
                                className="admin-btn admin-btn--danger"
                                onClick={() => handleDelete(showDeleteConfirm.id)}
                                disabled={isSubmitting}
                            >
                                {isSubmitting ? 'Đang xóa...' : 'Xóa'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

// HashtagModal component
const HashtagModal = ({ hashtag, onClose, onSubmit, isSubmitting }) => {
    const [formData, setFormData] = useState({
        code: hashtag?.code || '',
        nameVi: hashtag?.nameVi || '',
        nameEn: hashtag?.nameEn || '',
        category: hashtag?.category || 'topic',
        icon: hashtag?.icon || '\u{1F4DA}',
        color: hashtag?.color || '#6366F1',
        isActive: hashtag?.isActive !== false
    });

    const [showEmojiPicker, setShowEmojiPicker] = useState(false);
    const [errors, setErrors] = useState({});

    const validateForm = () => {
        const newErrors = {};
        if (!formData.code.trim()) {
            newErrors.code = 'Mã hashtag là bắt buộc';
        } else if (!/^[a-z0-9_]+$/.test(formData.code)) {
            newErrors.code = 'Mã chỉ cho phép chữ thường, số và dấu gạch dưới';
        }
        if (!formData.nameVi.trim()) {
            newErrors.nameVi = 'Tên tiếng Việt là bắt buộc';
        }
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (validateForm()) {
            onSubmit(formData);
        }
    };

    const handleCodeChange = (e) => {
        const value = e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '');
        setFormData({ ...formData, code: value });
    };

    return (
        <div className="hashtag-modal-overlay" onClick={onClose}>
            <div className="hashtag-modal" onClick={e => e.stopPropagation()}>
                <div className="hashtag-modal-header">
                    <h2>{hashtag ? 'Chỉnh sửa hashtag' : 'Tạo hashtag mới'}</h2>
                    <button className="hashtag-modal-close" onClick={onClose}>
                        <FiX size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="hashtag-modal-form">
                    <div className="hashtag-form-row">
                        <div className="hashtag-form-group">
                            <label>Mã (code) *</label>
                            <input
                                type="text"
                                value={formData.code}
                                onChange={handleCodeChange}
                                placeholder="environment"
                                disabled={!!hashtag} // Can't change code after creation
                                className={errors.code ? 'error' : ''}
                            />
                            {errors.code && <span className="hashtag-form-error">{errors.code}</span>}
                            <span className="hashtag-form-hint">Chỉ sử dụng chữ thường, số và dấu gạch dưới</span>
                        </div>

                        <div className="hashtag-form-group">
                            <label>Danh mục *</label>
                            <select
                                value={formData.category}
                                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                            >
                                {Object.entries(CATEGORIES).map(([key, { label }]) => (
                                    <option key={key} value={key}>{label}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className="hashtag-form-row">
                        <div className="hashtag-form-group">
                            <label>Tên tiếng Việt *</label>
                            <input
                                type="text"
                                value={formData.nameVi}
                                onChange={(e) => setFormData({ ...formData, nameVi: e.target.value })}
                                placeholder="Môi trường"
                                className={errors.nameVi ? 'error' : ''}
                            />
                            {errors.nameVi && <span className="hashtag-form-error">{errors.nameVi}</span>}
                        </div>

                        <div className="hashtag-form-group">
                            <label>Tên tiếng Anh</label>
                            <input
                                type="text"
                                value={formData.nameEn}
                                onChange={(e) => setFormData({ ...formData, nameEn: e.target.value })}
                                placeholder="Environment"
                            />
                        </div>
                    </div>

                    <div className="hashtag-form-row">
                        <div className="hashtag-form-group">
                            <label>Icon (Emoji)</label>
                            <div className="hashtag-icon-picker">
                                <button
                                    type="button"
                                    className="hashtag-icon-preview"
                                    onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                                >
                                    {formData.icon || '\u{1F3F7}'}
                                </button>
                                {showEmojiPicker && (
                                    <div className="hashtag-emoji-grid">
                                        {COMMON_EMOJIS.map((emoji, index) => (
                                            <button
                                                key={index}
                                                type="button"
                                                onClick={() => {
                                                    setFormData({ ...formData, icon: emoji });
                                                    setShowEmojiPicker(false);
                                                }}
                                            >
                                                {emoji}
                                            </button>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="hashtag-form-group">
                            <label>Màu sắc</label>
                            <div className="hashtag-color-picker">
                                <input
                                    type="color"
                                    value={formData.color}
                                    onChange={(e) => setFormData({ ...formData, color: e.target.value })}
                                    className="hashtag-color-input"
                                />
                                <input
                                    type="text"
                                    value={formData.color}
                                    onChange={(e) => setFormData({ ...formData, color: e.target.value })}
                                    placeholder="#6366F1"
                                    className="hashtag-color-text"
                                />
                            </div>
                            <div className="hashtag-color-presets">
                                {PRESET_COLORS.map(color => (
                                    <button
                                        key={color}
                                        type="button"
                                        className={`hashtag-color-preset ${formData.color === color ? 'selected' : ''}`}
                                        style={{ backgroundColor: color }}
                                        onClick={() => setFormData({ ...formData, color })}
                                    />
                                ))}
                            </div>
                        </div>
                    </div>

                    {/* Active Toggle */}
                    {hashtag && (
                        <div className="hashtag-form-group hashtag-form-toggle">
                            <label className="hashtag-toggle-label">
                                <input
                                    type="checkbox"
                                    checked={formData.isActive}
                                    onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                                />
                                <span className="hashtag-toggle-slider"></span>
                                <span className="hashtag-toggle-text">
                                    {formData.isActive ? 'Đang hoạt động' : 'Đã ẩn'}
                                </span>
                            </label>
                        </div>
                    )}

                    {/* Preview */}
                    <div className="hashtag-form-group">
                        <label>Xem trước</label>
                        <div
                            className="hashtag-preview-large"
                            style={{
                                backgroundColor: `${formData.color}20`,
                                color: formData.color,
                                border: `1px solid ${formData.color}`
                            }}
                        >
                            {formData.icon || '\u{1F3F7}'} {formData.nameVi || 'Tên hashtag'}
                        </div>
                    </div>

                    <div className="hashtag-modal-actions">
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
                            {isSubmitting ? 'Đang xử lý...' : (hashtag ? 'Cập nhật' : 'Tạo')}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default HashtagManagementPage;
