import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { PanelGroup, Panel, PanelResizeHandle } from 'react-resizable-panels';
import { writingApi } from '../api/backendApi';
import { 
    FiArrowLeft, FiRefreshCw, FiChevronDown, FiChevronRight,
    FiFileText, FiEdit3, FiBarChart2, FiCheckCircle, FiXCircle,
    FiAlertCircle, FiZap, FiTrendingUp, FiAward, FiBook,
    FiTarget, FiThumbsUp, FiAlertTriangle, FiEdit, FiInfo, FiRotateCw
} from 'react-icons/fi';

import '../css/WritingResultPage.css';

// Error type colors with Vietnamese labels - consistent across essay highlights and analysis sections
const ERROR_TYPE_COLORS = {
    grammar: { bg: '#fef2f2', border: '#ef4444', text: '#dc2626', label: 'Ngữ pháp' },
    spelling: { bg: '#fff7ed', border: '#f97316', text: '#ea580c', label: 'Chính tả' },
    vocabulary: { bg: '#faf5ff', border: '#8b5cf6', text: '#7c3aed', label: 'Từ vựng' },
    punctuation: { bg: '#ecfeff', border: '#06b6d4', text: '#0891b2', label: 'Dấu câu' },
    coherence: { bg: '#f0fdf4', border: '#10b981', text: '#059669', label: 'Mạch lạc' },
    style: { bg: '#fefce8', border: '#eab308', text: '#ca8a04', label: 'Văn phong' },
};

const getErrorStyle = (errorType) => {
    return ERROR_TYPE_COLORS[errorType?.toLowerCase()] || { bg: '#f3f4f6', border: '#6b7280', text: '#4b5563' };
};

