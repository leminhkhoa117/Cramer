import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import {
    FiDollarSign,
    FiUsers,
    FiCreditCard,
    FiTrendingUp,
    FiDownload,
    FiArrowRight,
    FiRefreshCw
} from 'react-icons/fi';

import {
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    PieChart,
    Pie,
    Cell,
    Legend,
    AreaChart,
    Area
} from 'recharts';
import MetricCard from '../../components/MetricCard';
import { TransactionStatusBadge } from '../../components/StatusBadge';
import { formatCurrency, formatShortCurrency } from '../../utils/formatUtils';
import { TIME_FILTER_OPTIONS } from '../../utils/constants';
import { useAdminFinanceStore } from '../../stores';
import '../../css/pages/finance/FinanceDashboard.css';

/**
 * FinanceDashboard - Trang tổng quan tài chính admin
 * Hiển thị doanh thu, biểu đồ và giao dịch với data thật từ database
 */
export default function FinanceDashboard() {
    const navigate = useNavigate();

    const {
        overview,
        revenueChart,
        revenueBreakdown,
        transactions,
        topSpenders,
        timeFilter,
        isLoadingOverview,
        isLoadingChart,
        isLoadingTransactions,
        isExporting,
        setTimeFilter,
        initializeFinance,
        refreshAll,
        exportToExcel
    } = useAdminFinanceStore();

    // Initialize on mount
    useEffect(() => {
        initializeFinance();
    }, [initializeFinance]);

    // Custom tooltip for charts
    const CustomTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            return (
                <div className="chart-tooltip">
                    <p className="chart-tooltip__label">{label}</p>
                    {payload.map((entry, index) => (
                        <p key={index} className="chart-tooltip__value" style={{ color: entry.color }}>
                            {entry.name}: {formatCurrency(entry.value)}
                        </p>
                    ))}
                </div>
            );
        }
        return null;
    };

    // Format X axis date
    const formatXAxisDate = (dateStr) => {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        return `${date.getDate()}/${date.getMonth() + 1}`;
    };

    // Format Y axis value
    const formatYAxisValue = (value) => {
        if (value >= 1000000) return `${(value / 1000000).toFixed(0)}M`;
        if (value >= 1000) return `${(value / 1000).toFixed(0)}K`;
        return value;
    };

    // Get recent transactions (last 5)
    const recentTransactions = transactions.slice(0, 5);

    // Handle export - lấy tất cả dữ liệu
    const handleExport = () => {
        exportToExcel(null, null, null);
    };

    return (
        <div className="admin-page finance-dashboard">
            <div className="admin-page__header">
                <div>
                    <h1 className="admin-page__title">Quản lý Tài chính</h1>
                    <p className="admin-page__subtitle">
                        Theo dõi doanh thu và giao dịch trên hệ thống
                    </p>
                </div>
                <div className="finance-dashboard__actions">
                    <select
                        className="time-filter-select"
                        value={timeFilter}
                        onChange={(e) => setTimeFilter(e.target.value)}
                    >
                        {TIME_FILTER_OPTIONS.map(option => (
                            <option key={option.value} value={option.value}>{option.label}</option>
                        ))}
                    </select>
                    <button
                        className="admin-btn admin-btn--secondary"
                        onClick={handleExport}
                        disabled={isExporting}
                    >
                        <FiDownload size={16} />
                        <span>{isExporting ? 'Đang xuất...' : 'Xuất báo cáo'}</span>
                    </button>
                    <button
                        className="admin-btn admin-btn--ghost"
                        onClick={refreshAll}
                        disabled={isLoadingOverview}
                    >
                        <FiRefreshCw size={16} className={isLoadingOverview ? 'spinning' : ''} />
                    </button>
                </div>
            </div>

            {/* Metric Cards */}
            <div className="finance-dashboard__metrics">
                <MetricCard
                    title="Tổng doanh thu"
                    value={formatShortCurrency(overview.totalRevenue)}
                    change={overview.totalRevenueChange}
                    icon={<FiDollarSign size={20} />}
                    iconColor="primary"
                    loading={isLoadingOverview}
                />
                <MetricCard
                    title="Doanh thu Subscription"
                    value={formatShortCurrency(overview.subscriptionRevenue)}
                    subtitle={`${overview.newSubscriptions} đăng ký mới`}
                    change={overview.subscriptionChange}
                    icon={<FiCreditCard size={20} />}
                    iconColor="success"
                    loading={isLoadingOverview}
                />
                <MetricCard
                    title="Doanh thu Lúa"
                    value={formatShortCurrency(overview.luaRevenue)}
                    subtitle={`${overview.luaPacksSold} gói đã bán`}
                    change={overview.luaPacksChange}
                    icon={<span style={{ fontSize: '1.25rem' }}>🌾</span>}
                    iconColor="warning"
                    loading={isLoadingOverview}
                />
                <MetricCard
                    title="Tăng trưởng"
                    value={`${overview.growthRate}%`}
                    subtitle={`MRR: ${formatShortCurrency(overview.mrr)}`}
                    icon={<FiTrendingUp size={20} />}
                    iconColor="info"
                    loading={isLoadingOverview}
                />
            </div>

            {/* Charts Section */}
            <div className="finance-dashboard__charts">
                {/* Revenue Line Chart */}
                <div className="chart-card chart-card--large">
                    <div className="chart-card__header">
                        <h3 className="chart-card__title">Biểu đồ doanh thu</h3>
                        <div className="chart-card__legend">
                            <span className="chart-legend-item" style={{ '--color': '#8B5CF6' }}>Subscription</span>
                            <span className="chart-legend-item" style={{ '--color': '#F59E0B' }}>Lúa</span>
                        </div>
                    </div>
                    <div className="chart-card__body">
                        {isLoadingChart ? (
                            <div className="chart-loading">
                                <div className="spinner"></div>
                                <span>Đang tải biểu đồ...</span>
                            </div>
                        ) : revenueChart.length > 0 ? (
                            <ResponsiveContainer width="100%" height={300}>
                                <AreaChart data={revenueChart} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                                    <defs>
                                        <linearGradient id="colorSubscriptions" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#8B5CF6" stopOpacity={0.3} />
                                            <stop offset="95%" stopColor="#8B5CF6" stopOpacity={0} />
                                        </linearGradient>
                                        <linearGradient id="colorLua" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#F59E0B" stopOpacity={0.3} />
                                            <stop offset="95%" stopColor="#F59E0B" stopOpacity={0} />
                                        </linearGradient>
                                    </defs>
                                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
                                    <XAxis
                                        dataKey="date"
                                        tickFormatter={formatXAxisDate}
                                        stroke="rgba(255,255,255,0.5)"
                                        fontSize={12}
                                    />
                                    <YAxis
                                        tickFormatter={formatYAxisValue}
                                        stroke="rgba(255,255,255,0.5)"
                                        fontSize={12}
                                    />
                                    <Tooltip content={<CustomTooltip />} />
                                    <Area
                                        type="monotone"
                                        dataKey="subscriptions"
                                        name="Subscription"
                                        stroke="#8B5CF6"
                                        fillOpacity={1}
                                        fill="url(#colorSubscriptions)"
                                        strokeWidth={2}
                                    />
                                    <Area
                                        type="monotone"
                                        dataKey="lua"
                                        name="Lúa"
                                        stroke="#F59E0B"
                                        fillOpacity={1}
                                        fill="url(#colorLua)"
                                        strokeWidth={2}
                                    />
                                </AreaChart>
                            </ResponsiveContainer>
                        ) : (
                            <div className="chart-empty">
                                <p>Chưa có dữ liệu doanh thu</p>
                            </div>
                        )}
                    </div>
                </div>

                {/* Revenue Breakdown Pie Chart */}
                <div className="chart-card">
                    <div className="chart-card__header">
                        <h3 className="chart-card__title">Phân bổ doanh thu</h3>
                    </div>
                    <div className="chart-card__body">
                        {revenueBreakdown.length > 0 ? (
                            <ResponsiveContainer width="100%" height={250}>
                                <PieChart>
                                    <Pie
                                        data={revenueBreakdown}
                                        cx="50%"
                                        cy="50%"
                                        innerRadius={60}
                                        outerRadius={90}
                                        paddingAngle={5}
                                        dataKey="value"
                                    >
                                        {revenueBreakdown.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={entry.color} />
                                        ))}
                                    </Pie>
                                    <Tooltip
                                        formatter={(value) => formatCurrency(value)}
                                        contentStyle={{
                                            background: 'rgba(15, 15, 35, 0.95)',
                                            border: '1px solid rgba(255,255,255,0.2)',
                                            borderRadius: '8px',
                                            boxShadow: '0 10px 40px rgba(0,0,0,0.5)'
                                        }}
                                        labelStyle={{
                                            color: '#F8FAFC',
                                            fontWeight: 600
                                        }}
                                        itemStyle={{
                                            color: '#F8FAFC'
                                        }}
                                    />
                                    <Legend
                                        formatter={(value) => <span style={{ color: '#94A3B8' }}>{value}</span>}
                                    />
                                </PieChart>
                            </ResponsiveContainer>
                        ) : (
                            <div className="chart-empty">
                                <p>Chưa có dữ liệu</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Bottom Section */}
            <div className="finance-dashboard__bottom">
                {/* Recent Transactions */}
                <div className="finance-section">
                    <div className="finance-section__header">
                        <h3 className="finance-section__title">Giao dịch gần đây</h3>
                        <button
                            className="finance-section__link"
                            onClick={() => navigate('/admin/finance/transactions')}
                        >
                            Xem tất cả <FiArrowRight size={14} />
                        </button>
                    </div>
                    <div className="transaction-list-compact">
                        {isLoadingTransactions ? (
                            <div className="loading-placeholder">Đang tải...</div>
                        ) : recentTransactions.length > 0 ? (
                            recentTransactions.map(txn => (
                                <div key={txn.id || txn.order_code} className="transaction-list-item">
                                    <div className="transaction-list-item__info">
                                        <span className="transaction-list-item__username">@{txn.username}</span>
                                        <span className="transaction-list-item__product">{txn.product_name || txn.description}</span>
                                    </div>
                                    <div className="transaction-list-item__details">
                                        <TransactionStatusBadge status={txn.status} />
                                        <span className={`transaction-list-item__amount ${txn.amount < 0 ? 'transaction-list-item__amount--refund' : ''}`}>
                                            {txn.amount >= 0 ? '+' : ''}{formatCurrency(txn.amount)}
                                        </span>
                                    </div>
                                </div>
                            ))
                        ) : (
                            <div className="empty-placeholder">Chưa có giao dịch nào</div>
                        )}
                    </div>
                </div>

                {/* Top Spenders */}
                <div className="finance-section">
                    <div className="finance-section__header">
                        <h3 className="finance-section__title">Top Khách hàng</h3>
                    </div>
                    <div className="top-spenders-list">
                        {topSpenders.length > 0 ? (
                            topSpenders.slice(0, 5).map((spender, index) => (
                                <div key={spender.user_id} className="top-spender-item">
                                    <div className={`top-spender-item__rank top-spender-item__rank--${index + 1}`}>
                                        {index < 3 ? ['🥇', '🥈', '🥉'][index] : index + 1}
                                    </div>
                                    <div className="top-spender-item__info">
                                        <span className="top-spender-item__name">{spender.full_name || spender.username}</span>
                                        <span className="top-spender-item__username">@{spender.username}</span>
                                    </div>
                                    <div className="top-spender-item__amount">
                                        {formatShortCurrency(spender.total_spent)}
                                    </div>
                                </div>
                            ))
                        ) : (
                            <div className="empty-placeholder">Chưa có dữ liệu</div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
