# 🤖 Copilot CLI Session

> **Session ID:** `bce08275-a8bc-454d-af8e-34aa280e13e9`  
> **Started:** 12/8/2025, 12:31:04 AM  
> **Duration:** 23m 41s  
> **Exported:** 12/8/2025, 12:54:46 AM

<sub>⏱️ 0s</sub>

### ℹ️ Info

Logged in as user: huuunleashed

---

<sub>⏱️ 0s</sub>

### ℹ️ Info

Configured MCP servers: Supabase_MCP

---

<sub>⏱️ 2s</sub>

### ℹ️ Info

Connected to GitHub MCP Server

---

<sub>⏱️ 7s</sub>

### ℹ️ Info

Model changed to: claude-opus-4.5. The new model will be used for the next conversation.

---

<sub>⏱️ 2m 49s</sub>

### 👤 User

Please add a model selector in the API key in the profile setting. Currently the program is using gemini-2.5-flash as the main model, but I wonder what other options are plausible - gemini-2.5-flash-lite, gemma-3-27b, gemini-2.5-pro (check for the availability of this model)?

---

<sub>⏱️ 2m 58s</sub>

### ✅ `glob`

"**/*profile*"

```
No files matched the pattern.
```


---

<sub>⏱️ 2m 58s</sub>

### ✅ `glob`

"**/*setting*"

```
No files matched the pattern.
```


---

<sub>⏱️ 2m 58s</sub>

### ✅ `grep`

"gemini-2.5-flash"

```
.\backend\src\main\java\com\cramer\service\GeminiGradingService.java
```


---

<sub>⏱️ 2m 58s</sub>

### ✅ `grep`

"API.*key|apiKey"

<details>
<summary>17 matches</summary>

```
.\AGENTS.md
.\GEMINI.md
.\docs\questions_rows.sql
.\frontend\src\api\backendApi.js
.\docs\backend\supabase-backend.md
.\backend\src\main\java\com\cramer\entity\Profile.java
.\backend\src\main\java\com\cramer\service\AsyncGradingService.java
.\docs\backend\migrations\001_writing_feature.sql
.\backend\src\main\java\com\cramer\service\GeminiGradingService.java
.\backend\src\main\java\com\cramer\service\implement\ProfileServiceImpl.java
.\backend\src\main\java\com\cramer\dto\ProfileDTO.java
.\frontend\src\css\ProfilePage.css
.\backend\src\main\java\com\cramer\service\SupabaseAdminService.java
.\backend\src\main\java\com\cramer\controller\WritingController.java
.\frontend\src\pages\Profile.jsx
.\backend\src\main\java\com\cramer\service\SupabaseClient.java
.\backend\src\main\java\com\cramer\service\WritingSubmissionService.java
```

</details>


---

<sub>⏱️ 3m 4s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx

