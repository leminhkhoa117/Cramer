/**
 * Mock Finance Data
 * Dữ liệu giả để phát triển UI Finance Management
 */

// Overview metrics for dashboard
export const mockFinanceOverview = {
  totalRevenue: 125500000,
  totalRevenueChange: 12.5, // % so với kỳ trước
  subscriptionRevenue: 89000000,
  luaRevenue: 36500000,
  
  newSubscriptions: 145,
  subscriptionChange: 8.3,
  renewedSubscriptions: 89,
  churnedSubscriptions: 12,
  churnRate: 3.2,
  
  luaPacksSold: 234,
  luaPacksChange: -2.1,
  averageOrderValue: 153846,
  
  growthRate: 15.2,
  mrr: 89000000, // Monthly Recurring Revenue
  arr: 1068000000, // Annual Recurring Revenue
};

// Revenue chart data (last 30 days)
export const mockRevenueChart = [
  { date: "2025-11-16", total: 3200000, subscriptions: 2200000, lua: 1000000 },
  { date: "2025-11-17", total: 4500000, subscriptions: 3500000, lua: 1000000 },
  { date: "2025-11-18", total: 2800000, subscriptions: 1800000, lua: 1000000 },
  { date: "2025-11-19", total: 5100000, subscriptions: 3600000, lua: 1500000 },
  { date: "2025-11-20", total: 3900000, subscriptions: 2400000, lua: 1500000 },
  { date: "2025-11-21", total: 4200000, subscriptions: 3000000, lua: 1200000 },
  { date: "2025-11-22", total: 3800000, subscriptions: 2500000, lua: 1300000 },
  { date: "2025-11-23", total: 4600000, subscriptions: 3200000, lua: 1400000 },
  { date: "2025-11-24", total: 5200000, subscriptions: 3800000, lua: 1400000 },
  { date: "2025-11-25", total: 3100000, subscriptions: 2100000, lua: 1000000 },
  { date: "2025-11-26", total: 4800000, subscriptions: 3300000, lua: 1500000 },
  { date: "2025-11-27", total: 3500000, subscriptions: 2500000, lua: 1000000 },
  { date: "2025-11-28", total: 4100000, subscriptions: 2900000, lua: 1200000 },
  { date: "2025-11-29", total: 5500000, subscriptions: 4000000, lua: 1500000 },
  { date: "2025-11-30", total: 4300000, subscriptions: 3000000, lua: 1300000 },
  { date: "2025-12-01", total: 4200000, subscriptions: 3000000, lua: 1200000 },
  { date: "2025-12-02", total: 3800000, subscriptions: 2500000, lua: 1300000 },
  { date: "2025-12-03", total: 4900000, subscriptions: 3400000, lua: 1500000 },
  { date: "2025-12-04", total: 3600000, subscriptions: 2400000, lua: 1200000 },
  { date: "2025-12-05", total: 5800000, subscriptions: 4200000, lua: 1600000 },
  { date: "2025-12-06", total: 4100000, subscriptions: 2800000, lua: 1300000 },
  { date: "2025-12-07", total: 3400000, subscriptions: 2200000, lua: 1200000 },
  { date: "2025-12-08", total: 4700000, subscriptions: 3200000, lua: 1500000 },
  { date: "2025-12-09", total: 5300000, subscriptions: 3800000, lua: 1500000 },
  { date: "2025-12-10", total: 4000000, subscriptions: 2700000, lua: 1300000 },
  { date: "2025-12-11", total: 4500000, subscriptions: 3100000, lua: 1400000 },
  { date: "2025-12-12", total: 5100000, subscriptions: 3600000, lua: 1500000 },
  { date: "2025-12-13", total: 3900000, subscriptions: 2600000, lua: 1300000 },
  { date: "2025-12-14", total: 4800000, subscriptions: 3300000, lua: 1500000 },
  { date: "2025-12-15", total: 5600000, subscriptions: 4000000, lua: 1600000 },
  { date: "2025-12-16", total: 4200000, subscriptions: 2900000, lua: 1300000 },
];

