import { useEffect, useMemo, useState } from 'react';
import { FiEdit3 } from 'react-icons/fi';
import { dashboardApi } from '../api/backendApi';
import { useAuth } from '../contexts/AuthContext';
import GoalModal from '../components/GoalModal'; // Import the modal
import heroFallback from '../pictures/cambridge-ielts-17.avif';
import '../css/Dashboard.css';

export default function Dashboard() {
  const { profile, profileLoading, user } = useAuth();
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [isGoalModalOpen, setIsGoalModalOpen] = useState(false);
  
  // Track the last profile ID we fetched for to avoid unnecessary refetches
  const [lastFetchedProfileId, setLastFetchedProfileId] = useState(null);

  // Use useEffect directly instead of useCallback to avoid dependency issues
  useEffect(() => {
    const fetchSummary = async () => {
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
    
    fetchSummary();
  }, [profile?.id, lastFetchedProfileId]); // Only depend on profile.id and lastFetchedProfileId

  const targets = summary?.goals ?? [];
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
          <div className="goals-pill">
            {targets.length === 0 && formattedSkills.length === 0 ? (
              <div className="goals-pill-empty">
                Chưa có mục tiêu hay dữ liệu kỹ năng để hiển thị.
              </div>
            ) : (
              <>
                <div className="goals-group">
                  <div className="goal-targets-row">
                    {targets.length > 0 ? (
                      targets.map((target) => (
                        <div key={`${target.label}-${target.value}`} className="goal-card">
                          <div className="goal-card__badge">{target.label}</div>
                          <div className="goal-card__value">{target.value}</div>
                        </div>
                      ))
                    ) : (
                      <div className="goal-empty">Chưa có mục tiêu. Nhấn biểu tượng cây bút để đặt mục tiêu.</div>
                    )}
                  </div>

                  <div className="goal-divider">
                    <span>Điểm kỹ năng</span>
                  </div>
                </div>

                <div className="goal-skills-group">
                  {formattedSkills.length > 0 ? (
                    formattedSkills.map((skill) => (
                      <div key={skill.label} className="goal-skill">
                        <div className="goal-skill__name">{skill.label}</div>
                        <div className="goal-skill__score">{skill.score}</div>
                      </div>
                    ))
                  ) : (
                    <div className="goal-empty">Chưa có dữ liệu kỹ năng.</div>
                  )}
                </div>
              </>
            )}
          </div>
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
                    className="course-card"
                  >
                  <div
                    className="course-card__thumb"
                    style={{ backgroundImage: `url(${heroFallback})` }}
                    aria-hidden="true"
                  />
                  <div className="course-card__body">
                    <div>
                      <div className="course-card__series">{formatCourseSeries(course)}</div>
                      <div className="course-card__meta">
                        <div><strong>Kỹ năng:</strong> {formatSkillName(course.skill)}</div>
                        <div><strong>Tổng số câu:</strong> {course.totalQuestions || '—'}</div>
                        <div><strong>Đã làm:</strong> {course.answersAttempted}</div>
                        <div><strong>Đúng:</strong> {course.correctAnswers}</div>
                        <div><strong>Lần làm gần nhất:</strong> {formatDate(course.lastAttempt)}</div>
                      </div>
                    </div>
                    <div className="course-card__footer">
                      <div className="course-card__score">{completion}%</div>
                      <div className="course-card__status">{course.status}</div>
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
          fetchSummary();
        }}
      />
    </div>
  );
}