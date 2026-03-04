import React, { useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
    FiUsers,
    FiDollarSign,
    FiFileText,
    FiTrendingUp,
    FiArrowUpRight,
    FiArrowDownRight,
    FiRefreshCw
} from 'react-icons/fi';
import { useAdminDashboardStore } from '../stores';

/**
 * AdminDashboard - Trang tổng quan admin
 * Hiển thị các metrics quan trọng và quick actions với dữ liệu thật từ database
 * 
 * Sử dụng caching để tránh fetch lại mỗi khi navigate
 */
export default function AdminDashboard() {
    const {
        stats,
        changes,
        recentActivities,
        systemStatus,
        isLoadingStats,
        isLoadingActivities,
        initializeDashboard,
        refreshAll
    } = useAdminDashboardStore();

    // Initialize data on mount - will use cache if available
    useEffect(() => {
        initializeDashboard();
    }, [initializeDashboard]);

    // Format number with commas - memoized
    const formatNumber = useMemo(() => (num) => {
        if (num === undefined || num === null) return '0';
        return num.toLocaleString('vi-VN');
    }, []);

    // Format currency - memoized
    const formatCurrency = useMemo(() => (amount) => {
        if (!amount) return '0 ₫';
        if (amount >= 1000000) {
            return `${(amount / 1000000).toFixed(1)}M ₫`;
        }
        return amount.toLocaleString('vi-VN') + ' ₫';
    }, []);

    // Stats data with real values from store - memoized
    const statsData = useMemo(() => [
        {
            label: 'Tổng người dùng',
            value: formatNumber(stats.totalUsers),
            change: changes.users?.value ? `+${changes.users.value}%` : '+0%',
            changeType: changes.users?.type || 'up',
            icon: <FiUsers size={24} />,
            iconColor: 'primary',
        },
        {
            label: 'Doanh thu tháng này',
            value: formatCurrency(stats.totalRevenue),
            change: changes.revenue?.value ? `+${changes.revenue.value}%` : '+0%',
            changeType: changes.revenue?.type || 'up',
            icon: <FiDollarSign size={24} />,
            iconColor: 'success',
        },
        {
            label: 'Đề thi đã publish',
            value: formatNumber(stats.publishedTests || Math.floor(stats.totalQuestions / 60)),
            change: changes.tests?.value ? `+${changes.tests.value}` : '+0',
            changeType: changes.tests?.type || 'up',
            icon: <FiFileText size={24} />,
            iconColor: 'info',
        },
        {
            label: 'Tăng trưởng',
            value: `${changes.growth?.value || 0}%`,
            change: stats.newUsersThisMonth > 0 ? `+${stats.newUsersThisMonth} user mới` : 'Ổn định',
            changeType: stats.newUsersThisMonth > 0 ? 'up' : 'neutral',
            icon: <FiTrendingUp size={24} />,
            iconColor: 'warning',
        },
    ], [stats, changes, formatNumber, formatCurrency]);

    // Force refresh all data
    const handleRefresh = () => {
        refreshAll();
    };

    return (
        <div className="admin-page">
            {/* Page Header */}
            <div className="admin-page__header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                    <h1 className="admin-page__title">Dashboard</h1>
                    <p className="admin-page__subtitle">
                        Chào mừng bạn đến với trang quản trị Cramer CMS
                    </p>
                </div>
                <button
                    className="admin-btn admin-btn--secondary"
                    onClick={handleRefresh}
                    disabled={isLoadingStats}
                >
                    <FiRefreshCw size={16} className={isLoadingStats ? 'spin' : ''} />
                    Làm mới
                </button>
            </div>

            {/* Stats Grid */}
            <div className="admin-stats-grid">
                {statsData.map((stat, index) => (
                    <div key={index} className="admin-stat-card">
                        <div className={`admin-stat-card__icon admin-stat-card__icon--${stat.iconColor}`}>
                            {stat.icon}
                        </div>
                        <div className="admin-stat-card__content">
                            <div className="admin-stat-card__label">{stat.label}</div>
                            <div className="admin-stat-card__value">
                                {isLoadingStats ? '...' : stat.value}
                            </div>
                            <div className={`admin-stat-card__change admin-stat-card__change--${stat.changeType}`}>
                                {stat.changeType === 'up' ? (
                                    <FiArrowUpRight size={14} />
                                ) : stat.changeType === 'down' ? (
                                    <FiArrowDownRight size={14} />
                                ) : null}
                                <span>{stat.change}</span>
                                <span style={{ color: 'var(--admin-text-muted)', marginLeft: '4px' }}>
                                    so với tháng trước
                                </span>
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            {/* Content Grid */}
            <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
                gap: 'var(--admin-spacing-lg)'
            }}>
                {/* Recent Activities */}
                <div className="admin-card">
                    <div className="admin-card__header">
                        <h3 className="admin-card__title">Hoạt động gần đây</h3>
                        <Link to="/admin/users" className="admin-btn admin-btn--ghost admin-btn--sm">
                            Xem tất cả
                        </Link>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                        {isLoadingActivities ? (
                            <div style={{ textAlign: 'center', padding: '20px', color: 'var(--admin-text-muted)' }}>
                                Đang tải...
                            </div>
                        ) : recentActivities.length > 0 ? (
                            recentActivities.map((activity, index) => (
                                <div
                                    key={activity.id || index}
                                    style={{
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        alignItems: 'center',
                                        padding: '12px',
                                        background: 'var(--admin-bg-card)',
                                        borderRadius: 'var(--admin-radius-md)',
                                    }}
                                >
                                    <div>
                                        <div style={{
                                            fontSize: '0.875rem',
                                            color: 'var(--admin-text-primary)',
                                            marginBottom: '4px'
                                        }}>
                                            {activity.action || activity.title}
                                        </div>
                                        <div style={{
                                            fontSize: '0.8125rem',
                                            color: 'var(--admin-text-muted)'
                                        }}>
                                            {activity.user || activity.detail || activity.description}
                                        </div>
                                    </div>
                                    <div style={{
                                        fontSize: '0.75rem',
                                        color: 'var(--admin-text-disabled)',
                                        whiteSpace: 'nowrap'
                                    }}>
                                        {activity.time}
                                    </div>
                                </div>
                            ))
                        ) : (
                            <div style={{ textAlign: 'center', padding: '20px', color: 'var(--admin-text-muted)' }}>
                                Chưa có hoạt động nào
                            </div>
                        )}
                    </div>
                </div>

                {/* Quick Actions */}
                <div className="admin-card">
                    <div className="admin-card__header">
                        <h3 className="admin-card__title">Thao tác nhanh</h3>
                    </div>
                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(2, 1fr)',
                        gap: '12px'
                    }}>
                        <QuickActionButton
                            icon={<FiUsers size={20} />}
                            label="Quản lý Users"
                            to="/admin/users"
                        />
                        <QuickActionButton
                            icon={<FiDollarSign size={20} />}
                            label="Xem doanh thu"
                            to="/admin/finance"
                        />
                        <QuickActionButton
                            icon={<FiFileText size={20} />}
                            label="Thêm đề thi"
                            to="/admin/content"
                        />
                        <QuickActionButton
                            icon={<FiTrendingUp size={20} />}
                            label="Báo cáo"
                            to="/admin/finance/reports"
                        />
                    </div>
                </div>
            </div>

            {/* System Status */}
            <div className="admin-card" style={{ marginTop: 'var(--admin-spacing-lg)' }}>
                <div className="admin-card__header">
                    <h3 className="admin-card__title">Trạng thái hệ thống</h3>
                </div>
                <div style={{
                    display: 'flex',
                    gap: 'var(--admin-spacing-xl)',
                    flexWrap: 'wrap'
                }}>
                    <SystemStatusItem label="API Server" status={systemStatus.apiServer} />
                    <SystemStatusItem label="Database" status={systemStatus.database} />
                    <SystemStatusItem label="Payment Gateway" status={systemStatus.paymentGateway} />
                    <SystemStatusItem label="AI Grading" status={systemStatus.aiGrading} />
                </div>
            </div>

            {/* Additional Stats */}
            <div className="admin-card" style={{ marginTop: 'var(--admin-spacing-lg)' }}>
                <div className="admin-card__header">
                    <h3 className="admin-card__title">Thống kê chi tiết</h3>
                </div>
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
                    gap: 'var(--admin-spacing-md)'
                }}>
                    <DetailStat label="Tổng câu hỏi" value={formatNumber(stats.totalQuestions)} />
                    <DetailStat label="Lượt làm bài" value={formatNumber(stats.totalTestAttempts)} />
                    <DetailStat label="User hoạt động" value={formatNumber(stats.activeUsers)} />
                    <DetailStat label="Từ vựng đã lưu" value={formatNumber(stats.totalVocabulary)} />
                </div>
            </div>
        </div>
    );
}

