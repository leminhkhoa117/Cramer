import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  FaChartBar, FaStore, FaHistory, FaChevronRight,
  FaCrown, FaStar, FaLeaf, FaCheckCircle, FaTimesCircle,
  FaClock, FaCoins, FaRobot, FaComments, FaArrowUp,
  FaBook, FaLanguage, FaExternalLinkAlt, FaCreditCard,
  FaDesktop, FaMobileAlt, FaTabletAlt, FaSignOutAlt,
  FaToggleOn, FaToggleOff, FaInfoCircle, FaExclamationTriangle
} from 'react-icons/fa';
import { FiLoader, FiRefreshCw, FiLogOut, FiCheck, FiMonitor, FiSmartphone } from 'react-icons/fi';
import { subscriptionApi, paymentApi } from '../api/backendApi';
import { showErrorToast, showSuccessToast } from '../utils/toast';
import {
  TIER_INFO, TIERS, LIMITS, LUA_PACKS, ATTEMPT_COSTS, TERMINOLOGY,
  formatVnd
} from '../constants/subscription';
import CreditHistoryList from '../components/CreditHistoryList';
import LuaPurchaseModal from '../components/LuaPurchaseModal';
import ConfirmationModal from '../components/ConfirmationModal';
import '../css/common/sidebar-layout.css';
import '../css/subscription-page.css';

// =============================================================================
// CONSTANTS & CONFIGS
// =============================================================================

// Tab definitions
const subscriptionTabs = [
  { id: 'limits', label: 'Hạn mức', icon: FaChartBar },
  { id: 'packages', label: 'Các gói khác', icon: FaStore },
  { id: 'history', label: 'Lịch sử', icon: FaHistory },
];

// Tier styling config
const TIER_CONFIG = {
  cramerie: {
    emoji: '🌾',
    icon: FaLeaf,
    colorClass: 'tier-cramerie',
    gradient: 'linear-gradient(135deg, #22c55e, #16a34a)',
    badgeColor: '#22c55e',
    name: 'Cramerie',
    description: 'Gói miễn phí dành cho người mới bắt đầu'
  },
  cramerich: {
    emoji: '🌻',
    icon: FaStar,
    colorClass: 'tier-cramerich',
    gradient: 'linear-gradient(135deg, #f59e0b, #d97706)',
    badgeColor: '#f59e0b',
    name: 'Cramerich',
    description: 'Gói trả phí với đầy đủ tính năng AI'
  }
};

// Animation variants
const tabContentVariants = {
  hidden: { opacity: 0, y: 10 },
  visible: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -10 }
};

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

// Format date in Vietnamese
const formatDate = (dateString) => {
  if (!dateString) return 'Không xác định';
  const date = new Date(dateString);
  return date.toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
};

// Format relative time
const formatRelativeTime = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = date - now;
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays < 0) return 'Đã hết hạn';
  if (diffDays === 0) return 'Hết hạn hôm nay';
  if (diffDays === 1) return 'Còn 1 ngày';
  return `Còn ${diffDays} ngày`;
};

// =============================================================================
// HELPER COMPONENTS
// =============================================================================

// Progress Bar Component
const ProgressBar = ({ used, limit, isUnlimited, colorClass }) => {
  const percent = isUnlimited ? 100 : (limit > 0 ? Math.min(100, (used / limit) * 100) : 0);
  const isOverLimit = !isUnlimited && used > limit;

  return (
    <div className="sub-progress-bar">
      <div
        className={`sub-progress-bar__fill ${colorClass} ${isOverLimit ? 'sub-progress-bar__fill--over' : ''}`}
        style={{ width: `${percent}%` }}
      />
    </div>
  );
};

// Usage Card Component
const UsageCard = ({ title, icon: Icon, used, limit, isUnlimited, resetInfo, colorClass, luaCost }) => {
  const displayUsage = isUnlimited
    ? 'Không giới hạn'
    : `${used}/${limit}`;
  const remaining = isUnlimited ? null : Math.max(0, limit - used);
  const isOverLimit = !isUnlimited && used >= limit;

  return (
    <div className={`sub-usage-card ${colorClass}`}>
      <div className="sub-usage-card__header">
        <div className="sub-usage-card__icon">
          <Icon />
        </div>
        <div className="sub-usage-card__title">{title}</div>
      </div>

      <div className="sub-usage-card__body">
        <div className="sub-usage-card__value">{displayUsage}</div>
        {!isUnlimited && remaining !== null && (
          <div className={`sub-usage-card__remaining ${isOverLimit ? 'sub-usage-card__remaining--warn' : ''}`}>
            Còn lại: <strong>{remaining}</strong>
          </div>
        )}
        <ProgressBar
          used={used}
          limit={limit}
          isUnlimited={isUnlimited}
          colorClass={colorClass}
        />
      </div>

      <div className="sub-usage-card__footer">
        <div className="sub-usage-card__reset">
          <FaClock className="sub-usage-card__reset-icon" />
          <span>{resetInfo}</span>
        </div>
        {luaCost && !isUnlimited && (
          <div className="sub-usage-card__cost">
            Vượt hạn: {luaCost} 🌾/lượt
          </div>
        )}
      </div>
    </div>
  );
};

