import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

import {
  FaClock, FaCheckCircle, FaTimesCircle, FaRobot,
  FaToggleOn, FaToggleOff, FaInfoCircle, FaCreditCard,
  FaStar, FaComments, FaChevronRight, FaArrowUp
} from 'react-icons/fa';
import { FiLoader } from 'react-icons/fi';

import {
  TIER_INFO, TIERS, LIMITS, ATTEMPT_COSTS, TERMINOLOGY,
  formatVnd
} from '../../constants/subscription';

const formatDate = (dateString) => {
  if (!dateString) return 'Không xác định';
  const date = new Date(dateString);
  return date.toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
};

export const ProgressBar = ({ used, limit, isUnlimited, colorClass }) => {
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

export const UsageCard = ({ title, icon: Icon, used, limit, isUnlimited, resetInfo, colorClass, luaCost }) => {
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

export const StatusBadge = ({ status }) => {
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

export const AiGradingToggleCard = ({
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

export const LuaPackCard = ({ pack, isProcessing, onPurchase }) => {
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

export const PaymentHistoryItem = ({ payment }) => {
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

export const TierUpgradeCard = ({ onUpgrade, isProcessing }) => {
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
