import React, { useState, useEffect } from 'react';
import { FiArrowLeft, FiCheckCircle, FiZap, FiInfo } from 'react-icons/fi';
import '../../css/common/GradingLoader.css';

/**
 * GradingLoader - A reusable AI grading loading screen component
 * 
 * @param {Object} props
 * @param {'PENDING'|'GRADING'|'COMPLETED'|'FAILED'} props.status - Current grading status
 * @param {Array<{key: string, label: string}>} props.stages - Array of stage objects
 * @param {Object} props.carousels - Carousel content { action: string[], tips: {icon, text}[], stats: {icon, text}[] }
 * @param {Function} props.onBackClick - Callback when back button is clicked
 * @param {string} props.backButtonText - Text for back button (default: "Quay về Dashboard")
 * @param {string} props.title - Main title (default: "Cramer đang phân tích")
 * @param {string} props.subtitle - Subtitle text
 */
const GradingLoader = ({
    status = 'PENDING',
    taskStatuses = {}, // NEW: { 1: 'PENDING', 2: 'GRADING' }
    stages = [
        { key: 'receive', label: 'Nhận bài' },
        { key: 'task1', label: 'Task 1' },
        { key: 'task2', label: 'Task 2' },
        { key: 'generate', label: 'Hoàn tất' },
    ],
    carousels = {
        action: [
            "Đang phân tích cấu trúc bài viết...",
            "Đang kiểm tra ngữ pháp và cấu trúc câu...",
            "Đang đánh giá độ đa dạng từ vựng...",
            "Đang phân tích tính mạch lạc và liên kết...",
            "Đang so sánh với tiêu chuẩn IELTS...",
            "Đang tạo nhận xét chi tiết...",
        ],
        tips: [
            { icon: '💡', text: 'Sử dụng từ vựng đa dạng giúp tăng điểm Lexical Resource đáng kể.' },
            { icon: '📝', text: 'Câu phức và câu ghép chính xác là chìa khóa cho điểm Grammar cao.' },
            { icon: '🔗', text: 'Từ nối (linking words) giúp bài viết mạch lạc và dễ đọc hơn.' },
            { icon: '📊', text: 'Trả lời đúng trọng tâm đề bài là yếu tố quan trọng nhất.' },
            { icon: '✍️', text: 'Đoạn văn cần có câu chủ đề rõ ràng và các câu hỗ trợ.' },
            { icon: '🎯', text: 'Task 1 cần ít nhất 150 từ, Task 2 cần ít nhất 250 từ.' },
        ],
        stats: [
            { icon: '📈', text: 'Band 7+ yêu cầu ít nhất 25% câu phức trong bài viết.' },
            { icon: '🌍', text: 'Hơn 3 triệu người thi IELTS mỗi năm trên toàn cầu.' },
            { icon: '⏱️', text: 'Thời gian làm bài Writing: 20 phút Task 1, 40 phút Task 2.' },
            { icon: '📚', text: 'AI Cramer phân tích hơn 50 yếu tố ngôn ngữ trong bài viết.' },
            { icon: '🏆', text: 'Chỉ 1% thí sinh đạt Band 9 trong Writing.' },
            { icon: '💪', text: 'Luyện tập đều đặn có thể tăng 0.5-1.0 band trong 3 tháng.' },
        ],
    },
    onBackClick,
    backButtonText = "Quay về Dashboard",
    title = "Cramer đang phân tích",
    subtitle = "AI đang đọc và đánh giá bài viết của bạn theo tiêu chuẩn IELTS",
}) => {
    // Multi-carousel state
    const [currentStageIndex, setCurrentStageIndex] = useState(0);
    const [progressPercent, setProgressPercent] = useState(5);
    const [carouselIndices, setCarouselIndices] = useState({ action: 0, tips: 0, stats: 0 });

    // Cycle through carousels with different intervals
    useEffect(() => {
        const actionInterval = setInterval(() => {
            setCarouselIndices(prev => ({ ...prev, action: (prev.action + 1) % carousels.action.length }));
        }, 3000);

        const tipsInterval = setInterval(() => {
            setCarouselIndices(prev => ({ ...prev, tips: (prev.tips + 1) % carousels.tips.length }));
        }, 5000);

        const statsInterval = setInterval(() => {
            setCarouselIndices(prev => ({ ...prev, stats: (prev.stats + 1) % carousels.stats.length }));
        }, 4000);

        return () => {
            clearInterval(actionInterval);
            clearInterval(tipsInterval);
            clearInterval(statsInterval);
        };
    }, [carousels]);

    // Progress animation - smooth increment
    useEffect(() => {
        const progressInterval = setInterval(() => {
            setProgressPercent(prev => {
                const targetByStatus = {
                    'PENDING': 20,
                    'GRADING': 75,
                    'COMPLETED': 100,
                    'PARTIAL_FAILURE': 100, // Treat partial failure as completed process
                    'FAILED': 100,
                };
                const target = targetByStatus[status] || 20;

                if (prev < target) {
                    const increment = Math.max(0.5, (target - prev) * 0.08);
                    return Math.min(prev + increment, target);
                }
                return prev;
            });
        }, 100);

        return () => clearInterval(progressInterval);
    }, [status]);

    // Update stage index based on actual task statuses
    useEffect(() => {
        const calculateStageIndex = () => {
            if (status === 'COMPLETED' || status === 'PARTIAL_FAILURE') return stages.length - 1;

            // Map task statuses to stage indices
            const task1Status = taskStatuses[1] || 'PENDING';
            const task2Status = taskStatuses[2] || 'PENDING';

            // Stage 0: receive (always done if not PENDING)
            if (status === 'PENDING') return 0;

            // Stage 1: task1
            if (task1Status === 'GRADING') return 1;
            if (task1Status === 'COMPLETED' && task2Status === 'PENDING') return 1;

            // Stage 2: task2
            if (task2Status === 'GRADING') return 2;
            if (task1Status === 'COMPLETED' && task2Status === 'COMPLETED') return 3;

            // Stage 3: generate (both completed)
            if (task1Status === 'COMPLETED' && task2Status === 'COMPLETED') {
                return 3;
            }

            // Default to stage based on GRADING status
            return status === 'GRADING' ? 1 : 0;
        };

        setCurrentStageIndex(calculateStageIndex());
    }, [status, taskStatuses, stages.length]);

    return (
        <div className="grading-loader">
            {/* Animated background with particles */}
            <div className="grading-loader-bg">
                <div className="bg-gradient-layer" />
                {[...Array(15)].map((_, i) => (
                    <div
                        key={i}
                        className={`floating-particle particle-${(i % 5) + 1}`}
                        style={{
                            left: `${Math.random() * 100}%`,
                            top: `${Math.random() * 100}%`,
                            animationDelay: `${Math.random() * 5}s`,
                            animationDuration: `${8 + Math.random() * 4}s`
                        }}
                    />
                ))}
                <div className="floating-shape shape-1" />
                <div className="floating-shape shape-2" />
                <div className="floating-shape shape-3" />
            </div>

            <div className="grading-loader-container">
                <div className="grading-loader-card">

                    {/* Neural Network Animation */}
                    <div className="neural-network-container">
                        <div className="neural-network">
                            <div className="brain-core">
                                <div className="core-inner" />
                                <div className="core-pulse pulse-1" />
                                <div className="core-pulse pulse-2" />
                                <div className="core-pulse pulse-3" />
                            </div>

                            {[...Array(8)].map((_, i) => (
                                <div key={i} className={`neural-node node-${i + 1}`}>
                                    <div className="node-dot" />
                                    <div className="node-connection" />
                                </div>
                            ))}

                            <div className="data-particles">
                                {[...Array(12)].map((_, i) => (
                                    <div
                                        key={i}
                                        className={`data-particle dp-${(i % 4) + 1}`}
                                        style={{ animationDelay: `${i * 0.3}s` }}
                                    />
                                ))}
                            </div>
                        </div>

                        <div className="ai-badge-floating">
                            <span className="badge-sparkle">✨</span>
                            <span className="badge-text">AI GRADING</span>
                        </div>
                    </div>

                    {/* Title Section */}
                    <div className="loading-title-section">
                        <h2 className="grading-title-new">
                            <span className="title-gradient">{title}</span>
                            <span className="typing-indicator">
                                <span className="dot" />
                                <span className="dot" />
                                <span className="dot" />
                            </span>
                        </h2>
                        <p className="grading-subtitle-new">{subtitle}</p>
                    </div>

                    {/* Stage Progress Indicator */}
                    <div className="stage-progress-container">
                        <div className="stage-progress-track">
                            {stages.map((stage, idx) => (
                                <React.Fragment key={stage.key}>
                                    <div className={`stage-item ${idx < currentStageIndex ? 'completed' :
                                        idx === currentStageIndex ? 'active' : 'pending'
                                        }`}>
                                        <div className="stage-icon-wrapper">
                                            {idx < currentStageIndex ? (
                                                <FiCheckCircle size={18} />
                                            ) : idx === currentStageIndex ? (
                                                <div className="stage-spinner" />
                                            ) : (
                                                <span className="stage-number">{idx + 1}</span>
                                            )}
                                        </div>
                                        <span className="stage-label">{stage.label}</span>
                                    </div>
                                    {idx < stages.length - 1 && (
                                        <div className={`stage-connector ${idx < currentStageIndex ? 'filled' : ''}`}>
                                            <div className="connector-fill" style={{
                                                width: idx < currentStageIndex ? '100%' :
                                                    idx === currentStageIndex ? '50%' : '0%'
                                            }} />
                                        </div>
                                    )}
                                </React.Fragment>
                            ))}
                        </div>
                    </div>

                    {/* Segmented Progress Bar */}
                    <div className="segmented-progress-container">
                        <div className="progress-header">
                            <span className="progress-label">Tiến trình phân tích</span>
                            <span className="progress-value">{Math.round(progressPercent)}%</span>
                        </div>
                        <div className="segmented-progress-bar">
                            <div className="progress-segments">
                                {['TA', 'CC', 'LR', 'GR'].map((segment, idx) => (
                                    <div key={segment} className={`segment segment-${idx + 1} ${progressPercent > (idx + 1) * 25 ? 'filled' :
                                        progressPercent > idx * 25 ? 'filling' : 'empty'
                                        }`}>
                                        <div className="segment-fill" style={{
                                            width: progressPercent > (idx + 1) * 25 ? '100%' :
                                                progressPercent > idx * 25 ? `${((progressPercent - idx * 25) / 25) * 100}%` : '0%'
                                        }}>
                                            <div className="segment-wave" />
                                        </div>
                                        <span className="segment-label">{segment}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                        <div className="segment-legend">
                            <span><strong>TA</strong> Task</span>
                            <span><strong>CC</strong> Coherence</span>
                            <span><strong>LR</strong> Lexical</span>
                            <span><strong>GR</strong> Grammar</span>
                        </div>
                    </div>

                    {/* Three Carousels */}
                    <div className="carousels-container">
                        {/* Carousel 1: Current Action */}
                        <div className="carousel-card carousel-action">
                            <div className="carousel-header">
                                <FiZap size={16} className="carousel-icon" />
                                <span className="carousel-title">Đang thực hiện</span>
                            </div>
                            <div className="carousel-content">
                                <p key={carouselIndices.action} className="carousel-text slide-in">
                                    {carousels.action[carouselIndices.action]}
                                </p>
                            </div>
                            <div className="carousel-indicators">
                                {carousels.action.map((_, idx) => (
                                    <span key={idx} className={`indicator ${idx === carouselIndices.action ? 'active' : ''}`} />
                                ))}
                            </div>
                        </div>

                        {/* Carousel 2: Tips */}
                        <div className="carousel-card carousel-tips">
                            <div className="carousel-header">
                                <span className="carousel-emoji">{carousels.tips[carouselIndices.tips].icon}</span>
                                <span className="carousel-title">Mẹo Writing IELTS</span>
                            </div>
                            <div className="carousel-content">
                                <p key={carouselIndices.tips} className="carousel-text slide-in">
                                    {carousels.tips[carouselIndices.tips].text}
                                </p>
                            </div>
                            <div className="carousel-indicators">
                                {carousels.tips.map((_, idx) => (
                                    <span key={idx} className={`indicator ${idx === carouselIndices.tips ? 'active' : ''}`} />
                                ))}
                            </div>
                        </div>

                        {/* Carousel 3: Statistics */}
                        <div className="carousel-card carousel-stats">
                            <div className="carousel-header">
                                <span className="carousel-emoji">{carousels.stats[carouselIndices.stats].icon}</span>
                                <span className="carousel-title">Bạn có biết?</span>
                            </div>
                            <div className="carousel-content">
                                <p key={carouselIndices.stats} className="carousel-text slide-in">
                                    {carousels.stats[carouselIndices.stats].text}
                                </p>
                            </div>
                            <div className="carousel-indicators">
                                {carousels.stats.map((_, idx) => (
                                    <span key={idx} className={`indicator ${idx === carouselIndices.stats ? 'active' : ''}`} />
                                ))}
                            </div>
                        </div>
                    </div>

                    {/* Info Notice */}
                    <div className="loading-notice">
                        <FiInfo size={14} />
                        <span>Bạn có thể đóng trang này và quay lại sau — kết quả sẽ được lưu tự động!</span>
                    </div>

                    {/* Action Button */}
                    {onBackClick && (
                        <button className="grading-loader-back-btn" onClick={onBackClick}>
                            <FiArrowLeft size={16} />
                            <span>{backButtonText}</span>
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default GradingLoader;
