import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { profileApi } from '../api/backendApi';
import { writingApi } from '../api/backendApi';
import { showErrorToast, showSuccessToast } from '../utils/toast.js';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  FiUser, 
  FiEdit3, 
  FiCamera, 
  FiKey, 
  FiSave, 
  FiX,
  FiShield,
  FiSliders,
  FiSmartphone,
  FiMonitor,
  FiLock,
  FiUnlock,
  FiAlertTriangle,
  FiTrash2,
  FiCheck,
  FiChevronRight,
  FiLogOut,
  FiGlobe,
  FiClock,
  FiLink,
  FiCpu,
  FiEye,
  FiEyeOff
} from 'react-icons/fi';
import { FaGoogle, FaFacebook } from 'react-icons/fa';
import '../css/ProfilePage.css';
import FullPageLoader from '../components/FullPageLoader';
import { supabase } from '../api/supabaseClient';
import { v4 as uuidv4 } from 'uuid';
import UploadImageModal from '../components/UploadImageModal';
import ChangePasswordModal from '../components/ChangePasswordModal';
import ConfirmationModal from '../components/ConfirmationModal';

// Animation variants
const tabContentVariants = {
  hidden: { opacity: 0, y: 10 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.3 } },
  exit: { opacity: 0, y: -10, transition: { duration: 0.2 } }
};

