import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    FiEdit,
    FiSearch,
    FiPlus,
    FiFolder,
    FiFileText,
    FiCheck,
    FiClock,
    FiAlertCircle,
    FiRefreshCw,
    FiZap,
    FiChevronRight,
    FiChevronDown,
    FiTrash2,
    FiEdit3,
    FiTag,
    FiMoreVertical
} from 'react-icons/fi';
import StatusBadge from '../../components/StatusBadge';
import CreateTestModal from '../../components/CreateTestModal';
import CreateTestSetModal from '../../components/CreateTestSetModal';
import { useToast } from '../../components/Toast';
import useAdminContentStore from '../../stores/useAdminContentStore';
import { DeleteConfirmModal } from '../../components/ConfirmModal/ConfirmModal';
import '../../css/pages/content/TestEditorSelectPage.css';

export default function TestEditorSelectPage() {
    const navigate = useNavigate();
    const toast = useToast();
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [showCreateTestModal, setShowCreateTestModal] = useState(false);
    const [showCreateSetModal, setShowCreateSetModal] = useState(false);
    const [editingSet, setEditingSet] = useState(null);
    const [editingTest, setEditingTest] = useState(null);

    // Store state
    const {
        getFilteredTopics,
        topics,
        isLoading,
        error,
        fetchTopics,
        createTest,
        updateTest, // Ensure this is available in store
        deleteTest,
        deleteTestSet,
        expandedTopics,
        toggleExpand,
        expandAll,
        collapseAll,
        overview,
        fetchOverview
    } = useAdminContentStore();

    const [selectedItem, setSelectedItem] = useState(null);
    const [deleteConfig, setDeleteConfig] = useState({ type: null, item: null });
    const [showDeleteModal, setShowDeleteModal] = useState(false);

    // Fetch data on mount
    useEffect(() => {
        fetchTopics();
        fetchOverview();
    }, []);

    // Get filtered data
    const filteredTopics = getFilteredTopics();

    // Handle Create Set
    const handleCreateSet = () => {
        setEditingSet(null);
        setShowCreateSetModal(true);
    };

    // Handle Edit Set
    const handleEditSet = (set, e) => {
        e.stopPropagation();
        setEditingSet(set);
        setShowCreateSetModal(true);
    };

    // Handle Edit Test
    const handleEditTest = (test, e) => {
        e.stopPropagation();
        setEditingTest(test); // Set the test being edited
        setShowCreateTestModal(true);
    };

    // Handle Delete Set
    const handleDeleteSet = (set, e) => {
        e.stopPropagation();
        setDeleteConfig({ type: 'set', item: set });
        setShowDeleteModal(true);
    };

    // Handle Delete Test
    const handleDeleteTest = (test, e) => {
        e.stopPropagation();
        setDeleteConfig({ type: 'test', item: test });
        setShowDeleteModal(true);
    };

    // Perform actual deletion
    const onConfirmDelete = async () => {
        const { type, item } = deleteConfig;
        setShowDeleteModal(false); // Close modal immediately

        try {
            if (type === 'set') {
                await deleteTestSet(item.id);
                toast.success(`Đã xóa bộ đề "${item.name || item.code}"`);
            } else {
                await deleteTest(item.id);
                toast.success(`Đã xóa đề thi "${item.name || 'Test ' + item.testNumber}"`);
            }
            // Force refresh topics list
            await fetchTopics(true);
        } catch (err) {
            console.error('Delete error:', err);
            toast.error(err.message || 'Lỗi khi xóa');
        }
    };

    // Handle create/update test
    const handleTestSubmit = async (data) => {
        console.log('handleTestSubmit called with:', data);
        try {
            if (editingTest || data.id) {
                // Update existing test - data comes from modal with name, difficulty, hashtagIds
                const testId = data.id || editingTest.id;
                const updatePayload = {
                    name: data.name || data.testName, // Support both field names
                    testNumber: data.testNumber,
                    difficulty: data.difficulty,
                    hashtagIds: data.hashtagIds || []
                };
                console.log('Updating test:', testId, updatePayload);

                const result = await updateTest(testId, updatePayload);
                // updateTest returns the updated test object directly, not {success: true}
                if (result) {
                    toast.success("Đã cập nhật bài thi!");
                    setShowCreateTestModal(false);
                    setEditingTest(null);
                    // Refresh data
                    await fetchTopics(true);
                }
            } else {
                // Create new test
                const result = await createTest(data);
                // createTest may return different response format
                if (result) {
                    toast.success(`Đã tạo đề thi thành công!`);
                    setShowCreateTestModal(false);
                    // The new system uses ID-based routing for editor
                    if (result.testId || result.id) {
                        navigate(`/admin/content/editor/${result.testId || result.id}`);
                    }
                }
            }
        } catch (err) {
            console.error('handleTestSubmit error:', err);
            toast.error(err.message || "Lỗi khi xử lý đề thi");
        }
    };

    const handleRefresh = () => {
        fetchTopics(true);
        fetchOverview(true);
    };

    return (
        <div className="admin-page content-hub-page">
            <div className="admin-page__header">
                <div>
                    <h1 className="admin-page__title">Quản lý nội dung</h1>
                    <p className="admin-page__subtitle">
                        Tổ chức bộ đề và đề thi một cách hiệu quả
                    </p>
                </div>
                <div className="header-actions">
                    <button
                        className="admin-btn admin-btn--secondary"
                        onClick={handleRefresh}
                        disabled={isLoading}
                    >
                        <FiRefreshCw size={16} className={isLoading ? 'spinning' : ''} />
                    </button>
                    <button
                        className="admin-btn admin-btn--outline"
                        onClick={() => navigate('/admin/content/hashtags')}
                    >
                        <FiTag size={16} />
                        <span>Hashtags</span>
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
                        onClick={handleCreateSet}
                    >
                        <FiPlus size={16} />
                        <span>Tạo Bộ đề mới</span>
                    </button>
                </div>
            </div>

            {/* Hub Stats Cards */}
            <div className="hub-stats">
                <div className="hub-stat-card">
                    <FiFolder className="hub-stat-card__icon" />
                    <div className="hub-stat-card__info">
                        <span className="hub-stat-card__label">Tổng Bộ đề</span>
                        <span className="hub-stat-card__value">{overview.totalTopics || 0}</span>
                    </div>
                </div>
                <div className="hub-stat-card">
                    <FiFileText className="hub-stat-card__icon" />
                    <div className="hub-stat-card__info">
                        <span className="hub-stat-card__label">Tổng Đề thi</span>
                        <span className="hub-stat-card__value">{overview.totalTests || 0}</span>
                    </div>
                </div>
                <div className="hub-stat-card">
                    <FiCheck className="hub-stat-card__icon" />
                    <div className="hub-stat-card__info">
                        <span className="hub-stat-card__label">Đã xuất bản</span>
                        <span className="hub-stat-card__value">{overview.publishedTests || 0}</span>
                    </div>
                </div>
                <div className="hub-stat-card">
                    <FiEdit className="hub-stat-card__icon" />
                    <div className="hub-stat-card__info">
                        <span className="hub-stat-card__label">Đang soạn</span>
                        <span className="hub-stat-card__value">{overview.draftTests || 0}</span>
                    </div>
                </div>
            </div>

            {/* Main Hub Content */}
            <div className="hub-content">
                <div className="hub-toolbar">
                    <div className="search-bar">
                        <FiSearch />
                        <input
                            type="text"
                            placeholder="Tìm kiếm bộ đề, đề thi..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <div className="view-actions">
                        <button className="text-btn" onClick={expandAll}>Mở rộng hết</button>
                        <button className="text-btn" onClick={collapseAll}>Thu gọn hết</button>
                    </div>
                </div>

                <div className="hub-list">
                    {isLoading && topics.length === 0 ? (
                        <div className="hub-loading">
                            <div className="spinner"></div>
                            <p>Đang chuẩn bị dữ liệu...</p>
                        </div>
                    ) : (
                        <>
                            {filteredTopics.map(topic => (
                                <div key={topic.id} className={`hub-folder ${expandedTopics.includes(topic.id) ? 'is-expanded' : ''}`}>
                                    <div className="hub-folder__header" onClick={() => toggleExpand(topic.id)}>
                                        <div className="hub-folder__info">
                                            {expandedTopics.includes(topic.id) ? <FiChevronDown /> : <FiChevronRight />}
                                            <FiFolder className="folder-icon" />
                                            <div className="folder-text">
                                                <span className="folder-name">{topic.name || topic.code}</span>
                                                <span className="folder-meta">{topic.testsCount} bài thi • {topic.code}</span>
                                            </div>
                                        </div>
                                        <div className="hub-folder__actions">
                                            <button
                                                className="icon-btn"
                                                title="Sửa bộ đề"
                                                onClick={(e) => handleEditSet(topic, e)}
                                            >
                                                <FiEdit3 size={16} />
                                            </button>
                                            <button
                                                className="icon-btn"
                                                title="Thêm bài thi vào bộ này"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    // Add this logic
                                                    toast.info("Tính năng đang hoàn thiện: Thêm trực tiếp vào folder");
                                                }}
                                            >
                                                <FiPlus size={16} />
                                            </button>
                                            <button
                                                className="icon-btn icon-btn--danger"
                                                onClick={(e) => handleDeleteSet(topic, e)}
                                            >
                                                <FiTrash2 size={16} />
                                            </button>
                                        </div>
                                    </div>

                                    {expandedTopics.includes(topic.id) && (
                                        <div className="hub-folder__content">
                                            {topic.tests?.length > 0 ? (
                                                <div className="test-items">
                                                    {topic.tests.map(test => (
                                                        <div
                                                            key={test.id}
                                                            className="test-item"
                                                            onClick={() => navigate(`/admin/content/editor/${test.id}`)}
                                                        >
                                                            <div className="test-item__info">
                                                                <FiFileText className="test-icon" />
                                                                <div className="test-text">
                                                                    <span className="test-name">{test.name || `Bài thi ${test.testNumber}`}</span>
                                                                    <div className="test-labels">
                                                                        <StatusBadge
                                                                            status={test.isPublished ? 'PUBLISHED' : 'DRAFT'}
                                                                            size="sm"
                                                                        />
                                                                        {test.difficulty && (
                                                                            <span className="difficulty-tag">{test.difficulty}</span>
                                                                        )}
                                                                        {test.hashtags?.map(h => (
                                                                            <span key={h.id} className="h-tag" style={{ borderLeftColor: h.color }}>
                                                                                {h.name}
                                                                            </span>
                                                                        ))}
                                                                    </div>
                                                                </div>
                                                            </div>

                                                            <div className="test-item__stats">
                                                                <div className="skill-dots">
                                                                    <span title="Reading" className={`dot ${test.skills?.reading >= 3 ? 'is-done' : test.skills?.reading > 0 ? 'is-draft' : ''}`}>R</span>
                                                                    <span title="Listening" className={`dot ${test.skills?.listening >= 4 ? 'is-done' : test.skills?.listening > 0 ? 'is-draft' : ''}`}>L</span>
                                                                    <span title="Writing" className={`dot ${test.skills?.writing >= 2 ? 'is-done' : test.skills?.writing > 0 ? 'is-draft' : ''}`}>W</span>
                                                                    <span title="Speaking" className={`dot ${test.skills?.speaking >= 3 ? 'is-done' : test.skills?.speaking > 0 ? 'is-draft' : ''}`}>S</span>
                                                                </div>
                                                                <div className="test-item__actions">
                                                                    <button
                                                                        className="icon-btn"
                                                                        onClick={(e) => handleEditTest(test, e)}
                                                                        title="Sửa thông tin đề thi"
                                                                    >
                                                                        <FiEdit3 size={14} />
                                                                    </button>

                                                                    <button
                                                                        className="icon-btn"
                                                                        onClick={(e) => handleDeleteTest(test, e)}
                                                                    >
                                                                        <FiTrash2 size={14} />
                                                                    </button>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            ) : (
                                                <div className="folder-empty">
                                                    <p>Bộ đề này chưa có bài thi nào.</p>
                                                    <button
                                                        className="text-btn"
                                                        onClick={() => {
                                                            setEditingTest(null);
                                                            setShowCreateTestModal(true);
                                                        }}
                                                    >
                                                        Tạo đề thi ngay
                                                    </button>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>
                            ))}
                        </>
                    )}

                    {filteredTopics.length === 0 && !isLoading && (
                        <div className="hub-empty">
                            <FiAlertCircle size={48} />
                            <h3>Không tìm thấy nội dung</h3>
                            <p>Thử tìm kiếm với từ khóa khác hoặc tạo nội dung mới.</p>
                        </div>
                    )}
                </div>
            </div>

            {/* Modals */}
            <CreateTestModal
                isOpen={showCreateTestModal}
                onClose={() => {
                    setShowCreateTestModal(false);
                    setEditingTest(null);
                }}
                onSubmit={handleTestSubmit}
                topics={topics}
                testToEdit={editingTest}
            />

            <CreateTestSetModal
                isOpen={showCreateSetModal}
                onClose={() => setShowCreateSetModal(false)}
                testSet={editingSet}
            />

            <DeleteConfirmModal
                isOpen={showDeleteModal}
                onClose={() => setShowDeleteModal(false)}
                onConfirm={onConfirmDelete}
                itemName={deleteConfig.item ? (deleteConfig.type === 'set' ? (deleteConfig.item.name || deleteConfig.item.code) : (deleteConfig.item.name || 'Test ' + deleteConfig.item.testNumber)) : ''}
                title={deleteConfig.type === 'set' ? 'Xóa Bộ đề' : 'Xóa Đề thi'}
            />
        </div>
    );
}
