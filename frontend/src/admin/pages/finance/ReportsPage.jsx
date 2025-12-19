import React, { useState, useEffect, useCallback } from 'react';
import {
    FiPieChart,
    FiTrendingUp,
    FiTrendingDown,
    FiDollarSign,
    FiCalendar,
    FiDownload,
    FiUsers,
    FiRefreshCw,
    FiFileText,
    FiBarChart2,
    FiFilter,
    FiExternalLink
} from 'react-icons/fi';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    BarChart,
    Bar,
    PieChart,
    Pie,
    Cell,
    Legend,
    AreaChart,
    Area,
    ComposedChart
} from 'recharts';
import { formatCurrency, formatShortCurrency } from '../../utils/formatUtils';
import adminApi from '../../api/adminApi';
import { exportToExcel } from '../../utils/exportUtils';
import '../../css/pages/finance/ReportsPage.css';

// Report Types Configuration
const REPORT_TYPES = [
    {
        id: 'revenue_summary',
        name: 'Tổng hợp Doanh thu',
        icon: <FiDollarSign size={20} />,
        description: 'Cái nhìn tổng quan về doanh thu trong kỳ',
        color: '#8B5CF6'
    },
    {
        id: 'subscription_analysis',
        name: 'Phân tích Subscriptions',
        icon: <FiUsers size={20} />,
        description: 'Đánh giá sức khỏe của subscription business',
        color: '#10B981'
    },
    {
        id: 'lua_economy',
        name: 'Kinh tế Lúa',
        icon: <span style={{ fontSize: '1.25rem' }}>🌾</span>,
        description: 'Theo dõi "tiền ảo" trong hệ thống',
        color: '#F59E0B'
    },
    {
        id: 'user_acquisition',
        name: 'User Acquisition Cost',
        icon: <FiTrendingUp size={20} />,
        description: 'Đánh giá chi phí thu hút user mới',
        color: '#3B82F6'
    }
];

// Granularity Options
const GRANULARITY_OPTIONS = [
    { value: 'daily', label: 'Theo ngày' },
    { value: 'weekly', label: 'Theo tuần' },
    { value: 'monthly', label: 'Theo tháng' }
];

// Default mock data (fallback if API fails)
const defaultRevenueData = [
    { date: '01/12', subscriptions: 0, lua: 0, total: 0 }
];

const defaultMetrics = {
    totalRevenue: 0,
    subscriptionRevenue: 0,
    luaRevenue: 0,
    newSubscriptions: 0,
    luaPacksSold: 0,
    pendingTransactions: 0,
    totalRevenueChange: 0,
    subscriptionChange: 0,
    luaPacksChange: 0,
    growthRate: 0,
    mrr: 0
};

/**
 * ReportsPage - Trang báo cáo tài chính
 * Hiển thị các báo cáo doanh thu, phân tích tài chính theo spec
 */
