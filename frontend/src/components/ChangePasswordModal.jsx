import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FiX, FiKey, FiEye, FiEyeOff } from 'react-icons/fi';
import { supabase } from '../api/supabaseClient';
import { showSuccessToast, showErrorToast } from '../utils/toast';
import '../css/ChangePasswordModal.css';

const modalVariants = {
    hidden: { opacity: 0, y: -50 },
    visible: { opacity: 1, y: 0 },
    exit: { opacity: 0, y: 50 }
};

const ChangePasswordModal = ({ isOpen, onClose }) => {
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [isUpdating, setIsUpdating] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const handleUpdatePassword = async (e) => {
        e.preventDefault();
        if (newPassword.length < 6) {
            showErrorToast('Mật khẩu phải có ít nhất 6 ký tự.');
            return;
        }
        if (newPassword !== confirmPassword) {
            showErrorToast('Mật khẩu xác nhận không khớp.');
            return;
        }

        setIsUpdating(true);
        try {
            const { error } = await supabase.auth.updateUser({
                password: newPassword,
            });

            if (error) {
                throw error;
            }

            showSuccessToast('Cập nhật mật khẩu thành công!');
            setNewPassword('');
            setConfirmPassword('');
            onClose();
        } catch (error) {
            console.error('Error updating password:', error);
            showErrorToast(error.message || 'Lỗi khi cập nhật mật khẩu.');
        } finally {
            setIsUpdating(false);
        }
    };

    const handleClose = () => {
        setNewPassword('');
        setConfirmPassword('');
        onClose();
    };

    return (
        <AnimatePresence>
            {isOpen && (
                <motion.div
                    className="cp-modal-backdrop"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    onClick={handleClose}
                >
                    <motion.div
                        className="cp-modal-content"
                        variants={modalVariants}
                        initial="hidden"
                        animate="visible"
                        exit="exit"
                        transition={{ type: 'spring', stiffness: 250, damping: 25 }}
                        onClick={(e) => e.stopPropagation()}
                    >
                        <form onSubmit={handleUpdatePassword}>
                            <div className="cp-modal-header">
                                <h3><FiKey /> Thay đổi mật khẩu</h3>
                                <button type="button" className="cp-modal-close-btn" onClick={handleClose}>
                                    <FiX />
                                </button>
                            </div>
                            
                            <div className="cp-modal-body">
                                <div className="form-group-pw">
                                    <label htmlFor="newPassword">Mật khẩu mới</label>
                                    <div className="password-input-wrapper">
                                        <input
                                            id="newPassword"
                                            type={showPassword ? 'text' : 'password'}
                                            value={newPassword}
                                            onChange={(e) => setNewPassword(e.target.value)}
                                            required
                                            minLength={6}
                                            placeholder="••••••••"
                                        />
                                        <button type="button" onClick={() => setShowPassword(!showPassword)}>
                                            {showPassword ? <FiEyeOff /> : <FiEye />}
                                        </button>
                                    </div>
                                </div>
                                <div className="form-group-pw">
                                    <label htmlFor="confirmPassword">Xác nhận mật khẩu mới</label>
                                     <div className="password-input-wrapper">
                                        <input
                                            id="confirmPassword"
                                            type={showConfirmPassword ? 'text' : 'password'}
                                            value={confirmPassword}
                                            onChange={(e) => setConfirmPassword(e.target.value)}
                                            required
                                            minLength={6}
                                            placeholder="••••••••"
                                        />
                                        <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)}>
                                            {showConfirmPassword ? <FiEyeOff /> : <FiEye />}
                                        </button>
                                    </div>
                                </div>
                            </div>

                            <div className="cp-modal-footer">
                                <button type="button" className="btn-action btn-cancel" onClick={handleClose} disabled={isUpdating}>Hủy</button>
                                <button type="submit" className="btn-action btn-save" disabled={isUpdating}>
                                    {isUpdating ? 'Đang cập nhật...' : 'Lưu thay đổi'}
                                </button>
                            </div>
                        </form>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
};

export default ChangePasswordModal;
