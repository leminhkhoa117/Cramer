import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import '../css/ConfirmationModal.css'; // Reuse the same style for consistency

const backdropVariants = {
    hidden: { opacity: 0 },
    visible: { opacity: 1 },
};

const modalVariants = {
    hidden: { y: "-50px", opacity: 0 },
    visible: { y: "0", opacity: 1, transition: { type: "spring", stiffness: 300, damping: 30 } },
    exit: { y: "50px", opacity: 0 },
};

const ResumeConfirmationModal = ({ isOpen, onResume, onStartNew, isStartingNew, attemptStatus = 'IN_PROGRESS' }) => {
    if (!isOpen) return null;

    const isCompleted = attemptStatus === 'COMPLETED';
    const title = isCompleted ? 'Đã có bài làm trước đó' : 'Bài làm đang dang dở';
    const message = isCompleted 
        ? 'Bạn đã hoàn thành bài test này trước đó. Bạn muốn xem kết quả hay làm bài mới?'
        : 'Chúng tôi tìm thấy một lần làm bài chưa hoàn thành cho bài test này. Bạn muốn tiếp tục hay bắt đầu một bài mới?';
    const resumeButtonText = isCompleted ? 'Xem kết quả' : 'Tiếp tục làm bài';

    return (
        <AnimatePresence>
            <motion.div
                className="confirmation-modal-backdrop"
                variants={backdropVariants}
                initial="hidden"
                animate="visible"
                exit="hidden"
            >
                <motion.div
                    className="confirmation-modal-content"
                    variants={modalVariants}
                    onClick={(e) => e.stopPropagation()}
                >
                    <div className="confirmation-modal-header">
                        <h3>{title}</h3>
                    </div>
                    <div className="confirmation-modal-body">
                        <p>{message}</p>
                    </div>
                    <div className="confirmation-modal-footer">
                        <button 
                            className="btn btn-secondary" 
                            onClick={onStartNew}
                            disabled={isStartingNew}
                        >
                            {isStartingNew ? 'Đang tạo...' : 'Làm bài mới'}
                        </button>
                        <button 
                            className="btn btn-primary-gradient" 
                            onClick={onResume}
                            disabled={isStartingNew}
                        >
                            {resumeButtonText}
                        </button>
                    </div>
                </motion.div>
            </motion.div>
        </AnimatePresence>
    );
};

export default ResumeConfirmationModal;
