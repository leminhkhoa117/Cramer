import React from 'react';
import { Link } from 'react-router-dom';
import { FiChevronRight, FiExternalLink } from 'react-icons/fi';
import { useProfileStore } from '../../../stores';

/**
 * AdminHeader - Header bar cho admin panel
 * Hiển thị breadcrumb và user info
 */
export default function AdminHeader({ breadcrumbs = [], collapsed }) {
    const profile = useProfileStore(state => state.profile);

    // Get display name
    const displayName = profile?.fullName || profile?.full_name || profile?.username || 'Admin';

    // Get initials for avatar
    const getInitials = (name) => {
        if (!name) return 'A';
        const parts = name.split(' ');
        if (parts.length >= 2) {
            return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
        }
        return name.substring(0, 2).toUpperCase();
    };

    return (
        <header className={`admin-header ${collapsed ? 'admin-header--collapsed' : ''}`}>
            {/* Breadcrumb */}
            <nav className="admin-header__breadcrumb" aria-label="Breadcrumb">
                {breadcrumbs.map((crumb, index) => (
                    <React.Fragment key={index}>
                        {index > 0 && (
                            <span className="admin-header__breadcrumb-separator">
                                <FiChevronRight size={14} />
                            </span>
                        )}
                        {crumb.isLast ? (
                            <span className="admin-header__breadcrumb-current">
                                {crumb.label}
                            </span>
                        ) : (
                            <span className="admin-header__breadcrumb-item">
                                {crumb.label}
                            </span>
                        )}
                    </React.Fragment>
                ))}
            </nav>

            {/* Actions */}
            <div className="admin-header__actions">
                {/* Link to user site */}
                <Link to="/" className="admin-header__back-link" target="_blank">
                    <FiExternalLink size={16} />
                    <span>Xem trang User</span>
                </Link>

                {/* User info */}
                <div className="admin-header__user">
                    <div className="admin-header__user-avatar">
                        {profile?.avatarUrl ? (
                            <img
                                src={profile.avatarUrl}
                                alt={displayName}
                                style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }}
                            />
                        ) : (
                            getInitials(displayName)
                        )}
                    </div>
                    <span className="admin-header__user-name">{displayName}</span>
                </div>
            </div>
        </header>
    );
}