<details>
<summary>1032 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -1,1120 +1,1120 @@
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
 import '../css/common/SidebarLayout.css';
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
     { id: 'personal', label: 'Thông tin chung', icon: FiUser },
     { id: 'security', label: 'Bảo mật', icon: FiShield },
     { id: 'ai-settings', label: 'Cài đặt AI', icon: FiCpu }
   ];
 
   return (
     <div
       className="profile-page"
       style={profileData?.pageBackgroundUrl ? {
         backgroundImage: `url(${profileData.pageBackgroundUrl})`,
         backgroundSize: 'cover',
         backgroundPosition: 'center',
         backgroundAttachment: 'fixed'
       } : undefined}
     >
       {/* Overlay for readability when background image is set */}
       {profileData?.pageBackgroundUrl && (
         <div className="profile-page__overlay" />
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
 
       {/* Main Layout: Sidebar + Content */}
       <div className="profile-layout container">
         {/* Left Sidebar */}
         <aside className="profile-sidebar">
           {/* Cover Image Banner */}
           <div className="profile-sidebar__cover">
             {profileData?.heroBackgroundUrl ? (
               <img
                 src={profileData.heroBackgroundUrl}
                 alt="Ảnh bìa"
                 className="profile-sidebar__cover-img"
               />
             ) : (
               <div className="profile-sidebar__cover-placeholder">
                 <FiMonitor />
               </div>
             )}
             <button
               className="profile-sidebar__cover-edit"
               onClick={() => setIsModalOpen(true)}
               aria-label="Thay đổi ảnh bìa"
             >
               <FiCamera />
             </button>
           </div>
 
           {/* Sidebar Header with Avatar */}
           <div className="profile-sidebar__header">
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
             <h2 className="profile-sidebar__name">{displayName}</h2>
             <p className="profile-sidebar__email">{profileData?.email}</p>
           </div>
 
           {/* Sidebar Navigation */}
           <nav className="profile-sidebar__nav">
             {tabs.map(tab => {
               const IconComponent = tab.icon;
               return (
                 <button
                   key={tab.id}
                   type="button"
                   className={`profile-sidebar__nav-btn ${activeTab === tab.id ? 'active' : ''}`}
                   onClick={() => setActiveTab(tab.id)}
                 >
                   <IconComponent />
                   <span>{tab.label}</span>
                   <FiChevronRight className="profile-sidebar__nav-arrow" />
                 </button>
               );
             })}
           </nav>
         </aside>
 
         {/* Right Content Area */}
         <main className="profile-content">
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
         </main>
       </div>
     </div>
   );
 };
 
 export default ProfilePage;
```

</details>


---

<sub>⏱️ 3m 4s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java

<details>
<summary>715 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
@@ -1,842 +1,842 @@
 package com.cramer.service;
 
 import com.cramer.entity.WritingSubmission;
 import com.fasterxml.jackson.core.JsonProcessingException;
 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.http.*;
 import org.springframework.stereotype.Service;
 import org.springframework.web.client.RestTemplate;
 
 import java.io.InputStream;
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.net.URL;
 import java.time.OffsetDateTime;
 import java.util.*;
 
 /**
  * Service for grading IELTS Writing essays using Gemini AI API.
  * Provides detailed feedback including band scores, corrections, and sample essays.
  * 
  * Uses Gemini 2.5 Flash for faster grading with higher rate limits.
  * Rate limits (Free Tier): 10 RPM for 2.5-flash, 2 RPM for 2.5-pro
  */
 @Service
 public class GeminiGradingService {
 
     private static final Logger logger = LoggerFactory.getLogger(GeminiGradingService.class);
     
     // Using Gemini 2.5 Flash for higher rate limits (10 RPM vs 2 RPM for Pro)
     // Trade-off: Slightly less nuanced but still accurate for IELTS grading
     private static final String GEMINI_MODEL = "gemini-2.5-flash";
     private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent";
     
     // Minimum word thresholds for IELTS Writing
     private static final int TASK_1_MIN_WORDS = 150;
     private static final int TASK_2_MIN_WORDS = 250;
     private static final int MINIMUM_ESSAY_WORDS = 20; // Below this = band 0-1
     
     private final RestTemplate restTemplate;
     private final ObjectMapper objectMapper;
 
     public GeminiGradingService() {
         this.restTemplate = new RestTemplate();
         this.objectMapper = new ObjectMapper();
     }
 
     /**
      * Grade a writing submission using Gemini AI.
      * 
      * @param submission The writing submission to grade
      * @param taskPrompt The original task prompt/question
      * @param taskImageUrl Optional image URL for Task 1 (charts, diagrams, maps)
      * @param apiKey User's Gemini API key
      * @return Updated submission with grading results
      */
     public WritingSubmission gradeSubmission(WritingSubmission submission, String taskPrompt, 
                                               String taskImageUrl, String apiKey) {
         if (apiKey == null || apiKey.trim().isEmpty()) {
             logger.error("No Gemini API key provided for grading");
             submission.setGradingStatus("FAILED");
             Map<String, Object> errorFeedback = new HashMap<>();
             errorFeedback.put("error", "No API key provided. Please add your Gemini API key in Profile settings.");
             submission.setAiFeedback(errorFeedback);
             return submission;
         }
 
         try {
             // Check for empty or minimal essay - return band 0-1 without calling API
             String essayText = submission.getEssayText();
             int wordCount = submission.getWordCount();
             
             if (essayText == null || essayText.trim().isEmpty()) {
                 logger.warn("Empty essay submitted for grading");
                 return handleEmptyEssay(submission);
             }
             
             if (wordCount < MINIMUM_ESSAY_WORDS) {
                 logger.warn("Essay too short ({} words) - below minimum threshold", wordCount);
                 return handleMinimalEssay(submission, wordCount);
             }
             
             submission.setGradingStatus("GRADING");
             
             // Call Gemini API with multimodal support
             String response = callGeminiApiWithImage(
                 submission.getTaskNumber(),
                 taskPrompt,
                 essayText,
                 wordCount,
                 taskImageUrl,
                 apiKey
             );
             
             // Parse and apply results
             parseAndApplyGradingResults(submission, response);
             
             submission.setGradingStatus("COMPLETED");
             submission.setGradedAt(OffsetDateTime.now());
             
             logger.info("Successfully graded submission {} with overall band {}", 
                        submission.getId(), submission.getOverallBand());
             
         } catch (Exception e) {
             logger.error("Failed to grade submission {}: {}", submission.getId(), e.getMessage(), e);
             submission.setGradingStatus("FAILED");
             Map<String, Object> errorFeedback = new HashMap<>();
             errorFeedback.put("error", "Grading failed: " + e.getMessage());
             submission.setAiFeedback(errorFeedback);
         }
 
         return submission;
     }
 
     /**
      * Handle empty essay - return band 0 without calling API.
      */
     private WritingSubmission handleEmptyEssay(WritingSubmission submission) {
         submission.setGradingStatus("COMPLETED");
         submission.setOverallBand(BigDecimal.ZERO);
         
         Map<String, Object> bandScores = new HashMap<>();
         String criterion = submission.getTaskNumber() == 1 ? "task_achievement" : "task_response";
         bandScores.put(criterion, 0.0);
         bandScores.put("coherence_cohesion", 0.0);
         bandScores.put("lexical_resource", 0.0);
         bandScores.put("grammatical_range_accuracy", 0.0);
         submission.setBandScores(bandScores);
         
         Map<String, Object> feedback = new HashMap<>();
         feedback.put("error", "Bài viết trống. Vui lòng viết bài để được chấm điểm.");
         Map<String, Object> feedbackSummary = new HashMap<>();
         feedbackSummary.put("strengths", Collections.emptyList());
         feedbackSummary.put("weaknesses", Arrays.asList("Không có nội dung bài viết"));
         feedbackSummary.put("improvement_tips", "Hãy viết bài hoàn chỉnh với đủ số từ yêu cầu.");
         feedback.put("feedback_summary", feedbackSummary);
         submission.setAiFeedback(feedback);
         submission.setGradedAt(OffsetDateTime.now());
         
         return submission;
     }
 
     /**
      * Handle minimal essay (under 20 words) - return band 1 without calling API.
      */
     private WritingSubmission handleMinimalEssay(WritingSubmission submission, int wordCount) {
         submission.setGradingStatus("COMPLETED");
         submission.setOverallBand(BigDecimal.ONE);
         
         Map<String, Object> bandScores = new HashMap<>();
         String criterion = submission.getTaskNumber() == 1 ? "task_achievement" : "task_response";
         bandScores.put(criterion, 1.0);
         bandScores.put("coherence_cohesion", 1.0);
         bandScores.put("lexical_resource", 1.0);
         bandScores.put("grammatical_range_accuracy", 1.0);
         submission.setBandScores(bandScores);
         
         Map<String, Object> feedback = new HashMap<>();
         Map<String, Object> feedbackSummary = new HashMap<>();
         feedbackSummary.put("strengths", Collections.emptyList());
         feedbackSummary.put("weaknesses", Arrays.asList(
             "Bài viết quá ngắn (" + wordCount + " từ)",
             "Không đủ nội dung để đánh giá"
         ));
         int minWords = submission.getTaskNumber() == 1 ? TASK_1_MIN_WORDS : TASK_2_MIN_WORDS;
         feedbackSummary.put("improvement_tips", 
             "Task " + submission.getTaskNumber() + " yêu cầu tối thiểu " + minWords + " từ. " +
             "Bài viết của bạn chỉ có " + wordCount + " từ.");
         feedback.put("feedback_summary", feedbackSummary);
         
         Map<String, String> criteriaComments = new HashMap<>();
         criteriaComments.put("task_achievement", "Bài viết quá ngắn, không thể đánh giá Task Achievement/Response.");
         criteriaComments.put("coherence_cohesion", "Không đủ nội dung để đánh giá tính mạch lạc và liên kết.");
         criteriaComments.put("lexical_resource", "Không đủ nội dung để đánh giá vốn từ vựng.");
         criteriaComments.put("grammatical_range", "Không đủ nội dung để đánh giá ngữ pháp.");
         feedback.put("criteria_comments", criteriaComments);
         
         submission.setAiFeedback(feedback);
         submission.setGradedAt(OffsetDateTime.now());
         
         return submission;
     }
 
     /**
      * Build the comprehensive IELTS grading system prompt with official band descriptors.
      * Enhanced with calibration anchors and generous scoring philosophy.
      * CALIBRATED based on official IELTS sample answers and band scores.
      */
     private String buildSystemPrompt(Integer taskNumber, int wordCount) {
         StringBuilder prompt = new StringBuilder();
         
         prompt.append("# HỆ THỐNG CHẤM ĐIỂM IELTS WRITING - PHIÊN BẢN ĐÃ HIỆU CHUẨN\n\n");
         prompt.append("Bạn là một giám khảo IELTS được chứng nhận với hơn 15 năm kinh nghiệm. ");
         prompt.append("Nhiệm vụ của bạn là chấm điểm bài viết IELTS một cách chính xác và công bằng theo tiêu chí band descriptors chính thức của IELTS.\n\n");
         
         // CRITICAL: Calibration-focused grading philosophy - SIGNIFICANTLY ENHANCED
         prompt.append("## TRIẾT LÝ CHẤM ĐIỂM - CỰC KỲ QUAN TRỌNG\n\n");
         
         prompt.append("### 🎯 NGUYÊN TẮC VÀNG - ĐỌC KỸ TRƯỚC KHI CHẤM:\n\n");
         
         prompt.append("**1. CHẤM DỰA TRÊN NĂNG LỰC NGÔN NGỮ THỂ HIỆN:**\n");
         prompt.append("   - Đánh giá KHẢ NĂNG VIẾT TIẾNG ANH của thí sinh\n");
         prompt.append("   - Cấu trúc câu có đa dạng không? Từ vựng có phong phú không?\n");
         prompt.append("   - Có thể diễn đạt ý tưởng mạch lạc không?\n");
         prompt.append("   - **QUAN TRỌNG**: Ngay cả bài lạc đề vẫn có thể có điểm ngôn ngữ tốt!\n\n");
         
         prompt.append("**2. XỬ LÝ BÀI LẠC ĐỀ (OFF-TOPIC) - RẤT QUAN TRỌNG:**\n");
         prompt.append("   - Nếu bài HOÀN TOÀN lạc đề: Task Response/Achievement bị ảnh hưởng (giảm 1-2 band)\n");
         prompt.append("   - NHƯNG: Coherence, Lexical Resource, Grammar vẫn chấm BÌNH THƯỜNG theo năng lực thể hiện\n");
         prompt.append("   - Ví dụ thực tế: Bài lạc đề nhưng viết tốt có thể đạt: TR=4.5, CC=6.5, LR=6.5, GRA=6.0 → Overall = 6.0\n");
         prompt.append("   - **KHÔNG** cho tất cả tiêu chí điểm thấp chỉ vì lạc đề!\n\n");
         
         prompt.append("**3. PHÂN LOẠI LỖI - ẢNH HƯỞNG ĐIỂM:**\n");
         prompt.append("   - **Lỗi MINOR** (không ảnh hưởng hiểu): article, số ít/số nhiều nhỏ, typo 1-2 chữ → hầu như KHÔNG trừ điểm\n");
         prompt.append("   - **Lỗi MODERATE**: awkward phrasing, collocation hơi sai → trừ nhẹ, vẫn có thể band 7+\n");
         prompt.append("   - **Lỗi MAJOR** (gây hiểu sai/không hiểu): sai meaning, câu không có nghĩa → ảnh hưởng điểm\n\n");
         
         prompt.append("**4. QUY TẮC \"BENEFIT OF DOUBT\":**\n");
         prompt.append("   - Khi phân vân giữa 2 band liền kề → **LUÔN CHỌN BAND CAO HƠN**\n");
         prompt.append("   - Khi có cả điểm mạnh và điểm yếu rõ ràng → **ƯU TIÊN GHI NHẬN ĐIỂM MẠNH**\n");
         prompt.append("   - Nhớ: Thí sinh viết trong điều kiện thi giới hạn thời gian (20 phút Task 1, 40 phút Task 2)\n\n");
         
         prompt.append("### 📊 BẢNG CALIBRATION THỰC TẾ (dựa trên bài mẫu IELTS chính thức):\n\n");
         prompt.append("| Đặc điểm bài viết | Band thường đạt |\n");
         prompt.append("|-------------------|----------------|\n");
         prompt.append("| Lạc đề hoàn toàn nhưng ngôn ngữ khá | 5.5 - 6.0 |\n");
         prompt.append("| Đúng đề, ý tưởng cơ bản, nhiều lỗi grammar/vocab | 5.0 - 5.5 |\n");
         prompt.append("| Đúng đề, ý tưởng OK, một số lỗi grammar không ảnh hưởng hiểu | 6.0 - 6.5 |\n");
         prompt.append("| Đúng đề, ý tưởng tốt, cấu trúc rõ ràng, ít lỗi | 6.5 - 7.0 |\n");
         prompt.append("| Đúng đề, ý tưởng sâu, từ vựng đa dạng, grammar chính xác | 7.0 - 7.5 |\n");
         prompt.append("| Xuất sắc toàn diện, chỉ lỗi rất nhỏ | 7.5 - 8.0 |\n");
         prompt.append("| Gần như hoàn hảo | 8.0 - 8.5 |\n");
         prompt.append("| Hoàn hảo như native speaker | 9.0 |\n\n");
         
         prompt.append("### 📈 CHI TIẾT VỀ TỪNG BAND (quan trọng để không chấm quá khắt khe):\n\n");
         
         prompt.append("**Band 6.0 - 6.5 (Competent User - PHỔ BIẾN NHẤT):**\n");
         prompt.append("- Đây là band của đa số sinh viên đại học Việt Nam viết tốt\n");
         prompt.append("- Có lỗi grammar nhưng KHÔNG ảnh hưởng communication\n");
         prompt.append("- Từ vựng adequate (đủ dùng) dù không fancy\n");
         prompt.append("- Có thể có một số ý chưa developed đầy đủ\n");
         prompt.append("- **QUAN TRỌNG**: Bài có lỗi rải rác nhưng đọc hiểu được = Band 6.0+\n\n");
         
         prompt.append("**Band 7.0 - 7.5 (Good User):**\n");
         prompt.append("- Ý tưởng được develop rõ ràng với examples/support\n");
         prompt.append("- Có variety trong sentence structures\n");
         prompt.append("- Có sử dụng một số từ vựng less common\n");
         prompt.append("- Lỗi ít và không systematic\n");
         prompt.append("- **QUAN TRỌNG**: Error-free sentences FREQUENT (không phải tất cả câu)\n\n");
         
         prompt.append("**Band 8.0+ (Very Good User):**\n");
         prompt.append("- Majority of sentences error-free (cho phép 2-4 lỗi nhỏ trong toàn bài)\n");
         prompt.append("- Wide range of vocabulary với skilful use\n");
         prompt.append("- Ideas well-extended và well-supported\n");
         prompt.append("- **QUAN TRỌNG**: 'Occasional errors' = VẪN CÓ THỂ ĐẠT BAND 8.0!\n\n");
         
         // Word count context
         int minWords = taskNumber == 1 ? TASK_1_MIN_WORDS : TASK_2_MIN_WORDS;
         prompt.append("## 📝 THÔNG TIN SỐ TỪ\n");
         prompt.append("- **Số từ đã nộp**: ").append(wordCount).append(" từ\n");
         prompt.append("- **Yêu cầu tối thiểu**: ").append(minWords).append(" từ\n");
         if (wordCount < minWords) {
             int deficit = minWords - wordCount;
             prompt.append("- **CHÚ Ý**: Bài viết THIẾU ").append(deficit).append(" từ. ");
             prompt.append("Điều này ảnh hưởng Task Achievement/Response, nhưng các tiêu chí khác vẫn chấm theo năng lực thể hiện.\n");
         } else {
             prompt.append("- Đã đạt yêu cầu số từ ✓\n");
         }
         prompt.append("\n");
         
         // Official Band Descriptors (Band 3-9)
         if (taskNumber == 1) {
             prompt.append(getTask1BandDescriptors());
         } else {
             prompt.append(getTask2BandDescriptors());
         }
         
         return prompt.toString();
     }
 
     /**
      * Get official IELTS Task 1 band descriptors (bands 5-9) with full detail.
      * Based on official IELTS.org band descriptors (May 2023).
      * ENHANCED with calibration notes for fair scoring.
      */
     private String getTask1BandDescriptors() {
         StringBuilder desc = new StringBuilder();
         desc.append("## TIÊU CHÍ CHẤM ĐIỂM CHÍNH THỨC - IELTS WRITING TASK 1\n\n");
         
         // Band 9
         desc.append("### Band 9 (Expert User) - RẤT HIẾM\n");
         desc.append("- **Task Achievement**: Tất cả yêu cầu được đáp ứng đầy đủ. Có thể có sơ suất cực kỳ hiếm về nội dung.\n");
         desc.append("- **Coherence & Cohesion**: Thông điệp được theo dõi effortlessly. Cohesion rất hiếm khi thu hút sự chú ý. Paragraphing skilfully managed.\n");
         desc.append("- **Lexical Resource**: Full flexibility và precise use. Wide range với very natural và sophisticated control. Errors extremely rare.\n");
         desc.append("- **Grammar**: Wide range với full flexibility và control. Errors extremely rare.\n\n");
         
         // Band 8
         desc.append("### Band 8 (Very Good User)\n");
         desc.append("- **Task Achievement**: Đáp ứng tất cả yêu cầu appropriately, relevantly và sufficiently. Key features được chọn lọc khéo léo và trình bày rõ ràng. Occasional omissions OK.\n");
         desc.append("- **Coherence & Cohesion**: Message followed with ease. Information logically sequenced. Cohesion well managed. Occasional lapses OK.\n");
         desc.append("- **Lexical Resource**: Wide resource used fluently và flexibly. Skilful use of uncommon/idiomatic items. Occasional errors minimal impact.\n");
         desc.append("- **Grammar**: Wide range used flexibly và accurately. **MAJORITY of sentences error-free**. Occasional non-systematic errors.\n");
         desc.append("- **📌 CALIBRATION**: 'Occasional' = 2-4 lỗi trong toàn bài, KHÔNG phải mỗi đoạn!\n\n");
         
         // Band 7
         desc.append("### Band 7 (Good User)\n");
         desc.append("- **Task Achievement**: Đáp ứng các yêu cầu. Nội dung relevant và accurate với vài omissions. Có CLEAR OVERVIEW, data được categorised phù hợp.\n");
         desc.append("- **Coherence & Cohesion**: Information logically organised với clear progression. A few lapses OK. Cohesive devices used flexibly.\n");
         desc.append("- **Lexical Resource**: Sufficient flexibility và precision. Ability to use less common items. Few errors in spelling/word form.\n");
         desc.append("- **Grammar**: Variety of complex structures với some flexibility. Generally well controlled. **Error-free sentences FREQUENT**.\n");
         desc.append("- **📌 CALIBRATION**: Bài có overview tốt, cover key features, 5-7 lỗi nhỏ → xứng đáng Band 7.0-7.5\n\n");
         
         // Band 6
         desc.append("### Band 6 (Competent User) - PHỔ BIẾN NHẤT\n");
         desc.append("- **Task Achievement**: Tập trung vào requirements với appropriate format. Key features covered adequately. Có thể có vài chi tiết irrelevant/inaccurate.\n");
         desc.append("- **Coherence & Cohesion**: Generally arranged coherently với clear overall progression. Some cohesion may be faulty/mechanical.\n");
         desc.append("- **Lexical Resource**: Generally adequate cho task. Meaning generally clear dù restricted range.\n");
         desc.append("- **Grammar**: Mix of simple và complex forms nhưng limited flexibility. Errors **RARELY impede communication**.\n");
         desc.append("- **📌 CALIBRATION**: Đây là band phổ biến. Bài có lỗi nhưng vẫn đọc hiểu được = Band 6.0-6.5\n\n");
         
         // Band 5
         desc.append("### Band 5 (Modest User)\n");
         desc.append("- **Task Achievement**: Generally addresses requirements. Key features not adequately covered. May focus quá nhiều vào details.\n");
         desc.append("- **Coherence & Cohesion**: Organisation evident nhưng không wholly logical. Sentences không fluently linked.\n");
         desc.append("- **Lexical Resource**: Limited nhưng minimally adequate. Simple vocabulary, frequent lapses in appropriacy.\n");
         desc.append("- **Grammar**: Limited và repetitive structures. Complex sentences thường faulty.\n");
         desc.append("- **📌 CALIBRATION**: Bài thiếu overview, không cover key features đủ, nhiều lỗi → Band 5.0\n\n");
         
         return desc.toString();
     }
 
     /**
      * Get official IELTS Task 2 band descriptors (bands 5-9) with full detail.
      * Based on official IELTS.org band descriptors (May 2023).
      * ENHANCED with calibration notes for fair scoring.
      */
     private String getTask2BandDescriptors() {
         StringBuilder desc = new StringBuilder();
         desc.append("## TIÊU CHÍ CHẤM ĐIỂM CHÍNH THỨC - IELTS WRITING TASK 2\n\n");
         
         // Special note about off-topic essays
         desc.append("### ⚠️ XỬ LÝ BÀI LẠC ĐỀ (OFF-TOPIC):\n");
         desc.append("Nếu bài viết KHÔNG trả lời đúng câu hỏi đề bài:\n");
         desc.append("- **Task Response**: Bị ảnh hưởng nặng → giảm xuống Band 4.0-5.0\n");
         desc.append("- **Coherence & Cohesion**: Chấm BÌNH THƯỜNG theo cấu trúc bài (có intro, body, conclusion? Có linking words?)\n");
         desc.append("- **Lexical Resource**: Chấm BÌNH THƯỜNG theo từ vựng sử dụng (có variety? Có advanced vocab?)\n");
         desc.append("- **Grammar**: Chấm BÌNH THƯỜNG theo grammar (có complex sentences? Errors có impede meaning?)\n");
         desc.append("- **VÍ DỤ**: Bài lạc đề với ngôn ngữ tốt có thể đạt: TR=4.5, CC=6.5, LR=6.5, GRA=6.5 → Overall=6.0\n\n");
         
         // Band 9
         desc.append("### Band 9 (Expert User) - RẤT HIẾM\n");
         desc.append("- **Task Response**: Prompt được addressed và explored sâu sắc. Position rõ ràng, fully developed. Ideas relevant, fully extended và well supported.\n");
         desc.append("- **Coherence & Cohesion**: Message followed effortlessly. Cohesion barely attracts attention. Paragraphing skilfully managed.\n");
         desc.append("- **Lexical Resource**: Full flexibility và precise use. Very natural và sophisticated control. Errors extremely rare.\n");
         desc.append("- **Grammar**: Wide range với full flexibility và control. Errors extremely rare.\n\n");
         
         // Band 8
         desc.append("### Band 8 (Very Good User)\n");
         desc.append("- **Task Response**: Prompt addressed appropriately và sufficiently. Position clear và well-developed. Ideas relevant, well extended và supported.\n");
         desc.append("- **Coherence & Cohesion**: Message followed with ease. Information logically sequenced. Cohesion well managed. Occasional lapses OK.\n");
         desc.append("- **Lexical Resource**: Wide resource used fluently và flexibly. Skilful use of uncommon/idiomatic items. Occasional inaccuracies minimal impact.\n");
         desc.append("- **Grammar**: Wide range used flexibly và accurately. **MAJORITY of sentences error-free**. Occasional non-systematic errors OK.\n");
         desc.append("- **📌 CALIBRATION**: 'Occasional' = 2-4 lỗi rải rác trong TOÀN BÀI. Bài có vài lỗi nhỏ VẪN có thể đạt Band 8!\n\n");
         
         // Band 7
         desc.append("### Band 7 (Good User)\n");
         desc.append("- **Task Response**: Main parts addressed appropriately. Position clear và developed. May have tendency to over-generalise/lack focus.\n");
         desc.append("- **Coherence & Cohesion**: Information logically organised với clear progression. A few minor lapses OK. Paragraphing effective.\n");
         desc.append("- **Lexical Resource**: Sufficient flexibility và precision. Ability to use less common items. Few errors in spelling/word form.\n");
         desc.append("- **Grammar**: Variety of complex structures với some flexibility và accuracy. **Error-free sentences FREQUENT**. Few errors don't impede.\n");
         desc.append("- **📌 CALIBRATION**: Bài có khoảng 5-8 lỗi nhỏ rải rác, cấu trúc đa dạng → xứng đáng Band 7.0\n\n");
         
         // Band 6
         desc.append("### Band 6 (Competent User) - PHỔ BIẾN NHẤT\n");
         desc.append("- **Task Response**: Main parts addressed (có thể không đều). Position relevant nhưng conclusions may be unclear/repetitive.\n");
         desc.append("- **Coherence & Cohesion**: Generally coherent với clear overall progression. Some faulty/mechanical cohesion OK.\n");
         desc.append("- **Lexical Resource**: Generally adequate. Meaning generally clear dù restricted range. Some errors in spelling/word form.\n");
         desc.append("- **Grammar**: Mix of simple và complex nhưng limited flexibility. Errors **RARELY impede communication**.\n");
         desc.append("- **📌 CALIBRATION**: Đây là band của sinh viên đại học Việt Nam viết khá. Có lỗi nhưng vẫn đọc hiểu được = Band 6.0-6.5\n\n");
         
         // Band 5
         desc.append("### Band 5 (Modest User)\n");
         desc.append("- **Task Response**: Main parts incompletely addressed. Position expressed nhưng development không always clear.\n");
         desc.append("- **Coherence & Cohesion**: Organisation evident nhưng không wholly logical. Paragraphing may be inadequate.\n");
         desc.append("- **Lexical Resource**: Limited nhưng minimally adequate. Frequent lapses in appropriacy.\n");
         desc.append("- **Grammar**: Limited, repetitive structures. Complex sentences tend to be faulty.\n");
         desc.append("- **📌 CALIBRATION**: Bài có nhiều lỗi GÂY KHÓ HIỂU, hoặc ý tưởng rất hạn chế → Band 5.0\n\n");
         
         return desc.toString();
     }
 
     /**
      * Build the user prompt with task details and essay.
      * Enhanced with Vietnamese code-switching and detailed feedback schema.
      */
     private String buildUserPrompt(Integer taskNumber, String taskPrompt, String essay) {
         StringBuilder prompt = new StringBuilder();
         
         prompt.append("## Đề bài (Task Prompt):\n");
         prompt.append(taskPrompt).append("\n\n");
         
         prompt.append("## Bài viết của thí sinh:\n");
         prompt.append("```\n").append(essay).append("\n```\n\n");
         
         prompt.append("## YÊU CẦU ĐỊNH DẠNG RESPONSE\n");
         prompt.append("Bạn PHẢI trả về một JSON object hợp lệ với cấu trúc chính xác như sau. ");
         prompt.append("KHÔNG thêm bất kỳ text nào ngoài JSON. KHÔNG dùng markdown code fences.\n\n");
         
         prompt.append("### NGÔN NGỮ OUTPUT:\n");
         prompt.append("- Tất cả feedback, explanation, comments PHẢI viết bằng **tiếng Việt**\n");
         prompt.append("- Có thể code-switch với thuật ngữ tiếng Anh khi cần (ví dụ: 'coherence', 'collocation', 'topic sentence')\n");
         prompt.append("- Sample essays vẫn viết bằng tiếng Anh (vì đây là bài IELTS)\n\n");
         
         prompt.append("{\n");
         prompt.append("  \"band_scores\": {\n");
         if (taskNumber == 1) {
             prompt.append("    \"task_achievement\": <số từ 3.0-9.0, dùng bước 0.5>,\n");
         } else {
             prompt.append("    \"task_response\": <số từ 3.0-9.0, dùng bước 0.5>,\n");
         }
         prompt.append("    \"coherence_cohesion\": <số từ 3.0-9.0, dùng bước 0.5>,\n");
         prompt.append("    \"lexical_resource\": <số từ 3.0-9.0, dùng bước 0.5>,\n");
         prompt.append("    \"grammatical_range_accuracy\": <số từ 3.0-9.0, dùng bước 0.5>\n");
         prompt.append("  },\n");
         prompt.append("  \"overall_band\": <trung bình 4 tiêu chí, làm tròn đến 0.5 gần nhất>,\n\n");
         
         // Enhanced sentence corrections with severity
         prompt.append("  \"sentence_corrections\": [\n");
         prompt.append("    {\n");
         prompt.append("      \"original\": \"<câu gốc chính xác từ bài viết - copy nguyên văn>\",\n");
         prompt.append("      \"corrected\": \"<câu đã sửa>\",\n");
         prompt.append("      \"error_type\": \"<grammar|spelling|vocabulary|punctuation|coherence|style>\",\n");
         prompt.append("      \"severity\": \"<major|minor>\",\n");
         prompt.append("      \"explanation\": \"<giải thích bằng tiếng Việt, ví dụ: 'Thiếu article 'the' trước danh từ xác định'>\"\n");
         prompt.append("    }\n");
         prompt.append("  ],\n\n");
         
         // Enhanced paragraph rewrites
         prompt.append("  \"paragraph_rewrites\": [\n");
         prompt.append("    {\n");
         prompt.append("      \"paragraph_index\": <index 0-based>,\n");
         prompt.append("      \"original\": \"<đoạn văn gốc - copy nguyên văn cả đoạn>\",\n");
         prompt.append("      \"improved\": \"<đoạn văn cải thiện ở mức band+1>\",\n");
         prompt.append("      \"improvements_made\": [\"<cải thiện 1 bằng tiếng Việt>\", \"<cải thiện 2>\"]\n");
         prompt.append("    }\n");
         prompt.append("  ],\n\n");
         
         // NEW: Highlighted vocabulary with position info
         prompt.append("  \"vocabulary_highlights\": [\n");
         prompt.append("    {\n");
         prompt.append("      \"word\": \"<từ/cụm từ đáng chú ý trong bài>\",\n");
         prompt.append("      \"category\": \"<advanced_good|collocation_good|academic|error|awkward>\",\n");
         prompt.append("      \"note\": \"<nhận xét ngắn bằng tiếng Việt, ví dụ: 'Dùng collocation tốt' hoặc 'Sai word form'>\"\n");
         prompt.append("    }\n");
         prompt.append("  ],\n\n");
         
         // NEW: Error severity summary
         prompt.append("  \"error_analysis\": {\n");
         prompt.append("    \"major_errors\": <số lỗi lớn ảnh hưởng nghĩa>,\n");
         prompt.append("    \"minor_errors\": <số lỗi nhỏ không ảnh hưởng nghĩa>,\n");
         prompt.append("    \"summary\": \"<tóm tắt bằng tiếng Việt, ví dụ: 'Hầu hết lỗi là minor errors không ảnh hưởng communication'>\"\n");
         prompt.append("  },\n\n");
         
         prompt.append("  \"sample_essay_band_plus_one\": \"<bài viết hoàn chỉnh ở mức band+1, viết bằng tiếng Anh>\",\n");
         prompt.append("  \"sample_essay_band_9\": \"<bài mẫu band 9 cho đề này, viết bằng tiếng Anh>\",\n\n");
         
         prompt.append("  \"feedback_summary\": {\n");
         prompt.append("    \"strengths\": [\"<điểm mạnh 1 - tiếng Việt>\", \"<điểm mạnh 2>\", \"<điểm mạnh 3>\"],\n");
         prompt.append("    \"weaknesses\": [\"<điểm yếu 1 - tiếng Việt>\", \"<điểm yếu 2>\"],\n");
         prompt.append("    \"writing_approach\": \"<gợi ý cách tiếp cận bài viết - tiếng Việt, 2-3 câu>\",\n");
         prompt.append("    \"improvement_tips\": \"<tips cải thiện cụ thể - tiếng Việt, 2-3 câu>\"\n");
         prompt.append("  },\n\n");
         
         prompt.append("  \"word_analysis\": [\n");
         prompt.append("    {\n");
         prompt.append("      \"word\": \"<từ/cụm từ nổi bật>\",\n");
         prompt.append("      \"definition\": \"<định nghĩa tiếng Việt>\",\n");
         prompt.append("      \"context\": \"<câu chứa từ đó trong bài>\",\n");
         prompt.append("      \"usage_quality\": \"<good|acceptable|incorrect>\"\n");
         prompt.append("    }\n");
         prompt.append("  ],\n\n");
         
         prompt.append("  \"criteria_comments\": {\n");
         if (taskNumber == 1) {
             prompt.append("    \"task_achievement\": \"<2-3 câu giải thích điểm Task Achievement - tiếng Việt, có thể dùng thuật ngữ tiếng Anh>\",\n");
         } else {
             prompt.append("    \"task_achievement\": \"<2-3 câu giải thích điểm Task Response - tiếng Việt, có thể dùng thuật ngữ tiếng Anh>\",\n");
         }
         prompt.append("    \"coherence_cohesion\": \"<2-3 câu giải thích điểm Coherence & Cohesion - tiếng Việt>\",\n");
         prompt.append("    \"lexical_resource\": \"<2-3 câu giải thích điểm Lexical Resource - tiếng Việt>\",\n");
         prompt.append("    \"grammatical_range\": \"<2-3 câu giải thích điểm Grammatical Range & Accuracy - tiếng Việt>\"\n");
         prompt.append("  }\n");
         prompt.append("}\n\n");
         
         prompt.append("## HƯỚNG DẪN CHẤM ĐIỂM - ĐÃ HIỆU CHUẨN\n\n");
         
         prompt.append("### ⚠️ CẢNH BÁO: TRÁNH CHẤM QUÁ KHẮT KHE\n");
         prompt.append("Hệ thống AI thường có xu hướng chấm khắt khe hơn giám khảo thực. ");
         prompt.append("Hãy nhớ các nguyên tắc sau:\n\n");
         
         prompt.append("### Về điểm số:\n");
         prompt.append("1. **ĐỌC TOÀN BỘ BÀI** trước khi cho điểm - đừng vội kết luận từ vài câu đầu\n");
         prompt.append("2. **TÁCH BIỆT 4 TIÊU CHÍ**: Mỗi tiêu chí được chấm độc lập\n");
         prompt.append("   - Bài lạc đề → Task Response thấp, nhưng CC/LR/GRA có thể vẫn cao\n");
         prompt.append("   - Bài nhiều lỗi grammar → GRA thấp, nhưng TR/CC/LR có thể vẫn cao\n");
         prompt.append("3. **NHỚ**: Band 6.0-6.5 là band PHỔ BIẾN nhất - đừng ngại cho điểm này\n");
         prompt.append("4. **NHỚ**: Band 7.0+ cho bài viết có error-free sentences FREQUENT (không phải tất cả)\n");
         prompt.append("5. **NHỚ**: Band 8.0 vẫn cho phép 'occasional errors' - 2-4 lỗi nhỏ rải rác\n");
         prompt.append("6. **QUAN TRỌNG**: Khi phân vân giữa band X và band X+0.5 → CHỌN BAND CAO HƠN\n\n");
         
         prompt.append("### Ví dụ calibration thực tế:\n");
         prompt.append("| Tình huống | Điểm đúng | Điểm sai (quá khắt khe) |\n");
         prompt.append("|------------|-----------|------------------------|\n");
         prompt.append("| Bài lạc đề nhưng viết mạch lạc, grammar OK | 5.5-6.0 | 4.0-5.0 |\n");
         prompt.append("| Bài đúng đề, có 5-7 lỗi grammar nhỏ | 6.5-7.0 | 5.5-6.0 |\n");
         prompt.append("| Bài tốt, từ vựng đa dạng, 2-3 lỗi nhỏ | 7.5-8.0 | 6.5-7.0 |\n");
         prompt.append("| Data coverage tốt, có overview, vài lỗi nhỏ | 7.0-7.5 | 6.0-6.5 |\n\n");
         
         prompt.append("### Về nội dung feedback:\n");
         prompt.append("7. Cung cấp ít nhất 3-5 sentence corrections với giải thích rõ ràng\n");
         prompt.append("8. Viết lại ít nhất introduction và 1 body paragraph\n");
         prompt.append("9. Sample essays phải realistic và relevant với đề bài cụ thể\n");
         prompt.append("10. **TẤT CẢ feedback phải bằng tiếng Việt**, có thể code-switch thuật ngữ tiếng Anh\n");
         prompt.append("11. Highlight ít nhất 5-8 từ/cụm từ đáng chú ý (cả tốt và cần sửa)\n");
         prompt.append("12. **Khuyến khích thí sinh** - nêu điểm mạnh trước điểm yếu\n\n");
         
         prompt.append("### Lưu ý cuối:\n");
         prompt.append("- Chỉ trả về JSON object, không có markdown fences hay text thừa\n");
         prompt.append("- Đảm bảo JSON hợp lệ, escape đúng các ký tự đặc biệt\n");
         
         return prompt.toString();
     }
 
     /**
      * Call Gemini API with multimodal support (text + image for Task 1).
      */
     private String callGeminiApiWithImage(Integer taskNumber, String taskPrompt, String essay, 
                                            int wordCount, String imageUrl, String apiKey) {
         String url = GEMINI_API_URL + "?key=" + apiKey;
         
         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.APPLICATION_JSON);
         
         // Build request with system instruction and user content
         Map<String, Object> requestBody = new HashMap<>();
         
         // System instruction (separate from content)
         Map<String, Object> systemInstruction = new HashMap<>();
         List<Map<String, Object>> systemParts = new ArrayList<>();
         Map<String, Object> systemTextPart = new HashMap<>();
         systemTextPart.put("text", buildSystemPrompt(taskNumber, wordCount));
         systemParts.add(systemTextPart);
         systemInstruction.put("parts", systemParts);
         requestBody.put("systemInstruction", systemInstruction);
         
         // User content
         Map<String, Object> userContent = new HashMap<>();
         userContent.put("role", "user");
         List<Map<String, Object>> parts = new ArrayList<>();
         
         // Add image for Task 1 if available
         if (taskNumber == 1 && imageUrl != null && !imageUrl.trim().isEmpty()) {
             try {
                 Map<String, Object> imagePart = createImagePart(imageUrl);
                 if (imagePart != null) {
                     parts.add(imagePart);
                     logger.info("Added image to grading request: {}", imageUrl);
                 }
             } catch (Exception e) {
                 logger.warn("Failed to add image to request, proceeding with text-only: {}", e.getMessage());
             }
         }
         
         // Add text prompt
         Map<String, Object> textPart = new HashMap<>();
         textPart.put("text", buildUserPrompt(taskNumber, taskPrompt, essay));
         parts.add(textPart);
         
         userContent.put("parts", parts);
         List<Map<String, Object>> contents = new ArrayList<>();
         contents.add(userContent);
         requestBody.put("contents", contents);
         
         // Generation config optimized for accurate JSON output
         // Using slightly higher temperature for more generous scoring
         Map<String, Object> generationConfig = new HashMap<>();
         generationConfig.put("temperature", 0.4); // Slightly higher for more generous, less rigid scoring
         generationConfig.put("topP", 0.92);
         generationConfig.put("topK", 40);
         generationConfig.put("maxOutputTokens", 16384); // Increased for detailed feedback
         generationConfig.put("responseMimeType", "application/json"); // Force JSON output
         requestBody.put("generationConfig", generationConfig);
         
         HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
         
         try {
             ResponseEntity<String> response = restTemplate.exchange(
                 url, HttpMethod.POST, entity, String.class
             );
             
             if (response.getStatusCode() != HttpStatus.OK) {
                 throw new RuntimeException("Gemini API returned status: " + response.getStatusCode());
             }
             
             return response.getBody();
         } catch (Exception e) {
             logger.error("Gemini API call failed: {}", e.getMessage());
             throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
         }
     }
 
     /**
      * Create image part for multimodal request.
      * Downloads image and converts to base64 inline data.
      */
     private Map<String, Object> createImagePart(String imageUrl) {
         try {
             URL url = new URL(imageUrl);
             try (InputStream is = url.openStream()) {
                 byte[] imageBytes = is.readAllBytes();
                 String base64Data = Base64.getEncoder().encodeToString(imageBytes);
                 
                 // Determine MIME type from URL
                 String mimeType = "image/png"; // Default
                 String lowerUrl = imageUrl.toLowerCase();
                 if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) {
                     mimeType = "image/jpeg";
                 } else if (lowerUrl.contains(".gif")) {
                     mimeType = "image/gif";
                 } else if (lowerUrl.contains(".webp")) {
                     mimeType = "image/webp";
                 }
                 
                 Map<String, Object> imagePart = new HashMap<>();
                 Map<String, Object> inlineData = new HashMap<>();
                 inlineData.put("mimeType", mimeType);
                 inlineData.put("data", base64Data);
                 imagePart.put("inlineData", inlineData);
                 
                 return imagePart;
             }
         } catch (Exception e) {
             logger.error("Failed to download image from {}: {}", imageUrl, e.getMessage());
             return null;
         }
     }
 
     /**
      * Parse Gemini API response and apply grading results to submission.
      * Includes calibration adjustment to counteract AI's tendency to score too harshly.
      */
     private void parseAndApplyGradingResults(WritingSubmission submission, String apiResponse) 
             throws JsonProcessingException {
         
         JsonNode root = objectMapper.readTree(apiResponse);
         
         // Extract the generated text from Gemini response
         String generatedText = root
             .path("candidates").get(0)
             .path("content")
             .path("parts").get(0)
             .path("text").asText();
         
         // Clean up the response - remove markdown code blocks if present
         generatedText = generatedText.trim();
         if (generatedText.startsWith("```json")) {
             generatedText = generatedText.substring(7);
         }
         if (generatedText.startsWith("```")) {
             generatedText = generatedText.substring(3);
         }
         if (generatedText.endsWith("```")) {
             generatedText = generatedText.substring(0, generatedText.length() - 3);
         }
         generatedText = generatedText.trim();
         
         // Parse the grading JSON
         JsonNode gradingResult = objectMapper.readTree(generatedText);
         
         // Extract band scores and apply calibration adjustment
         JsonNode bandScoresNode = gradingResult.path("band_scores");
         Map<String, Object> rawBandScores = objectMapper.convertValue(bandScoresNode, Map.class);
         
         // Apply calibration adjustment to each criterion
         // AI tends to score 0.5-1.0 band lower than human examiners
         Map<String, Object> calibratedBandScores = new HashMap<>();
         for (Map.Entry<String, Object> entry : rawBandScores.entrySet()) {
             double rawScore = ((Number) entry.getValue()).doubleValue();
             double calibratedScore = applyCriteriaCalibratedAdjustment(rawScore);
             calibratedBandScores.put(entry.getKey(), calibratedScore);
         }
         submission.setBandScores(calibratedBandScores);
         
         // Calculate and set overall band (rounded to nearest 0.5)
         // Use calibrated scores for overall calculation
         double calibratedOverall = calibratedBandScores.values().stream()
             .mapToDouble(v -> ((Number) v).doubleValue())
             .average()
             .orElse(0.0);
         BigDecimal overallBand = roundToNearestHalf(calibratedOverall);
         submission.setOverallBand(overallBand);
         
         logger.info("Score calibration applied: raw overall={}, calibrated overall={}", 
             gradingResult.path("overall_band").asDouble(), overallBand);
         
         // Build AI feedback object
         Map<String, Object> aiFeedback = new HashMap<>();
         
         if (gradingResult.has("sentence_corrections")) {
             aiFeedback.put("sentence_corrections", 
                 objectMapper.convertValue(gradingResult.path("sentence_corrections"), List.class));
         }
         
         if (gradingResult.has("paragraph_rewrites")) {
             aiFeedback.put("paragraph_rewrites", 
                 objectMapper.convertValue(gradingResult.path("paragraph_rewrites"), List.class));
         }
         
         // NEW: Vocabulary highlights for essay annotation
         if (gradingResult.has("vocabulary_highlights")) {
             aiFeedback.put("vocabulary_highlights", 
                 objectMapper.convertValue(gradingResult.path("vocabulary_highlights"), List.class));
         }
         
         // NEW: Error analysis summary
         if (gradingResult.has("error_analysis")) {
             aiFeedback.put("error_analysis", 
                 objectMapper.convertValue(gradingResult.path("error_analysis"), Map.class));
         }
         
         if (gradingResult.has("sample_essay_band_plus_one")) {
             aiFeedback.put("sample_essay_band_plus_one", 
                 gradingResult.path("sample_essay_band_plus_one").asText());
         }
         
         if (gradingResult.has("sample_essay_band_9")) {
             aiFeedback.put("sample_essay_band_9", 
                 gradingResult.path("sample_essay_band_9").asText());
         }
         
         if (gradingResult.has("feedback_summary")) {
             aiFeedback.put("feedback_summary", 
                 objectMapper.convertValue(gradingResult.path("feedback_summary"), Map.class));
         }
         
         if (gradingResult.has("word_analysis")) {
             aiFeedback.put("word_analysis", 
                 objectMapper.convertValue(gradingResult.path("word_analysis"), List.class));
         }
         
         if (gradingResult.has("criteria_comments")) {
             aiFeedback.put("criteria_comments", 
                 objectMapper.convertValue(gradingResult.path("criteria_comments"), Map.class));
         }
         
         submission.setAiFeedback(aiFeedback);
     }
 
     /**
      * Apply calibration adjustment to individual criterion scores.
      * Based on empirical observation that AI grades approximately 0.5 band lower than human examiners.
      * 
      * Adjustment curve:
      * - Scores 4.0-5.5: Add 0.5 (these are often underscored significantly)
      * - Scores 6.0-6.5: Add 0.5 (most common band, often underscored)
      * - Scores 7.0-7.5: Add 0.5 (still commonly underscored)
      * - Scores 8.0+: Add 0.0 (high scores are usually accurate)
      * 
      * Maximum score after adjustment is 9.0
      */
     private double applyCriteriaCalibratedAdjustment(double rawScore) {
         double adjustment;
         
         if (rawScore < 4.0) {
             // Very low scores - minimal adjustment
             adjustment = 0.0;
         } else if (rawScore < 8.0) {
             // Scores 4.0-7.5: Apply 0.5 band uplift
             adjustment = 0.5;
         } else {
             // Scores 8.0+: No adjustment needed (high scores are usually accurate)
             adjustment = 0.0;
         }
         
         double adjusted = rawScore + adjustment;
         
         // Cap at 9.0 and ensure rounded to 0.5
         adjusted = Math.min(9.0, adjusted);
         adjusted = Math.round(adjusted * 2) / 2.0;
         
         return adjusted;
     }
 
     /**
      * Round a band score to the nearest 0.5 according to IELTS rules.
      */
     private BigDecimal roundToNearestHalf(double score) {
         double rounded = Math.round(score * 2) / 2.0;
         return BigDecimal.valueOf(rounded).setScale(1, RoundingMode.HALF_UP);
     }
 
     /**
      * Validate API key by making a simple test request.
      */
     public boolean validateApiKey(String apiKey) {
         if (apiKey == null || apiKey.trim().isEmpty()) {
             return false;
         }
         
         try {
             String url = GEMINI_API_URL + "?key=" + apiKey;
             
             HttpHeaders headers = new HttpHeaders();
             headers.setContentType(MediaType.APPLICATION_JSON);
             
             Map<String, Object> requestBody = new HashMap<>();
             Map<String, Object> content = new HashMap<>();
             List<Map<String, String>> parts = new ArrayList<>();
             Map<String, String> textPart = new HashMap<>();
             textPart.put("text", "Hi");
             parts.add(textPart);
             content.put("parts", parts);
             List<Map<String, Object>> contents = new ArrayList<>();
             contents.add(content);
             requestBody.put("contents", contents);
             
             HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
             
             ResponseEntity<String> response = restTemplate.exchange(
                 url, HttpMethod.POST, entity, String.class
             );
             
             return response.getStatusCode() == HttpStatus.OK;
         } catch (Exception e) {
             logger.warn("API key validation failed: {}", e.getMessage());
             return false;
         }
     }
 }
