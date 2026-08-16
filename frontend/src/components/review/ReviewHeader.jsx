import { useNavigate } from 'react-router-dom';
import { FiArrowLeft } from 'react-icons/fi';
import '../../css/review-header.css';

/**
 * ReviewHeader - Unified header for review pages
 * 
 * @param {Object} props
 * @param {string} props.title - Page title (e.g., "CAM17 Test 1 - Reading Review")
 * @param {string} props.backUrl - URL to navigate back to (default: '/dashboard')
 * @param {Function} props.onBack - Optional custom back handler
 * @param {Object} props.scoreBadge - Optional overall score badge
 * @param {string} props.scoreBadge.label - Label (e.g., "Band" or "Overall")
 * @param {number|string} props.scoreBadge.value - Score value
 * @param {ReactNode} props.actions - Optional right-side action buttons
 * @param {Array} props.tabs - Optional tabs array [{id, label, value}]
 * @param {string|number} props.activeTab - Currently active tab id
 * @param {Function} props.onTabChange - Tab change handler
 * @param {ReactNode} props.subHeader - Optional sub-header content (e.g., collapsible score bar)
 */
const ReviewHeader = ({
    title,
    backUrl = '/dashboard',
    onBack,
    scoreBadge,
    actions,
    tabs,
    activeTab,
    onTabChange,
    subHeader
}) => {
    const navigate = useNavigate();

    const handleBack = () => {
        if (onBack) {
            onBack();
        } else {
            navigate(backUrl);
        }
    };

    // Get band color class based on score
    const getBandClass = (value) => {
        const numValue = parseFloat(value);
        if (isNaN(numValue)) return '';
        return `band-${Math.floor(numValue)}`;
    };

    return (
        <>
            {/* Top Header Bar */}
            <div className="review-top-header">
                <div className="header-left">
                    <button className="back-btn" onClick={handleBack}>
                        <FiArrowLeft size={16} /> Quay lại
                    </button>
                    <h1>{title}</h1>
                </div>
                <div className="header-right">
                    {actions}
                    {scoreBadge && (
                        <div className="overall-band-badge">
                            <span className="label">{scoreBadge.label}</span>
                            <span className={`value ${getBandClass(scoreBadge.value)}`}>
                                {typeof scoreBadge.value === 'number'
                                    ? scoreBadge.value.toFixed(1)
                                    : scoreBadge.value || 'N/A'}
                            </span>
                        </div>
                    )}
                </div>
            </div>

            {/* Optional Tabs */}
            {tabs && tabs.length > 0 && (
                <div className="review-tabs">
                    {tabs.map(tab => (
                        <button
                            key={tab.id}
                            className={`tab-btn ${activeTab === tab.id ? 'active' : ''}`}
                            onClick={() => onTabChange && onTabChange(tab.id)}
                        >
                            <span className="tab-label">{tab.label}</span>
                            {tab.value !== undefined && (
                                <span className={`tab-value ${getBandClass(tab.value)}`}>
                                    {typeof tab.value === 'number' ? tab.value.toFixed(1) : tab.value}
                                </span>
                            )}
                        </button>
                    ))}
                </div>
            )}

            {/* Optional Sub-header (e.g., collapsible score bar) */}
            {subHeader}
        </>
    );
};

export default ReviewHeader;
