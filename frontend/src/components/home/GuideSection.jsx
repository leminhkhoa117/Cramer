import React from 'react';
import { FaUserPlus, FaClipboardCheck, FaGraduationCap, FaArrowRight } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

const GuideCard = ({ step, icon: Icon, title, description, index }) => {
  const cardColors = [
    { bg: 'linear-gradient(135deg, #7c3aed 0%, #6366f1 100%)', shadow: 'rgba(124, 58, 237, 0.35)' },
    { bg: 'linear-gradient(135deg, #6366f1 0%, #3b82f6 100%)', shadow: 'rgba(99, 102, 241, 0.35)' },
    { bg: 'linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%)', shadow: 'rgba(139, 92, 246, 0.35)' },
  ];

  return (
    <div 
      className="guide-card"
      style={{ 
        animationDelay: `${index * 0.1}s`,
      }}
    >
      <div 
        className="guide-card-inner"
        style={{
          background: cardColors[index].bg,
          boxShadow: `0 20px 40px ${cardColors[index].shadow}`,
        }}
      >
        {/* Step number badge */}
        <div className="guide-step-badge">
          <span>{step}</span>
        </div>

        {/* Icon */}
        <div className="guide-icon-wrapper">
          <Icon className="guide-icon" />
        </div>

        {/* Content */}
        <div className="guide-card-content">
          <h3 className="guide-card-title">{title}</h3>
          <p className="guide-card-description">{description}</p>
        </div>

        {/* Decorative elements */}
        <div className="guide-card-decoration">
          <div className="decoration-circle decoration-circle--1" />
          <div className="decoration-circle decoration-circle--2" />
        </div>

        {/* Shine effect */}
        <div className="guide-card-shine" />
      </div>
    </div>
  );
};

const GuideSection = () => {
  const navigate = useNavigate();

  const handleStartClick = () => {
    navigate('/login');
    window.scrollTo(0, 0);
  };

  const steps = [
    {
      step: '01',
      icon: FaUserPlus,
      title: 'Đăng ký tài khoản',
      description: 'Tạo tài khoản miễn phí chỉ trong 30 giây. Bạn có thể đăng ký bằng email hoặc tài khoản Google.',
    },
    {
      step: '02',
      icon: FaClipboardCheck,
      title: 'Làm bài test đầu vào',
      description: 'Hoàn thành bài kiểm tra đánh giá năng lực để hệ thống hiểu rõ trình độ hiện tại của bạn.',
    },
    {
      step: '03',
      icon: FaGraduationCap,
      title: 'Nhận lộ trình học tập',
      description: 'AI phân tích kết quả và tạo lộ trình học tập cá nhân hóa, giúp bạn cải thiện từng kỹ năng.',
    },
  ];

  return (
    <section className="guide-section">
      <div className="guide-container">
        {/* Header */}
        <div className="guide-header">
          <span className="guide-label">Bắt đầu dễ dàng</span>
          <h2 className="guide-title">
            Chỉ <span className="text-gradient">3 bước đơn giản</span>
            <br />
            để bắt đầu hành trình
          </h2>
          <p className="guide-subtitle">
            Không cần chuẩn bị gì phức tạp, Cramer sẽ hướng dẫn bạn từng bước một
          </p>
        </div>

        {/* Cards */}
        <div className="guide-cards-grid">
          {steps.map((step, index) => (
            <GuideCard key={index} {...step} index={index} />
          ))}
        </div>

        {/* CTA Button */}
        <div className="guide-cta">
          <button 
            onClick={handleStartClick}
            className="guide-cta-btn"
          >
            <span>Bắt đầu ngay</span>
            <FaArrowRight className="cta-arrow" />
          </button>
        </div>
      </div>
    </section>
  );
};

export default GuideSection;
