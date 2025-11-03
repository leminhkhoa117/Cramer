import { useMemo } from 'react';
import { useAuth } from '../contexts/AuthContext';
import mockDashboardData from '../data/mockDashboardData.json';
import '../css/Dashboard.css';
import { FaFilter, FaBookOpen, FaHeadphones, FaMicrophoneAlt } from 'react-icons/fa';
import { FiChevronDown } from 'react-icons/fi';

const courseIconMap = {
  book: FaBookOpen,
  headphones: FaHeadphones,
  microphone: FaMicrophoneAlt,
};

export default function Dashboard() {
  const { profile } = useAuth();

  const dashboardData = useMemo(() => {
    if (!profile?.username) {
      return mockDashboardData;
    }

    return {
      ...mockDashboardData,
      userName: profile.username,
      hero: {
        ...mockDashboardData.hero,
        welcomeMessage: `Chào mừng ${profile.username}!`,
      },
    };
  }, [profile]);

  return (
    <div className="dashboard-page">
      <section className="dashboard-hero">
        <div
          className="dashboard-hero__bg"
          style={{ backgroundImage: `url(${dashboardData.hero.backgroundImage})` }}
          aria-hidden="true"
        />
        <div className="dashboard-hero__overlay" aria-hidden="true" />

        <div className="dashboard-hero__shell">
          <div className="dashboard-hero__panel">
            <div className="dashboard-hero__intro">
              <span className="dashboard-hero__eyebrow">{dashboardData.hero.greeting}</span>
              <h1 className="dashboard-hero__title">{dashboardData.hero.welcomeMessage}</h1>
            </div>

            <div className="dashboard-goal-row">
              <div className="dashboard-goal-label">{dashboardData.hero.tagline}</div>
              <div className="dashboard-goal-grid">
                {dashboardData.targets.map((target) => (
                  <article key={target.label} className="dashboard-goal-card">
                    <span className="dashboard-goal-card__label">{target.label}</span>
                    <span className="dashboard-goal-card__value">{target.value}</span>
                  </article>
                ))}

                <article className="dashboard-goal-card dashboard-goal-card--skills">
                  <header className="dashboard-goal-card__header">{dashboardData.skills.label}</header>
                  <div className="dashboard-skill-grid">
                    {dashboardData.skills.items.map((skill) => (
                      <div key={skill.label} className="dashboard-skill-pill">
                        <span className="dashboard-skill-pill__label">{skill.label}</span>
                        <span className="dashboard-skill-pill__score">{skill.score}</span>
                      </div>
                    ))}
                  </div>
                </article>
              </div>
            </div>

            <button type="button" className="dashboard-filter-floating">
              <FaFilter />
              <span>{dashboardData.coursesSection.filterLabel}</span>
            </button>
          </div>
        </div>
      </section>

      <section className="dashboard-courses">
        <div className="dashboard-section-header">
          <h2>{dashboardData.coursesSection.title}</h2>
          {dashboardData.coursesSection.note && (
            <p className="dashboard-section-note">{dashboardData.coursesSection.note}</p>
          )}
        </div>

        <div className="dashboard-course-grid">
          {dashboardData.coursesSection.courses.map((course) => {
            const IconComponent = courseIconMap[course.icon] || FaBookOpen;

            return (
              <article key={course.id} className="dashboard-course-card">
                <div className="dashboard-course-card__badge">
                  <IconComponent />
                </div>

                <div className="dashboard-course-card__body">
                  <span className="dashboard-course-card__series">{course.series}</span>
                  <div className="dashboard-course-card__meta">
                    <span><strong>Kỹ năng:</strong> {course.skill}</span>
                    <span><strong>Thời gian hoàn thành:</strong> {course.duration}</span>
                    <span><strong>Lần làm gần nhất:</strong> {course.lastAttempt}</span>
                  </div>
                  {course.status && (
                    <span className={`dashboard-course-card__status dashboard-course-card__status--${course.status === 'Hoàn thành' ? 'done' : 'progress'}`}>
                      {course.status}
                    </span>
                  )}
                </div>

                <div className="dashboard-course-card__score">
                  <span className="dashboard-course-card__score-value">{course.score}</span>
                  <span className="dashboard-course-card__score-label">Band: {course.band}</span>
                </div>
              </article>
            );
          })}
        </div>

        <button type="button" className="dashboard-view-all">
          <span>{dashboardData.coursesSection.viewAllLabel}</span>
          <FiChevronDown />
        </button>
      </section>
    </div>
  );
}
