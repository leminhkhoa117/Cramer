import React, { useState, useRef, useEffect } from 'react';
import { motion, useInView } from 'framer-motion';
import { FaGoogle, FaCheckCircle, FaRocket, FaShieldAlt, FaClock } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

const SignupSection = () => {
  const navigate = useNavigate();
  const sectionRef = useRef(null);
  const isInView = useInView(sectionRef, { once: true, margin: "-100px" });

  const handleNavigate = (path) => {
    window.scrollTo(0, 0);
    navigate(path);
  };

  const benefits = [
    { icon: FaRocket, text: 'Bắt đầu luyện tập ngay lập tức' },
    { icon: FaShieldAlt, text: 'Bảo mật với tài khoản Google' },
    { icon: FaClock, text: 'Đăng ký chỉ trong 5 giây' },
  ];

  return (
    <section ref={sectionRef} className="signup-section signup-section--redesigned">
      {/* Background decorations */}
      <div className="signup-bg-gradient" />
      <div className="signup-floating-orb signup-floating-orb--1" />
      <div className="signup-floating-orb signup-floating-orb--2" />

      <div className="signup-container signup-container--centered">
        <motion.div
          className="signup-card"
          initial={{ opacity: 0, y: 40, scale: 0.95 }}
          animate={isInView ? { opacity: 1, y: 0, scale: 1 } : {}}
          transition={{ duration: 0.7, ease: "easeOut" }}
        >
          {/* Top badge */}
          <div className="signup-badge-wrapper">
            <span className="signup-badge">🎯 Miễn phí</span>
          </div>

          {/* Main content */}
          <h2 className="signup-card-title">
            Bắt đầu hành trình
            <br />
            <span className="text-gradient">chinh phục IELTS</span>
          </h2>

          <p className="signup-card-description">
            Tham gia cùng <strong>10,000+ học viên</strong> đã đạt được band điểm mơ ước với Cramer
          </p>

          {/* Benefits list */}
          <div className="signup-benefits">
            {benefits.map((benefit, index) => (
              <motion.div
                key={index}
                className="signup-benefit-item"
                initial={{ opacity: 0, x: -20 }}
                animate={isInView ? { opacity: 1, x: 0 } : {}}
                transition={{ delay: 0.3 + index * 0.1 }}
              >
                <FaCheckCircle className="benefit-check" />
                <span>{benefit.text}</span>
              </motion.div>
            ))}
          </div>

          {/* Google Sign-in Button - Primary CTA */}
          <motion.button
            className="signup-google-btn"
            onClick={() => handleNavigate('/login')}
            whileHover={{ scale: 1.02, y: -2 }}
            whileTap={{ scale: 0.98 }}
          >
            <div className="google-icon-wrapper">
              <FaGoogle />
            </div>
            <span>Đăng ký với Google</span>
          </motion.button>

          {/* Terms */}
          <p className="signup-terms-text">
            Bằng việc đăng ký, bạn đồng ý với{' '}
            <a href="#">Điều khoản sử dụng</a> và{' '}
            <a href="#">Chính sách bảo mật</a>
          </p>

          {/* Already have account */}
          <div className="signup-login-prompt">
            Đã có tài khoản?{' '}
            <a
              href="/login"
              onClick={(e) => { e.preventDefault(); handleNavigate('/login'); }}
              className="signup-login-link"
            >
              Đăng nhập
            </a>
          </div>
        </motion.div>

        {/* Stats row below card */}
        <motion.div
          className="signup-stats-row"
          initial={{ opacity: 0, y: 20 }}
          animate={isInView ? { opacity: 1, y: 0 } : {}}
          transition={{ delay: 0.5 }}
        >
          <div className="signup-stat">
            <span className="signup-stat-number">10,000+</span>
            <span className="signup-stat-label">Học viên</span>
          </div>
          <div className="signup-stat-divider" />
          <div className="signup-stat">
            <span className="signup-stat-number">95%</span>
            <span className="signup-stat-label">Hài lòng</span>
          </div>
          <div className="signup-stat-divider" />
          <div className="signup-stat">
            <span className="signup-stat-number">7.0+</span>
            <span className="signup-stat-label">Band TB đạt được</span>
          </div>
        </motion.div>
      </div>
    </section>
  );
};

export default SignupSection;
