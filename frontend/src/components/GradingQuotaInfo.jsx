import React, { useEffect } from 'react';
import { FiAlertTriangle, FiCheckCircle, FiDollarSign } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import { useUserStatsStore } from '../stores';
import PropTypes from 'prop-types';
import '../css/GradingQuotaInfo.css';

/**
 * GradingQuotaInfo - Reusable component showing AI grading quota status
 * 
 * Features:
 * - Shows remaining AI grading attempts this month
 * - Warning if quota is exhausted
 * - Option to use Lúa for additional gradings
 * - Compact or detailed view modes
 */
const GradingQuotaInfo = ({ variant = 'default', showIcon = true, className = '' }) => {
    const { gradingStatus, credits, fetchUserStats } = useUserStatsStore();

    // Fetch latest stats on mount
    useEffect(() => {
        fetchUserStats();
    }, [fetchUserStats]);

    // Extract quota info
    const monthlyLimit = gradingStatus?.monthlyLimit ?? gradingStatus?.limit ?? 0;
    const used = gradingStatus?.usedThisMonth ?? gradingStatus?.used ?? 0;
    const remaining = gradingStatus?.remaining ?? Math.max(0, monthlyLimit - used);
    const luaBalance = credits?.balance ?? 0;
    const luaCost = 20; // Cost per AI grading in Lúa
    const canUseWithLua = luaBalance >= luaCost;

    // Determine status
    const hasQuota = remaining > 0;
    const isLowQuota = remaining > 0 && remaining <= 3;

    // Status class
    let statusClass = 'success';
    if (!hasQuota) {
        statusClass = canUseWithLua ? 'warning' : 'error';
    } else if (isLowQuota) {
        statusClass = 'low';
    }

    // Compact variant for inline usage
    if (variant === 'compact') {
        return (
            <div className={`grading-quota grading-quota--compact ${statusClass} ${className}`}>
                {showIcon && (
                    hasQuota ? <FiCheckCircle className="quota-icon" /> : <FiAlertTriangle className="quota-icon" />
                )}
                <span className="quota-text">
                    {hasQuota
                        ? `${remaining}/${monthlyLimit} lượt AI còn lại`
                        : canUseWithLua
                            ? `Dùng ${luaCost} Lúa cho lượt chấm này`
                            : 'Hết quota & Lúa'
                    }
                </span>
            </div>
        );
    }

    // Default variant - full info card
    return (
        <div className={`grading-quota grading-quota--card ${statusClass} ${className}`}>
            <div className="quota-header">
                {showIcon && (
                    hasQuota
                        ? <FiCheckCircle className="quota-icon" />
                        : <FiAlertTriangle className="quota-icon" />
                )}
                <span className="quota-title">
                    {hasQuota ? 'Lượt chấm AI trong tháng' : 'Thông báo quan trọng'}
                </span>
            </div>

            <div className="quota-body">
                {hasQuota ? (
                    <>
                        <div className="quota-stats">
                            <span className="quota-remaining">{remaining}</span>
                            <span className="quota-separator">/</span>
                            <span className="quota-limit">{monthlyLimit}</span>
                            <span className="quota-label">lượt còn lại</span>
                        </div>
                        {isLowQuota && (
                            <p className="quota-hint warning">
                                ⚠️ Bạn sắp hết lượt chấm AI miễn phí trong tháng này.
                            </p>
                        )}
                    </>
                ) : (
                    <>
                        <p className="quota-message error">
                            Bạn đã hết <strong>{monthlyLimit}</strong> lượt chấm AI miễn phí trong tháng.
                        </p>
                        {canUseWithLua ? (
                            <div className="quota-lua-option">
                                <FiDollarSign className="lua-icon" />
                                <span>
                                    Bài này sẽ dùng <strong>{luaCost} Lúa</strong> để chấm.
                                    (Còn <strong>{luaBalance}</strong> Lúa)
                                </span>
                            </div>
                        ) : (
                            <p className="quota-message blocked" style={{ marginTop: '0.75rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                <span>Bạn không có đủ Lúa ({luaBalance}/{luaCost}) để chấm bài.</span>
                                <div>
                                    <Link to="/subscription" className="quota-link">Mua thêm Lúa</Link>
                                    {' hoặc '}
                                    <Link to="/pricing" className="quota-link">nâng cấp gói</Link>!
                                </div>
                            </p>
                        )}
                    </>
                )}
            </div>

            {/* Progress bar */}
            {monthlyLimit > 0 && (
                <div className="quota-progress">
                    <div
                        className="quota-progress-fill"
                        style={{ width: `${Math.min(100, (remaining / monthlyLimit) * 100)}%` }}
                    />
                </div>
            )}
        </div>
    );
};

GradingQuotaInfo.propTypes = {
    variant: PropTypes.oneOf(['default', 'compact']),
    showIcon: PropTypes.bool,
    className: PropTypes.string,
};

export default GradingQuotaInfo;
