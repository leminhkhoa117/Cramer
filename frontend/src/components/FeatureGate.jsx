import React from 'react';
import { FiLock } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import useSubscriptionStore, { selectHasFeature, selectLoading } from '../stores/useSubscriptionStore';

/**
 * FeatureGate - Conditional rendering component based on subscription features.
 * 
 * Shows children if user has access to the feature, otherwise shows
 * a fallback or default upgrade prompt.
 * 
 * @param {Object} props
 * @param {string} props.feature - Feature code to check (e.g., "ai_writing_grading")
 * @param {React.ReactNode} props.children - Content to show if user has access
 * @param {React.ReactNode} props.fallback - Optional custom fallback content
 * @param {boolean} props.showUpgradePrompt - Whether to show upgrade prompt (default: true)
 * @param {string} props.upgradeMessage - Custom message for upgrade prompt
 * @param {boolean} props.inline - Whether to render inline (span) vs block (div)
 * 
 * @example
 * <FeatureGate feature="ai_writing_grading">
 *   <AIGradingButton />
 * </FeatureGate>
 * 
 * @example
 * <FeatureGate 
 *   feature="chatbot" 
 *   fallback={<BasicChatPlaceholder />}
 * >
 *   <AdvancedChatbot />
 * </FeatureGate>
 */
export default function FeatureGate({
    feature,
    children,
    fallback = null,
    showUpgradePrompt = true,
    upgradeMessage = null,
    inline = false,
}) {
    const navigate = useNavigate();
    const hasFeature = useSubscriptionStore(selectHasFeature(feature));
    const loading = useSubscriptionStore(selectLoading);
    const tierNameVi = useSubscriptionStore((state) => state.tierNameVi);

    // While loading, show nothing or a subtle placeholder
    if (loading) {
        return null;
    }

    // User has access - render children
    if (hasFeature) {
        return <>{children}</>;
    }

    // User doesn't have access - show fallback or upgrade prompt
    if (fallback) {
        return <>{fallback}</>;
    }

    // Default upgrade prompt
    if (showUpgradePrompt) {
        const Wrapper = inline ? 'span' : 'div';
        const defaultMessage = getDefaultUpgradeMessage(feature);

        return (
            <Wrapper className="feature-gate-locked">
                <div className="feature-gate-locked__content">
                    <FiLock className="feature-gate-locked__icon" />
                    <p className="feature-gate-locked__message">
                        {upgradeMessage || defaultMessage}
                    </p>
                    <button
                        className="feature-gate-locked__btn"
                        onClick={() => navigate('/subscription')}
                    >
                        Nâng cấp {tierNameVi === 'Cramerie' ? 'Cramerich' : 'gói cao hơn'}
                    </button>
                </div>
            </Wrapper>
        );
    }

    // No fallback and no upgrade prompt - render nothing
    return null;
}

/**
 * Get a user-friendly message for specific features.
 */
function getDefaultUpgradeMessage(featureCode) {
    const messages = {
        ai_writing_grading: 'Chấm bài Writing bằng AI là tính năng Premium',
        ai_reading_grading: 'Chấm bài Reading bằng AI là tính năng Premium',
        ai_listening_grading: 'Chấm bài Listening bằng AI là tính năng Premium',
        ai_speaking_grading: 'Chấm bài Speaking bằng AI là tính năng Premium',
        vocab_ai: 'Từ vựng AI là tính năng Premium',
        chatbot: 'AI Assistant là tính năng Premium',
        all_tests: 'Bài test này yêu cầu gói Premium',
        all_topics: 'Chủ đề này yêu cầu gói Premium',
        analytics: 'Phân tích chi tiết là tính năng Premium',
        full_progress: 'Theo dõi tiến độ đầy đủ là tính năng Premium',
    };

    return messages[featureCode] || 'Tính năng này yêu cầu nâng cấp gói';
}

/**
 * FeatureGateInline - Inline version of FeatureGate.
 * Useful for wrapping inline elements like buttons or links.
 */
export function FeatureGateInline(props) {
    return <FeatureGate {...props} inline />;
}
