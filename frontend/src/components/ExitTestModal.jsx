import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import '../css/ExitTestModal.css';

const backdropVariants = {
    hidden: { opacity: 0 },
    visible: { opacity: 1 },
};

const modalVariants = {
    hidden: { y: "-50px", opacity: 0 },
    visible: { y: "0", opacity: 1, transition: { type: "spring", stiffness: 300, damping: 30 } },
    exit: { y: "50px", opacity: 0 },
};

const ExitTestModal = ({ isOpen, onClose, onAbort, onSaveAndExit, isSaving }) => {
    return (
        <AnimatePresence>
            {isOpen && (
                <motion.div
                    className="exit-modal-backdrop"
                    variants={backdropVariants}
                    initial="hidden"
                    animate="visible"
                    exit="hidden"
                >
                    <motion.div
                        className="exit-modal-content"
                        variants={modalVariants}
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="exit-modal-header">
                            <h3>Thoát bài thi</h3>
                        </div>
                        <div className="exit-modal-body">
                            <p>Bạn có muốn lưu tiến trình làm bài để tiếp tục sau không?</p>
                            <ul className="exit-modal-options">
                                <li><strong>Lưu & Thoát:</strong> Bạn có thể quay lại và tiếp tục từ nơi đã dừng lại.</li>
                                <li><strong>Huỷ bài:</strong> Lần làm bài này sẽ bị xóa hoàn toàn.</li>
                            </ul>
                        </div>
                        <div className="exit-modal-footer">
                            <button className="btn btn-secondary" onClick={onClose} disabled={isSaving}>
                                Quay lại
                            </button>
                            <button className="btn btn-danger" onClick={onAbort} disabled={isSaving}>
                                Huỷ bài
                            </button>
                            <button className="btn btn-primary-gradient" onClick={onSaveAndExit} disabled={isSaving}>
                                {isSaving ? 'Đang lưu...' : 'Lưu & Thoát'}
                            </button>
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
};

export default ExitTestModal;
