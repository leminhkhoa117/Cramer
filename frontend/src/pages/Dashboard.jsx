import { useEffect, useMemo, useState } from 'react';
import { FiEdit3 } from 'react-icons/fi';
import { dashboardApi } from '../api/backendApi';
import { useAuth } from '../contexts/AuthContext';
import { motion } from 'framer-motion';
import GoalModal from '../components/GoalModal'; // Import the modal
import heroFallback from '../pictures/cambridge-ielts-17.avif';
import '../css/Dashboard.css';

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

  const skills = summary?.skillSummary ?? [];
  const courses = summary?.courseProgress ?? [];

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

  const formatCourseSeries = (course) => {
    if (!course) return '';
    const source = course.examSource ? course.examSource.toUpperCase().replace('CAM', 'CAMBRIDGE ') : 'IELTS';
    const test = course.testNumber ? `TEST ${course.testNumber}` : '';
    return `${source} ${test}`.trim();
  };

  const formatSkillName = (skill) => {
    if (!skill) return '-';
    const mapping = {
      reading: 'Đọc',
      listening: 'Nghe',
      writing: 'Viết',
      speaking: 'Nói',
    };
    return mapping[skill.toLowerCase()] || skill;
  };

  const formatDate = (value) => {
    if (!value) return 'Chưa có';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Chưa có';
    return date.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  };

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
                    <motion.div className="goal-targets-grid" variants={containerVariants}>
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

                {formattedSkills.length > 0 && (
                  <motion.div className="goal-skills-group" variants={containerVariants}>
                    {formattedSkills.map((skill) => (
                      <motion.div key={skill.label} className="goal-skill" variants={itemVariants}>
                        <div className="goal-skill__name">{skill.label}</div>
                        <div className="goal-skill__score">{skill.score}</div>
                      </motion.div>
                    ))}
                  </motion.div>
                )}
              </>
            )}
          </motion.div>
        </div>
      </section>

      {/* Courses */}
      <section className="dash-courses">
        <div className="container">
          <div className="dash-courses__header">
            <h2>Khoá học của tôi</h2>
            <button type="button" className="btn-filter">Lọc</button>
          </div>

          {courses.length === 0 ? (
            <div className="course-empty">Bạn chưa tham gia khoá học nào.</div>
          ) : (
            <div className="course-grid">
              {courses.map((course) => {
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
                        <span className="dash-course-card__progress-text">{completion}%</span>
                        <span className="dash-course-card__status-badge">{course.status}</span>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </div>
      </section>

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
    </div>
  );
}



