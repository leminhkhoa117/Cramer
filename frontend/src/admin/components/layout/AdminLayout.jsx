import React, { useState, useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import AdminSidebar from '../AdminSidebar';
import AdminHeader from './AdminHeader';
import { ToastProvider } from '../Toast';
import '../../css/admin.css';

/**
 * AdminLayout - Layout wrapper cho tất cả các trang admin
 * Bao gồm Sidebar, Header và Content area
 */
export default function AdminLayout() {
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
    const [sidebarOpen, setSidebarOpen] = useState(false); // Mobile sidebar state
    const [isMobileLayout, setIsMobileLayout] = useState(false);
    const location = useLocation();

    // Close mobile sidebar on route change
    useEffect(() => {
        setSidebarOpen(false);
    }, [location.pathname]);

    // Close mobile sidebar on escape key
    useEffect(() => {
        const handleEscape = (e) => {
            if (e.key === 'Escape') setSidebarOpen(false);
        };
        document.addEventListener('keydown', handleEscape);
        return () => document.removeEventListener('keydown', handleEscape);
    }, []);

    useEffect(() => {
        const mediaQuery = window.matchMedia('(max-width: 1024px)');
        const handleChange = () => setIsMobileLayout(mediaQuery.matches);

        handleChange();
        window.addEventListener('resize', handleChange);

        if (mediaQuery.addEventListener) {
            mediaQuery.addEventListener('change', handleChange);
            return () => {
                mediaQuery.removeEventListener('change', handleChange);
                window.removeEventListener('resize', handleChange);
            };
        }

        mediaQuery.addListener(handleChange);
        return () => {
            mediaQuery.removeListener(handleChange);
            window.removeEventListener('resize', handleChange);
        };
    }, []);

    const toggleSidebar = () => {
        setSidebarCollapsed(!sidebarCollapsed);
    };

    const toggleMobileSidebar = () => {
        setSidebarOpen(!sidebarOpen);
    };

    // Generate breadcrumb từ pathname
    const getBreadcrumbs = () => {
        const paths = location.pathname.split('/').filter(Boolean);
        const breadcrumbs = [];

        const pathLabels = {
            'admin': 'Admin',
            'dashboard': 'Tổng quan',
            'users': 'Người dùng',
            'finance': 'Tài chính',
            'content': 'Nội dung',
            'transactions': 'Giao dịch',
            'reports': 'Báo cáo',
            'editor': 'Soạn đề',
            'sets': 'Bộ đề',
            'hashtags': 'Hashtag',
            'generate': 'Tạo AI',
        };

        paths.forEach((path, index) => {
            breadcrumbs.push({
                label: pathLabels[path] || path,
                isLast: index === paths.length - 1,
            });
        });

        return breadcrumbs;
    };

    // Full-width pages that should have no padding
    const isFullWidthPage = location.pathname.includes('/content/generate') ||
        location.pathname.includes('/content/editor') ||
        location.pathname.includes('/content/tests') ||
        location.pathname.includes('/ai-studio');
    const mainStyle = isMobileLayout
        ? { width: '100%', maxWidth: '100%', marginLeft: 0 }
        : undefined;

    return (
        <ToastProvider>
            <div className="admin-root">
                {/* Mobile overlay */}
                {sidebarOpen && (
                    <div
                        className="admin-sidebar-overlay"
                        onClick={() => setSidebarOpen(false)}
                        aria-hidden="true"
                    />
                )}

                {/* Sidebar - below header */}
                <AdminSidebar
                    collapsed={sidebarCollapsed}
                    onToggle={toggleSidebar}
                    mobileOpen={sidebarOpen}
                />

                {/* Main content area */}
                <div
                    className={`admin-main ${sidebarCollapsed ? 'admin-main--collapsed' : ''}`}
                    style={mainStyle}
                >
                    <AdminHeader
                        breadcrumbs={getBreadcrumbs()}
                        collapsed={sidebarCollapsed}
                        onMenuClick={toggleMobileSidebar}
                    />
                    <div className={`admin-content ${isFullWidthPage ? 'admin-content--full' : ''}`}>
                        <Outlet />
                    </div>
                </div>
            </div>
        </ToastProvider>
    );
}