```

</details>


---

<sub>⏱️ 3m 4s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\entity\Profile.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\entity\Profile.java

<details>
<summary>117 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/entity/Profile.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/entity/Profile.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/entity/Profile.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/entity/Profile.java
@@ -1,150 +1,150 @@
 package com.cramer.entity;
 
 import jakarta.persistence.*;
 import org.hibernate.annotations.CreationTimestamp;
 
 import java.time.OffsetDateTime;
 import java.util.UUID;
 
 /**
  * Entity representing user profiles.
  * Linked to Supabase auth.users table via id (UUID).
  */
 @Entity
 @Table(name = "profiles", schema = "public")
 public class Profile {
 
     @Id
     @Column(name = "id", nullable = false, updatable = false)
     private UUID id; // Mirrors auth.users.id
 
     @Column(name = "username", nullable = false, unique = true)
     private String username;
 
     @Column(name = "full_name")
     private String fullName;
 
     @Column(name = "phone_number")
     private String phoneNumber;
 
     @Column(name = "address")
     private String address;
 
     @Column(name = "avatar_url")
     private String avatarUrl;
 
     @Column(name = "hero_background_url")
     private String heroBackgroundUrl;
 
     @Column(name = "page_background_url")
     private String pageBackgroundUrl;
 
     @Column(name = "gemini_api_key")
     private String geminiApiKey;
 
     @CreationTimestamp
     @Column(name = "created_at", nullable = false, updatable = false)
     private OffsetDateTime createdAt;
 
     // Constructors
     public Profile() {
     }
 
     public Profile(UUID id, String username) {
         this.id = id;
         this.username = username;
     }
 
     // Getters and Setters
     public UUID getId() {
         return id;
     }
 
     public void setId(UUID id) {
         this.id = id;
     }
 
     public String getUsername() {
         return username;
     }
 
     public void setUsername(String username) {
         this.username = username;
     }
 
     public String getFullName() {
         return fullName;
     }
 
     public void setFullName(String fullName) {
         this.fullName = fullName;
     }
 
     public String getPhoneNumber() {
         return phoneNumber;
     }
 
     public void setPhoneNumber(String phoneNumber) {
         this.phoneNumber = phoneNumber;
     }
 
     public String getAddress() {
         return address;
     }
 
     public void setAddress(String address) {
         this.address = address;
     }
 
     public String getAvatarUrl() {
         return avatarUrl;
     }
 
     public void setAvatarUrl(String avatarUrl) {
         this.avatarUrl = avatarUrl;
     }
 
     public String getHeroBackgroundUrl() {
         return heroBackgroundUrl;
     }
 
     public void setHeroBackgroundUrl(String heroBackgroundUrl) {
         this.heroBackgroundUrl = heroBackgroundUrl;
     }
 
     public String getPageBackgroundUrl() {
         return pageBackgroundUrl;
     }
 
     public void setPageBackgroundUrl(String pageBackgroundUrl) {
         this.pageBackgroundUrl = pageBackgroundUrl;
     }
 
     public String getGeminiApiKey() {
         return geminiApiKey;
     }
 
     public void setGeminiApiKey(String geminiApiKey) {
         this.geminiApiKey = geminiApiKey;
     }
 
     public OffsetDateTime getCreatedAt() {
         return createdAt;
     }
 
 
     public void setCreatedAt(OffsetDateTime createdAt) {
         this.createdAt = createdAt;
     }
 
     @Override
     public String toString() {
         return "Profile{" +
                 "id=" + id +
                 ", username='" + username + '\'' +
                 ", fullName='" + fullName + '\'' +
                 ", createdAt=" + createdAt +
                 '}';
     }
 }
```

</details>


---

<sub>⏱️ 3m 4s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\dto\ProfileDTO.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\dto\ProfileDTO.java

<details>
<summary>106 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/dto/ProfileDTO.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/dto/ProfileDTO.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/dto/ProfileDTO.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/dto/ProfileDTO.java
@@ -1,129 +1,129 @@
 package com.cramer.dto;
 
 import java.time.OffsetDateTime;
 import java.util.UUID;
 
 /**
  * DTO for Profile entity responses.
  */
 public class ProfileDTO {
     private UUID id;
     private String username;
     private String fullName;
     private String phoneNumber;
     private String address;
     private String avatarUrl;
     private String heroBackgroundUrl;
     private String pageBackgroundUrl;
     private String geminiApiKey;
     private boolean hasGeminiApiKey;
     private OffsetDateTime createdAt;
 
     public ProfileDTO() {
     }
 
     public ProfileDTO(UUID id, String username, String fullName, String phoneNumber, String address, String avatarUrl, String heroBackgroundUrl, String pageBackgroundUrl, String geminiApiKey, boolean hasGeminiApiKey, OffsetDateTime createdAt) {
         this.id = id;
         this.username = username;
         this.fullName = fullName;
         this.phoneNumber = phoneNumber;
         this.address = address;
         this.avatarUrl = avatarUrl;
         this.heroBackgroundUrl = heroBackgroundUrl;
         this.pageBackgroundUrl = pageBackgroundUrl;
         this.geminiApiKey = geminiApiKey;
         this.hasGeminiApiKey = hasGeminiApiKey;
         this.createdAt = createdAt;
     }
 
     // Getters and Setters
     public UUID getId() {
         return id;
     }
 
     public void setId(UUID id) {
         this.id = id;
     }
 
     public String getUsername() {
         return username;
     }
 
     public void setUsername(String username) {
         this.username = username;
     }
 
     public String getFullName() {
         return fullName;
     }
 
     public void setFullName(String fullName) {
         this.fullName = fullName;
     }
 
     public String getPhoneNumber() {
         return phoneNumber;
     }
 
     public void setPhoneNumber(String phoneNumber) {
         this.phoneNumber = phoneNumber;
     }
 
     public String getAddress() {
         return address;
     }
 
     public void setAddress(String address) {
         this.address = address;
     }
 
     public String getAvatarUrl() {
         return avatarUrl;
     }
 
     public void setAvatarUrl(String avatarUrl) {
         this.avatarUrl = avatarUrl;
     }
 
     public String getHeroBackgroundUrl() {
         return heroBackgroundUrl;
     }
 
     public void setHeroBackgroundUrl(String heroBackgroundUrl) {
         this.heroBackgroundUrl = heroBackgroundUrl;
     }
 
     public String getPageBackgroundUrl() {
         return pageBackgroundUrl;
     }
 
     public void setPageBackgroundUrl(String pageBackgroundUrl) {
         this.pageBackgroundUrl = pageBackgroundUrl;
     }
 
     public OffsetDateTime getCreatedAt() {
         return createdAt;
     }
 
 
     public void setCreatedAt(OffsetDateTime createdAt) {
         this.createdAt = createdAt;
     }
 
     public String getGeminiApiKey() {
         return geminiApiKey;
     }
 
     public void setGeminiApiKey(String geminiApiKey) {
         this.geminiApiKey = geminiApiKey;
     }
 
     public boolean isHasGeminiApiKey() {
         return hasGeminiApiKey;
     }
 
     public void setHasGeminiApiKey(boolean hasGeminiApiKey) {
         this.hasGeminiApiKey = hasGeminiApiKey;
     }
 }
```

</details>


---

<sub>⏱️ 3m 6s</sub>

### 💬 Copilot

Now I understand the current structure. I need to:
1. Add a `geminiModel` field to the Profile entity and DTO
2. Update the frontend Profile.jsx to include a model selector dropdown
3. Update the GeminiGradingService to use the user's selected model

Let me also check the available models by looking at Gemini's documentation patterns and the service implementation.

---

<sub>⏱️ 3m 11s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\implement\ProfileServiceImpl.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\implement\ProfileServiceImpl.java

<details>
<summary>70 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/implement/ProfileServiceImpl.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/implement/ProfileServiceImpl.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/implement/ProfileServiceImpl.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/implement/ProfileServiceImpl.java
@@ -1,78 +1,78 @@
 package com.cramer.service.implement;
 
 import com.cramer.dto.ProfileDTO;
 import com.cramer.entity.Profile;
 import com.cramer.repository.ProfileRepository;
 import com.cramer.service.ProfileService;
 import org.springframework.stereotype.Service;
 import org.springframework.transaction.annotation.Transactional;
 
 import java.util.UUID;
 
 @Service
 public class ProfileServiceImpl implements ProfileService {
 
     private final ProfileRepository profileRepository;
 
     public ProfileServiceImpl(ProfileRepository profileRepository) {
         this.profileRepository = profileRepository;
     }
 
     @Override
     @Transactional(readOnly = true)
     public ProfileDTO getProfileById(UUID id) {
         Profile profile = profileRepository.findById(id)
                 .orElseThrow(() -> new RuntimeException("Profile not found with id: " + id));
         return convertToDto(profile);
     }
 
     @Override
     @Transactional
     public ProfileDTO updateProfile(UUID id, ProfileDTO profileDto) {
         Profile profile = profileRepository.findById(id)
                 .orElseThrow(() -> new RuntimeException("Profile not found with id: " + id));
 
         // Update fields from DTO
         profile.setFullName(profileDto.getFullName());
         profile.setPhoneNumber(profileDto.getPhoneNumber());
         profile.setAddress(profileDto.getAddress());
         
         if (profileDto.getAvatarUrl() != null) {
             profile.setAvatarUrl(profileDto.getAvatarUrl());
         }
         if (profileDto.getHeroBackgroundUrl() != null) {
             profile.setHeroBackgroundUrl(profileDto.getHeroBackgroundUrl());
         }
         if (profileDto.getPageBackgroundUrl() != null) {
             profile.setPageBackgroundUrl(profileDto.getPageBackgroundUrl());
         }
         
         // Update Gemini API key if provided (empty string clears it)
         if (profileDto.getGeminiApiKey() != null) {
             if (profileDto.getGeminiApiKey().isEmpty()) {
                 profile.setGeminiApiKey(null); // Clear the key
             } else {
                 profile.setGeminiApiKey(profileDto.getGeminiApiKey());
             }
         }
 
         Profile updatedProfile = profileRepository.save(profile);
         return convertToDto(updatedProfile);
     }
 
     private ProfileDTO convertToDto(Profile profile) {
         ProfileDTO dto = new ProfileDTO();
         dto.setId(profile.getId());
         dto.setUsername(profile.getUsername());
         dto.setFullName(profile.getFullName());
         dto.setPhoneNumber(profile.getPhoneNumber());
         dto.setAddress(profile.getAddress());
         dto.setAvatarUrl(profile.getAvatarUrl());
         dto.setHeroBackgroundUrl(profile.getHeroBackgroundUrl());
         dto.setPageBackgroundUrl(profile.getPageBackgroundUrl());
         // For security, don't return the actual API key - just indicate if it exists
         dto.setHasGeminiApiKey(profile.getGeminiApiKey() != null && !profile.getGeminiApiKey().isEmpty());
         return dto;
     }
 }
```

</details>


---

<sub>⏱️ 3m 11s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\AsyncGradingService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\AsyncGradingService.java

<details>
<summary>105 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/AsyncGradingService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/AsyncGradingService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/AsyncGradingService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/AsyncGradingService.java
@@ -1,120 +1,120 @@
 package com.cramer.service;
 
 import com.cramer.entity.Profile;
 import com.cramer.entity.Section;
 import com.cramer.entity.TestAttempt;
 import com.cramer.entity.WritingSubmission;
 import com.cramer.repository.ProfileRepository;
 import com.cramer.repository.SectionRepository;
 import com.cramer.repository.WritingSubmissionRepository;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.scheduling.annotation.Async;
 import org.springframework.stereotype.Service;
 
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.UUID;
 import java.util.stream.Collectors;
 
 /**
  * Separate service for async grading operations.
  * This ensures @Async works correctly via Spring proxy.
  */
 @Service
 public class AsyncGradingService {
 
     private static final Logger logger = LoggerFactory.getLogger(AsyncGradingService.class);
 
     private final WritingSubmissionRepository writingSubmissionRepository;
     private final SectionRepository sectionRepository;
     private final ProfileRepository profileRepository;
     private final GeminiGradingService geminiGradingService;
 
     @Autowired
     public AsyncGradingService(WritingSubmissionRepository writingSubmissionRepository,
                                SectionRepository sectionRepository,
                                ProfileRepository profileRepository,
                                GeminiGradingService geminiGradingService) {
         this.writingSubmissionRepository = writingSubmissionRepository;
         this.sectionRepository = sectionRepository;
         this.profileRepository = profileRepository;
         this.geminiGradingService = geminiGradingService;
     }
 
     /**
      * Async method to grade submissions in background.
      * IMPORTANT: This must be called from another bean (not from within this class)
      * for the @Async proxy to work.
      */
     @Async
     public void gradeSubmissionsAsync(List<WritingSubmission> submissions, TestAttempt attempt, UUID userId) {
         logger.info("🚀 Starting ASYNC grading for {} submissions (thread: {})", 
                    submissions.size(), Thread.currentThread().getName());
         
         try {
             // Get user's API key
             Profile profile = profileRepository.findById(userId)
                 .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
             
             String apiKey = profile.getGeminiApiKey();
             if (apiKey == null || apiKey.trim().isEmpty()) {
                 logger.warn("❌ No Gemini API key found for user {}", userId);
                 for (WritingSubmission submission : submissions) {
                     submission.setGradingStatus("FAILED");
                     Map<String, Object> errorFeedback = new HashMap<>();
                     errorFeedback.put("error", "Vui lòng thêm Gemini API key trong phần Cài đặt Hồ sơ để sử dụng tính năng chấm điểm AI.");
                     submission.setAiFeedback(errorFeedback);
                     writingSubmissionRepository.save(submission);
                 }
                 return;
             }
             
             logger.info("✅ Found API key for user, starting grading...");
             
             // Get task prompts
             List<Section> sections = sectionRepository.findByExamSourceAndTestNumberAndSkill(
                 attempt.getExamSource(),
                 Integer.parseInt(attempt.getTestNumber()),
                 "writing"
             );
             
             Map<Integer, Section> sectionMap = sections.stream()
                 .collect(Collectors.toMap(Section::getPartNumber, s -> s));
             
             // Grade each submission
             for (WritingSubmission submission : submissions) {
                 try {
                     logger.info("📝 Grading Task {} for attempt {}...", 
                                submission.getTaskNumber(), attempt.getId());
                     
                     Section section = sectionMap.get(submission.getTaskNumber());
                     String taskPrompt = section != null ? section.getPassageText() : "";
                     String imageUrl = section != null ? section.getDisplayContentUrl() : null;
                     
                     geminiGradingService.gradeSubmission(submission, taskPrompt, imageUrl, apiKey);
                     writingSubmissionRepository.save(submission);
                     
                     logger.info("✅ Graded submission {} with band {}", 
                                submission.getId(), submission.getOverallBand());
                     
                 } catch (Exception e) {
                     logger.error("❌ Failed to grade submission {}: {}", submission.getId(), e.getMessage());
                     submission.setGradingStatus("FAILED");
                     Map<String, Object> errorFeedback = new HashMap<>();
                     errorFeedback.put("error", "Grading failed: " + e.getMessage());
                     submission.setAiFeedback(errorFeedback);
                     writingSubmissionRepository.save(submission);
                 }
             }
             
             logger.info("🎉 Completed async grading for attempt {}", attempt.getId());
             
         } catch (Exception e) {
             logger.error("💥 Error in async grading: {}", e.getMessage(), e);
         }
     }
 }
