import React, { useState, useRef, useEffect } from 'react';
import { FaGoogle, FaArrowRight } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

const SignupSection = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [isHovered, setIsHovered] = useState(false);
  const sectionRef = useRef(null);
  const [isInView, setIsInView] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsInView(true);
          observer.unobserve(entry.target);
        }
      },
      { threshold: 0.1 }
    );

    if (sectionRef.current) {
      observer.observe(sectionRef.current);
    }

    return () => {
      if (sectionRef.current) observer.unobserve(sectionRef.current);
    };
  }, []);

  const handleNavigate = (path, state = {}) => {
    window.scrollTo(0, 0);
    navigate(path, { state });
  };

  const handleQuickSignup = (e) => {
    e.preventDefault();
    handleNavigate('/login', { prefillEmail: email, mode: 'signup' });
  };

  return (
    <section ref={sectionRef} className={`signup-section ${isInView ? 'in-view' : ''}`}>
      {/* CSS animated floating decorative elements */}
      <div className="signup-floating-circle signup-floating-circle--1" />
      <div className="signup-floating-circle signup-floating-circle--2" />

      <div className="signup-container">
        {/* Left content */}
        <div className="signup-content">
          <span className="signup-label">Sẵn sàng chưa?</span>
          <h2 className="signup-title">
            Bắt đầu hành trình
            <br />
            <span className="text-gradient">chinh phục IELTS</span>
          </h2>
          <p className="signup-description">
            Tham gia cùng hàng nghìn học viên đã tin tưởng Cramer để đạt được band điểm mơ ước. 
            Đăng ký miễn phí ngay hôm nay!
          </p>

          {/* Stats */}
          <div className="signup-stats">
            <div className="stat-item">
              <span className="stat-number">10,000+</span>
              <span className="stat-label">Học viên</span>
            </div>
            <div className="stat-divider" />
            <div className="stat-item">
              <span className="stat-number">95%</span>
              <span className="stat-label">Hài lòng</span>
            </div>
            <div className="stat-divider" />
            <div className="stat-item">
              <span className="stat-number">7.0+</span>
              <span className="stat-label">Band trung bình</span>
            </div>
          </div>
        </div>

        {/* Right form */}
        <div 
          className="signup-form-wrapper"
          onMouseEnter={() => setIsHovered(true)}
          onMouseLeave={() => setIsHovered(false)}
        >
          {/* Animated border */}
          <div className={`signup-form-border ${isHovered ? 'hovered' : ''}`} />
          
          <div className="signup-form-inner">
            <h3 className="signup-form-title">Tạo tài khoản</h3>
            
            <form onSubmit={handleQuickSignup} className="signup-form">
              <label className="signup-input-label" htmlFor="signup-email">
                Địa chỉ email
              </label>
              <div className="signup-input-group">
                <input
                  id="signup-email"
                  type="email"
                  placeholder="example@email.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="signup-input"
                  required
                />
                <button type="submit" className="signup-submit-btn">
                  <span>Bắt đầu</span>
                  <FaArrowRight className="btn-arrow" />
                </button>
              </div>
            </form>

            <div className="signup-divider">
              <span>hoặc tiếp tục với</span>
            </div>

            <div className="signup-social-buttons signup-social-buttons--single">
              <button
                className="social-btn social-btn--google"
                onClick={() => handleNavigate('/login')}
              >
                <FaGoogle />
                <span>Đăng nhập với Google</span>
              </button>
            </div>

            <p className="signup-terms">
              Bằng việc đăng ký, bạn đồng ý với{' '}
              <a href="#">Điều khoản sử dụng</a> và{' '}
              <a href="#">Chính sách bảo mật</a>
            </p>

            <div className="signup-login-link">
              Đã có tài khoản?{' '}
              <a href="/login" onClick={(e) => { e.preventDefault(); handleNavigate('/login'); }}>
                Đăng nhập ngay
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default SignupSection;
