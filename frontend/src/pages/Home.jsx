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
            <h1 className="home-hero__title">Chào mừng đến với Cramer</h1>
            <p className="home-hero__subtitle">
              Nơi luyện tập và cải thiện kỹ năng tiếng Anh của bạn cho kỳ thi IELTS.
            </p>
            <div className="home-hero__actions">
              <button type="button" className="btn btn-primary">
                Xem khóa học
              </button>
              <button type="button" className="btn btn-secondary">
                Bắt đầu ngay
              </button>
            </div>
          </div>

          <div className="home-search">
            <div className="search-box">
              <h2 className="search-box__title">Tìm bài kiểm tra</h2>
              <form className="search-form" onSubmit={handleSearch}>
                <div className="form-group">
                  <input
                    type="text"
                    placeholder="Tìm kiếm bài kiểm tra..."
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
                    <option value="" disabled hidden>Kỹ năng</option>
                    <option>Nói</option>
                    <option>Đọc</option>
                  </select>
                </div>
                <button type="submit" className="btn-submit">
                  Tìm kiếm
                </button>
              </form>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="home-features">
        <div className="container">
          <h2 className="home-features__title">Bạn sẽ nhận được gì?</h2>
          <div className="features-grid">
            <Feature icon={<FaBookOpen />} title="Luyện tập cá nhân hóa">
              Các bài tập được thiết kế riêng để phù hợp với phong cách và tốc độ học của bạn.
            </Feature>
            <Feature icon={<FaStopwatch />} title="Mô phỏng kỳ thi thật">
              Trải nghiệm áp lực và thời gian của kỳ thi IELTS thực tế.
            </Feature>
            <Feature icon={<FaStar />} title="Nhận xét từ chuyên gia">
              Nhận phản hồi chi tiết từ các giảng viên tiếng Anh có chứng chỉ.
            </Feature>
          </div>
        </div>
      </section>
    </main>
  );
}
