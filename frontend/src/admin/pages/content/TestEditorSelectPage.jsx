import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    FiEdit,
    FiSearch,
    FiPlus,
    FiFileText,
    FiCheck,
    FiClock,
    FiAlertCircle,
    FiRefreshCw,
    FiZap
} from 'react-icons/fi';
import StatusBadge from '../../components/StatusBadge';
import CreateTestModal from '../../components/CreateTestModal';
import { useToast } from '../../components/Toast';
import useAdminContentStore from '../../stores/useAdminContentStore';
import '../../css/pages/content/TestEditorSelectPage.css';

// Test status configurations
const testStatuses = [
    { value: 'DRAFT', label: 'Nháp', color: 'neutral' },
    { value: 'REVIEW', label: 'Đang duyệt', color: 'warning' },
    { value: 'PUBLISHED', label: 'Đã xuất bản', color: 'success' },
    { value: 'ARCHIVED', label: 'Lưu trữ', color: 'info' },
];

const getStatusColor = (status) => {
    const statusObj = testStatuses.find(s => s.value === status);
    return statusObj ? statusObj.color : 'neutral';
};

export default function TestEditorSelectPage() {
    const navigate = useNavigate();
    const toast = useToast();
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [showCreateModal, setShowCreateModal] = useState(false);

    // Store state
    const {
        topics,
        isLoading,
        error,
        fetchTopics,
        getAllTests,
        createTest
    } = useAdminContentStore();

    // Fetch data on mount
    useEffect(() => {
        fetchTopics();
    }, []);

    // Get all tests
    const allTests = getAllTests();

    // Filter tests
    const filteredTests = allTests.filter(test => {
        const matchesSearch = !searchTerm ||
            test.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
            (test.topicName && test.topicName.toLowerCase().includes(searchTerm.toLowerCase()));
        const matchesStatus = statusFilter === 'ALL' || test.status === statusFilter;
        return matchesSearch && matchesStatus;
    });

    // Group by status for quick stats
    const draftTests = allTests.filter(t => t.status === 'DRAFT');
    const reviewTests = allTests.filter(t => t.status === 'REVIEW');
    const publishedTests = allTests.filter(t => t.status === 'PUBLISHED');

    // Get skill completion status
    const getSkillStatus = (skills) => {
        if (!skills) return { complete: 0, draft: 0, empty: 4 };
        const statuses = [
            skills.reading?.status,
            skills.listening?.status,
            skills.writing?.status,
            skills.speaking?.status
        ];
        const complete = statuses.filter(s => s === 'complete').length;
        const draft = statuses.filter(s => s === 'draft').length;
        return { complete, draft, empty: 4 - complete - draft };
    };

    // Handle create test
    const handleCreateTest = async (data) => {
        try {
            const result = await createTest(data);
            if (result && result.success) {
                toast.success(`Đã tạo đề thi "${data.testName}" thành công!`);
                setShowCreateModal(false);
                navigate(`/admin/content/editor/${result.examSource}/${result.testNumber}`);
            }
        } catch (err) {
            toast.error(err.message || "Lỗi khi tạo đề thi");
        }
    };

    // Handle refresh
    const handleRefresh = () => {
        fetchTopics();
    };

    return (
        <div className="admin-page test-editor-select-page">
            <div className="admin-page__header">
                <div>
                    <h1 className="admin-page__title">Soạn đề thi</h1>
                    <p className="admin-page__subtitle">
                        Chọn một đề thi để bắt đầu soạn hoặc chỉnh sửa
                    </p>
                </div>
                <div className="header-actions">
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
                        <span>Tạo đề thi mới</span>
                    </button>
                </div>
            </div>

            {/* Create Test Modal */}
            <CreateTestModal
                isOpen={showCreateModal}
                onClose={() => setShowCreateModal(false)}
                onSubmit={handleCreateTest}
                topics={topics}
            />

            {/* Quick Stats */}
            <div className="editor-quick-stats">
                <div className="editor-quick-stat editor-quick-stat--draft">
                    <FiEdit size={20} />
                    <div className="editor-quick-stat__content">
                        <span className="editor-quick-stat__value">{draftTests.length}</span>
                        <span className="editor-quick-stat__label">Đang soạn</span>
                    </div>
                </div>
                <div className="editor-quick-stat editor-quick-stat--review">
                    <FiClock size={20} />
                    <div className="editor-quick-stat__content">
                        <span className="editor-quick-stat__value">{reviewTests.length}</span>
                        <span className="editor-quick-stat__label">Chờ duyệt</span>
                    </div>
                </div>
                <div className="editor-quick-stat editor-quick-stat--published">
                    <FiCheck size={20} />
                    <div className="editor-quick-stat__content">
                        <span className="editor-quick-stat__value">{publishedTests.length}</span>
                        <span className="editor-quick-stat__label">Đã xuất bản</span>
                    </div>
                </div>
            </div>

            {/* Filters */}
            <div className="editor-filters">
                <div className="search-box">
                    <FiSearch className="search-box__icon" />
                    <input
                        type="text"
                        placeholder="Tìm kiếm đề thi..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="search-box__input"
                    />
                </div>
                <div className="filter-tabs">
                    <button
                        className={`filter-tab ${statusFilter === 'ALL' ? 'filter-tab--active' : ''}`}
                        onClick={() => setStatusFilter('ALL')}
                    >
                        Tất cả ({allTests.length})
                    </button>
                    <button
                        className={`filter-tab ${statusFilter === 'DRAFT' ? 'filter-tab--active' : ''}`}
                        onClick={() => setStatusFilter('DRAFT')}
                    >
                        Đang soạn ({draftTests.length})
                    </button>
                    <button
                        className={`filter-tab ${statusFilter === 'REVIEW' ? 'filter-tab--active' : ''}`}
                        onClick={() => setStatusFilter('REVIEW')}
                    >
                        Chờ duyệt ({reviewTests.length})
                    </button>
                    <button
                        className={`filter-tab ${statusFilter === 'PUBLISHED' ? 'filter-tab--active' : ''}`}
                        onClick={() => setStatusFilter('PUBLISHED')}
                    >
                        Đã xuất bản ({publishedTests.length})
                    </button>
                </div>
            </div>

            {/* Error message */}
            {error && (
                <div className="content-error">
                    <p>{error}</p>
                    <button onClick={handleRefresh}>Thử lại</button>
                </div>
            )}

            {/* Loading State */}
            {isLoading ? (
                <div className="content-loading">
                    <div className="spinner"></div>
                    <p>Đang tải danh sách đề thi...</p>
                </div>
            ) : (
                /* Test Cards Grid */
                <div className="editor-test-grid">
                    {filteredTests.map(test => {
                        const skillStatus = getSkillStatus(test.skills);
                        return (
                            <div
                                key={test.id}
                                className="editor-test-card"
                                onClick={() => navigate(`/admin/content/editor/${test.examSource}/${test.testNumber}`)}
                            >
                                <div className="editor-test-card__header">
                                    <span className="editor-test-card__topic">{test.topicName}</span>
                                    <StatusBadge status={test.status} variant={getStatusColor(test.status)} />
                                </div>
                                <h3 className="editor-test-card__title">
                                    <FiFileText size={18} />
                                    {test.name}
                                </h3>

                                {/* Skill Progress */}
                                <div className="editor-test-card__skills">
                                    <div className="skill-progress">
                                        <div className="skill-progress__bar">
                                            <div
                                                className="skill-progress__fill skill-progress__fill--complete"
                                                style={{ width: `${(skillStatus.complete / 4) * 100}%` }}
                                            />
                                            <div
                                                className="skill-progress__fill skill-progress__fill--draft"
                                                style={{ width: `${(skillStatus.draft / 4) * 100}%` }}
                                            />
                                        </div>
                                        <span className="skill-progress__text">
                                            {skillStatus.complete}/4 skills hoàn thành
                                        </span>
                                    </div>
                                </div>

                                {/* Skill Icons */}
                                <div className="editor-test-card__skill-icons">
                                    <span
                                        className={`skill-icon skill-icon--${test.skills?.reading?.status || 'empty'}`}
                                        title={`Reading: ${test.skills?.reading?.status || 'empty'}`}
                                    >
                                        📖
                                    </span>
                                    <span
                                        className={`skill-icon skill-icon--${test.skills?.listening?.status || 'empty'}`}
                                        title={`Listening: ${test.skills?.listening?.status || 'empty'}`}
                                    >
                                        🎧
                                    </span>
                                    <span
                                        className={`skill-icon skill-icon--${test.skills?.writing?.status || 'empty'}`}
                                        title={`Writing: ${test.skills?.writing?.status || 'empty'}`}
                                    >
                                        ✍️
                                    </span>
                                    <span
                                        className={`skill-icon skill-icon--${test.skills?.speaking?.status || 'empty'}`}
                                        title={`Speaking: ${test.skills?.speaking?.status || 'empty'}`}
                                    >
                                        🎤
                                    </span>
                                </div>

                                <div className="editor-test-card__footer">
                                    <button className="editor-test-card__btn">
                                        <FiEdit size={14} />
                                        Mở Editor
                                    </button>
                                </div>
                            </div>
                        );
                    })}

                    {filteredTests.length === 0 && !isLoading && (
                        <div className="empty-state">
                            <FiAlertCircle size={48} />
                            <p>Không tìm thấy đề thi nào</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
