import useQuotaStore from '../stores/useQuotaStore';
import '../css/quota-display.css';

/**
 * QuotaDisplay component for showing global and per-skill quota usage.
 * Displays progress bars with color coding.
 */
const QuotaDisplay = ({ skill = null, showGlobal = true, showLocal = true, compact = false }) => {
    const { quotaStatus, loading, isPremium, getGlobalProgress, getSkillProgress, getProgressColor } = useQuotaStore();

    // Premium users don't need to see quota
    if (isPremium()) {
        return (
            <div className={`quota-display ${compact ? 'compact' : ''}`}>
                <div className="quota-premium-badge">
                    <span className="premium-icon">⭐</span>
                    <span>Unlimited</span>
                </div>
            </div>
        );
    }

    if (loading && !quotaStatus) {
        return (
            <div className={`quota-display ${compact ? 'compact' : ''}`}>
                <div className="quota-loading">Loading...</div>
            </div>
        );
    }

    if (!quotaStatus) {
        return null;
    }

    const globalProgress = getGlobalProgress(false);
    const globalAIProgress = getGlobalProgress(true);
    const globalColor = getProgressColor(globalProgress);
    const globalAIColor = getProgressColor(globalAIProgress);

    return (
        <div className={`quota-display ${compact ? 'compact' : ''}`}>
            {/* Global Quota */}
            {showGlobal && (
                <div className="quota-section">
                    <div className="quota-header">
                        <span className="quota-label">📊 Quota tháng {quotaStatus.quotaMonth}</span>
                    </div>

                    {/* Regular attempts */}
                    <div className="quota-item">
                        <div className="quota-item-header">
                            <span className="quota-item-label">Bài làm</span>
                            <span className="quota-item-count">
                                {quotaStatus.globalAttempt} / {quotaStatus.globalAttemptCap}
                            </span>
                        </div>
                        <div className="quota-progress-bar">
                            <div
                                className={`quota-progress-fill ${globalColor}`}
                                style={{ width: `${globalProgress}%` }}
                            />
                        </div>
                    </div>

                    {/* AI attempts */}
                    <div className="quota-item">
                        <div className="quota-item-header">
                            <span className="quota-item-label">Chấm AI</span>
                            <span className="quota-item-count">
                                {quotaStatus.globalAttemptAI} / {quotaStatus.globalAttemptAICap}
                            </span>
                        </div>
                        <div className="quota-progress-bar">
                            <div
                                className={`quota-progress-fill ${globalAIColor}`}
                                style={{ width: `${globalAIProgress}%` }}
                            />
                        </div>
                    </div>
                </div>
            )}

            {/* Per-skill Quota */}
            {showLocal && skill && quotaStatus.skills?.[skill] && (
                <div className="quota-section skill-quota">
                    <div className="quota-header">
                        <span className="quota-label">🎯 {skill}</span>
                    </div>

                    {(() => {
                        const skillInfo = quotaStatus.skills[skill];
                        const skillProgress = getSkillProgress(skill, false);
                        const skillAIProgress = getSkillProgress(skill, true);
                        const skillColor = getProgressColor(skillProgress);
                        const skillAIColor = getProgressColor(skillAIProgress);

                        return (
                            <>
                                <div className="quota-item">
                                    <div className="quota-item-header">
                                        <span className="quota-item-label">Bài làm</span>
                                        <span className="quota-item-count">
                                            {skillInfo.attempt} / {skillInfo.attemptCap}
                                        </span>
                                    </div>
                                    <div className="quota-progress-bar">
                                        <div
                                            className={`quota-progress-fill ${skillColor}`}
                                            style={{ width: `${skillProgress}%` }}
                                        />
                                    </div>
                                </div>

                                <div className="quota-item">
                                    <div className="quota-item-header">
                                        <span className="quota-item-label">Chấm AI</span>
                                        <span className="quota-item-count">
                                            {skillInfo.attemptAI} / {skillInfo.attemptAICap}
                                        </span>
                                    </div>
                                    <div className="quota-progress-bar">
                                        <div
                                            className={`quota-progress-fill ${skillAIColor}`}
                                            style={{ width: `${skillAIProgress}%` }}
                                        />
                                    </div>
                                </div>
                            </>
                        );
                    })()}
                </div>
            )}
        </div>
    );
};

export default QuotaDisplay;
