import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    FiFolder,
    FiFileText,
    FiChevronRight,
    FiChevronDown,
    FiPlus,
    FiSearch,
    FiGrid,
    FiList,
    FiEdit,
    FiEye,
    FiMoreVertical,
    FiUsers,
    FiRefreshCw,
    FiClock,
    FiX
} from 'react-icons/fi';
import StatusBadge from '../../components/StatusBadge';
import MetricCard from '../../components/MetricCard';
import useAdminContentStore from '../../stores/useAdminContentStore';
import '../../css/pages/content/ContentListPage.css';

// Test status definitions
const testStatuses = [
    { value: 'DRAFT', label: 'Nháp', color: 'neutral' },
    { value: 'PUBLISHED', label: 'Đã xuất bản', color: 'success' },
];

// Helper functions
const getStatusColor = (status) => {
    const statusObj = testStatuses.find(s => s.value === status);
    return statusObj ? statusObj.color : 'neutral';
};

export default function ContentListPage() {
    const navigate = useNavigate();

    // Store state
    const {
        topics,
        overview,
        searchQuery,
        statusFilter,
        viewMode,
        expandedTopics,
        isLoading,
        isLoadingOverview,
        error,
        fetchTopics,
        fetchOverview,
        setSearchQuery,
        setStatusFilter,
        setViewMode,
        toggleExpand,
        getFilteredTopics,
        initializeContent
    } = useAdminContentStore();

    // Local search state for debouncing
    const [localSearchTerm, setLocalSearchTerm] = useState(searchQuery);

    // Initial data fetch - uses cache if available
    useEffect(() => {
        initializeContent();
    }, [initializeContent]);

    // Debounced search
    useEffect(() => {
        const timer = setTimeout(() => {
            setSearchQuery(localSearchTerm);
            if (localSearchTerm !== searchQuery) {
                fetchTopics();
            }
        }, 300);
        return () => clearTimeout(timer);
    }, [localSearchTerm]);

    // Format date
    const formatDate = (dateString) => {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
        });
    };

    // Skill indicator component
    const SkillIndicator = ({ skill, status }) => {
        const icons = {
            reading: '📖',
            listening: '🎧',
            writing: '✍️',
            speaking: '🎤',
        };

        const colors = {
            empty: '#64748B',
            draft: '#F59E0B',
            complete: '#10B981',
        };

        return (
            <span
                className="skill-indicator"
                style={{ color: colors[status] || colors.empty }}
                title={`${skill}: ${status || 'empty'}`}
            >
                {icons[skill]}
            </span>
        );
    };

    // Handle refresh - force reload
    const handleRefresh = () => {
        fetchTopics(true);
        fetchOverview(true);
    };

    // Get filtered topics
    const filteredTopics = getFilteredTopics();

    // Modal state for adding new topic
    const [showAddTopicModal, setShowAddTopicModal] = useState(false);
    const [newTopicName, setNewTopicName] = useState('');
    const [isAddingTopic, setIsAddingTopic] = useState(false);

    // State for adding new test to topic
    const [showAddTestModal, setShowAddTestModal] = useState(false);
    const [addTestToTopic, setAddTestToTopic] = useState(null);
    const [newTestNumber, setNewTestNumber] = useState('');

    // State for topic context menu
    const [topicMenuOpen, setTopicMenuOpen] = useState(null);

    // Handle add new topic
    const handleAddTopic = async () => {
        if (!newTopicName.trim()) {
            alert('Vui lòng nhập tên Topic');
            return;
        }

        // Format topic name (lowercase, no spaces)
        const topicCode = newTopicName.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z0-9_]/g, '');

        setIsAddingTopic(true);
        try {
            // Navigate to content editor to create first test
            navigate(`/admin/content/editor/${topicCode}/1`);
        } catch (error) {
            console.error('Error adding topic:', error);
            alert('Lỗi khi thêm topic');
        } finally {
            setIsAddingTopic(false);
            setShowAddTopicModal(false);
            setNewTopicName('');
        }
    };

    // Handle add new test to existing topic
    const handleAddTestClick = (e, topic) => {
        e.stopPropagation();
        setAddTestToTopic(topic);
        // Calculate next test number
        const existingTests = topic.tests || [];
        const maxTestNumber = existingTests.reduce((max, t) => Math.max(max, t.testNumber || 0), 0);
        setNewTestNumber(String(maxTestNumber + 1));
        setShowAddTestModal(true);
    };

    const handleAddTest = () => {
        if (!addTestToTopic || !newTestNumber.trim()) {
            alert('Vui lòng nhập số Test');
            return;
        }
        // Navigate to editor for new test
        navigate(`/admin/content/editor/${addTestToTopic.name}/${newTestNumber}`);
        setShowAddTestModal(false);
        setAddTestToTopic(null);
        setNewTestNumber('');
    };

    // Handle topic menu
    const handleTopicMenuClick = (e, topicId) => {
        e.stopPropagation();
        setTopicMenuOpen(topicMenuOpen === topicId ? null : topicId);
    };

    // Close menus when clicking outside
    useEffect(() => {
        const handleClickOutside = () => setTopicMenuOpen(null);
        if (topicMenuOpen) {
            document.addEventListener('click', handleClickOutside);
            return () => document.removeEventListener('click', handleClickOutside);
        }
    }, [topicMenuOpen]);

    // Handle preview test
    const handlePreviewTest = (test) => {
        // Open test in preview mode (new tab)
        const previewUrl = `/test/${test.examSource}/${test.testNumber}/reading`;
        window.open(previewUrl, '_blank');
    };

    return (
        <div className="admin-page content-list-page">
            <div className="admin-page__header">
                <div>
                    <h1 className="admin-page__title">Quản lý Nội dung Đề thi</h1>
                    <p className="admin-page__subtitle">
                        Soạn và quản lý các bộ đề thi IELTS
                    </p>
                </div>
                <div className="content-list-page__actions">
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
                        onClick={() => setShowAddTopicModal(true)}
                    >
                        <FiPlus size={16} />
                        <span>Thêm Topic mới</span>
                    </button>
                </div>
            </div>

            {/* Overview Stats */}
            <div className="content-stats">
                <MetricCard
                    title="Tổng số đề thi"
                    value={overview.totalTests}
                    subtitle={`${overview.publishedTests} đã xuất bản`}
                    icon={<FiFileText size={20} />}
                    iconColor="primary"
                    loading={isLoadingOverview}
                />
                <MetricCard
                    title="Đang soạn"
                    value={overview.draftTests}
                    icon={<FiEdit size={20} />}
                    iconColor="warning"
                    loading={isLoadingOverview}
                />
                <MetricCard
                    title="Tổng câu hỏi"
                    value={overview.totalQuestions?.toLocaleString() || 0}
                    icon={<FiClock size={20} />}
                    iconColor="info"
                    loading={isLoadingOverview}
                />
                <MetricCard
                    title="Lượt làm bài"
                    value={overview.totalAttempts?.toLocaleString() || 0}
                    icon={<FiUsers size={20} />}
                    iconColor="success"
                    loading={isLoadingOverview}
                />
            </div>

            {/* Toolbar */}
            <div className="content-toolbar">
                <div className="content-toolbar__left">
                    <div className="search-box">
                        <FiSearch className="search-box__icon" />
                        <input
                            type="text"
                            placeholder="Tìm kiếm đề thi..."
                            value={localSearchTerm}
                            onChange={(e) => setLocalSearchTerm(e.target.value)}
                            className="search-box__input"
                        />
                    </div>
                    <select
                        className="filter-select"
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                    >
                        <option value="ALL">Tất cả trạng thái</option>
                        {testStatuses.map(status => (
                            <option key={status.value} value={status.value}>{status.label}</option>
                        ))}
                    </select>
                </div>
                <div className="content-toolbar__right">
                    <div className="view-toggle">
                        <button
                            className={`view-toggle__btn ${viewMode === 'tree' ? 'view-toggle__btn--active' : ''}`}
                            onClick={() => setViewMode('tree')}
                            title="Tree View"
                        >
                            <FiList size={18} />
                        </button>
                        <button
                            className={`view-toggle__btn ${viewMode === 'grid' ? 'view-toggle__btn--active' : ''}`}
                            onClick={() => setViewMode('grid')}
                            title="Grid View"
                        >
                            <FiGrid size={18} />
                        </button>
                    </div>
                </div>
            </div>

            {/* Error message */}
            {error && (
                <div className="content-error">
                    <p>{error}</p>
                    <button onClick={handleRefresh}>Thử lại</button>
                </div>
            )}

            {/* Content Area */}
            <div className="content-area">
                {isLoading ? (
                    <div className="content-loading">
                        <div className="spinner"></div>
                        <p>Đang tải danh sách đề thi...</p>
                    </div>
                ) : viewMode === 'tree' ? (
                    /* Tree View */
                    <div className="tree-view">
                        {filteredTopics.map(topic => (
                            <div key={topic.id} className="tree-node tree-node--topic">
                                <div
                                    className="tree-node__header"
                                    onClick={() => toggleExpand(topic.id)}
                                >
                                    <button className="tree-node__toggle">
                                        {expandedTopics.includes(topic.id) ? (
                                            <FiChevronDown size={18} />
                                        ) : (
                                            <FiChevronRight size={18} />
                                        )}
                                    </button>
                                    <span className="tree-node__icon">
                                        <FiFolder size={20} />
                                    </span>
                                    <div className="tree-node__info">
                                        <span className="tree-node__name">{topic.displayName}</span>
                                        <span className="tree-node__meta">
                                            {topic.publishedTests}/{topic.testsCount} đề đã xuất bản
                                        </span>
                                    </div>
                                    <div className="tree-node__actions">
                                        <button
                                            className="tree-node__action-btn"
                                            onClick={(e) => handleAddTestClick(e, topic)}
                                            title="Thêm Test mới"
                                        >
                                            <FiPlus size={16} />
                                        </button>
                                        <div className="action-menu-wrapper">
                                            <button
                                                className="tree-node__action-btn"
                                                onClick={(e) => handleTopicMenuClick(e, topic.id)}
                                                title="Tùy chọn"
                                            >
                                                <FiMoreVertical size={16} />
                                            </button>
                                            {topicMenuOpen === topic.id && (
                                                <div className="action-menu action-menu--topic">
                                                    <button
                                                        className="action-menu__item"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            handleAddTestClick(e, topic);
                                                            setTopicMenuOpen(null);
                                                        }}
                                                    >
                                                        <FiPlus size={14} />
                                                        <span>Thêm Test mới</span>
                                                    </button>
                                                    <button
                                                        className="action-menu__item"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            setTopicMenuOpen(null);
                                                            // Navigate to first test for editing
                                                            if (topic.tests && topic.tests.length > 0) {
                                                                navigate(`/admin/content/editor/${topic.name}/${topic.tests[0].testNumber}`);
                                                            }
                                                        }}
                                                    >
                                                        <FiEdit size={14} />
                                                        <span>Chỉnh sửa Topic</span>
                                                    </button>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>

                                {/* Tests */}
                                {expandedTopics.includes(topic.id) && (
                                    <div className="tree-node__children">
                                        {(topic.tests || []).map(test => (
                                            <div key={test.id} className="tree-node tree-node--test">
                                                <div className="tree-node__header tree-node__header--test">
                                                    <span className="tree-node__icon">
                                                        <FiFileText size={18} />
                                                    </span>
                                                    <div className="tree-node__info">
                                                        <span className="tree-node__name">{test.name}</span>
                                                        <div className="tree-node__skills">
                                                            <SkillIndicator
                                                                skill="reading"
                                                                status={test.skills?.reading?.status}
                                                            />
                                                            <SkillIndicator
                                                                skill="listening"
                                                                status={test.skills?.listening?.status}
                                                            />
                                                            <SkillIndicator
                                                                skill="writing"
                                                                status={test.skills?.writing?.status}
                                                            />
                                                            <SkillIndicator
                                                                skill="speaking"
                                                                status={test.skills?.speaking?.status}
                                                            />
                                                        </div>
                                                    </div>
                                                    <div className="tree-node__status">
                                                        <StatusBadge
                                                            status={test.status}
                                                            variant={getStatusColor(test.status)}
                                                        />
                                                    </div>
                                                    <div className="tree-node__stats">
                                                        <span className="tree-node__stat">
                                                            <FiUsers size={14} />
                                                            {(test.totalAttempts || 0).toLocaleString()}
                                                        </span>
                                                    </div>
                                                    <div className="tree-node__actions">
                                                        <button
                                                            className="tree-node__action-btn"
                                                            onClick={() => navigate(`/admin/content/editor/${test.examSource}/${test.testNumber}`)}
                                                            title="Chỉnh sửa"
                                                        >
                                                            <FiEdit size={16} />
                                                        </button>
                                                        <button
                                                            className="tree-node__action-btn"
                                                            onClick={() => handlePreviewTest(test)}
                                                            title="Xem trước"
                                                        >
                                                            <FiEye size={16} />
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                        ))}

                                        {(!topic.tests || topic.tests.length === 0) && (
                                            <div className="tree-node__empty">
                                                <span>Chưa có test nào</span>
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                        ))}

                        {filteredTopics.length === 0 && (
                            <div className="empty-state">
                                <FiFileText size={48} />
                                <p>Không tìm thấy đề thi nào</p>
                            </div>
                        )}
                    </div>
                ) : (
                    /* Grid View */
                    <div className="grid-view">
                        {filteredTopics.flatMap(topic =>
                            (topic.tests || []).map(test => (
                                <div key={test.id} className="test-card">
                                    <div className="test-card__header">
                                        <span className="test-card__topic">{topic.displayName}</span>
                                        <StatusBadge
                                            status={test.status}
                                            variant={getStatusColor(test.status)}
                                        />
                                    </div>
                                    <h3 className="test-card__title">{test.name}</h3>
                                    <div className="test-card__skills">
                                        <SkillIndicator
                                            skill="reading"
                                            status={test.skills?.reading?.status}
                                        />
                                        <SkillIndicator
                                            skill="listening"
                                            status={test.skills?.listening?.status}
                                        />
                                        <SkillIndicator
                                            skill="writing"
                                            status={test.skills?.writing?.status}
                                        />
                                        <SkillIndicator
                                            skill="speaking"
                                            status={test.skills?.speaking?.status}
                                        />
                                    </div>
                                    <div className="test-card__footer">
                                        <span className="test-card__stat">
                                            <FiUsers size={14} />
                                            {(test.totalAttempts || 0).toLocaleString()} lượt
                                        </span>
                                        <div className="test-card__actions">
                                            <button
                                                className="test-card__action-btn"
                                                onClick={() => navigate(`/admin/content/editor/${test.examSource}/${test.testNumber}`)}
                                            >
                                                <FiEdit size={14} />
                                            </button>
                                            <button
                                                className="test-card__action-btn"
                                                onClick={() => handlePreviewTest(test)}
                                            >
                                                <FiEye size={14} />
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            ))
                        )}

                        {filteredTopics.length === 0 && (
                            <div className="empty-state">
                                <FiFileText size={48} />
                                <p>Không tìm thấy đề thi nào</p>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Add Topic Modal */}
            {showAddTopicModal && (
                <div className="modal-overlay" onClick={() => setShowAddTopicModal(false)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>Thêm Topic mới</h3>
                            <button className="modal-close" onClick={() => setShowAddTopicModal(false)}>
                                <FiX size={20} />
                            </button>
                        </div>
                        <div className="modal-body">
                            <div className="form-group">
                                <label>Tên Topic (ví dụ: Cambridge IELTS 19)</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    placeholder="Nhập tên topic..."
                                    value={newTopicName}
                                    onChange={(e) => setNewTopicName(e.target.value)}
                                    onKeyPress={(e) => e.key === 'Enter' && handleAddTopic()}
                                    autoFocus
                                />
                                <p className="form-hint">
                                    Sau khi tạo, bạn sẽ được chuyển đến trang soạn Test 1
                                </p>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button
                                className="admin-btn admin-btn--secondary"
                                onClick={() => setShowAddTopicModal(false)}
                            >
                                Hủy
                            </button>
                            <button
                                className="admin-btn admin-btn--primary"
                                onClick={handleAddTopic}
                                disabled={isAddingTopic || !newTopicName.trim()}
                            >
                                {isAddingTopic ? 'Đang tạo...' : 'Tạo Topic'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Add Test Modal */}
            {showAddTestModal && (
                <div className="modal-overlay" onClick={() => setShowAddTestModal(false)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h3>Thêm Test mới</h3>
                            <button className="modal-close" onClick={() => setShowAddTestModal(false)}>
                                <FiX size={20} />
                            </button>
                        </div>
                        <div className="modal-body">
                            <div className="form-group">
                                <label>Topic</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    value={addTestToTopic?.displayName || ''}
                                    disabled
                                />
                            </div>
                            <div className="form-group">
                                <label>Số Test (ví dụ: 1, 2, 3...)</label>
                                <input
                                    type="number"
                                    className="form-input"
                                    placeholder="Nhập số test..."
                                    value={newTestNumber}
                                    onChange={(e) => setNewTestNumber(e.target.value)}
                                    onKeyPress={(e) => e.key === 'Enter' && handleAddTest()}
                                    autoFocus
                                    min="1"
                                />
                                <p className="form-hint">
                                    Sau khi tạo, bạn sẽ được chuyển đến trang soạn nội dung
                                </p>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button
                                className="admin-btn admin-btn--secondary"
                                onClick={() => setShowAddTestModal(false)}
                            >
                                Hủy
                            </button>
                            <button
                                className="admin-btn admin-btn--primary"
                                onClick={handleAddTest}
                                disabled={!newTestNumber.trim()}
                            >
                                Tạo Test
                            </button>
                        </div>
                    </div>
                </div>
            )}

        </div>
    );
}
