import { motion, AnimatePresence } from 'framer-motion';
import { FiLoader, FiCreditCard } from 'react-icons/fi';
import '../css/common/modal.css';

/**
 * Modal for confirming Lúa package purchase.
 * Uses shared modal styles (cm-* classes) from common/modal.css
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
                className="cm-backdrop"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={onCancel}
            >
                <motion.div
                    className="cm-content cm-content--sm"
                    initial={{ scale: 0.9, opacity: 0, y: 20 }}
                    animate={{ scale: 1, opacity: 1, y: 0 }}
                    exit={{ scale: 0.9, opacity: 0, y: 20 }}
                    onClick={(e) => e.stopPropagation()}
                >
                    {/* Header */}
                    <div className="cm-header cm-header--no-border">
                        <h2 className="cm-title">
                            <span style={{ marginRight: '0.5rem' }}>🌾</span>
                            Xác nhận mua {pkg.name}
                        </h2>
                    </div>

                    {/* Body - Package Details */}
                    <div className="cm-body">
                        <ul className="cm-info-list">
                            <li>
                                <strong>Gói Lúa:</strong> {pkg.name}
                            </li>
                            <li>
                                <strong>Lúa cơ bản:</strong>{' '}
                                <span style={{ color: '#fbbf24' }}>
                                    {pkg.luaAmount.toLocaleString()} 🌾
                                </span>
                            </li>
                            {pkg.bonusPercent > 0 && (
                                <li>
                                    <strong>Bonus (+{pkg.bonusPercent}%):</strong>{' '}
                                    <span style={{ color: '#34d399' }}>
                                        +{bonusAmount.toLocaleString()} 🌾
                                    </span>
                                </li>
                            )}
                            <li>
                                <strong>Tổng Lúa nhận được:</strong>{' '}
                                <span style={{ color: '#f59e0b', fontWeight: 700 }}>
                                    {totalLua.toLocaleString()} 🌾
                                </span>
                            </li>
                        </ul>

                        <p style={{
                            textAlign: 'center',
                            fontSize: '1.1rem',
                            fontWeight: 600,
                            padding: '1rem',
                            background: 'rgba(255,255,255,0.1)',
                            borderRadius: '12px',
                            margin: 0
                        }}>
                            Thanh toán: {formatPrice(pkg.priceVnd)}
                        </p>
                    </div>

                    {/* Footer - Actions */}
                    <div className="cm-footer">
                        <button
                            className="cm-btn cm-btn--secondary"
                            onClick={onCancel}
                            disabled={isProcessing}
                        >
                            Hủy
                        </button>
                        <button
                            className="cm-btn cm-btn--primary"
                            onClick={onConfirm}
                            disabled={isProcessing}
                        >
                            {isProcessing ? (
                                <span className="cm-loading">Đang xử lý...</span>
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
