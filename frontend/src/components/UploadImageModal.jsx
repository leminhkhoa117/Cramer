import React, { useState, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FiX, FiUser, FiImage, FiBox, FiUploadCloud } from 'react-icons/fi';
import 'react-image-crop/dist/ReactCrop.css';
import HeroUploadTab from './HeroUploadTab';
import '../css/UploadImageModal.css';

const modalVariants = {
    hidden: { opacity: 0, scale: 0.9 },
    visible: { opacity: 1, scale: 1 },
    exit: { opacity: 0, scale: 0.9 }
};

// Generic file upload tab component
const FileUploadTab = ({ onFileSelect, fileType }) => {
    const [preview, setPreview] = useState(null);
    const fileInputRef = useRef(null);

    const handleFileChange = (event) => {
        const file = event.target.files[0];
        if (file) {
            const previewUrl = URL.createObjectURL(file);
            setPreview(previewUrl);
            onFileSelect(file);
        }
    };

    const handleDrop = useCallback((event) => {
        event.preventDefault();
        event.stopPropagation();
        const file = event.dataTransfer.files[0];
        if (file && file.type.startsWith('image/')) {
            const previewUrl = URL.createObjectURL(file);
            setPreview(previewUrl);
            onFileSelect(file);
        }
    }, [onFileSelect]);

    const handleDragOver = (event) => {
        event.preventDefault();
        event.stopPropagation();
    };

    const handleZoneClick = () => {
        fileInputRef.current.click();
    };

    return (
        <div className="upload-tab-content">
            <input 
                type="file" 
                ref={fileInputRef} 
                onChange={handleFileChange} 
                accept="image/png, image/jpeg"
                hidden 
            />
            <div 
                className="upload-drop-zone" 
                onClick={handleZoneClick}
                onDrop={handleDrop}
                onDragOver={handleDragOver}
            >
                {preview ? (
                    <img src={preview} alt="Xem trước" className={`image-preview ${fileType}-preview`} />
                ) : (
                    <div className="upload-placeholder">
                        <FiUploadCloud size={50} />
                        <p>Kéo và thả ảnh vào đây, hoặc nhấn để chọn ảnh</p>
                        <span>(Tối đa 5MB, PNG, JPG)</span>
                    </div>
                )}
            </div>
        </div>
    );
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
                return <FileUploadTab onFileSelect={setAvatarFile} fileType="avatar" />;
            case 'hero':
                 return <HeroUploadTab onFileCropped={setHeroFile} />;
            case 'page':
                return <FileUploadTab onFileSelect={setPageBgFile} fileType="pageBg" />;
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
                        transition={{ type: 'spring', stiffness: 300, damping: 30 }}
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
