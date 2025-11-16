import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import '../css/StartTestModal.css';

const backdropVariants = {
    hidden: { opacity: 0 },
    visible: { opacity: 1 },
};

const modalVariants = {
    hidden: { y: "-50px", opacity: 0 },
    visible: { y: "0", opacity: 1, transition: { type: "spring", stiffness: 300, damping: 30 } },
    exit: { y: "50px", opacity: 0 },
};

const StartTestModal = ({ isOpen, onClose, onConfirm, skill }) => {
    const isListening = skill === 'listening';
    const title = `Bắt đầu bài thi ${isListening ? "Listening" : "Reading"}`;
    const description = isListening
        ? "Bài thi sẽ bắt đầu ngay khi bạn nhấn nút 'Bắt đầu'. Audio cho mỗi phần sẽ tự động phát theo trình tự. Bạn sẽ chỉ được nghe một lần duy nhất."
        : "Thời gian làm bài sẽ bắt đầu được tính ngay khi bạn nhấn nút. Hãy chắc chắn rằng bạn đã sẵn sàng.";

    return (
        <AnimatePresence>
            {isOpen && (
                <motion.div
                    className="start-modal-backdrop"
                    variants={backdropVariants}
                    initial="hidden"
                    animate="visible"
                    exit="hidden"
                >
                    <motion.div
                        className="start-modal-content"
                        variants={modalVariants}
                        onClick={(e) => e.stopPropagation()} // Prevent closing on content click
                    >
                        <div className="start-modal-header">
                            <h3>{title}</h3>
                        </div>
                        <div className="start-modal-body">
                            <p>{description}</p>
                        </div>
                        <div className="start-modal-footer">
                            <button className="btn btn-secondary" onClick={onClose}>
                                Để sau
                            </button>
                            <button className="btn btn-primary-gradient" onClick={onConfirm}>
                                Bắt đầu
                            </button>
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
};

export default StartTestModal;
