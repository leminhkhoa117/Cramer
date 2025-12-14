/**
 * Subscription System Constants
 * 
 * Single source of truth for subscription tiers, Lúa packs, and pricing.
 * Updated: 2025-12-14
 * 
 * IMPORTANT: This file defines the frontend display. The actual limits
 * are stored in the database (subscription_tiers, lua_packs tables).
 */

// =============================================================================
// SUBSCRIPTION TIERS (2 tiers only: Cramerie and Cramerich)
// =============================================================================

export const TIERS = {
  CRAMERIE: 'cramerie',
  CRAMERICH: 'cramerich',
};

export const TIER_INFO = {
  [TIERS.CRAMERIE]: {
    code: 'cramerie',
    name: 'Cramerie',
    nameVi: 'Cramerie',
    emoji: '🌾',
    color: '#22c55e', // green-500
    gradient: 'from-green-400 to-green-600',
    bgClass: 'bg-green-500/10',
    borderClass: 'border-green-500/30',
    priceVnd: 0,
    priceLabel: 'Miễn phí',
    description: 'Bắt đầu hành trình IELTS của bạn',
  },
  [TIERS.CRAMERICH]: {
    code: 'cramerich',
    name: 'Cramerich',
    nameVi: 'Cramerich',
    emoji: '🌻',
    color: '#eab308', // yellow-500
    gradient: 'from-yellow-400 to-amber-500',
    bgClass: 'bg-yellow-500/10',
    borderClass: 'border-yellow-500/30',
    priceVnd: 69000,
    priceLabel: '69,000đ/tháng',
    description: 'Trải nghiệm đầy đủ với AI hỗ trợ',
  },
};

// =============================================================================
// FEATURE COMPARISON TABLE
// =============================================================================

export const FEATURE_CATEGORIES = [
  {
    name: 'Kho đề thi',
    features: [
      {
        name: 'Truy cập đề thi Reading & Listening',
        cramerie: 'Giới hạn',
        cramerich: 'Toàn bộ',
        tooltip: 'Cramerie: Chỉ một số đề mẫu. Cramerich: Toàn bộ kho đề Cambridge.',
      },
      {
        name: 'Truy cập đề thi Writing',
        cramerie: 'Giới hạn (không chấm AI)',
        cramerich: 'Toàn bộ + Chấm AI',
        tooltip: 'Cramerie: Có thể nộp bài nhưng không có chấm điểm AI. Cramerich: Chấm điểm AI chi tiết.',
      },
    ],
  },
  {
    name: 'Hệ thống Lượt chấm',
    features: [
      {
        name: 'Lượt chấm thường',
        cramerie: '60/tháng (20/kỹ năng)',
        cramerich: '60/tháng (20/kỹ năng)',
        tooltip: 'Lượt chấm thường: Làm bài với chấm điểm tự động cơ bản cho Reading/Listening. Giới hạn giống nhau cho cả hai gói.',
      },
      {
        name: 'Lượt chấm nâng cao',
        cramerie: 'Không có',
        cramerich: '30/tháng (3/kỹ năng)',
        tooltip: 'Lượt chấm nâng cao: AI chấm điểm và nhận xét cá nhân hóa (Writing). Chỉ dành cho Cramerich.',
      },
      {
        name: 'Chi phí vượt hạn mức',
        cramerie: '10 Lúa/lượt thường',
        cramerich: '10 Lúa/lượt thường, 20 Lúa/lượt nâng cao',
        tooltip: 'Khi hết lượt miễn phí, bạn có thể dùng Lúa để mua thêm.',
      },
    ],
  },
  {
    name: 'AI Hỗ trợ',
    features: [
      {
        name: 'Chấm bài Writing bằng AI',
        cramerie: '❌',
        cramerich: '✅ Cá nhân hóa',
        tooltip: 'Nhận xét chi tiết về Task Achievement, Coherence, Lexical, Grammar.',
      },
      {
        name: 'Nhận xét cá nhân hóa R/L',
        cramerie: '❌',
        cramerich: '✅',
        tooltip: 'Reading/Listening miễn phí được chấm tự động, nhưng Cramerich có thêm nhận xét AI.',
      },
    ],
  },
  {
    name: 'Sổ tay Từ vựng',
    features: [
      {
        name: 'Số từ vựng tối đa',
        cramerie: '250 từ',
        cramerich: '1,000 từ',
      },
      {
        name: 'Dịch tự động bằng AI',
        cramerie: '10 lần/ngày',
        cramerich: '50 lần/ngày',
      },
    ],
  },
  {
    name: 'Trợ lý Cramer (Chatbot)',
    features: [
      {
        name: 'Số câu hỏi',
        cramerie: '50 lần/tháng',
        cramerich: '500 lần/tháng',
      },
    ],
  },
  {
    name: 'Lúa (Tiền ảo)',
    features: [
      {
        name: 'Lúa khởi tạo',
        cramerie: '50 Lúa',
        cramerich: '100 Lúa',
      },
      {
        name: 'Lúa thưởng hàng tháng',
        cramerie: '0',
        cramerich: '20 Lúa/tháng',
      },
    ],
  },
];

