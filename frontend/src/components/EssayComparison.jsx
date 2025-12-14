import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { FiEye, FiEyeOff, FiInfo, FiChevronDown, FiChevronUp } from 'react-icons/fi';
import { computeParagraphDiffs, getDiffStats } from '../utils/textDiff';
import PropTypes from 'prop-types';

/**
 * EssayComparison Component
 * Displays side-by-side comparison of original essay and AI-enhanced version
 * with word-level diff highlighting
 */
const EssayComparison = ({ 
    originalEssay, 
    paragraphRewrites,
    onClose 
}) => {
    const [syncScroll, setSyncScroll] = useState(true);
    const [showLegend, setShowLegend] = useState(true);
    const leftRef = useRef(null);
    const rightRef = useRef(null);
    const isScrolling = useRef(false);

    // Compute diffs for all paragraphs
    const paragraphDiffs = useMemo(() => {
        return computeParagraphDiffs(originalEssay, paragraphRewrites);
    }, [originalEssay, paragraphRewrites]);

    // Get stats
    const stats = useMemo(() => {
        return getDiffStats(paragraphDiffs);
    }, [paragraphDiffs]);

    // Check if enhanced version is available
    const hasEnhancedVersion = paragraphRewrites?.length > 0 && 
        paragraphRewrites.some(p => p.improved);

    // Sync scroll handler
    const handleScroll = useCallback((source) => {
        if (!syncScroll || isScrolling.current) return;
        
        isScrolling.current = true;
        
        const sourceEl = source === 'left' ? leftRef.current : rightRef.current;
        const targetEl = source === 'left' ? rightRef.current : leftRef.current;
        
        if (sourceEl && targetEl) {
            const scrollPercentage = sourceEl.scrollTop / (sourceEl.scrollHeight - sourceEl.clientHeight);
            targetEl.scrollTop = scrollPercentage * (targetEl.scrollHeight - targetEl.clientHeight);
        }
        
        setTimeout(() => {
            isScrolling.current = false;
        }, 50);
    }, [syncScroll]);

    // Render diff segments
    const renderDiffSegments = (segments, side) => {
        return segments.map((segment, idx) => {
            if (segment.type === 'same') {
                return <span key={idx} className="diff-same">{segment.text}</span>;
            } else if (segment.type === 'add' && side === 'enhanced') {
                return <span key={idx} className="diff-add">{segment.text}</span>;
            } else if (segment.type === 'remove' && side === 'original') {
                return <span key={idx} className="diff-remove">{segment.text}</span>;
            }
            return <span key={idx}>{segment.text}</span>;
        });
    };

    if (!hasEnhancedVersion) {
        return (
            <div className="comparison-unavailable">
                <FiInfo size={24} />
                <p>Phiên bản cải thiện không khả dụng</p>
                <span className="hint">
                    AI chưa tạo phiên bản viết lại cho bài viết này.
                </span>
            </div>
        );
    }

    return (
        <div className="essay-comparison">
            {/* Comparison Header with Stats */}
            <div className="comparison-header">
                <div className="comparison-stats">
                    <span className="stat">
                        <span className="stat-value">{stats.paragraphsChanged}</span>
                        <span className="stat-label">đoạn thay đổi</span>
                    </span>
                    <span className="stat additions">
                        <span className="stat-value">+{stats.additions}</span>
                        <span className="stat-label">thêm</span>
                    </span>
                    <span className="stat removals">
                        <span className="stat-value">-{stats.removals}</span>
                        <span className="stat-label">bỏ</span>
                    </span>
                </div>
                <div className="comparison-controls">
                    <button 
                        className={`control-btn ${syncScroll ? 'active' : ''}`}
                        onClick={() => setSyncScroll(!syncScroll)}
                        title={syncScroll ? 'Tắt cuộn đồng bộ' : 'Bật cuộn đồng bộ'}
                    >
                        {syncScroll ? <FiEye size={14} /> : <FiEyeOff size={14} />}
                        <span>Đồng bộ cuộn</span>
                    </button>
                    <button
                        className={`control-btn ${showLegend ? 'active' : ''}`}
                        onClick={() => setShowLegend(!showLegend)}
                    >
                        {showLegend ? <FiChevronUp size={14} /> : <FiChevronDown size={14} />}
                        <span>Chú thích</span>
                    </button>
                </div>
            </div>

            {/* Legend */}
            {showLegend && (
                <div className="comparison-legend">
                    <div className="legend-item">
                        <span className="legend-color diff-remove-sample"></span>
                        <span>Đã xóa/thay thế</span>
                    </div>
                    <div className="legend-item">
                        <span className="legend-color diff-add-sample"></span>
                        <span>Thêm mới/cải thiện</span>
                    </div>
                </div>
            )}

            {/* Side-by-side comparison */}
            <div className="comparison-panels">
                {/* Original Essay Panel */}
                <div className="comparison-panel comparison-original">
                    <div className="panel-label">
                        <span className="label-text">Bài gốc</span>
                    </div>
                    <div 
                        className="panel-content"
                        ref={leftRef}
                        onScroll={() => handleScroll('left')}
                    >
                        {paragraphDiffs.map((para, idx) => (
                            <div 
                                key={idx} 
                                className={`comparison-paragraph ${para.hasDiff ? 'has-diff' : ''}`}
                            >
                                <span className="para-number">{idx + 1}</span>
                                <p>
                                    {renderDiffSegments(para.originalDiff, 'original')}
                                </p>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Enhanced Essay Panel */}
                <div className="comparison-panel comparison-enhanced">
                    <div className="panel-label">
                        <span className="label-text">Bài cải thiện</span>
                        <span className="label-badge">AI</span>
                    </div>
                    <div 
                        className="panel-content"
                        ref={rightRef}
                        onScroll={() => handleScroll('right')}
                    >
                        {paragraphDiffs.map((para, idx) => (
                            <div 
                                key={idx} 
                                className={`comparison-paragraph ${para.hasDiff ? 'has-diff' : ''}`}
                            >
                                <span className="para-number">{idx + 1}</span>
                                <p>
                                    {renderDiffSegments(para.enhancedDiff, 'enhanced')}
                                </p>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

EssayComparison.propTypes = {
    originalEssay: PropTypes.string.isRequired,
    paragraphRewrites: PropTypes.arrayOf(PropTypes.shape({
        original: PropTypes.string,
        improved: PropTypes.string,
        paragraph_index: PropTypes.number,
        improvements_made: PropTypes.arrayOf(PropTypes.string),
    })),
    onClose: PropTypes.func,
};

EssayComparison.defaultProps = {
    paragraphRewrites: [],
    onClose: () => {},
};

export default EssayComparison;
