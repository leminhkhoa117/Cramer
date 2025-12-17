import React from 'react';
import {
    FiUsers,
    FiDollarSign,
    FiFileText,
    FiTrendingUp,
    FiArrowUpRight,
    FiArrowDownRight
} from 'react-icons/fi';

/**
 * AdminDashboard - Trang tổng quan admin
 * Hiển thị các metrics quan trọng và quick actions
 */
export default function AdminDashboard() {
    // Mock data - sẽ được thay thế bằng API calls sau
    const stats = [
        {
            label: 'Tổng người dùng',
            value: '1,234',
            change: '+12.5%',
            changeType: 'up',
            icon: <FiUsers size={24} />,
            iconColor: 'primary',
        },
        {
            label: 'Doanh thu tháng này',
            value: '125.5M ₫',
            change: '+8.3%',
            changeType: 'up',
            icon: <FiDollarSign size={24} />,
            iconColor: 'success',
        },
        {
            label: 'Đề thi đã publish',
            value: '48',
            change: '+4',
            changeType: 'up',
            icon: <FiFileText size={24} />,
            iconColor: 'info',
        },
        {
            label: 'Tăng trưởng',
            value: '15.2%',
            change: '-2.1%',
            changeType: 'down',
            icon: <FiTrendingUp size={24} />,
            iconColor: 'warning',
        },
    ];

    const recentActivities = [
        { id: 1, action: 'User mới đăng ký', user: 'nguyenvana@gmail.com', time: '5 phút trước' },
        { id: 2, action: 'Thanh toán thành công', user: 'quochuu54', time: '12 phút trước' },
        { id: 3, action: 'Đề thi mới được publish', detail: 'Cambridge 18 Test 3', time: '1 giờ trước' },
        { id: 4, action: 'User nâng cấp gói', user: 'thanhpro', time: '2 giờ trước' },
        { id: 5, action: 'Feedback mới', detail: 'Rating 5 sao', time: '3 giờ trước' },
    ];

    return (
        <div className="admin-page">
            {/* Page Header */}
            <div className="admin-page__header">
                <h1 className="admin-page__title">Dashboard</h1>
                <p className="admin-page__subtitle">
                    Chào mừng bạn đến với trang quản trị Cramer CMS
                </p>
            </div>

            {/* Stats Grid */}
            <div className="admin-stats-grid">
                {stats.map((stat, index) => (
                    <div key={index} className="admin-stat-card">
                        <div className={`admin-stat-card__icon admin-stat-card__icon--${stat.iconColor}`}>
                            {stat.icon}
                        </div>
                        <div className="admin-stat-card__content">
                            <div className="admin-stat-card__label">{stat.label}</div>
                            <div className="admin-stat-card__value">{stat.value}</div>
                            <div className={`admin-stat-card__change admin-stat-card__change--${stat.changeType}`}>
                                {stat.changeType === 'up' ? (
                                    <FiArrowUpRight size={14} />
                                ) : (
                                    <FiArrowDownRight size={14} />
                                )}
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
                        <button className="admin-btn admin-btn--ghost admin-btn--sm">
                            Xem tất cả
                        </button>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                        {recentActivities.map(activity => (
                            <div
                                key={activity.id}
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
                                        {activity.action}
                                    </div>
                                    <div style={{
                                        fontSize: '0.8125rem',
                                        color: 'var(--admin-text-muted)'
                                    }}>
                                        {activity.user || activity.detail}
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
                        ))}
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
                    <SystemStatusItem label="API Server" status="operational" />
                    <SystemStatusItem label="Database" status="operational" />
                    <SystemStatusItem label="Payment Gateway" status="operational" />
                    <SystemStatusItem label="AI Grading" status="operational" />
                </div>
            </div>
        </div>
    );
}

// Quick Action Button Component
function QuickActionButton({ icon, label, to }) {
    return (
        <a
            href={to}
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
        </a>
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
                background: statusColors[status],
                boxShadow: `0 0 8px ${statusColors[status]}`,
            }} />
            <span style={{ fontSize: '0.875rem', color: 'var(--admin-text-secondary)' }}>
                {label}
            </span>
            <span style={{
                fontSize: '0.75rem',
                color: statusColors[status],
                fontWeight: 500
            }}>
                {statusLabels[status]}
            </span>
        </div>
    );
}
