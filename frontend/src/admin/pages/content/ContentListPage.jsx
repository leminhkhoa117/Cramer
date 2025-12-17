import React, { useState } from 'react';
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
    FiTrash,
    FiMoreVertical,
    FiCheck,
    FiX,
    FiClock,
    FiUsers
} from 'react-icons/fi';
import StatusBadge from '../../components/StatusBadge';
import MetricCard from '../../components/MetricCard';
import {
    mockTopics,
    mockContentOverview,
    mockContentActivities,
    testStatuses,
    getStatusColor,
    getSkillStatusColor,
} from '../../mock/mockContent';
import './ContentListPage.css';

export default function ContentListPage() {
    const navigate = useNavigate();
    const [viewMode, setViewMode] = useState('tree'); // 'tree' | 'grid'
    const [expandedTopics, setExpandedTopics] = useState([1, 2]); // Default expand first 2
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('ALL');

    // Toggle topic expansion
    const toggleExpand = (topicId) => {
        setExpandedTopics(prev =>
            prev.includes(topicId)
                ? prev.filter(id => id !== topicId)
                : [...prev, topicId]
        );
    };

    // Filter topics/tests based on search
    const filteredTopics = mockTopics.map(topic => ({
        ...topic,
        tests: topic.tests.filter(test => {
            const matchesSearch = !searchTerm ||
                test.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                topic.displayName.toLowerCase().includes(searchTerm.toLowerCase());
            const matchesStatus = statusFilter === 'ALL' || test.status === statusFilter;
            return matchesSearch && matchesStatus;
        }),
    })).filter(topic => topic.tests.length > 0 || !searchTerm);

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
                style={{ color: colors[status] }}
                title={`${skill}: ${status}`}
            >
                {icons[skill]}
            </span>
        );
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
                    <button className="admin-btn admin-btn--primary">
                        <FiPlus size={16} />
                        <span>Thêm Topic mới</span>
                    </button>
                </div>
            </div>

            {/* Overview Stats */}
            <div className="content-stats">
                <MetricCard
                    title="Tổng số đề thi"
                    value={mockContentOverview.totalTests}
                    subtitle={`${mockContentOverview.publishedTests} đã xuất bản`}
                    icon={<FiFileText size={20} />}
                    iconColor="primary"
                />
                <MetricCard
                    title="Đang soạn"
                    value={mockContentOverview.draftTests}
                    icon={<FiEdit size={20} />}
                    iconColor="warning"
                />
                <MetricCard
                    title="Chờ duyệt"
                    value={mockContentOverview.reviewTests}
                    icon={<FiClock size={20} />}
                    iconColor="info"
                />
                <MetricCard
                    title="Lượt làm bài"
                    value={mockContentOverview.totalAttempts.toLocaleString()}
                    icon={<FiUsers size={20} />}
                    iconColor="success"
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
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
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

            {/* Content Area */}
            <div className="content-area">
                {viewMode === 'tree' ? (
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
                                            onClick={(e) => { e.stopPropagation(); alert('Thêm Test mới'); }}
                                            title="Thêm Test"
                                        >
                                            <FiPlus size={16} />
                                        </button>
                                        <button
                                            className="tree-node__action-btn"
                                            onClick={(e) => { e.stopPropagation(); }}
                                            title="Thêm tùy chọn"
                                        >
                                            <FiMoreVertical size={16} />
                                        </button>
                                    </div>
                                </div>

                                {/* Tests */}
                                {expandedTopics.includes(topic.id) && (
                                    <div className="tree-node__children">
                                        {topic.tests.map(test => (
                                            <div key={test.id} className="tree-node tree-node--test">
                                                <div className="tree-node__header tree-node__header--test">
                                                    <span className="tree-node__icon">
                                                        <FiFileText size={18} />
                                                    </span>
                                                    <div className="tree-node__info">
                                                        <span className="tree-node__name">{test.name}</span>
                                                        <div className="tree-node__skills">
                                                            <SkillIndicator skill="reading" status={test.skills.reading.status} />
                                                            <SkillIndicator skill="listening" status={test.skills.listening.status} />
                                                            <SkillIndicator skill="writing" status={test.skills.writing.status} />
                                                            <SkillIndicator skill="speaking" status={test.skills.speaking.status} />
                                                        </div>
                                                    </div>
                                                    <div className="tree-node__status">
                                                        <StatusBadge status={test.status} variant={getStatusColor(test.status)} />
                                                    </div>
                                                    <div className="tree-node__stats">
                                                        <span className="tree-node__stat">
                                                            <FiUsers size={14} />
                                                            {test.totalAttempts.toLocaleString()}
                                                        </span>
                                                    </div>
                                                    <div className="tree-node__actions">
                                                        <button
                                                            className="tree-node__action-btn"
                                                            onClick={() => navigate(`/admin/content/editor/${test.id}`)}
                                                            title="Chỉnh sửa"
                                                        >
                                                            <FiEdit size={16} />
                                                        </button>
                                                        <button
                                                            className="tree-node__action-btn"
                                                            title="Xem trước"
                                                        >
                                                            <FiEye size={16} />
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
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
                            topic.tests.map(test => (
                                <div key={test.id} className="test-card">
                                    <div className="test-card__header">
                                        <span className="test-card__topic">{topic.displayName}</span>
                                        <StatusBadge status={test.status} variant={getStatusColor(test.status)} />
                                    </div>
                                    <h3 className="test-card__title">{test.name}</h3>
                                    <div className="test-card__skills">
                                        <SkillIndicator skill="reading" status={test.skills.reading.status} />
                                        <SkillIndicator skill="listening" status={test.skills.listening.status} />
                                        <SkillIndicator skill="writing" status={test.skills.writing.status} />
                                        <SkillIndicator skill="speaking" status={test.skills.speaking.status} />
                                    </div>
                                    <div className="test-card__footer">
                                        <span className="test-card__stat">
                                            <FiUsers size={14} />
                                            {test.totalAttempts.toLocaleString()} lượt
                                        </span>
                                        <div className="test-card__actions">
                                            <button
                                                className="test-card__action-btn"
                                                onClick={() => navigate(`/admin/content/editor/${test.id}`)}
                                            >
                                                <FiEdit size={14} />
                                            </button>
                                            <button className="test-card__action-btn">
                                                <FiEye size={14} />
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                )}
            </div>

            {/* Recent Activity */}
            <div className="recent-activity">
                <h3 className="recent-activity__title">Hoạt động gần đây</h3>
                <div className="recent-activity__list">
                    {mockContentActivities.map(activity => (
                        <div key={activity.id} className="activity-item">
                            <div className={`activity-item__icon activity-item__icon--${activity.type.toLowerCase()}`}>
                                {activity.type === 'PUBLISHED' && <FiCheck size={14} />}
                                {activity.type === 'UPDATED' && <FiEdit size={14} />}
                                {activity.type === 'CREATED' && <FiPlus size={14} />}
                                {activity.type === 'REVIEW' && <FiClock size={14} />}
                            </div>
                            <div className="activity-item__content">
                                <p className="activity-item__description">{activity.description}</p>
                                <span className="activity-item__meta">
                                    {activity.adminEmail} · {formatDate(activity.createdAt)}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