export default function ReportsPage() {
    const [selectedReport, setSelectedReport] = useState('revenue_summary');
    const [dateFrom, setDateFrom] = useState(() => {
        const date = new Date();
        date.setDate(1); // First day of month
        return date.toISOString().split('T')[0];
    });
    const [dateTo, setDateTo] = useState(() => new Date().toISOString().split('T')[0]);
    const [granularity, setGranularity] = useState('daily');
    const [isExporting, setIsExporting] = useState(false);
    const [isLoading, setIsLoading] = useState(true);

    // API data state
    const [overview, setOverview] = useState(defaultMetrics);
    const [chartData, setChartData] = useState(defaultRevenueData);
    const [breakdown, setBreakdown] = useState([]);

    // Fetch dashboard data
    const fetchData = useCallback(async () => {
        setIsLoading(true);
        try {
            const [overviewData, chartResponse, breakdownData] = await Promise.all([
                adminApi.finance.getOverview('30days'),
                adminApi.finance.getChart('30days'),
                adminApi.finance.getBreakdown('30days')
            ]);

            setOverview(overviewData || defaultMetrics);

            // Transform chart data
            if (chartResponse && chartResponse.length > 0) {
                const formattedChart = chartResponse.map(item => ({
                    date: new Date(item.date).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
                    subscriptions: item.subscriptions || 0,
                    lua: item.lua || 0,
                    total: item.total || 0
                }));
                setChartData(formattedChart);
            }

            setBreakdown(breakdownData || []);
        } catch (err) {
            console.error('Error fetching report data:', err);
        } finally {
            setIsLoading(false);
        }
    }, []);

    // Fetch data on mount
    useEffect(() => {
        fetchData();
    }, [fetchData]);

    // Handle export
    const handleExport = async (format) => {
        setIsExporting(true);
        try {
            if (format === 'xlsx') {
                const exportData = await adminApi.finance.getExportData(dateFrom, dateTo);
                if (exportData && exportData.length > 0) {
                    exportToExcel(exportData, 'Báo cáo Tài chính', 'finance_report');
                } else {
                    alert('Không có dữ liệu để xuất');
                }
            } else {
                alert(`Định dạng ${format.toUpperCase()} chưa được hỗ trợ`);
            }
        } catch (err) {
            console.error('Error exporting:', err);
            alert('Lỗi khi xuất báo cáo');
        } finally {
            setIsExporting(false);
        }
    };

    // Custom Chart Tooltip
    const CustomTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            return (
                <div className="reports-chart-tooltip">
                    <p className="reports-chart-tooltip__label">{label}</p>
                    {payload.map((entry, index) => (
                        <p key={index} className="reports-chart-tooltip__value" style={{ color: entry.color }}>
                            {entry.name}: {typeof entry.value === 'number' && entry.value > 1000
                                ? formatCurrency(entry.value)
                                : entry.value}
                        </p>
                    ))}
                </div>
            );
        }
        return null;
    };

    // Render Revenue Summary Report
    const renderRevenueSummary = () => (
        <div className="report-content">
            <div className="report-metrics-row">
                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(139, 92, 246, 0.15)' }}>
                        <FiDollarSign size={24} style={{ color: '#8B5CF6' }} />
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{formatShortCurrency(overview.totalRevenue)}</span>
                        <span className="report-metric-card__label">Tổng doanh thu</span>
                    </div>
                    <div className={`report-metric-card__change report-metric-card__change--${overview.totalRevenueChange >= 0 ? 'up' : 'down'}`}>
                        {overview.totalRevenueChange >= 0 ? <FiTrendingUp size={14} /> : <FiTrendingDown size={14} />}
                        {overview.totalRevenueChange >= 0 ? '+' : ''}{overview.totalRevenueChange}%
                    </div>
                </div>

                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(16, 185, 129, 0.15)' }}>
                        <FiUsers size={24} style={{ color: '#10B981' }} />
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{formatShortCurrency(overview.subscriptionRevenue)}</span>
                        <span className="report-metric-card__label">Doanh thu Subscriptions</span>
                    </div>
                    <div className={`report-metric-card__change report-metric-card__change--${overview.subscriptionChange >= 0 ? 'up' : 'down'}`}>
                        {overview.subscriptionChange >= 0 ? <FiTrendingUp size={14} /> : <FiTrendingDown size={14} />}
                        {overview.subscriptionChange >= 0 ? '+' : ''}{overview.subscriptionChange}%
                    </div>
                </div>

                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(245, 158, 11, 0.15)' }}>
                        <span style={{ fontSize: '1.5rem' }}>🌾</span>
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{formatShortCurrency(overview.luaRevenue)}</span>
                        <span className="report-metric-card__label">Doanh thu Lúa</span>
                    </div>
                    <div className={`report-metric-card__change report-metric-card__change--${overview.luaPacksChange >= 0 ? 'up' : 'down'}`}>
                        {overview.luaPacksChange >= 0 ? <FiTrendingUp size={14} /> : <FiTrendingDown size={14} />}
                        {overview.luaPacksChange >= 0 ? '+' : ''}{overview.luaPacksChange}%
                    </div>
                </div>

                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(59, 130, 246, 0.15)' }}>
                        <FiBarChart2 size={24} style={{ color: '#3B82F6' }} />
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{overview.growthRate >= 0 ? '+' : ''}{overview.growthRate}%</span>
                        <span className="report-metric-card__label">Tăng trưởng so với kỳ trước</span>
                    </div>
                </div>
            </div>

            <div className="report-chart-section">
                <h3 className="report-section-title">Xu hướng doanh thu</h3>
                <div className="report-chart-container">
                    <ResponsiveContainer width="100%" height={350}>
                        <AreaChart data={chartData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                            <defs>
                                <linearGradient id="colorSub" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#8B5CF6" stopOpacity={0.3} />
                                    <stop offset="95%" stopColor="#8B5CF6" stopOpacity={0} />
                                </linearGradient>
                                <linearGradient id="colorLuaReport" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#F59E0B" stopOpacity={0.3} />
                                    <stop offset="95%" stopColor="#F59E0B" stopOpacity={0} />
                                </linearGradient>
                            </defs>
                            <CartesianGrid strokeDasharray="3 3" stroke="rgba(148, 163, 184, 0.1)" />
                            <XAxis dataKey="date" stroke="#94A3B8" fontSize={12} />
                            <YAxis tickFormatter={(v) => formatShortCurrency(v)} stroke="#94A3B8" fontSize={12} />
                            <Tooltip content={<CustomTooltip />} />
                            <Legend />
                            <Area type="monotone" dataKey="subscriptions" name="Subscription" stroke="#8B5CF6" fill="url(#colorSub)" strokeWidth={2} />
                            <Area type="monotone" dataKey="lua" name="Lúa" stroke="#F59E0B" fill="url(#colorLuaReport)" strokeWidth={2} />
                        </AreaChart>
                    </ResponsiveContainer>
                </div>
            </div>

            <div className="report-comparison-section">
                <h3 className="report-section-title">So sánh với kỳ trước</h3>
                <div className="report-comparison-grid">
                    <div className="comparison-item">
                        <span className="comparison-item__label">Kỳ này (01/12 - 17/12)</span>
                        <span className="comparison-item__value">{formatCurrency(25800000)}</span>
                    </div>
                    <div className="comparison-item comparison-item--vs">VS</div>
                    <div className="comparison-item">
                        <span className="comparison-item__label">Kỳ trước (14/11 - 30/11)</span>
                        <span className="comparison-item__value comparison-item__value--muted">{formatCurrency(21123000)}</span>
                    </div>
                    <div className="comparison-item comparison-item--result">
                        <span className="comparison-item__change comparison-item__change--up">
                            <FiTrendingUp size={16} /> +22.1% ({formatShortCurrency(4677000)})
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );

    // Render Subscription Analysis Report
    const renderSubscriptionAnalysis = () => (
        <div className="report-content">
            <div className="report-metrics-row">
                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(139, 92, 246, 0.15)' }}>
                        <FiTrendingUp size={24} style={{ color: '#8B5CF6' }} />
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{formatShortCurrency(mockSubscriptionMetrics.mrr)}</span>
                        <span className="report-metric-card__label">MRR (Monthly Recurring Revenue)</span>
                    </div>
                    <div className="report-metric-card__change report-metric-card__change--up">
                        <FiTrendingUp size={14} /> +{mockSubscriptionMetrics.mrrChange}%
                    </div>
                </div>

                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(239, 68, 68, 0.15)' }}>
                        <FiTrendingDown size={24} style={{ color: '#EF4444' }} />
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{mockSubscriptionMetrics.churnRate}%</span>
                        <span className="report-metric-card__label">Churn Rate</span>
                    </div>
                    <div className="report-metric-card__change report-metric-card__change--up">
                        <FiTrendingDown size={14} /> {mockSubscriptionMetrics.churnRateChange}%
                    </div>
                </div>

                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(16, 185, 129, 0.15)' }}>
                        <FiDollarSign size={24} style={{ color: '#10B981' }} />
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{formatShortCurrency(mockSubscriptionMetrics.ltv)}</span>
                        <span className="report-metric-card__label">LTV (Lifetime Value)</span>
                    </div>
                    <div className="report-metric-card__change report-metric-card__change--up">
                        <FiTrendingUp size={14} /> +{mockSubscriptionMetrics.ltvChange}%
                    </div>
                </div>

                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(59, 130, 246, 0.15)' }}>
                        <FiUsers size={24} style={{ color: '#3B82F6' }} />
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{mockSubscriptionMetrics.activeSubscribers}</span>
                        <span className="report-metric-card__label">Active Subscribers</span>
                    </div>
                </div>
            </div>

            <div className="report-stats-grid">
                <div className="report-stat-box report-stat-box--success">
                    <FiUsers size={20} />
                    <div>
                        <span className="report-stat-box__value">{mockSubscriptionMetrics.newSubscribers}</span>
                        <span className="report-stat-box__label">Đăng ký mới</span>
                    </div>
                </div>
                <div className="report-stat-box report-stat-box--danger">
                    <FiUsers size={20} />
                    <div>
                        <span className="report-stat-box__value">{mockSubscriptionMetrics.cancelledSubscribers}</span>
                        <span className="report-stat-box__label">Đã hủy</span>
                    </div>
                </div>
                <div className="report-stat-box report-stat-box--info">
                    <FiTrendingUp size={20} />
                    <div>
                        <span className="report-stat-box__value">+{mockSubscriptionMetrics.newSubscribers - mockSubscriptionMetrics.cancelledSubscribers}</span>
                        <span className="report-stat-box__label">Net Growth</span>
                    </div>
                </div>
            </div>

            <div className="report-chart-section">
                <h3 className="report-section-title">Cohort Retention</h3>
                <div className="cohort-table">
                    <table>
                        <thead>
                            <tr>
                                <th>Cohort</th>
                                <th>Tháng 1</th>
                                <th>Tháng 2</th>
                                <th>Tháng 3</th>
                            </tr>
                        </thead>
                        <tbody>
                            {mockCohortData.map((row, idx) => (
                                <tr key={idx}>
                                    <td>{row.cohort}</td>
                                    <td><span className="cohort-cell" style={{ opacity: row.month1 / 100 }}>{row.month1}%</span></td>
                                    <td>{row.month2 && <span className="cohort-cell" style={{ opacity: row.month2 / 100 }}>{row.month2}%</span>}</td>
                                    <td>{row.month3 && <span className="cohort-cell" style={{ opacity: row.month3 / 100 }}>{row.month3}%</span>}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );

    // Render Lua Economy Report
    const renderLuaEconomy = () => (
        <div className="report-content">
            <div className="report-metrics-row">
                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(245, 158, 11, 0.15)' }}>
                        <span style={{ fontSize: '1.5rem' }}>🌾</span>
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{mockLuaEconomy.totalIssued.toLocaleString()}</span>
                        <span className="report-metric-card__label">Tổng Lúa đã phát hành</span>
                    </div>
                </div>

                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(239, 68, 68, 0.15)' }}>
                        <span style={{ fontSize: '1.5rem' }}>🔥</span>
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{mockLuaEconomy.totalSpent.toLocaleString()}</span>
                        <span className="report-metric-card__label">Tổng Lúa đã tiêu</span>
                    </div>
                </div>

                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(16, 185, 129, 0.15)' }}>
                        <span style={{ fontSize: '1.5rem' }}>💰</span>
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{mockLuaEconomy.inCirculation.toLocaleString()}</span>
                        <span className="report-metric-card__label">Lúa đang lưu hành</span>
                    </div>
                </div>

                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(59, 130, 246, 0.15)' }}>
                        <FiUsers size={24} style={{ color: '#3B82F6' }} />
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{mockLuaEconomy.avgBalance}</span>
                        <span className="report-metric-card__label">Số dư TB/user</span>
                    </div>
                </div>
            </div>

            <div className="report-charts-row">
                <div className="report-chart-section report-chart-section--half">
                    <h3 className="report-section-title">Nguồn Lúa</h3>
                    <div className="report-chart-container">
                        <ResponsiveContainer width="100%" height={250}>
                            <PieChart>
                                <Pie
                                    data={[
                                        { name: 'Mua', value: mockLuaEconomy.purchasedLua, color: '#8B5CF6' },
                                        { name: 'Bonus', value: mockLuaEconomy.bonusLua, color: '#F59E0B' }
                                    ]}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={60}
                                    outerRadius={90}
                                    paddingAngle={5}
                                    dataKey="value"
                                >
                                    <Cell fill="#8B5CF6" />
                                    <Cell fill="#F59E0B" />
                                </Pie>
                                <Tooltip formatter={(v) => v.toLocaleString() + ' Lúa'} />
                                <Legend />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                <div className="report-chart-section report-chart-section--half">
                    <h3 className="report-section-title">Top Features tiêu Lúa</h3>
                    <div className="lua-features-list">
                        {mockLuaEconomy.topFeatures.map((feature, idx) => (
                            <div key={idx} className="lua-feature-item">
                                <div className="lua-feature-item__info">
                                    <span className="lua-feature-item__name">{feature.name}</span>
                                    <span className="lua-feature-item__spent">{feature.spent.toLocaleString()} Lúa</span>
                                </div>
                                <div className="lua-feature-item__bar">
                                    <div
                                        className="lua-feature-item__bar-fill"
                                        style={{ width: `${feature.percentage}%` }}
                                    />
                                </div>
                                <span className="lua-feature-item__percentage">{feature.percentage}%</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );

    // Render User Acquisition Report
    const renderUserAcquisition = () => (
        <div className="report-content">
            <div className="report-metrics-row">
                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(239, 68, 68, 0.15)' }}>
                        <FiDollarSign size={24} style={{ color: '#EF4444' }} />
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{formatShortCurrency(mockAcquisitionMetrics.bonusCost)}</span>
                        <span className="report-metric-card__label">Chi phí Bonus Lúa</span>
                    </div>
                </div>

                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(16, 185, 129, 0.15)' }}>
                        <FiTrendingUp size={24} style={{ color: '#10B981' }} />
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{mockAcquisitionMetrics.conversionRate}%</span>
                        <span className="report-metric-card__label">Tỷ lệ Free → Paid</span>
                    </div>
                    <div className="report-metric-card__change report-metric-card__change--up">
                        <FiTrendingUp size={14} /> +{mockAcquisitionMetrics.conversionRateChange}%
                    </div>
                </div>

                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(59, 130, 246, 0.15)' }}>
                        <FiCalendar size={24} style={{ color: '#3B82F6' }} />
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{mockAcquisitionMetrics.avgTimeToConvert} ngày</span>
                        <span className="report-metric-card__label">Thời gian convert TB</span>
                    </div>
                </div>

                <div className="report-metric-card">
                    <div className="report-metric-card__icon" style={{ background: 'rgba(139, 92, 246, 0.15)' }}>
                        <FiBarChart2 size={24} style={{ color: '#8B5CF6' }} />
                    </div>
                    <div className="report-metric-card__content">
                        <span className="report-metric-card__value">{mockAcquisitionMetrics.promotionROI}%</span>
                        <span className="report-metric-card__label">ROI Promotions</span>
                    </div>
                </div>
            </div>

            <div className="report-funnel-section">
                <h3 className="report-section-title">Funnel Chuyển đổi</h3>
                <div className="conversion-funnel">
                    <div className="funnel-step funnel-step--large">
                        <div className="funnel-step__bar" style={{ width: '100%', background: 'linear-gradient(135deg, #8B5CF6, #6366F1)' }} />
                        <div className="funnel-step__info">
                            <span className="funnel-step__value">{mockAcquisitionMetrics.freeUsers.toLocaleString()}</span>
                            <span className="funnel-step__label">Free Users</span>
                        </div>
                    </div>
                    <div className="funnel-arrow">→</div>
                    <div className="funnel-step funnel-step--medium">
                        <div className="funnel-step__bar" style={{ width: `${mockAcquisitionMetrics.conversionRate * 3}%`, background: 'linear-gradient(135deg, #10B981, #059669)' }} />
                        <div className="funnel-step__info">
                            <span className="funnel-step__value">{mockAcquisitionMetrics.paidUsers}</span>
                            <span className="funnel-step__label">Paid Users</span>
                        </div>
                    </div>
                    <div className="funnel-step funnel-step--small">
                        <div className="funnel-step__badge">+{mockAcquisitionMetrics.convertedThisPeriod} kỳ này</div>
                    </div>
                </div>
            </div>
        </div>
    );

    // Render current report based on selection
    const renderReport = () => {
        switch (selectedReport) {
            case 'revenue_summary':
                return renderRevenueSummary();
            case 'subscription_analysis':
                return renderSubscriptionAnalysis();
            case 'lua_economy':
                return renderLuaEconomy();
            case 'user_acquisition':
                return renderUserAcquisition();
            default:
                return renderRevenueSummary();
        }
    };

    return (
        <div className="admin-page reports-page">
            {/* Page Header */}
            <div className="admin-page__header">
                <div>
                    <h1 className="admin-page__title">Báo cáo Tài chính</h1>
                    <p className="admin-page__subtitle">
                        Phân tích doanh thu và thống kê tài chính chi tiết
                    </p>
                </div>
            </div>

            {/* Report Type Selector */}
            <div className="reports-type-selector">
                {REPORT_TYPES.map(report => (
                    <button
                        key={report.id}
                        className={`report-type-btn ${selectedReport === report.id ? 'report-type-btn--active' : ''}`}
                        onClick={() => setSelectedReport(report.id)}
                        style={{ '--accent-color': report.color }}
                    >
                        <span className="report-type-btn__icon">{report.icon}</span>
                        <div className="report-type-btn__text">
                            <span className="report-type-btn__name">{report.name}</span>
                            <span className="report-type-btn__desc">{report.description}</span>
                        </div>
                    </button>
                ))}
            </div>

            {/* Report Configuration */}
            <div className="reports-config admin-card">
                <div className="reports-config__filters">
                    <div className="config-group">
                        <label>Từ ngày</label>
                        <input
                            type="date"
                            value={dateFrom}
                            onChange={(e) => setDateFrom(e.target.value)}
                            className="config-input"
                        />
                    </div>
                    <div className="config-group">
                        <label>Đến ngày</label>
                        <input
                            type="date"
                            value={dateTo}
                            onChange={(e) => setDateTo(e.target.value)}
                            className="config-input"
                        />
                    </div>
                    <div className="config-group">
                        <label>Độ chi tiết</label>
                        <select
                            value={granularity}
                            onChange={(e) => setGranularity(e.target.value)}
                            className="config-select"
                        >
                            {GRANULARITY_OPTIONS.map(opt => (
                                <option key={opt.value} value={opt.value}>{opt.label}</option>
                            ))}
                        </select>
                    </div>
                    <button
                        className="admin-btn admin-btn--secondary"
                        onClick={fetchData}
                        disabled={isLoading}
                    >
                        <FiRefreshCw size={16} className={isLoading ? 'spinning' : ''} />
                        {isLoading ? 'Đang tải...' : 'Làm mới'}
                    </button>
                </div>
                <div className="reports-config__export">
                    <span className="export-label">Xuất báo cáo:</span>
                    <div className="export-buttons">
                        <button
                            className="export-btn"
                            onClick={() => handleExport('csv')}
                            disabled={isExporting}
                        >
                            <FiDownload size={14} /> CSV
                        </button>
                        <button
                            className="export-btn export-btn--excel"
                            onClick={() => handleExport('xlsx')}
                            disabled={isExporting}
                        >
                            <FiDownload size={14} /> Excel
                        </button>
                        <button
                            className="export-btn export-btn--pdf"
                            onClick={() => handleExport('pdf')}
                            disabled={isExporting}
                        >
                            <FiDownload size={14} /> PDF
                        </button>
                    </div>
                </div>
            </div>

            {/* Report Content */}
            <div className="reports-content admin-card">
                {renderReport()}
            </div>
        </div>
    );
}