const WritingResultPage = () => {
    const { attemptId } = useParams();
    const navigate = useNavigate();

    // Core state
    const [review, setReview] = useState(null);
    const [loading, setLoading] = useState(true);
    const [gradingStatus, setGradingStatus] = useState('PENDING');
    const [error, setError] = useState(null);
    const [activeTask, setActiveTask] = useState(1);
    
    // UI state
    const [expandedScores, setExpandedScores] = useState({});
    const [scoresBarCollapsed, setScoresBarCollapsed] = useState(false);
    const [expandedSections, setExpandedSections] = useState({
        corrections: true,
        paragraphs: false,
        sampleBandPlus: false,
        sampleBand9: false,
        wordAnalysis: false,
    });
    const [selectedItemId, setSelectedItemId] = useState(null);
    const [isRegrading, setIsRegrading] = useState(false);
    
    // Refs for scroll-to functionality
    const analysisColumnRef = useRef(null);
    const itemRefs = useRef({});

    // Poll for grading status
    useEffect(() => {
        let pollInterval;

        const checkStatus = async () => {
            try {
                const statusRes = await writingApi.getGradingStatus(attemptId);
                const status = statusRes.data.status;
                setGradingStatus(status);

                if (status === 'COMPLETED' || status === 'PARTIAL_FAILURE') {
                    clearInterval(pollInterval);
                    const reviewRes = await writingApi.getWritingReview(attemptId);
                    setReview(reviewRes.data);
                    setLoading(false);
                } else if (status === 'FAILED') {
                    clearInterval(pollInterval);
                    setError('Chấm điểm thất bại. Vui lòng thử lại.');
                    setLoading(false);
                }
            } catch (err) {
                console.error('Error checking grading status:', err);
                setError('Không thể kiểm tra trạng thái chấm điểm.');
                setLoading(false);
                clearInterval(pollInterval);
            }
        };

        checkStatus();
        pollInterval = setInterval(checkStatus, 3000);
        return () => clearInterval(pollInterval);
    }, [attemptId]);

    // Helpers
    const getTaskReview = useCallback((taskNumber) => {
        if (!review?.tasks) return null;
        return review.tasks.find(t => t.taskNumber === taskNumber);
    }, [review]);

    const getTaskPrompt = useCallback((taskNumber) => {
        if (!review?.prompts) return null;
        return review.prompts.find(p => p.taskNumber === taskNumber);
    }, [review]);

    const currentTaskReview = getTaskReview(activeTask);
    const currentTaskPrompt = getTaskPrompt(activeTask);
    
    // Get AI feedback with proper key mapping
    const aiFeedback = useMemo(() => {
        if (!currentTaskReview?.aiFeedback) return {};
        const feedback = currentTaskReview.aiFeedback;
        return {
            sentenceCorrections: feedback.sentence_corrections || [],
            paragraphRewrites: feedback.paragraph_rewrites || [],
            sampleEssayBandPlus: feedback.sample_essay_band_plus_one || '',
            sampleEssayBand9: feedback.sample_essay_band_9 || '',
            feedbackSummary: feedback.feedback_summary || {},
            wordAnalysis: feedback.word_analysis || [],
            criteriaComments: feedback.criteria_comments || {},
            error: feedback.error || null,
        };
    }, [currentTaskReview]);

    // Get band scores
    const bandScores = useMemo(() => {
        if (!currentTaskReview?.bandScores) return {};
        const scores = currentTaskReview.bandScores;
        return {
            taskAchievement: scores.task_achievement || scores.task_response || 0,
            coherenceCohesion: scores.coherence_cohesion || 0,
            lexicalResource: scores.lexical_resource || 0,
            grammaticalRange: scores.grammatical_range_accuracy || 0,
        };
    }, [currentTaskReview]);

    // Build highlight map for essay
    const highlightMap = useMemo(() => {
        const map = [];
        
        // Add sentence corrections
        aiFeedback.sentenceCorrections?.forEach((corr, idx) => {
            if (corr.original) {
                map.push({
                    id: `correction-${idx}`,
                    text: corr.original,
                    type: corr.error_type || 'grammar',
                    category: 'corrections',
                });
            }
        });

        // Add paragraph issues (just mark first sentence of each paragraph)
        aiFeedback.paragraphRewrites?.forEach((para, idx) => {
            if (para.original) {
                const firstSentence = para.original.split('.')[0];
                if (firstSentence) {
                    map.push({
                        id: `paragraph-${idx}`,
                        text: firstSentence,
                        type: 'coherence',
                        category: 'paragraphs',
                    });
                }
            }
        });

        return map;
    }, [aiFeedback]);

    // Scroll to analysis item
    const scrollToItem = useCallback((itemId, category) => {
        setSelectedItemId(itemId);
        
        // Expand the relevant section if collapsed
        if (!expandedSections[category]) {
            setExpandedSections(prev => ({ ...prev, [category]: true }));
        }

        // Wait for expansion animation then scroll
        setTimeout(() => {
            const element = itemRefs.current[itemId];
            if (element && analysisColumnRef.current) {
                element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                element.classList.add('highlight-flash');
                setTimeout(() => element.classList.remove('highlight-flash'), 1500);
            }
        }, 100);
    }, [expandedSections]);

    // Handle re-grade request
    const handleRegrade = async () => {
        if (isRegrading) return;
        
        try {
            setIsRegrading(true);
            setLoading(true);
            setGradingStatus('PENDING');
            setReview(null);
            
            await writingApi.regradeAttempt(attemptId);
            // The useEffect polling will pick up the new grading status
        } catch (err) {
            console.error('Failed to start re-grading:', err);
            setError('Không thể bắt đầu chấm lại. Vui lòng thử lại sau.');
            setLoading(false);
            setIsRegrading(false);
        }
    };

    // Toggle section
    const toggleSection = (section) => {
        setExpandedSections(prev => ({ ...prev, [section]: !prev[section] }));
    };

    // Toggle score detail
    const toggleScoreDetail = (criterionKey) => {
        setExpandedScores(prev => ({ ...prev, [criterionKey]: !prev[criterionKey] }));
    };

    // Render essay with interactive highlights
    const renderHighlightedEssay = () => {
        const essayText = currentTaskReview?.essayText || '';
        if (!essayText) return <p className="no-content">Không có nội dung bài viết.</p>;

        let result = essayText;
        
        // Sort highlights by length (longest first) to avoid nested replacements
        const sortedHighlights = [...highlightMap].sort((a, b) => b.text.length - a.text.length);

        // Create a working copy with markers
        sortedHighlights.forEach((highlight) => {
            const escapedText = highlight.text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const regex = new RegExp(escapedText, 'gi');
            const style = getErrorStyle(highlight.type);
            result = result.replace(regex, `<mark class="essay-highlight" data-id="${highlight.id}" data-category="${highlight.category}" style="background-color: ${style.bg}; border-bottom: 2px solid ${style.border}; cursor: pointer;">${highlight.text}</mark>`);
        });

        // Split into paragraphs
        const paragraphs = result.split('\n').filter(p => p.trim());

        return (
            <div 
                className="essay-content"
                onClick={(e) => {
                    const mark = e.target.closest('mark.essay-highlight');
                    if (mark) {
                        const itemId = mark.dataset.id;
                        const category = mark.dataset.category;
                        scrollToItem(itemId, category);
                    }
                }}
            >
                {paragraphs.map((para, idx) => (
                    <p key={idx} dangerouslySetInnerHTML={{ __html: para }} />
                ))}
            </div>
        );
    };

    // Get score level description
    const getScoreLevel = (score) => {
        if (score >= 8) return { label: 'Xuất sắc', color: '#16a34a' };
        if (score >= 7) return { label: 'Rất tốt', color: '#22c55e' };
        if (score >= 6) return { label: 'Tốt', color: '#ca8a04' };
        if (score >= 5) return { label: 'Khá', color: '#ea580c' };
        return { label: 'Cần cải thiện', color: '#dc2626' };
    };

    // Render score bar component
    const renderScoreBar = (label, key, score, comment) => {
        const level = getScoreLevel(score);
        const isExpanded = expandedScores[key];
        const widthPercent = (score / 9) * 100;

        return (
            <div className={`score-criterion ${isExpanded ? 'expanded' : ''}`} key={key}>
                <button 
                    className="score-criterion-header"
                    onClick={() => toggleScoreDetail(key)}
                >
                    <div className="criterion-info">
                        <span className="criterion-label">{label}</span>
                        <div className="criterion-bar-container">
                            <div 
                                className="criterion-bar" 
                                style={{ width: `${widthPercent}%`, backgroundColor: level.color }}
                            />
                        </div>
                    </div>
                    <div className="criterion-score">
                        <span className="score-value" style={{ color: level.color }}>{score ? score.toFixed(1) : 'N/A'}</span>
                        <span className="expand-icon">{isExpanded ? '▼' : '▶'}</span>
                    </div>
                </button>
                {isExpanded && (
                    <div className="criterion-comment">
                        <p>{comment || 'Không có nhận xét chi tiết.'}</p>
                    </div>
                )}
            </div>
        );
    };

    // Loading state
    if (loading) {
        return (
            <div className="writing-result-loading">
                <div className="grading-animation-container">
                    <div className="grading-animation">
                        <div className="ai-brain">
                            <FiBarChart2 size={48} className="brain-icon" />
                            <div className="pulse-ring" />
                            <div className="pulse-ring delay-1" />
                            <div className="pulse-ring delay-2" />
                        </div>
                        <h2>Cramer đang chấm điểm bài viết...</h2>
                        <p>Quá trình này sẽ mất khoảng 2 phút.</p>
                        <p className="grading-tip"><FiInfo size={14} /> Bạn có thể đóng trang này và quay lại sau - kết quả sẽ được lưu tự động.</p>
                        
                        <div className="grading-steps">
                            <div className={`step ${gradingStatus !== 'PENDING' ? 'done' : 'active'}`}>
                                <FiFileText size={18} className="step-icon" />
                                <span>Nhận bài</span>
                            </div>
                            <div className={`step ${gradingStatus === 'GRADING' ? 'active' : gradingStatus === 'COMPLETED' ? 'done' : ''}`}>
                                <FiTarget size={18} className="step-icon" />
                                <span>Phân tích</span>
                            </div>
                            <div className={`step ${gradingStatus === 'COMPLETED' ? 'done' : ''}`}>
                                <FiCheckCircle size={18} className="step-icon" />
                                <span>Hoàn thành</span>
                            </div>
                        </div>
                        
                        <div className="progress-bar-container">
                            <div className={`progress-bar ${gradingStatus === 'GRADING' ? 'grading' : gradingStatus === 'COMPLETED' ? 'done' : ''}`} />
                        </div>
                        
                        <button className="back-to-dashboard-btn" onClick={() => navigate('/dashboard')}>
                            <FiArrowLeft size={16} /> Quay về Dashboard
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // Error state
    if (error) {
        return (
            <div className="writing-result-error">
                <div className="error-content">
                    <FiXCircle size={48} className="error-icon" />
                    <h2>Đã xảy ra lỗi</h2>
                    <p>{error}</p>
                    <button onClick={() => navigate('/dashboard')}>Quay về Dashboard</button>
                </div>
            </div>
        );
    }

    return (
        <div className="writing-result-page">
            {/* Purple Header - Unified Design */}
            <header className="review-header">
                <div className="review-header-top">
                    <div className="review-header-left">
                        <button className="back-btn" onClick={() => navigate('/dashboard')}>
                            <FiArrowLeft size={14} /> Quay lại
                        </button>
                        <h1 className="review-title">{review?.examSource?.toUpperCase()} · Test {review?.testNumber} · Writing</h1>
                    </div>
                    <div className="review-header-center">
                        <div className="summary-item">
                            <span className="summary-label">THỜI GIAN LÀM</span>
                            <span className="summary-value">
                                {review?.completedAt ? new Date(review.completedAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : 'N/A'}
                            </span>
                        </div>
                        <div className="summary-item">
                            <span className="summary-label">NGÀY LÀM</span>
                            <span className="summary-value">
                                {review?.completedAt ? new Date(review.completedAt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }) : 'N/A'}
                            </span>
                        </div>
                    </div>
                    <div className="review-header-right">
                        <div className="header-actions">
                            <button 
                                className="btn btn-regrade"
                                onClick={handleRegrade}
                                disabled={isRegrading}
                                title="Chấm lại bài viết với AI"
                            >
                                <FiRotateCw size={14} className={isRegrading ? 'spinning' : ''} /> Chấm lại
                            </button>
                            <Link to="/dashboard" className="btn btn-secondary">
                                Dashboard
                            </Link>
                        </div>
                        <div className="band-badge">
                            <span className="label">BAND</span>
                            <span className={`value band-${Math.floor(review?.overallBand || 0)}`}>
                                {review?.overallBand ? Number(review.overallBand).toFixed(1) : 'N/A'}
                            </span>
                        </div>
                    </div>
                </div>
            </header>

            {/* Task Tabs */}
            <div className="result-task-tabs">
                {review?.tasks?.sort((a, b) => a.taskNumber - b.taskNumber).map(task => (
                    <button
                        key={task.taskNumber}
                        className={`task-tab ${activeTask === task.taskNumber ? 'active' : ''}`}
                        onClick={() => setActiveTask(task.taskNumber)}
                    >
                        <span className="task-name">Task {task.taskNumber}</span>
                        <span className={`task-band band-${Math.floor(task.overallBand || 0)}`}>
                            {task.overallBand ? Number(task.overallBand).toFixed(1) : 'N/A'}
                        </span>
                    </button>
                ))}
            </div>

            {/* Collapsible Score Bar */}
            <div className={`scores-bar-wrapper ${scoresBarCollapsed ? 'collapsed' : ''}`}>
                <button 
                    className="scores-bar-toggle"
                    onClick={() => setScoresBarCollapsed(!scoresBarCollapsed)}
                >
                    <span className="toggle-label">
                        {scoresBarCollapsed ? <><FiChevronRight size={14} /> Hiển thị điểm thành phần</> : <><FiChevronDown size={14} /> Ẩn điểm thành phần</>}
                    </span>
                    <div className="task-band-mini">
                        <span className="label">Task {activeTask}</span>
                        <span className={`value band-${Math.floor(currentTaskReview?.overallBand || 0)}`}>
                            {currentTaskReview?.overallBand ? Number(currentTaskReview.overallBand).toFixed(1) : 'N/A'}
                        </span>
                    </div>
                </button>
                {!scoresBarCollapsed && (
                    <div className="scores-bar">
                        <div className="scores-grid">
                            {renderScoreBar(
                                activeTask === 1 ? 'Task Achievement' : 'Task Response',
                                'taskAchievement',
                                bandScores.taskAchievement || 0,
                                aiFeedback.criteriaComments?.task_achievement
                            )}
                            {renderScoreBar(
                                'Coherence & Cohesion',
                                'coherenceCohesion', 
                                bandScores.coherenceCohesion || 0,
                                aiFeedback.criteriaComments?.coherence_cohesion
                            )}
                            {renderScoreBar(
                                'Lexical Resource',
                                'lexicalResource',
                                bandScores.lexicalResource || 0,
                                aiFeedback.criteriaComments?.lexical_resource
                            )}
                            {renderScoreBar(
                                'Grammar Range & Accuracy',
                                'grammaticalRange',
                                bandScores.grammaticalRange || 0,
                                aiFeedback.criteriaComments?.grammatical_range
                            )}
                        </div>
                        <div className="task-band-summary">
                            <span className="label">Task {activeTask}</span>
                            <span className={`value band-${Math.floor(currentTaskReview?.overallBand || 0)}`}>
                                {currentTaskReview?.overallBand ? Number(currentTaskReview.overallBand).toFixed(1) : 'N/A'}
                            </span>
                        </div>
                    </div>
                )}
            </div>

            {/* Main Content - Three Column Resizable Layout */}
            <div className="result-main-content">
                <PanelGroup direction="horizontal" className="result-panel-group">
                    {/* Left Column - Task Prompt */}
                    <Panel defaultSize={25} minSize={15} maxSize={40}>
                        <div className="result-column prompt-column">
                            <div className="column-header">
                                <h3><FiFileText size={16} /> Đề bài</h3>
                            </div>
                            <div className="column-content">
                                {currentTaskPrompt?.promptText && (
                                    <div 
                                        className="task-prompt-text"
                                        dangerouslySetInnerHTML={{ __html: currentTaskPrompt.promptText }}
                                    />
                                )}
                                {currentTaskPrompt?.imageUrl && (
                                    <div className="task-prompt-image">
                                        <img src={currentTaskPrompt.imageUrl} alt="Task Figure" />
                                    </div>
                                )}
                                {/* <div className="word-requirement">
                                    <span>Yêu cầu tối thiểu:</span>
                                    <strong>{activeTask === 1 ? '150' : '250'} từ</strong>
                                </div> */}
                            </div>
                        </div>
                    </Panel>

                    <PanelResizeHandle className="resize-handle">
                        <div className="resize-handle-icon-container">
                            <span className="resize-handle-icon">↔</span>
                        </div>
                    </PanelResizeHandle>

                    {/* Middle Column - Essay */}
                    <Panel defaultSize={40} minSize={25}>
                        <div className="result-column essay-column">
                            <div className="column-header">
                                <h3><FiEdit3 size={16} /> Bài viết của bạn</h3>
                                <div className="word-count">{currentTaskReview?.wordCount || 0} từ</div>
                            </div>
                            <div className="column-content">
                                {/* Legend */}
                                <div className="highlight-legend">
                                    <span className="legend-title">Click vào text được highlight để xem chi tiết:</span>
                                    <div className="legend-items">
                                        {Object.entries(ERROR_TYPE_COLORS).map(([type, colors]) => (
                                            <span key={type} className="legend-item">
                                                <span className="dot" style={{ backgroundColor: colors.border }} />
                                                {colors.label}
                                            </span>
                                        ))}
                                    </div>
                                </div>

                                {/* Essay with highlights */}
                                <div className="essay-text-container">
                                    {renderHighlightedEssay()}
                                </div>

                                {/* Error notice */}
                                {aiFeedback.error && (
                                    <div className="grading-error-notice">
                                        <FiAlertTriangle size={18} className="error-icon" />
                                        <p>{aiFeedback.error}</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    </Panel>

                    <PanelResizeHandle className="resize-handle">
                        <div className="resize-handle-icon-container">
                            <span className="resize-handle-icon">↔</span>
                        </div>
                    </PanelResizeHandle>

                    {/* Right Column - Analysis & Feedback */}
                    <Panel defaultSize={35} minSize={20}>
                        <div className="result-column analysis-column" ref={analysisColumnRef}>
                            <div className="column-header">
                                <h3><FiBarChart2 size={16} /> Phân tích chi tiết</h3>
                            </div>
                            <div className="column-content">
                                {/* Feedback Summary */}
                                {aiFeedback.feedbackSummary && Object.keys(aiFeedback.feedbackSummary).length > 0 && (
                                    <div className="feedback-summary-cards">
                                        {aiFeedback.feedbackSummary.strengths?.length > 0 && (
                                            <div className="feedback-card strengths">
                                                <h4><FiThumbsUp size={14} /> Điểm mạnh</h4>
                                                <ul>
                                                    {aiFeedback.feedbackSummary.strengths.map((s, i) => (
                                                        <li key={i}>{s}</li>
                                                    ))}
                                                </ul>
                                            </div>
                                        )}
                                        {aiFeedback.feedbackSummary.weaknesses?.length > 0 && (
                                            <div className="feedback-card weaknesses">
                                                <h4><FiAlertTriangle size={14} /> Điểm yếu</h4>
                                                <ul>
                                                    {aiFeedback.feedbackSummary.weaknesses.map((w, i) => (
                                                        <li key={i}>{w}</li>
                                                    ))}
                                                </ul>
                                            </div>
                                        )}
                                        {aiFeedback.feedbackSummary.writing_approach && (
                                            <div className="feedback-card approach">
                                                <h4><FiEdit size={14} /> Hướng viết đề xuất</h4>
                                                <p>{aiFeedback.feedbackSummary.writing_approach}</p>
                                            </div>
                                        )}
                                        {aiFeedback.feedbackSummary.improvement_tips && (
                                            <div className="feedback-card tips">
                                                <h4><FiZap size={14} /> Hướng dẫn cải thiện</h4>
                                                <p>{aiFeedback.feedbackSummary.improvement_tips}</p>
                                            </div>
                                        )}
                                    </div>
                                )}

                                {/* Sentence Corrections */}
                                {aiFeedback.sentenceCorrections?.length > 0 && (
                                    <div className={`expandable-section ${expandedSections.corrections ? 'open' : ''}`}>
                                        <button 
                                            className="section-toggle"
                                            onClick={() => toggleSection('corrections')}
                                        >
                                            <FiEdit3 size={16} className="section-icon" />
                                            <span className="dropdown-title">Sửa lỗi câu ({aiFeedback.sentenceCorrections.length})</span>
                                            <span className="toggle-arrow">{expandedSections.corrections ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                                        </button>
                                        {expandedSections.corrections && (
                                            <div className="section-content">
                                                {aiFeedback.sentenceCorrections.map((corr, idx) => {
                                                    const itemId = `correction-${idx}`;
                                                    const style = getErrorStyle(corr.error_type);
                                                    return (
                                                        <div 
                                                            key={idx}
                                                            ref={el => itemRefs.current[itemId] = el}
                                                            className={`correction-item ${selectedItemId === itemId ? 'selected' : ''}`}
                                                            style={{ borderLeftColor: style.border }}
                                                        >
                                                            <div className="correction-header">
                                                                <span 
                                                                    className="error-type-badge"
                                                                    style={{ backgroundColor: style.border }}
                                                                >
                                                                    {corr.error_type || 'error'}
                                                                </span>
                                                            </div>
                                                            <div className="correction-original">
                                                                <span className="label"><FiXCircle size={14} /></span>
                                                                <span className="text">{corr.original}</span>
                                                            </div>
                                                            <div className="correction-fixed">
                                                                <span className="label"><FiCheckCircle size={14} /></span>
                                                                <span className="text">{corr.corrected}</span>
                                                            </div>
                                                            {corr.explanation && (
                                                                <div className="correction-explanation">
                                                                    <FiInfo size={14} /> {corr.explanation}
                                                                </div>
                                                            )}
                                                        </div>
                                                    );
                                                })}
                                            </div>
                                        )}
                                    </div>
                                )}

                                {/* Paragraph Rewrites */}
                                {aiFeedback.paragraphRewrites?.length > 0 && (
                                    <div className={`expandable-section ${expandedSections.paragraphs ? 'open' : ''}`}>
                                        <button 
                                            className="section-toggle"
                                            onClick={() => toggleSection('paragraphs')}
                                        >
                                            <FiBook size={16} className="section-icon" />
                                            <span className="dropdown-title">Viết lại đoạn ({aiFeedback.paragraphRewrites.length})</span>
                                            <span className="toggle-arrow">{expandedSections.paragraphs ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                                        </button>
                                        {expandedSections.paragraphs && (
                                            <div className="section-content">
                                                {aiFeedback.paragraphRewrites.map((para, idx) => {
                                                    const itemId = `paragraph-${idx}`;
                                                    return (
                                                        <div 
                                                            key={idx}
                                                            ref={el => itemRefs.current[itemId] = el}
                                                            className={`paragraph-item ${selectedItemId === itemId ? 'selected' : ''}`}
                                                        >
                                                            <div className="para-header">Đoạn {(para.paragraph_index || idx) + 1}</div>
                                                            <div className="original-para">
                                                                <span className="para-label"><FiFileText size={14} /> Bản gốc:</span>
                                                                <p>{para.original}</p>
                                                            </div>
                                                            <div className="improved-para">
                                                                <span className="para-label"><FiTrendingUp size={14} /> Bản cải thiện:</span>
                                                                <p>{para.improved}</p>
                                                            </div>
                                                            {para.improvements_made?.length > 0 && (
                                                                <div className="improvements-made">
                                                                    <span className="para-label"><FiZap size={14} /> Các cải thiện:</span>
                                                                    <ul>
                                                                        {para.improvements_made.map((imp, i) => (
                                                                            <li key={i}>{imp}</li>
                                                                        ))}
                                                                    </ul>
                                                                </div>
                                                            )}
                                                        </div>
                                                    );
                                                })}
                                            </div>
                                        )}
                                    </div>
                                )}

                                {/* Sample Essays */}
                                {aiFeedback.sampleEssayBandPlus && (
                                    <div className={`expandable-section ${expandedSections.sampleBandPlus ? 'open' : ''}`}>
                                        <button 
                                            className="section-toggle sample-toggle"
                                            onClick={() => toggleSection('sampleBandPlus')}
                                        >
                                            <FiTrendingUp size={16} className="section-icon" />
                                            <span className="dropdown-title">
                                                Phiên bản cải tiến Band {Math.min(9, Math.floor((currentTaskReview?.overallBand || 6)) + 1)}
                                            </span>
                                            <span className="toggle-arrow">{expandedSections.sampleBandPlus ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                                        </button>
                                        {expandedSections.sampleBandPlus && (
                                            <div className="section-content sample-content">
                                                <p className="sample-description">
                                                    Phiên bản cải thiện, đạt band cao hơn 1 điểm.
                                                </p>
                                                <div className="sample-essay-text">
                                                    {aiFeedback.sampleEssayBandPlus.split('\n').map((para, idx) => (
                                                        <p key={idx}>{para || '\u00A0'}</p>
                                                    ))}
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                )}

                                {aiFeedback.sampleEssayBand9 && (
                                    <div className={`expandable-section ${expandedSections.sampleBand9 ? 'open' : ''}`}>
                                        <button 
                                            className="section-toggle sample-toggle band-9"
                                            onClick={() => toggleSection('sampleBand9')}
                                        >
                                            <FiAward size={16} className="section-icon" />
                                            <span className="dropdown-title">Bài mẫu Band 9.0</span>
                                            <span className="toggle-arrow">{expandedSections.sampleBand9 ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                                        </button>
                                        {expandedSections.sampleBand9 && (
                                            <div className="section-content sample-content band-9">
                                                <p className="sample-description">
                                                    Bài mẫu đạt band 9.0 cho đề bài này.
                                                </p>
                                                <div className="sample-essay-text">
                                                    {aiFeedback.sampleEssayBand9.split('\n').map((para, idx) => (
                                                        <p key={idx}>{para || '\u00A0'}</p>
                                                    ))}
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                )}

                                {/* Word Analysis */}
                                {aiFeedback.wordAnalysis?.length > 0 && (
                                    <div className={`expandable-section ${expandedSections.wordAnalysis ? 'open' : ''}`}>
                                        <button 
                                            className="section-toggle"
                                            onClick={() => toggleSection('wordAnalysis')}
                                        >
                                            <FiBook size={16} className="section-icon" />
                                            <span className="dropdown-title">Phân tích từ vựng ({aiFeedback.wordAnalysis.length})</span>
                                            <span className="toggle-arrow">{expandedSections.wordAnalysis ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                                        </button>
                                        {expandedSections.wordAnalysis && (
                                            <div className="section-content">
                                                <div className="word-analysis-list">
                                                    {aiFeedback.wordAnalysis.map((word, idx) => (
                                                        <div key={idx} className={`word-item usage-${word.usage_quality || 'acceptable'}`}>
                                                            <div className="word-header">
                                                                <span className="word-text">{word.word}</span>
                                                                <span className={`usage-badge ${word.usage_quality || 'acceptable'}`}>
                                                                    {word.usage_quality === 'good' ? '✓ Tốt' : 
                                                                     word.usage_quality === 'incorrect' ? '✗ Sai' : '○ Được'}
                                                                </span>
                                                            </div>
                                                            <div className="word-definition">{word.definition}</div>
                                                            <div className="word-context">
                                                                <span className="context-label">Ngữ cảnh:</span> {word.context}
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                        </div>
                    </Panel>
                </PanelGroup>
            </div>
        </div>
    );
};

export default WritingResultPage;