// Revenue breakdown by source
export const mockRevenueBreakdown = [
  { name: "Cramerich", value: 62000000, color: "#8B5CF6" },
  { name: "Cramerie", value: 27000000, color: "#06B6D4" },
  { name: "Gói Lúa", value: 36500000, color: "#F59E0B" },
];

// Transaction types
export const transactionTypes = [
  { value: "SUBSCRIPTION_NEW", label: "Đăng ký mới", color: "success" },
  { value: "SUBSCRIPTION_RENEW", label: "Gia hạn", color: "info" },
  { value: "LUA_PURCHASE", label: "Mua Lúa", color: "warning" },
  { value: "REFUND", label: "Hoàn tiền", color: "danger" },
];

// Transaction statuses
export const transactionStatuses = [
  { value: "PAID", label: "Đã thanh toán", color: "success" },
  { value: "PENDING", label: "Chờ thanh toán", color: "warning" },
  { value: "EXPIRED", label: "Hết hạn", color: "neutral" },
  { value: "CANCELLED", label: "Đã hủy", color: "danger" },
  { value: "REFUNDED", label: "Đã hoàn tiền", color: "info" },
];

// Payment methods
export const paymentMethods = [
  { value: "BANK_TRANSFER", label: "Chuyển khoản ngân hàng" },
  { value: "MOMO", label: "Ví MoMo" },
  { value: "ZALOPAY", label: "ZaloPay" },
  { value: "VNPAY", label: "VNPay" },
];

