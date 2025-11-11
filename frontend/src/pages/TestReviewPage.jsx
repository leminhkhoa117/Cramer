import React, { useState, useEffect, useMemo } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { testAttemptApi } from '../api/backendApi';
import ReviewedQuestion from '../components/ReviewedQuestion';
import { IeltsScoreConverter } from '../utils/IeltsScoreConverter';
import { FiChevronDown } from 'react-icons/fi';
import { AnimatePresence, motion } from 'framer-motion';
import '../css/TestReviewPage.css';

const TestReviewPage = () => {
    const { attemptId } = useParams();
    const navigate = useNavigate();
    const [reviewData, setReviewData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isSummaryExpanded, setIsSummaryExpanded] = useState(true);

    useEffect(() => {
        const fetchReviewData = async () => {
            try {
                setLoading(true);
                const response = await testAttemptApi.getTestReview(attemptId);
                setReviewData(response.data);
            } catch (err) {
                console.error("Failed to fetch test review data:", err);
                setError("Không thể tải dữ liệu xem lại bài làm. Vui lòng thử lại sau.");
            } finally {
                setLoading(false);
            }
        };

        fetchReviewData();
    }, [attemptId]);

    const handleRetake = () => {
        if (!reviewData) return;
        navigate(`/test/${reviewData.examSource}/${reviewData.testNumber}/${reviewData.skill}`);
    };

    const { bandScore, duration, completionDate } = useMemo(() => {
        if (!reviewData) {
            return { bandScore: null, duration: 'N/A', completionDate: 'N/A' };
        }

        const score = reviewData.score;
        const band = score != null ? IeltsScoreConverter.convertToBand(score) : null;

        const { completedAt, startedAt } = reviewData;
        const date = completedAt ? new Date(completedAt) : new Date();
        const formattedDate = date.toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
        });

        if (!completedAt || !startedAt) {
            return { bandScore: band, duration: 'N/A', completionDate: formattedDate };
        }

        const timeDiffSeconds = Math.round((new Date(completedAt) - new Date(startedAt)) / 1000);
        const actualDuration = Math.min(timeDiffSeconds, 3600);
        
        const minutes = Math.floor(actualDuration / 60);
        const seconds = actualDuration % 60;
        const formattedDuration = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
        
        return { bandScore: band, duration: formattedDuration, completionDate: formattedDate };
    }, [reviewData]);

    if (loading) {
        return <div className="review-loading">Đang tải...</div>;
    }

    if (error) {
        return <div className="review-error">{error}</div>;
    }

    if (!reviewData) {
        return <div className="review-error">Không có dữ liệu.</div>;
    }

    const { score, totalQuestions, questions } = reviewData;

    return (
        <div className="review-page">
            <header className="review-header">
                <div 
                    className="review-header-top"
                    onClick={() => setIsSummaryExpanded(!isSummaryExpanded)}
                >
                    <h1 className="review-title">Kết quả bài làm</h1>
                    <motion.div
                        animate={{ rotate: isSummaryExpanded ? 0 : -180 }}
                        transition={{ duration: 0.3 }}
                    >
                        <FiChevronDown size={20} />
                    </motion.div>
                </div>
                <AnimatePresence>
                {isSummaryExpanded && (
                    <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        style={{ overflow: 'hidden' }}
                        transition={{ duration: 0.3 }}
                    >
                        <div className="review-summary-grid">
                            <div className="summary-item">
                                <span className="summary-label">Số câu đúng</span>
                                <span className="summary-value">{score}/{totalQuestions}</span>
                            </div>
                            <div className="summary-item">
                                <span className="summary-label">Điểm Band</span>
                                <span className="summary-value">{bandScore ? bandScore.toFixed(1) : 'N/A'}</span>
                            </div>
                            <div className="summary-item">
                                <span className="summary-label">Thời gian làm</span>
                                <span className="summary-value">{duration}</span>
                            </div>
                            <div className="summary-item">
                                <span className="summary-label">Ngày làm</span>
                                <span className="summary-value">{completionDate}</span>
                            </div>
                        </div>
                        <div className="review-actions">
                            <button onClick={handleRetake} className="btn btn-primary">Làm bài lại</button>
                            <Link to="/dashboard" className="btn btn-secondary">Về Bảng điều khiển</Link>
                        </div>
                    </motion.div>
                )}
                </AnimatePresence>
            </header>

            <main className="review-main">
                <h2 className="review-section-title">Đáp án & Giải thích</h2>
                <div className="review-questions-list">
                    {questions.map(q => (
                        <ReviewedQuestion key={q.questionUid} questionReview={q} />
                    ))}
                </div>
            </main>
        </div>
    );
};

export default TestReviewPage;