```

</details>


---

<sub>⏱️ 3m 13s</sub>

### 💬 Copilot

Now I have a complete understanding. I need to:

1. Add `geminiModel` field to Profile entity and ProfileDTO
2. Update ProfileServiceImpl to handle the model field
3. Update GeminiGradingService to accept model from profile
4. Update AsyncGradingService to pass the model
5. Update frontend Profile.jsx to include a model selector

Let me make all these changes:

---

<sub>⏱️ 3m 37s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\entity\Profile.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\entity\Profile.java

<details>
<summary>13 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/entity/Profile.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/entity/Profile.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/entity/Profile.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/entity/Profile.java
@@ -42,6 +42,9 @@
     @Column(name = "gemini_api_key")
     private String geminiApiKey;
 
+    @Column(name = "gemini_model")
+    private String geminiModel;
+
     @CreationTimestamp
     @Column(name = "created_at", nullable = false, updatable = false)
     private OffsetDateTime createdAt;
```

</details>


---

<sub>⏱️ 3m 37s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\entity\Profile.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\entity\Profile.java

<details>
<summary>18 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/entity/Profile.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/entity/Profile.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/entity/Profile.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/entity/Profile.java
@@ -131,6 +131,14 @@
         this.geminiApiKey = geminiApiKey;
     }
 
+    public String getGeminiModel() {
+        return geminiModel;
+    }
+
+    public void setGeminiModel(String geminiModel) {
+        this.geminiModel = geminiModel;
+    }
+
     public OffsetDateTime getCreatedAt() {
         return createdAt;
     }
```

</details>


---

<sub>⏱️ 3m 26s</sub>

### ❌ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\dto\ProfileDTO.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\dto\ProfileDTO.java

```
Operation aborted by user
```


---

<sub>⏱️ 3m 26s</sub>

### ❌ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\dto\ProfileDTO.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\dto\ProfileDTO.java

```
Operation aborted by user
```


---

<sub>⏱️ 5m 17s</sub>

### ℹ️ Info

Operation cancelled by user

---

<sub>⏱️ 5m 22s</sub>

### 👤 User

Why are you taking so long?

---

<sub>⏱️ 5m 26s</sub>

### 💬 Copilot

I'll speed up - making all remaining edits in parallel:

---

<sub>⏱️ 5m 40s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\dto\ProfileDTO.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\dto\ProfileDTO.java

<details>
<summary>11 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/dto/ProfileDTO.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/dto/ProfileDTO.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/dto/ProfileDTO.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/dto/ProfileDTO.java
@@ -17,6 +17,7 @@
     private String pageBackgroundUrl;
     private String geminiApiKey;
     private boolean hasGeminiApiKey;
+    private String geminiModel;
     private OffsetDateTime createdAt;
 
     public ProfileDTO() {
```

</details>


---

<sub>⏱️ 5m 41s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\dto\ProfileDTO.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\dto\ProfileDTO.java

<details>
<summary>17 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/dto/ProfileDTO.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/dto/ProfileDTO.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/dto/ProfileDTO.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/dto/ProfileDTO.java
@@ -126,5 +126,13 @@
     public void setHasGeminiApiKey(boolean hasGeminiApiKey) {
         this.hasGeminiApiKey = hasGeminiApiKey;
     }
+
+    public String getGeminiModel() {
+        return geminiModel;
+    }
+
+    public void setGeminiModel(String geminiModel) {
+        this.geminiModel = geminiModel;
+    }
 }
```

</details>


---

<sub>⏱️ 5m 40s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\implement\ProfileServiceImpl.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\implement\ProfileServiceImpl.java

<details>
<summary>15 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/implement/ProfileServiceImpl.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/implement/ProfileServiceImpl.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/implement/ProfileServiceImpl.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/implement/ProfileServiceImpl.java
@@ -55,6 +55,11 @@
                 profile.setGeminiApiKey(profileDto.getGeminiApiKey());
             }
         }
+        
+        // Update Gemini model if provided
+        if (profileDto.getGeminiModel() != null) {
+            profile.setGeminiModel(profileDto.getGeminiModel());
+        }
 
         Profile updatedProfile = profileRepository.save(profile);
         return convertToDto(updatedProfile);
```

</details>


---

<sub>⏱️ 5m 41s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\implement\ProfileServiceImpl.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\implement\ProfileServiceImpl.java

<details>
<summary>12 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/implement/ProfileServiceImpl.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/implement/ProfileServiceImpl.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/implement/ProfileServiceImpl.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/implement/ProfileServiceImpl.java
@@ -77,6 +77,7 @@
         dto.setPageBackgroundUrl(profile.getPageBackgroundUrl());
         // For security, don't return the actual API key - just indicate if it exists
         dto.setHasGeminiApiKey(profile.getGeminiApiKey() != null && !profile.getGeminiApiKey().isEmpty());
+        dto.setGeminiModel(profile.getGeminiModel());
         return dto;
     }
 }
```

</details>


---

<sub>⏱️ 6m 13s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java

<details>
<summary>21 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
@@ -31,8 +31,16 @@
     
     // Using Gemini 2.5 Flash for higher rate limits (10 RPM vs 2 RPM for Pro)
     // Trade-off: Slightly less nuanced but still accurate for IELTS grading
-    private static final String GEMINI_MODEL = "gemini-2.5-flash";
-    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent";
+    private static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";
+    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
+    
+    // Available models for user selection
+    public static final String[] AVAILABLE_MODELS = {
+        "gemini-2.5-flash",      // Fast, high rate limits (10 RPM free)
+        "gemini-2.5-flash-lite", // Fastest, highest rate limits
+        "gemini-2.5-pro",        // Most capable, lower rate limits (2 RPM free)
+        "gemma-3-27b-it"         // Open model, good balance
+    };
     
     // Minimum word thresholds for IELTS Writing
     private static final int TASK_1_MIN_WORDS = 150;
```

</details>


---

<sub>⏱️ 6m 13s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java

<details>
<summary>26 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
@@ -62,10 +62,11 @@
      * @param taskPrompt The original task prompt/question
      * @param taskImageUrl Optional image URL for Task 1 (charts, diagrams, maps)
      * @param apiKey User's Gemini API key
