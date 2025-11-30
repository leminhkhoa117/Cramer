import React, { useState, useRef, useEffect } from 'react';
import { FaBookOpen, FaRobot, FaRoute, FaChartLine } from 'react-icons/fa';

const FeatureCard = ({ icon: Icon, title, description, gradient }) => {
  const cardRef = useRef(null);
  const [transform, setTransform] = useState('');
  const [glareStyle, setGlareStyle] = useState({});
  const [isHovering, setIsHovering] = useState(false);
  const currentRotationRef = useRef({ x: 0, y: 0 });

  const handleMouseMove = (e) => {
    if (!cardRef.current) return;
    
    if (!isHovering) {
      setIsHovering(true);
    }
    
    const rect = cardRef.current.getBoundingClientRect();
    
    // Clamp mouse coordinates to card bounds
    const mouseX = Math.max(0, Math.min(rect.width, e.clientX - rect.left)) - rect.width / 2;
    const mouseY = Math.max(0, Math.min(rect.height, e.clientY - rect.top)) - rect.height / 2;
    
    // Calculate target rotation
    const targetRotateX = (mouseY / (rect.height / 2)) * -15;
    const targetRotateY = (mouseX / (rect.width / 2)) * 15;
    
    // Smooth interpolation (lerp) - using ref for current values
    const lerpFactor = 0.15; // Lower = smoother (matching guide cards)
    const smoothRotateX = currentRotationRef.current.x + (targetRotateX - currentRotationRef.current.x) * lerpFactor;
    const smoothRotateY = currentRotationRef.current.y + (targetRotateY - currentRotationRef.current.y) * lerpFactor;
    
    // Update ref
    currentRotationRef.current.x = smoothRotateX;
    currentRotationRef.current.y = smoothRotateY;
    
    // Clamp rotation values to prevent extreme angles
    const rotateXVal = Math.max(-15, Math.min(15, smoothRotateX));
    const rotateYVal = Math.max(-15, Math.min(15, smoothRotateY));
    const translateZVal = 20;
    
    const transformValue = `perspective(1000px) rotateX(${rotateXVal}deg) rotateY(${rotateYVal}deg) translateZ(${translateZVal}px) scale(1.05)`;
    setTransform(transformValue);
    
    const glareX = ((Math.max(0, Math.min(rect.width, e.clientX - rect.left)) / rect.width) * 100);
    const glareY = ((Math.max(0, Math.min(rect.height, e.clientY - rect.top)) / rect.height) * 100);
    setGlareStyle({
      background: `radial-gradient(circle at ${glareX}% ${glareY}%, rgba(255,255,255,0.3) 0%, transparent 50%)`,
    });
  };

  const handleMouseLeave = () => {
    // Reset rotation ref
    currentRotationRef.current.x = 0;
    currentRotationRef.current.y = 0;
    
    // Smoothly return to default state
    setTransform('perspective(1000px) rotateX(0deg) rotateY(0deg) translateZ(0px) scale(1)');
    setGlareStyle({});
    setIsHovering(false);
    
    // Reset after transition completes (match the transition duration)
    setTimeout(() => {
      setTransform('');
    }, 400);
  };

  return (
    <div
      ref={cardRef}
      className={`feature-card-3d ${isHovering ? 'is-hovering' : ''}`}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      style={{ 
        transform: transform || undefined,
        opacity: isHovering ? 1 : undefined,
      }}
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
  const sectionRef = useRef(null);
  const headerRef = useRef(null);
  const [isInView, setIsInView] = useState(false);
  const [headerInView, setHeaderInView] = useState(false);

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

    const headerObserver = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setHeaderInView(true);
          headerObserver.unobserve(entry.target);
        }
      },
      { threshold: 0.2 }
    );

    if (sectionRef.current) {
      observer.observe(sectionRef.current);
    }
    if (headerRef.current) {
      headerObserver.observe(headerRef.current);
    }

    return () => {
      if (sectionRef.current) observer.unobserve(sectionRef.current);
      if (headerRef.current) headerObserver.unobserve(headerRef.current);
    };
  }, []);

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
    <section ref={sectionRef} className="features-section">
      <div className="features-container">
        <div 
          ref={headerRef}
          className={`features-header ${headerInView ? 'in-view' : ''}`}
        >
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

        <div className={`features-grid ${isInView ? 'in-view' : ''}`}>
          {features.map((feature, index) => (
            <FeatureCard key={index} {...feature} index={index} />
          ))}
        </div>
      </div>
    </section>
  );
};

export default FeaturesSection;
