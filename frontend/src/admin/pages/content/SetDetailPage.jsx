import React, { useEffect, useState, useMemo, useCallback } from 'react';
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
    FiChevronDown,
    FiCheck,
    FiX,
    FiCopy,
    FiFolder,
    FiRefreshCw,
    FiArrowLeft,
    FiEye,
    FiEyeOff
} from 'react-icons/fi';
// Note: CreateTestModal is defined locally in this file
// The wizard version is at ../../components/CreateTestModal but not used here
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
    const [showEditTestModal, setShowEditTestModal] = useState(false);
    const [showPublishModal, setShowPublishModal] = useState(false);
    const [selectedTest, setSelectedTest] = useState(null);
    const [isDeleting, setIsDeleting] = useState(false);
    const [isPublishing, setIsPublishing] = useState(false);

    // Bulk selection state
    const [selectedTests, setSelectedTests] = useState(new Set());
    const [showBulkDeleteModal, setShowBulkDeleteModal] = useState(false);
    const [showBulkDifficultyModal, setShowBulkDifficultyModal] = useState(false);
    const [isBulkProcessing, setIsBulkProcessing] = useState(false);

    // Expand/Collapse state for list view
    const [expandedTests, setExpandedTests] = useState(new Set());
    const [allExpanded, setAllExpanded] = useState(true);

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

    // Handle update test
    const handleUpdateTest = async (testId, data) => {
        const { updateTest, updateTestHashtags } = useTestSetStore.getState();
        try {
            // Update basic info
            await updateTest(testId, {
                testNumber: data.testNumber,
                name: data.name,
                difficulty: data.difficulty
            });

            // Update hashtags if changed (simple approach: always update)
            if (data.hashtagIds) {
                await updateTestHashtags(testId, data.hashtagIds);
            }

            setShowEditTestModal(false);
            setSelectedTest(null);
        } catch (err) {
            console.error('Error updating test:', err);
            throw err; // Re-throw for modal to handle
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

    // Toggle single test expand/collapse
    const toggleTestExpanded = useCallback((testId) => {
        setExpandedTests(prev => {
            const next = new Set(prev);
            if (next.has(testId)) {
                next.delete(testId);
            } else {
                next.add(testId);
            }
            return next;
        });
    }, []);

    // Toggle all expand/collapse
    const toggleAllExpanded = useCallback(() => {
        if (allExpanded) {
            setExpandedTests(new Set());
            setAllExpanded(false);
        } else {
            setExpandedTests(new Set(selectedSetTests.map(t => t.id)));
            setAllExpanded(true);
        }
    }, [allExpanded, selectedSetTests]);

    // Initialize all expanded when tests load
    useEffect(() => {
        if (selectedSetTests.length > 0 && expandedTests.size === 0 && allExpanded) {
            setExpandedTests(new Set(selectedSetTests.map(t => t.id)));
        }
    }, [selectedSetTests, allExpanded]);

    // Existing test numbers for validation
    const existingTestNumbers = useMemo(() => {
        return selectedSetTests.map(t => t.testNumber);
    }, [selectedSetTests]);

    // Bulk selection handlers
    const toggleTestSelection = (testId, e) => {
        e.stopPropagation();
        setSelectedTests(prev => {
            const next = new Set(prev);
            if (next.has(testId)) {
                next.delete(testId);
            } else {
                next.add(testId);
            }
            return next;
        });
    };

    const clearSelection = () => {
        setSelectedTests(new Set());
    };

    // Bulk delete
    const handleBulkDelete = async () => {
        setIsBulkProcessing(true);
        try {
            for (const testId of selectedTests) {
                await deleteTest(testId);
            }
            setShowBulkDeleteModal(false);
            clearSelection();
        } catch (err) {
            console.error('Bulk delete error:', err);
        } finally {
            setIsBulkProcessing(false);
        }
    };

    // Bulk difficulty update
    const handleBulkDifficultyUpdate = async (difficulty) => {
        const { updateTest } = useTestSetStore.getState();
        setIsBulkProcessing(true);
        try {
            const updatePromises = Array.from(selectedTests).map(testId => {
                const test = selectedSetTests.find(t => t.id === testId);
                if (test) {
                    return updateTest(testId, { testNumber: test.testNumber, difficulty });
                }
                return Promise.resolve();
            });
            await Promise.all(updatePromises);
            setShowBulkDifficultyModal(false);
            clearSelection();
        } catch (err) {
            console.error('Bulk difficulty update error:', err);
        } finally {
            setIsBulkProcessing(false);
        }
    };

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

            {/* Bulk Action Toolbar */}
            {selectedTests.size > 0 && (
                <div className="bulk-toolbar">
                    <span className="bulk-toolbar__count">
                        Đã chọn {selectedTests.size} bài thi
                    </span>
                    <div className="bulk-toolbar__actions">
                        <button
                            className="admin-btn admin-btn--secondary"
                            onClick={() => setShowBulkDifficultyModal(true)}
                            disabled={isBulkProcessing}
                        >
                            Đổi độ khó
                        </button>
                        <button
                            className="admin-btn admin-btn--danger"
                            onClick={() => setShowBulkDeleteModal(true)}
                            disabled={isBulkProcessing}
                        >
                            <FiTrash2 size={14} />
                            Xóa đã chọn
                        </button>
                        <button
                            className="admin-btn admin-btn--secondary"
                            onClick={clearSelection}
                            disabled={isBulkProcessing}
                        >
                            <FiX size={14} />
                            Hủy chọn
                        </button>
                    </div>
                </div>
            )}

            {/* Tests List */}
            <div className="content-area">
                {isLoadingTests ? (
                    <div className="content-loading">
                        <div className="spinner"></div>
                        <p>Đang tải danh sách bài thi...</p>
                    </div>
                ) : (
                    <>
                        {/* List Header with Expand/Collapse Toggle */}
                        {selectedSetTests.length > 0 && (
                            <div className="tests-list-header">
                                <span className="tests-list-header__count">
                                    {selectedSetTests.length} bài thi
                                </span>
                                <button
                                    className="tests-list-header__toggle"
                                    onClick={toggleAllExpanded}
                                    title={allExpanded ? 'Thu gọn tất cả' : 'Mở rộng tất cả'}
                                >
                                    {allExpanded ? (
                                        <><FiChevronDown size={14} /> Thu gọn tất cả</>
                                    ) : (
                                        <><FiChevronRight size={14} /> Mở rộng tất cả</>
                                    )}
                                </button>
                            </div>
                        )}

                        {/* Tests List */}
                        <div className="tests-list">
                            {selectedSetTests.map(test => (
                                <TestListItem
                                    key={test.id}
                                    test={test}
                                    isExpanded={expandedTests.has(test.id)}
                                    isSelected={selectedTests.has(test.id)}
                                    onToggleExpand={() => toggleTestExpanded(test.id)}
                                    onToggleSelect={(e) => toggleTestSelection(test.id, e)}
                                    onEdit={() => { setSelectedTest(test); setShowEditTestModal(true); }}
                                    onDuplicate={() => handleDuplicate(test)}
                                    onPublish={() => handlePublishTest(test.id, !test.isPublished)}
                                    onDelete={() => confirmDeleteTest(test)}
                                    onClick={() => handleTestClick(test)}
                                />
                            ))}

                            {/* Add Test Row */}
                            <div
                                className="test-list-item test-list-item--add"
                                onClick={() => setShowCreateModal(true)}
                            >
                                <FiPlus size={20} />
                                <span>
                                    {selectedSetTests.length === 0
                                        ? 'Thêm bài thi đầu tiên'
                                        : 'Thêm bài thi mới'}
                                </span>
                            </div>
                        </div>
                    </>
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

            {/* Edit Test Modal */}
            {showEditTestModal && selectedTest && createPortal(
                <EditTestModal
                    setId={Number(setId)}
                    existingTestNumbers={existingTestNumbers.filter(n => n !== selectedTest.testNumber)}
                    hashtags={hashtags}
                    onClose={() => { setShowEditTestModal(false); setSelectedTest(null); }}
                    onSubmit={(data) => handleUpdateTest(selectedTest.id, data)}
                    editingTest={selectedTest}
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

            {/* Bulk Delete Modal with typed confirmation */}
            {showBulkDeleteModal && createPortal(
                <BulkDeleteModal
                    count={selectedTests.size}
                    onClose={() => setShowBulkDeleteModal(false)}
                    onConfirm={handleBulkDelete}
                    loading={isBulkProcessing}
                />,
                document.body
            )}

            {/* Bulk Difficulty Modal */}
            {showBulkDifficultyModal && createPortal(
                <BulkDifficultyModal
                    count={selectedTests.size}
                    onClose={() => setShowBulkDifficultyModal(false)}
                    onConfirm={handleBulkDifficultyUpdate}
                    loading={isBulkProcessing}
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
                // Check both lowercase and uppercase keys
                const count = skillCounts?.[skill.key] || skillCounts?.[skill.key.toUpperCase()] || 0;
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
                    </span>
                );
            })}
        </div>
    );
}

/**
 * TestListItem - Collapsible list item for a test
 */
function TestListItem({
    test,
    isExpanded,
    isSelected,
    onToggleExpand,
    onToggleSelect,
    onEdit,
    onDuplicate,
    onPublish,
    onDelete,
    onClick
}) {
    const skills = [
        { key: 'reading', label: 'Reading', shortLabel: 'R', color: '#3B82F6', partLabel: 'Passage' },
        { key: 'listening', label: 'Listening', shortLabel: 'L', color: '#10B981', partLabel: 'Part' },
        { key: 'writing', label: 'Writing', shortLabel: 'W', color: '#F59E0B', partLabel: 'Task' },
        { key: 'speaking', label: 'Speaking', shortLabel: 'S', color: '#EC4899', partLabel: 'Part' }
    ];

    const getSkillCount = (skillKey) => {
        return test.skillSectionCounts?.[skillKey] || test.skillSectionCounts?.[skillKey.toUpperCase()] || 0;
    };

    const hasAnyContent = skills.some(s => getSkillCount(s.key) > 0);

    return (
        <div className={`test-list-item ${isExpanded ? 'test-list-item--expanded' : ''} ${isSelected ? 'test-list-item--selected' : ''}`}>
            {/* Collapsed Header - Always Visible */}
            <div className="test-list-item__header">
                {/* Left Section: Expand Toggle + Checkbox + Info */}
                <div className="test-list-item__left">
                    <button
                        className="test-list-item__expand-btn"
                        onClick={(e) => { e.stopPropagation(); onToggleExpand(); }}
                        title={isExpanded ? 'Thu gọn' : 'Mở rộng'}
                    >
                        {isExpanded ? <FiChevronDown size={16} /> : <FiChevronRight size={16} />}
                    </button>
                    <input
                        type="checkbox"
                        className="test-list-item__checkbox"
                        checked={isSelected}
                        onChange={onToggleSelect}
                        onClick={(e) => e.stopPropagation()}
                    />
                    <div className="test-list-item__info" onClick={onClick}>
                        <span className="test-list-item__number">Bài {test.testNumber}</span>
                        <span className="test-list-item__name">{test.name || `Test ${test.testNumber}`}</span>
                    </div>
                </div>

                {/* Center Section: Status + Skills (always visible) */}
                <div className="test-list-item__center">
                    <span className={`status-dot ${test.isPublished ? 'status-dot--published' : 'status-dot--draft'}`}
                        title={test.isPublished ? 'Đã xuất bản' : 'Bản nháp'} />
                    <div className="skill-indicators skill-indicators--compact">
                        {skills.map(skill => {
                            const count = getSkillCount(skill.key);
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
                                    title={`${skill.label}: ${count} ${skill.partLabel.toLowerCase()}${count !== 1 ? 's' : ''}`}
                                >
                                    {skill.shortLabel}
                                </span>
                            );
                        })}
                    </div>
                </div>

                {/* Right Section: Actions */}
                <div className="test-list-item__actions" onClick={(e) => e.stopPropagation()}>
                    <button className="test-list-item__action-btn" onClick={onEdit} title="Chỉnh sửa">
                        <FiEdit2 size={14} />
                    </button>
                    <button className="test-list-item__action-btn" onClick={onDuplicate} title="Nhân bản">
                        <FiCopy size={14} />
                    </button>
                    <button className="test-list-item__action-btn" onClick={onPublish} title={test.isPublished ? 'Gỡ xuất bản' : 'Xuất bản'}>
                        {test.isPublished ? <FiEyeOff size={14} /> : <FiEye size={14} />}
                    </button>
                    <button className="test-list-item__action-btn test-list-item__action-btn--delete" onClick={onDelete} title="Xóa">
                        <FiTrash2 size={14} />
                    </button>
                </div>
            </div>

            {/* Expanded Content */}
            {isExpanded && (
                <div className="test-list-item__content">
                    {/* Sections by Skill */}
                    <div className="test-list-item__sections">
                        {skills.map(skill => {
                            const count = getSkillCount(skill.key);
                            if (count === 0) return null;
                            return (
                                <div key={skill.key} className="test-list-item__skill-section">
                                    <span
                                        className="test-list-item__skill-label"
                                        style={{ color: skill.color }}
                                    >
                                        {skill.label}
                                    </span>
                                    <span className="test-list-item__skill-count">
                                        {count} {skill.partLabel}{count !== 1 ? 's' : ''}
                                    </span>
                                </div>
                            );
                        })}
                        {!hasAnyContent && (
                            <span className="test-list-item__no-content">Chưa có nội dung</span>
                        )}
                    </div>

                    {/* Metadata Row */}
                    <div className="test-list-item__meta">
                        {/* Difficulty Badge */}
                        {test.difficulty && (
                            <span className={`difficulty-badge difficulty-badge--${test.difficulty.toLowerCase()}`}>
                                {test.difficulty}
                            </span>
                        )}

                        {/* AI Badge */}
                        {test.isAiGenerated && (
                            <span className="ai-badge">AI</span>
                        )}

                        {/* Hashtags */}
                        {test.hashtags && test.hashtags.length > 0 && (
                            <div className="test-list-item__hashtags">
                                {test.hashtags.slice(0, 5).map(tag => (
                                    <span
                                        key={tag.id}
                                        className="hashtag"
                                        style={{
                                            backgroundColor: (tag.color || '#8B5CF6') + '20',
                                            color: tag.color || '#8B5CF6'
                                        }}
                                    >
                                        {tag.icon} {tag.name}
                                    </span>
                                ))}
                                {test.hashtags.length > 5 && (
                                    <span className="hashtag hashtag--more">+{test.hashtags.length - 5}</span>
                                )}
                            </div>
                        )}
                    </div>

                    {/* AI Generation Metadata */}
                    {test.isAiGenerated && test.generationMetadata && (
                        <div className="test-list-item__ai-meta">
                            {test.generationMetadata.model && (
                                <span className="ai-meta-item ai-meta-item--model">
                                    Model: {test.generationMetadata.model.split('/').pop()}
                                </span>
                            )}
                            {test.generationMetadata.topic && (
                                <span className="ai-meta-item ai-meta-item--topic">
                                    Topic: {test.generationMetadata.topic.length > 40
                                        ? test.generationMetadata.topic.substring(0, 40) + '...'
                                        : test.generationMetadata.topic}
                                </span>
                            )}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

/**
 * CreateTestModal - Modal for creating/editing a test
 * @param {Object} editingTest - If provided, modal is in edit mode with pre-filled data
 */
function CreateTestModal({ setId, existingTestNumbers, hashtags, onClose, onSubmit, editingTest = null }) {
    const topicHashtags = hashtags.filter(h => h.category === 'topic');
    const isEditing = !!editingTest;

    const [formData, setFormData] = useState({
        testNumber: editingTest?.testNumber ?? Math.max(...existingTestNumbers, 0) + 1,
        name: editingTest?.name ?? '',
        difficulty: editingTest?.difficulty ?? 'INTERMEDIATE',
        hashtagIds: editingTest?.hashtagIds ?? []
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

// EditTestModal is the same as CreateTestModal - used for editing existing tests
const EditTestModal = CreateTestModal;

/**
 * EditSetModal - Modal for editing test set details
 * Includes cover image upload functionality
 */
function EditSetModal({ testSet, onClose }) {
    const { updateTestSet, selectedSetTests, updateTest } = useTestSetStore();
    const [formData, setFormData] = useState({
        code: testSet.code || '',
        name: testSet.name || '',
        description: testSet.description || '',
        sourceType: testSet.sourceType || 'custom',
        coverImageUrl: testSet.coverImageUrl || ''
    });
    const [batchDifficulty, setBatchDifficulty] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadError, setUploadError] = useState(null);
    const fileInputRef = React.useRef(null);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    // Handle file selection and upload
    const handleFileSelect = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // Validate file type
        if (!file.type.startsWith('image/')) {
            setUploadError('Vui lòng chọn file ảnh (jpg, png, webp)');
            return;
        }

        // Validate file size (max 5MB)
        if (file.size > 5 * 1024 * 1024) {
            setUploadError('Kích thước file tối đa là 5MB');
            return;
        }

        setUploadError(null);
        setIsUploading(true);

        try {
            // Dynamic import to avoid circular dependencies
            const { supabase } = await import('../../../api/supabaseClient');

            // Generate unique filename
            const fileExt = file.name.split('.').pop();
            const fileName = `cover_${Date.now()}.${fileExt}`;
            const filePath = `SETS/${testSet.id}/${fileName}`;

            // Upload to THUMBNAILS bucket
            const { error: uploadError } = await supabase.storage
                .from('THUMBNAILS')
                .upload(filePath, file, {
                    cacheControl: '3600',
                    upsert: true
                });

            if (uploadError) {
                console.error('Upload error:', uploadError);
                setUploadError(`Lỗi upload: ${uploadError.message}`);
                setIsUploading(false);
                return;
            }

            // Get public URL
            const { data } = supabase.storage
                .from('THUMBNAILS')
                .getPublicUrl(filePath);

            if (data?.publicUrl) {
                setFormData(prev => ({ ...prev, coverImageUrl: data.publicUrl }));
            }
        } catch (err) {
            console.error('Upload failed:', err);
            setUploadError('Không thể upload ảnh. Vui lòng thử lại.');
        } finally {
            setIsUploading(false);
        }
    };

    // Clear cover image
    const handleClearImage = () => {
        setFormData(prev => ({ ...prev, coverImageUrl: '' }));
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        try {
            // Update test set metadata
            await updateTestSet(testSet.id, formData);

            // Apply batch difficulty if selected
            if (batchDifficulty) {
                const updatePromises = selectedSetTests.map(test =>
                    updateTest(test.id, {
                        testNumber: test.testNumber,
                        difficulty: batchDifficulty
                    })
                );
                await Promise.all(updatePromises);
            }

            onClose();
        } catch (err) {
            console.error('Error updating test set:', err);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="admin-modal-overlay-custom" onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="admin-edit-modal admin-edit-modal--wide" onClick={e => e.stopPropagation()}>
                <div className="admin-edit-modal-header">
                    <h2>Chỉnh sửa bộ đề</h2>
                    <button className="admin-edit-modal-close" onClick={onClose} disabled={isSubmitting}>
                        <FiX size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    <div className="admin-edit-modal-body">
                        {/* Cover Image Section */}
                        <div className="form-group">
                            <label>Ảnh bìa (382x180 px)</label>
                            <div className="cover-image-upload">
                                {/* Preview */}
                                <div className="cover-image-preview">
                                    {formData.coverImageUrl ? (
                                        <img
                                            src={formData.coverImageUrl}
                                            alt="Cover preview"
                                            onError={(e) => {
                                                e.target.style.display = 'none';
                                                e.target.nextSibling.style.display = 'flex';
                                            }}
                                        />
                                    ) : null}
                                    <div
                                        className="cover-image-placeholder"
                                        style={{ display: formData.coverImageUrl ? 'none' : 'flex' }}
                                    >
                                        <FiPlus size={24} />
                                        <span>382 x 180</span>
                                    </div>
                                </div>

                                {/* Upload Controls */}
                                <div className="cover-image-controls">
                                    <input
                                        ref={fileInputRef}
                                        type="file"
                                        accept="image/*"
                                        onChange={handleFileSelect}
                                        disabled={isUploading || isSubmitting}
                                        style={{ display: 'none' }}
                                        id="cover-image-input"
                                    />
                                    <button
                                        type="button"
                                        className="admin-btn admin-btn--secondary admin-btn--sm"
                                        onClick={() => fileInputRef.current?.click()}
                                        disabled={isUploading || isSubmitting}
                                    >
                                        {isUploading ? 'Đang tải...' : 'Tải ảnh lên'}
                                    </button>
                                    {formData.coverImageUrl && (
                                        <button
                                            type="button"
                                            className="admin-btn admin-btn--danger admin-btn--sm"
                                            onClick={handleClearImage}
                                            disabled={isUploading || isSubmitting}
                                        >
                                            Xóa ảnh
                                        </button>
                                    )}
                                </div>

                                {/* URL Input */}
                                <div className="cover-image-url">
                                    <input
                                        type="url"
                                        name="coverImageUrl"
                                        className="form-input form-input--sm"
                                        placeholder="Hoặc nhập URL ảnh..."
                                        value={formData.coverImageUrl}
                                        onChange={handleChange}
                                        disabled={isUploading || isSubmitting}
                                    />
                                </div>

                                {/* Error */}
                                {uploadError && (
                                    <span className="form-error">{uploadError}</span>
                                )}
                            </div>
                        </div>

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

                        {selectedSetTests.length > 0 && (
                            <div className="form-group" style={{ marginTop: '16px', paddingTop: '16px', borderTop: '1px solid var(--admin-border-primary)' }}>
                                <label htmlFor="edit-batchDifficulty">Đổi độ khó tất cả bài thi</label>
                                <select
                                    id="edit-batchDifficulty"
                                    className="form-select"
                                    value={batchDifficulty}
                                    onChange={(e) => setBatchDifficulty(e.target.value)}
                                    disabled={isSubmitting}
                                >
                                    <option value="">-- Không thay đổi --</option>
                                    <option value="BEGINNER">Beginner (Cơ bản)</option>
                                    <option value="INTERMEDIATE">Intermediate (Trung bình)</option>
                                    <option value="ADVANCED">Advanced (Nâng cao)</option>
                                </select>
                                <p style={{ fontSize: '0.75rem', color: 'var(--admin-text-muted)', marginTop: '4px' }}>
                                    Áp dụng cho tất cả {selectedSetTests.length} bài thi
                                </p>
                            </div>
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
                            disabled={isSubmitting || isUploading}
                        >
                            {isSubmitting ? 'Đang lưu...' : 'Lưu thay đổi'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

/**
 * BulkDeleteModal - Modal with typed confirmation for bulk deletion
 */
function BulkDeleteModal({ count, onClose, onConfirm, loading }) {
    const [confirmText, setConfirmText] = useState('');
    const CONFIRM_PHRASE = 'I CONFIRM THE DELETION';
    const isConfirmValid = confirmText === CONFIRM_PHRASE;

    return (
        <div className="admin-modal-overlay-custom" onClick={(e) => e.target === e.currentTarget && !loading && onClose()}>
            <div className="admin-edit-modal bulk-delete-modal" onClick={e => e.stopPropagation()}>
                <div className="admin-edit-modal-header admin-edit-modal-header--danger">
                    <h2>⚠️ Xóa {count} bài thi</h2>
                    <button className="admin-edit-modal-close" onClick={onClose} disabled={loading}>
                        <FiX size={20} />
                    </button>
                </div>

                <div className="admin-edit-modal-body">
                    <p className="bulk-delete-warning">
                        Hành động này <strong>không thể hoàn tác</strong>. Tất cả {count} bài thi đã chọn
                        và dữ liệu liên quan sẽ bị xóa vĩnh viễn.
                    </p>

                    <div className="form-group">
                        <label>Nhập "<strong>{CONFIRM_PHRASE}</strong>" để xác nhận:</label>
                        <input
                            type="text"
                            className={`form-input ${confirmText && !isConfirmValid ? 'form-input--error' : ''}`}
                            value={confirmText}
                            onChange={(e) => setConfirmText(e.target.value)}
                            placeholder="I CONFIRM THE DELETION"
                            disabled={loading}
                            autoFocus
                        />
                    </div>
                </div>

                <div className="admin-edit-modal-footer">
                    <button
                        className="admin-btn admin-btn--secondary"
                        onClick={onClose}
                        disabled={loading}
                    >
                        Hủy
                    </button>
                    <button
                        className="admin-btn admin-btn--danger"
                        onClick={onConfirm}
                        disabled={loading || !isConfirmValid}
                    >
                        {loading ? (
                            <>
                                <span className="spinner small"></span>
                                Đang xóa...
                            </>
                        ) : (
                            <>
                                <FiTrash2 size={14} />
                                Xóa {count} bài thi
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}

/**
 * BulkDifficultyModal - Modal for updating difficulty of multiple tests
 */
function BulkDifficultyModal({ count, onClose, onConfirm, loading }) {
    const [difficulty, setDifficulty] = useState('INTERMEDIATE');

    return (
        <div className="admin-modal-overlay-custom" onClick={(e) => e.target === e.currentTarget && !loading && onClose()}>
            <div className="admin-edit-modal" onClick={e => e.stopPropagation()}>
                <div className="admin-edit-modal-header">
                    <h2>Đổi độ khó ({count} bài thi)</h2>
                    <button className="admin-edit-modal-close" onClick={onClose} disabled={loading}>
                        <FiX size={20} />
                    </button>
                </div>

                <div className="admin-edit-modal-body">
                    <div className="form-group">
                        <label htmlFor="bulk-difficulty">Độ khó mới</label>
                        <select
                            id="bulk-difficulty"
                            className="form-select"
                            value={difficulty}
                            onChange={(e) => setDifficulty(e.target.value)}
                            disabled={loading}
                        >
                            <option value="BEGINNER">Cơ bản (Beginner)</option>
                            <option value="LOWER_INTERMEDIATE">Thấp-Trung bình (Lower-Intermediate)</option>
                            <option value="INTERMEDIATE">Trung bình (Intermediate)</option>
                            <option value="UPPER_INTERMEDIATE">Cao-Trung bình (Upper-Intermediate)</option>
                            <option value="ADVANCED">Nâng cao (Advanced)</option>
                        </select>
                    </div>
                </div>

                <div className="admin-edit-modal-footer">
                    <button
                        className="admin-btn admin-btn--secondary"
                        onClick={onClose}
                        disabled={loading}
                    >
                        Hủy
                    </button>
                    <button
                        className="admin-btn admin-btn--primary"
                        onClick={() => onConfirm(difficulty)}
                        disabled={loading}
                    >
                        {loading ? 'Đang cập nhật...' : `Cập nhật ${count} bài thi`}
                    </button>
                </div>
            </div>
        </div>
    );
}
