import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FiX, FiUser, FiImage, FiBox } from 'react-icons/fi';
import 'react-image-crop/dist/ReactCrop.css';
import CroppableImageTab from './CroppableImageTab';
import '../css/UploadImageModal.css';

const modalVariants = {
    hidden: { opacity: 0, scale: 0.95 },
    visible: { opacity: 1, scale: 1 },
    exit: { opacity: 0, scale: 0.95 }
};

const UploadImageModal = ({ isOpen, onClose, onConfirm }) => {
    const [activeTab, setActiveTab] = useState('avatar');
    const [avatarFile, setAvatarFile] = useState(null);
    const [heroFile, setHeroFile] = useState(null);
    const [pageBgFile, setPageBgFile] = useState(null);

    const handleConfirm = () => {
        let fileToUpload;
        switch (activeTab) {
            case 'avatar':
                fileToUpload = avatarFile;
                break;
            case 'hero':
                fileToUpload = heroFile;
                break;
            case 'page':
                fileToUpload = pageBgFile;
                break;
            default:
                break;
        }

        if (fileToUpload) {
            onConfirm(activeTab, fileToUpload);
        }
        onClose();
    };

    const renderContent = () => {
        switch (activeTab) {
            case 'avatar':
                return (
                    <CroppableImageTab
                        onFileCropped={setAvatarFile}
                        aspectRatio={1}
                        minWidth={100}
                        maxSizeMB={5}
                    />
                );
            case 'hero':
                return (
                    <CroppableImageTab
                        onFileCropped={setHeroFile}
                        aspectRatio={16 / 5}
                        minWidth={300}
                        maxSizeMB={5}
                    />
                );
            case 'page':
                return (
                    <CroppableImageTab
                        onFileCropped={setPageBgFile}
                        aspectRatio={16 / 9}
                        minWidth={300}
                        maxSizeMB={5}
                    />
                );
            default:
                return null;
        }
    };

    const handleClose = () => {
        setAvatarFile(null);
        setHeroFile(null);
        setPageBgFile(null);
        onClose();
    };

    const isConfirmDisabled = () => {
        switch (activeTab) {
            case 'avatar': return !avatarFile;
            case 'hero': return !heroFile;
            case 'page': return !pageBgFile;
            default: return true;
        }
    };

    return (
        <AnimatePresence>
            {isOpen && (
                <motion.div
                    className="upload-modal-backdrop"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    onClick={handleClose}
                >
                    <motion.div
                        className="upload-modal-content"
                        variants={modalVariants}
                        initial="hidden"
                        animate="visible"
                        exit="exit"
                        transition={{ type: 'spring', stiffness: 300, damping: 25 }}
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="upload-modal-header">
                            <h3>Cập nhật hình ảnh</h3>
                            <button className="upload-modal-close-btn" onClick={handleClose}>
                                <FiX />
                            </button>
                        </div>

                        <div className="upload-modal-navigation">
                            <button
                                className={`nav-btn ${activeTab === 'avatar' ? 'active' : ''}`}
                                onClick={() => setActiveTab('avatar')}>
                                <FiUser />
                                <span>Ảnh đại diện</span>
                            </button>
                            <button
                                className={`nav-btn ${activeTab === 'hero' ? 'active' : ''}`}
                                onClick={() => setActiveTab('hero')}>
                                <FiImage />
                                <span>Ảnh bìa</span>
                            </button>
                            <button
                                className={`nav-btn ${activeTab === 'page' ? 'active' : ''}`}
                                onClick={() => setActiveTab('page')}>
                                <FiBox />
                                <span>Ảnh nền trang</span>
                            </button>
                        </div>

                        <div className="upload-modal-body">
                            {renderContent()}
                        </div>

                        <div className="upload-modal-footer">
                            <button className="btn-action btn-cancel" onClick={handleClose}>Hủy</button>
                            <button className="btn-action btn-save" onClick={handleConfirm} disabled={isConfirmDisabled()}>Xác nhận</button>
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
};

export default UploadImageModal;
