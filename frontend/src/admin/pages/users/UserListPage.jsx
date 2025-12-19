import React, { useEffect, useMemo, useCallback, useState } from 'react';
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
    FiRefreshCw,
    FiAlertCircle,
    FiSearch,
    FiChevronUp,
    FiChevronDown,
    FiChevronLeft,
    FiChevronRight
} from 'react-icons/fi';
import { AccountStatusBadge, SubscriptionBadge } from '../../components/StatusBadge';
import useAdminUsersStore from '../../stores/useAdminUsersStore';
import { exportUsersToExcel } from '../../utils/exportExcel';
import '../../css/pages/users/UserListPage.css';

// Filter options
const subscriptionOptions = [
    { value: 'FREE', label: 'Cramerie (Free)' },
    { value: 'CRAMERICH', label: 'Cramerich' }
];

const accountStatusOptions = [
    { value: 'ACTIVE', label: 'Hoạt động' },
    { value: 'BANNED', label: 'Bị cấm' },
    { value: 'DEACTIVATED', label: 'Ngừng hoạt động' }
];

export default function UserListPage() {
    const navigate = useNavigate();

    // Zustand store
    const {
        users,
        stats,
        isLoading,
        isExporting,
        error,
        fetchUsers,
        fetchStats,
        exportToExcel
    } = useAdminUsersStore();

    // Local state for client-side filtering/sorting (faster UX)
    const [searchQuery, setSearchQuery] = useState('');
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [subscriptionFilter, setSubscriptionFilter] = useState('ALL');
    const [sortBy, setSortBy] = useState('createdAt');
    const [sortOrder, setSortOrder] = useState('desc');
    const [currentPage, setCurrentPage] = useState(0);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const pageSize = 10;

    // Action menu state
    const [actionMenuOpen, setActionMenuOpen] = useState(null);

    // Initial load - fetch ALL users
    useEffect(() => {
        fetchUsers();
        fetchStats();
    }, []);

    // Client-side filtering
    const filteredUsers = useMemo(() => {
        let result = [...users];

        // Search filter
        if (searchQuery.trim()) {
            const query = searchQuery.toLowerCase();
            result = result.filter(user =>
                user.username?.toLowerCase().includes(query) ||
                user.fullName?.toLowerCase().includes(query) ||
                user.email?.toLowerCase().includes(query)
            );
        }

        // Status filter
        if (statusFilter !== 'ALL') {
            result = result.filter(user => user.accountStatus === statusFilter);
        }

        // Subscription filter
        if (subscriptionFilter !== 'ALL') {
            if (subscriptionFilter === 'FREE') {
                result = result.filter(user =>
                    !user.subscription || user.subscription === 'FREE' || user.subscription === 'CRAMERIE'
                );
            } else {
                result = result.filter(user =>
                    user.subscription?.toUpperCase() === subscriptionFilter
                );
            }
        }

        return result;
    }, [users, searchQuery, statusFilter, subscriptionFilter]);

    // Client-side sorting
    const sortedUsers = useMemo(() => {
        const sorted = [...filteredUsers];

        sorted.sort((a, b) => {
            let aValue = a[sortBy];
            let bValue = b[sortBy];

            // Handle null/undefined
            if (aValue == null) return 1;
            if (bValue == null) return -1;

            // Handle dates
            if (sortBy.includes('At') || sortBy === 'createdAt') {
                aValue = new Date(aValue).getTime();
                bValue = new Date(bValue).getTime();
            }

            // Handle strings
            if (typeof aValue === 'string') {
                aValue = aValue.toLowerCase();
                bValue = bValue.toLowerCase();
            }

            if (aValue < bValue) return sortOrder === 'asc' ? -1 : 1;
            if (aValue > bValue) return sortOrder === 'asc' ? 1 : -1;
            return 0;
        });

        return sorted;
    }, [filteredUsers, sortBy, sortOrder]);

    // Pagination
    const paginatedUsers = useMemo(() => {
        const start = currentPage * pageSize;
        return sortedUsers.slice(start, start + pageSize);
    }, [sortedUsers, currentPage, pageSize]);

    // Calculate stats from users array
    const displayStats = useMemo(() => {
        return {
            totalUsers: users.length,
            activeUsers: users.filter(u => u.accountStatus === 'ACTIVE' || !u.accountStatus).length,
            premiumUsers: users.filter(u =>
                u.subscription?.toUpperCase() === 'CRAMERICH'
            ).length,
            newUsersThisMonth: users.filter(u => {
                if (!u.createdAt) return false;
                const created = new Date(u.createdAt);
                const now = new Date();
                return created.getMonth() === now.getMonth() &&
                    created.getFullYear() === now.getFullYear();
            }).length
        };
    }, [users]);

    // Reset page when filters change
    useEffect(() => {
        setCurrentPage(0);
    }, [searchQuery, statusFilter, subscriptionFilter]);

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
    useEffect(() => {
        const handleClickOutside = () => setActionMenuOpen(null);
        if (actionMenuOpen) {
            document.addEventListener('click', handleClickOutside);
            return () => document.removeEventListener('click', handleClickOutside);
        }
    }, [actionMenuOpen]);

    // Handle sort
    const handleSort = (column) => {
        if (sortBy === column) {
            setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
        } else {
            setSortBy(column);
            setSortOrder('desc');
        }
    };

    // Handle refresh - reset all filters and reload
    const handleRefresh = async () => {
        setIsRefreshing(true);
        setSearchQuery('');
        setStatusFilter('ALL');
        setSubscriptionFilter('ALL');
        setCurrentPage(0);
        setSortBy('createdAt');
        setSortOrder('desc');

        await fetchUsers();
        await fetchStats();
        setIsRefreshing(false);
    };

    // Handle export
    const handleExport = async () => {
        try {
            const allUsers = await exportToExcel();
            exportUsersToExcel(allUsers);
        } catch (error) {
            console.error('Export failed:', error);
            alert('Xuất file thất bại. Vui lòng thử lại.');
        }
    };

    // Pagination calculated values
    const totalPages = Math.ceil(sortedUsers.length / pageSize) || 1;
    const startItem = sortedUsers.length > 0 ? currentPage * pageSize + 1 : 0;
    const endItem = Math.min((currentPage + 1) * pageSize, sortedUsers.length);

    // Sort icon renderer
    const renderSortIcon = (column) => {
        if (sortBy !== column) {
            return <FiChevronUp size={14} style={{ opacity: 0.3 }} />;
        }
        return sortOrder === 'asc' ? <FiChevronUp size={14} /> : <FiChevronDown size={14} />;
    };

    // Error state
    if (error && !isLoading && users.length === 0) {
        return (
            <div className="admin-page user-list-page">
                <div className="admin-page__header">
                    <h1 className="admin-page__title">Quản lý Người dùng</h1>
                </div>
                <div className="admin-card" style={{ textAlign: 'center', padding: '60px 40px' }}>
                    <FiAlertCircle size={48} style={{ color: '#EF4444', marginBottom: '16px' }} />
                    <h3 style={{ marginBottom: '8px' }}>Đã xảy ra lỗi</h3>
                    <p style={{ color: 'var(--admin-text-muted)', marginBottom: '24px' }}>{error}</p>
                    <button className="admin-btn admin-btn--primary" onClick={handleRefresh}>
                        <FiRefreshCw size={16} /> Thử lại
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="admin-page user-list-page">
            <div className="admin-page__header">
                <div>
                    <h1 className="admin-page__title">Quản lý Người dùng</h1>
                    <p className="admin-page__subtitle">
                        Xem và quản lý tài khoản người dùng trên hệ thống
                    </p>
                </div>
            </div>

            {/* Stats Cards */}
            <div className="user-list-page__stats">
                <div className="stat-card">
                    <div className="stat-card__value">{displayStats.totalUsers.toLocaleString()}</div>
                    <div className="stat-card__label">Tổng người dùng</div>
                </div>
                <div className="stat-card stat-card--success">
                    <div className="stat-card__value">
                        {displayStats.activeUsers.toLocaleString()}
                    </div>
                    <div className="stat-card__label">Đang hoạt động</div>
                </div>
                <div className="stat-card stat-card--primary">
                    <div className="stat-card__value">
                        {displayStats.premiumUsers.toLocaleString()}
                    </div>
                    <div className="stat-card__label">Có gói Premium</div>
                </div>
                <div className="stat-card stat-card--info">
                    <div className="stat-card__value">
                        {displayStats.newUsersThisMonth.toLocaleString()}
                    </div>
                    <div className="stat-card__label">Mới tháng này</div>
                </div>
            </div>

            {/* Table Card */}
            <div className="admin-card">
                {/* Table Header with Search and Filters */}
                <div className="table-header">
                    <div className="table-header__left">
                        <div className="search-box">
                            <FiSearch className="search-box__icon" />
                            <input
                                type="text"
                                placeholder="Tìm theo tên, username, email..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                className="search-box__input"
                            />
                        </div>
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
                    </div>
                    <div className="table-header__right">
                        <button
                            className="admin-btn admin-btn--secondary"
                            onClick={handleRefresh}
                            disabled={isRefreshing}
                        >
                            <FiRefreshCw size={16} className={isRefreshing ? 'spin' : ''} />
                            <span>Làm mới</span>
                        </button>
                        <button
                            className="admin-btn admin-btn--primary"
                            onClick={handleExport}
                            disabled={isExporting}
                        >
                            <FiDownload size={16} />
                            <span>{isExporting ? 'Đang xuất...' : 'Xuất Excel'}</span>
                        </button>
                    </div>
                </div>

                {/* Table */}
                <div className="table-wrapper">
                    <table className="users-table">
                        <thead>
                            <tr>
                                <th className="sortable" onClick={() => handleSort('username')}>
                                    <span>NGƯỜI DÙNG</span>
                                    {renderSortIcon('username')}
                                </th>
                                <th className="sortable" onClick={() => handleSort('email')}>
                                    <span>EMAIL</span>
                                    {renderSortIcon('email')}
                                </th>
                                <th className="sortable" onClick={() => handleSort('subscription')}>
                                    <span>GÓI</span>
                                    {renderSortIcon('subscription')}
                                </th>
                                <th className="sortable" onClick={() => handleSort('accountStatus')}>
                                    <span>TRẠNG THÁI</span>
                                    {renderSortIcon('accountStatus')}
                                </th>
                                <th className="sortable" onClick={() => handleSort('credits')}>
                                    <span>LÚA</span>
                                    {renderSortIcon('credits')}
                                </th>
                                <th className="sortable" onClick={() => handleSort('lastLoginAt')}>
                                    <span>ĐĂNG NHẬP GẦN NHẤT</span>
                                    {renderSortIcon('lastLoginAt')}
                                </th>
                                <th className="sortable" onClick={() => handleSort('createdAt')}>
                                    <span>NGÀY TẠO</span>
                                    {renderSortIcon('createdAt')}
                                </th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            {isLoading && users.length === 0 ? (
                                <tr>
                                    <td colSpan={8} className="table-loading">
                                        <div className="table-spinner" />
                                        <span>Đang tải...</span>
                                    </td>
                                </tr>
                            ) : paginatedUsers.length === 0 ? (
                                <tr>
                                    <td colSpan={8} className="table-empty">
                                        Không tìm thấy người dùng nào
                                    </td>
                                </tr>
                            ) : (
                                paginatedUsers.map(user => (
                                    <tr
                                        key={user.id}
                                        className="table-row"
                                        onClick={() => handleRowClick(user)}
                                    >
                                        <td>
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
                                        </td>
                                        <td>
                                            <div className="email-cell">
                                                <FiMail size={14} className="email-cell__icon" />
                                                <span>{user.email || '-'}</span>
                                            </div>
                                        </td>
                                        <td>
                                            <SubscriptionBadge subscription={user.subscription || 'FREE'} />
                                        </td>
                                        <td>
                                            <AccountStatusBadge status={user.accountStatus || 'ACTIVE'} />
                                        </td>
                                        <td>
                                            <div className="credits-cell">
                                                <span className="credits-cell__icon">🌾</span>
                                                <span className="credits-cell__value">{user.credits?.toLocaleString() || 0}</span>
                                            </div>
                                        </td>
                                        <td>
                                            <div className="date-cell">
                                                <FiCalendar size={14} className="date-cell__icon" />
                                                <span>{formatRelativeTime(user.lastLoginAt)}</span>
                                            </div>
                                        </td>
                                        <td>{formatDate(user.createdAt)}</td>
                                        <td>
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
                                                            onClick={() => navigate(`/admin/users/${user.id}`)}
                                                        >
                                                            <FiDollarSign size={14} />
                                                            <span>Chỉnh sửa Lúa</span>
                                                        </button>
                                                        <div className="action-menu__divider" />
                                                        <button
                                                            className="action-menu__item action-menu__item--danger"
                                                            onClick={() => navigate(`/admin/users/${user.id}`)}
                                                        >
                                                            <FiSlash size={14} />
                                                            <span>{user.accountStatus === 'BANNED' ? 'Unban user' : 'Ban user'}</span>
                                                        </button>
                                                    </div>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                {/* Pagination */}
                {sortedUsers.length > 0 && (
                    <div className="table-pagination">
                        <div className="table-pagination__info">
                            Hiển thị {startItem} - {endItem} trong tổng số {sortedUsers.length.toLocaleString()} kết quả
                        </div>
                        <div className="table-pagination__controls">
                            <button
                                className="pagination-btn"
                                onClick={() => setCurrentPage(currentPage - 1)}
                                disabled={currentPage === 0}
                            >
                                <FiChevronLeft size={16} />
                            </button>

                            {/* Page numbers */}
                            {Array.from({ length: totalPages }, (_, i) => i)
                                .filter(page => {
                                    return page === 0 ||
                                        page === totalPages - 1 ||
                                        Math.abs(page - currentPage) <= 1;
                                })
                                .reduce((acc, page, idx, arr) => {
                                    if (idx > 0 && page - arr[idx - 1] > 1) {
                                        acc.push('...');
                                    }
                                    acc.push(page);
                                    return acc;
                                }, [])
                                .map((page, idx) => (
                                    page === '...' ? (
                                        <span key={`ellipsis-${idx}`} className="pagination-ellipsis">...</span>
                                    ) : (
                                        <button
                                            key={page}
                                            className={`pagination-btn ${currentPage === page ? 'pagination-btn--active' : ''}`}
                                            onClick={() => setCurrentPage(page)}
                                        >
                                            {page + 1}
                                        </button>
                                    )
                                ))
                            }

                            <button
                                className="pagination-btn"
                                onClick={() => setCurrentPage(currentPage + 1)}
                                disabled={currentPage >= totalPages - 1}
                            >
                                <FiChevronRight size={16} />
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
