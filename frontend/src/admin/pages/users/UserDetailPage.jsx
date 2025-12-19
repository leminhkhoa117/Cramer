import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ActivityTimeline from '../../components/ActivityTimeline';
import adminApi from '../../api/adminApi';
import {
    FiArrowLeft,
    FiUser,
    FiCreditCard,
    FiDollarSign,
    FiActivity,
    FiSlash,
    FiMail,
    FiPhone,
    FiMapPin,
    FiCalendar,
    FiLogIn,
    FiCheckCircle,
    FiTarget,
    FiBook,
    FiLoader,
    FiAlertTriangle
} from 'react-icons/fi';
import useAdminUsersStore from '../../stores/useAdminUsersStore';
import { AccountStatusBadge, SubscriptionBadge } from '../../components/StatusBadge';
import BaseModal from '../../../components/common/BaseModal';
import '../../css/pages/users/UserDetailPage.css';

export default function UserDetailPage() {
    const { userId } = useParams();
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('profile');

    // Modal states
    const [banModalOpen, setBanModalOpen] = useState(false);
    const [banReason, setBanReason] = useState('');
    const [subscriptionModalOpen, setSubscriptionModalOpen] = useState(false);
    const [newSubscription, setNewSubscription] = useState('');
    const [creditsModalOpen, setCreditsModalOpen] = useState(false);
    const [creditAmount, setCreditAmount] = useState('');
    const [creditAction, setCreditAction] = useState('ADD');
    const [creditReason, setCreditReason] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const [activities, setActivities] = useState([]);
    const [auditLogs, setAuditLogs] = useState([]);
    const [loadingActivities, setLoadingActivities] = useState(false);

    // Zustand store
    const {
        selectedUser: user,
        isLoadingUser,
        error,
        fetchUserById,
        clearSelectedUser,
        updateUserStatus,
        updateUserCredits
    } = useAdminUsersStore();

    // Define fetch functions first (before useEffect)
    const fetchActivities = async () => {
        try {
            setLoadingActivities(true);
            const response = await adminApi.activity.getUserActivities(userId, {
                page: 0,
                size: 20
            });
            setActivities(response.content || []);
        } catch (err) {
            console.error('Lỗi khi tải hoạt động:', err);
        } finally {
            setLoadingActivities(false);
        }
    };

    const fetchAuditLogs = async () => {
        try {
            const response = await adminApi.activity.getAuditLogs(userId, {
                page: 0,
                size: 20
            });
            setAuditLogs(response.content || []);
        } catch (err) {
            console.error('Lỗi khi tải audit logs:', err);
        }
    };

    // Fetch user on mount
    useEffect(() => {
        if (userId) {
            fetchUserById(userId);
        }

        // Cleanup on unmount
        return () => {
            clearSelectedUser();
        };
    }, [userId, fetchUserById, clearSelectedUser]);

    // Fetch activities and audit logs when user is loaded
    useEffect(() => {
        if (userId && user) {
            fetchActivities();
            fetchAuditLogs();
        }
    }, [userId, user]);

    // Loading state
    if (isLoadingUser) {
        return (
            <div className="admin-page user-detail-page">
                <div className="user-loading">
                    <FiLoader size={32} className="spin" />
                    <p>Đang tải thông tin người dùng...</p>
                </div>
            </div>
        );
    }

    // Not found state
    if (!user && !isLoadingUser) {
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

    const isBanned = user.accountStatus === 'BANNED';

    // Handle ban/unban
    const handleBanConfirm = async () => {
        setIsSubmitting(true);
        try {
            const newStatus = isBanned ? 'ACTIVE' : 'BANNED';
            await updateUserStatus(userId, newStatus, banReason);
            setBanModalOpen(false);
            setBanReason('');
            // Refresh user
            await fetchUserById(userId);
        } catch (error) {
            console.error('Error updating status:', error);
            alert('Có lỗi xảy ra. Vui lòng thử lại.');
        } finally {
            setIsSubmitting(false);
        }
    };

    // Handle credits update
    const handleCreditsConfirm = async () => {
        const amount = parseInt(creditAmount);
        if (isNaN(amount) || amount <= 0) {
            alert('Vui lòng nhập số Lúa hợp lệ');
            return;
        }

        setIsSubmitting(true);
        try {
            await updateUserCredits(userId, amount, creditAction, creditReason);
            setCreditsModalOpen(false);
            setCreditAmount('');
            setCreditReason('');
            setCreditAction('ADD');
            // Refresh user
            await fetchUserById(userId);
        } catch (error) {
            console.error('Error updating credits:', error);
            alert('Có lỗi xảy ra. Vui lòng thử lại.');
        } finally {
            setIsSubmitting(false);
        }
    };

    // Handle subscription update (placeholder)
    const handleSubscriptionConfirm = async () => {
        setIsSubmitting(true);
        try {
            // TODO: Implement subscription update API
            alert('Chức năng đang phát triển');
            setSubscriptionModalOpen(false);
        } finally {
            setIsSubmitting(false);
        }
    };

    const tabs = [
        { id: 'profile', label: 'Hồ sơ', icon: FiUser },
        { id: 'subscription', label: 'Gói đăng ký', icon: FiCreditCard },
        { id: 'credits', label: 'Lúa', icon: FiDollarSign },
        { id: 'activity', label: 'Hoạt động', icon: FiActivity },
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
                    <button
                        className={`admin-btn ${isBanned ? 'admin-btn--success' : 'admin-btn--danger'}`}
                        onClick={() => setBanModalOpen(true)}
                    >
                        <FiSlash size={16} />
                        <span>{isBanned ? 'Unban' : 'Ban'}</span>
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
                                                <span className="info-item__value">{user.email || '-'}</span>
                                            </div>
                                        </div>
                                        <div className="info-item">
                                            <FiPhone className="info-item__icon" />
                                            <div className="info-item__content">
                                                <span className="info-item__label">Số điện thoại</span>
                                                <span className="info-item__value">{user.phoneNumber || '-'}</span>
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
                                                <span className="info-item__label">ID người dùng</span>
                                                <span className="info-item__value" style={{ fontSize: '0.75rem', fontFamily: 'monospace' }}>
                                                    {user.id}
                                                </span>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div className="info-section">
                                    <h3 className="info-section__title">Thống kê học tập</h3>
                                    <div className="stats-grid">
                                        <div className="mini-stat">
                                            <FiTarget className="mini-stat__icon" />
                                            <div className="mini-stat__value">{user.totalTests || 0}</div>
                                            <div className="mini-stat__label">Bài thi hoàn thành</div>
                                        </div>
                                        <div className="mini-stat">
                                            <FiBook className="mini-stat__icon" />
                                            <div className="mini-stat__value">{user.totalVocabulary || 0}</div>
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
                                    </div>
                                    <button
                                        className="admin-btn admin-btn--primary"
                                        onClick={() => {
                                            setNewSubscription(user.subscription || 'FREE');
                                            setSubscriptionModalOpen(true);
                                        }}
                                    >
                                        <FiCreditCard size={16} />
                                        <span>Thay đổi gói</span>
                                    </button>
                                </div>

                                {user.subscription !== 'FREE' && (
                                    <div className="subscription-card__details">
                                        <div className="subscription-detail">
                                            <span className="subscription-detail__label">Ngày bắt đầu</span>
                                            <span className="subscription-detail__value">{formatDate(user.subscriptionStart)}</span>
                                        </div>
                                        <div className="subscription-detail">
                                            <span className="subscription-detail__label">Ngày hết hạn</span>
                                            <span className="subscription-detail__value">{formatDate(user.subscriptionEnd)}</span>
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
                        </div>
                    )}

                    {/* Credits Tab */}
                    {activeTab === 'credits' && (
                        <div className="tab-content">
                            <div className="credits-overview">
                                <div className="credits-balance">
                                    <span className="credits-balance__icon">🌾</span>
                                    <div className="credits-balance__info">
                                        <span className="credits-balance__value">{user.credits?.toLocaleString() || 0}</span>
                                        <span className="credits-balance__label">Lúa hiện tại</span>
                                    </div>
                                    <div className="credits-balance__actions">
                                        <button
                                            className="admin-btn admin-btn--primary"
                                            onClick={() => setCreditsModalOpen(true)}
                                        >
                                            <FiDollarSign size={16} />
                                            <span>Thay đổi số Lúa</span>
                                        </button>
                                    </div>
                                </div>
                            </div>

                            <div className="transaction-history">
                                <h3 className="transaction-history__title">Lịch sử giao dịch Lúa</h3>
                                <p className="empty-message">Chưa có dữ liệu lịch sử giao dịch</p>
                            </div>
                        </div>
                    )}

                    {/* Activity Tab */}
                    {activeTab === 'activity' && (
                        <section className="user-detail__section">
                            <h3 style={{
                                color: '#ffffff',
                                fontSize: '1.25rem',
                                fontWeight: 600,
                                marginBottom: '20px',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px'
                            }}>
                                📋 Timeline Hoạt động
                            </h3>
                            <ActivityTimeline
                                activities={activities}
                                loading={loadingActivities}
                            />

                            <h3 style={{
                                color: '#ffffff',
                                fontSize: '1.25rem',
                                fontWeight: 600,
                                marginTop: '48px',
                                marginBottom: '20px',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px'
                            }}>
                                🔒 Nhật ký Admin
                            </h3>
                            <ActivityTimeline
                                activities={auditLogs}
                                loading={loadingActivities}
                            />
                        </section>
                    )}
                </div>
            </div>

            {/* Ban/Unban Modal */}
            <BaseModal
                isOpen={banModalOpen}
                onClose={() => setBanModalOpen(false)}
                title={isBanned ? 'Unban người dùng' : 'Ban người dùng'}
                size="sm"
                className="admin-modal"
                footer={
                    <>
                        <button
                            className="admin-btn admin-btn--secondary"
                            onClick={() => setBanModalOpen(false)}
                            disabled={isSubmitting}
                        >
                            Hủy
                        </button>
                        <button
                            className={`admin-btn ${isBanned ? 'admin-btn--success' : 'admin-btn--danger'}`}
                            onClick={handleBanConfirm}
                            disabled={isSubmitting || (!isBanned && !banReason.trim())}
                        >
                            {isSubmitting ? 'Đang xử lý...' : (isBanned ? 'Xác nhận Unban' : 'Xác nhận Ban')}
                        </button>
                    </>
                }
            >
                <div className="ban-modal-content">
                    <div className="ban-modal-warning">
                        <FiAlertTriangle size={24} className="ban-modal-warning__icon" />
                        <p>
                            {isBanned
                                ? `Bạn có chắc chắn muốn unban người dùng "${user.fullName || user.username}"?`
                                : `Bạn có chắc chắn muốn ban người dùng "${user.fullName || user.username}"?`
                            }
                        </p>
                    </div>
                    {!isBanned && (
                        <div className="form-group">
                            <label className="form-label">Lý do ban <span className="required">*</span></label>
                            <textarea
                                className="form-textarea"
                                placeholder="Nhập lý do ban người dùng..."
                                value={banReason}
                                onChange={(e) => setBanReason(e.target.value)}
                                rows={3}
                            />
                        </div>
                    )}
                </div>
            </BaseModal>

            {/* Credits Modal */}
            <BaseModal
                isOpen={creditsModalOpen}
                onClose={() => setCreditsModalOpen(false)}
                title="Thay đổi số Lúa"
                size="sm"
                className="admin-modal"
                footer={
                    <>
                        <button
                            className="admin-btn admin-btn--secondary"
                            onClick={() => setCreditsModalOpen(false)}
                            disabled={isSubmitting}
                        >
                            Hủy
                        </button>
                        <button
                            className="admin-btn admin-btn--primary"
                            onClick={handleCreditsConfirm}
                            disabled={isSubmitting || !creditAmount || !creditReason.trim()}
                        >
                            {isSubmitting ? 'Đang xử lý...' : 'Xác nhận'}
                        </button>
                    </>
                }
            >
                <div className="credits-modal-content">
                    <div className="current-balance">
                        <span>Số Lúa hiện tại:</span>
                        <strong>🌾 {user.credits?.toLocaleString() || 0}</strong>
                    </div>

                    <div className="form-group">
                        <label className="form-label">Thao tác</label>
                        <div className="credit-action-buttons">
                            <button
                                className={`action-btn ${creditAction === 'ADD' ? 'action-btn--active action-btn--add' : ''}`}
                                onClick={() => setCreditAction('ADD')}
                            >
                                + Cộng Lúa
                            </button>
                            <button
                                className={`action-btn ${creditAction === 'SUBTRACT' ? 'action-btn--active action-btn--subtract' : ''}`}
                                onClick={() => setCreditAction('SUBTRACT')}
                            >
                                − Trừ Lúa
                            </button>
                        </div>
                    </div>

                    <div className="form-group">
                        <label className="form-label">Số Lúa <span className="required">*</span></label>
                        <input
                            type="number"
                            className="form-input"
                            placeholder="Nhập số Lúa..."
                            value={creditAmount}
                            onChange={(e) => setCreditAmount(e.target.value)}
                            min="1"
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Lý do <span className="required">*</span></label>
                        <textarea
                            className="form-textarea"
                            placeholder="Nhập lý do thay đổi..."
                            value={creditReason}
                            onChange={(e) => setCreditReason(e.target.value)}
                            rows={2}
                        />
                    </div>

                    {creditAmount && (
                        <div className="preview-balance">
                            <span>Số Lúa sau khi thay đổi:</span>
                            <strong>
                                🌾 {creditAction === 'ADD'
                                    ? ((user.credits || 0) + parseInt(creditAmount || 0)).toLocaleString()
                                    : Math.max(0, (user.credits || 0) - parseInt(creditAmount || 0)).toLocaleString()
                                }
                            </strong>
                        </div>
                    )}
                </div>
            </BaseModal>

            {/* Subscription Modal */}
            <BaseModal
                isOpen={subscriptionModalOpen}
                onClose={() => setSubscriptionModalOpen(false)}
                title="Thay đổi gói đăng ký"
                size="sm"
                className="admin-modal"
                footer={
                    <>
                        <button
                            className="admin-btn admin-btn--secondary"
                            onClick={() => setSubscriptionModalOpen(false)}
                            disabled={isSubmitting}
                        >
                            Hủy
                        </button>
                        <button
                            className="admin-btn admin-btn--primary"
                            onClick={handleSubscriptionConfirm}
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? 'Đang xử lý...' : 'Xác nhận'}
                        </button>
                    </>
                }
            >
                <div className="subscription-modal-content">
                    <div className="current-subscription">
                        <span>Gói hiện tại:</span>
                        <SubscriptionBadge subscription={user.subscription} />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Chọn gói mới</label>
                        <div className="subscription-options">
                            <label className={`subscription-option ${newSubscription === 'FREE' ? 'subscription-option--selected' : ''}`}>
                                <input
                                    type="radio"
                                    name="subscription"
                                    value="FREE"
                                    checked={newSubscription === 'FREE'}
                                    onChange={(e) => setNewSubscription(e.target.value)}
                                />
                                <div className="subscription-option__content">
                                    <strong>Cramerie (Free)</strong>
                                    <span>Gói miễn phí với các tính năng cơ bản</span>
                                </div>
                            </label>
                            <label className={`subscription-option ${newSubscription === 'CRAMERICH' ? 'subscription-option--selected' : ''}`}>
                                <input
                                    type="radio"
                                    name="subscription"
                                    value="CRAMERICH"
                                    checked={newSubscription === 'CRAMERICH'}
                                    onChange={(e) => setNewSubscription(e.target.value)}
                                />
                                <div className="subscription-option__content">
                                    <strong>Cramerich</strong>
                                    <span>Gói premium với đầy đủ tính năng</span>
                                </div>
                            </label>
                        </div>
                    </div>
                </div>
            </BaseModal>
        </div>
    );
}
