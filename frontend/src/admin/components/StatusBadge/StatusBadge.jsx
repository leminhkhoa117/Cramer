import React from 'react';
import './StatusBadge.css';

/**
 * StatusBadge - Component hiển thị trạng thái với màu sắc tương ứng
 * 
 * @param {string} status - Giá trị trạng thái
 * @param {string} type - Loại badge: 'account' | 'subscription' | 'transaction' | 'custom'
 * @param {string} variant - Variant color: 'success' | 'warning' | 'danger' | 'info' | 'primary' | 'neutral'
 * @param {string} size - Size: 'sm' | 'md' | 'lg'
 * @param {boolean} dot - Hiển thị dot indicator
 */
export default function StatusBadge({
    status,
    type = 'custom',
    variant,
    size = 'md',
    dot = false,
    children,
}) {
    // Get variant based on status and type
    const getVariant = () => {
        if (variant) return variant;

        const statusMappings = {
            // Account Status
            ACTIVE: 'success',
            INACTIVE: 'warning',
            SUSPENDED: 'warning',
            BANNED: 'danger',

            // Subscription Status
            EXPIRED: 'danger',
            CANCELLED: 'neutral',

            // Subscription Types
            FREE: 'neutral',
            CRAMERIE: 'info',
            CRAMERICH: 'primary',

            // Transaction Status
            PAID: 'success',
            PENDING: 'warning',
            FAILED: 'danger',
            REFUNDED: 'info',

            // Generic
            SUCCESS: 'success',
            ERROR: 'danger',
            WARNING: 'warning',
            INFO: 'info',
        };

        return statusMappings[status?.toUpperCase()] || 'neutral';
    };

    // Get display label based on status
    const getLabel = () => {
        if (children) return children;

        const labelMappings = {
            // Account Status
            ACTIVE: 'Hoạt động',
            INACTIVE: 'Không hoạt động',
            SUSPENDED: 'Tạm khóa',
            BANNED: 'Bị cấm',

            // Subscription Status (when used as subscription status)
            EXPIRED: 'Hết hạn',
            CANCELLED: 'Đã hủy',

            // Subscription Types
            FREE: 'Free',
            CRAMERIE: 'Cramerie',
            CRAMERICH: 'Cramerich',

            // Transaction Status
            PAID: 'Đã thanh toán',
            PENDING: 'Chờ xử lý',
            FAILED: 'Thất bại',
            REFUNDED: 'Hoàn tiền',
        };

        return labelMappings[status?.toUpperCase()] || status || 'N/A';
    };

    return (
        <span className={`status-badge status-badge--${getVariant()} status-badge--${size}`}>
            {dot && <span className="status-badge__dot" />}
            <span className="status-badge__label">{getLabel()}</span>
        </span>
    );
}

/**
 * AccountStatusBadge - Preset cho account status
 */
export function AccountStatusBadge({ status, ...props }) {
    return <StatusBadge status={status} type="account" dot {...props} />;
}

/**
 * SubscriptionBadge - Preset cho subscription type
 */
export function SubscriptionBadge({ subscription, ...props }) {
    return <StatusBadge status={subscription} type="subscription" {...props} />;
}

/**
 * SubscriptionStatusBadge - Preset cho subscription status
 */
export function SubscriptionStatusBadge({ status, ...props }) {
    return <StatusBadge status={status} type="subscription" dot {...props} />;
}

/**
 * TransactionStatusBadge - Preset cho transaction status
 */
export function TransactionStatusBadge({ status, ...props }) {
    return <StatusBadge status={status} type="transaction" dot {...props} />;
}
