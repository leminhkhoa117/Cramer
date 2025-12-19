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
            'editor': 'editor',
        };

        paths.forEach((path, index) => {
            breadcrumbs.push({
                label: pathLabels[path] || path,
                isLast: index === paths.length - 1,
            });
        });

        return breadcrumbs;
    };

    return (
        <ToastProvider>
            <div className="admin-root">
                <AdminSidebar
                    collapsed={sidebarCollapsed}
                    onToggle={toggleSidebar}
                />
                <div className={`admin-main ${sidebarCollapsed ? 'admin-main--collapsed' : ''}`}>
                    <AdminHeader
                        breadcrumbs={getBreadcrumbs()}
                        collapsed={sidebarCollapsed}
                    />
                    <div className="admin-content">
                        <Outlet />
                    </div>
                </div>
            </div>
        </ToastProvider>
    );
}
