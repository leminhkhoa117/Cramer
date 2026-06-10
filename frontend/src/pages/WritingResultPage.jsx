import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { sanitizeHtml } from '../utils/sanitize';
import { Group, Panel, Separator } from 'react-resizable-panels';
import { writingApi } from '../api/backendApi';
import GradingLoader from '../components/common/GradingLoader';
import EssayComparison from '../components/EssayComparison';
import GradingQuotaInfo from '../components/GradingQuotaInfo';
import QuotaExceededModal from '../components/QuotaExceededModal';
import WritingFeedbackSections from '../components/writing/WritingFeedbackSections';
import useUserStatsStore from '../stores/useUserStatsStore';
import useAuthStore from '../stores/useAuthStore';
import {
    FiArrowLeft, FiRefreshCw, FiChevronDown, FiChevronRight,
    FiFileText, FiEdit3, FiBarChart2, FiCheckCircle, FiXCircle,
    FiAlertCircle, FiZap, FiTrendingUp, FiAward, FiBook,
    FiTarget, FiThumbsUp, FiAlertTriangle, FiEdit, FiInfo, FiRotateCw,
    FiColumns, FiEye, FiCheckSquare
} from 'react-icons/fi';

import '../css/writing-result-page.css';
import '../css/common/modal.css';

