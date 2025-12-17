import React from 'react';
import { FiTrendingUp, FiTrendingDown } from 'react-icons/fi';
import './MetricCard.css';

/**
 * MetricCard - Component hiển thị metric với icon, value, label và trend
 * 
 * @param {string} title - Tiêu đề metric
 * @param {string|number} value - Giá trị chính
 * @param {string} subtitle - Mô tả phụ (optional)
 * @param {number} change - % thay đổi so với kỳ trước
 * @param {string} changeLabel - Label cho change (vd: "so với tháng trước")
 * @param {React.ReactNode} icon - Icon component
 * @param {string} iconColor - Màu nền icon: 'primary' | 'success' | 'warning' | 'danger' | 'info'
 * @param {boolean} loading - Loading state
 */
export default function MetricCard({
    title,
    value,
    subtitle,
    change,
    changeLabel = 'so với tháng trước',
    icon,
    iconColor = 'primary',
    loading = false,
}) {
    const isPositive = change > 0;
    const isNegative = change < 0;
    const changeValue = Math.abs(change);

    if (loading) {
        return (
            <div className="metric-card metric-card--loading">
                <div className="metric-card__skeleton metric-card__skeleton--icon" />
                <div className="metric-card__skeleton metric-card__skeleton--value" />
                <div className="metric-card__skeleton metric-card__skeleton--label" />
            </div>
        );
    }

    return (
        <div className="metric-card">
            <div className="metric-card__header">
                {icon && (
                    <div className={`metric-card__icon metric-card__icon--${iconColor}`}>
                        {icon}
                    </div>
                )}
                <span className="metric-card__title">{title}</span>
            </div>

            <div className="metric-card__value">{value}</div>

            {subtitle && (
                <div className="metric-card__subtitle">{subtitle}</div>
            )}

            {change !== undefined && change !== null && (
                <div className="metric-card__change">
                    <span className={`metric-card__trend ${isPositive ? 'metric-card__trend--up' : ''} ${isNegative ? 'metric-card__trend--down' : ''}`}>
                        {isPositive && <FiTrendingUp size={14} />}
                        {isNegative && <FiTrendingDown size={14} />}
                        <span>{isPositive ? '+' : isNegative ? '-' : ''}{changeValue}%</span>
                    </span>
                    <span className="metric-card__change-label">{changeLabel}</span>
                </div>
            )}
        </div>
    );
}
