import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    FiUser,
    FiMail,
    FiCalendar,
    FiMoreVertical,
    FiEye,
    FiEdit,
    FiSlash,
    FiDollarSign,
    FiDownload,
    FiPlus
} from 'react-icons/fi';
import DataTable from '../../components/DataTable';
import { AccountStatusBadge, SubscriptionBadge } from '../../components/StatusBadge';
import { mockUsers, subscriptionOptions, accountStatusOptions } from '../../mock/mockUsers';
import './UserListPage.css';

export default function UserListPage() {
    const navigate = useNavigate();
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [subscriptionFilter, setSubscriptionFilter] = useState('ALL');
    const [actionMenuOpen, setActionMenuOpen] = useState(null);

    // Filter users based on selected filters
    const filteredUsers = useMemo(() => {
        return mockUsers.filter(user => {
            const matchesStatus = statusFilter === 'ALL' || user.accountStatus === statusFilter;
            const matchesSubscription = subscriptionFilter === 'ALL' || user.subscription === subscriptionFilter;
            return matchesStatus && matchesSubscription;
        });
    }, [statusFilter, subscriptionFilter]);

    // Format date helper
    const formatDate = (dateString) => {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
        });
    };

    // Format relative time
    const formatRelativeTime = (dateString) => {
        if (!dateString) return '-';
        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

        if (diffDays === 0) return 'Hôm nay';
        if (diffDays === 1) return 'Hôm qua';
        if (diffDays < 7) return `${diffDays} ngày trước`;
        if (diffDays < 30) return `${Math.floor(diffDays / 7)} tuần trước`;
        return formatDate(dateString);
    };

    // Handle row click
    const handleRowClick = (user) => {
        navigate(`/admin/users/${user.id}`);
    };

    // Handle action menu
    const handleActionClick = (e, userId) => {
        e.stopPropagation();
        setActionMenuOpen(actionMenuOpen === userId ? null : userId);
    };

    // Close action menu when clicking outside
    React.useEffect(() => {
        const handleClickOutside = () => setActionMenuOpen(null);
        if (actionMenuOpen) {
            document.addEventListener('click', handleClickOutside);
            return () => document.removeEventListener('click', handleClickOutside);
        }
    }, [actionMenuOpen]);

    // Table columns configuration
    const columns = [
        {
            key: 'user',
            label: 'Người dùng',
            sortable: true,
            render: (_, user) => (
                <div className="user-cell">
                    <div className="user-cell__avatar">
                        {user.avatarUrl ? (
                            <img src={user.avatarUrl} alt={user.fullName} />
                        ) : (
                            <FiUser size={18} />
                        )}
                    </div>
                    <div className="user-cell__info">
                        <div className="user-cell__name">{user.fullName || user.username}</div>
                        <div className="user-cell__username">@{user.username}</div>
                    </div>
                </div>
            ),
        },
        {
            key: 'email',
            label: 'Email',
            sortable: true,
            render: (email) => (
                <div className="email-cell">
                    <FiMail size={14} className="email-cell__icon" />
                    <span>{email}</span>
                </div>
            ),
        },
        {
            key: 'subscription',
            label: 'Gói',
            sortable: true,
            render: (subscription) => <SubscriptionBadge subscription={subscription} />,
        },
        {
            key: 'accountStatus',
            label: 'Trạng thái',
            sortable: true,
            render: (status) => <AccountStatusBadge status={status} />,
        },
        {
            key: 'credits',
            label: 'Lúa',
            sortable: true,
            render: (credits) => (
                <div className="credits-cell">
                    <span className="credits-cell__icon">🌾</span>
                    <span className="credits-cell__value">{credits?.toLocaleString() || 0}</span>
                </div>
            ),
        },
        {
            key: 'lastLoginAt',
            label: 'Đăng nhập gần nhất',
            sortable: true,
            render: (date) => (
                <div className="date-cell">
                    <FiCalendar size={14} className="date-cell__icon" />
                    <span>{formatRelativeTime(date)}</span>
                </div>
            ),
        },
        {
            key: 'createdAt',
            label: 'Ngày tạo',
            sortable: true,
            render: (date) => formatDate(date),
        },
        {
            key: 'actions',
            label: '',
            width: '50px',
            render: (_, user) => (
                <div className="action-cell" onClick={(e) => e.stopPropagation()}>
                    <button
                        className="action-cell__btn"
                        onClick={(e) => handleActionClick(e, user.id)}
                    >
                        <FiMoreVertical size={18} />
                    </button>
                    {actionMenuOpen === user.id && (
                        <div className="action-menu">
                            <button
                                className="action-menu__item"
                                onClick={() => navigate(`/admin/users/${user.id}`)}
                            >
                                <FiEye size={14} />
                                <span>Xem chi tiết</span>
                            </button>
                            <button
                                className="action-menu__item"
                                onClick={() => alert('Sẽ mở modal chỉnh sửa Lúa')}
                            >
                                <FiDollarSign size={14} />
                                <span>Chỉnh sửa Lúa</span>
                            </button>
                            <button
                                className="action-menu__item"
                                onClick={() => alert('Sẽ mở modal chỉnh sửa gói')}
                            >
                                <FiEdit size={14} />
                                <span>Chỉnh sửa gói</span>
                            </button>
                            <div className="action-menu__divider" />
                            <button
                                className="action-menu__item action-menu__item--danger"
                                onClick={() => alert('Sẽ mở modal ban user')}
                            >
                                <FiSlash size={14} />
                                <span>{user.accountStatus === 'BANNED' ? 'Unban user' : 'Ban user'}</span>
                            </button>
                        </div>
                    )}
                </div>
            ),
        },
    ];

    // Filter components
    const FiltersComponent = (
        <>
            <select
                className="filter-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
            >
                <option value="ALL">Tất cả trạng thái</option>
                {accountStatusOptions.map(option => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                ))}
            </select>
            <select
                className="filter-select"
                value={subscriptionFilter}
                onChange={(e) => setSubscriptionFilter(e.target.value)}
            >
                <option value="ALL">Tất cả gói</option>
                {subscriptionOptions.map(option => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                ))}
            </select>
        </>
    );

    // Actions components
    const ActionsComponent = (
        <>
            <button className="admin-btn admin-btn--secondary">
                <FiDownload size={16} />
                <span>Xuất Excel</span>
            </button>
            <button className="admin-btn admin-btn--primary">
                <FiPlus size={16} />
                <span>Thêm user</span>
            </button>
        </>
    );

    return (
        <div className="admin-page user-list-page">
            <div className="admin-page__header">
                <h1 className="admin-page__title">Quản lý Người dùng</h1>
                <p className="admin-page__subtitle">
                    Xem và quản lý tài khoản người dùng trên hệ thống
                </p>
            </div>

            {/* Stats Cards */}
            <div className="user-list-page__stats">
                <div className="stat-card">
                    <div className="stat-card__value">{mockUsers.length}</div>
                    <div className="stat-card__label">Tổng người dùng</div>
                </div>
                <div className="stat-card stat-card--success">
                    <div className="stat-card__value">
                        {mockUsers.filter(u => u.accountStatus === 'ACTIVE').length}
                    </div>
                    <div className="stat-card__label">Đang hoạt động</div>
                </div>
                <div className="stat-card stat-card--primary">
                    <div className="stat-card__value">
                        {mockUsers.filter(u => u.subscription !== 'FREE').length}
                    </div>
                    <div className="stat-card__label">Có gói Premium</div>
                </div>
                <div className="stat-card stat-card--danger">
                    <div className="stat-card__value">
                        {mockUsers.filter(u => u.accountStatus === 'BANNED' || u.accountStatus === 'SUSPENDED').length}
                    </div>
                    <div className="stat-card__label">Bị khóa/cấm</div>
                </div>
            </div>

            {/* Data Table */}
            <DataTable
                columns={columns}
                data={filteredUsers}
                searchPlaceholder="Tìm theo tên, username, email..."
                searchKeys={['username', 'fullName', 'email', 'phone']}
                onRowClick={handleRowClick}
                pageSize={10}
                filters={FiltersComponent}
                actions={ActionsComponent}
                emptyMessage="Không tìm thấy người dùng nào"
            />
        </div>
    );
}
