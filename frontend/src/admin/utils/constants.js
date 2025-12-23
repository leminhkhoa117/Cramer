/**
 * Admin CMS Constants
 * Các hằng số sử dụng trong Admin CMS
 */

// =====================
// CONTENT MANAGEMENT
// =====================

// Test statuses
export const TEST_STATUSES = [
    { value: 'DRAFT', label: 'Nháp', color: 'neutral' },
    { value: 'REVIEW', label: 'Đang duyệt', color: 'warning' },
    { value: 'PUBLISHED', label: 'Đã xuất bản', color: 'success' },
    { value: 'ARCHIVED', label: 'Lưu trữ', color: 'info' },
];

// Skill statuses
export const SKILL_STATUSES = {
    empty: { label: 'Chưa có', color: 'neutral' },
    draft: { label: 'Đang soạn', color: 'warning' },
    complete: { label: 'Hoàn thành', color: 'success' },
};

// Question types
export const QUESTION_TYPES = [
    { value: 'FILL_IN_BLANK', label: 'Điền từ' },
    { value: 'TRUE_FALSE_NOT_GIVEN', label: 'True/False/Not Given' },
    { value: 'MULTIPLE_CHOICE', label: 'Trắc nghiệm' },
    { value: 'MATCHING_HEADINGS', label: 'Nối heading' },
    { value: 'MATCHING_INFORMATION', label: 'Nối thông tin' },
    { value: 'SENTENCE_COMPLETION', label: 'Hoàn thành câu' },
    { value: 'SHORT_ANSWER', label: 'Trả lời ngắn' },
    { value: 'SUMMARY_COMPLETION', label: 'Hoàn thành summary' },
    { value: 'DIAGRAM_LABELLING', label: 'Gắn nhãn diagram' },
    { value: 'MAP_LABELLING', label: 'Gắn nhãn map' },
    { value: 'NOTE_COMPLETION', label: 'Hoàn thành note' },
    { value: 'TABLE_COMPLETION', label: 'Hoàn thành bảng' },
    { value: 'PLAN_MAP_DIAGRAM', label: 'Plan/Map/Diagram' },
];

// =====================
// FINANCE MANAGEMENT
// =====================

// Transaction types - values must match database payment_orders.type column
export const TRANSACTION_TYPES = [
    { value: 'SUBSCRIPTION', label: 'Đăng ký/Gia hạn', color: 'success' },
    { value: 'LUA_PACK', label: 'Mua Lúa', color: 'warning' },
    { value: 'REFUND', label: 'Hoàn tiền', color: 'danger' },
];

// Transaction statuses
export const TRANSACTION_STATUSES = [
    { value: 'PAID', label: 'Đã thanh toán', color: 'success' },
    { value: 'PENDING', label: 'Chờ thanh toán', color: 'warning' },
    { value: 'EXPIRED', label: 'Hết hạn', color: 'neutral' },
    { value: 'CANCELLED', label: 'Đã hủy', color: 'danger' },
    { value: 'REFUNDED', label: 'Đã hoàn tiền', color: 'info' },
];

// Payment methods
export const PAYMENT_METHODS = [
    { value: 'BANK_TRANSFER', label: 'Chuyển khoản ngân hàng' },
    { value: 'MOMO', label: 'Ví MoMo' },
    { value: 'ZALOPAY', label: 'ZaloPay' },
    { value: 'VNPAY', label: 'VNPay' },
];

// Time filter options
export const TIME_FILTER_OPTIONS = [
    { value: 'today', label: 'Hôm nay' },
    { value: 'yesterday', label: 'Hôm qua' },
    { value: '7days', label: '7 ngày qua' },
    { value: '30days', label: '30 ngày qua' },
    { value: 'thisMonth', label: 'Tháng này' },
    { value: 'lastMonth', label: 'Tháng trước' },
    { value: 'thisYear', label: 'Năm nay' },
    { value: 'custom', label: 'Tùy chọn' },
];

// =====================
// USER MANAGEMENT
// =====================

// User account statuses
export const USER_STATUSES = [
    { value: 'ACTIVE', label: 'Hoạt động', color: 'success' },
    { value: 'BANNED', label: 'Bị cấm', color: 'danger' },
    { value: 'DEACTIVATED', label: 'Đã khóa', color: 'neutral' },
];

// Subscription tiers
export const SUBSCRIPTION_TIERS = [
    { value: 'FREE', label: 'Miễn phí', color: 'neutral' },
    { value: 'CRAMERIE', label: 'Cramerie', color: 'info' },
    { value: 'CRAMERICH', label: 'Cramerich', color: 'primary' },
];

// =====================
// HELPER FUNCTIONS
// =====================

/**
 * Get status/type object by value
 */
export const getTestStatusConfig = (status) => {
    return TEST_STATUSES.find(s => s.value === status) || TEST_STATUSES[0];
};

export const getTransactionTypeConfig = (type) => {
    return TRANSACTION_TYPES.find(t => t.value === type) || { value: type, label: type, color: 'neutral' };
};

export const getTransactionStatusConfig = (status) => {
    return TRANSACTION_STATUSES.find(s => s.value === status) || { value: status, label: status, color: 'neutral' };
};

export const getUserStatusConfig = (status) => {
    return USER_STATUSES.find(s => s.value === status) || USER_STATUSES[0];
};

export const getSubscriptionTierConfig = (tier) => {
    return SUBSCRIPTION_TIERS.find(t => t.value === tier) || SUBSCRIPTION_TIERS[0];
};
