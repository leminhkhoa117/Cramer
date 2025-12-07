import React, { useState } from 'react';
import { FiKey, FiEye, FiEyeOff } from 'react-icons/fi';
import { supabase } from '../api/supabaseClient';
import { showSuccessToast, showErrorToast } from '../utils/toast';
import { useAuth } from '../contexts/AuthContext';
import BaseModal from './common/BaseModal';
import '../css/ChangePasswordModal.css';

/**
 * ChangePasswordModal - Change user password
 * 
 * @param {boolean} isOpen - Modal visibility
 * @param {function} onClose - Close handler
 */
const ChangePasswordModal = ({ isOpen, onClose }) => {
  const { user } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const handleUpdatePassword = async (e) => {
    e.preventDefault();
    if (newPassword.length < 6) {
      showErrorToast('Mật khẩu mới phải có ít nhất 6 ký tự.');
      return;
    }
    if (newPassword !== confirmPassword) {
      showErrorToast('Mật khẩu xác nhận không khớp.');
      return;
    }

    setIsUpdating(true);
    try {
      // 1. Re-authenticate with current password
      const { error: signInError } = await supabase.auth.signInWithPassword({
        email: user.email,
        password: currentPassword,
      });

      if (signInError) {
        throw new Error('Mật khẩu hiện tại không đúng.');
      }

      // 2. Update password
      const { error: updateError } = await supabase.auth.updateUser({
        password: newPassword,
      });

      if (updateError) {
        throw updateError;
      }

      showSuccessToast('Cập nhật mật khẩu thành công!');
      handleClose();
    } catch (error) {
      console.error('Error updating password:', error);
      showErrorToast(error.message || 'Lỗi khi cập nhật mật khẩu.');
    } finally {
      setIsUpdating(false);
    }
  };

  const handleClose = () => {
    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    onClose();
  };

  return (
    <BaseModal
      isOpen={isOpen}
      onClose={handleClose}
      title={
        <span className="cp-title-icon">
          <FiKey /> Thay đổi mật khẩu
        </span>
      }
      showCloseButton={true}
      footer={
        <>
          <button 
            type="button" 
            className="cm-btn cm-btn--secondary" 
            onClick={handleClose} 
            disabled={isUpdating}
          >
            Hủy
          </button>
          <button 
            type="submit" 
            form="change-password-form"
            className="cm-btn cm-btn--primary" 
            disabled={isUpdating}
          >
            {isUpdating ? (
              <span className="cm-loading">Đang cập nhật...</span>
            ) : 'Lưu thay đổi'}
          </button>
        </>
      }
    >
      <form id="change-password-form" onSubmit={handleUpdatePassword} className="cm-form">
        <div className="cm-form-group">
          <label htmlFor="currentPassword" className="cm-form-label">Mật khẩu hiện tại</label>
          <div className="cp-password-wrapper">
            <input
              id="currentPassword"
              type={showCurrentPassword ? 'text' : 'password'}
              className="cm-form-input cp-password-input"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
              placeholder="••••••••"
            />
            <button 
              type="button" 
              className="cp-toggle-btn"
              onClick={() => setShowCurrentPassword(!showCurrentPassword)}
              aria-label={showCurrentPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            >
              {showCurrentPassword ? <FiEyeOff /> : <FiEye />}
            </button>
          </div>
        </div>

        <div className="cm-form-group">
          <label htmlFor="newPassword" className="cm-form-label">Mật khẩu mới</label>
          <div className="cp-password-wrapper">
            <input
              id="newPassword"
              type={showPassword ? 'text' : 'password'}
              className="cm-form-input cp-password-input"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
              minLength={6}
              placeholder="••••••••"
            />
            <button 
              type="button" 
              className="cp-toggle-btn"
              onClick={() => setShowPassword(!showPassword)}
              aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            >
              {showPassword ? <FiEyeOff /> : <FiEye />}
            </button>
          </div>
        </div>

        <div className="cm-form-group">
          <label htmlFor="confirmPassword" className="cm-form-label">Xác nhận mật khẩu mới</label>
          <div className="cp-password-wrapper">
            <input
              id="confirmPassword"
              type={showConfirmPassword ? 'text' : 'password'}
              className="cm-form-input cp-password-input"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              minLength={6}
              placeholder="••••••••"
            />
            <button 
              type="button" 
              className="cp-toggle-btn"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            >
              {showConfirmPassword ? <FiEyeOff /> : <FiEye />}
            </button>
          </div>
        </div>
      </form>
    </BaseModal>
  );
};

export default ChangePasswordModal;
