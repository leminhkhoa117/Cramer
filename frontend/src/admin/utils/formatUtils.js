/**
 * Format Utilities for Admin CMS
 * Các hàm tiện ích format dữ liệu
 */

/**
 * Format currency to Vietnamese Dong format
 * @param {number} amount - Số tiền cần format
 * @returns {string} Chuỗi tiền tệ đã được format (e.g., "125.500.000 ₫")
 */
export const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
    }).format(amount);
};

/**
 * Format short currency (compact format)
 * @param {number} amount - Số tiền cần format
 * @returns {string} Chuỗi tiền tệ rút gọn (e.g., "125.5M đ")
 */
export const formatShortCurrency = (amount) => {
    if (amount >= 1000000000) {
        return `${(amount / 1000000000).toFixed(1)}B đ`;
    }
    if (amount >= 1000000) {
        return `${(amount / 1000000).toFixed(1)}M đ`;
    }
    if (amount >= 1000) {
        return `${(amount / 1000).toFixed(0)}K đ`;
    }
    return formatCurrency(amount);
};

/**
 * Format date to Vietnamese format
 * @param {string|Date} date - Date string hoặc Date object
 * @param {object} options - Intl.DateTimeFormat options
 * @returns {string} Chuỗi ngày đã được format
 */
export const formatDate = (date, options = {}) => {
    if (!date) return '-';
    const defaultOptions = {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        ...options
    };
    return new Date(date).toLocaleDateString('vi-VN', defaultOptions);
};

/**
 * Format datetime to Vietnamese format
 * @param {string|Date} date - Date string hoặc Date object
 * @returns {string} Chuỗi ngày giờ đã được format
 */
export const formatDateTime = (date) => {
    if (!date) return '-';
    return new Date(date).toLocaleString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
};

/**
 * Format relative time (e.g., "2 giờ trước")
 * @param {string|Date} date - Date string hoặc Date object
 * @returns {string} Chuỗi thời gian tương đối
 */
export const formatRelativeTime = (date) => {
    if (!date) return '-';
    
    const now = new Date();
    const past = new Date(date);
    const diffMs = now - past;
    const diffSeconds = Math.floor(diffMs / 1000);
    const diffMinutes = Math.floor(diffSeconds / 60);
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);
    
    if (diffSeconds < 60) {
        return 'Vừa xong';
    }
    if (diffMinutes < 60) {
        return `${diffMinutes} phút trước`;
    }
    if (diffHours < 24) {
        return `${diffHours} giờ trước`;
    }
    if (diffDays < 7) {
        return `${diffDays} ngày trước`;
    }
    
    return formatDate(date);
};

/**
 * Format number with thousand separators
 * @param {number} num - Số cần format
 * @returns {string} Chuỗi số đã được format
 */
export const formatNumber = (num) => {
    if (num === null || num === undefined) return '0';
    return new Intl.NumberFormat('vi-VN').format(num);
};

/**
 * Format percentage
 * @param {number} value - Giá trị phần trăm
 * @param {number} decimals - Số chữ số thập phân
 * @returns {string} Chuỗi phần trăm
 */
export const formatPercent = (value, decimals = 1) => {
    if (value === null || value === undefined) return '0%';
    return `${value.toFixed(decimals)}%`;
};
