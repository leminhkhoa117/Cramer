import React, { useState, useRef, useEffect } from 'react';
import { FaUserPlus, FaClipboardCheck, FaGraduationCap, FaArrowRight } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

const GuideCard = ({ step, icon: Icon, title, description, index }) => {
  const cardRef = useRef(null);
  const [transform, setTransform] = useState('');
  const [glareStyle, setGlareStyle] = useState({});
  const [isHovering, setIsHovering] = useState(false);
  const currentRotationRef = useRef({ x: 0, y: 0 });
  const animationFrameRef = useRef(null);

  const cardColors = [
    { bg: 'linear-gradient(135deg, #7c3aed 0%, #6366f1 100%)', shadow: 'rgba(124, 58, 237, 0.35)' },
    { bg: 'linear-gradient(135deg, #6366f1 0%, #3b82f6 100%)', shadow: 'rgba(99, 102, 241, 0.35)' },
    { bg: 'linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%)', shadow: 'rgba(139, 92, 246, 0.35)' },
  ];

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
    const lerpFactor = 0.15; // Lower = smoother
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
      className={`guide-card guide-card-3d ${isHovering ? 'is-hovering' : ''}`}
      style={{
        animationDelay: `${index * 0.1}s`,
        transform: transform || undefined,
        opacity: isHovering ? 1 : undefined,
      }}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
    >
      <div
        className="guide-card-inner"
        style={{
          background: cardColors[index].bg,
          boxShadow: `0 20px 40px ${cardColors[index].shadow}`,
        }}
      >
        <div className="guide-card-glare" style={glareStyle} />
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
    <section ref={sectionRef} className="guide-section">
      <div className="guide-container">
        {/* Header */}
        <div 
          ref={headerRef}
          className={`guide-header ${headerInView ? 'in-view' : ''}`}
        >
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
        <div className={`guide-cards-grid ${isInView ? 'in-view' : ''}`}>
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
