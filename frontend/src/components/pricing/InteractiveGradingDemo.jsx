import { useState, useEffect, useRef } from 'react';
import { motion, useInView, AnimatePresence } from 'framer-motion';

import {
    FiFileText,
    FiEdit3,
    FiBarChart2,
    FiThumbsUp,
    FiAlertTriangle,
    FiZap,
    FiChevronDown,
    FiChevronRight,
    FiBook
} from 'react-icons/fi';
import { TERMINOLOGY } from '../../constants/subscription';

// Demo content - realistic Writing review data
const DEMO_ESSAY = {
    original: `factories clustered around a central roundabout and along an eastern access road, will be comprehensively redeveloped. Most industrial buildings will be removed and replaced by housing, while the road network will be expanded and new services such as shops, a school, a medical centre and a playground will be introduced.`,
    improved: `Currently, the industrial area is situated south of a river, adjacent to farmland on the northern bank. A single road extends northwards from the main southern road to a central roundabout, with a short western spur and a longer eastern road accessing multiple factories. Under the proposed plan, the main roundabout is preserved and linked to a new smaller roundabout at the southern entry to enhance traffic circulation.`
};

const DEMO_SCORES = {
    taskAchievement: { score: 7.5, label: 'Task Achievement', color: '#22c55e' },
    coherenceCohesion: { score: 7.0, label: 'Coherence & Cohesion', color: '#22c55e' },
    lexicalResource: { score: 6.5, label: 'Lexical Resource', color: '#ca8a04' },
    grammaticalRange: { score: 8.0, label: 'Grammar Range', color: '#16a34a' }
};

const DEMO_ANALYSIS = {
    strengths: [
        'Tổ chức thông tin hợp lý với progression tốt',
        'Từ vựng đa dạng và phù hợp với task'
    ],
    weaknesses: [
        'Có 1-2 lỗi nhỏ về style và spelling'
    ],
    suggestions: [
        { original: 'more and more', corrected: 'increasingly', type: 'vocabulary' },
        { original: 'in my opinion I think', corrected: 'In my view,', type: 'grammar' }
    ]
};

// Score bar animation component
const AnimatedScoreBar = ({ label, score, color, isVisible, delay = 0 }) => {
    const [animatedScore, setAnimatedScore] = useState(0);
    const widthPercent = (animatedScore / 9) * 100;

    useEffect(() => {
        if (!isVisible) return;
        const timer = setTimeout(() => {
            let current = 0;
            const interval = setInterval(() => {
                current += 0.2;
                if (current >= score) {
                    setAnimatedScore(score);
                    clearInterval(interval);
                } else {
                    setAnimatedScore(current);
                }
            }, 30);
            return () => clearInterval(interval);
        }, delay);
        return () => clearTimeout(timer);
    }, [isVisible, score, delay]);

    return (
        <div className="wr-demo__score-row">
            <span className="wr-demo__score-label">{label}</span>
            <div className="wr-demo__score-bar">
                <motion.div
                    className="wr-demo__score-fill"
                    style={{ backgroundColor: color }}
                    initial={{ width: 0 }}
                    animate={{ width: isVisible ? `${widthPercent}%` : 0 }}
                    transition={{ duration: 0.8, delay: delay / 1000 }}
                />
            </div>
            <span className="wr-demo__score-value" style={{ color }}>
                {animatedScore.toFixed(1)}
            </span>
        </div>
    );
};

