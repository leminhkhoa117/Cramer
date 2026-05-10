import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useCourseStore } from '../stores';
import FilterModal from '../components/FilterModal';
import FullPageLoader from '../components/FullPageLoader';
import '../css/courses.css';

import { FaSearch } from 'react-icons/fa';

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.06 },
  },
};

const cardVariants = {
  hidden: { y: 24, opacity: 0 },
  visible: {
    y: 0,
    opacity: 1,
    transition: { duration: 0.35, ease: 'easeOut' },
  },
};

export default function Courses() {
  const {
    courses,
    loading,
    error,
    searchQuery,
    fetchCoursesV2,
    setSearchQuery,
  } = useCourseStore();

  const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
  const [activeFilters, setActiveFilters] = useState({});

  useEffect(() => {
    fetchCoursesV2();
  }, [fetchCoursesV2]);

  const availableFilters = useMemo(() => {
    const examSources = courses.map((course) => ({
      value: course.code,
      label: course.name || course.code,
    }));
    return {
      source: { label: 'Bộ đề', options: examSources },
    };
  }, [courses]);

  const filteredCourses = useMemo(() => {
    let filtered = [...courses];

    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (course) =>
          (course.name && course.name.toLowerCase().includes(query)) ||
          (course.code && course.code.toLowerCase().includes(query)) ||
          (course.description &&
            course.description.toLowerCase().includes(query))
      );
    }

    if (activeFilters.source && activeFilters.source.length > 0) {
      filtered = filtered.filter((course) =>
        activeFilters.source.includes(course.code)
      );
    }

    return filtered;
  }, [courses, searchQuery, activeFilters]);

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

      <div className="cr-courses-page">
        <section className="cr-courses-hero">
          <div className="cr-courses-hero__overlay" />
          <div className="container">
            <h1 className="cr-courses-hero__title">
              Khám phá các bộ đề IELTS
            </h1>
            <p className="cr-courses-hero__description">
              Nâng cao kỹ năng của bạn với các bài thi chất lượng cao từ
              Cambridge và các nguồn chính thống khác.
            </p>
          </div>
        </section>

        <div className="cr-courses__container">
          <div className="cr-courses-controls">
            <div className="sl-search-container">
              <FaSearch className="cr-courses-controls__search-icon" />
              <input
                type="text"
                placeholder="Tìm kiếm theo tên bộ đề..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="sl-search-input"
              />
            </div>
            <button
              type="button"
              className="sl-btn sl-btn--primary cr-courses-controls__filter-btn"
              onClick={() => setIsFilterModalOpen(true)}
            >
              Lọc
            </button>
          </div>

          {error && (
            <div className="sl-error cr-courses__error">
              {error}
            </div>
          )}

          {!loading && !error && (
            <>
              {filteredCourses.length > 0 ? (
                <motion.div
                  className="cr-courses__grid"
                  variants={containerVariants}
                  initial="hidden"
                  animate="visible"
                  key={searchQuery + JSON.stringify(activeFilters)}
                >
                  {filteredCourses.map((course) => (
                    <motion.div
                      className="cr-courses__card"
                      key={course.code}
                      variants={cardVariants}
                      onMouseMove={handleMouseMove}
                    >
                      <div className="cr-courses__card-image-container">
                        {course.coverImageUrl ? (
                          <img
                            src={course.coverImageUrl}
                            alt={course.name || course.code}
                            className="cr-courses__card-image"
                          />
                        ) : (
                          <div className="cr-courses__card-image-placeholder" />
                        )}
                      </div>
                      <div className="cr-courses__card-content">
                        <h3 className="cr-courses__card-title">
                          {course.name || course.code}
                        </h3>
                        <p className="cr-courses__card-description">
                          {course.description ||
                            `Bộ đề ${course.name || course.code}`}
                        </p>
                        <div className="cr-courses__card-footer">
                          <Link
                            to={`/courses/${course.code}`}
                            className="cr-courses__card-btn"
                          >
                            Xem các bài test
                          </Link>
                        </div>
                      </div>
                    </motion.div>
                  ))}
                </motion.div>
              ) : (
                <div className="sl-empty cr-courses__empty">
                  Không tìm thấy bộ đề nào phù hợp.
                </div>
              )}

              {loading && !showLoader && (
                <div className="cr-courses__loading-more">
                  Đang tải...
                </div>
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
    </>
  );
}
