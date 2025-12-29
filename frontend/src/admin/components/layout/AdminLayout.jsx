import React, { useState } from 'react';
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
    const location = useLocation();

    const toggleSidebar = () => {
        setSidebarCollapsed(!sidebarCollapsed);
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

    return (
        <ToastProvider>
            <div className="admin-root">
                {/* Header - fixed at top, full width */}
                <AdminHeader
                    breadcrumbs={getBreadcrumbs()}
                    collapsed={sidebarCollapsed}
                />

                {/* Sidebar - below header */}
                <AdminSidebar
                    collapsed={sidebarCollapsed}
                    onToggle={toggleSidebar}
                />

                {/* Main content area */}
                <div className={`admin-main ${sidebarCollapsed ? 'admin-main--collapsed' : ''}`}>
                    <AdminHeader
                        breadcrumbs={getBreadcrumbs()}
                        collapsed={sidebarCollapsed}
                    />
                    <div className={`admin-content ${isFullWidthPage ? 'admin-content--full' : ''}`}>
                        <Outlet />
                    </div>
                </div>
            </div>
        </ToastProvider>
    );
}
