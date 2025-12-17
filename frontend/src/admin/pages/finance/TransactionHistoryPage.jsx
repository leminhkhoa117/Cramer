import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    FiSearch,
    FiFilter,
    FiDownload,
    FiEye,
    FiExternalLink,
    FiCalendar,
    FiX
} from 'react-icons/fi';
import DataTable from '../../components/DataTable';
import { TransactionStatusBadge } from '../../components/StatusBadge';
import {
    mockTransactions,
    transactionTypes,
    transactionStatuses,
    formatCurrency,
    getTransactionById,
} from '../../mock/mockFinance';
import './TransactionHistoryPage.css';

export default function TransactionHistoryPage() {
    const navigate = useNavigate();
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [typeFilter, setTypeFilter] = useState('ALL');
    const [selectedTransaction, setSelectedTransaction] = useState(null);

    // Filter transactions
    const filteredTransactions = useMemo(() => {
        return mockTransactions.filter(txn => {
            const matchesStatus = statusFilter === 'ALL' || txn.status === statusFilter;
            const matchesType = typeFilter === 'ALL' || txn.type === typeFilter;
            return matchesStatus && matchesType;
        });
    }, [statusFilter, typeFilter]);

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
        const typeObj = transactionTypes.find(t => t.value === type);
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
            key: 'orderCode',
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
                    <span className="customer-cell__name">@{username}</span>
                    <span className="customer-cell__email">{txn.userEmail}</span>
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
            key: 'productName',
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
            key: 'createdAt',
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

    // Calculate summary stats
    const stats = useMemo(() => {
        const totalAmount = filteredTransactions
            .filter(t => t.status === 'PAID')
            .reduce((sum, t) => sum + t.amount, 0);
        const pendingAmount = filteredTransactions
            .filter(t => t.status === 'PENDING')
            .reduce((sum, t) => sum + t.amount, 0);
        const refundedAmount = filteredTransactions
            .filter(t => t.status === 'REFUNDED')
            .reduce((sum, t) => sum + Math.abs(t.amount), 0);

        return {
            total: filteredTransactions.length,
            paid: filteredTransactions.filter(t => t.status === 'PAID').length,
            pending: filteredTransactions.filter(t => t.status === 'PENDING').length,
            totalAmount,
            pendingAmount,
            refundedAmount,
        };
    }, [filteredTransactions]);

    // Filters component
    const FiltersComponent = (
        <>
            <select
                className="filter-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
            >
                <option value="ALL">Tất cả trạng thái</option>
                {transactionStatuses.map(status => (
                    <option key={status.value} value={status.value}>{status.label}</option>
                ))}
            </select>
            <select
                className="filter-select"
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
            >
                <option value="ALL">Tất cả loại</option>
                {transactionTypes.map(type => (
                    <option key={type.value} value={type.value}>{type.label}</option>
                ))}
            </select>
        </>
    );

    // Actions component  
    const ActionsComponent = (
        <button className="admin-btn admin-btn--secondary">
            <FiDownload size={16} />
            <span>Xuất Excel</span>
        </button>
    );

    return (
        <div className="admin-page transaction-history-page">
            <div className="admin-page__header">
                <h1 className="admin-page__title">Lịch sử Giao dịch</h1>
                <p className="admin-page__subtitle">
                    Xem và quản lý tất cả các giao dịch trên hệ thống
                </p>
            </div>

            {/* Summary Stats */}
            <div className="transaction-stats">
                <div className="transaction-stat">
                    <span className="transaction-stat__value">{stats.total}</span>
                    <span className="transaction-stat__label">Tổng giao dịch</span>
                </div>
                <div className="transaction-stat transaction-stat--success">
                    <span className="transaction-stat__value">{formatCurrency(stats.totalAmount)}</span>
                    <span className="transaction-stat__label">{stats.paid} đã thanh toán</span>
                </div>
                <div className="transaction-stat transaction-stat--warning">
                    <span className="transaction-stat__value">{formatCurrency(stats.pendingAmount)}</span>
                    <span className="transaction-stat__label">{stats.pending} đang chờ</span>
                </div>
                <div className="transaction-stat transaction-stat--danger">
                    <span className="transaction-stat__value">{formatCurrency(stats.refundedAmount)}</span>
                    <span className="transaction-stat__label">Đã hoàn tiền</span>
                </div>
            </div>

            {/* Data Table */}
            <DataTable
                columns={columns}
                data={filteredTransactions}
                searchPlaceholder="Tìm theo mã đơn, username, email..."
                searchKeys={['orderCode', 'username', 'userEmail', 'productName']}
                onRowClick={handleViewDetail}
                pageSize={10}
                filters={FiltersComponent}
                actions={ActionsComponent}
                emptyMessage="Không có giao dịch nào"
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
                                <span className="drawer-field__value">#{selectedTransaction.orderCode}</span>
                            </div>

                            <div className="drawer-field">
                                <span className="drawer-field__label">Trạng thái</span>
                                <TransactionStatusBadge status={selectedTransaction.status} />
                            </div>

                            <div className="drawer-divider" />

                            <div className="drawer-field">
                                <span className="drawer-field__label">Khách hàng</span>
                                <div className="drawer-field__value">
                                    <strong>@{selectedTransaction.username}</strong>
                                    <br />
                                    <span style={{ color: 'var(--admin-text-muted)' }}>
                                        {selectedTransaction.userEmail}
                                    </span>
                                </div>
                            </div>

                            <div className="drawer-field">
                                <span className="drawer-field__label">Loại giao dịch</span>
                                <span className="drawer-field__value">{getTypeLabel(selectedTransaction.type)}</span>
                            </div>

                            <div className="drawer-field">
                                <span className="drawer-field__label">Sản phẩm</span>
                                <span className="drawer-field__value">{selectedTransaction.productName}</span>
                            </div>

                            <div className="drawer-field">
                                <span className="drawer-field__label">Số tiền</span>
                                <span className={`drawer-field__value drawer-field__value--amount ${selectedTransaction.amount < 0 ? 'drawer-field__value--negative' : ''}`}>
                                    {selectedTransaction.amount >= 0 ? '+' : ''}{formatCurrency(selectedTransaction.amount)}
                                </span>
                            </div>

                            <div className="drawer-divider" />

                            <div className="drawer-field">
                                <span className="drawer-field__label">Phương thức</span>
                                <span className="drawer-field__value">{selectedTransaction.paymentMethod}</span>
                            </div>

                            <div className="drawer-field">
                                <span className="drawer-field__label">Thời gian tạo</span>
                                <span className="drawer-field__value">{formatDate(selectedTransaction.createdAt)}</span>
                            </div>

                            {selectedTransaction.paidAt && (
                                <div className="drawer-field">
                                    <span className="drawer-field__label">Thời gian thanh toán</span>
                                    <span className="drawer-field__value">{formatDate(selectedTransaction.paidAt)}</span>
                                </div>
                            )}

                            {selectedTransaction.refundReason && (
                                <div className="drawer-field">
                                    <span className="drawer-field__label">Lý do hoàn tiền</span>
                                    <span className="drawer-field__value">{selectedTransaction.refundReason}</span>
                                </div>
                            )}

                            {selectedTransaction.cancelReason && (
                                <div className="drawer-field">
                                    <span className="drawer-field__label">Lý do hủy</span>
                                    <span className="drawer-field__value">{selectedTransaction.cancelReason}</span>
                                </div>
                            )}
                        </div>

                        <div className="transaction-drawer__footer">
                            <button
                                className="admin-btn admin-btn--secondary"
                                onClick={() => navigate(`/admin/users/${selectedTransaction.userId}`)}
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
