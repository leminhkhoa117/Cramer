import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    FiArrowLeft,
    FiUser,
    FiCreditCard,
    FiDollarSign,
    FiActivity,
    FiFileText,
    FiEdit,
    FiSlash,
    FiMail,
    FiPhone,
    FiMapPin,
    FiCalendar,
    FiLogIn,
    FiCheckCircle,
    FiTarget,
    FiBook,
    FiPlus,
    FiMinus
} from 'react-icons/fi';
import {
    getUserById,
    getUserActivities,
    getUserAuditLogs,
    getCreditTransactions,
    getUserQuotas,
    quotaTypes
} from '../../mock/mockUsers';
import { AccountStatusBadge, SubscriptionBadge, SubscriptionStatusBadge } from '../../components/StatusBadge';
import './UserDetailPage.css';

export default function UserDetailPage() {
    const { userId } = useParams();
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('profile');

    const user = getUserById(userId);
    const activities = getUserActivities(userId);
    const auditLogs = getUserAuditLogs(userId);
    const creditTransactions = getCreditTransactions(userId);
    const userQuotas = user ? getUserQuotas(userId, user.subscription) : null;

    if (!user) {
        return (
            <div className="admin-page user-detail-page">
                <div className="user-not-found">
                    <h2>Không tìm thấy người dùng</h2>
                    <p>ID: {userId}</p>
                    <button className="admin-btn admin-btn--primary" onClick={() => navigate('/admin/users')}>
                        Quay lại danh sách
                    </button>
                </div>
            </div>
        );
    }

    const formatDate = (dateString) => {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
    };

    const tabs = [
        { id: 'profile', label: 'Hồ sơ', icon: FiUser },
        { id: 'subscription', label: 'Gói đăng ký', icon: FiCreditCard },
        { id: 'credits', label: 'Lúa', icon: FiDollarSign },
        { id: 'activity', label: 'Hoạt động', icon: FiActivity },
        { id: 'audit', label: 'Lịch sử Admin', icon: FiFileText },
    ];

    return (
        <div className="admin-page user-detail-page">
            {/* Back Button */}
            <button className="back-button" onClick={() => navigate('/admin/users')}>
                <FiArrowLeft size={18} />
                <span>Quay lại danh sách</span>
            </button>

            {/* User Header Card */}
            <div className="user-header-card">
                <div className="user-header-card__left">
                    <div className="user-header-card__avatar">
                        {user.avatarUrl ? (
                            <img src={user.avatarUrl} alt={user.fullName} />
                        ) : (
                            <FiUser size={32} />
                        )}
                    </div>
                    <div className="user-header-card__info">
                        <h1 className="user-header-card__name">{user.fullName || user.username}</h1>
                        <p className="user-header-card__username">@{user.username}</p>
                        <div className="user-header-card__badges">
                            <SubscriptionBadge subscription={user.subscription} />
                            <AccountStatusBadge status={user.accountStatus} />
                        </div>
                    </div>
                </div>
                <div className="user-header-card__actions">
                    <button className="admin-btn admin-btn--secondary">
                        <FiEdit size={16} />
                        <span>Chỉnh sửa</span>
                    </button>
                    <button className={`admin-btn ${user.accountStatus === 'BANNED' ? 'admin-btn--success' : 'admin-btn--danger'}`}>
                        <FiSlash size={16} />
                        <span>{user.accountStatus === 'BANNED' ? 'Unban' : 'Ban'}</span>
                    </button>
                </div>
            </div>

            {/* Tabs */}
            <div className="user-tabs">
                <div className="user-tabs__nav">
                    {tabs.map(tab => (
                        <button
                            key={tab.id}
                            className={`user-tabs__tab ${activeTab === tab.id ? 'user-tabs__tab--active' : ''}`}
                            onClick={() => setActiveTab(tab.id)}
                        >
                            <tab.icon size={16} />
                            <span>{tab.label}</span>
                        </button>
                    ))}
                </div>

                <div className="user-tabs__content">
                    {/* Profile Tab */}
                    {activeTab === 'profile' && (
                        <div className="tab-content">
                            <div className="info-grid">
                                <div className="info-section">
                                    <h3 className="info-section__title">Thông tin liên hệ</h3>
                                    <div className="info-list">
                                        <div className="info-item">
                                            <FiMail className="info-item__icon" />
                                            <div className="info-item__content">
                                                <span className="info-item__label">Email</span>
                                                <span className="info-item__value">{user.email}</span>
                                            </div>
                                        </div>
                                        <div className="info-item">
                                            <FiPhone className="info-item__icon" />
                                            <div className="info-item__content">
                                                <span className="info-item__label">Số điện thoại</span>
                                                <span className="info-item__value">{user.phone || '-'}</span>
                                            </div>
                                        </div>
                                        <div className="info-item">
                                            <FiMapPin className="info-item__icon" />
                                            <div className="info-item__content">
                                                <span className="info-item__label">Địa chỉ</span>
                                                <span className="info-item__value">{user.address || '-'}</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div className="info-section">
                                    <h3 className="info-section__title">Thông tin tài khoản</h3>
                                    <div className="info-list">
                                        <div className="info-item">
                                            <FiCalendar className="info-item__icon" />
                                            <div className="info-item__content">
                                                <span className="info-item__label">Ngày tạo</span>
                                                <span className="info-item__value">{formatDate(user.createdAt)}</span>
                                            </div>
                                        </div>
                                        <div className="info-item">
                                            <FiLogIn className="info-item__icon" />
                                            <div className="info-item__content">
                                                <span className="info-item__label">Đăng nhập gần nhất</span>
                                                <span className="info-item__value">{formatDate(user.lastLoginAt)}</span>
                                            </div>
                                        </div>
                                        <div className="info-item">
                                            <FiCheckCircle className="info-item__icon" />
                                            <div className="info-item__content">
                                                <span className="info-item__label">Số lần đăng nhập</span>
                                                <span className="info-item__value">{user.loginCount} lần</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div className="info-section">
                                    <h3 className="info-section__title">Thống kê học tập</h3>
                                    <div className="stats-grid">
                                        <div className="mini-stat">
                                            <FiTarget className="mini-stat__icon" />
                                            <div className="mini-stat__value">{user.testsCompleted}</div>
                                            <div className="mini-stat__label">Bài thi hoàn thành</div>
                                        </div>
                                        <div className="mini-stat">
                                            <div className="mini-stat__icon">🎯</div>
                                            <div className="mini-stat__value">{user.averageScore || 0}</div>
                                            <div className="mini-stat__label">Điểm trung bình</div>
                                        </div>
                                        <div className="mini-stat">
                                            <div className="mini-stat__icon">🔥</div>
                                            <div className="mini-stat__value">{user.highestStreak}</div>
                                            <div className="mini-stat__label">Streak cao nhất</div>
                                        </div>
                                        <div className="mini-stat">
                                            <FiBook className="mini-stat__icon" />
                                            <div className="mini-stat__value">{user.vocabularySaved}</div>
                                            <div className="mini-stat__label">Từ vựng đã lưu</div>
                                        </div>
                                    </div>
                                </div>

                                {user.statusReason && (
                                    <div className="info-section info-section--warning">
                                        <h3 className="info-section__title">Lý do khóa tài khoản</h3>
                                        <p className="info-section__text">{user.statusReason}</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {/* Subscription Tab */}
                    {activeTab === 'subscription' && (
                        <div className="tab-content">
                            <div className="subscription-card">
                                <div className="subscription-card__header">
                                    <div className="subscription-card__type">
                                        <SubscriptionBadge subscription={user.subscription} size="lg" />
                                        {user.subscriptionStatus && (
                                            <SubscriptionStatusBadge status={user.subscriptionStatus} />
                                        )}
                                    </div>
                                    <button className="admin-btn admin-btn--primary">
                                        <FiEdit size={16} />
                                        <span>Chỉnh sửa gói</span>
                                    </button>
                                </div>

                                {user.subscription !== 'FREE' && (
                                    <div className="subscription-card__details">
                                        <div className="subscription-detail">
                                            <span className="subscription-detail__label">Ngày bắt đầu</span>
                                            <span className="subscription-detail__value">{formatDate(user.subscriptionStartDate)}</span>
                                        </div>
                                        <div className="subscription-detail">
                                            <span className="subscription-detail__label">Ngày hết hạn</span>
                                            <span className="subscription-detail__value">{formatDate(user.subscriptionEndDate)}</span>
                                        </div>
                                        <div className="subscription-detail">
                                            <span className="subscription-detail__label">Tự động gia hạn</span>
                                            <span className={`subscription-detail__value ${user.autoRenew ? 'text-success' : 'text-danger'}`}>
                                                {user.autoRenew ? 'Có' : 'Không'}
                                            </span>
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* Quotas Section */}
                            <div className="quotas-section">
                                <div className="quotas-section__header">
                                    <h3 className="quotas-section__title">Hạn mức sử dụng</h3>
                                    {userQuotas?.resetDate && (
                                        <span className="quotas-section__reset">
                                            Reset: {formatDate(userQuotas.resetDate)}
                                        </span>
                                    )}
                                </div>

                                <div className="quotas-grid">
                                    {quotaTypes.map(quotaType => {
                                        const quota = userQuotas?.[quotaType.key] || { used: 0, limit: 0, customLimit: null };
                                        const effectiveLimit = quota.customLimit !== null ? quota.customLimit : quota.limit;
                                        const isUnlimited = effectiveLimit === -1;
                                        const percentage = isUnlimited ? 0 : (effectiveLimit > 0 ? (quota.used / effectiveLimit) * 100 : 100);
                                        const isNearLimit = percentage >= 80;
                                        const isOverLimit = percentage >= 100;

                                        return (
                                            <div key={quotaType.key} className="quota-card">
                                                <div className="quota-card__header">
                                                    <span className="quota-card__icon">{quotaType.icon}</span>
                                                    <div className="quota-card__info">
                                                        <span className="quota-card__label">{quotaType.label}</span>
                                                        <span className="quota-card__description">{quotaType.description}</span>
                                                    </div>
                                                    <button
                                                        className="quota-card__edit-btn"
                                                        onClick={() => alert(`Sẽ mở modal chỉnh sửa ${quotaType.label}`)}
                                                        title="Chỉnh sửa hạn mức"
                                                    >
                                                        <FiEdit size={14} />
                                                    </button>
                                                </div>

                                                <div className="quota-card__usage">
                                                    <div className="quota-card__numbers">
                                                        <span className="quota-card__used">{quota.used.toLocaleString()}</span>
                                                        <span className="quota-card__separator">/</span>
                                                        <span className="quota-card__limit">
                                                            {isUnlimited ? '∞' : effectiveLimit.toLocaleString()}
                                                        </span>
                                                        {quota.customLimit !== null && (
                                                            <span className="quota-card__custom-badge">Tùy chỉnh</span>
                                                        )}
                                                    </div>

                                                    {!isUnlimited && effectiveLimit > 0 && (
                                                        <div className="quota-card__progress">
                                                            <div
                                                                className={`quota-card__progress-bar ${isOverLimit ? 'quota-card__progress-bar--danger' : isNearLimit ? 'quota-card__progress-bar--warning' : ''}`}
                                                                style={{ width: `${Math.min(percentage, 100)}%` }}
                                                            />
                                                        </div>
                                                    )}

                                                    {isUnlimited && (
                                                        <div className="quota-card__unlimited-badge">
                                                            ✨ Không giới hạn
                                                        </div>
                                                    )}

                                                    {effectiveLimit === 0 && !isUnlimited && (
                                                        <div className="quota-card__disabled-badge">
                                                            🚫 Không khả dụng
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Credits Tab */}
                    {activeTab === 'credits' && (
                        <div className="tab-content">
                            <div className="credits-overview">
                                <div className="credits-balance">
                                    <span className="credits-balance__icon">🌾</span>
                                    <div className="credits-balance__info">
                                        <span className="credits-balance__value">{user.credits?.toLocaleString()}</span>
                                        <span className="credits-balance__label">Lúa hiện tại</span>
                                    </div>
                                    <div className="credits-balance__actions">
                                        <button className="admin-btn admin-btn--primary">
                                            <FiPlus size={16} />
                                            <span>Cộng Lúa</span>
                                        </button>
                                        <button className="admin-btn admin-btn--secondary">
                                            <FiMinus size={16} />
                                            <span>Trừ Lúa</span>
                                        </button>
                                    </div>
                                </div>

                                <div className="credits-summary">
                                    <div className="credits-summary__item">
                                        <span className="credits-summary__label">Tổng đã nhận</span>
                                        <span className="credits-summary__value credits-summary__value--plus">
                                            +{user.totalCreditsEarned?.toLocaleString()}
                                        </span>
                                    </div>
                                    <div className="credits-summary__item">
                                        <span className="credits-summary__label">Tổng đã dùng</span>
                                        <span className="credits-summary__value credits-summary__value--minus">
                                            -{user.totalCreditsSpent?.toLocaleString()}
                                        </span>
                                    </div>
                                </div>
                            </div>

                            <div className="transaction-history">
                                <h3 className="transaction-history__title">Lịch sử giao dịch Lúa</h3>
                                {creditTransactions.length > 0 ? (
                                    <div className="transaction-list">
                                        {creditTransactions.map(tx => (
                                            <div key={tx.id} className="transaction-item">
                                                <div className={`transaction-item__icon ${tx.amount > 0 ? 'transaction-item__icon--plus' : 'transaction-item__icon--minus'}`}>
                                                    {tx.amount > 0 ? <FiPlus size={14} /> : <FiMinus size={14} />}
                                                </div>
                                                <div className="transaction-item__info">
                                                    <span className="transaction-item__description">{tx.description}</span>
                                                    <span className="transaction-item__date">{formatDate(tx.createdAt)}</span>
                                                </div>
                                                <div className={`transaction-item__amount ${tx.amount > 0 ? 'transaction-item__amount--plus' : 'transaction-item__amount--minus'}`}>
                                                    {tx.amount > 0 ? '+' : ''}{tx.amount}
                                                </div>
                                                <div className="transaction-item__balance">
                                                    Số dư: {tx.balance}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    <p className="empty-message">Chưa có giao dịch nào</p>
                                )}
                            </div>
                        </div>
                    )}

                    {/* Activity Tab */}
                    {activeTab === 'activity' && (
                        <div className="tab-content">
                            <h3 className="section-title">Hoạt động gần đây</h3>
                            {activities.length > 0 ? (
                                <div className="activity-timeline">
                                    {activities.map(activity => (
                                        <div key={activity.id} className="activity-item">
                                            <div className="activity-item__dot" />
                                            <div className="activity-item__content">
                                                <span className="activity-item__type">{activity.type}</span>
                                                <span className="activity-item__description">{activity.description}</span>
                                                <span className="activity-item__time">{formatDate(activity.createdAt)}</span>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <p className="empty-message">Chưa có hoạt động nào được ghi nhận</p>
                            )}
                        </div>
                    )}

                    {/* Audit Log Tab */}
                    {activeTab === 'audit' && (
                        <div className="tab-content">
                            <h3 className="section-title">Lịch sử thao tác Admin</h3>
                            {auditLogs.length > 0 ? (
                                <div className="audit-list">
                                    {auditLogs.map(log => (
                                        <div key={log.id} className="audit-item">
                                            <div className="audit-item__header">
                                                <span className="audit-item__action">{log.action}</span>
                                                <span className="audit-item__time">{formatDate(log.createdAt)}</span>
                                            </div>
                                            <p className="audit-item__description">{log.description}</p>
                                            <span className="audit-item__admin">Bởi: {log.adminEmail}</span>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <p className="empty-message">Chưa có thao tác admin nào được ghi nhận</p>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
