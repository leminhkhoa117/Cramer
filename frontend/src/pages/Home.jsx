import React from 'react';
import { FaBookOpen, FaStopwatch, FaStar } from 'react-icons/fa';
import '../css/Home.css';

const Feature = ({ icon, title, children }) => (
  <div className="feature-card">
    <div className="feature-card__icon">{icon}</div>
    <h3 className="feature-card__title">{title}</h3>
    <p className="feature-card__text">{children}</p>
  </div>
);

export default function Home() {
  const handleSearch = (e) => {
    e.preventDefault();
    // Handle search logic
  };

  return (
    <main className="home-page">
      {/* Hero Section */}
      <section
        className="home-hero"
        style={{
          backgroundImage:
            'url("https://thumbs.dreamstime.com/b/diverse-group-adult-students-having-conversations-english-speaking-club-diverse-group-people-talking-to-each-other-251584879.jpg")',
        }}
      >
        <div className="home-hero__overlay" />
        <div className="container">
          <div className="home-hero__content">
            <h1 className="home-hero__title">Welcome to Cramer</h1>
            <p className="home-hero__subtitle">
              Your place to practice and improve your English skills for the IELTS exam.
            </p>
            <div className="home-hero__actions">
              <button type="button" className="btn btn-primary">
                Browse Courses
              </button>
              <button type="button" className="btn btn-secondary">
                Get Started
              </button>
            </div>
          </div>

          <div className="home-search">
            <div className="search-box">
              <h2 className="search-box__title">Find a Test</h2>
              <form className="search-form" onSubmit={handleSearch}>
                <div className="form-group">
                  <input
                    type="text"
                    placeholder="Search for a test..."
                    className="form-control"
                  />
                </div>
                <div className="search-form__row">
                  {/* These should be replaced with custom dropdown components for full styling control */}
                  <select className="form-control" defaultValue="">
                    <option value="" disabled hidden>IELTS Cambridge</option>
                    <option>Cambridge 18</option>
                    <option>Cambridge 17</option>
                  </select>
                  <select className="form-control" defaultValue="">
                    <option value="" disabled hidden>Skill</option>
                    <option>Speaking</option>
                    <option>Reading</option>
                  </select>
                </div>
                <button type="submit" className="btn-submit">
                  Search
                </button>
              </form>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="home-features">
        <div className="container">
          <h2 className="home-features__title">What you'll get</h2>
          <div className="features-grid">
            <Feature icon={<FaBookOpen />} title="Personalized practice">
              Tailored exercises to fit your learning style and pace.
            </Feature>
            <Feature icon={<FaStopwatch />} title="Real exam simulations">
              Experience the pressure and timing of the actual IELTS test.
            </Feature>
            <Feature icon={<FaStar />} title="Expert feedback">
              Receive detailed feedback from certified English instructors.
            </Feature>
          </div>
        </div>
      </section>
    </main>
  );
}
