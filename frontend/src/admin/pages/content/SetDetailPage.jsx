import { useEffect, useState, useMemo, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { useParams, useNavigate, Link, useSearchParams } from 'react-router-dom';
import useTestSetStore from '../../stores/useTestSetStore';
import useHashtagStore from '../../stores/useHashtagStore';
import { DeleteConfirmModal } from '../../components/ConfirmModal/ConfirmModal';
import {
    FiPlus,
    FiEdit2,
    FiTrash2,
    FiChevronRight,
    FiChevronDown,
    FiX,
    FiFolder,
    FiRefreshCw,
    FiArrowLeft,
    FiEye,
    FiEyeOff
} from 'react-icons/fi';
import {
    BulkDeleteModal,
    BulkDifficultyModal,
    CreateTestModal,
    EditSetModal,
    EditTestModal,
    TestListItem
} from '../../components/content/set-detail';
import '../../css/pages/content/SetDetailPage.css';
import '../../css/pages/content/SetDetailPageList.css';

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

            // Update hashtags - always update with the current selection (could be empty array)
            // data.hashtagCodes contains the list of hashtag codes from the form
            if (data.hashtagCodes !== undefined) {
                await updateTestHashtags(testId, data.hashtagCodes);
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
