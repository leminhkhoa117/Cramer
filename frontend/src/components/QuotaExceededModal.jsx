import '../css/quota-exceeded-modal.css';

/**
 * Modal shown when quota is exceeded.
 * Explains which cap was hit and offers options to continue.
 */
const QuotaExceededModal = ({
    isOpen,
    onClose,
    billingResult,
    onBuyLua,
    onUpgrade
}) => {
    if (!isOpen || !billingResult) return null;

    const { blockType, reason, message, luaCharged } = billingResult;
    // S7 (BUG_AUDIT): backend sends "message", not "reason". Support both for compat.
    const displayReason = reason || message;

    // Determine required Lua from reason (parse "Cần X Lua")
    const requiredLua = displayReason?.match(/Cần (\d+) Lua/)?.[1] || '?';

    const getBlockTypeLabel = () => {
        switch (blockType) {
            case 'global':
                return 'Quota tháng';
            case 'local':
                return 'Quota kỹ năng';
            case 'insufficient_lua':
                return 'Không đủ Lua';
            default:
                return 'Quota';
        }
    };

    const getBlockTypeIcon = () => {
        switch (blockType) {
            case 'global':
                return '🌍';
            case 'local':
                return '🎯';
            case 'insufficient_lua':
                return '💰';
            default:
                return '⚠️';
        }
    };

    return (
        <div className="quota-exceeded-overlay" onClick={onClose}>
            <div className="quota-exceeded-modal" onClick={e => e.stopPropagation()}>
                {/* Header */}
                <div className="quota-exceeded-header">
                    <span className="quota-exceeded-icon">{getBlockTypeIcon()}</span>
                    <h2>Đã hết {getBlockTypeLabel()}</h2>
                </div>

                {/* Content */}
                <div className="quota-exceeded-content">
                    <p className="quota-exceeded-message">{displayReason}</p>

                    <div className="quota-exceeded-info">
                        <div className="info-item">
                            <span className="info-label">Chi phí tiếp tục:</span>
                            <span className="info-value lua-amount">{requiredLua} Lua</span>
                        </div>
                    </div>

                    {blockType === 'insufficient_lua' && (
                        <p className="quota-exceeded-hint">
                            Nạp thêm Lua để tiếp tục học hoặc nâng cấp Cramerich để không giới hạn!
                        </p>
                    )}
                </div>

                {/* Actions */}
                <div className="quota-exceeded-actions">
                    <button
                        className="btn-buy-lua"
                        onClick={onBuyLua}
                    >
                        💰 Mua Lua
                    </button>
                    <button
                        className="btn-upgrade"
                        onClick={onUpgrade}
                    >
                        ⭐ Nâng cấp Cramerich
                    </button>
                </div>

                {/* Close button */}
                <button className="quota-exceeded-close" onClick={onClose}>
                    ✕
                </button>
            </div>
        </div>
    );
};

export default QuotaExceededModal;
