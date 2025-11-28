import React, { useState, useRef } from 'react';
import { FaBookOpen, FaRobot, FaRoute, FaChartLine } from 'react-icons/fa';

const FeatureCard = ({ icon: Icon, title, description, gradient }) => {
  const cardRef = useRef(null);
  const [transform, setTransform] = useState('');
  const [glareStyle, setGlareStyle] = useState({});

  const handleMouseMove = (e) => {
    if (!cardRef.current) return;
    
    const rect = cardRef.current.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    
    const mouseX = e.clientX - centerX;
    const mouseY = e.clientY - centerY;
    
    const rotateXVal = (mouseY / (rect.height / 2)) * -8;
    const rotateYVal = (mouseX / (rect.width / 2)) * 8;
    
    setTransform(`perspective(1000px) rotateX(${rotateXVal}deg) rotateY(${rotateYVal}deg) scale(1.02)`);
    
    const glareX = ((e.clientX - rect.left) / rect.width) * 100;
    const glareY = ((e.clientY - rect.top) / rect.height) * 100;
    setGlareStyle({
      background: `radial-gradient(circle at ${glareX}% ${glareY}%, rgba(255,255,255,0.3) 0%, transparent 50%)`,
    });
  };

  const handleMouseLeave = () => {
    setTransform('');
    setGlareStyle({});
  };

  return (
    <div
      ref={cardRef}
      className="feature-card-3d"
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      style={{ transform }}
    >
      <div className="feature-card-border" />
      <div className="feature-card-glare" style={glareStyle} />
      <div className="feature-card-inner">
        <div className={`feature-icon-wrapper ${gradient}`}>
          <Icon className="feature-icon" />
        </div>
        <h3 className="feature-title">{title}</h3>
        <p className="feature-description">{description}</p>
      </div>
    </div>
  );
};

const FeaturesSection = () => {
  const features = [
    {
      icon: FaBookOpen,
      title: 'Luyện thi IELTS',
      description: 'Hơn 1000+ đề thi từ Cambridge, British Council và IDP. Trải nghiệm thi thử như phòng thi thật.',
      gradient: 'gradient-purple',
    },
    {
      icon: FaRobot,
      title: 'AI Đánh giá',
      description: 'Công nghệ AI phân tích bài làm, chấm điểm tự động và đưa ra nhận xét chi tiết cho từng kỹ năng.',
      gradient: 'gradient-blue',
    },
    {
      icon: FaRoute,
      title: 'Lộ trình cá nhân',
      description: 'Lộ trình học tập được thiết kế riêng dựa trên điểm mạnh, điểm yếu và mục tiêu của bạn.',
      gradient: 'gradient-teal',
    },
    {
      icon: FaChartLine,
      title: 'Theo dõi tiến độ',
      description: 'Biểu đồ trực quan, báo cáo chi tiết giúp bạn theo dõi sự tiến bộ một cách hiệu quả.',
      gradient: 'gradient-orange',
    },
  ];

  return (
    <section className="features-section">
      <div className="features-container">
        <div className="features-header">
          <span className="features-label">Tính năng nổi bật</span>
          <h2 className="features-title">
            Tất cả những gì bạn cần
            <br />
            <span className="text-gradient">để chinh phục IELTS</span>
          </h2>
          <p className="features-subtitle">
            Cramer tích hợp đầy đủ công cụ và tài nguyên để giúp bạn đạt band điểm mơ ước
          </p>
        </div>

        <div className="features-grid">
          {features.map((feature, index) => (
            <FeatureCard key={index} {...feature} />
          ))}
        </div>
      </div>
    </section>
  );
};

export default FeaturesSection;
