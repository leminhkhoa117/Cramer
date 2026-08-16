import { Navigate } from 'react-router-dom';

/**
 * ContentListPage - Legacy entry point.
 * Redirects to the new SetListPage which handles the Test Set hierarchy.
 * 
 * @since 2025-12-26 - Updated to redirect to new hierarchy
 */
export default function ContentListPage() {
    return <Navigate to="/admin/content/sets" replace />;
}
