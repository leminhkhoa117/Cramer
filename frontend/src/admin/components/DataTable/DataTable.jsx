import React, { useState, useMemo } from 'react';
import { FiChevronUp, FiChevronDown, FiSearch, FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import './DataTable.css';

/**
 * DataTable - Reusable data table component với sorting, filtering, pagination
 * 
 * @param {Array} columns - Mảng cấu hình columns [{key, label, sortable, render}]
 * @param {Array} data - Mảng dữ liệu
 * @param {string} searchPlaceholder - Placeholder cho search input
 * @param {Array} searchKeys - Các keys để search (vd: ['username', 'email'])
 * @param {Function} onRowClick - Handler khi click vào row
 * @param {number} pageSize - Số items mỗi trang (default: 10)
 * @param {boolean} showSearch - Hiển thị search bar (default: true)
 * @param {React.ReactNode} filters - Render prop cho custom filters
 * @param {React.ReactNode} actions - Render prop cho header actions
 * @param {boolean} loading - Loading state
 * @param {string} emptyMessage - Message khi không có data
 */
export default function DataTable({
    columns = [],
    data = [],
    searchPlaceholder = 'Tìm kiếm...',
    searchKeys = [],
    onRowClick,
    pageSize = 10,
    showSearch = true,
    filters,
    actions,
    loading = false,
    emptyMessage = 'Không có dữ liệu',
}) {
    const [searchTerm, setSearchTerm] = useState('');
    const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });
    const [currentPage, setCurrentPage] = useState(1);

    // Filter data based on search term
    const filteredData = useMemo(() => {
        if (!searchTerm || searchKeys.length === 0) return data;

        const lowerSearch = searchTerm.toLowerCase();
        return data.filter(item =>
            searchKeys.some(key => {
                const value = item[key];
                if (value === null || value === undefined) return false;
                return String(value).toLowerCase().includes(lowerSearch);
            })
        );
    }, [data, searchTerm, searchKeys]);

    // Sort data
    const sortedData = useMemo(() => {
        if (!sortConfig.key) return filteredData;

        return [...filteredData].sort((a, b) => {
            const aValue = a[sortConfig.key];
            const bValue = b[sortConfig.key];

            if (aValue === null || aValue === undefined) return 1;
            if (bValue === null || bValue === undefined) return -1;

            let comparison = 0;
            if (typeof aValue === 'string') {
                comparison = aValue.localeCompare(bValue);
            } else if (aValue instanceof Date || (typeof aValue === 'string' && !isNaN(Date.parse(aValue)))) {
                comparison = new Date(aValue) - new Date(bValue);
            } else {
                comparison = aValue - bValue;
            }

            return sortConfig.direction === 'asc' ? comparison : -comparison;
        });
    }, [filteredData, sortConfig]);

    // Paginate data
    const paginatedData = useMemo(() => {
        const start = (currentPage - 1) * pageSize;
        const end = start + pageSize;
        return sortedData.slice(start, end);
    }, [sortedData, currentPage, pageSize]);

    // Calculate pagination info
    const totalPages = Math.ceil(sortedData.length / pageSize);
    const startItem = (currentPage - 1) * pageSize + 1;
    const endItem = Math.min(currentPage * pageSize, sortedData.length);

    // Handle sort
    const handleSort = (key) => {
        setSortConfig(prev => ({
            key,
            direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc',
        }));
    };

    // Handle page change
    const handlePageChange = (page) => {
        setCurrentPage(Math.max(1, Math.min(page, totalPages)));
    };

    // Reset to page 1 when search changes
    React.useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm]);

    return (
        <div className="data-table">
            {/* Header with search and actions */}
            <div className="data-table__header">
                <div className="data-table__header-left">
                    {showSearch && (
                        <div className="data-table__search">
                            <FiSearch className="data-table__search-icon" />
                            <input
                                type="text"
                                placeholder={searchPlaceholder}
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="data-table__search-input"
                            />
                        </div>
                    )}
                    {filters && <div className="data-table__filters">{filters}</div>}
                </div>
                {actions && <div className="data-table__actions">{actions}</div>}
            </div>

            {/* Table */}
            <div className="data-table__wrapper">
                <table className="data-table__table">
                    <thead>
                        <tr>
                            {columns.map((column) => (
                                <th
                                    key={column.key}
                                    className={`data-table__th ${column.sortable ? 'data-table__th--sortable' : ''}`}
                                    onClick={() => column.sortable && handleSort(column.key)}
                                    style={{ width: column.width }}
                                >
                                    <div className="data-table__th-content">
                                        <span>{column.label}</span>
                                        {column.sortable && (
                                            <span className="data-table__sort-icon">
                                                {sortConfig.key === column.key ? (
                                                    sortConfig.direction === 'asc' ? (
                                                        <FiChevronUp size={14} />
                                                    ) : (
                                                        <FiChevronDown size={14} />
                                                    )
                                                ) : (
                                                    <FiChevronUp size={14} style={{ opacity: 0.3 }} />
                                                )}
                                            </span>
                                        )}
                                    </div>
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr>
                                <td colSpan={columns.length} className="data-table__loading">
                                    <div className="data-table__spinner" />
                                    <span>Đang tải...</span>
                                </td>
                            </tr>
                        ) : paginatedData.length === 0 ? (
                            <tr>
                                <td colSpan={columns.length} className="data-table__empty">
                                    {emptyMessage}
                                </td>
                            </tr>
                        ) : (
                            paginatedData.map((row, rowIndex) => (
                                <tr
                                    key={row.id || rowIndex}
                                    className={`data-table__row ${onRowClick ? 'data-table__row--clickable' : ''}`}
                                    onClick={() => onRowClick && onRowClick(row)}
                                >
                                    {columns.map((column) => (
                                        <td key={column.key} className="data-table__td">
                                            {column.render ? column.render(row[column.key], row) : row[column.key]}
                                        </td>
                                    ))}
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {/* Pagination */}
            {sortedData.length > 0 && (
                <div className="data-table__pagination">
                    <div className="data-table__pagination-info">
                        Hiển thị {startItem} - {endItem} trong tổng số {sortedData.length} kết quả
                    </div>
                    <div className="data-table__pagination-controls">
                        <button
                            className="data-table__pagination-btn"
                            onClick={() => handlePageChange(currentPage - 1)}
                            disabled={currentPage === 1}
                        >
                            <FiChevronLeft size={16} />
                        </button>

                        {/* Page numbers */}
                        {Array.from({ length: totalPages }, (_, i) => i + 1)
                            .filter(page => {
                                // Show first, last, current, and adjacent pages
                                return page === 1 ||
                                    page === totalPages ||
                                    Math.abs(page - currentPage) <= 1;
                            })
                            .reduce((acc, page, idx, arr) => {
                                // Add ellipsis where there are gaps
                                if (idx > 0 && page - arr[idx - 1] > 1) {
                                    acc.push('...');
                                }
                                acc.push(page);
                                return acc;
                            }, [])
                            .map((page, idx) => (
                                page === '...' ? (
                                    <span key={`ellipsis-${idx}`} className="data-table__pagination-ellipsis">...</span>
                                ) : (
                                    <button
                                        key={page}
                                        className={`data-table__pagination-btn ${currentPage === page ? 'data-table__pagination-btn--active' : ''}`}
                                        onClick={() => handlePageChange(page)}
                                    >
                                        {page}
                                    </button>
                                )
                            ))
                        }

                        <button
                            className="data-table__pagination-btn"
                            onClick={() => handlePageChange(currentPage + 1)}
                            disabled={currentPage === totalPages}
                        >
                            <FiChevronRight size={16} />
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}
