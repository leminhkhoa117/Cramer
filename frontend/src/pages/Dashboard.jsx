import { useEffect, useMemo, useState } from 'react';
import { FiEdit3 } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../api/backendApi';
import { useAuth } from '../contexts/AuthContext';
import { motion, AnimatePresence } from 'framer-motion';
import GoalModal from '../components/GoalModal';
import FilterModal from '../components/FilterModal';

import '../css/Dashboard.css';
import '../css/FilterModal.css';
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

export default function Dashboard() {
  const { profile, profileLoading, user } = useAuth();
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [isGoalModalOpen, setIsGoalModalOpen] = useState(false);
  const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
  const [activeFilters, setActiveFilters] = useState({});
  const [activeView, setActiveView] = useState('courses');

  // Pagination and Search states
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  // Track the last profile ID we fetched for to avoid unnecessary refetches
  const [lastFetchedProfileId, setLastFetchedProfileId] = useState(null);

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0); // Reset to first page on search change
    }, 500);
    return () => clearTimeout(timer);
  }, [search]);

  const fetchDashboardData = async () => {
    if (!profile?.id) {
      return;
    }

    try {
      console.log('📥 Fetching dashboard summary for:', profile.id);
      setLoading(true);
      // Pass pagination and search params
      const response = await dashboardApi.getSummary(profile.id, page, 3, debouncedSearch);
      setSummary(response.data);

      // Update pagination info from response
      if (response.data.courseProgress && response.data.courseProgress.totalPages !== undefined) {
        setTotalPages(response.data.courseProgress.totalPages);
      }

      setLastFetchedProfileId(profile.id);
      setError(null);
    } catch (err) {
      console.error('Failed to load dashboard summary:', err);
      setError('Không thể tải dữ liệu dashboard.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, [profile?.id, page, debouncedSearch]);

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
      <div className="dash">
        <div className="dash-empty-state">Vui lòng đăng nhập để xem dashboard.</div>
      </div>
    );
  }

  const showLoader = (profileLoading || loading) && (!summary && !error);

  if (error) {
    return (
      <div className="dash">
        <div className="dash-error">{error}</div>
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
        <div className="dash">
          {/* Hero */}
          <section className="dash-hero" style={{ backgroundImage: 'url("https://thumbs.dreamstime.com/b/diverse-group-adult-students-having-conversations-english-speaking-club-diverse-group-people-talking-to-each-other-251584879.jpg")' }}>
            <div className="dash-hero__overlay" aria-hidden="true" />
            <div className="container">
              <h1 className="dash-hero__title">{heroData.welcomeMessage}</h1>

              <div className="dash-hero__subtitle-row">
                <h2 className="dash-hero__subtitle">{heroData.tagline}</h2>
                <button type="button" className="goal-edit-btn" aria-label="Chỉnh sửa mục tiêu" onClick={() => setIsGoalModalOpen(true)}>
                  <FiEdit3 aria-hidden="true" />
                </button>
              </div>

              {/* Glass pill */}
              <motion.div
                className="goals-pill"
                initial="hidden"
                animate="visible"
                variants={containerVariants}
              >
                {(!overallTarget && skillTargets.length === 0) && formattedSkills.length === 0 ? (
                  <div className="goals-pill-empty">
                    Chưa có mục tiêu hay dữ liệu kỹ năng để hiển thị.
                  </div>
                ) : (
                  <>
                    {/* Left Group: Exam Info & Overall */}
                    <motion.div className="goals-group" variants={containerVariants}>
                      <motion.div className="goal-meta-row" variants={containerVariants}>
                        <motion.div className="goal-meta-card" variants={itemVariants}>
                          <p className="goal-meta-card__label">Kì thi sắp tới</p>
                          <p className="goal-meta-card__value" title={targetExamName}>{targetExamName}</p>
                        </motion.div>
                        <motion.div className="goal-meta-card" variants={itemVariants}>
                          <p className="goal-meta-card__label">Ngày thi</p>
                          <p className="goal-meta-card__value">{examDateDisplay}</p>
                        </motion.div>
                        <motion.div className="goal-meta-card goal-meta-card--overall" variants={itemVariants}>
                          <p className="goal-meta-card__label">Mục tiêu chung</p>
                          <p className="goal-meta-card__value goal-meta-card__value--badge">
                            {overallTarget?.value ?? '--'}
                          </p>
                        </motion.div>
                      </motion.div>
                    </motion.div>

                    {/* Divider */}
                    <div className="goal-divider">
                      <span>MỤC TIÊU</span>
                    </div>

                    {/* Right Group: Skill Targets */}
                    <motion.div className="goal-skills-group" variants={containerVariants}>
                      {skillTargets.length > 0 ? (
                        skillTargets.map((target) => (
                          <motion.div key={target.id} className="goal-card" variants={itemVariants}>
                            <div className="goal-card__badge">{target.shortLabel || target.label}</div>
                            <div className="goal-card__value">{target.value}</div>
                          </motion.div>
                        ))
                      ) : (
                        <motion.div className="goal-empty goal-empty--inline" variants={itemVariants}>
                          Chưa có điểm mục tiêu.
                        </motion.div>
                      )}
                    </motion.div>
                  </>
                )}
              </motion.div>
            </div>
          </section>

          <div className="container">
            <nav className="dash-nav">
              <button
                type="button"
                className={`dash-nav-btn ${activeView === 'courses' ? 'active' : ''}`}
                onClick={() => setActiveView('courses')}
              >
                Lịch sử làm bài
              </button>
              <button
                type="button"
                className={`dash-nav-btn ${activeView === 'progress' ? 'active' : ''}`}
                onClick={() => setActiveView('progress')}
              >
                Biểu đồ tiến độ
              </button>
              <button
                type="button"
                className={`dash-nav-btn ${activeView === 'analysis' ? 'active' : ''}`}
                onClick={() => setActiveView('analysis')}
              >
                Phân tích Kỹ năng
              </button>
            </nav>
          </div>

          <main className="dash-main-content">
            {activeView === 'courses' && (
              <section className="dash-courses">
                <div className="container">
                  <div className="dash-courses__header">
                    <h2>Khoá học của tôi</h2>
                    <div className="dash-courses__controls">
                      <div className="dash-search-container">
                        <input
                          type="text"
                          placeholder="Tìm kiếm khoá học..."
                          value={search}
                          onChange={(e) => setSearch(e.target.value)}
                          className="dash-search-input"
                        />
                      </div>
                      <button type="button" className="btn-filter" onClick={() => setIsFilterModalOpen(true)}>Lọc</button>
                    </div>
                  </div>

                  <div className="course-list-container">
                    <AnimatePresence>
                      {loading && (
                        <motion.div
                          className="course-loading-overlay"
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
                      <div className="course-empty">Không tìm thấy khoá học nào khớp với bộ lọc.</div>
                    ) : (
                      <>
                        <div className="course-grid">
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
                                      <span>{course.status}</span>
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
                                      {course.bandScore != null ? (
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
                                              to={`/test/review/${course.attemptId}`}
                                              className="btn-action-dashboard btn-action-dashboard--outline"
                                            >
                                              Xem lại
                                            </Link>
                                          )}
                                          <Link
                                            to={`/test/${course.examSource}/${course.testNumber}/${course.skill}`}
                                            className="btn-action-dashboard"
                                          >
                                            Làm lại
                                          </Link>
                                        </>
                                      )}
                                      {course.status === 'IN_PROGRESS' && (
                                        <Link
                                          to={`/test/${course.examSource}/${course.testNumber}/${course.skill}`}
                                          className="btn-action-dashboard"
                                        >
                                          Tiếp tục
                                        </Link>
                                      )}
                                      {course.status !== 'COMPLETED' && course.status !== 'IN_PROGRESS' && (
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
                                    onAttemptDeleted={fetchDashboardData}
                                  />
                                </div>
                              </article>
                            );
                          })}
                        </div>
                        <Pagination
                          currentPage={page}
                          totalPages={totalPages}
                          onPageChange={setPage}
                        />
                      </>
                    )}
                  </div>
                </div>
              </section>
            )}

            {activeView === 'progress' && (
              <div className="container">
                <ProgressChart data={courses} />
              </div>
            )}

            {activeView === 'analysis' && (
              <div className="container">
                <SkillAnalysis courseData={courses} targets={skillTargets} />
              </div>
            )}
          </main>

          <GoalModal
            isOpen={isGoalModalOpen}
            onClose={() => setIsGoalModalOpen(false)}
            currentTarget={summary?.target}
            onSave={() => {
              setIsGoalModalOpen(false);
              // Invalidate the cache and re-fetch
              setLastFetchedProfileId(null);
              fetchDashboardData();
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



