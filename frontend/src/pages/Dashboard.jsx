import { useEffect, useMemo, useState } from 'react';
import { FiEdit3 } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../api/backendApi';
import { useAuth } from '../contexts/AuthContext';
import { motion } from 'framer-motion';
import GoalModal from '../components/GoalModal';
import FilterModal from '../components/FilterModal';
import heroFallback from '../pictures/cambridge-ielts-17.avif';
import '../css/Dashboard.css';
import '../css/FilterModal.css';
import ProgressChart from '../components/ProgressChart';
import SkillAnalysis from '../components/SkillAnalysis';
import '../css/ProgressChart.css';
import '../css/SkillAnalysis.css';

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
  
  // Track the last profile ID we fetched for to avoid unnecessary refetches
  const [lastFetchedProfileId, setLastFetchedProfileId] = useState(null);

  // Use useEffect directly instead of useCallback to avoid dependency issues
  const fetchDashboardData = async () => {
    if (!profile?.id) {
      return;
    }
    
    // Don't fetch if we already have data for this profile
    if (profile.id === lastFetchedProfileId) {
      console.log('📊 Dashboard data already loaded for this user, skipping fetch');
      return;
    }
    
    try {
      console.log('📥 Fetching dashboard summary for:', profile.id);
      setLoading(true);
      const response = await dashboardApi.getSummary(profile.id);
      setSummary(response.data);
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
  }, [profile?.id, lastFetchedProfileId]); // Only depend on profile.id and lastFetchedProfileId

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

  const courses = summary?.courseProgress ?? [];
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
      backgroundImage: heroFallback,
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

  if (profileLoading || loading) {
    return (
      <div className="dash">
        <div className="dash-loading">Đang tải dữ liệu...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dash">
        <div className="dash-error">{error}</div>
      </div>
    );
  }

  return (
    <div className="dash">
      {/* Hero */}
      <section className="dash-hero" style={{ backgroundImage: `url(${heroData.backgroundImage})` }}>
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
                <motion.div className="goals-group" variants={containerVariants}>
                  <motion.div className="goal-meta-row" variants={containerVariants}>
                    <motion.div className="goal-meta-card" variants={itemVariants}>
                      <p className="goal-meta-card__label">Kì thi sắp tới</p>
                      <p className="goal-meta-card__value">{targetExamName}</p>
                    </motion.div>
                    <motion.div className="goal-meta-card" variants={itemVariants}>
                      <p className="goal-meta-card__label">Ngày thi</p>
                      <p className="goal-meta-card__value">{examDateDisplay}</p>
                    </motion.div>
                    <motion.div className="goal-meta-card goal-meta-card--overall" variants={itemVariants}>
                      <p className="goal-meta-card__label">Điểm kỹ năng</p>
                      <p className="goal-meta-card__value goal-meta-card__value--badge">
                        {overallTarget?.value ?? '--'}
                      </p>
                      <span className="goal-meta-card__hint">Band tổng kỳ vọng</span>
                    </motion.div>
                  </motion.div>

                  {skillTargets.length > 0 ? (
                    <motion.div className="goal-targets-grid mobile-margin-top" variants={containerVariants}>
                      {skillTargets.map((target) => (
                        <motion.div key={target.id} className="goal-card" variants={itemVariants}>
                          <div className="goal-card__badge">{target.shortLabel || target.label}</div>
                          <div className="goal-card__value">{target.value}</div>
                        </motion.div>
                      ))}
                    </motion.div>
                  ) : (
                    <motion.div className="goal-empty goal-empty--inline" variants={itemVariants}>
                      Chưa có điểm mục tiêu. Nhấn biểu tượng cây bút để thiết lập.
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
                <button type="button" className="btn-filter" onClick={() => setIsFilterModalOpen(true)}>Lọc</button>
              </div>

              {filteredCourses.length === 0 ? (
                <div className="course-empty">Không tìm thấy khoá học nào khớp với bộ lọc.</div>
              ) : (
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
                      >
                        <div className="dash-course-card__image-container">
                          <img src={heroFallback} alt={formatCourseSeries(course)} className="dash-course-card__image" />
                        </div>
                        <div className="dash-course-card__content">
                          <h3 className="dash-course-card__title">{formatCourseSeries(course)}</h3>
                          <div className="dash-course-card__meta">
                            <p><strong>Kỹ năng:</strong> {formatSkillName(course.skill)}</p>
                            <p><strong>Đã làm:</strong> {course.answersAttempted}</p>
                            <p><strong>Tổng số câu:</strong> {course.totalQuestions || '—'}</p>
                            <p><strong>Đúng:</strong> {course.correctAnswers}</p>
                            <p><strong>Lần làm gần nhất:</strong> {formatDate(course.lastAttempt)}</p>
                          </div>
                          <div className="dash-course-card__footer">
                            <div className="dash-course-card__score-status">
                              {course.bandScore != null ? (
                                <span className="dash-course-card__progress-text">
                                  Band {course.bandScore.toFixed(1)}
                                </span>
                              ) : (
                                <span className="dash-course-card__progress-text">{completion}%</span>
                              )}
                              <span className="dash-course-card__status-badge">{course.status}</span>
                            </div>
                                                                                                    {course.status === 'COMPLETED' && (
                                                                                                      <Link to={`/test/${course.examSource}/${course.testNumber}/${course.skill}`} className="btn-action-dashboard">
                                                                                                        Làm bài lại
                                                                                                      </Link>
                                                                                                    )}
                                                                                                    {course.status === 'IN_PROGRESS' && (
                                                                                                      <Link to={`/test/${course.examSource}/${course.testNumber}/${course.skill}`} className="btn-action-dashboard">
                                                                                                        Tiếp tục làm
                                                                                                      </Link>
                                                                                                    )}
                                                                                                  </div>                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
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
  );
}