// Error type colors with Vietnamese labels - consistent across essay highlights and analysis sections
const ERROR_TYPE_COLORS = {
    grammar: { bg: '#fef2f2', border: '#ef4444', text: '#dc2626', label: 'Ngữ pháp' },
    spelling: { bg: '#fef3c7', border: '#f59e0b', text: '#d97706', label: 'Chính tả' },
    vocabulary: { bg: '#faf5ff', border: '#8b5cf6', text: '#7c3aed', label: 'Từ vựng' },
    punctuation: { bg: '#e0f2fe', border: '#0ea5e9', text: '#0284c7', label: 'Dấu câu' },
    coherence: { bg: '#f0fdf4', border: '#22c55e', text: '#16a34a', label: 'Mạch lạc' },
    style: { bg: '#fefce8', border: '#eab308', text: '#ca8a04', label: 'Văn phong' },
    // Vocabulary highlight types (for good/error vocabulary)
    vocabulary_good: { bg: '#dcfce7', border: '#22c55e', text: '#15803d', label: 'Từ vựng tốt' },
    vocabulary_error: { bg: '#fee2e2', border: '#ef4444', text: '#dc2626', label: 'Từ vựng sai' },
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
    const [taskStatuses, setTaskStatuses] = useState({}); // NEW: Track individual task statuses
    const [error, setError] = useState(null);
    const [activeTask, setActiveTask] = useState(1);
    const [pollKey, setPollKey] = useState(0); // Used to force restart polling

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
    const [showRegradeModal, setShowRegradeModal] = useState(false);
    const [quotaBlock, setQuotaBlock] = useState(null);

    // View mode for essay column: 'original' or 'compare'
    const [essayViewMode, setEssayViewMode] = useState(() => {
        // Restore from localStorage if available
        const saved = localStorage.getItem('writingResult_essayViewMode');
        return saved === 'compare' ? 'compare' : 'original';
    });

    // Save view mode preference to localStorage
    useEffect(() => {
        localStorage.setItem('writingResult_essayViewMode', essayViewMode);
    }, [essayViewMode]);

    // Refs to let the polling effect read latest review/error without re-triggering
    const reviewRef = useRef(review);
    reviewRef.current = review;
    const errorRef = useRef(error);
    errorRef.current = error;

    // Refs for scroll-to functionality
    const analysisColumnRef = useRef(null);
    const itemRefs = useRef({});

    // Poll for grading status with improved reliability
    useEffect(() => {
        // Don't poll if we already have review data or there's an error
        if (reviewRef.current || errorRef.current) return;

        const abortController = new AbortController();
        let pollInterval;
        let isMounted = true;
        let consecutiveErrors = 0;
        let currentPollCount = 0;
        const MAX_CONSECUTIVE_ERRORS = 3;
        const MAX_POLL_COUNT = 120; // Max 6 minutes of polling (120 * 3 seconds)

        // Add initial delay for regrade to let backend update status
        const initialDelay = isRegrading ? 2000 : 0;

        const checkStatus = async () => {
            if (!isMounted || abortController.signal.aborted) return;

            try {
                const statusRes = await writingApi.getGradingStatus(attemptId);
                const status = statusRes.data.status;
                const taskStatusData = statusRes.data.taskStatuses || {}; // NEW: Get task statuses

                if (!isMounted || abortController.signal.aborted) return;

                consecutiveErrors = 0; // Reset error counter on success
                setGradingStatus(status);
                setTaskStatuses(taskStatusData); // NEW: Update task statuses
                currentPollCount++;

                if (status === 'COMPLETED' || status === 'PARTIAL_FAILURE') {
                    clearInterval(pollInterval);
                    // Fetch review data
                    try {
                        const reviewRes = await writingApi.getWritingReview(attemptId);
                        if (isMounted && !abortController.signal.aborted) {
                            setReview(reviewRes.data);
                            setLoading(false);
                            setIsRegrading(false); // Reset regrading state
                        }
                    } catch (reviewErr) {
                        console.error('Error fetching review:', reviewErr);
                        if (isMounted && !abortController.signal.aborted) {
                            setError('Không thể tải kết quả chấm điểm. Vui lòng tải lại trang.');
                            setLoading(false);
                            setIsRegrading(false);
                        }
                    }
                } else if (status === 'FAILED') {
                    clearInterval(pollInterval);
                    // Try to fetch review data even on failure to see if there's a specific error message (e.g., Quota Exceeded)
                    try {
                        const reviewRes = await writingApi.getWritingReview(attemptId);
                        if (isMounted && !abortController.signal.aborted) {
                            setReview(reviewRes.data); // This might contain aiFeedback.error

                            // Check if there is specific aiFeedback error
                            const hasTaskError = reviewRes.data?.tasks?.some(t => t.aiFeedback?.error);
                            if (!hasTaskError) {
                                setError('Chấm điểm thất bại. Vui lòng thử chấm lại hoặc liên hệ hỗ trợ.');
                            }
                            // If hasTaskError is true, the UI will render the error from aiFeedback in the main view

                            setLoading(false);
                            setIsRegrading(false);
                        }
                    } catch (reviewErr) {
                        if (isMounted && !abortController.signal.aborted) {
                            setError('Chấm điểm thất bại. Vui lòng thử chấm lại.');
                            setLoading(false);
                            setIsRegrading(false);
                        }
                    }
                }
            } catch (err) {
                console.error('Error checking grading status:', err);
                consecutiveErrors++;

                if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                    clearInterval(pollInterval);
                    if (isMounted && !abortController.signal.aborted) {
                        setError('Không thể kiểm tra trạng thái chấm điểm. Vui lòng tải lại trang.');
                        setLoading(false);
                        setIsRegrading(false);
                    }
                }
            }
        };

        // Start polling with optional initial delay
        const startPolling = () => {
            // Initial check
            checkStatus();

            // Start polling - every 3 seconds
            pollInterval = setInterval(() => {
                if (currentPollCount < MAX_POLL_COUNT) {
                    checkStatus();
                } else {
                    clearInterval(pollInterval);
                    if (isMounted && !abortController.signal.aborted) {
                        setError('Quá thời gian chờ. Vui lòng tải lại trang để xem kết quả.');
                        setLoading(false);
                        setIsRegrading(false);
                    }
                }
            }, 3000);
        };

        if (initialDelay > 0) {
            const delayTimeout = setTimeout(startPolling, initialDelay);
            return () => {
                isMounted = false;
                abortController.abort();
                clearTimeout(delayTimeout);
                clearInterval(pollInterval);
            };
        } else {
            startPolling();
            return () => {
                isMounted = false;
                abortController.abort();
                clearInterval(pollInterval);
            };
        }
    }, [attemptId, pollKey, isRegrading]); // pollKey and isRegrading force restart when regrading

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
            vocabularyHighlights: feedback.vocabulary_highlights || [],
            errorAnalysis: feedback.error_analysis || {},
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

    // Build highlight map for essay - enhanced with vocabulary and full paragraphs
    const highlightMap = useMemo(() => {
        const map = [];

        // 1. Add sentence corrections (errors to fix)
        aiFeedback.sentenceCorrections?.forEach((corr, idx) => {
            if (corr.original) {
                map.push({
                    id: `correction-${idx}`,
                    text: corr.original,
                    type: corr.error_type || 'grammar',
                    severity: corr.severity || 'minor',
                    category: 'corrections',
                });
            }
        });

        // 2. Add vocabulary highlights (both good and problematic)
        aiFeedback.vocabularyHighlights?.forEach((vocab, idx) => {
            if (vocab.word) {
                const category = vocab.category || 'vocabulary';
                let type = 'vocabulary';
                if (category.includes('good') || category === 'advanced_good' || category === 'collocation_good' || category === 'academic') {
                    type = 'vocabulary_good';
                } else if (category === 'error' || category === 'awkward') {
                    type = 'vocabulary_error';
                }
                map.push({
                    id: `vocab-${idx}`,
                    text: vocab.word,
                    type: type,
                    category: 'wordAnalysis',
                    note: vocab.note,
                });
            }
        });

        // 3. Add word analysis items (from the detailed word analysis section)
        aiFeedback.wordAnalysis?.forEach((word, idx) => {
            if (word.word) {
                let type = 'vocabulary';
                if (word.usage_quality === 'good') {
                    type = 'vocabulary_good';
                } else if (word.usage_quality === 'incorrect') {
                    type = 'vocabulary_error';
                }
                // Only add if not already in vocabularyHighlights
                const alreadyExists = map.some(m => m.text.toLowerCase() === word.word.toLowerCase());
                if (!alreadyExists) {
                    map.push({
                        id: `word-${idx}`,
                        text: word.word,
                        type: type,
                        category: 'wordAnalysis',
                    });
                }
            }
        });

        // Note: Paragraph rewrites are NOT added to highlightMap
        // They use a different visual mechanism (vertical left bar) - see renderHighlightedEssay

        return map;
    }, [aiFeedback]);

    // Build a map of paragraph indices that have rewrites
    const paragraphRewriteMap = useMemo(() => {
        const map = new Map();
        aiFeedback.paragraphRewrites?.forEach((para, idx) => {
            if (para.original) {
                // Try to match by paragraph index if available
                const paragraphIndex = para.paragraph_index ?? idx;
                map.set(paragraphIndex, {
                    id: `paragraph-${idx}`,
                    original: para.original,
                });
            }
        });
        return map;
    }, [aiFeedback]);

    // Scroll to analysis item - improved reliability
    const scrollToItem = useCallback((itemId, category) => {
        // Handle vocab-* IDs by finding matching word in wordAnalysis
        let targetId = itemId;
        if (itemId.startsWith('vocab-')) {
            // Find the word from vocabularyHighlights
            const vocabIdx = parseInt(itemId.replace('vocab-', ''), 10);
            const vocabWord = aiFeedback.vocabularyHighlights?.[vocabIdx]?.word;
            if (vocabWord) {
                // Find matching word in wordAnalysis array
                const wordIdx = aiFeedback.wordAnalysis?.findIndex(
                    w => w.word?.toLowerCase() === vocabWord.toLowerCase()
                );
                if (wordIdx !== -1) {
                    targetId = `word-${wordIdx}`;
                }
            }
        }

        setSelectedItemId(targetId);

        // Expand the relevant section if collapsed
        if (!expandedSections[category]) {
            setExpandedSections(prev => ({ ...prev, [category]: true }));
        }

        // Use requestAnimationFrame + longer timeout for reliable DOM updates after expansion
        requestAnimationFrame(() => {
            setTimeout(() => {
                const element = itemRefs.current[targetId];
                if (element) {
                    element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    element.classList.add('highlight-flash');
                    setTimeout(() => element.classList.remove('highlight-flash'), 1500);
                }
            }, 250);  // Longer delay for section expansion animation
        });
    }, [expandedSections, aiFeedback.vocabularyHighlights, aiFeedback.wordAnalysis]);

    // Handle re-grade request
    const handleRegrade = async () => {
        if (isRegrading) return;

        try {
            setIsRegrading(true);
            setLoading(true);
            setGradingStatus('PENDING');
            setReview(null); // Reset review to trigger polling
            setError(null);  // Reset error to allow polling

            await writingApi.regradeAttempt(attemptId);

            // Increment pollKey to force useEffect to restart with fresh state
            setPollKey(prev => prev + 1);
        } catch (err) {
            console.error('Failed to start re-grading:', err);
            const status = err?.response?.status;
            if (status === 402) {
                setQuotaBlock(err.response.data);
            } else if (status === 429) {
                setError('Bạn đang gửi yêu cầu quá nhanh. Vui lòng thử lại sau vài phút.');
            } else if (status === 403) {
                setError('Bạn không có quyền thực hiện thao tác này.');
            } else {
                setError('Không thể bắt đầu chấm lại. Vui lòng thử lại sau.');
            }
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

    // Render essay with interactive highlights and paragraph indicators
    const renderHighlightedEssay = () => {
        const essayText = currentTaskReview?.essayText || '';
        if (!essayText) return <p className="no-content">Không có nội dung bài viết.</p>;

        let result = essayText;

        // Sort highlights by length (longest first) to avoid nested replacements
        // Only use sentence/word highlights, not paragraphs (those use vertical bar)
        const sortedHighlights = [...highlightMap].sort((a, b) => b.text.length - a.text.length);

        // Create a working copy with markers
        sortedHighlights.forEach((highlight) => {
            const escapedText = highlight.text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const regex = new RegExp(escapedText, 'gi');
            const style = getErrorStyle(highlight.type);
            // Add data-type attribute for CSS styling of vocabulary highlights
            result = result.replace(regex, `<mark class="essay-highlight" data-id="${highlight.id}" data-category="${highlight.category}" data-type="${highlight.type}" style="background-color: ${style.bg}; border-bottom: 2px solid ${style.border}; cursor: pointer;">${highlight.text}</mark>`);
        });

        // Split into lines (each line becomes a paragraph)
        const lines = result.split('\n').filter(p => p.trim());

        // Function to check if a line is part of a paragraph rewrite
        const findMatchingRewrite = (lineText) => {
            const plainText = lineText.replace(/<[^>]*>/g, '').trim().toLowerCase();
            for (const [, rewrite] of paragraphRewriteMap) {
                // Check if this line's text appears in the rewrite's original text
                const originalLower = rewrite.original.toLowerCase();
                // Use first 30 chars of line to check containment
                const lineStart = plainText.substring(0, 30);
                if (lineStart && originalLower.includes(lineStart)) {
                    return rewrite;
                }
            }
            return null;
        };

        // Group consecutive lines that belong to the same rewrite
        const groupedParagraphs = [];
        let currentGroup = null;

        lines.forEach((line, idx) => {
            const rewrite = findMatchingRewrite(line);

            if (rewrite) {
                // This line belongs to a rewrite
                if (currentGroup && currentGroup.rewriteId === rewrite.id) {
                    // Continue existing group
                    currentGroup.lines.push(line);
                } else {
                    // Start new group
                    if (currentGroup) {
                        groupedParagraphs.push(currentGroup);
                    }
                    currentGroup = {
                        type: 'rewrite',
                        rewriteId: rewrite.id,
                        lines: [line],
                    };
                }
            } else {
                // Regular line - not part of any rewrite
                if (currentGroup) {
                    groupedParagraphs.push(currentGroup);
                    currentGroup = null;
                }
                groupedParagraphs.push({
                    type: 'normal',
                    lines: [line],
                });
            }
        });

        // Don't forget the last group
        if (currentGroup) {
            groupedParagraphs.push(currentGroup);
        }

        return (
            <div
                className="essay-content"
                onClick={(e) => {
                    // Handle inline highlight clicks
                    const mark = e.target.closest('mark.essay-highlight');
                    if (mark) {
                        const itemId = mark.dataset.id;
                        const category = mark.dataset.category;
                        scrollToItem(itemId, category);
                        return;
                    }
                    // Handle paragraph indicator clicks
                    const paraIndicator = e.target.closest('.essay-paragraph-rewrite');
                    if (paraIndicator) {
                        const itemId = paraIndicator.dataset.id;
                        scrollToItem(itemId, 'paragraphs');
                    }
                }}
            >
                {groupedParagraphs.map((group, idx) => {
                    if (group.type === 'rewrite') {
                        return (
                            <div
                                key={idx}
                                className="essay-paragraph-rewrite"
                                data-id={group.rewriteId}
                                title="Đoạn này có bản viết lại - click để xem"
                            >
                                <div className="paragraph-rewrite-indicator" />
                                {group.lines.map((line, lineIdx) => (
                                    <p key={lineIdx} dangerouslySetInnerHTML={{ __html: sanitizeHtml(line) }} />
                                ))}
                            </div>
                        );
                    }
                    return group.lines.map((line, lineIdx) => (
                        <p key={`${idx}-${lineIdx}`} dangerouslySetInnerHTML={{ __html: sanitizeHtml(line) }} />
                    ));
                })}
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

    // Loading state - Use reusable GradingLoader component
    if (loading) {
        return (
            <GradingLoader
                status={gradingStatus}
                taskStatuses={taskStatuses}
                onBackClick={() => navigate('/dashboard')}
            />
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
                                onClick={() => setShowRegradeModal(true)}
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
                <Group orientation="horizontal" className="result-panel-group">
                    {/* Left Column - Task Prompt */}
                    <Panel defaultSize="25%" minSize="15%" maxSize="40%">
                        <div className="result-column prompt-column">
                            <div className="column-header">
                                <h3><FiFileText size={16} /> Đề bài</h3>
                            </div>
                            <div className="column-content">
                                {currentTaskPrompt?.promptText && (
                                    <div
                                        className="task-prompt-text"
                                        dangerouslySetInnerHTML={{ __html: sanitizeHtml(currentTaskPrompt.promptText) }}
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

                    <Separator className="resize-handle">
                        <div className="resize-handle-icon-container">
                            <span className="resize-handle-icon">↔</span>
                        </div>
                    </Separator>

                    {/* Middle Column - Essay */}
                    <Panel defaultSize="40%" minSize="25%">
                        <div className="result-column essay-column">
                            <div className="column-header">
                                <h3><FiEdit3 size={16} /> Bài viết của bạn</h3>
                                <div className="column-header-right">
                                    {/* View Mode Toggle - Single button to show/hide comparison */}
                                    <button
                                        className={`comparison-toggle-btn ${essayViewMode === 'compare' ? 'active' : ''}`}
                                        onClick={() => setEssayViewMode(essayViewMode === 'compare' ? 'original' : 'compare')}
                                        title={essayViewMode === 'compare' ? 'Ẩn so sánh' : 'Hiển thị so sánh'}
                                    >
                                        <FiColumns size={14} />
                                        <span>{essayViewMode === 'compare' ? 'Ẩn so sánh' : 'Hiển thị so sánh'}</span>
                                    </button>
                                    <div className="word-count">{currentTaskReview?.wordCount || 0} từ</div>
                                </div>
                            </div>
                            <div className="column-content">
                                {essayViewMode === 'original' ? (
                                    <>
                                        {/* Legend */}
                                        <div className="highlight-legend">
                                            <span className="legend-title">Nhấn vào từ/cụm từ/câu/đoạn được highlight để xem chi tiết:</span>
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
                                            <div className="essay-error-container">
                                                {aiFeedback.error.toLowerCase().includes('lượt') || aiFeedback.error.toLowerCase().includes('quota') ? (
                                                    <div className="quota-error-wrapper">
                                                        <GradingQuotaInfo
                                                            variant="default"
                                                            showLuaOption={true}
                                                            message={aiFeedback.error}
                                                            userStats={useUserStatsStore.getState().stats}
                                                            userProfile={useAuthStore.getState().profile}
                                                            status="blocked"
                                                        />
                                                    </div>
                                                ) : (
                                                    <div className="grading-error-notice">
                                                        <FiAlertTriangle size={18} className="error-icon" />
                                                        <p>{aiFeedback.error}</p>
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                    </>
                                ) : (
                                    /* Comparison View */
                                    <EssayComparison
                                        originalEssay={currentTaskReview?.essayText || ''}
                                        paragraphRewrites={aiFeedback.paragraphRewrites}
                                        onItemClick={scrollToItem}
                                    />
                                )}
                            </div>
                        </div>
                    </Panel>

                    <Separator className="resize-handle">
                        <div className="resize-handle-icon-container">
                            <span className="resize-handle-icon">↔</span>
                        </div>
                    </Separator>

                    {/* Right Column - Analysis & Feedback */}
                    <Panel defaultSize="35%" minSize="20%">
                        <div className="result-column analysis-column" ref={analysisColumnRef}>
                            <div className="column-header">
                                <h3><FiBarChart2 size={16} /> Phân tích chi tiết</h3>
                            </div>
                            <div className="column-content">
                                {/* Error State */}
                                {aiFeedback.error && (
                                    <div className="analysis-error-state" style={{ padding: '1rem' }}>
                                        <div className="quota-error-wrapper">
                                            <GradingQuotaInfo
                                                variant="default"
                                                showLuaOption={true}
                                                message={aiFeedback.error}
                                                userStats={useUserStatsStore.getState().stats}
                                                userProfile={useAuthStore.getState().profile}
                                                status="blocked"
                                            />
                                        </div>
                                    </div>
                                )}

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
                                        {aiFeedback.feedbackSummary.grammar_patterns && (
                                            <div className="feedback-card grammar-patterns">
                                                <h4><FiCheckSquare size={14} /> Mẫu ngữ pháp</h4>
                                                {aiFeedback.feedbackSummary.grammar_patterns.strong_patterns?.length > 0 && (
                                                    <div className="pattern-section strong">
                                                        <strong>Dùng tốt:</strong>
                                                        <ul>
                                                            {aiFeedback.feedbackSummary.grammar_patterns.strong_patterns.map((p, i) => (
                                                                <li key={i}>{p}</li>
                                                            ))}
                                                        </ul>
                                                    </div>
                                                )}
                                                {aiFeedback.feedbackSummary.grammar_patterns.weak_patterns?.length > 0 && (
                                                    <div className="pattern-section weak">
                                                        <strong>Cần cải thiện:</strong>
                                                        <ul>
                                                            {aiFeedback.feedbackSummary.grammar_patterns.weak_patterns.map((p, i) => (
                                                                <li key={i}>{p}</li>
                                                            ))}
                                                        </ul>
                                                    </div>
                                                )}
                                                {aiFeedback.feedbackSummary.grammar_patterns.missing_patterns?.length > 0 && (
                                                    <div className="pattern-section missing">
                                                        <strong>Nên thêm vào:</strong>
                                                        <ul>
                                                            {aiFeedback.feedbackSummary.grammar_patterns.missing_patterns.map((p, i) => (
                                                                <li key={i}>{p}</li>
                                                            ))}
                                                        </ul>
                                                    </div>
                                                )}
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

                                <WritingFeedbackSections
                                    aiFeedback={aiFeedback}
                                    currentTaskReview={currentTaskReview}
                                    expandedSections={expandedSections}
                                    getErrorStyle={getErrorStyle}
                                    itemRefs={itemRefs}
                                    selectedItemId={selectedItemId}
                                    toggleSection={toggleSection}
                                />
                            </div>
                        </div>
                    </Panel>
                </Group >
            </div >

            {/* Regrade Confirmation Modal */}
            {
                showRegradeModal && (
                    <div
                        className={`cm-backdrop ${showRegradeModal === 'closing' ? 'closing' : ''}`}
                        onClick={() => {
                            // Trigger closing animation
                            setShowRegradeModal('closing');
                            setTimeout(() => setShowRegradeModal(false), 400);
                        }}
                    >
                        <div
                            className={`cm-content cm-content--sm ${showRegradeModal === 'closing' ? 'closing' : ''}`}
                            onClick={(e) => e.stopPropagation()}
                        >
                            <div className="cm-header cm-header--no-border">
                                <h3 className="cm-title">Xác nhận chấm lại?</h3>
                                <button
                                    className="cm-close-btn"
                                    onClick={() => {
                                        setShowRegradeModal('closing');
                                        setTimeout(() => setShowRegradeModal(false), 400);
                                    }}
                                >×</button>
                            </div>
                            <div className="cm-body">
                                <p>Bạn có chắc chắn muốn chấm lại bài viết này không?</p>
                                <p>Quá trình chấm điểm sẽ được thực hiện lại từ đầu và có thể mất vài phút.</p>
                            </div>
                            <div className="cm-footer cm-footer--no-border">
                                <button
                                    className="cm-btn cm-btn--secondary"
                                    onClick={() => {
                                        setShowRegradeModal('closing');
                                        setTimeout(() => setShowRegradeModal(false), 400);
                                    }}
                                >
                                    Hủy
                                </button>
                                <button
                                    className="cm-btn cm-btn--primary"
                                    onClick={() => {
                                        setShowRegradeModal('closing');
                                        setTimeout(() => {
                                            setShowRegradeModal(false);
                                            handleRegrade();
                                        }, 400);
                                    }}
                                >
                                    <FiRotateCw size={14} /> Chấm lại
                                </button>
                            </div>
                        </div>
                    </div>
                )
            }

            <QuotaExceededModal
                isOpen={!!quotaBlock}
                billingResult={quotaBlock}
                onClose={() => setQuotaBlock(null)}
                onBuyLua={() => navigate('/subscription?tab=lua')}
                onUpgrade={() => navigate('/subscription')}
            />
        </div >
    );
};

export default WritingResultPage;
