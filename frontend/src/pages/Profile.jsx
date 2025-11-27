import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { profileApi } from '../api/backendApi';
import { showErrorToast, showSuccessToast } from '../utils/toast.js';
import { FiUser, FiEdit, FiCamera, FiKey, FiSave, FiXCircle } from 'react-icons/fi';
import '../css/ProfilePage.css';
import FullPageLoader from '../components/FullPageLoader';
import { supabase } from '../api/supabaseClient';
import { v4 as uuidv4 } from 'uuid';
import { motion } from 'framer-motion';
import UploadImageModal from '../components/UploadImageModal';
import ChangePasswordModal from '../components/ChangePasswordModal';

const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: {
        y: 0,
        opacity: 1,
        transition: { type: 'spring', stiffness: 100 }
    }
};

const ProfilePage = () => {
    const { user, profileLoading, updateProfileContext } = useAuth();
    const [isEditing, setIsEditing] = useState(false);
    const [profileData, setProfileData] = useState(null);
    const [editedProfile, setEditedProfile] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    const avatarFileRef = useRef(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isChangePasswordModalOpen, setIsChangePasswordModalOpen] = useState(false);
    const [pageBackgroundUrl, setPageBackgroundUrl] = useState(null);
    const [heroBackgroundUrl, setHeroBackgroundUrl] = useState(null);
    const [cardTheme, setCardTheme] = useState('light'); // 'light' or 'dark'

    // Helper to determine if background is light or dark
    const getImageBrightness = async (imageUrl) => {
        if (!imageUrl) return 'light'; // Default to light theme
        
        return new Promise((resolve, reject) => {
            const img = new Image();
            img.crossOrigin = "Anonymous";
            img.src = imageUrl;
            img.onload = () => {
                const canvas = document.createElement('canvas');
                canvas.width = img.width;
                canvas.height = img.height;
                const ctx = canvas.getContext('2d');
                ctx.drawImage(img, 0, 0);
                const imageData = ctx.getImageData(0, 0, img.width, img.height);
                const data = imageData.data;
                let r, g, b, avg;
                let colorSum = 0;

                for(let x = 0, len = data.length; x < len; x += 4) {
                    r = data[x];
                    g = data[x+1];
                    b = data[x+2];
                    avg = Math.floor((r + g + b) / 3);
                    colorSum += avg;
                }

                const brightness = Math.floor(colorSum / (img.width * img.height));
                resolve(brightness < 128 ? 'dark' : 'light');
            };
            img.onerror = (err) => {
                console.error("Failed to load image for brightness check:", err);
                resolve('light'); // Default to light on error
            };
        });
    };

    useEffect(() => {
        const fetchProfile = async () => {
            if (!user?.id) return;
            try {
                setLoading(true);
                const response = await profileApi.getById(user.id);
                const fullProfile = { ...response.data, email: user.email };
                setProfileData(fullProfile);
                setEditedProfile(fullProfile);
                if (fullProfile.pageBackgroundUrl) {
                    setPageBackgroundUrl(fullProfile.pageBackgroundUrl);
                    const theme = await getImageBrightness(fullProfile.pageBackgroundUrl);
                    setCardTheme(theme);
                }
                if (fullProfile.heroBackgroundUrl) {
                    setHeroBackgroundUrl(fullProfile.heroBackgroundUrl);
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

    const handleEditToggle = () => {
        if (!isEditing) {
            setEditedProfile({ ...profileData });
        }
        setIsEditing(!isEditing);
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setEditedProfile((prev) => ({ ...prev, [name]: value }));
    };

    const handleSave = async (extraData = {}) => {
        console.log('Attempting to save profile with extra data:', extraData);
        const profileToSave = { ...editedProfile, ...extraData };
        console.log('Final profile data to save:', profileToSave);
        try {
            const response = await profileApi.update(user.id, profileToSave);
            console.log('Backend API response:', response);
            const updatedProfile = { ...response.data, email: user.email };
            setProfileData(updatedProfile);
            updateProfileContext(response.data);

            // Update local state for dynamic backgrounds
            if (extraData.pageBackgroundUrl) {
                setPageBackgroundUrl(extraData.pageBackgroundUrl);
            }
            if (extraData.heroBackgroundUrl) {
                setHeroBackgroundUrl(extraData.heroBackgroundUrl);
            }

            showSuccessToast('Cập nhật thông tin thành công!');
            setIsEditing(false);
        } catch (err) {
            console.error('Error saving profile:', err);
            showErrorToast('Lỗi khi cập nhật thông tin.');
        } finally {
            setIsUploading(false); // Ensure uploading state is reset
        }
    };

    const parseSupabaseUrl = (url) => {
        try {
            const urlPath = new URL(url).pathname;
            const publicIndex = urlPath.indexOf('/public/');
            if (publicIndex === -1) return null;

            // Extract the path after '/public/'
            const pathAfterPublic = urlPath.substring(publicIndex + '/public/'.length);
            
            // The bucket name is now always 'userImages'
            const bucketName = 'userImages';

            // The rest of the path is the file path within the userImages bucket, including the folder
            // Example: "avatars/2d843ce6-..." or "hero-backgrounds/abc-..."
            const filePath = pathAfterPublic.substring(bucketName.length + 1); // +1 for the '/'

            if (!filePath) return null;

            return {
                bucket: bucketName,
                path: filePath,
            };
        } catch (error) {
            console.error('Could not parse Supabase URL:', error);
            return null;
        }
    };

    const uploadHeroBackground = async (file) => {
        if (!file) return;
        setIsUploading(true);

        if (profileData?.heroBackgroundUrl) {
            const oldImage = parseSupabaseUrl(profileData.heroBackgroundUrl);
            if (oldImage) {
                try {
                    console.log(`Attempting to delete old hero background: ${oldImage.path}`);
                    await supabase.storage.from(oldImage.bucket).remove([oldImage.path]);
                } catch (error) {
                    console.error('Failed to delete old hero background, continuing with upload...', error);
                }
            }
        }

        try {
            const fileExt = file.name.split('.').pop();
            const fileName = `${user.id}-${uuidv4()}.${fileExt}`;
            const filePath = `hero-backgrounds/${fileName}`; // Use sub-folder in userImages bucket

            const { error: uploadError } = await supabase.storage.from('userImages').upload(filePath, file, {
                cacheControl: '3600',
                upsert: false
            });
            if (uploadError) throw uploadError;

            const { data } = supabase.storage.from('userImages').getPublicUrl(filePath);
            if (!data.publicUrl) throw new Error('Could not get public URL for hero background.');

            await handleSave({ heroBackgroundUrl: data.publicUrl });

        } catch (error) {
            console.error('An error occurred during hero background upload:', error);
            showErrorToast(error.message || 'Failed to upload hero background.');
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
                    console.log(`Attempting to delete old page background: ${oldImage.path}`);
                    await supabase.storage.from('userImages').remove([oldImage.path]); // Use 'userImages'
                } catch (error) {
                    console.error('Failed to delete old page background, continuing with upload...', error);
                }
            }
        }

        try {
            const fileExt = file.name.split('.').pop();
            const fileName = `${user.id}-${uuidv4()}.${fileExt}`;
            const filePath = `backgrounds/${fileName}`; // Use sub-folder in userImages bucket

            const { error: uploadError } = await supabase.storage.from('userImages').upload(filePath, file, {
                cacheControl: '3600',
                upsert: false
            });
            if (uploadError) throw uploadError;

            const { data } = supabase.storage.from('userImages').getPublicUrl(filePath);
            if (!data.publicUrl) throw new Error('Could not get public URL for page background.');

            // After successful upload, determine theme and save
            const theme = await getImageBrightness(URL.createObjectURL(file));
            setCardTheme(theme);
            await handleSave({ pageBackgroundUrl: data.publicUrl });

        } catch (error) {
            console.error('An error occurred during page background upload:', error);
            showErrorToast(error.message || 'Failed to upload page background.');
            setIsUploading(false);
        }
    };

    const uploadAvatar = async (file) => {
        if (!file) return;

        setIsUploading(true);

        // Delete old avatar before uploading new one
        if (profileData?.avatarUrl) {
            const oldImage = parseSupabaseUrl(profileData.avatarUrl);
            if (oldImage) {
                try {
                    console.log(`Attempting to delete old avatar: ${oldImage.path}`);
                    await supabase.storage.from('userImages').remove([oldImage.path]); // Use 'userImages'
                } catch (error) {
                    console.error('Failed to delete old avatar, continuing with upload...', error);
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
                        // Create a new file with a modified name to reflect it's a jpeg
                        const newFile = new File([blob], file.name.replace(/\.[^/.]+$/, ".jpg"), {
                            type: 'image/jpeg',
                            lastModified: Date.now()
                        });
                        resolve(newFile);
                    } else {
                        reject(new Error('Canvas to Blob conversion failed'));
                    }
                }, 'image/jpeg', 0.9); // Quality set to 90%
            };
            img.onerror = (error) => {
                reject(error);
            };
        });

        try {
            const resizedFile = await resizeImage(file, 200, 200);
            console.log('Resized file ready for upload:', resizedFile.name);
            
            const fileExt = resizedFile.name.split('.').pop();
            const fileName = `${user.id}-${uuidv4()}.${fileExt}`;
            const filePath = `avatars/${fileName}`; // Use sub-folder in userImages bucket

            const { error: uploadError } = await supabase.storage.from('userImages').upload(filePath, resizedFile, {
                cacheControl: '3600',
                upsert: false
            });

            if (uploadError) {
                throw uploadError;
            }
            
            const { data } = supabase.storage.from('userImages').getPublicUrl(filePath);
            const publicUrl = data.publicUrl;

            if (!publicUrl) {
                throw new Error('Could not get public URL for avatar.');
            }

            await handleSave({ avatarUrl: publicUrl });
        } catch (error) {
            console.error('An error occurred during avatar upload process:', error);
            showErrorToast(error.message || 'Failed to upload avatar.');
            setIsUploading(false);
        }
    };

    const handleAvatarClick = () => setIsModalOpen(true);
    const handleCloseModal = () => setIsModalOpen(false);
    const openChangePasswordModal = () => setIsChangePasswordModalOpen(true);
    const closeChangePasswordModal = () => setIsChangePasswordModalOpen(false);

    const handleImageUpdate = (type, file) => {
        switch (type) {
            case 'avatar':
                uploadAvatar(file);
                break;
            case 'hero':
                uploadHeroBackground(file);
                break;
            case 'page':
                uploadPageBackground(file);
                break;
            default:
                console.warn('Unknown image update type:', type);
        }
    };
    
    const handleAvatarUpload = (event) => {
        const file = event.target.files[0];
        console.log('File selected:', file);
        if (file) {
            uploadAvatar(file);
        }
    };


    const getInitials = (name) => {
        if (!name) return '';
        const nameParts = name.split(' ');
        if (nameParts.length > 1) {
            return `${nameParts[0][0]}${nameParts[nameParts.length - 1][0]}`.toUpperCase();
        }
        return name.substring(0, 2).toUpperCase();
    };

    if (loading || profileLoading) {
        return <FullPageLoader message="Đang tải trang cá nhân..." />;
    }

    if (error) {
        return <div className="dash-error container">{error}</div>;
    }

    const pageStyle = pageBackgroundUrl ? { 
        backgroundImage: `url(${pageBackgroundUrl})` 
    } : {};
    
    const heroStyle = heroBackgroundUrl ? {
        backgroundImage: `url(${heroBackgroundUrl})`
    } : {};

    return (
        <div className={`dash profile-page ${cardTheme}-card-theme`} style={pageStyle}>
            <div className="page-background-blur"></div>
            <UploadImageModal isOpen={isModalOpen} onClose={handleCloseModal} onConfirm={handleImageUpdate} />
            <ChangePasswordModal isOpen={isChangePasswordModalOpen} onClose={closeChangePasswordModal} />

            <input type="file" ref={avatarFileRef} onChange={handleAvatarUpload} hidden accept="image/png, image/jpeg" />
            
            <section className="profile-hero">
                <div className="container">
                    <div className="profile-hero-content-wrapper" style={heroStyle}>
                        <motion.div className="profile-header" variants={itemVariants}>
                            <div className="profile-avatar-wrapper">
                                {isUploading ? (
                                    <div className="profile-avatar-uploading" />
                                ) : profileData?.avatarUrl ? (
                                    <img src={profileData.avatarUrl} alt="Avatar" className="profile-avatar" />
                                ) : (
                                    <div className="profile-avatar">
                                        {getInitials(profileData?.fullName || user?.email)}
                                    </div>
                                )}
                                <button className="profile-avatar-edit-btn" aria-label="Change avatar" onClick={handleAvatarClick} disabled={isUploading}>
                                    <FiCamera />
                                </button>
                            </div>
                            <h1 className="profile-name">{profileData?.fullName || user.email}</h1>
                        </motion.div>
                    </div>
                </div>
            </section>

            <main className="container profile-main-content">
                <motion.section 
                    className="profile-card profile-card--personal-info"
                    variants={itemVariants}
                    initial="hidden"
                    animate="visible"
                >
                    <div className="profile-card-header">
                        <h3><FiUser /><span>Thông tin cá nhân</span></h3>
                        <button className="btn-edit" onClick={handleEditToggle}>
                            <FiEdit /> {isEditing ? 'Hủy' : 'Chỉnh sửa'}
                        </button>
                    </div>

                    {isEditing ? (
                        <div className="profile-edit-form">
                            <div className="profile-grid">
                                <div className="form-group">
                                    <label htmlFor="fullName">Họ và Tên</label>
                                    <input type="text" id="fullName" name="fullName" value={editedProfile.fullName || ''} onChange={handleInputChange} />
                                </div>
                                <div className="form-group">
                                    <label htmlFor="email">Email</label>
                                    <input type="email" id="email" name="email" value={editedProfile.email || ''} disabled />
                                </div>
                                <div className="form-group">
                                    <label htmlFor="phoneNumber">Số điện thoại</label>
                                    <input type="text" id="phoneNumber" name="phoneNumber" value={editedProfile.phoneNumber || ''} onChange={handleInputChange} />
                                </div>
                                <div className="form-group">
                                    <label htmlFor="address">Địa chỉ</label>
                                    <input type="text" id="address" name="address" value={editedProfile.address || ''} onChange={handleInputChange} />
                                </div>
                            </div>
                            <div className="profile-actions">
                                <button className="btn-action btn-cancel" onClick={() => setIsEditing(false)}><FiXCircle />Hủy</button>
                                <button className="btn-action btn-save" onClick={() => handleSave()}><FiSave />Lưu thay đổi</button>
                            </div>
                        </div>
                    ) : (
                        <div className="profile-grid">
                            <div className="info-item"><label>Họ và Tên</label><p>{profileData?.fullName || <span className="not-updated">Chưa cập nhật</span>}</p></div>
                            <div className="info-item"><label>Email</label><p>{profileData?.email}</p></div>
                            <div className="info-item"><label>Số điện thoại</label><p>{profileData?.phoneNumber || <span className="not-updated">Chưa cập nhật</span>}</p></div>
                            <div className="info-item"><label>Địa chỉ</label><p>{profileData?.address || <span className="not-updated">Chưa cập nhật</span>}</p></div>
                        </div>
                    )}
                </motion.section>

                <motion.section 
                    className="profile-card profile-card--security"
                    variants={itemVariants}
                    initial="hidden"
                    animate="visible"
                >
                    <div className="profile-card-header">
                        <h3><FiKey /><span>Bảo mật</span></h3>
                    </div>
                    <div className="security-item" onClick={openChangePasswordModal} style={{cursor: 'pointer'}}>
                        <div className="security-item-content">
                            <h4>Thay đổi mật khẩu</h4>
                            <p>Thay đổi mật khẩu đăng nhập của bạn</p>
                        </div>
                        {/* <FiChevronRight className="security-item-arrow" /> */}
                    </div>
                </motion.section>
            </main>
        </div>
    );
};

export default ProfilePage;
