import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FiX, FiLoader, FiCreditCard } from 'react-icons/fi';

/**
 * Modal for confirming Lúa package purchase.
 * Shows package details, bonus, and total with payment button.
 */
export default function LuaPurchaseModal({
    package: pkg,
    onConfirm,
    onCancel,
    isProcessing,
    formatPrice,
    getTotalLua
}) {
    if (!pkg) return null;

    const bonusAmount = Math.floor(pkg.luaAmount * pkg.bonusPercent / 100);
    const totalLua = getTotalLua(pkg);

    return (
        <AnimatePresence>
            <motion.div
                className="lua-modal-overlay"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={onCancel}
            >
                <motion.div
                    className="lua-modal"
                    initial={{ scale: 0.9, opacity: 0, y: 20 }}
                    animate={{ scale: 1, opacity: 1, y: 0 }}
                    exit={{ scale: 0.9, opacity: 0, y: 20 }}
                    onClick={(e) => e.stopPropagation()}
                >
                    {/* Header */}
                    <div className="lua-modal__header">
                        <span className="lua-modal__icon">🌾</span>
                        <h2 className="lua-modal__title">Xác nhận mua {pkg.name}</h2>
                    </div>

                    {/* Body - Package Details */}
                    <div className="lua-modal__body">
                        <div className="lua-modal__row">
                            <span className="lua-modal__label">Gói Lúa</span>
                            <span className="lua-modal__value">{pkg.name}</span>
                        </div>

                        <div className="lua-modal__row">
                            <span className="lua-modal__label">Lúa cơ bản</span>
                            <span className="lua-modal__value lua-modal__value--highlight">
                                {pkg.luaAmount.toLocaleString()} 🌾
                            </span>
                        </div>

                        {pkg.bonusPercent > 0 && (
                            <div className="lua-modal__row">
                                <span className="lua-modal__label">Bonus (+{pkg.bonusPercent}%)</span>
                                <span className="lua-modal__value" style={{ color: '#10b981' }}>
                                    +{bonusAmount.toLocaleString()} 🌾
                                </span>
                            </div>
                        )}

                        <div className="lua-modal__row">
                            <span className="lua-modal__label">Tổng Lúa nhận được</span>
                            <span className="lua-modal__value lua-modal__value--total">
                                {totalLua.toLocaleString()} 🌾
                            </span>
                        </div>

                        <div className="lua-modal__row" style={{ paddingTop: '1rem', borderTop: '2px solid #e5e7eb', marginTop: '0.5rem' }}>
                            <span className="lua-modal__label" style={{ fontWeight: 600, color: '#1f2937' }}>Thanh toán</span>
                            <span className="lua-modal__value" style={{ fontSize: '1.25rem', fontWeight: 700, color: '#1f2937' }}>
                                {formatPrice(pkg.priceVnd)}
                            </span>
                        </div>
                    </div>

                    {/* Footer - Actions */}
                    <div className="lua-modal__footer">
                        <button
                            className="lua-modal__btn lua-modal__btn--cancel"
                            onClick={onCancel}
                            disabled={isProcessing}
                        >
                            Hủy
                        </button>
                        <button
                            className="lua-modal__btn lua-modal__btn--confirm"
                            onClick={onConfirm}
                            disabled={isProcessing}
                        >
                            {isProcessing ? (
                                <>
                                    <FiLoader className="lua-package-card__spinner" />
                                    Đang xử lý...
                                </>
                            ) : (
                                <>
                                    <FiCreditCard />
                                    Thanh toán
                                </>
                            )}
                        </button>
                    </div>
                </motion.div>
            </motion.div>
        </AnimatePresence>
    );
}