// Status Badge Component
const StatusBadge = ({ status }) => {
  const statusConfig = {
    ACTIVE: { label: 'Đang hoạt động', icon: FaCheckCircle, className: 'sub-status--active' },
    EXPIRED: { label: 'Đã hết hạn', icon: FaTimesCircle, className: 'sub-status--expired' },
    CANCELLED: { label: 'Đã hủy', icon: FaTimesCircle, className: 'sub-status--cancelled' }
  };

  const config = statusConfig[status] || statusConfig.ACTIVE;
  const Icon = config.icon;

  return (
    <span className={`sub-status-badge ${config.className}`}>
      <Icon />
      {config.label}
    </span>
  );
};

// AI Grading Toggle Card Component
const AiGradingToggleCard = ({
  isEnabled,
  isProcessing,
  onToggle
}) => {
  return (
    <div className="sl-card sub-ai-toggle-card">
      <div className="sub-ai-toggle-card__content">
        <div className="sub-ai-toggle-card__info">
          <div className="sub-ai-toggle-card__header">
            <FaRobot className="sub-ai-toggle-card__icon" />
            <h3 className="sub-ai-toggle-card__title">Lượt chấm nâng cao</h3>
          </div>
          <p className="sub-ai-toggle-card__description">
            {isEnabled ? (
              <>Các lượt chấm sẽ có thêm sự hỗ trợ của AI với phản hồi chi tiết và band điểm ước tính, bám sát các tiêu chí chấm bài của IELTS.</>
            ) : (
              <>Bài Writing sẽ chỉ được lưu lại, không có nhận xét và điểm số từ AI.</>
            )}
          </p>
        </div>

        <button
          type="button"
          className={`sub-ai-toggle-btn ${isEnabled ? 'sub-ai-toggle-btn--on' : 'sub-ai-toggle-btn--off'}`}
          onClick={() => onToggle(!isEnabled)}
          disabled={isProcessing}
          aria-label={isEnabled ? 'Tắt chấm bài AI' : 'Bật chấm bài AI'}
        >
          {isProcessing ? (
            <FiLoader className="sub-spinner" />
          ) : isEnabled ? (
            <FaToggleOn className="sub-ai-toggle-btn__icon" />
          ) : (
            <FaToggleOff className="sub-ai-toggle-btn__icon" />
          )}
          <span className="sub-ai-toggle-btn__label">
            {isEnabled ? 'Đang bật' : 'Đang tắt'}
          </span>
        </button>
      </div>

      {/* Info box about costs */}
      <div className="sub-ai-toggle-card__info-box">
        <FaInfoCircle />
        <span>
          Khi công tắc được bật, mỗi lần chấm bài sẽ tiêu hao <strong>1 {TERMINOLOGY.ATTEMPT_AI}</strong>.
          Khi hết số {TERMINOLOGY.ATTEMPT_AI} trong tháng, mỗi lượt chấm tiếp theo với trạng thái công tắc được bật sẽ tiêu hao <strong>{ATTEMPT_COSTS.ATTEMPT_AI} Lúa</strong>.
        </span>
      </div>
    </div>
  );
};



