import { useMemo } from 'react';
import { useAuth } from '../contexts/AuthContext';
import mockDashboardData from '../data/mockDashboardData.json';
import '../css/Dashboard.css';

export default function Dashboard() {
  const { profile } = useAuth();

  const data = useMemo(() => {
    const base = mockDashboardData;
    if (!profile?.username) return base;
    return {
      ...base,
      userName: profile.username,
      hero: {
        ...base.hero,
        welcomeMessage: `Chào mừng, ${profile.username}!`,
      },
    };
  }, [profile]);

  return (
    <div className="dash">
      {/* Hero */}
      <section className="dash-hero" style={{ backgroundImage: `url(${data.hero.backgroundImage})` }}>
        <div className="dash-hero__overlay" aria-hidden="true" />
        <div className="container">
          <h1 className="dash-hero__title">{data.hero.welcomeMessage}</h1>

          <h2 className="dash-hero__subtitle">{data.hero.tagline || 'Mục tiêu của tôi'}</h2>

          {/* Glass pill */}
          <div className="goals-pill">
            <div className="goals-group">
              <div className="goal-targets-row">
                {data.targets.map((t) => (
                  <div key={t.label} className="goal-card">
                    <div className="goal-card__badge">{t.label}</div>
                    <div className="goal-card__value">{t.value}</div>
                  </div>
                ))}
              </div>

              <div className="goal-divider">
                <span>Điểm kỹ năng</span>
              </div>
            </div>

            <div className="goal-skills-group">
              {data.skills.items.map((s) => (
                <div key={s.label} className="goal-skill">
                  <div className="goal-skill__name">{s.label.split(' - ')[0]}</div>
                  <div className="goal-skill__score">{s.score}</div>
                </div>
              ))}
            </div>
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

          <div className="course-grid">
            {data.coursesSection.courses.map((c) => (
              <article key={c.id} className="course-card">
                <div className="course-card__thumb" style={{ backgroundImage: `url(/src/pictures/cambridge-ielts-17.jpg)` }}>
                  {/* Image is now a background */}
                </div>
                <div className="course-card__body">
                  <div>
                    <div className="course-card__series">{c.series}</div>
                    <div className="course-card__meta">
                      <div><strong>Kỹ năng:</strong> {c.skill}</div>
                      <div><strong>Hoàn thành trong:</strong> {c.duration}</div>
                      <div><strong>Lần làm gần nhất:</strong> {c.lastAttempt}</div>
                    </div>
                  </div>
                  <div className="course-card__footer">
                    <div className="course-card__score">{c.score}</div>
                    <div className="course-card__band">Band {c.band}</div>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