// Quick Action Button Component
function QuickActionButton({ icon, label, to }) {
    return (
        <Link
            to={to}
            style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '8px',
                padding: '20px',
                background: 'var(--admin-bg-card)',
                borderRadius: 'var(--admin-radius-md)',
                border: '1px solid var(--admin-border-primary)',
                textDecoration: 'none',
                color: 'var(--admin-text-secondary)',
                transition: 'all 0.2s ease',
                cursor: 'pointer',
            }}
            onMouseEnter={(e) => {
                e.currentTarget.style.background = 'var(--admin-primary-light)';
                e.currentTarget.style.color = 'var(--admin-primary)';
                e.currentTarget.style.borderColor = 'var(--admin-primary)';
            }}
            onMouseLeave={(e) => {
                e.currentTarget.style.background = 'var(--admin-bg-card)';
                e.currentTarget.style.color = 'var(--admin-text-secondary)';
                e.currentTarget.style.borderColor = 'var(--admin-border-primary)';
            }}
        >
            {icon}
            <span style={{ fontSize: '0.8125rem', fontWeight: 500 }}>{label}</span>
        </Link>
    );
}

// System Status Item Component
function SystemStatusItem({ label, status }) {
    const statusColors = {
        operational: 'var(--admin-success)',
        degraded: 'var(--admin-warning)',
        down: 'var(--admin-danger)',
    };

    const statusLabels = {
        operational: 'Hoạt động',
        degraded: 'Chậm',
        down: 'Lỗi',
    };

    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                background: statusColors[status] || statusColors.operational,
                boxShadow: `0 0 8px ${statusColors[status] || statusColors.operational}`,
            }} />
            <span style={{ fontSize: '0.875rem', color: 'var(--admin-text-secondary)' }}>
                {label}
            </span>
            <span style={{
                fontSize: '0.75rem',
                color: statusColors[status] || statusColors.operational,
                fontWeight: 500
            }}>
                {statusLabels[status] || 'N/A'}
            </span>
        </div>
    );
}

// Detail Stat Component
function DetailStat({ label, value }) {
    return (
        <div style={{
            padding: 'var(--admin-spacing-md)',
            background: 'var(--admin-bg-card)',
            borderRadius: 'var(--admin-radius-md)',
            textAlign: 'center'
        }}>
            <div style={{
                fontSize: '1.5rem',
                fontWeight: 700,
                color: 'var(--admin-text-primary)',
                marginBottom: '4px'
            }}>
                {value}
            </div>
            <div style={{
                fontSize: '0.75rem',
                color: 'var(--admin-text-muted)',
                textTransform: 'uppercase',
                letterSpacing: '0.5px'
            }}>
                {label}
            </div>
        </div>
    );
}