// Lúa Pack Card Component
const LuaPackCard = ({ pack, isProcessing, onPurchase }) => {
  return (
    <div className={`sub-lua-pack ${pack.popular ? 'sub-lua-pack--popular' : ''} ${pack.bestValue ? 'sub-lua-pack--best' : ''}`}>
      {pack.popular && <div className="sub-lua-pack__badge">Phổ biến</div>}
      {pack.bestValue && <div className="sub-lua-pack__badge sub-lua-pack__badge--best">Giá tốt nhất</div>}

      <div className="sub-lua-pack__emoji">{pack.emoji}</div>
      <h3 className="sub-lua-pack__name">{pack.name}</h3>
      <div className="sub-lua-pack__amount">
        <span className="sub-lua-pack__lua">{pack.luaAmount.toLocaleString()}</span>
        <span className="sub-lua-pack__unit">🌾 Lúa</span>
      </div>

      {pack.discountPercent > 0 && (
        <div className="sub-lua-pack__discount">
          Tiết kiệm {pack.discountPercent}%
        </div>
      )}

      <p className="sub-lua-pack__desc">{pack.description}</p>

      <div className="sub-lua-pack__price">{formatVnd(pack.priceVnd)}</div>

      <button
        className="sub-lua-pack__btn"
        onClick={onPurchase}
        disabled={isProcessing}
      >
        {isProcessing ? (
          <>
            <FiLoader className="sub-spinner" />
            Đang xử lý...
          </>
        ) : (
          <>
            <FaCreditCard />
            Mua ngay
          </>
        )}
      </button>
    </div>
  );
};

// Payment History Item
const PaymentHistoryItem = ({ payment }) => {
  const statusConfig = {
    PAID: { label: 'Thành công', className: 'sub-payment--paid' },
    PENDING: { label: 'Đang xử lý', className: 'sub-payment--pending' },
    CANCELLED: { label: 'Đã hủy', className: 'sub-payment--cancelled' },
    EXPIRED: { label: 'Hết hạn', className: 'sub-payment--expired' }
  };

  const config = statusConfig[payment.status] || statusConfig.PENDING;

  return (
    <div className="sub-payment-item">
      <div className="sub-payment-item__info">
        <div className="sub-payment-item__desc">
          {payment.description || (payment.type === 'SUBSCRIPTION' ? 'Nâng cấp gói' : 'Mua Lúa')}
        </div>
        <div className="sub-payment-item__date">
          {formatDate(payment.paidAt || payment.createdAt)}
        </div>
      </div>
      <div className="sub-payment-item__amount">
        {formatVnd(payment.amountVnd)}
      </div>
      <span className={`sub-payment-status ${config.className}`}>
        {config.label}
      </span>
    </div>
  );
};

