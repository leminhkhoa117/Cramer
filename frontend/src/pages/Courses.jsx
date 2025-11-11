import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { courseApi } from '../api/backendApi';
import FilterModal from '../components/FilterModal';
import './../css/Courses.css';
import heroFallback from '../pictures/cambridge-ielts-17.avif';
import { FaSearch } from 'react-icons/fa';

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
    const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
    const [activeFilters, setActiveFilters] = useState({});

    useEffect(() => {
        const fetchCourses = async () => {
            try {
                setLoading(true);
                const response = await courseApi.getAll();
                setCourses(response.data);
                setError(null);
            } catch (err) {
                setError('Không thể tải danh sách khóa học.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchCourses();
    }, []);

    const availableFilters = useMemo(() => {
        const examSources = courses.map(name => ({ value: name, label: formatCourseName(name) }));
        return {
            source: { label: 'Bộ đề', options: examSources },
        };
    }, [courses]);

    const filteredCourses = useMemo(() => {
        let filtered = [...courses];

        // Apply search term
        if (searchTerm) {
            filtered = filtered.filter(courseName =>
                formatCourseName(courseName).toLowerCase().includes(searchTerm.toLowerCase())
            );
        }

        // Apply active filters
        if (activeFilters.source && activeFilters.source.length > 0) {
            filtered = filtered.filter(courseName => activeFilters.source.includes(courseName));
        }

        return filtered;
    }, [courses, searchTerm, activeFilters]);


    return (
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
                    <h1 className="new-courses-title">Các bộ đề hiện có ({filteredCourses.length})</h1>
                </div>

                {loading && <p>Đang tải...</p>}
                {error && <p className="courses-no-results">{error}</p>}

                {!loading && !error && (
                    <>
                        <div className="new-courses-grid">
                            {filteredCourses.map(courseName => (
                                <div className="new-course-card" key={courseName}>
                                    <div className="new-course-card__image-container">
                                        <img src={heroFallback} alt={formatCourseName(courseName)} className="new-course-card__image" />
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
    );
}