// Mock transactions
export const mockTransactions = [
  {
    id: "txn-001",
    orderCode: "1234567",
    paymentLinkId: "pl_abc123",
    userId: "550e8400-e29b-41d4-a716-446655440001",
    username: "quochuu54",
    userEmail: "quochuu@gmail.com",
    type: "SUBSCRIPTION_NEW",
    productName: "Cramerich Monthly",
    amount: 199000,
    status: "PAID",
    paymentMethod: "BANK_TRANSFER",
    createdAt: "2025-12-15T14:30:00Z",
    paidAt: "2025-12-15T14:32:15Z",
  },
  {
    id: "txn-002",
    orderCode: "1234568",
    paymentLinkId: "pl_abc124",
    userId: "550e8400-e29b-41d4-a716-446655440002",
    username: "thanhpro",
    userEmail: "thanhpro@gmail.com",
    type: "LUA_PURCHASE",
    productName: "Gói 500 Lúa",
    amount: 99000,
    status: "PAID",
    paymentMethod: "MOMO",
    createdAt: "2025-12-15T10:15:00Z",
    paidAt: "2025-12-15T10:15:45Z",
  },
  {
    id: "txn-003",
    orderCode: "1234569",
    paymentLinkId: "pl_abc125",
    userId: "550e8400-e29b-41d4-a716-446655440005",
    username: "vip_student",
    userEmail: "kimanh.vip@gmail.com",
    type: "SUBSCRIPTION_RENEW",
    productName: "Cramerich Yearly",
    amount: 1990000,
    status: "PAID",
    paymentMethod: "BANK_TRANSFER",
    createdAt: "2025-12-14T16:00:00Z",
    paidAt: "2025-12-14T16:05:30Z",
  },
  {
    id: "txn-004",
    orderCode: "1234570",
    paymentLinkId: "pl_abc126",
    userId: "550e8400-e29b-41d4-a716-446655440008",
    username: "ielts_master",
    userEmail: "huong.ielts@gmail.com",
    type: "LUA_PURCHASE",
    productName: "Gói 200 Lúa",
    amount: 49000,
    status: "PAID",
    paymentMethod: "ZALOPAY",
    createdAt: "2025-12-14T11:30:00Z",
    paidAt: "2025-12-14T11:30:20Z",
  },
  {
    id: "txn-005",
    orderCode: "1234571",
    paymentLinkId: "pl_abc127",
    userId: "550e8400-e29b-41d4-a716-446655440010",
    username: "pro_user_hcm",
    userEmail: "phuc.hoang@gmail.com",
    type: "SUBSCRIPTION_NEW",
    productName: "Cramerie Monthly",
    amount: 99000,
    status: "PENDING",
    paymentMethod: "BANK_TRANSFER",
    createdAt: "2025-12-16T08:00:00Z",
    paidAt: null,
  },
  {
    id: "txn-006",
    orderCode: "1234572",
    paymentLinkId: "pl_abc128",
    userId: "550e8400-e29b-41d4-a716-446655440003",
    username: "hoangyen_ielts",
    userEmail: "hoangyen@gmail.com",
    type: "LUA_PURCHASE",
    productName: "Gói 100 Lúa",
    amount: 29000,
    status: "EXPIRED",
    paymentMethod: "VNPAY",
    createdAt: "2025-12-13T09:00:00Z",
    paidAt: null,
  },
  {
    id: "txn-007",
    orderCode: "1234573",
    paymentLinkId: "pl_abc129",
    userId: "550e8400-e29b-41d4-a716-446655440009",
    username: "casual_learner",
    userEmail: "ducle@gmail.com",
    type: "SUBSCRIPTION_RENEW",
    productName: "Cramerie Monthly",
    amount: 99000,
    status: "PAID",
    paymentMethod: "MOMO",
    createdAt: "2025-12-12T15:45:00Z",
    paidAt: "2025-12-12T15:45:30Z",
  },
  {
    id: "txn-008",
    orderCode: "1234574",
    paymentLinkId: "pl_abc130",
    userId: "550e8400-e29b-41d4-a716-446655440012",
    username: "expiring_soon",
    userEmail: "expiring@gmail.com",
    type: "REFUND",
    productName: "Cramerich Monthly - Hoàn tiền",
    amount: -199000,
    status: "REFUNDED",
    paymentMethod: "BANK_TRANSFER",
    createdAt: "2025-12-11T10:00:00Z",
    paidAt: "2025-12-11T14:30:00Z",
    refundReason: "Khách hàng yêu cầu hoàn tiền trong vòng 7 ngày",
  },
  {
    id: "txn-009",
    orderCode: "1234575",
    paymentLinkId: "pl_abc131",
    userId: "550e8400-e29b-41d4-a716-446655440005",
    username: "vip_student",
    userEmail: "kimanh.vip@gmail.com",
    type: "LUA_PURCHASE",
    productName: "Gói 1000 Lúa",
    amount: 179000,
    status: "PAID",
    paymentMethod: "BANK_TRANSFER",
    createdAt: "2025-12-10T20:00:00Z",
    paidAt: "2025-12-10T20:02:00Z",
  },
  {
    id: "txn-010",
    orderCode: "1234576",
    paymentLinkId: "pl_abc132",
    userId: "550e8400-e29b-41d4-a716-446655440001",
    username: "quochuu54",
    userEmail: "quochuu@gmail.com",
    type: "SUBSCRIPTION_RENEW",
    productName: "Cramerich Monthly",
    amount: 199000,
    status: "PAID",
    paymentMethod: "MOMO",
    createdAt: "2025-12-01T09:00:00Z",
    paidAt: "2025-12-01T09:01:00Z",
  },
  {
    id: "txn-011",
    orderCode: "1234577",
    paymentLinkId: "pl_abc133",
    userId: "550e8400-e29b-41d4-a716-446655440008",
    username: "ielts_master",
    userEmail: "huong.ielts@gmail.com",
    type: "SUBSCRIPTION_NEW",
    productName: "Cramerich 6 Months",
    amount: 990000,
    status: "PAID",
    paymentMethod: "BANK_TRANSFER",
    createdAt: "2025-11-25T14:00:00Z",
    paidAt: "2025-11-25T14:05:00Z",
  },
  {
    id: "txn-012",
    orderCode: "1234578",
    paymentLinkId: "pl_abc134",
    userId: "550e8400-e29b-41d4-a716-446655440002",
    username: "thanhpro",
    userEmail: "thanhpro@gmail.com",
    type: "SUBSCRIPTION_NEW",
    productName: "Cramerie Monthly",
    amount: 99000,
    status: "CANCELLED",
    paymentMethod: "ZALOPAY",
    createdAt: "2025-11-20T11:00:00Z",
    paidAt: null,
    cancelReason: "Khách hàng hủy đơn hàng",
  },
];