// Tier Upgrade Card - Collapsible with positive language
const TierUpgradeCard = ({ onUpgrade, isProcessing }) => {
  const [showDetails, setShowDetails] = useState(false);
  const cramerichInfo = TIER_INFO[TIERS.CRAMERICH];
  const cramerichLimits = LIMITS.cramerich;

  return (
    <div className="sub-upgrade-card">
      <div className="sub-upgrade-card__header">
        <span className="sub-upgrade-card__emoji">🌻</span>
        <h3 className="sub-upgrade-card__title">Nâng cấp để mở khóa</h3>
        <p className="sub-upgrade-card__subtitle">Trải nghiệm trọn vẹn cùng Cramerich</p>
      </div>

      {/* Key Benefits - Always visible */}
      <div className="sub-upgrade-card__highlights">
        <div className="sub-upgrade-highlight">
          <FaStar className="sub-upgrade-highlight__icon sub-upgrade-highlight__icon--gold" />
          <span>Toàn bộ kho đề Cambridge</span>
        </div>
        <div className="sub-upgrade-highlight">
          <FaRobot className="sub-upgrade-highlight__icon sub-upgrade-highlight__icon--blue" />
          <span>Chấm bài Writing bằng AI</span>
        </div>
        <div className="sub-upgrade-highlight">
          <FaComments className="sub-upgrade-highlight__icon sub-upgrade-highlight__icon--purple" />
          <span>Hỗ trợ học tập cá nhân hóa</span>
        </div>
      </div>

      {/* Expandable Details */}
      <button
        type="button"
        className="sub-upgrade-card__toggle"
        onClick={() => setShowDetails(!showDetails)}
        aria-expanded={showDetails}
      >
        <span>Xem chi tiết hạn mức</span>
        <FaChevronRight className={`sub-upgrade-card__toggle-icon ${showDetails ? 'sub-upgrade-card__toggle-icon--open' : ''}`} />
      </button>

      <AnimatePresence>
        {showDetails && (
          <motion.div
            className="sub-upgrade-card__details"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
          >
            <div className="sub-upgrade-card__features">
              <div className="sub-upgrade-feature">
                <FaCheckCircle className="sub-upgrade-feature__icon" />
                <span>{cramerichLimits.monthlyAttempts} {TERMINOLOGY.ATTEMPT}/tháng</span>
              </div>
              <div className="sub-upgrade-feature">
                <FaCheckCircle className="sub-upgrade-feature__icon" />
                <span>{cramerichLimits.monthlyAttemptAis} {TERMINOLOGY.ATTEMPT_AI}/tháng</span>
              </div>
              <div className="sub-upgrade-feature">
                <FaCheckCircle className="sub-upgrade-feature__icon" />
                <span>{cramerichLimits.chatbotMonthly} câu hỏi chatbot/tháng</span>
              </div>
              <div className="sub-upgrade-feature">
                <FaCheckCircle className="sub-upgrade-feature__icon" />
                <span>{cramerichLimits.monthlyTranslations} lượt dịch/tháng</span>
              </div>
              <div className="sub-upgrade-feature">
                <FaCheckCircle className="sub-upgrade-feature__icon" />
                <span>Tối đa {cramerichLimits.maxVocabulary.toLocaleString()} từ vựng</span>
              </div>
              <div className="sub-upgrade-feature">
                <FaCheckCircle className="sub-upgrade-feature__icon" />
                <span>+{cramerichLimits.monthlyLuaBonus} Lúa mỗi tháng</span>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="sub-upgrade-card__price">
        <span className="sub-upgrade-card__price-value">{cramerichInfo.priceLabel}</span>
      </div>

      <button
        className="sub-upgrade-card__btn"
        onClick={() => onUpgrade(TIERS.CRAMERICH)}
        disabled={isProcessing}
      >
        {isProcessing ? (
          <>
            <FiLoader className="sub-spinner" />
            Đang xử lý...
          </>
        ) : (
          <>
            <FaArrowUp />
            Nâng cấp ngay
          </>
        )}
      </button>
    </div>
  );
};

// =============================================================================
// MAIN COMPONENT
// =============================================================================

export default function SubscriptionPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('limits');
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState(null);
  const [error, setError] = useState(null);
  const [selectedPackage, setSelectedPackage] = useState(null);
  const [processingPayment, setProcessingPayment] = useState(null);

  // AI Grading Toggle state
  const [aiGradingEnabled, setAiGradingEnabled] = useState(true);
  const [aiGradingProcessing, setAiGradingProcessing] = useState(false);

  // Mock sessions data - will be replaced with API calls
  const [sessions] = useState([
    {
      id: '1',
      device: 'Chrome trên Windows',
      location: 'Hồ Chí Minh, Việt Nam',
      lastActive: 'Đang hoạt động',
      isCurrent: true,
      icon: FiMonitor
    },
    {
      id: '2',
      device: 'Safari trên iPhone',
      location: 'Hà Nội, Việt Nam',
      lastActive: '2 giờ trước',
      isCurrent: false,
      icon: FiSmartphone
    }
  ]);

  // Handle session logout
  const handleRevokeSession = (sessionId) => {
    // TODO: Implement API call to revoke session
    showSuccessToast('Đã đăng xuất thiết bị');
  };

  const handleRevokeAllSessions = () => {
    // TODO: Implement API call to revoke all sessions except current
    showSuccessToast('Đã đăng xuất tất cả thiết bị khác');
  };

  // Fetch subscription status on mount
  useEffect(() => {
    fetchSubscriptionStatus();
  }, []);

  const fetchSubscriptionStatus = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await subscriptionApi.getMyStatus();
      setStatus(response.data);
      // Set AI grading enabled state from subscription info
      if (response.data?.subscription?.aiGradingEnabled !== undefined) {
        setAiGradingEnabled(response.data.subscription.aiGradingEnabled);
      }
    } catch (err) {
      console.error('Failed to fetch subscription status:', err);
      setError('Không thể tải thông tin đăng ký. Vui lòng thử lại.');
      showErrorToast('Không thể tải thông tin đăng ký');
    } finally {
      setLoading(false);
    }
  };

  // Handle AI Grading toggle
  const handleAiGradingToggle = async (enabled) => {
    setAiGradingProcessing(true);
    try {
      await subscriptionApi.setAiGradingEnabled(enabled);
      setAiGradingEnabled(enabled);
      showSuccessToast(enabled ? 'Đã bật chấm bài AI' : 'Đã tắt chấm bài AI');
    } catch (err) {
      console.error('Failed to toggle AI grading:', err);
      showErrorToast(err.response?.data?.message || 'Không thể thay đổi cài đặt');
    } finally {
      setAiGradingProcessing(false);
    }
  };

  // Handle Lúa pack purchase
  const handlePurchaseLua = async (pack) => {
    setProcessingPayment(`lua-${pack.code}`);
    try {
      const response = await paymentApi.createLuaPackPayment(pack.luaAmount, pack.priceVnd);
      if (response.data?.checkoutUrl) {
        showSuccessToast('Đang chuyển đến trang thanh toán...');
        window.location.href = response.data.checkoutUrl;
      } else {
        showErrorToast('Không thể tạo link thanh toán');
      }
    } catch (error) {
      console.error('Payment error:', error);
      showErrorToast(error.response?.data?.message || 'Có lỗi xảy ra');
    } finally {
      setProcessingPayment(null);
      setSelectedPackage(null);
    }
  };

  // Handle tier upgrade
  const handleUpgrade = async (tierCode) => {
    setProcessingPayment(`tier-${tierCode}`);
    try {
      const response = await paymentApi.createSubscriptionPayment(null, tierCode);
      if (response.data?.checkoutUrl) {
        showSuccessToast('Đang chuyển đến trang thanh toán...');
        window.location.href = response.data.checkoutUrl;
      } else {
        showErrorToast('Không thể tạo link thanh toán');
      }
    } catch (error) {
      console.error('Upgrade error:', error);
      showErrorToast(error.response?.data?.message || 'Có lỗi xảy ra');
    } finally {
      setProcessingPayment(null);
    }
  };

  // Loading state
  if (loading) {
    return (
      <div className="sl-page subscription-page">
        <div className="sub-loading">
          <FiLoader className="sub-loading__spinner" />
          <p>Đang tải thông tin đăng ký...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="sl-page subscription-page">
        <div className="sl-error">
          <FaTimesCircle style={{ fontSize: '2rem', marginBottom: '1rem' }} />
          <h2>Đã xảy ra lỗi</h2>
          <p>{error}</p>
          <button onClick={fetchSubscriptionStatus} className="sl-btn sl-btn--primary" style={{ marginTop: '1rem' }}>
            <FiRefreshCw /> Thử lại
          </button>
        </div>
      </div>
    );
  }

  // No data
  if (!status) {
    return null;
  }

  const tierCode = status?.tier?.code || 'cramerie';
  const tierConfig = TIER_CONFIG[tierCode] || TIER_CONFIG.cramerie;
  const TierIcon = tierConfig.icon;
  const isFreeTier = status?.tier?.isFree;
  const canUpgrade = tierCode === 'cramerie';

  return (
    <div className="sl-page subscription-page">
      <div className="sl-layout container">
        {/* ==================== LEFT SIDEBAR ==================== */}
        <aside className="sl-sidebar">
          {/* Cover with gradient */}
          <div className="sl-sidebar__cover" style={{ background: tierConfig.gradient }}>
            <div className="sub-tier-badge">
              <span className="sub-tier-badge__emoji">{tierConfig.emoji}</span>
            </div>
          </div>

          {/* Sidebar Header */}
          <div className="sl-sidebar__header sl-sidebar__header--centered">
            <h1 className="sl-sidebar__title">{status?.tier?.name || 'Cramerie'}</h1>
            <p className="sl-sidebar__subtitle">{tierConfig.description}</p>
            <StatusBadge status={status?.subscription?.status} />
          </div>

          {/* Sidebar Navigation Tabs */}
          <nav className="sl-sidebar__nav">
            {subscriptionTabs.map(tab => {
              const IconComponent = tab.icon;
              return (
                <button
                  key={tab.id}
                  type="button"
                  className={`sl-sidebar__nav-btn ${activeTab === tab.id ? 'active' : ''}`}
                  onClick={() => setActiveTab(tab.id)}
                >
                  <IconComponent />
                  <span>{tab.label}</span>
                  <FaChevronRight className="sl-sidebar__nav-arrow" />
                </button>
              );
            })}
          </nav>

          {/* Lúa Balance Section in Sidebar */}
          <div className="sl-sidebar__section">
            <div className="sl-sidebar__section-header">
              <div className="sl-sidebar__section-title">
                <FaCoins />
                <span>Số dư Lúa</span>
              </div>
            </div>
            <div className="sub-lua-balance-mini">
              <span className="sub-lua-balance-mini__emoji">🌾</span>
              <span className="sub-lua-balance-mini__value">{status?.credits?.balance || 0}</span>
              <span className="sub-lua-balance-mini__unit">Lúa</span>
            </div>
            <div className="sub-lua-stats-mini">
              <div className="sub-lua-stat-mini">
                <span className="sub-lua-stat-mini__label">Đã nhận</span>
                <span className="sub-lua-stat-mini__value sub-lua-stat-mini__value--earned">
                  +{status?.credits?.lifetimeEarned || 0}
                </span>
              </div>
              <div className="sub-lua-stat-mini">
                <span className="sub-lua-stat-mini__label">Đã dùng</span>
                <span className="sub-lua-stat-mini__value sub-lua-stat-mini__value--spent">
                  -{status?.credits?.lifetimeSpent || 0}
                </span>
              </div>
            </div>
          </div>
        </aside>

        {/* ==================== RIGHT CONTENT AREA ==================== */}
        <main className="sl-content">
          <AnimatePresence mode="wait">
            {/* ==================== TAB 1: Hạn mức ==================== */}
            {activeTab === 'limits' && (
              <motion.div
                key="limits"
                className="sl-tab-panel"
                variants={tabContentVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
                transition={{ duration: 0.3 }}
              >
                {/* Subscription Details Card */}
                <div className="sl-card">
                  <div className="sl-card__header">
                    <div className="sl-card__header-left">
                      <h2 className="sl-card__title">
                        <TierIcon />
                        Chi tiết gói đăng ký
                      </h2>
                      <p className="sl-card__description">
                        Thông tin về gói {status?.tier?.name} của bạn
                      </p>
                    </div>
                    {canUpgrade && (
                      <button
                        className="sl-btn sl-btn--primary"
                        onClick={() => navigate('/pricing')}
                      >
                        <FaArrowUp /> Nâng cấp
                      </button>
                    )}
                  </div>

                  <div className="sub-details-grid">
                    <div className="sub-detail">
                      <span className="sub-detail__label">Giá gói</span>
                      <span className="sub-detail__value">
                        {isFreeTier ? 'Miễn phí' : `${formatVnd(status?.tier?.priceVnd)}/tháng`}
                      </span>
                    </div>

                    <div className="sub-detail">
                      <span className="sub-detail__label">Ngày bắt đầu</span>
                      <span className="sub-detail__value">
                        {formatDate(status?.subscription?.startedAt)}
                      </span>
                    </div>

                    <div className="sub-detail">
                      <span className="sub-detail__label">Ngày hết hạn</span>
                      <span className="sub-detail__value">
                        {status?.subscription?.isLifetime
                          ? 'Vĩnh viễn'
                          : formatDate(status?.subscription?.expiresAt)}
                      </span>
                    </div>

                    {!status?.subscription?.isLifetime && status?.subscription?.daysRemaining !== null && (
                      <div className="sub-detail">
                        <span className="sub-detail__label">Thời gian còn lại</span>
                        <span className="sub-detail__value sub-detail__value--highlight">
                          {formatRelativeTime(status?.subscription?.expiresAt)}
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Subscription Progress */}
                  {!status?.subscription?.isLifetime && status?.subscription?.progressPercent !== null && (
                    <div className="sub-progress-section">
                      <div className="sub-progress-section__label">
                        <span>Thời gian sử dụng</span>
                        <span>{Math.round(status?.subscription?.progressPercent || 0)}%</span>
                      </div>
                      <div className="sub-progress-bar sub-progress-bar--large">
                        <div
                          className="sub-progress-bar__fill"
                          style={{
                            width: `${status?.subscription?.progressPercent || 0}%`,
                            background: tierConfig.gradient
                          }}
                        />
                      </div>
                    </div>
                  )}
                </div>

                {/* AI Grading Toggle Card */}
                <AiGradingToggleCard
                  isEnabled={aiGradingEnabled}
                  isProcessing={aiGradingProcessing}
                  onToggle={handleAiGradingToggle}
                />

                {/* Usage Stats Card */}
                <div className="sl-card">
                  <div className="sl-card__header">
                    <div className="sl-card__header-left">
                      <h2 className="sl-card__title">
                        <FaRobot />
                        Thống kê sử dụng
                      </h2>
                      <p className="sl-card__description">
                        Theo dõi hạn mức sử dụng các tính năng
                      </p>
                    </div>
                  </div>

                  <div className="sub-usage-list">
                    {/* Lượt chấm thường (ATTEMPTs) */}
                    <UsageCard
                      title={TERMINOLOGY.ATTEMPT}
                      icon={FaBook}
                      used={status?.attempts?.used || 0}
                      limit={status?.attempts?.limit || 0}
                      isUnlimited={status?.attempts?.isUnlimited}
                      resetInfo={status?.attempts?.resetInfo || 'Đặt lại hàng tháng'}
                      colorClass={tierConfig.colorClass}
                      luaCost={ATTEMPT_COSTS.ATTEMPT}
                    />

                    {/* Lượt chấm nâng cao (ATTEMPT_AIs) */}
                    <UsageCard
                      title={TERMINOLOGY.ATTEMPT_AI}
                      icon={FaStar}
                      used={status?.attemptAis?.used || 0}
                      limit={status?.attemptAis?.limit || 0}
                      isUnlimited={status?.attemptAis?.isUnlimited}
                      resetInfo={status?.attemptAis?.resetInfo || 'Đặt lại hàng tháng'}
                      colorClass="tier-attempt-ai"
                      luaCost={ATTEMPT_COSTS.ATTEMPT_AI}
                    />

                    {/* Chatbot */}
                    <UsageCard
                      title="Trò chuyện AI"
                      icon={FaComments}
                      used={status?.chatbot?.used || 0}
                      limit={status?.chatbot?.limit || 50}
                      isUnlimited={status?.chatbot?.isUnlimited}
                      resetInfo={status?.chatbot?.resetInfo || 'Đặt lại hàng tháng'}
                      colorClass={tierConfig.colorClass}
                      luaCost={2}
                    />

                    {/* Translation */}
                    <UsageCard
                      title="Dịch từ vựng"
                      icon={FaLanguage}
                      used={status?.translation?.used || 0}
                      limit={status?.translation?.limit || 10}
                      isUnlimited={status?.translation?.isUnlimited}
                      resetInfo={status?.translation?.resetInfo || 'Đặt lại hàng ngày'}
                      colorClass={tierConfig.colorClass}
                      luaCost={1}
                    />
                  </div>
                </div>

                {/* Features List (if available) */}
                {status?.features && status.features.length > 0 && (
                  <div className="sl-card">
                    <div className="sl-card__header">
                      <h2 className="sl-card__title">
                        <FaCheckCircle />
                        Tính năng bao gồm
                      </h2>
                    </div>
                    <div className="sub-features-list">
                      {status.features.map((feature, index) => (
                        <div key={index} className="sub-feature-item">
                          <FaCheckCircle className="sub-feature-item__icon" />
                          <span>{feature}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </motion.div>
            )}

            {/* ==================== TAB 2: Các gói khác ==================== */}
            {activeTab === 'packages' && (
              <motion.div
                key="packages"
                className="sl-tab-panel"
                variants={tabContentVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
                transition={{ duration: 0.3 }}
              >
                {/* Tier Upgrade Section (if free) */}
                {canUpgrade && (
                  <div className="sl-card">
                    <div className="sl-card__header">
                      <div className="sl-card__header-left">
                        <h2 className="sl-card__title">
                          <FaCrown />
                          Nâng cấp gói đăng ký
                        </h2>
                        <p className="sl-card__description">
                          Mở khóa toàn bộ tính năng AI với gói Cramerich
                        </p>
                      </div>
                    </div>
                    <TierUpgradeCard
                      onUpgrade={handleUpgrade}
                      isProcessing={processingPayment === `tier-${TIERS.CRAMERICH}`}
                    />
                  </div>
                )}

                {/* Already on Cramerich message */}
                {!canUpgrade && (
                  <div className="sl-card">
                    <div className="sub-current-tier-message">
                      <span className="sub-current-tier-message__emoji">🌻</span>
                      <h3>Bạn đang sử dụng gói Cramerich</h3>
                      <p>Đây là gói cao cấp nhất với đầy đủ tính năng AI hỗ trợ học IELTS.</p>
                    </div>
                  </div>
                )}

                {/* Lúa Packs Section */}
                <div className="sl-card">
                  <div className="sl-card__header">
                    <div className="sl-card__header-left">
                      <h2 className="sl-card__title">
                        <FaCoins />
                        Mua thêm Lúa
                      </h2>
                      <p className="sl-card__description">
                        Lúa dùng để mua thêm lượt chấm khi vượt hạn mức
                      </p>
                    </div>
                  </div>

                  <div className="sub-lua-packs-grid">
                    {LUA_PACKS.map(pack => (
                      <LuaPackCard
                        key={pack.code}
                        pack={pack}
                        isProcessing={processingPayment === `lua-${pack.code}`}
                        onPurchase={() => setSelectedPackage(pack)}
                      />
                    ))}
                  </div>

                  {/* Lúa usage info */}
                  <div className="sub-lua-info">
                    <h4>🌾 Lúa dùng để làm gì?</h4>
                    <ul>
                      <li>
                        <strong>{TERMINOLOGY.ATTEMPT}</strong> thêm: <span className="sub-lua-info__cost">{ATTEMPT_COSTS.ATTEMPT} Lúa/lượt</span>
                      </li>
                      <li>
                        <strong>{TERMINOLOGY.ATTEMPT_AI}</strong> thêm: <span className="sub-lua-info__cost">{ATTEMPT_COSTS.ATTEMPT_AI} Lúa/lượt</span>
                      </li>
                      <li>
                        <strong>Dịch từ vựng</strong> vượt hạn mức: <span className="sub-lua-info__cost">1 Lúa/từ</span>
                      </li>
                      <li>
                        <strong>Câu hỏi chatbot</strong> vượt hạn mức: <span className="sub-lua-info__cost">2 Lúa/câu</span>
                      </li>
                    </ul>
                  </div>
                </div>
              </motion.div>
            )}

            {/* ==================== TAB 3: Lịch sử ==================== */}
            {activeTab === 'history' && (
              <motion.div
                key="history"
                className="sl-tab-panel"
                variants={tabContentVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
                transition={{ duration: 0.3 }}
              >
                {/* Credit Transactions */}
                <div className="sl-card">
                  <div className="sl-card__header">
                    <div className="sl-card__header-left">
                      <h2 className="sl-card__title">
                        <FaCoins />
                        Lịch sử Lúa
                      </h2>
                      <p className="sl-card__description">
                        Các giao dịch nhận và chi tiêu Lúa
                      </p>
                    </div>
                  </div>
                  <CreditHistoryList />
                </div>

                {/* Payment History */}
                <div className="sl-card">
                  <div className="sl-card__header">
                    <div className="sl-card__header-left">
                      <h2 className="sl-card__title">
                        <FaHistory />
                        Lịch sử thanh toán
                      </h2>
                      <p className="sl-card__description">
                        Các giao dịch nạp tiền và nâng cấp gói
                      </p>
                    </div>
                  </div>

                  {status?.recentPayments && status.recentPayments.length > 0 ? (
                    <div className="sub-payment-list">
                      {status.recentPayments.map((payment, index) => (
                        <PaymentHistoryItem key={index} payment={payment} />
                      ))}
                    </div>
                  ) : (
                    <div className="sl-empty">
                      <FaHistory style={{ fontSize: '2rem', marginBottom: '0.75rem', opacity: 0.5 }} />
                      <p>Chưa có lịch sử thanh toán</p>
                      <button
                        className="sl-btn sl-btn--secondary"
                        onClick={() => navigate('/pricing')}
                        style={{ marginTop: '1rem' }}
                      >
                        Xem gói đăng ký
                        <FaExternalLinkAlt style={{ marginLeft: '0.5rem', fontSize: '0.8rem' }} />
                      </button>
                    </div>
                  )}
                </div>

                {/* Login Sessions */}
                <div className="sl-card">
                  <div className="sl-card__header">
                    <div className="sl-card__header-left">
                      <h2 className="sl-card__title">
                        <FaDesktop />
                        Phiên đăng nhập
                      </h2>
                      <p className="sl-card__description">
                        Các thiết bị đang đăng nhập vào tài khoản của bạn
                      </p>
                    </div>
                    {sessions.length > 1 && (
                      <button
                        className="sl-btn sl-btn--secondary sl-btn--small"
                        onClick={handleRevokeAllSessions}
                      >
                        <FiLogOut style={{ marginRight: '0.4rem' }} />
                        Đăng xuất tất cả
                      </button>
                    )}
                  </div>

                  <div className="sub-sessions-list">
                    {sessions.map(session => (
                      <div
                        key={session.id}
                        className={`sub-session-item ${session.isCurrent ? 'sub-session-item--current' : ''}`}
                      >
                        <div className="sub-session-item__info">
                          <div className="sub-session-item__icon">
                            <session.icon />
                          </div>
                          <div className="sub-session-item__details">
                            <h4>
                              {session.device}
                              {session.isCurrent && (
                                <span className="sub-session-badge">
                                  <FiCheck /> Phiên hiện tại
                                </span>
                              )}
                            </h4>
                            <p>{session.location} • {session.lastActive}</p>
                          </div>
                        </div>
                        {!session.isCurrent && (
                          <div className="sub-session-item__actions">
                            <button
                              className="sub-session-btn sub-session-btn--danger"
                              onClick={() => handleRevokeSession(session.id)}
                            >
                              <FiLogOut /> Đăng xuất
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </main>
      </div>

      {/* Purchase Modal */}
      {selectedPackage && (
        <LuaPurchaseModal
          package={selectedPackage}
          onConfirm={() => handlePurchaseLua(selectedPackage)}
          onCancel={() => setSelectedPackage(null)}
          isProcessing={processingPayment === `lua-${selectedPackage.code}`}
          formatPrice={formatVnd}
          getTotalLua={(p) => p.luaAmount}
        />
      )}


    </div>
  );
}
