import React from 'react';
import { NavLink } from 'react-router-dom';
import {
    FiHome,
    FiUsers,
    FiDollarSign,
    FiFileText,
    FiSettings,
    FiChevronLeft,
    FiChevronRight,
    FiList,
    FiPieChart,
    FiEdit3,
    FiArrowLeft
} from 'react-icons/fi';

/**
 * AdminSidebar - Sidebar navigation cho admin panel
 * Hiển thị menu với icons và hỗ trợ collapsed state
 */
export default function AdminSidebar({ collapsed, onToggle }) {
    const menuSections = [
        {
            title: 'Tổng quan',
            items: [
                {
                    to: '/admin',
                    icon: <FiHome size={20} />,
                    label: 'Dashboard',
                    end: true
                },
            ]
        },
        {
            title: 'Quản lý',
            items: [
                {
                    to: '/admin/users',
                    icon: <FiUsers size={20} />,
                    label: 'Người dùng'
                },
                {
                    to: '/admin/finance',
                    icon: <FiDollarSign size={20} />,
                    label: 'Tài chính',
                    end: true
                },
                {
                    to: '/admin/content',
                    icon: <FiFileText size={20} />,
                    label: 'Nội dung đề thi',
                    end: true
                },
            ]
        },
        {
            title: 'Tài chính',
            items: [
                {
                    to: '/admin/finance/transactions',
                    icon: <FiList size={20} />,
                    label: 'Giao dịch'
                },
                {
                    to: '/admin/finance/reports',
                    icon: <FiPieChart size={20} />,
                    label: 'Báo cáo'
                },
            ]
        },
        {
            title: 'Nội dung',
            items: [
                {
                    to: '/admin/content/editor',
                    icon: <FiEdit3 size={20} />,
                    label: 'Soạn đề thi'
                },
            ]
        },
    ];

    return (
        <aside className={`admin-sidebar ${collapsed ? 'admin-sidebar--collapsed' : ''}`}>
            {/* Logo */}
            <div className="admin-sidebar__logo">
                <img
                    src="/pictures/logo/Icon.png"
                    alt="Cramer Logo"
                    className="admin-sidebar__logo-img"
                />
                {!collapsed && (
                    <span className="admin-sidebar__logo-text">cramer</span>
                )}
            </div>

            {/* Navigation */}
            <nav className="admin-sidebar__nav">
                {menuSections.map((section, sectionIndex) => (
                    <div key={sectionIndex} className="admin-sidebar__section">
                        <div className="admin-sidebar__section-title">
                            {section.title}
                        </div>
                        <ul className="admin-sidebar__menu">
                            {section.items.map((item, itemIndex) => (
                                <li key={itemIndex} className="admin-sidebar__menu-item">
                                    <NavLink
                                        to={item.to}
                                        end={item.end}
                                        className={({ isActive }) =>
                                            `admin-sidebar__menu-link ${isActive ? 'admin-sidebar__menu-link--active' : ''}`
                                        }
                                        title={collapsed ? item.label : undefined}
                                    >
                                        <span className="admin-sidebar__menu-icon">
                                            {item.icon}
                                        </span>
                                        <span className="admin-sidebar__menu-text">
                                            {item.label}
                                        </span>
                                    </NavLink>
                                </li>
                            ))}
                        </ul>
                    </div>
                ))}
            </nav>

            {/* Footer with collapse button and back to user link */}
            <div className="admin-sidebar__footer">
                <NavLink
                    to="/"
                    className="admin-sidebar__menu-link"
                    style={{ marginBottom: '8px' }}
                >
                    <span className="admin-sidebar__menu-icon">
                        <FiArrowLeft size={20} />
                    </span>
                    <span className="admin-sidebar__menu-text">
                        Về trang User
                    </span>
                </NavLink>

                <button
                    className="admin-sidebar__collapse-btn"
                    onClick={onToggle}
                    title={collapsed ? 'Mở rộng' : 'Thu gọn'}
                >
                    {collapsed ? <FiChevronRight size={20} /> : <FiChevronLeft size={20} />}
                </button>
            </div>
        </aside>
    );
}