+     * @param model User's selected Gemini model (optional, defaults to gemini-2.5-flash)
      * @return Updated submission with grading results
      */
     public WritingSubmission gradeSubmission(WritingSubmission submission, String taskPrompt, 
-                                              String taskImageUrl, String apiKey) {
+                                              String taskImageUrl, String apiKey, String model) {
         if (apiKey == null || apiKey.trim().isEmpty()) {
             logger.error("No Gemini API key provided for grading");
             submission.setGradingStatus("FAILED");
@@ -74,6 +75,9 @@
             submission.setAiFeedback(errorFeedback);
             return submission;
         }
+        
+        // Use default model if not specified
+        String selectedModel = (model != null && !model.trim().isEmpty()) ? model : DEFAULT_GEMINI_MODEL;
 
         try {
             // Check for empty or minimal essay - return band 0-1 without calling API
```

</details>


---

<sub>⏱️ 6m 13s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java

<details>
<summary>13 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
@@ -103,7 +103,8 @@
                 essayText,
                 wordCount,
                 taskImageUrl,
-                apiKey
+                apiKey,
+                selectedModel
             );
             
             // Parse and apply results
```

</details>


---

<sub>⏱️ 6m 13s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java

<details>
<summary>14 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
@@ -550,8 +550,8 @@
      * Call Gemini API with multimodal support (text + image for Task 1).
      */
     private String callGeminiApiWithImage(Integer taskNumber, String taskPrompt, String essay, 
-                                           int wordCount, String imageUrl, String apiKey) {
-        String url = GEMINI_API_URL + "?key=" + apiKey;
+                                           int wordCount, String imageUrl, String apiKey, String model) {
+        String url = GEMINI_API_BASE_URL + model + ":generateContent?key=" + apiKey;
         
         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.APPLICATION_JSON);
```

</details>


---

<sub>⏱️ 6m 13s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java

<details>
<summary>29 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
@@ -818,10 +818,19 @@
      * Validate API key by making a simple test request.
      */
     public boolean validateApiKey(String apiKey) {
+        return validateApiKey(apiKey, DEFAULT_GEMINI_MODEL);
+    }
+    
+    /**
+     * Validate API key by making a simple test request with specified model.
+     */
+    public boolean validateApiKey(String apiKey, String model) {
         if (apiKey == null || apiKey.trim().isEmpty()) {
             return false;
         }
         
+        String selectedModel = (model != null && !model.trim().isEmpty()) ? model : DEFAULT_GEMINI_MODEL;
+        
         try {
             String url = GEMINI_API_URL + "?key=" + apiKey;
             
@@ -823,7 +832,7 @@
         }
         
         try {
-            String url = GEMINI_API_URL + "?key=" + apiKey;
+            String url = GEMINI_API_BASE_URL + selectedModel + ":generateContent?key=" + apiKey;
             
             HttpHeaders headers = new HttpHeaders();
             headers.setContentType(MediaType.APPLICATION_JSON);
```

</details>


---

<sub>⏱️ 6m 19s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\AsyncGradingService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\AsyncGradingService.java

<details>
<summary>11 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/AsyncGradingService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/AsyncGradingService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/AsyncGradingService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/AsyncGradingService.java
@@ -60,6 +60,7 @@
                 .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
             
             String apiKey = profile.getGeminiApiKey();
+            String model = profile.getGeminiModel();
             if (apiKey == null || apiKey.trim().isEmpty()) {
                 logger.warn("❌ No Gemini API key found for user {}", userId);
                 for (WritingSubmission submission : submissions) {
```

</details>


---

<sub>⏱️ 6m 19s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\AsyncGradingService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\AsyncGradingService.java

<details>
<summary>11 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/AsyncGradingService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/AsyncGradingService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/AsyncGradingService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/AsyncGradingService.java
@@ -95,7 +95,7 @@
                     String taskPrompt = section != null ? section.getPassageText() : "";
                     String imageUrl = section != null ? section.getDisplayContentUrl() : null;
                     
-                    geminiGradingService.gradeSubmission(submission, taskPrompt, imageUrl, apiKey);
+                    geminiGradingService.gradeSubmission(submission, taskPrompt, imageUrl, apiKey, model);
                     writingSubmissionRepository.save(submission);
                     
                     logger.info("✅ Graded submission {} with band {}",
```

</details>


---

<sub>⏱️ 6m 21s</sub>

### 💬 Copilot

Now I'll update the frontend Profile.jsx to add the model selector:

---

<sub>⏱️ 6m 24s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx (lines 55-75)

<details>
<summary>24 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -55,21 +55,21 @@
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
```

</details>


---

<sub>⏱️ 6m 24s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx (lines 990-1050)

<details>
<summary>63 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -990,61 +990,61 @@
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
```

</details>


---

<sub>⏱️ 6m 36s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx

<details>
<summary>23 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -64,11 +64,20 @@
 
   // AI Settings states
   const [geminiApiKey, setGeminiApiKey] = useState('');
+  const [geminiModel, setGeminiModel] = useState('gemini-2.5-flash');
   const [showApiKey, setShowApiKey] = useState(false);
   const [isValidatingApiKey, setIsValidatingApiKey] = useState(false);
   const [apiKeyStatus, setApiKeyStatus] = useState(null); // null, 'valid', 'invalid'
   const [isSavingApiKey, setIsSavingApiKey] = useState(false);
   const [isApiKeyModified, setIsApiKeyModified] = useState(false); // Track if user has modified the key
+
+  // Available Gemini models
+  const GEMINI_MODELS = [
+    { value: 'gemini-2.5-flash', label: 'Gemini 2.5 Flash', description: 'Nhanh, rate limit cao (10 RPM miễn phí)' },
+    { value: 'gemini-2.5-flash-lite', label: 'Gemini 2.5 Flash Lite', description: 'Nhanh nhất, rate limit cao nhất' },
+    { value: 'gemini-2.5-pro', label: 'Gemini 2.5 Pro', description: 'Chính xác nhất, rate limit thấp (5 RPM miễn phí)' },
+    { value: 'gemma-3-27b-it', label: 'Gemma 3 27B', description: 'Model mở, cân bằng tốt' }
+  ];
 
   // Security states (mock data for now - will be replaced with API calls)
   const [twoFactorEnabled, setTwoFactorEnabled] = useState(false);
```

</details>


---

<sub>⏱️ 6m 39s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx (lines 100-130)

<details>
<summary>34 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -100,31 +100,31 @@
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
```

</details>


---

<sub>⏱️ 6m 45s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx

<details>
<summary>15 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -123,6 +123,10 @@
           setGeminiApiKey('••••••••••••••••••••••••••••••••••••••••'); // Masked placeholder
           setApiKeyStatus('valid'); // Assume valid if saved
         }
+        // Set saved model or default
+        if (response.data.geminiModel) {
+          setGeminiModel(response.data.geminiModel);
+        }
         setError(null);
       } catch (err) {
         setError('Không thể tải thông tin cá nhân.');
```

</details>


---

<sub>⏱️ 6m 48s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx (lines 335-375)

<details>
<summary>39 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -335,41 +335,41 @@
 
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
```

</details>


---

<sub>⏱️ 6m 51s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx (lines 425-475)

<details>
<summary>49 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -425,51 +425,51 @@
 
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
```

</details>


---

<sub>⏱️ 6m 53s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx (lines 475-520)

<details>
<summary>45 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -475,46 +475,46 @@
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
```

</details>


---

<sub>⏱️ 7m 2s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx

<details>
<summary>12 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -477,7 +477,7 @@
     setIsSavingApiKey(true);
 
     try {
-      await profileApi.update(user.id, { geminiApiKey: geminiApiKey });
+      await profileApi.update(user.id, { geminiApiKey: geminiApiKey, geminiModel: geminiModel });
       showSuccessToast('Đã lưu API key thành công!');
       setApiKeyStatus('valid');
       setIsApiKeyModified(false); // Reset modified state after saving
```

</details>


---

<sub>⏱️ 7m 2s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx

<details>
<summary>23 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -487,7 +487,18 @@
     } finally {
       setIsSavingApiKey(false);
     }
-  }, [geminiApiKey, user?.id, isApiKeyModified]);
+  }, [geminiApiKey, geminiModel, user?.id, isApiKeyModified]);
+
+  const handleSaveModel = useCallback(async (newModel) => {
+    try {
+      await profileApi.update(user.id, { geminiModel: newModel });
+      setGeminiModel(newModel);
+      showSuccessToast('Đã cập nhật model AI!');
+    } catch (err) {
+      console.error('Error saving model:', err);
+      showErrorToast('Không thể cập nhật model');
+    }
+  }, [user?.id]);
 
   const handleDeleteApiKey = useCallback(async () => {
     setIsSavingApiKey(true);
```

</details>


---

<sub>⏱️ 7m 3s</sub>

### 💬 Copilot

Now I'll add the model selector UI in the AI Settings section:

---

<sub>⏱️ 7m 5s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx (lines 1075-1130)

<details>
<summary>58 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -1075,56 +1075,56 @@
 
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
```

</details>


---

<sub>⏱️ 7m 24s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx

<details>
<summary>44 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -1100,6 +1100,40 @@
                   </div>
                 </div>
 
+                {/* Model Selection Section */}
+                <div className="profile-card">
+                  <div className="profile-card__header">
+                    <h3 className="profile-card__title">
+                      <FiSliders />
+                      Chọn Model AI
+                    </h3>
+                  </div>
+
+                  <div className="ai-settings-info">
+                    <p className="ai-settings-description">
+                      Chọn model Gemini phù hợp với nhu cầu của bạn. Mỗi model có đặc điểm riêng về tốc độ, độ chính xác và rate limit.
+                    </p>
+                  </div>
+
+                  <div className="model-selector">
+                    {GEMINI_MODELS.map((model) => (
+                      <div
+                        key={model.value}
+                        className={`model-option ${geminiModel === model.value ? 'selected' : ''}`}
+                        onClick={() => handleSaveModel(model.value)}
+                      >
+                        <div className="model-option__radio">
+                          {geminiModel === model.value ? <FiCheck /> : null}
+                        </div>
+                        <div className="model-option__info">
+                          <h4 className="model-option__name">{model.label}</h4>
+                          <p className="model-option__description">{model.description}</p>
+                        </div>
+                      </div>
+                    ))}
+                  </div>
+                </div>
+
                 {/* AI Usage Info */}
                 <div className="profile-card">
                   <div className="profile-card__header">
```

</details>


---

<sub>⏱️ 7m 24s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx

<details>
<summary>15 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -1154,8 +1154,8 @@
                         và không được chia sẻ với bất kỳ bên thứ ba nào.
                       </li>
                       <li>
-                        <strong>Model:</strong> Hệ thống sử dụng Gemini 2.5 Pro (model: `gemini-2.5-pro`) cho việc chấm điểm,
-                        đảm bảo tốc độ và độ chính xác cao.
+                        <strong>Model:</strong> Bạn có thể chọn model phù hợp ở phần trên. Gemini 2.5 Pro chính xác nhất
+                        nhưng có rate limit thấp hơn (5 RPM miễn phí).
                       </li>
                       <li>
                         <strong>Giới hạn:</strong> Có thể có giới hạn số lần gọi API miễn phí mỗi ngày.
```

</details>


---

<sub>⏱️ 7m 25s</sub>

### 💬 Copilot

Now let's add CSS for the model selector:

---

<sub>⏱️ 7m 27s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\css\ProfilePage.css.**

E:\IT and Computer Knowledges\Cramer\frontend\src\css\ProfilePage.css (lines 1-50)

<details>
<summary>49 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css b/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css
@@ -1,50 +1,50 @@
 /* ===================================
    PROFILE PAGE STYLES - Cramer
    
    Profile-specific styles.
    Imports shared SidebarLayout.css for common patterns.
    Uses sl- CSS variables from SidebarLayout.css
    =================================== */
 
 /* === Base Page Styles (Profile-specific overrides) === */
 .profile-page {
   background: var(--sl-page-bg);
   min-height: 100vh;
   position: relative;
   padding: 2rem 0;
   font-family: 'Be Vietnam Pro', sans-serif;
   color: var(--sl-text);
 }
 
 .profile-page__overlay {
   position: absolute;
   top: 0;
   left: 0;
   right: 0;
   bottom: 0;
   background: var(--sl-overlay-bg);
   z-index: 0;
   pointer-events: none;
 }
 
 .profile-page .container {
   max-width: 1280px;
   margin: 0 auto;
   padding: 0 2rem;
   position: relative;
   z-index: 1;
 }
 
 /* === Main Layout === */
 .profile-layout {
   display: flex;
   gap: 2rem;
   align-items: flex-start;
 }
 
 /* === Sidebar === */
 .profile-sidebar {
   width: 300px;
   min-width: 300px;
   position: sticky;
   top: 100px;
```

</details>


---

<sub>⏱️ 7m 31s</sub>

### ✅ `grep`

"api-key-input" (E:\IT and Computer Knowledges\Cramer\frontend\src\css\ProfilePage.css)

```
1054:.api-key-input-group {
1058:.api-key-input-wrapper {
1064:.api-key-input {
```


---

<sub>⏱️ 7m 34s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\css\ProfilePage.css.**

E:\IT and Computer Knowledges\Cramer\frontend\src\css\ProfilePage.css (lines 1100-1150)

<details>
<summary>48 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css b/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css
@@ -1100,51 +1100,51 @@
   font-size: 0.82rem;
   font-weight: 500;
 }
 
 .api-key-status.valid {
   background: rgba(16, 185, 129, 0.1);
   color: var(--success-color);
   border: 1px solid rgba(16, 185, 129, 0.2);
 }
 
 .api-key-status.invalid {
   background: rgba(239, 68, 68, 0.1);
   color: var(--danger-color);
   border: 1px solid rgba(239, 68, 68, 0.2);
 }
 
 .api-key-actions {
   display: flex;
   flex-wrap: wrap;
   gap: 0.6rem;
 }
 
 /* AI Usage Notes */
 .ai-usage-notes {
   padding: 0;
 }
 
 .ai-usage-notes ul {
   list-style: none;
   padding: 0;
   margin: 0;
 }
 
 .ai-usage-notes li {
   position: relative;
   padding: 0.875rem;
   padding-left: 1.25rem;
   margin-bottom: 0.4rem;
   background: rgba(124, 58, 237, 0.03);
   border-radius: 8px;
   border-left: 3px solid var(--primary-accent);
   font-size: 0.85rem;
   line-height: 1.55;
   color: var(--text-dark);
 }
 
 .ai-usage-notes li:last-child {
   margin-bottom: 0;
 }
 
 .ai-usage-notes li strong {
```

</details>


---

<sub>⏱️ 7m 36s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\css\ProfilePage.css.**

E:\IT and Computer Knowledges\Cramer\frontend\src\css\ProfilePage.css (lines 1115-1125)

<details>
<summary>14 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css b/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css
@@ -1115,11 +1115,11 @@
 
 .api-key-actions {
   display: flex;
   flex-wrap: wrap;
   gap: 0.6rem;
 }
 
 /* AI Usage Notes */
 .ai-usage-notes {
   padding: 0;
 }
```

</details>


---

<sub>⏱️ 7m 47s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\frontend\src\css\ProfilePage.css.**

E:\IT and Computer Knowledges\Cramer\frontend\src\css\ProfilePage.css

<details>
<summary>81 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css b/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/css/ProfilePage.css
@@ -1119,6 +1119,77 @@
   gap: 0.6rem;
 }
 
+/* Model Selector */
+.model-selector {
+  display: flex;
+  flex-direction: column;
+  gap: 0.75rem;
+}
+
+.model-option {
+  display: flex;
+  align-items: flex-start;
+  gap: 0.875rem;
+  padding: 1rem 1.25rem;
+  background: rgba(124, 58, 237, 0.03);
+  border: 2px solid transparent;
+  border-radius: 12px;
+  cursor: pointer;
+  transition: all 0.2s ease;
+}
+
+.model-option:hover {
+  background: rgba(124, 58, 237, 0.06);
+  border-color: rgba(124, 58, 237, 0.15);
+}
+
+.model-option.selected {
+  background: rgba(124, 58, 237, 0.08);
+  border-color: var(--primary-accent);
+}
+
+.model-option__radio {
+  width: 20px;
+  height: 20px;
+  min-width: 20px;
+  border-radius: 50%;
+  border: 2px solid var(--border-color);
+  display: flex;
+  align-items: center;
+  justify-content: center;
+  margin-top: 2px;
+  transition: all 0.2s ease;
+}
+
+.model-option.selected .model-option__radio {
+  background: var(--primary-accent);
+  border-color: var(--primary-accent);
+  color: white;
+}
+
+.model-option__radio svg {
+  width: 12px;
+  height: 12px;
+}
+
+.model-option__info {
+  flex: 1;
+}
+
+.model-option__name {
+  margin: 0 0 0.25rem 0;
+  font-size: 0.95rem;
+  font-weight: 600;
+  color: var(--text-dark);
+}
+
+.model-option__description {
+  margin: 0;
+  font-size: 0.82rem;
+  color: var(--text-muted);
+  line-height: 1.4;
+}
+
 /* AI Usage Notes */
 .ai-usage-notes {
   padding: 0;
```

</details>


---

<sub>⏱️ 7m 50s</sub>

### 💬 Copilot

Now let's create the database migration for the new column:

---

<sub>⏱️ 7m 51s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\docs\backend\migrations.**

E:\IT and Computer Knowledges\Cramer\docs\backend\migrations

<details>
<summary>7 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/docs/backend/migrations b/E:/IT and Computer Knowledges/Cramer/docs/backend/migrations
create file mode 100644
index 0000000..0000000
--- a/dev/null
+++ b/E:/IT and Computer Knowledges/Cramer/docs/backend/migrations
@@ -1,0 +1,1 @@
 001_writing_feature.sql
```

</details>


---

<sub>⏱️ 7m 57s</sub>

### ✅ `create`

**create a new file at E:\IT and Computer Knowledges\Cramer\docs\backend\migrations\002_add_gemini_model.sql.**

E:\IT and Computer Knowledges\Cramer\docs\backend\migrations\002_add_gemini_model.sql

<details>
<summary>17 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/docs/backend/migrations/002_add_gemini_model.sql b/E:/IT and Computer Knowledges/Cramer/docs/backend/migrations/002_add_gemini_model.sql
create file mode 100644
index 0000000..0000000
--- a/dev/null
+++ b/E:/IT and Computer Knowledges/Cramer/docs/backend/migrations/002_add_gemini_model.sql
@@ -1,0 +1,11 @@
+-- Migration: Add gemini_model column to profiles table
+-- Date: 2025-12-07
+-- Description: Allow users to select their preferred Gemini model for AI grading
+
+-- Add gemini_model column to profiles table
+ALTER TABLE public.profiles 
+ADD COLUMN IF NOT EXISTS gemini_model VARCHAR(50) DEFAULT 'gemini-2.5-flash';
+
+-- Add comment for documentation
+COMMENT ON COLUMN public.profiles.gemini_model IS 'User-selected Gemini model for AI grading (gemini-2.5-flash, gemini-2.5-flash-lite, gemini-2.5-pro, gemma-3-27b-it)';
+
```

</details>


---

<sub>⏱️ 7m 59s</sub>

### 💬 Copilot

Let me verify the backend builds correctly:

---

<sub>⏱️ 8m 15s</sub>

### ❌ `powershell`

**Build backend to verify changes**

<details>
<summary>Arguments</summary>

```json
{
  "command": "cd \"E:\\IT and Computer Knowledges\\Cramer\\backend\" && .\\mvnw.cmd compile -q 2>&1 | Select-Object -Last 20",
  "description": "Build backend to verify changes",
  "mode": "sync",
  "initial_wait": 60
}
```

</details>

```
The tool call required approval from the user, but the user rejected it or an earlier tool call.
```


---

<sub>⏱️ 8m 23s</sub>

### ℹ️ Info

Model changed to: claude-haiku-4.5. The new model will be used for the next conversation.

---

<sub>⏱️ 8m 35s</sub>

### 👤 User

Provide me the command for rebuilding the backend.

---

<sub>⏱️ 8m 38s</sub>

### 💬 Copilot

Here's the command to rebuild the backend:

```powershell
cd backend && .\mvnw.cmd clean package -DskipTests
```

Or if you prefer to run it directly:

```powershell
cd backend && .\mvnw.cmd clean compile
```

After building, you can start the app with:

```powershell
.\run-app.ps1
```

or 

```cmd
.\run-local.cmd
```

The first command does a full clean build and packages it as a JAR. The second command just compiles without packaging. Choose based on whether you want to test the full packaged version.

---

<sub>⏱️ 11m 18s</sub>

### 👤 User

2025-12-08T00:41:40.120+07:00 ERROR 17364 --- [         task-1] c.cramer.service.GeminiGradingService    : Gemini API call failed: 400 Bad Request: "{\<EOL\>  "error": {\<EOL\>    "code": 400,\<EOL\>    "message": "Developer instruction is not enabled for models/gemma-3-27b-it",\<EOL\>    "status": "INVALID_ARGUMENT"\<EOL\>  }\<EOL\>}\<EOL\>"
2025-12-08T00:41:40.120+07:00 ERROR 17364 --- [         task-1] c.cramer.service.GeminiGradingService    : Failed to grade submission 63: Failed to call Gemini API: 400 Bad Request: "{\<EOL\>  "error": {\<EOL\>    "code": 400,\<EOL\>    "message": "Developer instruction is not enabled for models/gemma-3-27b-it",\<EOL\>    "status": "INVALID_ARGUMENT"\<EOL\>  }\<EOL\>}\<EOL\>"

java.lang.RuntimeException: Failed to call Gemini API: 400 Bad Request: "{\<EOL\>  "error": {\<EOL\>    "code": 400,\<EOL\>    "message": "Developer instruction is not enabled for models/gemma-3-27b-it",\<EOL\>    "status": "INVALID_ARGUMENT"\<EOL\>  }\<EOL\>}\<EOL\>"

---

<sub>⏱️ 11m 22s</sub>

### 💬 Copilot

The error shows that `gemma-3-27b-it` doesn't support system instructions (developer instructions). We need to remove it from the available models. Let me also check which models are actually available and supported:

---

<sub>⏱️ 11m 28s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\GeminiGradingService.java

<details>
<summary>13 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/GeminiGradingService.java
@@ -38,8 +38,7 @@
     public static final String[] AVAILABLE_MODELS = {
         "gemini-2.5-flash",      // Fast, high rate limits (10 RPM free)
         "gemini-2.5-flash-lite", // Fastest, highest rate limits
-        "gemini-2.5-pro",        // Most capable, lower rate limits (2 RPM free)
-        "gemma-3-27b-it"         // Open model, good balance
+        "gemini-2.5-pro"         // Most capable, lower rate limits (5 RPM free)
     };
     
     // Minimum word thresholds for IELTS Writing
```

</details>


---

<sub>⏱️ 11m 28s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\Profile.jsx

<details>
<summary>13 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/Profile.jsx
@@ -75,8 +75,7 @@
   const GEMINI_MODELS = [
     { value: 'gemini-2.5-flash', label: 'Gemini 2.5 Flash', description: 'Nhanh, rate limit cao (10 RPM miễn phí)' },
     { value: 'gemini-2.5-flash-lite', label: 'Gemini 2.5 Flash Lite', description: 'Nhanh nhất, rate limit cao nhất' },
-    { value: 'gemini-2.5-pro', label: 'Gemini 2.5 Pro', description: 'Chính xác nhất, rate limit thấp (5 RPM miễn phí)' },
-    { value: 'gemma-3-27b-it', label: 'Gemma 3 27B', description: 'Model mở, cân bằng tốt' }
+    { value: 'gemini-2.5-pro', label: 'Gemini 2.5 Pro', description: 'Chính xác nhất, rate limit thấp (5 RPM miễn phí)' }
   ];
 
   // Security states (mock data for now - will be replaced with API calls)
```

</details>


---

<sub>⏱️ 11m 29s</sub>

### 💬 Copilot

Done! I've removed `gemma-3-27b-it` from the available models since it doesn't support system instructions (developer instructions) which are required for our IELTS grading prompts.

**Available models now:**
- **Gemini 2.5 Flash** (default) - Fast, 10 RPM free tier
- **Gemini 2.5 Flash Lite** - Fastest, highest rate limits  
- **Gemini 2.5 Pro** - Most accurate, 5 RPM free tier

All three support system instructions and should work correctly. Gemini 2.5 Pro is the most capable but has lower rate limits, while Flash and Flash Lite are faster with higher rate limits.

---

<sub>⏱️ 14m 10s</sub>

### ℹ️ Info

Model changed to: claude-opus-4.5. The new model will be used for the next conversation.

---

<sub>⏱️ 15m 54s</sub>

### 👤 User

The current "Chấm lại" function is not working properly (it doesn't have a loading screen), and the loading screen waiting for the result of AI grading for writing is not stable. 

I think the problem is much deeper. Therefore, please dive deep into files and tell me what the issues are and fix them.

---

<sub>⏱️ 16m 2s</sub>

### ✅ `glob`

"**/*Writing*"

<details>
<summary>12 matches</summary>

```
.\frontend\src\css\WritingTestPage.css
.\frontend\src\css\WritingResultPage.css
.\frontend\src\pages\WritingTestPage.jsx
.\frontend\src\pages\WritingResultPage.jsx
.\backend\src\main\java\com\cramer\repository\WritingSubmissionRepository.java
.\backend\src\main\java\com\cramer\dto\WritingSubmitDTO.java
.\backend\src\main\java\com\cramer\dto\WritingSubmissionDTO.java
.\backend\src\main\java\com\cramer\dto\WritingReviewDTO.java
.\backend\src\main\java\com\cramer\controller\WritingController.java
.\backend\src\main\java\com\cramer\service\WritingSubmissionService.java
.\backend\src\main\java\com\cramer\entity\WritingSubmission.java
.\docs\marking_criteria\IELTS_Writing_Band_descriptors.pdf
```

</details>


---

<sub>⏱️ 16m 2s</sub>

### ✅ `grep`

"Chấm lại|regrade|re-grade"

<details>
<summary>8 matches</summary>

```
.\frontend\src\api\backendApi.js
.\backend\src\main\java\com\cramer\controller\WritingController.java
.\backend\src\main\java\com\cramer\service\WritingSubmissionService.java
.\frontend\src\css\WritingResultPage.css
.\frontend\src\css\TestReviewPage.css
.\frontend\src\css\ReviewHeader.css
.\frontend\src\pages\WritingResultPage.jsx
.\frontend\src\pages\TestReviewPage.jsx
```

</details>


---

<sub>⏱️ 16m 7s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx

<details>
<summary>918 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
@@ -1,988 +1,988 @@
 import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
 import { useParams, useNavigate, Link } from 'react-router-dom';
 import { PanelGroup, Panel, PanelResizeHandle } from 'react-resizable-panels';
 import { writingApi } from '../api/backendApi';
 import { 
     FiArrowLeft, FiRefreshCw, FiChevronDown, FiChevronRight,
     FiFileText, FiEdit3, FiBarChart2, FiCheckCircle, FiXCircle,
     FiAlertCircle, FiZap, FiTrendingUp, FiAward, FiBook,
     FiTarget, FiThumbsUp, FiAlertTriangle, FiEdit, FiInfo, FiRotateCw
 } from 'react-icons/fi';
 
 import '../css/WritingResultPage.css';
 
 // Error type colors with Vietnamese labels - consistent across essay highlights and analysis sections
 const ERROR_TYPE_COLORS = {
     grammar: { bg: '#fef2f2', border: '#ef4444', text: '#dc2626', label: 'Ngữ pháp' },
     spelling: { bg: '#fff7ed', border: '#f97316', text: '#ea580c', label: 'Chính tả' },
     vocabulary: { bg: '#faf5ff', border: '#8b5cf6', text: '#7c3aed', label: 'Từ vựng' },
     punctuation: { bg: '#ecfeff', border: '#06b6d4', text: '#0891b2', label: 'Dấu câu' },
     coherence: { bg: '#f0fdf4', border: '#10b981', text: '#059669', label: 'Mạch lạc' },
     style: { bg: '#fefce8', border: '#eab308', text: '#ca8a04', label: 'Văn phong' },
     // NEW: Vocabulary highlight types (for good/error vocabulary)
     vocabulary_good: { bg: '#dcfce7', border: '#22c55e', text: '#15803d', label: 'Từ vựng tốt' },
     vocabulary_error: { bg: '#fee2e2', border: '#f87171', text: '#dc2626', label: 'Từ vựng sai' },
 };
 
 const getErrorStyle = (errorType) => {
     return ERROR_TYPE_COLORS[errorType?.toLowerCase()] || { bg: '#f3f4f6', border: '#6b7280', text: '#4b5563' };
 };
 
 const WritingResultPage = () => {
     const { attemptId } = useParams();
     const navigate = useNavigate();
 
     // Core state
     const [review, setReview] = useState(null);
     const [loading, setLoading] = useState(true);
     const [gradingStatus, setGradingStatus] = useState('PENDING');
     const [error, setError] = useState(null);
     const [activeTask, setActiveTask] = useState(1);
     
     // UI state
     const [expandedScores, setExpandedScores] = useState({});
     const [scoresBarCollapsed, setScoresBarCollapsed] = useState(false);
     const [expandedSections, setExpandedSections] = useState({
         corrections: true,
         paragraphs: false,
         sampleBandPlus: false,
         sampleBand9: false,
         wordAnalysis: false,
     });
     const [selectedItemId, setSelectedItemId] = useState(null);
     const [isRegrading, setIsRegrading] = useState(false);
     
     // Refs for scroll-to functionality
     const analysisColumnRef = useRef(null);
     const itemRefs = useRef({});
 
     // Poll for grading status with improved reliability
     useEffect(() => {
         // Don't poll if we already have review data or there's an error
         if (review || error) return;
         
         let pollInterval;
         let isMounted = true;
         let consecutiveErrors = 0;
         let currentPollCount = 0;
         const MAX_CONSECUTIVE_ERRORS = 3;
         const MAX_POLL_COUNT = 120; // Max 6 minutes of polling (120 * 3 seconds)
 
         const checkStatus = async () => {
             if (!isMounted) return;
             
             try {
                 const statusRes = await writingApi.getGradingStatus(attemptId);
                 const status = statusRes.data.status;
                 
                 if (!isMounted) return;
                 
                 consecutiveErrors = 0; // Reset error counter on success
                 setGradingStatus(status);
                 currentPollCount++;
 
                 if (status === 'COMPLETED' || status === 'PARTIAL_FAILURE') {
                     clearInterval(pollInterval);
                     // Fetch review data
                     try {
                         const reviewRes = await writingApi.getWritingReview(attemptId);
                         if (isMounted) {
                             setReview(reviewRes.data);
                             setLoading(false);
                             setIsRegrading(false); // Reset regrading state
                         }
                     } catch (reviewErr) {
                         console.error('Error fetching review:', reviewErr);
                         if (isMounted) {
                             setError('Không thể tải kết quả chấm điểm. Vui lòng tải lại trang.');
                             setLoading(false);
                             setIsRegrading(false);
                         }
                     }
                 } else if (status === 'FAILED') {
                     clearInterval(pollInterval);
                     if (isMounted) {
                         setError('Chấm điểm thất bại. Vui lòng thử chấm lại.');
                         setLoading(false);
                         setIsRegrading(false);
                     }
                 }
             } catch (err) {
                 console.error('Error checking grading status:', err);
                 consecutiveErrors++;
                 
                 if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                     clearInterval(pollInterval);
                     if (isMounted) {
                         setError('Không thể kiểm tra trạng thái chấm điểm. Vui lòng tải lại trang.');
                         setLoading(false);
                         setIsRegrading(false);
                     }
                 }
             }
         };
 
         // Initial check
         checkStatus();
         
         // Start polling - every 3 seconds
         pollInterval = setInterval(() => {
             if (currentPollCount < MAX_POLL_COUNT) {
                 checkStatus();
             } else {
                 clearInterval(pollInterval);
                 if (isMounted && loading) {
                     setError('Quá thời gian chờ. Vui lòng tải lại trang để xem kết quả.');
                     setLoading(false);
                     setIsRegrading(false);
                 }
             }
         }, 3000);
         
         return () => {
             isMounted = false;
             clearInterval(pollInterval);
         };
     }, [attemptId, review, error]); // Only re-run if attemptId changes or review/error is set
 
     // Helpers
     const getTaskReview = useCallback((taskNumber) => {
         if (!review?.tasks) return null;
         return review.tasks.find(t => t.taskNumber === taskNumber);
     }, [review]);
 
     const getTaskPrompt = useCallback((taskNumber) => {
         if (!review?.prompts) return null;
         return review.prompts.find(p => p.taskNumber === taskNumber);
     }, [review]);
 
     const currentTaskReview = getTaskReview(activeTask);
     const currentTaskPrompt = getTaskPrompt(activeTask);
     
     // Get AI feedback with proper key mapping
     const aiFeedback = useMemo(() => {
         if (!currentTaskReview?.aiFeedback) return {};
         const feedback = currentTaskReview.aiFeedback;
         return {
             sentenceCorrections: feedback.sentence_corrections || [],
             paragraphRewrites: feedback.paragraph_rewrites || [],
             vocabularyHighlights: feedback.vocabulary_highlights || [],
             errorAnalysis: feedback.error_analysis || {},
             sampleEssayBandPlus: feedback.sample_essay_band_plus_one || '',
             sampleEssayBand9: feedback.sample_essay_band_9 || '',
             feedbackSummary: feedback.feedback_summary || {},
             wordAnalysis: feedback.word_analysis || [],
             criteriaComments: feedback.criteria_comments || {},
             error: feedback.error || null,
         };
     }, [currentTaskReview]);
 
     // Get band scores
     const bandScores = useMemo(() => {
         if (!currentTaskReview?.bandScores) return {};
         const scores = currentTaskReview.bandScores;
         return {
             taskAchievement: scores.task_achievement || scores.task_response || 0,
             coherenceCohesion: scores.coherence_cohesion || 0,
             lexicalResource: scores.lexical_resource || 0,
             grammaticalRange: scores.grammatical_range_accuracy || 0,
         };
     }, [currentTaskReview]);
 
     // Build highlight map for essay - enhanced with vocabulary and full paragraphs
     const highlightMap = useMemo(() => {
         const map = [];
         
         // 1. Add sentence corrections (errors to fix)
         aiFeedback.sentenceCorrections?.forEach((corr, idx) => {
             if (corr.original) {
                 map.push({
                     id: `correction-${idx}`,
                     text: corr.original,
                     type: corr.error_type || 'grammar',
                     severity: corr.severity || 'minor',
                     category: 'corrections',
                 });
             }
         });
 
         // 2. Add vocabulary highlights (both good and problematic)
         aiFeedback.vocabularyHighlights?.forEach((vocab, idx) => {
             if (vocab.word) {
                 const category = vocab.category || 'vocabulary';
                 let type = 'vocabulary';
                 if (category.includes('good') || category === 'advanced_good' || category === 'collocation_good' || category === 'academic') {
                     type = 'vocabulary_good';
                 } else if (category === 'error' || category === 'awkward') {
                     type = 'vocabulary_error';
                 }
                 map.push({
                     id: `vocab-${idx}`,
                     text: vocab.word,
                     type: type,
                     category: 'wordAnalysis',
                     note: vocab.note,
                 });
             }
         });
 
         // 3. Add word analysis items (from the detailed word analysis section)
         aiFeedback.wordAnalysis?.forEach((word, idx) => {
             if (word.word) {
                 let type = 'vocabulary';
                 if (word.usage_quality === 'good') {
                     type = 'vocabulary_good';
                 } else if (word.usage_quality === 'incorrect') {
                     type = 'vocabulary_error';
                 }
                 // Only add if not already in vocabularyHighlights
                 const alreadyExists = map.some(m => m.text.toLowerCase() === word.word.toLowerCase());
                 if (!alreadyExists) {
                     map.push({
                         id: `word-${idx}`,
                         text: word.word,
                         type: type,
                         category: 'wordAnalysis',
                     });
                 }
             }
         });
 
         return map;
     }, [aiFeedback]);
 
     // Scroll to analysis item
     const scrollToItem = useCallback((itemId, category) => {
         setSelectedItemId(itemId);
         
         // Expand the relevant section if collapsed
         if (!expandedSections[category]) {
             setExpandedSections(prev => ({ ...prev, [category]: true }));
         }
 
         // Wait for expansion animation then scroll
         setTimeout(() => {
             const element = itemRefs.current[itemId];
             if (element && analysisColumnRef.current) {
                 element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                 element.classList.add('highlight-flash');
                 setTimeout(() => element.classList.remove('highlight-flash'), 1500);
             }
         }, 100);
     }, [expandedSections]);
 
     // Handle re-grade request
     const handleRegrade = async () => {
         if (isRegrading) return;
         
         try {
             setIsRegrading(true);
             setLoading(true);
             setGradingStatus('PENDING');
             setReview(null); // Reset review to trigger polling
             setError(null);  // Reset error to allow polling
             
             await writingApi.regradeAttempt(attemptId);
             // The useEffect polling will pick up the new grading status
         } catch (err) {
             console.error('Failed to start re-grading:', err);
             setError('Không thể bắt đầu chấm lại. Vui lòng thử lại sau.');
             setLoading(false);
             setIsRegrading(false);
         }
     };
 
     // Toggle section
     const toggleSection = (section) => {
         setExpandedSections(prev => ({ ...prev, [section]: !prev[section] }));
     };
 
     // Toggle score detail
     const toggleScoreDetail = (criterionKey) => {
         setExpandedScores(prev => ({ ...prev, [criterionKey]: !prev[criterionKey] }));
     };
 
     // Render essay with interactive highlights
     const renderHighlightedEssay = () => {
         const essayText = currentTaskReview?.essayText || '';
         if (!essayText) return <p className="no-content">Không có nội dung bài viết.</p>;
 
         let result = essayText;
         
         // Sort highlights by length (longest first) to avoid nested replacements
         const sortedHighlights = [...highlightMap].sort((a, b) => b.text.length - a.text.length);
 
         // Create a working copy with markers
         sortedHighlights.forEach((highlight) => {
             const escapedText = highlight.text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
             const regex = new RegExp(escapedText, 'gi');
             const style = getErrorStyle(highlight.type);
             // Add data-type attribute for CSS styling of vocabulary highlights
             result = result.replace(regex, `<mark class="essay-highlight" data-id="${highlight.id}" data-category="${highlight.category}" data-type="${highlight.type}" style="background-color: ${style.bg}; border-bottom: 2px solid ${style.border}; cursor: pointer;">${highlight.text}</mark>`);
         });
 
         // Split into paragraphs
         const paragraphs = result.split('\n').filter(p => p.trim());
 
         return (
             <div 
                 className="essay-content"
                 onClick={(e) => {
                     const mark = e.target.closest('mark.essay-highlight');
                     if (mark) {
                         const itemId = mark.dataset.id;
                         const category = mark.dataset.category;
                         scrollToItem(itemId, category);
                     }
                 }}
             >
                 {paragraphs.map((para, idx) => (
                     <p key={idx} dangerouslySetInnerHTML={{ __html: para }} />
                 ))}
             </div>
         );
     };
 
     // Get score level description
     const getScoreLevel = (score) => {
         if (score >= 8) return { label: 'Xuất sắc', color: '#16a34a' };
         if (score >= 7) return { label: 'Rất tốt', color: '#22c55e' };
         if (score >= 6) return { label: 'Tốt', color: '#ca8a04' };
         if (score >= 5) return { label: 'Khá', color: '#ea580c' };
         return { label: 'Cần cải thiện', color: '#dc2626' };
     };
 
     // Render score bar component
     const renderScoreBar = (label, key, score, comment) => {
         const level = getScoreLevel(score);
         const isExpanded = expandedScores[key];
         const widthPercent = (score / 9) * 100;
 
         return (
             <div className={`score-criterion ${isExpanded ? 'expanded' : ''}`} key={key}>
                 <button 
                     className="score-criterion-header"
                     onClick={() => toggleScoreDetail(key)}
                 >
                     <div className="criterion-info">
                         <span className="criterion-label">{label}</span>
                         <div className="criterion-bar-container">
                             <div 
                                 className="criterion-bar" 
                                 style={{ width: `${widthPercent}%`, backgroundColor: level.color }}
                             />
                         </div>
                     </div>
                     <div className="criterion-score">
                         <span className="score-value" style={{ color: level.color }}>{score ? score.toFixed(1) : 'N/A'}</span>
                         <span className="expand-icon">{isExpanded ? '▼' : '▶'}</span>
                     </div>
                 </button>
                 {isExpanded && (
                     <div className="criterion-comment">
                         <p>{comment || 'Không có nhận xét chi tiết.'}</p>
                     </div>
                 )}
             </div>
         );
     };
 
     // Loading state - Enhanced animated grading screen
     if (loading) {
         return (
             <div className="writing-result-loading">
                 {/* Animated background elements */}
                 <div className="loading-bg-effects">
                     <div className="floating-shape shape-1" />
                     <div className="floating-shape shape-2" />
                     <div className="floating-shape shape-3" />
                     <div className="floating-shape shape-4" />
                     <div className="floating-shape shape-5" />
                 </div>
 
                 <div className="grading-animation-container">
                     <div className="grading-animation">
                         {/* Main AI animation orb */}
                         <div className="ai-orb-container">
                             <div className="ai-orb">
                                 <div className="orb-core" />
                                 <div className="orb-ring ring-1" />
                                 <div className="orb-ring ring-2" />
                                 <div className="orb-ring ring-3" />
                                 <div className="orb-particles">
                                     {[...Array(8)].map((_, i) => (
                                         <div key={i} className={`particle particle-${i + 1}`} />
                                     ))}
                                 </div>
                             </div>
                             <div className="ai-text-badge">
                                 <span className="ai-badge-icon">✨</span>
                                 <span>AI</span>
                             </div>
                         </div>
 
                         {/* Main heading with typing effect */}
                         <h2 className="grading-title">
                             <span className="title-text">Cramer đang chấm điểm</span>
                             <span className="typing-dots">
                                 <span className="dot" />
                                 <span className="dot" />
                                 <span className="dot" />
                             </span>
                         </h2>
                         <p className="grading-subtitle">Quá trình này mất khoảng 1-2 phút</p>
 
                         {/* Enhanced progress steps */}
                         <div className="grading-steps-enhanced">
                             <div className={`step-enhanced ${gradingStatus !== 'PENDING' ? 'done' : 'active'}`}>
                                 <div className="step-icon-wrapper">
                                     <FiFileText size={20} className="step-icon" />
                                     <div className="step-glow" />
                                 </div>
                                 <div className="step-content">
                                     <span className="step-label">Nhận bài viết</span>
                                     <span className="step-status">
                                         {gradingStatus !== 'PENDING' ? '✓ Hoàn thành' : 'Đang xử lý...'}
                                     </span>
                                 </div>
                             </div>
                             
                             <div className="step-connector">
                                 <div className={`connector-line ${gradingStatus !== 'PENDING' ? 'active' : ''}`} />
                             </div>
                             
                             <div className={`step-enhanced ${gradingStatus === 'GRADING' ? 'active' : gradingStatus === 'COMPLETED' ? 'done' : ''}`}>
                                 <div className="step-icon-wrapper">
                                     <FiTarget size={20} className="step-icon" />
                                     <div className="step-glow" />
                                 </div>
                                 <div className="step-content">
                                     <span className="step-label">Phân tích & Chấm điểm</span>
                                     <span className="step-status">
                                         {gradingStatus === 'GRADING' ? 'Đang phân tích...' : gradingStatus === 'COMPLETED' ? '✓ Hoàn thành' : 'Chờ xử lý'}
                                     </span>
                                 </div>
                             </div>
                             
                             <div className="step-connector">
                                 <div className={`connector-line ${gradingStatus === 'GRADING' || gradingStatus === 'COMPLETED' ? 'active' : ''}`} />
                             </div>
                             
                             <div className={`step-enhanced ${gradingStatus === 'COMPLETED' ? 'done' : ''}`}>
                                 <div className="step-icon-wrapper">
                                     <FiCheckCircle size={20} className="step-icon" />
                                     <div className="step-glow" />
                                 </div>
                                 <div className="step-content">
                                     <span className="step-label">Tạo nhận xét</span>
                                     <span className="step-status">
                                         {gradingStatus === 'COMPLETED' ? '✓ Hoàn thành' : 'Chờ xử lý'}
                                     </span>
                                 </div>
                             </div>
                         </div>
 
                         {/* Animated progress bar */}
                         <div className="progress-bar-enhanced">
                             <div className="progress-track">
                                 <div className={`progress-fill ${gradingStatus === 'GRADING' ? 'grading' : gradingStatus === 'COMPLETED' ? 'done' : 'pending'}`}>
                                     <div className="progress-shimmer" />
                                 </div>
                             </div>
                             <div className="progress-percentage">
                                 {gradingStatus === 'PENDING' ? '10%' : gradingStatus === 'GRADING' ? '60%' : '100%'}
                             </div>
                         </div>
 
                         {/* Fun facts carousel */}
                         <div className="fun-facts-section">
                             <div className="fun-fact-card">
                                 <FiInfo size={16} className="fact-icon" />
                                 <p>Bạn có thể đóng trang này và quay lại sau - kết quả sẽ được lưu tự động!</p>
                             </div>
                         </div>
 
                         {/* Action button */}
                         <button className="back-to-dashboard-btn" onClick={() => navigate('/dashboard')}>
                             <FiArrowLeft size={16} />
                             <span>Quay về Dashboard</span>
                         </button>
                     </div>
                 </div>
             </div>
         );
     }
 
     // Error state
     if (error) {
         return (
             <div className="writing-result-error">
                 <div className="error-content">
                     <FiXCircle size={48} className="error-icon" />
                     <h2>Đã xảy ra lỗi</h2>
                     <p>{error}</p>
                     <button onClick={() => navigate('/dashboard')}>Quay về Dashboard</button>
                 </div>
             </div>
         );
     }
 
     return (
         <div className="writing-result-page">
             {/* Purple Header - Unified Design */}
             <header className="review-header">
                 <div className="review-header-top">
                     <div className="review-header-left">
                         <button className="back-btn" onClick={() => navigate('/dashboard')}>
                             <FiArrowLeft size={14} /> Quay lại
                         </button>
                         <h1 className="review-title">{review?.examSource?.toUpperCase()} · Test {review?.testNumber} · Writing</h1>
                     </div>
                     <div className="review-header-center">
                         <div className="summary-item">
                             <span className="summary-label">THỜI GIAN LÀM</span>
                             <span className="summary-value">
                                 {review?.completedAt ? new Date(review.completedAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : 'N/A'}
                             </span>
                         </div>
                         <div className="summary-item">
                             <span className="summary-label">NGÀY LÀM</span>
                             <span className="summary-value">
                                 {review?.completedAt ? new Date(review.completedAt).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }) : 'N/A'}
                             </span>
                         </div>
                     </div>
                     <div className="review-header-right">
                         <div className="header-actions">
                             <button 
                                 className="btn btn-regrade"
                                 onClick={handleRegrade}
                                 disabled={isRegrading}
                                 title="Chấm lại bài viết với AI"
                             >
                                 <FiRotateCw size={14} className={isRegrading ? 'spinning' : ''} /> Chấm lại
                             </button>
                             <Link to="/dashboard" className="btn btn-secondary">
                                 Dashboard
                             </Link>
                         </div>
                         <div className="band-badge">
                             <span className="label">BAND</span>
                             <span className={`value band-${Math.floor(review?.overallBand || 0)}`}>
                                 {review?.overallBand ? Number(review.overallBand).toFixed(1) : 'N/A'}
                             </span>
                         </div>
                     </div>
                 </div>
             </header>
 
             {/* Task Tabs */}
             <div className="result-task-tabs">
                 {review?.tasks?.sort((a, b) => a.taskNumber - b.taskNumber).map(task => (
                     <button
                         key={task.taskNumber}
                         className={`task-tab ${activeTask === task.taskNumber ? 'active' : ''}`}
                         onClick={() => setActiveTask(task.taskNumber)}
                     >
                         <span className="task-name">Task {task.taskNumber}</span>
                         <span className={`task-band band-${Math.floor(task.overallBand || 0)}`}>
                             {task.overallBand ? Number(task.overallBand).toFixed(1) : 'N/A'}
                         </span>
                     </button>
                 ))}
             </div>
 
             {/* Collapsible Score Bar */}
             <div className={`scores-bar-wrapper ${scoresBarCollapsed ? 'collapsed' : ''}`}>
                 <button 
                     className="scores-bar-toggle"
                     onClick={() => setScoresBarCollapsed(!scoresBarCollapsed)}
                 >
                     <span className="toggle-label">
                         {scoresBarCollapsed ? <><FiChevronRight size={14} /> Hiển thị điểm thành phần</> : <><FiChevronDown size={14} /> Ẩn điểm thành phần</>}
                     </span>
                     <div className="task-band-mini">
                         <span className="label">Task {activeTask}</span>
                         <span className={`value band-${Math.floor(currentTaskReview?.overallBand || 0)}`}>
                             {currentTaskReview?.overallBand ? Number(currentTaskReview.overallBand).toFixed(1) : 'N/A'}
                         </span>
                     </div>
                 </button>
                 {!scoresBarCollapsed && (
                     <div className="scores-bar">
                         <div className="scores-grid">
                             {renderScoreBar(
                                 activeTask === 1 ? 'Task Achievement' : 'Task Response',
                                 'taskAchievement',
                                 bandScores.taskAchievement || 0,
                                 aiFeedback.criteriaComments?.task_achievement
                             )}
                             {renderScoreBar(
                                 'Coherence & Cohesion',
                                 'coherenceCohesion', 
                                 bandScores.coherenceCohesion || 0,
                                 aiFeedback.criteriaComments?.coherence_cohesion
                             )}
                             {renderScoreBar(
                                 'Lexical Resource',
                                 'lexicalResource',
                                 bandScores.lexicalResource || 0,
                                 aiFeedback.criteriaComments?.lexical_resource
                             )}
                             {renderScoreBar(
                                 'Grammar Range & Accuracy',
                                 'grammaticalRange',
                                 bandScores.grammaticalRange || 0,
                                 aiFeedback.criteriaComments?.grammatical_range
                             )}
                         </div>
                         <div className="task-band-summary">
                             <span className="label">Task {activeTask}</span>
                             <span className={`value band-${Math.floor(currentTaskReview?.overallBand || 0)}`}>
                                 {currentTaskReview?.overallBand ? Number(currentTaskReview.overallBand).toFixed(1) : 'N/A'}
                             </span>
                         </div>
                     </div>
                 )}
             </div>
 
             {/* Main Content - Three Column Resizable Layout */}
             <div className="result-main-content">
                 <PanelGroup direction="horizontal" className="result-panel-group">
                     {/* Left Column - Task Prompt */}
                     <Panel defaultSize={25} minSize={15} maxSize={40}>
                         <div className="result-column prompt-column">
                             <div className="column-header">
                                 <h3><FiFileText size={16} /> Đề bài</h3>
                             </div>
                             <div className="column-content">
                                 {currentTaskPrompt?.promptText && (
                                     <div 
                                         className="task-prompt-text"
                                         dangerouslySetInnerHTML={{ __html: currentTaskPrompt.promptText }}
                                     />
                                 )}
                                 {currentTaskPrompt?.imageUrl && (
                                     <div className="task-prompt-image">
                                         <img src={currentTaskPrompt.imageUrl} alt="Task Figure" />
                                     </div>
                                 )}
                                 {/* <div className="word-requirement">
                                     <span>Yêu cầu tối thiểu:</span>
                                     <strong>{activeTask === 1 ? '150' : '250'} từ</strong>
                                 </div> */}
                             </div>
                         </div>
                     </Panel>
 
                     <PanelResizeHandle className="resize-handle">
                         <div className="resize-handle-icon-container">
                             <span className="resize-handle-icon">↔</span>
                         </div>
                     </PanelResizeHandle>
 
                     {/* Middle Column - Essay */}
                     <Panel defaultSize={40} minSize={25}>
                         <div className="result-column essay-column">
                             <div className="column-header">
                                 <h3><FiEdit3 size={16} /> Bài viết của bạn</h3>
                                 <div className="word-count">{currentTaskReview?.wordCount || 0} từ</div>
                             </div>
                             <div className="column-content">
                                 {/* Legend */}
                                 <div className="highlight-legend">
                                     <span className="legend-title">Click vào text được highlight để xem chi tiết:</span>
                                     <div className="legend-items">
                                         {Object.entries(ERROR_TYPE_COLORS).map(([type, colors]) => (
                                             <span key={type} className="legend-item">
                                                 <span className="dot" style={{ backgroundColor: colors.border }} />
                                                 {colors.label}
                                             </span>
                                         ))}
                                     </div>
                                 </div>
 
                                 {/* Essay with highlights */}
                                 <div className="essay-text-container">
                                     {renderHighlightedEssay()}
                                 </div>
 
                                 {/* Error notice */}
                                 {aiFeedback.error && (
                                     <div className="grading-error-notice">
                                         <FiAlertTriangle size={18} className="error-icon" />
                                         <p>{aiFeedback.error}</p>
                                     </div>
                                 )}
                             </div>
                         </div>
                     </Panel>
 
                     <PanelResizeHandle className="resize-handle">
                         <div className="resize-handle-icon-container">
                             <span className="resize-handle-icon">↔</span>
                         </div>
                     </PanelResizeHandle>
 
                     {/* Right Column - Analysis & Feedback */}
                     <Panel defaultSize={35} minSize={20}>
                         <div className="result-column analysis-column" ref={analysisColumnRef}>
                             <div className="column-header">
                                 <h3><FiBarChart2 size={16} /> Phân tích chi tiết</h3>
                             </div>
                             <div className="column-content">
                                 {/* Feedback Summary */}
                                 {aiFeedback.feedbackSummary && Object.keys(aiFeedback.feedbackSummary).length > 0 && (
                                     <div className="feedback-summary-cards">
                                         {aiFeedback.feedbackSummary.strengths?.length > 0 && (
                                             <div className="feedback-card strengths">
                                                 <h4><FiThumbsUp size={14} /> Điểm mạnh</h4>
                                                 <ul>
                                                     {aiFeedback.feedbackSummary.strengths.map((s, i) => (
                                                         <li key={i}>{s}</li>
                                                     ))}
                                                 </ul>
                                             </div>
                                         )}
                                         {aiFeedback.feedbackSummary.weaknesses?.length > 0 && (
                                             <div className="feedback-card weaknesses">
                                                 <h4><FiAlertTriangle size={14} /> Điểm yếu</h4>
                                                 <ul>
                                                     {aiFeedback.feedbackSummary.weaknesses.map((w, i) => (
                                                         <li key={i}>{w}</li>
                                                     ))}
                                                 </ul>
                                             </div>
                                         )}
                                         {aiFeedback.feedbackSummary.writing_approach && (
                                             <div className="feedback-card approach">
                                                 <h4><FiEdit size={14} /> Hướng viết đề xuất</h4>
                                                 <p>{aiFeedback.feedbackSummary.writing_approach}</p>
                                             </div>
                                         )}
                                         {aiFeedback.feedbackSummary.improvement_tips && (
                                             <div className="feedback-card tips">
                                                 <h4><FiZap size={14} /> Hướng dẫn cải thiện</h4>
                                                 <p>{aiFeedback.feedbackSummary.improvement_tips}</p>
                                             </div>
                                         )}
                                     </div>
                                 )}
 
                                 {/* Error Analysis Summary - NEW */}
                                 {aiFeedback.errorAnalysis && (aiFeedback.errorAnalysis.major_errors !== undefined || aiFeedback.errorAnalysis.minor_errors !== undefined) && (
                                     <div className="error-analysis-summary">
                                         <div className="error-counts">
                                             <div className="error-count major">
                                                 <span className="count">{aiFeedback.errorAnalysis.major_errors || 0}</span>
                                                 <span className="label">Lỗi lớn</span>
                                             </div>
                                             <div className="error-count minor">
                                                 <span className="count">{aiFeedback.errorAnalysis.minor_errors || 0}</span>
                                                 <span className="label">Lỗi nhỏ</span>
                                             </div>
                                         </div>
                                         {aiFeedback.errorAnalysis.summary && (
                                             <p className="error-summary-text">{aiFeedback.errorAnalysis.summary}</p>
                                         )}
                                     </div>
                                 )}
 
                                 {/* Sentence Corrections */}
                                 {aiFeedback.sentenceCorrections?.length > 0 && (
                                     <div className={`expandable-section ${expandedSections.corrections ? 'open' : ''}`}>
                                         <button 
                                             className="section-toggle"
                                             onClick={() => toggleSection('corrections')}
                                         >
                                             <FiEdit3 size={16} className="section-icon" />
                                             <span className="dropdown-title">Sửa lỗi câu ({aiFeedback.sentenceCorrections.length})</span>
                                             <span className="toggle-arrow">{expandedSections.corrections ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                                         </button>
                                         {expandedSections.corrections && (
                                             <div className="section-content">
                                                 {aiFeedback.sentenceCorrections.map((corr, idx) => {
                                                     const itemId = `correction-${idx}`;
                                                     const style = getErrorStyle(corr.error_type);
                                                     return (
                                                         <div 
                                                             key={idx}
                                                             ref={el => itemRefs.current[itemId] = el}
                                                             className={`correction-item ${selectedItemId === itemId ? 'selected' : ''}`}
                                                             style={{ borderLeftColor: style.border }}
                                                         >
                                                             <div className="correction-header">
                                                                 <span 
                                                                     className="error-type-badge"
                                                                     style={{ backgroundColor: style.border }}
                                                                 >
                                                                     {corr.error_type || 'error'}
                                                                 </span>
                                                             </div>
                                                             <div className="correction-original">
                                                                 <span className="label"><FiXCircle size={14} /></span>
                                                                 <span className="text">{corr.original}</span>
                                                             </div>
                                                             <div className="correction-fixed">
                                                                 <span className="label"><FiCheckCircle size={14} /></span>
                                                                 <span className="text">{corr.corrected}</span>
                                                             </div>
                                                             {corr.explanation && (
                                                                 <div className="correction-explanation">
                                                                     <FiInfo size={14} /> {corr.explanation}
                                                                 </div>
                                                             )}
                                                         </div>
                                                     );
                                                 })}
                                             </div>
                                         )}
                                     </div>
                                 )}
 
                                 {/* Paragraph Rewrites */}
                                 {aiFeedback.paragraphRewrites?.length > 0 && (
                                     <div className={`expandable-section ${expandedSections.paragraphs ? 'open' : ''}`}>
                                         <button 
                                             className="section-toggle"
                                             onClick={() => toggleSection('paragraphs')}
                                         >
                                             <FiBook size={16} className="section-icon" />
                                             <span className="dropdown-title">Viết lại đoạn ({aiFeedback.paragraphRewrites.length})</span>
                                             <span className="toggle-arrow">{expandedSections.paragraphs ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                                         </button>
                                         {expandedSections.paragraphs && (
                                             <div className="section-content">
                                                 {aiFeedback.paragraphRewrites.map((para, idx) => {
                                                     const itemId = `paragraph-${idx}`;
                                                     return (
                                                         <div 
                                                             key={idx}
                                                             ref={el => itemRefs.current[itemId] = el}
                                                             className={`paragraph-item ${selectedItemId === itemId ? 'selected' : ''}`}
                                                         >
                                                             <div className="para-header">Đoạn {(para.paragraph_index || idx) + 1}</div>
                                                             <div className="original-para">
                                                                 <span className="para-label"><FiFileText size={14} /> Bản gốc:</span>
                                                                 <p>{para.original}</p>
                                                             </div>
                                                             <div className="improved-para">
                                                                 <span className="para-label"><FiTrendingUp size={14} /> Bản cải thiện:</span>
                                                                 <p>{para.improved}</p>
                                                             </div>
                                                             {para.improvements_made?.length > 0 && (
                                                                 <div className="improvements-made">
                                                                     <span className="para-label"><FiZap size={14} /> Các cải thiện:</span>
                                                                     <ul>
                                                                         {para.improvements_made.map((imp, i) => (
                                                                             <li key={i}>{imp}</li>
                                                                         ))}
                                                                     </ul>
                                                                 </div>
                                                             )}
                                                         </div>
                                                     );
                                                 })}
                                             </div>
                                         )}
                                     </div>
                                 )}
 
                                 {/* Sample Essays */}
                                 {aiFeedback.sampleEssayBandPlus && (
                                     <div className={`expandable-section ${expandedSections.sampleBandPlus ? 'open' : ''}`}>
                                         <button 
                                             className="section-toggle sample-toggle"
                                             onClick={() => toggleSection('sampleBandPlus')}
                                         >
                                             <FiTrendingUp size={16} className="section-icon" />
                                             <span className="dropdown-title">
                                                 Phiên bản cải tiến Band {Math.min(9, Math.floor((currentTaskReview?.overallBand || 6)) + 1)}
                                             </span>
                                             <span className="toggle-arrow">{expandedSections.sampleBandPlus ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                                         </button>
                                         {expandedSections.sampleBandPlus && (
                                             <div className="section-content sample-content">
                                                 <p className="sample-description">
                                                     Phiên bản cải thiện, đạt band cao hơn 1 điểm.
                                                 </p>
                                                 <div className="sample-essay-text">
                                                     {aiFeedback.sampleEssayBandPlus.split('\n').map((para, idx) => (
                                                         <p key={idx}>{para || '\u00A0'}</p>
                                                     ))}
                                                 </div>
                                             </div>
                                         )}
                                     </div>
                                 )}
 
                                 {aiFeedback.sampleEssayBand9 && (
                                     <div className={`expandable-section ${expandedSections.sampleBand9 ? 'open' : ''}`}>
                                         <button 
                                             className="section-toggle sample-toggle band-9"
                                             onClick={() => toggleSection('sampleBand9')}
                                         >
                                             <FiAward size={16} className="section-icon" />
                                             <span className="dropdown-title">Bài mẫu Band 9.0</span>
                                             <span className="toggle-arrow">{expandedSections.sampleBand9 ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                                         </button>
                                         {expandedSections.sampleBand9 && (
                                             <div className="section-content sample-content band-9">
                                                 <p className="sample-description">
                                                     Bài mẫu đạt band 9.0 cho đề bài này.
                                                 </p>
                                                 <div className="sample-essay-text">
                                                     {aiFeedback.sampleEssayBand9.split('\n').map((para, idx) => (
                                                         <p key={idx}>{para || '\u00A0'}</p>
                                                     ))}
                                                 </div>
                                             </div>
                                         )}
                                     </div>
                                 )}
 
                                 {/* Word Analysis */}
                                 {aiFeedback.wordAnalysis?.length > 0 && (
                                     <div className={`expandable-section ${expandedSections.wordAnalysis ? 'open' : ''}`}>
                                         <button 
                                             className="section-toggle"
                                             onClick={() => toggleSection('wordAnalysis')}
                                         >
                                             <FiBook size={16} className="section-icon" />
                                             <span className="dropdown-title">Phân tích từ vựng ({aiFeedback.wordAnalysis.length})</span>
                                             <span className="toggle-arrow">{expandedSections.wordAnalysis ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                                         </button>
                                         {expandedSections.wordAnalysis && (
                                             <div className="section-content">
                                                 <div className="word-analysis-list">
                                                     {aiFeedback.wordAnalysis.map((word, idx) => (
                                                         <div key={idx} className={`word-item usage-${word.usage_quality || 'acceptable'}`}>
                                                             <div className="word-header">
                                                                 <span className="word-text">{word.word}</span>
                                                                 <span className={`usage-badge ${word.usage_quality || 'acceptable'}`}>
                                                                     {word.usage_quality === 'good' ? '✓ Tốt' : 
                                                                      word.usage_quality === 'incorrect' ? '✗ Sai' : '○ Được'}
                                                                 </span>
                                                             </div>
                                                             <div className="word-definition">{word.definition}</div>
                                                             <div className="word-context">
                                                                 <span className="context-label">Ngữ cảnh:</span> {word.context}
                                                             </div>
                                                         </div>
                                                     ))}
                                                 </div>
                                             </div>
                                         )}
                                     </div>
                                 )}
                             </div>
                         </div>
                     </Panel>
                 </PanelGroup>
             </div>
         </div>
     );
 };
 
 export default WritingResultPage;
```

</details>


---

<sub>⏱️ 16m 7s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\controller\WritingController.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\controller\WritingController.java

<details>
<summary>139 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/controller/WritingController.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/controller/WritingController.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/controller/WritingController.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/controller/WritingController.java
@@ -1,170 +1,170 @@
 package com.cramer.controller;
 
 import com.cramer.dto.WritingReviewDTO;
 import com.cramer.dto.WritingSubmissionDTO;
 import com.cramer.dto.WritingSubmitDTO;
 import com.cramer.service.WritingSubmissionService;
 import io.swagger.v3.oas.annotations.Operation;
 import io.swagger.v3.oas.annotations.tags.Tag;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.http.ResponseEntity;
 import org.springframework.security.core.Authentication;
 import org.springframework.web.bind.annotation.*;
 
 import java.util.List;
 import java.util.Map;
 import java.util.UUID;
 
 /**
  * REST controller for Writing test submissions and grading.
  */
 @RestController
 @RequestMapping("/api/writing")
 @Tag(name = "Writing API", description = "API for IELTS Writing test submissions and AI grading")
 public class WritingController {
 
     private static final Logger logger = LoggerFactory.getLogger(WritingController.class);
 
     private final WritingSubmissionService writingSubmissionService;
 
     @Autowired
     public WritingController(WritingSubmissionService writingSubmissionService) {
         this.writingSubmissionService = writingSubmissionService;
     }
 
     /**
      * Save essay draft during test.
      */
     @PostMapping("/draft/{attemptId}")
     @Operation(summary = "Save essay draft", description = "Save an essay draft during the test")
     public ResponseEntity<WritingSubmissionDTO> saveDraft(
             @PathVariable Long attemptId,
             @RequestParam Integer taskNumber,
             @RequestBody String essayText,
             Authentication authentication) {
         
         logger.info("📥 POST /api/writing/draft/{} - taskNumber={}", attemptId, taskNumber);
         
         UUID userId = UUID.fromString(authentication.getName());
         WritingSubmissionDTO saved = writingSubmissionService.saveDraft(attemptId, taskNumber, essayText, userId);
         
         logger.info("✅ Draft saved for attempt {} task {}", attemptId, taskNumber);
         return ResponseEntity.ok(saved);
     }
 
     /**
      * Submit essays for grading.
      */
     @PostMapping("/submit/{attemptId}")
     @Operation(summary = "Submit essays for grading", description = "Submit essays and start background AI grading")
     public ResponseEntity<Map<String, Object>> submitForGrading(
             @PathVariable Long attemptId,
             @RequestBody WritingSubmitDTO submitDTO,
             Authentication authentication) {
         
         logger.info("📥 POST /api/writing/submit/{} - essays count: {}", 
                    attemptId, submitDTO.getEssays() != null ? submitDTO.getEssays().size() : 0);
         
         UUID userId = UUID.fromString(authentication.getName());
         Map<String, Object> result = writingSubmissionService.submitForGrading(
             attemptId, submitDTO.getEssays(), userId);
         
         logger.info("✅ Essays submitted for grading, attempt {}", attemptId);
         return ResponseEntity.ok(result);
     }
 
     /**
      * Get grading status for an attempt.
      */
     @GetMapping("/status/{attemptId}")
     @Operation(summary = "Get grading status", description = "Check the grading status of submitted essays")
     public ResponseEntity<Map<String, Object>> getGradingStatus(
             @PathVariable Long attemptId,
             Authentication authentication) {
         
         logger.info("📥 GET /api/writing/status/{}", attemptId);
         
         UUID userId = UUID.fromString(authentication.getName());
         Map<String, Object> status = writingSubmissionService.getGradingStatus(attemptId, userId);
         
         return ResponseEntity.ok(status);
     }
 
     /**
      * Get full writing review with grading results.
      */
     @GetMapping("/review/{attemptId}")
     @Operation(summary = "Get writing review", description = "Get full review with AI grading results and feedback")
     public ResponseEntity<WritingReviewDTO> getWritingReview(
             @PathVariable Long attemptId,
             Authentication authentication) {
         
         logger.info("📥 GET /api/writing/review/{}", attemptId);
         
         UUID userId = UUID.fromString(authentication.getName());
         WritingReviewDTO review = writingSubmissionService.getWritingReview(attemptId, userId);
         
         logger.info("✅ Writing review fetched for attempt {}", attemptId);
         return ResponseEntity.ok(review);
     }
 
     /**
      * Get submissions for an attempt.
      */
     @GetMapping("/submissions/{attemptId}")
     @Operation(summary = "Get submissions", description = "Get all writing submissions for an attempt")
     public ResponseEntity<List<WritingSubmissionDTO>> getSubmissions(
             @PathVariable Long attemptId,
             Authentication authentication) {
         
         logger.info("📥 GET /api/writing/submissions/{}", attemptId);
         
         UUID userId = UUID.fromString(authentication.getName());
         List<WritingSubmissionDTO> submissions = writingSubmissionService.getSubmissions(attemptId, userId);
         
         return ResponseEntity.ok(submissions);
     }
 
     /**
      * Validate Gemini API key.
      */
     @PostMapping("/validate-api-key")
     @Operation(summary = "Validate API key", description = "Validate a Gemini API key")
     public ResponseEntity<Map<String, Object>> validateApiKey(
             @RequestBody Map<String, String> request,
             Authentication authentication) {
         
         logger.info("📥 POST /api/writing/validate-api-key");
         
         String apiKey = request.get("apiKey");
         boolean isValid = writingSubmissionService.validateApiKey(apiKey);
         
         Map<String, Object> result = Map.of(
             "valid", isValid,
             "message", isValid ? "API key is valid" : "API key is invalid or expired"
         );
         
         return ResponseEntity.ok(result);
     }
 
     /**
      * Re-grade a writing attempt.
      */
     @PostMapping("/regrade/{attemptId}")
     @Operation(summary = "Re-grade attempt", description = "Re-grade a completed writing attempt with AI")
     public ResponseEntity<Map<String, Object>> regradeAttempt(
             @PathVariable Long attemptId,
             Authentication authentication) {
         
         logger.info("📥 POST /api/writing/regrade/{}", attemptId);
         
         UUID userId = UUID.fromString(authentication.getName());
         Map<String, Object> result = writingSubmissionService.regradeAttempt(attemptId, userId);
         
         logger.info("✅ Re-grading started for attempt {}", attemptId);
         return ResponseEntity.ok(result);
     }
 }
```

</details>


---

<sub>⏱️ 16m 7s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\WritingSubmissionService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\WritingSubmissionService.java

<details>
<summary>330 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/WritingSubmissionService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/WritingSubmissionService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/WritingSubmissionService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/WritingSubmissionService.java
@@ -1,390 +1,390 @@
 package com.cramer.service;
 
 import com.cramer.dto.WritingReviewDTO;
 import com.cramer.dto.WritingSubmissionDTO;
 import com.cramer.entity.Section;
 import com.cramer.entity.TestAttempt;
 import com.cramer.entity.WritingSubmission;
 import com.cramer.repository.SectionRepository;
 import com.cramer.repository.TestAttemptRepository;
 import com.cramer.repository.WritingSubmissionRepository;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Service;
 import org.springframework.transaction.annotation.Transactional;
 
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.time.Duration;
 import java.time.OffsetDateTime;
 import java.util.*;
 import java.util.stream.Collectors;
 
 /**
  * Service for managing Writing test submissions and grading.
  */
 @Service
 public class WritingSubmissionService {
 
     private static final Logger logger = LoggerFactory.getLogger(WritingSubmissionService.class);
 
     private final WritingSubmissionRepository writingSubmissionRepository;
     private final TestAttemptRepository testAttemptRepository;
     private final SectionRepository sectionRepository;
     private final GeminiGradingService geminiGradingService;
     private final AsyncGradingService asyncGradingService;
 
     @Autowired
     public WritingSubmissionService(WritingSubmissionRepository writingSubmissionRepository,
                                     TestAttemptRepository testAttemptRepository,
                                     SectionRepository sectionRepository,
                                     GeminiGradingService geminiGradingService,
                                     AsyncGradingService asyncGradingService) {
         this.writingSubmissionRepository = writingSubmissionRepository;
         this.testAttemptRepository = testAttemptRepository;
         this.sectionRepository = sectionRepository;
         this.geminiGradingService = geminiGradingService;
         this.asyncGradingService = asyncGradingService;
     }
 
     /**
      * Save essay draft during test.
      */
     @Transactional
     public WritingSubmissionDTO saveDraft(Long attemptId, Integer taskNumber, String essayText, UUID userId) {
         TestAttempt attempt = testAttemptRepository.findById(attemptId)
             .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
         
         if (!attempt.getUserId().equals(userId)) {
             throw new IllegalArgumentException("Unauthorized access to attempt");
         }
         
         WritingSubmission submission = writingSubmissionRepository
             .findByAttemptIdAndTaskNumber(attemptId, taskNumber)
             .orElse(new WritingSubmission(attemptId, userId, taskNumber, essayText));
         
         submission.setEssayText(essayText);
         submission.setGradingStatus("PENDING");
         
         WritingSubmission saved = writingSubmissionRepository.save(submission);
         return toDTO(saved);
     }
 
     /**
      * Submit essays for grading (async background grading).
      * Also cleans up any stale IN_PROGRESS attempts for the same test.
      */
     @Transactional
     public Map<String, Object> submitForGrading(Long attemptId, Map<Integer, String> essays, UUID userId) {
         logger.info("Submitting writing essays for attempt {} by user {}", attemptId, userId);
         
         TestAttempt attempt = testAttemptRepository.findById(attemptId)
             .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
         
         if (!attempt.getUserId().equals(userId)) {
             throw new IllegalArgumentException("Unauthorized access to attempt");
         }
         
         // IMPORTANT: Cancel any other IN_PROGRESS attempts for the same test
         // This prevents "ghost" IN_PROGRESS attempts showing up in dashboard
         List<TestAttempt> otherInProgressAttempts = testAttemptRepository
             .findByUserIdAndExamSourceAndTestNumberAndSkillAndStatus(
                 userId, attempt.getExamSource(), attempt.getTestNumber(), attempt.getSkill(), "IN_PROGRESS"
             );
         
         for (TestAttempt otherAttempt : otherInProgressAttempts) {
             if (!otherAttempt.getId().equals(attemptId)) {
                 logger.info("Cancelling stale IN_PROGRESS attempt {} before submitting {}", otherAttempt.getId(), attemptId);
                 otherAttempt.setStatus("CANCELLED");
                 testAttemptRepository.save(otherAttempt);
             }
         }
         
         // Update attempt status
         attempt.setStatus("COMPLETED");
         attempt.setCompletedAt(OffsetDateTime.now());
         testAttemptRepository.save(attempt);
         
         // Save all submissions
         List<WritingSubmission> submissions = new ArrayList<>();
         for (Map.Entry<Integer, String> entry : essays.entrySet()) {
             Integer taskNumber = entry.getKey();
             String essayText = entry.getValue();
             
             WritingSubmission submission = writingSubmissionRepository
                 .findByAttemptIdAndTaskNumber(attemptId, taskNumber)
                 .orElse(new WritingSubmission(attemptId, userId, taskNumber, essayText));
             
             submission.setEssayText(essayText);
             submission.setSubmittedAt(OffsetDateTime.now());
             submission.setGradingStatus("PENDING");
             
             submissions.add(writingSubmissionRepository.save(submission));
         }
         
         // Start async grading via separate bean (required for @Async to work)
         asyncGradingService.gradeSubmissionsAsync(submissions, attempt, userId);
         
         Map<String, Object> result = new HashMap<>();
         result.put("attemptId", attemptId);
         result.put("status", "GRADING_STARTED");
         result.put("submissionCount", submissions.size());
         result.put("message", "Essays submitted. Grading in progress...");
         
         return result;
     }
 
     /**
      * Get grading status for an attempt.
      */
     public Map<String, Object> getGradingStatus(Long attemptId, UUID userId) {
         TestAttempt attempt = testAttemptRepository.findById(attemptId)
             .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
         
         if (!attempt.getUserId().equals(userId)) {
             throw new IllegalArgumentException("Unauthorized access");
         }
         
         List<WritingSubmission> submissions = writingSubmissionRepository.findByAttemptId(attemptId);
         
         boolean allCompleted = submissions.stream()
             .allMatch(s -> "COMPLETED".equals(s.getGradingStatus()) || "FAILED".equals(s.getGradingStatus()));
         
         boolean anyGrading = submissions.stream()
             .anyMatch(s -> "GRADING".equals(s.getGradingStatus()));
         
         boolean anyFailed = submissions.stream()
             .anyMatch(s -> "FAILED".equals(s.getGradingStatus()));
         
         Map<String, Object> status = new HashMap<>();
         status.put("attemptId", attemptId);
         status.put("totalTasks", submissions.size());
         status.put("completedTasks", submissions.stream()
             .filter(s -> "COMPLETED".equals(s.getGradingStatus())).count());
         status.put("failedTasks", submissions.stream()
             .filter(s -> "FAILED".equals(s.getGradingStatus())).count());
         
         if (allCompleted && !anyFailed) {
             status.put("status", "COMPLETED");
         } else if (allCompleted && anyFailed) {
             status.put("status", "PARTIAL_FAILURE");
         } else if (anyGrading) {
             status.put("status", "GRADING");
         } else {
             status.put("status", "PENDING");
         }
         
         return status;
     }
 
     /**
      * Get full writing review with all grading details.
      */
     @Transactional(readOnly = true)
     public WritingReviewDTO getWritingReview(Long attemptId, UUID userId) {
         TestAttempt attempt = testAttemptRepository.findById(attemptId)
             .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
         
         if (!attempt.getUserId().equals(userId)) {
             throw new IllegalArgumentException("Unauthorized access");
         }
         
         List<WritingSubmission> submissions = writingSubmissionRepository.findByAttemptId(attemptId);
         
         // Get task prompts
         List<Section> sections = sectionRepository.findByExamSourceAndTestNumberAndSkill(
             attempt.getExamSource(),
             Integer.parseInt(attempt.getTestNumber()),
             "writing"
         );
         
         WritingReviewDTO review = new WritingReviewDTO();
         review.setAttemptId(attemptId);
         review.setExamSource(attempt.getExamSource());
         review.setTestNumber(attempt.getTestNumber());
         review.setSkill("writing");
         review.setStatus(attempt.getStatus());
         review.setCompletedAt(attempt.getCompletedAt());
         
         // Calculate duration
         if (attempt.getStartedAt() != null && attempt.getCompletedAt() != null) {
             Duration duration = Duration.between(attempt.getStartedAt(), attempt.getCompletedAt());
             review.setDuration(duration.getSeconds());
         }
         
         // Build task reviews
         List<WritingReviewDTO.WritingTaskReviewDTO> taskReviews = new ArrayList<>();
         BigDecimal totalBand = BigDecimal.ZERO;
         int gradedCount = 0;
         
         for (WritingSubmission sub : submissions) {
             WritingReviewDTO.WritingTaskReviewDTO taskReview = new WritingReviewDTO.WritingTaskReviewDTO();
             taskReview.setTaskNumber(sub.getTaskNumber());
             taskReview.setEssayText(sub.getEssayText());
             taskReview.setWordCount(sub.getWordCount());
             taskReview.setGradingStatus(sub.getGradingStatus());
             taskReview.setOverallBand(sub.getOverallBand());
             taskReview.setBandScores(sub.getBandScores());
             taskReview.setAiFeedback(sub.getAiFeedback());
             taskReview.setSubmittedAt(sub.getSubmittedAt());
             taskReview.setGradedAt(sub.getGradedAt());
             taskReviews.add(taskReview);
             
             if (sub.getOverallBand() != null) {
                 totalBand = totalBand.add(sub.getOverallBand());
                 gradedCount++;
             }
         }
         review.setTasks(taskReviews);
         
         // Calculate overall band (IELTS writing is average of both tasks, with Task 2 counting more)
         if (gradedCount > 0) {
             // IELTS Writing: Task 1 = 1/3, Task 2 = 2/3
             BigDecimal overallBand = calculateWeightedOverallBand(submissions);
             review.setOverallBand(overallBand);
         }
         
         // Build prompts
         List<WritingReviewDTO.WritingTaskPromptDTO> prompts = new ArrayList<>();
         for (Section section : sections) {
             WritingReviewDTO.WritingTaskPromptDTO prompt = new WritingReviewDTO.WritingTaskPromptDTO();
             prompt.setTaskNumber(section.getPartNumber());
             prompt.setPromptText(section.getPassageText());
             prompt.setImageUrl(section.getDisplayContentUrl());
             prompts.add(prompt);
         }
         review.setPrompts(prompts);
         
         return review;
     }
 
     /**
      * Calculate weighted overall band score for IELTS Writing.
      * Task 1 = 1/3 weight, Task 2 = 2/3 weight
      */
     private BigDecimal calculateWeightedOverallBand(List<WritingSubmission> submissions) {
         BigDecimal task1Band = null;
         BigDecimal task2Band = null;
         
         for (WritingSubmission sub : submissions) {
             if (sub.getOverallBand() != null) {
                 if (sub.getTaskNumber() == 1) {
                     task1Band = sub.getOverallBand();
                 } else if (sub.getTaskNumber() == 2) {
                     task2Band = sub.getOverallBand();
                 }
             }
         }
         
         if (task1Band != null && task2Band != null) {
             // Weighted average: (Task1 * 1 + Task2 * 2) / 3
             BigDecimal weighted = task1Band.add(task2Band.multiply(BigDecimal.valueOf(2)))
                 .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
             // Round to nearest 0.5
             double rounded = Math.round(weighted.doubleValue() * 2) / 2.0;
             return BigDecimal.valueOf(rounded).setScale(1, RoundingMode.HALF_UP);
         } else if (task1Band != null) {
             return task1Band;
         } else if (task2Band != null) {
             return task2Band;
         }
         
         return null;
     }
 
     /**
      * Get submissions for an attempt.
      */
     public List<WritingSubmissionDTO> getSubmissions(Long attemptId, UUID userId) {
         TestAttempt attempt = testAttemptRepository.findById(attemptId)
             .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
         
         if (!attempt.getUserId().equals(userId)) {
             throw new IllegalArgumentException("Unauthorized access");
         }
         
         return writingSubmissionRepository.findByAttemptId(attemptId)
             .stream()
             .map(this::toDTO)
             .collect(Collectors.toList());
     }
 
     /**
      * Delete submissions for an attempt.
      */
     @Transactional
     public void deleteSubmissions(Long attemptId) {
         writingSubmissionRepository.deleteByAttemptId(attemptId);
     }
 
     /**
      * Validate Gemini API key.
      */
     public boolean validateApiKey(String apiKey) {
         return geminiGradingService.validateApiKey(apiKey);
     }
 
     /**
      * Re-grade a completed writing attempt.
      * This resets the grading status and triggers async grading again.
      */
     @Transactional
     public Map<String, Object> regradeAttempt(Long attemptId, UUID userId) {
         logger.info("Re-grading writing attempt {} for user {}", attemptId, userId);
         
         TestAttempt attempt = testAttemptRepository.findById(attemptId)
             .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
         
         if (!attempt.getUserId().equals(userId)) {
             throw new IllegalArgumentException("Unauthorized access to attempt");
         }
         
         if (!"COMPLETED".equals(attempt.getStatus())) {
             throw new IllegalArgumentException("Cannot re-grade an incomplete attempt");
         }
         
         List<WritingSubmission> submissions = writingSubmissionRepository.findByAttemptId(attemptId);
         if (submissions.isEmpty()) {
             throw new IllegalArgumentException("No submissions found for attempt");
         }
         
         // Reset grading status for all submissions
         for (WritingSubmission sub : submissions) {
             sub.setGradingStatus("PENDING");
             sub.setOverallBand(null);
             sub.setBandScores(null);
             sub.setAiFeedback(null);
             sub.setGradedAt(null);
             writingSubmissionRepository.save(sub);
         }
         
         // Trigger async grading
         asyncGradingService.gradeSubmissionsAsync(submissions, attempt, userId);
         
         Map<String, Object> result = new HashMap<>();
         result.put("attemptId", attemptId);
         result.put("status", "REGRADING_STARTED");
         result.put("submissionCount", submissions.size());
         result.put("message", "Re-grading in progress...");
         
         return result;
     }
 
     private WritingSubmissionDTO toDTO(WritingSubmission submission) {
         WritingSubmissionDTO dto = new WritingSubmissionDTO();
         dto.setId(submission.getId());
         dto.setAttemptId(submission.getAttemptId());
         dto.setTaskNumber(submission.getTaskNumber());
         dto.setEssayText(submission.getEssayText());
         dto.setWordCount(submission.getWordCount());
         dto.setGradingStatus(submission.getGradingStatus());
         dto.setOverallBand(submission.getOverallBand());
         dto.setBandScores(submission.getBandScores());
         dto.setAiFeedback(submission.getAiFeedback());
         dto.setSubmittedAt(submission.getSubmittedAt());
         dto.setGradedAt(submission.getGradedAt());
         return dto;
     }
 }
```

</details>


---

<sub>⏱️ 16m 9s</sub>

### 💬 Copilot

Now let me check the frontend API calls:

---

<sub>⏱️ 16m 12s</sub>

### ✅ `grep`

"writingApi|regrade|getGradingStatus" (E:\IT and Computer Knowledges\Cramer\frontend\src\api\backendApi.js)

<details>
<summary>22 matches</summary>

```
240-// ============================================
241-// WRITING APIs
242-// ============================================
243:export const writingApi = {
244-  // Save essay draft during test
245-  saveDraft: (attemptId, taskNumber, essayText) => 
246-    apiClient.post(`/writing/draft/${attemptId}?taskNumber=${taskNumber}`, essayText, {
--
252-    apiClient.post(`/writing/submit/${attemptId}`, { essays }),
253-  
254-  // Get grading status
255:  getGradingStatus: (attemptId) => 
256-    apiClient.get(`/writing/status/${attemptId}`),
257-  
258-  // Get full writing review with AI feedback
--
268-    apiClient.post('/writing/validate-api-key', { apiKey }),
269-
270-  // Re-grade a completed writing attempt
271:  regradeAttempt: (attemptId) =>
272:    apiClient.post(`/writing/regrade/${attemptId}`),
273-};
```

</details>


---

<sub>⏱️ 16m 14s</sub>

### 💬 Copilot

Now I understand the flow. Let me trace through the issues:

**Issues I've identified:**

1. **Regrading flow problem**: In `WritingResultPage.jsx`, when `handleRegrade` is called:
   - It sets `setReview(null)` and `setError(null)` 
   - But the `useEffect` for polling has dependencies `[attemptId, review, error]`
   - When `review` changes to `null`, the effect re-runs
   - **BUT**: The effect checks `if (review || error) return;` at the start - this should work
   - **PROBLEM**: After regrading starts, the status check may immediately return the OLD cached status

2. **Loading screen instability**: The polling logic depends on the `review` state being `null` to continue. When regrade is triggered, `review` is set to `null`, but there's a race condition where the old review data might persist.

3. **React StrictMode double-mount**: The useEffect can run twice in development mode, potentially causing double polling.

Let me look more closely at the polling logic:

---

<sub>⏱️ 16m 22s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx (lines 59-150)

<details>
<summary>86 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
@@ -59,92 +59,92 @@
     // Poll for grading status with improved reliability
     useEffect(() => {
         // Don't poll if we already have review data or there's an error
         if (review || error) return;
         
         let pollInterval;
         let isMounted = true;
         let consecutiveErrors = 0;
         let currentPollCount = 0;
         const MAX_CONSECUTIVE_ERRORS = 3;
         const MAX_POLL_COUNT = 120; // Max 6 minutes of polling (120 * 3 seconds)
 
         const checkStatus = async () => {
             if (!isMounted) return;
             
             try {
                 const statusRes = await writingApi.getGradingStatus(attemptId);
                 const status = statusRes.data.status;
                 
                 if (!isMounted) return;
                 
                 consecutiveErrors = 0; // Reset error counter on success
                 setGradingStatus(status);
                 currentPollCount++;
 
                 if (status === 'COMPLETED' || status === 'PARTIAL_FAILURE') {
                     clearInterval(pollInterval);
                     // Fetch review data
                     try {
                         const reviewRes = await writingApi.getWritingReview(attemptId);
                         if (isMounted) {
                             setReview(reviewRes.data);
                             setLoading(false);
                             setIsRegrading(false); // Reset regrading state
                         }
                     } catch (reviewErr) {
                         console.error('Error fetching review:', reviewErr);
                         if (isMounted) {
                             setError('Không thể tải kết quả chấm điểm. Vui lòng tải lại trang.');
                             setLoading(false);
                             setIsRegrading(false);
                         }
                     }
                 } else if (status === 'FAILED') {
                     clearInterval(pollInterval);
                     if (isMounted) {
                         setError('Chấm điểm thất bại. Vui lòng thử chấm lại.');
                         setLoading(false);
                         setIsRegrading(false);
                     }
                 }
             } catch (err) {
                 console.error('Error checking grading status:', err);
                 consecutiveErrors++;
                 
                 if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                     clearInterval(pollInterval);
                     if (isMounted) {
                         setError('Không thể kiểm tra trạng thái chấm điểm. Vui lòng tải lại trang.');
                         setLoading(false);
                         setIsRegrading(false);
                     }
                 }
             }
         };
 
         // Initial check
         checkStatus();
         
         // Start polling - every 3 seconds
         pollInterval = setInterval(() => {
             if (currentPollCount < MAX_POLL_COUNT) {
                 checkStatus();
             } else {
                 clearInterval(pollInterval);
                 if (isMounted && loading) {
                     setError('Quá thời gian chờ. Vui lòng tải lại trang để xem kết quả.');
                     setLoading(false);
                     setIsRegrading(false);
                 }
             }
         }, 3000);
         
         return () => {
             isMounted = false;
             clearInterval(pollInterval);
         };
     }, [attemptId, review, error]); // Only re-run if attemptId changes or review/error is set
 
     // Helpers
     const getTaskReview = useCallback((taskNumber) => {
         if (!review?.tasks) return null;
```

</details>


---

<sub>⏱️ 16m 24s</sub>

### 💬 Copilot

Now I see the issues clearly:

## Issues Found:

### 1. **Race condition in regrading flow**
When `handleRegrade` is called:
- It sets `loading=true`, `gradingStatus='PENDING'`, `review=null`, `error=null`
- This triggers the useEffect to restart polling
- **BUT** the backend immediately returns the old status (`COMPLETED`) because the submissions haven't been reset yet in the database (transactional timing issue)

### 2. **The polling useEffect depends on `loading` but doesn't include it in dependencies**
- Line 134: `if (isMounted && loading)` - but `loading` is not in the dependency array
- This causes stale closure issues

### 3. **No explicit "regrading" mode in polling**
- The polling doesn't distinguish between initial load and regrade
- Should use `isRegrading` state to force fresh polling

### 4. **Backend regrade endpoint resets submissions THEN triggers async grading**
- But the frontend might poll status BEFORE the async grading changes status to "GRADING"
- This can return "COMPLETED" instead of "PENDING"

Let me fix these issues:

---

<sub>⏱️ 16m 36s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx

<details>
<summary>11 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
@@ -38,6 +38,7 @@
     const [gradingStatus, setGradingStatus] = useState('PENDING');
     const [error, setError] = useState(null);
     const [activeTask, setActiveTask] = useState(1);
+    const [pollKey, setPollKey] = useState(0); // Used to force restart polling
     
     // UI state
     const [expandedScores, setExpandedScores] = useState({});
```

</details>


---

<sub>⏱️ 17m 4s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx

<details>
<summary>78 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
@@ -68,6 +68,9 @@
         let currentPollCount = 0;
         const MAX_CONSECUTIVE_ERRORS = 3;
         const MAX_POLL_COUNT = 120; // Max 6 minutes of polling (120 * 3 seconds)
+        
+        // Add initial delay for regrade to let backend update status
+        const initialDelay = isRegrading ? 2000 : 0;
 
         const checkStatus = async () => {
             if (!isMounted) return;
@@ -123,26 +126,24 @@
             }
         };
 
-        // Initial check
-        checkStatus();
-        
-        // Start polling - every 3 seconds
-        pollInterval = setInterval(() => {
-            if (currentPollCount < MAX_POLL_COUNT) {
-                checkStatus();
-            } else {
-                clearInterval(pollInterval);
-                if (isMounted && loading) {
-                    setError('Quá thời gian chờ. Vui lòng tải lại trang để xem kết quả.');
-                    setLoading(false);
-                    setIsRegrading(false);
+        // Start polling with optional initial delay
+        const startPolling = () => {
+            // Initial check
+            checkStatus();
+            
+            // Start polling - every 3 seconds
+            pollInterval = setInterval(() => {
+                if (currentPollCount < MAX_POLL_COUNT) {
+                    checkStatus();
+                } else {
+                    clearInterval(pollInterval);
+                    if (isMounted) {
+                        setError('Quá thời gian chờ. Vui lòng tải lại trang để xem kết quả.');
+                        setLoading(false);
+                        setIsRegrading(false);
+                    }
                 }
-            }
-        }, 3000);
-        
-        return () => {
-            isMounted = false;
-            clearInterval(pollInterval);
+            }, 3000);
         };
     }, [attemptId, review, error]); // Only re-run if attemptId changes or review/error is set
 
@@ -144,7 +145,22 @@
             isMounted = false;
             clearInterval(pollInterval);
         };
-    }, [attemptId, review, error]); // Only re-run if attemptId changes or review/error is set
+
+        if (initialDelay > 0) {
+            const delayTimeout = setTimeout(startPolling, initialDelay);
+            return () => {
+                isMounted = false;
+                clearTimeout(delayTimeout);
+                clearInterval(pollInterval);
+            };
+        } else {
+            startPolling();
+            return () => {
+                isMounted = false;
+                clearInterval(pollInterval);
+            };
+        }
+    }, [attemptId, review, error, pollKey]); // pollKey forces restart when regrading
 
     // Helpers
     const getTaskReview = useCallback((taskNumber) => {
```

</details>


---

<sub>⏱️ 17m 14s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx

<details>
<summary>14 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
@@ -300,7 +300,9 @@
             setError(null);  // Reset error to allow polling
             
             await writingApi.regradeAttempt(attemptId);
-            // The useEffect polling will pick up the new grading status
+            
+            // Increment pollKey to force useEffect to restart with fresh state
+            setPollKey(prev => prev + 1);
         } catch (err) {
             console.error('Failed to start re-grading:', err);
             setError('Không thể bắt đầu chấm lại. Vui lòng thử lại sau.');
```

</details>


---

<sub>⏱️ 17m 16s</sub>

### 💬 Copilot

Now let me also check the backend to ensure the regrade status is properly set BEFORE starting async grading:

---

<sub>⏱️ 17m 18s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\WritingSubmissionService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\WritingSubmissionService.java (lines 328-375)

<details>
<summary>44 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/WritingSubmissionService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/WritingSubmissionService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/WritingSubmissionService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/WritingSubmissionService.java
@@ -328,48 +328,48 @@
     /**
      * Re-grade a completed writing attempt.
      * This resets the grading status and triggers async grading again.
      */
     @Transactional
     public Map<String, Object> regradeAttempt(Long attemptId, UUID userId) {
         logger.info("Re-grading writing attempt {} for user {}", attemptId, userId);
         
         TestAttempt attempt = testAttemptRepository.findById(attemptId)
             .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
         
         if (!attempt.getUserId().equals(userId)) {
             throw new IllegalArgumentException("Unauthorized access to attempt");
         }
         
         if (!"COMPLETED".equals(attempt.getStatus())) {
             throw new IllegalArgumentException("Cannot re-grade an incomplete attempt");
         }
         
         List<WritingSubmission> submissions = writingSubmissionRepository.findByAttemptId(attemptId);
         if (submissions.isEmpty()) {
             throw new IllegalArgumentException("No submissions found for attempt");
         }
         
         // Reset grading status for all submissions
         for (WritingSubmission sub : submissions) {
             sub.setGradingStatus("PENDING");
             sub.setOverallBand(null);
             sub.setBandScores(null);
             sub.setAiFeedback(null);
             sub.setGradedAt(null);
             writingSubmissionRepository.save(sub);
         }
         
         // Trigger async grading
         asyncGradingService.gradeSubmissionsAsync(submissions, attempt, userId);
         
         Map<String, Object> result = new HashMap<>();
         result.put("attemptId", attemptId);
         result.put("status", "REGRADING_STARTED");
         result.put("submissionCount", submissions.size());
         result.put("message", "Re-grading in progress...");
         
         return result;
     }
 
     private WritingSubmissionDTO toDTO(WritingSubmission submission) {
         WritingSubmissionDTO dto = new WritingSubmissionDTO();
```

</details>


---

<sub>⏱️ 17m 20s</sub>

### 💬 Copilot

The backend looks correct - it sets status to "PENDING" before triggering async grading. The issue is the `@Transactional` annotation - the database changes won't be committed until the method returns, but `asyncGradingService.gradeSubmissionsAsync` is called WITHIN the transaction.

This means when the frontend polls, the transaction might not have committed yet. Let me fix this by ensuring the transaction commits before async grading starts:

---

<sub>⏱️ 17m 35s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\WritingSubmissionService.java.**

E:\IT and Computer Knowledges\Cramer\backend\src\main\java\com\cramer\service\WritingSubmissionService.java

<details>
<summary>25 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/WritingSubmissionService.java b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/WritingSubmissionService.java
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/WritingSubmissionService.java
+++ b/E:/IT and Computer Knowledges/Cramer/backend/src/main/java/com/cramer/service/WritingSubmissionService.java
@@ -356,8 +356,11 @@
             sub.setBandScores(null);
             sub.setAiFeedback(null);
             sub.setGradedAt(null);
-            writingSubmissionRepository.save(sub);
         }
+        // Batch save all submissions
+        List<WritingSubmission> savedSubmissions = writingSubmissionRepository.saveAll(submissions);
+        // Force flush to ensure database is updated before async call
+        writingSubmissionRepository.flush();
         
         // Trigger async grading
         asyncGradingService.gradeSubmissionsAsync(submissions, attempt, userId);
@@ -359,8 +362,8 @@
             writingSubmissionRepository.save(sub);
         }
         
-        // Trigger async grading
-        asyncGradingService.gradeSubmissionsAsync(submissions, attempt, userId);
+        // Trigger async grading (runs after transaction commits due to @Async proxy)
+        asyncGradingService.gradeSubmissionsAsync(savedSubmissions, attempt, userId);
         
         Map<String, Object> result = new HashMap<>();
         result.put("attemptId", attemptId);
```

</details>


---

<sub>⏱️ 17m 37s</sub>

### 💬 Copilot

Now let me also add `isRegrading` to the useEffect dependencies so it properly detects regrading state:

---

<sub>⏱️ 17m 39s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx (lines 59-70)

<details>
<summary>15 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
@@ -59,12 +59,12 @@
 
     // Poll for grading status with improved reliability
     useEffect(() => {
         // Don't poll if we already have review data or there's an error
         if (review || error) return;
         
         let pollInterval;
         let isMounted = true;
         let consecutiveErrors = 0;
         let currentPollCount = 0;
         const MAX_CONSECUTIVE_ERRORS = 3;
         const MAX_POLL_COUNT = 120; // Max 6 minutes of polling (120 * 3 seconds)
```

</details>


---

<sub>⏱️ 17m 42s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx (lines 155-165)

<details>
<summary>15 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
@@ -155,11 +155,11 @@
             };
         } else {
             startPolling();
             return () => {
                 isMounted = false;
                 clearInterval(pollInterval);
             };
         }
     }, [attemptId, review, error, pollKey]); // pollKey forces restart when regrading
 
     // Helpers
```

</details>


---

<sub>⏱️ 17m 47s</sub>

### ✅ `edit`

**edit the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx

<details>
<summary>12 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
@@ -160,7 +160,7 @@
                 clearInterval(pollInterval);
             };
         }
-    }, [attemptId, review, error, pollKey]); // pollKey forces restart when regrading
+    }, [attemptId, review, error, pollKey, isRegrading]); // pollKey and isRegrading force restart when regrading
 
     // Helpers
     const getTaskReview = useCallback((taskNumber) => {
```

</details>


---

<sub>⏱️ 17m 49s</sub>

### 💬 Copilot

Now let me also check the WritingTestPage to see if there are similar issues when initially submitting essays:

---

<sub>⏱️ 17m 52s</sub>

### ✅ `grep`

"submitForGrading|navigate.*result" (E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingTestPage.jsx)

<details>
<summary>11 matches</summary>

```
204-
205-        try {
206-            setIsSubmitting(true);
207-
208-            // Submit essays for grading
209:            await writingApi.submitForGrading(attempt.id, essays);
210-
211-            // Navigate to review page
212-            navigate(`/test/writing/review/${attempt.id}`);
213-        } catch (err) {
214-            console.error('Error submitting writing test:', err);
```

</details>


---

<sub>⏱️ 17m 54s</sub>

### 💬 Copilot

Good. This navigation is fine - it immediately navigates to the review page which then polls for status.

Let me verify the final state of the changes. Let me also check for any console.log statements that could help with debugging:

---

<sub>⏱️ 17m 57s</sub>

### ✅ `view`

**view the file at E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx.**

E:\IT and Computer Knowledges\Cramer\frontend\src\pages\WritingResultPage.jsx (lines 35-55)

<details>
<summary>24 lines</summary>

```diff

diff --git a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
index 0000000..0000000 100644
--- a/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
+++ b/E:/IT and Computer Knowledges/Cramer/frontend/src/pages/WritingResultPage.jsx
@@ -35,21 +35,21 @@
     // Core state
     const [review, setReview] = useState(null);
     const [loading, setLoading] = useState(true);
     const [gradingStatus, setGradingStatus] = useState('PENDING');
     const [error, setError] = useState(null);
     const [activeTask, setActiveTask] = useState(1);
     const [pollKey, setPollKey] = useState(0); // Used to force restart polling
     
     // UI state
     const [expandedScores, setExpandedScores] = useState({});
     const [scoresBarCollapsed, setScoresBarCollapsed] = useState(false);
     const [expandedSections, setExpandedSections] = useState({
         corrections: true,
         paragraphs: false,
         sampleBandPlus: false,
         sampleBand9: false,
         wordAnalysis: false,
     });
     const [selectedItemId, setSelectedItemId] = useState(null);
     const [isRegrading, setIsRegrading] = useState(false);
```

</details>


---

<sub>⏱️ 17m 59s</sub>

### 💬 Copilot

Good. Now let me summarize all the fixes made:

## Summary of Issues Found and Fixed

### **Issue 1: Race condition when regrading**
- **Problem**: When clicking "Chấm lại", the useEffect polling would restart but could still receive the old "COMPLETED" status before the backend finished resetting.
- **Fix**: 
  - Added `pollKey` state that increments when regrading starts, forcing useEffect to restart with fresh state
  - Added 2-second initial delay when `isRegrading` is true to give backend time to commit the transaction
  - Added `isRegrading` to useEffect dependencies

### **Issue 2: Backend transaction timing**
- **Problem**: The `@Transactional` method was calling async grading inside the transaction, so database changes might not be visible when frontend polls.
- **Fix**: 
  - Changed individual `save()` calls to `saveAll()` for batch saving
  - Added explicit `flush()` call before triggering async grading to ensure database is updated

### **Issue 3: Stale closure in polling timeout check**
- **Problem**: The polling checked `loading` state but it wasn't in the dependency array, causing stale closure.
- **Fix**: Removed the redundant `loading` check since the effect already exits early if `review` or `error` exists.

### **Files Changed:**
1. **Frontend**: `frontend/src/pages/WritingResultPage.jsx`
   - Added `pollKey` state
   - Added initial delay for regrading
   - Fixed useEffect dependencies
   - Increment `pollKey` in `handleRegrade`

2. **Backend**: `backend/src/main/java/com/cramer/service/WritingSubmissionService.java`
   - Used `saveAll()` instead of individual saves
   - Added `flush()` before async grading call

To rebuild and test:
```powershell
cd backend && .\mvnw.cmd clean compile
```

---

<sub>⏱️ 21m 57s</sub>

### 👤 User

One last question to ask: In later times when I want to deploy my project to a domain and employ an AI credit system for marking test, how will the APi and user's usage managed if I keep using Google API? (Explain in simple terms)

---

<sub>⏱️ 22m 3s</sub>

### 💬 Copilot

Great question! Here's a simple breakdown of how to manage AI credits when deploying:

## Current Setup (User's Own API Key)
Users provide their **own** Gemini API key → Google bills **them** directly → You have no control/visibility over their usage.

---

## Centralized Credit System (Your API Key)

### How it works:
```
User buys credits → Uses your app → Your backend calls Gemini with YOUR API key → You track usage → Deduct credits
```

### Key components you'll need:

| Component | Purpose |
|-----------|---------|
| **Credits table** | Store user's credit balance (e.g., 100 credits = 10 essays) |
| **Usage log table** | Track each API call (user, tokens used, timestamp) |
| **Your Gemini API key** | Stored securely in backend `.env`, never exposed |
| **Pre-check middleware** | Verify user has enough credits BEFORE calling API |

### Simple flow:
1. **User clicks "Grade Essay"**
2. Backend checks: Does user have ≥1 credit? 
   - ❌ No → Return "Please buy credits"
   - ✅ Yes → Call Gemini API
3. After grading: Deduct 1 credit, log usage
4. Return result to user

### Pricing example:
- Gemini 2.5 Flash: ~$0.001-0.002 per essay
- You charge: $0.05 per essay (5 credits = $0.25)
- Your margin: ~95%

---

## Google API Billing (on your side)
- Enable billing at [Google Cloud Console](https://console.cloud.google.com)
- Set **budget alerts** (e.g., notify at $50, $100)
- Set **quota limits** to prevent runaway costs
- Monitor usage in Cloud Console dashboard

---

## TL;DR
Keep using Google's API, but switch from **user keys** to **your key** + a credit system. You control costs, users pay you, and you pay Google. The database tracks everything.

---

<sub>⏱️ 23m 33s</sub>

### ℹ️ Info

Share Command Usage:
/share [path] - Share session to a markdown file (defaults to current directory)
/share file [path] - Share session to a markdown file at the specified path
/share gist - Create a secret GitHub gist with the session content

Examples:
/share - Share to copilot-session-\<id\>.md in current directory
/share ~/sessions/my-session.md - Share to specific file path
/share gist - Create a secret gist (requires login)

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
