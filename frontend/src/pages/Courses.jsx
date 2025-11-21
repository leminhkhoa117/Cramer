import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import { courseApi } from '../api/backendApi';
import FilterModal from '../components/FilterModal';
import './../css/Courses.css';

import { FaSearch } from 'react-icons/fa';
import FullPageLoader from '../components/FullPageLoader';
import Pagination from '../components/Pagination';

// Helper to format course source names
const formatCourseName = (source) => {
    if (source.toLowerCase().startsWith('cam')) {
        return `IELTS Cambridge ${source.substring(3)}`;
    }
    return source;
};

export default function Courses() {
    const [courses, setCourses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
    const [activeFilters, setActiveFilters] = useState({});

    // Pagination states
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    // Debounce search
    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedSearch(searchTerm);
            setPage(0); // Reset to first page on search change
        }, 500);
        return () => clearTimeout(timer);
    }, [searchTerm]);

    useEffect(() => {
        const fetchCourses = async () => {
            try {
                setLoading(true);
                // Pass pagination and search params
                const response = await courseApi.getAll(page, 6, debouncedSearch);

                if (response.data && response.data.content) {
                    setCourses(response.data.content);
                    setTotalPages(response.data.totalPages);
                } else {
                    // Fallback for backward compatibility or empty response
                    setCourses([]);
                    setTotalPages(0);
                }

                setError(null);
            } catch (err) {
                setError('Không thể tải danh sách khóa học.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchCourses();
    }, [page, debouncedSearch]);

    const availableFilters = useMemo(() => {
        const examSources = courses.map(name => ({ value: name, label: formatCourseName(name) }));
        return {
            source: { label: 'Bộ đề', options: examSources },
        };
    }, [courses]);

    const filteredCourses = useMemo(() => {
        let filtered = [...courses];

        // Client-side filtering for other filters if needed, 
        // but search is now server-side.
        // Note: activeFilters might need adjustment if we want server-side filtering for them too.
        // For now, we filter the current page's results.

        if (activeFilters.source && activeFilters.source.length > 0) {
            filtered = filtered.filter(courseName => activeFilters.source.includes(courseName));
        }

        return filtered;
    }, [courses, activeFilters]);

    const handleMouseMove = (e) => {
        const card = e.currentTarget;
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        card.style.setProperty('--mouse-x', `${x}px`);
        card.style.setProperty('--mouse-y', `${y}px`);
    };

    const showLoader = loading && !error && courses.length === 0;

    return (
        <>
            <AnimatePresence>
                {showLoader && (
                    <FullPageLoader
                        key="loader"
                        message="Đang tải danh sách bộ đề..."
                        subMessage="Vui lòng chờ trong giây lát, chúng tôi đang tải các bộ đề IELTS khả dụng."
                    />
                )}
            </AnimatePresence>

            <div className="new-courses-page">
                <div
                    className="courses-main-banner"
                    style={{
                        backgroundImage: 'url("https://thumbs.dreamstime.com/b/diverse-group-adult-students-having-conversations-english-speaking-club-diverse-group-people-talking-to-each-other-251584879.jpg")',
                    }}
                >
                    <div className="courses-main-banner__overlay" />
                    <div className="container">
                        <h1 className="courses-main-banner__title">Khám phá các bộ đề IELTS</h1>
                        <p className="courses-main-banner__description">Nâng cao kỹ năng của bạn với các bài thi chất lượng cao từ Cambridge và các nguồn chính thống khác.</p>
                    </div>
                </div>
                <div className="new-courses-container">
                    <div className="courses-controls">
                        <div className="courses-search-container">
                            <FaSearch className="courses-search-icon" />
                            <input
                                type="text"
                                placeholder="Tìm kiếm theo tên bộ đề..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="courses-search-input"
                            />
                        </div>
                        <button type="button" className="btn-filter" onClick={() => setIsFilterModalOpen(true)}>Lọc</button>
                    </div>

                    <div className="courses-header">
                        <h1 className="new-courses-title">Các bộ đề hiện có</h1>
                    </div>

                    {loading && !showLoader && <p>Đang tải...</p>}
                    {error && <p className="courses-no-results">{error}</p>}

                    {!loading && !error && (
                        <>
                            <div className="new-courses-grid">
                                {filteredCourses.map(courseName => (
                                    <div
                                        className="new-course-card"
                                        key={courseName}
                                        onMouseMove={handleMouseMove}
                                    >
                                        <div className="new-course-card__image-container">
                                            <div className="new-course-card__image-placeholder" />
                                        </div>
                                        <div className="new-course-card__content">
                                            <div className="new-course-card__header">
                                                <span className="new-course-card__title">{formatCourseName(courseName)}</span>
                                            </div>
                                            <div className="new-course-card__meta">
                                                <p>Bộ đề thi IELTS chính thức.</p>
                                            </div>
                                            <div className="new-course-card__footer">
                                                <Link to={`/courses/${courseName}`} className="new-course-card__details-button">
                                                    Xem các bài test
                                                </Link>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>

                            {filteredCourses.length === 0 && (
                                <p className="courses-no-results">Không tìm thấy bộ đề nào phù hợp.</p>
                            )}

                            <Pagination
                                currentPage={page}
                                totalPages={totalPages}
                                onPageChange={setPage}
                            />
                        </>
                    )}
                </div>

                <FilterModal
                    isOpen={isFilterModalOpen}
                    onClose={() => setIsFilterModalOpen(false)}
                    onApply={setActiveFilters}
                    availableFilters={availableFilters}
                    currentFilters={activeFilters}
                />
            </div>
        </>
    );
}
