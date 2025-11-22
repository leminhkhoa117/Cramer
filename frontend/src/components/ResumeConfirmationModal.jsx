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

const ResumeConfirmationModal = ({ isOpen, onResume, onStartNew, isStartingNew }) => {
    if (!isOpen) return null;

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
                        <h3>Bài làm đang dang dở</h3>
                    </div>
                    <div className="confirmation-modal-body">
                        <p>Chúng tôi tìm thấy một lần làm bài chưa hoàn thành cho bài test này. Bạn muốn tiếp tục hay bắt đầu một bài mới?</p>
                    </div>
                    <div className="confirmation-modal-footer">
                        <button 
                            className="btn btn-secondary" 
                            onClick={onStartNew}
                            disabled={isStartingNew}
                        >
                            {isStartingNew ? 'Đang tạo...' : 'Bắt đầu bài mới'}
                        </button>
                        <button 
                            className="btn btn-primary-gradient" 
                            onClick={onResume}
                            disabled={isStartingNew}
                        >
                            Tiếp tục làm bài
                        </button>
                    </div>
                </motion.div>
            </motion.div>
        </AnimatePresence>
    );
};

export default ResumeConfirmationModal;
