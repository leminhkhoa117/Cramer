import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useAuthStore, useProfileStore } from '../stores';
import { profileApi } from '../api/backendApi';
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
  FiSmartphone,
  FiMonitor,
  FiLock,
  FiAlertTriangle,
  FiTrash2,
  FiCheck,
  FiChevronRight,
  FiLogOut,
  FiGlobe,
  FiClock,
  FiLink,
  FiMenu
} from 'react-icons/fi';
import { FaGoogle, FaFacebook } from 'react-icons/fa';
import '../css/pages/profile.css';
import '../css/shared/layout.css';
import FullPageLoader from '../components/FullPageLoader';
import { supabase } from '../api/supabaseClient';
import { v4 as uuidv4 } from 'uuid';
import UploadImageModal from '../components/UploadImageModal';
import ChangePasswordModal from '../components/ChangePasswordModal';
import ConfirmationModal from '../components/ConfirmationModal';

const tabContentVariants = {
  hidden: { opacity: 0, y: 10 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.3 } },
  exit: { opacity: 0, y: -10, transition: { duration: 0.2 } }
};

const ProfilePage = () => {
  const user = useAuthStore(state => state.user);
  const profile = useProfileStore(state => state.profile);
  const updateProfile = useProfileStore(state => state.updateProfile);
  const profileLoading = useProfileStore(state => state.loading);

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
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const closeSidebar = () => setIsSidebarOpen(false);

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

  const hasFetchedRef = useRef(false);

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user?.id || hasFetchedRef.current) return;
      hasFetchedRef.current = true;

      try {
        setLoading(true);
        const response = await profileApi.getById(user.id);
        const fullProfile = { ...response.data, email: user.email };
        setProfileData(fullProfile);
        setEditedProfile(fullProfile);
        setError(null);
      } catch (err) {
        console.error('Profile fetch error:', err);
        setError('Không thể tải thông tin cá nhân.');
        showErrorToast('Lỗi khi tải thông tin cá nhân.');
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, [user?.id]);

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
    const imageFields = ['avatarUrl', 'heroBackgroundUrl', 'pageBackgroundUrl'];
    const isImageUpdate = Object.keys(extraData).some(key => imageFields.includes(key));
    const profileToSave = isImageUpdate ? extraData : { ...editedProfile, ...extraData };

    try {
      const response = await profileApi.update(user.id, profileToSave);
      const updatedProfile = { ...response.data, email: user.email };
      setProfileData(updatedProfile);
      setEditedProfile(prev => ({ ...prev, ...updatedProfile }));
      await updateProfile(profileToSave);
      showSuccessToast('Cập nhật thông tin thành công!');
      setIsEditing(false);
    } catch (err) {
      console.error('Error saving profile:', err);
      showErrorToast('Lỗi khi cập nhật thông tin.');
    } finally {
      setIsUploading(false);
    }
  }, [editedProfile, user?.id, user?.email, updateProfile]);

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
        try { await supabase.storage.from('userImages').remove([oldImage.path]); } catch (error) { console.error('Failed to delete old avatar:', error); }
      }
    }

    const resizeImage = (file, maxWidth, maxHeight) => new Promise((resolve, reject) => {
      const img = new Image();
      img.src = URL.createObjectURL(file);
      img.onload = () => {
        const canvas = document.createElement('canvas');
        let { width, height } = img;
        if (width > height) {
          if (width > maxWidth) { height = Math.round((height * maxWidth) / width); width = maxWidth; }
        } else {
          if (height > maxHeight) { width = Math.round((width * maxHeight) / height); height = maxHeight; }
        }
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);
        canvas.toBlob((blob) => {
          if (blob) {
            resolve(new File([blob], file.name.replace(/\.[^/.]+$/, ".jpg"), { type: 'image/jpeg', lastModified: Date.now() }));
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
        try { await supabase.storage.from('userImages').remove([oldImage.path]); } catch (error) { console.error('Failed to delete old hero background:', error); }
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
        try { await supabase.storage.from('userImages').remove([oldImage.path]); } catch (error) { console.error('Failed to delete old page background:', error); }
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

  const openDeleteModal = useCallback((type) => {
    setDeleteImageModal({ isOpen: true, type });
  }, []);

  const closeDeleteModal = useCallback(() => {
    setDeleteImageModal({ isOpen: false, type: null });
  }, []);

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

      if (oldUrl) {
        const oldImage = parseSupabaseUrl(oldUrl);
        if (oldImage) {
          try { await supabase.storage.from('userImages').remove([oldImage.path]); } catch (storageError) { console.error('Failed to delete from storage:', storageError); }
        }
      }

      const response = await profileApi.update(user.id, updateData);
      console.log('Profile updated:', response);
      setProfileData(prev => ({ ...prev, ...updateData }));
      setEditedProfile(prev => ({ ...prev, ...updateData }));
      await updateProfile(updateData);
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

  const getInitials = useCallback((name) => {
    if (!name) return '';
    const nameParts = name.split(' ');
    if (nameParts.length > 1) {
      return `${nameParts[0][0]}${nameParts[nameParts.length - 1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }, []);

  const displayInitials = useMemo(() =>
    getInitials(profileData?.fullName || user?.email),
    [getInitials, profileData?.fullName, user?.email]
  );

  const displayName = useMemo(() =>
    profileData?.fullName || 'Người dùng',
    [profileData?.fullName]
  );

  if (loading || profileLoading) {
    return <FullPageLoader message="Đang tải trang cá nhân..." />;
  }

  if (error) {
    return (
      <div className="sl-page">
        <div className="sl-error">{error}</div>
      </div>
    );
  }

  const tabs = [
    { id: 'personal', label: 'Thông tin chung', icon: FiUser },
    { id: 'security', label: 'Bảo mật', icon: FiShield }
  ];

  return (
    <div
      className={`sl-page profile-page${profileData?.pageBackgroundUrl ? ' sl-page--has-bg' : ''}`}
    >
      {profileData?.pageBackgroundUrl && (
        <div
          className="sl-page__bg"
          style={{ backgroundImage: `url(${profileData.pageBackgroundUrl})` }}
        />
      )}
      {profileData?.pageBackgroundUrl && (
        <div className="sl-page__overlay" />
      )}

      <UploadImageModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} onConfirm={handleImageUpdate} />
      <ChangePasswordModal isOpen={isChangePasswordModalOpen} onClose={() => setIsChangePasswordModalOpen(false)} />

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

      <div className="sl-layout container">
        <aside className={`sl-sidebar ${isSidebarOpen ? 'sl-sidebar--drawer-open' : ''}`}>
          <div className="sl-sidebar__cover">
            {profileData?.heroBackgroundUrl ? (
              <img
                src={profileData.heroBackgroundUrl}
                alt="Ảnh bìa"
                className="sl-sidebar__cover-img"
              />
            ) : (
              <div className="sl-sidebar__cover-placeholder">
                <FiMonitor />
              </div>
            )}
            <button
              className="sl-sidebar__cover-edit"
              onClick={() => setIsModalOpen(true)}
              aria-label="Thay đổi ảnh bìa"
            >
              <FiCamera />
            </button>
          </div>

          <div className="sl-sidebar__header sl-sidebar__header--centered">
            <div className="profile-sidebar__avatar-container">
              {isUploading ? (
                <div className="profile-sidebar__avatar-uploading" />
              ) : profileData?.avatarUrl ? (
                <img
                  src={profileData.avatarUrl}
                  alt="Avatar"
                  className="profile-sidebar__avatar"
                />
              ) : (
                <div className="profile-sidebar__avatar profile-sidebar__avatar--placeholder">
                  {displayInitials}
                </div>
              )}
            </div>
            <h2 className="sl-sidebar__title">{displayName}</h2>
            <p className="sl-sidebar__subtitle">{profileData?.email}</p>
          </div>

          <nav className="sl-sidebar__nav">
            {tabs.map(tab => {
              const IconComponent = tab.icon;
              return (
                <button
                  key={tab.id}
                  type="button"
                  className={`sl-sidebar__nav-btn ${activeTab === tab.id ? 'active' : ''}`}
                  onClick={() => { setActiveTab(tab.id); closeSidebar(); }}
                >
                  <IconComponent />
                  <span>{tab.label}</span>
                  <FiChevronRight className="sl-sidebar__nav-arrow" />
                </button>
              );
            })}
          </nav>
        </aside>

        <main className="sl-content">
          <div className="profile-mobile-header">
            <span className="profile-mobile-header__title">
              {tabs.find(t => t.id === activeTab)?.label || 'Hồ sơ'}
            </span>
            <button
              type="button"
              className="profile-hamburger"
              onClick={() => setIsSidebarOpen(true)}
              aria-label="Mở menu"
            >
              <FiMenu />
            </button>
          </div>

          {/* Mobile sidebar drawer overlay — inside content, absolute positioned, never covers sidebar */}
          {isSidebarOpen && (
            <div
              className="profile-sidebar-overlay profile-sidebar-overlay--visible"
              onClick={closeSidebar}
              aria-hidden="true"
            />
          )}

          <AnimatePresence mode="wait">
            {activeTab === 'personal' && (
              <motion.div
                key="personal"
                className="sl-tab-panel"
                variants={tabContentVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
              >
                <div className="sl-card">
                  <div className="sl-card__header">
                    <div className="sl-card__header-left">
                      <h3 className="sl-card__title">
                        <FiGlobe />
                        Hình nền trang
                      </h3>
                      <p className="sl-card__description">Hình nền cho toàn bộ trang hồ sơ</p>
                    </div>
                    <div className="sl-card__actions">
                      <button
                        className="sl-btn sl-btn--secondary sl-btn--small"
                        onClick={() => setIsModalOpen(true)}
                      >
                        <FiEdit3 /> Thay đổi
                      </button>
                      {profileData?.pageBackgroundUrl && (
                        <button
                          className="sl-btn sl-btn--danger sl-btn--small"
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

                <div className="sl-card">
                  <div className="sl-card__header">
                    <h3 className="sl-card__title">
                      <FiUser />
                      Thông tin cá nhân
                    </h3>
                    <button className="sl-btn sl-btn--secondary sl-btn--small" onClick={handleEditToggle}>
                      {isEditing ? <><FiX /> Hủy</> : <><FiEdit3 /> Chỉnh sửa</>}
                    </button>
                  </div>

                  {isEditing ? (
                    <>
                      <div className="sl-form-grid">
                        <div className="sl-form-group">
                          <label className="sl-form-label">Họ và Tên</label>
                          <input
                            type="text"
                            name="fullName"
                            className="sl-form-input"
                            value={editedProfile.fullName || ''}
                            onChange={handleInputChange}
                            placeholder="Nhập họ và tên"
                          />
                        </div>
                        <div className="sl-form-group">
                          <label className="sl-form-label">Email</label>
                          <input
                            type="email"
                            className="sl-form-input"
                            value={editedProfile.email || ''}
                            disabled
                          />
                        </div>
                        <div className="sl-form-group">
                          <label className="sl-form-label">Số điện thoại</label>
                          <input
                            type="text"
                            name="phoneNumber"
                            className="sl-form-input"
                            value={editedProfile.phoneNumber || ''}
                            onChange={handleInputChange}
                            placeholder="Nhập số điện thoại"
                          />
                        </div>
                        <div className="sl-form-group">
                          <label className="sl-form-label">Địa chỉ</label>
                          <input
                            type="text"
                            name="address"
                            className="sl-form-input"
                            value={editedProfile.address || ''}
                            onChange={handleInputChange}
                            placeholder="Nhập địa chỉ"
                          />
                        </div>
                      </div>
                      <div className="sl-form-actions">
                        <button className="sl-btn sl-btn--secondary" onClick={() => setIsEditing(false)}>
                          <FiX /> Hủy
                        </button>
                        <button className="sl-btn sl-btn--primary" onClick={() => handleSave()}>
                          <FiSave /> Lưu thay đổi
                        </button>
                      </div>
                    </>
                  ) : (
                    <div className="sl-form-grid">
                      <div className="sl-form-group">
                        <label className="sl-form-label">Họ và Tên</label>
                        <p className={`sl-form-value ${!profileData?.fullName ? 'sl-form-value--muted' : ''}`}>
                          {profileData?.fullName || 'Chưa cập nhật'}
                        </p>
                      </div>
                      <div className="sl-form-group">
                        <label className="sl-form-label">Email</label>
                        <p className="sl-form-value">{profileData?.email}</p>
                      </div>
                      <div className="sl-form-group">
                        <label className="sl-form-label">Số điện thoại</label>
                        <p className={`sl-form-value ${!profileData?.phoneNumber ? 'sl-form-value--muted' : ''}`}>
                          {profileData?.phoneNumber || 'Chưa cập nhật'}
                        </p>
                      </div>
                      <div className="sl-form-group">
                        <label className="sl-form-label">Địa chỉ</label>
                        <p className={`sl-form-value ${!profileData?.address ? 'sl-form-value--muted' : ''}`}>
                          {profileData?.address || 'Chưa cập nhật'}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </motion.div>
            )}

            {activeTab === 'security' && (
              <motion.div
                key="security"
                className="sl-tab-panel"
                variants={tabContentVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
              >
                <div className="sl-card">
                  <div className="sl-card__header">
                    <h3 className="sl-card__title">
                      <FiLock />
                      Xác thực
                    </h3>
                  </div>

                  <div className="sl-list-item sl-list-item--clickable" onClick={() => setIsChangePasswordModalOpen(true)}>
                    <div className="sl-list-item__info">
                      <div className="sl-list-item__icon">
                        <FiKey />
                      </div>
                      <div className="sl-list-item__text">
                        <h4>Đổi mật khẩu</h4>
                        <p>Thay đổi mật khẩu đăng nhập của bạn</p>
                      </div>
                    </div>
                    <FiChevronRight style={{ color: 'var(--sl-text-muted)' }} />
                  </div>

                  <div className="sl-list-item profile-security-item--disabled">
                    <div className="sl-list-item__info">
                      <div className="sl-list-item__icon">
                        <FiShield />
                      </div>
                      <div className="sl-list-item__text">
                        <h4>Xác thực hai yếu tố (2FA)</h4>
                        <p>Tính năng đang được phát triển</p>
                      </div>
                    </div>
                    <span className="profile-security-badge profile-security-badge--disabled">
                      Sắp ra mắt
                    </span>
                  </div>
                </div>

                <div className="sl-card">
                  <div className="sl-card__header">
                    <h3 className="sl-card__title">
                      <FiMonitor />
                      Phiên đăng nhập
                    </h3>
                    {sessions.length > 1 && (
                      <button
                        className="sl-btn sl-btn--secondary sl-btn--small"
                        onClick={handleRevokeAllSessions}
                      >
                        <FiLogOut /> Đăng xuất tất cả
                      </button>
                    )}
                  </div>

                  <div>
                    {sessions.map(session => (
                      <div key={session.id} className={`sl-list-item ${session.isCurrent ? 'profile-list-item--current' : ''}`}>
                        <div className="sl-list-item__info">
                          <div className="sl-list-item__icon">
                            <session.icon />
                          </div>
                          <div className="sl-list-item__text">
                            <h4>
                              {session.device}
                              {session.isCurrent && (
                                <span className="profile-session__current-badge">
                                  <FiCheck /> Phiên hiện tại
                                </span>
                              )}
                            </h4>
                            <p>{session.location} &bull; {session.lastActive}</p>
                          </div>
                        </div>
                        {!session.isCurrent && (
                          <div className="sl-list-item__actions">
                            <button
                              className="sl-btn sl-btn--danger sl-btn--small"
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

                <div className="sl-card">
                  <div className="sl-card__header">
                    <h3 className="sl-card__title">
                      <FiClock />
                      Lịch sử đăng nhập
                    </h3>
                  </div>

                  <div className="profile-login-history">
                    {loginHistory.map(item => (
                      <div key={item.id} className="profile-login-history__item">
                        <div className="profile-login-history__info">
                          <div className={`profile-login-history__icon ${item.success ? 'profile-login-history__icon--success' : 'profile-login-history__icon--failed'}`}>
                            {item.success ? <FiCheck /> : <FiX />}
                          </div>
                          <div className="profile-login-history__details">
                            <h4>{item.device}</h4>
                            <p>IP: {item.ip}</p>
                          </div>
                        </div>
                        <span className="profile-login-history__time">{item.time}</span>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="sl-card">
                  <div className="sl-card__header">
                    <h3 className="sl-card__title">
                      <FiLink />
                      Tài khoản liên kết
                    </h3>
                  </div>

                  <div className="sl-list-item">
                    <div className="sl-list-item__info">
                      <div className="sl-list-item__icon profile-linked-account__icon--google">
                        <FaGoogle />
                      </div>
                      <div className="sl-list-item__text">
                        <h4>Google</h4>
                        <p>Chưa liên kết</p>
                      </div>
                    </div>
                    <button className="sl-btn sl-btn--secondary sl-btn--small">
                      Liên kết
                    </button>
                  </div>

                  <div className="sl-list-item">
                    <div className="sl-list-item__info">
                      <div className="sl-list-item__icon profile-linked-account__icon--facebook">
                        <FaFacebook />
                      </div>
                      <div className="sl-list-item__text">
                        <h4>Facebook</h4>
                        <p>Chưa liên kết</p>
                      </div>
                    </div>
                    <button className="sl-btn sl-btn--secondary sl-btn--small">
                      Liên kết
                    </button>
                  </div>
                </div>

                <div className="sl-card profile-danger-zone">
                  <div className="sl-card__header">
                    <h3 className="sl-card__title">
                      <FiAlertTriangle />
                      Vùng nguy hiểm
                    </h3>
                  </div>

                  <div className="profile-danger-zone__content">
                    <div className="profile-danger-zone__text">
                      <h4>Xóa tài khoản</h4>
                      <p>Sau khi xóa, tất cả dữ liệu của bạn sẽ bị mất vĩnh viễn.</p>
                    </div>
                    <button className="sl-btn sl-btn--danger">
                      <FiTrash2 /> Xóa tài khoản
                    </button>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
};

export default ProfilePage;