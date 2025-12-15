import React, { useState, useRef, useMemo, useCallback } from 'react';
import { FiEye, FiEyeOff, FiInfo, FiChevronDown, FiChevronUp, FiMousePointer } from 'react-icons/fi';
import { computeParagraphDiffs, getDiffStats } from '../utils/textDiff';
import PropTypes from 'prop-types';

/**
 * EssayComparison Component - REWRITTEN
 * Displays side-by-side comparison of original essay and AI-enhanced version
 * with word-level diff highlighting and click-to-scroll functionality
 * 
 * Key Features:
 * - Split view: Original (left) | Improved (right)
 * - Synchronized scrolling between panels
 * - Click on any paragraph → scroll to corresponding analysis in right column
 * - Enhanced legends with clear descriptions
 */
const EssayComparison = ({
    originalEssay,
    paragraphRewrites,
    onItemClick, // Callback for scrolling to analysis: (itemId, sectionType) => void
    onClose
}) => {
    const [syncScroll, setSyncScroll] = useState(true);
    const [showLegend, setShowLegend] = useState(true);
    const [selectedParagraph, setSelectedParagraph] = useState(null);
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

    // Handle paragraph click - scroll to corresponding analysis in right column
    const handleParagraphClick = useCallback((paragraphIndex, hasDiff) => {
        // Find the corresponding paragraph rewrite
        const paraRewrite = paragraphRewrites?.find(p =>
            (p.paragraphNumber === paragraphIndex + 1) || (p.paragraph_index === paragraphIndex)
        );

        // Visual feedback - highlight selected paragraph briefly
        setSelectedParagraph(paragraphIndex);
        setTimeout(() => setSelectedParagraph(null), 1500);

        if (paraRewrite && onItemClick) {
            // If paragraph has an ID, use it to scroll to analysis
            if (paraRewrite.id) {
                onItemClick(paraRewrite.id, 'paragraphs');
            } else {
                // If no ID, scroll to the paragraphs section with the index
                onItemClick(`paragraph-${paragraphIndex}`, 'paragraphs');
            }
        }
    }, [paragraphRewrites, onItemClick]);

    // Render diff segments with styling
    const renderDiffSegments = (segments, side) => {
        return segments.map((segment, idx) => {
            if (segment.type === 'same') {
                return <span key={idx} className="diff-same">{segment.text}</span>;
            } else if (segment.type === 'add' && side === 'enhanced') {
                return (
                    <span key={idx} className="diff-add" title="Đã thêm/cải thiện">
                        {segment.text}
                    </span>
                );
            } else if (segment.type === 'remove' && side === 'original') {
                return (
                    <span key={idx} className="diff-remove" title="Đã xóa/thay thế">
                        {segment.text}
                    </span>
                );
            }
            return <span key={idx}>{segment.text}</span>;
        });
    };

    // Render paragraph with click handler
    const renderParagraph = (para, idx, side) => {
        const isSelected = selectedParagraph === idx;
        const isClickable = para.hasDiff;

        return (
            <div
                key={idx}
                className={`comparison-paragraph 
                    ${para.hasDiff ? 'has-diff' : ''} 
                    ${isClickable ? 'clickable' : ''}
                    ${isSelected ? 'selected' : ''}`}
                onClick={() => handleParagraphClick(idx, para.hasDiff)}
                title={isClickable ? 'Nhấn để xem phân tích chi tiết' : ''}
            >
                <span className="para-number">{idx + 1}</span>
                <p>
                    {side === 'original'
                        ? renderDiffSegments(para.originalDiff, 'original')
                        : renderDiffSegments(para.enhancedDiff, 'enhanced')
                    }
                </p>
                {para.hasDiff && side === 'original' && (
                    <span className="click-hint">
                        <FiMousePointer size={12} />
                    </span>
                )}
            </div>
        );
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

            {/* Enhanced Legend */}
            {showLegend && (
                <div className="comparison-legend enhanced">
                    <div className="legend-row">
                        <div className="legend-item">
                            <span className="legend-color diff-remove-sample"></span>
                            <div className="legend-text">
                                <strong>Đã xóa/thay thế</strong>
                                <span>Phần văn bản cần sửa hoặc thay thế</span>
                            </div>
                        </div>
                        <div className="legend-item">
                            <span className="legend-color diff-add-sample"></span>
                            <div className="legend-text">
                                <strong>Thêm mới/cải thiện</strong>
                                <span>Phần văn bản được AI cải thiện</span>
                            </div>
                        </div>
                    </div>
                    <div className="legend-hint">
                        <FiMousePointer size={12} />
                        <span>Nhấn vào đoạn văn có thay đổi để xem phân tích chi tiết bên phải</span>
                    </div>
                </div>
            )}

            {/* Side-by-side comparison */}
            <div className="comparison-panels">
                {/* Original Essay Panel */}
                <div className="comparison-panel comparison-original">
                    <div className="panel-label">
                        <span className="label-text">Bài gốc</span>
                        <span className="label-hint">Có {stats.paragraphsChanged} đoạn cần cải thiện</span>
                    </div>
                    <div
                        className="panel-content"
                        ref={leftRef}
                        onScroll={() => handleScroll('left')}
                    >
                        {paragraphDiffs.map((para, idx) => renderParagraph(para, idx, 'original'))}
                    </div>
                </div>

                {/* Enhanced Essay Panel */}
                <div className="comparison-panel comparison-enhanced">
                    <div className="panel-label">
                        <span className="label-text">Bài cải thiện</span>
                        <span className="label-badge">AI Band+1</span>
                    </div>
                    <div
                        className="panel-content"
                        ref={rightRef}
                        onScroll={() => handleScroll('right')}
                    >
                        {paragraphDiffs.map((para, idx) => renderParagraph(para, idx, 'enhanced'))}
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
        paragraphNumber: PropTypes.number,
        improvements_made: PropTypes.arrayOf(PropTypes.string),
        id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    })),
    onItemClick: PropTypes.func,
    onClose: PropTypes.func,
};

EssayComparison.defaultProps = {
    paragraphRewrites: [],
    onItemClick: null,
    onClose: null,
};

export default EssayComparison;
