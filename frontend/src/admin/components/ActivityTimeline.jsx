import React from 'react';
import '../css/pages/activity/ActivityTimeline.css';

// Emoji icons cho từng loại activity - đẹp và trực quan hơn
const ACTIVITY_ICONS = {
    TEST_COMPLETED: '📝',
    VOCAB_SAVED: '📚',
    SUBSCRIPTION_CHANGED: '💎',
    LOGIN: '🔐',
    ACHIEVEMENT_EARNED: '🏆',
    CREDITS_CHANGED: '🌾',
    PROFILE_UPDATED: '👤',
    // Audit log actions
    STATUS_CHANGE: '🔄',
    CREDITS_ADD: '➕',
    CREDITS_SUBTRACT: '➖',
    BAN: '🚫',
    UNBAN: '✅'
};

// CSS class cho icon background gradient
const ACTIVITY_ICON_CLASSES = {
    TEST_COMPLETED: 'activity-item__icon--test',
    VOCAB_SAVED: 'activity-item__icon--vocab',
    SUBSCRIPTION_CHANGED: 'activity-item__icon--subscription',
    LOGIN: 'activity-item__icon--login',
    ACHIEVEMENT_EARNED: 'activity-item__icon--achievement',
    CREDITS_CHANGED: 'activity-item__icon--credits',
    PROFILE_UPDATED: 'activity-item__icon--default',
    STATUS_CHANGE: 'activity-item__icon--subscription',
    CREDITS_ADD: 'activity-item__icon--credits',
    CREDITS_SUBTRACT: 'activity-item__icon--credits',
    BAN: 'activity-item__icon--test',
    UNBAN: 'activity-item__icon--vocab'
};

export default function ActivityTimeline({ activities, loading }) {
    const groupedActivities = groupByDate(activities);

    if (loading) {
        return <div className="activity-timeline-loading">⏳ Đang tải hoạt động...</div>;
    }

    if (!activities || activities.length === 0) {
        return <div className="activity-timeline-empty">📭 Không có hoạt động nào</div>;
    }

    return (
        <div className="activity-timeline">
            {Object.entries(groupedActivities).map(([date, items]) => (
                <div key={date} className="activity-group">
                    <div className="activity-group__date">{date}</div>
                    <div className="activity-group__items">
                        {items.map(activity => {
                            const activityType = activity.activityType || activity.action;
                            const icon = ACTIVITY_ICONS[activityType] || '📌';
                            const iconClass = ACTIVITY_ICON_CLASSES[activityType] || 'activity-item__icon--default';

                            return (
                                <div key={activity.id} className="activity-item">
                                    <div className={`activity-item__icon ${iconClass}`}>
                                        {icon}
                                    </div>
                                    <div className="activity-item__content">
                                        <div className="activity-item__title">
                                            {activity.title || activity.description || formatActionTitle(activity.action)}
                                        </div>
                                        {activity.description && activity.title && (
                                            <div className="activity-item__description">
                                                {activity.description}
                                            </div>
                                        )}
                                        {!activity.title && activity.action && (
                                            <div className="activity-item__description">
                                                {activity.adminEmail && `Bởi: ${activity.adminEmail}`}
                                            </div>
                                        )}
                                    </div>
                                    <div className="activity-item__time">
                                        <span className="activity-item__time-date">
                                            {formatDate(activity.createdAt)}
                                        </span>
                                        <span className="activity-item__time-hour">
                                            {formatTime(activity.createdAt)}
                                        </span>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            ))}
        </div>
    );
}

// Group activities by date label
function groupByDate(activities) {
    if (!activities || activities.length === 0) return {};

    const groups = {};
    const today = new Date().toDateString();
    const yesterday = new Date(Date.now() - 86400000).toDateString();

    activities.forEach(activity => {
        const date = new Date(activity.createdAt).toDateString();
        let label;

        if (date === today) {
            label = '📅 Hôm nay';
        } else if (date === yesterday) {
            label = '📅 Hôm qua';
        } else {
            label = '📅 ' + new Date(activity.createdAt).toLocaleDateString('vi-VN', {
                weekday: 'long',
                day: '2-digit',
                month: '2-digit',
                year: 'numeric'
            });
        }

        if (!groups[label]) {
            groups[label] = [];
        }
        groups[label].push(activity);
    });

    return groups;
}

// Format date: DD/MM/YYYY
function formatDate(dateString) {
    if (!dateString) return '';
    return new Date(dateString).toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
}

// Format time: HH:MM
function formatTime(dateString) {
    if (!dateString) return '';
    return new Date(dateString).toLocaleTimeString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Format action title for audit logs
function formatActionTitle(action) {
    const actionTitles = {
        'STATUS_CHANGE': 'Thay đổi trạng thái',
        'CREDITS_ADD': 'Thêm Lúa',
        'CREDITS_SUBTRACT': 'Trừ Lúa',
        'SUBSCRIPTION_CHANGE': 'Thay đổi gói đăng ký',
        'PROFILE_UPDATE': 'Cập nhật hồ sơ',
        'BAN': 'Khóa tài khoản',
        'UNBAN': 'Mở khóa tài khoản'
    };
    return actionTitles[action] || action || 'Hoạt động';
}
