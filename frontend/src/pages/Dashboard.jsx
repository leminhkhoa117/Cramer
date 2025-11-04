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

        <div className="dashboard-hero__content">
          <header className="dashboard-hero__heading">
            <h1 className="dashboard-hero__title">{dashboardData.hero.welcomeMessage}</h1>
          </header>

          <div className="dashboard-hero__goals container-fluid px-0">
            <div className="dashboard-hero__goals-label">{dashboardData.hero.tagline}</div>
            <div className="row gx-4 gy-4 align-items-stretch dashboard-hero__cards">
              {dashboardData.targets.map((target) => (
                <div key={target.label} className="col-12 col-md-6 col-lg-2 d-flex">
                  <article className="dashboard-target-card h-100 w-100">
                    <span className="dashboard-target-card__label">{target.label}</span>
                    <span className="dashboard-target-card__value">{target.value}</span>
                  </article>
                </div>
              ))}

              <div className="col-12 col-md-6 col-lg-8 d-flex">
                <article className="dashboard-skill-card h-100 w-100">
                  <div className="dashboard-skill-card__title">{dashboardData.skills.label}</div>
                  <div className="dashboard-skill-card__items">
                    {dashboardData.skills.items.map((skill) => (
                      <div key={skill.label} className="dashboard-skill-item">
                        <span className="dashboard-skill-item__label">{skill.label}</span>
                        <span className="dashboard-skill-item__score">{skill.score}</span>
                      </div>
                    ))}
                  </div>
                </article>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="dashboard-courses">
        <div className="dashboard-courses__header">
          <div>
            <h2>{dashboardData.coursesSection.title}</h2>
            {dashboardData.coursesSection.note && (
              <p className="dashboard-section-note">{dashboardData.coursesSection.note}</p>
            )}
          </div>
          <button type="button" className="dashboard-courses__filter">
            <FaFilter />
            <span>{dashboardData.coursesSection.filterLabel}</span>
          </button>
        </div>

        <div className="dashboard-course-grid">
          {dashboardData.coursesSection.courses.map((course) => {
            const IconComponent = courseIconMap[course.icon] || FaBookOpen;

            return (
              <article key={course.id} className="dashboard-course-card">
                <div className="dashboard-course-card__icon">
                  <span className="dashboard-course-card__icon-circle">
                    <IconComponent />
                  </span>
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
