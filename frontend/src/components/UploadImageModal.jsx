import React, { useState } from 'react';
import { FiUser, FiImage, FiBox } from 'react-icons/fi';
import 'react-image-crop/dist/ReactCrop.css';
import CroppableImageTab from './CroppableImageTab';
import BaseModal from './common/BaseModal';
import '../css/upload-image-modal.css';

/**
 * UploadImageModal - Upload and crop profile images
 * 
 * @param {boolean} isOpen - Modal visibility
 * @param {function} onClose - Close handler
 * @param {function} onConfirm - Confirm upload handler
 */
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
    handleClose();
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
    <BaseModal
      isOpen={isOpen}
      onClose={handleClose}
      title="Cập nhật hình ảnh"
      showCloseButton={true}
      size="lg"
      className="upload-modal"
      footer={
        <>
          <button 
            type="button"
            className="cm-btn cm-btn--secondary" 
            onClick={handleClose}
          >
            Hủy
          </button>
          <button 
            type="button"
            className="cm-btn cm-btn--primary" 
            onClick={handleConfirm} 
            disabled={isConfirmDisabled()}
          >
            Xác nhận
          </button>
        </>
      }
    >
      {/* Tab Navigation */}
      <div className="upload-modal-tabs">
        <button
          type="button"
          className={`upload-modal-tab ${activeTab === 'avatar' ? 'active' : ''}`}
          onClick={() => setActiveTab('avatar')}
        >
          <FiUser />
          <span>Ảnh đại diện</span>
        </button>
        <button
          type="button"
          className={`upload-modal-tab ${activeTab === 'hero' ? 'active' : ''}`}
          onClick={() => setActiveTab('hero')}
        >
          <FiImage />
          <span>Ảnh bìa</span>
        </button>
        <button
          type="button"
          className={`upload-modal-tab ${activeTab === 'page' ? 'active' : ''}`}
          onClick={() => setActiveTab('page')}
        >
          <FiBox />
          <span>Ảnh nền trang</span>
        </button>
      </div>

      {/* Tab Content */}
      <div className="upload-modal-content-area">
        {renderContent()}
      </div>
    </BaseModal>
  );
};

export default UploadImageModal;
