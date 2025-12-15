import { useEffect, useMemo, useState, useCallback } from 'react';
import { FiEdit3, FiClock, FiTrendingUp, FiPieChart, FiTarget, FiChevronRight } from 'react-icons/fi';
import { Link, useLocation } from 'react-router-dom';
import { useAuthStore, useProfileStore, useDashboardStore } from '../stores';
import { motion, AnimatePresence } from 'framer-motion';
import GoalModal from '../components/GoalModal';
import FilterModal from '../components/FilterModal';

import '../css/common/SidebarLayout.css';
import '../css/Dashboard.css';
import ProgressChart from '../components/ProgressChart';
import SkillAnalysis from '../components/SkillAnalysis';
import '../css/ProgressChart.css';
import '../css/SkillAnalysis.css';
import FullPageLoader from '../components/FullPageLoader';
import Pagination from '../components/Pagination';
import AttemptHistoryDropdown from '../components/AttemptHistoryDropdown';

// Helper functions moved to the top level
const formatCourseSeries = (course) => `Cambridge ${course.examSource.substring(3)} - Test ${course.testNumber}`;

const formatSkillName = (skill) => {
  if (!skill) return '';
  return skill.charAt(0).toUpperCase() + skill.slice(1);
};

const formatDate = (dateString) => {
  if (!dateString) return 'Chưa có';
  return new Date(dateString).toLocaleDateString('vi-VN');
};

// Map status codes to Vietnamese labels
const formatStatus = (status) => {
  const statusMap = {
    'COMPLETED': 'Hoàn thành',
    'IN_PROGRESS': 'Đang làm',
    'GRADING': 'Đang chấm',
    'PENDING': 'Chờ xử lý',
    'FAILED': 'Lỗi',
    'NOT_STARTED': 'Chưa bắt đầu',
  };
  return statusMap[status] || status;
};

// Animation variants for staggering children
const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1
    }
  }
};

const itemVariants = {
  hidden: { y: 20, opacity: 0 },
  visible: {
    y: 0,
    opacity: 1
  }
};

// Tab definitions for sidebar navigation
const dashboardTabs = [
  { id: 'courses', label: 'Lịch sử làm bài', icon: FiClock },
  { id: 'progress', label: 'Biểu đồ tiến độ', icon: FiTrendingUp },
  { id: 'analysis', label: 'Phân tích Kỹ năng', icon: FiPieChart },
];

