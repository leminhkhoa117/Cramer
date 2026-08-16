import { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

import {
    FiDownload,
    FiEye,
    FiExternalLink,
    FiX,
    FiRefreshCw
} from 'react-icons/fi';
import DataTable from '../../components/DataTable';
import { TransactionStatusBadge } from '../../components/StatusBadge';
import { formatCurrency } from '../../utils/formatUtils';
import { TRANSACTION_TYPES, TRANSACTION_STATUSES } from '../../utils/constants';
import adminApi from '../../api/adminApi';
import { useToast } from '../../components/Toast';
import { exportToExcel } from '../../utils/exportUtils';
import '../../css/pages/finance/TransactionHistoryPage.css';

export default function TransactionHistoryPage() {
    const navigate = useNavigate();
    const { showToast } = useToast();

    // State
    const [transactions, setTransactions] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [pagination, setPagination] = useState({
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0
    });
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [typeFilter, setTypeFilter] = useState('ALL');
    const [selectedTransaction, setSelectedTransaction] = useState(null);
    const [isExporting, setIsExporting] = useState(false);

    // Server-side stats from the overview endpoint (accurate totals across ALL transactions)
    const [serverStats, setServerStats] = useState({
        totalRevenue: 0,          // Total PAID amount (all transactions, not just current page)
        pendingTransactions: 0,   // Count of PENDING transactions
        isLoading: true
    });

    // Fetch transactions from API
    const fetchTransactions = useCallback(async () => {
        setIsLoading(true);
        setError(null);
        try {
            const response = await adminApi.finance.getTransactions({
                page: pagination.page,
                size: pagination.size,
                status: statusFilter !== 'ALL' ? statusFilter : null,
                type: typeFilter !== 'ALL' ? typeFilter : null
            });

            setTransactions(response.content || []);
            setPagination(prev => ({
                ...prev,
                totalElements: response.totalElements || 0,
                totalPages: response.totalPages || 0
            }));
        } catch (err) {
            console.error('Error fetching transactions:', err);
            setError('Không thể tải danh sách giao dịch');
            setTransactions([]);
        } finally {
            setIsLoading(false);
        }
    }, [pagination.page, pagination.size, statusFilter, typeFilter]);

    // Fetch on mount and when filters change
    useEffect(() => {
        fetchTransactions();
    }, [fetchTransactions]);

    // Fetch server-side stats from the overview endpoint (once on mount)
    // This provides accurate totals across ALL transactions, not just the current page
    const fetchServerStats = useCallback(async () => {
        try {
            // Use 'all' or a very long period to get all-time stats
            // The overview endpoint returns totalRevenue, pendingTransactions, etc.
            const overview = await adminApi.finance.getOverview('year');
            setServerStats({
                totalRevenue: overview.totalRevenue || 0,
                pendingTransactions: overview.pendingTransactions || 0,
                isLoading: false
            });
        } catch (err) {
            console.error('Error fetching server stats:', err);
            setServerStats(prev => ({ ...prev, isLoading: false }));
        }
    }, []);

    // Fetch server stats on mount (only once, as these are aggregate totals)
    useEffect(() => {
        fetchServerStats();
    }, [fetchServerStats]);

    // Export to Excel
    const handleExportExcel = async () => {
        setIsExporting(true);
        try {
            const exportData = await adminApi.finance.getExportData();
            if (exportData && exportData.length > 0) {
                exportToExcel(exportData, 'transactions', 'Giao dịch');
            } else {
                showToast('Không có dữ liệu để xuất', 'warning');
            }
        } catch (err) {
            console.error('Error exporting:', err);
            showToast('Lỗi khi xuất file Excel', 'error');
        } finally {
            setIsExporting(false);
        }
    };

    // Format date
    const formatDate = (dateString) => {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    // Get type label
    const getTypeLabel = (type) => {
        const typeObj = TRANSACTION_TYPES.find(t => t.value === type);
        return typeObj ? typeObj.label : type;
    };

    // Handle view detail
    const handleViewDetail = (txn) => {
        setSelectedTransaction(txn);
    };

    // Close drawer
    const closeDrawer = () => {
        setSelectedTransaction(null);
    };

    // Table columns
    const columns = [
        {
            key: 'order_code',
            label: 'Mã đơn',
            sortable: true,
            render: (code) => (
                <span className="order-code">#{code}</span>
            ),
        },
        {
            key: 'username',
            label: 'Khách hàng',
            sortable: true,
            render: (username, txn) => (
                <div className="customer-cell">
                    <span className="customer-cell__name">@{username || 'N/A'}</span>
                </div>
            ),
        },
        {
            key: 'type',
            label: 'Loại',
            sortable: true,
            render: (type) => (
                <span className={`transaction-type transaction-type--${type.toLowerCase()}`}>
                    {getTypeLabel(type)}
                </span>
            ),
        },
        {
            key: 'product_name',
            label: 'Sản phẩm',
            sortable: true,
        },
        {
            key: 'amount',
            label: 'Số tiền',
            sortable: true,
            render: (amount) => (
                <span className={`amount-cell ${amount < 0 ? 'amount-cell--negative' : ''}`}>
                    {amount >= 0 ? '+' : ''}{formatCurrency(amount)}
                </span>
            ),
        },
        {
            key: 'status',
            label: 'Trạng thái',
            sortable: true,
            render: (status) => <TransactionStatusBadge status={status} />,
        },
        {
            key: 'created_at',
            label: 'Thời gian',
            sortable: true,
            render: (date) => formatDate(date),
        },
        {
            key: 'actions',
            label: '',
            width: '50px',
            render: (_, txn) => (
                <button
                    className="action-btn"
                    onClick={(e) => {
                        e.stopPropagation();
                        handleViewDetail(txn);
                    }}
                    title="Xem chi tiết"
                >
                    <FiEye size={16} />
                </button>
            ),
        },
    ];

    // Calculate summary stats - use server-side stats for accurate totals
    // NOTE: The amounts (totalAmount, pendingAmount) come from the server-side overview endpoint
    // which calculates across ALL transactions, not just the current page of 20 items.
    // Only the "on this page" counts come from the paginated transactions array.
    const stats = useMemo(() => {
        // Count transactions on current page by status (for display purposes)
        const paidOnPage = transactions.filter(t => t.status === 'PAID').length;
        const pendingOnPage = transactions.filter(t => t.status === 'PENDING').length;

        return {
            // Total count comes from pagination (server-side count)
            total: pagination.totalElements,
            // Use server-side stats for accurate revenue totals (not limited to current page)
            totalAmount: serverStats.totalRevenue,
            // Pending count from server stats (accurate total)
            pendingCount: serverStats.pendingTransactions,
            // Stats loading state
            statsLoading: serverStats.isLoading,
            // Page-level counts (just for the label showing current page context)
            paidOnPage,
            pendingOnPage,
        };
    }, [transactions, pagination.totalElements, serverStats]);

    // Filters component
    const FiltersComponent = (
        <>
            <select
                className="filter-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
            >
                <option value="ALL">Tất cả trạng thái</option>
                {TRANSACTION_STATUSES.map(status => (
                    <option key={status.value} value={status.value}>{status.label}</option>
                ))}
            </select>
            <select
                className="filter-select"
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
            >
                <option value="ALL">Tất cả loại</option>
                {TRANSACTION_TYPES.map(type => (
                    <option key={type.value} value={type.value}>{type.label}</option>
                ))}
            </select>
        </>
    );

    // Actions component
    const ActionsComponent = (
        <>
            <button
                className="admin-btn admin-btn--secondary"
                onClick={fetchTransactions}
                disabled={isLoading}
            >
                <FiRefreshCw size={16} className={isLoading ? 'spinning' : ''} />
                <span>Làm mới</span>
            </button>
            <button
                className="admin-btn admin-btn--secondary"
                onClick={handleExportExcel}
                disabled={isExporting}
            >
                <FiDownload size={16} />
                <span>{isExporting ? 'Đang xuất...' : 'Xuất Excel'}</span>
            </button>
        </>
    );

    return (
        <div className="admin-page transaction-history-page">
            <div className="admin-page__header">
                <h1 className="admin-page__title">Lịch sử Giao dịch</h1>
                <p className="admin-page__subtitle">
                    Xem và quản lý tất cả các giao dịch trên hệ thống
                </p>
            </div>

            {/* Summary Stats - Using server-side aggregated data for accuracy */}
            <div className="transaction-stats">
                <div className="transaction-stat">
                    <span className="transaction-stat__value">{stats.total}</span>
                    <span className="transaction-stat__label">Tổng giao dịch</span>
                </div>
                <div className="transaction-stat transaction-stat--success">
                    <span className="transaction-stat__value">
                        {stats.statsLoading ? '...' : formatCurrency(stats.totalAmount)}
                    </span>
                    <span className="transaction-stat__label">Tổng doanh thu (đã thanh toán)</span>
                </div>
                <div className="transaction-stat transaction-stat--warning">
                    <span className="transaction-stat__value">
                        {stats.statsLoading ? '...' : stats.pendingCount}
                    </span>
                    <span className="transaction-stat__label">Giao dịch đang chờ</span>
                </div>
            </div>

            {/* Data Table */}
            {error && (
                <div className="admin-error-message">
                    {error}
                    <button onClick={fetchTransactions}>Thử lại</button>
                </div>
            )}

            <DataTable
                columns={columns}
                data={transactions}
                searchPlaceholder="Tìm theo mã đơn, username..."
                searchKeys={['order_code', 'username', 'product_name']}
                onRowClick={handleViewDetail}
                pageSize={pagination.size}
                filters={FiltersComponent}
                actions={ActionsComponent}
                emptyMessage={isLoading ? 'Đang tải dữ liệu...' : 'Không có giao dịch nào'}
                isLoading={isLoading}
            />

            {/* Transaction Detail Drawer */}
            {selectedTransaction && (
                <>
                    <div className="drawer-overlay" onClick={closeDrawer} />
                    <div className="transaction-drawer">
                        <div className="transaction-drawer__header">
                            <h2 className="transaction-drawer__title">
                                Chi tiết giao dịch
                            </h2>
                            <button className="transaction-drawer__close" onClick={closeDrawer}>
                                <FiX size={20} />
                            </button>
                        </div>

                        <div className="transaction-drawer__body">
                            <div className="drawer-field">
                                <span className="drawer-field__label">Mã đơn hàng</span>
                                <span className="drawer-field__value">#{selectedTransaction.order_code}</span>
                            </div>

                            <div className="drawer-field">
                                <span className="drawer-field__label">Trạng thái</span>
                                <TransactionStatusBadge status={selectedTransaction.status} />
                            </div>

                            <div className="drawer-divider" />

                            <div className="drawer-field">
                                <span className="drawer-field__label">Khách hàng</span>
                                <div className="drawer-field__value">
                                    <strong>@{selectedTransaction.username || 'N/A'}</strong>
                                </div>
                            </div>

                            <div className="drawer-field">
                                <span className="drawer-field__label">Loại giao dịch</span>
                                <span className="drawer-field__value">{getTypeLabel(selectedTransaction.type)}</span>
                            </div>

                            <div className="drawer-field">
                                <span className="drawer-field__label">Sản phẩm</span>
                                <span className="drawer-field__value">{selectedTransaction.product_name}</span>
                            </div>

                            <div className="drawer-field">
                                <span className="drawer-field__label">Số tiền</span>
                                <span className={`drawer-field__value drawer-field__value--amount ${selectedTransaction.amount < 0 ? 'drawer-field__value--negative' : ''}`}>
                                    {selectedTransaction.amount >= 0 ? '+' : ''}{formatCurrency(selectedTransaction.amount)}
                                </span>
                            </div>

                            <div className="drawer-divider" />

                            <div className="drawer-field">
                                <span className="drawer-field__label">Thời gian tạo</span>
                                <span className="drawer-field__value">{formatDate(selectedTransaction.created_at)}</span>
                            </div>

                            {selectedTransaction.paid_at && (
                                <div className="drawer-field">
                                    <span className="drawer-field__label">Thời gian thanh toán</span>
                                    <span className="drawer-field__value">{formatDate(selectedTransaction.paid_at)}</span>
                                </div>
                            )}
                        </div>

                        <div className="transaction-drawer__footer">
                            <button
                                className="admin-btn admin-btn--secondary"
                                onClick={() => navigate(`/admin/users/${selectedTransaction.user_id}`)}
                            >
                                <FiExternalLink size={16} />
                                <span>Xem người dùng</span>
                            </button>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}
