import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { PanelGroup, Panel, PanelResizeHandle } from 'react-resizable-panels';
import { testAttemptApi } from '../api/backendApi';
import { IeltsScoreConverter } from '../utils/IeltsScoreConverter';
import {
    FiArrowLeft, FiRefreshCw, FiChevronDown,
    FiFileText, FiEdit3, FiCheckCircle, FiXCircle, FiHelpCircle, FiRotateCw,
    FiHeadphones, FiImage
} from 'react-icons/fi';
import { AnimatePresence, motion } from 'framer-motion';
import '../css/TestReviewPage.css';
import FullPageLoader from '../components/FullPageLoader';
import ReviewAnswerColumn from '../components/review/ReviewAnswerColumn';

const TestReviewPage = () => {
    const { attemptId } = useParams();
    const navigate = useNavigate();
    const [reviewData, setReviewData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isSummaryExpanded, setIsSummaryExpanded] = useState(true);
    const [activePartIndex, setActivePartIndex] = useState(0);
    const [selectedQuestionId, setSelectedQuestionId] = useState(null);
    const [isRegrading, setIsRegrading] = useState(false);

    // Refs for scroll sync
    const answersColumnRef = useRef(null);
    const questionRefs = useRef({});

    useEffect(() => {
        const fetchReviewData = async () => {
            try {
                setLoading(true);
                const response = await testAttemptApi.getTestReview(attemptId);
                setReviewData(response.data);
            } catch (err) {
                console.error('Failed to fetch test review data:', err);
                setError('Không thể tải dữ liệu xem lại bài làm. Vui lòng thử lại sau.');
            } finally {
                setLoading(false);
            }
        };

        fetchReviewData();
    }, [attemptId]);

    const handleRetake = () => {
        if (!reviewData) return;
        navigate(`/test/${reviewData.examSource}/${reviewData.testNumber}/${reviewData.skill}`, {
            state: { forceNew: true }
        });
    };

    const handleRegrade = async () => {
        if (isRegrading || !reviewData) return;
        setIsRegrading(true);
        
        try {
            // Call the regrade API to re-score existing answers
            const response = await testAttemptApi.regradeAttempt(attemptId);
            console.log('✅ Regrade completed:', response.data);
            
            // Refresh the review data to show updated scores
            const reviewResponse = await testAttemptApi.getTestReview(attemptId);
            setReviewData(reviewResponse.data);
        } catch (err) {
            console.error('❌ Failed to regrade:', err);
            setError('Không thể chấm lại bài làm. Vui lòng thử lại sau.');
        } finally {
            setIsRegrading(false);
        }
    };

    const { bandScore, duration, completionDate } = useMemo(() => {
        if (!reviewData) {
            return { bandScore: null, duration: 'N/A', completionDate: 'N/A' };
        }

        const score = reviewData.score;
        const band = score != null ? IeltsScoreConverter.convertToBand(score) : null;

        const date = reviewData.completedAt ? new Date(reviewData.completedAt) : new Date();
        const formattedDate = date.toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
        });

        let formattedDuration = 'N/A';
        if (reviewData.duration != null) {
            const totalSeconds = reviewData.duration;
            const hours = Math.floor(totalSeconds / 3600);
            const minutes = Math.floor((totalSeconds % 3600) / 60);
            const seconds = totalSeconds % 60;

            if (hours > 0) {
                formattedDuration = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
            } else {
                formattedDuration = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
            }
        }

        return { bandScore: band, duration: formattedDuration, completionDate: formattedDate };
    }, [reviewData]);

    const testDisplayName = useMemo(() => {
        if (!reviewData) return '';
        const examLabel = reviewData.examSource ? reviewData.examSource.toUpperCase() : '';
        const testLabel = reviewData.testNumber ? `Test ${reviewData.testNumber}` : '';
        const skillLabel = reviewData.skill ? reviewData.skill.charAt(0).toUpperCase() + reviewData.skill.slice(1) : '';
        return [examLabel, testLabel, skillLabel].filter(Boolean).join(' · ');
    }, [reviewData]);

    // Get current section data
    const currentSection = useMemo(() => {
        if (!reviewData?.sections || reviewData.sections.length === 0) return null;
        return reviewData.sections[activePartIndex] || reviewData.sections[0];
    }, [reviewData, activePartIndex]);

    // Determine if this is a listening test
    const isListeningTest = useMemo(() => {
        return reviewData?.skill?.toLowerCase() === 'listening';
    }, [reviewData]);

    // Check if current section has an image to display
    const hasImage = useMemo(() => {
        return !!currentSection?.displayContentUrl;
    }, [currentSection]);

    // Compute stats for current section
    const sectionStats = useMemo(() => {
        if (!currentSection?.questions) return { correct: 0, total: 0 };
        const correct = currentSection.questions.filter(q => q.isCorrect === true).length;
        return { correct, total: currentSection.questions.length };
    }, [currentSection]);

    // Scroll to explanation in the rightmost column
    const scrollToExplanation = (questionId) => {
        setSelectedQuestionId(questionId);
        const element = questionRefs.current[questionId];
        if (element && answersColumnRef.current) {
            element.scrollIntoView({ behavior: 'smooth', block: 'center' });
            element.classList.add('highlight-flash');
            setTimeout(() => element.classList.remove('highlight-flash'), 1500);
        }
    };

    // Get user answer display text
    const getUserAnswerText = (question) => {
        const answer = question.userAnswerContent;
        if (!answer) return 'Không trả lời';
        if (typeof answer === 'string') return answer;
        if (answer.value) return answer.value;
        if (Array.isArray(answer)) return answer.join(', ');
        return JSON.stringify(answer);
    };

    // Get correct answer display text
    const getCorrectAnswerText = (question) => {
        if (!question.correctAnswer) return 'N/A';
        if (Array.isArray(question.correctAnswer)) {
            return question.correctAnswer.join(' / ');
        }
        if (typeof question.correctAnswer === 'string') return question.correctAnswer;
        return JSON.stringify(question.correctAnswer);
    };

    if (error) {
        return <div className="review-error">{error}</div>;
    }

    if (!loading && !reviewData) {
        return <div className="review-error">Không có dữ liệu.</div>;
    }

    const { score, totalQuestions, sections } = reviewData || {};
    const hasSections = sections && sections.length > 0;

    return (
        <>
            <AnimatePresence>
                {loading && (
                    <FullPageLoader
                        key="loader"
                        message="Đang tải kết quả bài làm..."
                        subMessage="Vui lòng chờ trong giây lát, chúng tôi đang tổng hợp chi tiết bài làm của bạn."
                    />
                )}
            </AnimatePresence>

            {reviewData && (
                <div className="review-page">
                    {/* Purple Header - ORIGINAL DESIGN */}
                    <header className="review-header">
                        <div
                            className="review-header-top"
                            onClick={() => setIsSummaryExpanded(!isSummaryExpanded)}
                        >
                            <div className="review-header-left">
                                <button className="back-btn" onClick={(e) => { e.stopPropagation(); navigate('/dashboard'); }}>
                                    <FiArrowLeft size={14} /> Quay lại
                                </button>
                                <h1 className="review-title">{testDisplayName} - Review</h1>
                            </div>
                            <div className="review-header-center">
                                <div className="summary-item">
                                    <span className="summary-label">SỐ CÂU ĐÚNG</span>
                                    <span className="summary-value">{score}/{totalQuestions}</span>
                                </div>
                                <div className="summary-item">
                                    <span className="summary-label">THỜI GIAN LÀM</span>
                                    <span className="summary-value">{duration}</span>
                                </div>
                                <div className="summary-item">
                                    <span className="summary-label">NGÀY LÀM</span>
                                    <span className="summary-value">{completionDate}</span>
                                </div>
                            </div>
                            <div className="review-header-right">
                                <div className="header-actions">
                                    <button 
                                        onClick={(e) => { e.stopPropagation(); handleRegrade(); }} 
                                        className="btn btn-regrade"
                                        disabled={isRegrading}
                                    >
                                        <FiRotateCw size={14} className={isRegrading ? 'spinning' : ''} /> Chấm lại
                                    </button>
                                    <button onClick={(e) => { e.stopPropagation(); handleRetake(); }} className="btn btn-primary">
                                        <FiRefreshCw size={14} /> Làm lại
                                    </button>
                                    <Link to="/dashboard" className="btn btn-secondary" onClick={(e) => e.stopPropagation()}>
                                        Dashboard
                                    </Link>
                                </div>
                                <div className="band-badge">
                                    <span className="label">BAND</span>
                                    <span className={`value band-${Math.floor(bandScore || 0)}`}>
                                        {bandScore ? bandScore.toFixed(1) : 'N/A'}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </header>

                    {/* Part Tabs - ORIGINAL DESIGN */}
                    {hasSections && sections.length > 1 && (
                        <div className="review-part-tabs">
                            {sections.map((section, index) => (
                                <button
                                    key={section.sectionId}
                                    className={`part-tab ${activePartIndex === index ? 'active' : ''}`}
                                    onClick={() => setActivePartIndex(index)}
                                >
                                    Part {section.partNumber}
                                    <span className="part-questions-count">
                                        ({section.questions?.length || 0} câu)
                                    </span>
                                </button>
                            ))}
                        </div>
                    )}

                    {/* Main Content - FLEXIBLE COLUMN LAYOUT */}
                    <main className="review-main-content">
                        <PanelGroup direction="horizontal" className="review-panel-group">
                            {/* LISTENING: Column 1 - Transcript (always shown) */}
                            {/* READING: Column 1 - Passage */}
                            <Panel defaultSize={isListeningTest ? 25 : 30} minSize={15} maxSize={40}>
                                <div className="review-column passage-column">
                                    <div className="column-header">
                                        <h3>
                                            {isListeningTest ? (
                                                <><FiHeadphones size={16} /> Transcript</>
                                            ) : (
                                                <><FiFileText size={16} /> Bài đọc</>
                                            )}
                                        </h3>
                                        <span className="part-badge">Part {currentSection?.partNumber || 1}</span>
                                    </div>
                                    <div className="column-content">
                                        {currentSection?.passageText ? (
                                            <div
                                                className="passage-text"
                                                dangerouslySetInnerHTML={{ __html: currentSection.passageText }}
                                            />
                                        ) : (
                                            <p className="no-content">
                                                {isListeningTest 
                                                    ? 'Không có transcript cho phần này.' 
                                                    : 'Không có nội dung bài đọc cho phần này.'}
                                            </p>
                                        )}
                                    </div>
                                </div>
                            </Panel>

                            <PanelResizeHandle className="resize-handle">
                                <div className="resize-handle-icon-container">
                                    <span className="resize-handle-icon">↔</span>
                                </div>
                            </PanelResizeHandle>

                            {/* LISTENING ONLY: Column 2 - Part Image (conditional) */}
                            {isListeningTest && hasImage && (
                                <>
                                    <Panel defaultSize={20} minSize={15} maxSize={35}>
                                        <div className="review-column image-column">
                                            <div className="column-header">
                                                <h3><FiImage size={16} /> Hình ảnh</h3>
                                                <span className="part-badge">Part {currentSection?.partNumber || 1}</span>
                                            </div>
                                            <div className="column-content">
                                                <div className="passage-image">
                                                    <img src={currentSection.displayContentUrl} alt="Test content" />
                                                </div>
                                            </div>
                                        </div>
                                    </Panel>

                                    <PanelResizeHandle className="resize-handle">
                                        <div className="resize-handle-icon-container">
                                            <span className="resize-handle-icon">↔</span>
                                        </div>
                                    </PanelResizeHandle>
                                </>
                            )}

                            {/* Column: Answer Field (test-taking style with highlights) */}
                            <Panel defaultSize={isListeningTest ? (hasImage ? 25 : 35) : 35} minSize={20}>
                                <div className="review-column answers-column">
                                    <div className="column-header">
                                        <h3><FiEdit3 size={16} /> Bài làm của bạn</h3>
                                        <div className="answers-summary">
                                            <span className="correct-count">{sectionStats.correct} đúng</span>
                                            <span className="separator">/</span>
                                            <span className="total-count">{sectionStats.total} câu</span>
                                        </div>
                                    </div>
                                    <div className="column-content">
                                        <ReviewAnswerColumn
                                            section={currentSection}
                                            onQuestionClick={scrollToExplanation}
                                            selectedQuestionId={selectedQuestionId}
                                            skill={reviewData?.skill}
                                        />
                                    </div>
                                </div>
                            </Panel>

                            <PanelResizeHandle className="resize-handle">
                                <div className="resize-handle-icon-container">
                                    <span className="resize-handle-icon">↔</span>
                                </div>
                            </PanelResizeHandle>

                            {/* Last Column - Đáp án & Giải thích */}
                            <Panel defaultSize={isListeningTest ? (hasImage ? 30 : 40) : 35} minSize={20}>
                                <div className="review-column explanation-column" ref={answersColumnRef}>
                                    <div className="column-header">
                                        <h3><FiCheckCircle size={16} /> Đáp án & Giải thích</h3>
                                    </div>
                                    <div className="column-content">
                                        <div className="explanations-list">
                                            {currentSection?.questions?.map((q, idx) => (
                                                <div
                                                    key={q.questionUid || idx}
                                                    ref={el => questionRefs.current[q.questionUid] = el}
                                                    className={`explanation-card ${q.isCorrect === true ? 'correct' : q.isCorrect === false ? 'incorrect' : 'unanswered'} ${selectedQuestionId === q.questionUid ? 'highlighted' : ''}`}
                                                >
                                                    <div className="explanation-header">
                                                        <span className="question-number">Câu {q.questionNumber}</span>
                                                        <span className={`result-badge ${q.isCorrect === true ? 'correct' : q.isCorrect === false ? 'incorrect' : 'unanswered'}`}>
                                                            {q.isCorrect === true ? 'Đúng' : q.isCorrect === false ? 'Sai' : 'Bỏ qua'}
                                                        </span>
                                                    </div>

                                                    {/* Question Content */}
                                                    {q.questionContent && (
                                                        <div className="question-content-box">
                                                            <div
                                                                className="question-text"
                                                                dangerouslySetInnerHTML={{
                                                                    __html: typeof q.questionContent === 'string'
                                                                        ? q.questionContent
                                                                        : q.questionContent.text || q.questionContent.prompt || ''
                                                                }}
                                                            />
                                                        </div>
                                                    )}

                                                    {/* Answer Comparison */}
                                                    <div className="answer-comparison">
                                                        <div className="comparison-row your-answer">
                                                            <span className="comparison-label">Câu trả lời của bạn:</span>
                                                            <span className={`comparison-value ${q.isCorrect === true ? 'correct' : q.isCorrect === false ? 'incorrect' : 'empty'}`}>
                                                                {getUserAnswerText(q)}
                                                            </span>
                                                        </div>
                                                        <div className="comparison-row correct-answer">
                                                            <span className="comparison-label">Đáp án đúng:</span>
                                                            <span className="comparison-value correct">
                                                                {getCorrectAnswerText(q)}
                                                            </span>
                                                        </div>
                                                    </div>

                                                    {/* Explanation */}
                                                    {q.explanation && (
                                                        <div className="explanation-content">
                                                            <div className="explanation-title">
                                                                <FiHelpCircle size={14} /> Giải thích
                                                            </div>
                                                            <p className="explanation-text">{q.explanation}</p>
                                                        </div>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                </div>
                            </Panel>
                        </PanelGroup>
                    </main>
                </div>
            )}
        </>
    );
};

export default TestReviewPage;
