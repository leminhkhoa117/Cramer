import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { testAttemptApi } from '../api/backendApi';
import ConfirmationModal from './ConfirmationModal'; // Import ConfirmationModal
import { FiTrash2 } from 'react-icons/fi'; // Import trash icon
import '../css/AttemptHistoryDropdown.css';

const formatDate = (dateString) => {
    if (!dateString) return 'Chưa hoàn thành';
    return new Date(dateString).toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
};

const AttemptHistoryDropdown = ({ history, examSource, testNumber, skill, onAttemptDeleted }) => {
    const [isOpen, setIsOpen] = useState(false);
    const [visibleCount, setVisibleCount] = useState(5);
    const [loadingAttemptId, setLoadingAttemptId] = useState(null);
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [attemptToDelete, setAttemptToDelete] = useState(null);
    const navigate = useNavigate();

    if (!history || history.length === 0) return null;

    const handleResume = async (attemptId) => {
        setLoadingAttemptId(attemptId);
        try {
            await testAttemptApi.resumeAttempt(attemptId);
            navigate(`/test/${examSource}/${testNumber}/${skill}`);
        } catch (error) {
            console.error("Failed to resume attempt:", error);
            alert("Không thể tiếp tục lần làm bài này. Vui lòng thử lại.");
        } finally {
            setLoadingAttemptId(null);
        }
    };

    const handleDeleteClick = (attemptId) => {
        setAttemptToDelete(attemptId);
        setIsDeleteModalOpen(true);
    };

    const handleDeleteConfirm = async () => {
        if (!attemptToDelete) return;
        setLoadingAttemptId(attemptToDelete); // Use for delete loading state
        try {
            await testAttemptApi.deleteAttempt(attemptToDelete);
            setIsDeleteModalOpen(false);
            setAttemptToDelete(null);
            if (onAttemptDeleted) {
                onAttemptDeleted(); // Callback to refresh dashboard
            }
        } catch (error) {
            console.error("Failed to delete attempt:", error);
            alert("Không thể xóa lần làm bài này. Vui lòng thử lại.");
        } finally {
            setLoadingAttemptId(null);
        }
    };

    const visibleHistory = history.slice(0, visibleCount);
    const hasMore = history.length > visibleCount;

    return (
        <div className="attempt-history-dropdown">
            <button
                className="attempt-history-dropdown__toggle"
                onClick={() => setIsOpen(!isOpen)}
                aria-expanded={isOpen}
            >
                <span>Lịch sử làm bài ({history.length})</span>
                <svg
                    className={`attempt-history-dropdown__icon ${isOpen ? 'rotated' : ''}`}
                    width="16"
                    height="16"
                    viewBox="0 0 16 16"
                    fill="none"
                >
                    <path d="M4 6L8 10L12 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                </svg>
            </button>

            <AnimatePresence>
                {isOpen && (
                    <motion.div
                        className="attempt-history-dropdown__content"
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.2 }}
                    >
                        <div className="attempt-history-list">
                            {visibleHistory.map((attempt, index) => (
                                <div key={attempt.attemptId} className="attempt-history-item">
                                    <div className="attempt-history-item__header">
                                        <span className="attempt-history-item__index">
                                            #{history.length - index}
                                            {index === 0 && <span className="attempt-history-item__badge">Gần nhất</span>}
                                        </span>
                                        <span className="attempt-history-item__date">{formatDate(attempt.completedAt)}</span>
                                    </div>
                                    <div className="attempt-history-item__stats">
                                        <div className="attempt-history-item__stat">
                                            <span className="label">Trạng thái:</span>
                                            <span className={`value status-${attempt.status?.toLowerCase()}`}>
                                                {attempt.status === 'COMPLETED' ? 'Hoàn thành' : (attempt.status === 'IN_PROGRESS' ? 'Đang làm' : 'Đã hủy')}
                                            </span>
                                        </div>
                                        {attempt.bandScore != null && (
                                            <div className="attempt-history-item__stat">
                                                <span className="label">Kết quả:</span>
                                                <span className="value score">Band {attempt.bandScore.toFixed(1)}</span>
                                            </div>
                                        )}
                                    </div>
                                    <div className="attempt-history-item__actions">
                                        {attempt.status === 'COMPLETED' && attempt.attemptId && (
                                            <Link
                                                to={`/test/review/${attempt.attemptId}`}
                                                className="attempt-history-item__action"
                                            >
                                                Xem chi tiết →
                                            </Link>
                                        )}
                                        {attempt.status === 'IN_PROGRESS' && (
                                            <button
                                                onClick={() => handleResume(attempt.attemptId)}
                                                className="attempt-history-item__action"
                                                disabled={loadingAttemptId === attempt.attemptId}
                                            >
                                                {loadingAttemptId === attempt.attemptId ? 'Đang tải...' : 'Tiếp tục →'}
                                            </button>
                                        )}
                                        <button
                                            onClick={() => handleDeleteClick(attempt.attemptId)}
                                            className="attempt-history-item__delete-btn"
                                            title="Xóa lần làm bài này"
                                            disabled={loadingAttemptId === attempt.attemptId}
                                        >
                                            <FiTrash2 />
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>

                        {hasMore && (
                            <button
                                className="attempt-history-dropdown__load-more"
                                onClick={() => setVisibleCount(prev => prev + 5)}
                            >
                                Xem thêm {Math.min(5, history.length - visibleCount)} lần làm bài
                            </button>
                        )}
                    </motion.div>
                )}
            </AnimatePresence>

            <ConfirmationModal
                isOpen={isDeleteModalOpen}
                onClose={() => setIsDeleteModalOpen(false)}
                onConfirm={handleDeleteConfirm}
                title="Xác nhận xóa"
                confirmText={loadingAttemptId === attemptToDelete ? "Đang xóa..." : "Xóa"}
                isConfirming={loadingAttemptId === attemptToDelete}
            >
                <p>Bạn có chắc chắn muốn xóa lần làm bài này không? Hành động này không thể hoàn tác.</p>
            </ConfirmationModal>
        </div>
    );
};

export default AttemptHistoryDropdown;
