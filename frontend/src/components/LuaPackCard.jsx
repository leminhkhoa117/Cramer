import React from 'react';
import { motion } from 'framer-motion';
import { FiLoader } from 'react-icons/fi';

/**
 * LuaPackCard - Lúa credit pack card component
 * Displays credit pack details with discount badge
 * 
 * @param {Object} props
 * @param {Object} props.pack - Pack data
 * @param {boolean} props.isProcessing - Whether payment is being processed
 * @param {Function} props.onPurchase - Callback when purchase button clicked
 */
export default function LuaPackCard({ pack, isProcessing = false, onPurchase }) {
  const {
    name = 'Túi Lúa',
    lua = 50,
    price = 5000,
    pricePerLua = 100,
    discount = 0,
  } = pack;

  const formatPrice = (value) => {
    return new Intl.NumberFormat('vi-VN').format(value) + 'đ';
  };

  return (
    <motion.div
      className={`lua-pack-card ${discount > 0 ? 'lua-pack-card--discounted' : ''}`}
      whileHover={{ y: -6, scale: 1.02, transition: { duration: 0.2 } }}
      whileTap={{ scale: 0.98 }}
    >
      {/* Discount Badge */}
      {discount > 0 && (
        <div className="lua-pack-card__discount-badge">
          <span>-{discount}%</span>
        </div>
      )}

      {/* Rice Icon */}
      <div className="lua-pack-card__icon">
        <span>🌾</span>
      </div>

      {/* Pack Name */}
      <h4 className="lua-pack-card__name">{name}</h4>

      {/* Lúa Amount */}
      <div className="lua-pack-card__amount">
        <span className="lua-pack-card__lua-value">{lua.toLocaleString('vi-VN')}</span>
        <span className="lua-pack-card__lua-label">Lúa</span>
      </div>

      {/* Price */}
      <div className="lua-pack-card__price">
        {formatPrice(price)}
      </div>

      {/* Per-Lúa Price */}
      <div className="lua-pack-card__per-lua">
        {formatPrice(pricePerLua)}/Lúa
      </div>

      {/* Purchase Button */}
      {isProcessing ? (
        <button className="lua-pack-card__btn lua-pack-card__btn--processing" disabled>
          <FiLoader className="lua-pack-card__spinner" />
        </button>
      ) : (
        <button 
          className="lua-pack-card__btn"
          onClick={() => onPurchase?.(pack)}
        >
          Mua ngay
        </button>
      )}
    </motion.div>
  );
}
