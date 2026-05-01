import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore, useProfileStore } from '../../stores';

/**
 * ADMIN_USER_IDS - Danh sách user IDs được phép truy cập admin
 * TODO: Chuyển sang environment variable sau khi connect backend
 */
const ADMIN_USER_IDS = [
    // Thêm user IDs của admin vào đây
    // Ví dụ: '550e8400-e29b-41d4-a716-446655440001'
];

/**
 * AdminRouteGuard - Component bảo vệ routes admin
 * Chỉ cho phép users có ID trong danh sách admin truy cập
 * 
 * Hiện tại đang ở chế độ DEV - cho phép tất cả users đã đăng nhập
 * để dễ dàng test giao diện
 */
export default function AdminRouteGuard({ children }) {
    const user = useAuthStore(state => state.user);
    const loading = useAuthStore(state => state.loading);
    const profile = useProfileStore(state => state.profile);
    const profileLoading = useProfileStore(state => state.loading);

    // Nếu đang loading, hiển thị loading state
    if (loading || profileLoading) {
        return (
            <div className="admin-root" style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                minHeight: '100vh',
                background: '#0F0F23',
                color: '#F8FAFC'
            }}>
                <div style={{ textAlign: 'center' }}>
                    <div style={{
                        width: 40,
                        height: 40,
                        border: '3px solid rgba(139, 92, 246, 0.3)',
                        borderTopColor: '#8B5CF6',
                        borderRadius: '50%',
                        animation: 'spin 1s linear infinite',
                        margin: '0 auto 16px'
                    }} />
                    <style>{`
            @keyframes spin {
              to { transform: rotate(360deg); }
            }
          `}</style>
                    <p style={{ margin: 0, fontSize: '0.875rem', color: '#94A3B8' }}>
                        Đang xác thực...
                    </p>
                </div>
            </div>
        );
    }

    // Nếu chưa đăng nhập, redirect về login
    if (!user) {
        return <Navigate to="/login" replace />;
    }

    // ======================================================================
    // DEV MODE: Only allow in development environment
    // Uses VITE_DEV_ADMIN_BYPASS environment variable
    // ======================================================================
    const isDevBypass = import.meta.env.VITE_DEV_ADMIN_BYPASS === 'true' && import.meta.env.DEV;

    if (isDevBypass) {
        // In dev mode with bypass enabled, allow all authenticated users
        console.warn('⚠️ Admin bypass enabled - DEV MODE ONLY');
        return children ?? <Outlet />;
    }

    // ======================================================================
    // PRODUCTION MODE: Kiểm tra admin user ID
    // ======================================================================

    const isAdmin = ADMIN_USER_IDS.includes(user.id);

    const isAdminFromProfile = profile?.isAdmin === true || profile?.is_admin === true;

    if (!isAdmin && !isAdminFromProfile) {
        return <Navigate to="/" replace />;
    }

    return children ?? <Outlet />;
}
/**
 * useIsAdmin - Hook kiểm tra user có phải admin không
 * Có thể sử dụng ở bất kỳ đâu trong app
 */
export function useIsAdmin() {
    const user = useAuthStore(state => state.user);
    const profile = useProfileStore(state => state.profile);

    if (!user) return false;

    // DEV MODE - only with explicit bypass and in dev environment
    const isDevBypass = import.meta.env.VITE_DEV_ADMIN_BYPASS === 'true' && import.meta.env.DEV;
    if (isDevBypass) return true;

    // PRODUCTION MODE
    const isAdminFromList = ADMIN_USER_IDS.includes(user.id);
    const isAdminFromProfile = profile?.isAdmin === true || profile?.is_admin === true;

    return isAdminFromList || isAdminFromProfile;
}