// Top spenders ranking
export const mockTopSpenders = [
  { rank: 1, userId: "550e8400-e29b-41d4-a716-446655440005", username: "vip_student", fullName: "Phạm Thị Kim Anh", totalSpent: 4560000, transactionCount: 8 },
  { rank: 2, userId: "550e8400-e29b-41d4-a716-446655440001", username: "quochuu54", fullName: "Nguyễn Quốc Hữu", totalSpent: 2890000, transactionCount: 12 },
  { rank: 3, userId: "550e8400-e29b-41d4-a716-446655440008", username: "ielts_master", fullName: "Nguyễn Thị Hương", totalSpent: 2340000, transactionCount: 6 },
  { rank: 4, userId: "550e8400-e29b-41d4-a716-446655440010", username: "pro_user_hcm", fullName: "Hoàng Minh Phúc", totalSpent: 1890000, transactionCount: 5 },
  { rank: 5, userId: "550e8400-e29b-41d4-a716-446655440002", username: "thanhpro", fullName: "Lê Minh Thành", totalSpent: 1450000, transactionCount: 7 },
  { rank: 6, userId: "550e8400-e29b-41d4-a716-446655440009", username: "casual_learner", fullName: "Lê Văn Đức", totalSpent: 890000, transactionCount: 4 },
  { rank: 7, userId: "550e8400-e29b-41d4-a716-446655440012", username: "expiring_soon", fullName: "Trần Thị Expiring", totalSpent: 650000, transactionCount: 3 },
  { rank: 8, userId: "550e8400-e29b-41d4-a716-446655440003", username: "hoangyen_ielts", fullName: "Trần Hoàng Yến", totalSpent: 350000, transactionCount: 2 },
];

// Recent activity for dashboard
export const mockRecentActivity = [
  { id: 1, type: "PAYMENT", description: "quochuu54 đã thanh toán Cramerich Monthly", amount: 199000, createdAt: "2025-12-15T14:32:15Z" },
  { id: 2, type: "PAYMENT", description: "thanhpro đã mua 500 Lúa", amount: 99000, createdAt: "2025-12-15T10:15:45Z" },
  { id: 3, type: "SUBSCRIPTION", description: "vip_student đã gia hạn Cramerich Yearly", amount: 1990000, createdAt: "2025-12-14T16:05:30Z" },
  { id: 4, type: "REFUND", description: "Hoàn tiền cho expiring_soon", amount: -199000, createdAt: "2025-12-11T14:30:00Z" },
  { id: 5, type: "PAYMENT", description: "vip_student đã mua 1000 Lúa", amount: 179000, createdAt: "2025-12-10T20:02:00Z" },
];

// Time filter options
export const timeFilterOptions = [
  { value: "today", label: "Hôm nay" },
  { value: "yesterday", label: "Hôm qua" },
  { value: "7days", label: "7 ngày qua" },
  { value: "30days", label: "30 ngày qua" },
  { value: "thisMonth", label: "Tháng này" },
  { value: "lastMonth", label: "Tháng trước" },
  { value: "thisYear", label: "Năm nay" },
  { value: "custom", label: "Tùy chọn" },
];

// Helper: Format currency
export const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', { 
    style: 'currency', 
    currency: 'VND',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
};

// Helper: Format short currency (e.g., 125.5M)
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

// Helper: Get transaction by ID
export const getTransactionById = (id) => {
  return mockTransactions.find(t => t.id === id);
};