export default function Dashboard() {
  // Auth & Profile from Zustand stores
  const user = useAuthStore(state => state.user);
  const profile = useProfileStore(state => state.profile);
  const profileLoading = useProfileStore(state => state.loading);

  // Dashboard store for summary data, loading, error, and pagination
  const {
    summary,
    loading,
    error,
    currentPage,
    totalPages,
    searchQuery,
    debouncedSearchQuery,
    fetchSummary,
    refreshSummary,
    setPage,
    setSearchQuery,
    setDebouncedSearchQuery,
    updateSummary,
  } = useDashboardStore();

  const location = useLocation();
  const [isGoalModalOpen, setIsGoalModalOpen] = useState(false);
  const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
  const [activeFilters, setActiveFilters] = useState({});
  const [activeView, setActiveView] = useState('courses');

  // Handle refresh request from navigation (e.g., after cancelling test)
  useEffect(() => {
    console.log('📍 Dashboard location.state:', location.state);
    if (location.state?.refreshData) {
      console.log('🔄 Refresh triggered from navigation state!');
      // Clear the navigation state to prevent re-refresh on subsequent renders
      window.history.replaceState({}, document.title);
      // Force refresh using store action
      refreshSummary();
    }
  }, [location.state, refreshSummary]);

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchQuery(searchQuery);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchQuery, setDebouncedSearchQuery]);

  // Fetch dashboard data using store action
  useEffect(() => {
    if (!profile?.id) {
      return;
    }
    console.log('📥 Fetching dashboard summary via store');
    fetchSummary(currentPage, 4, debouncedSearchQuery).catch(err => {
      console.error('Failed to load dashboard summary:', err);
    });
  }, [profile?.id, currentPage, debouncedSearchQuery, fetchSummary]);

  // Callback for refreshing data (used by child components)
  const handleRefreshData = useCallback(() => {
    refreshSummary();
  }, [refreshSummary]);

  const { overallTarget, skillTargets } = useMemo(() => {
    if (!summary?.target) {
      return { overallTarget: null, skillTargets: [] };
    }

    const skillMeta = {
      listening: { label: 'Listening', shortLabel: 'Nghe' },
      speaking: { label: 'Speaking', shortLabel: 'Nói' },
      reading: { label: 'Reading', shortLabel: 'Đọc' },
      writing: { label: 'Writing', shortLabel: 'Viết' },
    };

    const overallScore = summary.target.overall ?? (() => {
      const scores = [
        summary.target.listening,
        summary.target.reading,
        summary.target.writing,
        summary.target.speaking,
      ].filter((s) => s !== null && s !== undefined);

      if (scores.length === 0) return null;

      const sum = scores.reduce((acc, score) => acc + score, 0);
      const avg = sum / scores.length;
      return Math.round(avg * 2) / 2;
    })();

    const allTargets = {
      ...summary.target,
      overall: overallScore,
    };

    let overall = null;
    if (allTargets.overall !== null && allTargets.overall !== undefined) {
      overall = {
        label: 'Overall',
        value: allTargets.overall.toFixed(1),
      };
    }

    const skillsOrder = ['listening', 'speaking', 'reading', 'writing'];
    const sTargets = skillsOrder
      .map((key) => {
        const value = allTargets[key];
        const meta = skillMeta[key];
        if (meta && value !== null && value !== undefined) {
          return {
            id: key,
            label: meta.label,
            shortLabel: meta.shortLabel,
            value: value.toFixed(1),
          };
        }
        return null;
      })
      .filter(Boolean);

    return { overallTarget: overall, skillTargets: sTargets };
  }, [summary?.target]);

  const targetExamName = summary?.target?.examName || 'Chưa đặt';
  const examDateDisplay = useMemo(() => {
    const isoDate = summary?.target?.examDate;
    if (!isoDate) return 'Chưa đặt';
    const parsed = new Date(isoDate);
    if (Number.isNaN(parsed.getTime())) {
      return isoDate;
    }
    return parsed.toLocaleDateString('vi-VN', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }, [summary?.target?.examDate]);

  // Handle paginated course progress
  const courses = summary?.courseProgress?.content ?? [];
  const skills = summary?.skillSummary ?? [];

  const availableFilters = useMemo(() => {
    const skills = [...new Set(courses.map(c => c.skill))].map(s => ({ value: s, label: formatSkillName(s) }));
    const statuses = [...new Set(courses.map(c => c.status))].map(s => ({ value: s, label: s }));

    return {
      skill: { label: 'Kỹ năng', options: skills },
      status: { label: 'Trạng thái', options: statuses },
    };
  }, [courses]);

  const filteredCourses = useMemo(() => {
    if (Object.keys(activeFilters).length === 0) {
      return courses;
    }
    return courses.filter(course => {
      return Object.entries(activeFilters).every(([group, values]) => {
        if (!values || values.length === 0) return true;
        return values.includes(course[group]);
      });
    });
  }, [courses, activeFilters]);

  const heroData = useMemo(() => {
    const username = summary?.profile?.username || profile?.username || 'bạn';
    return {
      welcomeMessage: `Chào mừng, ${username}!`,
      tagline: 'Mục tiêu của tôi', // Keep it consistent
    };
  }, [profile?.username, summary?.profile?.username]);

  const formattedSkills = useMemo(() => {
    if (!skills.length) return [];
    const capitalize = (value) => value ? value.charAt(0).toUpperCase() + value.slice(1) : '';
    const viLabel = (skill) => {
      const mapping = {
        reading: 'Reading - Đọc',
        listening: 'Listening - Nghe',
        writing: 'Writing - Viết',
        speaking: 'Speaking - Nói',
      };
      return mapping[skill?.toLowerCase()] || capitalize(skill);
    };
    return skills.map((skill) => ({
      label: viLabel(skill.skill),
      score: `${Math.round(skill.accuracy)}%`,
    }));
  }, [skills]);

  if (!user && !profileLoading) {
    return (
      <div className="sl-page">
        <div className="sl-empty" style={{ margin: '4rem auto', maxWidth: '480px' }}>Vui lòng đăng nhập để xem dashboard.</div>
      </div>
    );
  }

  const showLoader = (profileLoading || loading) && (!summary && !error);

  if (error) {
    return (
      <div className="sl-page">
        <div className="sl-error">{error}</div>
      </div>
    );
  }

  return (
    <>
      <AnimatePresence>
        {showLoader && (
          <FullPageLoader
            key="loader"
            message="Đang tải bảng điều khiển học tập..."
            subMessage="Chúng tôi đang tổng hợp tiến độ và gợi ý lộ trình học cho bạn."
          />
        )}
      </AnimatePresence>
      {summary && (
        <div
          className="sl-page dashboard-page"
          style={profile?.pageBackgroundUrl ? {
            backgroundImage: `url(${profile.pageBackgroundUrl})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            backgroundAttachment: 'fixed'
          } : undefined}
        >
          {/* Overlay for readability when background image is set */}
          {profile?.pageBackgroundUrl && (
            <div className="sl-page__overlay" />
          )}

          {/* Main Layout: Sidebar + Content */}
          <div className="sl-layout container">
            {/* Left Sidebar */}
            <aside className="sl-sidebar">
              {/* Cover Image Banner */}
              <div className="sl-sidebar__cover">
                {profile?.heroBackgroundUrl ? (
                  <img
                    src={profile.heroBackgroundUrl}
                    alt="Ảnh bìa"
                    className="sl-sidebar__cover-img"
                  />
                ) : (
                  <div className="sl-sidebar__cover-placeholder" />
                )}
              </div>

              {/* Sidebar Header */}
              <div className="sl-sidebar__header">
                <h1 className="sl-sidebar__title">{heroData.welcomeMessage}</h1>
                <p className="sl-sidebar__subtitle">Bảng điều khiển học tập</p>
              </div>

              {/* Sidebar Navigation */}
              <nav className="sl-sidebar__nav">
                {dashboardTabs.map(tab => {
                  const IconComponent = tab.icon;
                  return (
                    <button
                      key={tab.id}
                      type="button"
                      className={`sl-sidebar__nav-btn ${activeView === tab.id ? 'active' : ''}`}
                      onClick={() => setActiveView(tab.id)}
                    >
                      <IconComponent />
                      <span>{tab.label}</span>
                      <FiChevronRight className="sl-sidebar__nav-arrow" />
                    </button>
                  );
                })}
              </nav>

              {/* Goals Section */}
              <div className="sl-sidebar__section dashboard-goals">
                <div className="sl-sidebar__section-header">
                  <div className="sl-sidebar__section-title">
                    <FiTarget />
                    <span>Mục tiêu của tôi</span>
                  </div>
                  <button
                    type="button"
                    className="sl-edit-btn"
                    aria-label="Chỉnh sửa mục tiêu"
                    onClick={() => setIsGoalModalOpen(true)}
                  >
                    <FiEdit3 />
                  </button>
                </div>

                {(!overallTarget && skillTargets.length === 0) ? (
                  <div className="sl-empty dashboard-goals__empty">
                    Chưa có mục tiêu. Nhấn nút chỉnh sửa để đặt mục tiêu.
                  </div>
                ) : (
                  <motion.div
                    className="dashboard-goals__content"
                    initial="hidden"
                    animate="visible"
                    variants={containerVariants}
                  >
                    {/* Exam Info */}
                    <motion.div className="dashboard-goal-meta" variants={itemVariants}>
                      <div className="dashboard-goal-meta__item">
                        <span className="dashboard-goal-meta__label">Kì thi</span>
                        <span className="dashboard-goal-meta__value" title={targetExamName}>{targetExamName}</span>
                      </div>
                      <div className="dashboard-goal-meta__item">
                        <span className="dashboard-goal-meta__label">Ngày thi</span>
                        <span className="dashboard-goal-meta__value">{examDateDisplay}</span>
                      </div>
                    </motion.div>

                    {/* Overall Target */}
                    <motion.div className="dashboard-goal-overall" variants={itemVariants}>
                      <span className="dashboard-goal-overall__label">Mục tiêu chung</span>
                      <span className="dashboard-goal-overall__value">{overallTarget?.value ?? '--'}</span>
                    </motion.div>

                    {/* Skill Targets - 2x2 Grid */}
                    {skillTargets.length > 0 && (
                      <motion.div className="dashboard-goal-skills" variants={containerVariants}>
                        {skillTargets.map((target) => (
                          <motion.div key={target.id} className="dashboard-goal-skill" variants={itemVariants}>
                            <span className="dashboard-goal-skill__label">{target.shortLabel}</span>
                            <span className="dashboard-goal-skill__value">{target.value}</span>
                          </motion.div>
                        ))}
                      </motion.div>
                    )}
                  </motion.div>
                )}
              </div>
            </aside>

            {/* Right Content Area */}
            <main className="sl-content">
              <AnimatePresence mode="wait">
                {activeView === 'courses' && (
                  <motion.div
                    key="courses"
                    className="sl-tab-panel"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    transition={{ duration: 0.3 }}
                  >
                    {/* Courses Card */}
                    <div className="sl-card">
                      <div className="sl-card__header">
                        <h2 className="sl-card__title">
                          <FiClock />
                          Lịch sử làm bài
                        </h2>
                        <div className="sl-card__controls">
                          <div className="sl-search-container">
                            <input
                              type="text"
                              placeholder="Tìm kiếm khoá học..."
                              value={searchQuery}
                              onChange={(e) => setSearchQuery(e.target.value)}
                              className="sl-search-input"
                            />
                          </div>
                          <button type="button" className="sl-btn sl-btn--secondary" onClick={() => setIsFilterModalOpen(true)}>Lọc</button>
                        </div>
                      </div>

                      <div className="dashboard-course-list">
                        <AnimatePresence>
                          {loading && (
                            <motion.div
                              className="sl-loading-overlay"
                              initial={{ opacity: 0 }}
                              animate={{ opacity: 1 }}
                              exit={{ opacity: 0 }}
                              transition={{ duration: 0.2 }}
                            >
                              <span>Đang tải...</span>
                            </motion.div>
                          )}
                        </AnimatePresence>

                        {filteredCourses.length === 0 && !loading ? (
                          <div className="sl-empty">Không tìm thấy khoá học nào khớp với bộ lọc.</div>
                        ) : (
                          <>
                            <div className="dashboard-course-grid">
                              {filteredCourses.map((course) => {
                                const completion = Math.min(
                                  100,
                                  Math.max(0, Math.round((course.completionRate ?? 0) * 100))
                                );

                                return (
                                  <article
                                    key={`${course.examSource}-${course.testNumber}-${course.skill}`}
                                    className="dash-course-card"
                                    onMouseMove={(e) => {
                                      const card = e.currentTarget;
                                      const rect = card.getBoundingClientRect();
                                      const x = e.clientX - rect.left;
                                      const y = e.clientY - rect.top;
                                      card.style.setProperty('--mouse-x', `${x}px`);
                                      card.style.setProperty('--mouse-y', `${y}px`);
                                    }}
                                  >
                                    <div className="dash-course-card__image-container">
                                      <div className="dash-course-card__image-placeholder" />
                                    </div>
                                    <div className="dash-course-card__content">
                                      <h3 className="dash-course-card__title">{formatCourseSeries(course)}</h3>

                                      <div className="dash-course-card__meta">
                                        <p>
                                          <strong>Kỹ năng</strong>
                                          <span>{formatSkillName(course.skill)}</span>
                                        </p>
                                        <p>
                                          <strong>Trạng thái</strong>
                                          <span className={`status-${course.status?.toLowerCase()}`}>
                                            {formatStatus(course.status)}
                                          </span>
                                        </p>
                                        <p>
                                          <strong>Đã làm</strong>
                                          <span>{course.answersAttempted}/{course.totalQuestions || '?'} câu</span>
                                        </p>
                                        <p>
                                          <strong>Đúng</strong>
                                          <span>{course.correctAnswers} câu</span>
                                        </p>
                                        <p style={{ gridColumn: '1 / -1' }}>
                                          <strong>Lần làm gần nhất</strong>
                                          <span>{formatDate(course.lastAttempt)}</span>
                                        </p>
                                      </div>

                                      <div className="dash-course-card__footer">
                                        <div className="dash-course-card__score-status">
                                          <span className="dash-course-card__status-badge">Kết quả</span>
                                          {course.status === 'GRADING' ? (
                                            <span className="dash-course-card__progress-text grading">
                                              Đang chấm...
                                            </span>
                                          ) : course.bandScore != null ? (
                                            <span className="dash-course-card__progress-text">
                                              Band {course.bandScore.toFixed(1)}
                                            </span>
                                          ) : (
                                            <span className="dash-course-card__progress-text">{completion}%</span>
                                          )}
                                        </div>
                                        <div className="dash-course-card__actions">
                                          {course.status === 'COMPLETED' && (
                                            <>
                                              {course.attemptId && (
                                                <Link
                                                  to={course.skill === 'writing'
                                                    ? `/test/writing/review/${course.attemptId}`
                                                    : `/test/review/${course.attemptId}`
                                                  }
                                                  className="btn-action-dashboard btn-action-dashboard--outline"
                                                >
                                                  Xem lại
                                                </Link>
                                              )}
                                              <Link
                                                to={course.skill === 'writing'
                                                  ? `/test/writing/${course.examSource}/${course.testNumber}`
                                                  : `/test/${course.examSource}/${course.testNumber}/${course.skill}`
                                                }
                                                state={{ forceNew: true }}
                                                className="btn-action-dashboard"
                                              >
                                                Làm lại
                                              </Link>
                                            </>
                                          )}
                                          {course.status === 'IN_PROGRESS' && (
                                            <Link
                                              to={course.skill === 'writing'
                                                ? `/test/writing/${course.examSource}/${course.testNumber}`
                                                : `/test/${course.examSource}/${course.testNumber}/${course.skill}`
                                              }
                                              className="btn-action-dashboard"
                                            >
                                              Tiếp tục
                                            </Link>
                                          )}
                                          {course.status === 'GRADING' && (
                                            <Link
                                              to={course.skill === 'writing'
                                                ? `/test/writing/review/${course.attemptId}`
                                                : `/test/review/${course.attemptId}`
                                              }
                                              className="btn-action-dashboard btn-action-dashboard--grading"
                                            >
                                              Xem tiến độ
                                            </Link>
                                          )}
                                          {course.status !== 'COMPLETED' && course.status !== 'IN_PROGRESS' && course.status !== 'GRADING' && (
                                            <Link
                                              to={`/test/${course.examSource}/${course.testNumber}/${course.skill}`}
                                              className="btn-action-dashboard"
                                            >
                                              Bắt đầu
                                            </Link>
                                          )}
                                        </div>
                                      </div>

                                      <AttemptHistoryDropdown
                                        history={course.history}
                                        examSource={course.examSource}
                                        testNumber={course.testNumber}
                                        skill={course.skill}
                                        onAttemptDeleted={handleRefreshData}
                                      />
                                    </div>
                                  </article>
                                );
                              })}
                            </div>
                            <Pagination
                              currentPage={currentPage}
                              totalPages={totalPages}
                              onPageChange={setPage}
                            />
                          </>
                        )}
                      </div>
                    </div>
                  </motion.div>
                )}

                {activeView === 'progress' && (
                  <motion.div
                    key="progress"
                    className="sl-tab-panel"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    transition={{ duration: 0.3 }}
                  >
                    <div className="sl-card">
                      <div className="sl-card__header">
                        <h2 className="sl-card__title">
                          <FiTrendingUp />
                          Biểu đồ tiến độ
                        </h2>
                      </div>
                      <ProgressChart data={courses} />
                    </div>
                  </motion.div>
                )}

                {activeView === 'analysis' && (
                  <motion.div
                    key="analysis"
                    className="sl-tab-panel"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    transition={{ duration: 0.3 }}
                  >
                    <div className="sl-card">
                      <div className="sl-card__header">
                        <h2 className="sl-card__title">
                          <FiPieChart />
                          Phân tích Kỹ năng
                        </h2>
                      </div>
                      <SkillAnalysis courseData={courses} targets={skillTargets} />
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </main>
          </div>

          <GoalModal
            isOpen={isGoalModalOpen}
            onClose={() => setIsGoalModalOpen(false)}
            currentTarget={summary?.target}
            onSave={(newTargetData) => {
              setIsGoalModalOpen(false);
              // Safely update summary with null check to prevent TypeError
              updateSummary(prevSummary => {
                if (!prevSummary) {
                  return { target: newTargetData };
                }
                return {
                  ...prevSummary,
                  target: newTargetData,
                };
              });
            }}
          />

          <FilterModal
            isOpen={isFilterModalOpen}
            onClose={() => setIsFilterModalOpen(false)}
            onApply={setActiveFilters}
            availableFilters={availableFilters}
            currentFilters={activeFilters}
          />
        </div>
      )}
    </>
  );
}