// Main component - Realistic Writing Result Preview
const InteractiveGradingDemo = () => {
    const sectionRef = useRef(null);
    const isInView = useInView(sectionRef, { once: true, margin: '-80px' });
    const [activeMode, setActiveMode] = useState('ai'); // 'basic' or 'ai'
    const [expandedSection, setExpandedSection] = useState('strengths');
    const [typedText, setTypedText] = useState('');

    // Typing animation for improved essay
    useEffect(() => {
        if (!isInView || activeMode !== 'ai') return;

        let index = 0;
        setTypedText('');
        const text = DEMO_ESSAY.improved;

        const interval = setInterval(() => {
            if (index < text.length) {
                setTypedText(text.slice(0, index + 1));
                index++;
            } else {
                clearInterval(interval);
            }
        }, 15);

        return () => clearInterval(interval);
    }, [isInView, activeMode]);

    return (
        <section ref={sectionRef} className="wr-demo-section">
            <div className="pricing-container">
                {/* Header */}
                <motion.div
                    className="pricing-section__header"
                    initial={{ opacity: 0, y: 20 }}
                    animate={isInView ? { opacity: 1, y: 0 } : {}}
                    transition={{ duration: 0.5 }}
                >
                    <h2 className="pricing-section__title">Trải nghiệm sự khác biệt</h2>
                    <p className="pricing-section__subtitle">
                        So sánh giữa {TERMINOLOGY.ATTEMPT} và {TERMINOLOGY.ATTEMPT_AI}
                    </p>
                </motion.div>

                {/* Mode Switcher */}
                <motion.div
                    className="wr-demo__switcher"
                    initial={{ opacity: 0 }}
                    animate={isInView ? { opacity: 1 } : {}}
                    transition={{ delay: 0.3 }}
                >
                    <button
                        className={`wr-demo__switch-btn ${activeMode === 'basic' ? 'active' : ''}`}
                        onClick={() => setActiveMode('basic')}
                    >
                        <span className="wr-demo__switch-badge wr-demo__switch-badge--basic">Cơ bản</span>
                        {TERMINOLOGY.ATTEMPT}
                    </button>
                    <button
                        className={`wr-demo__switch-btn ${activeMode === 'ai' ? 'active' : ''}`}
                        onClick={() => setActiveMode('ai')}
                    >
                        <span className="wr-demo__switch-badge wr-demo__switch-badge--ai">AI</span>
                        {TERMINOLOGY.ATTEMPT_AI}
                    </button>
                </motion.div>

                {/* Demo Preview Window */}
                <motion.div
                    className={`wr-demo__window ${activeMode === 'ai' ? 'wr-demo__window--ai' : ''}`}
                    initial={{ opacity: 0, y: 30 }}
                    animate={isInView ? { opacity: 1, y: 0 } : {}}
                    transition={{ duration: 0.6, delay: 0.2 }}
                >
                    {/* Mini Header */}
                    <div className="wr-demo__header">
                        <div className="wr-demo__header-left">
                            <span className="wr-demo__test-title">CAM17 · Test 1 · Writing</span>
                        </div>
                        <div className="wr-demo__header-right">
                            <div className="wr-demo__band-badge">
                                <span className="wr-demo__band-label">BAND</span>
                                <span className="wr-demo__band-value">7.5</span>
                            </div>
                        </div>
                    </div>

                    {/* Task Tabs */}
                    <div className="wr-demo__tabs">
                        <button className="wr-demo__tab active">
                            Task 1 <span className="wr-demo__tab-score">7.5</span>
                        </button>
                        <button className="wr-demo__tab">
                            Task 2 <span className="wr-demo__tab-score">8.0</span>
                        </button>
                    </div>

                    {/* Main Content - 3 Column Layout */}
                    <div className="wr-demo__content">
                        {/* Left Column - Đề bài */}
                        <div className="wr-demo__column wr-demo__column--prompt">
                            <div className="wr-demo__column-header">
                                <FiFileText size={14} />
                                <span>Đề bài</span>
                            </div>
                            <div className="wr-demo__column-body">
                                <p className="wr-demo__prompt-text">
                                    The two maps below show an industrial area in the town of Norbiton, and planned changes to it.
                                </p>
                                <div className="wr-demo__prompt-image">
                                    <div className="wr-demo__map-placeholder">
                                        <span>📍</span>
                                        <span>Map Image</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Middle Column - Bài viết */}
                        <div className="wr-demo__column wr-demo__column--essay">
                            <div className="wr-demo__column-header">
                                <FiEdit3 size={14} />
                                <span>Bài viết của bạn</span>
                                {activeMode === 'ai' && (
                                    <div className="wr-demo__view-toggle">
                                        <button className="active">Bài gốc</button>
                                        <button>So sánh</button>
                                    </div>
                                )}
                            </div>
                            <div className="wr-demo__column-body">
                                <AnimatePresence mode="wait">
                                    {activeMode === 'basic' ? (
                                        <motion.div
                                            key="basic"
                                            initial={{ opacity: 0 }}
                                            animate={{ opacity: 1 }}
                                            exit={{ opacity: 0 }}
                                            className="wr-demo__essay-basic"
                                        >
                                            <p>{DEMO_ESSAY.original}</p>
                                            <div className="wr-demo__basic-score">
                                                <div className="wr-demo__basic-band">7.5</div>
                                                <span>Band Score</span>
                                            </div>
                                        </motion.div>
                                    ) : (
                                        <motion.div
                                            key="ai"
                                            initial={{ opacity: 0 }}
                                            animate={{ opacity: 1 }}
                                            exit={{ opacity: 0 }}
                                            className="wr-demo__essay-ai"
                                        >
                                            {/* Comparison View */}
                                            <div className="wr-demo__comparison">
                                                <div className="wr-demo__compare-stats">
                                                    <span className="wr-demo__stat wr-demo__stat--changed">2 đoạn thay đổi</span>
                                                    <span className="wr-demo__stat wr-demo__stat--add">+29 thêm</span>
                                                    <span className="wr-demo__stat wr-demo__stat--remove">-24 bỏ</span>
                                                </div>
                                                <div className="wr-demo__compare-columns">
                                                    <div className="wr-demo__compare-col">
                                                        <div className="wr-demo__compare-label">Bài gốc</div>
                                                        <p className="wr-demo__compare-text">
                                                            <span className="wr-demo__highlight wr-demo__highlight--remove">factories clustered</span> around a central roundabout and <span className="wr-demo__highlight wr-demo__highlight--remove">along an eastern access road</span>, will be comprehensively redeveloped...
                                                        </p>
                                                    </div>
                                                    <div className="wr-demo__compare-col wr-demo__compare-col--improved">
                                                        <div className="wr-demo__compare-label">
                                                            Bài cải thiện <span className="wr-demo__ai-badge">AI</span>
                                                        </div>
                                                        <p className="wr-demo__compare-text">
                                                            <span className="wr-demo__highlight wr-demo__highlight--add">Currently, the industrial area is situated</span> south of a river, <span className="wr-demo__highlight wr-demo__highlight--add">adjacent to farmland</span> on the northern bank...
                                                        </p>
                                                    </div>
                                                </div>
                                            </div>
                                        </motion.div>
                                    )}
                                </AnimatePresence>
                            </div>
                        </div>

                        {/* Right Column - Phân tích chi tiết (AI only) */}
                        <div className={`wr-demo__column wr-demo__column--analysis ${activeMode !== 'ai' ? 'wr-demo__column--disabled' : ''}`}>
                            <div className="wr-demo__column-header">
                                <FiBarChart2 size={14} />
                                <span>Phân tích chi tiết</span>
                            </div>
                            <div className="wr-demo__column-body">
                                {activeMode === 'ai' ? (
                                    <>
                                        {/* Score Bars */}
                                        <div className="wr-demo__scores">
                                            {Object.entries(DEMO_SCORES).map(([key, data], idx) => (
                                                <AnimatedScoreBar
                                                    key={key}
                                                    label={data.label}
                                                    score={data.score}
                                                    color={data.color}
                                                    isVisible={isInView && activeMode === 'ai'}
                                                    delay={300 + idx * 150}
                                                />
                                            ))}
                                        </div>

                                        {/* Expandable Sections */}
                                        <div className="wr-demo__sections">
                                            {/* Strengths */}
                                            <div className={`wr-demo__section ${expandedSection === 'strengths' ? 'open' : ''}`}>
                                                <button
                                                    className="wr-demo__section-toggle"
                                                    onClick={() => setExpandedSection(expandedSection === 'strengths' ? '' : 'strengths')}
                                                >
                                                    <FiThumbsUp size={12} />
                                                    <span>Điểm mạnh ({DEMO_ANALYSIS.strengths.length})</span>
                                                    {expandedSection === 'strengths' ? <FiChevronDown size={12} /> : <FiChevronRight size={12} />}
                                                </button>
                                                {expandedSection === 'strengths' && (
                                                    <div className="wr-demo__section-content wr-demo__section-content--strengths">
                                                        {DEMO_ANALYSIS.strengths.map((s, i) => (
                                                            <div key={i} className="wr-demo__list-item">• {s}</div>
                                                        ))}
                                                    </div>
                                                )}
                                            </div>

                                            {/* Weaknesses */}
                                            <div className={`wr-demo__section ${expandedSection === 'weaknesses' ? 'open' : ''}`}>
                                                <button
                                                    className="wr-demo__section-toggle"
                                                    onClick={() => setExpandedSection(expandedSection === 'weaknesses' ? '' : 'weaknesses')}
                                                >
                                                    <FiAlertTriangle size={12} />
                                                    <span>Điểm yếu ({DEMO_ANALYSIS.weaknesses.length})</span>
                                                    {expandedSection === 'weaknesses' ? <FiChevronDown size={12} /> : <FiChevronRight size={12} />}
                                                </button>
                                                {expandedSection === 'weaknesses' && (
                                                    <div className="wr-demo__section-content wr-demo__section-content--weaknesses">
                                                        {DEMO_ANALYSIS.weaknesses.map((w, i) => (
                                                            <div key={i} className="wr-demo__list-item">• {w}</div>
                                                        ))}
                                                    </div>
                                                )}
                                            </div>

                                            {/* Suggestions */}
                                            <div className={`wr-demo__section ${expandedSection === 'suggestions' ? 'open' : ''}`}>
                                                <button
                                                    className="wr-demo__section-toggle"
                                                    onClick={() => setExpandedSection(expandedSection === 'suggestions' ? '' : 'suggestions')}
                                                >
                                                    <FiZap size={12} />
                                                    <span>Sửa lỗi ({DEMO_ANALYSIS.suggestions.length})</span>
                                                    {expandedSection === 'suggestions' ? <FiChevronDown size={12} /> : <FiChevronRight size={12} />}
                                                </button>
                                                {expandedSection === 'suggestions' && (
                                                    <div className="wr-demo__section-content">
                                                        {DEMO_ANALYSIS.suggestions.map((sug, i) => (
                                                            <div key={i} className="wr-demo__correction">
                                                                <span className="wr-demo__correction-original">{sug.original}</span>
                                                                <span className="wr-demo__correction-arrow">→</span>
                                                                <span className="wr-demo__correction-fixed">{sug.corrected}</span>
                                                            </div>
                                                        ))}
                                                    </div>
                                                )}
                                            </div>

                                            {/* More sections placeholder */}
                                            <div className="wr-demo__section">
                                                <button className="wr-demo__section-toggle">
                                                    <FiBook size={12} />
                                                    <span>Phân tích từ vựng (5)</span>
                                                    <FiChevronRight size={12} />
                                                </button>
                                            </div>
                                        </div>
                                    </>
                                ) : (
                                    <div className="wr-demo__locked">
                                        <div className="wr-demo__locked-icon">🔒</div>
                                        <p>Nâng cấp lên {TERMINOLOGY.ATTEMPT_AI} để xem phân tích chi tiết</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </motion.div>

                {/* Hint */}
                <motion.p
                    className="wr-demo__hint"
                    initial={{ opacity: 0 }}
                    animate={isInView ? { opacity: 0.6 } : {}}
                    transition={{ delay: 1 }}
                >
                    💡 Click vào các tab để xem sự khác biệt giữa hai loại chấm điểm
                </motion.p>
            </div>
        </section>
    );
};

export default InteractiveGradingDemo;
