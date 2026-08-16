import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore, useProfileStore } from '../../stores';

/**
 * Extra admin user IDs from the environment, comma-separated.
 * The profile.isAdmin flag from the backend stays the source of truth;
 * this list only adds local overrides.
 */
const EXTRA_ADMIN_USER_IDS = (import.meta.env.VITE_ADMIN_USER_IDS || '')
    .split(',')
    .map((id) => id.trim())
    .filter(Boolean);

/**
 * AdminRouteGuard - Component bảo vệ routes admin.
 * Chỉ cho phép users có ID trong danh sách admin truy cập.
 */
export default function AdminRouteGuard({ children }) {
    const location = useLocation();
    const user = useAuthStore(state => state.user);
    const loading = useAuthStore(state => state.loading);
    const profile = useProfileStore(state => state.profile);
    const profileLoading = useProfileStore(state => state.loading);

    // DEV MODE: Only allow in development environment
    // Uses VITE_DEV_ADMIN_BYPASS environment variable
    const isDevBypass = import.meta.env.VITE_DEV_ADMIN_BYPASS === 'true' && import.meta.env.DEV;

    if (isDevBypass) {
        console.warn('Admin bypass enabled - DEV MODE ONLY');
        return children ?? <Outlet />;
    }

    // Nếu đang loading, hiển thị loading state
    if (loading || profileLoading) {
        return (
            <div className="admin-root admin-loading" style={{ minHeight: '100vh' }}>
                <div style={{ textAlign: 'center' }}>
                    <div className="spinner" style={{ width: 40, height: 40, borderWidth: '3px', margin: '0 auto 16px' }} />
                    <p style={{ margin: 0, fontSize: '0.875rem', color: '#94A3B8' }}>
                        Đang xác thực...
                    </p>
                </div>
            </div>
        );
    }

    // Nếu chưa đăng nhập, redirect về login
    if (!user) {
        return <Navigate to="/login" replace state={{ from: location }} />;
    }

    // PRODUCTION MODE: Kiểm tra admin user ID
    const isAdminFromList = EXTRA_ADMIN_USER_IDS.includes(user.id);
    const isAdminFromProfile = profile?.isAdmin === true || profile?.is_admin === true;

    if (!isAdminFromList && !isAdminFromProfile) {
        return <Navigate to="/" replace />;
    }

    return children ?? <Outlet />;
}

/**
 * useIsAdmin - Hook kiểm tra user có phải admin không.
 * Có thể sử dụng ở bất kỳ đâu trong app.
 */
export function useIsAdmin() {
    const user = useAuthStore(state => state.user);
    const profile = useProfileStore(state => state.profile);

    if (!user) return false;

    // DEV MODE - only with explicit bypass and in dev environment
    const isDevBypass = import.meta.env.VITE_DEV_ADMIN_BYPASS === 'true' && import.meta.env.DEV;
    if (isDevBypass) return true;

    // PRODUCTION MODE
    const isAdminFromList = EXTRA_ADMIN_USER_IDS.includes(user.id);
    const isAdminFromProfile = profile?.isAdmin === true || profile?.is_admin === true;

    return isAdminFromList || isAdminFromProfile;
}
