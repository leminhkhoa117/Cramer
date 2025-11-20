import React from 'react';
import { FaChevronLeft, FaChevronRight } from 'react-icons/fa';
import '../css/Pagination.css';

const Pagination = ({ currentPage, totalPages, onPageChange }) => {
    if (totalPages <= 1) return null;

    const getPageNumbers = () => {
        const pages = [];
        const maxVisiblePages = 5;

        if (totalPages <= maxVisiblePages) {
            for (let i = 0; i < totalPages; i++) {
                pages.push(i);
            }
        } else {
            let start = Math.max(0, currentPage - 2);
            let end = Math.min(totalPages - 1, currentPage + 2);

            if (start === 0) {
                end = Math.min(totalPages - 1, maxVisiblePages - 1);
            }

            if (end === totalPages - 1) {
                start = Math.max(0, totalPages - maxVisiblePages);
            }

            for (let i = start; i <= end; i++) {
                pages.push(i);
            }
        }
        return pages;
    };

    return (
        <div className="pagination-container">
            <button
                className="pagination-btn"
                onClick={() => onPageChange(currentPage - 1)}
                disabled={currentPage === 0}
                aria-label="Previous page"
            >
                <FaChevronLeft />
            </button>

            {getPageNumbers().map(page => (
                <button
                    key={page}
                    className={`pagination-btn ${currentPage === page ? 'active' : ''}`}
                    onClick={() => onPageChange(page)}
                >
                    {page + 1}
                </button>
            ))}

            <button
                className="pagination-btn"
                onClick={() => onPageChange(currentPage + 1)}
                disabled={currentPage === totalPages - 1}
                aria-label="Next page"
            >
                <FaChevronRight />
            </button>
        </div>
    );
};

export default Pagination;