// =============================================================================
// LÚA PACKS (Virtual Currency)
// =============================================================================

export const LUA_PACKS = [
  {
    code: 'lua_100',
    name: 'Túi Lúa Nhỏ',
    emoji: '🌾',
    luaAmount: 100,
    priceVnd: 10000,
    discountPercent: 0,
    pricePer100: 10000,
    description: 'Gói khởi đầu hoàn hảo',
    color: '#22c55e',
  },
  {
    code: 'lua_500',
    name: 'Túi Lúa Vừa',
    emoji: '🌻',
    luaAmount: 500,
    priceVnd: 45000,
    discountPercent: 10,
    pricePer100: 9000,
    description: 'Tiết kiệm 10% - Phổ biến nhất!',
    popular: true,
    color: '#eab308',
  },
  {
    code: 'lua_2000',
    name: 'Bao Lúa Lớn',
    emoji: '🌟',
    luaAmount: 2000,
    priceVnd: 150000,
    discountPercent: 25,
    pricePer100: 7500,
    description: 'Tiết kiệm 25% - Giá trị tốt nhất!',
    bestValue: true,
    color: '#a855f7',
  },
];

// =============================================================================
// ATTEMPT COST CONSTANTS (Lượt chấm)
// =============================================================================

export const ATTEMPT_COSTS = {
  ATTEMPT: 10,      // Lúa per extra Lượt chấm thường
  ATTEMPT_AI: 20,   // Lúa per extra Lượt chấm nâng cao
};

// =============================================================================
// USER-FACING TERMINOLOGY (Vietnamese)
// =============================================================================

export const TERMINOLOGY = {
  ATTEMPT: 'Lượt chấm thường',
  ATTEMPT_AI: 'Lượt chấm nâng cao',
  ATTEMPT_PLURAL: 'Lượt chấm thường',
  ATTEMPT_AI_PLURAL: 'Lượt chấm nâng cao',
  PER_ATTEMPT: '/lượt thường',
  PER_ATTEMPT_AI: '/lượt nâng cao',
};

// =============================================================================
// LIMIT CONSTANTS (for UI display)
// monthlyAttempts = Lượt chấm thường/tháng
// monthlyAttemptAis = Lượt chấm nâng cao/tháng
// =============================================================================

export const LIMITS = {
  cramerie: {
    monthlyAttempts: 60,      // Same as Cramerich
    monthlyAttemptAis: 0,     // No AI grading
    perSkillAttempts: 20,     // Same as Cramerich
    perSkillAttemptAis: 0,    // No AI grading
    dailyTranslations: 10,
    maxVocabulary: 250,
    chatbotMonthly: 50,
    initialLua: 50,
  },
  cramerich: {
    monthlyAttempts: 60,
    monthlyAttemptAis: 30,
    perSkillAttempts: 20,
    perSkillAttemptAis: 3,
    dailyTranslations: 50,
    maxVocabulary: 1000,
    chatbotMonthly: 500,
    initialLua: 100,
    monthlyLuaBonus: 20,
  },
};

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * Get tier info by code.
 */
export function getTierInfo(tierCode) {
  return TIER_INFO[tierCode] || TIER_INFO[TIERS.CRAMERIE];
}

/**
 * Get tier emoji.
 */
export function getTierEmoji(tierCode) {
  return getTierInfo(tierCode).emoji;
}

/**
 * Get tier color.
 */
export function getTierColor(tierCode) {
  return getTierInfo(tierCode).color;
}

/**
 * Check if tier is premium (paid).
 */
export function isPremiumTier(tierCode) {
  return tierCode === TIERS.CRAMERICH;
}

/**
 * Format VND currency.
 */
export function formatVnd(amount) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
}

/**
 * Format number with Vietnamese locale.
 */
export function formatNumber(num) {
  return new Intl.NumberFormat('vi-VN').format(num);
}

// =============================================================================
// DEFAULT EXPORT
// =============================================================================

export default {
  TIERS,
  TIER_INFO,
  FEATURE_CATEGORIES,
  LUA_PACKS,
  ATTEMPT_COSTS,
  TERMINOLOGY,
  LIMITS,
  getTierInfo,
  getTierEmoji,
  getTierColor,
  isPremiumTier,
  formatVnd,
  formatNumber,
};