const ProfilePage = () => {
  const { user, profileLoading, updateProfileContext } = useAuth();
  
  // State
  const [activeTab, setActiveTab] = useState('personal');
  const [isEditing, setIsEditing] = useState(false);
  const [profileData, setProfileData] = useState(null);
  const [editedProfile, setEditedProfile] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isChangePasswordModalOpen, setIsChangePasswordModalOpen] = useState(false);
  const [deleteImageModal, setDeleteImageModal] = useState({ isOpen: false, type: null });
  const [isDeleting, setIsDeleting] = useState(false);
  
  // AI Settings states
  const [geminiApiKey, setGeminiApiKey] = useState('');
  const [showApiKey, setShowApiKey] = useState(false);
  const [isValidatingApiKey, setIsValidatingApiKey] = useState(false);
  const [apiKeyStatus, setApiKeyStatus] = useState(null); // null, 'valid', 'invalid'
  const [isSavingApiKey, setIsSavingApiKey] = useState(false);
  const [isApiKeyModified, setIsApiKeyModified] = useState(false); // Track if user has modified the key
  
  // Security states (mock data for now - will be replaced with API calls)
  const [twoFactorEnabled, setTwoFactorEnabled] = useState(false);
  const [sessions, setSessions] = useState([
    { 
      id: '1', 
      device: 'Chrome trên Windows', 
      location: 'Hồ Chí Minh, Việt Nam',
      lastActive: 'Đang hoạt động',
      isCurrent: true,
      icon: FiMonitor
    },
    { 
      id: '2', 
      device: 'Safari trên iPhone', 
      location: 'Hà Nội, Việt Nam',
      lastActive: '2 giờ trước',
      isCurrent: false,
      icon: FiSmartphone
    }
  ]);
  const [loginHistory, setLoginHistory] = useState([
    { id: '1', device: 'Chrome / Windows', ip: '192.168.1.1', time: 'Hôm nay, 10:30', success: true },
    { id: '2', device: 'Safari / iPhone', ip: '192.168.1.2', time: 'Hôm qua, 18:45', success: true },
    { id: '3', device: 'Firefox / MacOS', ip: '103.21.45.67', time: '2 ngày trước', success: false }
  ]);

  // Refs
  const avatarFileRef = useRef(null);

  // Fetch profile on mount
  useEffect(() => {
    const fetchProfile = async () => {
      if (!user?.id) return;
      try {
        setLoading(true);
        const response = await profileApi.getById(user.id);
        const fullProfile = { ...response.data, email: user.email };
        setProfileData(fullProfile);
        setEditedProfile(fullProfile);
        // Check if API key exists (backend only returns hasGeminiApiKey, not the actual key for security)
        if (response.data.hasGeminiApiKey) {
          setGeminiApiKey('••••••••••••••••••••••••••••••••••••••••'); // Masked placeholder
          setApiKeyStatus('valid'); // Assume valid if saved
        }
        setError(null);
      } catch (err) {
        setError('Không thể tải thông tin cá nhân.');
        showErrorToast('Lỗi khi tải thông tin cá nhân.');
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, [user?.id]);

  // Handlers - memoized for performance
  const handleEditToggle = useCallback(() => {
    if (!isEditing) {
      setEditedProfile({ ...profileData });
    }
    setIsEditing(!isEditing);
  }, [isEditing, profileData]);

  const handleInputChange = useCallback((e) => {
    const { name, value } = e.target;
    setEditedProfile((prev) => ({ ...prev, [name]: value }));
  }, []);

  const handleSave = useCallback(async (extraData = {}) => {
    const profileToSave = { ...editedProfile, ...extraData };
    try {
      const response = await profileApi.update(user.id, profileToSave);
      const updatedProfile = { ...response.data, email: user.email };
      setProfileData(updatedProfile);
      updateProfileContext(response.data);
      showSuccessToast('Cập nhật thông tin thành công!');
      setIsEditing(false);
    } catch (err) {
      console.error('Error saving profile:', err);
      showErrorToast('Lỗi khi cập nhật thông tin.');
    } finally {
      setIsUploading(false);
    }
  }, [editedProfile, user?.id, user?.email, updateProfileContext]);

  const parseSupabaseUrl = (url) => {
    try {
      const urlPath = new URL(url).pathname;
      const publicIndex = urlPath.indexOf('/public/');
      if (publicIndex === -1) return null;
      const pathAfterPublic = urlPath.substring(publicIndex + '/public/'.length);
      const bucketName = 'userImages';
      const filePath = pathAfterPublic.substring(bucketName.length + 1);
      if (!filePath) return null;
      return { bucket: bucketName, path: filePath };
    } catch (error) {
      console.error('Could not parse Supabase URL:', error);
      return null;
    }
  };

  const uploadAvatar = async (file) => {
    if (!file) return;
    setIsUploading(true);

    if (profileData?.avatarUrl) {
      const oldImage = parseSupabaseUrl(profileData.avatarUrl);
      if (oldImage) {
        try {
          await supabase.storage.from('userImages').remove([oldImage.path]);
        } catch (error) {
          console.error('Failed to delete old avatar:', error);
        }
      }
    }

    const resizeImage = (file, maxWidth, maxHeight) => new Promise((resolve, reject) => {
      const img = new Image();
      img.src = URL.createObjectURL(file);
      img.onload = () => {
        const canvas = document.createElement('canvas');
        let { width, height } = img;
        if (width > height) {
          if (width > maxWidth) {
            height = Math.round((height * maxWidth) / width);
            width = maxWidth;
          }
        } else {
          if (height > maxHeight) {
            width = Math.round((width * maxHeight) / height);
            height = maxHeight;
          }
        }
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);
        canvas.toBlob((blob) => {
          if (blob) {
            const newFile = new File([blob], file.name.replace(/\.[^/.]+$/, ".jpg"), {
              type: 'image/jpeg',
              lastModified: Date.now()
            });
            resolve(newFile);
          } else {
            reject(new Error('Canvas to Blob conversion failed'));
          }
        }, 'image/jpeg', 0.9);
      };
      img.onerror = reject;
    });

    try {
      const resizedFile = await resizeImage(file, 200, 200);
      const fileExt = resizedFile.name.split('.').pop();
      const fileName = `${user.id}-${uuidv4()}.${fileExt}`;
      const filePath = `avatars/${fileName}`;

      const { error: uploadError } = await supabase.storage.from('userImages').upload(filePath, resizedFile, {
        cacheControl: '3600',
        upsert: false
      });

      if (uploadError) throw uploadError;

      const { data } = supabase.storage.from('userImages').getPublicUrl(filePath);
      if (!data.publicUrl) throw new Error('Could not get public URL for avatar.');

      await handleSave({ avatarUrl: data.publicUrl });
    } catch (error) {
      console.error('Avatar upload error:', error);
      showErrorToast(error.message || 'Không thể tải lên ảnh đại diện.');
      setIsUploading(false);
    }
  };

  const uploadHeroBackground = async (file) => {
    if (!file) return;
    setIsUploading(true);

    if (profileData?.heroBackgroundUrl) {
      const oldImage = parseSupabaseUrl(profileData.heroBackgroundUrl);
      if (oldImage) {
        try {
          await supabase.storage.from('userImages').remove([oldImage.path]);
        } catch (error) {
          console.error('Failed to delete old hero background:', error);
        }
      }
    }

    try {
      const fileExt = file.name.split('.').pop();
      const fileName = `${user.id}-${uuidv4()}.${fileExt}`;
      const filePath = `hero-backgrounds/${fileName}`;

      const { error: uploadError } = await supabase.storage.from('userImages').upload(filePath, file, {
        cacheControl: '3600',
        upsert: false
      });
      if (uploadError) throw uploadError;

      const { data } = supabase.storage.from('userImages').getPublicUrl(filePath);
      if (!data.publicUrl) throw new Error('Could not get public URL.');

      await handleSave({ heroBackgroundUrl: data.publicUrl });
    } catch (error) {
      console.error('Hero background upload error:', error);
      showErrorToast(error.message || 'Không thể tải lên ảnh bìa.');
      setIsUploading(false);
    }
  };

  const uploadPageBackground = async (file) => {
    if (!file) return;
    setIsUploading(true);

    if (profileData?.pageBackgroundUrl) {
      const oldImage = parseSupabaseUrl(profileData.pageBackgroundUrl);
      if (oldImage) {
        try {
          await supabase.storage.from('userImages').remove([oldImage.path]);
        } catch (error) {
          console.error('Failed to delete old page background:', error);
        }
      }
    }

    try {
      const fileExt = file.name.split('.').pop();
      const fileName = `${user.id}-${uuidv4()}.${fileExt}`;
      const filePath = `backgrounds/${fileName}`;

      const { error: uploadError } = await supabase.storage.from('userImages').upload(filePath, file, {
        cacheControl: '3600',
        upsert: false
      });
      if (uploadError) throw uploadError;

      const { data } = supabase.storage.from('userImages').getPublicUrl(filePath);
      if (!data.publicUrl) throw new Error('Could not get public URL.');

      await handleSave({ pageBackgroundUrl: data.publicUrl });
    } catch (error) {
      console.error('Page background upload error:', error);
      showErrorToast(error.message || 'Không thể tải lên hình nền.');
      setIsUploading(false);
    }
  };

  const handleImageUpdate = (type, file) => {
    switch (type) {
      case 'avatar': uploadAvatar(file); break;
      case 'hero': uploadHeroBackground(file); break;
      case 'page': uploadPageBackground(file); break;
      default: console.warn('Unknown image update type:', type);
    }
  };

  // Open delete confirmation modal - memoized
  const openDeleteModal = useCallback((type) => {
    setDeleteImageModal({ isOpen: true, type });
  }, []);

  // Close delete confirmation modal - memoized
  const closeDeleteModal = useCallback(() => {
    setDeleteImageModal({ isOpen: false, type: null });
  }, []);

  // Actually delete the image
  const handleDeleteImage = async () => {
    const type = deleteImageModal.type;
    if (!type) return;

    setIsDeleting(true);
    
    try {
      let updateData = {};
      let oldUrl = null;
      
      switch (type) {
        case 'avatar':
          oldUrl = profileData?.avatarUrl;
          updateData = { avatarUrl: null };
          break;
        case 'hero':
          oldUrl = profileData?.heroBackgroundUrl;
          updateData = { heroBackgroundUrl: null };
          break;
        case 'page':
          oldUrl = profileData?.pageBackgroundUrl;
          updateData = { pageBackgroundUrl: null };
          break;
        default:
          setIsDeleting(false);
          closeDeleteModal();
          return;
      }

      // Delete from storage if exists
      if (oldUrl) {
        const oldImage = parseSupabaseUrl(oldUrl);
        if (oldImage) {
          try {
            await supabase.storage.from('userImages').remove([oldImage.path]);
            console.log('Deleted from storage:', oldImage.path);
          } catch (storageError) {
            console.error('Failed to delete from storage:', storageError);
          }
        }
      }

      // Update profile in database
      const response = await profileApi.update(user.id, updateData);
      console.log('Profile updated:', response);
      
      // Update local state
      setProfileData(prev => ({ ...prev, ...updateData }));
      setEditedProfile(prev => ({ ...prev, ...updateData }));
      
      // Update context
      if (updateProfileContext) {
        await updateProfileContext();
      }

      showSuccessToast('Đã xoá ảnh thành công.');
      closeDeleteModal();
    } catch (error) {
      console.error('Delete image error:', error);
      showErrorToast('Không thể xoá ảnh. Vui lòng thử lại.');
    } finally {
      setIsDeleting(false);
    }
  };

  const handleRevokeSession = useCallback((sessionId) => {
    setSessions(prev => prev.filter(s => s.id !== sessionId));
    showSuccessToast('Đã đăng xuất phiên làm việc.');
  }, []);

  const handleRevokeAllSessions = useCallback(() => {
    setSessions(prev => prev.filter(s => s.isCurrent));
    showSuccessToast('Đã đăng xuất tất cả các phiên khác.');
  }, []);

  // AI Settings Handlers
  const handleValidateApiKey = useCallback(async () => {
    if (!geminiApiKey.trim()) {
      showErrorToast('Vui lòng nhập API key');
      return;
    }
    
    // Don't validate masked placeholder
    if (!isApiKeyModified && geminiApiKey.startsWith('••')) {
      showSuccessToast('API key đã được lưu và xác thực trước đó');
      return;
    }
    
    setIsValidatingApiKey(true);
    setApiKeyStatus(null);
    
    try {
      const response = await writingApi.validateApiKey(geminiApiKey);
      if (response.data.valid) {
        setApiKeyStatus('valid');
        showSuccessToast('API key hợp lệ!');
      } else {
        setApiKeyStatus('invalid');
        showErrorToast('API key không hợp lệ hoặc đã hết hạn');
      }
    } catch (err) {
      console.error('API key validation error:', err);
      setApiKeyStatus('invalid');
      showErrorToast('Không thể xác thực API key');
    } finally {
      setIsValidatingApiKey(false);
    }
  }, [geminiApiKey, isApiKeyModified]);

  const handleSaveApiKey = useCallback(async () => {
    if (!geminiApiKey.trim()) {
      showErrorToast('Vui lòng nhập API key');
      return;
    }
    
    // Don't save masked placeholder
    if (!isApiKeyModified && geminiApiKey.startsWith('••')) {
      showSuccessToast('API key đã được lưu trước đó');
      return;
    }
    
    setIsSavingApiKey(true);
    
    try {
      await profileApi.update(user.id, { geminiApiKey: geminiApiKey });
      showSuccessToast('Đã lưu API key thành công!');
      setApiKeyStatus('valid');
      setIsApiKeyModified(false); // Reset modified state after saving
    } catch (err) {
      console.error('Error saving API key:', err);
      showErrorToast('Không thể lưu API key');
    } finally {
      setIsSavingApiKey(false);
    }
  }, [geminiApiKey, user?.id, isApiKeyModified]);

  const handleDeleteApiKey = useCallback(async () => {
    setIsSavingApiKey(true);
    
    try {
      await profileApi.update(user.id, { geminiApiKey: '' }); // Send empty string to clear
      setGeminiApiKey('');
      setApiKeyStatus(null);
      setIsApiKeyModified(false);
      showSuccessToast('Đã xoá API key');
    } catch (err) {
      console.error('Error deleting API key:', err);
      showErrorToast('Không thể xoá API key');
    } finally {
      setIsSavingApiKey(false);
    }
  }, [user?.id]);

  const getInitials = useCallback((name) => {
    if (!name) return '';
    const nameParts = name.split(' ');
    if (nameParts.length > 1) {
      return `${nameParts[0][0]}${nameParts[nameParts.length - 1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }, []);

  // Memoized computed values
  const displayInitials = useMemo(() => 
    getInitials(profileData?.fullName || user?.email), 
    [getInitials, profileData?.fullName, user?.email]
  );

  const displayName = useMemo(() => 
    profileData?.fullName || 'Người dùng', 
    [profileData?.fullName]
  );

  // Loading and error states
  if (loading || profileLoading) {
    return <FullPageLoader message="Đang tải trang cá nhân..." />;
  }

  if (error) {
    return <div className="dash-error container">{error}</div>;
  }

  // Tab definitions
  const tabs = [
    { id: 'personal', label: 'Thông tin', icon: FiUser },
    { id: 'appearance', label: 'Giao diện', icon: FiSliders },
    { id: 'security', label: 'Bảo mật', icon: FiShield },
    { id: 'ai-settings', label: 'Cài đặt AI', icon: FiCpu }
  ];

  return (
    <div 
      className="profile-page-new"
      style={profileData?.pageBackgroundUrl ? {
        backgroundImage: `url(${profileData.pageBackgroundUrl})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundAttachment: 'fixed'
      } : undefined}
    >
      {/* Overlay for readability when background image is set */}
      {profileData?.pageBackgroundUrl && (
        <div className="profile-page-new__overlay" />
      )}
      
      <UploadImageModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} onConfirm={handleImageUpdate} />
      <ChangePasswordModal isOpen={isChangePasswordModalOpen} onClose={() => setIsChangePasswordModalOpen(false)} />
      
      {/* Delete Image Confirmation Modal */}
      <ConfirmationModal
        isOpen={deleteImageModal.isOpen}
        onClose={closeDeleteModal}
        onConfirm={handleDeleteImage}
        title="Xác nhận xoá ảnh"
        confirmText={isDeleting ? "Đang xoá..." : "Xoá"}
        isConfirming={isDeleting}
      >
        <p style={{ textAlign: 'left', fontSize: '1rem' }}>
          {deleteImageModal.type === 'avatar' && 'Bạn có chắc muốn xoá ảnh đại diện? Hành động này không thể hoàn tác.'}
          {deleteImageModal.type === 'hero' && 'Bạn có chắc muốn xoá ảnh bìa? Hành động này không thể hoàn tác.'}
          {deleteImageModal.type === 'page' && 'Bạn có chắc muốn xoá hình nền trang? Hành động này không thể hoàn tác.'}
        </p>
      </ConfirmationModal>

      {/* Hero Section - Simple gradient background */}
      <section className={`profile-hero ${profileData?.heroBackgroundUrl ? 'profile-hero--has-cover' : ''} ${profileData?.pageBackgroundUrl ? 'profile-hero--transparent' : ''}`}>
        {/* Cover image box */}
        {profileData?.heroBackgroundUrl && (
          <div className="profile-cover-box">
            <div className="profile-cover-box__inner">
              <img 
                src={profileData.heroBackgroundUrl} 
                alt="Ảnh bìa" 
                className="profile-cover-box__img"
              />
            </div>
          </div>
        )}
        
        <div className="profile-hero__content">
          <div className="profile-avatar-container">
            {isUploading ? (
              <div className="profile-avatar-uploading" />
            ) : profileData?.avatarUrl ? (
              <img 
                src={profileData.avatarUrl} 
                alt="Avatar" 
                className="profile-avatar-large" 
              />
            ) : (
              <div className="profile-avatar-large profile-avatar-large--placeholder">
                {displayInitials}
              </div>
            )}
            <button 
              className="profile-avatar-edit" 
              onClick={() => setIsModalOpen(true)}
              disabled={isUploading}
              aria-label="Thay đổi ảnh đại diện"
            >
              <FiCamera />
            </button>
          </div>
          <h1 className="profile-user-name">
            {displayName}
          </h1>
          <p className="profile-user-email">
            {profileData?.email}
          </p>
        </div>
        {/* note: do not add container wrapper here - hero is intentionally positioned relative to viewport */}
      </section>

      {/* Tab Navigation */}
      <div className="profile-tabs container">
        <nav className="profile-tabs__nav">
          {tabs.map(tab => (
            <button
              key={tab.id}
              className={`profile-tabs__btn ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <tab.icon />
              {tab.label}
            </button>
          ))}
        </nav>

        {/* Tab Content */}
        <div className="profile-tabs__content">
          <AnimatePresence mode="wait">
            {activeTab === 'personal' && (
              <motion.div
                key="personal"
                className="profile-tab-panel"
                variants={tabContentVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
              >
                {/* Personal Info Card */}
                <div className="profile-card">
                  <div className="profile-card__header">
                    <h3 className="profile-card__title">
                      <FiUser />
                      Thông tin cá nhân
                    </h3>
                    <button className="profile-btn profile-btn--secondary profile-btn--small" onClick={handleEditToggle}>
                      {isEditing ? <><FiX /> Hủy</> : <><FiEdit3 /> Chỉnh sửa</>}
                    </button>
                  </div>

                  {isEditing ? (
                    <>
                      <div className="profile-form-grid">
                        <div className="profile-form-group">
                          <label className="profile-form-label">Họ và Tên</label>
                          <input 
                            type="text" 
                            name="fullName"
                            className="profile-form-input"
                            value={editedProfile.fullName || ''} 
                            onChange={handleInputChange} 
                            placeholder="Nhập họ và tên"
                          />
                        </div>
                        <div className="profile-form-group">
                          <label className="profile-form-label">Email</label>
                          <input 
                            type="email" 
                            className="profile-form-input"
                            value={editedProfile.email || ''} 
                            disabled 
                          />
                        </div>
                        <div className="profile-form-group">
                          <label className="profile-form-label">Số điện thoại</label>
                          <input 
                            type="text" 
                            name="phoneNumber"
                            className="profile-form-input"
                            value={editedProfile.phoneNumber || ''} 
                            onChange={handleInputChange} 
                            placeholder="Nhập số điện thoại"
                          />
                        </div>
                        <div className="profile-form-group">
                          <label className="profile-form-label">Địa chỉ</label>
                          <input 
                            type="text" 
                            name="address"
                            className="profile-form-input"
                            value={editedProfile.address || ''} 
                            onChange={handleInputChange} 
                            placeholder="Nhập địa chỉ"
                          />
                        </div>
                      </div>
                      <div className="profile-actions">
                        <button className="profile-btn profile-btn--secondary" onClick={() => setIsEditing(false)}>
                          <FiX /> Hủy
                        </button>
                        <button className="profile-btn profile-btn--primary" onClick={() => handleSave()}>
                          <FiSave /> Lưu thay đổi
                        </button>
                      </div>
                    </>
                  ) : (
                    <div className="profile-form-grid">
                      <div className="profile-form-group">
                        <label className="profile-form-label">Họ và Tên</label>
                        <p className={`profile-form-value ${!profileData?.fullName ? 'not-set' : ''}`}>
                          {profileData?.fullName || 'Chưa cập nhật'}
                        </p>
                      </div>
                      <div className="profile-form-group">
                        <label className="profile-form-label">Email</label>
                        <p className="profile-form-value">{profileData?.email}</p>
                      </div>
                      <div className="profile-form-group">
                        <label className="profile-form-label">Số điện thoại</label>
                        <p className={`profile-form-value ${!profileData?.phoneNumber ? 'not-set' : ''}`}>
                          {profileData?.phoneNumber || 'Chưa cập nhật'}
                        </p>
                      </div>
                      <div className="profile-form-group">
                        <label className="profile-form-label">Địa chỉ</label>
                        <p className={`profile-form-value ${!profileData?.address ? 'not-set' : ''}`}>
                          {profileData?.address || 'Chưa cập nhật'}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </motion.div>
            )}

            {activeTab === 'appearance' && (
              <motion.div
                key="appearance"
                className="profile-tab-panel"
                variants={tabContentVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
              >
                {/* Avatar Section */}
                <div className="profile-card">
                  <div className="profile-card__header">
                    <div className="profile-card__header-left">
                      <h3 className="profile-card__title">
                        <FiCamera />
                        Ảnh đại diện
                      </h3>
                      <p className="profile-card__description">Hiển thị ở header và trang hồ sơ</p>
                    </div>
                    <div className="profile-card__actions">
                      <button 
                        className="profile-btn profile-btn--secondary profile-btn--small"
                        onClick={() => setIsModalOpen(true)}
                      >
                        <FiEdit3 /> Thay đổi
                      </button>
                      {profileData?.avatarUrl && (
                        <button 
                          className="profile-btn profile-btn--danger profile-btn--small"
                          onClick={() => openDeleteModal('avatar')}
                        >
                          <FiTrash2 /> Xoá
                        </button>
                      )}
                    </div>
                  </div>
                  <div className="appearance-preview appearance-preview--avatar">
                    {profileData?.avatarUrl ? (
                      <img 
                        src={profileData.avatarUrl} 
                        alt="Ảnh đại diện" 
                        className="appearance-preview__avatar-img"
                      />
                    ) : (
                      <div className="appearance-preview__avatar-placeholder">
                        <FiUser />
                        <span>Chưa có ảnh đại diện</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Hero Background Section */}
                <div className="profile-card">
                  <div className="profile-card__header">
                    <div className="profile-card__header-left">
                      <h3 className="profile-card__title">
                        <FiMonitor />
                        Ảnh bìa
                      </h3>
                      <p className="profile-card__description">Ảnh banner hiển thị ở đầu trang hồ sơ</p>
                    </div>
                    <div className="profile-card__actions">
                      <button 
                        className="profile-btn profile-btn--secondary profile-btn--small"
                        onClick={() => setIsModalOpen(true)}
                      >
                        <FiEdit3 /> Thay đổi
                      </button>
                      {profileData?.heroBackgroundUrl && (
                        <button 
                          className="profile-btn profile-btn--danger profile-btn--small"
                          onClick={() => openDeleteModal('hero')}
                        >
                          <FiTrash2 /> Xoá
                        </button>
                      )}
                    </div>
                  </div>
                  <div className="appearance-preview appearance-preview--hero">
                    {profileData?.heroBackgroundUrl ? (
                      <img 
                        src={profileData.heroBackgroundUrl} 
                        alt="Ảnh bìa" 
                        className="appearance-preview__hero-img"
                      />
                    ) : (
                      <div className="appearance-preview__hero-placeholder">
                        <FiMonitor />
                        <span>Chưa có ảnh bìa</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Page Background Section */}
                <div className="profile-card">
                  <div className="profile-card__header">
                    <div className="profile-card__header-left">
                      <h3 className="profile-card__title">
                        <FiGlobe />
                        Hình nền trang
                      </h3>
                      <p className="profile-card__description">Hình nền cho toàn bộ trang hồ sơ</p>
                    </div>
                    <div className="profile-card__actions">
                      <button 
                        className="profile-btn profile-btn--secondary profile-btn--small"
                        onClick={() => setIsModalOpen(true)}
                      >
                        <FiEdit3 /> Thay đổi
                      </button>
                      {profileData?.pageBackgroundUrl && (
                        <button 
                          className="profile-btn profile-btn--danger profile-btn--small"
                          onClick={() => openDeleteModal('page')}
                        >
                          <FiTrash2 /> Xoá
                        </button>
                      )}
                    </div>
                  </div>
                  <div className="appearance-preview appearance-preview--background">
                    {profileData?.pageBackgroundUrl ? (
                      <img 
                        src={profileData.pageBackgroundUrl} 
                        alt="Hình nền trang" 
                        className="appearance-preview__background-img"
                      />
                    ) : (
                      <div className="appearance-preview__background-placeholder">
                        <FiGlobe />
                        <span>Chưa có hình nền trang</span>
                      </div>
                    )}
                  </div>
                </div>
              </motion.div>
            )}

            {activeTab === 'security' && (
              <motion.div
                key="security"
                className="profile-tab-panel"
                variants={tabContentVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
              >
                {/* Authentication Section */}
                <div className="profile-card">
                  <div className="profile-card__header">
                    <h3 className="profile-card__title">
                      <FiLock />
                      Xác thực
                    </h3>
                  </div>

                  <div className="security-item" onClick={() => setIsChangePasswordModalOpen(true)}>
                    <div className="security-item__info">
                      <div className="security-item__icon">
                        <FiKey />
                      </div>
                      <div className="security-item__text">
                        <h4>Đổi mật khẩu</h4>
                        <p>Thay đổi mật khẩu đăng nhập của bạn</p>
                      </div>
                    </div>
                    <FiChevronRight style={{ color: 'var(--text-muted)' }} />
                  </div>

                  <div className="security-item">
                    <div className="security-item__info">
                      <div className="security-item__icon">
                        <FiShield />
                      </div>
                      <div className="security-item__text">
                        <h4>Xác thực hai yếu tố (2FA)</h4>
                        <p>Thêm một lớp bảo mật cho tài khoản của bạn</p>
                      </div>
                    </div>
                    <div className="security-item__status">
                      {twoFactorEnabled ? (
                        <span className="security-badge security-badge--enabled">
                          <FiCheck /> Đã bật
                        </span>
                      ) : (
                        <span className="security-badge security-badge--disabled">
                          Chưa bật
                        </span>
                      )}
                      <div 
                        className={`toggle-switch ${twoFactorEnabled ? 'active' : ''}`}
                        onClick={() => setTwoFactorEnabled(!twoFactorEnabled)}
                      />
                    </div>
                  </div>
                </div>

                {/* Active Sessions */}
                <div className="profile-card">
                  <div className="profile-card__header">
                    <h3 className="profile-card__title">
                      <FiMonitor />
                      Phiên đăng nhập
                    </h3>
                    {sessions.length > 1 && (
                      <button 
                        className="profile-btn profile-btn--secondary profile-btn--small"
                        onClick={handleRevokeAllSessions}
                      >
                        <FiLogOut /> Đăng xuất tất cả
                      </button>
                    )}
                  </div>

                  <div className="sessions-list">
                    {sessions.map(session => (
                      <div key={session.id} className={`session-item ${session.isCurrent ? 'current' : ''}`}>
                        <div className="session-item__info">
                          <div className="session-item__icon">
                            <session.icon />
                          </div>
                          <div className="session-item__details">
                            <h4>
                              {session.device}
                              {session.isCurrent && (
                                <span className="session-item__current-badge">
                                  <FiCheck /> Phiên hiện tại
                                </span>
                              )}
                            </h4>
                            <p>{session.location} • {session.lastActive}</p>
                          </div>
                        </div>
                        {!session.isCurrent && (
                          <div className="session-item__actions">
                            <button 
                              className="profile-btn profile-btn--danger profile-btn--small"
                              onClick={() => handleRevokeSession(session.id)}
                            >
                              <FiLogOut /> Đăng xuất
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>

                {/* Login History */}
                <div className="profile-card">
                  <div className="profile-card__header">
                    <h3 className="profile-card__title">
                      <FiClock />
                      Lịch sử đăng nhập
                    </h3>
                  </div>

                  <div className="login-history">
                    {loginHistory.map(item => (
                      <div key={item.id} className="login-history-item">
                        <div className="login-history-item__info">
                          <div className={`login-history-item__icon ${item.success ? 'success' : 'failed'}`}>
                            {item.success ? <FiCheck /> : <FiX />}
                          </div>
                          <div className="login-history-item__details">
                            <h4>{item.device}</h4>
                            <p>IP: {item.ip}</p>
                          </div>
                        </div>
                        <span className="login-history-item__time">{item.time}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Linked Accounts */}
                <div className="profile-card">
                  <div className="profile-card__header">
                    <h3 className="profile-card__title">
                      <FiLink />
                      Tài khoản liên kết
                    </h3>
                  </div>

                  <div className="linked-account">
                    <div className="linked-account__info">
                      <div className="linked-account__icon google">
                        <FaGoogle />
                      </div>
                      <div className="linked-account__text">
                        <h4>Google</h4>
                        <p>Chưa liên kết</p>
                      </div>
                    </div>
                    <button className="profile-btn profile-btn--secondary profile-btn--small">
                      Liên kết
                    </button>
                  </div>

                  <div className="linked-account">
                    <div className="linked-account__info">
                      <div className="linked-account__icon facebook">
                        <FaFacebook />
                      </div>
                      <div className="linked-account__text">
                        <h4>Facebook</h4>
                        <p>Chưa liên kết</p>
                      </div>
                    </div>
                    <button className="profile-btn profile-btn--secondary profile-btn--small">
                      Liên kết
                    </button>
                  </div>
                </div>

                {/* Danger Zone */}
                <div className="profile-card danger-zone">
                  <div className="profile-card__header">
                    <h3 className="profile-card__title">
                      <FiAlertTriangle />
                      Vùng nguy hiểm
                    </h3>
                  </div>

                  <div className="danger-zone__content">
                    <div className="danger-zone__text">
                      <h4>Xóa tài khoản</h4>
                      <p>Sau khi xóa, tất cả dữ liệu của bạn sẽ bị mất vĩnh viễn.</p>
                    </div>
                    <button className="profile-btn profile-btn--danger">
                      <FiTrash2 /> Xóa tài khoản
                    </button>
                  </div>
                </div>
              </motion.div>
            )}

            {activeTab === 'ai-settings' && (
              <motion.div
                key="ai-settings"
                className="profile-tab-panel"
                variants={tabContentVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
              >
                {/* Gemini API Key Section */}
                <div className="profile-card">
                  <div className="profile-card__header">
                    <h3 className="profile-card__title">
                      <FiCpu />
                      Gemini API Key
                    </h3>
                  </div>

                  <div className="ai-settings-info">
                    <p className="ai-settings-description">
                      Để sử dụng tính năng chấm điểm bài Writing bằng AI, bạn cần cung cấp API key từ Google Gemini.
                      API key của bạn được lưu trữ an toàn và chỉ được sử dụng để chấm bài viết của bạn.
                    </p>
                    <a 
                      href="https://aistudio.google.com/app/apikey" 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="ai-settings-link"
                    >
                      <FiLink /> Lấy API key từ Google AI Studio
                    </a>
                  </div>

                  <div className="api-key-input-group">
                    <div className="api-key-input-wrapper">
                      <input
                        type={showApiKey ? 'text' : 'password'}
                        className="profile-form-input api-key-input"
                        value={geminiApiKey}
                        onChange={(e) => {
                          setGeminiApiKey(e.target.value);
                          setApiKeyStatus(null);
                          setIsApiKeyModified(true); // Mark as modified by user
                        }}
                        placeholder="Nhập Gemini API key của bạn (AIza...)"
                      />
                      <button
                        type="button"
                        className="api-key-toggle-btn"
                        onClick={() => setShowApiKey(!showApiKey)}
                        aria-label={showApiKey ? 'Ẩn API key' : 'Hiện API key'}
                      >
                        {showApiKey ? <FiEyeOff /> : <FiEye />}
                      </button>
                    </div>
                    
                    {apiKeyStatus && (
                      <div className={`api-key-status ${apiKeyStatus}`}>
                        {apiKeyStatus === 'valid' ? (
                          <><FiCheck /> API key hợp lệ</>
                        ) : (
                          <><FiX /> API key không hợp lệ</>
                        )}
                      </div>
                    )}
                  </div>

                  <div className="api-key-actions">
                    <button
                      className="profile-btn profile-btn--secondary"
                      onClick={handleValidateApiKey}
                      disabled={isValidatingApiKey || !geminiApiKey.trim()}
                    >
                      {isValidatingApiKey ? 'Đang kiểm tra...' : 'Kiểm tra API key'}
                    </button>
                    <button
                      className="profile-btn profile-btn--primary"
                      onClick={handleSaveApiKey}
                      disabled={isSavingApiKey || !geminiApiKey.trim()}
                    >
                      <FiSave /> {isSavingApiKey ? 'Đang lưu...' : 'Lưu API key'}
                    </button>
                    {geminiApiKey && (
                      <button
                        className="profile-btn profile-btn--danger"
                        onClick={handleDeleteApiKey}
                        disabled={isSavingApiKey}
                      >
                        <FiTrash2 /> Xoá
                      </button>
                    )}
                  </div>
                </div>

                {/* AI Usage Info */}
                <div className="profile-card">
                  <div className="profile-card__header">
                    <h3 className="profile-card__title">
                      <FiAlertTriangle />
                      Lưu ý khi sử dụng
                    </h3>
                  </div>

                  <div className="ai-usage-notes">
                    <ul>
                      <li>
                        <strong>Chi phí:</strong> Google Gemini tính phí theo số lượng token sử dụng. 
                        Mỗi bài Writing thường tiêu tốn khoảng 2,000-5,000 tokens.
                      </li>
                      <li>
                        <strong>Bảo mật:</strong> API key của bạn chỉ được sử dụng để gọi API Gemini 
                        và không được chia sẻ với bất kỳ bên thứ ba nào.
                      </li>
                      <li>
                        <strong>Model:</strong> Hệ thống sử dụng Gemini 2.5 Pro (model: `gemini-2.5-pro`) cho việc chấm điểm, 
                        đảm bảo tốc độ và độ chính xác cao.
                      </li>
                      <li>
                        <strong>Giới hạn:</strong> Có thể có giới hạn số lần gọi API miễn phí mỗi ngày. 
                        Kiểm tra quota tại Google AI Studio.
                      </li>
                    </ul>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
