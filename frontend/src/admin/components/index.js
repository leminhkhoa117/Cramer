/**
 * Admin Components Index
 * Clean exports for all admin components
 */

// Layout
export { AdminLayout, AdminHeader } from './layout';

// Core components
export { default as AdminSidebar } from './AdminSidebar';
export { default as AdminRouteGuard } from './AdminRouteGuard';

// Reusable components
export { default as DataTable } from './DataTable';
export { default as MetricCard } from './MetricCard';
export { default as StatusBadge, AccountStatusBadge, SubscriptionBadge, SubscriptionStatusBadge, TransactionStatusBadge } from './StatusBadge';
export { default as ConfirmModal } from './ConfirmModal';
export { default as CreateTestModal } from './CreateTestModal';
export { ToastProvider, useToast } from './Toast';
