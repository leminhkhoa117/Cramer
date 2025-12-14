import React from 'react';
import { FiCheck, FiX, FiLoader } from 'react-icons/fi';
import { motion } from 'framer-motion';

/**
 * TierCard - Subscription tier card component
 * Displays tier details with glassmorphic styling
 * 
 * @param {Object} props
 * @param {Object} props.tier - Tier data
 * @param {boolean} props.isCurrentTier - Whether this is user's current tier
 * @param {boolean} props.isPopular - Whether to show "PHỔ BIẾN" badge
 * @param {boolean} props.isProcessing - Whether payment is being processed
 * @param {boolean} props.isDowngrade - Whether this tier is lower than current (hide upgrade button)
 * @param {boolean} props.isDisabled - Whether this tier is disabled/unavailable
 * @param {Function} props.onUpgrade - Callback when upgrade button clicked
 */
export default function TierCard({
  tier,
  isCurrentTier = false,
  isPopular = false,
  isProcessing = false,
  isDowngrade = false,
  isDisabled = false,
  onUpgrade
}) {
  const {
    emoji = '🌾',
    name = 'Cramerie',
    price = 0,
    priceLabel = 'Miễn phí',
    features = [],
    unavailableFeatures = [],
  } = tier;

  return (
    <motion.div
      className={`tier-card ${isPopular ? 'tier-card--popular' : ''} ${isCurrentTier ? 'tier-card--current' : ''}`}
      whileHover={{ y: -8, transition: { duration: 0.3 } }}
    >
      {/* Popular Badge */}
      {isPopular && (
        <div className="tier-card__popular-badge">
          <span>⭐ PHỔ BIẾN</span>
        </div>
      )}

      {/* Current Tier Badge */}
      {isCurrentTier && (
        <div className="tier-card__current-badge">
          <span>✓ Gói hiện tại</span>
        </div>
      )}

      {/* Header */}
      <div className="tier-card__header">
        <span className="tier-card__emoji">{emoji}</span>
        <h3 className="tier-card__name">{name}</h3>
      </div>

      {/* Price */}
      <div className="tier-card__price-section">
        <span className="tier-card__price">{priceLabel}</span>
        {price > 0 && <span className="tier-card__period">/tháng</span>}
      </div>

      {/* Features List */}
      <ul className="tier-card__features">
        {features.map((feature, index) => (
          <li key={index} className="tier-card__feature tier-card__feature--included">
            <FiCheck className="tier-card__feature-icon tier-card__feature-icon--check" />
            <span>{feature}</span>
          </li>
        ))}
        {unavailableFeatures.map((feature, index) => (
          <li key={`unavail-${index}`} className="tier-card__feature tier-card__feature--excluded">
            <FiX className="tier-card__feature-icon tier-card__feature-icon--x" />
            <span>{feature}</span>
          </li>
        ))}
      </ul>

      {/* Action Button */}
      <div className="tier-card__action">
        {isCurrentTier ? (
          <button className="tier-card__btn tier-card__btn--current" disabled>
            Gói hiện tại
          </button>
        ) : isDisabled ? (
          <button className="tier-card__btn tier-card__btn--disabled" disabled>
            Sắp ra mắt
          </button>
        ) : isDowngrade ? (
          // Don't show button for lower tiers when user has higher subscription
          null
        ) : isProcessing ? (
          <button className="tier-card__btn tier-card__btn--processing" disabled>
            <FiLoader className="tier-card__spinner" />
            Đang xử lý...
          </button>
        ) : (
          <button
            className={`tier-card__btn ${isPopular ? 'tier-card__btn--popular' : 'tier-card__btn--upgrade'}`}
            onClick={() => onUpgrade?.(tier)}
          >
            {price === 0 ? 'Đăng ký' : 'Nâng cấp'}
          </button>
        )}
      </div>
    </motion.div>
  );
}
