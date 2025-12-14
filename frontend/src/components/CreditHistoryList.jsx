import React, { useState, useEffect, useCallback } from 'react';
import { FiLoader, FiChevronDown } from 'react-icons/fi';
import { creditsApi } from '../api/backendApi';

// Filter options
const FILTERS = [
    { value: 'all', label: 'Tất cả' },
    { value: 'earn', label: 'Nhận' },
    { value: 'spend', label: 'Chi tiêu' }
];

/**
 * Paginated transaction history list with type filter.
 * Displays credit transactions with icons, descriptions, and amounts.
 */
export default function CreditHistoryList() {
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [filter, setFilter] = useState('all');
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [totalElements, setTotalElements] = useState(0);

    // Fetch transactions
    const fetchTransactions = useCallback(async (pageNum = 0, filterType = filter, append = false) => {
        if (pageNum === 0) {
            setLoading(true);
        } else {
            setLoadingMore(true);
        }

        try {
            // Use the history endpoint with type filter
            const response = await fetch(`/api/credits/history?type=${filterType}&page=${pageNum}&size=10`, {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('sb-access-token')}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                const newTransactions = data.content || [];

                if (append) {
                    setTransactions(prev => [...prev, ...newTransactions]);
                } else {
                    setTransactions(newTransactions);
                }

                setTotalElements(data.totalElements || 0);
                setHasMore(data.page < data.totalPages - 1);
            } else {
                // Fallback to existing transactions endpoint
                const fallbackRes = await creditsApi.getTransactions(pageNum, 10);
                const data = fallbackRes.data;
                const newTransactions = data.content || [];

                // Filter client-side if needed
                const filtered = filterType === 'all'
                    ? newTransactions
                    : newTransactions.filter(tx => tx.type?.toLowerCase() === filterType);

                if (append) {
                    setTransactions(prev => [...prev, ...filtered]);
                } else {
                    setTransactions(filtered);
                }

                setHasMore(data.page < data.totalPages - 1);
            }
        } catch (error) {
            console.error('Failed to fetch transactions:', error);
        } finally {
            setLoading(false);
            setLoadingMore(false);
        }
    }, [filter]);

    // Initial fetch and refetch on filter change
    useEffect(() => {
        setPage(0);
        fetchTransactions(0, filter, false);
    }, [filter, fetchTransactions]);

    // Handle filter change
    const handleFilterChange = (newFilter) => {
        if (newFilter !== filter) {
            setFilter(newFilter);
        }
    };

    // Handle load more
    const handleLoadMore = () => {
        const nextPage = page + 1;
        setPage(nextPage);
        fetchTransactions(nextPage, filter, true);
    };

    // Format date
    const formatDate = (dateStr) => {
        const date = new Date(dateStr);
        return date.toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    // Get icon for transaction (use backend-provided or fallback)
    const getIcon = (tx) => {
        if (tx.icon) return tx.icon;
        switch (tx.type) {
            case 'EARN': return '💰';
            case 'SPEND': return '💸';
            case 'BONUS': return '⭐';
            default: return '💰';
        }
    };

    // Get icon class for styling
    const getIconClass = (type) => {
        switch (type) {
            case 'EARN': return 'lua-history__icon--earn';
            case 'SPEND': return 'lua-history__icon--spend';
            case 'BONUS': return 'lua-history__icon--bonus';
            default: return 'lua-history__icon--earn';
        }
    };

    // Get amount class for styling
    const getAmountClass = (type) => {
        switch (type) {
            case 'EARN': return 'lua-history__amount--earn';
            case 'SPEND': return 'lua-history__amount--spend';
            case 'BONUS': return 'lua-history__amount--bonus';
            default: return 'lua-history__amount--earn';
        }
    };

    if (loading) {
        return (
            <div className="lua-history__loading">
                <FiLoader className="animate-spin" size={24} />
                <span style={{ marginLeft: '0.5rem' }}>Đang tải...</span>
            </div>
        );
    }

    return (
        <>
            {/* Filter Tabs */}
            <div className="lua-history__header" style={{ marginTop: '1rem' }}>
                <div className="lua-history__filters">
                    {FILTERS.map(f => (
                        <button
                            key={f.value}
                            className={`lua-history__filter-btn ${filter === f.value ? 'lua-history__filter-btn--active' : ''}`}
                            onClick={() => handleFilterChange(f.value)}
                        >
                            {f.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* Transaction List */}
            {transactions.length === 0 ? (
                <div className="lua-history__empty">
                    <div className="lua-history__empty-icon">📭</div>
                    <p className="lua-history__empty-text">
                        {filter === 'all'
                            ? 'Chưa có giao dịch nào'
                            : filter === 'earn'
                                ? 'Chưa có giao dịch nhận Lúa'
                                : 'Chưa có giao dịch chi tiêu'}
                    </p>
                </div>
            ) : (
                <div className="lua-history__list">
                    {transactions.map((tx) => (
                        <div key={tx.id} className="lua-history__item">
                            <div className={`lua-history__icon ${getIconClass(tx.type)}`}>
                                {getIcon(tx)}
                            </div>

                            <div className="lua-history__details">
                                <span className="lua-history__description">{tx.description}</span>
                                <span className="lua-history__date">{formatDate(tx.date || tx.createdAt)}</span>
                            </div>

                            <span className={`lua-history__amount ${getAmountClass(tx.type)}`}>
                                {tx.type === 'SPEND' ? '' : '+'}{tx.amount?.toLocaleString()} 🌾
                            </span>

                            <span className="lua-history__balance">
                                Còn {tx.balanceAfter?.toLocaleString()} 🌾
                            </span>
                        </div>
                    ))}
                </div>
            )}

            {/* Load More Button */}
            {hasMore && transactions.length > 0 && (
                <div className="lua-history__load-more">
                    <button
                        className="lua-history__load-btn"
                        onClick={handleLoadMore}
                        disabled={loadingMore}
                    >
                        {loadingMore ? (
                            <>
                                <FiLoader className="animate-spin" style={{ marginRight: '0.5rem' }} />
                                Đang tải...
                            </>
                        ) : (
                            <>
                                Xem thêm
                                <FiChevronDown style={{ marginLeft: '0.5rem' }} />
                            </>
                        )}
                    </button>
                </div>
            )}
        </>
    );
}
